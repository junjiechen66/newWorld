package com.newworld.controller;

import com.newworld.common.Result;
import com.newworld.config.AuthInterceptor;
import com.newworld.dto.*;
import com.newworld.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Tag(name = "共享管理", description = "共享成员与分享链接的 CRUD")
@RestController
@RequestMapping("/api/shares")
public class ShareController {

    @Autowired
    private ShareService shareService;

    // ========== 共享成员 ==========

    @Operation(summary = "获取共享成员列表")
    @GetMapping
    public Result<List<ShareMemberVO>> getMembers(@RequestParam String resourceType,
                                                   @RequestParam Long resourceId) {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(shareService.getMembers(resourceType, resourceId, userId));
    }

    @Operation(summary = "添加共享成员")
    @PostMapping
    public Result<Void> addMember(@Valid @RequestBody AddMemberDTO dto) {
        Long userId = AuthInterceptor.getCurrentUserId();
        shareService.addMember(userId, dto);
        return Result.success("添加成功");
    }

    @Operation(summary = "修改成员权限")
    @PutMapping("/{id}")
    public Result<Void> updateMember(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = AuthInterceptor.getCurrentUserId();
        shareService.updateMemberPermission(userId, id, body.get("permission"));
        return Result.success("修改成功");
    }

    @Operation(summary = "移除共享成员")
    @DeleteMapping("/{id}")
    public Result<Void> removeMember(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        shareService.removeMember(userId, id);
        return Result.success("移除成功");
    }

    // ========== 分享链接 ==========

    @Operation(summary = "获取分享链接列表")
    @GetMapping("/links")
    public Result<List<ShareLinkVO>> getShareLinks(@RequestParam String resourceType,
                                                    @RequestParam Long resourceId) {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(shareService.getShareLinks(resourceType, resourceId, userId));
    }

    @Operation(summary = "创建分享链接")
    @PostMapping("/links")
    public Result<ShareLinkVO> createShareLink(@Valid @RequestBody CreateShareLinkDTO dto) {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success("创建成功", shareService.createShareLink(userId, dto));
    }

    @Operation(summary = "修改分享链接")
    @PutMapping("/links/{id}")
    public Result<Void> updateShareLink(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = AuthInterceptor.getCurrentUserId();
        String permission = (String) body.get("permission");
        Boolean requireLogin = body.containsKey("requireLogin") ? (Boolean) body.get("requireLogin") : null;
        shareService.updateShareLink(userId, id, permission, requireLogin);
        return Result.success("修改成功");
    }

    @Operation(summary = "禁用分享链接")
    @DeleteMapping("/links/{id}")
    public Result<Void> disableShareLink(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        shareService.disableShareLink(userId, id);
        return Result.success("已禁用");
    }

    // ========== 辅助 ==========

    @Operation(summary = "搜索用户")
    @GetMapping("/search-users")
    public Result<List<Map<String, Object>>> searchUsers(@RequestParam String keyword) {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(shareService.searchUsers(keyword, userId));
    }

    @Operation(summary = "共享给我的资源")
    @GetMapping("/shared-with-me")
    public Result<List<SharedResourceVO>> sharedWithMe() {
        Long userId = AuthInterceptor.getCurrentUserId();
        return Result.success(shareService.getSharedWithMe(userId));
    }
}
