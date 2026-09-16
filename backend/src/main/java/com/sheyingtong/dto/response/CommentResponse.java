package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论响应。parentId 非空时 replyToName 指向被回复人的昵称（前端显示 @xxx）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long workId;
    private Long parentId;
    private String content;
    private LocalDateTime createdAt;
    private AuthorInfo author;
    private String replyToName;
}
