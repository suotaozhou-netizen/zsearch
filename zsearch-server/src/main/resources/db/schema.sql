-- =============================================================================
-- zsearch-server 当前最新建表脚本
-- 生成日期：2026-11-22
-- 对齐实体：com.zst.zsearch.biz.model.entity 下的 4 个实体类
--   MonitorGroup          -> monitor_group
--   MonitorSetting        -> monitor_setting
--   SourceMonitorGroup    -> source_monitor_group
--   SourceMonitorSetting  -> source_monitor_setting
--
-- 说明：
-- 1. 本文件是当前结构的权威建表脚本，取代已过时的 monitor.sql 与
--    monitor_source_monitor_migration.sql（后者仍含 monitor_type / config_json /
--    config_version / version 等实体中已不存在的字段）。
-- 2. 所有 create_time / update_time / status / create_user_* 字段均由 Java 代码
--    在 com.zst.zsearch.biz.service.*Service 中显式赋值后写入，数据库默认值仅作兜底。
-- 3. 采用逻辑删除（status=1），因此【不对 name 建物理唯一索引】：
--    业务要求删除后允许重建同名（见 ensureGroupNameUnique / ensureSettingNameUnique，
--    校验条件为 status=0 AND name=? AND id<>excludeId）。若建唯一索引，已删除记录会
--    占用名称导致无法重建。名称唯一性由应用层在该事务内校验；索引仅用于加速该查询。
-- 4. 用户表 sys_user 见同目录 auth.sql，本文件不涉及。
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. 关键词监测 · 分组
--    实体：MonitorGroup
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor_group (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50) NOT NULL                COMMENT '分组名称',
    status      TINYINT     NOT NULL DEFAULT 0      COMMENT '0:正常 1:删除',
    create_time DATETIME    DEFAULT NULL            COMMENT '创建时间',
    update_time DATETIME    DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_monitor_group_status_name (status, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '关键词监测-分组';

-- -----------------------------------------------------------------------------
-- 2. 关键词监测 · 设置（方案）
--    实体：MonitorSetting
--    注意：实体虽 import 了 @Version，但没有任何字段使用该注解，故无 version 列；
--          MybatisPlusConfig 中的乐观锁拦截器当前实际不生效。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor_setting (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id         BIGINT        NOT NULL                COMMENT '所属分组 id',
    name             VARCHAR(30)   NOT NULL                COMMENT '方案名',
    keywords         VARCHAR(2000) DEFAULT NULL            COMMENT '关键词表达式（+ AND / | OR / * NEAR / () 分组）',
    exclude_keywords VARCHAR(2000) DEFAULT NULL            COMMENT '排除词表达式',
    status           TINYINT       NOT NULL DEFAULT 0      COMMENT '0:正常 1:删除',
    create_time      DATETIME      DEFAULT NULL            COMMENT '创建时间',
    update_time      DATETIME      DEFAULT NULL            COMMENT '更新时间',
    create_user_id   INT           DEFAULT NULL            COMMENT '创建用户id（sys_user.id）',
    create_user_name VARCHAR(50)   DEFAULT NULL            COMMENT '创建用户名（sys_user.username）',
    PRIMARY KEY (id),
    KEY idx_monitor_setting_group_status_name (group_id, status, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '关键词监测-设置';

-- -----------------------------------------------------------------------------
-- 3. 信息源监测 · 分组
--    实体：SourceMonitorGroup
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS source_monitor_group (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50) NOT NULL                COMMENT '分组名称',
    status      TINYINT     NOT NULL DEFAULT 0      COMMENT '0:正常 1:删除',
    create_time DATETIME    DEFAULT NULL            COMMENT '创建时间',
    update_time DATETIME    DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_source_monitor_group_status_name (status, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '信息源监测-分组';

-- -----------------------------------------------------------------------------
-- 4. 信息源监测 · 设置（方案）
--    实体：SourceMonitorSetting
--    site_ids / uids 存逗号分隔字符串，由 MonitorSettingValues.join 写入，
--    读出时经 splitToLongs / split 还原为 List。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS source_monitor_setting (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id         BIGINT        NOT NULL                COMMENT '所属分组 id',
    name             VARCHAR(30)   NOT NULL                COMMENT '方案名',
    keywords         VARCHAR(2000) DEFAULT NULL            COMMENT '关键词表达式（可空，仅按信息源过滤时）',
    exclude_keywords VARCHAR(2000) DEFAULT NULL            COMMENT '排除词表达式',
    status           TINYINT       NOT NULL DEFAULT 0      COMMENT '0:正常 1:删除',
    site_ids         VARCHAR(1000) DEFAULT NULL            COMMENT '固定监测平台，逗号分隔的 siteId 编码，如 1,2,3',
    uids             VARCHAR(2000) DEFAULT NULL            COMMENT '固定监测平台账号，逗号分隔',
    create_time      DATETIME      DEFAULT NULL            COMMENT '创建时间',
    update_time      DATETIME      DEFAULT NULL            COMMENT '更新时间',
    create_user_id   INT           DEFAULT NULL            COMMENT '创建用户id（sys_user.id）',
    create_user_name VARCHAR(50)   DEFAULT NULL            COMMENT '创建用户名（sys_user.username）',
    PRIMARY KEY (id),
    KEY idx_source_monitor_setting_group_status_name (group_id, status, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '信息源监测-设置';
