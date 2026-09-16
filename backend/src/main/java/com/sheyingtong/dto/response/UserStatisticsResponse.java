package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponse {

    private Integer totalPhotos;

    private Integer totalChatSessions;

    private Integer totalRetouchRecords;

    private Integer totalScoreRecords;

    private String joinDate;

    /** 摄影社区：发布的作品数 */
    private Long worksCount;

    /** 摄影社区：粉丝数 */
    private Long followersCount;

    /** 摄影社区：关注数 */
    private Long followingCount;
}