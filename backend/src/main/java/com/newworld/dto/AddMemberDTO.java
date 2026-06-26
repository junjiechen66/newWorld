package com.newworld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "添加共享成员请求")
public class AddMemberDTO {

    @NotBlank(message = "资源类型不能为空")
    @Schema(description = "资源类型: PROJECT / TASK")
    private String resourceType;

    @NotNull(message = "资源ID不能为空")
    @Schema(description = "资源ID")
    private Long resourceId;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "被共享用户用户名")
    private String username;

    @Schema(description = "权限: VIEW / EDIT，默认 VIEW")
    private String permission = "VIEW";

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
}
