package com.newworld.service;

import com.newworld.dto.*;
import com.newworld.entity.ShareLink;
import com.newworld.entity.Task;

import java.util.List;
import java.util.Map;

public interface ShareService {

    /**
     * 获取某资源的共享成员列表
     */
    List<ShareMemberVO> getMembers(String resourceType, Long resourceId, Long ownerId);

    /**
     * 添加共享成员
     */
    void addMember(Long ownerId, AddMemberDTO dto);

    /**
     * 修改成员权限
     */
    void updateMemberPermission(Long ownerId, Long memberId, String permission);

    /**
     * 移除共享成员
     */
    void removeMember(Long ownerId, Long memberId);

    /**
     * 获取某资源的所有分享链接
     */
    List<ShareLinkVO> getShareLinks(String resourceType, Long resourceId, Long ownerId);

    /**
     * 创建分享链接
     */
    ShareLinkVO createShareLink(Long ownerId, CreateShareLinkDTO dto);

    /**
     * 修改分享链接
     */
    void updateShareLink(Long ownerId, Long linkId, String permission, Boolean requireLogin);

    /**
     * 禁用分享链接
     */
    void disableShareLink(Long ownerId, Long linkId);

    /**
     * 通过分享码获取资源内容
     */
    Map<String, Object> getSharedResource(String shareCode, Long currentUserId);

    /**
     * 通过分享码编辑资源
     */
    Task updateSharedResource(String shareCode, Long currentUserId, Task task);

    /**
     * 搜索用户（排除自己）
     */
    List<Map<String, Object>> searchUsers(String keyword, Long currentUserId);

    /**
     * 获取共享给我的资源列表
     */
    List<SharedResourceVO> getSharedWithMe(Long userId);

    /**
     * 通用权限判断
     */
    boolean hasPermission(String resourceType, Long resourceId, Long userId, String requiredLevel);

    /**
     * 带共享信息的任务列表查询
     */
    List<Task> queryTasksWithShare(Long userId, Boolean isNote, Long projectId, String status, String priority, String keyword);
}
