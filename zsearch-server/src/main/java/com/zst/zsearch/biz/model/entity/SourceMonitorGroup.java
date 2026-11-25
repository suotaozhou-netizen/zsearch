package com.zst.zsearch.biz.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("source_monitor_group")
public class SourceMonitorGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /**
     * 0:正常  1:删除
     */
    private Integer status;


    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
