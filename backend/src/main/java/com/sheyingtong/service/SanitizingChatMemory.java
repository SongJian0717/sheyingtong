package com.sheyingtong.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ChatMemory 装饰器：在读取侧按"链路"清洗会话记忆。文字链路与图片链路各自包一层，共用同一份底层记忆。
 * ----------------------------------------------------------------
 * 写入侧：**原样落库**（不再剥图）。图片必须留在记忆里，"记住图片"才有内容可用；
 *        读取侧再按链路决定给不给模型看。
 *
 * 读取侧做两件事：
 *  ① 丢掉重复的 SystemMessage（只保留最后一份）
 *     ⚠️ 机制（离线实测的调用顺序，很反直觉）：
 *         memory.add(SYSTEM) → memory.messages()【请求就在这里组装】→ memory.add(USER) → memory.add(AI)
 *     也就是说 **本轮用户消息是在请求组装之后才写进记忆的**，所以 messages() 里永远看不到本轮内容；
 *     同时又必须保留那份刚写入的 SYSTEM —— 它正是本轮系统指令的来源（写入侧跳过它会让请求里 SystemMessage=0）。
 *     重复的来源是"每轮写一份 + 每轮从记忆组装"：实测 3 轮后请求为
 *     [SYSTEM,USER,AI, SYSTEM,USER,AI, SYSTEM,USER+图]，同一份指令出现 3 次。过滤后每轮只发 1 份。
 *
 *  ② 按 {@link ImagePolicy} 裁剪图片
 *     · TEXT_ONLY（文字链路）：所有图片换成文字占位。
 *       硬约束：qwen-plus 是非 VL 模型，收到 image 内容会直接报错。
 *     · KEEP_RECENT（图片链路）：只保留最近 keepImages 张**历史**图片，更早的换成文字占位。
 *       为什么必须裁剪：模型请求是"整段记忆 + 本轮消息"，历史图每一轮都会被重新上传。
 *       实测（1.5MB 图 / 单张 base64 1.96MB）：1 张 2.75s / 3 张 4.47s / 6 张 7.46s，约每张 +0.9s，
 *       且每轮都要重付一次并重复计费。
 *       ⚠️ keepImages 只约束**历史**：上传新图的那一轮，本轮图片不在 messages() 里（见上），
 *          框架会把它追加进请求，所以那一轮实际带 keepImages + 1 张（新图 + 最近的历史图）。
 *          追问轮没有新图，实际就是 keepImages 张。
 *
 * 代价：被裁剪的历史图不再重放，只保留文字占位；当前提问的图片不受影响（由 AiServices 拼进当轮 UserMessage）。
 *
 *  ③ 拼上"较早对话摘要"（可选，由 {@code summarySupplier} 提供）
 *     记忆被裁剪（条数或 token 超预算）时，RedisChatMemoryStore 会把丢掉的那段交给 ChatSummaryService
 *     压成 150~300 字摘要，存在 sheyingtong:chat:summary:{sessionId}。
 *     这里把它拼到 system prompt 最前面 —— 只发一份 system，比额外插一条 SystemMessage 更稳，
 *     也让"已经被丢掉的早期内容"以极低成本继续可用。
 */
public class SanitizingChatMemory implements ChatMemory {

    /** 读取侧图片策略：图片链路保留最近 N 张，文字链路一张都不给 */
    public enum ImagePolicy {
        KEEP_RECENT,
        TEXT_ONLY
    }

    private static final String IMAGE_PLACEHOLDER = "[图片：这轮对话曾上传过一张图片，此处只保留文字记录]";

    /** 较早对话摘要放在 system prompt 最前面的前缀 */
    private static final String SUMMARY_PREFIX = "[较早对话的摘要，请据此保持前后连贯；若与最近消息冲突，以最近消息为准]\n";

    private final ChatMemory delegate;
    private final ImagePolicy imagePolicy;
    private final int keepImages;
    /** 读"较早对话摘要"；为 null 或返回空表示没有摘要 */
    private final Supplier<String> summarySupplier;

    public SanitizingChatMemory(ChatMemory delegate, ImagePolicy imagePolicy, int keepImages) {
        this(delegate, imagePolicy, keepImages, null);
    }

    public SanitizingChatMemory(ChatMemory delegate, ImagePolicy imagePolicy, int keepImages,
                                Supplier<String> summarySupplier) {
        this.delegate = delegate;
        this.imagePolicy = imagePolicy;
        this.keepImages = keepImages;
        this.summarySupplier = summarySupplier;
    }

    @Override
    public Object id() {
        return delegate.id();
    }

    /** 写入侧原样落库：SystemMessage 与图片都留着，裁剪推迟到读取时按链路做 */
    @Override
    public void add(ChatMessage message) {
        delegate.add(message);
    }

    @Override
    public List<ChatMessage> messages() {
        List<ChatMessage> stored = delegate.messages();
        String summary = (summarySupplier == null) ? null : summarySupplier.get();
        boolean hasSummary = summary != null && !summary.isBlank();

        int lastSystemIndex = -1;
        for (int i = 0; i < stored.size(); i++) {
            if (stored.get(i) instanceof SystemMessage) {
                lastSystemIndex = i;
            }
        }

        Map<Integer, Integer> keepPerMessage = planImageRetention(stored);

        List<ChatMessage> sanitized = new ArrayList<>(stored.size() + 1);
        boolean systemAdded = false;
        for (int i = 0; i < stored.size(); i++) {
            ChatMessage message = stored.get(i);
            if (message instanceof SystemMessage) {
                if (i == lastSystemIndex) {
                    // ③ 把"较早对话摘要"拼到 system prompt 最前面：只有一份 system，
                    //    比额外插一条 SystemMessage 更稳（部分兼容层对多 system 处理不一致）
                    sanitized.add(hasSummary
                            ? SystemMessage.from(SUMMARY_PREFIX + summary + "\n\n" + ((SystemMessage) message).text())
                            : message);
                    systemAdded = true;
                }
                continue;
            }
            sanitized.add(applyImagePolicy(message, keepPerMessage.getOrDefault(i, 0)));
        }

        // 极端情况（记忆里没有 system prompt）也要让摘要送到
        if (!systemAdded && hasSummary) {
            sanitized.add(0, SystemMessage.from(SUMMARY_PREFIX + summary));
        }
        return sanitized;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * 从最新一条往前分配"可以保留的图片"名额，得到 消息下标 → 该消息保留几张图。
     * 名额 = TEXT_ONLY 为 0；KEEP_RECENT 为 keepImages。
     */
    private Map<Integer, Integer> planImageRetention(List<ChatMessage> stored) {
        Map<Integer, Integer> keepPerMessage = new HashMap<>();
        int remaining = imagePolicy == ImagePolicy.TEXT_ONLY ? 0 : Math.max(keepImages, 0);
        if (remaining == 0) {
            return keepPerMessage;
        }
        for (int i = stored.size() - 1; i >= 0 && remaining > 0; i--) {
            int imagesInMessage = countImages(stored.get(i));
            if (imagesInMessage == 0) {
                continue;
            }
            int keep = Math.min(imagesInMessage, remaining);
            keepPerMessage.put(i, keep);
            remaining -= keep;
        }
        return keepPerMessage;
    }

    /** 按名额处理单条消息：超出名额的 ImageContent 换成文字占位（从最早的开始换） */
    private static ChatMessage applyImagePolicy(ChatMessage message, int keep) {
        if (!(message instanceof UserMessage userMessage) || countImages(userMessage) == 0) {
            return message;
        }
        List<Content> contents = userMessage.contents();
        int toReplace = countImages(userMessage) - keep;

        List<Content> rebuilt = new ArrayList<>(contents.size());
        for (Content content : contents) {
            if (content instanceof ImageContent && toReplace > 0) {
                toReplace--;
                rebuilt.add(TextContent.from(IMAGE_PLACEHOLDER));
            } else {
                rebuilt.add(content);
            }
        }
        return UserMessage.from(rebuilt.toArray(new Content[0]));
    }

    private static int countImages(ChatMessage message) {
        if (!(message instanceof UserMessage userMessage)) {
            return 0;
        }
        int count = 0;
        for (Content content : userMessage.contents()) {
            if (content instanceof ImageContent) {
                count++;
            }
        }
        return count;
    }
}