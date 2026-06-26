package com.newworld.controller;

import com.newworld.common.JwtUtil;
import com.newworld.common.Result;
import com.newworld.entity.Task;
import com.newworld.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 公开分享访问接口（不需要登录即可访问 GET）
 */
@Tag(name = "公开分享", description = "通过分享码访问资源")
@RestController
@RequestMapping("/api/public/share")
public class PublicShareController {

    @Autowired
    private ShareService shareService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "通过分享码获取资源内容")
    @GetMapping("/{shareCode}")
    public Result<Map<String, Object>> getSharedResource(@PathVariable String shareCode,
                                                          HttpServletRequest request) {
        Long currentUserId = extractUserIdFromToken(request);
        return Result.success(shareService.getSharedResource(shareCode, currentUserId));
    }

    @Operation(summary = "通过分享码编辑资源（需登录）")
    @PutMapping("/{shareCode}")
    public Result<Task> updateSharedResource(@PathVariable String shareCode,
                                              @RequestBody Task task,
                                              HttpServletRequest request) {
        Long currentUserId = extractUserIdFromToken(request);
        return Result.success(shareService.updateSharedResource(shareCode, currentUserId, task));
    }

    /**
     * 尝试从请求中提取用户ID（不强制，未登录返回 null）
     */
    private Long extractUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) return null;
        if (token.startsWith("Bearer ")) token = token.substring(7);
        try {
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
