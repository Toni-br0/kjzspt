package com.pearadmin.modules.report.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.service.impl.PushObjectManageServiceImpl;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportIndexWarning;
import com.pearadmin.modules.report.service.impl.RepClassManageServiceImpl;
import com.pearadmin.modules.report.service.impl.ReportIndexWarningServiceImpl;
import com.pearadmin.modules.sys.domain.SysDictData;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.service.impl.SysUserServiceImpl;
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
 * 创建日期：2025-10-16
 * 指标预警
 **/

@RestController
@Api(tags = {"指标预警"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "indexWarning")
public class RepIndexWarningController  extends BaseController {

  //基础路径
  private final String modelPath = "autoData/indexWarning/";

  @Resource
  private ReportIndexWarningServiceImpl reportIndexWarningService;

  @Resource
  private RepClassManageServiceImpl repClassManageService;


  @Resource
  private PushObjectManageServiceImpl pushObjectManageServiceImpl;



  /**
   * 打开指标预警页面
   * @return
   */
  @GetMapping("toIndexWarning")
  @ApiOperation(value = "打开指标预警页面",notes = "打开指标预警页面")
  @PreAuthorize("hasPermission('/autoData/indexWarning/toIndexWarning','autoData:indexWarning:toIndexWarning')")
  public ModelAndView toIndexWarning(Model model) {

    List<SysDictData> dsList = reportIndexWarningService.getSysDictDsData();
    model.addAttribute("dsList", dsList);

    return jumpPage(modelPath + "indexWarning");
  }



  /**
   * 获取指标预警列表数据
   * @param reportIndexWarning
   * @param pageDomain
   * @return
   */
  @GetMapping("getIndexWarningList")
  @ApiOperation(value = "获取指标预警列表数据",notes = "获取指标预警列表数据")
  public ResultTable getIndexWarningList(ReportIndexWarning reportIndexWarning, PageDomain pageDomain) {
    PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
    PageInfo<ReportIndexWarning> pageInfo = new PageInfo<>(reportIndexWarningService.getIndexWarningList(reportIndexWarning));
    return pageTable(pageInfo.getList(), pageInfo.getTotal());
  }

  /**
   * 单个删除指标预警数据
   * Param: warningId
   * Return: 文件
   */
  @DeleteMapping("remove/{warningId}")
  @ApiOperation(value = "单个删除指标预警数据",notes = "单个删除指标预警数据")
  @Log(title = "单个删除指标预警数据", describe = "单个删除指标预警数据", type = BusinessType.REMOVE)
  public JSONObject remove(@PathVariable("warningId") String warningId) {
    return reportIndexWarningService.remove(warningId);
  }

  /**
   * 批量删除指标预警数据
   * @param warningIds
   * @return
   */
  @DeleteMapping("batchRemove/{warningIds}")
  @ApiOperation(value = "批量删除指标预警数据",notes = "批量删除指标预警数据")
  @Log(title = "批量删除指标预警数据", describe = "批量删除指标预警数据", type = BusinessType.REMOVE)
  public JSONObject batchRemove(@PathVariable("warningIds") String warningIds) {
    return reportIndexWarningService.batchRemove(warningIds);
  }

  /**
   * 打开新增指标预警页面
   * @return
   */
  @GetMapping("toAddIndexWarning")
  @ApiOperation(value = "打开新增指标预警页面",notes = "打开新增指标预警页面")
  public ModelAndView toAddIndexWarning(Model model) {
    List<ReportClassify> classifyList = repClassManageService.getClassList();
    model.addAttribute("classifyList", classifyList);

    List<SysDictData> dsList = reportIndexWarningService.getSysDictDsData();
    model.addAttribute("dsList", dsList);

    //当前登录人信息
    SysUser currentUser = UserContext.currentUser();
    ImPushObjectManage imPushObjectManage = pushObjectManageServiceImpl.getImPushObjectByObjectId(currentUser.getUsername());
    if(imPushObjectManage ==null){
      imPushObjectManage = new ImPushObjectManage();
    }
    model.addAttribute("imPushObjectManage", imPushObjectManage);

    return jumpPage(modelPath + "addIndexWarning");
  }

  /**
   * 根据分类获取指标下拉框
   * @return
   */
  @GetMapping("getIndexSelect")
  @ApiOperation(value = "根据分类获取指标下拉框",notes = "根据分类获取指标下拉框")
  public Object getIndexSelect(int classifyId,int warningIndexId){
    return reportIndexWarningService.getIndexSelect(classifyId,warningIndexId);
  }


  /**
   * 获取推送对象管理下拉框
   * @return
   */
  @GetMapping("getPushObjectSelectSel")
  @ApiOperation(value = "获取推送对象管理下拉框",notes = "获取推送对象管理下拉框")
  public Object getPushObjectSelectSel(String localCity,String pushObjectId){
    return  reportIndexWarningService.getPushObjectSelectSel(localCity,pushObjectId);
  }

  /**
   * 保存指标预警数据
   * @param reportIndexWarning
   * @return
   */
  @Repeat
  @PostMapping("saveIndexWarning")
  @ApiOperation(value = "保存指标预警数据",notes = "保存指标预警数据")
  @Log(title = "保存指标预警数据", describe = "保存指标预警数据", type = BusinessType.ADD)
  public JSONObject saveIndexWarning(@RequestBody ReportIndexWarning reportIndexWarning){
    return reportIndexWarningService.saveIndexWarning(reportIndexWarning);
  }

  /**
   * 修改指标预警数据
   * @param reportIndexWarning
   * @return
   */
  @Repeat
  @PostMapping("updateIndexWarning")
  @ApiOperation(value = "修改指标预警数据",notes = "修改指标预警数据")
  public JSONObject updateIndexWarning(@RequestBody ReportIndexWarning reportIndexWarning){
    return reportIndexWarningService.updateIndexWarning(reportIndexWarning);
  }


  /**
   * 打开指标预警修改页面
   */
  @GetMapping("toEditIndexWarning")
  @ApiOperation(value = "打开指标预警修改页面",notes = "打开指标预警修改页面")
  public ModelAndView toEditIndexWarning(Model model,String warningId) {

    List<ReportClassify> classifyList = repClassManageService.getClassList();
    model.addAttribute("classifyList", classifyList);

    ReportIndexWarning reportIndexWarning = reportIndexWarningService.getById(warningId);
    model.addAttribute("reportIndexWarning", reportIndexWarning);

    List<SysDictData> dsList = reportIndexWarningService.getSysDictDsData();
    model.addAttribute("dsList", dsList);

    if(reportIndexWarning != null){
      String mangeId = reportIndexWarning.getPushObjectId() ==null || reportIndexWarning.getPushObjectId().equals("")?"0":reportIndexWarning.getPushObjectId();
      int iMangeId = Integer.parseInt(mangeId);
      ImPushObjectManage imPushObjectManage = pushObjectManageServiceImpl.getById(iMangeId);
      if(imPushObjectManage ==null){
        imPushObjectManage = new ImPushObjectManage();
      }
      model.addAttribute("imPushObjectManage", imPushObjectManage);
    }


    return jumpPage(modelPath + "editIndexWarning");
  }


}
