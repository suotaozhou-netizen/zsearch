package com.zst.zsearch.biz.controller;

import com.zst.zsearch.biz.model.dto.MonitorGroupSaveDto;
import com.zst.zsearch.biz.model.dto.MonitorSettingSaveDto;
import com.zst.zsearch.biz.model.vo.MonitorGroupTreeVo;
import com.zst.zsearch.biz.model.vo.MonitorGroupVo;
import com.zst.zsearch.biz.model.vo.MonitorSettingVo;
import com.zst.zsearch.biz.service.MonitorService;
import com.zst.zsearch.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @PostMapping("/groups/saveOrUpdate")
    public MonitorGroupVo saveOrUpdateGroup(@Valid @RequestBody MonitorGroupSaveDto request) {
        return monitorService.saveOrUpdateGroup(request);
    }

    @GetMapping("/groups/{id}")
    public MonitorGroupVo getGroup(@PathVariable Long id) {
        return monitorService.getGroup(id);
    }

    @PostMapping("/groups/delete/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        monitorService.deleteGroup(id);
        return ApiResponse.success();
    }

    @PostMapping("/settings/saveOrUpdate")
    public MonitorSettingVo saveOrUpdateSetting(@Valid @RequestBody MonitorSettingSaveDto request) {
        return monitorService.saveOrUpdateSetting(request);
    }

    @GetMapping("/settings/{id}")
    public MonitorSettingVo getSetting(@PathVariable Long id) {
        return monitorService.getSetting(id);
    }

    @PostMapping("/settings/delete/{id}")
    public ApiResponse<Void> deleteSetting(@PathVariable Long id) {
        monitorService.deleteSetting(id);
        return ApiResponse.success();
    }

    @GetMapping("/tree")
    public List<MonitorGroupTreeVo> tree() {
        return monitorService.tree();
    }
}