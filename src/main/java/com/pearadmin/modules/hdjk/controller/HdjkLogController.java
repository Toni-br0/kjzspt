package com.pearadmin.modules.hdjk.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.hdjk.domain.HdjkLog;
import com.pearadmin.modules.hdjk.service.impl.RbhdjkLogServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import javax.annotation.Resource;

/**
 * 创建日期：2025-05-26
 * 活动监日志
 **/

@RestController
@Api(tags = {"活动监日志管理控制器"})
@Slf4j
@RequestMapping(ControllerConstant.API_RBHDJK_PREFIX + "log")
public class HdjkLogController extends BaseController {

    @Resource
    private RbhdjkLogServiceImpl rbhdjkLogService;

    /**
     * 基础路径
     */
    private final String modulePath = "hdjk/log/";

    /**
     * 打开活动监控日志页面
     * @return
     */
    @GetMapping("toLog")
    @ApiOperation(value = "打开活动监控日志页面",notes = "打开活动监控日志页面")
    @PreAuthorize("hasPermission('/rbhdjk/log/toLog','rbhdjk:log:toLog')")
    public ModelAndView toLog() {
        return jumpPage(modulePath + "log");
    }

    /**
     * 获取活动监控日志列表数据
     * @param rbhdjkLog
     * @param pageDomain
     * @return
     */
    @GetMapping("logDataList")
    @ApiOperation(value = "获取活动监控日志列表数据",notes = "获取活动监控日志列表数据")
    public ResultTable logDataList(HdjkLog rbhdjkLog, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<HdjkLog> pageInfo = new PageInfo<>(rbhdjkLogService.logDataList(rbhdjkLog));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }


}
