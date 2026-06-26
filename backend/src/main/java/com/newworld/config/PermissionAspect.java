package com.newworld.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.newworld.common.annotation.RequirePermission;
import com.newworld.common.exception.BusinessException;
import com.newworld.entity.Project;
import com.newworld.entity.ShareMember;
import com.newworld.entity.Task;
import com.newworld.mapper.ProjectMapper;
import com.newworld.mapper.ShareMemberMapper;
import com.newworld.mapper.TaskMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 权限校验 AOP 切面
 * 拦截 @RequirePermission 注解的方法，校验当前用户对资源的访问权限
 */
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ShareMemberMapper shareMemberMapper;

    @Around("@annotation(anno)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequirePermission anno) throws Throwable {
        Long currentUserId = AuthInterceptor.getCurrentUserId();
        Long resourceId = extractResourceId(pjp, anno.resourceIdParam());

        String resourceType = anno.resourceType();
        String requiredLevel = anno.level();

        // 1. 检查是否是资源所有者
        if (isOwner(resourceType, resourceId, currentUserId)) {
            return pjp.proceed();
        }

        // OWNER 级别要求必须是所有者
        if ("OWNER".equals(requiredLevel)) {
            throw new BusinessException(403, "没有权限，仅所有者可操作");
        }

        // 2. 检查直接授权（任务/项目级 share_member）
        ShareMember member = findMember(resourceType, resourceId, currentUserId);
        if (member != null && isPermissionSufficient(member.getPermission(), requiredLevel)) {
            return pjp.proceed();
        }

        // 3. 如果是 TASK 类型，向上检查 PROJECT 级别的共享传递
        if ("TASK".equals(resourceType)) {
            Task task = taskMapper.selectById(resourceId);
            if (task != null && task.getProjectId() != null) {
                ShareMember projectMember = findMember("PROJECT", task.getProjectId(), currentUserId);
                if (projectMember != null && isPermissionSufficient(projectMember.getPermission(), requiredLevel)) {
                    return pjp.proceed();
                }
            }
        }

        throw new BusinessException(403, "没有访问权限");
    }

    /**
     * 判断当前用户是否为资源所有者
     */
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

    /**
     * 查找某资源对某用户的直接授权
     */
    private ShareMember findMember(String resourceType, Long resourceId, Long userId) {
        return shareMemberMapper.selectOne(
                new LambdaQueryWrapper<ShareMember>()
                        .eq(ShareMember::getResourceType, resourceType)
                        .eq(ShareMember::getResourceId, resourceId)
                        .eq(ShareMember::getUserId, userId));
    }

    /**
     * 判断实际权限是否满足所需权限
     * 权限排序: OWNER > EDIT > VIEW > NONE
     */
    private boolean isPermissionSufficient(String actualPermission, String requiredLevel) {
        int actual = permissionRank(actualPermission);
        int required = permissionRank(requiredLevel);
        return actual >= required;
    }

    private int permissionRank(String permission) {
        if ("OWNER".equals(permission)) return 3;
        if ("EDIT".equals(permission)) return 2;
        if ("VIEW".equals(permission)) return 1;
        return 0;
    }

    /**
     * 从方法参数中提取资源ID
     */
    private Long extractResourceId(ProceedingJoinPoint pjp, String paramName) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] paramNames = sig.getParameterNames();
        Object[] args = pjp.getArgs();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (paramName.equals(paramNames[i]) && args[i] != null) {
                    if (args[i] instanceof Long) {
                        return (Long) args[i];
                    }
                    try {
                        return Long.parseLong(args[i].toString());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        throw new BusinessException(400, "无法获取资源ID");
    }
}
