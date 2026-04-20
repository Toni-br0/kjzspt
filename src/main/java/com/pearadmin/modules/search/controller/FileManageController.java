/*
  package com.pearadmin.modules.search.controller;

*/
/**
   * 创建日期：2024-12-06
   * 智能搜索文件管理
   **//*



  import com.alibaba.fastjson.JSONObject;
  import com.github.pagehelper.PageHelper;
  import com.github.pagehelper.PageInfo;
  import com.pearadmin.common.aop.annotation.Log;
  import com.pearadmin.common.aop.enums.BusinessType;
  import com.pearadmin.common.constant.CommonConstant;
  import com.pearadmin.common.constant.ControllerConstant;
  import com.pearadmin.common.web.base.BaseController;
  import com.pearadmin.common.web.domain.request.PageDomain;
  import com.pearadmin.common.web.domain.response.Result;
  import com.pearadmin.common.web.domain.response.module.ResultTable;
  import com.pearadmin.modules.search.domain.SearchFileManage;
  import com.pearadmin.modules.search.service.impl.FileManageServiceImpl;
  import io.swagger.annotations.Api;
  import io.swagger.annotations.ApiOperation;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.core.io.InputStreamResource;
  import org.springframework.http.ResponseEntity;
  import org.springframework.security.access.prepost.PreAuthorize;
  import org.springframework.transaction.annotation.Transactional;
  import org.springframework.ui.Model;
  import org.springframework.web.bind.annotation.*;
  import org.springframework.web.multipart.MultipartFile;
  import org.springframework.web.servlet.ModelAndView;
  import java.io.IOException;


*/
/**
   * 打开文件管理页面
   *//*


  @RestController
  @Api(tags = {"文件管理"})
  @Slf4j
  @RequiredArgsConstructor
  @RequestMapping(ControllerConstant.API_SEARCH_PREFIX + "fileManage")
  public class FileManageController extends BaseController {
    private final String modulePath = "search/fileManage/";

    private final FileManageServiceImpl fileManageService;

*/
/**
     * 打开智能搜索文件管理页面
     * @return*//*



    @GetMapping("toFileManage")
    @ApiOperation(value = "智能搜索文件管理页面",notes = "智能搜索文件管理页面")
    @PreAuthorize("hasPermission('/search/fileManage/toFileManage','search:fileManage:toFileManage')")
    public ModelAndView toFileManage(Model model) {
      return jumpPage(modulePath + "fileManage");
    }

*/
/**
     * 获取智能搜索文件管理列表数据
     * @param searchFileManage
     * @param pageDomain
     * @return*//*



    @GetMapping("fileDataList")
    @ApiOperation(value = "获取智能搜索文件管理列表数据",notes = "获取智能搜索文件管理列表数据")
    public ResultTable fileDataList(SearchFileManage searchFileManage, PageDomain pageDomain) {
      PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
      PageInfo<SearchFileManage> pageInfo = new PageInfo<>(fileManageService.fileDataList(searchFileManage));
      return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

*/
/**
     * 打开智能搜索文件管理文件上传页面
     * @return*//*



    @GetMapping("toFileAdd")
    @ApiOperation(value = "智能搜索文件管理文件上传页面",notes = "智能搜索文件管理文件上传页面")
    public ModelAndView toFileAdd(Model model) {
      return jumpPage(modulePath + "fileAdd");
    }

*/
/**
     * 智能搜索文件管理文件上传
     * @param file
     * @return*//*



    @RequestMapping(value = "uploadFile")
    @ApiOperation(value = "智能搜索文件管理文件上传",notes = "智能搜索文件管理文件上传")
    @Log(title = "智能搜索文件管理文件上传", describe = "智能搜索文件管理文件上传", type = BusinessType.UPLOAD)
    @ResponseBody
    public Result uploadFile(@RequestParam("file") MultipartFile file,String fileClassifyCode,String fileClassifyName) {
      JSONObject retJson = fileManageService.uploadFile(file,fileClassifyCode,fileClassifyName);
      if (retJson.getString("retCode").equals("200")){
        return Result.success(200, "上传成功", retJson);
      } else {
        return Result.failure("上传失败");
      }
    }

*/
/**
     * Describe: 智能搜索文件删除接口
     * Param: id
     * Return: 文件流*//*



    @DeleteMapping("fileRemove/{fileId}")
    @ApiOperation(value = "智能搜索文件删除",notes = "智能搜索文件删除")
    @Log(title = "智能搜索文件删除", describe = "智能搜索文件删除", type = BusinessType.REMOVE)
    public Result fileRemove(@PathVariable("fileId") String fileId) {
      boolean result = fileManageService.fileRemove(fileId);
      return Result.decide(result, "删除成功", "删除失败");
    }

*/
/**
     * 智能搜索文件批量删除
     * @param fileIds
     * @return*//*



    @Transactional(rollbackFor = Exception.class)
    @ApiOperation(value = "智能搜索文件批量删除",notes = "智能搜索文件批量删除")
    @DeleteMapping("batchFileRemove/{fileIds}")
    @Log(title = "智能搜索文件批量删除", describe = "智能搜索文件批量删除", type = BusinessType.REMOVE)
    public Result batchRemove(@PathVariable("fileIds") String fileIds) {
      for (String fileId : fileIds.split(CommonConstant.COMMA)) {
        fileManageService.fileRemove(fileId);
      }
      return Result.success("删除成功");
    }

*/
/**
     * 文件下载
     * @param fileId
     * @return
     * @throws IOException*//*



    @RequestMapping(value = "/downloadFile", method = RequestMethod.GET)
    @ApiOperation(value = "文件下载",notes = "文件下载")
    @Log(title = "文件下载", describe = "文件下载", type = BusinessType.DOWNLOAD)
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam(name = "fileId") String fileId) throws Exception{
      return fileManageService.downloadFile(fileId);
    }

*/
/**
     * 获取文件分类树
     * @return*//*



    @RequestMapping(value = "/treeload")
    @ResponseBody
    public Object treeload(){
      return fileManageService.treeload();
    }


  }
*/
