package com.zst.zsearch.controller;

import com.zst.zsearch.biz.controller.MonitorController;
import com.zst.zsearch.biz.mapper.MonitorGroupMapper;
import com.zst.zsearch.biz.mapper.MonitorSettingMapper;
import com.zst.zsearch.biz.model.vo.MonitorGroupVo;
import com.zst.zsearch.search.service.SearchApplicationService;
import com.zst.zsearch.biz.service.MonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitorController.class)
class ApiResponseAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitorService monitorService;

    @MockitoBean
    private SearchApplicationService searchApplicationService;

    @MockitoBean
    private MonitorGroupMapper monitorGroupMapper;

    @MockitoBean
    private MonitorSettingMapper monitorSettingMapper;

    @Test
    void wrapsSuccessResponse() throws Exception {
        MonitorGroupVo group = new MonitorGroupVo();
        group.setId(1L);
        group.setName("东莞企业");
        when(monitorService.getGroup(1L)).thenReturn(group);

        mockMvc.perform(get("/api/monitor/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("东莞企业"));
    }

    @Test
    void wrapsErrorResponse() throws Exception {
        when(monitorService.getGroup(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "监测分组不存在: 99"));

        mockMvc.perform(get("/api/monitor/groups/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("监测分组不存在: 99"));
    }

    @Test
    void wrapsMethodNotAllowedResponse() throws Exception {
        mockMvc.perform(post("/api/monitor/groups/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.message").value("请求方法不支持"));
    }
}
