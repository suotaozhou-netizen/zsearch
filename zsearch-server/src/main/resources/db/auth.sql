SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id          INT          NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    nickname    VARCHAR(50)  DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0:正常 1:停用',
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- 默认管理员：admin / admin123，首次登录后请尽快修改密码
INSERT INTO sys_user (username, password, nickname, status, create_time, update_time)
VALUES ('admin', '$2b$10$yk.7Qxil3rrart7Z33UG8uHtz3x5E6K8llsZWEeKndEYq5kaZLYXe', '管理员', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE id = id;