package com.pearadmin.modules.im.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.im.domain.ImFilePushManage;
import com.pearadmin.modules.im.domain.ImOperateLog;
import com.pearadmin.modules.im.service.impl.ImOperateLogServiceImpl;
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
 * 创建日期：2025-04-30
 * IM操作日志
 **/

@RestController
@Api(tags = {"IM操作日志"})
@Slf4j
@RequestMapping(ControllerConstant.API_IM_PREFIX + "operateLog")
public class ImOperateLogController extends BaseController {

    @Resource
    private ImOperateLogServiceImpl imOperateLogServiceImpl;

    /**
     * 基础路径
     */
    private final String modulePath = "im/operateLog/";

    /**
     * 打开IM操作日志
     * @return
     */
    @GetMapping("toOperateLog")
    @ApiOperation(value = "打开IM操作日志",notes = "打开IM操作日志")
    @PreAuthorize("hasPermission('/im/operateLog/toOperateLog','im:operateLog:toOperateLog')")
    public ModelAndView toOperateLog(Model model) {
        return jumpPage(modulePath + "main");
    }

    /**
     * 获取IM操作日志列表数据
     * @param imOperateLog
     * @param pageDomain
     * @return
     */
    @GetMapping("operateLogList")
    @ApiOperation(value = "获取IM操作日志列表数据",notes = "获取IM操作日志列表数据")
    public ResultTable operateLogList(ImOperateLog imOperateLog, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ImOperateLog> pageInfo = new PageInfo<>(imOperateLogServiceImpl.operateLogList(imOperateLog));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

}
