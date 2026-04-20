package com.pearadmin.modules.report.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportAutoCreateLog;
import com.pearadmin.modules.report.service.impl.RepMyTaskLogServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * 创建日期：2025-08-06
 * 自助取数任务日志管理控制器
 **/

@RestController
@Api(tags = {"自助取数我的任务管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "myTaskLog")
public class RepMyTaskLogManageController extends BaseController {
    //基础路径
    private final String modelPath = "autoData/myTaskLog/";

    @Resource
    private RepMyTaskLogServiceImpl repMyTaskLogService;

    /**
     * 打开自助取数我的任务日志页面
     * @return
     */
    @GetMapping("toMyTaskLog")
    @ApiOperation(value = "打开自助取数我的任务日志页面",notes = "打开自助取数我的任务日志页面")
    @PreAuthorize("hasPermission('/autoData/myTaskLog/toMyTaskLog','autoData:myTaskLog:toMyTaskLog')")
    public ModelAndView toMyTaskLog(Model model) {
        return jumpPage(modelPath + "myTaskLogManage");
    }


    /**
     * 获取自助取数我的任务日志列表数据
     * @param reportAutoCreateLog
     * @param pageDomain
     * @return
     */
    @GetMapping("getMyTaskLogList")
    @ApiOperation(value = "获取自助取数我的任务日志列表数据",notes = "获取自助取数我的任务日志列表数据")
    public ResultTable getMyTaskLogList(ReportAutoCreateLog reportAutoCreateLog, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ReportAutoCreateLog> pageInfo = new PageInfo<>(repMyTaskLogService.getMyTaskLogList(reportAutoCreateLog));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }


}
