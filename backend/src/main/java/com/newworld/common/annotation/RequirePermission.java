package com.newworld.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * 标注在 Controller 方法上，AOP 切面自动校验当前用户对资源的访问权限
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 资源类型：TASK / PROJECT
     */
    String resourceType();

    /**
     * 最低权限级别：VIEW / EDIT / OWNER
     * VIEW: 可查看
     * EDIT: 可编辑（包含查看）
     * OWNER: 仅所有者（包含编辑）
     */
    String level() default "VIEW";

    /**
     * 从哪个参数取资源ID，默认 "id"（对应 @PathVariable 名）
     */
    String resourceIdParam() default "id";
}
