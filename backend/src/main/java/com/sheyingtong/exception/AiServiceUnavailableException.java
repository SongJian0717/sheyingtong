package com.sheyingtong.exception;

/**
 * AI 任务队列不可用（broker 挂了且未开启同步降级）
 * ----------------------------------------------------------------
 * 单独一个类型是为了拿到 503 而不是被 GlobalExceptionHandler 里的
 * RuntimeException 关键词规则判成 400/500 —— 客户端没做错任何事，
 * 是服务端依赖暂时不可用，语义上就该是 503。
 */
public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
