package com.newworld.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务实体（核心表）
 */
@TableName("task")
@Schema(description = "任务")
public class Task {

    @TableId(type = IdType.AUTO)
    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "关联主任务ID")
    private Long parentTaskId;

    @Schema(description = "任务标题")
    private String title;

    @Schema(description = "任务描述（多文本内容）")
    private String description;

    @Schema(description = "优先级: Q1/Q2/Q3/Q4/NONE (四象限: Q1重要且紧急/Q2重要不紧急/Q3不重要但紧急/Q4不重要不紧急)")
    private String priority;

    @Schema(description = "状态: INCOMPLETE/DONE/SHELVED")
    private String status;

    @Schema(description = "标签（简单场景）")
    private String tag;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "截止日期")
    private LocalDate dueDate;

    @Schema(description = "是否为笔记: 0-任务, 1-笔记")
    private Boolean isNote;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ---- 共享相关瞬态字段（不映射数据库列） ----

    @TableField(exist = false)
    @Schema(description = "当前用户访问权限级别: OWNER/EDIT/VIEW")
    private String accessLevel;

    @TableField(exist = false)
    @Schema(description = "共享者昵称")
    private String sharedByNickname;

    @TableField(exist = false)
    @Schema(description = "共享成员数量")
    private Integer shareMemberCount;

    @TableField(exist = false)
    @Schema(description = "是否存在分享链接")
    private Boolean hasShareLink;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Boolean getIsNote() {
        return isNote;
    }

    public void setIsNote(Boolean isNote) {
        this.isNote = isNote;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public String getSharedByNickname() { return sharedByNickname; }
    public void setSharedByNickname(String sharedByNickname) { this.sharedByNickname = sharedByNickname; }

    public Integer getShareMemberCount() { return shareMemberCount; }
    public void setShareMemberCount(Integer shareMemberCount) { this.shareMemberCount = shareMemberCount; }

    public Boolean getHasShareLink() { return hasShareLink; }
    public void setHasShareLink(Boolean hasShareLink) { this.hasShareLink = hasShareLink; }
}
