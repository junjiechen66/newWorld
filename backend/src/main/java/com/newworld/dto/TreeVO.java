package com.newworld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 树形结构VO - 用于前端左侧导航树
 */
@Schema(description = "树形节点")
public class TreeVO {

    @Schema(description = "节点ID")
    private Long id;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点类型: group/project/task")
    private String type;

    @Schema(description = "项目颜色（仅项目类型）")
    private String color;

    @Schema(description = "任务优先级")
    private String priority;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "子节点列表")
    private List<TreeVO> children;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<TreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<TreeVO> children) {
        this.children = children;
    }
}
