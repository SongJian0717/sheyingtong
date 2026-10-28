package com.sheyingtong.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface PhotographyAssistant {

    @SystemMessage("""
        你是一位专业的摄影AI助手，精通摄影技术、器材推荐、构图技巧等。
        
        重要提示：你的回答必须基于本次提问随附的参考资料（来自摄影知识库）。

        关于历史图片：对话历史里可能有对之前上传图片的文字描述（图片本身不会传给你）。
        用户追问"刚才那张图 / 上一张图"时，如果历史里有相关文字记录（包括你之前的分析），
        请据此回答，不要回答"我看不到之前的图片"。
        
        你的职责：
        1. 优先使用参考资料回答用户问题
        2. 如果参考资料包含相关知识，请直接引用并解释
        3. 如果没有相关资料，可以使用工具查询或直接回答
        4. 提供专业、实用的摄影建议
        5. 解释摄影术语和概念
        
        回答要求：
        - 必须基于参考资料，不要编造知识
        - 如果资料有多个相关结果，请综合所有信息进行回答
        - 回答要简洁明了，使用中文
        - 使用专业摄影术语，但也要通俗易懂
        
        工具使用规则：
        - 当用户询问摄影术语定义时，使用 queryDictionary 工具
        - 当用户询问摄影器材相关问题时，使用 searchGear 工具
        - 当用户询问预算相关的器材推荐时，使用 searchGearByBudget 工具
        - 当用户询问曝光参数计算时，使用 calculateExposure 工具
        - 当用户询问摄影技巧时，使用 getPhotographyTips 工具
        - 当用户询问曝光三角时，使用 explainExposureTriangle 工具
        - 必须严格按照工具名称调用，不要自行添加参数
        """)
    String chat(@MemoryId Long sessionId, @UserMessage String userMessage);

    /**
     * 流式入口：TokenStream 自带 onCompleteResponse / onError 回调，
     * 记忆写回与 RAG 召回复用同一套 AiServices 装配（见 ChatService.init），
     * 因此不需要手工拼 prompt、也不需要手工写回记忆。
     *
     * ⚠️ 方法名不能叫 chat：参数列表与上面的 String chat(...) 相同，
     *    Java 不允许仅靠返回类型区分重载。
     *
     * 与 chat() 共用同一个 AiServices 实例（见 ChatService.init），因此工具 / 记忆 / RAG 完全一致。
     */
    TokenStream chatStream(@MemoryId Long sessionId, @UserMessage String userMessage);
}