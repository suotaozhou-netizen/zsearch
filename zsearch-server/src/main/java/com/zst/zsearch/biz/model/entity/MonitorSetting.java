package com.zst.zsearch.biz.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("monitor_setting")
public class MonitorSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分组id
     */
    private Long groupId;

    /**
     * 方案名
     */
    private String name;

    /**
     * 关键词表达式
     */
    private String keywords;

    /**
     * 排除词表达式
     */
    private String excludeKeywords;

    /**
     * 0:正常  1:删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建用户id
     */
    private Integer createUserId;

    /**
     * 创建用户名
     */
    private String createUserName;
}
