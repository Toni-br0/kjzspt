package com.pearadmin.modules.knowBase.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.knowBase.domain.KnowbaseClassInfo;
import com.pearadmin.modules.knowBase.service.impl.ClassManageServiceImpl;
import com.pearadmin.modules.ppt.domain.PptConvertType;
import com.pearadmin.modules.ppt.domain.PptModelConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建日期：2025-06-25
 * 类别管理
 **/

@RestController
@Api(tags = {"类别管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_KNOWMANAGE_PREFIX + "classManage")
public class ClassManageController extends BaseController {

  @Resource
  private ClassManageServiceImpl classManageService;

  /**
   * 基础路径
   */
  private final String modulePath = "knowBase/classManage/";

  /**
   * 打开类别管理页面
   * @return
   */
  @GetMapping("toClassManage")
  @ApiOperation(value = "打开类别管理页面",notes = "打开类别管理页面")
  @PreAuthorize("hasPermission('/knowBase/classManage/toClassManage','knowBase:classManage:toClassManage')")
  public ModelAndView toClassManage(Model model) {
    return jumpPage(modulePath + "classManage");
  }

  /**
   * 获取类别管理列表数据
   * @param knowbaseClassInfo
   * @param pageDomain
   * @return
   */
  @GetMapping("getClassList")
  @ApiOperation(value = "获取类别管理列表数据",notes = "获取类别管理列表数据")
  public ResultTable getClassList(KnowbaseClassInfo knowbaseClassInfo, PageDomain pageDomain) {
    PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
    PageInfo<KnowbaseClassInfo> pageInfo = new PageInfo<>(classManageService.getClassList(knowbaseClassInfo));
    return pageTable(pageInfo.getList(), pageInfo.getTotal());
  }

  /**
   * 打开类别新增页面
   * @return
   */
  @GetMapping("toAddClass")
  @ApiOperation(value = "打开类别新增页面",notes = "打开类别新增页面")
  public ModelAndView toAddClass(Model model) {
    return jumpPage(modulePath + "addClass");
  }

  /**
   * 保存类别数据
   * @param knowbaseClassInfo
   * @return
   */
  @Repeat
  @PostMapping("saveClassInfo")
  @ApiOperation(value = "保存类别数据",notes = "保存类别数据")
  @Log(title = "保存类别数据", describe = "保存类别数据", type = BusinessType.ADD)
  public JSONObject saveClassInfo(@RequestBody KnowbaseClassInfo knowbaseClassInfo){
    return classManageService.saveClassInfo(knowbaseClassInfo);
  }

  /**
   * 单个删除类别数据
   * Param: id
   * Return: 文件
   */
  @DeleteMapping("remove/{classId}")
  @ApiOperation(value = "单个删除类别数据",notes = "单个删除类别数据")
  @Log(title = "单个删除类别数据", describe = "单个删除类别数据", type = BusinessType.REMOVE)
  public JSONObject remove(@PathVariable("classId") int classId) {
    return classManageService.remove(classId);
  }

  /**
   * 批量删除类别数据
   * @param classIds
   * @return
   */
  @DeleteMapping("batchRemove/{classIds}")
  @ApiOperation(value = "批量删除类别数据",notes = "批量删除类别数据")
  @Log(title = "批量删除类别数据", describe = "批量删除类别数据", type = BusinessType.REMOVE)
  public JSONObject batchRemove(@PathVariable("classIds") String classIds) {
    return classManageService.batchRemove(classIds);
  }

  /**
   * 打开类别管理修改页面
   */
  @GetMapping("toEditClass")
  @ApiOperation(value = "打开类别管理修改页面",notes = "打开类别管理修改页面")
  public ModelAndView toEditClass(Model model,int classId) {

    KnowbaseClassInfo knowbaseClassInfo = classManageService.getClassInfoById(classId);
    model.addAttribute("knowbaseClassInfo", knowbaseClassInfo);

    return jumpPage(modulePath + "editClass");
  }


  /**
   * 获取类别下拉树
   * @return
   */
  @GetMapping("getClassSelect")
  @ApiOperation(value = "获取类别下拉树",notes = "获取类别下拉树")
  public Object getClassSelect() {
    return classManageService.getClassSelect();
  }


  /**
   * 获取类别下拉树(已选中)
   * @return
   */
  @GetMapping("getClassSelectSel")
  @ApiOperation(value = "获取类别下拉树(已选中)",notes = "获取类别下拉树(已选中)")
  public Object getClassSelectSel(@RequestParam("classCode") String classCode) {
    return classManageService.getClassSelectSel(classCode);
  }


}
