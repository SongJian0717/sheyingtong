package com.sheyingtong.service;

import com.sheyingtong.entity.Gear;
import com.sheyingtong.entity.PhotographyTerm;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhotographyTools {

    // 走 DictionaryService / GearService 而不是直接查库：用户每发一条消息，工具链可能查多次
    // 词库和器材，复用同一份整表缓存可以省掉这些 SQL
    private final DictionaryService dictionaryService;
    private final GearService gearService;

    @Tool("查询摄影词典中的术语定义")
    public String queryDictionary(String term) {
        log.info("调用工具: queryDictionary, 参数: {}", term);
        Optional<PhotographyTerm> foundTerm = dictionaryService.findTerm(term == null ? null : term.trim());

        if (foundTerm.isEmpty()) {
            return "未找到术语: " + term;
        }
        
        PhotographyTerm photographyTerm = foundTerm.get();
        return String.format("【%s】\n分类: %s\n定义: %s\n详细解释: %s\n示例: %s",
                photographyTerm.getTerm(),
                photographyTerm.getCategory(),
                photographyTerm.getDefinition(),
                photographyTerm.getExplanation(),
                photographyTerm.getExamples());
    }

    @Tool("搜索摄影器材")
    public String searchGear(String query) {
        log.info("调用工具: searchGear, 参数: {}", query);
        List<Gear> gears = gearService.searchByKeyword(query);
        
        if (gears.isEmpty()) {
            return "未找到符合条件的器材";
        }
        
        StringBuilder result = new StringBuilder("找到以下器材：\n");
        for (int i = 0; i < Math.min(5, gears.size()); i++) {
            Gear gear = gears.get(i);
            result.append(String.format("%d. %s %s - 价格: %s元 - 评分: %.1f\n",
                    i + 1,
                    gear.getBrand(),
                    gear.getModel(),
                    gear.getPrice(),
                    gear.getRating()));
            if (gear.getDescription() != null && !gear.getDescription().isEmpty()) {
                result.append("   简介: ").append(gear.getDescription().substring(0, 
                        Math.min(100, gear.getDescription().length()))).append("...\n");
            }
        }
        
        return result.toString();
    }

    @Tool("根据预算搜索摄影器材")
    public String searchGearByBudget(Integer minBudget, Integer maxBudget) {
        log.info("调用工具: searchGearByBudget, 参数: minBudget={}, maxBudget={}", minBudget, maxBudget);
        
        BigDecimal min = minBudget != null ? BigDecimal.valueOf(minBudget) : BigDecimal.ZERO;
        BigDecimal max = maxBudget != null ? BigDecimal.valueOf(maxBudget) : new BigDecimal("1000000");
        
        List<Gear> gears = gearService.findByPriceRange(min, max);
        
        if (gears.isEmpty()) {
            return "未找到符合预算的器材";
        }
        
        StringBuilder result = new StringBuilder(String.format("预算 %d-%d 元范围内的器材：\n", minBudget, maxBudget));
        for (int i = 0; i < Math.min(5, gears.size()); i++) {
            Gear gear = gears.get(i);
            result.append(String.format("%d. %s %s - 价格: %s元 - 评分: %.1f\n",
                    i + 1,
                    gear.getBrand(),
                    gear.getModel(),
                    gear.getPrice(),
                    gear.getRating()));
        }
        
        return result.toString();
    }

    @Tool("计算曝光参数（根据光圈和ISO计算快门速度）")
    public String calculateExposure(String aperture, Integer iso) {
        log.info("调用工具: calculateExposure, 参数: aperture={}, iso={}", aperture, iso);
        
        try {
            double apertureValue = Double.parseDouble(aperture.replace("f/", "").replace("F/", ""));
            double isoValue = iso != null ? iso : 100;
            
            double baseShutter = 1.0 / 100.0;
            double apertureFactor = (apertureValue * apertureValue) / (16.0 * 16.0);
            double isoFactor = isoValue / 100.0;
            
            double shutterSpeed = baseShutter * apertureFactor * isoFactor;
            
            String shutterString;
            if (shutterSpeed >= 1.0) {
                shutterString = String.format("%ds", shutterSpeed);
            } else {
                int denominator = (int) Math.round(1.0 / shutterSpeed);
                shutterString = String.format("1/%ds", denominator);
            }
            
            Map<String, Object> exposureResult = new HashMap<>();
            exposureResult.put("aperture", aperture);
            exposureResult.put("iso", iso);
            exposureResult.put("shutterSpeed", shutterString);
            exposureResult.put("calculatedValue", shutterSpeed);
            
            return String.format("根据曝光三角计算：\n光圈: %s\nISO: %d\n推荐快门速度: %s\n\n提示：这是基础计算值，实际拍摄时可能需要根据光线条件调整曝光补偿。",
                    aperture, iso, shutterString);
                    
        } catch (NumberFormatException e) {
            return "无效的光圈值格式，请使用如 'f/2.8' 或 '2.8' 的格式";
        }
    }

    @Tool("获取常用摄影技巧")
    public String getPhotographyTips(String category) {
        log.info("调用工具: getPhotographyTips, 参数: {}", category);
        
        Map<String, List<String>> tips = new HashMap<>();
        
        tips.put("构图", List.of(
            "三分法：将画面分成三等分，主体放在线条交点上",
            "引导线：利用道路、河流等线条引导视线",
            "框架构图：利用门窗等形成天然画框",
            "对称构图：适合建筑和风景摄影",
            "前景：加入前景元素增加画面层次感"
        ));
        
        tips.put("光线", List.of(
            "黄金时刻：日出后和日落前1小时的柔和光线",
            "逆光拍摄：让光线从主体背后照射，创造剪影效果",
            "补光技巧：使用反光板或闪光灯补光",
            "避免正午强光：正午阳光直射容易产生硬阴影",
            "利用阴影：阴影可以增加画面的立体感"
        ));
        
        tips.put("人像", List.of(
            "眼神交流：让模特直视镜头增加亲切感",
            "浅景深：使用大光圈(f/1.4-f/2.8)虚化背景",
            "引导姿势：指导模特放松身体，避免僵硬",
            "环境人像：将人物融入环境中讲述故事",
            "特写镜头：捕捉人物表情和细节"
        ));
        
        tips.put("风光", List.of(
            "使用三脚架：保持画面稳定，适合长曝光",
            "小光圈：使用f/11-f/16获得大景深",
            "前景兴趣点：寻找有趣的前景增加画面层次",
            "长曝光：使用ND滤镜拍摄水流和云彩的动态",
            "全景拼接：拍摄多张照片合成宽幅全景"
        ));
        
        tips.put("夜景", List.of(
            "使用三脚架：避免手持抖动",
            "高ISO降噪：开启相机的高ISO降噪功能",
            "手动对焦：夜景自动对焦可能失效",
            "长时间曝光：根据场景调整快门速度",
            "避免直射光源：防止镜头眩光"
        ));
        
        List<String> categoryTips = tips.getOrDefault(category, tips.get("构图"));
        if (!tips.containsKey(category)) {
            return "未找到该分类的技巧，以下是通用构图技巧：\n" + String.join("\n", categoryTips);
        }
        
        StringBuilder result = new StringBuilder(String.format("%s摄影技巧：\n", category));
        for (int i = 0; i < categoryTips.size(); i++) {
            result.append(String.format("%d. %s\n", i + 1, categoryTips.get(i)));
        }
        
        return result.toString();
    }

    @Tool("解释曝光三角")
    public String explainExposureTriangle() {
        log.info("调用工具: explainExposureTriangle");
        
        return """
            曝光三角是摄影中控制画面亮度的三个核心要素：
            
            1. 光圈(Aperture)
            - 控制进光量和景深
            - 光圈值越小（如f/1.4），进光越多，景深越浅
            - 光圈值越大（如f/16），进光越少，景深越深
            
            2. 快门速度(Shutter Speed)
            - 控制曝光时间和动态效果
            - 快门越快（如1/1000s），适合抓拍运动物体
            - 快门越慢（如1/30s），适合拍摄静态场景，可能需要三脚架
            
            3. ISO感光度
            - 控制相机传感器对光线的敏感度
            - ISO越低（如100），画质越细腻，噪点越少
            - ISO越高（如3200），适合低光环境，但噪点会增加
            
            三者关系：增大光圈（减小f值）、放慢快门、提高ISO都能增加曝光。
            调整其中一个时，通常需要调整其他两个来保持正确曝光。
            
            记忆口诀："大光圈虚化背景，快快门凝固瞬间，高ISO照亮黑暗"
            """;
    }
}