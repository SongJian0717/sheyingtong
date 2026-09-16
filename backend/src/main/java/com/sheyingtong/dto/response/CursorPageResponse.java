package com.sheyingtong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用游标分页响应：items 一页数据，nextCursor 传给下一页（无更多时为 null）
 * ----------------------------------------------------------------
 * 分页方式：主键 id 游标（id < cursor 倒序取一页），规避深分页 OFFSET
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponse<T> {
    private List<T> items;
    private Long nextCursor;
    private Boolean hasMore;
}
