package com.pearadmin.modules.report.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.common.web.domain.response.module.ResultTree;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportCondit;
import com.pearadmin.modules.report.domain.ReportIndex;
import com.pearadmin.modules.report.service.impl.RepClassManageServiceImpl;
import com.pearadmin.modules.report.service.impl.RepConditManageServiceImpl;
import com.pearadmin.modules.sys.domain.SysDept;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;

/**
 * 创建日期：2025-07-02
 * 条件管理
 **/

@RestController
@Api(tags = {"自助报表条件管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "conditManage")
public class RepConditManageController extends BaseController {

    //基础路径
    private final String modelPath = "autoData/conditManage/";

    @Resource
    private RepConditManageServiceImpl repConditManageService;

    @Resource
    private RepClassManageServiceImpl repClassManageService;


    /**
     * 打开自助取数条件管理页面
     * @return
     */
    @GetMapping("toConditManage")
    @ApiOperation(value = "打开自助取数条件管理页面",notes = "打开自助取数条件管理页面")
    @PreAuthorize("hasPermission('/autoData/conditManage/toConditManage','autoData:conditManage:toConditManage')")
    public ModelAndView toConditManage(Model model) {

        return jumpPage(modelPath + "conditManage");
    }

    /**
     * 获取条件管理列表数据
     * @param reportCondit
     * @param pageDomain
     * @return
     */
    @GetMapping("getConditList")
    @ApiOperation(value = "获取条件管理列表数据",notes = "获取条件管理列表数据")
    public ResultTable getConditList(ReportCondit reportCondit, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ReportCondit> pageInfo = new PageInfo<>(repConditManageService.getConditList(reportCondit));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 打开条件新增页面
     * @return
     */
    @GetMapping("toAddCondit")
    @ApiOperation(value = "打开条件新增页面",notes = "打开条件新增页面")
    public ModelAndView toAddCondit(Model model) {

        return jumpPage(modelPath + "addCondit");
    }

    /**
     * 保存条件数据
     * @param reportCondit
     * @return
     */
    @Repeat
    @PostMapping("saveConditInfo")
    @ApiOperation(value = "保存条件数据",notes = "保存条件数据")
    @Log(title = "保存条件数据", describe = "保存条件数据", type = BusinessType.ADD)
    public JSONObject saveConditInfo(@RequestBody ReportCondit reportCondit){
        return repConditManageService.saveConditInfo(reportCondit);
    }

    /**
     * 打开条件管理修改页面
     */
    @GetMapping("toEditCondit")
    @ApiOperation(value = "打开条件管理修改页面",notes = "打开条件管理修改页面")
    public ModelAndView toEditCondit(Model model,int conditId) {

        ReportCondit reportCondit = repConditManageService.getConditById(conditId);
        model.addAttribute("reportCondit", reportCondit);

        return jumpPage(modelPath + "editCondit");
    }

    /**
     * 单个删除条件数据
     * Param: id
     * Return: 文件
     */
    @DeleteMapping("remove/{conditId}")
    @ApiOperation(value = "单个删除条件数据",notes = "单个删除条件数据")
    @Log(title = "单个删除条件数据", describe = "单个删除条件数据", type = BusinessType.REMOVE)
    public JSONObject remove(@PathVariable("conditId") int conditId) {
        return repConditManageService.remove(conditId);
    }

    /**
     * 批量删除条件数据
     * @param conditIds
     * @return
     */
    @DeleteMapping("batchRemove/{conditIds}")
    @ApiOperation(value = "批量删除条件数据",notes = "批量删除条件数据")
    @Log(title = "批量删除条件数据", describe = "批量删除条件数据", type = BusinessType.REMOVE)
    public JSONObject batchRemove(@PathVariable("conditIds") String conditIds) {
        return repConditManageService.batchRemove(conditIds);
    }




}
