package com.pearadmin.modules.im.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.modules.im.domain.ImFilePushManage;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 创建日期：2025-04-25
 * IM文件推送管理
 **/

public interface IFilePushManageService {

    /**
     * 获取IM文件推送管理列表
     * @param imFilePushManage
     * @return
     */
    List<ImFilePushManage> getImFilePushManageList(ImFilePushManage imFilePushManage);

    /**
     * 保存IM文件推送管理信息
     * @param imFilePushManage
     * @return
     */
    boolean save(ImFilePushManage imFilePushManage);

    /**
     * Describe: 文件推送管理单个删除
     */
     boolean remove(int manageId);

    /**
     * 根据id获取IM文件推送管理信息
     * @param manageId
     * @return
     */
    ImFilePushManage getById(int manageId);

    /**
     * 获取页面的模板下拉框
     * @return
     */
    Object getModelSelect();

    /**
     * 获取模板下拉树（选中值）
     * @return
     */
    Object getModelSelectSel(int modelId);

    /**
     * 在线预览PPT文件
     * @param param
     * @param response
     */
    JSONObject fileView(String param, HttpServletResponse response);

    /**
     * 文件下载
     * @param manageId
     * @return
     * @throws Exception
     */
    ResponseEntity<InputStreamResource> downloadFile(int manageId) throws Exception;


    /**
     * 一键绑定
     * @param param
     * @return
     */
    JSONObject binding(String param);

    /**
     * 批量一键绑定
     * @param param
     * @return
     */
    JSONObject batchBinding(String param);

    /**
     * 一键推送
     * @param param
     */
    public JSONObject push(String param);

    /**
     * 一键推送至领导
     * @param param
     */
    public JSONObject pushLeader(String param);

    /**
     * 批量推送
     * @param param
     * @return
     */
    public JSONObject batchPush(String param);

    /**
     * 批量推送至领导
     * @param param
     * @return
     */
    public JSONObject batchPushLeader(String param);

    /**
     * 文件上传
     * @param file
     * @return
     */
    public boolean upload(MultipartFile file, int manageId);

}
