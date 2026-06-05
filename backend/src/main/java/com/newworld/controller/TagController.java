package com.newworld.controller;

import com.newworld.common.Result;
import com.newworld.config.AuthInterceptor;
import com.newworld.entity.Tag;
import com.newworld.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理", description = "标签 CRUD")
@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @Operation(summary = "获取标签列表")
    @GetMapping
    public Result<List<Tag>> list() {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(tagService.getList(userId));
    }

    @Operation(summary = "新建标签")
    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag) {
        Long userId = AuthInterceptor.getCurrentUserId();
        tag.setUserId(userId);
        return Result.success("创建成功", tagService.create(tag));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success("删除成功");
    }
}
