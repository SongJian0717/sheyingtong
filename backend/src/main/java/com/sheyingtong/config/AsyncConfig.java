package com.sheyingtong.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 应用级 @Async 线程池（有界）
 * ----------------------------------------------------------------
 * @EnableAsync 开在启动类上，但默认 executor 是 SimpleAsyncTaskExecutor ——
 * 每次异步执行都新建一个线程、不设上限，任务堆积时会无节制地消耗线程
 * （本项目 ChatService 的流式推送就是裸 new Thread，见设计方案文档）。
 *
 * 这里给 @Async 挂一个**有界**池：核心 4 / 最大 8 / 等待队列 500，
 * 拒绝策略用 CallerRuns（队列满了由调用线程就地执行，不静默丢任务）。
 *
 * ⚠️ RabbitMQ 消费者不走这个池（它有专用容器工厂 + prefetch 限制），别混为一谈；
 * 本配置只影响 @Async 注解的调用。ChatService 的两处裸线程属于既有的历史实现，
 * 不在本次改动范围内（要改需另起一期，见设计方案"明确不做"）。
 */
@Configuration
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public TaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("app-async-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("[async] 有界异步线程池就绪: core=4, max=8, queue=500, 拒绝策略=CallerRuns");
        return executor;
    }
}