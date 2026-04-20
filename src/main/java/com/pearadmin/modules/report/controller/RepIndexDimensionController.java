package com.pearadmin.modules.report.controller;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.modules.hdjk.domain.HdjkModelManage;
import com.pearadmin.modules.report.domain.ReportDimension;
import com.pearadmin.modules.report.domain.ReportIndexStateInfo;
import com.pearadmin.modules.report.service.RepIndexDimensionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * 创建日期：2025-11-07
 *  自助取树维度管理
 **/

@RestController
@Api(tags = {"维度管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "dimension")
public class RepIndexDimensionController  extends BaseController {

    //基础路径
    private final String modelPath = "autoData/dimension/";

    @Resource
    private RepIndexDimensionService repIndexDimensionService;


    /**
     * 打开自助取数维度管理页面
     * @return
     */
    @GetMapping("toDimension")
    @ApiOperation(value = "打开自助取数指标管理页面",notes = "打开自助取数指标管理页面")
    @PreAuthorize("hasPermission('/autoData/dimension/toDimension','autoData:dimension:toDimension')")
    public ModelAndView toDimension(Model model) {
        return jumpPage(modelPath + "dimension");
    }

    /**
     * 获取维度树数据
     * @return
     */

    @RequestMapping(value = "/dimensionTreeload")
    @ResponseBody
    public Object dimensionTreeload(@RequestParam("isNonStand") String isNonStand){
        return repIndexDimensionService.dimensionTreeload(isNonStand);
    }

    /**
     * 打开自助取数维度新增页面
     */
    @GetMapping("toAdd")
    @ApiOperation(value = "打开自助取数维度新增页面",notes = "打开自助取数维度新增页面")
    public ModelAndView toAdd(Model model, String dimensionId,String dimensionName,String isNonStand) {

        model.addAttribute("dimensionId", dimensionId);
        model.addAttribute("dimensionName", dimensionName);
        model.addAttribute("isNonStand", isNonStand);
        return jumpPage(modelPath + "addDimension");
    }

    /**
     * 打开自助取数维度修改页面
     */
    @GetMapping("toEdit")
    @ApiOperation(value = "打开自助取数维度修改页面",notes = "打开自助取数维度修改页面")
    public ModelAndView toEdit(Model model, String dimensionId) {
        ReportDimension reportDimension = repIndexDimensionService.getById(dimensionId);
        if(reportDimension ==null){
            reportDimension = new ReportDimension();
        }

        ReportDimension parentReportDimension = repIndexDimensionService.getParentById(dimensionId);
        if(parentReportDimension ==null){
            parentReportDimension = new ReportDimension();
        }

        model.addAttribute("reportDimension", reportDimension);
        model.addAttribute("parentReportDimension", parentReportDimension);
        return jumpPage(modelPath + "editDimension");
    }

    /**
     * 获取自助取数维度信息
     */
    @PostMapping("getDimensionInfo")
    @ApiOperation(value = "获取自助取数维度信息",notes = "获取自助取数维度信息")
    public JSONObject getDimensionInfo(@RequestBody String dimensionId) {
        JSONObject retJsonObject = new JSONObject();

        ReportDimension reportDimension = repIndexDimensionService.getById(dimensionId);
        if(reportDimension ==null){
            reportDimension = new ReportDimension();
        }

        String areaName ="";

        if(StringUtil.isNotEmpty(reportDimension.getField())){
            if(reportDimension.getField().equals("hx_latn_name")){
                areaName = "地市";
            }else if(reportDimension.getField().equals("hx_area_name")){
                areaName = "区县";
            }else if(reportDimension.getField().equals("x_hx5_bp_name")){
                areaName = "区格";
            }
        }

        ReportDimension parentReportDimension = repIndexDimensionService.getParentById(dimensionId);
        if(parentReportDimension ==null){
            parentReportDimension = new ReportDimension();
        }

        retJsonObject.put("reportDimension",reportDimension);
        retJsonObject.put("parentReportDimension",parentReportDimension);
        retJsonObject.put("areaName",areaName);

        return retJsonObject;
    }


    /**
     * 保存自助取数维度管理数据
     * @param reportDimension
     * @return
     */
    @Repeat
    @PostMapping("saveDimensionInfo")
    @ApiOperation(value = "保存自助取数维度管理数据",notes = "保存自助取数维度管理数据")
    @Log(title = "保存自助取数维度管理数据", describe = "保存自助取数维度管理数据", type = BusinessType.ADD)
    public JSONObject saveDimensionInfo(@RequestBody ReportDimension reportDimension){
        return repIndexDimensionService.saveDimensionInfo(reportDimension);
    }

    /**
     * 保存修改自助取数维度管理数据
     * @param reportDimension
     * @return
     */
    @Repeat
    @PostMapping("updateDimensionInfo")
    @ApiOperation(value = "保存修改自助取数维度管理数据",notes = "保存修改自助取数维度管理数据")
    @Log(title = "保存修改自助取数维度管理数据", describe = "保存修改自助取数维度管理数据", type = BusinessType.EDIT)
    public JSONObject updateDimensionInfo(@RequestBody ReportDimension reportDimension){
        return repIndexDimensionService.updateDimensionInfo(reportDimension);
    }

    /**
     * 删除自助取数维度数据
     * Param: id
     * Return: 文件
     */
    @DeleteMapping("remove/{dimensionId}")
    @ApiOperation(value = "删除自助取数维度数据",notes = "删除自助取数维度数据")
    @Log(title = "删除自助取数维度数据", describe = "删除自助取数维度数据", type = BusinessType.REMOVE)
    public JSONObject remove(@PathVariable("dimensionId") String  dimensionId) {
        return repIndexDimensionService.remove(dimensionId);
    }


}
