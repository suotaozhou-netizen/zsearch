-- =============================================================================
-- 文章离线导出任务表
-- 生成日期：2026-11-23
-- 对齐实体：com.zst.zsearch.biz.model.entity.ExportTask
--
-- 说明：
-- 1. 本表仅用于「导出全部数据」的离线导出；实时导出（选中/当前页/前1000条）不落任务表。
-- 2. monitor_type 与 MonitorType 枚举一致：0 通用查询，1 关键词监测，2 定向监测。
-- 3. query_params 保存创建任务时的查询条件 + 导出字段 + 排序快照（JSON），
--    任务执行时按快照重新构建 ES 查询，保证结果可复现。
-- 4. 导出文件按任务 id 命名，固定存放在 C:/Users/zst12/Desktop/zSearch/files 下，不落库路径。
-- 5. 本文件需手动在目标库执行；执行前请确认已选中正确 schema。
-- =============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS export_task (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    monitor_type     TINYINT      NOT NULL                COMMENT '模块:0通用 1关键词监测 2定向监测',
    task_name        VARCHAR(200) DEFAULT NULL            COMMENT '下载/展示名',
    query_params     MEDIUMTEXT   NOT NULL                COMMENT '查询条件+导出字段+排序快照JSON',
    total_rows       INT          DEFAULT NULL            COMMENT '满足条件总数',
    exported_rows    INT          NOT NULL DEFAULT 0      COMMENT '已导出行数',
    status           TINYINT      NOT NULL DEFAULT 0      COMMENT '0待处理 1处理中 2成功 3失败',
    error_msg        VARCHAR(1000) DEFAULT NULL           COMMENT '失败原因',
    create_user_id   INT          NOT NULL                COMMENT '创建用户id',
    create_user_name VARCHAR(50)  DEFAULT NULL            COMMENT '创建用户名',
    create_time      DATETIME     DEFAULT NULL            COMMENT '创建时间',
    update_time      DATETIME     DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_export_task_user_time (create_user_id, create_time),
    KEY idx_export_task_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '离线导出任务';