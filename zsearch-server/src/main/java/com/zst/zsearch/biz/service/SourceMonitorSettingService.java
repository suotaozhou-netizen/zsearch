package com.zst.zsearch.biz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zst.zsearch.auth.security.AuthUtils;
import com.zst.zsearch.auth.security.LoginUser;
import com.zst.zsearch.biz.mapper.SourceMonitorGroupMapper;
import com.zst.zsearch.biz.mapper.SourceMonitorSettingMapper;
import com.zst.zsearch.biz.model.dto.SourceMonitorGroupSaveDto;
import com.zst.zsearch.biz.model.dto.SourceMonitorSettingSaveDto;
import com.zst.zsearch.biz.model.entity.SourceMonitorGroup;
import com.zst.zsearch.biz.model.entity.SourceMonitorSetting;
import com.zst.zsearch.biz.model.support.MonitorSettingValues;
import com.zst.zsearch.biz.model.vo.SourceMonitorGroupTreeVo;
import com.zst.zsearch.biz.model.vo.SourceMonitorGroupVo;
import com.zst.zsearch.biz.model.vo.SourceMonitorSettingVo;
import com.zst.zsearch.common.enums.SiteId;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SourceMonitorSettingService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_DELETED = 1;

    private final SourceMonitorSettingMapper sourceMonitorSettingMapper;
    private final SourceMonitorGroupMapper sourceMonitorGroupMapper;

    public SourceMonitorSettingService(SourceMonitorSettingMapper sourceMonitorSettingMapper,
                                       SourceMonitorGroupMapper sourceMonitorGroupMapper) {
        this.sourceMonitorSettingMapper = sourceMonitorSettingMapper;
        this.sourceMonitorGroupMapper = sourceMonitorGroupMapper;
    }

    @Transactional
    public SourceMonitorGroupVo saveOrUpdateGroup(SourceMonitorGroupSaveDto request) {
        return request.getId() == null ? createGroup(request) : updateGroup(request.getId(), request);
    }

    @Transactional
    public SourceMonitorSettingVo saveOrUpdateSetting(SourceMonitorSettingSaveDto request) {
        return request.getId() == null ? createSetting(request) : updateSetting(request.getId(), request);
    }

    public SourceMonitorGroupVo getGroup(Long id) {
        return toGroupVo(getGroupEntity(id));
    }

    @Transactional
    public void deleteGroup(Long id) {
        SourceMonitorGroup group = getGroupEntity(id);
        Long settingCount = sourceMonitorSettingMapper.selectCount(
                new LambdaQueryWrapper<SourceMonitorSetting>()
                        .eq(SourceMonitorSetting::getGroupId, id)
                        .eq(SourceMonitorSetting::getStatus, STATUS_NORMAL));
        if (settingCount != null && settingCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "分组下存在信息源监测设置，不能删除: " + id);
        }
        group.setStatus(STATUS_DELETED);
        group.setUpdateTime(LocalDateTime.now());
        sourceMonitorGroupMapper.updateById(group);
    }

    public SourceMonitorSettingVo getSetting(Long id) {
        return toSettingVo(getSettingEntity(id));
    }

    public SourceMonitorSetting loadActiveSetting(Long id) {
        return getSettingEntity(id);
    }

    @Transactional
    public void deleteSetting(Long id) {
        SourceMonitorSetting setting = getSettingEntity(id);
        setting.setStatus(STATUS_DELETED);
        setting.setUpdateTime(LocalDateTime.now());
        sourceMonitorSettingMapper.updateById(setting);
    }

    public List<SourceMonitorGroupTreeVo> tree() {
        List<SourceMonitorGroup> groups = sourceMonitorGroupMapper.selectList(
                new LambdaQueryWrapper<SourceMonitorGroup>()
                        .eq(SourceMonitorGroup::getStatus, STATUS_NORMAL)
                        .orderByDesc(SourceMonitorGroup::getUpdateTime)
                        .orderByDesc(SourceMonitorGroup::getId));
        if (groups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = groups.stream().map(SourceMonitorGroup::getId).toList();
        List<SourceMonitorSetting> settings = sourceMonitorSettingMapper.selectList(
                new LambdaQueryWrapper<SourceMonitorSetting>()
                        .eq(SourceMonitorSetting::getStatus, STATUS_NORMAL)
                        .in(SourceMonitorSetting::getGroupId, groupIds)
                        .orderByDesc(SourceMonitorSetting::getUpdateTime)
                        .orderByDesc(SourceMonitorSetting::getId));
        Map<Long, List<SourceMonitorSettingVo>> settingsByGroup = settings.stream()
                .collect(Collectors.groupingBy(SourceMonitorSetting::getGroupId,
                        Collectors.mapping(this::toSettingVo, Collectors.toList())));

        return groups.stream()
                .map(group -> toGroupTreeVo(group,
                        settingsByGroup.getOrDefault(group.getId(), List.of())))
                .toList();
    }

    private SourceMonitorGroupVo createGroup(SourceMonitorGroupSaveDto request) {
        String name = request.getName().trim();
        ensureGroupNameUnique(name, null);

        LocalDateTime now = LocalDateTime.now();
        SourceMonitorGroup group = new SourceMonitorGroup();
        group.setName(name);
        group.setStatus(STATUS_NORMAL);
        group.setCreateTime(now);
        group.setUpdateTime(now);
        sourceMonitorGroupMapper.insert(group);
        return toGroupVo(group);
    }

    private SourceMonitorGroupVo updateGroup(Long id, SourceMonitorGroupSaveDto request) {
        SourceMonitorGroup group = getGroupEntity(id);
        String name = request.getName().trim();
        ensureGroupNameUnique(name, id);

        group.setName(name);
        group.setUpdateTime(LocalDateTime.now());
        sourceMonitorGroupMapper.updateById(group);
        return toGroupVo(group);
    }

    private SourceMonitorSettingVo createSetting(SourceMonitorSettingSaveDto request) {
        getGroupEntity(request.getGroupId());
        String name = request.getName().trim();
        String keywords = trimToNull(request.getKeywords());
        String excludeKeywords = trimToNull(request.getExcludeKeywords());
        validateKeywords(keywords, excludeKeywords);
        String siteIds = validateAndJoinSiteIds(request.getSiteIds());
        String uids = MonitorSettingValues.join(request.getUids());
        ensureSettingNameUnique(request.getGroupId(), name, null);

        LocalDateTime now = LocalDateTime.now();
        SourceMonitorSetting setting = new SourceMonitorSetting();
        setting.setGroupId(request.getGroupId());
        setting.setName(name);
        setting.setKeywords(keywords);
        setting.setExcludeKeywords(excludeKeywords);
        setting.setSiteIds(siteIds);
        setting.setUids(uids);
        setting.setStatus(STATUS_NORMAL);
        setting.setCreateTime(now);
        setting.setUpdateTime(now);
        LoginUser currentUser = AuthUtils.currentUserOrNull();
        if (currentUser != null) {
            setting.setCreateUserId(currentUser.id());
            setting.setCreateUserName(currentUser.username());
        }
        sourceMonitorSettingMapper.insert(setting);
        return toSettingVo(setting);
    }

    private SourceMonitorSettingVo updateSetting(Long id, SourceMonitorSettingSaveDto request) {
        SourceMonitorSetting setting = getSettingEntity(id);
        getGroupEntity(request.getGroupId());
        String name = request.getName().trim();
        String keywords = trimToNull(request.getKeywords());
        String excludeKeywords = trimToNull(request.getExcludeKeywords());
        validateKeywords(keywords, excludeKeywords);
        String siteIds = validateAndJoinSiteIds(request.getSiteIds());
        String uids = MonitorSettingValues.join(request.getUids());
        ensureSettingNameUnique(request.getGroupId(), name, id);

        setting.setGroupId(request.getGroupId());
        setting.setName(name);
        setting.setKeywords(keywords);
        setting.setExcludeKeywords(excludeKeywords);
        setting.setSiteIds(siteIds);
        setting.setUids(uids);
        setting.setUpdateTime(LocalDateTime.now());
        sourceMonitorSettingMapper.updateById(setting);
        return toSettingVo(setting);
    }

    private String validateAndJoinSiteIds(List<Long> siteIds) {
        if (siteIds == null || siteIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "固定监测平台不能为空");
        }
        List<Integer> codes = siteIds.stream()
                .filter(Objects::nonNull)
                .map(id -> SiteId.fromCode(id.intValue()).getCode())
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "固定监测平台不能为空");
        }
        return MonitorSettingValues.join(codes);
    }

    private void validateKeywords(String keywords, String excludeKeywords) {
        if (!StringUtils.hasText(keywords) && StringUtils.hasText(excludeKeywords)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "排除关键词必须与关键词一起使用");
        }
        if (StringUtils.hasText(keywords)) {
            parseKeywords(keywords, "关键词");
        }
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
        Long count = sourceMonitorGroupMapper.selectCount(new LambdaQueryWrapper<SourceMonitorGroup>()
                .eq(SourceMonitorGroup::getStatus, STATUS_NORMAL)
                .eq(SourceMonitorGroup::getName, name)
                .ne(excludeId != null, SourceMonitorGroup::getId, excludeId));
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "信息源监测分组名称已存在: " + name);
        }
    }

    private void ensureSettingNameUnique(Long groupId, String name, Long excludeId) {
        Long count = sourceMonitorSettingMapper.selectCount(new LambdaQueryWrapper<SourceMonitorSetting>()
                .eq(SourceMonitorSetting::getStatus, STATUS_NORMAL)
                .eq(SourceMonitorSetting::getGroupId, groupId)
                .eq(SourceMonitorSetting::getName, name)
                .ne(excludeId != null, SourceMonitorSetting::getId, excludeId));
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "同一分组下已存在同名信息源监测设置: " + name);
        }
    }

    private SourceMonitorGroup getGroupEntity(Long id) {
        SourceMonitorGroup group = sourceMonitorGroupMapper.selectOne(
                new LambdaQueryWrapper<SourceMonitorGroup>()
                        .eq(SourceMonitorGroup::getId, id)
                        .eq(SourceMonitorGroup::getStatus, STATUS_NORMAL));
        if (group == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "信息源监测分组不存在: " + id);
        }
        return group;
    }

    private SourceMonitorSetting getSettingEntity(Long id) {
        SourceMonitorSetting setting = sourceMonitorSettingMapper.selectOne(
                new LambdaQueryWrapper<SourceMonitorSetting>()
                        .eq(SourceMonitorSetting::getId, id)
                        .eq(SourceMonitorSetting::getStatus, STATUS_NORMAL));
        if (setting == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "信息源监测设置不存在: " + id);
        }
        return setting;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private SourceMonitorGroupVo toGroupVo(SourceMonitorGroup group) {
        SourceMonitorGroupVo response = new SourceMonitorGroupVo();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setCreateTime(format(group.getCreateTime()));
        response.setUpdateTime(format(group.getUpdateTime()));
        return response;
    }

    private SourceMonitorGroupTreeVo toGroupTreeVo(SourceMonitorGroup group,
                                                   List<SourceMonitorSettingVo> settings) {
        SourceMonitorGroupTreeVo response = new SourceMonitorGroupTreeVo();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setCreateTime(format(group.getCreateTime()));
        response.setUpdateTime(format(group.getUpdateTime()));
        response.setSettings(settings);
        return response;
    }

    private SourceMonitorSettingVo toSettingVo(SourceMonitorSetting setting) {
        SourceMonitorSettingVo response = new SourceMonitorSettingVo();
        response.setId(setting.getId());
        response.setGroupId(setting.getGroupId());
        response.setName(setting.getName());
        response.setKeywords(setting.getKeywords());
        response.setExcludeKeywords(setting.getExcludeKeywords());
        response.setSiteIds(MonitorSettingValues.splitToLongs(setting.getSiteIds()));
        response.setUids(MonitorSettingValues.split(setting.getUids()));
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