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
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.service.impl.RepClassManageServiceImpl;
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
 * 创建日期：2025-07-01
 * 分类管理
 **/

@RestController
@Api(tags = {"分类管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "classManage")
public class RepClassManageController extends BaseController {

  //基础路径
  private final String modelPath = "autoData/classManage/";

  @Resource
  private RepClassManageServiceImpl repClassManageService;

  /**
   * 打开自助取数分类管理页面
   * @return
   */
  @GetMapping("toRepClassManage")
  @ApiOperation(value = "打开自助取数分类管理页面",notes = "打开自助取数分类管理页面")
  @PreAuthorize("hasPermission('/autoData/classManage/toRepClassManage','autoData:classManage:toRepClassManage')")
  public ModelAndView toRepClassManage(Model model) {
    return jumpPage(modelPath + "repClassManage");
  }

  /**
   * 获取分类管理列表数据
   * @param reportClassify
   * @param pageDomain
   * @return
   */
  @GetMapping("getClassList")
  @ApiOperation(value = "获取分类管理列表数据",notes = "获取分类管理列表数据")
  public ResultTable getClassList(ReportClassify reportClassify, PageDomain pageDomain) {
    PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
    PageInfo<ReportClassify> pageInfo = new PageInfo<>(repClassManageService.getClassList(reportClassify));
    return pageTable(pageInfo.getList(), pageInfo.getTotal());
  }

  /**
   * 打开分类新增页面
   * @return
   */
  @GetMapping("toAddClass")
  @ApiOperation(value = "打开分类新增页面",notes = "打开分类新增页面")
  public ModelAndView toAddClass(Model model) {
    return jumpPage(modelPath + "addClass");
  }

  /**
   * 保存分类数据
   * @param reportClassify
   * @return
   */
  @Repeat
  @PostMapping("saveClassInfo")
  @ApiOperation(value = "保存分类数据",notes = "保存分类数据")
  @Log(title = "保存分类数据", describe = "保存分类数据", type = BusinessType.ADD)
  public JSONObject saveClassInfo(@RequestBody ReportClassify reportClassify){
    return repClassManageService.saveClassInfo(reportClassify);
  }


  /**
   * 打开分类管理修改页面
   */
  @GetMapping("toEditClass")
  @ApiOperation(value = "打开分类管理修改页面",notes = "打开分类管理修改页面")
  public ModelAndView toEditClass(Model model,String classifyId) {

    ReportClassify reportClassify = repClassManageService.getClassById(classifyId);
    model.addAttribute("reportClassify", reportClassify);

    return jumpPage(modelPath + "editClass");
  }

  /**
   * 单个删除分类数据
   * Param: id
   * Return: 文件
   */
  @DeleteMapping("remove/{classifyId}")
  @ApiOperation(value = "单个删除分类数据",notes = "单个删除分类数据")
  @Log(title = "单个删除分类数据", describe = "单个删除分类数据", type = BusinessType.REMOVE)
  public JSONObject remove(@PathVariable("classifyId") int classifyId) {
    return repClassManageService.remove(classifyId);
  }

  /**
   * 批量删除分类数据
   * @param classifyIds
   * @return
   */
  @DeleteMapping("batchRemove/{classifyIds}")
  @ApiOperation(value = "批量删除分类数据",notes = "批量删除分类数据")
  @Log(title = "批量删除分类数据", describe = "批量删除分类数据", type = BusinessType.REMOVE)
  public JSONObject batchRemove(@PathVariable("classifyIds") String classifyIds) {
    return repClassManageService.batchRemove(classifyIds);
  }

  /**
   * 获取分类下拉框数据
   * @return
   */
  @PostMapping("getClassSelect")
  @ApiOperation(value = "获取分类下拉框数据",notes = "获取分类下拉框数据")
  public Object getClassSelect(){
    return repClassManageService.getClassSelect();
  }



}
