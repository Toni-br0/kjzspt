package com.pearadmin.modules.knowBase.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.modules.knowBase.domain.KnowbaseClassInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 创建日期：2025-06-25
 * 类别管理
 **/

public interface ClassManageService {

    /**
     * 获取类别管理列表数据
     * @param knowbaseClassInfo
     * @return
     */
    public List<KnowbaseClassInfo> getClassList(KnowbaseClassInfo knowbaseClassInfo);

    /**
     * 保存类别信息
     * @param knowbaseClassInfo
     * @return
     */
    public JSONObject saveClassInfo(KnowbaseClassInfo knowbaseClassInfo);

    /**
     * 单个删除类别数据
     * @param classId
     * @return
     */
    public JSONObject remove(int classId);

    /**
     * 批量删除类别数据
     * @param classIds
     * @return
     */
    public JSONObject  batchRemove(String classIds);


    /**
     * 根据类别ID获取类别信息
     * @param classId
     * @return
     */
    public KnowbaseClassInfo getClassInfoById(int classId);

    /**
     * 获取类别下拉树
     * @return
     */
    public Object getClassSelect();


    /**
     * 获取类别下拉树(已选中)
     * @return
     */
    public Object getClassSelectSel(String classCode);

}
