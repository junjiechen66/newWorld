package com.newworld.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.newworld.common.exception.BusinessException;
import com.newworld.dto.TaskQueryDTO;
import com.newworld.dto.TaskStatisticsVO;
import com.newworld.entity.Task;
import com.newworld.mapper.TaskMapper;
import com.newworld.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public List<Task> queryList(TaskQueryDTO query, Long userId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(query.getProjectId() != null, Task::getProjectId, query.getProjectId())
                .eq(StrUtil.isNotBlank(query.getStatus()), Task::getStatus, query.getStatus())
                .eq(StrUtil.isNotBlank(query.getPriority()), Task::getPriority, query.getPriority())
                .eq(StrUtil.isNotBlank(query.getTag()), Task::getTag, query.getTag())
                .eq(query.getIsNote() != null, Task::getIsNote, query.getIsNote())
                .ge(query.getStartDateFrom() != null, Task::getStartDate, query.getStartDateFrom())
                .le(query.getStartDateTo() != null, Task::getStartDate, query.getStartDateTo())
                .ge(query.getDueDateFrom() != null, Task::getDueDate, query.getDueDateFrom())
                .le(query.getDueDateTo() != null, Task::getDueDate, query.getDueDateTo())
                .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                        .like(Task::getTitle, query.getKeyword())
                        .or()
                        .like(Task::getDescription, query.getKeyword()))
                .orderByAsc(Task::getSortOrder)
                .orderByDesc(Task::getCreateTime);

        return taskMapper.selectList(wrapper);
    }

    @Override
    public Task getById(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    @Override
    public Task create(Task task) {
        if (task.getPriority() == null) {
            task.setPriority("Q1");
        }
        if (task.getStatus() == null) {
            task.setStatus("INCOMPLETE");
        }
        if (task.getIsNote() == null) {
            task.setIsNote(false);
        }
        taskMapper.insert(task);
        return task;
    }

    @Override
    public Task update(Task task) {
        Task existing = taskMapper.selectById(task.getId());
        if (existing == null) {
            throw new BusinessException("任务不存在");
        }
        taskMapper.updateById(task);
        return taskMapper.selectById(task.getId());
    }

    @Override
    public void delete(Long id) {
        Task existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("任务不存在");
        }
        taskMapper.deleteById(id);
    }

    @Override
    public Task updateStatus(Long id, String status) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        task.setStatus(status);
        taskMapper.updateById(task);
        return task;
    }

    @Override
    public Task updatePriority(Long id, String priority) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        task.setPriority(priority);
        taskMapper.updateById(task);
        return task;
    }

    @Override
    public Task duplicate(Long id) {
        Task original = taskMapper.selectById(id);
        if (original == null) {
            throw new BusinessException("任务不存在");
        }
        Task copy = new Task();
        copy.setUserId(original.getUserId());
        copy.setProjectId(original.getProjectId());
        copy.setTitle(original.getTitle() + " (副本)");
        copy.setDescription(original.getDescription());
        copy.setPriority(original.getPriority());
        copy.setStatus("INCOMPLETE");
        copy.setTag(original.getTag());
        copy.setStartDate(original.getStartDate());
        copy.setDueDate(original.getDueDate());
        copy.setIsNote(false);
        taskMapper.insert(copy);
        return copy;
    }

    @Override
    public Task archive(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        task.setStatus("SHELVED");
        taskMapper.updateById(task);
        return task;
    }

    @Override
    public Task convertToNote(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        task.setIsNote(true);
        taskMapper.updateById(task);
        return task;
    }

    @Override
    public List<Task> search(String keyword, Long userId) {
        return taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .like(Task::getTitle, keyword)
                        .orderByDesc(Task::getCreateTime));
    }

    @Override
    public TaskStatisticsVO statistics(Long userId) {
        TaskStatisticsVO vo = new TaskStatisticsVO();

        // 只统计任务（不包含笔记）
        vo.setIncompleteCount(taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .eq(Task::getIsNote, false)
                        .eq(Task::getStatus, "INCOMPLETE")));
        vo.setDoneCount(taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .eq(Task::getIsNote, false)
                        .eq(Task::getStatus, "DONE")));
        vo.setShelvedCount(taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .eq(Task::getIsNote, false)
                        .eq(Task::getStatus, "SHELVED")));

        long total = vo.getIncompleteCount() + vo.getDoneCount() + vo.getShelvedCount();
        vo.setTotalCount(total);

        return vo;
    }

    @Override
    public List<Task> getTodayTasks(Long userId) {
        LocalDate today = LocalDate.now();
        return taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .eq(Task::getIsNote, false)
                        .and(wrapper -> wrapper
                                // 开始结束日期包含当天: (start_date <= today OR IS NULL) AND (due_date >= today OR IS NULL)
                                .and(w -> w
                                        .le(Task::getStartDate, today)
                                        .or()
                                        .isNull(Task::getStartDate)
                                )
                                .and(w -> w
                                        .ge(Task::getDueDate, today)
                                        .or()
                                        .isNull(Task::getDueDate)
                                )
                        )
                        // 排除两个日期都为空的任务
                        .and(w -> w
                                .isNotNull(Task::getStartDate)
                                .or()
                                .isNotNull(Task::getDueDate)
                        )
                        .orderByAsc(Task::getPriority)
                        .orderByAsc(Task::getDueDate)
        );
    }
}
