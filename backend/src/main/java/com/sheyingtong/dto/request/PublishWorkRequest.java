package com.sheyingtong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布作品请求：从一条 AI 打分记录(photoAnalysisId)发布为公开作品
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishWorkRequest {

    @NotNull(message = "打分记录ID不能为空")
    private Long photoAnalysisId;

    @NotBlank(message = "作品标题不能为空")
    @Size(max = 100, message = "作品标题最长100个字符")
    private String title;

    @Size(max = 5000, message = "创作说明最长5000个字符")
    private String content;
}
