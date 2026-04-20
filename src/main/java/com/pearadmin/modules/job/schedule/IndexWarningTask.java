package com.pearadmin.modules.job.schedule;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.quartz.base.BaseQuartz;
import com.pearadmin.common.tools.DateTimeUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.im.util.ImUtil;
import com.pearadmin.modules.report.domain.ReportIndex;
import com.pearadmin.modules.report.domain.ReportIndexWarning;
import com.pearadmin.modules.report.domain.ReportIndexWarningLog;
import com.pearadmin.modules.report.mapper.ReportIndexMapper;
import com.pearadmin.modules.report.mapper.ReportIndexWarningLogMapper;
import com.pearadmin.modules.report.mapper.ReportIndexWarningMapper;
import com.pearadmin.modules.report.service.RepConnSecondTableService;
import com.pearadmin.modules.report.service.impl.RepConnSecondTableServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.LocalDate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 创建日期：2025-10-21
 * 指标预警定时任务
 **/

@Slf4j
@Component("indexWarningTask")
public class IndexWarningTask implements BaseQuartz {

  @Resource
  private ReportIndexWarningMapper reportIndexWarningMapper;

  @Resource
  private ReportIndexMapper reportIndexMapper;


  @Resource
  private RepConnSecondTableServiceImpl repConnSecondTableService;

  @Resource
  private ImPushObjectManageMapper imPushObjectManageMapper;

  @Resource
  private ImUtil imUtil;

  @Resource
  private ReportIndexWarningLogMapper reportIndexWarningLogMapper;


  private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");


  @Override
  public void run(String params) throws Exception {
    log.info("++++++++++指标预警定时任务++++++++++");
    try {
        LocalDateTime now = LocalDateTime.now();

        // 获取时间信息
        String currentDay = now.format(DAY_FORMATTER);
        String currentHm = now.format(TIME_FORMATTER);
        DayOfWeek currentWeek = now.getDayOfWeek();

        // 计算查询时间范围
        int minutesBefore = Integer.parseInt(params);
        String startTime = now.minusMinutes(minutesBefore).format(TIME_FORMATTER);

        //根据开始日期和结束日期查询指标预警信息
        List<ReportIndexWarning> warningList = reportIndexWarningMapper.getListByTime(startTime,currentHm);

        if (CollectionUtils.isNotEmpty(warningList)) {
            processWarnings(warningList, currentDay, currentWeek);
        }

    }catch (Exception e){
      e.printStackTrace();
      log.error("处理报表指标预警任务失败, params: {}", params, e);
    }

  }

    private void processWarnings(List<ReportIndexWarning> warnings, String currentDay, DayOfWeek currentWeek) {
        /*for (ReportIndexWarning warning : warnings) {
             processSingleWarning(warning, currentDay, currentWeek);
        }*/

        int batchSize = 10; // 每批处理数量
        int total = warnings.size();

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<ReportIndexWarning> batch = warnings.subList(i, end);

            batch.parallelStream().forEach(warning -> processSingleWarning(warning, currentDay, currentWeek));
        }

    }

    private void processSingleWarning(ReportIndexWarning warning, String currentDay, DayOfWeek currentWeek) {
        try {
            String execPeriod = warning.getExecPeriod();
            // 校验执行条件
            if(execPeriod.equals("month")){//月
                String execDay = warning.getExecDay()==null?"":warning.getExecDay();
                if(!execDay.equals(currentDay)){ //发送日期不是当天
                    return;
                }
            }else if(execPeriod.equals("week")){//周
                String exeWeek = warning.getExecWeek()==null?"":warning.getExecWeek();
                if(!currentWeek.equals(exeWeek)){
                    return;
                }
            }

            // 更新为执行中状态
            updateWarningState(warning, "1", (warning.getExecCount() == null ? 0 : warning.getExecCount()) + 1);

            // 执行核心业务
            queryIndexValueAndSendWarnMsg(warning);

            // 恢复为未执行状态
            updateWarningState(warning, "0", warning.getExecCount());

        } catch (Exception e) {
            log.error("处理单个预警任务失败, ID: {}", warning.getWarningId(), e);
            // 异常时恢复状态
            recoverWarningState(warning);
        }
    }

    /**
     * 更新预警状态及次数
     * @param warning
     * @param state
     * @param execCount
     */
    private void updateWarningState(ReportIndexWarning warning, String state, Integer execCount) {
        warning.setExecState(state);
        warning.setExecCount(execCount);
        reportIndexWarningMapper.updateById(warning);
    }

    /**
     * 更新预警状态为初始状态
     * @param warning
     */
    private void recoverWarningState(ReportIndexWarning warning) {
        warning.setExecState("0");
        reportIndexWarningMapper.updateById(warning);
    }

    /**
     *  查询指标值,并发送预警信息
     * @return
     */
    public JSONObject queryIndexValueAndSendWarnMsg(ReportIndexWarning reportIndexWarning){
        JSONObject retJsonObject = new JSONObject();

        String warningMsg ="";
        String isSuccess ="0";
        String resultMsg ="";
        String execSqlContent = "";
        String lastPustObjectIds ="";

        try {

            int indexId = reportIndexWarning.getWarningIndexId(); //指标ID
            //获取指标信息
            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
            if(reportIndex != null){
                String indexName = reportIndex.getIndexName(); //指标名称
                String tableName = reportIndex.getTableName();  //表名
                String tableExpr = reportIndex.getTableExpr();  //字段表达式
                String warningName = reportIndexWarning.getWarningName(); //预警名称

                String localCity = reportIndexWarning.getLocalCity(); //本地网名称
                String indexStanValue = reportIndexWarning.getIndexStanValue().trim(); //指标标准值
                String strIndexStanValue = indexStanValue;
                String indexDeviRange = reportIndexWarning.getIndexDeviRange().trim(); //指标偏离范围
                warningMsg = reportIndexWarning.getWarningMsg();

                String dataPeriod = reportIndexWarning.getDataPeriod(); //数据周期
                //替换时间
                tableExpr = replaceTableExprDate(tableExpr,dataPeriod);

                //拼接SQL语句(地市)
                //String sqlContent ="select "+tableExpr +","+tableName+".hx_latn_name from "+tableName +" "+tableName +" where 1=1 and "+tableName+".hx_latn_name='"+localCity+"' group by "+tableName+".hx_latn_name";sqlContent = sqlContent.toUpperCase();
                //String sqlContent ="select "+tableExpr +" from "+tableName +" "+tableName +" where 1=1 and "+tableName+".hx_latn_name='"+localCity+"' group by "+tableName+".hx_latn_name";
                String sqlContent = buildSql(tableExpr,tableName,localCity,dataPeriod);
                execSqlContent = sqlContent;
                log.info("+++++++++++++++地市：{} +++++预警名称：{} -- 指标预警SQL：{}",localCity,warningName,sqlContent);
                List<Map<String, Object>> retReportDataList = repConnSecondTableService.getReportDataListMap(sqlContent);
                if(retReportDataList != null && retReportDataList.size() > 0){
                    Map<String, Object> dataMap = retReportDataList.get(0);
                    if(dataMap !=null && dataMap.size() >0){
                        String currentIndexValue = "";
                        String indexValue = "";
                        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                            //System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
                            /*if(!entry.getKey().equals("HX_LATN_NAME")){
                                indexValue = entry.getValue().toString();
                                currentIndexValue = entry.getValue().toString();
                            }*/

                            indexValue = entry.getValue().toString();
                            currentIndexValue = entry.getValue().toString();
                        }

                        if(StringUtil.isNotEmpty(indexValue)){
                            if(indexValue.indexOf("%") >=0){
                                indexValue = indexValue.replaceAll("%","");
                            }

                            double dIndexValue = Double.parseDouble(indexValue);

                            if(indexStanValue.indexOf("%") >=0){
                                indexStanValue = indexStanValue.replaceAll("%","");
                            }

                            //标准值
                            double dIndexStanValue = Double.parseDouble(indexStanValue);

                            JSONObject sendJsonObject = null;
                            //高于
                            if(indexDeviRange.equals("above")){
                                if(dIndexValue > dIndexStanValue){ //高于时预警
                                    warningMsg = replaceWarningMsg(warningMsg,warningName,currentIndexValue,"高于",strIndexStanValue);
                                    warningMsg = localCity+" "+warningMsg;
                                    //发送预警信息
                                    lastPustObjectIds = reportIndexWarning.getPushObjectId();
                                    String apprState = reportIndexWarning.getApprState()==null?"":reportIndexWarning.getApprState();
                                    if(apprState.equals("1")){ //审批通过，推送人加上其他推送人
                                        lastPustObjectIds = lastPustObjectIds+";"+reportIndexWarning.getPushOtherObjectId();
                                    }
                                    log.info("+++++++++++++++地市：{} +++++预警名称：{} -- 推送对象ID：{}",localCity,warningName,lastPustObjectIds);
                                    sendJsonObject = sendIndexWarningMsg(warningMsg,lastPustObjectIds);
                                }else{
                                    warningMsg = "关键指标："+indexName+" 当前值为："+currentIndexValue
                                               +" 未高于标准值："+strIndexStanValue +" 预警未触发";
                                    warningMsg = localCity+" "+warningMsg;
                                }
                            }else if(indexDeviRange.equals("below")){ //低于
                                if(dIndexValue < dIndexStanValue){ //低于时预警
                                    warningMsg = replaceWarningMsg(warningMsg,warningName,currentIndexValue,"低于",strIndexStanValue);
                                    warningMsg = localCity+" "+warningMsg;
                                    //发送预警信息
                                    lastPustObjectIds = reportIndexWarning.getPushObjectId();
                                    String apprState = reportIndexWarning.getApprState()==null?"":reportIndexWarning.getApprState();
                                    if(apprState.equals("1")){ //审批通过，推送人加上其他推送人
                                        lastPustObjectIds = lastPustObjectIds+";"+reportIndexWarning.getPushOtherObjectId();
                                    }
                                    log.info("+++++++++++++++地市：{} +++++预警名称：{} -- 推送对象ID：{}",localCity,warningName,lastPustObjectIds);
                                    sendJsonObject = sendIndexWarningMsg(warningMsg,lastPustObjectIds);
                                }else{
                                    warningMsg = "关键指标："+indexName+" 当前值为："+currentIndexValue
                                            +" 未低于标准值："+strIndexStanValue +" 预警未触发";
                                    warningMsg = localCity+" "+warningMsg;
                                }
                            }

                            if(sendJsonObject != null){ //触发预警，执行推送操作
                                String sendRetCode = sendJsonObject.getString("retCode");
                                if(sendRetCode.equals("0")){ //发送成功
                                    resultMsg = "执行成功";
                                }else { //发送失败
                                    isSuccess = "-1";
                                    resultMsg = sendJsonObject.getString("retMsg");
                                }
                            }else { //未触发预警，未执行推送操作
                                resultMsg ="未达到预警触发条件，预警未被触发";
                            }
                        }else{
                            isSuccess = "-1";
                            resultMsg ="查询指标结果数据为空";
                        }
                    }else{
                        isSuccess = "-1";
                        resultMsg ="未查询到指标结果数据";
                    }
                }else{
                    isSuccess = "-1";
                    resultMsg ="未查询到数据信息";
                }
            }else{//未查询到指标信息
                isSuccess = "-1";
                resultMsg ="根据指标ID["+indexId+"]未查询到指标信息";
            }

        }catch (Exception e){
            e.printStackTrace();
            isSuccess = "-1";
            resultMsg = "系统异常："+e.toString();
        }finally {
            saveWarningLog(reportIndexWarning,warningMsg,isSuccess,resultMsg,execSqlContent);
        }
        return retJsonObject;
    }

    /**
     * 构建SQL语句
     * @param tableExpr
     * @param tableName
     * @param localCity
     * @return
     */
    private String buildSql(String tableExpr, String tableName, String localCity,String dataPeriod) {
        /*return String.format("select %s from %s %s where 1=1 and %s.hx_latn_name='%s' group by %s.hx_latn_name",
                tableExpr, tableName, tableName, tableName, localCity, tableName);*/

        String retSql = String.format("select %s from %s %s where 1=1 and %s.hx_latn_name='%s' ", tableExpr, tableName, tableName, tableName, localCity);
        if(retSql.indexOf(".op_date") <0 && retSql.indexOf(".OP_DATE") <0){
            String strDate ="";
            if(dataPeriod.equals("month")){//月
                strDate = DateTimeUtil.getLastMonth("yyyyMM");
            }else{ //日
                strDate = DateTimeUtil.getYesterday("yyyyMMdd");
            }
            retSql = retSql + " and "+tableName+".op_date='"+strDate+"'";
        }
        return retSql;
    }

    /**
     * 替换时间
     * @param tableExpr
     * @param dataPeriod
     * @return
     */
    private String replaceTableExprDate(String tableExpr,String dataPeriod){

        if(dataPeriod.equals("month")){ //月
            //获取上个月日期
            String strDate = DateTimeUtil.getLastMonth("yyyyMM");
            if(tableExpr.indexOf("{repl_date}") >=0){
                tableExpr = tableExpr.replaceAll("\\{repl_date}",strDate);
            }

            if(tableExpr.indexOf("{repl_date-1}") >=0){
                String strBefoDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",-2);
                tableExpr = tableExpr.replaceAll("\\{repl_date-1}",strBefoDate);
            }

            if(tableExpr.indexOf("{repl_other_date}") >=0){
                String strBefoDate = DateTimeUtil.getLastMonth("yyyyMM");
                tableExpr = tableExpr.replaceAll("\\{repl_other_date}",strBefoDate);
            }

            if(tableExpr.indexOf("{repl_other_date-1}") >=0){
                String strBefoDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",-2);
                tableExpr = tableExpr.replaceAll("\\{repl_other_date-1}",strBefoDate);
            }
        }else{
            String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
            if(tableExpr.indexOf("{repl_date}") >=0){
                tableExpr = tableExpr.replaceAll("\\{repl_date}",strDate);
            }

            if(tableExpr.indexOf("{repl_date-1}") >=0){
                //获取当前日期的前两天
                String strBefoDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",-2);
                tableExpr = tableExpr.replaceAll("\\{repl_date-1}",strBefoDate);
            }

            if(tableExpr.indexOf("{repl_other_date}") >=0){
                String strBefoDate = DateTimeUtil.getYesterday("yyyyMMdd");
                tableExpr = tableExpr.replaceAll("\\{repl_other_date}",strBefoDate);
            }

            if(tableExpr.indexOf("{repl_other_date-1}") >=0){
                String strBefoDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",-2);
                tableExpr = tableExpr.replaceAll("\\{repl_other_date-1}",strBefoDate);
            }
        }

        return tableExpr;
    }

    /**
     * 替换预警信息中的占位符
     * @param warningMsg
     * @param warningName
     * @param currentIndexValue
     * @param pllx
     * @param strIndexStanValue
     * @return
     */
    private String replaceWarningMsg(String warningMsg,String warningName,String currentIndexValue,String pllx,String strIndexStanValue){
        warningMsg = warningMsg.replaceAll("\\{zbmc}",warningName);
        warningMsg = warningMsg.replaceAll("\\{dqz}",currentIndexValue);
        warningMsg = warningMsg.replaceAll("\\{pllx}",pllx);
        warningMsg = warningMsg.replaceAll("\\{zbbzz}",strIndexStanValue);
        return warningMsg;
    }

    /**
     * 保存预警日志信息
     * @param reportIndexWarning
     * @param warningMsg
     * @param isSuccess
     * @param resultMsg
     * @param execSqlContent
     */
    private void saveWarningLog(ReportIndexWarning reportIndexWarning,String warningMsg,String isSuccess,String resultMsg,String execSqlContent){
        ReportIndexWarningLog reportIndexWarningLog = new ReportIndexWarningLog();
        reportIndexWarningLog.setLogId(IdUtil.simpleUUID());
        reportIndexWarningLog.setWarningIndexId(reportIndexWarning.getWarningIndexId());
        reportIndexWarningLog.setLocalCity(reportIndexWarning.getLocalCity());
        reportIndexWarningLog.setIndexStanValue(reportIndexWarning.getIndexStanValue());
        reportIndexWarningLog.setIndexDeviRange(reportIndexWarning.getIndexDeviRange());
        reportIndexWarningLog.setPushObjectId(reportIndexWarning.getPushObjectId());
        reportIndexWarningLog.setPushOtherObjectId(reportIndexWarning.getPushOtherObjectId());
        reportIndexWarningLog.setExecPeriod(reportIndexWarning.getExecPeriod());
        reportIndexWarningLog.setExecDay(reportIndexWarning.getExecDay());
        reportIndexWarningLog.setExecTime(reportIndexWarning.getExecTime());
        reportIndexWarningLog.setCreateTime(LocalDateTime.now());
        reportIndexWarningLog.setCreateUserId(reportIndexWarning.getCreateUserId());
        reportIndexWarningLog.setWarningLevel(reportIndexWarning.getWarningLevel());
        reportIndexWarningLog.setExecWeek(reportIndexWarning.getExecWeek());
        reportIndexWarningLog.setIndexClass(reportIndexWarning.getIndexClass());
        reportIndexWarningLog.setWarningMsg(warningMsg);
        reportIndexWarningLog.setIsSuccess(isSuccess);
        if(StringUtil.isNotEmpty(resultMsg) && resultMsg.length() >=1000){
            resultMsg = resultMsg.substring(0,1000);
        }
        reportIndexWarningLog.setResultMsg(resultMsg);
        reportIndexWarningLog.setExecSqlContent(execSqlContent);

        reportIndexWarningLog.setApprState(reportIndexWarning.getApprState());
        reportIndexWarningLog.setApprPersonId(reportIndexWarning.getApprPersonId());
        reportIndexWarningLog.setApprTime(reportIndexWarning.getApprTime());
        reportIndexWarningLog.setApplyOpinion(reportIndexWarning.getApplyOpinion());
        reportIndexWarningLog.setDataPeriod(reportIndexWarning.getDataPeriod());
        reportIndexWarningLog.setWarningName(reportIndexWarning.getWarningName());

        reportIndexWarningLogMapper.insert(reportIndexWarningLog);
    }



    /**
     * 发送指标预警信息
     * @return
     */
    public JSONObject sendIndexWarningMsg(String warningMsg, String pushObjectId) {
        JSONObject retJsonObject = new JSONObject();

        try {
            // 使用Stream将字符串数组转换为整型List
            List<Integer> pushObjectIdList = Arrays.stream(pushObjectId.split(";"))
                    .map(String::trim)
                    .filter(str -> !str.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            // 根据推送对象管理id集合，查询推送对象集合
            List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);

            if (imPushObjectManageList == null || imPushObjectManageList.isEmpty()) {
                retJsonObject.put("retCode", "-1");
                retJsonObject.put("retMsg", "未查询到推送对象信息");
                return retJsonObject;
            }

            // 检查绑定时间并收集推送对象名称
            Optional<ImPushObjectManage> unboundObject = imPushObjectManageList.stream()
                    .filter(obj -> obj.getBindTime() == null)
                    .findFirst();

            if (unboundObject.isPresent()) {
                retJsonObject.put("retCode", "-1");
                retJsonObject.put("retMsg", "推送对象[" + unboundObject.get().getPushObjectName() + "]未绑定");
                return retJsonObject;
            }

            // 使用Stream分离用户和群组
            Map<Boolean, List<ImPushObjectManage>> groupedByType = imPushObjectManageList.stream()
                    .collect(Collectors.partitioningBy(obj -> "users".equals(obj.getPushObjectType())));

            List<ImPushObjectManage> userObjects = groupedByType.get(true);
            List<ImPushObjectManage> groupObjects = groupedByType.get(false);

            // 构建推送对象ID字符串
            String userPushObjectId = buildUserPushObjectId(userObjects);
            String groupPushObjectId = buildGroupPushObjectId(groupObjects);
            String lastPushObjectId = buildFinalPushObjectId(userPushObjectId, groupPushObjectId);

            // 构建推送对象名称
            String pushObjectName = imPushObjectManageList.stream()
                    .map(ImPushObjectManage::getPushObjectName)
                    .collect(Collectors.joining(";"));

            // 发送消息
            return sendMessages(warningMsg, lastPushObjectId, pushObjectName);

        } catch (NumberFormatException e) {
            log.error("推送对象ID格式错误", e);
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "推送对象ID格式错误");
            return retJsonObject;
        } catch (Exception e) {
            log.error("发送指标预警消息异常", e);
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "系统异常：推送预警信息失败");
            return retJsonObject;
        }
    }

    /**
     * 构建用户推送对象ID
     */
    private String buildUserPushObjectId(List<ImPushObjectManage> userObjects) {
        if (userObjects == null || userObjects.isEmpty()) {
            return "";
        }

        String userIds = userObjects.stream()
                .map(ImPushObjectManage::getPushObjectId)
                .collect(Collectors.joining(","));

        return "[" + userIds + "]";
    }

    /**
     * 构建群组推送对象ID
     */
    private String buildGroupPushObjectId(List<ImPushObjectManage> groupObjects) {
        if (groupObjects == null || groupObjects.isEmpty()) {
            return "";
        }

        return groupObjects.stream()
                .map(ImPushObjectManage::getPushObjectId)
                .collect(Collectors.joining(";"));
    }

    /**
     * 构建最终的推送对象ID
     */
    private String buildFinalPushObjectId(String userPushObjectId, String groupPushObjectId) {
        if (!userPushObjectId.isEmpty() && !groupPushObjectId.isEmpty()) {
            return userPushObjectId + ";" + groupPushObjectId;
        } else if (!userPushObjectId.isEmpty()) {
            return userPushObjectId;
        } else {
            return groupPushObjectId;
        }
    }

    /**
     * 发送消息给所有推送对象
     */
    private JSONObject sendMessages(String warningMsg, String lastPushObjectId, String pushObjectName) {
        JSONObject retJsonObject = new JSONObject();

        if (lastPushObjectId == null || lastPushObjectId.isEmpty()) {
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "推送对象ID为空");
            return retJsonObject;
        }

        String[] arrPushObjectId = lastPushObjectId.split(";");

        // 使用Stream处理所有推送对象
        Optional<JSONObject> failedResult = Arrays.stream(arrPushObjectId)
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .map(pushId -> sendSingleMessage(warningMsg, pushId))
                .filter(result -> !"0".equals(result.getString("retCode")))
                .findFirst();

        if (failedResult.isPresent()) {
            return failedResult.get();
        } else {
            retJsonObject.put("retCode", "0");
            retJsonObject.put("retMsg", "预警信息推送成功");
            return retJsonObject;
        }
    }

    /**
     * 发送单条消息
     */
    private JSONObject sendSingleMessage(String warningMsg, String pushObjectId) {
        JSONObject result = new JSONObject();

        try {
            // 确定目标类型
            String targetType = pushObjectId.startsWith("[") && pushObjectId.endsWith("]") ? "users" : "group";
            String target = pushObjectId;

            String sendResult = imUtil.sendImMsg(warningMsg, targetType, target);
            log.info("++++++++++++++++++++指标预警发送预警信息结果：{}", sendResult);

            if (StringUtil.isEmpty(sendResult)) {
                result.put("retCode", "-1");
                result.put("retMsg", "预警信息推送失败: 推送接口返回空");
                return result;
            }

            JSONObject bindingJson = JSONObject.parseObject(sendResult);
            String status = bindingJson.getString("status");

            if (StringUtil.isNotEmpty(status) && "success".equals(status)) {
                result.put("retCode", "0");
                result.put("retMsg", "预警信息推送成功: "+sendResult);
            } else {
                result.put("retCode", "-1");
                result.put("retMsg", "预警信息推送失败: " + sendResult);
            }

        } catch (Exception e) {
            log.error("发送单条预警消息失败, pushObjectId: {}", pushObjectId, e);
            result.put("retCode", "-1");
            result.put("retMsg", "发送预警消息异常: " + e.toString());
        }

        return result;
    }

    /**
     * 发送指标预警信息
     * @return
     */
    /*public JSONObject sendIndexWarningMsg(String warningMsg,String pushObjectId){
        JSONObject retJsonObject = new JSONObject();
        try {

            //字符数组转 整型List
            String[] pushObjectIdArr = pushObjectId.split(";");
            List<Integer> pushObjectIdList = new ArrayList<>();
            for(int i=0;i<pushObjectIdArr.length;i++){
                String strPushObjectId = pushObjectIdArr[i];
                pushObjectIdList.add(Integer.parseInt(strPushObjectId));
            }


            //根据推送对象管理id集合，查询推送对象集合
            List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
            if(imPushObjectManageList != null && imPushObjectManageList.size() > 0){
                //拼接推送对象id
                //用户对象id 格式为 [user1,user2,user3]
                //群对象id 格式为 group1;group2;group3
                //最终推送格式为 用户对象id + 群对象id
                String lastPushObjectId ="";
                String userPushObjectId = "";
                String groupPushObjectId = "";
                int userIndex =0;
                int groupIndex =0;
                int index =0;
                String pushObjectName ="";
                for(ImPushObjectManage imPushObjectManage : imPushObjectManageList){
                    String pustObjectType = imPushObjectManage.getPushObjectType();
                    String queryPushObjectName = imPushObjectManage.getPushObjectName();
                    LocalDateTime bindTime = imPushObjectManage.getBindTime();

                    if(index ==0){
                        pushObjectName = pushObjectName + imPushObjectManage.getPushObjectName();
                    }else{
                        pushObjectName = pushObjectName +";"+ imPushObjectManage.getPushObjectName();
                    }

                    index++;

                    if(bindTime ==null){
                        retJsonObject.put("retCode","-1");
                        retJsonObject.put("retMsg","推送对象["+queryPushObjectName+"]未绑定");
                        return retJsonObject;
                    }

                    if(pustObjectType.equals("users")){ //用户
                        if(userIndex ==0){
                            userPushObjectId = userPushObjectId +imPushObjectManage.getPushObjectId();
                        }else{
                            userPushObjectId = userPushObjectId +","+imPushObjectManage.getPushObjectId();
                        }

                        userIndex++;
                    }else if(pustObjectType.equals("group")){ //群
                        if(groupIndex ==0){
                            groupPushObjectId = groupPushObjectId +imPushObjectManage.getPushObjectId();
                        }else{
                            groupPushObjectId = groupPushObjectId +";"+imPushObjectManage.getPushObjectId();
                        }

                        groupIndex++;
                    }
                }

                if(!userPushObjectId.equals("")){
                    userPushObjectId = "["+userPushObjectId+"]";
                    lastPushObjectId = lastPushObjectId + userPushObjectId;
                }

                if(!groupPushObjectId.equals("")){
                    if(userPushObjectId.equals("")){
                        lastPushObjectId = lastPushObjectId + groupPushObjectId;
                    }else{
                        lastPushObjectId = lastPushObjectId +";"+ groupPushObjectId;
                    }
                }

                String[] arrPushObjectId = lastPushObjectId.split(";");
                if(arrPushObjectId != null && arrPushObjectId.length >0){
                    String targetType ="";
                    String target = "";
                    for(String strPushObjectId : arrPushObjectId){
                        target = strPushObjectId;
                        //用户
                        if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                            targetType = "users";
                        }else{
                            targetType = "group";
                        }

                        String sendResult = imUtil.sendImMsg(warningMsg,targetType,target);
                        log.info("+++++++++++++++++++++指标预警发送预警信息结果：{}",sendResult);
                        if(StringUtil.isNotEmpty(sendResult)){
                            JSONObject bindingJson = JSONObject.parseObject(sendResult);
                            String status = bindingJson.getString("status");
                            //推送成功
                            if(StringUtil.isNotEmpty(status) && "success".equals(status)){
                                retJsonObject.put("retCode","0");
                                retJsonObject.put("retMsg","预警信息推送成功");
                            }else{ //推送失败
                                retJsonObject.put("retCode","-1");
                                retJsonObject.put("retMsg","预警信息推送失败: "+sendResult);
                            }
                        }else{
                            retJsonObject.put("retCode","-1");
                            retJsonObject.put("retMsg","预警信息推送失败: 推送接口返回空");
                        }
                    }
                }
            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","未查询到推送对象信息");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","系统异常：推送预警信息失败");
        }
        return retJsonObject;
    }*/

}
