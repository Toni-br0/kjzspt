package com.pearadmin.modules.job.schedule;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.quartz.base.BaseQuartz;
import com.pearadmin.common.tools.DateTimeUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.hdjk.domain.HdjkLog;
import com.pearadmin.modules.hdjk.domain.HdjkModelManage;
import com.pearadmin.modules.hdjk.mapper.HdjkLogMapper;
import com.pearadmin.modules.hdjk.mapper.HdjkModelManageMapper;
import com.pearadmin.modules.hdjk.service.impl.HdjkConnSecondTableServiceImpl;
import com.pearadmin.modules.hdjk.util.RbExcelUtil;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import com.pearadmin.modules.im.mapper.ImHdjkPushManageMapper;
import com.pearadmin.modules.im.service.impl.HdjkFilePushManageServiceImpl;
import com.pearadmin.modules.report.domain.ReportIndexWarning;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * 创建日期：2025-12-25
 * 活动监控定时任务（新）
 *
 **/

@Slf4j
@Component("hdjkCreateNewTask")
public class HdjkCreateNewTask  implements BaseQuartz {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${hdjk-target-path}")
    private String hdjkTargetPath;

    @Value("${hdjk-createFile-maxData}")
    private int hdjkCreateFileMaxData;

    @Resource
    private ImHdjkPushManageMapper imHdjkPushManageMapper;

    @Resource
    private HdjkModelManageMapper hdjkModelManageMapper;

    @Resource
    private HdjkConnSecondTableServiceImpl hdjkConnSecondTableService;

    @Resource
    private HdjkFilePushManageServiceImpl hdjkFilePushManageService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private HdjkLogMapper hdjkLogMapper;


    @Override
    public void run(String params) throws Exception {

        log.info("++++++++++++++++活动监控定时任务开始执行");
        log.info("活动监控入参 === >> " + params);

        LocalDateTime now = LocalDateTime.now();

        // 获取时间信息
        String currentDay = now.format(DAY_FORMATTER);
        String currentHm = now.format(TIME_FORMATTER);
        DayOfWeek currentWeek = now.getDayOfWeek();

        // 计算查询时间范围
        int minutesBefore = Integer.parseInt(params);
        String startTime = now.minusMinutes(minutesBefore).format(TIME_FORMATTER);

        //根据开始时间和结束时间查询数据
        List<ImHdjkPushManage> imHdjkPushManageList  = imHdjkPushManageMapper.getListByTime(startTime,currentHm);
        if(CollectionUtils.isNotEmpty(imHdjkPushManageList)){
            int batchSize = 10; // 每批处理数量
            int total = imHdjkPushManageList.size();

            for (int i = 0; i < total; i += batchSize) {
                int end = Math.min(i + batchSize, total);
                List<ImHdjkPushManage> batch = imHdjkPushManageList.subList(i, end);

                batch.parallelStream().forEach(imHdjkPushManage -> processSingleImHdjkPush(imHdjkPushManage, currentDay, currentWeek.toString()));
            }
        }

    }

    private void processSingleImHdjkPush(ImHdjkPushManage imHdjkPushManage, String currentDay, String currentWeek) {
        try {
            String execPeriod = imHdjkPushManage.getPushObjectPeriod() ==null?"":imHdjkPushManage.getPushObjectPeriod();
            // 校验执行条件
            if(execPeriod.equals("month")){//月
                String execDay = imHdjkPushManage.getSendObjectDay()==null?"":imHdjkPushManage.getSendObjectDay();
                if(!execDay.equals(currentDay)){ //发送日期不是当天
                    return;
                }
            }else if(execPeriod.equals("week")){//周
                String exeWeek = imHdjkPushManage.getSendObjectWeek()==null?"":imHdjkPushManage.getSendObjectWeek();
                if(!currentWeek.equals(exeWeek)){
                    return;
                }
            }

            //周末是否推送
            String isWeekendPush = imHdjkPushManage.getIsWeekendPush()==null?"":imHdjkPushManage.getIsWeekendPush();
            if(isWeekendPush.equals("0")){ //周末不推送
                if(currentWeek.equals("SATURDAY") || currentWeek.equals("SUNDAY")){
                    return;
                }
            }

            // 更新为执行中状态
            updateImHdjkPushState(imHdjkPushManage, "1");

            // 执行核心业务
            exeCreateFileSendIm(imHdjkPushManage);

            // 恢复为未执行状态
            updateImHdjkPushState(imHdjkPushManage, "0");

        } catch (Exception e) {
            log.error("处理单个活动监控失败, ID: {}", imHdjkPushManage.getManageId(), e);
            // 异常时恢复状态
            updateImHdjkPushState(imHdjkPushManage, "0");
        }
    }

    /**
     * 更新状态
     * @param imHdjkPushManage
     * @param state
     */
    private void updateImHdjkPushState(ImHdjkPushManage imHdjkPushManage, String state) {
        imHdjkPushManage.setExecState(state);
        imHdjkPushManageMapper.updateById(imHdjkPushManage);
    }

    /**
     * 生成文件并发送至IM
     * @param imHdjkPushManage
     */
    private void exeCreateFileSendIm(ImHdjkPushManage imHdjkPushManage){
        HdjkModelManage hdjkModelManage = null;
        boolean isResult = false;
        String resultStr ="文件生成失败";
        try {
            int modelId = imHdjkPushManage.getModelId();
            String isPushImg = imHdjkPushManage.getIsPushImg()==null?"":imHdjkPushManage.getIsPushImg();

            hdjkModelManage = hdjkModelManageMapper.selectById(modelId);
            if(hdjkModelManage != null){
                String sourFilePath = hdjkModelManage.getModelSourcePath();
                String tableName = hdjkModelManage.getTableName();
                String modelType = hdjkModelManage.getModelType();
                String backgroundColor = hdjkModelManage.getBackgroundColor() ==null?"":hdjkModelManage.getBackgroundColor();
                String lineColor = hdjkModelManage.getLineColor() ==null?"":hdjkModelManage.getLineColor();
                String isCreateImg = hdjkModelManage.getIsCreateImg() ==null?"":hdjkModelManage.getIsCreateImg();
                String sendFileName = hdjkModelManage.getSendFileName() ==null?"":hdjkModelManage.getSendFileName();

                String acctDay ="";
                //类型为空或者1（日报）
                if(StringUtil.isEmpty(modelType) || modelType.equals("1")){
                    acctDay = DateTimeUtil.getYesterday("yyyyMMdd");
                }else if(modelType.equals("2")){ //月报
                    acctDay = DateTimeUtil.getLastMonth("yyyyMM");
                }

                String modelName = hdjkModelManage.getModelName();
                String suffName = modelName.substring(0,modelName.lastIndexOf("."));

                /*suffName = suffName+ DateTimeUtil.getYesterday("MMdd");
                String outputFilePath = hdjkTargetPath + suffName +".xlsx";*/

                suffName = suffName+"_"+ acctDay;

                String outputFilePath = "";
                if(StringUtil.isNotEmpty(sendFileName)){

                        sendFileName = sendFileName.replaceAll(".xlsx","");
                        sendFileName = sendFileName.replaceAll(".xls","");
                        outputFilePath = hdjkTargetPath+sendFileName+"_"+acctDay+".xlsx";

                }else{
                    outputFilePath = hdjkTargetPath+suffName+".xlsx";
                }

                String modelParam = hdjkModelManage.getModelParam();
                if(StringUtil.isNotEmpty(modelParam)){
                    int dataCount = 0;
                    Map<String,List<Map<String,Object>>> dataListMap = new HashMap<>();
                    String modelParamArr[] = modelParam.split(";");
                    for(int i=0;i<modelParamArr.length;i++){
                        String subModelParam = modelParamArr[i];
                        subModelParam = subModelParam.trim();

                        //查询数据库，获取数据信息
                        List<Map<String,Object>> dataList = hdjkConnSecondTableService.zyzGjzMkt(tableName,subModelParam,acctDay);
                        if(dataList != null && dataList.size() >0){
                            dataListMap.put("xh"+(i+1),dataList);
                            dataCount = dataCount + dataList.size();
                        }else{
                            log.info("++++++hdjkCreateNewTask_createFile 根据账期{}查询数据库信息为空 ",acctDay);
                            resultStr = "根据账期"+acctDay+"未查询到数据库信息";
                        }
                    }

                    if(dataListMap != null && dataListMap.size() >0){
                        /*boolean creatFileReslut = false;
                        if(dataCount >0 && dataCount <=hdjkCreateFileMaxData){
                           creatFileReslut = RbExcelUtil.createExcelFile2(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                        }else if(dataCount >hdjkCreateFileMaxData){
                            creatFileReslut = RbExcelUtil.createBigDataExcelFile(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                        }*/

                        //创建文件
                        JSONObject creatFileReslutJson = RbExcelUtil.createBigDataExcelFileAuto(sourFilePath,outputFilePath,dataListMap,backgroundColor,lineColor);
                        log.info("++++++hdjkCreateNewTask_creatFileReslutJsonAuto: "+creatFileReslutJson);
                        String createFileRetCode = creatFileReslutJson.getString("retCode")==null?"":creatFileReslutJson.getString("retCode");

                        if(createFileRetCode.equals("0")){
                            String imgOutputFilePath = "";
                            //创建图片
                            if(isPushImg.equals("1")){
                                String imgSourFilePath = outputFilePath;

                                /*String jpegName = suffName + ".png";
                                //String jpegName = suffName + ".jpeg";
                                imgOutputFilePath = hdjkTargetPath + jpegName;*/

                                if(StringUtil.isNotEmpty(sendFileName)){
                                    sendFileName = sendFileName.replaceAll(".xlsx","");
                                    sendFileName = sendFileName.replaceAll(".xls","");
                                    imgOutputFilePath = hdjkTargetPath+sendFileName+"_"+acctDay+".png";
                                }else{
                                    imgOutputFilePath = hdjkTargetPath + suffName+".png";
                                }

                                boolean toImgResult = false;
                                if(dataCount >0 && dataCount <= hdjkCreateFileMaxData){
                                   toImgResult = RbExcelUtil.excelToImg(imgSourFilePath,imgOutputFilePath);
                                }else if(dataCount > hdjkCreateFileMaxData){
                                    toImgResult = RbExcelUtil.excelBigDataToImg(imgSourFilePath,imgOutputFilePath);
                                }

                                log.info("++++++hdjkCreateNewTask_toImgResult: "+toImgResult);

                            }


                                //更新IM推送文件管理表
                                imHdjkPushManage.setFileName(suffName+".xlsx");
                                imHdjkPushManage.setFileCreatTime(LocalDateTime.now());
                                imHdjkPushManage.setFilePath(outputFilePath);
                                imHdjkPushManage.setImgPath(imgOutputFilePath);
                                imHdjkPushManage.setFilePushTime(null);
                                int upResult = imHdjkPushManageMapper.updateById(imHdjkPushManage);
                                String isAutoPush = imHdjkPushManage.getIsAutoPush() ==null?"":imHdjkPushManage.getIsAutoPush();
                                if(isAutoPush.equals("1") && upResult >0){ //自动推送
                                    JSONObject pushParam = new JSONObject();
                                    pushParam.put("manageId",imHdjkPushManage.getManageId());
                                    //JSONObject filePushObject = rbFilePushManageService.push(pushParam.toString());
                                    JSONObject filePushObject = hdjkFilePushManageService.pushNew(pushParam.toString());
                                    log.info("++++++++++++++hdjkCreateNewTask_filePushObject: {}",filePushObject);
                                    String retCode = filePushObject.getString("retCode");
                                    if(StringUtil.isNotEmpty(retCode) && retCode.equals("0")){
                                        resultStr = "文件生成并发送成功";
                                        isResult = true;
                                        //修改推送时间
                                        imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                        imHdjkPushManageMapper.updateById(imHdjkPushManage);
                                    }else{
                                        resultStr = "文件生成成功,发送失败";
                                    }
                                }else{
                                    isResult = true;
                                    resultStr = "文件生成成功";
                                }


                            hdjkModelManage.setFileCreateTime(LocalDateTime.now());
                            hdjkModelManage.setModelTargetPath(outputFilePath);
                            hdjkModelManageMapper.updateById(hdjkModelManage);
                        }else{ //文件生成失败
                            isResult = false;
                            String createFileRetMsg = creatFileReslutJson.getString("retMsg")==null?"":creatFileReslutJson.getString("retMsg");
                            resultStr = "文件生成失败: "+createFileRetMsg;
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(hdjkModelManage != null){
                //当前登录人信息
                SysUser currentUser = UserContext.currentUser();
                if(currentUser ==null){
                    //取管理员信息
                    currentUser = sysUserService.getById("1309861917694623744");
                }

                //保存日志信息
                HdjkLog rbhdjkLog = new HdjkLog();
                rbhdjkLog.setModelName(hdjkModelManage.getModelName());
                rbhdjkLog.setLogType("create");
                rbhdjkLog.setLogTime(LocalDateTime.now());
                rbhdjkLog.setModelSourcePath(hdjkModelManage.getModelSourcePath());
                rbhdjkLog.setCreateUserId(currentUser.getUserId());
                rbhdjkLog.setCreateUserName(currentUser.getRealName());
                rbhdjkLog.setModelArea(hdjkModelManage.getModelArea());
                rbhdjkLog.setFileSize(hdjkModelManage.getFileSize());
                rbhdjkLog.setTableName(hdjkModelManage.getTableName());
                rbhdjkLog.setModelParam(hdjkModelManage.getModelParam());
                rbhdjkLog.setModelType(hdjkModelManage.getModelType());
                if(isResult){
                    rbhdjkLog.setIsSuccess("0");
                }else{
                    rbhdjkLog.setIsSuccess("-1");
                }
                if(StringUtil.isNotEmpty(resultStr) && resultStr.length() >2000){
                    resultStr = resultStr.substring(0,1000);
                }
                rbhdjkLog.setLogMsg(resultStr);

                hdjkLogMapper.insert(rbhdjkLog);
            }
        }

    }

}
