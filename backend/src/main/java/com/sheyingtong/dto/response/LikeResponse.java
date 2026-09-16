package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞/取消点赞操作结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {
    private Boolean liked;
    private Integer likesCount;
}
