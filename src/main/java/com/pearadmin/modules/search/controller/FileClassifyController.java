/*
package com.pearadmin.modules.search.controller;

import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.tools.SequenceUtil;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.common.web.domain.response.module.ResultTree;
import com.pearadmin.modules.search.domain.SearchFileClassify;
import com.pearadmin.modules.search.service.impl.FileClassifyServiceImpl;
import com.pearadmin.modules.sys.domain.SysDept;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;

*/
/**
 * 创建日期：2025-01-15
 **//*


@RestController
@Api(tags = {"文件分类"})
@Slf4j
@RequiredArgsConstructor
@RequestMapping(ControllerConstant.API_SEARCH_PREFIX + "fileClassify")
public class FileClassifyController extends BaseController {

  private final String modulePath = "search/fileClassify/";

  @Resource
  private FileClassifyServiceImpl fileClassifyService;

  */
/**
   * 打开文件分类管理页面
   * @return
   *//*

  @GetMapping("toFileClassify")
  @ApiOperation(value = "打开文件分类管理页",notes = "打开文件分类管理页")
  @PreAuthorize("hasPermission('/search/fileClassify/toFileClassify','search:fileClassify:toFileClassify')")
  public ModelAndView toFileClassify(Model model) {
    return jumpPage(modulePath + "fileClassify");
  }

  @GetMapping("toAdd")
  public ModelAndView toAdd() {
    return jumpPage(modulePath + "add");
  }

  @GetMapping("data")
  public ResultTable data() {
    List<SearchFileClassify> data = fileClassifyService.list();
    return dataTable(data);
  }


  */
/**
   * Describe: 文件分类树状数据结构
   * Param ModelAndView
   * Return ModelAndView
   *//*

  @GetMapping("tree")
  public ResultTree tree() {
    List<SearchFileClassify> data = fileClassifyService.list();
    return dataTree(data);
  }

  */
/**
   * Describe: 文件分类树状数据结构
   * Param ModelAndView
   * Return ModelAndView
   *//*

  @GetMapping("queryTree")
  public ResultTree queryTree() {
    List<SearchFileClassify> data = fileClassifyService.queryList();
    return dataTree(data);
  }

  */
/**
   * Describe: 保存文件分类信息
   * Param SysDept
   * Return 执行结果
   *//*

  @PostMapping("save")
  @ApiOperation(value = "保存文件分类信息")
  @Log(title = "保存文件分类信息", describe = "保存文件分类信息", type = BusinessType.ADD)
  public Result save(@RequestBody SearchFileClassify searchFileClassify) {

    int result = fileClassifyService.save(searchFileClassify);
    if(result == -1){
      return failure("文件分类编码已存在");
    }else if(result == -2){
      return failure("文件分类名称已存在");
    }else if(result == -3){
      return failure("文件分类保存失败");
    }
    return decide(true);
  }

  */
/**
   * Describe: 文件分类删除接口
   * Param: id
   * Return: Result
   *//*

  @DeleteMapping("remove/{classifyId}")
  @Log(title = "文件分类删除", describe = "文件分类删除", type = BusinessType.REMOVE)
  public Result remove(@PathVariable String classifyId) {
    if(classifyId.equals("1")){
      return failure("不能删除主节点");
    }

    if (fileClassifyService.selectByParentId(classifyId).size() > 0) {
      return failure("请先删除下级文件分类");
    }

    int result = fileClassifyService.remove(classifyId);
    if(result == 0){
      return decide(true);
    }else if(result == -1){
      return failure("文件分类删除失败");
    }else if(result == -2){
      return failure("文件分类在文件管理中已使用");
    }
    return failure("文件分类删除失败");

  }

}
*/
