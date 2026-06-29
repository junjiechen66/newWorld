package com.newworld.service;

import com.newworld.entity.Task;
import com.newworld.dto.TaskQueryDTO;
import com.newworld.dto.TaskStatisticsVO;

import java.util.List;

public interface TaskService {

    /**
     * 查询任务列表
     */
    List<Task> queryList(TaskQueryDTO query, Long userId);

    /**
     * 获取单个任务
     */
    Task getById(Long id);

    /**
     * 新建任务
     */
    Task create(Task task);

    /**
     * 更新任务
     */
    Task update(Task task);

    /**
     * 删除任务
     */
    void delete(Long id);

    /**
     * 更新状态
     */
    Task updateStatus(Long id, String status);

    /**
     * 更新优先级
     */
    Task updatePriority(Long id, String priority);

    /**
     * 复制任务
     */
    Task duplicate(Long id);

    /**
     * 归档任务
     */
    Task archive(Long id);

    /**
     * 归档笔记
     */
    Task noteArchive(Long id);

    /**
     * 取消归档笔记
     */
    Task noteUnarchive(Long id);

    /**
     * 转换为笔记
     */
    Task convertToNote(Long id);

    /**
     * 搜索任务
     */
    List<Task> search(String keyword, Long userId);

    /**
     * 获取任务统计
     */
    TaskStatisticsVO statistics(Long userId);

    /**
     * 获取今日任务
     */
    List<Task> getTodayTasks(Long userId);
}
