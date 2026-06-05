package com.newworld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 任务统计VO
 */
@Schema(description = "任务统计")
public class TaskStatisticsVO {

    @Schema(description = "待办数量")
    private Long todoCount;

    @Schema(description = "进行中数量")
    private Long inProgressCount;

    @Schema(description = "已完成数量")
    private Long doneCount;

    @Schema(description = "已归档数量")
    private Long archivedCount;

    @Schema(description = "总数量")
    private Long totalCount;

    public Long getTodoCount() {
        return todoCount;
    }

    public void setTodoCount(Long todoCount) {
        this.todoCount = todoCount;
    }

    public Long getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(Long inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public Long getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(Long doneCount) {
        this.doneCount = doneCount;
    }

    public Long getArchivedCount() {
        return archivedCount;
    }

    public void setArchivedCount(Long archivedCount) {
        this.archivedCount = archivedCount;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }
}
