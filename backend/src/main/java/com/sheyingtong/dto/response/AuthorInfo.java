package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作者简要信息（作品/评论/用户卡通用，不含邮箱等隐私字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorInfo {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
}
