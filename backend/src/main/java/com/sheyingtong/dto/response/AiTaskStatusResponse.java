package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异步任务状态查询响应（GET /api/ai/tasks/{taskId}）
 * ----------------------------------------------------------------
 * 前端轮询这个接口，直到 terminal=true。
 *
 * <pre>
 *   PENDING  → 排队中
 *   RUNNING  → 分析中
 *   SUCCESS  → 完成（result 为真实 AI 结果）
 *   DEGRADED → 完成但为降级结果（result 为兜底数据，可重试）
 *   FAILED   → 失败（无结果，可重试）
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskStatusResponse {

    private Long taskId;

    private String bizType;

    private String status;

    /** 是否已到终态（前端据此决定停止轮询） */
    private boolean terminal;

    /** 是否可重试（FAILED / DEGRADED 为 true，前端据此显示重试按钮） */
    private boolean retryable;

    /** 结果业务行 id（photo_analysis.id / retouch_records.id），终态后才有 */
    private Long bizId;

    /** 失败或降级原因，正常成功时为 null */
    private String errorMessage;

    /**
     * 最终结果负载，结构与改造前同步接口的返回体**完全一致**（前端渲染代码不用改）：
     *  - PHOTO_SCORE → { score, analysis:{6 个维度}, suggestions }
     *  - RETOUCH     → 完整 RetouchResponse（objective/steps/imageParams/colorPalette…）
     * 未到终态时为 null。
     */
    private Object result;
}
