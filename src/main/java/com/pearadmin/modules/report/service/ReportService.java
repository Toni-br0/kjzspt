package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.modules.report.domain.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 自助取数
 */
public interface ReportService {

    /**
     * 查询分类
     * @return
     */
    List<ReportClassify> searchClassify();

    //报表信息
    PageInfo<RetReportData> getReportList(ReportParam reportParam, PageDomain pageDomain);

    //报表信息
    Map<String, Object> reportListNew(ReportParam reportParam, PageDomain pageDomain);

    //保存报表
    JSONObject saveReport(JSONObject param);

    //保存模板
    JSONObject saveTemplate(JSONObject param);

    //查询我的报表
    List<ReportInfo> getReportInfoList(ReportInfo reportInfo);

    //查询我的模板
    List<ReportTemplate> getTemplateInfoList(ReportTemplate templateInfo);

    //根据ID删除报表
    public boolean reportRemove(int id);

    /**
     * 根据ID删除模板
     * @param id
     * @return
     */
    public boolean templateRemove(int id);

    public void exportExcel(JSONObject param, HttpServletResponse response);

    /**
     * 下载报表
     * @param reportId
     * @return
     */
    ResponseEntity<InputStreamResource> downloadReport(String reportId);

    /**
     * 下载模板
     * @param templateId
     * @return
     */
    ResponseEntity<InputStreamResource> downloadTemplate(int templateId);

    /**
     * 获取维度树数据
     * @return
     */
    public Object dimensionTreeload(String isNonStand);

    /**
     * 获取维度树数据 根节点
     * @return
     */
    public Object dimensionTreeloadRoot(String isNonStand);

    /**
     * 获取维度树数据 子节点
     * @return
     */
    public Object dimensionTreeloadChild(String isNonStand,String parentId);

    /**
     * 通过报表ID获取报表详情信息
     * @param param
     * @return
     */
    JSONObject reportInfo(String param);

    /**
     * 通过模板ID获取模板详情信息
     * @param templateId
     * @return
     */
    JSONObject templateInfo(int templateId);

    /**
     * 通过分类Id获取指标树数据
     * @param classifyId
     * @return
     */
    Object getQuotaByClassId(int classifyId, String indexType,String dateType,String roleType);

    /**
     * 根据指标Id获取条件
     * @param indexId
     * @return
     */
    public Object getConditByIndexId(int indexId,String tjCodeStr);

    /**
     * 下载报表
     * @param reportParam
     * @return
     */
    public JSONObject fileDownload(ReportParam reportParam);

    /**
     * 自动化报表
     * @param reportParam
     * @return
     */
    public JSONObject autoCreateReport(ReportParam reportParam);

    /**
     * 获取当前登录人所属的所有地市
     */
    public JSONObject getAllLatnInfo();

    /**
     * 通过数据周期,分析角色,指标类型 获取指标中已配置的分类信息
     * @return
     */
    public Object getClassByWhere(String checkdateType, String checkRoleType,String checkIndexType);


    /**
     * 获取当前登录人的区域信息
     * @return
     */
    public String getCurrAreaInfo();

}