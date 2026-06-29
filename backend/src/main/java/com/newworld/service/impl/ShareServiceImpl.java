package com.newworld.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.newworld.common.exception.BusinessException;
import com.newworld.dto.*;
import com.newworld.entity.*;
import com.newworld.mapper.*;
import com.newworld.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ShareServiceImpl implements ShareService {

    private static final String SHARE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private ShareMemberMapper shareMemberMapper;

    @Autowired
    private ShareLinkMapper shareLinkMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<ShareMemberVO> getMembers(String resourceType, Long resourceId, Long ownerId) {
        // 校验所有权
        checkOwnership(resourceType, resourceId, ownerId);

        List<Map<String, Object>> rows = shareMemberMapper.selectMembersWithUser(resourceType, resourceId);
        List<ShareMemberVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ShareMemberVO vo = new ShareMemberVO();
            vo.setId(toLong(row.get("id")));
            vo.setUserId(toLong(row.get("userId")));
            vo.setUsername((String) row.get("username"));
            vo.setNickname((String) row.get("nickname"));
            vo.setAvatar((String) row.get("avatar"));
            vo.setPermission((String) row.get("permission"));
            Object ct = row.get("createTime");
            if (ct instanceof java.sql.Timestamp) {
                vo.setCreateTime(((java.sql.Timestamp) ct).toLocalDateTime());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void addMember(Long ownerId, AddMemberDTO dto) {
        // 校验所有权
        checkOwnership(dto.getResourceType(), dto.getResourceId(), ownerId);

        // 查找目标用户
        User targetUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (targetUser == null) {
            throw new BusinessException("用户不存在: " + dto.getUsername());
        }
        if (targetUser.getId().equals(ownerId)) {
            throw new BusinessException("不能共享给自己");
        }

        // 检查是否已存在，存在则更新权限
        ShareMember existing = shareMemberMapper.selectOne(
                new LambdaQueryWrapper<ShareMember>()
                        .eq(ShareMember::getResourceType, dto.getResourceType())
                        .eq(ShareMember::getResourceId, dto.getResourceId())
                        .eq(ShareMember::getUserId, targetUser.getId()));
        if (existing != null) {
            existing.setPermission(dto.getPermission());
            shareMemberMapper.updateById(existing);
            return;
        }

        ShareMember member = new ShareMember();
        member.setResourceType(dto.getResourceType());
        member.setResourceId(dto.getResourceId());
        member.setOwnerId(ownerId);
        member.setUserId(targetUser.getId());
        member.setPermission(dto.getPermission());
        shareMemberMapper.insert(member);
    }

    @Override
    public void updateMemberPermission(Long ownerId, Long memberId, String permission) {
        ShareMember member = shareMemberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("共享记录不存在");
        }
        checkOwnership(member.getResourceType(), member.getResourceId(), ownerId);
        member.setPermission(permission);
        shareMemberMapper.updateById(member);
    }

    @Override
    public void removeMember(Long ownerId, Long memberId) {
        ShareMember member = shareMemberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("共享记录不存在");
        }
        checkOwnership(member.getResourceType(), member.getResourceId(), ownerId);
        shareMemberMapper.deleteById(memberId);
    }

    @Override
    public List<ShareLinkVO> getShareLinks(String resourceType, Long resourceId, Long ownerId) {
        checkOwnership(resourceType, resourceId, ownerId);
        List<ShareLink> links = shareLinkMapper.selectList(
                new LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getResourceType, resourceType)
                        .eq(ShareLink::getResourceId, resourceId)
                        .orderByDesc(ShareLink::getCreateTime));
        List<ShareLinkVO> result = new ArrayList<>();
        for (ShareLink link : links) {
            result.add(toShareLinkVO(link));
        }
        return result;
    }

    @Override
    public ShareLinkVO createShareLink(Long ownerId, CreateShareLinkDTO dto) {
        checkOwnership(dto.getResourceType(), dto.getResourceId(), ownerId);

        ShareLink link = new ShareLink();
        link.setShareCode(generateShareCode());
        link.setResourceType(dto.getResourceType());
        link.setResourceId(dto.getResourceId());
        link.setOwnerId(ownerId);
        link.setPermission(dto.getPermission());
        link.setRequireLogin(dto.getRequireLogin() != null ? dto.getRequireLogin() : false);
        link.setExpireTime(dto.getExpireTime());
        link.setIsActive(true);
        shareLinkMapper.insert(link);
        return toShareLinkVO(link);
    }

    @Override
    public void updateShareLink(Long ownerId, Long linkId, String permission, Boolean requireLogin) {
        ShareLink link = shareLinkMapper.selectById(linkId);
        if (link == null) {
            throw new BusinessException("分享链接不存在");
        }
        checkOwnership(link.getResourceType(), link.getResourceId(), ownerId);
        if (permission != null) link.setPermission(permission);
        if (requireLogin != null) link.setRequireLogin(requireLogin);
        shareLinkMapper.updateById(link);
    }

    @Override
    public void disableShareLink(Long ownerId, Long linkId) {
        ShareLink link = shareLinkMapper.selectById(linkId);
        if (link == null) {
            throw new BusinessException("分享链接不存在");
        }
        checkOwnership(link.getResourceType(), link.getResourceId(), ownerId);
        link.setIsActive(false);
        shareLinkMapper.updateById(link);
    }

    @Override
    public Map<String, Object> getSharedResource(String shareCode, Long currentUserId) {
        ShareLink link = shareLinkMapper.selectOne(
                new LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getShareCode, shareCode)
                        .eq(ShareLink::getIsActive, true));
        if (link == null) {
            throw new BusinessException("分享链接不存在或已失效");
        }
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("分享链接已过期");
        }
        // EDIT 权限需要登录
        if ("EDIT".equals(link.getPermission()) && link.getRequireLogin() && currentUserId == null) {
            throw new BusinessException(401, "编辑需要登录");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("permission", link.getPermission());
        result.put("requireLogin", link.getRequireLogin());
        result.put("resourceType", link.getResourceType());
        result.put("resourceId", link.getResourceId());

        // 获取资源内容
        if ("TASK".equals(link.getResourceType())) {
            Task task = taskMapper.selectById(link.getResourceId());
            if (task == null) {
                throw new BusinessException("资源不存在");
            }
            result.put("resource", task);
            // 获取所有者信息
            User owner = userMapper.selectById(task.getUserId());
            if (owner != null) {
                result.put("ownerNickname", owner.getNickname());
                result.put("ownerUsername", owner.getUsername());
            }
            // 获取项目名称
            if (task.getProjectId() != null) {
                Project project = projectMapper.selectById(task.getProjectId());
                if (project != null) {
                    result.put("projectName", project.getName());
                }
            }
        }
        return result;
    }

    @Override
    public Task updateSharedResource(String shareCode, Long currentUserId, Task task) {
        if (currentUserId == null) {
            throw new BusinessException(401, "编辑需要登录");
        }
        ShareLink link = shareLinkMapper.selectOne(
                new LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getShareCode, shareCode)
                        .eq(ShareLink::getIsActive, true));
        if (link == null) {
            throw new BusinessException("分享链接不存在或已失效");
        }
        if (!"EDIT".equals(link.getPermission())) {
            throw new BusinessException(403, "该分享链接仅支持查看");
        }
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("分享链接已过期");
        }

        Task existing = taskMapper.selectById(link.getResourceId());
        if (existing == null) {
            throw new BusinessException("资源不存在");
        }
        existing.setTitle(task.getTitle());
        existing.setDescription(task.getDescription());
        if (task.getStatus() != null) existing.setStatus(task.getStatus());
        if (task.getPriority() != null) existing.setPriority(task.getPriority());
        taskMapper.updateById(existing);
        return taskMapper.selectById(existing.getId());
    }

    @Override
    public List<Map<String, Object>> searchUsers(String keyword, Long currentUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .ne(User::getId, currentUserId)
                .and(w -> w.like(User::getUsername, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .last("LIMIT 10");
        List<User> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("nickname", u.getNickname());
            map.put("avatar", u.getAvatar());
            result.add(map);
        }
        return result;
    }

    @Override
    public List<SharedResourceVO> getSharedWithMe(Long userId) {
        // 查找所有共享给当前用户的成员记录
        List<ShareMember> members = shareMemberMapper.selectList(
                new LambdaQueryWrapper<ShareMember>()
                        .eq(ShareMember::getUserId, userId));

        List<SharedResourceVO> result = new ArrayList<>();
        Set<Long> taskIds = new HashSet<>();
        Set<Long> projectIds = new HashSet<>();

        // 收集所有资源ID
        for (ShareMember m : members) {
            if ("TASK".equals(m.getResourceType())) {
                taskIds.add(m.getResourceId());
            } else if ("PROJECT".equals(m.getResourceType())) {
                projectIds.add(m.getResourceId());
            }
        }

        // 处理项目级共享：获取项目下的所有任务
        for (Long projectId : projectIds) {
            List<Task> tasks = taskMapper.selectList(
                    new LambdaQueryWrapper<Task>().eq(Task::getProjectId, projectId));
            for (Task t : tasks) {
                if (!taskIds.contains(t.getId())) {
                    taskIds.add(t.getId());
                    // 查找项目级共享的权限
                    ShareMember projectMember = shareMemberMapper.selectOne(
                            new LambdaQueryWrapper<ShareMember>()
                                    .eq(ShareMember::getResourceType, "PROJECT")
                                    .eq(ShareMember::getResourceId, projectId)
                                    .eq(ShareMember::getUserId, userId));
                    if (projectMember != null) {
                        SharedResourceVO vo = buildSharedResourceVO(t, projectMember.getPermission(),
                                projectMember.getOwnerId());
                        result.add(vo);
                    }
                }
            }
        }

        // 处理任务级共享
        for (ShareMember m : members) {
            if ("TASK".equals(m.getResourceType())) {
                Task task = taskMapper.selectById(m.getResourceId());
                if (task != null) {
                    SharedResourceVO vo = buildSharedResourceVO(task, m.getPermission(), m.getOwnerId());
                    result.add(vo);
                }
            }
        }

        return result;
    }

    @Override
    public boolean hasPermission(String resourceType, Long resourceId, Long userId, String requiredLevel) {
        // 检查所有者
        if (isOwner(resourceType, resourceId, userId)) return true;
        if ("OWNER".equals(requiredLevel)) return false;

        // 检查直接授权
        ShareMember member = shareMemberMapper.selectOne(
                new LambdaQueryWrapper<ShareMember>()
                        .eq(ShareMember::getResourceType, resourceType)
                        .eq(ShareMember::getResourceId, resourceId)
                        .eq(ShareMember::getUserId, userId));
        if (member != null && isPermissionSufficient(member.getPermission(), requiredLevel)) return true;

        // 项目级传递
        if ("TASK".equals(resourceType)) {
            Task task = taskMapper.selectById(resourceId);
            if (task != null && task.getProjectId() != null) {
                ShareMember projectMember = shareMemberMapper.selectOne(
                        new LambdaQueryWrapper<ShareMember>()
                                .eq(ShareMember::getResourceType, "PROJECT")
                                .eq(ShareMember::getResourceId, task.getProjectId())
                                .eq(ShareMember::getUserId, userId));
                if (projectMember != null && isPermissionSufficient(projectMember.getPermission(), requiredLevel)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Task> queryTasksWithShare(Long userId, Boolean isNote, Long projectId,
                                          String status, String priority, String keyword, Boolean archived) {
        List<Map<String, Object>> rows = shareMemberMapper.selectTasksWithShare(
                userId, isNote, projectId, status, priority, keyword, archived);
        List<Task> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Task task = new Task();
            task.setId(toLong(row.get("id")));
            task.setUserId(toLong(row.get("userId")));
            Object pid = row.get("projectId");
            task.setProjectId(pid != null ? toLong(pid) : null);
            Object ptid = row.get("parentTaskId");
            task.setParentTaskId(ptid != null ? toLong(ptid) : null);
            task.setTitle((String) row.get("title"));
            task.setDescription((String) row.get("description"));
            task.setPriority((String) row.get("priority"));
            task.setStatus((String) row.get("status"));
            task.setTag((String) row.get("tag"));
            Object sd = row.get("startDate");
            if (sd instanceof java.sql.Date) {
                task.setStartDate(((java.sql.Date) sd).toLocalDate());
            }
            Object dd = row.get("dueDate");
            if (dd instanceof java.sql.Date) {
                task.setDueDate(((java.sql.Date) dd).toLocalDate());
            }
            Object isNoteVal = row.get("isNote");
            task.setIsNote(isNoteVal != null && toBool(isNoteVal));
            Object archivedVal = row.get("archived");
            task.setArchived(archivedVal != null && toBool(archivedVal));
            Object so = row.get("sortOrder");
            task.setSortOrder(so != null ? ((Number) so).intValue() : 0);
            Object ct = row.get("createTime");
            if (ct instanceof java.sql.Timestamp) {
                task.setCreateTime(((java.sql.Timestamp) ct).toLocalDateTime());
            }
            Object ut = row.get("updateTime");
            if (ut instanceof java.sql.Timestamp) {
                task.setUpdateTime(((java.sql.Timestamp) ut).toLocalDateTime());
            }
            // 共享瞬态字段
            task.setAccessLevel((String) row.get("accessLevel"));
            task.setSharedByNickname((String) row.get("sharedByNickname"));
            Object smc = row.get("shareMemberCount");
            task.setShareMemberCount(smc != null ? ((Number) smc).intValue() : 0);
            Object hsl = row.get("hasShareLink");
            task.setHasShareLink(hsl != null && toBool(hsl));
            result.add(task);
        }
        return result;
    }

    // ============ 私有方法 ============

    private void checkOwnership(String resourceType, Long resourceId, Long userId) {
        if (!isOwner(resourceType, resourceId, userId)) {
            throw new BusinessException(403, "没有管理权限，仅所有者可操作");
        }
    }

    private boolean isOwner(String resourceType, Long resourceId, Long userId) {
        if ("TASK".equals(resourceType)) {
            Task task = taskMapper.selectById(resourceId);
            return task != null && userId.equals(task.getUserId());
        } else if ("PROJECT".equals(resourceType)) {
            Project project = projectMapper.selectById(resourceId);
            return project != null && userId.equals(project.getUserId());
        }
        return false;
    }

    private boolean isPermissionSufficient(String actual, String required) {
        return permissionRank(actual) >= permissionRank(required);
    }

    private int permissionRank(String p) {
        if ("OWNER".equals(p)) return 3;
        if ("EDIT".equals(p)) return 2;
        if ("VIEW".equals(p)) return 1;
        return 0;
    }

    private String generateShareCode() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(SHARE_CHARS.charAt(SECURE_RANDOM.nextInt(SHARE_CHARS.length())));
        }
        return sb.toString();
    }

    private ShareLinkVO toShareLinkVO(ShareLink link) {
        ShareLinkVO vo = new ShareLinkVO();
        vo.setId(link.getId());
        vo.setShareCode(link.getShareCode());
        vo.setShareUrl("/s/" + link.getShareCode());
        vo.setResourceType(link.getResourceType());
        vo.setResourceId(link.getResourceId());
        vo.setPermission(link.getPermission());
        vo.setRequireLogin(link.getRequireLogin());
        vo.setExpireTime(link.getExpireTime());
        vo.setIsActive(link.getIsActive());
        vo.setCreateTime(link.getCreateTime());
        return vo;
    }

    private SharedResourceVO buildSharedResourceVO(Task task, String permission, Long ownerId) {
        SharedResourceVO vo = new SharedResourceVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setIsNote(task.getIsNote());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setStartDate(task.getStartDate());
        vo.setDueDate(task.getDueDate());
        vo.setProjectId(task.getProjectId());
        vo.setAccessLevel(permission);
        vo.setCreateTime(task.getCreateTime());

        // 获取所有者信息
        User owner = userMapper.selectById(ownerId);
        if (owner != null) {
            vo.setSharedByNickname(owner.getNickname());
            vo.setSharedByUsername(owner.getUsername());
        }

        // 获取项目名称
        if (task.getProjectId() != null) {
            Project project = projectMapper.selectById(task.getProjectId());
            if (project != null) {
                vo.setProjectName(project.getName());
            }
        }
        return vo;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        return ((Number) obj).longValue();
    }

    private boolean toBool(Object obj) {
        if (obj instanceof Boolean) return (Boolean) obj;
        return ((Number) obj).intValue() == 1;
    }
}
