package com.sheyingtong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发表评论请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论最长500个字符")
    private String content;

    /** 回复哪条评论（一层楼中楼），为空表示顶楼评论 */
    private Long parentId;
}
