package com.newworld.controller;

import com.newworld.common.Result;
import com.newworld.common.annotation.RequirePermission;
import com.newworld.config.AuthInterceptor;
import com.newworld.dto.TaskQueryDTO;
import com.newworld.dto.TaskStatisticsVO;
import com.newworld.entity.Task;
import com.newworld.service.ShareService;
import com.newworld.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "任务管理", description = "任务全生命周期 CRUD 及操作")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ShareService shareService;

    @Operation(summary = "查询任务列表（含共享给我的）")
    @GetMapping
    public Result<List<Task>> list(TaskQueryDTO query) {
        Long userId = AuthInterceptor.getCurrentUserId();
        List<Task> list = shareService.queryTasksWithShare(
                userId, query.getIsNote(), query.getProjectId(),
                query.getStatus(), query.getPriority(), query.getKeyword(), query.getArchived());
        return Result.success(list);
    }

    @Operation(summary = "获取单个任务")
    @GetMapping("/{id}")
    @RequirePermission(resourceType = "TASK", level = "VIEW")
    public Result<Task> getById(@PathVariable Long id) {
        return Result.success(taskService.getById(id));
    }

    @Operation(summary = "新建任务")
    @PostMapping
    public Result<Task> create(@RequestBody Task task) {
        Long userId = AuthInterceptor.getCurrentUserId();
        task.setUserId(userId);
        return Result.success("创建成功", taskService.create(task));
    }

    @Operation(summary = "更新任务（含拖拽排期）")
    @PutMapping("/{id}")
    @RequirePermission(resourceType = "TASK", level = "EDIT")
    public Result<Task> update(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        return Result.success(taskService.update(task));
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    @RequirePermission(resourceType = "TASK", level = "OWNER")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "更新任务状态")
    @PutMapping("/{id}/status")
    @RequirePermission(resourceType = "TASK", level = "EDIT")
    public Result<Task> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(taskService.updateStatus(id, body.get("status")));
    }

    @Operation(summary = "设置任务优先级")
    @PutMapping("/{id}/priority")
    @RequirePermission(resourceType = "TASK", level = "EDIT")
    public Result<Task> updatePriority(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(taskService.updatePriority(id, body.get("priority")));
    }

    @Operation(summary = "创建任务副本")
    @PutMapping("/{id}/duplicate")
    @RequirePermission(resourceType = "TASK", level = "EDIT")
    public Result<Task> duplicate(@PathVariable Long id) {
        return Result.success("复制成功", taskService.duplicate(id));
    }

    @Operation(summary = "归档任务")
    @PutMapping("/{id}/archive")
    @RequirePermission(resourceType = "TASK", level = "EDIT")
    public Result<Task> archive(@PathVariable Long id) {
        return Result.success("归档成功", taskService.archive(id));
    }

    @Operation(summary = "归档笔记")
    @PutMapping("/{id}/note-archive")
    @RequirePermission(resourceType = "TASK", level = "EDIT")
    public Result<Task> noteArchive(@PathVariable Long id) {
        return Result.success("归档成功", taskService.noteArchive(id));
    }

    @Operation(summary = "取消归档笔记")
    @PutMapping("/{id}/note-unarchive")
    @RequirePermission(resourceType = "TASK", level = "EDIT")
    public Result<Task> noteUnarchive(@PathVariable Long id) {
        return Result.success("取消归档成功", taskService.noteUnarchive(id));
    }

    @Operation(summary = "转换为笔记")
    @PutMapping("/{id}/convert-note")
    @RequirePermission(resourceType = "TASK", level = "EDIT")
    public Result<Task> convertToNote(@PathVariable Long id) {
        return Result.success("转换成功", taskService.convertToNote(id));
    }

    @Operation(summary = "搜索任务")
    @GetMapping("/search")
    public Result<List<Task>> search(@RequestParam String keyword) {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(taskService.search(keyword, userId));
    }

    @Operation(summary = "任务统计")
    @GetMapping("/statistics")
    public Result<TaskStatisticsVO> statistics() {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(taskService.statistics(userId));
    }

    @Operation(summary = "今日任务")
    @GetMapping("/today")
    public Result<List<Task>> todayTasks() {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(taskService.getTodayTasks(userId));
    }
}

