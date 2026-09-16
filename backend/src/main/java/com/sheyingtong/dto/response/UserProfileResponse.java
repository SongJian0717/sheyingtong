package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户公开主页信息（社区作者卡/个人主页）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Long worksCount;
    private Long followersCount;
    private Long followingCount;
    private String joinedDate;
    /** 当前登录浏览者是否已关注该用户（未登录为 false） */
    private Boolean followedByMe;
}
