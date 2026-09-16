package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作品响应（广场/我的作品/用户主页通用）
 * 前端图片地址由 photoAnalysisId 拼出：/api/photo/image/{photoAnalysisId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkResponse {
    private Long id;
    private Long photoAnalysisId;
    private String title;
    private String content;
    private Integer likesCount;
    private Integer commentsCount;
    private LocalDateTime createdAt;
    private AuthorInfo author;
}
