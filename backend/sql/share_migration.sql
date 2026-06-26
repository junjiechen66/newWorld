-- NewWorld 共享功能迁移脚本
-- 新增 share_member 和 share_link 两张表

USE newworld;

-- 共享成员表（指定用户共享）
CREATE TABLE IF NOT EXISTS share_member (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    resource_type   VARCHAR(20)  NOT NULL COMMENT '资源类型: PROJECT / TASK',
    resource_id     BIGINT       NOT NULL COMMENT '资源ID（project.id 或 task.id）',
    owner_id        BIGINT       NOT NULL COMMENT '资源所有者用户ID',
    user_id         BIGINT       NOT NULL COMMENT '被共享用户ID',
    permission      VARCHAR(20)  NOT NULL DEFAULT 'VIEW' COMMENT '权限: VIEW(只读) / EDIT(可编辑)',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_resource_user (resource_type, resource_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_owner_id (owner_id),
    FOREIGN KEY (owner_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享成员表';

-- 分享链接表（公开链接分享）
CREATE TABLE IF NOT EXISTS share_link (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    share_code      VARCHAR(32)  NOT NULL UNIQUE COMMENT '分享码（URL安全随机串）',
    resource_type   VARCHAR(20)  NOT NULL COMMENT '资源类型: PROJECT / TASK',
    resource_id     BIGINT       NOT NULL COMMENT '资源ID',
    owner_id        BIGINT       NOT NULL COMMENT '资源所有者用户ID',
    permission      VARCHAR(20)  NOT NULL DEFAULT 'VIEW' COMMENT '权限: VIEW / EDIT',
    require_login   TINYINT(1)  DEFAULT 0 COMMENT '是否需要登录: 0-否 1-是',
    expire_time     DATETIME     NULL COMMENT '过期时间（NULL为永不过期）',
    is_active       TINYINT(1)  DEFAULT 1 COMMENT '是否启用: 0-禁用 1-启用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_owner (owner_id),
    FOREIGN KEY (owner_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享链接表';
