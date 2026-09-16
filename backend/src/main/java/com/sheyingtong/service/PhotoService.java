package com.sheyingtong.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sheyingtong.dto.response.AiTaskSubmitResponse;
import com.sheyingtong.dto.response.PhotoScoreResponse;
import com.sheyingtong.entity.AiBizType;
import com.sheyingtong.entity.AiTask;
import com.sheyingtong.entity.PhotoAnalysis;
import com.sheyingtong.entity.User;
import com.sheyingtong.repository.PhotoAnalysisRepository;
import com.sheyingtong.repository.UserRepository;
import com.sheyingtong.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 照片打分领域服务
 * ----------------------------------------------------------------
 * 异步化改造后职责边界：
 *  - **提交**（submitScore）：落盘原图 → 建 ai_task 任务 → 投消息 → 立即返回 taskId。
 *    真正的图片压缩、Qwen-VL 调用发生在 PhotoScoreConsumer 里，期间不持有任何 DB 事务。
 *  - **持久化**（persistScore / persistMockScore / toResponse）：供消费者写结果，也供查询接口复用。
 *  - 查询 / 删除（getHistory / getAnalysisById / deleteAnalysis）保持不变。
 *
 * Mock 降级保留在这里：AI 彻底失败且允许降级时，消费者调用 persistMockScore 写兜底数据，
 * 但状态会标成 DEGRADED 而不是 SUCCESS —— 降级从"静默发生"变成"显式可见"。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoService {

    private final PhotoAnalysisRepository photoAnalysisRepository;
    private final UserRepository userRepository;
    private final WorkRepository workRepository;
    private final AiTaskService aiTaskService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * 照片打分提交（原 analyzePhoto 的入口）。
     * ----------------------------------------------------------------
     * ⚠️ 刻意**不**加 @Transactional：整个方法只有 aiTaskService.submit 内部建任务那一小段
     * 是事务，文件落盘与消息投递都在事务外。改造前这里用 @Transactional 包住了整段
     * AI 调用（最坏 9 分钟），期间把 Hikari 连接和 DB 事务全占住，是这个项目最大的
     * 架构风险点 —— 并发 10 个上传就会打满默认连接池拖垮全站。
     *
     * @return 异步任务提交响应，前端拿 taskId 轮询 GET /api/ai/tasks/{taskId}
     */
    public AiTaskSubmitResponse submitScore(MultipartFile file, String description, String username) {
        log.info("提交照片打分任务，文件名: {}, 用户: {}", file.getOriginalFilename(), username);

        String filePath = saveFile(file);
        log.info("照片已落盘: {}", filePath);

        return aiTaskService.submit(AiBizType.PHOTO_SCORE, filePath, file.getOriginalFilename(), description, username);
    }

    /**
     * 消费者调用：把 AI 解析结果持久化成 PhotoAnalysis 并返回落地实体。
     * 调用方（消费者线程）不是事务方法，这里单条 save 自成一个短事务、立即提交。
     */
    public PhotoAnalysis persistScore(AiTask task, Map<String, Object> aiResult) {
        User user = task.getUser();

        int score = (Integer) aiResult.getOrDefault("score", 75);
        String analysis;
        try {
            analysis = aiResult.get("analysis") != null
                    ? objectMapper.writeValueAsString(aiResult.get("analysis")) : "{}";
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            analysis = "{}";
            log.warn("序列化analysis失败: {}", e.getMessage());
        }
        String suggestions = (String) aiResult.getOrDefault("suggestions", "");

        PhotoAnalysis photoAnalysis = PhotoAnalysis.builder()
                .user(user)
                .filePath(task.getFilePath())
                .fileName(task.getFileName())
                .description(task.getDescription())
                .score(score)
                .analysis(analysis)
                .suggestions(suggestions)
                .build();

        PhotoAnalysis saved = photoAnalysisRepository.save(photoAnalysis);
        log.info("照片打分结果已入库: taskId={}, 记录ID={}, 评分={}", task.getId(), saved.getId(), score);
        return saved;
    }

    /** 消费者调用：AI 彻底失败且允许降级时，写入兜底(mock)数据 */
    public PhotoAnalysis persistMockScore(AiTask task) {
        return persistScore(task, generateMockAIResult());
    }

    /**
     * 把持久化的 PhotoAnalysis 组装成前端要的响应结构。
     * 同时被 GET /api/photo/detail/{id} 与消费者（写 result_json）复用。
     */
    public PhotoScoreResponse toResponse(PhotoAnalysis analysis) {
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> analysisMap = analysis.getAnalysis() != null
                ? safeParseAnalysis(analysis.getAnalysis())
                : Map.of();

        return PhotoScoreResponse.builder()
                .score(analysis.getScore())
                .analysis(Map.of(
                        "composition", analysisMap.getOrDefault("composition", generateCompositionAnalysis(analysis.getScore())),
                        "lighting", analysisMap.getOrDefault("lighting", generateLightingAnalysis(analysis.getScore())),
                        "color", analysisMap.getOrDefault("color", generateColorAnalysis(analysis.getScore())),
                        "focus", analysisMap.getOrDefault("focus", generateFocusAnalysis(analysis.getScore())),
                        "content", analysisMap.getOrDefault("content", generateContentAnalysis(analysis.getScore())),
                        "suggestion", analysisMap.getOrDefault("suggestion", generateSuggestionAnalysis(analysis.getScore()))
                ))
                .suggestions(analysis.getSuggestions())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> safeParseAnalysis(String analysisJson) {
        try {
            return objectMapper.readValue(analysisJson,
                    new TypeReference<Map<String, Map<String, Object>>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 解析 AI 返回的 JSON 文本 → Map。
     * 消费者调用。注意：
     *  - 剥离 markdown fences（模型偶尔会包 ```json ... ```）；
     *  - suggestions 可能返回数组，这里统一转成 "；" 分隔的字符串；
     *  - **解析失败直接抛异常**（不再静默返回 mock）——mock 降级只能走 DEGRADED
     *    路径显式发生，否则"拿 mock 数据标 SUCCESS"会掩盖 AI 质量问题。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseAIResponse(String response) {
        try {
            response = response.trim();
            if (response.startsWith("```json")) {
                response = response.substring(7);
            }
            if (response.endsWith("```")) {
                response = response.substring(0, response.length() - 3);
            }
            response = response.trim();
            Map<String, Object> result = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            
            Object suggestionsObj = result.get("suggestions");
            if (suggestionsObj instanceof List) {
                List<String> suggestionsList = (List<String>) suggestionsObj;
                String suggestionsStr = String.join("；", suggestionsList);
                result.put("suggestions", suggestionsStr);
            }
            
            return result;
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", e.getMessage());
            throw new RuntimeException("AI 响应解析失败: " + e.getMessage(), e);
        }
    }
    
    private Map<String, Object> generateMockAIResult() {
        int score = generateMockScore();
        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("analysis", Map.of(
                "composition", generateCompositionAnalysis(score),
                "lighting", generateLightingAnalysis(score),
                "color", generateColorAnalysis(score),
                "focus", generateFocusAnalysis(score),
                "content", generateContentAnalysis(score),
                "suggestion", generateSuggestionAnalysis(score)
        ));
        result.put("suggestions", generateMockSuggestions(score));
        return result;
    }
    
    private Map<String, Object> generateContentAnalysis(int score) {
        int contentScore = Math.max(60, Math.min(100, score + (int) (Math.random() * 10) - 5));
        String comment;
        if (score >= 95) {
            String[] comments = {
                "照片充满故事性，作者想要表达的情感和想法非常清晰，让人印象深刻",
                "画面极具感染力，内涵丰富，每一个细节都在诉说故事",
                "这是一张有灵魂的照片，完美传达了作者的创作意图"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (score >= 90) {
            String[] comments = {
                "照片主题明确，作者的表达意图清晰可见，画面富有感染力",
                "内涵丰富，构图和内容完美结合，能够引起观者共鸣",
                "故事性强，通过画面成功传递了作者想要表达的情感"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (score >= 80) {
            String[] comments = {
                "照片主题明确，作者想要表达的想法基本清晰，趣味性较好",
                "画面内容充实，叙事能力较强，能够传达一定的情感",
                "内涵尚可，建议在主题表达上更加突出，增强画面的故事性"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (score >= 70) {
            String[] comments = {
                "照片内容较为普通，建议挖掘更有深度的主题和表达角度",
                "作者想要表达的想法不够清晰，建议加强叙事能力",
                "画面趣味性一般，建议寻找更独特的视角和表达方式"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else {
            String[] comments = {
                "照片内容缺乏深度，建议思考想要表达的主题和情感",
                "画面较为空洞，缺乏内涵和故事性，建议加强前期策划",
                "作者的表达意图不清晰，建议在拍摄前明确创作主题"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        }
        return Map.of("score", contentScore, "comment", comment);
    }
    
    private Map<String, Object> generateSuggestionAnalysis(int score) {
        int suggestionScore = score >= 95 ? 100 : Math.max(60, Math.min(95, score + 5));
        String comment;
        if (score >= 95) {
            comment = "这张照片非常出色，无需修改！";
        } else if (score >= 90) {
            String[] comments = {
                "照片整体表现优秀，建议微调白平衡以获得更准确的色彩还原",
                "构图和光线都很棒，可以尝试不同角度拍摄增加多样性",
                "画面质量很高，建议适当增加前景元素丰富层次"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (score >= 80) {
            String[] comments = {
                "建议从更低角度拍摄，可以增强主体的气势和画面层次感",
                "注意高光区域控制，避免过曝导致细节丢失",
                "可以尝试使用引导线构图，增强视觉引导效果",
                "建议调整白平衡，让肤色/环境色更自然",
                "适当缩小光圈增加景深，让前后景都更清晰"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (score >= 70) {
            String[] comments = {
                "建议学习三分法构图，将主体放在画面交点位置",
                "注意光线方向，尝试使用侧光增强立体感",
                "调整色彩饱和度和对比度，增强画面冲击力",
                "检查对焦是否准确，确保主体清晰锐利",
                "尝试改变拍摄角度，寻找更有表现力的视角"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else {
            String[] comments = {
                "建议系统学习摄影基础知识，掌握曝光三要素",
                "注意构图法则，避免画面过于杂乱",
                "学习光线运用，避免背光拍摄导致主体过暗",
                "使用三脚架确保画面稳定，避免模糊",
                "建议在黄金时刻拍摄，利用柔和光线"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        }
        return Map.of("score", suggestionScore, "comment", comment);
    }
    
    public Page<PhotoAnalysis> getHistory(String username, Pageable pageable) {
        if (username == null || username.isEmpty()) {
            return photoAnalysisRepository.findAll(pageable);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return photoAnalysisRepository.findByUser(user, pageable);
    }
    
    public PhotoAnalysis getAnalysisById(Long id) {
        return photoAnalysisRepository.findById(id).orElse(null);
    }
    
    @Transactional
    public void deleteAnalysis(Long id) {
        PhotoAnalysis analysis = photoAnalysisRepository.findById(id).orElse(null);
        if (analysis != null) {
            // 社区联动：照片已发布为作品时禁止删除（作品图与 AI 评语都依赖这条打分记录）
            if (workRepository.existsByPhotoAnalysisId(id)) {
                throw new RuntimeException("删除失败，该照片已发布为作品，请先在『我的作品』中删除对应作品");
            }
            try {
                Files.deleteIfExists(Paths.get(analysis.getFilePath()));
            } catch (IOException e) {
                log.warn("删除照片文件失败: {}", e.getMessage());
            }
            photoAnalysisRepository.deleteById(id);
            log.info("删除照片分析记录: {}", id);
        }
    }
    
    private String saveFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    private int generateMockScore() {
        return 60 + (int) (Math.random() * 35);
    }
    
    private String generateMockAnalysis(int score) {
        if (score >= 90) {
            return "这是一张非常出色的照片！构图合理，光线运用得当，色彩搭配和谐，焦点清晰。整体表现优秀，几乎没有明显的瑕疵。";
        } else if (score >= 80) {
            return "这是一张很不错的照片。构图基本合理，光线运用较好，色彩搭配协调。虽然有些小瑕疵，但整体表现良好。";
        } else if (score >= 70) {
            return "这是一张合格的照片。构图基本合理，但还有改进空间。光线和色彩表现尚可，建议进一步优化。";
        } else {
            return "这张照片还有较大的提升空间。建议从构图、光线、色彩等方面进行改进，多参考优秀作品学习。";
        }
    }
    
    private String generateMockSuggestions(int score) {
        if (score >= 90) {
            return "继续保持！可以尝试更多创新的构图方式，比如尝试对角线构图或框架构图来增加画面的层次感和故事性。探索不同的拍摄角度，如低角度仰拍可以增强主体的气势，高角度俯拍可以展现场景的全貌。同时可以尝试使用不同的焦距和镜头，不断拓展自己的摄影风格。";
        } else if (score >= 80) {
            return "建议尝试优化构图，使用三分法将主体放在画面的交点位置，或者利用引导线将观众的视线引向主体。注意光线的运用，避免过曝或欠曝，在拍摄时可以使用曝光补偿来微调曝光。适当调整色彩饱和度和对比度，让画面更加生动有层次感。另外，可以尝试在画面中加入前景元素，增加画面的深度和立体感。";
        } else if (score >= 70) {
            return "建议学习基本构图法则，如三分法、对称构图、引导线构图等，并在实际拍摄中加以运用。注意光线方向和强度，尽量避免背光拍摄导致主体过暗，在拍摄人像时可以尝试使用反光板补光。控制好景深，根据拍摄需求选择合适的光圈大小。后期可以适当调整色彩和对比度，增强画面的表现力。";
        } else {
            return "建议系统学习摄影基础知识：掌握曝光三要素（光圈、快门、ISO）之间的关系，学会根据不同场景调整参数。学习构图法则，理解如何通过构图来引导观众视线、突出主体。了解光线运用，学会利用自然光和人造光来塑造画面。多练习多拍摄，积累经验，同时多看优秀作品学习借鉴，逐步提升自己的摄影水平。";
        }
    }
    
    private Map<String, Object> generateCompositionAnalysis(int score) {
        int compositionScore = Math.max(50, Math.min(100, score + (int) (Math.random() * 10) - 5));
        String comment;
        if (compositionScore >= 90) {
            String[] comments = {
                "构图非常出色！运用了三分法或引导线构图，画面均衡和谐，主体在视觉上非常突出，观众的视线能够自然地被引导到画面的核心区域。整体布局合理，留白恰到好处，给人舒适的视觉体验。",
                "构图精妙，黄金分割点运用恰当，画面层次分明，节奏感强。前景、中景和背景的关系处理得当，创造出良好的空间深度感。构图富有创意，展现了作者独特的视角和审美。",
                "对称构图完美，平衡感极佳，主体位置恰到好处，画面稳定而庄重。这种构图方式非常适合表现建筑、风光等对称场景，能够传达出一种宁静、和谐的美感。",
                "对角线构图增加了画面的动感和延伸感，视角独特，画面张力十足。对角线元素如道路、河流、栏杆等将观众的视线从画面一角引导至另一角，创造出强烈的视觉引导效果。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (compositionScore >= 80) {
            String[] comments = {
                "构图合理，基本符合构图法则，主体位置恰当。画面布局清晰，观众能够快速找到视觉焦点。虽然整体表现不错，但可以尝试更多创新的构图方式来增加画面的趣味性和独特性。",
                "三分法运用基本正确，地平线位置合理，画面均衡稳定。主体放置在三分线交点附近，符合视觉习惯。建议在构图时多加考虑前景元素的运用，以增加画面的层次感和深度。",
                "构图较为完整，主体突出，但可以尝试更多变化。当前构图虽然稳定，但略显保守。可以尝试使用框架构图、留白构图等方式，让画面更具艺术感染力。",
                "画面布局合理，但留白可以更讲究一些。适当的留白能够给观众留出想象空间，增强画面的意境。建议在拍摄时注意画面边缘元素的处理，避免画面过于拥挤。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (compositionScore >= 70) {
            String[] comments = {
                "构图基本合理，但主体不够突出。画面中存在一些干扰元素分散了观众的注意力。建议在拍摄时更加明确主体，使用大光圈虚化背景或者调整拍摄角度来突出主体。",
                "地平线位置偏高/偏低，建议调整到三分线位置。地平线的位置对画面的平衡感有很大影响，过高会使天空占据过多空间，过低则会使地面显得拥挤。",
                "画面略显杂乱，可以适当裁剪去除多余元素。画面中存在一些不必要的元素，影响了主体的表达。建议在拍摄时或后期处理中进行裁剪，使画面更加简洁有力。",
                "主体位置偏中，可以尝试偏移增加活力。将主体放在画面正中央虽然稳定，但容易显得死板。尝试使用三分法或黄金分割，让主体稍微偏移，能够增加画面的动感和活力。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else {
            String[] comments = {
                "构图有待改进，主体不明确，画面混乱。画面中没有明显的视觉焦点，观众难以确定作者想要表达的内容。建议在拍摄前明确主体，并通过构图方式来突出它。",
                "地平线切头/切脚，建议重新构图。地平线穿过人物头部或脚部会破坏画面的美感，使人物显得不完整。建议调整拍摄角度或位置，避免这种情况的发生。",
                "画面元素过多，缺乏主次，建议简化。画面中包含了太多的元素，没有明确的主次关系，导致视觉混乱。建议在拍摄时只保留最能表达主题的元素，去除干扰。",
                "构图过于死板，缺乏创意和变化。画面构图过于简单，缺乏层次感和视觉冲击力。建议学习更多的构图法则，如引导线、框架构图、对角线等，并在实际拍摄中灵活运用。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        }
        return Map.of("score", compositionScore, "comment", comment);
    }
    
    private Map<String, Object> generateLightingAnalysis(int score) {
        int lightingScore = Math.max(50, Math.min(100, score + (int) (Math.random() * 10) - 3));
        String comment;
        if (lightingScore >= 90) {
            String[] comments = {
                "光线运用得当！曝光准确，高光和暗部细节都得到很好保留。画面中光影层次丰富，明暗过渡自然平滑，没有出现死黑或死白的区域。光线很好地塑造了主体的立体感和质感，色彩还原真实自然，整体氛围营造出色。",
                "光影层次分明，立体感强，明暗过渡自然。光线从侧面照射主体，形成了丰富的光影对比，突出了主体的轮廓和质感。暗部细节清晰可见，高光部分也没有溢出，展现了出色的曝光控制能力。",
                "黄金时刻拍摄，光线柔和温暖，氛围营造极佳。清晨或傍晚的金色阳光赋予画面温暖的色调，光线角度低，能够创造出长长的阴影，增加画面的层次感和故事性。这种光线条件非常适合人像和风光摄影。",
                "侧光运用出色，轮廓清晰，质感表现突出。侧光能很好地表现物体的纹理和质感，创造出强烈的立体感。画面中光影对比明显，但不过分，暗部和高光都保留了丰富的细节，展现了精湛的用光技巧。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (lightingScore >= 80) {
            String[] comments = {
                "光线表现良好，基本曝光准确，明暗对比适中。画面整体曝光正常，主体清晰可见，色彩还原基本准确。虽然没有特别出彩的光影效果，但整体表现稳定，符合摄影的基本要求。",
                "自然光运用得当，但阴影部分可以更柔和一些。光线方向选择合理，主体受光均匀，但阴影区域略显生硬。可以尝试使用反光板或补光设备来柔化阴影，使画面更加和谐。",
                "光线充足，色彩还原较好，建议注意高光控制。画面整体明亮，但高光区域有轻微溢出的迹象。建议在拍摄时适当降低曝光补偿，或使用渐变滤镜来平衡天空和地面的曝光。",
                "曝光基本准确，建议后期微调对比度。画面曝光正常，但对比度可以进一步提升，让画面更有层次感。后期处理中可以适当调整曲线或对比度滑块，使明暗对比更加鲜明。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (lightingScore >= 70) {
            String[] comments = {
                "光线表现一般，存在轻微过曝/欠曝。画面整体曝光有偏差，高光或暗部细节有所丢失。建议在拍摄时使用直方图检查曝光情况，或使用曝光补偿进行微调。",
                "阴影部分较重，细节有所丢失。画面暗部区域偏暗，部分细节难以辨认。可以尝试增加曝光补偿，或在后期处理中提亮暗部，恢复丢失的细节。",
                "高光区域溢出，建议使用渐变滤镜或包围曝光。天空或亮部区域出现了死白，细节完全丢失。建议使用渐变中性密度滤镜来平衡曝光，或采用包围曝光技术，通过后期合成获得完整的动态范围。",
                "光线对比度偏低，画面略显平淡。画面中明暗对比不够强烈，缺乏层次感和视觉冲击力。可以尝试寻找光线条件更好的拍摄时机，或在后期处理中增加对比度。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else {
            String[] comments = {
                "光线控制欠佳，存在明显过曝或欠曝。画面中高光或暗部严重溢出，大量细节丢失，影响了画面的整体质量。建议学习曝光三要素的基本知识，掌握正确的曝光方法。",
                "光源位置不当，主体曝光不足。主体处于阴影或背光位置，导致曝光不足，细节模糊不清。建议调整拍摄角度，让主体获得充足的光照，或使用闪光灯进行补光。",
                "高ISO噪点明显，建议使用三脚架降低ISO。画面中存在大量噪点，影响了画质的清晰度和细腻度。建议在光线不足的情况下使用三脚架固定相机，降低ISO值，以获得更干净的画面。",
                "光线过于强烈，缺乏层次感，建议等待合适光线。正午的强光使画面明暗对比过于强烈，阴影生硬，高光刺眼。建议等待黄金时刻拍摄，或寻找有遮挡的位置，利用柔和的散射光。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        }
        return Map.of("score", lightingScore, "comment", comment);
    }
    
    private Map<String, Object> generateColorAnalysis(int score) {
        int colorScore = Math.max(50, Math.min(100, score + (int) (Math.random() * 8) - 4));
        String comment;
        if (colorScore >= 90) {
            String[] comments = {
                "色彩搭配和谐，视觉效果出色，色调统一。画面中各种颜色相互协调，没有出现冲突或刺眼的色彩组合。色彩饱和度适中，既不过于艳丽也不过于暗淡，给人舒适的视觉体验。白平衡准确，色彩还原真实自然。",
                "色彩饱和度适中，白平衡准确，真实还原。画面中的色彩鲜艳而不刺眼，柔和而不暗淡，恰到好处地展现了拍摄对象的真实色彩。无论是人物肤色、自然景物还是人造物体，色彩还原都非常准确，没有偏色现象。",
                "色彩层次丰富，冷暖对比恰当，氛围营造到位。画面中冷暖色调相互呼应，形成了丰富的色彩层次和对比效果。暖色传达出温暖、热情的感觉，冷色则带来宁静、清新的氛围，整体色彩搭配非常有感染力。",
                "色调风格独特，色彩传达情感准确。作者通过色彩的运用成功地传达了想要表达的情感和氛围。无论是复古怀旧的暖色调，还是清新淡雅的冷色调，都与画面主题完美契合，展现了出色的色彩把控能力。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (colorScore >= 80) {
            String[] comments = {
                "色彩表现正常，搭配基本协调。画面中各种颜色没有明显的冲突，整体看起来比较和谐。虽然没有特别出彩的色彩效果，但基本符合摄影的要求。建议在后期处理中适当调整色彩，让画面更具个性。",
                "白平衡基本准确，色彩还原尚可。画面整体色调没有明显的偏色，色彩还原基本符合真实场景。可以在后期处理中微调白平衡，让色彩更加准确，特别是在人像摄影中，肤色的准确还原非常重要。",
                "色彩饱和度适中，可以稍微增强一点。画面色彩略显平淡，饱和度可以适当提高，让画面更加生动有活力。但要注意不要过度增加饱和度，以免色彩显得过于艳丽和不自然。",
                "色调统一，但可以增加一些色彩对比。画面整体色调比较统一，但缺乏足够的色彩对比，略显单调。可以尝试在画面中加入一些对比色元素，或在后期处理中增加色彩对比度，让画面更加丰富。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (colorScore >= 70) {
            String[] comments = {
                "色彩表现一般，略有偏色。画面存在轻微的色彩偏差，可能是白平衡设置不当或光线条件影响所致。建议在拍摄时注意白平衡的设置，或在后期处理中进行校正，使色彩更加准确。",
                "白平衡不准确，画面偏黄/偏蓝。画面整体色调偏向黄色或蓝色，色彩还原失真。这可能是由于拍摄时光线条件复杂，或者相机白平衡设置不正确。建议在后期处理中使用白平衡工具进行调整。",
                "色彩饱和度偏低，画面略显灰暗。画面色彩不够鲜艳，整体感觉比较灰暗，缺乏活力。可以在后期处理中适当提高饱和度和对比度，让画面更加明亮生动。",
                "色彩过于单调，建议增加色彩层次。画面中色彩种类较少，显得比较单调。可以尝试寻找色彩更加丰富的拍摄场景，或在构图时加入一些色彩鲜艳的元素，增加画面的视觉吸引力。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else {
            String[] comments = {
                "色彩表现较差，存在严重偏色。画面色彩严重失真，出现明显的偏色现象，影响了画面的整体美感和信息传达。建议学习白平衡的基本知识，在拍摄时正确设置白平衡，或在后期处理中进行全面的色彩校正。",
                "白平衡错误，色彩失真严重。画面中色彩完全偏离了真实场景，可能是由于相机设置错误或后期处理不当造成的。建议重新拍摄或进行彻底的后期色彩调整，恢复画面的真实色彩。",
                "色彩过于饱和/暗淡，缺乏美感。画面色彩要么过于艳丽刺眼，要么过于暗淡灰暗，都不符合视觉美学的要求。建议在后期处理中合理调整色彩饱和度，找到合适的平衡点。",
                "色彩搭配不协调，视觉冲突明显。画面中各种颜色相互冲突，给人混乱、不舒服的感觉。建议学习色彩搭配的基本原则，在拍摄和后期处理中注意色彩的和谐统一。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        }
        return Map.of("score", colorScore, "comment", comment);
    }
    
    private Map<String, Object> generateFocusAnalysis(int score) {
        int focusScore = Math.max(50, Math.min(100, score + (int) (Math.random() * 8) - 2));
        String comment;
        if (focusScore >= 90) {
            String[] comments = {
                "焦点精准！主体细节清晰锐利，景深控制完美。画面中主体的每一个细节都清晰可见，无论是人物的睫毛、皮肤纹理，还是物体的质感都得到了完美的展现。景深控制恰到好处，背景虚化自然柔和，有效突出了主体。",
                "眼睛/主体对焦清晰，睫毛/纹理细节可见。人像摄影中，眼睛是灵魂之窗，精准对焦在眼睛上能够让人物更加生动传神。画面中人物的眼睛清晰锐利，睫毛根根分明，展现了出色的对焦能力。",
                "景深恰到好处，背景虚化自然，主体突出。使用大光圈拍摄，背景被柔和地虚化，形成了美丽的散景效果，而主体则清晰锐利，形成了强烈的视觉对比。这种处理方式非常适合人像和特写摄影。",
                "整体清晰度高，从前景到背景过渡自然。使用较小的光圈获得了较大的景深，画面中从前景到背景都保持了良好的清晰度。这种处理方式适合风景摄影和需要展现环境的场景。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (focusScore >= 80) {
            String[] comments = {
                "焦点基本清晰，主体可辨识。画面中主体基本清晰，能够辨认出主体的轮廓和主要特征。虽然细节上可能略有欠缺，但整体效果尚可，能够满足基本的拍摄需求。",
                "景深效果尚可，但可以更明显一些。背景虚化效果有一定表现，但不够柔和自然。可以尝试使用更大的光圈或更长的焦距来获得更明显的背景虚化效果，突出主体。",
                "对焦基本准确，建议使用单次自动对焦。画面对焦基本准确，但可能存在轻微的对焦偏移。建议在拍摄时使用单次自动对焦模式，确保对焦精准后再按下快门。",
                "主体清晰，但边缘略有模糊。画面中心主体部分清晰，但边缘区域略显模糊。这可能是由于镜头边缘画质下降或轻微的相机抖动造成的。建议使用三脚架或提高快门速度。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else if (focusScore >= 70) {
            String[] comments = {
                "焦点不够清晰，存在轻微模糊。画面整体略显模糊，主体细节不够锐利。这可能是由于快门速度过慢、相机抖动或对焦不准确造成的。建议使用更快的快门速度或使用三脚架稳定相机。",
                "景深过浅/过深，建议调整光圈。景深效果不符合预期，要么背景虚化过度导致主体部分失焦，要么景深过大导致背景不够模糊。建议根据拍摄需求调整光圈大小。",
                "对焦偏移，主体不在焦点上。焦点落在了主体前方或后方，导致主体不够清晰。建议在拍摄时仔细观察对焦情况，或使用手动对焦模式确保精准对焦。",
                "部分区域模糊，建议使用更小光圈。画面中部分区域模糊不清，影响了整体效果。可以尝试使用更小的光圈来增加景深，让画面中更多的区域保持清晰。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        } else {
            String[] comments = {
                "焦点模糊严重，主体无法清晰辨认。画面中主体严重模糊，无法看清细节，严重影响了画面的质量和信息传达。建议检查相机设置，确保对焦模式正确，并使用足够快的快门速度。",
                "对焦失败，整个画面模糊。画面整体都处于模糊状态，可能是由于对焦系统故障、相机严重抖动或快门速度过慢造成的。建议使用三脚架稳定相机，并检查对焦设置。",
                "快门速度过慢，存在运动模糊。画面中存在明显的运动模糊，主体边缘出现拖影。这是由于快门速度低于安全快门速度造成的。建议提高快门速度，或使用三脚架避免相机抖动。",
                "光圈过大导致景深过浅，主体部分失焦。使用过大的光圈导致景深过浅，主体的部分区域不在焦点范围内，出现模糊。建议根据拍摄需求选择合适的光圈大小，必要时使用焦点合成技术。"
            };
            comment = comments[(int) (Math.random() * comments.length)];
        }
        return Map.of("score", focusScore, "comment", comment);
    }
}
