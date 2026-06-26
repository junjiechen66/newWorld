package com.newworld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "创建分享链接请求")
public class CreateShareLinkDTO {

    @NotBlank(message = "资源类型不能为空")
    @Schema(description = "资源类型: PROJECT / TASK")
    private String resourceType;

    @NotNull(message = "资源ID不能为空")
    @Schema(description = "资源ID")
    private Long resourceId;

    @Schema(description = "权限: VIEW / EDIT，默认 VIEW")
    private String permission = "VIEW";

    @Schema(description = "是否需要登录，默认 false")
    private Boolean requireLogin = false;

    @Schema(description = "过期时间（null 表示永不过期）")
    private LocalDateTime expireTime;

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public Boolean getRequireLogin() { return requireLogin; }
    public void setRequireLogin(Boolean requireLogin) { this.requireLogin = requireLogin; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
