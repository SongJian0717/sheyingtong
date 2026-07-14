package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoScoreResponse {
    
    private Integer score;
    
    private Map<String, Object> analysis;
    
    private String suggestions;
}
