package com.newworld.service;

import com.newworld.entity.Group;
import com.newworld.dto.TreeVO;

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
}
