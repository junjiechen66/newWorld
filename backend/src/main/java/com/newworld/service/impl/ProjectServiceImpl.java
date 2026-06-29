package com.newworld.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.newworld.dto.SortItem;
import com.newworld.common.exception.BusinessException;
import com.newworld.entity.Project;
import com.newworld.entity.Task;
import com.newworld.mapper.ProjectMapper;
import com.newworld.mapper.TaskMapper;
import com.newworld.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public List<Project> getByGroupId(Long userId, Long groupId) {
        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId)
                        .eq(groupId != null, Project::getGroupId, groupId)
                        .orderByAsc(Project::getSortOrder));
    }

    @Override
    public Project create(Project project) {
        projectMapper.insert(project);
        return project;
    }

    @Override
    public Project update(Project project) {
        Project existing = projectMapper.selectById(project.getId());
        if (existing == null) {
            throw new BusinessException("项目不存在");
        }
        projectMapper.updateById(project);
        return project;
    }

    @Override
    public void delete(Long id) {
        // 检查项目下是否有任务
        Long taskCount = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getProjectId, id));
        if (taskCount > 0) {
            throw new BusinessException("项目下存在任务，无法删除");
        }
        projectMapper.deleteById(id);
    }

    @Override
    public void updateSort(List<SortItem> items) {
        for (SortItem item : items) {
            Project project = new Project();
            project.setId(item.getId());
            project.setSortOrder(item.getSortOrder());
            projectMapper.updateById(project);
        }
    }
}
