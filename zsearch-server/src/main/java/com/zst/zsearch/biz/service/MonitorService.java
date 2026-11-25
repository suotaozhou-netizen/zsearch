package com.zst.zsearch.biz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zst.zsearch.auth.security.AuthUtils;
import com.zst.zsearch.auth.security.LoginUser;
import com.zst.zsearch.biz.mapper.MonitorGroupMapper;
import com.zst.zsearch.biz.mapper.MonitorSettingMapper;
import com.zst.zsearch.biz.model.dto.MonitorGroupSaveDto;
import com.zst.zsearch.biz.model.dto.MonitorSettingSaveDto;
import com.zst.zsearch.biz.model.entity.MonitorGroup;
import com.zst.zsearch.biz.model.entity.MonitorSetting;
import com.zst.zsearch.biz.model.vo.MonitorGroupTreeVo;
import com.zst.zsearch.biz.model.vo.MonitorGroupVo;
import com.zst.zsearch.biz.model.vo.MonitorSettingVo;
import com.zst.zsearch.util.SearchExpressionParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MonitorService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_DELETED = 1;

    private final MonitorGroupMapper monitorGroupMapper;
    private final MonitorSettingMapper monitorSettingMapper;

    public MonitorService(MonitorGroupMapper monitorGroupMapper,
                          MonitorSettingMapper monitorSettingMapper) {
        this.monitorGroupMapper = monitorGroupMapper;
        this.monitorSettingMapper = monitorSettingMapper;
    }

    @Transactional
    public MonitorGroupVo saveOrUpdateGroup(MonitorGroupSaveDto request) {
        return request.getId() == null ? createGroup(request) : updateGroup(request.getId(), request);
    }

    @Transactional
    public MonitorSettingVo saveOrUpdateSetting(MonitorSettingSaveDto request) {
        return request.getId() == null ? createSetting(request) : updateSetting(request.getId(), request);
    }

    public MonitorGroupVo getGroup(Long id) {
        return toGroupVo(getGroupEntity(id));
    }

    @Transactional
    public void deleteGroup(Long id) {
        MonitorGroup group = getGroupEntity(id);
        Long settingCount = monitorSettingMapper.selectCount(new LambdaQueryWrapper<MonitorSetting>()
                .eq(MonitorSetting::getGroupId, id)
                .eq(MonitorSetting::getStatus, STATUS_NORMAL));
        if (settingCount != null && settingCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "分组下存在监测设置，不能删除: " + id);
        }
        group.setStatus(STATUS_DELETED);
        group.setUpdateTime(LocalDateTime.now());
        monitorGroupMapper.updateById(group);
    }

    public MonitorSettingVo getSetting(Long id) {
        return toSettingVo(getSettingEntity(id));
    }

    public MonitorSetting loadActiveSetting(Long id) {
        return getSettingEntity(id);
    }

    @Transactional
    public void deleteSetting(Long id) {
        MonitorSetting setting = getSettingEntity(id);
        setting.setStatus(STATUS_DELETED);
        setting.setUpdateTime(LocalDateTime.now());
        monitorSettingMapper.updateById(setting);
    }

    public List<MonitorGroupTreeVo> tree() {
        List<MonitorGroup> groups = monitorGroupMapper.selectList(new LambdaQueryWrapper<MonitorGroup>()
                .eq(MonitorGroup::getStatus, STATUS_NORMAL)
                .orderByDesc(MonitorGroup::getUpdateTime)
                .orderByDesc(MonitorGroup::getId));
        if (groups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = groups.stream().map(MonitorGroup::getId).toList();
        List<MonitorSetting> settings = monitorSettingMapper.selectList(
                new LambdaQueryWrapper<MonitorSetting>()
                        .eq(MonitorSetting::getStatus, STATUS_NORMAL)
                        .in(MonitorSetting::getGroupId, groupIds)
                        .orderByDesc(MonitorSetting::getUpdateTime)
                        .orderByDesc(MonitorSetting::getId));
        Map<Long, List<MonitorSettingVo>> settingsByGroup = settings.stream()
                .collect(Collectors.groupingBy(MonitorSetting::getGroupId,
                        Collectors.mapping(this::toSettingVo, Collectors.toList())));

        return groups.stream()
                .map(group -> toGroupTreeVo(group,
                        settingsByGroup.getOrDefault(group.getId(), List.of())))
                .toList();
    }

    private MonitorGroupVo createGroup(MonitorGroupSaveDto request) {
        String name = request.getName().trim();
        ensureGroupNameUnique(name, null);

        LocalDateTime now = LocalDateTime.now();
        MonitorGroup group = new MonitorGroup();
        group.setName(name);
        group.setStatus(STATUS_NORMAL);
        group.setCreateTime(now);
        group.setUpdateTime(now);
        monitorGroupMapper.insert(group);
        return toGroupVo(group);
    }

    private MonitorGroupVo updateGroup(Long id, MonitorGroupSaveDto request) {
        MonitorGroup group = getGroupEntity(id);
        String name = request.getName().trim();
        ensureGroupNameUnique(name, id);

        group.setName(name);
        group.setUpdateTime(LocalDateTime.now());
        monitorGroupMapper.updateById(group);
        return toGroupVo(group);
    }

    private MonitorSettingVo createSetting(MonitorSettingSaveDto request) {
        getGroupEntity(request.getGroupId());
        String name = request.getName().trim();
        String keywords = request.getKeywords().trim();
        String excludeKeywords = trimToNull(request.getExcludeKeywords());
        validateKeywords(keywords, excludeKeywords);
        ensureSettingNameUnique(request.getGroupId(), name, null);

        LocalDateTime now = LocalDateTime.now();
        MonitorSetting setting = new MonitorSetting();
        setting.setGroupId(request.getGroupId());
        setting.setName(name);
        setting.setKeywords(keywords);
        setting.setExcludeKeywords(excludeKeywords);
        setting.setStatus(STATUS_NORMAL);
        setting.setCreateTime(now);
        setting.setUpdateTime(now);
        LoginUser currentUser = AuthUtils.currentUserOrNull();
        if (currentUser != null) {
            setting.setCreateUserId(currentUser.id());
            setting.setCreateUserName(currentUser.username());
        }
        monitorSettingMapper.insert(setting);
        return toSettingVo(setting);
    }

    private MonitorSettingVo updateSetting(Long id, MonitorSettingSaveDto request) {
        MonitorSetting setting = getSettingEntity(id);
        getGroupEntity(request.getGroupId());
        String name = request.getName().trim();
        String keywords = request.getKeywords().trim();
        String excludeKeywords = trimToNull(request.getExcludeKeywords());
        validateKeywords(keywords, excludeKeywords);
        ensureSettingNameUnique(request.getGroupId(), name, id);

        setting.setGroupId(request.getGroupId());
        setting.setName(name);
        setting.setKeywords(keywords);
        setting.setExcludeKeywords(excludeKeywords);
        setting.setUpdateTime(LocalDateTime.now());
        monitorSettingMapper.updateById(setting);
        return toSettingVo(setting);
    }

    private void validateKeywords(String keywords, String excludeKeywords) {
        if (!StringUtils.hasText(keywords) && StringUtils.hasText(excludeKeywords)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "排除关键词必须与关键词一起使用");
        }
        parseKeywords(keywords, "关键词");
        if (StringUtils.hasText(excludeKeywords)) {
            parseKeywords(excludeKeywords, "排除关键词");
        }
    }

    private void parseKeywords(String keywords, String label) {
        try {
            SearchExpressionParser.parse(keywords);
        } catch (SearchExpressionParser.ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + e.getMessage(), e);
        }
    }

    private void ensureGroupNameUnique(String name, Long excludeId) {
        Long count = monitorGroupMapper.selectCount(new LambdaQueryWrapper<MonitorGroup>()
                .eq(MonitorGroup::getStatus, STATUS_NORMAL)
                .eq(MonitorGroup::getName, name)
                .ne(excludeId != null, MonitorGroup::getId, excludeId));
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "监测分组名称已存在: " + name);
        }
    }

    private void ensureSettingNameUnique(Long groupId, String name, Long excludeId) {
        Long count = monitorSettingMapper.selectCount(new LambdaQueryWrapper<MonitorSetting>()
                .eq(MonitorSetting::getStatus, STATUS_NORMAL)
                .eq(MonitorSetting::getGroupId, groupId)
                .eq(MonitorSetting::getName, name)
                .ne(excludeId != null, MonitorSetting::getId, excludeId));
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "同一分组下已存在同名监测设置: " + name);
        }
    }

    private MonitorGroup getGroupEntity(Long id) {
        MonitorGroup group = monitorGroupMapper.selectOne(new LambdaQueryWrapper<MonitorGroup>()
                .eq(MonitorGroup::getId, id)
                .eq(MonitorGroup::getStatus, STATUS_NORMAL));
        if (group == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "监测分组不存在: " + id);
        }
        return group;
    }

    private MonitorSetting getSettingEntity(Long id) {
        MonitorSetting setting = monitorSettingMapper.selectOne(new LambdaQueryWrapper<MonitorSetting>()
                .eq(MonitorSetting::getId, id)
                .eq(MonitorSetting::getStatus, STATUS_NORMAL));
        if (setting == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "监测设置不存在: " + id);
        }
        return setting;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private MonitorGroupVo toGroupVo(MonitorGroup group) {
        MonitorGroupVo response = new MonitorGroupVo();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setCreateTime(format(group.getCreateTime()));
        response.setUpdateTime(format(group.getUpdateTime()));
        return response;
    }

    private MonitorGroupTreeVo toGroupTreeVo(MonitorGroup group, List<MonitorSettingVo> settings) {
        MonitorGroupTreeVo response = new MonitorGroupTreeVo();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setCreateTime(format(group.getCreateTime()));
        response.setUpdateTime(format(group.getUpdateTime()));
        response.setSettings(settings);
        return response;
    }

    private MonitorSettingVo toSettingVo(MonitorSetting setting) {
        MonitorSettingVo response = new MonitorSettingVo();
        response.setId(setting.getId());
        response.setGroupId(setting.getGroupId());
        response.setName(setting.getName());
        response.setKeywords(setting.getKeywords());
        response.setExcludeKeywords(setting.getExcludeKeywords());
        response.setStatus(setting.getStatus());
        response.setCreateUserId(setting.getCreateUserId());
        response.setCreateUserName(setting.getCreateUserName());
        response.setCreateTime(format(setting.getCreateTime()));
        response.setUpdateTime(format(setting.getUpdateTime()));
        return response;
    }

    private String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME);
    }
}