-- NewWorld 数据库初始化脚本
-- 个人工作计划管理工具

CREATE DATABASE IF NOT EXISTS newworld DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE newworld;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    nickname    VARCHAR(50) COMMENT '昵称',
    avatar      VARCHAR(500) COMMENT '头像',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 项目分组表
CREATE TABLE IF NOT EXISTS project_group (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分组ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    name        VARCHAR(100) NOT NULL COMMENT '分组名称',
    sort_order  INT DEFAULT 0 COMMENT '排序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目分组表';

-- 项目表
CREATE TABLE IF NOT EXISTS project (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    group_id    BIGINT NOT NULL COMMENT '分组ID',
    name        VARCHAR(100) NOT NULL COMMENT '项目名称',
    color       VARCHAR(20) DEFAULT '#409EFF' COMMENT '项目颜色',
    description VARCHAR(500) COMMENT '项目描述',
    sort_order  INT DEFAULT 0 COMMENT '排序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES project_group(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 任务表（核心表）
CREATE TABLE IF NOT EXISTS task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    project_id      BIGINT COMMENT '项目ID',
    parent_task_id  BIGINT COMMENT '关联主任务ID',
    title           VARCHAR(200) NOT NULL COMMENT '任务标题',
    description     TEXT COMMENT '任务描述（多文本内容）',
    priority        VARCHAR(20) DEFAULT 'NONE' COMMENT '优先级: RED/YELLOW/BLUE/FLAG/NONE',
    status          VARCHAR(20) DEFAULT 'INCOMPLETE' COMMENT '状态: INCOMPLETE/DONE/SHELVED',
    tag             VARCHAR(100) COMMENT '标签',
    start_date      DATE COMMENT '开始日期',
    due_date        DATE COMMENT '截止日期',
    is_note         TINYINT(1) DEFAULT 0 COMMENT '是否为笔记',
    sort_order      INT DEFAULT 0 COMMENT '排序号',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_task_id) REFERENCES task(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    name        VARCHAR(50) NOT NULL COMMENT '标签名称',
    color       VARCHAR(20) DEFAULT '#409EFF' COMMENT '标签颜色',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 任务标签关联表
CREATE TABLE IF NOT EXISTS task_tag (
    task_id BIGINT NOT NULL COMMENT '任务ID',
    tag_id  BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (task_id, tag_id),
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务标签关联表';

-- 索引
CREATE INDEX idx_task_user_date ON task(user_id, start_date, due_date);
CREATE INDEX idx_task_project ON task(project_id);
CREATE INDEX idx_task_status ON task(status);
CREATE INDEX idx_task_priority ON task(priority);

-- 插入默认管理员用户（密码: admin123）
INSERT INTO sys_user (username, password, nickname) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '管理员');
