package com.zst.zsearch.biz.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("export_task")
public class ExportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模块:0通用 1关键词监测 2定向监测
     */
    private Integer monitorType;

    /**
     * 下载/展示名
     */
    private String taskName;

    /**
     * 查询条件+导出字段+排序快照JSON
     */
    private String queryParams;

    /**
     * 满足条件总数
     */
    private Integer totalRows;

    /**
     * 已导出行数
     */
    private Integer exportedRows;

    /**
     * 0待处理 1处理中 2成功 3失败
     */
    private Integer status;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 创建用户id
     */
    private Integer createUserId;

    /**
     * 创建用户名
     */
    private String createUserName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}