package com.newworld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 任务统计VO
 */
@Schema(description = "任务统计")
public class TaskStatisticsVO {

    @Schema(description = "未完成数量")
    private Long incompleteCount;

    @Schema(description = "已完成数量")
    private Long doneCount;

    @Schema(description = "搁置数量")
    private Long shelvedCount;

    @Schema(description = "总数量")
    private Long totalCount;

    public Long getIncompleteCount() {
        return incompleteCount;
    }

    public void setIncompleteCount(Long incompleteCount) {
        this.incompleteCount = incompleteCount;
    }

    public Long getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(Long doneCount) {
        this.doneCount = doneCount;
    }

    public Long getShelvedCount() {
        return shelvedCount;
    }

    public void setShelvedCount(Long shelvedCount) {
        this.shelvedCount = shelvedCount;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }
}
