/*
package com.pearadmin.modules.search.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.search.domain.*;
import com.pearadmin.modules.search.service.impl.FileContentSearchServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;

*/
/**
 * 创建日期：2024-12-10
 * 描述：文件内容搜索控制器
 **//*



@RestController
@Api(tags = {"文件内容搜索"})
@Slf4j
@RequiredArgsConstructor
@RequestMapping(ControllerConstant.API_SEARCH_PREFIX + "fileContentSearch")
public class FileContentSearchController extends BaseController {

    private final String modulePath = "search/fileContentSearch/";

    private final FileContentSearchServiceImpl fileContentSearchService;

    */
/**
     * 打开智能搜索文件搜索页面
     * @param model
     * @return
     *//*


    @GetMapping("toFileContentSearch")
    @ApiOperation(value = "打开智能搜索文件搜索页面",notes = "打开智能搜索文件搜索页面")
    @PreAuthorize("hasPermission('/search/fileContentSearch/toFileContentSearch','search:fileContentSearch:toFileContentSearch')")
    public ModelAndView toFileManage(Model model) {
        return jumpPage(modulePath + "fileContentSearch");
    }

*/
/**
     * 文件内容搜索
     * @param dto
     * @return*//*



    @PostMapping(value = "/searchPage")
    public JSONObject searchPage(@RequestBody FileDTO dto) {
        return fileContentSearchService.searchPage(dto);
    }

*/
/**
     * 文件内容搜索
     * @param dto
     * @return*//*



    @GetMapping(value = "/searchTable")
    public ResultTable searchTable(FileDTO dto, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<FileResult> pageInfo = new PageInfo<>(fileContentSearchService.searchTable(dto));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }


*/
/**
     * 文件预览
     * @param param
     * @return
     * @throws IOException*//*



    @RequestMapping(value = "/fileView", method = RequestMethod.POST)
@ApiOperation(value = "文件预览",notes = "文件预览")
    @Log(title = "文件预览", describe = "文件预览", type = BusinessType.OPEN)

    public String fileView(@RequestBody String param) throws Exception{
        return fileContentSearchService.fileView(param);
    }


}
*/
