package com.pearadmin.modules.knowBase.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.knowBase.domain.KnowbaseDraftInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseKnowInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseUndercarrInfo;
import com.pearadmin.modules.knowBase.service.impl.KnowManageServiceImpl;
import com.pearadmin.modules.ppt.domain.PptConvertConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * 创建日期：2025-06-17
 * 知识管理
 **/

@RestController
@Api(tags = {"知识管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_KNOWMANAGE_PREFIX + "knowManage")
public class KnowManageController extends BaseController {

    @Resource
    private KnowManageServiceImpl knowManageService;

    /**
     * 基础路径
     */
    private final String modulePath = "knowBase/knowManage/";

    /**
     * 打开新建知识页面
     * @return
     */
    @GetMapping("toAddKnow")
    @ApiOperation(value = "打开新建知识页面",notes = "打开新建知识页面")
    @PreAuthorize("hasPermission('/knowBase/knowManage/toAddKnow','knowBase:knowManage:toAddKnow')")
    public ModelAndView toAddKnow(Model model) {
        return jumpPage(modulePath + "addKnow");
    }

    /**
     * 发布知识
     * @param images 封面
     * @param files 附件
     * @param videos 视频
     * @param knowTitle 知识标题
     * @param knowType  知识类型
     * @param knowContent 知识内容
     * @param knowClass 知识类别
     * @param sharePurv 分享权限
     * @return
     */
    @PostMapping("/publishKnow")
    public JSONObject publishKnow(@RequestParam(required = false) MultipartFile[] files,
                              @RequestParam(required = false) MultipartFile[] images,
                              @RequestParam(required = false) MultipartFile[] videos,
                              @RequestParam(required = false) String knowTitle,
                              @RequestParam(required = false) String knowType,
                              @RequestParam(required = false) String knowContent,
                              @RequestParam(required = false) String knowClass,
                              @RequestParam(required = false) String sharePurv) {

        KnowbaseKnowInfo knowInfo = new KnowbaseKnowInfo();
        knowInfo.setKnowTitle(knowTitle);
        knowInfo.setKnowType(knowType);
        knowInfo.setKnowContent(knowContent);
        knowInfo.setKnowClass(knowClass);
        knowInfo.setSharePurv(sharePurv);

        JSONObject jsonObject = knowManageService.publishKnow(files,images,videos,knowInfo);

        return jsonObject;

    }

    /**
     * 保存草稿
     * @param images 封面
     * @param files 附件
     * @param videos 视频
     * @param knowTitle 知识标题
     * @param knowType  知识类型
     * @param knowContent 知识内容
     * @param knowClass 知识类别
     * @param sharePurv 分享权限
     * @return
     */
    @PostMapping("/saveDraft")
    public JSONObject saveDraft(@RequestParam(required = false) MultipartFile[] files,
                                  @RequestParam(required = false) MultipartFile[] images,
                                  @RequestParam(required = false) MultipartFile[] videos,
                                  @RequestParam(required = false) String knowTitle,
                                  @RequestParam(required = false) String knowType,
                                  @RequestParam(required = false) String knowContent,
                                  @RequestParam(required = false) String knowClass,
                                  @RequestParam(required = false) String sharePurv) {

        KnowbaseDraftInfo knowbaseDraftInfo = new KnowbaseDraftInfo();
        knowbaseDraftInfo.setKnowTitle(knowTitle);
        knowbaseDraftInfo.setKnowType(knowType);
        knowbaseDraftInfo.setKnowContent(knowContent);
        knowbaseDraftInfo.setKnowClass(knowClass);
        knowbaseDraftInfo.setSharePurv(sharePurv);

        JSONObject jsonObject = knowManageService.saveDraft(files,images,videos,knowbaseDraftInfo);

        return jsonObject;

    }



    /**
     * 打开知识管理页面
     * @return
     */
    @GetMapping("toKnowManage")
    @ApiOperation(value = "打开知识管理页面",notes = "打开知识管理页面")
    @PreAuthorize("hasPermission('/knowBase/knowManage/toKnowManage','knowBase:knowManage:toKnowManage')")
    public ModelAndView toKnowManage(Model model) {
        return jumpPage(modulePath + "knowManage");
    }

    /**
     * 获取知识管理列表数据
     * @param knowbaseKnowInfo
     * @param pageDomain
     * @return
     */
    @GetMapping("getKnowDataList")
    @ApiOperation(value = "获取知识管理列表数据",notes = "获取知识管理列表数据")
    public ResultTable getKnowDataList(KnowbaseKnowInfo knowbaseKnowInfo, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        if(StringUtils.isEmpty(knowbaseKnowInfo.getQueryType()) || knowbaseKnowInfo.getQueryType().equals("yfb")){//已发布
            PageInfo<KnowbaseKnowInfo>  pageInfo = new PageInfo<>(knowManageService.getKnowDataList(knowbaseKnowInfo));
            return pageTable(pageInfo.getList(), pageInfo.getTotal());
        }if(knowbaseKnowInfo.getQueryType().equals("yxj")){ //已下架
            PageInfo<KnowbaseUndercarrInfo>  pageInfo = new PageInfo<>(knowManageService.getUndercarrDataList(knowbaseKnowInfo));
            return pageTable(pageInfo.getList(), pageInfo.getTotal());
        }else{ //草稿箱
            PageInfo<KnowbaseDraftInfo>  pageInfo = new PageInfo<>(knowManageService.getDraftDataList(knowbaseKnowInfo));
            return pageTable(pageInfo.getList(), pageInfo.getTotal());
        }
    }

    /**
     * 知识下架
     */
    @GetMapping("undercarr/{infoId}")
    @ApiOperation(value = "知识下架",notes = "知识下架")
    @Log(title = "知识下架", describe = "知识下架", type = BusinessType.REMOVE)
    public JSONObject undercarr(@PathVariable("infoId") int infoId) {
        return knowManageService.undercarr(infoId);
    }

    /**
     * 知识上架
     */
    @GetMapping("uppercarr")
    @ApiOperation(value = "知识下架",notes = "知识下架")
    @Log(title = "知识下架", describe = "知识下架", type = BusinessType.REMOVE)
    public JSONObject uppercarr(@RequestParam("infoId") int infoId,@RequestParam("queryType") String queryType) {
        return knowManageService.uppercarr(infoId,queryType);
    }


    /**
     * 单个删除草稿箱知识
     */
    @DeleteMapping("draftRemove/{infoId}")
    @ApiOperation(value = "单个删除草稿箱知识",notes = "单个删除草稿箱知识")
    @Log(title = "单个删除草稿箱知识", describe = "单个删除草稿箱知识", type = BusinessType.REMOVE)
    public JSONObject draftRemove(@PathVariable("infoId") int infoId) {
        return knowManageService.draftRemove(infoId);
    }


    /**
     * 跳转至编辑知识页面
     * @return
     */
    @GetMapping("toKnowEdit")
    @ApiOperation(value = "跳转至编辑知识页面",notes = "跳转至编辑知识页面")
    public ModelAndView toKnowEdit(Model model,@RequestParam("infoId") int infoId,@RequestParam("queryType") String queryType) {
        model.addAttribute("knowInfo", knowManageService.toKnowEdit(infoId,queryType));
        model.addAttribute("queryType", queryType);
        return jumpPage(modulePath + "editKnow");
    }

}
