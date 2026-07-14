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
}