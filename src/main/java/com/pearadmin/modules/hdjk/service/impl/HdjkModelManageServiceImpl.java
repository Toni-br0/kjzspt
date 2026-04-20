package com.pearadmin.modules.hdjk.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.DateTimeUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import com.pearadmin.modules.im.mapper.ImHdjkPushManageMapper;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.im.service.impl.HdjkFilePushManageServiceImpl;
import com.pearadmin.modules.ppt.domain.PptModelConfig;
import com.pearadmin.modules.ppt.mapper.*;
import com.pearadmin.modules.hdjk.domain.HdjkLog;
import com.pearadmin.modules.hdjk.domain.HdjkModelManage;
import com.pearadmin.modules.hdjk.mapper.HdjkLogMapper;
import com.pearadmin.modules.hdjk.mapper.HdjkModelManageMapper;
import com.pearadmin.modules.hdjk.service.HdjkModelManageService;
import com.pearadmin.modules.hdjk.util.RbExcelUtil;
import com.pearadmin.modules.search.util.FileToPdfUtils;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.service.impl.SysUserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板管理ServiceImpl
 */
@Service
@Slf4j
public class HdjkModelManageServiceImpl implements HdjkModelManageService {

    @Value("${hdjk-target-path}")
    private String hdjkTargetPath;

    @Value("${hdjk-upload-path}")
    private String hdjkUploadPath;

    @Value("${hdjk-excelToPdf-path}")
    private String hdjkExcelToPdfPath;

    @Value("${hdjk-createFile-maxData}")
    private int hdjkCreateFileMaxData;

    @Resource
    private PptModelConfigMapper pptModelConfigMapper;

    @Resource
    private HdjkModelManageMapper rbhdjkModelManageMapper;

    @Resource
    private ImHdjkPushManageMapper imRbFilePushManageMapper;

    @Resource
    private HdjkFilePushManageServiceImpl rbFilePushManageService;

    @Resource
    private HdjkConnSecondTableServiceImpl rbConnSecondTableService;

    @Resource
    private HdjkLogMapper rbhdjkLogMapper;

    @Resource
    private Environment env;

    @Resource
    private SysUserServiceImpl sysUserService;
    @Autowired
    private ImHdjkPushManageMapper imHdjkPushManageMapper;


    @Override
    public List<HdjkModelManage> modelDataList(HdjkModelManage rbhdjkModelManage) {
        QueryWrapper<HdjkModelManage> modelQu = new QueryWrapper<>();
        /*downloadQu.eq("model_name", modelName);
        orderQu.eq("model_target_path", exportPath);*/
        String modelName = rbhdjkModelManage.getModelName()==null?"":rbhdjkModelManage.getModelName().trim();
        if(modelName.length() >0){
            modelQu.like("model_name",modelName);
        }

        String createUserName = rbhdjkModelManage.getCreateUserName()==null?"":rbhdjkModelManage.getCreateUserName().trim();
        if(createUserName.length() >0){
            modelQu.like("create_user_name",createUserName);
        }

        modelQu.orderByDesc("create_time");
        List<HdjkModelManage> manageList = rbhdjkModelManageMapper.selectList(modelQu);
        return manageList;
    }

    /**
     * 模板管理 -模板上传
     * @param file
     * @return
     */
    @Override
    public boolean upload(MultipartFile file, String qyValue, String tableName, String modelType,String modelParam,String backgroundColor,String lineColor,String isCreateImg,String sendFileName) {
        boolean isResult = true;
        if (file.isEmpty()) {
            return false;
        }

        int modelId =0;
        String modelName ="";
        String createUserId = "";
        String createUserName = "";
        String modelSourcePath ="";
        String modelTargetPath ="";
        String modelRange =qyValue;
        String fileSize ="";
        String isSuccess ="0";
        String errorMsg ="模板上传成功";

        InputStream inputStream =null;
        FileOutputStream outputStream =null;
        try {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            // 获取文件名
            String fileName = file.getOriginalFilename();
            log.info("上传的文件名为：" + fileName);

            modelName = fileName;
            modelSourcePath = hdjkUploadPath + fileName;
            modelTargetPath = hdjkTargetPath +fileName;


            // 获取文件的后缀名
            String suffixName = fileName.substring(fileName.lastIndexOf("."));
            log.info("上传的后缀名为：" + suffixName);

            double dFileSize = file.getSize();
            BigDecimal value = new BigDecimal(dFileSize);
            BigDecimal divisor = new BigDecimal(1024);

            // 除法运算，并设置保留两位小数，以及舍入模式
            fileSize = value.divide(divisor, 1, RoundingMode.HALF_UP).toString()+"KB";

            // 文件上传路径
            String filePath = hdjkUploadPath;

            // 解决中文问题，liunx下中文路径，图片显示问题
            // fileName = UUID.randomUUID() + suffixName;

            File dest = new File(filePath + fileName);

            // 检测是否存在目录
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }

            file.transferTo(dest);


            //根据文件名查询模板信息
            QueryWrapper<HdjkModelManage> modelQu = new QueryWrapper<>();
            modelQu.eq("model_name", fileName);
            modelQu.last("limit 1");
            HdjkModelManage rbhdjkModelManage = rbhdjkModelManageMapper.selectOne(modelQu);

            if(rbhdjkModelManage != null){
                rbhdjkModelManage.setCreateTime(LocalDateTime.now());
                rbhdjkModelManage.setCreateUserId(currentUser.getUserId());
                //pptModelConfig.setCreateUserName(currentUser.getUsername());
                rbhdjkModelManage.setCreateUserName(currentUser.getRealName());
                rbhdjkModelManage.setFileSize(fileSize);
                rbhdjkModelManage.setModelArea(qyValue);
                rbhdjkModelManage.setModelType(modelType);
                rbhdjkModelManage.setModelSourcePath(filePath + fileName);
                //rbhdjkModelManage.setModelTargetPath(rbhdjkTargetPath +fileName);
                rbhdjkModelManage.setTableName(tableName);
                rbhdjkModelManage.setModelParam(modelParam);
                rbhdjkModelManage.setBackgroundColor(backgroundColor);
                rbhdjkModelManage.setLineColor(lineColor);
                rbhdjkModelManage.setIsCreateImg(isCreateImg);
                rbhdjkModelManage.setSendFileName(sendFileName);

                rbhdjkModelManageMapper.updateById(rbhdjkModelManage);

            }else{
                //保存数据记录
                rbhdjkModelManage = new HdjkModelManage();
                rbhdjkModelManage.setModelName(fileName);
                rbhdjkModelManage.setCreateTime(LocalDateTime.now());
                rbhdjkModelManage.setModelSourcePath(filePath + fileName);
                //rbhdjkModelManage.setModelTargetPath(rbhdjkTargetPath +fileName);
                rbhdjkModelManage.setModelArea(qyValue);
                rbhdjkModelManage.setCreateUserId(currentUser.getUserId());
                //pptModelConfig.setCreateUserName(currentUser.getUsername());
                rbhdjkModelManage.setCreateUserName(currentUser.getRealName());
                rbhdjkModelManage.setFileSize(fileSize);
                rbhdjkModelManage.setModelType(modelType);
                rbhdjkModelManage.setTableName(tableName);
                rbhdjkModelManage.setModelParam(modelParam);
                rbhdjkModelManage.setBackgroundColor(backgroundColor);
                rbhdjkModelManage.setLineColor(lineColor);
                rbhdjkModelManage.setIsCreateImg(isCreateImg);
                rbhdjkModelManage.setSendFileName(sendFileName);

                rbhdjkModelManageMapper.insert(rbhdjkModelManage);

            }

        } catch (IllegalStateException e) {
            e.printStackTrace();
            isResult = false;
            isSuccess ="-1";
            if(e.getMessage() != null){
                errorMsg = e.getMessage().substring(0,1000);
            }

        } catch (IOException e) {
            e.printStackTrace();
            isResult = false;
            isSuccess ="-1";
            if(e.getMessage() != null){
                errorMsg = e.getMessage().substring(0,1000);
            }
        }finally {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            //保存日志信息
            HdjkLog rbhdjkLog = new HdjkLog();
            rbhdjkLog.setModelName(modelName);
            rbhdjkLog.setLogType("add");
            rbhdjkLog.setLogTime(LocalDateTime.now());
            rbhdjkLog.setModelSourcePath(modelSourcePath);
            rbhdjkLog.setCreateUserId(currentUser.getUserId());
            rbhdjkLog.setCreateUserName(currentUser.getRealName());
            rbhdjkLog.setModelArea(modelRange);
            rbhdjkLog.setFileSize(fileSize);
            rbhdjkLog.setTableName(tableName);
            rbhdjkLog.setModelParam(modelParam);
            rbhdjkLog.setIsSuccess(isSuccess);
            rbhdjkLog.setLogMsg(errorMsg);
            rbhdjkLog.setModelType(modelType);

            rbhdjkLogMapper.insert(rbhdjkLog);

            try {
                if(inputStream != null){
                    inputStream.close();
                }
                if(outputStream != null){
                    outputStream.close();
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        return isResult;
    }

    /**
     * 根据模板ID删除模板信息
     * @param modelId
     * @return
     */
    @Override
    public boolean modelRemove(int modelId) {
        boolean result = false;
        String isSuccess = "-1";
        String errMsg ="删除失败";
        HdjkModelManage rbhdjkModelManage = null;
        try {
            rbhdjkModelManage = rbhdjkModelManageMapper.selectById(modelId);
            if(rbhdjkModelManage != null){
                int resCount = rbhdjkModelManageMapper.deleteById(modelId);
                if(resCount >0){
                    //删除IM日报文件推送管理中的记录
                    QueryWrapper<ImHdjkPushManage> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("model_id", modelId);
                    imRbFilePushManageMapper.delete(queryWrapper);

                    result = true;
                    isSuccess = "0";
                    errMsg ="删除成功";
                }else{
                    result = false;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            if(e.getMessage() != null && e.getMessage().length() >1000){
                errMsg = errMsg.substring(0,1000);
            }
        }finally {
            if(rbhdjkModelManage != null){

                //当前登录人信息
                SysUser currentUser = UserContext.currentUser();

                //保存日志信息
                HdjkLog rbhdjkLog = new HdjkLog();
                rbhdjkLog.setModelName(rbhdjkModelManage.getModelName());
                rbhdjkLog.setLogType("del");
                rbhdjkLog.setLogTime(LocalDateTime.now());
                rbhdjkLog.setModelSourcePath(rbhdjkModelManage.getModelSourcePath());
                rbhdjkLog.setCreateUserId(currentUser.getUserId());
                rbhdjkLog.setCreateUserName(currentUser.getRealName());
                rbhdjkLog.setModelArea(rbhdjkModelManage.getModelArea());
                rbhdjkLog.setFileSize(rbhdjkModelManage.getFileSize());
                rbhdjkLog.setTableName(rbhdjkModelManage.getTableName());
                rbhdjkLog.setModelParam(rbhdjkModelManage.getModelParam());
                rbhdjkLog.setIsSuccess(isSuccess);
                rbhdjkLog.setLogMsg(errMsg);
                rbhdjkLog.setModelType(rbhdjkModelManage.getModelType());

                rbhdjkLogMapper.insert(rbhdjkLog);
            }
        }

        return result;

    }


    /**
     * 文件下载
     * @param modelId
     * @return
     */
    @Override
    public ResponseEntity<InputStreamResource> downloadFile(int modelId) {
        String isSuccess = "0";
        String errMsg ="";
        try {
            log.info("进入下载方法... modelId: {}",modelId);

            //通过模板ID查询模板信息
            HdjkModelManage rbhdjkModelManage = rbhdjkModelManageMapper.selectById(modelId);
            if(rbhdjkModelManage != null){
                //读取文件
                String filePath = rbhdjkModelManage.getModelSourcePath();

                FileSystemResource file = new FileSystemResource(filePath);

                /*String encodedFilename = new String(file.getFilename().getBytes("UTF-8"), "ISO-8859-1");

                log.info("进入下载方法... encodedFilename: {}",encodedFilename);

                //设置响应头
                HttpHeaders headers = new HttpHeaders();
                headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                //headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", URLEncoder.encode(file.getFilename(),"UTF-8")));
                //headers.add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getFilename(), "UTF-8"));
                headers.add("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
                headers.add("Pragma", "no-cache");
                headers.add("Expires", "0");

                return ResponseEntity
                        .ok()
                        .headers(headers)
                        .contentLength(file.contentLength())
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .body(new InputStreamResource(file.getInputStream()));*/

                HttpHeaders headers = new HttpHeaders();
                // 确保不缓存
                headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.add("Pragma", "no-cache");
                headers.add("Expires", "0");

                // 处理特殊字符的文件名
                String encodedFileName = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8.toString());
                log.info("进入下载方法... encodedFilename: {}",encodedFileName);
                headers.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

                try {
                    return ResponseEntity
                            .ok()
                            .headers(headers)
                            .contentLength(file.contentLength())
                            .contentType(MediaType.parseMediaType("application/octet-stream"))
                            .body(new InputStreamResource(file.getInputStream()));
                }catch (Exception ex){
                    ex.printStackTrace();
                    isSuccess = "-1";
                    if(ex.getMessage() != null){
                        errMsg = ex.getMessage().substring(0,1000);
                    }
                    return null;
                }

            }else{
                return  null;
            }

        }catch (Exception e){
            e.printStackTrace();
            isSuccess = "-1";
            if(e.getMessage() != null){
                errMsg = e.getMessage().substring(0,1000);
            }
            return null;
        }finally {
            //保存下载记录
            /*if(id != null && type.equals("target")){
                PptDownloadData pptDownloadData = pptDownloadDataMapper.selectById(id);
                if(pptDownloadData != null){
                    //获取当前登录用户信息
                    SysUser currentUser = UserContext.currentUser();
                    PptDownloadLog pptDownloadLog = new PptDownloadLog();
                    pptDownloadLog.setFileId(id);
                    pptDownloadLog.setFileName(pptDownloadData.getModelName());
                    pptDownloadLog.setCreateUserId(pptDownloadData.getCreateUserId());
                    pptDownloadLog.setCreateUserName(pptDownloadData.getCreateUserName());
                    pptDownloadLog.setCreateTime(pptDownloadData.getCreateTime());
                    pptDownloadLog.setDownloadUserId(currentUser.getUserId());
                    //pptDownloadLog.setDownloadUserName(currentUser.getUsername());
                    pptDownloadLog.setDownloadUserName(currentUser.getRealName());
                    pptDownloadLog.setDownloadTime(LocalDateTime.now());
                    pptDownloadLog.setModelTargetPath(pptDownloadData.getModelTargetPath());
                    pptDownloadLog.setModelLevel(pptDownloadData.getModelLevel());
                    pptDownloadLog.setFileSize(pptDownloadData.getFileSize());
                    pptDownloadLog.setIsSuccess(isSuccess);
                    pptDownloadLog.setErrorMsg(errMsg);

                    pptDownloadLogMapper.insert(pptDownloadLog);
                }

            }*/
        }
    }

    /**
     * 获取模板列表
     * @return
     */
    @Override
    public List<PptModelConfig> getModelList() {
        //通过模板ID查询模板信息
        QueryWrapper<PptModelConfig> modelQu = new QueryWrapper<>();
        //modelQu.select("model_id", "model_name");
        modelQu.orderByDesc("create_time");
        List<PptModelConfig> pptModelConfigList = pptModelConfigMapper.selectList(modelQu);

        /*List<JSONObject> list = new ArrayList<>();
        if(pptModelConfigList !=null && pptModelConfigList.size() >0){
            for(PptModelConfig pptModelConfig : pptModelConfigList){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("model_id",pptModelConfig.getModelId());
                jsonObject.put("model_name",pptModelConfig.getModelName());
                list.add(jsonObject);
            }
        }*/

        return pptModelConfigList;
    }

    /**
     * 创建文件
     * @param modelId
     * @return
     */
    @Override
    public boolean createFile(int modelId) {
        boolean isResult = false;
        String createFileRetMsg ="";
        HdjkModelManage rbhdjkModelManage = null;
        try {
            rbhdjkModelManage = rbhdjkModelManageMapper.selectById(modelId);
            if(rbhdjkModelManage != null){
                String sourFilePath = rbhdjkModelManage.getModelSourcePath();
                String tableName = rbhdjkModelManage.getTableName();
                String modelType = rbhdjkModelManage.getModelType();
                String backgroundColor = rbhdjkModelManage.getBackgroundColor() ==null?"":rbhdjkModelManage.getBackgroundColor();
                String lineColor = rbhdjkModelManage.getLineColor() ==null?"":rbhdjkModelManage.getLineColor();
                String isCreateImg = rbhdjkModelManage.getIsCreateImg() ==null?"":rbhdjkModelManage.getIsCreateImg();
                String sendFileName = rbhdjkModelManage.getSendFileName() ==null?"":rbhdjkModelManage.getSendFileName();

                String acctDay ="";
                //类型为空或者1（日报）
                if(StringUtil.isEmpty(modelType) || modelType.equals("1")){
                    acctDay = DateTimeUtil.getYesterday("yyyyMMdd");
                }else if(modelType.equals("2")){ //月报
                    acctDay = DateTimeUtil.getLastMonth("yyyyMM");
                }

                //String outputFilePath = hdjkTargetPath+suffName+".xlsx";

                String modelName = rbhdjkModelManage.getModelName();
                String suffName = modelName.substring(0,modelName.lastIndexOf("."));
                suffName = suffName+"_"+ acctDay;


                String outputFilePath = "";
                if(StringUtil.isNotEmpty(sendFileName)){

                        sendFileName = sendFileName.replaceAll(".xlsx","");
                        sendFileName = sendFileName.replaceAll(".xls","");
                        outputFilePath = hdjkTargetPath+sendFileName+"_"+acctDay+".xlsx";

                }else{
                    outputFilePath = hdjkTargetPath+suffName+".xlsx";
                }


                String modelParam = rbhdjkModelManage.getModelParam();
                if(StringUtil.isNotEmpty(modelParam)){
                    int dataCount =0 ;
                    Map<String,List<Map<String,Object>>> dataListMap = new HashMap<>();
                    String modelParamArr[] = modelParam.split(";");
                    for(int i=0;i<modelParamArr.length;i++){
                        String subModelParam = modelParamArr[i];
                        subModelParam = subModelParam.trim();
                        /*if(tableName.equals("gz_zyz_gjz_mkt")){
                            //查询数据库，获取数据信息
                            List<Map<String,Object>> dataList = rbConnSecondTableService.zyzGjzMkt(subModelParam);
                            if(dataList != null && dataList.size() >0){
                                dataListMap.put("xh"+(i+1),dataList);
                            }else{
                                return false;
                            }
                        }*/

                        //查询数据库，获取数据信息
                        List<Map<String,Object>> dataList = rbConnSecondTableService.zyzGjzMkt(tableName,subModelParam,acctDay);
                        if(dataList != null && dataList.size() >0){
                            dataListMap.put("xh"+(i+1),dataList);
                            dataCount = dataCount + dataList.size();
                        }else{
                            log.info("++++++hdjkModelManage_createFile 根据账期{}查询数据库信息为空 ",acctDay);
                            return false;
                        }

                    }

                    log.info("++++++hdjkModelManage_dataCount: {}",dataCount);

                    /*boolean creatFileReslut = false;
                    if(dataCount >0 && dataCount <=hdjkCreateFileMaxData){
                        creatFileReslut = RbExcelUtil.createExcelFile2(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                    }else if(dataCount > hdjkCreateFileMaxData){
                        creatFileReslut = RbExcelUtil.createBigDataExcelFile(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                    }*/

                    //boolean creatFileReslut = RbExcelUtil.createBigDataExcelFile(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                    JSONObject createFileResultAuto = RbExcelUtil.createBigDataExcelFileAuto(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                    log.info("++++++createFileResultAuto: "+createFileResultAuto);
                    String creatFileRetCode = createFileResultAuto.getString("retCode")==null?"":createFileResultAuto.getString("retCode");

                    if(creatFileRetCode.equals("0")){
                        String imgOutputFilePath = "";
                        //创建图片
                        if(isCreateImg.equals("1")){
                            String imgSourFilePath = outputFilePath;

                           /* String jpegName = suffName + ".png";
                            //String jpegName = suffName + ".jpeg";
                            imgOutputFilePath = hdjkTargetPath + jpegName;*/

                            if(StringUtil.isNotEmpty(sendFileName)){

                                    sendFileName = sendFileName.replaceAll(".xlsx","");
                                    sendFileName = sendFileName.replaceAll(".xls","");
                                    imgOutputFilePath = hdjkTargetPath+sendFileName+"_"+acctDay+".png";

                            }else{
                                imgOutputFilePath = hdjkTargetPath+suffName+".png";
                            }

                            boolean toImgResult = false;
                            if(dataCount >0 && dataCount <=hdjkCreateFileMaxData){
                                toImgResult = RbExcelUtil.excelToImg(imgSourFilePath,imgOutputFilePath);
                            }else if(dataCount >hdjkCreateFileMaxData){
                                toImgResult = RbExcelUtil.excelBigDataToImg(imgSourFilePath,imgOutputFilePath);
                            }

                            log.info("++++++toImgResult: "+toImgResult);

                        }

                        //根据模板ID查询日报IM推送文件配置信息
                        QueryWrapper<ImHdjkPushManage> filePushQu = new QueryWrapper<>();
                        filePushQu.eq("model_id",modelId);

                        ImHdjkPushManage imRbFilePushManage = imRbFilePushManageMapper.selectOne(filePushQu);
                        if(imRbFilePushManage != null){
                            //更新IM推送文件管理表
                            imRbFilePushManage.setFileName(suffName+".xlsx");
                            imRbFilePushManage.setFileCreatTime(LocalDateTime.now());
                            imRbFilePushManage.setFilePath(outputFilePath);
                            imRbFilePushManage.setImgPath(imgOutputFilePath);
                            imRbFilePushManage.setFilePushTime(null);
                            int upResult = imRbFilePushManageMapper.updateById(imRbFilePushManage);
                            /*String isAutoPush = imRbFilePushManage.getIsAutoPush();
                            if(isAutoPush.equals("1") && upResult >0){ //自动推送
                                JSONObject pushParam = new JSONObject();
                                pushParam.put("manageId",imRbFilePushManage.getManageId());
                                //JSONObject filePushObject = rbFilePushManageService.push(pushParam.toString());
                                JSONObject filePushObject = rbFilePushManageService.pushNew(pushParam.toString());
                                log.info("++++++++++++++hdjk_createFile_filePushObject: {}",filePushObject);
                            }*/
                        }

                        rbhdjkModelManage.setFileCreateTime(LocalDateTime.now());
                        rbhdjkModelManage.setModelTargetPath(outputFilePath);
                        rbhdjkModelManageMapper.updateById(rbhdjkModelManage);

                        isResult = true;
                    }else{
                        createFileRetMsg = createFileResultAuto.getString("retMsg")==null?"":createFileResultAuto.getString("retMsg");
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(rbhdjkModelManage != null){
                //当前登录人信息
                SysUser currentUser = UserContext.currentUser();
                if(currentUser ==null){
                    //取管理员信息
                    currentUser = sysUserService.getById("1309861917694623744");
                }

                //保存日志信息
                HdjkLog rbhdjkLog = new HdjkLog();
                rbhdjkLog.setModelName(rbhdjkModelManage.getModelName());
                rbhdjkLog.setLogType("create");
                rbhdjkLog.setLogTime(LocalDateTime.now());
                rbhdjkLog.setModelSourcePath(rbhdjkModelManage.getModelSourcePath());
                rbhdjkLog.setCreateUserId(currentUser.getUserId());
                rbhdjkLog.setCreateUserName(currentUser.getRealName());
                rbhdjkLog.setModelArea(rbhdjkModelManage.getModelArea());
                rbhdjkLog.setFileSize(rbhdjkModelManage.getFileSize());
                rbhdjkLog.setTableName(rbhdjkModelManage.getTableName());
                rbhdjkLog.setModelParam(rbhdjkModelManage.getModelParam());
                rbhdjkLog.setModelType(rbhdjkModelManage.getModelType());
                if(isResult){
                    rbhdjkLog.setIsSuccess("0");
                    rbhdjkLog.setLogMsg("文件生成成功");
                }else{
                    rbhdjkLog.setIsSuccess("-1");
                    rbhdjkLog.setLogMsg("文件生成失败:"+createFileRetMsg);
                }

                rbhdjkLogMapper.insert(rbhdjkLog);
            }
        }
        return isResult;
    }

    /**
     * 批量创建文件
     * @param param
     * @return
     */
    @Override
    public boolean batchCreateFile(String param) {
        boolean isResult = false;
        String creatFileRetMsg ="";
        try {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();
            JSONObject jsonParam = JSON.parseObject(param);
            String modelIds = jsonParam.getString("modelIds");
            String[] modelIdArr = modelIds.split(",");
            for(String modelId : modelIdArr){
                int iModelId = Integer.parseInt(modelId);
                HdjkModelManage rbhdjkModelManage = rbhdjkModelManageMapper.selectById(iModelId);
                if(rbhdjkModelManage != null){
                    String sourFilePath = rbhdjkModelManage.getModelSourcePath();
                    String tableName = rbhdjkModelManage.getTableName();
                    String modelType = rbhdjkModelManage.getModelType();
                    String backgroundColor = rbhdjkModelManage.getBackgroundColor() ==null?"":rbhdjkModelManage.getBackgroundColor();
                    String lineColor = rbhdjkModelManage.getLineColor() ==null?"":rbhdjkModelManage.getLineColor();
                    String isCreateImg = rbhdjkModelManage.getIsCreateImg() ==null?"":rbhdjkModelManage.getIsCreateImg();
                    String sendFileName = rbhdjkModelManage.getSendFileName() ==null?"":rbhdjkModelManage.getSendFileName();

                    String acctDay ="";
                    //类型为空或者1（日报）
                    if(StringUtil.isEmpty(modelType) || modelType.equals("1")){
                        acctDay = DateTimeUtil.getYesterday("yyyyMMdd");
                    }else if(modelType.equals("2")){ //月报
                        acctDay = DateTimeUtil.getLastMonth("yyyyMM");
                    }

                    String modelName = rbhdjkModelManage.getModelName();
                    String suffName = modelName.substring(0,modelName.lastIndexOf("."));

                    /*suffName = suffName+ DateTimeUtil.getYesterday("MMdd");
                    String outputFilePath = hdjkTargetPath+suffName+".xlsx";*/

                    suffName = suffName+"_"+ acctDay;
                    String outputFilePath = "";
                    if(StringUtil.isNotEmpty(sendFileName)){

                            sendFileName = sendFileName.replaceAll(".xlsx","");
                            sendFileName = sendFileName.replaceAll(".xls","");
                            outputFilePath = hdjkTargetPath+sendFileName+"_"+acctDay+".xlsx";

                    }else{
                        outputFilePath = hdjkTargetPath+suffName+".xlsx";
                    }


                    String modelParam = rbhdjkModelManage.getModelParam();
                    if(StringUtil.isNotEmpty(modelParam)){
                        int dataCount =0 ;
                        Map<String,List<Map<String,Object>>> dataListMap = new HashMap<>();
                        String modelParamArr[] = modelParam.split(";");
                        for(int i=0;i<modelParamArr.length;i++){
                            String subModelParam = modelParamArr[i];
                            /*if(tableName.equals("gz_zyz_gjz_mkt")){
                                //查询数据库，获取数据信息
                                List<Map<String,Object>> dataList = rbConnSecondTableService.zyzGjzMkt(tableName,subModelParam);
                                if(dataList != null && dataList.size() >0){
                                    dataListMap.put("xh"+(i+1),dataList);
                                }else{
                                    return false;
                                }
                            }*/

                            //查询数据库，获取数据信息
                            List<Map<String,Object>> dataList = rbConnSecondTableService.zyzGjzMkt(tableName,subModelParam,acctDay);
                            if(dataList != null && dataList.size() >0){
                                dataListMap.put("xh"+(i+1),dataList);
                                dataCount = dataCount + dataList.size();
                            }else{
                                log.info("++++++batchCreateFile查询账期["+acctDay+"]类型["+subModelParam+"]数据为空");
                                return false;
                            }

                        }

                        /*boolean creatFileReslut = false;
                        if(dataCount >0 && dataCount <=hdjkCreateFileMaxData){
                            creatFileReslut = RbExcelUtil.createExcelFile2(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                        }else if(dataCount >hdjkCreateFileMaxData){
                            creatFileReslut = RbExcelUtil.createBigDataExcelFile(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                        }*/

                        //boolean creatFileReslut = RbExcelUtil.createBigDataExcelFile(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                        JSONObject createFileResultAuto = RbExcelUtil.createBigDataExcelFileAuto(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                        log.info("++++++createFileResultAuto: "+createFileResultAuto);
                        String creatFileRetCode = createFileResultAuto.getString("retCode") ==null?"":createFileResultAuto.getString("retCode");

                        if(creatFileRetCode.equals("0")){

                            String imgOutputFilePath = "";
                            if(isCreateImg.equals("1")){
                                String imgSourFilePath = outputFilePath;

                                //String jpegName = suffName + ".png";

                                if(StringUtil.isNotEmpty(sendFileName)){

                                        sendFileName = sendFileName.replaceAll(".xlsx","");
                                        sendFileName = sendFileName.replaceAll(".xls","");
                                        imgOutputFilePath = hdjkTargetPath+sendFileName+"_"+acctDay+".png";

                                }else{
                                    imgOutputFilePath = hdjkTargetPath+suffName+".png";
                                }

                                boolean toImgResult = false;
                                if(dataCount >0 && dataCount <= hdjkCreateFileMaxData){
                                    toImgResult = RbExcelUtil.excelToImg(imgSourFilePath,imgOutputFilePath);
                                }else if(dataCount > hdjkCreateFileMaxData){
                                    toImgResult = RbExcelUtil.excelBigDataToImg(imgSourFilePath,imgOutputFilePath);
                                }

                                log.info("++++++toImgResult: "+toImgResult);
                            }


                            //根据模板ID查询日报IM推送文件配置信息
                            QueryWrapper<ImHdjkPushManage> filePushQu = new QueryWrapper<>();
                            filePushQu.eq("model_id",modelId);

                            ImHdjkPushManage imRbFilePushManage = imRbFilePushManageMapper.selectOne(filePushQu);
                            if(imRbFilePushManage != null){
                                //更新IM推送文件管理表
                                imRbFilePushManage.setFileName(suffName+".xlsx");
                                imRbFilePushManage.setFileCreatTime(LocalDateTime.now());
                                imRbFilePushManage.setFilePath(outputFilePath);
                                imRbFilePushManage.setImgPath(imgOutputFilePath);
                                imRbFilePushManage.setFilePushTime(null);
                                int upResult = imRbFilePushManageMapper.updateById(imRbFilePushManage);
                                /*String isAutoPush = imRbFilePushManage.getIsAutoPush();
                                if(isAutoPush.equals("1") && upResult >0){ //自动推送
                                    JSONObject pushParam = new JSONObject();
                                    pushParam.put("manageId",imRbFilePushManage.getManageId());
                                    *//*JSONObject filePushObject = rbFilePushManageService.push(pushParam.toString());*//*
                                    JSONObject filePushObject = rbFilePushManageService.pushNew(pushParam.toString());
                                    log.info("++++++++++++++Rb_batchCreateFile_filePushObject: {}",filePushObject);
                                }*/
                            }

                            rbhdjkModelManage.setFileCreateTime(LocalDateTime.now());
                            rbhdjkModelManage.setModelTargetPath(outputFilePath);
                            rbhdjkModelManageMapper.updateById(rbhdjkModelManage);

                            isResult = true;
                        }else{
                            creatFileRetMsg = createFileResultAuto.getString("retMsg") ==null?"":createFileResultAuto.getString("retMsg");
                            return false;
                        }

                    }else{
                        return false;
                    }

                    //保存日志信息
                    HdjkLog rbhdjkLog = new HdjkLog();
                    rbhdjkLog.setModelName(rbhdjkModelManage.getModelName());
                    rbhdjkLog.setLogType("create");
                    rbhdjkLog.setLogTime(LocalDateTime.now());
                    rbhdjkLog.setModelSourcePath(rbhdjkModelManage.getModelSourcePath());
                    rbhdjkLog.setCreateUserId(currentUser.getUserId());
                    rbhdjkLog.setCreateUserName(currentUser.getRealName());
                    rbhdjkLog.setModelArea(rbhdjkModelManage.getModelArea());
                    rbhdjkLog.setFileSize(rbhdjkModelManage.getFileSize());
                    rbhdjkLog.setTableName(rbhdjkModelManage.getTableName());
                    rbhdjkLog.setModelParam(rbhdjkModelManage.getModelParam());
                    rbhdjkLog.setModelType(rbhdjkModelManage.getModelType());
                    if(isResult){
                        rbhdjkLog.setIsSuccess("0");
                        rbhdjkLog.setLogMsg("文件生成成功");
                    }else{
                        rbhdjkLog.setIsSuccess("-1");
                        rbhdjkLog.setLogMsg("文件生成失败: "+creatFileRetMsg);
                    }

                    rbhdjkLogMapper.insert(rbhdjkLog);

                }else{
                    return false;
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return isResult;
    }

    /**
     * 在线预览
     * @param param
     * @param response
     * @return
     */
    @Override
    public JSONObject fileView(String param, HttpServletResponse response) {
        JSONObject retJson  = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            int modelId = paramJson.getInteger("modelId");
            HdjkModelManage rbhdjkModelManage = rbhdjkModelManageMapper.selectById(modelId);
            if(rbhdjkModelManage != null){

                String path = rbhdjkModelManage.getModelSourcePath();
                String fileName = rbhdjkModelManage.getModelName();
                String pdfPath = hdjkExcelToPdfPath;
                FileToPdfUtils.officeToPdf(path,pdfPath);
                String port = env.getProperty("server.port");
                String serverIp = env.getProperty("server.server-ip");

                String pdfName = fileName.substring(0,fileName.lastIndexOf(".")+1)+"pdf";
                String retPdfPath =serverIp+":"+port+"/hdjkExcelToPdf/"+pdfName;

                retJson.put("retCode","0");
                retJson.put("retMsg",retPdfPath);
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","未查询到文件信息！");
            }

        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常！");
        }

        return retJson;
    }


    /**
     * 根据ID查询活动监控信息
     * @param modelId
     * @return
     */
    @Override
    public HdjkModelManage getById(int modelId) {
        HdjkModelManage hdjkModelManage = rbhdjkModelManageMapper.selectById(modelId);
        return hdjkModelManage;
    }


    /**
     * 活动监控修改
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
    @Override
    public boolean editHdjkMb(MultipartFile file, String qyValue, String tableName, String modelType, String modelParam, String backgroundColor, String lineColor, String isCreateImg, int modelId,String sendFileName) {
        boolean isResult = true;
        if (file.isEmpty()) {
            return false;
        }

        String modelName ="";
        String createUserId = "";
        String createUserName = "";
        String modelSourcePath ="";
        String modelTargetPath ="";
        String modelRange =qyValue;
        String fileSize ="";
        String isSuccess ="0";
        String errorMsg ="模板修改成功";

        InputStream inputStream =null;
        FileOutputStream outputStream =null;
        try {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            // 获取文件名
            String fileName = file.getOriginalFilename();
            log.info("上传的文件名为：" + fileName);

            modelName = fileName;
            modelSourcePath = hdjkUploadPath + fileName;
            modelTargetPath = hdjkTargetPath +fileName;


            // 获取文件的后缀名
            String suffixName = fileName.substring(fileName.lastIndexOf("."));
            log.info("上传的后缀名为：" + suffixName);

            double dFileSize = file.getSize();
            BigDecimal value = new BigDecimal(dFileSize);
            BigDecimal divisor = new BigDecimal(1024);

            // 除法运算，并设置保留两位小数，以及舍入模式
            fileSize = value.divide(divisor, 1, RoundingMode.HALF_UP).toString()+"KB";

            // 文件上传路径
            String filePath = hdjkUploadPath;

            // 解决中文问题，liunx下中文路径，图片显示问题
            // fileName = UUID.randomUUID() + suffixName;

            File dest = new File(filePath + fileName);

            // 检测是否存在目录
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }

            file.transferTo(dest);


            //根据ID查询模板信息

            HdjkModelManage rbhdjkModelManage = rbhdjkModelManageMapper.selectById(modelId);

            if(rbhdjkModelManage != null){
                rbhdjkModelManage.setCreateTime(LocalDateTime.now());
                rbhdjkModelManage.setCreateUserId(currentUser.getUserId());
                //pptModelConfig.setCreateUserName(currentUser.getUsername());
                rbhdjkModelManage.setCreateUserName(currentUser.getRealName());
                rbhdjkModelManage.setFileSize(fileSize);
                rbhdjkModelManage.setModelArea(qyValue);
                rbhdjkModelManage.setModelType(modelType);
                rbhdjkModelManage.setModelSourcePath(filePath + fileName);
                //rbhdjkModelManage.setModelTargetPath(rbhdjkTargetPath +fileName);
                rbhdjkModelManage.setTableName(tableName);
                rbhdjkModelManage.setModelParam(modelParam);
                rbhdjkModelManage.setBackgroundColor(backgroundColor);
                rbhdjkModelManage.setLineColor(lineColor);
                rbhdjkModelManage.setIsCreateImg(isCreateImg);
                rbhdjkModelManage.setModelName(fileName);
                rbhdjkModelManage.setSendFileName(sendFileName);

                int updateResult = rbhdjkModelManageMapper.updateById(rbhdjkModelManage);
                if(updateResult >0){
                    QueryWrapper<ImHdjkPushManage> imHdjkPushManageQueryWrapper = new QueryWrapper<>();
                    imHdjkPushManageQueryWrapper.eq("model_id",modelId);
                    imHdjkPushManageQueryWrapper.last("limit 1");

                    ImHdjkPushManage imHdjkPushManage= imHdjkPushManageMapper.selectOne(imHdjkPushManageQueryWrapper);
                    if(imHdjkPushManage != null){
                        imHdjkPushManage.setModelName(fileName);
                        imHdjkPushManageMapper.updateById(imHdjkPushManage);
                    }
                }

            }else{
                //保存数据记录
                rbhdjkModelManage = new HdjkModelManage();
                rbhdjkModelManage.setModelName(fileName);
                rbhdjkModelManage.setCreateTime(LocalDateTime.now());
                rbhdjkModelManage.setModelSourcePath(filePath + fileName);
                //rbhdjkModelManage.setModelTargetPath(rbhdjkTargetPath +fileName);
                rbhdjkModelManage.setModelArea(qyValue);
                rbhdjkModelManage.setCreateUserId(currentUser.getUserId());
                //pptModelConfig.setCreateUserName(currentUser.getUsername());
                rbhdjkModelManage.setCreateUserName(currentUser.getRealName());
                rbhdjkModelManage.setFileSize(fileSize);
                rbhdjkModelManage.setModelType(modelType);
                rbhdjkModelManage.setTableName(tableName);
                rbhdjkModelManage.setModelParam(modelParam);
                rbhdjkModelManage.setBackgroundColor(backgroundColor);
                rbhdjkModelManage.setLineColor(lineColor);
                rbhdjkModelManage.setIsCreateImg(isCreateImg);
                rbhdjkModelManage.setSendFileName(sendFileName);

                rbhdjkModelManageMapper.insert(rbhdjkModelManage);

            }

        } catch (IllegalStateException e) {
            e.printStackTrace();
            isResult = false;
            isSuccess ="-1";
            if(e.getMessage() != null){
                errorMsg = e.getMessage().substring(0,1000);
            }

        } catch (IOException e) {
            e.printStackTrace();
            isResult = false;
            isSuccess ="-1";
            if(e.getMessage() != null){
                errorMsg = e.getMessage().substring(0,1000);
            }
        }finally {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            //保存日志信息
            HdjkLog rbhdjkLog = new HdjkLog();
            rbhdjkLog.setModelName(modelName);
            rbhdjkLog.setLogType("edit");
            rbhdjkLog.setLogTime(LocalDateTime.now());
            rbhdjkLog.setModelSourcePath(modelSourcePath);
            rbhdjkLog.setCreateUserId(currentUser.getUserId());
            rbhdjkLog.setCreateUserName(currentUser.getRealName());
            rbhdjkLog.setModelArea(modelRange);
            rbhdjkLog.setFileSize(fileSize);
            rbhdjkLog.setTableName(tableName);
            rbhdjkLog.setModelParam(modelParam);
            rbhdjkLog.setIsSuccess(isSuccess);
            rbhdjkLog.setLogMsg(errorMsg);
            rbhdjkLog.setModelType(modelType);

            rbhdjkLogMapper.insert(rbhdjkLog);

            try {
                if(inputStream != null){
                    inputStream.close();
                }
                if(outputStream != null){
                    outputStream.close();
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        return isResult;
    }

}
