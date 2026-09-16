package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注/取关操作结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponse {
    private Boolean following;
    private Long followersCount;
}
