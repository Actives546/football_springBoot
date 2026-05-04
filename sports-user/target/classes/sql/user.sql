-- =====================================================
-- 数据库初始化脚本
-- 数据库：sports_user
-- 描述：用户微服务数据库初始化脚本
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS sports_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sports_user;

-- =====================================================
-- 用户表 t_user
-- =====================================================
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id BIGINT(20) NOT NULL COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    nickname VARCHAR(100) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    gender TINYINT(1) DEFAULT 0 COMMENT '性别（0：未知，1：男，2：女）',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status TINYINT(1) DEFAULT 1 COMMENT '状态（0：禁用，1：正常）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记（0：未删除，1：已删除）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username) COMMENT '用户名唯一索引',
    UNIQUE KEY uk_phone (phone) COMMENT '手机号唯一索引',
    KEY idx_status (status) COMMENT '状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 插入测试数据（密码使用BCrypt加密，默认密码：123456）
-- =====================================================
-- 测试用户1：用户名 admin，密码 123456
INSERT INTO t_user (id, username, password, phone, nickname, avatar, gender, email, status, create_time, update_time, deleted)
VALUES (
    1713500000000000001,
    'admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E',
    '13800138001',
    '管理员',
    NULL,
    1,
    'admin@sports.com',
    1,
    NOW(),
    NOW(),
    0
);

-- 测试用户2：用户名 test，密码 123456
INSERT INTO t_user (id, username, password, phone, nickname, avatar, gender, email, status, create_time, update_time, deleted)
VALUES (
    1713500000000000002,
    'test',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E',
    '13800138002',
    '测试用户',
    NULL,
    0,
    'test@sports.com',
    1,
    NOW(),
    NOW(),
    0
);
