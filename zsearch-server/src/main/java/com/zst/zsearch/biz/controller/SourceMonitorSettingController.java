package com.zst.zsearch.biz.controller;

import com.zst.zsearch.biz.model.dto.SourceMonitorGroupSaveDto;
import com.zst.zsearch.biz.model.dto.SourceMonitorSettingSaveDto;
import com.zst.zsearch.biz.model.vo.SourceMonitorGroupTreeVo;
import com.zst.zsearch.biz.model.vo.SourceMonitorGroupVo;
import com.zst.zsearch.biz.model.vo.SourceMonitorSettingVo;
import com.zst.zsearch.biz.service.SourceMonitorSettingService;
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
@RequestMapping("/api/source-monitor")
public class SourceMonitorSettingController {

    private final SourceMonitorSettingService sourceMonitorSettingService;

    public SourceMonitorSettingController(SourceMonitorSettingService sourceMonitorSettingService) {
        this.sourceMonitorSettingService = sourceMonitorSettingService;
    }

    @PostMapping("/groups/saveOrUpdate")
    public SourceMonitorGroupVo saveOrUpdateGroup(@Valid @RequestBody SourceMonitorGroupSaveDto request) {
        return sourceMonitorSettingService.saveOrUpdateGroup(request);
    }

    @GetMapping("/groups/{id}")
    public SourceMonitorGroupVo getGroup(@PathVariable Long id) {
        return sourceMonitorSettingService.getGroup(id);
    }

    @PostMapping("/groups/delete/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        sourceMonitorSettingService.deleteGroup(id);
        return ApiResponse.success();
    }

    @PostMapping("/settings/saveOrUpdate")
    public SourceMonitorSettingVo saveOrUpdateSetting(@Valid @RequestBody SourceMonitorSettingSaveDto request) {
        return sourceMonitorSettingService.saveOrUpdateSetting(request);
    }

    @GetMapping("/settings/{id}")
    public SourceMonitorSettingVo getSetting(@PathVariable Long id) {
        return sourceMonitorSettingService.getSetting(id);
    }

    @PostMapping("/settings/delete/{id}")
    public ApiResponse<Void> deleteSetting(@PathVariable Long id) {
        sourceMonitorSettingService.deleteSetting(id);
        return ApiResponse.success();
    }

    @GetMapping("/tree")
    public List<SourceMonitorGroupTreeVo> tree() {
        return sourceMonitorSettingService.tree();
    }
}