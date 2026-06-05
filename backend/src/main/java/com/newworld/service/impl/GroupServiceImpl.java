package com.newworld.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.newworld.common.exception.BusinessException;
import com.newworld.dto.TreeVO;
import com.newworld.entity.Group;
import com.newworld.entity.Project;
import com.newworld.entity.Task;
import com.newworld.mapper.GroupMapper;
import com.newworld.mapper.ProjectMapper;
import com.newworld.mapper.TaskMapper;
import com.newworld.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public List<Group> getList(Long userId) {
        return groupMapper.selectList(
                new LambdaQueryWrapper<Group>()
                        .eq(Group::getUserId, userId)
                        .orderByAsc(Group::getSortOrder));
    }

    @Override
    public List<TreeVO> getTree(Long userId, Long projectId) {
        // 查询该用户所有分组
        List<Group> groups = getList(userId);
        // 查询所有项目（如果指定了projectId，只查该项目的）
        List<Project> projects;
        if (projectId != null) {
            Project p = projectMapper.selectById(projectId);
            projects = p != null ? java.util.Collections.singletonList(p) : java.util.Collections.emptyList();
        } else {
            projects = projectMapper.selectList(
                    new LambdaQueryWrapper<Project>()
                            .eq(Project::getUserId, userId)
                            .orderByAsc(Project::getSortOrder));
        }

        // 查询所有任务（仅待办和进行中）
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .in(Task::getStatus, "TODO", "IN_PROGRESS")
                        .orderByAsc(Task::getSortOrder));

        // 按项目ID分组任务
        Map<Long, List<Task>> taskMap = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getProjectId() != null ? t.getProjectId() : 0L));

        // 按分组ID分组项目
        Map<Long, List<Project>> projectMap = projects.stream()
                .collect(Collectors.groupingBy(Project::getGroupId));

        // 构建树
        List<TreeVO> tree = new ArrayList<>();
        for (Group group : groups) {
            TreeVO groupNode = new TreeVO();
            groupNode.setId(group.getId());
            groupNode.setName(group.getName());
            groupNode.setType("group");
            groupNode.setSortOrder(group.getSortOrder());

            List<TreeVO> projectNodes = new ArrayList<>();
            List<Project> groupProjects = projectMap.getOrDefault(group.getId(), new ArrayList<>());
            for (Project project : groupProjects) {
                TreeVO projectNode = new TreeVO();
                projectNode.setId(project.getId());
                projectNode.setName(project.getName());
                projectNode.setType("project");
                projectNode.setColor(project.getColor());
                projectNode.setSortOrder(project.getSortOrder());

                // 添加任务子节点
                List<TreeVO> taskNodes = new ArrayList<>();
                List<Task> projectTasks = taskMap.getOrDefault(project.getId(), new ArrayList<>());
                for (Task task : projectTasks) {
                    TreeVO taskNode = new TreeVO();
                    taskNode.setId(task.getId());
                    taskNode.setName(task.getTitle());
                    taskNode.setType("task");
                    taskNode.setPriority(task.getPriority());
                    taskNode.setStatus(task.getStatus());
                    taskNode.setSortOrder(task.getSortOrder());
                    taskNodes.add(taskNode);
                }
                projectNode.setChildren(taskNodes);
                projectNodes.add(projectNode);
            }
            groupNode.setChildren(projectNodes);
            tree.add(groupNode);
        }
        return tree;
    }

    @Override
    public Group create(Group group) {
        groupMapper.insert(group);
        return group;
    }

    @Override
    public Group update(Group group) {
        Group existing = groupMapper.selectById(group.getId());
        if (existing == null) {
            throw new BusinessException("分组不存在");
        }
        groupMapper.updateById(group);
        return group;
    }

    @Override
    public void delete(Long id) {
        // 检查分组下是否有项目
        Long projectCount = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getGroupId, id));
        if (projectCount > 0) {
            throw new BusinessException("分组下存在项目，无法删除");
        }
        groupMapper.deleteById(id);
    }
}
