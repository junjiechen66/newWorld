package com.newworld.service;

import com.newworld.dto.SortItem;
import com.newworld.entity.Group;
import com.newworld.dto.TreeVO;

import java.time.LocalDate;
import java.util.List;

public interface GroupService {

    /**
     * 获取分组列表
     */
    List<Group> getList(Long userId);

    /**
     * 获取完整树形结构（分组 -> 项目 -> 任务）
     */
    List<TreeVO> getTree(Long userId, Long projectId);

    /**
     * 获取日期过滤的树形结构（隐藏当月无任务的项目）
     */
    List<TreeVO> getTree(Long userId, Long projectId, LocalDate startDate, LocalDate endDate);

    /**
     * 新建分组
     */
    Group create(Group group);

    /**
     * 更新分组
     */
    Group update(Group group);

    /**
     * 删除分组
     */
    void delete(Long id);

    /**
     * 批量更新排序
     */
    void updateSort(List<SortItem> items);
}
