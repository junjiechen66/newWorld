package com.newworld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 任务查询参数
 */
@Schema(description = "任务查询参数")
public class TaskQueryDTO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "状态: TODO/IN_PROGRESS/DONE/ARCHIVED")
    private String status;

    @Schema(description = "优先级: RED/YELLOW/BLUE/FLAG/NONE")
    private String priority;

    @Schema(description = "开始日期-起始")
    private LocalDate startDateFrom;

    @Schema(description = "开始日期-截止")
    private LocalDate startDateTo;

    @Schema(description = "截止日期-起始")
    private LocalDate dueDateFrom;

    @Schema(description = "截止日期-截止")
    private LocalDate dueDateTo;

    @Schema(description = "搜索关键字")
    private String keyword;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "是否笔记")
    private Boolean isNote;

    @Schema(description = "当前页码")
    private Long page = 1L;

    @Schema(description = "每页条数")
    private Long pageSize = 100L;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getStartDateFrom() {
        return startDateFrom;
    }

    public void setStartDateFrom(LocalDate startDateFrom) {
        this.startDateFrom = startDateFrom;
    }

    public LocalDate getStartDateTo() {
        return startDateTo;
    }

    public void setStartDateTo(LocalDate startDateTo) {
        this.startDateTo = startDateTo;
    }

    public LocalDate getDueDateFrom() {
        return dueDateFrom;
    }

    public void setDueDateFrom(LocalDate dueDateFrom) {
        this.dueDateFrom = dueDateFrom;
    }

    public LocalDate getDueDateTo() {
        return dueDateTo;
    }

    public void setDueDateTo(LocalDate dueDateTo) {
        this.dueDateTo = dueDateTo;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Boolean getIsNote() {
        return isNote;
    }

    public void setIsNote(Boolean isNote) {
        this.isNote = isNote;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }
}
