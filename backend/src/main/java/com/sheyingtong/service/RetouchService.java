package com.sheyingtong.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sheyingtong.dto.response.AiTaskSubmitResponse;
import com.sheyingtong.dto.response.RetouchResponse;
import com.sheyingtong.dto.response.RetouchResponse.*;
import com.sheyingtong.entity.AiBizType;
import com.sheyingtong.entity.AiTask;
import com.sheyingtong.entity.RetouchRecord;
import com.sheyingtong.entity.User;
import com.sheyingtong.repository.RetouchRecordRepository;
import com.sheyingtong.repository.UserRepository;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 修图建议服务：上传图片 → Qwen-VL-Plus → 结构化修图建议（8 大模块）
 * ----------------------------------------------------------------
 * 返回 RetouchResponse 包含：
 *  objective         - 一句话修图目标
 *  suggestions       - 详细文字建议
 *  imageParams       - 8 个滤镜实时参数（亮度/对比度/饱和度/色相旋转/模糊/复古/灰度/透明度）
 *                      前端 RetouchPage.vue 直接 setCSSFilter 应用到 Canvas，左右滑杆对比前后
 *  steps[]           - 至少 5 个具体修图步骤（title/description/tools[]/icon）
 *  recommendedTools[]- 4 个常用修图工具（LR/PS/醒图/Darktable 等）
 *  colorPalette[]    - 6 个推荐主色 hex
 *  whiteBalance      - 白平衡调整方向
 *  toneCurve         - 色调曲线建议
 *  techniques[]      - 4 个修图技巧（蒙版/渐变滤镜等）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetouchService {

    private final RetouchRecordRepository retouchRecordRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final AiTaskService aiTaskService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * 修图建议提交（原 getRetouchSuggestions 的入口）。
     * ----------------------------------------------------------------
     * ⚠️ 刻意**不**加 @Transactional：只有 aiTaskService.submit 内部建任务那一小段是事务，
     * 文件落盘与消息投递都在事务外。改造前这里用 @Transactional 包住了整段 AI 调用
     * （timeout 180s × 3 次重试最坏 9 分钟），与照片打分是同一类架构风险点。
     *
     * 真正的图片压缩、Qwen-VL 调用发生在 RetouchConsumer 里，期间不持有任何 DB 事务。
     *
     * @return 异步任务提交响应，前端拿 taskId 轮询 GET /api/ai/tasks/{taskId}
     */
    public AiTaskSubmitResponse submitSuggestions(MultipartFile file, String description, String username) {
        log.info("提交修图建议任务，文件名: {}, 用户: {}", file.getOriginalFilename(), username);

        String filePath = saveFile(file);
        log.info("修图照片已落盘: {}", filePath);

        return aiTaskService.submit(AiBizType.RETOUCH, filePath, file.getOriginalFilename(), description, username);
    }

    /**
     * 消费者调用：把 AI 返回的结构化修图建议持久化成 RetouchRecord 并返回落地实体。
     * 调用方（消费者线程）不是事务方法，这里单条 save 自成一个短事务、立即提交。
     * 完整响应（imageParams/colorPalette/techniques 等未落库的模块）由消费者序列化进
     * ai_task.result_json；历史详情接口走 getRetouchDetail 的默认补齐兜底逻辑。
     */
    public RetouchRecord persistResult(AiTask task, RetouchResponse response) {
        RetouchRecord record = RetouchRecord.builder()
                .user(task.getUser())
                .filePath(task.getFilePath())
                .fileName(task.getFileName())
                .description(task.getDescription())
                .suggestions(response.getObjective())
                .steps(response.getSteps() != null ? serializeSteps(response.getSteps()) : "[]")
                .build();

        record = retouchRecordRepository.save(record);
        record.setImageUrl("/api/retouch/image/" + record.getId());
        retouchRecordRepository.save(record);

        log.info("修图建议结果已入库: taskId={}, 记录ID={}", task.getId(), record.getId());
        return record;
    }

    /**
     * 消费者调用：调 Qwen-VL-Plus 并解析为修图结构化响应
     * ----------------------------------------------------------------
     * Step 1：图片已由 RetouchConsumer 用 ImageCompressor 压缩（消费线程内，无事务）
     * Step 2：Base64 编码
     * Step 3：构造超长 systemPrompt（含每个字段的 JSON 契约、
     *         枚举参数范围、steps/tools 最少 5 条等约束）
     * Step 4：aiService.analyzeImage() 单次调用（重试已上移到消息层，
     *         不再在 AIService 内 3 次串联，避免 3×3×180s 的最坏 27 分钟）
     * Step 5：正则把 ```json ... ``` 剥离
     * Step 6：objectMapper.readValue 转 Map → parseAIResponse 按模块提取字段，
     *         缺失的模块用默认生成器补齐（generateDefaultSteps() 等），
     *         保证前端 UI 模块永远不会空
     */
    public RetouchResponse runAiAnalysis(byte[] compressedImage, String description) throws Exception {
        String base64Image = Base64.getEncoder().encodeToString(compressedImage);

        String systemPrompt = "你是一位专业的修图师和摄影后期专家。请分析这张照片，提供详细的修图建议。";
        
        String userPrompt = String.format("请分析这张照片，提供专业的修图建议。\n\n拍摄场景描述：%s\n\n请按照以下JSON格式返回结果（不要包含markdown代码块）：\n{\n  \"objective\": \"修图目标，一句话概括需要达成的效果\",\n  \"suggestions\": \"详细的修图建议，包括需要调整的方面和原因\",\n  \"imageParams\": {\n    \"brightness\": 1.0,\n    \"contrast\": 1.0,\n    \"saturation\": 1.0,\n    \"hueRotate\": 0,\n    \"blur\": 0,\n    \"sepia\": 0,\n    \"grayscale\": 0,\n    \"opacity\": 1.0\n  },\n  \"steps\": [\n    {\n      \"title\": \"步骤标题\",\n      \"description\": \"详细的操作步骤和参数建议\",\n      \"tools\": [\"推荐使用的工具1\", \"工具2\"],\n      \"icon\": \"🎨\"\n    }\n  ],\n  \"recommendedTools\": [\n    {\n      \"id\": 1,\n      \"name\": \"工具名称\",\n      \"icon\": \"🖥️\",\n      \"description\": \"工具用途描述\"\n    }\n  ],\n  \"colorPalette\": [\n    {\"name\": \"颜色名称\", \"hex\": \"#ffffff\"}\n  ],\n  \"whiteBalance\": \"白平衡调整建议\",\n  \"toneCurve\": \"色调曲线调整建议\",\n  \"techniques\": [\n    {\n      \"title\": \"技巧名称\",\n      \"icon\": \"💡\",\n      \"description\": \"技巧描述\"\n    }\n  ]\n}\n\n注意：\n1. imageParams是图像处理参数，用于前端实时预览效果：\n   - brightness: 亮度(0-2.0，1.0为原图)\n   - contrast: 对比度(0-2.0，1.0为原图)\n   - saturation: 饱和度(0-2.0，1.0为原图)\n   - hueRotate: 色相旋转(0-360度，0为原图)\n   - blur: 模糊(0-20px，0为原图)\n   - sepia: 复古色调(0-1.0，0为原图)\n   - grayscale: 灰度(0-1.0，0为原图)\n   - opacity: 透明度(0-1.0，1.0为原图)\n2. 根据照片分析给出合适的参数值\n3. steps至少包含5个步骤，每个步骤要有具体的操作描述\n4. recommendedTools推荐4个常用修图工具\n5. colorPalette提供6个适合这张照片的配色方案\n6. whiteBalance和toneCurve要有具体的调整方向\n7. techniques提供4个相关的修图技巧", 
        description != null ? description : "无");

        String result = aiService.analyzeImage(base64Image, userPrompt, systemPrompt);
        
        result = result.replaceAll("```json", "").replaceAll("```", "").trim();
        
        Map<String, Object> aiResult = objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {});

        return parseAIResponse(aiResult);
    }

    private RetouchResponse parseAIResponse(Map<String, Object> aiResult) {
        RetouchResponse.RetouchResponseBuilder builder = RetouchResponse.builder();
        
        builder.objective((String) aiResult.getOrDefault("objective", "这张照片可以从多个方面进行优化"));
        builder.suggestions((String) aiResult.getOrDefault("suggestions", ""));
        builder.whiteBalance((String) aiResult.getOrDefault("whiteBalance", "根据照片情况调整白平衡"));
        builder.toneCurve((String) aiResult.getOrDefault("toneCurve", "建议使用S型曲线增加对比度"));

        Object paramsObj = aiResult.get("imageParams");
        if (paramsObj instanceof Map) {
            Map<String, Object> params = (Map<String, Object>) paramsObj;
            RetouchResponse.ImageParams imageParams = RetouchResponse.ImageParams.builder()
                    .brightness(getDoubleValue(params, "brightness", 1.0))
                    .contrast(getDoubleValue(params, "contrast", 1.0))
                    .saturation(getDoubleValue(params, "saturation", 1.0))
                    .hueRotate(getDoubleValue(params, "hueRotate", 0.0))
                    .blur(getDoubleValue(params, "blur", 0.0))
                    .sepia(getDoubleValue(params, "sepia", 0.0))
                    .grayscale(getDoubleValue(params, "grayscale", 0.0))
                    .opacity(getDoubleValue(params, "opacity", 1.0))
                    .build();
            builder.imageParams(imageParams);
        } else {
            builder.imageParams(generateDefaultImageParams());
        }

        Object stepsObj = aiResult.get("steps");
        if (stepsObj instanceof List) {
            List<RetouchStep> steps = objectMapper.convertValue(stepsObj, 
                new TypeReference<List<RetouchStep>>() {});
            builder.steps(steps);
        } else {
            builder.steps(generateDefaultSteps());
        }

        Object toolsObj = aiResult.get("recommendedTools");
        if (toolsObj instanceof List) {
            List<RetouchTool> tools = objectMapper.convertValue(toolsObj,
                new TypeReference<List<RetouchTool>>() {});
            builder.recommendedTools(tools);
        } else {
            builder.recommendedTools(generateDefaultTools());
        }

        Object colorsObj = aiResult.get("colorPalette");
        if (colorsObj instanceof List) {
            List<ColorItem> colors = objectMapper.convertValue(colorsObj,
                new TypeReference<List<ColorItem>>() {});
            builder.colorPalette(colors);
        } else {
            builder.colorPalette(generateDefaultColors());
        }

        Object techniquesObj = aiResult.get("techniques");
        if (techniquesObj instanceof List) {
            List<TechniqueItem> techniques = objectMapper.convertValue(techniquesObj,
                new TypeReference<List<TechniqueItem>>() {});
            builder.techniques(techniques);
        } else {
            builder.techniques(generateDefaultTechniques());
        }

        return builder.build();
    }

    public RetouchResponse generateMockResponse(String description) {
        List<RetouchStep> steps = generateDefaultSteps();
        if (description != null && !description.isEmpty()) {
            if (description.toLowerCase().contains("人像")) {
                steps = List.of(
                    RetouchStep.builder().title("基础调整").description("使用Camera Raw滤镜进行基础调整，曝光+0.5，对比度+10，清晰度+20").tools(List.of("Lightroom", "Camera Raw")).icon("🎨").build(),
                    RetouchStep.builder().title("肤色调整").description("创建曲线调整图层，提亮肤色，让皮肤更加通透自然").tools(List.of("Photoshop", "曲线")).icon("💡").build(),
                    RetouchStep.builder().title("磨皮处理").description("使用频率分离技术，分别处理皮肤细节和色调").tools(List.of("Photoshop", "高斯模糊")).icon("✨").build(),
                    RetouchStep.builder().title("五官精修").description("使用液化工具微调五官形状，让面部更精致").tools(List.of("Photoshop", "液化")).icon("👩").build(),
                    RetouchStep.builder().title("色调风格").description("添加渐变映射或颜色查找调整图层，打造独特色调风格").tools(List.of("Photoshop", "渐变映射")).icon("🌈").build()
                );
            } else if (description.toLowerCase().contains("风光") || description.toLowerCase().contains("风景")) {
                steps = List.of(
                    RetouchStep.builder().title("镜头校正").description("使用镜头校正工具修复畸变和暗角").tools(List.of("Lightroom", "ACR")).icon("📐").build(),
                    RetouchStep.builder().title("曝光调整").description("调整曝光、对比度、阴影和高光，恢复画面细节").tools(List.of("Lightroom", "曲线")).icon("☀️").build(),
                    RetouchStep.builder().title("色彩分级").description("分别调整天空、地面、前景的色相和饱和度").tools(List.of("Lightroom", "Camera Raw")).icon("🎨").build(),
                    RetouchStep.builder().title("清晰度增强").description("使用清晰度和纹理滑块增强画面细节").tools(List.of("Lightroom", "USM锐化")).icon("🔍").build(),
                    RetouchStep.builder().title("暗角添加").description("添加轻微暗角，增强视觉焦点").tools(List.of("Lightroom", "径向滤镜")).icon("🌑").build()
                );
            } else if (description.toLowerCase().contains("夜景")) {
                steps = List.of(
                    RetouchStep.builder().title("降噪处理").description("使用AI降噪工具减少噪点，保留画面细节").tools(List.of("Lightroom", "Topaz DeNoise")).icon("🔇").build(),
                    RetouchStep.builder().title("暗部提亮").description("使用阴影和黑色滑块提亮暗部细节").tools(List.of("Lightroom", "曲线")).icon("💡").build(),
                    RetouchStep.builder().title("高光压制").description("调整高光和白色，避免过曝区域").tools(List.of("Lightroom", "Camera Raw")).icon("🌙").build(),
                    RetouchStep.builder().title("色彩校正").description("调整白平衡，还原夜景真实色彩").tools(List.of("Lightroom", "色温")).icon("🎨").build(),
                    RetouchStep.builder().title("锐化处理").description("应用适度锐化，增强画面清晰度").tools(List.of("Lightroom", "USM锐化")).icon("✨").build()
                );
            }
        }

        return RetouchResponse.builder()
                .objective("这张照片可以从以下几个方面进行优化：基础调整、色彩校正、清晰度提升、局部调整、风格滤镜")
                .suggestions("根据照片分析，建议从基础参数调整开始，然后进行色彩校正和细节增强，最后添加合适的滤镜风格")
                .imageParams(generateDefaultImageParams())
                .steps(steps)
                .recommendedTools(generateDefaultTools())
                .colorPalette(generateDefaultColors())
                .whiteBalance("根据照片情况调整白平衡，还原真实色彩")
                .toneCurve("建议使用S型曲线，增加对比度和层次感")
                .techniques(generateDefaultTechniques())
                .build();
    }

    private List<RetouchStep> generateDefaultSteps() {
        return List.of(
            RetouchStep.builder().title("基础调整").description("打开照片，调整曝光、对比度、亮度、饱和度等基础参数").tools(List.of("Lightroom", "Camera Raw")).icon("🎨").build(),
            RetouchStep.builder().title("白平衡校正").description("调整白平衡，还原照片真实色彩").tools(List.of("Lightroom", "色温工具")).icon("⚖️").build(),
            RetouchStep.builder().title("影调优化").description("使用曲线调整图层优化画面影调").tools(List.of("Photoshop", "曲线")).icon("📈").build(),
            RetouchStep.builder().title("细节增强").description("应用适当的锐化处理，增强画面清晰度").tools(List.of("Lightroom", "USM锐化")).icon("✨").build(),
            RetouchStep.builder().title("风格添加").description("根据照片类型添加合适的滤镜风格").tools(List.of("Lightroom", "预设")).icon("🌈").build()
        );
    }

    private List<RetouchTool> generateDefaultTools() {
        return List.of(
            RetouchTool.builder().id(1L).name("Adobe Lightroom").icon("🖥️").description("专业RAW格式处理，适合批量修图").build(),
            RetouchTool.builder().id(2L).name("Adobe Photoshop").icon("🎨").description("强大的像素级编辑，适合精细修图").build(),
            RetouchTool.builder().id(3L).name("醒图").icon("📱").description("手机端优秀修图App，滤镜丰富").build(),
            RetouchTool.builder().id(4L).name("Snapseed").icon("🌸").description("Google出品，功能强大且免费").build()
        );
    }

    private List<ColorItem> generateDefaultColors() {
        return List.of(
            ColorItem.builder().name("肤色").hex("#f5d6c8").build(),
            ColorItem.builder().name("暖白").hex("#fff8f0").build(),
            ColorItem.builder().name("暖黄").hex("#ffd966").build(),
            ColorItem.builder().name("浅棕").hex("#c9a962").build(),
            ColorItem.builder().name("深红").hex("#c75b7a").build(),
            ColorItem.builder().name("暗蓝").hex("#2d4a53").build()
        );
    }

    private List<TechniqueItem> generateDefaultTechniques() {
        return List.of(
            TechniqueItem.builder().title("频率分离").icon("🔄").description("将图片分解为高频（细节）和低频（色调）两个图层，分别进行处理，既能磨皮又能保留皮肤纹理").build(),
            TechniqueItem.builder().title("D&B（加深减淡）").icon("🎨").description("通过加深和减淡工具，手动调整面部光影，塑造立体感").build(),
            TechniqueItem.builder().title("色彩分级").icon("🌈").description("分别调整阴影、中间调、高光的色相和饱和度，创造独特的色调风格").build(),
            TechniqueItem.builder().title("蒙版技巧").icon("🔍").description("使用蒙版精确控制调整范围，让修图更加自然").build()
        );
    }

    private RetouchResponse.ImageParams generateDefaultImageParams() {
        return RetouchResponse.ImageParams.builder()
                .brightness(1.15)
                .contrast(1.2)
                .saturation(1.15)
                .hueRotate(0.0)
                .blur(0.0)
                .sepia(0.1)
                .grayscale(0.0)
                .opacity(1.0)
                .build();
    }

    private Double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    public RetouchRecord getRecordById(Long id) {
        return retouchRecordRepository.findById(id).orElse(null);
    }

    public RetouchResponse getRetouchDetail(RetouchRecord record) {
        RetouchResponse.RetouchResponseBuilder builder = RetouchResponse.builder();
        
        builder.objective(record.getSuggestions() != null ? record.getSuggestions() : "查看历史修图记录");
        
        List<RetouchStep> steps = generateDefaultSteps();
        if (record.getSteps() != null && !record.getSteps().isEmpty()) {
            try {
                steps = objectMapper.readValue(record.getSteps(), new TypeReference<List<RetouchStep>>() {});
            } catch (Exception e) {
                log.warn("解析steps失败: {}", e.getMessage());
            }
        }
        
        builder.steps(steps);
        builder.recommendedTools(generateDefaultTools());
        builder.colorPalette(generateDefaultColors());
        builder.whiteBalance("根据照片情况调整白平衡，还原真实色彩");
        builder.toneCurve("建议使用S型曲线，增加对比度和层次感");
        builder.techniques(generateDefaultTechniques());
        builder.imageParams(generateDefaultImageParams());
        
        return builder.build();
    }

    public Page<RetouchRecord> getHistory(String username, Pageable pageable) {
        if (username == null || username.isEmpty()) {
            return retouchRecordRepository.findAll(pageable);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return retouchRecordRepository.findByUser(user, pageable);
    }

    @Transactional
    public void deleteRecord(Long id) {
        RetouchRecord record = retouchRecordRepository.findById(id).orElse(null);
        if (record != null) {
            try {
                Files.deleteIfExists(Paths.get(record.getFilePath()));
            } catch (IOException e) {
                log.warn("删除照片文件失败: {}", e.getMessage());
            }
            retouchRecordRepository.deleteById(id);
            log.info("删除修图记录: {}", id);
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

    private String serializeSteps(List<RetouchStep> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            log.warn("序列化steps失败: {}", e.getMessage());
            return "[]";
        }
    }
}