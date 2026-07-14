package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetouchResponse {

    private String objective;

    private String suggestions;

    private ImageParams imageParams;

    private List<RetouchStep> steps;

    private List<RetouchTool> recommendedTools;

    private List<ColorItem> colorPalette;

    private String whiteBalance;

    private String toneCurve;

    private List<TechniqueItem> techniques;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetouchStep {
        private String title;
        private String description;
        private List<String> tools;
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetouchTool {
        private Long id;
        private String name;
        private String icon;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorItem {
        private String name;
        private String hex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechniqueItem {
        private String title;
        private String icon;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageParams {
        private Double brightness;
        private Double contrast;
        private Double saturation;
        private Double hueRotate;
        private Double blur;
        private Double sepia;
        private Double grayscale;
        private Double opacity;
    }
}