package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作品详情（比列表多一个 liked：当前登录用户是否已点赞）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkDetailResponse {
    private Long id;
    private Long photoAnalysisId;
    private String title;
    private String content;
    private Integer likesCount;
    private Integer commentsCount;
    private Boolean liked;
    private LocalDateTime createdAt;
    private AuthorInfo author;
}
