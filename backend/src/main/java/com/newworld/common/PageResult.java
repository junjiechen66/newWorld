package com.newworld.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页响应体
 */
@Data
@Schema(description = "分页返回结果")
public class PageResult<T> {

    @Schema(description = "记录列表")
    private List<T> records;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "当前页码")
    private Long page;

    @Schema(description = "每页条数")
    private Long pageSize;

    public static <T> PageResult<T> of(List<T> records, Long total, Long page, Long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.records = records;
        result.total = total;
        result.page = page;
        result.pageSize = pageSize;
        return result;
    }
}
