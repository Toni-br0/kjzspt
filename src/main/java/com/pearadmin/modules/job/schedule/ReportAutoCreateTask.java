package com.pearadmin.modules.job.schedule;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.quartz.base.BaseQuartz;
import com.pearadmin.common.tools.DateTimeUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.im.util.ImUtil;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportAutoCreateLog;
import com.pearadmin.modules.report.domain.ReportIndex;
import com.pearadmin.modules.report.domain.ReportInfo;
import com.pearadmin.modules.report.mapper.ReportAutoCreateInfoMapper;
import com.pearadmin.modules.report.mapper.ReportAutoCreateLogMapper;
import com.pearadmin.modules.report.mapper.ReportIndexMapper;
import com.pearadmin.modules.report.service.impl.RepConnSecondTableServiceImpl;
import com.pearadmin.modules.report.util.ReportUtil;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自助取数 自动创建报表并发送到IM
 */

@Slf4j
@Component("reportAutoCreateTask")
public class ReportAutoCreateTask implements BaseQuartz {

    @Value("${report-auto-create-path}")
    private String reportAutoCreatePath;
    @Resource
    private ReportAutoCreateInfoMapper reportAutoCreateInfoMapper;

    @Resource
    private RepConnSecondTableServiceImpl repConnSecondTableService;

    @Resource
    private ImPushObjectManageMapper imPushObjectManageMapper;

    @Resource
    private ImUtil imUtil;

    @Resource
    private ReportAutoCreateLogMapper reportAutoCreateLogMapper;

    @Resource
    private SysUserMapper sysUserMapper;
    @Autowired
    private ReportIndexMapper reportIndexMapper;

    /**
     * 任务实现
     */
    @Override
    public void run(String params) {
        log.info("++++++++++自助取数自动创建报表并发送到IM++++++++++");
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

            //根据开始日期和结束日期查询报表自动创建信息列表
             List<ReportAutoCreateInfo> reportAutoCreateList = reportAutoCreateInfoMapper.getListByTime(twoMinutesBeforeHm,currentHm);
             if(reportAutoCreateList != null && reportAutoCreateList.size() > 0) {
                for(ReportAutoCreateInfo reportAutoCreateInfo : reportAutoCreateList){
                    String sendCycle = reportAutoCreateInfo.getSendCycle();
                    if(sendCycle.equals("monthly")){ //每月
                        String sendDay = reportAutoCreateInfo.getSendDay();
                        if(!sendDay.equals(currentDay)){ //发送日期不是当天
                            continue;
                        }
                    }

                    ReportAutoCreateLog reportAutoCreateLog = new ReportAutoCreateLog();
                    try {

                        reportAutoCreateLog.setReportName(reportAutoCreateInfo.getReportName());
                        reportAutoCreateLog.setSendCycle(sendCycle);
                        reportAutoCreateLog.setSendDay(reportAutoCreateInfo.getSendDay());
                        reportAutoCreateLog.setSendTime(reportAutoCreateInfo.getSendTime());
                        //reportAutoCreateLog.setPushObjectId(reportAutoCreateInfo.getPushObjectId());
                        reportAutoCreateLog.setIndexInfo(reportAutoCreateInfo.getIndexInfo());
                        reportAutoCreateLog.setCreateTime(LocalDateTime.now());
                        reportAutoCreateLog.setCreateUserId(reportAutoCreateInfo.getCreateUserId());
                        reportAutoCreateLog.setDateType(reportAutoCreateInfo.getDateType());
                        reportAutoCreateLog.setPushObjectId(reportAutoCreateInfo.getPushObjectId());
                        reportAutoCreateLog.setIndexId(reportAutoCreateInfo.getIndexId());
                        reportAutoCreateLog.setIsNonStand(reportAutoCreateInfo.getIsNonStand());
                        reportAutoCreateLog.setPushObjectType(reportAutoCreateInfo.getPushObjectType());


                        //更新数据状态为执行中
                        reportAutoCreateInfo.setState("1");
                        reportAutoCreateInfoMapper.updateById(reportAutoCreateInfo);

                        //创建自助取数报表文件
                        JSONObject createFileJson = createReportFile(reportAutoCreateInfo);
                        log.info("++++++++++++++{}--自助取数自动创建报表并发送到IM创建文件结果_{}：{}",reportAutoCreateInfo.getReportName(),createFileJson);
                        String sqlContent = createFileJson.getString("sqlContent");
                        reportAutoCreateLog.setSqlContent(sqlContent);

                        String createFileRetCode = createFileJson.getString("retCode");

                        if(StringUtil.isNotEmpty(createFileRetCode) && createFileRetCode.equals("0")){ //成功
                            String filePath = createFileJson.getString("filePath");

                            reportAutoCreateLog.setFilePath(filePath);
                            if(StringUtils.isNotEmpty(filePath)){

                                String applyResult = reportAutoCreateInfo.getApplyResult() ==null?"":reportAutoCreateInfo.getApplyResult();//申请结果
                                String pushLeaderId = reportAutoCreateInfo.getPushLeaderId();//推送领导ID
                                String pushObjectId = reportAutoCreateInfo.getPushObjectId();

                                //将文件发送至IM

                                JSONObject sendFileJson = null;
                                String pushObjectType = reportAutoCreateInfo.getPushObjectType()==null?"":reportAutoCreateInfo.getPushObjectType();
                                //不是组织维度时
                                if(!pushObjectType.equals("zzwd")){

                                    String pushAllObjectIds = pushObjectId;
                                    //审批通过并且推送领导ID不为空
                                    if(applyResult.equals("2") && StringUtil.isNotEmpty(pushLeaderId)){
                                        pushAllObjectIds =  pushAllObjectIds+";"+pushLeaderId;
                                    }

                                    reportAutoCreateLog.setPushObjectId(pushAllObjectIds);
                                    sendFileJson = sendFileToIM(pushAllObjectIds,filePath);
                                }else{

                                    sendFileJson = sendZzwdFileToIM(pushObjectId,pushLeaderId,filePath,applyResult);
                                    String pushObjectName = sendFileJson.getString("pushObjectName");
                                    //需要拼接
                                    if(StringUtil.isNotEmpty(pushObjectName)){
                                        reportAutoCreateLog.setPushObjectName(pushObjectName+";"+reportAutoCreateInfo.getPushLeaderName());
                                    }
                                }

                                log.info("++++++++++++++自助取数自动创建报表并发送到IM文件发送至IM结果{}：{}",reportAutoCreateInfo.getReportName(),sendFileJson);
                                String sendFileRetCode = sendFileJson.getString("retCode");
                                if(StringUtil.isNotEmpty(sendFileRetCode) && sendFileRetCode.equals("0")){ //成功
                                    reportAutoCreateLog.setIsSuccess("0");
                                    reportAutoCreateLog.setInterRetmsg("文件发送成功");
                                }else{ //失败
                                    reportAutoCreateLog.setIsSuccess("-1");
                                    String sendFileRetMsg = sendFileJson.getString("retMsg");
                                    if(StringUtil.isNotEmpty(sendFileRetMsg) && sendFileRetMsg.length() >1000){
                                        sendFileRetMsg = sendFileRetMsg.substring(0,1000);
                                    }
                                    reportAutoCreateLog.setInterRetmsg(sendFileRetMsg);
                                }
                            }
                        }else{ //失败
                            reportAutoCreateLog.setIsSuccess("-1");
                            String createFileRetMsg = createFileJson.getString("retMsg");
                            if(StringUtil.isNotEmpty(createFileRetMsg) && createFileRetMsg.length() >1000){
                                createFileRetMsg = createFileRetMsg.substring(0,1000);
                            }
                            reportAutoCreateLog.setInterRetmsg(createFileRetMsg);
                        }

                    }catch (Exception ex){
                        ex.printStackTrace();
                        reportAutoCreateLog.setIsSuccess("-1");
                        if(ex.toString().length() >1000){
                            reportAutoCreateLog.setInterRetmsg(ex.toString().substring(0,1000));
                        }else{
                            reportAutoCreateLog.setInterRetmsg(ex.toString());
                        }

                    }finally {
                        reportAutoCreateInfo.setState("0");
                        reportAutoCreateInfoMapper.updateById(reportAutoCreateInfo);
                        reportAutoCreateLogMapper.insert(reportAutoCreateLog);
                    }
                }
             }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 创建自助取数报表文件
     * @param reportAutoCreateInfo
     */
    private JSONObject createReportFile(ReportAutoCreateInfo reportAutoCreateInfo){
        JSONObject retJsonObj = new JSONObject();
        ExcelWriter excelWriter = null;
        try {
                String sqlContent = reportAutoCreateInfo.getSqlContent();
                String dateType = reportAutoCreateInfo.getDateType();

                String reportName = reportAutoCreateInfo.getReportName();
                String fileName = "";

                String indexIds = reportAutoCreateInfo.getIndexId() == null?"":reportAutoCreateInfo.getIndexId();

                String createUserName ="";
                String createUserId = reportAutoCreateInfo.getCreateUserId();
                if(StringUtil.isNotEmpty(createUserId)){
                   SysUser sysUser = sysUserMapper.selectById(createUserId);
                   if(sysUser !=null){
                       createUserName = sysUser.getRealName();
                   }
                }

                //替换日期
                if(dateType.equals("month")){ //月
                    /*if(sqlContent.indexOf("{repl_date") >=0){
                       String replDate = sqlContent.substring(sqlContent.indexOf("{repl_date")+10,sqlContent.indexOf("}"));
                        String strDate = "";
                        if(replDate.equals("")){
                            strDate = DateTimeUtil.getLastMonth("yyyyMM");
                        }else{
                            int iReplDate = Integer.parseInt(replDate);
                            strDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",iReplDate);
                        }

                        sqlContent = sqlContent.replaceAll("\\{repl_date"+replDate+"}",strDate);
                    }*/

                    String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                    if(sqlContent.indexOf("{repl_date}") >=0){
                        sqlContent = sqlContent.replaceAll("\\{repl_date}",strDate);
                    }

                    if(sqlContent.indexOf("{repl_date-1}") >=0){
                        String strBefoDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",-2);
                        sqlContent = sqlContent.replaceAll("\\{repl_date-1}",strBefoDate);
                    }

                    if(sqlContent.indexOf("{repl_other_date}") >=0){
                        String strBefoDate = DateTimeUtil.getLastMonth("yyyyMM");
                        sqlContent = sqlContent.replaceAll("\\{repl_other_date}",strBefoDate);
                    }

                    if(sqlContent.indexOf("{repl_other_date-1}") >=0){
                        String strBefoDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",-2);
                        sqlContent = sqlContent.replaceAll("\\{repl_other_date-1}",strBefoDate);
                    }

                    fileName = reportName+"_"+createUserName +"_"+strDate+ ".xlsx";

                }else{ //日
                    /*String replDate = "";
                    if(sqlContent.indexOf("{repl_date") >=0){
                        replDate = sqlContent.substring(sqlContent.indexOf("{repl_date")+10,sqlContent.indexOf("}"));
                        String strDate = "";
                        if(replDate.equals("")){
                            strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                        }else{
                            int iReplDate = Integer.parseInt(replDate);
                            strDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",iReplDate);
                        }

                        sqlContent = sqlContent.replaceAll("\\{repl_date"+replDate+"}",strDate);
                    }*/

                    String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                    if(sqlContent.indexOf("{repl_date}") >=0){
                        sqlContent = sqlContent.replaceAll("\\{repl_date}",strDate);
                    }

                    if(sqlContent.indexOf("{repl_date-1}") >=0){
                        //获取当前日期的前两天
                        String strBefoDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",-2);
                        sqlContent = sqlContent.replaceAll("\\{repl_date-1}",strBefoDate);
                    }

                    if(sqlContent.indexOf("{repl_other_date}") >=0){
                        String strBefoDate = DateTimeUtil.getYesterday("yyyyMMdd");
                        sqlContent = sqlContent.replaceAll("\\{repl_other_date}",strBefoDate);
                    }

                    if(sqlContent.indexOf("{repl_other_date-1}") >=0){
                        String strBefoDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",-2);
                        sqlContent = sqlContent.replaceAll("\\{repl_other_date-1}",strBefoDate);
                    }

                    fileName = reportName+"_"+createUserName +"_"+strDate+ ".xlsx";

                }

                sqlContent = sqlContent.toUpperCase();
                log.info("++++++++++++创建报表文件查询SQL：{}",sqlContent);
                retJsonObj.put("sqlContent",sqlContent);

                List<Map<String, Object>> retReportDataList = repConnSecondTableService.getReportDataListMap(sqlContent);
                if(retReportDataList != null && retReportDataList.size() > 0) {

                    String selArea ="";
                    //2025-11-20 判读是地市、区县还是网格
                    String indexInfo = reportAutoCreateInfo.getIndexInfo();
                    if(StringUtil.isNotEmpty(indexInfo)){
                        JSONArray indexJsonArr = JSONArray.parseArray(indexInfo);
                        if(indexJsonArr != null && indexJsonArr.size() > 0){
                            for(int i=0;i<indexJsonArr.size();i++){
                                JSONObject indexJsonObj = indexJsonArr.getJSONObject(i);
                                String fieldValue = indexJsonObj.getString("field");
                                if(fieldValue.equals("hxLatnName") || fieldValue.equals("hxAreaName") || fieldValue.equals("xHx5BpName")){
                                    selArea = fieldValue;
                                    break;
                                }
                            }
                        }
                    }

                    List headsList = new ArrayList<>();
                    List<String> idList = new ArrayList<>();

                    //2025-11-20
                    if(selArea.equals("hxAreaName")){ //区县
                        List<String> dsHeadList = new ArrayList<>();
                        dsHeadList.add("地市");
                        headsList.add(dsHeadList);

                        //idList.add("hx_latn_name");
                        idList.add("hxLatnName");
                    }else if(selArea.equals("xHx5BpName")){ //网格
                        List<String> dsHeadList = new ArrayList<>();
                        dsHeadList.add("地市");
                        headsList.add(dsHeadList);

                        //idList.add("hx_latn_name");
                        idList.add("hxLatnName");

                        List<String> qxHeadList = new ArrayList<>();
                        qxHeadList.add("区县");
                        headsList.add(qxHeadList);

                        //idList.add("hx_area_name");
                        idList.add("hxAreaName");
                    }

                    String col = reportAutoCreateInfo.getIndexInfo();
                    if(StringUtils.isNotBlank(col)){
                        JSONArray colArr = JSON.parseArray(col);
                        if(colArr != null && colArr.size() > 0){
                            for(int i = 0; i < colArr.size(); i ++){
                                JSONObject colObj = colArr.getJSONObject(i);
                                String title = colObj.getString("title");

                                /*String id = ReportUtil.convertToLowercaseWithUnderscore(colObj.getString("field"));
                                idList.add(id);*/

                                idList.add(colObj.getString("field"));

                                List<String> headList = new ArrayList<>();
                                headList.add(title);
                                headsList.add(headList);

                            }
                        }

                        //计算合计列数据
                        if(retReportDataList !=null && retReportDataList.size() >0){
                            //全区，不统计合计
                            if(sqlContent.indexOf("SEL_ALL_QUAN_QU") <= 0){

                                //Map<String, Object> dataMap = retReportDataList.get(0);
                                // 从List<Map<String, Object>>中获取键数量最多的Map
                                /*Map<String, Object> dataMap = retReportDataList.stream()
                                        .max(Comparator.comparingInt(Map::size))
                                        .orElse(null); // 如果list为空，返回null*/
                                //2025-12-5
                                //通过指标配置，获取表名和是否是大宽表
                                boolean isTjKb = false; //是否统计宽表
                                String tableName = "";

                                List<Integer> reportIndexIdsList = Arrays.stream(indexIds.split(";"))
                                        .map(Integer::parseInt) // 简写：每个字符串转Integer
                                        .collect(Collectors.toList());

                               if(reportIndexIdsList != null && reportIndexIdsList.size() > 0){
                                   List<ReportIndex> reportIndexList = reportIndexMapper.selectBatchIds(reportIndexIdsList);
                                   if(reportIndexList != null && reportIndexList.size() > 0){
                                       ReportIndex reportIndex = reportIndexList.get(0);
                                       tableName = reportIndex.getTableName();
                                       String isFiveInten = reportIndex.getIsFiveInten();
                                       if(StringUtil.isNotEmpty(isFiveInten) && isFiveInten.equals("1")){ //统计宽表
                                           isTjKb = true;
                                       }
                                   }
                               }

                                Map<String, Object> newAllMap = new HashMap<>();
                                for(Map<String, Object> dataMap : retReportDataList){
                                    newAllMap.putAll(dataMap);
                                }

                                if(!isTjKb){ //非统计宽表
                                    Set<String> keyset = newAllMap.keySet();
                                    Map<String, Object> totalMap = new HashMap<>();
                                    for(String key : keyset){
                                        Object objValue = newAllMap.get(key);

                                        if(key.equalsIgnoreCase("HX_LATN_NAME")){
                                            totalMap.put(key,"合计");
                                        }else if(key.equalsIgnoreCase("HX_AREA_NAME") || key.equalsIgnoreCase("X_HX5_BP_NAME")){
                                            totalMap.put(key,"-");
                                        }else if(objValue != null && objValue instanceof String && (objValue.toString().endsWith("%") || objValue.toString().endsWith("pp") || objValue.toString().endsWith("PP") )){
                                            //BigDecimal average = calculateAver(retReportDataList, key);
                                            boolean isLv = true;
                                            if(key.endsWith("_hb") || key.endsWith("_HB")){
                                                isLv = false;
                                            }
                                            String averageStr = calculateSumLv(retReportDataList, key,isLv);
                                            if(objValue.toString().endsWith("%")){
                                                averageStr = averageStr+"%";
                                            }else if(objValue.toString().endsWith("pp")){
                                                averageStr = averageStr+"pp";
                                            }else if(objValue.toString().endsWith("PP")){
                                                averageStr = averageStr+"PP";
                                            }

                                            totalMap.put(key,averageStr);
                                        }else if(objValue instanceof Number){ //数值求合
                                            BigDecimal totalSum = calculateSum(retReportDataList, key);
                                            totalMap.put(key,totalSum);
                                        }else{
                                            totalMap.put(key,"-");
                                        }
                                    }

                                    retReportDataList.add(totalMap);
                                }else { //统计宽表
                                    //计算合计列数据
                                    retReportDataList = getCountListTjkb(retReportDataList,tableName);
                                }
                            }
                        }


                        // 动态构建数据
                        List<List<Object>> exportData = buildDataDown(retReportDataList, idList);
                        // 确保目录存在
                        File dir = new File(reportAutoCreatePath);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        // 完整文件路径
                        String filePath = reportAutoCreatePath + fileName;

                        // 创建表头样式和内容样式
                        HorizontalCellStyleStrategy styleStrategy = createStyleStrategy();

                        excelWriter = EasyExcel.write(filePath).build();
                        WriteSheet writeSheet = EasyExcel.writerSheet(reportName)
                                .head(headsList)
                                /*.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                                .registerWriteHandler(ExcelUtil.createCellStyle())*/
                                .registerWriteHandler(styleStrategy)
                                .build();

                        // 动态添加表头
                        excelWriter.write(exportData, writeSheet);
                        excelWriter.finish();

                        /*if(exportData != null && exportData.size() > 0){
                            List<List<Object>> newExportData = new ArrayList<>();
                            for(int i=0;i<exportData.size();i++){
                                newExportData.add(exportData.get(i));
                                if(i!=0 && i%2000 == 0){
                                    excelWriter.write(newExportData, writeSheet);
                                    newExportData.clear();
                                    log.info("+++++newExportData——i: {}",i);
                                }
                            }

                            // 处理最后剩余的不足2000条的数据
                            if (exportData.size() % 2000 != 0) {
                                excelWriter.write(newExportData, writeSheet);
                                newExportData.clear();
                                log.info("----newExportData——size: {}",exportData.size() % 2000);
                            }

                            excelWriter.finish();
                        }*/

                        log.info("+++++createReportFile_filePath: {}",filePath);

                        retJsonObj.put("retCode","0");
                        retJsonObj.put("filePath",filePath);
                        retJsonObj.put("retMsg","创建报表文件成功");
                    }
                }else{
                    retJsonObj.put("retCode","-1");
                    retJsonObj.put("retMsg","创建报表文件失败，查询数据为空");
                }

        } catch (Exception e) {
            e.printStackTrace();
            retJsonObj.put("retCode","-1");
            retJsonObj.put("retMsg","创建报表文件失败，系统异常："+e.getMessage());
        }finally {
            if(excelWriter != null){
                excelWriter.finish();
            }
        }

        return retJsonObj;
    }

    private static List<List<Object>> buildDataDown(List<Map<String, Object>> dataList, List<String> fieldOrder) {
        List<List<Object>> data = new ArrayList<>();

        List<Map<String, Object>> repDataList = replaceDashInKeys(dataList);

        for (Map<String, Object> map : repDataList) {
            List<Object> row = new ArrayList<>();
            for (String field : fieldOrder) {
                row.add(map.get(field.toUpperCase()));
            }
            data.add(row);
        }
        return data;
    }

    /**
     * 替换Map中键名称中的-和_
     * @param list
     * @return
     */
    public static List<Map<String, Object>> replaceDashInKeys(List<Map<String, Object>> list) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> map : list) {
            Map<String, Object> newMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                // 替换键中的 '-' 为空
                String newKey = entry.getKey().replace("-", "");
                newKey =newKey.replace("_","");
                newMap.put(newKey, entry.getValue());
            }

            result.add(newMap);
        }

        return result;
    }

    /**
     * 创建表头和内容的样式策略
     */
    private static HorizontalCellStyleStrategy createStyleStrategy() {
        // 表头样式设置
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();

        // 设置表头背景色（这里使用POI的IndexedColors）
        headWriteCellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        headWriteCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        //headWriteCellStyle.setWrapped(false); // 强制表头不换行

        // 设置表头边框
        headWriteCellStyle.setBorderTop(BorderStyle.THIN);
        headWriteCellStyle.setBorderRight(BorderStyle.THIN);
        headWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        headWriteCellStyle.setBorderLeft(BorderStyle.THIN);

        // 设置表头对齐方式
        headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 设置表头字体
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontName("微软雅黑");
        headWriteFont.setFontHeightInPoints((short) 11);  // 字体大小
        headWriteFont.setBold(true);  // 加粗
        headWriteCellStyle.setWriteFont(headWriteFont);

        // 内容样式设置
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();

        // 内容边框
        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);

        // 内容对齐方式
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.LEFT);
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 内容字体
        WriteFont contentWriteFont = new WriteFont();
        contentWriteFont.setFontName("微软雅黑");
        contentWriteFont.setFontHeightInPoints((short) 11);
        contentWriteCellStyle.setWriteFont(contentWriteFont);

        // 返回样式策略（表头样式，内容样式）
        return new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
    }

    /**
     * 将文件推送至IM
     * @param pushObjectId
     * @param filePath
     */
    private JSONObject sendFileToIM(String pushObjectId,String filePath){
        JSONObject retJson  = new JSONObject();
        FileInputStream inputStream = null;
        try {
            //字符数组转 整型List
            String[] pushObjectIdArr = pushObjectId.split(";");
            List<Integer> pushObjectIdList = new ArrayList<>();
            for(int i=0;i<pushObjectIdArr.length;i++){
                String strPushObjectId = pushObjectIdArr[i];
                pushObjectIdList.add(Integer.parseInt(strPushObjectId));
            }

            String lastPushObjectId ="";

            //根据推送对象管理id集合，查询推送对象集合
            List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
            if(imPushObjectManageList !=null && imPushObjectManageList.size() > 0){
                //拼接推送对象id
                //用户对象id 格式为 [user1,user2,user3]
                //群对象id 格式为 group1;group2;group3
                //最终推送格式为 用户对象id + 群对象id
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
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","推送对象["+queryPushObjectName+"]未绑定");
                        return retJson;

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

                    //推送文件
                    if(filePath != null && filePath.trim().length() >0){
                        File file = new File(filePath);
                        inputStream = new FileInputStream(file);
                        String originalFilename = file.getName();
                        String contentType = "application/octet-stream"; // 根据文件类型修改
                        MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);
                        inputStream.close();

                        //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                        String targetType = "";
                        String target = "";
                        String fileType ="pushFile";

                        for(String strPushObjectId : arrPushObjectId){
                            target = strPushObjectId;
                            //用户
                            if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                                targetType = "users";
                            }else{
                                targetType = "group";
                            }

                            String sendResult = imUtil.sendImFile(multipartFile,targetType,target,fileType);
                            log.info("++++++++++ReportAutoCreateTask_sendFileToIM_{}_文件推送结果：{}",originalFilename,sendResult);
                            if(StringUtil.isNotEmpty(sendResult)){
                                JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                String status = bindingJson.getString("status");
                                //推送成功
                                if(StringUtil.isNotEmpty(status) && "success".equals(status)){
                                    retJson.put("retCode","0");
                                    retJson.put("retMsg","报表文件推送成功");
                                }else{ //推送失败
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","报表文件推送失败: "+sendResult);
                                }
                            }else{
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","报表文件推送接口返回空");
                            }
                        }
                    }
                }
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","未查询到推送对象信息");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","报表文件推送系统异常: "+e.getMessage());
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return retJson;
    }

    /**
     * 将文件推送至IM
     * @param pushObjectId
     * @param filePath
     */
    private JSONObject sendZzwdFileToIM(String pushObjectId,String pushLeaderId,String filePath,String applyResult){
        JSONObject retJson  = new JSONObject();
        FileInputStream inputStream = null;
        String pushObjectName ="";
        try {
            String lastPushObjectId ="";
            //根据 ID查询IM推送对象信息
            ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectById(pushObjectId);
            if(imPushObjectManage != null){
                lastPushObjectId = imPushObjectManage.getPushObjectId();
                pushObjectName = imPushObjectManage.getPushObjectName();
            }

            if(applyResult.equals("2") && StringUtil.isNotEmpty(pushLeaderId)){
                //字符数组转 整型List
                String[] pushObjectIdArr = pushLeaderId.split(";");

                for(int i=0;i<pushObjectIdArr.length;i++){
                    String strPushObjectId = pushObjectIdArr[i];
                    if(StringUtil.isEmpty(lastPushObjectId)){
                        lastPushObjectId = strPushObjectId;
                    }else{
                        lastPushObjectId = lastPushObjectId+","+strPushObjectId;
                    }
                }
            }


            lastPushObjectId = "["+lastPushObjectId+"]";

            //推送文件
            if(filePath != null && filePath.trim().length() >0){
                File file = new File(filePath);
                inputStream = new FileInputStream(file);
                String originalFilename = file.getName();
                String contentType = "application/octet-stream"; // 根据文件类型修改
                MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);
                inputStream.close();

                //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                String targetType = "users";

                String fileType ="pushFile";


                    String sendResult = imUtil.sendImFile(multipartFile,targetType,lastPushObjectId,fileType);
                    log.info("++++++++++ReportAutoCreateTask_sendFileToIM_{}_文件推送结果：{}",originalFilename,sendResult);
                    if(StringUtil.isNotEmpty(sendResult)){
                        JSONObject bindingJson = JSONObject.parseObject(sendResult);
                        String status = bindingJson.getString("status");
                        //推送成功
                        if(StringUtil.isNotEmpty(status) && "success".equals(status)){
                            retJson.put("retCode","0");
                            retJson.put("retMsg","报表文件推送成功");
                        }else{ //推送失败
                            retJson.put("retCode","-1");
                            retJson.put("retMsg","报表文件推送失败: "+sendResult);
                        }
                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","报表文件推送接口返回空");
                    }
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","报表文件推送系统异常: "+e.getMessage());
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }

            retJson.put("pushObjectName",pushObjectName);
        }

        return retJson;
    }

    /**
     * 计算合计
     * @param dataList
     * @param fieldName
     * @return
     */
    public static BigDecimal calculateSum (List<Map<String, Object>> dataList, String fieldName) {

        BigDecimal total = BigDecimal.ZERO;
        if (dataList == null || dataList.isEmpty()) {
            return total;
        }
        for (Map<String, Object> map : dataList) {

            Object value = map.get (fieldName);

            if (value != null) {
                BigDecimal num;
                if (value instanceof Number) {

                    num = new BigDecimal (value.toString ());
                } else if (value instanceof String) {

                    try {
                        num = new BigDecimal((String) value);
                    } catch (NumberFormatException e) {

                        continue;
                    }
                } else {

                    continue;
                }

                total = total.add (num);
            }
        }
        return total;
    }

    /**
     * 计算平均值
     * @param dataList
     * @param fieldName
     * @return
     */
    public static BigDecimal calculateAver(List<Map<String, Object>> dataList, String fieldName) {

        BigDecimal total = BigDecimal.ZERO;
        if (dataList == null || dataList.isEmpty()) {
            return total;
        }
        int index =0;
        for (Map<String, Object> map : dataList) {

            Object value = map.get (fieldName);

            if (value != null) {
                BigDecimal num = BigDecimal.ZERO;
                if (value instanceof String) {
                    try {
                        if(((String) value).indexOf("%") >0){
                            String newValue = ((String) value).replaceAll("%","");
                            num = new BigDecimal((String) newValue);
                            index++;
                        }

                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                total = total.add (num);
            }
        }

        BigDecimal aver = BigDecimal.ZERO;

        if(index != 0){
            aver = total.divide(BigDecimal.valueOf(index), 2, RoundingMode.HALF_UP);
        }

        return aver;
    }

    /**
     * 计算合计(率)
     * @param dataList
     * @param fieldName
     * @return
     */
    public static String calculateSumLv(List<Map<String, Object>> dataList, String fieldName,boolean isLv) {

        BigDecimal fzTotal = BigDecimal.ZERO;
        BigDecimal fmTotal = BigDecimal.ZERO;

        BigDecimal fzErTotal = BigDecimal.ZERO;
        BigDecimal fmErTotal = BigDecimal.ZERO;

        if (dataList == null || dataList.isEmpty()) {
            return "-";
        }
        for (Map<String, Object> map : dataList) {
            //分子1
            Object fzValue = map.get (fieldName+"_fenzi");
            if(fzValue ==null){
                fzValue = map.get (fieldName+"_FENZI");
            }
            if (fzValue != null) {
                BigDecimal fzNum;
                if (fzValue instanceof Number) {

                    fzNum = new BigDecimal (fzValue.toString ());
                } else if (fzValue instanceof String) {

                    try {
                        fzNum = new BigDecimal((String) fzValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fzTotal = fzTotal.add (fzNum);
            }

            //分母1
            Object fmValue = map.get (fieldName+"_fenmu");
            if(fmValue ==null){
                fmValue = map.get (fieldName+"_FENMU");
            }

            if (fmValue != null) {
                BigDecimal fmNum;
                if (fmValue instanceof Number) {

                    fmNum = new BigDecimal (fmValue.toString ());
                } else if (fmValue instanceof String) {

                    try {
                        fmNum = new BigDecimal((String) fmValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fmTotal = fmTotal.add (fmNum);
            }

            //分子2
            Object fzErValue = map.get (fieldName+"_fenzier");
            if(fzErValue ==null){
                fzErValue = map.get (fieldName+"_FENZIER");
            }
            if (fzErValue != null) {
                BigDecimal fzErNum;
                if (fzErValue instanceof Number) {

                    fzErNum = new BigDecimal (fzErValue.toString ());
                } else if (fzErValue instanceof String) {

                    try {
                        fzErNum = new BigDecimal((String) fzErValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fzErTotal = fzErTotal.add (fzErNum);
            }

            //分母2
            Object fmErValue = map.get (fieldName+"_fenmuer");
            if(fmErValue ==null){
                fmErValue = map.get (fieldName+"_FENMUER");
            }

            if (fmErValue != null) {
                BigDecimal fmErNum;
                if (fmErValue instanceof Number) {

                    fmErNum = new BigDecimal (fmErValue.toString ());
                } else if (fmErValue instanceof String) {

                    try {
                        fmErNum = new BigDecimal((String) fmErValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fmErTotal = fmErTotal.add (fmErNum);
            }

        }

        BigDecimal total = BigDecimal.ZERO;
        //率
        if(isLv){

            if(fmTotal == BigDecimal.ZERO || fmTotal.compareTo(BigDecimal.ZERO) == 0){
                return "-";
            }

            total = fzTotal.divide(fmTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                    .multiply(new BigDecimal("100"))          // 乘以100
                    .setScale(2, RoundingMode.HALF_UP);
        }else{ //环比

            if(fmTotal == BigDecimal.ZERO || fmTotal.compareTo(BigDecimal.ZERO) == 0){
                return "-";
            }

            if(fzErTotal == BigDecimal.ZERO || fzErTotal.compareTo(BigDecimal.ZERO) == 0){
                return "-";
            }

            if(fmErTotal == BigDecimal.ZERO || fmErTotal.compareTo(BigDecimal.ZERO) == 0){
                return "-";
            }

            BigDecimal subTotal = fzTotal.divide(fmTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                    .multiply(new BigDecimal("100"))          // 乘以100
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal subTotal2 = fzErTotal.divide(fmErTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                    .multiply(new BigDecimal("100"))          // 乘以100
                    .setScale(2, RoundingMode.HALF_UP);

            total = subTotal.subtract(subTotal2);
        }

        return total+"";
    }

    /**
     * 计算合计列 统计大宽表
     * @param retReportDataList
     * @return
     */
    public List<Map<String,Object>> getCountListTjkb(List<Map<String,Object>> retReportDataList,String tableName){
        //计算合计列数据
        if(retReportDataList !=null && retReportDataList.size() >0){
            Map<String, Object> totalMap = new HashMap<>();
            //Map<String, Object> dataMap = retReportDataList.get(0);
            // 从List<Map<String, Object>>中获取键数量最多的Map
            /*Map<String, Object> dataMap = retReportDataList.stream()
                    .max(Comparator.comparingInt(Map::size))
                    .orElse(null); // 如果list为空，返回null*/

            Map<String, Object> newAllMap = new HashMap<>();
            for(Map<String, Object> dataMap : retReportDataList){
                newAllMap.putAll(dataMap);
            }

            Set<String> keyset = newAllMap.keySet();
            ReportIndex reportIndex = null;

            for(String key : keyset){

                //通过表名和字段名查询指标信息
                QueryWrapper<ReportIndex> indexQueryWrapper = new QueryWrapper<>();
                indexQueryWrapper.eq("table_name",tableName);
                indexQueryWrapper.eq("column_name",key);
                indexQueryWrapper.last("limit 1");

                reportIndex = reportIndexMapper.selectOne(indexQueryWrapper);
                if(reportIndex != null){
                    String indexClass = reportIndex.getIndexClass()==null?"":reportIndex.getIndexClass();
                    Object objValue = newAllMap.get(key);

                    if(key.equalsIgnoreCase("hx_latn_name")){
                        totalMap.put(key,"合计");
                    }else if(key.equalsIgnoreCase("hx_area_name") || key.equalsIgnoreCase("x_hx5_bp_name")){
                        totalMap.put(key,"-");
                    }else if(indexClass.equalsIgnoreCase("lv") || indexClass.equalsIgnoreCase("zb")){ //率,占比
                        String averageStr = calculateSumLvTjkb(retReportDataList, key,true);
                        if(!averageStr.equals("-")){
                            if(objValue.toString().endsWith("%")){
                                averageStr = averageStr+"%";
                            }else if(objValue.toString().endsWith("pp")){
                                averageStr = averageStr+"pp";
                            }else if(objValue.toString().endsWith("PP")){
                                averageStr = averageStr+"PP";
                            }
                        }

                        totalMap.put(key,averageStr);
                    }else if(indexClass.equalsIgnoreCase("hj")){ //户均
                        String averageStr = calculateSumHj(retReportDataList, key);

                        totalMap.put(key,averageStr);
                    }else if(indexClass.equalsIgnoreCase("hb")){ //环比
                        String averageStr = calculateSumLvTjkb(retReportDataList, key,false);
                        if(!averageStr.equals("-")){
                            if(objValue.toString().endsWith("%")){
                                averageStr = averageStr+"%";
                            }else if(objValue.toString().endsWith("pp")){
                                averageStr = averageStr+"pp";
                            }else if(objValue.toString().endsWith("PP")){
                                averageStr = averageStr+"PP";
                            }
                        }

                        totalMap.put(key,averageStr);
                    }else if(StringUtil.isEmpty(indexClass) && objValue instanceof Number){ //数值求合
                        BigDecimal totalSum = calculateSumTjkb(retReportDataList, key);
                        totalMap.put(key,totalSum);
                    }else{
                        totalMap.put(key,"-");
                    }

                }else if(key.equalsIgnoreCase("hx_latn_name")){
                    totalMap.put(key,"合计");
                }else if(key.equalsIgnoreCase("hx_area_name") || key.equalsIgnoreCase("x_hx5_bp_name")){
                    totalMap.put(key,"-");
                }else{
                    totalMap.put(key,"-");
                }

                /*Object objValue = newAllMap.get(key);

                if(key.equalsIgnoreCase("hx_latn_name")){
                    totalMap.put(key,"合计");
                }else if(key.equalsIgnoreCase("hx_area_name") || key.equalsIgnoreCase("x_hx5_bp_name")){
                    totalMap.put(key,"-");
                }else if(objValue != null && objValue instanceof String && (objValue.toString().endsWith("%") || objValue.toString().endsWith("pp") || objValue.toString().endsWith("PP"))){
                    //BigDecimal average = calculateAver(retReportDataList, key);
                    boolean isLv = true;
                    if(key.endsWith("_hb") || key.endsWith("_HB")){
                        isLv = false;
                    }
                    String averageStr = calculateSumLvTjkb(retReportDataList, key,isLv);
                    if(!averageStr.equals("-")){
                        if(objValue.toString().endsWith("%")){
                            averageStr = averageStr+"%";
                        }else if(objValue.toString().endsWith("pp")){
                            averageStr = averageStr+"pp";
                        }else if(objValue.toString().endsWith("PP")){
                            averageStr = averageStr+"PP";
                        }
                    }

                    totalMap.put(key,averageStr);
                }else if(objValue instanceof Number){ //数值求合
                    BigDecimal totalSum = calculateSumTjkb(retReportDataList, key);
                    totalMap.put(key,totalSum);
                }else{
                    totalMap.put(key,"-");
                }*/
            }
            retReportDataList.add(totalMap);
        }

        return retReportDataList;
    }

    /**
     * 计算合计(率) 统计大宽表
     * @param dataList
     * @param fieldName
     * @return
     */
    public static String calculateSumLvTjkb(List<Map<String, Object>> dataList, String fieldName,boolean isLv) {

        BigDecimal fzTotal = BigDecimal.ZERO;
        BigDecimal fmTotal = BigDecimal.ZERO;

        BigDecimal fzErTotal = BigDecimal.ZERO;
        BigDecimal fmErTotal = BigDecimal.ZERO;

        if (dataList == null || dataList.isEmpty()) {
            return "-";
        }
        for (Map<String, Object> map : dataList) {
            //分子1
            Object fzValue = map.get (fieldName+"_fenzi");
            if(fzValue ==null){
                fzValue = map.get (fieldName+"_FENZI");
            }
            if (fzValue != null) {
                BigDecimal fzNum;
                if (fzValue instanceof Number) {

                    fzNum = new BigDecimal (fzValue.toString ());
                } else if (fzValue instanceof String) {

                    try {
                        fzNum = new BigDecimal((String) fzValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fzTotal = fzTotal.add (fzNum);
            }

            //分母1
            Object fmValue = map.get (fieldName+"_fenmu");
            if(fmValue ==null){
                fmValue = map.get (fieldName+"_FENMU");
            }

            if (fmValue != null) {
                BigDecimal fmNum;
                if (fmValue instanceof Number) {

                    fmNum = new BigDecimal (fmValue.toString ());
                } else if (fmValue instanceof String) {

                    try {
                        fmNum = new BigDecimal((String) fmValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fmTotal = fmTotal.add (fmNum);
            }

            //分子2
            Object fzErValue = map.get (fieldName+"_fenzier");
            if(fzErValue ==null){
                fzErValue = map.get (fieldName+"_FENZIER");
            }
            if (fzErValue != null) {
                BigDecimal fzErNum;
                if (fzErValue instanceof Number) {

                    fzErNum = new BigDecimal (fzErValue.toString ());
                } else if (fzErValue instanceof String) {

                    try {
                        fzErNum = new BigDecimal((String) fzErValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fzErTotal = fzErTotal.add (fzErNum);

            }

            //分母2
            Object fmErValue = map.get (fieldName+"_fenmuer");
            if(fmErValue ==null){
                fmErValue = map.get (fieldName+"_FENMUER");
            }

            if (fmErValue != null) {
                BigDecimal fmErNum;
                if (fmErValue instanceof Number) {

                    fmErNum = new BigDecimal (fmErValue.toString ());
                } else if (fmErValue instanceof String) {

                    try {
                        fmErNum = new BigDecimal((String) fmErValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fmErTotal = fmErTotal.add (fmErNum);

            }

        }

        BigDecimal total = BigDecimal.ZERO;
        //率
        if(isLv){

            if(fmTotal == BigDecimal.ZERO || fmTotal.compareTo(BigDecimal.ZERO) == 0){
                return "-";
            }

            total = fzTotal.divide(fmTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                    .multiply(new BigDecimal("100"))          // 乘以100
                    .setScale(2, RoundingMode.HALF_UP);
        }else{ //环比

            if(fmTotal == BigDecimal.ZERO || fmTotal.compareTo(BigDecimal.ZERO) == 0){
                return "-";
            }

            if(fzErTotal == BigDecimal.ZERO || fzErTotal.compareTo(BigDecimal.ZERO) == 0){
                return "-";
            }

            if(fmErTotal == BigDecimal.ZERO || fmErTotal.compareTo(BigDecimal.ZERO) == 0){
                return "-";
            }

            BigDecimal subTotal = fzTotal.divide(fmTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                    .multiply(new BigDecimal("100"))          // 乘以100
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal subTotal2 = fzErTotal.divide(fmErTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                    .multiply(new BigDecimal("100"))          // 乘以100
                    .setScale(2, RoundingMode.HALF_UP);

            total = subTotal.subtract(subTotal2);
        }

        return total+"";
    }

    /**
     * 计算合计 统一宽表
     * @param dataList
     * @param fieldName
     * @return
     */
    public static BigDecimal calculateSumTjkb(List<Map<String, Object>> dataList, String fieldName) {

        BigDecimal total = BigDecimal.ZERO;
        if (dataList == null || dataList.isEmpty()) {
            return total;
        }
        for (Map<String, Object> map : dataList) {

            Object value = map.get (fieldName);

            if (value != null) {
                BigDecimal num;
                if (value instanceof Number) {

                    num = new BigDecimal (value.toString ());
                } else if (value instanceof String) {

                    try {
                        num = new BigDecimal((String) value);
                    } catch (NumberFormatException e) {

                        continue;
                    }
                } else {

                    continue;
                }

                total = total.add (num);
            }
        }
        return total;
    }

    /**
     * 计算合计(率) 户均
     * @param dataList
     * @param fieldName
     * @return
     */
    public static String calculateSumHj(List<Map<String, Object>> dataList, String fieldName) {

        BigDecimal fzTotal = BigDecimal.ZERO;
        BigDecimal fmTotal = BigDecimal.ZERO;

        if (dataList == null || dataList.isEmpty()) {
            return "-";
        }
        for (Map<String, Object> map : dataList) {
            //分子1
            Object fzValue = map.get (fieldName+"_fenzi");
            if(fzValue ==null){
                fzValue = map.get (fieldName+"_FENZI");
            }
            if (fzValue != null) {
                BigDecimal fzNum;
                if (fzValue instanceof Number) {

                    fzNum = new BigDecimal (fzValue.toString ());
                } else if (fzValue instanceof String) {

                    try {
                        fzNum = new BigDecimal((String) fzValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fzTotal = fzTotal.add (fzNum);
            }

            //分母1
            Object fmValue = map.get (fieldName+"_fenmu");
            if(fmValue ==null){
                fmValue = map.get (fieldName+"_FENMU");
            }

            if (fmValue != null) {
                BigDecimal fmNum;
                if (fmValue instanceof Number) {

                    fmNum = new BigDecimal (fmValue.toString ());
                } else if (fmValue instanceof String) {

                    try {
                        fmNum = new BigDecimal((String) fmValue);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                fmTotal = fmTotal.add (fmNum);
            }
        }

        BigDecimal total = BigDecimal.ZERO;


        if(fmTotal == BigDecimal.ZERO || fmTotal.compareTo(BigDecimal.ZERO) == 0){
            return "-";
        }

        total = fzTotal.divide(fmTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                .multiply(new BigDecimal("1"))          // 乘以100
                .setScale(2, RoundingMode.HALF_UP);

        return total+"";
    }

}
