package com.pearadmin.modules.job.schedule;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.quartz.base.BaseQuartz;
import com.pearadmin.common.tools.DateTimeUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import com.pearadmin.modules.im.util.ImUtil;
import com.pearadmin.modules.wgppt.domain.VWgzsUserLevelD;
import com.pearadmin.modules.znyj.domain.KhjyDwdKdlsyjD;
import com.pearadmin.modules.znyj.domain.WarnLog;
import com.pearadmin.modules.znyj.domain.WarnModelSet;
import com.pearadmin.modules.znyj.domain.WarnPushPersonSet;
import com.pearadmin.modules.znyj.mapper.KhjyDwdKdlsyjDMapper;
import com.pearadmin.modules.znyj.mapper.WarnLogMapper;
import com.pearadmin.modules.znyj.mapper.WarnModelSetMapper;
import com.pearadmin.modules.znyj.mapper.WarnPushPersonSetMapper;
import com.pearadmin.modules.znyj.service.ZnyjDorisOperateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * 创建日期：2026-02-26
 * 智能预警定时任务
 **/

@Slf4j
@Component("znyjTask")
public class ZnyjTask implements BaseQuartz {

    @Resource
    private WarnModelSetMapper warnModelSetMapper;

    @Resource
    private WarnPushPersonSetMapper warnPushPersonSetMapper;

    @Resource
    private ImUtil imUtil;

    @Resource
    private ZnyjDorisOperateService znyjDorisOperateService;

    @Resource
    private WarnLogMapper warnLogMapper;



    @Override
    public void run(String params){
        log.info("++++++++++智能预警定时任务++++++++++");
        List<WarnModelSet> updateWarnModelSetList = null;
        try {
            //获取当前的月份
            String currentDay = new SimpleDateFormat("dd").format(new Date());
            //获取当前的时分
            String currentHm = new SimpleDateFormat("HH:mm").format(new Date());

            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();
            // 计算几分钟前的时间
            int minutesBefore = Integer.parseInt(params);
            LocalDateTime twoMinutesBefore = now.minusMinutes(minutesBefore);
            // 格式化输出（可选）
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String twoMinutesBeforeHm = twoMinutesBefore.format(formatter);

            DayOfWeek currentWeek = now.getDayOfWeek();

            //根据开始日期和结束日期查询智能预警数据
            List<WarnModelSet> warnModelSetList = warnModelSetMapper.getListByTime(twoMinutesBeforeHm,currentHm);
            if(CollectionUtils.isNotEmpty(warnModelSetList)){

                updateWarnModelSetList = warnModelSetList;

                //批量设置智能预警的执行状态为 执行中
                setExecState(warnModelSetList,1);

                int batchSize = 10; // 每批处理数量
                int total = warnModelSetList.size();

                for (int i = 0; i < total; i += batchSize) {
                    int end = Math.min(i + batchSize, total);
                    List<WarnModelSet> batch = warnModelSetList.subList(i, end);

                    batch.parallelStream().forEach(warnModelSet -> processSingle(warnModelSet, currentDay, currentWeek.toString()));
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //批量设置智能预警的执行状态为 未执行
            if(updateWarnModelSetList != null && updateWarnModelSetList.size() >0){
                setExecState(updateWarnModelSetList,0);
            }
        }
    }

    /**
     * 批量更新执行状态
     * @param warnModelSetList
     * @param state
     */
    private void setExecState(List<WarnModelSet> warnModelSetList,int state){
        List<Integer> idList = new ArrayList<>();
        for(WarnModelSet warnModelSet : warnModelSetList){
            idList.add(warnModelSet.getWarnModelId());
        }

        if(idList.size() >0){
            warnModelSetMapper.batchSetExecState(idList,state);
        }

    }

    private void processSingle(WarnModelSet warnModelSet, String currentDay, String currentWeek) {
        try {
            String pushPeriod = warnModelSet.getPushPeriod() ==null?"":warnModelSet.getPushPeriod();
            // 校验执行条件
            if(pushPeriod.equals("month")){//月
                String execDay = warnModelSet.getPushDay()==null?"":warnModelSet.getPushDay();
                if(!execDay.equals(currentDay)){ //发送日期不是当天
                    return;
                }
            }else if(pushPeriod.equals("week")){//周
                String exeWeek = warnModelSet.getPushWeek()==null?"":warnModelSet.getPushWeek();
                if(!currentWeek.equals(exeWeek)){
                    return;
                }
            }

            exeWarnPush(warnModelSet);

            //累积执行次数
            warnModelSet.setExecuteNum(warnModelSet.getExecuteNum() + 1);
            int index = warnModelSetMapper.updateById(warnModelSet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 时间点内执行
     * @param warnModelSet
     */
    private void exeWarnPush(WarnModelSet warnModelSet){
        try {
            int warnModelId = warnModelSet.getWarnModelId();
            String warnMsg = warnModelSet.getWarnMsg()==null?"":warnModelSet.getWarnMsg(); //预警信息
            String notyetWarnMsg = warnModelSet.getNotyetWarnMsg()==null?"":warnModelSet.getNotyetWarnMsg(); //预警信息
            String threshoRule = warnModelSet.getThreshoRule() ==null?"":warnModelSet.getThreshoRule(); //预警阀值规则  static静态   interval区间
            String threshoTarget = warnModelSet.getThreshoTarget() ==null?"0":warnModelSet.getThreshoTarget(); //阀值目标  dy大于 xy小于 dydy大于等于 xydy小于等于
            String threshoMinValue = warnModelSet.getThreshoMinValue()==null?"":warnModelSet.getThreshoMinValue(); //阀值最小值
            float ithreshoMinValue = Float.parseFloat(threshoMinValue);
//            float ithreshoMinValue = 0.0f;
//            if(threshoMinValue.indexOf("%") >=0){
//                ithreshoMinValue = Float.parseFloat(threshoMinValue.replaceAll("%",""));
//                ithreshoMinValue = ithreshoMinValue/100f;
//            }
            String threshoMaxValue = warnModelSet.getThreshoMaxValue()==null?"0":warnModelSet.getThreshoMaxValue(); //阀值最大值
            float ithreshoMaxValue = 0.0f;
            if(StringUtil.isNotEmpty(threshoMaxValue)){
                ithreshoMaxValue = Float.parseFloat(threshoMaxValue);
            }
//            float ithreshoMaxValue = 0.0f;
//            if(threshoMaxValue.indexOf("%") >=0){
//                ithreshoMaxValue = Float.parseFloat(threshoMaxValue.replaceAll("%",""));
//                ithreshoMaxValue = ithreshoMaxValue/100f;
//            }

            int notyetWarnPush = warnModelSet.getNotyetWarnPush(); //未达预警时推送 1启用  0不启用
            String warnQuotaId = warnModelSet.getWarnQuotaId() ==null?"":warnModelSet.getWarnQuotaId(); //预警指标编码

            //根据数据周期 获取账期
            String opDate ="";
            String dataPeriod = warnModelSet.getDataPeriod() ==null?"":warnModelSet.getDataPeriod(); //数据周期  day日  month月
            if(dataPeriod.equals("month")){
                opDate = DateTimeUtil.getLastMonth("yyyyMM");
            }else{
                opDate = DateTimeUtil.getYesterday("yyyyMMdd");
            }

            String warnGranula = warnModelSet.getWarnGranula() ==null?"":warnModelSet.getWarnGranula(); //预警粒度  qj 全疆  bdw本地网  xf县分  zj支局  wg网格

            QueryWrapper<WarnPushPersonSet> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("warn_model_id",warnModelId);
            List<WarnPushPersonSet> warnPushPersonSetList = warnPushPersonSetMapper.selectList(queryWrapper);
            if(warnPushPersonSetList != null && warnPushPersonSetList.size() >0){

                String executeResult = "";
                String executeResultInfo = "";
                String pushType ="";

                for(WarnPushPersonSet warnPushPersonSet : warnPushPersonSetList){

                    String pushPersonId = "";
                    String areaId = warnPushPersonSet.getAreaId();
                    String areaName = warnPushPersonSet.getAreaName();
                    //String pushPersonId = warnPushPersonSet.getPushPersonId();
                    //String pushPersonName = warnPushPersonSet.getPushPersonName();

                    WarnLog warnLog = new WarnLog();
                    warnLog.setWarnModelId(warnModelId);
                    warnLog.setExecuteTime(LocalDateTime.now());
                    //warnLog.setPushPersonName(pushPersonName);
                    warnLog.setAreaName(areaName);

                    boolean isAddWarnLog = false;

                    //根据指标ID,区域ID,账期查询预警指标信息
                    KhjyDwdKdlsyjD khjyDwdKdlsyjD = znyjDorisOperateService.getIndicByCodeAraeIdOpDate(warnQuotaId,areaId,opDate,warnGranula);
                    if(khjyDwdKdlsyjD != null){
                        float warnQuotaValue = khjyDwdKdlsyjD.getField10() ==null?0f:khjyDwdKdlsyjD.getField10();

                            JSONObject retSendMsgJson = null;

                            //静态
                            if(threshoRule.equals("static")){
                                //大于
                                if(threshoTarget.equals("dy")){
                                    if(warnQuotaValue > ithreshoMinValue){
                                        isAddWarnLog = true;

                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newWarnMsg = createWarnMsg(warnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isEmpty(newWarnMsg)){
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }else{
                                                warnLog.setWarnMsg(newWarnMsg);
                                                //预警推送
                                                retSendMsgJson = warnPushIM(newWarnMsg,pushPersonId,areaName);
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }

                                        pushType = "yjtc";
                                    }

                                    if(notyetWarnPush ==1){
                                        isAddWarnLog = true;
                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newNotyetWarnMsg = createNotyetWarnMsg(notyetWarnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isNotEmpty(newNotyetWarnMsg)){
                                                warnLog.setWarnMsg(newNotyetWarnMsg);
                                                retSendMsgJson = warnPushIM(newNotyetWarnMsg,pushPersonId,areaName);
                                            }else{
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }

                                        pushType = "wdyjtc";
                                    }
                                }

                                //小于
                                if(threshoTarget.equals("xy")){
                                    if(warnQuotaValue < ithreshoMinValue){
                                        isAddWarnLog = true;
                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newWarnMsg = createWarnMsg(warnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isEmpty(newWarnMsg)){
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }else{
                                                warnLog.setWarnMsg(newWarnMsg);
                                                //预警推送
                                                retSendMsgJson = warnPushIM(newWarnMsg,pushPersonId,areaName);
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }

                                        pushType = "yjtc";
                                    }

                                    if(notyetWarnPush ==1){
                                        isAddWarnLog = true;
                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newNotyetWarnMsg = createNotyetWarnMsg(notyetWarnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isNotEmpty(newNotyetWarnMsg)){
                                                warnLog.setWarnMsg(newNotyetWarnMsg);
                                                retSendMsgJson = warnPushIM(newNotyetWarnMsg,pushPersonId,areaName);
                                            }else{
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }
                                        pushType = "wdyjtc";
                                    }
                                }

                                //等于
                                if(threshoTarget.equals("eq")){
                                    if(warnQuotaValue < ithreshoMinValue){
                                        isAddWarnLog = true;
                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newWarnMsg = createWarnMsg(warnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isEmpty(newWarnMsg)){
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }else{
                                                warnLog.setWarnMsg(newWarnMsg);
                                                //预警推送
                                                retSendMsgJson = warnPushIM(newWarnMsg,pushPersonId,areaName);
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }

                                        pushType = "yjtc";
                                    }

                                    if(notyetWarnPush ==1){
                                        isAddWarnLog = true;
                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newNotyetWarnMsg = createNotyetWarnMsg(notyetWarnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isNotEmpty(newNotyetWarnMsg)){
                                                warnLog.setWarnMsg(newNotyetWarnMsg);
                                                retSendMsgJson = warnPushIM(newNotyetWarnMsg,pushPersonId,areaName);
                                            }else{
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }

                                        pushType = "wdyjtc";
                                    }
                                }

                                //大于等于
                                if(threshoTarget.equals("dydy")){
                                    if(warnQuotaValue >= ithreshoMinValue){
                                        isAddWarnLog = true;
                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newWarnMsg = createWarnMsg(warnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isEmpty(newWarnMsg)){
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }else{
                                                warnLog.setWarnMsg(newWarnMsg);
                                                //预警推送
                                                retSendMsgJson = warnPushIM(newWarnMsg,pushPersonId,areaName);
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }

                                        pushType = "yjtc";
                                    }

                                    if(notyetWarnPush ==1){
                                        isAddWarnLog = true;
                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newNotyetWarnMsg = createNotyetWarnMsg(notyetWarnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isNotEmpty(newNotyetWarnMsg)){
                                                warnLog.setWarnMsg(newNotyetWarnMsg);
                                                retSendMsgJson = warnPushIM(newNotyetWarnMsg,pushPersonId,areaName);
                                            }else{
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }

                                        pushType = "wdyjtc";
                                    }
                                }

                                //小于等于
                                if(threshoTarget.equals("xydy")){
                                    if(warnQuotaValue <= ithreshoMinValue){
                                        isAddWarnLog = true;
                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newWarnMsg = createWarnMsg(warnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isEmpty(newWarnMsg)){
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }else{
                                                warnLog.setWarnMsg(newWarnMsg);
                                                //预警推送
                                                retSendMsgJson = warnPushIM(newWarnMsg,pushPersonId,areaName);
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }

                                        pushType = "yjtc";
                                    }
                                    if(notyetWarnPush ==1){
                                        isAddWarnLog = true;
                                        //获取推送人
                                        Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                        pushPersonId = pushPersonMap.get("pushPersonId");
                                        if(StringUtil.isNotEmpty(pushPersonId)){
                                            warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                            //创建推送消息内容
                                            String newNotyetWarnMsg = createNotyetWarnMsg(notyetWarnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                            if(StringUtil.isNotEmpty(newNotyetWarnMsg)){
                                                warnLog.setWarnMsg(newNotyetWarnMsg);
                                                retSendMsgJson = warnPushIM(newNotyetWarnMsg,pushPersonId,areaName);
                                            }else{
                                                executeResult = "0";
                                                executeResultInfo = "生成预警信息为空";
                                            }
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "推送人为空";
                                        }

                                        pushType = "wdyjtc";
                                    }
                                }
                            }else{ //区间
                                if(ithreshoMinValue <= warnQuotaValue && warnQuotaValue <= ithreshoMaxValue){
                                    isAddWarnLog = true;
                                    //获取推送人
                                    Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                    pushPersonId = pushPersonMap.get("pushPersonId");
                                    if(StringUtil.isNotEmpty(pushPersonId)){
                                        warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                        //创建推送消息内容
                                        String newWarnMsg = createWarnMsg(warnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                        if(StringUtil.isEmpty(newWarnMsg)){
                                            executeResult = "0";
                                            executeResultInfo = "生成预警信息为空";
                                        }else{
                                            warnLog.setWarnMsg(newWarnMsg);
                                            //预警推送
                                            retSendMsgJson = warnPushIM(newWarnMsg,pushPersonId,areaName);
                                        }
                                    }else{
                                        executeResult = "0";
                                        executeResultInfo = "推送人为空";
                                    }

                                    pushType = "yjtc";
                                }
                                if(notyetWarnPush ==1){
                                    isAddWarnLog = true;
                                    //获取推送人
                                    Map<String,String> pushPersonMap = getPushPerson(warnGranula,warnPushPersonSet);
                                    pushPersonId = pushPersonMap.get("pushPersonId");
                                    if(StringUtil.isNotEmpty(pushPersonId)){
                                        warnLog.setPushPersonName(pushPersonMap.get("pushPersonName"));
                                        //创建推送消息内容
                                        String newNotyetWarnMsg = createNotyetWarnMsg(notyetWarnMsg,warnGranula,opDate,warnQuotaId,areaId,threshoMinValue,dataPeriod);
                                        if(StringUtil.isNotEmpty(newNotyetWarnMsg)){
                                            warnLog.setWarnMsg(newNotyetWarnMsg);
                                            retSendMsgJson = warnPushIM(newNotyetWarnMsg,pushPersonId,areaName);
                                        }else{
                                            executeResult = "0";
                                            executeResultInfo = "生成预警信息为空";
                                        }
                                    }else{
                                        executeResult = "0";
                                        executeResultInfo = "推送人为空";
                                    }

                                    pushType = "wdyjtc";
                                }
                            }

                            if(retSendMsgJson != null && retSendMsgJson.size()>0){
                                executeResult = retSendMsgJson.getString("executeResult");
                                executeResultInfo = retSendMsgJson.getString("executeResultInfo");
                            }

                    }else{
                        executeResult = "0";
                        executeResultInfo = "根据指标["+warnQuotaId+"],区域["+areaId+"],账期["+opDate+"],粒度["+warnGranula+"]未查询到预警指标信息";
                    }

                    if(isAddWarnLog){
                        warnLog.setExecuteResult(executeResult);
                        warnLog.setExecuteResultInfo(executeResultInfo);
                        warnLog.setPushType(pushType);
                        //新增日志信息
                        warnLogMapper.insert(warnLog);
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 推送至IM
     */
    private JSONObject warnPushIM(String warnMsg,String pushPersonId,String areaName){
        JSONObject retJsonObject = new JSONObject();

        String executeResult = ""; //执行结果
        String executeResultInfo  =""; //执行结果信息
        try {
            //根据预警指标，替换预警消息中的占位符
            //String lastWarnMsg = replacePlaceholders(warnMsg,khjyDwdKdlsyjD);
            //根据
            //拼接推送对象ID，用户格式为[user1,user2],群组格式为 group1;group2
            String pushUserIds = "";
            String pushGroupIds ="";
            if(StringUtil.isNotEmpty(pushPersonId)){
                String[] pushPersonIdArr = pushPersonId.split(",");
                for(String personId : pushPersonIdArr){
                    if(personId.length() ==11){ //用户
                        if(StringUtil.isEmpty(pushUserIds)){
                            pushUserIds = personId;
                        }else{
                            pushUserIds = pushUserIds+","+personId;
                        }
                    }else{
                        if(StringUtil.isEmpty(pushGroupIds)){
                            pushGroupIds = personId;
                        }else{
                            pushGroupIds = pushGroupIds+";"+personId;
                        }
                    }
                }
            }

            if(StringUtil.isEmpty(pushUserIds) && StringUtil.isEmpty(pushGroupIds)){
                executeResult = "0";
                executeResultInfo ="推送人或群为空";
            }else{
                if(StringUtil.isNotEmpty(pushUserIds)){
                    pushUserIds = "["+pushUserIds+"]";
                    //通过接口给IM推送信息
                    String sendMsgResult = imUtil.sendImMsg(warnMsg,"users",pushUserIds);
                    log.info("++++++++++{}_用户文字信息推送结果：{}",areaName,sendMsgResult);
                    if(StringUtil.isNotEmpty(sendMsgResult)){
                        JSONObject sendMsgJson = JSONObject.parseObject(sendMsgResult);
                        String sendMsgStatus = sendMsgJson.getString("status");
                        //推送成功
                        if(StringUtil.isNotEmpty(sendMsgStatus) && "success".equals(sendMsgStatus)){
                            executeResult = "1";
                            executeResultInfo = sendMsgResult;
                        }else{
                            executeResult = "0";
                            executeResultInfo = sendMsgResult;
                        }
                    }
                }

                if(StringUtil.isNotEmpty(pushGroupIds)){
                    String[] pushGroupIdsArr = pushGroupIds.split(";");
                    if(pushGroupIdsArr != null && pushGroupIdsArr.length >0){
                        for(String pushGroupId : pushGroupIdsArr){
                            //通过接口给IM推送信息
                            String sendMsgResult = imUtil.sendImMsg(warnMsg,"group",pushGroupId);
                            log.info("++++++++++{}_群文字信息推送结果：{}",areaName,sendMsgResult);
                            if(StringUtil.isNotEmpty(sendMsgResult)){
                                JSONObject sendMsgJson = JSONObject.parseObject(sendMsgResult);
                                String sendMsgStatus = sendMsgJson.getString("status");
                                //推送成功
                                if(StringUtil.isNotEmpty(sendMsgStatus) && "success".equals(sendMsgStatus)){
                                    executeResult = "1";
                                    executeResultInfo = sendMsgResult;
                                }else{
                                    executeResult = "0";
                                    executeResultInfo = sendMsgResult;
                                }
                            }
                        }
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            executeResult = "0";
            executeResultInfo = "系统异常："+e.toString();
        }finally {
            //保存推送日志
            /*WarnLog warnLog = new WarnLog();
            warnLog.setWarnModelId(warnModelId);
            warnLog.setExecuteTime(LocalDateTime.now());
            warnLog.setPushType(pushType);
            warnLog.setPushPersonName(pushPersonName);
            warnLog.setAreaName(areaName);
            warnLog.setExecuteResult(executeResult);
            warnLog.setExecuteResultInfo(executeResultInfo);
            warnLogMapper.insert(warnLog);*/
            retJsonObject.put("executeResult",executeResult);
            retJsonObject.put("executeResultInfo",executeResultInfo);
        }
        return retJsonObject;
    }

    /**
     * 推送至IM
     */
    private JSONObject warnPushUserIM(String warnMsg,KhjyDwdKdlsyjD khjyDwdKdlsyjD,String pushPersonId,String pushPersonName,String pushType,String areaName,String pushPersonNames){
        JSONObject retJsonObject = new JSONObject();
        String executeResult ="";
        String executeResultInfo ="";
        try {
            //根据预警指标，替换预警消息中的占位符
            String lastWarnMsg = replacePlaceholders(warnMsg,khjyDwdKdlsyjD,"");
            //拼接推送对象ID，用户格式为[user1,user2]
            if(StringUtil.isNotEmpty(pushPersonId)){
                pushPersonId = "["+pushPersonId+"]";
                //通过接口给IM推送信息
                String sendMsgResult = imUtil.sendImMsg(lastWarnMsg,"users",pushPersonId);
                log.info("++++++++++{}_用户文字信息推送结果：{}",areaName,sendMsgResult);
                if(StringUtil.isNotEmpty(sendMsgResult)){
                    JSONObject sendMsgJson = JSONObject.parseObject(sendMsgResult);
                    String sendMsgStatus = sendMsgJson.getString("status");
                    //推送成功
                    if(StringUtil.isNotEmpty(sendMsgStatus) && "success".equals(sendMsgStatus)){
                        executeResult = "成功";
                        executeResultInfo = sendMsgResult;
                    }else{
                        executeResult = "失败";
                        executeResultInfo = sendMsgResult;
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJsonObject.put("executeResult",executeResult);
            retJsonObject.put("executeResultInfo",executeResultInfo);
        }
        return retJsonObject;
    }


    public static String replacePlaceholders(String template, KhjyDwdKdlsyjD khjyDwdKdlsyjD,String percentColumn) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        String repPercentColumn ="";
        if(StringUtil.isNotEmpty(percentColumn)){
            repPercentColumn = underscoreToCamel(percentColumn);
        }

        while (matcher.find()) {
            String fieldNameUnderscore = matcher.group(1);               // 如 warn_model_name
            String fieldNameCamel = underscoreToCamel(fieldNameUnderscore); // 如 warnModelName
            Object value = null;

            try {
                Field field = KhjyDwdKdlsyjD.class.getDeclaredField(fieldNameCamel);
                field.setAccessible(true);
                value = field.get(khjyDwdKdlsyjD);

                if(StringUtil.isNotEmpty(repPercentColumn)){
                    if(fieldNameCamel.equals(repPercentColumn)){
                        value = Float.parseFloat(value+"")*100+"%";
                    }
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                // 字段不存在或访问异常，可保留原占位符或替换为空字符串（这里替换为空）
                // 你可以根据需求调整，例如：matcher.appendReplacement(sb, matcher.group(0));
            }

            String replacement = value != null ? String.valueOf(value) : "";
            // 注意对替换文本中的特殊字符进行转义
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String underscoreToCamel(String underscore) {
        String[] parts = underscore.split("_");
        StringBuilder camel = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camel.append(parts[i].substring(0, 1).toUpperCase())
                    .append(parts[i].substring(1).toLowerCase());
        }
        return camel.toString();
    }

    /**
     * 根据 粒度，账期，指标ID,区域ID,组装已达到预警指标信息
     * @return
     */
    private String createWarnMsg(String warnMsg,String warnGranula,String opDate,String warnQuotaId,String areaId,String threshoMinValue,String dataPeriod){
        String repWarnMsg = "";
        threshoMinValue = threshoMinValue.replaceAll("%","");
        float fThreshoMinValue = Float.parseFloat(threshoMinValue)/100f;

        //替换时间
        if(StringUtil.isNotEmpty(warnMsg)){
            String repOpDate = "";
            if(dataPeriod.equals("month")){
                repOpDate = DateTimeUtil.getLastMonth("MM月");
            }else{
                repOpDate = DateTimeUtil.getYesterday("MM月dd日");
            }

            warnMsg = warnMsg.replace("${date}", repOpDate);
        }

        //全疆
        if(warnGranula.equals("qj")){
            String repQjSubWarnMsg = "";
            String repBdwSubWarnMsg ="";
            //全疆
            KhjyDwdKdlsyjD khjyDwdKdlsyjD = znyjDorisOperateService.getIndicByCodeAraeIdOpDate(warnQuotaId,"9999",opDate,warnGranula,"field_10",fThreshoMinValue);
            if(khjyDwdKdlsyjD != null){

                if(warnMsg.indexOf("##") >= 0){
                    String qjSubWarnMsg = warnMsg.substring(0,warnMsg.indexOf("##"));
                    if(StringUtil.isNotEmpty(qjSubWarnMsg)){
                        repQjSubWarnMsg = replacePlaceholders(qjSubWarnMsg,khjyDwdKdlsyjD,"");
                    }
                }

                //16个本地网
                List<KhjyDwdKdlsyjD> khjyDwdKdlsyjDList = znyjDorisOperateService.getIndicByCodeOpdateHxType(warnQuotaId,opDate,"分公司");
                if(khjyDwdKdlsyjDList != null && khjyDwdKdlsyjDList.size() >0){
                    if(warnMsg.indexOf("##") >=0 && warnMsg.indexOf("###") >=0){
                        String bdwSubWarnMsg = warnMsg.substring(warnMsg.indexOf("##"),warnMsg.indexOf("###"));
                        if(StringUtil.isNotEmpty(bdwSubWarnMsg)){
                            for(KhjyDwdKdlsyjD bdwKhjyDwdKdlsyjD : khjyDwdKdlsyjDList){
                                if(StringUtil.isEmpty(repBdwSubWarnMsg)){
                                    repBdwSubWarnMsg = replacePlaceholders(bdwSubWarnMsg,bdwKhjyDwdKdlsyjD,"");
                                }else{
                                    repBdwSubWarnMsg = repBdwSubWarnMsg+","+replacePlaceholders(bdwSubWarnMsg,bdwKhjyDwdKdlsyjD,"");
                                }
                            }
                        }
                    }
                }
                repWarnMsg = repQjSubWarnMsg + repBdwSubWarnMsg+"###【区客经数据赋能室】";
            }

        }else if(warnGranula.equals("bdw")){ //本地网
            String repBdwSubWarnMsg = "";
            String repXfSubWarnMsg ="";
            String repWgSubWarnMsg ="";

            KhjyDwdKdlsyjD khjyDwdKdlsyjD = znyjDorisOperateService.getIndicByCodeAraeIdOpDate(warnQuotaId,areaId,opDate,warnGranula,"field_10",fThreshoMinValue);
            if(khjyDwdKdlsyjD != null){
                if(warnMsg.indexOf("##") >=0){
                    String subBdwWarnMsg = warnMsg.substring(0,warnMsg.indexOf("##"));
                    if(StringUtil.isNotEmpty(subBdwWarnMsg)){
                        repBdwSubWarnMsg = replacePlaceholders(subBdwWarnMsg,khjyDwdKdlsyjD,"");
                    }
                }

                //根据本地网ID，获取本地网下的县分
                String subLatenId = areaId.substring(0,5);
                List<KhjyDwdKdlsyjD> xfKhjyDwdKdlsyjDList = znyjDorisOperateService.getIndicByCodeOpdateHxTypeAreaId(warnQuotaId,opDate,"县分","hx_area_id",subLatenId);
                if(xfKhjyDwdKdlsyjDList !=null && xfKhjyDwdKdlsyjDList.size() >0){
                    if(warnMsg.indexOf("##") >=0 && warnMsg.indexOf("###") >=0){
                        String xfSubWarnMsg = warnMsg.substring(warnMsg.indexOf("##"),warnMsg.indexOf("###"));
                        if(StringUtil.isNotEmpty(xfSubWarnMsg)){

                            String subWgMsg = "";

                            for(KhjyDwdKdlsyjD xfKhjyDwdKdlsyjD : xfKhjyDwdKdlsyjDList){
                                if(StringUtil.isEmpty(repXfSubWarnMsg)){
                                    repXfSubWarnMsg = replacePlaceholders(xfSubWarnMsg,xfKhjyDwdKdlsyjD,"");
                                }else{
                                    repXfSubWarnMsg = repXfSubWarnMsg+","+replacePlaceholders(xfSubWarnMsg,xfKhjyDwdKdlsyjD,"");
                                }

                                //根据县分ID，获取县分下不达标网格的数量
                                String hxAraeId = xfKhjyDwdKdlsyjD.getHxAreaId();
                                if(StringUtil.isNotEmpty(hxAraeId)){
                                    String subHxAreaId = hxAraeId.substring(0,7);
                                    List<KhjyDwdKdlsyjD> wgKhjyDwdKdlsyjDList = znyjDorisOperateService.getWdgWgCountByAreaId(warnQuotaId,opDate,"网格",subHxAreaId,"field_10",fThreshoMinValue);
                                    if(wgKhjyDwdKdlsyjDList != null && wgKhjyDwdKdlsyjDList.size() >0){
                                        for(KhjyDwdKdlsyjD wgKhjyDwdKdlsyjD : wgKhjyDwdKdlsyjDList){
                                            if(StringUtil.isEmpty(subWgMsg)){
                                                subWgMsg =  xfKhjyDwdKdlsyjD.getHxAreaName()+wgKhjyDwdKdlsyjD.getWgCount()+"个网格";
                                            }else{
                                                subWgMsg = subWgMsg +"," +xfKhjyDwdKdlsyjD.getHxAreaName()+wgKhjyDwdKdlsyjD.getWgCount()+"个网格";
                                            }
                                        }
                                    }
                                }
                            }

                            if(StringUtil.isNotEmpty(subWgMsg)){
                                if(warnMsg.indexOf("###") >=0){
                                    String wgSubWarnMsg = warnMsg.substring(warnMsg.indexOf("###"));
                                    khjyDwdKdlsyjD.setWgCount(subWgMsg);
                                    repWgSubWarnMsg = replacePlaceholders(wgSubWarnMsg,khjyDwdKdlsyjD,"");
                                }
                            }
                        }
                    }
                }
            }

            repWarnMsg = repBdwSubWarnMsg + repXfSubWarnMsg + repWgSubWarnMsg;

        }else if(warnGranula.equals("xf")){ //县分
            String repXfSubWarnMsg ="";
            String repWgSubWarnMsg ="";

            KhjyDwdKdlsyjD xfKhjyDwdKdlsyjD = znyjDorisOperateService.getIndicByCodeAraeIdOpDate(warnQuotaId,areaId,opDate,warnGranula,"field_10",fThreshoMinValue);
            if(xfKhjyDwdKdlsyjD != null ){
                if(warnMsg.indexOf("##") >=0){
                    String subXfWarnMsg = warnMsg.substring(0,warnMsg.indexOf("##"));
                    if(StringUtil.isNotEmpty(subXfWarnMsg)){
                        repXfSubWarnMsg = replacePlaceholders(subXfWarnMsg,xfKhjyDwdKdlsyjD,"");
                    }
                }

                //根据县分ID，获取县分下不达标网格
                String subAreaId = areaId.substring(0,7);
                List<KhjyDwdKdlsyjD> wgKhjyDwdKdlsyjDList = znyjDorisOperateService.getWdbWgIndic(warnQuotaId,opDate,"网格",subAreaId,"field_10",fThreshoMinValue);
                if(wgKhjyDwdKdlsyjDList != null && wgKhjyDwdKdlsyjDList.size() >0){
                    String subWgWarn = "";
                    String subWddWgCount = "";
                    if(warnMsg.indexOf("##") >=0 && warnMsg.indexOf("###") >=0){
                        String subWgMsg1 = warnMsg.substring(warnMsg.indexOf("##"),warnMsg.indexOf("###"));
                        if(StringUtil.isNotEmpty(subWgMsg1)){
                            xfKhjyDwdKdlsyjD.setWgCount(wgKhjyDwdKdlsyjDList.size()+"");
                            subWgWarn = replacePlaceholders(subWgMsg1,xfKhjyDwdKdlsyjD,"");

                            if(warnMsg.indexOf("###") >=0 && warnMsg.indexOf("####") >=0){
                                String subWgMsg2 = warnMsg.substring(warnMsg.indexOf("###"),warnMsg.indexOf("####"));
                                for(KhjyDwdKdlsyjD wgKhjyDwdKdlsyjD : wgKhjyDwdKdlsyjDList){
                                    if(StringUtil.isEmpty(subWddWgCount)){
                                        subWddWgCount = replacePlaceholders(subWgMsg2,wgKhjyDwdKdlsyjD,"");
                                    }else{
                                        //subWddWgCount = subWddWgCount+","+replacePlaceholders(subWgMsg2,wgKhjyDwdKdlsyjD);
                                        subWddWgCount = subWddWgCount + replacePlaceholders(subWgMsg2,wgKhjyDwdKdlsyjD,"");
                                    }
                                }
                            }
                        }
                    }

                    String subLastWgMsg = "";
                    if(warnMsg.indexOf("####") >=0){
                        subLastWgMsg = warnMsg.substring(warnMsg.indexOf("####"));
                    }
                    repWgSubWarnMsg = subWgWarn + subWddWgCount + subLastWgMsg;
                }

                repWarnMsg = repXfSubWarnMsg + repWgSubWarnMsg;
            }
        }else if(warnGranula.equals("zj")){ //支局
            String repWgWarnMsg ="";
            String subHxBpId = areaId.substring(0,9);

            //List<KhjyDwdKdlsyjD> wgKhjyDwdKdlsyjDList = znyjDorisOperateService.getWdgWgCountByAreaId(warnQuotaId,opDate,"网格",subHxBpId,"field_10",fThreshoMinValue);
            List<KhjyDwdKdlsyjD> wgKhjyDwdKdlsyjDList = znyjDorisOperateService.getWdbWgIndic(warnQuotaId,opDate,"网格",subHxBpId,"field_10",fThreshoMinValue);
            if(wgKhjyDwdKdlsyjDList != null && wgKhjyDwdKdlsyjDList.size() >0){
                String subHeadMsg = "";
                if(warnMsg.indexOf("##") >=0){
                    subHeadMsg = warnMsg.substring(0,warnMsg.indexOf("##"));
                }

                String repSubWgMsg ="";
                String subWgMsg2 ="";
                if(warnMsg.indexOf("##") >=0 && warnMsg.indexOf("###") >=0){
                    String subWgMsg = warnMsg.substring(warnMsg.indexOf("##"),warnMsg.indexOf("###"));
                    subWgMsg2 = warnMsg.substring(warnMsg.indexOf("###"));

                    for(KhjyDwdKdlsyjD khjyDwdKdlsyjD : wgKhjyDwdKdlsyjDList){
                        subWgMsg2 = replacePlaceholders(subWgMsg2,khjyDwdKdlsyjD,"");
                        if(StringUtil.isEmpty(repSubWgMsg)){
                            repSubWgMsg = replacePlaceholders(subWgMsg,khjyDwdKdlsyjD,"");
                        }else{
                            repSubWgMsg = repSubWgMsg + "," +replacePlaceholders(subWgMsg,khjyDwdKdlsyjD,"");
                        }
                    }
                }

                repWgWarnMsg = subHeadMsg + repSubWgMsg + subWgMsg2;
            }

            repWarnMsg = repWgWarnMsg;
        }else if(warnGranula.equals("wg")){ //网格
            String repWgWarnMsg ="";
            //List<KhjyDwdKdlsyjD> wgKhjyDwdKdlsyjDList = znyjDorisOperateService.getWdgWgCountByAreaId(warnQuotaId,opDate,"网格",areaId,"field_10",fThreshoMinValue);
            List<KhjyDwdKdlsyjD> wgKhjyDwdKdlsyjDList = znyjDorisOperateService.getWdbWgIndic(warnQuotaId,opDate,"网格",areaId,"field_10",fThreshoMinValue);
            if(wgKhjyDwdKdlsyjDList != null && wgKhjyDwdKdlsyjDList.size() >0){
                String subHeadMsg = "";
                if(warnMsg.indexOf("##") >=0){
                    subHeadMsg = warnMsg.substring(0,warnMsg.indexOf("##"));
                }

                String repSubWgMsg ="";
                String subWgMsg2 = "";
                if(warnMsg.indexOf("##") >=0 && warnMsg.indexOf("###") >=0){
                    String subWgMsg = warnMsg.substring(warnMsg.indexOf("##"),warnMsg.indexOf("###"));
                    subWgMsg2 = warnMsg.substring(warnMsg.indexOf("###"));

                    for(KhjyDwdKdlsyjD khjyDwdKdlsyjD : wgKhjyDwdKdlsyjDList){
                        subWgMsg2 = replacePlaceholders(subWgMsg2,khjyDwdKdlsyjD,"");
                        if(StringUtil.isEmpty(repSubWgMsg)){
                            repSubWgMsg = replacePlaceholders(subWgMsg,khjyDwdKdlsyjD,"");
                        }else{
                            repSubWgMsg = repSubWgMsg + "," +replacePlaceholders(subWgMsg,khjyDwdKdlsyjD,"");
                        }
                    }
                }

                repWgWarnMsg = subHeadMsg + repSubWgMsg + subWgMsg2;
            }
            repWarnMsg = repWgWarnMsg;
        }

        if(StringUtil.isNotEmpty(repWarnMsg)){
            if(repWarnMsg.indexOf("####")>=0){
                repWarnMsg = repWarnMsg.replaceAll("####","");
            }

            if(repWarnMsg.indexOf("###")>=0){
                repWarnMsg = repWarnMsg.replaceAll("###","");
            }
        }

        log.info("+++++++++++++++{}生成预警信息内容：{}",areaId,repWarnMsg);

        return repWarnMsg;
    }

    /**
     * 根据 粒度，账期，指标ID,区域ID,组装未达到预警指标信息
     * @return
     */
    private String createNotyetWarnMsg(String warnMsg,String warnGranula,String opDate,String warnQuotaId,String areaId,String threshoMinValue,String dataPeriod){
        String repWarnMsg = "";
        threshoMinValue = threshoMinValue.replaceAll("%","");
        float fThreshoMinValue = Float.parseFloat(threshoMinValue)/100f;

        //替换时间
        if(StringUtil.isNotEmpty(warnMsg)){
            String repOpDate = "";
            if(dataPeriod.equals("month")){
                repOpDate = DateTimeUtil.getLastMonth("MM月");
            }else{
                repOpDate = DateTimeUtil.getYesterday("MM月dd日");
            }

            warnMsg = warnMsg.replace("${date}", repOpDate);
        }

        //全疆
        if(warnGranula.equals("qj")){


        }else if(warnGranula.equals("bdw")){ //本地网

        }else if(warnGranula.equals("xf")){ //县分
            String repXfSubWarnMsg ="";
            String repWgSubWarnMsg ="";

            KhjyDwdKdlsyjD xfKhjyDwdKdlsyjD = znyjDorisOperateService.getIndicByCodeAraeIdOpDate(warnQuotaId,areaId,opDate,warnGranula);
            if(xfKhjyDwdKdlsyjD != null ){
                if(warnMsg.indexOf("##") >=0){
                    String subXfWarnMsg = warnMsg.substring(0,warnMsg.indexOf("##"));
                    if(StringUtil.isNotEmpty(subXfWarnMsg)){
                        repXfSubWarnMsg = replacePlaceholders(subXfWarnMsg,xfKhjyDwdKdlsyjD,"");
                    }
                }


                //根据县分ID，获取县分下不达标网格
                String subAreaId = areaId.substring(0,7);

                //List<KhjyDwdKdlsyjD> wgKhjyDwdKdlsyjDList = znyjDorisOperateService.getWdbWgIndic(warnQuotaId,opDate,"网格",subAreaId,"field_10",fThreshoMinValue);
                List<KhjyDwdKdlsyjD> wgKhjyDwdKdlsyjDList = znyjDorisOperateService.getTopWgIndic(warnQuotaId,opDate,"网格",subAreaId,"field_9" ,"desc",5);
                if(wgKhjyDwdKdlsyjDList != null && wgKhjyDwdKdlsyjDList.size() >0){

                    String subWddWgCount = "";
                    if(warnMsg.indexOf("##") >=0 && warnMsg.indexOf("###") >=0){
                        String subWgMsg1 = warnMsg.substring(warnMsg.indexOf("##"),warnMsg.indexOf("###"));

                        if(StringUtil.isNotEmpty(subWgMsg1)){

                            for(KhjyDwdKdlsyjD wgKhjyDwdKdlsyjD : wgKhjyDwdKdlsyjDList){

                                if(StringUtil.isEmpty(subWddWgCount)){
                                    subWddWgCount = replacePlaceholders(subWgMsg1,wgKhjyDwdKdlsyjD,"");
                                }else{
                                    //subWddWgCount = subWddWgCount+","+replacePlaceholders(subWgMsg2,wgKhjyDwdKdlsyjD);
                                    subWddWgCount = subWddWgCount + replacePlaceholders(subWgMsg1,wgKhjyDwdKdlsyjD,"");
                                }
                            }
                        }
                    }

                    String subLastWgMsg = "";
                    if(warnMsg.indexOf("###") >=0){
                        subLastWgMsg = warnMsg.substring(warnMsg.indexOf("###"));
                    }

                    repWgSubWarnMsg = subWddWgCount + subLastWgMsg;
                }

                repWarnMsg = repXfSubWarnMsg + repWgSubWarnMsg;
            }
        }else if(warnGranula.equals("zj")){ //支局

        }else if(warnGranula.equals("wg")){ //网格

        }

        if(StringUtil.isNotEmpty(repWarnMsg)){
            if(repWarnMsg.indexOf("####")>=0){
                repWarnMsg = repWarnMsg.replaceAll("####","");
            }

            if(repWarnMsg.indexOf("###")>=0){
                repWarnMsg = repWarnMsg.replaceAll("###","");
            }
        }

        log.info("+++++++++++++++{}未达预警时生成预警信息内容：{}",areaId,repWarnMsg);

        return repWarnMsg;
    }

    /**
     * 获取推送人
     * @param warnGranula
     * @return
     */
    public Map<String,String> getPushPerson(String warnGranula, WarnPushPersonSet warnPushPersonSet){
        Map<String,String> retMap = new HashMap<>();
        String pushPersonId = "";
        String pushPersonName ="";

        if(warnGranula.equals("qj") || warnGranula.equals("bdw") ){
            pushPersonId = warnPushPersonSet.getPushPersonId();
            pushPersonName = warnPushPersonSet.getPushPersonName();
        }else{
            String level ="";
            String areaId = warnPushPersonSet.getAreaId();
            if(warnGranula.equals("xf")){
                level ="县分";
            }else if(warnGranula.equals("zj")){
                level ="支局";
            }else if(warnGranula.equals("wg")){
                level ="网格";
            }
            List<VWgzsUserLevelD> vWgzsUserLevelDList = znyjDorisOperateService.getWgUserLevelDListById(areaId,level);
            if(vWgzsUserLevelDList != null && vWgzsUserLevelDList.size() >0){
                for(VWgzsUserLevelD vWgzsUserLevelD : vWgzsUserLevelDList){
                    if(StringUtil.isEmpty(pushPersonId)){
                        pushPersonId = vWgzsUserLevelD.getTelephone();
                    }else{
                        pushPersonId = pushPersonId +","+vWgzsUserLevelD.getTelephone();
                    }

                    if(StringUtil.isEmpty(pushPersonName)){
                        pushPersonName = vWgzsUserLevelD.getUserName()+"["+vWgzsUserLevelD.getTelephone()+"]";
                    }else{
                        pushPersonName = pushPersonName +","+vWgzsUserLevelD.getUserName()+"["+vWgzsUserLevelD.getTelephone()+"]";
                    }
                }
            }
        }
        retMap.put("pushPersonId",pushPersonId);
        retMap.put("pushPersonName",pushPersonName);

        return retMap;
    }
}
