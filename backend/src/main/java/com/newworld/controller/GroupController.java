package com.newworld.controller;

import com.newworld.common.Result;
import com.newworld.config.AuthInterceptor;
import com.newworld.dto.SortItem;
import com.newworld.dto.TreeVO;
import com.newworld.entity.Group;
import com.newworld.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "分组管理", description = "项目分组 CRUD 及树形结构")
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Operation(summary = "获取分组列表")
    @GetMapping
    public Result<List<Group>> list() {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(groupService.getList(userId));
    }

    @Operation(summary = "获取树形结构（分组→项目→任务）")
    @GetMapping("/tree")
    public Result<List<TreeVO>> tree(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (startDate != null && endDate != null) {
            return Result.success(groupService.getTree(userId, projectId, startDate, endDate));
        }
        return Result.success(groupService.getTree(userId, projectId));
    }

    @Operation(summary = "新建分组")
    @PostMapping
    public Result<Group> create(@RequestBody Group group) {
        Long userId = AuthInterceptor.getCurrentUserId();
        group.setUserId(userId);
        return Result.success("创建成功", groupService.create(group));
    }

    @Operation(summary = "更新分组")
    @PutMapping("/{id}")
    public Result<Group> update(@PathVariable Long id, @RequestBody Group group) {
        group.setId(id);
        return Result.success(groupService.update(group));
    }

    @Operation(summary = "删除分组")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        groupService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "批量更新分组排序")
    @PutMapping("/sort")
    public Result<Void> sort(@RequestBody List<SortItem> items) {
        groupService.updateSort(items);
        return Result.success("排序更新成功");
    }
}
