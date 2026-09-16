package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异步任务提交响应（POST /api/photo/score、POST /api/retouch/suggestions 的返回体）
 * ----------------------------------------------------------------
 * 改造前这两个接口会阻塞几十秒到几分钟才返回结果；现在立即返回本对象，
 * 前端拿 taskId 去轮询 GET /api/ai/tasks/{taskId} 取最终结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskSubmitResponse {

    /** 任务 id，轮询用 */
    private Long taskId;

    /** 业务类型：PHOTO_SCORE / RETOUCH */
    private String bizType;

    /**
     * 提交那一刻的状态。
     * 正常异步路径下是 PENDING；若 broker 不可达且开了 fallback-to-sync，
     * 请求线程会就地跑完任务，此时这里已经是 SUCCESS / DEGRADED / FAILED
     * （前端先立即查一次即可拿到结果，不用干等轮询）。
     */
    private String status;

    /** 是否走了"就地同步执行"降级路径（前端可据此提示"当前未走异步队列"） */
    private boolean degradedToSync;
}
