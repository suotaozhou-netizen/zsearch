package com.zst.zsearch.biz.search.export;

import com.zst.zsearch.biz.model.entity.ExportTask;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ExportTaskVo {

    private Long id;
    private Integer monitorType;
    private String taskName;
    private Integer totalRows;
    private Integer exportedRows;
    private Integer status;
    private String statusText;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ExportTaskVo from(ExportTask task) {
        ExportTaskVo vo = new ExportTaskVo();
        vo.setId(task.getId());
        vo.setMonitorType(task.getMonitorType());
        vo.setTaskName(task.getTaskName());
        vo.setTotalRows(task.getTotalRows());
        vo.setExportedRows(task.getExportedRows());
        vo.setStatus(task.getStatus());
        if (task.getStatus() != null) {
            vo.setStatusText(ExportTaskStatus.fromCode(task.getStatus()).getText());
        }
        vo.setErrorMsg(task.getErrorMsg());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }
}