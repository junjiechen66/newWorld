package com.newworld.service;

import com.newworld.dto.SortItem;
import com.newworld.entity.Project;

import java.util.List;

public interface ProjectService {

    /**
     * 按分组获取项目列表
     */
    List<Project> getByGroupId(Long userId, Long groupId);

    /**
     * 新建项目
     */
    Project create(Project project);

    /**
     * 更新项目
     */
    Project update(Project project);

    /**
     * 删除项目
     */
    void delete(Long id);

    /**
     * 批量更新排序
     */
    void updateSort(List<SortItem> items);
}
