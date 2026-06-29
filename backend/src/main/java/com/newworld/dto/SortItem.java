package com.newworld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "排序项")
public class SortItem {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "排序号")
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
