package com.pearadmin.modules.hdjk.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.modules.ppt.domain.PptModelConfig;
import com.pearadmin.modules.hdjk.domain.HdjkModelManage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 日报活动监控模板管理Service
 */
public interface HdjkModelManageService {
    /**
     * 获取模板管理列表数据
     * @param rbhdjkModelManage
     * @return
     */
     public List<HdjkModelManage> modelDataList(HdjkModelManage rbhdjkModelManage);

    /**
     * 模板管理-模板上传
     * @param file
     * @return
     */
    public boolean upload(MultipartFile file, String qyValue, String tableName, String modelType,String modelParam,String backgroundColor,String lineColor,String isCreateImg,String sendFileName);

    /**
     * 根据模板ID删除模板信息
     * @param modelId
     * @return
     */
    public boolean modelRemove(int modelId);

    /**
     * 文件下载
     * @param modelId
     * @return
     */
    ResponseEntity<InputStreamResource> downloadFile(int modelId);

    /**
     * 获取模板列表
     * @return
     */
    public List<PptModelConfig> getModelList();

    /**
     * 创建文件
     */
    public boolean createFile(int modelId);

    /**
     * 批量创建文件
     */
    public boolean batchCreateFile(String param);

    /**
     * 在线预览
     * @param param
     * @param response
     */
    public JSONObject fileView(@RequestBody String param, HttpServletResponse response);


    /**
     * 根据ID查询活动监控信息
     */
    public HdjkModelManage getById(int modelId);


    /**
     * 活动监控模块管理修改
     * @param file
     * @param qyValue
     * @param tableName
     * @param modelType
     * @param modelParam
     * @param backgroundColor
     * @param lineColor
     * @param isCreateImg
     * @param modelId
     * @return
     */
    public boolean editHdjkMb(@RequestParam("file") MultipartFile file, String qyValue, String tableName, String modelType, String modelParam, String backgroundColor, String lineColor, String isCreateImg, int modelId,String sendFileName);

}
