-- =============================================================================
-- 旧结构 → 当前结构 数据迁移脚本（一次性执行，执行前务必备份数据库）
-- 生成日期：2026-11-22
--
-- 【适用场景】
--   数据库里已存在旧版 monitor_group / monitor_setting 表，需要原地升级到当前
--   实体（biz/model/entity）对应的结构。全新数据库请直接用 schema.sql，不要执行本脚本。
--
-- 【旧结构假设】（来自已废弃的 monitor.sql / monitor_source_monitor_migration.sql）
--   monitor_group(id, name, create_time, update_time, UNIQUE KEY uk_monitor_group_name(name))
--   monitor_setting(id, group_id, monitor_type, name, keywords, exclude_keywords,
--                   config_json, config_version, create_time, update_time, version,
--                   UNIQUE KEY uk_monitor_setting_group_type_name(group_id, monitor_type, name))
--   monitor_type: 0=通用查询 1=关键词监测 2=信息源监测
--   config_json: 形如 {"siteIds":[1,4],"uids":["a","b"]}
--
-- 【目标结构】
--   monitor_group          +status                        （当前实体有 status，旧表没有）
--   monitor_setting        +status +create_user_id +create_user_name；
--                          删除 monitor_type/config_json/config_version/version
--   source_monitor_group   / source_monitor_setting：新建，承载旧 monitor_type=2 的数据
--   唯一索引 → 普通索引：(group_id, status, name) / (status, name)
--     原因：逻辑删除后需允许重建同名，见 monitorService.ensure*NameUnique
--
-- 【执行前提】
--   1. MySQL 5.7+（使用 JSON_EXTRACT）；2. 已完整备份；3. 建议在停写窗口执行。
--   4. 本脚本不可重复执行（ALTER TABLE ADD COLUMN 无幂等保护）。
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 第 1 步：为旧表补充当前实体新增的列（均用默认值，存量数据视为“正常”）
-- -----------------------------------------------------------------------------
ALTER TABLE monitor_group
    ADD COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '0:正常 1:删除' AFTER name;

ALTER TABLE monitor_setting
    ADD COLUMN status           TINYINT     NOT NULL DEFAULT 0    COMMENT '0:正常 1:删除' AFTER exclude_keywords,
    ADD COLUMN create_user_id   INT         DEFAULT NULL          COMMENT '创建用户id' AFTER update_time,
    ADD COLUMN create_user_name VARCHAR(50) DEFAULT NULL          COMMENT '创建用户名' AFTER create_user_id;

-- -----------------------------------------------------------------------------
-- 第 2 步：创建当前结构的信息源监测表（与 schema.sql 保持一致）
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

CREATE TABLE IF NOT EXISTS source_monitor_setting (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id         BIGINT        NOT NULL                COMMENT '所属分组 id',
    name             VARCHAR(30)   NOT NULL                COMMENT '方案名',
    keywords         VARCHAR(2000) DEFAULT NULL            COMMENT '关键词表达式',
    exclude_keywords VARCHAR(2000) DEFAULT NULL            COMMENT '排除词表达式',
    status           TINYINT       NOT NULL DEFAULT 0      COMMENT '0:正常 1:删除',
    site_ids         VARCHAR(1000) DEFAULT NULL            COMMENT '固定监测平台，逗号分隔的 siteId 编码',
    uids             VARCHAR(2000) DEFAULT NULL            COMMENT '固定监测平台账号，逗号分隔',
    create_time      DATETIME      DEFAULT NULL            COMMENT '创建时间',
    update_time      DATETIME      DEFAULT NULL            COMMENT '更新时间',
    create_user_id   INT           DEFAULT NULL            COMMENT '创建用户id',
    create_user_name VARCHAR(50)   DEFAULT NULL            COMMENT '创建用户名',
    PRIMARY KEY (id),
    KEY idx_source_monitor_setting_group_status_name (group_id, status, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '信息源监测-设置';

-- -----------------------------------------------------------------------------
-- 第 3 步：迁移分组 —— 把“含信息源监测方案的分组”复制到 source_monitor_group
--   复用原 group 的 id（不同表主键独立，不会冲突）。
-- -----------------------------------------------------------------------------
INSERT INTO source_monitor_group (id, name, status, create_time, update_time)
SELECT DISTINCT g.id, g.name, 0, g.create_time, g.update_time
FROM monitor_group g
         JOIN monitor_setting s ON s.group_id = g.id AND s.monitor_type = 2
ON DUPLICATE KEY UPDATE id = id;

-- -----------------------------------------------------------------------------
-- 第 4 步：迁移方案 —— monitor_type=2 的记录迁入 source_monitor_setting
--   config_json 的 siteIds / uids 数组：JSON_EXTRACT 取数组 → CAST 成字符串 →
--   去掉 [ ] " 及空格 → 得到逗号分隔串（与 MonitorSettingValues.join 的输出格式一致）。
--   当字段缺失或为空数组时结果为 NULL。
-- -----------------------------------------------------------------------------
INSERT INTO source_monitor_setting
(id, group_id, name, keywords, exclude_keywords, status,
 site_ids, uids, create_time, update_time, create_user_id, create_user_name)
SELECT s.id,
       s.group_id,
       s.name,
       s.keywords,
       s.exclude_keywords,
       0,
       NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(
                                      CAST(JSON_EXTRACT(s.config_json, '$.siteIds') AS CHAR),
                                      '[', ''), ']', ''), '"', ''), ' ', ''), ''),
       NULLIF(REPLACE(REPLACE(REPLACE(REPLACE(
                                      CAST(JSON_EXTRACT(s.config_json, '$.uids') AS CHAR),
                                      '[', ''), ']', ''), '"', ''), ' ', ''), ''),
       s.create_time,
       s.update_time,
       NULL,
       NULL
FROM monitor_setting s
WHERE s.monitor_type = 2
ON DUPLICATE KEY UPDATE id = id;

-- -----------------------------------------------------------------------------
-- 第 5 步：删除旧表中已迁出的信息源监测记录
-- -----------------------------------------------------------------------------
DELETE FROM monitor_setting WHERE monitor_type = 2;

-- -----------------------------------------------------------------------------
-- 第 6 步：调整索引 —— 唯一索引改成普通复合索引（逻辑删除后需允许重建同名）
-- -----------------------------------------------------------------------------
ALTER TABLE monitor_group
    DROP INDEX uk_monitor_group_name,
    ADD KEY idx_monitor_group_status_name (status, name);

ALTER TABLE monitor_setting
    DROP INDEX uk_monitor_setting_group_type_name,
    ADD KEY idx_monitor_setting_group_status_name (group_id, status, name);

-- -----------------------------------------------------------------------------
-- 第 7 步：删除当前实体已不存在的列
-- -----------------------------------------------------------------------------
ALTER TABLE monitor_setting
    DROP COLUMN monitor_type,
    DROP COLUMN config_json,
    DROP COLUMN config_version,
    DROP COLUMN version;

-- -----------------------------------------------------------------------------
-- 第 8 步：验证（迁移后应能看到正确的数据分布与结构）
-- -----------------------------------------------------------------------------
SELECT 'monitor_group' AS tbl, COUNT(*) AS cnt FROM monitor_group
UNION ALL SELECT 'monitor_setting', COUNT(*) FROM monitor_setting
UNION ALL SELECT 'source_monitor_group', COUNT(*) FROM source_monitor_group
UNION ALL SELECT 'source_monitor_setting', COUNT(*) FROM source_monitor_setting;

SELECT id, group_id, name, site_ids, uids FROM source_monitor_setting;

SHOW CREATE TABLE monitor_setting;
