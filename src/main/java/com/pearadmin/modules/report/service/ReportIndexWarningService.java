package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.modules.report.domain.ReportIndexWarning;
import com.pearadmin.modules.sys.domain.SysDictData;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 创建日期：2025-10-16
 * 指标预警配置表
 **/

public interface ReportIndexWarningService {

    /**
     * 获取指标预警列表数据
     * @param reportIndexWarning
     * @return
     */
    public List<ReportIndexWarning> getIndexWarningList(ReportIndexWarning reportIndexWarning);


    /**
     * 单个删除指标预警数据
     * @param warningId
     * @return
     */
    public JSONObject remove(String warningId);

    /**
     * 批量删除指标预警数据
     * @param warningIds
     * @return
     */
    public JSONObject batchRemove(String warningIds);

    /**
     * 根据分类获取指标下拉框
     * @param classifyId
     * @return
     */
    public Object getIndexSelect(int classifyId,int warningIndexId);


    /**
     * 获取推送对象管理下拉框
     * @param localCity
     * @return
     */
    public Object getPushObjectSelectSel(String localCity,String pushObjectId);

    /**
     * 保存指标预警数据
     * @param reportIndexWarning
     * @return
     */
    public JSONObject saveIndexWarning(ReportIndexWarning reportIndexWarning);


    /**
     * 修改指标预警数据
     * @param reportIndexWarning
     * @return
     */
    public JSONObject updateIndexWarning(ReportIndexWarning reportIndexWarning);


    /**
     * 根据ID根据对象
     * @param warningId
     * @return
     */
    public ReportIndexWarning getById(String warningId);

    /**
     * 获取当前登录人数据字典中地市的值
     * @return
     */
    public List<SysDictData> getSysDictDsData();

}
