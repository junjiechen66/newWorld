package com.newworld.controller;

import com.newworld.common.Result;
import com.newworld.common.annotation.RequirePermission;
import com.newworld.config.AuthInterceptor;
import com.newworld.dto.SortItem;
import com.newworld.entity.Project;
import com.newworld.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "项目管理", description = "项目 CRUD")
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Operation(summary = "按分组获取项目列表")
    @GetMapping
    public Result<List<Project>> list(@RequestParam(required = false) Long groupId) {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(projectService.getByGroupId(userId, groupId));
    }

    @Operation(summary = "新建项目")
    @PostMapping
    public Result<Project> create(@RequestBody Project project) {
        Long userId = AuthInterceptor.getCurrentUserId();
        project.setUserId(userId);
        return Result.success("创建成功", projectService.create(project));
    }

    @Operation(summary = "更新项目")
    @PutMapping("/{id}")
    @RequirePermission(resourceType = "PROJECT", level = "EDIT")
    public Result<Project> update(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        return Result.success(projectService.update(project));
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    @RequirePermission(resourceType = "PROJECT", level = "OWNER")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "批量更新项目排序")
    @PutMapping("/sort")
    public Result<Void> sort(@RequestBody List<SortItem> items) {
        projectService.updateSort(items);
        return Result.success("排序更新成功");
    }
}
