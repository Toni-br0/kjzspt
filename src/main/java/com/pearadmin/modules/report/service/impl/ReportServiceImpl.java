package com.pearadmin.modules.report.service.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.DateTimeUtil;
import com.pearadmin.common.tools.check.CheckUtil;
import com.pearadmin.common.tools.excel.ExcelUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.modules.ppt.util.ToolUtil;
import com.pearadmin.modules.report.domain.*;
import com.pearadmin.modules.report.mapper.*;
import com.pearadmin.modules.report.service.ReportService;
import com.pearadmin.modules.report.util.ReportUtil;
import com.pearadmin.modules.sys.domain.SysDept;
import com.pearadmin.modules.sys.domain.SysDictData;
import com.pearadmin.modules.sys.domain.SysRole;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.mapper.SysDeptMapper;
import com.pearadmin.modules.sys.mapper.SysDictDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.bcel.generic.RET;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Value("${report-target-path}")
    private String reportTargetPath;

    @Value("${report-template-path}")
    private String reportTemplatePath;

    @Resource
    private ReportClassifyMapper reportClassifyMapper;

    @Resource
    private ReportDataMapper reportDataMapper;

    @Resource
    private ReportInfoMapper reportInfoMapper;

    @Resource
    private ReportDimensionMapper reportDimensionMapper;

    @Resource
    private TemplateInfoMapper templateInfoMapper;

    @Resource
    private ReportIndexMapper reportIndexMapper;

    @Resource
    private ReportConditMapper reportConditMapper;

    @Resource
    private RepConnSecondTableServiceImpl repConnSecondTableService;

    @Resource
    private Environment env;

    @Resource
    private SysDeptMapper sysDeptMapper;

    @Resource
    private ReportAutoCreateInfoMapper reportAutoCreateInfoMapper;

    @Resource
    private ReportPustLeaderApprovalMapper reportPustLeaderApprovalMapper;

    @Resource
    private SysDictDataMapper sysDictDataMapper;


    /**
     * 查询分类
     * @return
     */
    @Override
    public List<ReportClassify> searchClassify() {

        String kczzqsjsName ="kczzqsjs";
        String kcZzqsClassCode ="";
        boolean isKczzqsjs = false;


        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        //是否是超级管理员或全部数据查看角色
        boolean isAdmin = CheckUtil.hasSuperAdmin(currentUser);

        List<SysRole> roleList = currentUser.getRoles();

        //非超级管理员或全部数据查看角色
        if(!isAdmin){
            //获取数据字典中科创自助取数对应的分类名
            QueryWrapper<SysDictData> dictDataQueryWrapper = new QueryWrapper<>();
            dictDataQueryWrapper.eq("data_label",kczzqsjsName);
            dictDataQueryWrapper.last("limit 1");
            SysDictData sysDictData = sysDictDataMapper.selectOne(dictDataQueryWrapper);
            if(sysDictData != null){
                kcZzqsClassCode = sysDictData.getDataValue();
            }

            //当前登录人是否有科创自助取数角色
            if(roleList != null && roleList.size() > 0){
                SysRole kczzqsRole = roleList.stream()
                        .filter(role -> kczzqsjsName.equals(role.getRoleCode()))
                        .findFirst()
                        .orElse(null);

                if(kczzqsRole != null){
                    isKczzqsjs = true;
                }
            }
        }

        QueryWrapper<ReportClassify> classifyQu = new QueryWrapper<>();
        //classifyQu.eq("state", "0");
        if(!isAdmin){
            if(isKczzqsjs){
                classifyQu.eq("classify_code",kcZzqsClassCode);
            }else{
                classifyQu.ne("classify_code",kcZzqsClassCode);
            }
        }

        classifyQu.orderByAsc("sort_id");
        List<ReportClassify> classifyList = reportClassifyMapper.selectList(classifyQu);
        return classifyList;
    }


    public List<ReportClassify> searchMainClassisyList(ReportClassify reportClassify) {
        QueryWrapper<ReportClassify> classifyQu = new QueryWrapper<>();
        classifyQu.eq("parent_id", "1");
        classifyQu.eq("state", "0");
        classifyQu.orderByDesc("create_time");
//        List<ReportClassify> classifyList = reportClassifyMapper.selectList(classifyQu);
//        JSONArray classifyArr =new JSONArray();
//        if(!classifyList.isEmpty()){
//            for(int i = 0; i < classifyList.size(); i ++){
//                ReportClassify classify = classifyList.get(i);
//                JSONObject classifyObj = new JSONObject();
//                classifyObj.put("title", classify.getClassifyName());
//                classifyObj.put("id",classify.getClassifyId());
//                classifyObj.put("field",classify.getField());
//                classifyObj.put("parentId",classify.getParentId());
//                if (i == 0) {
//                    classifyObj.put("spread", true);
//                }
//                JSONArray childrenClassifyArr = searchChildrenClassify(classify.getClassifyId());
//                classifyObj.put("children", childrenClassifyArr);
//                classifyArr.add(classifyObj);
//            }
//        }
        return null;
    }

//    public JSONArray searchChildrenClassify(String parentId) {
//        QueryWrapper<ReportClassify> classifyQu = new QueryWrapper<>();
//        classifyQu.eq("parent_id", parentId);
//        classifyQu.eq("state", "0");
//        classifyQu.orderByDesc("create_time");
//        List<ReportClassify> classifyList = reportClassifyMapper.selectList(classifyQu);
//        JSONArray classifyArr =new JSONArray();
//        if(!classifyList.isEmpty()){
//            for(ReportClassify classify : classifyList){
//                JSONObject classifyObj = new JSONObject();
//                classifyObj.put("id", classify.getClassifyId());
//                classifyObj.put("title",classify.getClassifyName());
//                classifyObj.put("field",classify.getField());
//                classifyObj.put("parentId",classify.getParentId());
//                classifyArr.add(classifyObj);
//            }
//        }
//        return classifyArr;
//    }
//
//    public JSONArray searchThreeChildrenClassify(String parentId) {
//        QueryWrapper<ReportClassify> classifyQu = new QueryWrapper<>();
//        classifyQu.eq("parent_id", parentId);
//        classifyQu.eq("state", "0");
//        classifyQu.orderByDesc("create_time");
//        List<ReportClassify> classifyList = reportClassifyMapper.selectList(classifyQu);
//        JSONArray classifyArr =new JSONArray();
//        if(!classifyList.isEmpty()){
//            for(ReportClassify classify : classifyList){
//                JSONObject classifyObj = new JSONObject();
//                classifyObj.put("id", classify.getClassifyId());
//                classifyObj.put("title",classify.getClassifyName());
//                classifyObj.put("field",classify.getField());
//                classifyObj.put("parentId",classify.getParentId());
//                classifyArr.add(classifyObj);
//            }
//        }
//        return classifyArr;
//    }

    /**
     * 获取数据信息
     * @param reportParam
     * @param pageDomain
     * @return
     */
    @Override
    public PageInfo<RetReportData> getReportList(ReportParam reportParam, PageDomain pageDomain) {
        //PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        List<RetReportData> retReportDataList = new ArrayList<>();
        if(reportParam.getIndexType().equals("details")){ //明细
            retReportDataList = getReportDetailsList(reportParam, pageDomain);
        }else{ //统计
            retReportDataList = getReportCountList(reportParam, pageDomain);
        }

        return new PageInfo<>(retReportDataList);
    }

    /**
     * 获取数据信息
     * @param reportParam
     * @param pageDomain
     * @return
     */
    @Override
    public Map<String, Object> reportListNew(ReportParam reportParam, PageDomain pageDomain) {

        JSONObject dataJson = new JSONObject();
        if(reportParam.getIndexType().equals("details")){ //明细
            dataJson = getReportDetailsListNew(reportParam, pageDomain);
        }else{ //统计

            String dimensionInfo = reportParam.getDimensionInfo();
            //2025-11-25
            String indexInfo = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexInfo) && indexInfo.indexOf("\"isFiveInten\":\"1\"") >=0){ //统计大宽表

                //页面维度全区，地市，区县，网格 一键选择功能 ，选择了其中一个
                if(StringUtil.isNotEmpty(dimensionInfo) &&
                        (dimensionInfo.indexOf("selAllQuanQu") >=0 || dimensionInfo.indexOf("selAllDiShi") >=0
                                || dimensionInfo.indexOf("selAllQuXian") >=0 || dimensionInfo.indexOf("selAllWangGe") >=0)){
                    dataJson = getReportCountAllSelectList(reportParam, pageDomain,true);
                }else{//单独勾选地市，区县，网格
                    dataJson = getReportCountListNewTjkb(reportParam, pageDomain);
                }
            }else{//非统计大宽表
                //页面维度全区，地市，区县，网格 一键选择功能 ，选择了其中一个
                if(StringUtil.isNotEmpty(dimensionInfo) &&
                        (dimensionInfo.indexOf("selAllQuanQu") >=0 || dimensionInfo.indexOf("selAllDiShi") >=0
                                || dimensionInfo.indexOf("selAllQuXian") >=0 || dimensionInfo.indexOf("selAllWangGe") >=0)){
                    dataJson = getReportCountAllSelectList(reportParam, pageDomain,false);
                }else{//单独勾选地市，区县，网格
                    dataJson = getReportCountListNew(reportParam, pageDomain);
                }
            }

        }

        List<Map<String, Object>> list = (List<Map<String, Object>>)dataJson.get("dataList");
        long dataCount = (Long)dataJson.get("dataCount");

        // 模拟数据
        /*List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("isMqF", i);
            map.put("isMq", "用户" + i);
            map.put("isCs", "user" + i + "@example.com");
            map.put("isCsF", i % 2 == 0 ? "男" : "女");
            map.put("hxLatnName", "城市" + i);
            list.add(map);
        }*/
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "成功");
        result.put("count", dataCount); // 总记录数（这里为了简单，直接写10。实际中需要根据数据库查询总数）
        result.put("data", list);
        return result;

    }

    /**
     * 获取报表详情数据
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private List<RetReportData> getReportDetailsList(ReportParam reportParam, PageDomain pageDomain){
        List<RetReportData> retReportDataList = new ArrayList<>();

        try {

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    columnMap.put(tableName, tableName + "." + columnName);
                                }else{
                                    tableNameMapValue = tableNameMapValue+","+tableName + "." + columnName;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();
            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);

                        if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }

                        /*if("1".equals(dimensionObj.getString("level"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("2".equals(dimensionObj.getString("level"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("3".equals(dimensionObj.getString("level"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("4".equals(dimensionObj.getString("level"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }*/
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlDataStr = "";    //日期
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                            }else{
                                sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                            }
                            sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        }
                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                            }else{
                                sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                            }

                            sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        }
                    }

                    //支局
                    if(!hxRegionIdList.isEmpty()){
                        for(String zjValue : hxRegionIdList){
                            if(sqlZjWhereStr.equals("")){
                                sqlZjWhereStr = key+".hx_region_name='"+zjValue+"'";
                            }else{
                                sqlZjWhereStr = sqlZjWhereStr +" or " + key+".hx_region_name='"+zjValue+"'";
                            }

                            sqlColumnStr = sqlColumnStr+","+key+".hx_region_name";
                        }
                    }

                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                            }else{
                                sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                            }

                            sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        }
                    }

                    //数据周期
                    if(reportParam.getDateType().equals("day")){ //日
                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                        sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";

                    }else{ //月
                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                        sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";
                    }

                    //客户
                    String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                }

                log.info("------details-sqlColumnStr: "+sqlColumnStr);
                log.info("------details-sqlTableStr: "+sqlTableStr);
                log.info("------details-sqlWhereStr: "+sqlWhereStr);
                log.info("------details-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------details-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------details-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------details-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------details-sqlDataStr: "+sqlDataStr);
                log.info("------details-sqlCustTypeStr: "+sqlCustTypeStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr +sqlDataStr + sqlCustTypeStr + sqlRoleTypeStr;
                    log.info("+++++++++自助报表详情SQL: {}",sqlStr);

                    //查询结果
                    retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                            .doSelectPage(() -> repConnSecondTableService.getReportDataList(sqlStr));

                }
            }

            //log.info("=======自助报表详情查询结果：{}",retReportDataList);

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }

    /**
     * 获取报表详情数据
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportDetailsListNew(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJson = new JSONObject();
        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    columnMap.put(tableName, tableName + "." + columnName);
                                }else{
                                    tableNameMapValue = tableNameMapValue+","+tableName + "." + columnName;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();
            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }
                        /*if("1".equals(dimensionObj.getString("level"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("2".equals(dimensionObj.getString("level"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("3".equals(dimensionObj.getString("level"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("4".equals(dimensionObj.getString("level"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }*/
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlDataStr = "";    //日期
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlOrderByStr =""; //排序

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                            }else{
                                sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                            }
                        }
                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";

                        if(sqlOrderByStr.equals("")){
                            sqlOrderByStr = key+".hx_latn_name";
                        }else{
                            sqlOrderByStr = sqlOrderByStr+","+key+".hx_latn_name";
                        }
                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                            }else{
                                sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                            }
                        }
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";

                        if(sqlOrderByStr.equals("")){
                            sqlOrderByStr = key+".hx_area_name";
                        }else{
                            sqlOrderByStr = sqlOrderByStr+","+key+".hx_area_name";
                        }
                    }

                    //支局
                    if(!hxRegionIdList.isEmpty()){
                        for(String zjValue : hxRegionIdList){
                            if(sqlZjWhereStr.equals("")){
                                sqlZjWhereStr = key+".hx_region_name='"+zjValue+"'";
                            }else{
                                sqlZjWhereStr = sqlZjWhereStr +" or " + key+".hx_region_name='"+zjValue+"'";
                            }
                        }
                        sqlColumnStr = sqlColumnStr+","+key+".hx_region_name";

                        if(sqlOrderByStr.equals("")){
                            sqlOrderByStr = key+".hx_region_name";
                        }else{
                            sqlOrderByStr = sqlOrderByStr+","+key+".hx_region_name";
                        }
                    }

                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                            }else{
                                sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                            }
                        }
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";

                        if(sqlOrderByStr.equals("")){
                            sqlOrderByStr = key+".x_hx5_bp_name";
                        }else{
                            sqlOrderByStr = sqlOrderByStr+","+key+".x_hx5_bp_name";
                        }
                    }


                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    //数据周期
                    if(reportParam.getDateType().equals("day")){ //日

                        //开始时间和结束时间不为空时
                        if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                            sqlDataStr = sqlDataStr + " and " + key+".op_date >='"+startDate+"' and "+ key+".op_date <='"+endDate+"'";
                        }else if(StringUtil.isNotEmpty(startDate)){ //只有开始日期
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+startDate+"'";
                        }else if(StringUtil.isNotEmpty(endDate)){//只有结束日期
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+endDate+"'";
                        }else{ //开始日期和结束日期都为空
                            String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";
                        }

                    }else{ //月

                        //开始时间和结束时间不为空时
                        if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                            sqlDataStr = sqlDataStr + " and " + key+".op_date >='"+startDate+"' and "+ key+".op_date <='"+endDate+"'";
                        }else if(StringUtil.isNotEmpty(startDate)){ //只有开始日期
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+startDate+"'";
                        }else if(StringUtil.isNotEmpty(endDate)){//只有结束日期
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+endDate+"'";
                        }else{ //开始日期和结束日期都为空
                            String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";
                        }

                    }

                    //客户
                    String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                }

                log.info("------details-sqlColumnStr: "+sqlColumnStr);
                log.info("------details-sqlTableStr: "+sqlTableStr);
                log.info("------details-sqlWhereStr: "+sqlWhereStr);
                log.info("------details-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------details-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------details-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------details-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------details-sqlDataStr: "+sqlDataStr);
                log.info("------details-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------details-sqlOrderByStr: "+sqlOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                    if(StringUtil.isNotEmpty(sqlOrderByStr)){
                        sqlOrderByStr = " order by  "+sqlOrderByStr;
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr +sqlDataStr + sqlCustTypeStr + sqlRoleTypeStr +sqlOrderByStr;
                    log.info("+++++++++自助报表详情SQL: {}",sqlStr);

                    //获取总数
                    dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStr));
                    if(dataCount >0){
                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));
                    }
                }
            }

            //log.info("=======自助报表详情查询结果：{}",retReportDataList);
            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJson.put("dataCount",dataCount);
            retJson.put("dataList",retReportDataList);
        }

        return retJson;
    }


    /**
     * 获取报表统计数据
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private List<RetReportData> getReportCountList(ReportParam reportParam, PageDomain pageDomain){
        List<RetReportData> retReportDataList = new ArrayList<>();

        try {

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                //tableExpr = "("+tableExpr+") ";

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);
                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;
                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();
            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }

                        /*if("1".equals(dimensionObj.getString("level"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("2".equals(dimensionObj.getString("level"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("3".equals(dimensionObj.getString("level"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("4".equals(dimensionObj.getString("level"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }*/
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlDataStr = "";    //日期
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                            }else{
                                sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                            }

                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                            }else{
                                sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                            }
                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_area_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                        }

                    }

                    //支局
                    if(!hxRegionIdList.isEmpty()){
                        for(String zjValue : hxRegionIdList){
                            if(sqlZjWhereStr.equals("")){
                                sqlZjWhereStr = key+".hx_region_name='"+zjValue+"'";
                            }else{
                                sqlZjWhereStr = sqlZjWhereStr +" or " + key+".hx_region_name='"+zjValue+"'";
                            }
                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_region_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_region_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_region_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_region_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_region_name";
                        }
                    }

                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                            }else{
                                sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                            }

                        }

                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".x_hx5_bp_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                        }
                    }

                    //数据周期
                    if(reportParam.getDateType().equals("day")){ //日
                        String replDate = "";
                        if(sqlColumnStr.indexOf("{repl_date") >=0){
                            replDate = sqlColumnStr.substring(sqlColumnStr.indexOf("{repl_date")+10,sqlColumnStr.indexOf("}"));
                            String strDate = "";
                            if(replDate.equals("")){
                               strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                            }else{
                                int iReplDate = Integer.parseInt(replDate);
                                strDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",iReplDate);
                            }

                            //sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";

                            sqlColumnStr = sqlColumnStr.replaceAll("\\{repl_date"+replDate+"}",strDate);
                        }


                    }else{ //月
                        String replDate = "";
                        if(sqlColumnStr.indexOf("{repl_date") >=0){
                            replDate = sqlColumnStr.substring(sqlColumnStr.indexOf("{repl_date")+10,sqlColumnStr.indexOf("}"));
                            String strDate = "";
                            if(replDate.equals("")){
                                strDate = DateTimeUtil.getLastMonth("yyyyMM");
                            }else{
                                int iReplDate = Integer.parseInt(replDate);
                                strDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",iReplDate);
                            }

                            //sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";

                            sqlColumnStr = sqlColumnStr.replaceAll("\\{repl_date"+replDate+"}",strDate);
                        }
                    }

                    //客户
                    String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------count-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------count-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------count-sqlDataStr: "+sqlDataStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr +sqlDataStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr;

                    /*String sqlStr = "select count(IS_MQ) IS_MQ,count(is_cs) is_cs,khjy_sp_mobcj_mkt_d.hx_latn_name " +
                            "from khjy_sp_mobcj_mkt_d khjy_sp_mobcj_mkt_d where 1=1 " +
                            "and (khjy_sp_mobcj_mkt_d.hx_latn_name='乌鲁木齐' or khjy_sp_mobcj_mkt_d.hx_latn_name='和田') " +
                            "and khjy_sp_mobcj_mkt_d.op_date='20250714' and khjy_sp_mobcj_mkt_d.cust_id !=''";*/

                    log.info("+++++++++自助报表统计SQL: {}",sqlStr);


                    //查询结果
                    retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                            .doSelectPage(() -> repConnSecondTableService.getReportDataList(sqlStr));

                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }

    /**
     * 获取报表统计数据
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountListNew(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJson = new JSONObject();

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr()==null?"":reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr()==null?"":reportIndex.getFmBdsEr(); //分母表达式2
                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    //2025-10-14
                                    //替换分子表达式的日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceDateLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一天日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceDateLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式的日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds = replaceDateLast(fmBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式前一天日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceDateLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2的日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceDateLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分子表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceDateLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2的日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceDateLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceDateLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    //2025-10-14
                                    //替换分子表达式日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceMonthLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一个月
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceMonthLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds =replaceMonthLast(fmBds,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分母表达式前一个月
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceMonthLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceMonthLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式2前一个月
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceMonthLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceMonthLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一个月
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceMonthLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                //tableExpr = "("+tableExpr+") ";
                                //替换日期占位符
                                tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();

            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        String wdColumnName = dimensionObj.getString("columnName");
                        //2025-11-19 下一级维度
                        if(StringUtil.isNotEmpty(reportParam.getXyjwd()) && reportParam.getXyjwd().equals("1")){
                            String id = dimensionObj.getString("id");
                            if(wdColumnName.equals("hxLatnName")){ //地市
                                wdColumnName = "hxAreaName";
                                //根据地市获取地市下一级的区县
                                QueryWrapper<ReportDimension> qxWrapper = new QueryWrapper<>();
                                qxWrapper.eq("parent_id",id);
                                List<ReportDimension> qxDimensionList = reportDimensionMapper.selectList(qxWrapper);
                                if(qxDimensionList != null && qxDimensionList.size() >0){
                                    for(ReportDimension qxReportDimension:qxDimensionList){
                                        hxAreaIdList.add(qxReportDimension.getDimensionName());
                                    }
                                }

                            }else if(wdColumnName.equals("hxAreaName")){ //区县
                                wdColumnName = "xHx5BpName";
                                //根据区县获取区县下一级的网格
                                QueryWrapper<ReportDimension> wgWrapper = new QueryWrapper<>();
                                wgWrapper.eq("parent_id",id);
                                List<ReportDimension> wgDimensionList = reportDimensionMapper.selectList(wgWrapper);
                                if(wgDimensionList != null && wgDimensionList.size() >0){
                                    for(ReportDimension wgReportDimension:wgDimensionList){
                                        xHx5BpIdList.add(wgReportDimension.getDimensionName());
                                    }
                                }
                            }else if(wdColumnName.equals("xHx5BpName")){ //网格
                                wdColumnName = "xHx5BpName";
                                xHx5BpIdList.add(dimensionObj.getString("title"));
                            }
                        }else{
                            if("hxLatnName".equals(wdColumnName)){
                                //hxLatnIdList.add(dimensionObj.getString("id"));
                                hxLatnIdList.add(dimensionObj.getString("title"));
                            }else if("hxAreaName".equals(wdColumnName)){
                                //hxAreaIdList.add(dimensionObj.getString("id"));
                                hxAreaIdList.add(dimensionObj.getString("title"));
                            }else if("hxRegionName".equals(wdColumnName)){
                                //hxRegionIdList.add(dimensionObj.getString("id"));
                                hxRegionIdList.add(wdColumnName);
                            }else if("xHx5BpName".equals(wdColumnName)){
                                //xHx5BpIdList.add(dimensionObj.getString("id"));
                                xHx5BpIdList.add(dimensionObj.getString("title"));
                            }
                        }

                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlHeJiColumnStr =""; //查询列字段合计信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr ="";   //地州排序条件
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlHeJiColumnStr.equals("")){
                        sqlHeJiColumnStr = columnStr;
                    }else{
                        sqlHeJiColumnStr = sqlHeJiColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                //sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                                sqlDzWhereStr = "'"+dzValue+"'";
                            }else{
                                //sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                                sqlDzWhereStr = sqlDzWhereStr +",'"+dzValue+"'";
                            }

                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlHeJiColumnStr = sqlHeJiColumnStr+", '合计' as hx_latn_name";
                        sqlHeJiColumnStr = sqlHeJiColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        //地州排序字段顺序
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";
                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                //sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                                sqlXfWhereStr =  "'"+xfValue+"'";
                            }else{
                                //sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                                sqlXfWhereStr = sqlXfWhereStr +",'"+xfValue+"'";
                            }
                        }

                        //2025-11-19
                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name"+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlHeJiColumnStr = sqlHeJiColumnStr+", '合计' as hx_area_name";
                        sqlHeJiColumnStr = sqlHeJiColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        //地州排序字段顺序
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";
                    }

                    //支局
                    if(!hxRegionIdList.isEmpty()){
                        for(String zjValue : hxRegionIdList){
                            if(sqlZjWhereStr.equals("")){
                                sqlZjWhereStr = key+".hx_region_name='"+zjValue+"'";
                            }else{
                                sqlZjWhereStr = sqlZjWhereStr +" or " + key+".hx_region_name='"+zjValue+"'";
                            }
                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_region_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_region_name");

                        sqlHeJiColumnStr = sqlHeJiColumnStr+", '合计' as hx_region_name";
                        sqlHeJiColumnStr = sqlHeJiColumnStr.replaceAll("\\{area_name}","hx_region_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_region_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_region_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_region_name";
                        }
                    }

                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                //sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                                sqlWgWhereStr = "'"+wgValue+"'";
                            }else{
                                //sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                                sqlWgWhereStr = sqlWgWhereStr +",'"+wgValue+"'";
                            }

                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_area_name"+","+key+".hx_latn_name";

                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlHeJiColumnStr = sqlHeJiColumnStr+", '合计' as x_hx5_bp_name";
                        sqlHeJiColumnStr = sqlHeJiColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_area_name"+","+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_area_name"+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        //地州排序字段顺序
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    }

                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                }

                log.info("------getReportCountListNew-count-sqlColumnStr: "+sqlColumnStr);
                log.info("------getReportCountListNew-count-sqlHeJiColumnStr: "+sqlHeJiColumnStr);
                log.info("------getReportCountListNew-count-sqlTableStr: "+sqlTableStr);
                log.info("------getReportCountListNew-count-sqlWhereStr: "+sqlWhereStr);
                log.info("------getReportCountListNew-count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------getReportCountListNew-count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------getReportCountListNew-count-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------getReportCountListNew-count-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------getReportCountListNew-count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------getReportCountListNew-count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------getReportCountListNew-count-sqlGroupByStr: "+sqlGroupByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        //sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                        sqlDzWhereStr =  " and hx_latn_name in ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        //sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                        sqlXfWhereStr =  " and hx_area_name in ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        //sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                        sqlWgWhereStr =  " and x_hx5_bp_name in ("+sqlWgWhereStr+")";
                    }


                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr + sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        /*String heJiSqlStr = " UNION ALL select "+sqlHeJiColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                                + sqlWgWhereStr + sqlCustTypeStr + sqlRoleTypeStr;

                        String allSql = sqlStr + heJiSqlStr;
                        log.info("+++++++++自助报表统计SQL: {}",allSql);*/

                        log.info("+++++++++getReportCountListNew-自助取数统计SQL: {}",sqlStr);

                        /*long startTime = System.nanoTime();
                        //获取总数
                        dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStr));
                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;
                        log.info("++++++++getReportCountListNew-自助取数统计总数SQL方法执行时间：" + formatTime(execTime));


                        if(dataCount >0){
                            startTime = System.nanoTime();
                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));

                            endTime = System.nanoTime();

                            execTime = endTime - startTime;
                            log.info("++++++++getReportCountListNew-自助取数统计SQL方法执行时间：" + formatTime(execTime));
                            //2025-9-2
                            *//*String heJiSqlStr = "select "+sqlHeJiColumnStr+" from "+sqlTableStr +" where 1=1 "
                                    +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                                    + sqlWgWhereStr + sqlCustTypeStr + sqlRoleTypeStr;

                            log.info("+++++++++自助报表合计统计SQL: {}",sqlStr);
                            //查询合计结果
                            if(retReportDataList !=null && retReportDataList.size() >0){
                                List<Map<String,Object>> retTotalDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                        .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(heJiSqlStr));
                                if(retTotalDataList != null && retTotalDataList.size() >0){
                                    Map<String,Object> totalDataMap = retTotalDataList.get(0);
                                    retReportDataList.add(totalDataMap);
                                }
                            }*//*

                        }*/

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountListNew-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountListNew-自助取数SQL方法执行时间：" + formatTime(execTime));

                    }else{

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                                + sqlWgWhereStr + " and " +whereDateStr + sqlDzOrderByStr;

                        log.info("+++++++++getReportCountListNew-自助取数统计SQL: {}",sqlStrDate);

                        /*long startTime = System.nanoTime();
                        //获取总数
                        dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                        long endTime = System.nanoTime();

                        long execTime = endTime - startTime;
                        log.info("++++++++getReportCountListNew-自助取数统计总数SQL方法执行时间：" + formatTime(execTime));

                        if(dataCount >0){
                            startTime = System.nanoTime();
                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                            endTime = System.nanoTime();
                            execTime = endTime - startTime;
                            log.info("++++++++getReportCountListNew-自助取数统计SQL方法执行时间：" + formatTime(execTime));

                        }*/

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountListNew-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountListNew-自助取数SQL方法执行时间：" + formatTime(execTime));

                    }

                    //计算合计列数据
                    retReportDataList = getCountListTj(retReportDataList);
                    /*if(retReportDataList !=null && retReportDataList.size() >0){
                        Map<String, Object> totalMap = new HashMap<>();
                        Map<String, Object> dataMap = retReportDataList.get(0);
                        Set<String> keyset = dataMap.keySet();
                        for(String key : keyset){
                            Object objValue = dataMap.get(key);

                            if(key.equals("hx_latn_name")){
                                totalMap.put(key,"合计");
                            }else if(key.equals("hx_area_name") || key.equals("x_hx5_bp_name")){
                                totalMap.put(key,"-");
                            }else if(objValue != null && objValue instanceof String && objValue.toString().indexOf("%") >0){ //百分率求平均值
                                //BigDecimal average = calculateAver(retReportDataList, key);
                                BigDecimal average = calculateSumLv(retReportDataList, key);
                                String averageStr = average+"%";
                                totalMap.put(key,averageStr);
                            }else{ //数值求合
                                BigDecimal totalSum = calculateSum(retReportDataList, key);
                                totalMap.put(key,totalSum);
                            }
                        }
                        retReportDataList.add(totalMap);
                    }*/
                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJson.put("dataCount",dataCount);
            retJson.put("dataList",retReportDataList);
        }

        return retJson;
    }

    /**
     * 统计宽表
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountListNewTjkb(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJson = new JSONObject();

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            String whereDateStr ="";
            String tableName ="";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds();
                                String fmBds = reportIndex.getFmBds();
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();

            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        String wdColumnName = dimensionObj.getString("columnName");
                        //2025-11-19 下一级维度
                        if(StringUtil.isNotEmpty(reportParam.getXyjwd()) && reportParam.getXyjwd().equals("1")){
                            String id = dimensionObj.getString("id");
                            if(wdColumnName.equals("hxLatnName")){ //地市
                                wdColumnName = "hxAreaName";
                                //根据地市获取地市下一级的区县
                                QueryWrapper<ReportDimension> qxWrapper = new QueryWrapper<>();
                                qxWrapper.eq("parent_id",id);
                                List<ReportDimension> qxDimensionList = reportDimensionMapper.selectList(qxWrapper);
                                if(qxDimensionList != null && qxDimensionList.size() >0){
                                    for(ReportDimension qxReportDimension:qxDimensionList){
                                        //hxAreaIdList.add(qxReportDimension.getDimensionName());
                                        String dimensionId = qxReportDimension.getDimensionId();
                                        if(dimensionId.indexOf("_f") >=0){
                                            dimensionId = dimensionId.replaceAll("_f","");
                                        }

                                        hxAreaIdList.add(dimensionId);
                                    }
                                }

                            }else if(wdColumnName.equals("hxAreaName")){ //区县
                                wdColumnName = "xHx5BpName";
                                //根据区县获取区县下一级的网格
                                QueryWrapper<ReportDimension> wgWrapper = new QueryWrapper<>();
                                wgWrapper.eq("parent_id",id);
                                List<ReportDimension> wgDimensionList = reportDimensionMapper.selectList(wgWrapper);
                                if(wgDimensionList != null && wgDimensionList.size() >0){
                                    for(ReportDimension wgReportDimension:wgDimensionList){
                                        //xHx5BpIdList.add(wgReportDimension.getDimensionName());
                                        String dimensionId = wgReportDimension.getDimensionId();
                                        if(dimensionId.indexOf("_f") >=0){
                                            dimensionId = dimensionId.replaceAll("_f","");
                                        }

                                        xHx5BpIdList.add(dimensionId);
                                    }
                                }
                            }else if(wdColumnName.equals("xHx5BpName")){ //网格
                                wdColumnName = "xHx5BpName";
                                String dimensionId = dimensionObj.getString("id");
                                if(dimensionId.indexOf("_f") >=0){
                                    dimensionId = dimensionId.replaceAll("_f","");
                                }

                                xHx5BpIdList.add(dimensionId);
                            }
                        }else{

                            String dimensionId = dimensionObj.getString("id");
                            if(dimensionId.indexOf("_f") >=0){
                                dimensionId = dimensionId.replaceAll("_f","");
                            }

                            if("hxLatnName".equals(wdColumnName)){

                                hxLatnIdList.add(dimensionId);
                                //hxLatnIdList.add(dimensionObj.getString("title"));
                            }else if("hxAreaName".equals(wdColumnName)){
                                hxAreaIdList.add(dimensionId);
                                //hxAreaIdList.add(dimensionObj.getString("title"));
                            }else if("hxRegionName".equals(wdColumnName)){
                                //hxRegionIdList.add(dimensionObj.getString("id"));
                                hxRegionIdList.add(wdColumnName);
                            }else if("xHx5BpName".equals(wdColumnName)){
                                xHx5BpIdList.add(dimensionId);
                                //xHx5BpIdList.add(dimensionObj.getString("title"));
                            }
                        }

                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlHeJiColumnStr =""; //查询列字段合计信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr ="";   //地州排序条件
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlGroupByStr = "";  //分组信息
            String sqlHxType = "";  //划小类型

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlHeJiColumnStr.equals("")){
                        sqlHeJiColumnStr = columnStr;
                    }else{
                        sqlHeJiColumnStr = sqlHeJiColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }

                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                //sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                                sqlDzWhereStr = "'"+dzValue+"'";
                            }else{
                                //sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                                sqlDzWhereStr = sqlDzWhereStr +",'"+dzValue+"'";
                            }

                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlHeJiColumnStr = sqlHeJiColumnStr+", '合计' as hx_latn_name";
                        sqlHeJiColumnStr = sqlHeJiColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        //地州排序字段顺序
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                        sqlHxType = " and hx_type ='分公司' ";
                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                //sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                                sqlXfWhereStr = "'"+xfValue+"'";
                            }else{
                                //sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                                sqlXfWhereStr = sqlXfWhereStr +",'"+xfValue+"'";
                            }
                        }

                        //2025-11-19
                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name"+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlHeJiColumnStr = sqlHeJiColumnStr+", '合计' as hx_area_name";
                        sqlHeJiColumnStr = sqlHeJiColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";

                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                        sqlHxType = " and hx_type ='县分' ";

                    }

                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                //sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                                sqlWgWhereStr = "'"+wgValue+"'";
                            }else{
                                //sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                                sqlWgWhereStr = sqlWgWhereStr +",'"+wgValue+"'";
                            }

                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_area_name"+","+key+".hx_latn_name";

                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlHeJiColumnStr = sqlHeJiColumnStr+", '合计' as x_hx5_bp_name";
                        sqlHeJiColumnStr = sqlHeJiColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_area_name"+","+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_area_name"+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                        sqlHxType = " and hx_type ='网格' ";
                    }

                }

                log.info("------getReportCountListNewTjkb-count-sqlColumnStr: "+sqlColumnStr);
                log.info("------getReportCountListNewTjkb-count-sqlHeJiColumnStr: "+sqlHeJiColumnStr);
                log.info("------getReportCountListNewTjkb-count-sqlTableStr: "+sqlTableStr);
                log.info("------getReportCountListNewTjkb-count-sqlWhereStr: "+sqlWhereStr);
                log.info("------getReportCountListNewTjkb-count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------getReportCountListNewTjkb-count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------getReportCountListNewTjkb-count-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------getReportCountListNewTjkb-count-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------getReportCountListNewTjkb-count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------getReportCountListNewTjkb-count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------getReportCountListNewTjkb-count-sqlHxType: "+sqlHxType);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        //sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                        sqlDzWhereStr =  " and  hx_latn_id in ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        //sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                        sqlXfWhereStr =  " and hx_area_id in ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        //sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                        sqlWgWhereStr =  " and x_hx5_bp_id in ("+sqlWgWhereStr+")";
                    }

                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                                + sqlWgWhereStr + " and " +whereDateStr + sqlHxType + " group by " +sqlGroupByStr + sqlDzOrderByStr;*/

                    String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr + " and " +whereDateStr + sqlHxType + sqlDzOrderByStr;

                        log.info("+++++++++getReportCountListNewTjkb-自助取数统计SQL: {}",sqlStrDate);

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountListNewTjkb-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountListNewTjkb-自助取数SQL方法执行时间：" + formatTime(execTime));


                    //计算合计列数据
                    retReportDataList = getCountListTjkb(retReportDataList,tableName);
                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJson.put("dataCount",dataCount);
            retJson.put("dataList",retReportDataList);
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
     * 计算合计(率)
     * @param dataList
     * @param fieldName
     * @return
     */
    public static BigDecimal calculateSumLv (List<Map<String, Object>> dataList, String fieldName) {

        BigDecimal fzTotal = BigDecimal.ZERO;
        BigDecimal fmTotal = BigDecimal.ZERO;

        BigDecimal fzErTotal = BigDecimal.ZERO;
        BigDecimal fmErTotal = BigDecimal.ZERO;

        boolean isLv = true;

        if (dataList == null || dataList.isEmpty()) {
            return BigDecimal.ZERO;
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

                isLv = false;
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
                return BigDecimal.ZERO;
            }

            total = fzTotal.divide(fmTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                    .multiply(new BigDecimal("100"))          // 乘以100
                    .setScale(2, RoundingMode.HALF_UP);
        }else{ //环比

            if(fmTotal == BigDecimal.ZERO || fmTotal.compareTo(BigDecimal.ZERO) == 0){
                return BigDecimal.ZERO;
            }

            if(fmErTotal == BigDecimal.ZERO || fmErTotal.compareTo(BigDecimal.ZERO) == 0){
                return BigDecimal.ZERO;
            }

            BigDecimal subTotal = fzTotal.divide(fmTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                    .multiply(new BigDecimal("100"))          // 乘以100
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal subTotal2 = fzErTotal.divide(fmErTotal, 4, RoundingMode.HALF_UP)  // 先除法，保留4位小数确保精度
                    .multiply(new BigDecimal("100"))          // 乘以100
                    .setScale(2, RoundingMode.HALF_UP);

            total = subTotal.subtract(subTotal2);;
        }


        return total;
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
     * 处理List中所有Map，将键转换为驼峰命名
     * @param originalList 原始List<Map<String, Object>>
     * @return 转换后的List<Map<String, Object>>
     */
    public static List<Map<String, Object>> convertListMapKeysToCamelCase(List<Map<String, Object>> originalList) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        if (originalList == null || originalList.isEmpty()) {
            return resultList;
        }

        // 循环遍历List中的每个Map
        for (Map<String, Object> originalMap : originalList) {
            // 转换单个Map的键为驼峰命名
            Map<String, Object> camelCaseMap = convertMapKeysToCamelCase(originalMap);
            resultList.add(camelCaseMap);
        }

        return resultList;
    }
    /**
     * 转换单个Map的键为驼峰命名（支持嵌套Map）
     * @param originalMap 原始Map
     * @return 转换后的Map
     */
    private static Map<String, Object> convertMapKeysToCamelCase(Map<String, Object> originalMap) {
        if (originalMap == null || originalMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> camelCaseMap = new HashMap<>(originalMap.size());
        Set<Map.Entry<String, Object>> entrySet = originalMap.entrySet();

        for (Map.Entry<String, Object> entry : entrySet) {
            String originalKey = entry.getKey();
            Object value = entry.getValue();

            // 转换键为驼峰命名
            String camelCaseKey = toCamelCase(originalKey);

            // 如果值是Map，递归处理嵌套Map
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                camelCaseMap.put(camelCaseKey, convertMapKeysToCamelCase(nestedMap));
            }
            // 如果值是List<Map>，递归处理列表中的Map
            else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> nestedList = (List<Map<String, Object>>) value;
                camelCaseMap.put(camelCaseKey, convertListMapKeysToCamelCase(nestedList));
            }
            // 其他类型直接放入
            else {
                camelCaseMap.put(camelCaseKey, value);
            }
        }

        return camelCaseMap;
    }

    /**
     * 将下划线命名的字符串转换为驼峰命名
     * 例如：user_name -> userName, user -> user, USER_AGE -> userAge
     * @param underScoreName 下划线命名的字符串
     * @return 驼峰命名的字符串
     */
    private static String toCamelCase(String underScoreName) {
        if (underScoreName == null || underScoreName.isEmpty()) {
            return underScoreName;
        }

        // 先将所有字符转为小写
        String lowerCase = underScoreName.toLowerCase();
        StringBuilder camelCase = new StringBuilder();
        boolean nextUpperCase = false;

        for (int i = 0; i < lowerCase.length(); i++) {
            char c = lowerCase.charAt(i);

            if (c == '_') {
                // 遇到下划线，标记下一个字符需要大写
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    // 下一个字符转为大写
                    camelCase.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    // 普通字符直接添加
                    camelCase.append(c);
                }
            }
        }

        return camelCase.toString();
    }

    /**
     * 保存报表
     * @param param
     * @return
     */
    @Override
    public JSONObject saveReport(JSONObject param) {
        JSONObject resultObj = new JSONObject();
        try{

            SysUser currentUser = UserContext.currentUser();
            String userId = currentUser.getUserId();

            String headCol = param.getString("col");
            List<ReportHead> headList = JSON.parseArray(headCol, ReportHead.class);
            String cols = "";
            String fields = "";
            for(int i = 0; i < headList.size(); i++){
                ReportHead head = headList.get(i);
                fields += head.getId() + ",";
                cols += head.getTitle() + ",";
            }

            ReportInfo reportInfo = null;
            String operate = "add";

            String reportName = param.getString("reportName");
            QueryWrapper<ReportInfo> reportQu = new QueryWrapper<>();
            reportQu.eq("report_name", reportName);
            reportInfo = reportInfoMapper.selectOne(reportQu);
            if(reportInfo ==null){
                reportInfo = new ReportInfo();
            }else{
                operate = "update";
            }

            reportInfo.setReportName(param.getString("reportName"));
            reportInfo.setCols(headCol);
            reportInfo.setFields(fields);
            /*reportInfo.setCustZq(param.getString("custZQ"));
            reportInfo.setCustGz(param.getString("custGZ"));*/
            reportInfo.setDateType(param.getString("dateType"));
            reportInfo.setStartDate(param.getString("startDate"));
            reportInfo.setEndDate(param.getString("endDate"));
            reportInfo.setCreateBy(userId);
            reportInfo.setCreateTime(LocalDateTime.now());
            reportInfo.setQuota(param.getString("quota"));
            reportInfo.setDimension(param.getString("dimension"));
            reportInfo.setRoleType(param.getString("roleType"));
            reportInfo.setIndexType(param.getString("indexType"));
            reportInfo.setWhereStr(param.getString("whereStr"));
            reportInfo.setReportClassify(param.getString("reportClassify"));
            reportInfo.setWhereCheck(param.getString("whereCheck"));
            reportInfo.setStartDate(param.getString("startDate"));
            reportInfo.setEndDate(param.getString("endDate"));
            reportInfo.setCustType(param.getString("custType"));
            reportInfo.setIsNonStand(param.getString("isNonNtand"));
            reportInfo.setXyjwd(param.getString("xyjwd"));


            /*String filePath = saveReportToDisk(param);
            if(StringUtils.isNotBlank(filePath)){
                reportInfo.setReportPath(filePath);
                int result = 0;
                if(operate.equals("add")){
                    result = reportInfoMapper.insert(reportInfo);
                }else{
                    result = reportInfoMapper.updateById(reportInfo);
                }

                if(result > 0){
                    int reportId = reportInfo.getReportId();
                    resultObj.put("retCode", "0");
                    resultObj.put("retMsg", "保存报表成功！");
                    resultObj.put("reportId", reportId);
                } else {
                    resultObj.put("retCode", "1");
                    resultObj.put("retMsg", "保存报表失败！");
                }

            } else {
                resultObj.put("retCode", "1");
                resultObj.put("retMsg", "保存报表失败！");
            }*/

            int result = 0;
            if(operate.equals("add")){
                result = reportInfoMapper.insert(reportInfo);
            }else{
                result = reportInfoMapper.updateById(reportInfo);
            }

            if(result > 0){
                int reportId = reportInfo.getReportId();
                resultObj.put("retCode", "0");
                resultObj.put("retMsg", "保存报表成功！");
                resultObj.put("reportId", reportId);
            } else {
                resultObj.put("retCode", "1");
                resultObj.put("retMsg", "保存报表失败！");
            }


            return resultObj;
        }catch (Exception e){
            e.printStackTrace();
            resultObj.put("retCode", "1");
            resultObj.put("retMsg", "保存报表失败！" + e.getMessage());
            return resultObj;
        }
    }


    /**
     * 保存模板
     * @param param
     * @return
     */
    @Override
    public JSONObject saveTemplate(JSONObject param) {
        JSONObject resultObj = new JSONObject();
        try{

            SysUser currentUser = UserContext.currentUser();
            String userId = currentUser.getUserId();

            String headCol = param.getString("col");
            List<ReportHead> headList = JSON.parseArray(headCol, ReportHead.class);
            String cols = "";
            String fields = "";
            for(int i = 0; i < headList.size(); i++){
                ReportHead head = headList.get(i);
                fields += head.getId() + ",";
                cols += head.getTitle() + ",";
            }

            ReportTemplate templateInfo = null;
            String operate = "add";

            String templateName = param.getString("templateName");
            QueryWrapper<ReportTemplate> reportQu = new QueryWrapper<>();
            reportQu.eq("template_name", templateName);
            templateInfo = templateInfoMapper.selectOne(reportQu);
            if(templateInfo ==null){
                templateInfo = new ReportTemplate();
            }else{
                operate = "update";
            }

            templateInfo.setTemplateName(param.getString("templateName"));
            templateInfo.setCols(headCol);
            templateInfo.setFields(fields);
            /*templateInfo.setCustZq(param.getString("custZQ"));
            templateInfo.setCustGz(param.getString("custGZ"));*/
            templateInfo.setDateType(param.getString("dateType"));
            templateInfo.setStartDate(param.getString("startDate"));
            templateInfo.setEndDate(param.getString("endDate"));
            templateInfo.setCreateBy(userId);
            templateInfo.setCreateTime(LocalDateTime.now());
            templateInfo.setQuota(param.getString("quota"));
            templateInfo.setDimension(param.getString("dimension"));
            templateInfo.setRoleType(param.getString("roleType"));
            templateInfo.setIndexType(param.getString("indexType"));
            templateInfo.setWhereStr(param.getString("whereStr"));
            templateInfo.setReportClassify(param.getString("reportClassify"));
            templateInfo.setWhereCheck(param.getString("whereCheck"));
            templateInfo.setStartDate(param.getString("startDate"));
            templateInfo.setEndDate(param.getString("endDate"));
            templateInfo.setCustType(param.getString("custType"));
            templateInfo.setIsNonStand(param.getString("isNonNtand"));
            templateInfo.setXyjwd(param.getString("xyjwd"));

            /*String filePath = saveReportToDisk(param);
            if(StringUtils.isNotBlank(filePath)){
                reportInfo.setReportPath(filePath);
                int result = 0;
                if(operate.equals("add")){
                    result = reportInfoMapper.insert(reportInfo);
                }else{
                    result = reportInfoMapper.updateById(reportInfo);
                }

                if(result > 0){
                    int reportId = reportInfo.getReportId();
                    resultObj.put("retCode", "0");
                    resultObj.put("retMsg", "保存报表成功！");
                    resultObj.put("reportId", reportId);
                } else {
                    resultObj.put("retCode", "1");
                    resultObj.put("retMsg", "保存报表失败！");
                }

            } else {
                resultObj.put("retCode", "1");
                resultObj.put("retMsg", "保存报表失败！");
            }*/

            int result = 0;
            if(operate.equals("add")){
                result = templateInfoMapper.insert(templateInfo);
            }else{
                result = templateInfoMapper.updateById(templateInfo);
            }

            if(result > 0){
                int templateId = templateInfo.getTemplateId();
                resultObj.put("retCode", "0");
                resultObj.put("retMsg", "保存模板成功！");
                resultObj.put("templateId", templateId);
            } else {
                resultObj.put("retCode", "1");
                resultObj.put("retMsg", "保存模板失败！");
            }

            return resultObj;
        }catch (Exception e){
            e.printStackTrace();
            resultObj.put("retCode", "1");
            resultObj.put("retMsg", "保存模板失败！" + e.getMessage());
            return resultObj;
        }
    }

    /**
     * 获取我的报表列表数据
     * @param reportInfo
     * @return
     */
    @Override
    public List<ReportInfo> getReportInfoList(ReportInfo reportInfo) {
        SysUser currentUser = UserContext.currentUser();
        boolean isAdmin = CheckUtil.hasSuperAdmin(currentUser);

        String userId = currentUser.getUserId();
        /*String reportName = reportInfo.getReportName() == null ? "" : reportInfo.getReportName().trim();
        QueryWrapper<ReportInfo> query = new QueryWrapper<>();
        if(reportName.length() >0){
            query.like("report_name",reportName);
        }
        query.eq("state", "0");
        if(!isAdmin){
            query.eq("create_by", userId);
        }

        query.orderByDesc("create_time");
        List<ReportInfo> reportInfoList = reportInfoMapper.selectList(query);*/

        List<ReportInfo> reportInfoList = reportInfoMapper.selReportInfoList(reportInfo,isAdmin,userId);
        return reportInfoList;
    }


    /**
     * 获取我的模板列表数据
     * @param templateInfo
     * @return
     */
    @Override
    public List<ReportTemplate> getTemplateInfoList(ReportTemplate templateInfo) {
        SysUser currentUser = UserContext.currentUser();
        String userId = currentUser.getUserId();
        boolean isAdmin = CheckUtil.hasSuperAdmin(currentUser);
        /*String templateName = templateInfo.getTemplateName() == null ? "" : templateInfo.getTemplateName().trim();
        QueryWrapper<ReportTemplate> query = new QueryWrapper<>();
        if(templateName.length() >0){
            query.like("template_name",templateName);
        }
        query.eq("state", "0");
        query.eq("create_by", userId);
        query.orderByDesc("create_time");
        List<ReportTemplate> templateInfoList = templateInfoMapper.selectList(query);*/

        List<ReportTemplate> templateInfoList = templateInfoMapper.selTempInfoList(templateInfo,isAdmin,userId);
        return templateInfoList;
    }

    /**
     * 根据报表ID删除报表
     * @param id
     * @return
     */
    @Override
    public boolean reportRemove(int id) {
        int result = reportInfoMapper.deleteById(id);
        if(result >0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 根据模板ID删除模板
     * @param id
     * @return
     */
    @Override
    public boolean templateRemove(int id) {
        int result = templateInfoMapper.deleteById(id);
        if(result >0){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void exportExcel(JSONObject param, HttpServletResponse response) {
        try {
            String reportName = param.getString("reportName");
            String isQf = param.getString("isQf");
            String isHy = param.getString("isHy");
            String isFiveg = param.getString("isFiveg");
            String isFivegb = param.getString("isFivegb");
            String isFivegtc = param.getString("isFivegtc");
            String isQzxsp = param.getString("isQzxsp");
            String isQzsl = param.getString("isQzsl");
            String isSfllb = param.getString("isSfllb");
            String isQzxq = param.getString("isQzxq");
            String quota = param.getString("quota");
            String dimension = param.getString("dimension");

            List headsList = new ArrayList<>();
            List<String> idList = new ArrayList<>();
            if(StringUtils.isNotBlank(quota)){
                JSONArray quotaArr = JSON.parseArray(quota);
                if(quotaArr != null && quotaArr.size() > 0){
                    for(int i = 0; i < quotaArr.size(); i ++){
                        JSONObject quotaObj = quotaArr.getJSONObject(i);
                        String title = quotaObj.getString("title");
                        String id = ReportUtil.convertToLowercaseWithUnderscore(quotaObj.getString("id"));
                        List<String> headList = new ArrayList<>();
                        headList.add(title);
                        headsList.add(headList);
                        idList.add(id);
                    }
                }
            }

            QueryWrapper<ReportData> query = new QueryWrapper<>();
            query.select(idList.toArray(new String[0]));
            if("1".equals(isQf)){
                query.eq("is_qf", isQf);
            }
            if("1".equals(isHy)){
                query.eq("is_hy", isHy);
            }
            if("1".equals(isFiveg)){
                query.eq("is_fiveg", isFiveg);
            }
            if("1".equals(isFivegb)){
                query.eq("is_fivegb", isFivegb);
            }
            if("1".equals(isFivegtc)){
                query.eq("is_fivegtc", isFivegtc);
            }
            if("1".equals(isQzxsp)){
                query.eq("is_qzxsp", isQzxsp);
            }
            if("1".equals(isQzsl)){
                query.eq("is_qzsl", isQzsl);
            }
            if("1".equals(isSfllb)){
                query.eq("is_sfllb", isSfllb);
            }
            if("1".equals(isQzxq)){
                query.eq("is_qzxq", isQzxq);
            }



            List<String> hxLatnIdList = new ArrayList<String>();
            List<String> hxAreaIdList = new ArrayList<String>();
            List<String> hxRegionIdList = new ArrayList<String>();
            List<String> xHx5BpIdList = new ArrayList<String>();
            if(StringUtils.isNotBlank(dimension)){
                JSONArray dimensionArr = JSON.parseArray(dimension);
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }


                        /*if("1".equals(dimensionObj.getString("level"))){
                            hxLatnIdList.add(dimensionObj.getString("id"));
                        }
                        if("2".equals(dimensionObj.getString("level"))){
                            hxAreaIdList.add(dimensionObj.getString("id"));
                        }
                        if("3".equals(dimensionObj.getString("level"))){
                            hxRegionIdList.add(dimensionObj.getString("id"));
                        }
                        if("4".equals(dimensionObj.getString("level"))){
                            xHx5BpIdList.add(dimensionObj.getString("id"));
                        }*/
                    }
                }
            }
            if(!hxLatnIdList.isEmpty()){
                query.in("hx_latn_id", hxLatnIdList);
            }
            if(!hxAreaIdList.isEmpty()){
                query.in("hx_area_id", hxAreaIdList);
            }
            if(!hxRegionIdList.isEmpty()){
                query.in("hx_region_id", hxRegionIdList);
            }
            if(!xHx5BpIdList.isEmpty()){
                query.in("x_hx5_bp_id", xHx5BpIdList);
            }

            List<Map<String, Object>> reportList = reportDataMapper.selectMaps(query);

            // 动态构建数据
            List<List<Object>> exportData = buildData(reportList, idList);

            String exportPath = "D:/report";
            String fileName = reportName + ".xlsx";
            // 确保目录存在
            File dir = new File(exportPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 完整文件路径
            String filePath = exportPath + File.separator + fileName;
            ExcelWriter excelWriter = EasyExcel.write(filePath).build();

            WriteSheet writeSheet = EasyExcel.writerSheet(reportName)
                    .head(headsList)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(ExcelUtil.createCellStyle())
                    .build();

            // 动态添加表头
            excelWriter.write(exportData, writeSheet);
            excelWriter.finish();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 下载报表
     * @param reportId
     * @return
     */
    @Override
    public ResponseEntity<InputStreamResource> downloadReport(String reportId) {
        String isSuccess = "0";
        String errMsg ="";
        try {
            //通过reportId查询reportPath
            QueryWrapper<ReportInfo> reportInfoQu = new QueryWrapper<>();
            reportInfoQu.select("report_path");
            reportInfoQu.eq("report_id", reportId);

            ReportInfo reportInfo = reportInfoMapper.selectOne(reportInfoQu);
            String filePath = reportInfo.getReportPath();

            log.info("进入下载方法... fileName: {}", filePath);
            //读取文件
//            String filePath = reportTargetPath + reportPath;

            FileSystemResource file = new FileSystemResource(filePath);

            /*String encodedFilename = new String(file.getFilename().getBytes("UTF-8"), "ISO-8859-1");

            log.info("进入下载方法... encodedFilename: {}",encodedFilename);

            //设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
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

        }catch (Exception e){
            e.printStackTrace();
            isSuccess = "-1";
            if(e.getMessage() != null){
                errMsg = e.getMessage().substring(0,1000);
            }
            return null;
        }finally {
            //保存下载记录
            if(reportId != null){
//                PptDownloadData pptDownloadData = pptDownloadDataMapper.selectById(id);
//                if(pptDownloadData != null){
//                    //获取当前登录用户信息
//                    SysUser currentUser = UserContext.currentUser();
//                    PptDownloadLog pptDownloadLog = new PptDownloadLog();
//                    pptDownloadLog.setFileId(id);
//                    pptDownloadLog.setFileName(pptDownloadData.getModelName());
//                    pptDownloadLog.setCreateUserId(pptDownloadData.getCreateUserId());
//                    pptDownloadLog.setCreateUserName(pptDownloadData.getCreateUserName());
//                    pptDownloadLog.setCreateTime(pptDownloadData.getCreateTime());
//                    pptDownloadLog.setDownloadUserId(currentUser.getUserId());
//                    //pptDownloadLog.setDownloadUserName(currentUser.getUsername());
//                    pptDownloadLog.setDownloadUserName(currentUser.getRealName());
//                    pptDownloadLog.setDownloadTime(LocalDateTime.now());
//                    pptDownloadLog.setModelTargetPath(pptDownloadData.getModelTargetPath());
//                    pptDownloadLog.setModelLevel(pptDownloadData.getModelLevel());
//                    pptDownloadLog.setFileSize(pptDownloadData.getFileSize());
//                    pptDownloadLog.setIsSuccess(isSuccess);
//                    pptDownloadLog.setErrorMsg(errMsg);
//
//                    pptDownloadLogMapper.insert(pptDownloadLog);
//                }

            }
        }
    }

    /**
     * 下载模板
     * @param templateId
     * @return
     */
    @Override
    public ResponseEntity<InputStreamResource> downloadTemplate(int templateId) {
        String isSuccess = "0";
        String errMsg ="";
        try {
            //通过templateId查询template_path
            QueryWrapper<ReportTemplate> templateInfoQu = new QueryWrapper<>();
            templateInfoQu.select("template_path");
            templateInfoQu.eq("template_id", templateId);

            ReportTemplate templateInfo = templateInfoMapper.selectOne(templateInfoQu);
            String filePath = templateInfo.getTemplatePath();

            log.info("进入下载方法... fileName: {}", filePath);
            //读取文件
//            String filePath = reportTargetPath + reportPath;

            FileSystemResource file = new FileSystemResource(filePath);

            /*String encodedFilename = new String(file.getFilename().getBytes("UTF-8"), "ISO-8859-1");

            log.info("进入下载方法... encodedFilename: {}",encodedFilename);

            //设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
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

        }catch (Exception e){
            e.printStackTrace();
            isSuccess = "-1";
            if(e.getMessage() != null){
                errMsg = e.getMessage().substring(0,1000);
            }
            return null;
        }finally {

        }
    }

    /**
     * 获取维度树数据
     * @return
     */
    @Override
    public Object dimensionTreeload(String isNonStand) {
        JSONObject retArr = new JSONObject();
        QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", "0");
        List<ReportDimension> list = reportDimensionMapper.selectList(queryWrapper);
        if(!list.isEmpty()){

            String deptName = "";
            String wdId ="";

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            SysDept sysDept = sysDeptMapper.selectById(deptId);
            if(sysDept != null){
                if(sysDept.getParentId().equals("0")){ //全疆
                    deptName = "全疆";
                }else{
                    deptName = sysDept.getDeptName();
                }
            }

            ReportDimension queryReportDimension = null;
            if(deptName.equals("全疆")){
                queryReportDimension = reportDimensionMapper.selectOne(new QueryWrapper<ReportDimension>().eq("dimension_name", deptName));
            }else{
                queryReportDimension = reportDimensionMapper.selectOne(new QueryWrapper<ReportDimension>().eq("dimension_name", deptName).eq("is_non_stand",isNonStand));
            }

            if(queryReportDimension != null){
                wdId = queryReportDimension.getDimensionId();
            }

            String columnName = "";
            for(ReportDimension reportDimension : list){
                retArr.put("title", reportDimension.getDimensionName());
                retArr.put("id", reportDimension.getDimensionId());
                retArr.put("level", reportDimension.getLevel());
                columnName = ToolUtil.toCamelCase(reportDimension.getField());
                retArr.put("columnName",columnName);
                retArr.put("isNonStand",isNonStand);

                //retArr.put("spread",true);

//                retArr.put("spread",true);
                //retArr.put("children",getChildTreeNode(reportDimension.getDimensionId()));
                if(wdId.equals("1")){
                    retArr.put("children",getChildTreeNode(reportDimension.getDimensionId(),isNonStand));
                }else{
                    retArr.put("children",getCurrentTreeNode(wdId,isNonStand));
                }

            }
        }

        String resultStr = "["+retArr.toJSONString()+"]";
        return resultStr;
    }


    /**
     * 获取维度树数据 根节点
     * @return
     */
    @Override
    public Object dimensionTreeloadRoot(String isNonStand) {
        JSONObject retArr = new JSONObject();
        QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", "0");
        List<ReportDimension> list = reportDimensionMapper.selectList(queryWrapper);
        if(!list.isEmpty()){

            String deptName = "";
            String wdId ="";

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            SysDept sysDept = sysDeptMapper.selectById(deptId);
            if(sysDept != null){
                if(sysDept.getParentId().equals("0")){ //全疆
                    deptName = "全疆";
                }else{
                    deptName = sysDept.getDeptName();
                }
            }

            ReportDimension queryReportDimension = null;
            if(deptName.equals("全疆")){
                queryReportDimension = reportDimensionMapper.selectOne(new QueryWrapper<ReportDimension>().eq("dimension_name", deptName));
            }else{
                queryReportDimension = reportDimensionMapper.selectOne(new QueryWrapper<ReportDimension>().eq("dimension_name", deptName).eq("is_non_stand",isNonStand));
            }

            if(queryReportDimension != null){
                wdId = queryReportDimension.getDimensionId();
            }

            String columnName = "";
            for(ReportDimension reportDimension : list){
                retArr.put("title", reportDimension.getDimensionName());
                retArr.put("id", reportDimension.getDimensionId());
                retArr.put("level", reportDimension.getLevel());
                columnName = ToolUtil.toCamelCase(reportDimension.getField());
                retArr.put("columnName",columnName);
                retArr.put("isNonStand",isNonStand);

                retArr.put("children","[]");

            }
        }

        String resultStr = "["+retArr.toJSONString()+"]";
        return resultStr;
    }

    /**
     * 获取维度树数据 子节点
     * @return
     */
    @Override
    public Object dimensionTreeloadChild(String isNonStand,String parentId) {

        JSONArray retArray = new JSONArray();
        QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("is_non_stand", isNonStand);
        List<ReportDimension> list = reportDimensionMapper.selectList(queryWrapper);
        if(!list.isEmpty()){
            String columnName = "";
            for(ReportDimension reportDimension : list){
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("title", reportDimension.getDimensionName());
                jSONObject.put("id", reportDimension.getDimensionId());
                jSONObject.put("level", reportDimension.getLevel());
                columnName = ToolUtil.toCamelCase(reportDimension.getField());
                jSONObject.put("columnName",columnName);
                jSONObject.put("isNonStand",isNonStand);
                jSONObject.put("spread",true);
                jSONObject.put("children","[]");
                retArray.add(jSONObject);
            }
        }

        //String resultStr = "["+retArr.toJSONString()+"]";
        String resultStr = retArray.toJSONString();
        return resultStr;
    }


    /**
     * 通过报表ID获取报表详情信息
     * @param reportId
     * @return
     */
    @Override
    public JSONObject reportInfo(String reportId) {
        JSONObject resultObj = new JSONObject();
        QueryWrapper<ReportInfo> reportQu = new QueryWrapper<>();
        reportQu.eq("report_id", reportId);
        ReportInfo reportInfo = reportInfoMapper.selectOne(reportQu);

        if(reportInfo != null){
            resultObj = (JSONObject) JSONObject.toJSON(reportInfo);

            //获取指标对于的表名
            String fields = reportInfo.getFields();
            if(StringUtil.isNotEmpty(fields)){

                List<Long> indexIds = Arrays.stream(fields.split(","))
                        .map(String::trim)  // 先去空格
                        .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                        .map(Long::parseLong)
                        .collect(Collectors.toList());

              List<ReportIndex> indexList = reportIndexMapper.selectBatchIds(indexIds);
              if(indexList != null && indexList.size() >0){
                  resultObj.put("indexTableName", indexList.get(0).getTableName());
              }
            }

            resultObj.put("retCode", "0");

        } else {
            resultObj.put("retCode", "1");
            resultObj.put("retMsg", "获取报表信息失败！");
        }
        return resultObj;
    }

    /**
     * 通过模板ID获取模板详情信息
     * @param templateId
     * @return
     */
    @Override
    public JSONObject templateInfo(int templateId) {
        JSONObject resultObj = new JSONObject();
        QueryWrapper<ReportTemplate> templateQu = new QueryWrapper<>();
        templateQu.eq("template_id", templateId);
        ReportTemplate templateInfo = templateInfoMapper.selectOne(templateQu);

        if(templateInfo != null){
            resultObj = (JSONObject) JSONObject.toJSON(templateInfo);

            //获取指标对于的表名
            String fields = templateInfo.getFields();
            if(StringUtil.isNotEmpty(fields)){
                List<Long> indexIds = Arrays.stream(fields.split(","))
                        .map(String::trim)  // 先去空格
                        .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                        .map(Long::parseLong)
                        .collect(Collectors.toList());

                List<ReportIndex> indexList = reportIndexMapper.selectBatchIds(indexIds);
                if(indexList != null && indexList.size() >0){
                    resultObj.put("indexTableName", indexList.get(0).getTableName());
                }
            }

            resultObj.put("retCode", "0");

        }else {
            resultObj.put("retCode", "1");
            resultObj.put("retMsg", "获取模板信息失败！");
        }
        return resultObj;
    }

    private JSONArray getChildTreeNode(String parentId,String isNonStand){
        JSONArray retArr = new JSONArray();
        QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("is_non_stand", isNonStand);
        queryWrapper.orderByAsc("sort_id");
        List<ReportDimension> list = reportDimensionMapper.selectList(queryWrapper);
        if (list != null && list.size() > 0) {
            String columnName = "";
            for(ReportDimension reportDimension : list){
                JSONObject childObj = new JSONObject();
                childObj.put("title", reportDimension.getDimensionName());
                childObj.put("id", reportDimension.getDimensionId());
                childObj.put("level", reportDimension.getLevel());
                columnName = ToolUtil.toCamelCase(reportDimension.getField());
                childObj.put("columnName",columnName);
                childObj.put("isNonStand",isNonStand);

//                childObj.put("spread",true);
                childObj.put("children",getChildTreeNode(reportDimension.getDimensionId(),isNonStand)) ;
                retArr.add(childObj);
            }
        }

        return retArr;

    }

    /**
     * 获取当前维度树数据
     * @param dimensionId
     * @return
     */
    private JSONArray getCurrentTreeNode(String dimensionId,String isNonStand){
        JSONArray retArr = new JSONArray();
        QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
        //queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("dimension_id", dimensionId);
        queryWrapper.eq("is_non_stand",isNonStand);
        queryWrapper.orderByAsc("sort_id");
        List<ReportDimension> list = reportDimensionMapper.selectList(queryWrapper);
        if (list != null && list.size() > 0) {
            String columnName = "";
            for(ReportDimension reportDimension : list){
                JSONObject childObj = new JSONObject();
                childObj.put("title", reportDimension.getDimensionName());
                childObj.put("id", reportDimension.getDimensionId());
                childObj.put("level", reportDimension.getLevel());
                columnName = ToolUtil.toCamelCase(reportDimension.getField());
                childObj.put("columnName",columnName);
                childObj.put("isNonStand",isNonStand);
//                childObj.put("spread",true);
                childObj.put("children",getChildTreeNode(reportDimension.getDimensionId(),isNonStand)) ;
                retArr.add(childObj);
            }
        }

        return retArr;

    }

    private static List<List<Object>> buildData(List<Map<String, Object>> dataList, List<String> fieldOrder) {
        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> map : dataList) {
            List<Object> row = new ArrayList<>();
            for (String field : fieldOrder) {
                row.add(map.get(field));
            }
            data.add(row);
        }
        return data;
    }

    public String saveReportToDisk(JSONObject param) {
        try {
            String reportName = param.getString("reportName");
            String isQf = param.getString("isQf");
            String isHy = param.getString("isHy");
            String isFiveg = param.getString("isFiveg");
            String isFivegb = param.getString("isFivegb");
            String isFivegtc = param.getString("isFivegtc");
            String isQzxsp = param.getString("isQzxsp");
            String isQzsl = param.getString("isQzsl");
            String isSfllb = param.getString("isSfllb");
            String isQzxq = param.getString("isQzxq");
            String quota = param.getString("quota");
            String dimension = param.getString("dimension");
            String col = param.getString("col");

            List headsList = new ArrayList<>();
            List<String> idList = new ArrayList<>();

            if(StringUtils.isNotBlank(col)){
                JSONArray colArr = JSON.parseArray(col);
                if(colArr != null && colArr.size() > 0){
                    for(int i = 0; i < colArr.size(); i ++){
                        JSONObject colObj = colArr.getJSONObject(i);
                        String title = colObj.getString("title");
                        String id = ReportUtil.convertToLowercaseWithUnderscore(colObj.getString("field"));
                        List<String> headList = new ArrayList<>();
                        headList.add(title);
                        headsList.add(headList);
                        idList.add(id);
                    }
                }
            }

            QueryWrapper<ReportData> query = new QueryWrapper<>();
            query.select(idList.toArray(new String[0]));
            if("1".equals(isQf)){
                query.eq("is_qf", isQf);
            }
            if("1".equals(isHy)){
                query.eq("is_hy", isHy);
            }
            if("1".equals(isFiveg)){
                query.eq("is_fiveg", isFiveg);
            }
            if("1".equals(isFivegb)){
                query.eq("is_fivegb", isFivegb);
            }
            if("1".equals(isFivegtc)){
                query.eq("is_fivegtc", isFivegtc);
            }
            if("1".equals(isQzxsp)){
                query.eq("is_qzxsp", isQzxsp);
            }
            if("1".equals(isQzsl)){
                query.eq("is_qzsl", isQzsl);
            }
            if("1".equals(isSfllb)){
                query.eq("is_sfllb", isSfllb);
            }
            if("1".equals(isQzxq)){
                query.eq("is_qzxq", isQzxq);
            }



            List<String> hxLatnIdList = new ArrayList<String>();
            List<String> hxAreaIdList = new ArrayList<String>();
            List<String> hxRegionIdList = new ArrayList<String>();
            List<String> xHx5BpIdList = new ArrayList<String>();
            if(StringUtils.isNotBlank(dimension)){
                JSONArray dimensionArr = JSON.parseArray(dimension);
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }

                        /*if("1".equals(dimensionObj.getString("level"))){
                            hxLatnIdList.add(dimensionObj.getString("id"));
                        }
                        if("2".equals(dimensionObj.getString("level"))){
                            hxAreaIdList.add(dimensionObj.getString("id"));
                        }
                        if("3".equals(dimensionObj.getString("level"))){
                            hxRegionIdList.add(dimensionObj.getString("id"));
                        }
                        if("4".equals(dimensionObj.getString("level"))){
                            xHx5BpIdList.add(dimensionObj.getString("id"));
                        }*/
                    }
                }
            }
            if(!hxLatnIdList.isEmpty()){
                query.in("hx_latn_id", hxLatnIdList);
            }
            if(!hxAreaIdList.isEmpty()){
                query.in("hx_area_id", hxAreaIdList);
            }
            if(!hxRegionIdList.isEmpty()){
                query.in("hx_region_id", hxRegionIdList);
            }
            if(!xHx5BpIdList.isEmpty()){
                query.in("x_hx5_bp_id", xHx5BpIdList);
            }

            List<Map<String, Object>> reportList = reportDataMapper.selectMaps(query);

            // 动态构建数据
            List<List<Object>> exportData = buildData(reportList, idList);

//            String exportPath = "D:/report";

            // 确保目录存在
            File dir = new File(reportTargetPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = reportName + ".xlsx";
            // 完整文件路径
            String filePath = reportTargetPath + fileName;
            ExcelWriter excelWriter = EasyExcel.write(filePath).build();

            WriteSheet writeSheet = EasyExcel.writerSheet(reportName)
                    .head(headsList)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(ExcelUtil.createCellStyle())
                    .build();

            // 动态添加表头
            excelWriter.write(exportData, writeSheet);
            excelWriter.finish();
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public String saveTemplateToDisk(JSONObject param) {
        try {
            String templateName = param.getString("templateName");
            String isQf = param.getString("isQf");
            String isHy = param.getString("isHy");
            String isFiveg = param.getString("isFiveg");
            String isFivegb = param.getString("isFivegb");
            String isFivegtc = param.getString("isFivegtc");
            String isQzxsp = param.getString("isQzxsp");
            String isQzsl = param.getString("isQzsl");
            String isSfllb = param.getString("isSfllb");
            String isQzxq = param.getString("isQzxq");
            String quota = param.getString("quota");
            String dimension = param.getString("dimension");
            String col = param.getString("col");

            List headsList = new ArrayList<>();
            List<String> idList = new ArrayList<>();

            if(StringUtils.isNotBlank(col)){
                JSONArray colArr = JSON.parseArray(col);
                if(colArr != null && colArr.size() > 0){
                    for(int i = 0; i < colArr.size(); i ++){
                        JSONObject colObj = colArr.getJSONObject(i);
                        String title = colObj.getString("title");
                        String id = ReportUtil.convertToLowercaseWithUnderscore(colObj.getString("field"));
                        List<String> headList = new ArrayList<>();
                        headList.add(title);
                        headsList.add(headList);
                        idList.add(id);
                    }
                }
            }

            QueryWrapper<ReportData> query = new QueryWrapper<>();
            query.select(idList.toArray(new String[0]));
            if("1".equals(isQf)){
                query.eq("is_qf", isQf);
            }
            if("1".equals(isHy)){
                query.eq("is_hy", isHy);
            }
            if("1".equals(isFiveg)){
                query.eq("is_fiveg", isFiveg);
            }
            if("1".equals(isFivegb)){
                query.eq("is_fivegb", isFivegb);
            }
            if("1".equals(isFivegtc)){
                query.eq("is_fivegtc", isFivegtc);
            }
            if("1".equals(isQzxsp)){
                query.eq("is_qzxsp", isQzxsp);
            }
            if("1".equals(isQzsl)){
                query.eq("is_qzsl", isQzsl);
            }
            if("1".equals(isSfllb)){
                query.eq("is_sfllb", isSfllb);
            }
            if("1".equals(isQzxq)){
                query.eq("is_qzxq", isQzxq);
            }



            List<String> hxLatnIdList = new ArrayList<String>();
            List<String> hxAreaIdList = new ArrayList<String>();
            List<String> hxRegionIdList = new ArrayList<String>();
            List<String> xHx5BpIdList = new ArrayList<String>();
            if(StringUtils.isNotBlank(dimension)){
                JSONArray dimensionArr = JSON.parseArray(dimension);
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }

                        /*if("1".equals(dimensionObj.getString("level"))){
                            hxLatnIdList.add(dimensionObj.getString("id"));
                        }
                        if("2".equals(dimensionObj.getString("level"))){
                            hxAreaIdList.add(dimensionObj.getString("id"));
                        }
                        if("3".equals(dimensionObj.getString("level"))){
                            hxRegionIdList.add(dimensionObj.getString("id"));
                        }
                        if("4".equals(dimensionObj.getString("level"))){
                            xHx5BpIdList.add(dimensionObj.getString("id"));
                        }*/
                    }
                }
            }
            if(!hxLatnIdList.isEmpty()){
                query.in("hx_latn_id", hxLatnIdList);
            }
            if(!hxAreaIdList.isEmpty()){
                query.in("hx_area_id", hxAreaIdList);
            }
            if(!hxRegionIdList.isEmpty()){
                query.in("hx_region_id", hxRegionIdList);
            }
            if(!xHx5BpIdList.isEmpty()){
                query.in("x_hx5_bp_id", xHx5BpIdList);
            }

            List<Map<String, Object>> reportList = reportDataMapper.selectMaps(query);

            // 动态构建数据
            List<List<Object>> exportData = buildData(reportList, idList);

//            String exportPath = "D:/report";

            // 确保目录存在
            File dir = new File(reportTemplatePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = templateName + ".xlsx";
            // 完整文件路径
            String filePath = reportTemplatePath + fileName;
            ExcelWriter excelWriter = EasyExcel.write(filePath).build();

            WriteSheet writeSheet = EasyExcel.writerSheet(templateName)
                    .head(headsList)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(ExcelUtil.createCellStyle())
                    .build();

            // 动态添加表头
            excelWriter.write(exportData, writeSheet);
            excelWriter.finish();
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过分类Id获取指标树数据
     * @param classifyId
     * @return
     */
    @Override
    public Object getQuotaByClassId(int classifyId, String indexType,String dateType,String roleType){
        JSONObject retArr = new JSONObject();
        try {
            //根据ID查询分类信息
            ReportClassify reportClassify = reportClassifyMapper.selectById(classifyId);
            if(reportClassify != null){
                retArr.put("title",reportClassify.getClassifyName());
                retArr.put("id",reportClassify.getClassifyId());
                retArr.put("spread",true);

                JSONArray subArr = new JSONArray();

                //根据查询条件查询指标数据信息
                List<ReportIndex> list = reportIndexMapper.selIndexListByIdType(classifyId,indexType,dateType,roleType);

                if(StringUtil.isNotEmpty(list)){
                    String title = "";
                    String value = "";
                    int indexId = -1;
                    String columnName = "";
                    String tableName ="";
                    String indexClass ="";
                    String isIncludeNoStand= "";
                    String isFiveInten= "";

                    for(ReportIndex reportIndex:list){
                        JSONObject subJson = new JSONObject();
                        title = reportIndex.getIndexName();
                        value = reportIndex.getIndexId()+"";
                        indexId = reportIndex.getIndexId();
                        indexClass = reportIndex.getIndexClass() ==null?"":reportIndex.getIndexClass();
                        tableName = reportIndex.getTableName() ==null?"":reportIndex.getTableName();
                        isIncludeNoStand = reportIndex.getIsIncludeNoStand() ==null?"":reportIndex.getIsIncludeNoStand();
                        isFiveInten = reportIndex.getIsFiveInten() ==null?"":reportIndex.getIsFiveInten();

                        columnName = ToolUtil.toCamelCase(reportIndex.getColumnName());

                        subJson.put("title",title);
                        subJson.put("id",value);
                        subJson.put("tableName",tableName);
                        //subJson.put("spread",true);
                        subJson.put("columnName", columnName);
                        subJson.put("indexClass", indexClass);
                        subJson.put("isIncludeNoStand", isIncludeNoStand);
                        subJson.put("isFiveInten", isFiveInten);
                        subJson.put("children",getIndexChildTreeNode(indexId,title,title));

                        subArr.add(subJson);

                    }
                }

                retArr.put("children",subArr);

            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return retArr;
    }

    private JSONArray getIndexChildTreeNode(int indexId,String rootName,String parentName){
        JSONArray retArr = new JSONArray();
        QueryWrapper<ReportIndex> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", indexId);
        List<ReportIndex> list = reportIndexMapper.selectList(queryWrapper);
        if(list != null && list.size() >0){
            String title = "";
            String value ="";
            int subIndexId = -1;
            String columnName = "";
            String indexClass ="";
            String tableName ="";
            String isIncludeNoStand ="";
            String isFiveInten ="";

            for(ReportIndex reportIndex:list){
                JSONObject subJson = new JSONObject();
                title = reportIndex.getIndexName();
                value = reportIndex.getIndexId()+"";
                subIndexId = reportIndex.getIndexId();
                columnName = ToolUtil.toCamelCase(reportIndex.getColumnName());
                indexClass = reportIndex.getIndexClass() ==null?"":reportIndex.getIndexClass();
                tableName = reportIndex.getTableName() ==null?"":reportIndex.getTableName();
                isIncludeNoStand = reportIndex.getIsIncludeNoStand() ==null?"":reportIndex.getIsIncludeNoStand();
                isFiveInten = reportIndex.getIsFiveInten() ==null?"":reportIndex.getIsFiveInten();

                subJson.put("title",title);
                subJson.put("id",value);
                subJson.put("tableName",tableName);
                //subJson.put("spread",true);
                subJson.put("columnName",columnName);
                subJson.put("children",getIndexChildTreeNode(subIndexId,rootName,title)) ;
                subJson.put("parentName",parentName);
                subJson.put("rootName",rootName);
                subJson.put("indexClass", indexClass);
                subJson.put("isIncludeNoStand", isIncludeNoStand);
                subJson.put("isFiveInten", isFiveInten);

                retArr.add(subJson);
            }
        }

        return retArr;

    }

    /**
     * 根据指标Id获取条件
     * @param indexId
     * @return
     */
    public Object getConditByIndexId(int indexId,String tjCodeStr){
        List<String> tjCodeList = new ArrayList<>();
        if(StringUtil.isNotEmpty(tjCodeStr)){
            String[] tjCodeArray = tjCodeStr.split(";");
            tjCodeList = Arrays.asList(tjCodeArray);
        }

        int queryIndexId =0;
        ReportIndex reportIndex = getIndexById(indexId);
        if(reportIndex != null){
            while (1==1){
                reportIndex = getIndexById(reportIndex.getParentId());
                if(reportIndex.getIsQuery().equals("1")){
                    queryIndexId = reportIndex.getIndexId();
                    break;
                }
            }

        }

        List<ReportCondit> conditList = reportConditMapper.selConditNotConditCode(tjCodeList,queryIndexId);

        if(conditList != null && conditList.size() >0){
            for(ReportCondit reportCondit:conditList){
                reportCondit.setIndexId(indexId);
            }
        }

        return  conditList;

    }

    public ReportIndex getIndexById(int indexId){
        return reportIndexMapper.selectById(indexId);
    }


    /**
     * 导出报表
     * @param reportParam
     * @return
     */
    @Override
    public JSONObject fileDownload(ReportParam reportParam){
        JSONObject retJson = new JSONObject();
        ExcelWriter excelWriter = null;
        try {
            List<Map<String, Object>> retReportDataList = new ArrayList<>();
            if(reportParam.getIndexType().equals("details")){ //明细
                retReportDataList = getReportDetailsList(reportParam);
            }else{ //统计
                String dimensionInfo = reportParam.getDimensionInfo();

                //2025-11-25
                String indexInfo = reportParam.getIndexInfo();
                if(StringUtil.isNotEmpty(indexInfo) && indexInfo.indexOf("\"isFiveInten\":\"1\"") >=0){ //统计大宽表
                    //页面维度全区，地市，区县，网格 选择了其中一个
                    if(StringUtil.isNotEmpty(dimensionInfo) &&
                            (dimensionInfo.indexOf("selAllQuanQu") >=0 || dimensionInfo.indexOf("selAllDiShi") >=0
                                    || dimensionInfo.indexOf("selAllQuXian") >=0 || dimensionInfo.indexOf("selAllWangGe") >=0)){
                        retReportDataList = getReportCountAllSelectExport(reportParam,true);
                    }else{
                        retReportDataList = getReportCountListTjkb(reportParam);
                    }
                }else{//非统计大宽表
                    //页面维度全区，地市，区县，网格 选择了其中一个
                    if(StringUtil.isNotEmpty(dimensionInfo) &&
                            (dimensionInfo.indexOf("selAllQuanQu") >=0 || dimensionInfo.indexOf("selAllDiShi") >=0
                                    || dimensionInfo.indexOf("selAllQuXian") >=0 || dimensionInfo.indexOf("selAllWangGe") >=0)){
                        retReportDataList = getReportCountAllSelectExport(reportParam,false);
                    }else{
                        retReportDataList = getReportCountList(reportParam);
                    }
                }

            }

            List headsList = new ArrayList<>();
            List<String> idList = new ArrayList<>();

            //2025-11-20
            String selArea ="";
            String selColumnName ="";
            String dimensionInfo = reportParam.getDimensionInfo();
            JSONArray dimensionJsonArray = JSONArray.parseArray(dimensionInfo);
            if(dimensionJsonArray != null && dimensionJsonArray.size() >0){
                for(Object dimensionJsonObject: dimensionJsonArray){
                    JSONObject dimensionJson = (JSONObject)dimensionJsonObject;
                    selArea = dimensionJson.getString("id");
                    selColumnName = dimensionJson.getString("columnName");
                }
            }

            if(selArea.equals("selAllQuXian")){ //区县
                List<String> dsHeadList = new ArrayList<>();
                dsHeadList.add("地市");
                headsList.add(dsHeadList);

                //idList.add("hx_latn_name");
                idList.add("hxLatnName");
            }else if(selArea.equals("selAllWangGe")){ //网格
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

            //选中下一级维度
            if(StringUtil.isNotEmpty(reportParam.getXyjwd()) && reportParam.getXyjwd().equals("1")){
                if(selColumnName.equals("hxLatnName")){//地市
                    List<String> dsHeadList = new ArrayList<>();
                    dsHeadList.add("地市");
                    headsList.add(dsHeadList);

                    //idList.add("hx_latn_name");
                    idList.add("hxLatnName");
                }else if(selColumnName.equals("hxAreaName")){//区县
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
                }else if(selColumnName.equals("xHx5BpName")){//网格
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
            }else{
                if(selColumnName.equals("hxAreaName")){//区县
                    List<String> dsHeadList = new ArrayList<>();
                    dsHeadList.add("地市");
                    headsList.add(dsHeadList);

                    //idList.add("hx_latn_name");
                    idList.add("hxLatnName");

                }else if(selColumnName.equals("xHx5BpName")){//网格
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
            }


            String col = reportParam.getIndexInfo();
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
            }

            if(retReportDataList == null){
                retReportDataList = new ArrayList<>();
            }


            // 动态构建数据
            List<List<Object>> exportData = buildDataDown(retReportDataList, idList);

            // 确保目录存在
            File dir = new File(reportTargetPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String reportName = reportParam.getReportName();
            String fileName = reportName + ".xlsx";
            // 完整文件路径
            String filePath = reportTargetPath + fileName;

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
            /*excelWriter.write(exportData, writeSheet);
            excelWriter.finish();*/

            if(exportData != null && exportData.size() > 0){
                List<List<Object>> newExportData = new ArrayList<>();
                for(int i=0;i<exportData.size();i++){
                    newExportData.add(exportData.get(i));
                    if(i!=0 && i%2000 == 0){
                        excelWriter.write(newExportData, writeSheet);
                        newExportData.clear();

                    }
                }

                // 处理最后剩余的不足2000条的数据
                if (exportData.size() % 2000 != 0) {
                    excelWriter.write(newExportData, writeSheet);
                    newExportData.clear();
                    log.info("----newExportData——size: {}",exportData.size() % 2000);
                }

                excelWriter.finish();
            }

            log.info("+++++filePath: {}",filePath);

            String port = env.getProperty("server.port");
            String serverIp = env.getProperty("server.server-ip");

            String retFilePath =serverIp+":"+port+"/autoReportFile/"+fileName;

            retJson.put("retCode","0");
            retJson.put("filePath",retFilePath);
            retJson.put("fileName",fileName);

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
        }finally {
            // 关闭流释放资源
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
        return retJson;
    }

    /**
     * 获取报表详情数据
     * @param reportParam
     * @return
     */
    private List<Map<String, Object>> getReportDetailsList(ReportParam reportParam){
        List<Map<String, Object>> retReportDataList = new ArrayList<>();

        try {

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    columnMap.put(tableName, tableName + "." + columnName);
                                }else{
                                    tableNameMapValue = tableNameMapValue+","+tableName + "." + columnName;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();
            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }

                        /*if("1".equals(dimensionObj.getString("level"))){
                            //hxLatnIdList.add(dimensionObj.getString("id"));
                            hxLatnIdList.add(dimensionObj.getString("title"));
                        }
                        if("2".equals(dimensionObj.getString("level"))){
                            //hxAreaIdList.add(dimensionObj.getString("id"));
                            hxAreaIdList.add(dimensionObj.getString("title"));
                        }
                        if("3".equals(dimensionObj.getString("level"))){
                            //hxRegionIdList.add(dimensionObj.getString("id"));
                            hxRegionIdList.add(dimensionObj.getString("title"));
                        }
                        if("4".equals(dimensionObj.getString("level"))){
                            //xHx5BpIdList.add(dimensionObj.getString("id"));
                            xHx5BpIdList.add(dimensionObj.getString("title"));
                        }*/
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlDataStr = "";    //日期
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                            }else{
                                sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                            }
                            sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        }
                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                            }else{
                                sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                            }

                            sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        }
                    }

                    //支局
                    if(!hxRegionIdList.isEmpty()){
                        for(String zjValue : hxRegionIdList){
                            if(sqlZjWhereStr.equals("")){
                                sqlZjWhereStr = key+".hx_region_name='"+zjValue+"'";
                            }else{
                                sqlZjWhereStr = sqlZjWhereStr +" or " + key+".hx_region_name='"+zjValue+"'";
                            }

                            sqlColumnStr = sqlColumnStr+","+key+".hx_region_name";
                        }
                    }

                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                            }else{
                                sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                            }

                            sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        }
                    }

                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    //数据周期
                    if(reportParam.getDateType().equals("day")){ //日
                        //开始时间和结束时间不为空时
                        if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                            sqlDataStr = sqlDataStr + " and " + key+".op_date >='"+startDate+"' and "+ key+".op_date <='"+endDate+"'";

                        }else if(StringUtil.isNotEmpty(startDate)){ //只有开始日期
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+startDate+"'";
                        }else if(StringUtil.isNotEmpty(endDate)){//只有结束日期
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+endDate+"'";
                        }else{ //开始日期和结束日期都为空
                            String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";
                        }

                    }else{ //月

                        //开始时间和结束时间不为空时
                        if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                            sqlDataStr = sqlDataStr + " and " + key+".op_date >='"+startDate+"' and "+ key+".op_date <='"+endDate+"'";
                        }else if(StringUtil.isNotEmpty(startDate)){ //只有开始日期
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+startDate+"'";
                        }else if(StringUtil.isNotEmpty(endDate)){//只有结束日期
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+endDate+"'";
                        }else{ //开始日期和结束日期都为空
                            String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                            sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";
                        }
                    }

                    //客户
                    String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                }

                log.info("------details-sqlColumnStr: "+sqlColumnStr);
                log.info("------details-sqlTableStr: "+sqlTableStr);
                log.info("------details-sqlWhereStr: "+sqlWhereStr);
                log.info("------details-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------details-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------details-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------details-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------details-sqlDataStr: "+sqlDataStr);
                log.info("------details-sqlCustTypeStr: "+sqlCustTypeStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr +sqlDataStr + sqlCustTypeStr + sqlRoleTypeStr;

                    sqlStr = sqlStr.toUpperCase();

                    log.info("+++++++++自助报表导出详情SQL: {}",sqlStr);

                    //查询结果
                    retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStr);

                }
            }

            //log.info("=======自助报表详情查询结果：{}",retReportDataList);

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }


    /**
     * 获取报表统计数据
     * @param reportParam
     * @return
     */
    private List<Map<String, Object>> getReportCountList(ReportParam reportParam){
        List<Map<String, Object>> retReportDataList = new ArrayList<>();

        try {

            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr()==null?"":reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr()==null?"":reportIndex.getFmBdsEr(); //分母表达式2

                                //tableExpr = "("+tableExpr+") ";
                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    //2025-10-14
                                    //替换分子表达式的日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceDateLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一天日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceDateLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式的日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds = replaceDateLast(fmBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式前一天日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceDateLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2的日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceDateLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分子表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceDateLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2的日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceDateLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceDateLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    //2025-10-14
                                    //替换分子表达式日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceMonthLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一个月
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceMonthLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds =replaceMonthLast(fmBds,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分母表达式前一个月
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceMonthLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceMonthLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式2前一个月
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceMonthLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceMonthLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一个月
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceMonthLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }
                                }

                                //替换日期占位符
                                tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);


                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();

            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        String wdColumnName = dimensionObj.getString("columnName");
                        //2025-11-19 下一级维度
                        if(StringUtil.isNotEmpty(reportParam.getXyjwd()) && reportParam.getXyjwd().equals("1")){
                            String id = dimensionObj.getString("id");
                            if(wdColumnName.equals("hxLatnName")){ //地市
                                wdColumnName = "hxAreaName";
                                //根据地市获取地市下一级的区县
                                QueryWrapper<ReportDimension> qxWrapper = new QueryWrapper<>();
                                qxWrapper.eq("parent_id",id);
                                List<ReportDimension> qxDimensionList = reportDimensionMapper.selectList(qxWrapper);
                                if(qxDimensionList != null && qxDimensionList.size() >0){
                                    for(ReportDimension qxReportDimension:qxDimensionList){
                                        hxAreaIdList.add(qxReportDimension.getDimensionName());
                                    }
                                }

                            }else if(wdColumnName.equals("hxAreaName")){ //区县
                                wdColumnName = "xHx5BpName";
                                //根据区县获取区县下一级的网格
                                QueryWrapper<ReportDimension> wgWrapper = new QueryWrapper<>();
                                wgWrapper.eq("parent_id",id);
                                List<ReportDimension> wgDimensionList = reportDimensionMapper.selectList(wgWrapper);
                                if(wgDimensionList != null && wgDimensionList.size() >0){
                                    for(ReportDimension wgReportDimension:wgDimensionList){
                                        xHx5BpIdList.add(wgReportDimension.getDimensionName());
                                    }
                                }
                            }else if(wdColumnName.equals("xHx5BpName")){ //网格
                                wdColumnName = "xHx5BpName";
                                xHx5BpIdList.add(dimensionObj.getString("title"));
                            }
                        }else{
                            if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                                //hxLatnIdList.add(dimensionObj.getString("id"));
                                hxLatnIdList.add(dimensionObj.getString("title"));
                            }
                            if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                                //hxAreaIdList.add(dimensionObj.getString("id"));
                                hxAreaIdList.add(dimensionObj.getString("title"));
                            }
                            if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                                //hxRegionIdList.add(dimensionObj.getString("id"));
                                hxRegionIdList.add(dimensionObj.getString("title"));
                            }
                            if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                                //xHx5BpIdList.add(dimensionObj.getString("id"));
                                xHx5BpIdList.add(dimensionObj.getString("title"));
                            }
                        }
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                            }else{
                                sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                            }
                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                            }else{
                                sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                            }
                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                    }

                    //支局
                    if(!hxRegionIdList.isEmpty()){
                        for(String zjValue : hxRegionIdList){
                            if(sqlZjWhereStr.equals("")){
                                sqlZjWhereStr = key+".hx_region_name='"+zjValue+"'";
                            }else{
                                sqlZjWhereStr = sqlZjWhereStr +" or " + key+".hx_region_name='"+zjValue+"'";
                            }
                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_region_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_region_name,"+key+".hx_area_name,"+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_region_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_region_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_region_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_region_name";
                        }
                    }

                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                            }else{
                                sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                            }

                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_area_name,"+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_area_name,"+key+".hx_latn_name";
                        }
                    }



                    //客户类型
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------count-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------count-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr + sqlDzOrderByStr;

                    /*String sqlStr = "select count(IS_MQ) IS_MQ,count(is_cs) is_cs,khjy_sp_mobcj_mkt_d.hx_latn_name " +
                            "from khjy_sp_mobcj_mkt_d khjy_sp_mobcj_mkt_d where 1=1 " +
                            "and (khjy_sp_mobcj_mkt_d.hx_latn_name='乌鲁木齐' or khjy_sp_mobcj_mkt_d.hx_latn_name='和田') " +
                            "and khjy_sp_mobcj_mkt_d.op_date='20250714' and khjy_sp_mobcj_mkt_d.cust_id !=''";*/

                    sqlStr = sqlStr.toUpperCase();


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){
                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);
                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStr);
                    }else{
                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                                + sqlWgWhereStr + " and " +whereDateStr + sqlDzOrderByStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStrDate);
                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);
                    }

                    //计算合计列数据
                    retReportDataList = getCountListTj(retReportDataList);
                    /*if(retReportDataList !=null && retReportDataList.size() >0){
                        Map<String, Object> totalMap = new HashMap<>();
                        Map<String, Object> dataMap = retReportDataList.get(0);
                        Set<String> keyset = dataMap.keySet();
                        for(String key : keyset){
                            Object objValue = dataMap.get(key);

                            if(key.equals("HX_LATN_NAME")){
                                totalMap.put(key,"合计");
                            }else if(key.equals("HX_AREA_NAME") || key.equals("X_HX5_BP_NAME")){
                                totalMap.put(key,"-");
                            }else if(objValue != null && objValue instanceof String && objValue.toString().indexOf("%") >0){ //百分率求平均值
                                //BigDecimal average = calculateAver(retReportDataList, key);
                                BigDecimal average = calculateSumLv(retReportDataList, key);
                                String averageStr = average+"%";
                                totalMap.put(key,averageStr);
                            }else{ //数值求合
                                BigDecimal totalSum = calculateSum(retReportDataList, key);
                                totalMap.put(key,totalSum);
                            }
                        }
                        retReportDataList.add(totalMap);
                    }*/

                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }


    /**
     * 获取报表统计数据_五项集约
     * @param reportParam
     * @return
     */
    private List<Map<String, Object>> getReportCountListTjkb(ReportParam reportParam){
        List<Map<String, Object>> retReportDataList = new ArrayList<>();

        try {

            String whereDateStr ="";
            String tableName ="";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds();
                                String fmBds = reportIndex.getFmBds();
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                //tableExpr = "("+tableExpr+") ";
                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }
                                }


                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();

            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        String wdColumnName = dimensionObj.getString("columnName");
                        //2025-11-19 下一级维度
                        if(StringUtil.isNotEmpty(reportParam.getXyjwd()) && reportParam.getXyjwd().equals("1")){
                            String id = dimensionObj.getString("id");
                            if(wdColumnName.equals("hxLatnName")){ //地市
                                wdColumnName = "hxAreaName";
                                //根据地市获取地市下一级的区县
                                QueryWrapper<ReportDimension> qxWrapper = new QueryWrapper<>();
                                qxWrapper.eq("parent_id",id);
                                List<ReportDimension> qxDimensionList = reportDimensionMapper.selectList(qxWrapper);
                                if(qxDimensionList != null && qxDimensionList.size() >0){
                                    for(ReportDimension qxReportDimension:qxDimensionList){
                                        //hxAreaIdList.add(qxReportDimension.getDimensionName());
                                        String dimensionId = qxReportDimension.getDimensionId();
                                        if(dimensionId.indexOf("_f") >=0){
                                            dimensionId = dimensionId.replaceAll("_f","");
                                        }

                                        hxAreaIdList.add(dimensionId);
                                    }
                                }

                            }else if(wdColumnName.equals("hxAreaName")){ //区县
                                wdColumnName = "xHx5BpName";
                                //根据区县获取区县下一级的网格
                                QueryWrapper<ReportDimension> wgWrapper = new QueryWrapper<>();
                                wgWrapper.eq("parent_id",id);
                                List<ReportDimension> wgDimensionList = reportDimensionMapper.selectList(wgWrapper);
                                if(wgDimensionList != null && wgDimensionList.size() >0){
                                    for(ReportDimension wgReportDimension:wgDimensionList){
                                        //xHx5BpIdList.add(wgReportDimension.getDimensionName());
                                        String dimensionId = wgReportDimension.getDimensionId();
                                        if(dimensionId.indexOf("_f") >=0){
                                            dimensionId = dimensionId.replaceAll("_f","");
                                        }

                                        xHx5BpIdList.add(dimensionId);
                                    }
                                }
                            }else if(wdColumnName.equals("xHx5BpName")){ //网格
                                wdColumnName = "xHx5BpName";
                                String dimensionId = dimensionObj.getString("id");
                                if(dimensionId.indexOf("_f") >=0){
                                    dimensionId = dimensionId.replaceAll("_f","");
                                }

                                xHx5BpIdList.add(dimensionId);
                            }
                        }else{

                            String dimensionId = dimensionObj.getString("id");
                            if(dimensionId.indexOf("_f") >=0){
                                dimensionId = dimensionId.replaceAll("_f","");
                            }

                            if("hxLatnName".equals(wdColumnName)){

                                hxLatnIdList.add(dimensionId);
                                //hxLatnIdList.add(dimensionObj.getString("title"));
                            }else if("hxAreaName".equals(wdColumnName)){
                                hxAreaIdList.add(dimensionId);
                                //hxAreaIdList.add(dimensionObj.getString("title"));
                            }else if("hxRegionName".equals(wdColumnName)){
                                //hxRegionIdList.add(dimensionObj.getString("id"));
                                hxRegionIdList.add(wdColumnName);
                            }else if("xHx5BpName".equals(wdColumnName)){
                                xHx5BpIdList.add(dimensionId);
                                //xHx5BpIdList.add(dimensionObj.getString("title"));
                            }
                        }

                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlGroupByStr = "";  //分组信息
            String sqlHxTypeStr ="";

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }

                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                //sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                                sqlDzWhereStr = "'"+dzValue+"'";
                            }else{
                                //sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                                sqlDzWhereStr = sqlDzWhereStr +",'"+dzValue+"'";
                            }

                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        sqlHxTypeStr = " and hx_type='分公司' ";

                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                //sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                                sqlXfWhereStr = "'"+xfValue+"'";
                            }else{
                                //sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                                sqlXfWhereStr = sqlXfWhereStr +",'"+xfValue+"'";
                            }
                        }

                        //2025-11-19
                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name"+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                        sqlHxTypeStr = " and hx_type='县分' ";

                    }


                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                //sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                                sqlWgWhereStr = "'"+wgValue+"'";
                            }else{
                                //sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                                sqlWgWhereStr = sqlWgWhereStr +",'"+wgValue+"'";
                            }
                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_area_name"+","+key+".hx_latn_name";

                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_area_name"+","+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_area_name"+","+key+".hx_latn_name";
                        }

                        sqlHxTypeStr = " and hx_type='网格' ";

                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";
                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------count-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------count-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlHxTypeStr: "+sqlHxTypeStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        //sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                        sqlDzWhereStr =  " and  hx_latn_id in ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        //sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                        sqlXfWhereStr =  " and hx_area_id in ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        //sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                        sqlWgWhereStr =  " and x_hx5_bp_id in ("+sqlWgWhereStr+")";
                    }

                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr + " and " +whereDateStr + " group by " +sqlGroupByStr + sqlDzOrderByStr;*/

                    String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr + " and " +whereDateStr + sqlHxTypeStr + sqlDzOrderByStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStrDate);
                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);


                    //计算合计列数据
                    retReportDataList = getCountListTjkb(retReportDataList,tableName);

                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
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
     * 自动化报表
     * @param reportParam
     * @return
     */
    @Override
    public JSONObject autoCreateReport(ReportParam reportParam){
        JSONObject retJsonObject =  new JSONObject();
        try {
            //获取当前登录用户信息
            SysUser currentUser = UserContext.currentUser();

            //拼接SQL语句
            String countSql = "";

            String dimensionInfo = reportParam.getDimensionInfo();
            //2025-11-25
            String indexInfo = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexInfo) && indexInfo.indexOf("\"isFiveInten\":\"1\"") >=0){//大宽表统计
                //页面维度全区，地市，区县，网格 选择了其中一个
                if(StringUtil.isNotEmpty(dimensionInfo) &&
                        (dimensionInfo.indexOf("selAllQuanQu") >=0 || dimensionInfo.indexOf("selAllDiShi") >=0
                                || dimensionInfo.indexOf("selAllQuXian") >=0 || dimensionInfo.indexOf("selAllWangGe") >=0)){
                    countSql = getReportCountAllSelectSql(reportParam,true);
                }else{
                    countSql = getReportCountSqlWxjy(reportParam);
                }
            }else{ //非大宽表统计
                //页面维度全区，地市，区县，网格 选择了其中一个
                if(StringUtil.isNotEmpty(dimensionInfo) &&
                        (dimensionInfo.indexOf("selAllQuanQu") >=0 || dimensionInfo.indexOf("selAllDiShi") >=0
                                || dimensionInfo.indexOf("selAllQuXian") >=0 || dimensionInfo.indexOf("selAllWangGe") >=0)){
                    countSql = getReportCountAllSelectSql(reportParam,false);
                }else{
                    countSql = getReportCountSql(reportParam);
                }
            }

             log.info("+++++autoCreateReport--countSql: {}",countSql);

             //获取所选指标ID
             String selIndexIds = getSelIndexId(reportParam.getIndexInfo());

             String addPushLeaderId = "";
             String addPushLeaderName = "";
             String classType ="bqwd";
             String queryPushLeaderId = reportParam.getAddPushLeaderId();
             if(StringUtil.isNotEmpty(queryPushLeaderId)){
                JSONArray pushLeaderIdArr = JSONArray.parseArray(queryPushLeaderId);
                if(pushLeaderIdArr != null && pushLeaderIdArr.size() >0){
                    for(Object Object : pushLeaderIdArr){
                        JSONObject pushLeaderObject = (JSONObject) Object;
                        classType = pushLeaderObject.getString("classType")==null?"":pushLeaderObject.getString("classType");
                        String phone = pushLeaderObject.getString("phone");
                        String name = pushLeaderObject.getString("name");
                        String manageId = pushLeaderObject.getString("manageId");

                        if(classType.equals("zzwd")){
                            if(StringUtil.isEmpty(addPushLeaderId)){
                                addPushLeaderId = phone;
                            }else{
                                addPushLeaderId = addPushLeaderId +";"+phone;
                            }
                            if(StringUtil.isEmpty(addPushLeaderName)){
                                addPushLeaderName = name;
                            }else{
                                addPushLeaderName = addPushLeaderName +";"+name;
                            }
                        }else{
                            if(StringUtil.isEmpty(addPushLeaderId)){
                                addPushLeaderId = manageId;
                            }else{
                                addPushLeaderId = addPushLeaderId +";"+manageId;
                            }
                            if(StringUtil.isEmpty(addPushLeaderName)){
                                addPushLeaderName = name;
                            }else{
                                addPushLeaderName = addPushLeaderName +";"+name;
                            }
                        }
                    }
                }
             }

             ReportAutoCreateInfo autoCreateInfo = new ReportAutoCreateInfo();
             autoCreateInfo.setReportName(reportParam.getReportName());
             autoCreateInfo.setSendCycle(reportParam.getSendCycle());
             autoCreateInfo.setSendDay(reportParam.getSendMonth());
             autoCreateInfo.setSendTime(reportParam.getSendTime());
             autoCreateInfo.setPushObjectId(reportParam.getPushObjectId());
             autoCreateInfo.setSqlContent(countSql);
             autoCreateInfo.setIndexInfo(reportParam.getIndexInfo());
             autoCreateInfo.setCreateTime(LocalDateTime.now());
             autoCreateInfo.setCreateUserId(currentUser.getUserId());
             autoCreateInfo.setState("0");
             autoCreateInfo.setDateType(reportParam.getDateType());
             //autoCreateInfo.setPushLeaderId(reportParam.getPushLeaderId());
             autoCreateInfo.setPushLeaderId(addPushLeaderId);
             autoCreateInfo.setIndexId(selIndexIds);
             autoCreateInfo.setIsNonStand(reportParam.getIsNonNtand());
             autoCreateInfo.setPushObjectType(classType);
             autoCreateInfo.setPushLeaderName(addPushLeaderName);

             if(StringUtil.isNotEmpty(reportParam.getAddPushLeaderId())){
                 autoCreateInfo.setApplyResult("1");
             }

             int addResult = reportAutoCreateInfoMapper.insert(autoCreateInfo);
             if(addResult >0){

                 String pushLeaderId = reportParam.getAddPushLeaderId();
                 if(StringUtil.isNotEmpty(pushLeaderId)){ //推送领导ID不为空
                     String pushTime = "";
                     String sendCycle = reportParam.getSendCycle();
                     if(sendCycle.equals("daily")){ //每日
                         pushTime = reportParam.getSendTime().replaceAll(":","时")+"分";
                     }else{ //每月
                         pushTime = reportParam.getSendMonth()+"日"+reportParam.getSendTime().replaceAll(":","时")+"分";;
                     }


                     //创建推送至领导申请记录
                     ReportPustLeaderApproval reportPustLeaderApproval  = new ReportPustLeaderApproval();
                     reportPustLeaderApproval.setReportName(reportParam.getReportName());
                     reportPustLeaderApproval.setPushTime(pushTime);
                     //reportPustLeaderApproval.setPushLeaderId(reportParam.getPushLeaderId());
                     reportPustLeaderApproval.setApplyPersonId(currentUser.getUserId());
                     reportPustLeaderApproval.setCreateTime(LocalDateTime.now());
                     reportPustLeaderApproval.setApplyState("1");
                     reportPustLeaderApproval.setAutoCreateId(autoCreateInfo.getInfoId());
                     reportPustLeaderApproval.setPushLeaderId(addPushLeaderId);
                     reportPustLeaderApproval.setPushLeaderName(addPushLeaderName);
                     reportPustLeaderApproval.setPushObjectType(classType);

                     int addPustLeaderApproval = reportPustLeaderApprovalMapper.insert(reportPustLeaderApproval);
                     if(addPustLeaderApproval >0){
                         retJsonObject.put("retCode","0");
                         retJsonObject.put("retMsg","成功");
                     }else{
                         retJsonObject.put("retCode","-1");
                         retJsonObject.put("retMsg","失败");
                     }
                 }else{
                     retJsonObject.put("retCode","0");
                     retJsonObject.put("retMsg","成功");
                 }

             }else{
                 retJsonObject.put("retCode","-1");
                 retJsonObject.put("retMsg","失败");
             }


        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","系统异常");
        }
        return retJsonObject;
    }


    /**
     * 获取自动化报表统计SQL语句
     * @param reportParam
     * @return
     */
    private String getReportCountSql(ReportParam reportParam){
        String sqlStr = "";

        try {

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();
            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        String wdColumnName = dimensionObj.getString("columnName");
                        //2025-11-19 下一级维度
                        if(StringUtil.isNotEmpty(reportParam.getXyjwd()) && reportParam.getXyjwd().equals("1")){
                            String id = dimensionObj.getString("id");
                            if(wdColumnName.equals("hxLatnName")){ //地市
                                wdColumnName = "hxAreaName";
                                //根据地市获取地市下一级的区县
                                QueryWrapper<ReportDimension> qxWrapper = new QueryWrapper<>();
                                qxWrapper.eq("parent_id",id);
                                List<ReportDimension> qxDimensionList = reportDimensionMapper.selectList(qxWrapper);
                                if(qxDimensionList != null && qxDimensionList.size() >0){
                                    for(ReportDimension qxReportDimension:qxDimensionList){
                                        hxAreaIdList.add(qxReportDimension.getDimensionName());
                                    }
                                }

                            }else if(wdColumnName.equals("hxAreaName")){ //区县
                                wdColumnName = "xHx5BpName";
                                //根据区县获取区县下一级的网格
                                QueryWrapper<ReportDimension> wgWrapper = new QueryWrapper<>();
                                wgWrapper.eq("parent_id",id);
                                List<ReportDimension> wgDimensionList = reportDimensionMapper.selectList(wgWrapper);
                                if(wgDimensionList != null && wgDimensionList.size() >0){
                                    for(ReportDimension wgReportDimension:wgDimensionList){
                                        xHx5BpIdList.add(wgReportDimension.getDimensionName());
                                    }
                                }
                            }else if(wdColumnName.equals("xHx5BpName")){ //网格
                                wdColumnName = "xHx5BpName";
                                xHx5BpIdList.add(dimensionObj.getString("title"));
                            }
                        }else{
                            if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                                //hxLatnIdList.add(dimensionObj.getString("id"));
                                hxLatnIdList.add(dimensionObj.getString("title"));
                            }
                            if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                                //hxAreaIdList.add(dimensionObj.getString("id"));
                                hxAreaIdList.add(dimensionObj.getString("title"));
                            }
                            if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                                //hxRegionIdList.add(dimensionObj.getString("id"));
                                hxRegionIdList.add(dimensionObj.getString("title"));
                            }
                            if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                                //xHx5BpIdList.add(dimensionObj.getString("id"));
                                xHx5BpIdList.add(dimensionObj.getString("title"));
                            }
                        }
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息
            String sqlWhereDate =""; //查询日期

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                            }else{
                                sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                            }

                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";
                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                            }else{
                                sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                            }
                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";
                    }

                    //支局
                    if(!hxRegionIdList.isEmpty()){
                        for(String zjValue : hxRegionIdList){
                            if(sqlZjWhereStr.equals("")){
                                sqlZjWhereStr = key+".hx_region_name='"+zjValue+"'";
                            }else{
                                sqlZjWhereStr = sqlZjWhereStr +" or " + key+".hx_region_name='"+zjValue+"'";
                            }
                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_region_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_region_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_region_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_region_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_region_name";
                        }
                    }

                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                            }else{
                                sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                            }

                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    }

                    //sqlDataStr = sqlDataStr + " and " + key+".op_date='{repl_date}'";
                    //数据周期
                    /*if(reportParam.getDateType().equals("day")){ //日
                        String replDate = "";
                        if(sqlColumnStr.indexOf("{repl_date") >=0){
                            replDate = sqlColumnStr.substring(sqlColumnStr.indexOf("{repl_date")+10,sqlColumnStr.indexOf("}"));
                            System.out.println("replDate: "+replDate);
                            String strDate = "";
                            if(replDate.equals("")){
                                strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                            }else{
                                int iReplDate = Integer.parseInt(replDate);
                                strDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",iReplDate);
                            }

                            //sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";

                            sqlColumnStr = sqlColumnStr.replaceAll("\\{repl_date"+replDate+"}",strDate);
                        }


                    }else{ //月
                        String replDate = "";
                        if(sqlColumnStr.indexOf("{repl_date") >=0){
                            replDate = sqlColumnStr.substring(sqlColumnStr.indexOf("{repl_date")+10,sqlColumnStr.indexOf("}"));
                            System.out.println("replDate: "+replDate);
                            String strDate = "";
                            if(replDate.equals("")){
                                strDate = DateTimeUtil.getLastMonth("yyyyMM");
                            }else{
                                int iReplDate = Integer.parseInt(replDate);
                                strDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",iReplDate);
                            }

                            //sqlDataStr = sqlDataStr + " and " + key+".op_date='"+strDate+"'";

                            sqlColumnStr = sqlColumnStr.replaceAll("\\{repl_date"+replDate+"}",strDate);
                        }
                    }*/

                    //客户类型 2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                    if(sqlWhereDate.equals("")){
                        sqlWhereDate = key+".op_date='{repl_date}' ";
                    }else{
                        sqlWhereDate = sqlWhereDate+" and " +key+".op_date='{repl_date}' ";
                    }

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------count-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------count-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlWhereDate: "+sqlWhereDate);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                    sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr + sqlDzOrderByStr;

                    /*String sqlStr = "select count(IS_MQ) IS_MQ,count(is_cs) is_cs,khjy_sp_mobcj_mkt_d.hx_latn_name " +
                            "from khjy_sp_mobcj_mkt_d khjy_sp_mobcj_mkt_d where 1=1 " +
                            "and (khjy_sp_mobcj_mkt_d.hx_latn_name='乌鲁木齐' or khjy_sp_mobcj_mkt_d.hx_latn_name='和田') " +
                            "and khjy_sp_mobcj_mkt_d.op_date='20250714' and khjy_sp_mobcj_mkt_d.cust_id !=''";*/
                    if(sqlStr.indexOf(".op_date") <0 && sqlStr.indexOf(".OP_DATE") <0){
                        sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                                + sqlWgWhereStr + sqlCustTypeStr + " and "+sqlWhereDate + sqlDzOrderByStr;
                    }

                    log.info("+++++++++自动报表统计SQL: {}",sqlStr);

                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }


    /**
     * 获取自动化报表统计SQL语句 统计大宽表
     * @param reportParam
     * @return
     */
    private String getReportCountSqlWxjy(ReportParam reportParam){
        String sqlStr = "";

        try {

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ columnName;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析维度信息
            /**
             * 分公司
             */
            List<String> hxLatnIdList = new ArrayList<String>();

            /**
             * 县分
             */
            List<String> hxAreaIdList = new ArrayList<String>();
            /**
             * 支局
             */
            List<String> hxRegionIdList = new ArrayList<String>();
            /**
             * 网格
             */
            List<String> xHx5BpIdList = new ArrayList<String>();
            if(reportParam.getDimensionInfo() != null){
                JSONArray dimensionArr = JSON.parseArray(reportParam.getDimensionInfo());
                if(dimensionArr != null && dimensionArr.size() > 0){
                    for(int i = 0; i < dimensionArr.size(); i ++){
                        JSONObject dimensionObj = dimensionArr.getJSONObject(i);
                        String wdColumnName = dimensionObj.getString("columnName");
                        //2025-11-19 下一级维度
                        if(StringUtil.isNotEmpty(reportParam.getXyjwd()) && reportParam.getXyjwd().equals("1")){
                            String id = dimensionObj.getString("id");
                            if(wdColumnName.equals("hxLatnName")){ //地市
                                wdColumnName = "hxAreaName";
                                //根据地市获取地市下一级的区县
                                QueryWrapper<ReportDimension> qxWrapper = new QueryWrapper<>();
                                qxWrapper.eq("parent_id",id);
                                List<ReportDimension> qxDimensionList = reportDimensionMapper.selectList(qxWrapper);
                                if(qxDimensionList != null && qxDimensionList.size() >0){
                                    for(ReportDimension qxReportDimension:qxDimensionList){
                                        //hxAreaIdList.add(qxReportDimension.getDimensionName());
                                        String dimensionId = qxReportDimension.getDimensionId();
                                        if(dimensionId.indexOf("_f") >=0){
                                            dimensionId = dimensionId.replaceAll("_f","");
                                        }

                                        hxAreaIdList.add(dimensionId);
                                    }
                                }

                            }else if(wdColumnName.equals("hxAreaName")){ //区县
                                wdColumnName = "xHx5BpName";
                                //根据区县获取区县下一级的网格
                                QueryWrapper<ReportDimension> wgWrapper = new QueryWrapper<>();
                                wgWrapper.eq("parent_id",id);
                                List<ReportDimension> wgDimensionList = reportDimensionMapper.selectList(wgWrapper);
                                if(wgDimensionList != null && wgDimensionList.size() >0){
                                    for(ReportDimension wgReportDimension:wgDimensionList){
                                        //xHx5BpIdList.add(wgReportDimension.getDimensionName());
                                        String dimensionId = wgReportDimension.getDimensionId();
                                        if(dimensionId.indexOf("_f") >=0){
                                            dimensionId = dimensionId.replaceAll("_f","");
                                        }

                                        xHx5BpIdList.add(dimensionId);
                                    }
                                }
                            }else if(wdColumnName.equals("xHx5BpName")){ //网格
                                wdColumnName = "xHx5BpName";
                                String dimensionId = dimensionObj.getString("id");
                                if(dimensionId.indexOf("_f") >=0){
                                    dimensionId = dimensionId.replaceAll("_f","");
                                }

                                xHx5BpIdList.add(dimensionId);
                            }
                        }else{

                            String dimensionId = dimensionObj.getString("id");
                            if(dimensionId.indexOf("_f") >=0){
                                dimensionId = dimensionId.replaceAll("_f","");
                            }

                            if("hxLatnName".equals(dimensionObj.getString("columnName"))){
                                hxLatnIdList.add(dimensionId);
                                //hxLatnIdList.add(dimensionObj.getString("title"));
                            }
                            if("hxAreaName".equals(dimensionObj.getString("columnName"))){
                                hxAreaIdList.add(dimensionId);
                                //hxAreaIdList.add(dimensionObj.getString("title"));
                            }
                            if("hxRegionName".equals(dimensionObj.getString("columnName"))){
                                //hxRegionIdList.add(dimensionObj.getString("id"));
                                hxRegionIdList.add(wdColumnName);
                            }
                            if("xHx5BpName".equals(dimensionObj.getString("columnName"))){
                                xHx5BpIdList.add(dimensionId);
                                //xHx5BpIdList.add(dimensionObj.getString("title"));
                            }
                        }
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlXfWhereStr ="";   //县分条件信息
            String sqlZjWhereStr ="";   //支局条件信息
            String sqlWgWhereStr ="";   //网络条件信息
            String sqlGroupByStr = "";  //分组信息
            String sqlWhereDate =""; //查询日期
            String sqlHxType =""; //划小类型

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    //地州
                    if(!hxLatnIdList.isEmpty()){
                        for(String dzValue : hxLatnIdList){
                            if(sqlDzWhereStr.equals("")){
                                //sqlDzWhereStr = key+".hx_latn_name='"+dzValue+"'";
                                sqlDzWhereStr = "'"+dzValue+"'";
                            }else{
                                //sqlDzWhereStr = sqlDzWhereStr +" or " + key+".hx_latn_name='"+dzValue+"'";
                                sqlDzWhereStr = sqlDzWhereStr +",'"+dzValue+"'";
                            }

                        }

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        sqlHxType = " and hx_type='分公司' ";
                    }

                    //县分
                    if(!hxAreaIdList.isEmpty()){
                        for(String xfValue : hxAreaIdList){
                            if(sqlXfWhereStr.equals("")){
                                //sqlXfWhereStr = key+".hx_area_name='"+xfValue+"'";
                                sqlXfWhereStr = "'"+xfValue+"'";
                            }else{
                                //sqlXfWhereStr = sqlXfWhereStr +" or " + key+".hx_area_name='"+xfValue+"'";
                                sqlXfWhereStr = sqlXfWhereStr +",'"+xfValue+"'";
                            }
                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }
                        sqlHxType = " and hx_type='县分' ";
                    }

                    //网格
                    if(!xHx5BpIdList.isEmpty()){
                        for(String wgValue : xHx5BpIdList){
                            if(sqlWgWhereStr.equals("")){
                                //sqlWgWhereStr = key+".x_hx5_bp_name='"+wgValue+"'";
                                sqlWgWhereStr = "'"+wgValue+"'";
                            }else{
                                //sqlWgWhereStr = sqlWgWhereStr +" or " + key+".x_hx5_bp_name='"+wgValue+"'";
                                sqlWgWhereStr = sqlWgWhereStr +",'"+wgValue+"'";
                            }

                        }

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }
                        sqlHxType = " and hx_type='网格' ";
                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    if(sqlWhereDate.equals("")){
                        sqlWhereDate = key+".op_date='{repl_date}' ";
                    }else{
                        sqlWhereDate = sqlWhereDate+" and " +key+".op_date='{repl_date}' ";
                    }

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------count-sqlXfWhereStr: "+sqlXfWhereStr);
                log.info("------count-sqlZjWhereStr: "+sqlZjWhereStr);
                log.info("------count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlWhereDate: "+sqlWhereDate);
                log.info("------count-sqlHxType: "+sqlHxType);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        //sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                        sqlDzWhereStr =  " and  hx_latn_id in ("+sqlDzWhereStr+")";
                    }

                    if(!sqlXfWhereStr.equals("")){
                        //sqlXfWhereStr =  " and ("+sqlXfWhereStr+")";
                        sqlXfWhereStr =  " and hx_area_id in ("+sqlXfWhereStr+")";
                    }

                    if(!sqlZjWhereStr.equals("")){
                        sqlZjWhereStr =  " and ("+sqlZjWhereStr+")";
                    }

                    if(!sqlWgWhereStr.equals("")){
                        //sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                        sqlWgWhereStr =  " and x_hx5_bp_id in ("+sqlWgWhereStr+")";
                    }

                    /*sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr + " and " +sqlWhereDate + " group by " +sqlGroupByStr + sqlDzOrderByStr;*/
                    sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlXfWhereStr + sqlZjWhereStr
                            + sqlWgWhereStr + " and " +sqlWhereDate + sqlHxType + sqlDzOrderByStr;

                    log.info("+++++++++自动报表统计SQL: {}",sqlStr);

                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }


    /**
     * 获取当前登录人所属的所有地市
     * @return
     */
    @Override
    public JSONObject getAllLatnInfo() {
        JSONObject retJson = new JSONObject();
        try {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            SysDept sysDept = sysDeptMapper.selectById(deptId);
            String deptName ="";
            if(sysDept != null){
                if(sysDept.getParentId().equals("0")){ //全疆
                    deptName = "";
                }else{
                    deptName = sysDept.getDeptName();
                }
            }

            JSONArray dataJsonArray = new JSONArray();
            QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("field","hx_latn_name");
            if(StringUtil.isNotEmpty(deptName)){
                queryWrapper.eq("dimension_name",deptName);
            }

            List<ReportDimension> list = reportDimensionMapper.selectList(queryWrapper);
            if(list != null && list.size() >0){
                for(ReportDimension reportDimension: list){
                    JSONObject dataJsonObject = new JSONObject();
                    dataJsonObject.put("level",reportDimension.getLevel());
                    dataJsonObject.put("id",reportDimension.getDimensionId());
                    dataJsonObject.put("title",reportDimension.getDimensionName());
                    dataJsonObject.put("columnName",ToolUtil.toCamelCase(reportDimension.getField()));
                    dataJsonArray.add(dataJsonObject);
                }

                retJson.put("retCode","0");
                retJson.put("retMsg","成功");
                retJson.put("retData",dataJsonArray);
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","未查询到当前登录人所属的地市信息");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","系统异常，请联系管理员");
        }

        return retJson;
    }


    /**
     * 通过数据周期,分析角色,指标类型 获取指标中已配置的分类信息
     * @return
     */
    @Override
    public Object getClassByWhere(String checkdateType, String checkRoleType,String checkIndexType){
        JSONObject retJsonObject = new JSONObject();
        try {
            QueryWrapper<ReportIndex> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("classify_id");
            queryWrapper.eq("data_cycle",checkdateType);
            queryWrapper.eq("anal_role",checkRoleType);
            queryWrapper.eq("index_type",checkIndexType);
            queryWrapper.groupBy("classify_id");

            String classIdStr = "";
            List<ReportIndex> list = reportIndexMapper.selectList(queryWrapper);
            if(list != null && list.size() >0){

                for(ReportIndex reportIndex :list){
                    int classifyId = reportIndex.getClassifyId();
                    if(classIdStr.equals("")){
                        classIdStr = classifyId+"";
                    }else{
                        classIdStr = classIdStr+";"+classifyId;
                    }
                }
            }

            retJsonObject.put("retCode","0");
            retJsonObject.put("retMsg","成功");
            retJsonObject.put("classIds",classIdStr);


        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","系统异常");
        }
        return  retJsonObject;
    }


    /**
     * 获取报表统计数据
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountAllSelectList(ReportParam reportParam, PageDomain pageDomain,boolean isWxjytj){
        JSONObject retJsonObject = new JSONObject();
        try {
             String selArea ="";
             String dimensionInfo = reportParam.getDimensionInfo();
             JSONArray dimensionJsonArray = JSONArray.parseArray(dimensionInfo);
             if(dimensionJsonArray != null && dimensionJsonArray.size() >0){
                 for(Object dimensionJsonObject: dimensionJsonArray){
                        JSONObject dimensionJson = (JSONObject)dimensionJsonObject;
                        selArea = dimensionJson.getString("id");
                 }
             }

             //2025-11-25
             if(isWxjytj){ //统计大宽表
                 //全区
                 if(selArea.equals("selAllQuanQu")){
                     retJsonObject = getReportCountAllQuanQuTjkb(reportParam,pageDomain);
                 }else if(selArea.equals("selAllDiShi")){ //地市
                     retJsonObject = getReportCountAllDiShiTjkb(reportParam,pageDomain);
                 }else if(selArea.equals("selAllQuXian")){ //区县
                     retJsonObject = getReportCountAllQuXianTjkb(reportParam,pageDomain);
                 }else if(selArea.equals("selAllWangGe")){ //网格
                     retJsonObject = getReportCountAllWangGeTjkb(reportParam,pageDomain);
                 }
             }else { //非统计大宽表
                 //全区
                 if(selArea.equals("selAllQuanQu")){
                     retJsonObject = getReportCountAllQuanQu(reportParam,pageDomain);
                 }else if(selArea.equals("selAllDiShi")){ //地市
                     retJsonObject = getReportCountAllDiShi(reportParam,pageDomain);
                 }else if(selArea.equals("selAllQuXian")){ //区县
                     retJsonObject = getReportCountAllQuXian(reportParam,pageDomain);
                 }else if(selArea.equals("selAllWangGe")){ //网格
                     retJsonObject = getReportCountAllWangGe(reportParam,pageDomain);
                 }
             }

        }catch (Exception e){
            e.printStackTrace();
        }
        return retJsonObject;

    }


    /**
     * 获取全区统计数据
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountAllQuanQu(ReportParam reportParam, PageDomain pageDomain){
            JSONObject retJsonObject = new JSONObject();
            List<Map<String,Object>> retReportDataList = new ArrayList<>();
            long dataCount = 0;
            try {

                //获取当前登录人所在的地市
                String localCity = ""; //所属地市
                SysUser currentUser = UserContext.currentUser();
                String deptId = currentUser.getDeptId();
                if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                    //获取维度表中的地市信息
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        for(ReportDimension reportDimension:reportDimensionList){
                            String dimensionName = reportDimension.getDimensionName();
                            if(localCity.equals("")){
                                localCity = "'"+dimensionName+"'";
                            }else{
                                localCity = localCity+","+"'"+dimensionName+"'";
                            }
                        }
                        localCity = "("+localCity+")";
                    }
                }else if(StringUtil.isNotEmpty(deptId)){
                    //根据部门ID获取部门名称
                    SysDept sysDept = sysDeptMapper.selectById(deptId);
                    if(sysDept != null){
                        String deptName = sysDept.getDeptName();
                        //通过部门名称查询此部门是否是维度中的地市
                        QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                        dimensionQueryWrapper.eq("field","hx_latn_name");
                        dimensionQueryWrapper.eq("dimension_name",deptName);
                        dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                        if(reportDimensionList != null && reportDimensionList.size() >0){ //地市账号
                            for(ReportDimension reportDimension:reportDimensionList){
                                String dimensionName = reportDimension.getDimensionName();
                                if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }
                            }
                            localCity = "("+localCity+")";
                        }else{ //区县账号
                            //通过部门名称查询此部门是否是维度中的区县
                            QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                            quXianQueryWrapper.eq("field","hx_area_name");
                            quXianQueryWrapper.eq("dimension_name",deptName);
                            quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                            List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                            if(quXianDimensionList != null && quXianDimensionList.size() >0){
                                for(ReportDimension reportDimension:quXianDimensionList){
                                    String dimensionName = reportDimension.getDimensionName();
                                    if(localCity.equals("")){
                                        localCity = "'"+dimensionName+"'";
                                    }else{
                                        localCity = localCity+","+"'"+dimensionName+"'";
                                    }
                                }
                                localCity = "("+localCity+")";
                            }
                        }
                    }
                }

                if(localCity.equals("")){
                    retJsonObject.put("dataCount",dataCount);
                    retJsonObject.put("dataList",retReportDataList);
                    return retJsonObject;
                }

                String whereDateStr ="";
                String sumColumnStr ="";

                //解析指标信息
                Map<String, String> columnMap = new HashMap<>();
                String indexStr = reportParam.getIndexInfo();
                if(StringUtil.isNotEmpty(indexStr)){
                    JSONArray indexJsonArray = JSON.parseArray(indexStr);
                    if(indexJsonArray !=null && indexJsonArray.size() >0){
                        //数据周期
                        String dateType = reportParam.getDateType();
                        String startDate = reportParam.getStartDate();
                        String endDate = reportParam.getEndDate();
                        startDate = startDate.replaceAll("-","");
                        endDate = endDate.replaceAll("-","");

                        for(int i = 0; i < indexJsonArray.size();i++){
                            JSONObject indexObj = indexJsonArray.getJSONObject(i);
                            String indexIdStr = indexObj.getString("id");
                            if(!indexIdStr.startsWith("wd_")){
                                int indexId = Integer.parseInt(indexIdStr);
                                ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                                if(reportIndex != null){
                                    String tableName = reportIndex.getTableName();
                                    String columnName = reportIndex.getColumnName();
                                    String tableExpr = reportIndex.getTableExpr();
                                    //2025-8-11
                                    if(dateType.equals("day")){//日

                                        if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                            whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                        }else if(StringUtil.isNotEmpty(startDate)){
                                            whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                        }else if(StringUtil.isNotEmpty(endDate)){
                                            whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                        }else{
                                            String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                            whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                        }

                                    }else{ //月

                                        if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                            whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                        }else if(StringUtil.isNotEmpty(startDate)){
                                            whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                        }else if(StringUtil.isNotEmpty(endDate)){
                                            whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                        }else{
                                            String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                            whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                        }

                                    }

                                    //替换日期占位符
                                    tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);

                                    //tableExpr = "("+tableExpr+") ";

                                    String tableNameMapValue = columnMap.get(tableName);
                                    if(StringUtil.isEmpty(tableNameMapValue)){
                                        //columnMap.put(tableName, tableExpr +columnName);
                                        columnMap.put(tableName, tableExpr);
                                    }else{
                                        //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;
                                        tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                        columnMap.put(tableName, tableNameMapValue);
                                    }

                                    if(sumColumnStr.equals("")){
                                        sumColumnStr = " SUM ("+tableExpr+") AS "+columnName;
                                    }else{
                                        sumColumnStr = sumColumnStr+","+ " SUM ("+tableExpr+") AS "+columnName;
                                    }
                                }
                            }
                    }
                    }
                }

                //解析条件信息
                Map<String,String> conditMap = new HashMap<>();
                String whereStr = reportParam.getWhereInfo();
                if(StringUtils.isNotEmpty(whereStr)){
                    ObjectMapper mapper = new ObjectMapper();
                    String[][] result = mapper.readValue(whereStr, String[][].class);

                    for (String[] row : result) {
                        String conditIdStr = row[0];
                        String conditWhereStr = row[1];

                        ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                        if(reportCondit != null){
                            int indexId = reportCondit.getIndexId();
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String coditWhereMapValue = conditMap.get(tableName);
                                if(StringUtil.isEmpty(coditWhereMapValue)){
                                    conditMap.put(tableName, tableName+"."+conditWhereStr);
                                }else{
                                    coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                    conditMap.put(tableName, coditWhereMapValue);
                                }
                            }
                        }
                    }
                }

                String sqlColumnStr =""; //查询列字段信息
                String sqlQqColumnStr =" ,'全区' sel_all_quan_qu";   //全区字段信息
                String sqlTableStr ="";  //查询表信息
                String sqlWhereStr ="";  //查询条件信息
                String sqlQqWhereStr ="";   //全区条件信息
                String sqlCustTypeStr = "";    //客户类型
                String sqlRoleTypeStr = "";    //分析角色
                String sqlGroupByStr = " group by '全区'";  //分组信息

                if(columnMap != null && columnMap.size()>0){
                    for (String key : columnMap.keySet()) {
                        String columnStr = columnMap.get(key);

                        if(sqlColumnStr.equals("")){
                            sqlColumnStr = columnStr;
                        }else{
                            sqlColumnStr = sqlColumnStr+","+columnStr;
                        }

                        if(sqlTableStr.equals("")){
                            sqlTableStr = key +" "+key;
                        }else{
                            sqlTableStr = sqlTableStr+","+key +" "+key;
                        }

                        //查询条件
                        String conditWhereStr = conditMap.get(key);
                        if(StringUtil.isNotEmpty(conditWhereStr)){
                            sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                        }

                        //客户类型 2025-9-8
                        /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                        String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                        if(custZq.equals("1") && !custGz.equals("1")){ //政企
                            sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                        }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                            sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                        }*/

                        String custType = reportParam.getCustType();
                        if(StringUtil.isNotEmpty(custType)){
                            String[] custTypeArr = custType.split(";");
                            if(custTypeArr.length ==2){
                                sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                            }
                        }

                        //分析角色
                        String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                        if(roleType.equals("customer")){ //客户
                            sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                        }else if(roleType.equals("user")){ //用户
                            sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                        }else if(roleType.equals("account")){ //账户
                            sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                        }

                        //16个地市
                        sqlQqWhereStr = " and "+key+".hx_latn_name in "+localCity;

                    }

                    log.info("------getReportCountAllQuanQu-count-sqlColumnStr: "+sqlColumnStr);
                    log.info("------getReportCountAllQuanQu-count-sqlQqColumnStr: "+sqlQqColumnStr);
                    log.info("------getReportCountAllQuanQu-count-sqlTableStr: "+sqlTableStr);
                    log.info("------getReportCountAllQuanQu-count-sqlWhereStr: "+sqlWhereStr);
                    log.info("------getReportCountAllQuanQu-ount-sqlCustTypeStr: "+sqlCustTypeStr);
                    log.info("------getReportCountAllQuanQu-count-sqlGroupByStr: "+sqlGroupByStr);
                    log.info("------getReportCountAllQuanQu-count-sqlQqWhereStr: "+sqlQqWhereStr);


                    //拼接完整的SQL查询语句
                    if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){

                        String sqlStr = "select " + sqlColumnStr + sqlQqColumnStr+" from " + sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlQqWhereStr + sqlCustTypeStr + sqlRoleTypeStr +sqlGroupByStr;

                        if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                            log.info("+++++++++getReportCountAllQuanQu-自助取数统计SQL: {}",sqlStr);

                            long startTime = System.nanoTime();

                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));

                            PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                            dataCount = pageInfo.getTotal();
                            log.info("+++++++++getReportCountAllQuanQu-总记录数: {}",dataCount);

                            long endTime = System.nanoTime();
                            long execTime = endTime - startTime;

                            log.info("++++++++getReportCountAllQuanQu-自助取数SQL方法执行时间：" + formatTime(execTime));

                        }else{

                            String sqlStrDate = "select "+sumColumnStr+" from "+sqlTableStr +" where 1=1 "
                                    +sqlWhereStr + sqlQqWhereStr+ " and " +whereDateStr +sqlGroupByStr;

                            log.info("+++++++++getReportCountAllQuanQu-自助报表统计SQL: {}",sqlStrDate);

                            /*long startTime = System.nanoTime();
                            //获取总数
                            dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                            long endTime = System.nanoTime();
                            long execTime = endTime - startTime;
                            log.info("++++++++getReportCountAllQuanQu-自助取数统计总数SQL方法执行时间：" + formatTime(execTime));
                            if(dataCount >0){
                                startTime = System.nanoTime();
                                //查询结果
                                retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                        .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                                endTime = System.nanoTime();
                                execTime = endTime - startTime;
                                log.info("++++++++getReportCountAllQuanQu-自助取数统计SQL方法执行时间：" + formatTime(execTime));

                            }*/

                            long startTime = System.nanoTime();

                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                            PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                            dataCount = pageInfo.getTotal();
                            log.info("+++++++++getReportCountAllQuanQu-总记录数: {}",dataCount);

                            long endTime = System.nanoTime();
                            long execTime = endTime - startTime;

                            log.info("++++++++getReportCountAllQuanQu-自助取数SQL方法执行时间：" + formatTime(execTime));

                        }
                    }
                }

                //log.info("=======自助报表统计查询结果：{}",retReportDataList);
                if(retReportDataList != null && retReportDataList.size() >0){
                    // 转换处理
                    retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                retJsonObject.put("dataCount",dataCount);
                retJsonObject.put("dataList",retReportDataList);
            }
            return retJsonObject;
    }


    /**
     * 获取全区统计数据统计大宽表
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountAllQuanQuTjkb(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJsonObject = new JSONObject();
        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                localCity ="('9999')";
            }

            if(localCity.equals("")){
                retJsonObject.put("dataCount",dataCount);
                retJsonObject.put("dataList",retReportDataList);
                return retJsonObject;
            }

            String whereDateStr ="";
            String sumColumnStr ="";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }


                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);
                                    columnMap.put(tableName, columnName);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;
                                    tableNameMapValue = tableNameMapValue+","+ columnName;
                                    columnMap.put(tableName, tableNameMapValue);
                                }

                                if(sumColumnStr.equals("")){
                                    sumColumnStr =  columnName;
                                }else{
                                    sumColumnStr = sumColumnStr+","+columnName;
                                }
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlQqWhereStr ="";   //全区条件信息
            String sqlQqColumnStr =" ,'全区' sel_all_quan_qu ";   //全区字段信息
            String sqlHxTypeStr =" and hx_type='分公司' ";   //全区字段信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }

                    //16个地市
                    sqlQqWhereStr = " and "+key+".hx_latn_id in "+localCity;

                }

                log.info("------getReportCountAllQuanQuTjkb-count-sqlColumnStr: "+sqlColumnStr);
                log.info("------getReportCountAllQuanQuTjkb-count-sqlTableStr: "+sqlTableStr);
                log.info("------getReportCountAllQuanQuTjkb-count-sqlQqWhereStr: "+sqlQqWhereStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){

                        String sqlStrDate = "select "+sumColumnStr+ sqlQqColumnStr +" from "+sqlTableStr +" where 1=1 "
                               + sqlQqWhereStr+ " and " +whereDateStr + sqlHxTypeStr;

                        log.info("+++++++++getReportCountAllQuanQuTjkb-自助报表统计SQL: {}",sqlStrDate);

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllQuanQuTjkb-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllQuanQuTjkb-自助取数SQL方法执行时间：" + formatTime(execTime));

                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJsonObject.put("dataCount",dataCount);
            retJsonObject.put("dataList",retReportDataList);
        }
        return retJsonObject;
    }

    /**
     * 替换表达式中的日期
     * @param dateType
     * @param tableName
     * @param tableExpr
     * @param startDate
     * @param endDate
     * @return
     */
    public String replDateStr(String dateType,String tableName,String tableExpr,String startDate,String endDate){
        try {
            if(dateType.equals("day")){//日
                if(tableExpr.indexOf("{repl_date}") >=0){
                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                        String repStr = tableName+".op_date='\\{repl_date}'";
                        String repValue = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);

                    }else if(StringUtil.isNotEmpty(startDate)){
                        String repStr = tableName+".op_date='\\{repl_date}'";
                        String repValue = " "+tableName+".op_date ='"+startDate +"' ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);

                    }else if(StringUtil.isNotEmpty(endDate)){
                        String repStr = tableName+".op_date='\\{repl_date}'";
                        String repValue = " "+tableName+".op_date ='"+endDate +"' ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);
                    }else{
                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                        tableExpr = tableExpr.replaceAll("\\{repl_date}", strDate);
                    }
                }

                //替换前一天日期
                if(tableExpr.indexOf("{repl_date-1}") >=0){
                    if(StringUtil.isNotEmpty(startDate)){
                        //获取开始日期的前一天
                        String befoStartDate = DateTimeUtil.getStrDateBefore(startDate,"yyyyMMdd",1);
                        String repStr = tableName+".op_date='\\{repl_date-1}'";
                        String repValue = " "+tableName+".op_date ='"+befoStartDate +"' ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);

                    }else if(StringUtil.isNotEmpty(endDate)){
                        //获取结束日期的前一天
                        String befoEndDate = DateTimeUtil.getStrDateBefore(endDate,"yyyyMMdd",1);
                        String repStr = tableName+".op_date='\\{repl_date-1}'";
                        String repValue = " "+tableName+".op_date ='"+befoEndDate +"' ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);
                    }else{
                        //获取当前日期的前两天
                        String strDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",-2);
                        tableExpr = tableExpr.replaceAll("\\{repl_date-1}", strDate);
                    }
                }

                if(tableExpr.indexOf("{repl_other_date}") >=0){
                    if(StringUtil.isNotEmpty(startDate)){
                        String repStr = "'\\{repl_other_date}'";
                        String repValue = "'"+startDate +"'";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);
                    }else if(StringUtil.isNotEmpty(endDate)){
                        String repStr = "'\\{repl_other_date}'";
                        String repValue = "'"+endDate +"'";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);
                    }else{
                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                        tableExpr = tableExpr.replaceAll("\\{repl_other_date}", strDate);
                    }
                }

                if(tableExpr.indexOf("{repl_other_date-1}") >=0){
                    if(StringUtil.isNotEmpty(startDate)){
                        String repStr = "'\\{repl_other_date-1}'";
                        String befoStartDate = "'"+DateTimeUtil.getStrDateBefore(startDate,"yyyyMMdd",1)+"'";
                        tableExpr = tableExpr.replaceAll(repStr, befoStartDate);
                    }else if(StringUtil.isNotEmpty(endDate)){
                        String repStr = "'\\{repl_other_date-1}'";
                        String befoEndDate = "'"+DateTimeUtil.getStrDateBefore(endDate,"yyyyMMdd",1)+"'";
                        tableExpr = tableExpr.replaceAll(repStr, befoEndDate);
                    }else{
                        String strDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",-2);
                        tableExpr = tableExpr.replaceAll("\\{repl_other_date-1}", strDate);
                    }
                }

            }else{ //月
                if(tableExpr.indexOf("{repl_date}") >=0){
                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                        String repStr = tableName+".op_date='\\{repl_date}'";
                        String repValue = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);

                    }else if(StringUtil.isNotEmpty(startDate)){
                        String repStr = tableName+".op_date='\\{repl_date}'";
                        String repValue = " "+tableName+".op_date ='"+startDate +"' ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);

                    }else if(StringUtil.isNotEmpty(endDate)){
                        String repStr = tableName+".op_date='\\{repl_date}'";
                        String repValue = " "+tableName+".op_date ='"+endDate +"' ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);
                    }else{
                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                        tableExpr = tableExpr.replaceAll("\\{repl_date}", strDate);
                    }
                }

                if(tableExpr.indexOf("{repl_date-1}") >=0){
                    if(StringUtil.isNotEmpty(startDate)){
                        String beforeDate = DateTimeUtil.getStrMonthBefore(startDate,"yyyyMM",1);
                        String repStr = tableName+".op_date='\\{repl_date-1}'";
                        String repValue = " "+tableName+".op_date ='"+beforeDate +"' ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);

                    }else if(StringUtil.isNotEmpty(endDate)){
                        String beforeDate = DateTimeUtil.getStrMonthBefore(endDate,"yyyyMM",1);
                        String repStr = tableName+".op_date='\\{repl_date-1}'";
                        String repValue = " "+tableName+".op_date ='"+beforeDate +"' ";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);
                    }else{
                        String strDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",-2);
                        tableExpr = tableExpr.replaceAll("\\{repl_date-1}", strDate);
                    }
                }

                if(tableExpr.indexOf("{repl_other_date}") >=0){
                    if(StringUtil.isNotEmpty(startDate)){
                        String repStr = "'\\{repl_other_date}'";
                        String repValue = "'"+startDate +"'";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);
                    }else if(StringUtil.isNotEmpty(endDate)){
                        String repStr = "'\\{repl_other_date}'";
                        String repValue = "'"+endDate +"'";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);
                    }else{
                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                        tableExpr = tableExpr.replaceAll("\\{repl_other_date}", strDate);
                    }
                }

                if(tableExpr.indexOf("{repl_other_date-1}") >=0){
                    if(StringUtil.isNotEmpty(startDate)){
                        String beforeDate = DateTimeUtil.getStrMonthBefore(startDate,"yyyyMM",1);
                        String repStr = "'\\{repl_other_date-1}'";
                        String repValue = "'"+beforeDate +"'";;
                        tableExpr = tableExpr.replaceAll(repStr, repValue);

                    }else if(StringUtil.isNotEmpty(endDate)){
                        String beforeDate = DateTimeUtil.getStrMonthBefore(endDate,"yyyyMM",1);
                        String repStr = "'\\{repl_other_date-1}'";
                        String repValue = "'"+beforeDate +"'";
                        tableExpr = tableExpr.replaceAll(repStr, repValue);
                    }else{
                        String strDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",-2);
                        tableExpr = tableExpr.replaceAll("\\{repl_other_date-1}", strDate);
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return tableExpr;
    }


    /**
     * 获取报表统计数据地市
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountAllDiShi(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJson = new JSONObject();

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的地市信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_latn_name");
                dimensionQueryWrapper.eq("is_non_stand", reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand", reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        for(ReportDimension reportDimension:reportDimensionList){
                            String dimensionName = reportDimension.getDimensionName();
                            if(localCity.equals("")){
                                localCity = "'"+dimensionName+"'";
                            }else{
                                localCity = localCity+","+"'"+dimensionName+"'";
                            }
                        }
                        localCity = "("+localCity+")";
                    }
                }
            }

            if(localCity.equals("")){
                retJson.put("dataCount",dataCount);
                retJson.put("dataList",retReportDataList);
                return retJson;
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName(); //表名
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr(); //表达式
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr()==null?"":reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr()==null?"":reportIndex.getFmBdsEr(); //分母表达式2

                                //2025-8-11
                                if(dateType.equals("day")){//日
                                    //2025-10-14
                                    //替换分子表达式的日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceDateLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一天日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceDateLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式的日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds = replaceDateLast(fmBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式前一天日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceDateLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2的日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceDateLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分子表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceDateLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2的日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceDateLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceDateLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    //2025-10-14
                                    //替换分子表达式日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceMonthLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一个月
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceMonthLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds =replaceMonthLast(fmBds,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分母表达式前一个月
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceMonthLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceMonthLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式2前一个月
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceMonthLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceMonthLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一个月
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceMonthLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                //tableExpr = "("+tableExpr+") ";
                                //替换日期占位符
                                tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);
                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableExpr);

                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;
                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlDzWhereStr = key+".hx_latn_name in "+localCity;

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        //2026-1-23
                        //地州排序字段顺序
                        String sortLatnName = ReportUtil.getSortLatnName();

                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    }

                    //客户类型 2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }
                }

                log.info("------getReportCountAllDiShi-count-sqlColumnStr: "+sqlColumnStr);
                log.info("------getReportCountAllDiShi-count-sqlTableStr: "+sqlTableStr);
                log.info("------getReportCountAllDiShi-count-sqlWhereStr: "+sqlWhereStr);
                log.info("------getReportCountAllDiShi-count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------getReportCountAllDiShi-count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------getReportCountAllDiShi-count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------getReportCountAllDiShi-count-sqlGroupByStr: "+sqlGroupByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr + sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        log.info("+++++++++getReportCountAllDiShi-自助取数统计SQL: {}",sqlStr);
                        /*long startTime = System.nanoTime();
                        //获取总数
                        dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStr));
                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;
                        log.info("++++++++getReportCountAllDiShi-自助取数统计总数SQL方法执行时间：" + formatTime(execTime));

                        if(dataCount >0){
                            startTime = System.nanoTime();
                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));
                            endTime = System.nanoTime();
                            execTime = endTime - startTime;
                            log.info("++++++++getReportCountAllDiShi-自助取数统计SQL方法执行时间：" + formatTime(execTime));
                        }*/

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllDiShi-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllDiShi-自助取数SQL方法执行时间：" + formatTime(execTime));

                    }else{

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + sqlDzOrderByStr;

                        log.info("+++++++++getReportCountAllDiShi-自助报表统计SQL: {}",sqlStrDate);

                        /*long startTime = System.nanoTime();
                        //获取总数
                        dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;
                        log.info("++++++++getReportCountAllDiShi-自助取数统计总数SQL方法执行时间：" + formatTime(execTime));

                        if(dataCount >0){
                            startTime = System.nanoTime();
                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                            endTime = System.nanoTime();
                            execTime = endTime - startTime;
                            log.info("++++++++getReportCountAllDiShi-自助取数统计SQL方法执行时间：" + formatTime(execTime));
                        }*/

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllDiShi-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllDiShi-自助取数SQL方法执行时间：" + formatTime(execTime));

                    }

                    //计算合计列数据
                    retReportDataList = getCountListTj(retReportDataList);
                    /*if(retReportDataList !=null && retReportDataList.size() >0){
                        Map<String, Object> totalMap = new HashMap<>();
                        Map<String, Object> dataMap = retReportDataList.get(0);
                        Set<String> keyset = dataMap.keySet();
                        for(String key : keyset){
                            Object objValue = dataMap.get(key);

                            if(key.equals("hx_latn_name") || key.equals("hx_area_name") || key.equals("hx_region_name") || key.equals("x_hx5_bp_name")){
                                totalMap.put(key,"合计");
                            }else if(objValue != null && objValue instanceof String && objValue.toString().indexOf("%") >0){ //百分率求平均值
                                //BigDecimal average = calculateAver(retReportDataList, key);
                                BigDecimal average = calculateSumLv(retReportDataList, key);
                                String averageStr = average+"%";
                                totalMap.put(key,averageStr);
                            }else{ //数值求合
                                BigDecimal totalSum = calculateSum(retReportDataList, key);
                                totalMap.put(key,totalSum);
                            }
                        }
                        retReportDataList.add(totalMap);
                    }*/


                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJson.put("dataCount",dataCount);
            retJson.put("dataList",retReportDataList);
        }

        return retJson;
    }


    /**
     * 获取报表统计数据地市_五项集约
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountAllDiShiTjkb(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJson = new JSONObject();

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            String tableName = ""; //表名

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();

            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的地市信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_latn_name");
                dimensionQueryWrapper.eq("is_non_stand", reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        //String dimensionName = reportDimension.getDimensionName();
                        String dimensionId = reportDimension.getDimensionId();
                        if(dimensionId.indexOf("_f") >=0){
                            dimensionId = dimensionId.replaceAll("_f","");
                        }

                        if(localCity.equals("")){
                            localCity = "'"+dimensionId+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionId+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand", reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        for(ReportDimension reportDimension:reportDimensionList){
                            //String dimensionName = reportDimension.getDimensionName();
                            String dimensionId = reportDimension.getDimensionId();
                            if(dimensionId.indexOf("_f") >=0){
                                dimensionId = dimensionId.replaceAll("_f","");
                            }

                            if(localCity.equals("")){
                                localCity = "'"+dimensionId+"'";
                            }else{
                                localCity = localCity+","+"'"+dimensionId+"'";
                            }
                        }
                        localCity = "("+localCity+")";
                    }
                }
            }

            if(localCity.equals("")){
                retJson.put("dataCount",dataCount);
                retJson.put("dataList",retReportDataList);
                return retJson;
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                tableName = reportIndex.getTableName(); //表名
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr(); //分母表达式2

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName +","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName +","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);

                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue+","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue+","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlGroupByStr = "";  //分组信息
            String sqlHxType = " and hx_type ='分公司'";

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    //地州
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlDzWhereStr = key+".hx_latn_id in "+localCity;

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    }
                }

                log.info("------getReportCountAllDiShiTjkb-count-sqlColumnStr: "+sqlColumnStr);
                log.info("------getReportCountAllDiShiTjkb-count-sqlTableStr: "+sqlTableStr);
                log.info("------getReportCountAllDiShiTjkb-count-sqlWhereStr: "+sqlWhereStr);
                log.info("------getReportCountAllDiShiTjkb-count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------getReportCountAllDiShiTjkb-count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------getReportCountAllDiShiTjkb-count-sqlGroupByStr: "+sqlGroupByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + " group by "+sqlGroupByStr+ sqlDzOrderByStr;*/

                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + sqlHxType +" group by "+ sqlGroupByStr + sqlDzOrderByStr;*/

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + sqlHxType + sqlDzOrderByStr;

                        log.info("+++++++++getReportCountAllDiShiTjkb-自助报表统计SQL: {}",sqlStrDate);

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllDiShiTjkb-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllDiShiTjkb-自助取数SQL方法执行时间：" + formatTime(execTime));


                    //计算合计列数据
                    retReportDataList = getCountListTjkb(retReportDataList,tableName);

                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJson.put("dataCount",dataCount);
            retJson.put("dataList",retReportDataList);
        }

        return retJson;
    }


    /**
     * 获取报表统计数据区县
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountAllQuXian(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJson = new JSONObject();

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的区县信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_area_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        /*if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }*/

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+",'"+dimensionName+"'";
                        }

                    }
                    //localCity = "'"+localCity+"'";
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                String dimensionName = quXianDimension.getDimensionName();
                                /*if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }*/

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+dimensionName+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                String dimensionName = quXianDimension.getDimensionName();
                                /*if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }*/

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+dimensionName+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }
                    }
                }
            }

            if(localCity.equals("")){
                retJson.put("dataCount",dataCount);
                retJson.put("dataList",retReportDataList);
                return retJson;
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr()==null?"":reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr()==null?"":reportIndex.getFmBdsEr(); //分母表达式2

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    //2025-10-14
                                    //替换分子表达式的日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceDateLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一天日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceDateLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式的日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds = replaceDateLast(fmBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式前一天日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceDateLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2的日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceDateLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分子表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceDateLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2的日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceDateLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceDateLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    //2025-10-14
                                    //替换分子表达式日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceMonthLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一个月
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceMonthLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds =replaceMonthLast(fmBds,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分母表达式前一个月
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceMonthLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceMonthLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式2前一个月
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceMonthLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceMonthLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一个月
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceMonthLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                //tableExpr = "("+tableExpr+") ";
                                //替换日期占位符
                                tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);
                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;
                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQxWhereStr ="";   //区县条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息
            //地州排序字段顺序
            String sqlDzOrderByStr = "";


            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //区县
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlQxWhereStr = key+".hx_area_name in "+localCity;
                        //sqlQxWhereStr = key+".hx_area_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name"+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                    }

                    //客户类型 2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                    //排序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------getReportCountAllQuXian-count-sqlColumnStr: "+sqlColumnStr);
                log.info("------getReportCountAllQuXian-count-sqlTableStr: "+sqlTableStr);
                log.info("------getReportCountAllQuXian-count-sqlWhereStr: "+sqlWhereStr);
                log.info("------getReportCountAllQuXian-count-sqlQxWhereStr: "+sqlQxWhereStr);
                log.info("------getReportCountAllQuXian-count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------getReportCountAllQuXian-count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------getReportCountAllQuXian-count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlQxWhereStr.equals("")){
                        sqlQxWhereStr =  " and ("+sqlQxWhereStr+")";
                    }


                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlQxWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr + sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        log.info("+++++++++getReportCountAllQuXian-自助取数统计SQL: {}",sqlStr);

                        /*long startTime = System.nanoTime();
                        //获取总数
                        dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStr));
                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;
                        log.info("++++++++getReportCountAllQuXian-自助取数统计总数SQL方法执行时间：" + formatTime(execTime));

                        if(dataCount >0){
                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));
                        }*/

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllQuXian-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllQuXian-自助取数SQL方法执行时间：" + formatTime(execTime));

                    }else{

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr;

                        log.info("+++++++++getReportCountAllQuXian自助报表统计SQL: {}",sqlStrDate);

                        /*long startTime = System.nanoTime();
                        //获取总数
                        dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;
                        log.info("++++++++getReportCountAllQuXian-自助取数统计总数SQL方法执行时间：" + formatTime(execTime));

                        if(dataCount >0){
                            startTime = System.nanoTime();
                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                            endTime = System.nanoTime();
                            execTime = endTime - startTime;
                            log.info("++++++++getReportCountAllQuXian-自助取数统计SQL方法执行时间：" + formatTime(execTime));
                        }*/

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllQuXian-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllQuXian-自助取数SQL方法执行时间：" + formatTime(execTime));

                    }

                    //计算合计列数据
                    retReportDataList = getCountListTj(retReportDataList);
                    /*if(retReportDataList !=null && retReportDataList.size() >0){
                        Map<String, Object> totalMap = new HashMap<>();
                        Map<String, Object> dataMap = retReportDataList.get(0);
                        Set<String> keyset = dataMap.keySet();
                        for(String key : keyset){
                            Object objValue = dataMap.get(key);

                            if(key.equals("hx_latn_name")){
                                totalMap.put(key,"合计");
                            }else if(key.equals("hx_area_name") || key.equals("x_hx5_bp_name")){
                                totalMap.put(key,"-");
                            }else if(objValue != null && objValue instanceof String && objValue.toString().indexOf("%") >0){ //百分率求平均值
                                //BigDecimal average = calculateAver(retReportDataList, key);
                                BigDecimal average = calculateSumLv(retReportDataList, key);
                                String averageStr = average+"%";
                                totalMap.put(key,averageStr);
                            }else{ //数值求合
                                BigDecimal totalSum = calculateSum(retReportDataList, key);
                                totalMap.put(key,totalSum);
                            }
                        }
                        retReportDataList.add(totalMap);
                    }*/

                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJson.put("dataCount",dataCount);
            retJson.put("dataList",retReportDataList);
        }

        return retJson;
    }


    /**
     * 获取报表统计数据区县_统计大宽表
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountAllQuXianTjkb(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJson = new JSONObject();

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的区县信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_area_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        //String dimensionName = reportDimension.getDimensionName();
                        String dimensionId = reportDimension.getDimensionId();
                        if(dimensionId.indexOf("_f") >=0){
                            dimensionId = dimensionId.replaceAll("_f","");
                        }

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionId+"'";
                        }else{
                            localCity = localCity+",'"+dimensionId+"'";
                        }

                    }
                    //localCity = "'"+localCity+"'";
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //String dimensionName = quXianDimension.getDimensionName();
                                String qxDimensionId = quXianDimension.getDimensionId();
                                if(qxDimensionId.indexOf("_f") >=0){
                                    qxDimensionId = qxDimensionId.replaceAll("_f","");
                                }
                                /*if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }*/

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+qxDimensionId+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+qxDimensionId+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //String dimensionName = quXianDimension.getDimensionName();

                                String qxDimensionId = quXianDimension.getDimensionId();
                                if(qxDimensionId.indexOf("_f") >=0){
                                    qxDimensionId = qxDimensionId.replaceAll("_f","");
                                }

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+qxDimensionId+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+qxDimensionId+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }
                    }
                }
            }

            if(localCity.equals("")){
                retJson.put("dataCount",dataCount);
                retJson.put("dataList",retReportDataList);
                return retJson;
            }


            String whereDateStr ="";
            String tableName = "";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds();
                                String fmBds = reportIndex.getFmBds();
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName+","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName+","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQxWhereStr ="";   //区县条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息
            //地州排序字段顺序
            String sqlDzOrderByStr = "";
            String sqlHxType = " and hx_type ='县分' ";


            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    //区县
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlQxWhereStr = key+".hx_area_id in "+localCity;
                        //sqlQxWhereStr = key+".hx_area_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name"+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                    }

                    //客户类型 2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //排序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";
                }

                log.info("------getReportCountAllQuXianTjkb-count-sqlColumnStr: "+sqlColumnStr);
                log.info("------getReportCountAllQuXianTjkb-count-sqlTableStr: "+sqlTableStr);
                log.info("------getReportCountAllQuXianTjkb-count-sqlWhereStr: "+sqlWhereStr);
                log.info("------getReportCountAllQuXianTjkb-count-sqlQxWhereStr: "+sqlQxWhereStr);
                log.info("------getReportCountAllQuXianTjkb-count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------getReportCountAllQuXianTjkb-count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------getReportCountAllQuXianTjkb-count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlQxWhereStr.equals("")){
                        sqlQxWhereStr =  " and ("+sqlQxWhereStr+")";
                    }

                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr + " group by "+sqlGroupByStr + sqlDzOrderByStr;*/

                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr + sqlHxType + " group by " + sqlGroupByStr + sqlDzOrderByStr;*/

                    String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr + sqlHxType + sqlDzOrderByStr;


                    log.info("+++++++++getReportCountAllQuXianTjkb自助报表统计SQL: {}",sqlStrDate);


                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllQuXianTjkb-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllQuXianTjkb-自助取数SQL方法执行时间：" + formatTime(execTime));


                    //计算合计列数据
                    retReportDataList = getCountListTjkb(retReportDataList,tableName);

                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJson.put("dataCount",dataCount);
            retJson.put("dataList",retReportDataList);
        }

        return retJson;
    }


    /**
     * 获取报表统计数据网格
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountAllWangGe(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJson = new JSONObject();

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的网格信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","x_hx5_bp_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        /*if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }*/

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionName+"'";
                        }else{
                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                            localCity = localCity+",'"+dimensionName+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        String dimensionName = wangGereportDimension.getDimensionName();
                                        /*if(localCity.equals("")){
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            localCity = localCity+","+"'"+dimensionName+"'";
                                        }*/

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+dimensionName+"'";
                                        }

                                    }
                                }
                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        String dimensionName = wangGereportDimension.getDimensionName();
                                        /*if(localCity.equals("")){
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            localCity = localCity+","+"'"+dimensionName+"'";
                                        }*/

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+dimensionName+"'";
                                        }
                                    }

                                    localCity = "("+localCity+")";
                                    //localCity = "'"+localCity+"'";
                                }
                            }
                        }else{ //查询此账号是否是网格账号
                            QueryWrapper<ReportDimension> wangGeDimensionQueryWrapper = new QueryWrapper<>();
                            wangGeDimensionQueryWrapper.eq("field","x_hx5_bp_name");
                            wangGeDimensionQueryWrapper.eq("dimension_name",deptName);
                            wangGeDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                            List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeDimensionQueryWrapper);
                            if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                    String dimensionName = wangGereportDimension.getDimensionName();
                                        /*if(localCity.equals("")){
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            localCity = localCity+","+"'"+dimensionName+"'";
                                        }*/

                                    if(localCity.equals("")){
                                        //localCity = "^"+dimensionName+"$";
                                        localCity = "'"+dimensionName+"'";
                                    }else{
                                        //localCity = localCity+"|"+"^"+dimensionName+"$";
                                        localCity = localCity+",'"+dimensionName+"'";
                                    }

                                }

                                localCity = "("+localCity+")";
                                //localCity = "'"+localCity+"'";
                            }
                        }
                    }
                }
            }

            if(localCity.equals("")){
                retJson.put("dataCount",dataCount);
                retJson.put("dataList",retReportDataList);
                return retJson;
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr()==null?"":reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr()==null?"":reportIndex.getFmBdsEr(); //分母表达式2

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    //2025-10-14
                                    //替换分子表达式的日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceDateLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一天日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceDateLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式的日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds = replaceDateLast(fmBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式前一天日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceDateLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2的日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceDateLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分子表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceDateLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2的日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceDateLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceDateLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    //2025-10-14
                                    //替换分子表达式日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceMonthLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一个月
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceMonthLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds =replaceMonthLast(fmBds,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分母表达式前一个月
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceMonthLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceMonthLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式2前一个月
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceMonthLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceMonthLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一个月
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceMonthLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                //tableExpr = "("+tableExpr+") ";
                                //替换日期占位符
                                tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlWgWhereStr ="";   //网格条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr =""; //按地市排序

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //网格
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlWgWhereStr = key+".x_hx5_bp_name in "+localCity;
                        //sqlWgWhereStr = key+".x_hx5_bp_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }

                    }

                    //客户类型 2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                    //排序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------getReportCountAllWangGe-count-sqlColumnStr: "+sqlColumnStr);
                log.info("------getReportCountAllWangGe-count-sqlTableStr: "+sqlTableStr);
                log.info("------getReportCountAllWangGe-count-sqlWhereStr: "+sqlWhereStr);
                log.info("------getReportCountAllWangGe-count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------getReportCountAllWangGe-count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------getReportCountAllWangGe-count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------getReportCountAllWangGe-count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlWgWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr +sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        log.info("+++++++++getReportCountAllWangGe-自助取数统计SQL: {}",sqlStr);

                        /*long startTime = System.nanoTime();
                        //获取总数
                        dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStr));
                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;
                        log.info("++++++++getReportCountAllWangGe-自助取数统计总数SQL方法执行时间：" + formatTime(execTime));

                        if(dataCount >0){
                            startTime = System.nanoTime();
                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));
                            endTime = System.nanoTime();
                            execTime = endTime - startTime;
                            log.info("++++++++getReportCountAllWangGe-自助取数统计SQL方法执行时间：" + formatTime(execTime));
                        }*/

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStr));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllWangGe-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllWangGe-自助取数SQL方法执行时间：" + formatTime(execTime));

                    }else{

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlWgWhereStr + " and " +whereDateStr;

                        log.info("+++++++++getReportCountAllWangGe-自助取数统计SQL: {}",sqlStrDate);

                        /*long startTime = System.nanoTime();
                        //获取总数
                        dataCount = PageHelper.count(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;
                        log.info("++++++++getReportCountAllWangGe-自助取数统计总数SQL方法执行时间：" + formatTime(execTime));

                        if(dataCount >0){
                            startTime = System.nanoTime();
                            //查询结果
                            retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                    .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));
                            endTime = System.nanoTime();
                            execTime = endTime - startTime;
                            log.info("++++++++getReportCountAllWangGe-自助取数统计SQL方法执行时间：" + formatTime(execTime));
                        }*/

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllWangGe-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllWangGe-自助取数SQL方法执行时间：" + formatTime(execTime));

                    }

                    //计算合计列数据
                    retReportDataList = getCountListTj(retReportDataList);
                    /*if(retReportDataList !=null && retReportDataList.size() >0){
                        Map<String, Object> totalMap = new HashMap<>();
                        Map<String, Object> dataMap = retReportDataList.get(0);
                        Set<String> keyset = dataMap.keySet();
                        for(String key : keyset){
                            Object objValue = dataMap.get(key);

                            if(key.equals("hx_latn_name")){
                                totalMap.put(key,"合计");
                            }else if(key.equals("hx_area_name") || key.equals("x_hx5_bp_name")){
                                totalMap.put(key,"-");
                            }else if(objValue != null && objValue instanceof String && objValue.toString().indexOf("%") >0){ //百分率求平均值
                                //BigDecimal average = calculateAver(retReportDataList, key);
                                BigDecimal average = calculateSumLv(retReportDataList, key);
                                String averageStr = average+"%";
                                totalMap.put(key,averageStr);
                            }else{ //数值求合
                                BigDecimal totalSum = calculateSum(retReportDataList, key);
                                totalMap.put(key,totalSum);
                            }
                        }
                        retReportDataList.add(totalMap);
                    }*/
                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJson.put("dataCount",dataCount);
            retJson.put("dataList",retReportDataList);
        }

        return retJson;
    }


    /**
     * 获取报表统计数据网格
     * @param reportParam
     * @param pageDomain
     * @return
     */
    private JSONObject getReportCountAllWangGeTjkb(ReportParam reportParam, PageDomain pageDomain){
        JSONObject retJson = new JSONObject();

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的网格信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","x_hx5_bp_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        //String dimensionName = reportDimension.getDimensionName();
                        String dimensionId = reportDimension.getDimensionId();
                        if(dimensionId.indexOf("_f") >=0){
                            dimensionId = dimensionId.replaceAll("_f","");
                        }
                        /*if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }*/

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionId+"'";
                        }else{
                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                            localCity = localCity+",'"+dimensionId+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        //String dimensionName = wangGereportDimension.getDimensionName();
                                        String wgDimensionId = wangGereportDimension.getDimensionId();
                                        if(wgDimensionId.indexOf("_f") >=0){
                                            wgDimensionId = wgDimensionId.replaceAll("_f","");
                                        }

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+wgDimensionId+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+wgDimensionId+"'";
                                        }

                                    }
                                }
                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        //String dimensionName = wangGereportDimension.getDimensionName();
                                        String wgDimensionId = wangGereportDimension.getDimensionId();
                                        if(wgDimensionId.indexOf("_f") >=0){
                                            wgDimensionId = wgDimensionId.replaceAll("_f","");
                                        }

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+wgDimensionId+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+wgDimensionId+"'";
                                        }
                                    }

                                    localCity = "("+localCity+")";
                                    //localCity = "'"+localCity+"'";
                                }
                            }
                        }else{ //查询此账号是否是网格账号
                            QueryWrapper<ReportDimension> wangGeDimensionQueryWrapper = new QueryWrapper<>();
                            wangGeDimensionQueryWrapper.eq("field","x_hx5_bp_name");
                            wangGeDimensionQueryWrapper.eq("dimension_name",deptName);
                            wangGeDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                            List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeDimensionQueryWrapper);
                            if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                    //String dimensionName = wangGereportDimension.getDimensionName();
                                    String wgDimensionId = wangGereportDimension.getDimensionId();
                                    if(wgDimensionId.indexOf("_f") >=0){
                                        wgDimensionId = wgDimensionId.replaceAll("_f","");
                                    }

                                    if(localCity.equals("")){
                                        //localCity = "^"+dimensionName+"$";
                                        localCity = "'"+wgDimensionId+"'";
                                    }else{
                                        //localCity = localCity+"|"+"^"+dimensionName+"$";
                                        localCity = localCity+",'"+wgDimensionId+"'";
                                    }

                                }

                                localCity = "("+localCity+")";
                                //localCity = "'"+localCity+"'";
                            }
                        }
                    }
                }
            }

            if(localCity.equals("")){
                retJson.put("dataCount",dataCount);
                retJson.put("dataList",retReportDataList);
                return retJson;
            }


            String whereDateStr ="";
            String tableName ="";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds();
                                String fmBds = reportIndex.getFmBds();
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlWgWhereStr ="";   //网格条件信息
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr =""; //按地市排序
            String sqlHxType =" and hx_type ='网格' "; //按地市排序

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }

                    //网格
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlWgWhereStr = key+".x_hx5_bp_id in "+localCity;
                        //sqlWgWhereStr = key+".x_hx5_bp_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }

                    }

                    //排序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------getReportCountAllWangGeTjkb-count-sqlColumnStr: "+sqlColumnStr);
                log.info("------getReportCountAllWangGeTjkb-count-sqlTableStr: "+sqlTableStr);
                log.info("------getReportCountAllWangGeTjkb-count-sqlWhereStr: "+sqlWhereStr);
                log.info("------getReportCountAllWangGeTjkb-count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------getReportCountAllWangGeTjkb-count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------getReportCountAllWangGeTjkb-count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlWgWhereStr + " and " +whereDateStr + sqlHxType + " group by " + sqlGroupByStr + sqlDzOrderByStr;*/

                     String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlWgWhereStr + " and " +whereDateStr + sqlHxType + sqlDzOrderByStr;

                        log.info("+++++++++getReportCountAllWangGeTjkb-自助取数统计SQL: {}",sqlStrDate);

                        long startTime = System.nanoTime();

                        //查询结果
                        retReportDataList = PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit(),true)
                                .doSelectPage(() -> repConnSecondTableService.getReportDataListMap(sqlStrDate));

                        PageInfo<Map<String,Object>> pageInfo = new PageInfo<>(retReportDataList);
                        dataCount = pageInfo.getTotal();
                        log.info("+++++++++getReportCountAllWangGeTjkb-总记录数: {}",dataCount);

                        long endTime = System.nanoTime();
                        long execTime = endTime - startTime;

                        log.info("++++++++getReportCountAllWangGeTjkb-自助取数SQL方法执行时间：" + formatTime(execTime));

                        //计算合计列数据
                        retReportDataList = getCountListTjkb(retReportDataList,tableName);
                }
            }

            //log.info("=======自助报表统计查询结果：{}",retReportDataList);
            if(retReportDataList != null && retReportDataList.size() >0){
                // 转换处理
                retReportDataList = convertListMapKeysToCamelCase(retReportDataList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            retJson.put("dataCount",dataCount);
            retJson.put("dataList",retReportDataList);
        }

        return retJson;
    }


    /**
     * 获取报表统计数据 导出
     * @param reportParam
     * @return
     */
    private List<Map<String, Object>> getReportCountAllSelectExport(ReportParam reportParam,boolean isTjkb){
        List<Map<String, Object>> retReportDataList = new ArrayList<>();
        try {
            String selArea ="";
            String dimensionInfo = reportParam.getDimensionInfo();
            JSONArray dimensionJsonArray = JSONArray.parseArray(dimensionInfo);
            if(dimensionJsonArray != null && dimensionJsonArray.size() >0){
                for(Object dimensionJsonObject: dimensionJsonArray){
                    JSONObject dimensionJson = (JSONObject)dimensionJsonObject;
                    selArea = dimensionJson.getString("id");
                }
            }

            //2025-11-25
            if(isTjkb){ //统计大宽表
                //全区
                if(selArea.equals("selAllQuanQu")){
                    retReportDataList = getReportCountAllQuanQuExportTjkb(reportParam);
                }else if(selArea.equals("selAllDiShi")){ //地市
                    retReportDataList = getReportCountAllDiShiExportTjkb(reportParam);
                }else if(selArea.equals("selAllQuXian")){ //区县
                    retReportDataList = getReportCountAllQuXianExportTjkb(reportParam);
                }else if(selArea.equals("selAllWangGe")){ //网格
                    retReportDataList = getReportCountAllWangGeExportTjkb(reportParam);
                }
            }else { //非统计大宽表
                //全区
                if(selArea.equals("selAllQuanQu")){
                    retReportDataList = getReportCountAllQuanQuExport(reportParam);
                }else if(selArea.equals("selAllDiShi")){ //地市
                    retReportDataList = getReportCountAllDiShiExport(reportParam);
                }else if(selArea.equals("selAllQuXian")){ //区县
                    retReportDataList = getReportCountAllQuXianExport(reportParam);
                }else if(selArea.equals("selAllWangGe")){ //网格
                    retReportDataList = getReportCountAllWangGeExport(reportParam);
                }
            }



        }catch (Exception e){
            e.printStackTrace();
        }
        return retReportDataList;

    }

    /**
     * 获取全区统计数据导出
     * @param reportParam
     * @return
     */
    private List<Map<String, Object>>  getReportCountAllQuanQuExport(ReportParam reportParam){
        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的地市信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_latn_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        for(ReportDimension reportDimension:reportDimensionList){
                            String dimensionName = reportDimension.getDimensionName();
                            if(localCity.equals("")){
                                localCity = "'"+dimensionName+"'";
                            }else{
                                localCity = localCity+","+"'"+dimensionName+"'";
                            }
                        }
                        localCity = "("+localCity+")";
                    }
                }
            }

            if(localCity.equals("")){

                return retReportDataList;
            }


            String whereDateStr ="";
            String sumColumnStr ="";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                //tableExpr = "("+tableExpr+") ";
                                //替换日期占位符
                                tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);
                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;
                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }

                                if(sumColumnStr.equals("")){
                                    sumColumnStr = " SUM ("+tableExpr+") AS "+columnName;
                                }else{
                                    sumColumnStr = sumColumnStr+","+ " SUM ("+tableExpr+") AS "+columnName;
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlQqColumnStr =" ,'全区' sel_all_quan_qu";   //全区字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQqWhereStr ="";   //全区条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = " group by '全区' ";  //分组信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }

                    //查询条件
                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }

                    //客户类型  2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                    //16个地市
                    sqlQqWhereStr = " and "+key+".hx_latn_name in "+localCity;

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlQqColumnStr: "+sqlQqColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlQqWhereStr: "+sqlQqWhereStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){

                    String sqlStr = "select " + sqlColumnStr + sqlQqColumnStr+" from " + sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlQqWhereStr + sqlCustTypeStr + sqlRoleTypeStr +sqlGroupByStr;

                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        sqlStr = sqlStr.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStr);

                    }else{

                        String sqlStrDate = "select "+sumColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlQqWhereStr+ " and " +whereDateStr +sqlGroupByStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStrDate);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return retReportDataList;
    }


    /**
     * 获取全区统计数据导出_五项集约
     * @param reportParam
     * @return
     */
    private List<Map<String, Object>>  getReportCountAllQuanQuExportTjkb(ReportParam reportParam){
        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                localCity ="('9999')";
            }

            if(localCity.equals("")){

                return retReportDataList;
            }


            String whereDateStr ="";
            String sumColumnStr ="";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);
                                    columnMap.put(tableName, columnName);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;
                                    tableNameMapValue = tableNameMapValue+","+ columnName;
                                    columnMap.put(tableName, tableNameMapValue);
                                }

                                if(sumColumnStr.equals("")){
                                    sumColumnStr =  columnName;
                                }else{
                                    sumColumnStr = sumColumnStr+"," +columnName;
                                }
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlQqColumnStr =" ,'全区' sel_all_quan_qu";   //全区字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQqWhereStr ="";   //全区条件信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    //16个地市
                    sqlQqWhereStr = " and "+key+".hx_latn_id in "+localCity;

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlQqColumnStr: "+sqlQqColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlQqWhereStr: "+sqlQqWhereStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){

                        String sqlStrDate = "select "+sumColumnStr+ sqlQqColumnStr +" from "+sqlTableStr +" where 1=1 "
                            + sqlQqWhereStr+ " and " +whereDateStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStrDate);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return retReportDataList;
    }


    /**
     * 获取报表统计数据地市导出
     * @param reportParam
     * @return
     */
    private List<Map<String,Object>> getReportCountAllDiShiExport(ReportParam reportParam){
        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的地市信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_latn_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        for(ReportDimension reportDimension:reportDimensionList){
                            String dimensionName = reportDimension.getDimensionName();
                            if(localCity.equals("")){
                                localCity = "'"+dimensionName+"'";
                            }else{
                                localCity = localCity+","+"'"+dimensionName+"'";
                            }
                        }
                        localCity = "("+localCity+")";
                    }
                }
            }

            if(localCity.equals("")){
                return retReportDataList;
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr()==null?"":reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr()==null?"":reportIndex.getFmBdsEr(); //分母表达式2

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    //2025-10-14
                                    //替换分子表达式的日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceDateLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一天日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceDateLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式的日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds = replaceDateLast(fmBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式前一天日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceDateLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2的日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceDateLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分子表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceDateLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2的日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceDateLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceDateLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    //2025-10-14
                                    //替换分子表达式日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceMonthLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一个月
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceMonthLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds =replaceMonthLast(fmBds,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分母表达式前一个月
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceMonthLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceMonthLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式2前一个月
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceMonthLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceMonthLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一个月
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceMonthLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                //tableExpr = "("+tableExpr+") ";
                                //替换日期占位符
                                tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }




            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlDzWhereStr = key+".hx_latn_name in "+localCity;

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    }

                    //客户类型  2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                }

                log.info("------export-sqlColumnStr: "+sqlColumnStr);
                log.info("------export-sqlTableStr: "+sqlTableStr);
                log.info("------export-sqlWhereStr: "+sqlWhereStr);
                log.info("------export-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------export-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------export-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------export-sqlGroupByStr: "+sqlGroupByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr + sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){
                        sqlStr = sqlStr.toUpperCase();
                        log.info("+++++++++自助取数导出报表统计SQL: {}",sqlStr);
                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStr);

                    }else{

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + sqlDzOrderByStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助取数导出报表统计SQL: {}",sqlStrDate);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);
                    }

                    //计算合计列数据
                    retReportDataList = getCountListTj(retReportDataList);
                    /*if(retReportDataList !=null && retReportDataList.size() >0){
                        Map<String, Object> totalMap = new HashMap<>();
                        Map<String, Object> dataMap = retReportDataList.get(0);
                        Set<String> keyset = dataMap.keySet();
                        for(String key : keyset){
                            Object objValue = dataMap.get(key);

                            if(key.equals("HX_LATN_NAME") || key.equals("HX_AREA_NAME") || key.equals("HX_REGION_NAME") || key.equals("X_HX5_BP_NAME")){
                                totalMap.put(key,"合计");
                            }else if(objValue != null && objValue instanceof String && objValue.toString().indexOf("%") >0){ //百分率求平均值
                                //BigDecimal average = calculateAver(retReportDataList, key);
                                BigDecimal average = calculateSumLv(retReportDataList, key);
                                String averageStr = average+"%";
                                totalMap.put(key,averageStr);
                            }else{ //数值求合
                                BigDecimal totalSum = calculateSum(retReportDataList, key);
                                totalMap.put(key,totalSum);
                            }
                        }
                        retReportDataList.add(totalMap);
                    }*/

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }


    /**
     * 获取报表统计数据地市导出_五项集约
     * @param reportParam
     * @return
     */
    private List<Map<String,Object>> getReportCountAllDiShiExportTjkb(ReportParam reportParam){
        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的地市信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_latn_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        //String dimensionName = reportDimension.getDimensionName();
                        String dimensionId = reportDimension.getDimensionId();
                        if(dimensionId.indexOf("_f") >=0){
                            dimensionId = dimensionId.replaceAll("_f","");
                        }

                        if(localCity.equals("")){
                            localCity = "'"+dimensionId+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionId+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        for(ReportDimension reportDimension:reportDimensionList){
                            //String dimensionName = reportDimension.getDimensionName();
                            String dimensionId = reportDimension.getDimensionId();
                            if(dimensionId.indexOf("_f") >=0){
                                dimensionId = dimensionId.replaceAll("_f","");
                            }

                            if(localCity.equals("")){
                                localCity = "'"+dimensionId+"'";
                            }else{
                                localCity = localCity+","+"'"+dimensionId+"'";
                            }
                        }
                        localCity = "("+localCity+")";
                    }
                }
            }

            if(localCity.equals("")){
                return retReportDataList;
            }


            String whereDateStr ="";
            String tableName = "";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }


                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue+","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue+","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlGroupByStr = "";  //分组信息
            String sqlHxType =" and hx_type ='分公司' ";

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }

                    //地州
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlDzWhereStr = key+".hx_latn_id in "+localCity;

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    }

                }

                log.info("------export-sqlColumnStr: "+sqlColumnStr);
                log.info("------export-sqlTableStr: "+sqlTableStr);
                log.info("------export-sqlWhereStr: "+sqlWhereStr);
                log.info("------export-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------export-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------export-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------export-sqlHxType: "+sqlHxType);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }
                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + sqlHxType +" group by "+ sqlGroupByStr + sqlDzOrderByStr;*/

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + sqlHxType + sqlDzOrderByStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助取数导出报表统计SQL: {}",sqlStrDate);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);


                        //计算合计列数据
                        retReportDataList = getCountListTjkb(retReportDataList,tableName);

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }


    /**
     * 获取报表统计数据区县导出
     * @param reportParam
     * @return
     */
    private List<Map<String,Object>> getReportCountAllQuXianExport(ReportParam reportParam){
        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的区县信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_area_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        /*if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }*/

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionName+"'";
                        }else{
                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                            localCity = localCity+",'"+dimensionName+"'";
                        }

                    }
                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                String dimensionName = quXianDimension.getDimensionName();
                                /*if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }*/

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+dimensionName+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                String dimensionName = quXianDimension.getDimensionName();
                                /*if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }*/

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+dimensionName+"'";
                                }

                            }
                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }
                    }
                }
            }

            if(localCity.equals("")){
                return retReportDataList;
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr()==null?"":reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr()==null?"":reportIndex.getFmBdsEr(); //分母表达式2

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    //2025-10-14
                                    //替换分子表达式的日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceDateLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一天日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceDateLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式的日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds = replaceDateLast(fmBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式前一天日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceDateLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2的日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceDateLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分子表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceDateLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2的日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceDateLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceDateLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    //2025-10-14
                                    //替换分子表达式日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceMonthLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一个月
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceMonthLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds =replaceMonthLast(fmBds,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分母表达式前一个月
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceMonthLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceMonthLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式2前一个月
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceMonthLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceMonthLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一个月
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceMonthLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                //tableExpr = "("+tableExpr+") ";
                                //替换日期占位符
                                tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQxWhereStr ="";   //区县条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr =""; //地市排序

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //区县
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlQxWhereStr = key+".hx_area_name in "+localCity;
                        //sqlQxWhereStr = key+".hx_area_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                    }

                    //客户类型 2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlQxWhereStr: "+sqlQxWhereStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlQxWhereStr.equals("")){
                        sqlQxWhereStr =  " and ("+sqlQxWhereStr+")";
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlQxWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr+sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){
                        sqlStr = sqlStr.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStr);
                    }else{

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr+sqlDzOrderByStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStrDate);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);
                    }

                    //计算合计列数据
                    retReportDataList = getCountListTj(retReportDataList);
                    /*if(retReportDataList !=null && retReportDataList.size() >0){
                        Map<String, Object> totalMap = new HashMap<>();
                        Map<String, Object> dataMap = retReportDataList.get(0);
                        Set<String> keyset = dataMap.keySet();
                        for(String key : keyset){
                            Object objValue = dataMap.get(key);

                            if(key.equals("HX_LATN_NAME")){
                                totalMap.put(key,"合计");
                            }else if(key.equals("HX_AREA_NAME") || key.equals("X_HX5_BP_NAME")){
                                totalMap.put(key,"-");
                            }else if(objValue != null && objValue instanceof String && objValue.toString().indexOf("%") >0){ //百分率求平均值
                                //BigDecimal average = calculateAver(retReportDataList, key);
                                BigDecimal average = calculateSumLv(retReportDataList, key);
                                String averageStr = average+"%";
                                totalMap.put(key,averageStr);
                            }else{ //数值求合
                                BigDecimal totalSum = calculateSum(retReportDataList, key);
                                totalMap.put(key,totalSum);
                            }
                        }
                        retReportDataList.add(totalMap);
                    }*/

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }


    /**
     * 获取报表统计数据区县导出_五项集约
     * @param reportParam
     * @return
     */
    private List<Map<String,Object>> getReportCountAllQuXianExportTjkb(ReportParam reportParam){
        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的区县信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_area_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        //String dimensionName = reportDimension.getDimensionName();
                        String dimensionId = reportDimension.getDimensionId();
                        if(dimensionId.indexOf("_f") >=0){
                            dimensionId = dimensionId.replaceAll("_f","");
                        }

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionId+"'";
                        }else{
                            localCity = localCity+",'"+dimensionId+"'";
                        }

                    }
                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //String dimensionName = quXianDimension.getDimensionName();
                                String qxDimensionId = quXianDimension.getDimensionId();
                                if(qxDimensionId.indexOf("_f") >=0){
                                    qxDimensionId = qxDimensionId.replaceAll("_f","");
                                }
                                /*if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }*/

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+qxDimensionId+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+qxDimensionId+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //String dimensionName = quXianDimension.getDimensionName();

                                String qxDimensionId = quXianDimension.getDimensionId();
                                if(qxDimensionId.indexOf("_f") >=0){
                                    qxDimensionId = qxDimensionId.replaceAll("_f","");
                                }

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+qxDimensionId+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+qxDimensionId+"'";
                                }

                            }
                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }
                    }
                }
            }

            if(localCity.equals("")){
                return retReportDataList;
            }


            String whereDateStr ="";
            String tableName = "";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds();
                                String fmBds = reportIndex.getFmBds();
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }


                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName+","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName+","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQxWhereStr ="";   //区县条件信息
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr =""; //地市排序
            String sqlHxTypeStr =" and hx_type='县分' ";

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //区县
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlQxWhereStr = key+".hx_area_id in "+localCity;
                        //sqlQxWhereStr = key+".hx_area_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlQxWhereStr: "+sqlQxWhereStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------count-sqlHxTypeStr: "+sqlHxTypeStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlQxWhereStr.equals("")){
                        sqlQxWhereStr =  " and ("+sqlQxWhereStr+")";
                    }
                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr + sqlHxTypeStr +" group by " + sqlGroupByStr + sqlDzOrderByStr;*/

                    String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr + sqlHxTypeStr + sqlDzOrderByStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStrDate);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);


                    //计算合计列数据
                    retReportDataList = getCountListTjkb(retReportDataList,tableName);

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }


    /**
     * 获取报表统计数据网格 导出
     * @param reportParam
     * @return
     */
    private List<Map<String,Object>> getReportCountAllWangGeExport(ReportParam reportParam){

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的网格信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","x_hx5_bp_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        /*if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }*/

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionName+"'";
                        }else{
                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                            localCity = localCity+",'"+dimensionName+"'";
                        }

                    }
                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){ //地市账号
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        String dimensionName = wangGereportDimension.getDimensionName();
                                        /*if(localCity.equals("")){
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            localCity = localCity+","+"'"+dimensionName+"'";
                                        }*/

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+dimensionName+"'";
                                        }

                                    }
                                }
                            }

                            localCity = "("+localCity+")";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        String dimensionName = wangGereportDimension.getDimensionName();
                                        /*if(localCity.equals("")){
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            localCity = localCity+","+"'"+dimensionName+"'";
                                        }*/

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+dimensionName+"'";
                                        }
                                    }

                                    localCity = "("+localCity+")";
                                    //localCity = "'"+localCity+"'";
                                }
                            }

                        }else{ //网格

                            QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                            wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                            wangGeQueryWrapper.eq("dimension_name",deptName);
                            wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                            List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                            if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                    String dimensionName = wangGereportDimension.getDimensionName();

                                    if(localCity.equals("")){
                                        //localCity = "^"+dimensionName+"$";
                                        localCity = "'"+dimensionName+"'";
                                    }else{
                                        //localCity = localCity+"|"+"^"+dimensionName+"$";
                                        localCity = localCity+",'"+dimensionName+"'";
                                    }
                                }

                                localCity = "("+localCity+")";
                                //localCity = "'"+localCity+"'";
                            }
                        }
                    }
                }
            }

            if(localCity.equals("")){
                return retReportDataList;
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr()==null?"":reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr()==null?"":reportIndex.getFmBdsEr(); //分母表达式2

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    //2025-10-14
                                    //替换分子表达式的日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceDateLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一天日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceDateLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式的日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds = replaceDateLast(fmBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式前一天日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceDateLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2的日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceDateLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分子表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceDateLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2的日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceDateLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一天日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceDateLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    //2025-10-14
                                    //替换分子表达式日期
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date}") >=0){
                                        fzBds = replaceMonthLast(fzBds,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式前一个月
                                    if(StringUtil.isNotEmpty(fzBds) && fzBds.indexOf("{repl_date-1}") >=0){
                                        fzBds = replaceMonthLastTwo(fzBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式日期
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date}") >=0){
                                        fmBds =replaceMonthLast(fmBds,"{repl_date}",tableName,startDate,endDate);

                                    }

                                    //替换分母表达式前一个月
                                    if(StringUtil.isNotEmpty(fmBds) && fmBds.indexOf("{repl_date-1}") >=0){
                                        fmBds = replaceMonthLastTwo(fmBds,"{repl_date-1}",startDate,endDate);
                                    }

                                    //2025-10-14
                                    //替换分子表达式2日期
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date}") >=0){
                                        fzBdsEr = replaceMonthLast(fzBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分子表达式2前一个月
                                    if(StringUtil.isNotEmpty(fzBdsEr) && fzBdsEr.indexOf("{repl_date-1}") >=0){
                                        fzBdsEr = replaceMonthLastTwo(fzBdsEr,"{repl_date-1}",startDate,endDate);
                                    }

                                    //替换分母表达式2日期
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date}") >=0){
                                        fmBdsEr = replaceMonthLast(fmBdsEr,"{repl_date}",tableName,startDate,endDate);
                                    }

                                    //替换分母表达式2前一个月
                                    if(StringUtil.isNotEmpty(fmBdsEr) && fmBdsEr.indexOf("{repl_date-1}") >=0){
                                        fmBdsEr = replaceMonthLastTwo(fmBdsEr,"{repl_date-1}",startDate,endDate);
                                    }


                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                //tableExpr = "("+tableExpr+") ";
                                //替换日期占位符
                                tableExpr = replDateStr(dateType,tableName,tableExpr,startDate,endDate);

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlWgWhereStr ="";   //网格条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr =""; //地市排序

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //区县
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlWgWhereStr = key+".x_hx5_bp_name in "+localCity;
                        //sqlWgWhereStr = key+".x_hx5_bp_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";

                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }

                    }

                    //客户类型 2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                    String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlWgWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr +sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        sqlStr = sqlStr.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStr);
                    }else{

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlWgWhereStr + " and " +whereDateStr + sqlDzOrderByStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStrDate);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);
                    }

                    //计算合计列数据
                    retReportDataList = getCountListTj(retReportDataList);
                    /*if(retReportDataList !=null && retReportDataList.size() >0){
                        Map<String, Object> totalMap = new HashMap<>();
                        Map<String, Object> dataMap = retReportDataList.get(0);
                        Set<String> keyset = dataMap.keySet();
                        for(String key : keyset){
                            Object objValue = dataMap.get(key);

                            if(key.equals("HX_LATN_NAME")){
                                totalMap.put(key,"合计");
                            }else if(key.equals("HX_AREA_NAME") || key.equals("X_HX5_BP_NAME")){
                                totalMap.put(key,"-");
                            }else if(objValue != null && objValue instanceof String && objValue.toString().indexOf("%") >0){ //百分率求平均值
                                //BigDecimal average = calculateAver(retReportDataList, key);
                                BigDecimal average = calculateSumLv(retReportDataList, key);
                                String averageStr = average+"%";
                                totalMap.put(key,averageStr);
                            }else{ //数值求合
                                BigDecimal totalSum = calculateSum(retReportDataList, key);
                                totalMap.put(key,totalSum);
                            }
                        }
                        retReportDataList.add(totalMap);
                    }*/

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }


    /**
     * 获取报表统计数据网格 导出 _五项集约
     * @param reportParam
     * @return
     */
    private List<Map<String,Object>> getReportCountAllWangGeExportTjkb(ReportParam reportParam){

        List<Map<String,Object>> retReportDataList = new ArrayList<>();
        long dataCount = 0;
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的网格信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","x_hx5_bp_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionId = reportDimension.getDimensionId();
                        if(dimensionId.indexOf("_f") >=0){
                            dimensionId = dimensionId.replaceAll("_f","");
                        }

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionId+"'";
                        }else{
                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                            localCity = localCity+",'"+dimensionId+"'";
                        }

                    }
                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){ //地市账号
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        //String dimensionName = wangGereportDimension.getDimensionName();
                                        String wgDimensionId = wangGereportDimension.getDimensionId();
                                        if(wgDimensionId.indexOf("_f") >=0){
                                            wgDimensionId = wgDimensionId.replaceAll("_f","");
                                        }

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+wgDimensionId+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+wgDimensionId+"'";
                                        }

                                    }
                                }
                            }

                            localCity = "("+localCity+")";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        //String dimensionName = wangGereportDimension.getDimensionName();
                                        String wgDimensionId = wangGereportDimension.getDimensionId();
                                        if(wgDimensionId.indexOf("_f") >=0){
                                            wgDimensionId = wgDimensionId.replaceAll("_f","");
                                        }

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+wgDimensionId+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+wgDimensionId+"'";
                                        }
                                    }

                                    localCity = "("+localCity+")";
                                    //localCity = "'"+localCity+"'";
                                }
                            }

                        }else{ //查询此账号是否是网格账号

                            QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                            wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                            wangGeQueryWrapper.eq("dimension_name",deptName);
                            wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                            List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                            if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                    //String dimensionName = wangGereportDimension.getDimensionName();
                                    String wgDimensionId = wangGereportDimension.getDimensionId();
                                    if(wgDimensionId.indexOf("_f") >=0){
                                        wgDimensionId = wgDimensionId.replaceAll("_f","");
                                    }

                                    if(localCity.equals("")){
                                        //localCity = "^"+dimensionName+"$";
                                        localCity = "'"+wgDimensionId+"'";
                                    }else{
                                        //localCity = localCity+"|"+"^"+dimensionName+"$";
                                        localCity = localCity+",'"+wgDimensionId+"'";
                                    }
                                }

                                localCity = "("+localCity+")";
                                //localCity = "'"+localCity+"'";
                            }
                        }
                    }
                }
            }

            if(localCity.equals("")){
                return retReportDataList;
            }


            String whereDateStr ="";
            String tableName = "";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){
                    //数据周期
                    String dateType = reportParam.getDateType();
                    String startDate = reportParam.getStartDate();
                    String endDate = reportParam.getEndDate();
                    startDate = startDate.replaceAll("-","");
                    endDate = endDate.replaceAll("-","");

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds();
                                String fmBds = reportIndex.getFmBds();
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                //2025-8-11
                                if(dateType.equals("day")){//日

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }else{ //月

                                    if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";

                                    }else if(StringUtil.isNotEmpty(startDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+startDate +"' ";

                                    }else if(StringUtil.isNotEmpty(endDate)){
                                        whereDateStr = " "+tableName+".op_date ='"+endDate +"' ";
                                    }else{
                                        String strDate = DateTimeUtil.getLastMonth("yyyyMM");
                                        whereDateStr = " "+tableName+".op_date ='"+strDate +"' ";
                                    }

                                }

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlWgWhereStr ="";   //网格条件信息
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr =""; //地市排序
            String sqlHxTypeStr =" and hx_type='网格' ";

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    //区县
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlWgWhereStr = key+".x_hx5_bp_id in "+localCity;
                        //sqlWgWhereStr = key+".x_hx5_bp_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";

                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }

                    }


                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------count-sqlHxTypeStr: "+sqlHxTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                        /*String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlWgWhereStr + " and " +whereDateStr + sqlHxTypeStr + " group by " + sqlGroupByStr + sqlDzOrderByStr;*/

                        String sqlStrDate = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlWgWhereStr + " and " +whereDateStr + sqlHxTypeStr + sqlDzOrderByStr;

                        sqlStrDate = sqlStrDate.toUpperCase();
                        log.info("+++++++++自助报表统计SQL: {}",sqlStrDate);

                        //查询结果
                        retReportDataList = repConnSecondTableService.getReportDataListMap(sqlStrDate);


                    //计算合计列数据
                    retReportDataList = getCountListTjkb(retReportDataList,tableName);

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return retReportDataList;
    }


    /**
     * 获取报表统计数据 导出
     * @param reportParam
     * @return
     */
    private String getReportCountAllSelectSql(ReportParam reportParam,boolean isWxjy){
        String sqlStr ="";
        try {
            String selArea ="";
            String dimensionInfo = reportParam.getDimensionInfo();
            JSONArray dimensionJsonArray = JSONArray.parseArray(dimensionInfo);
            if(dimensionJsonArray != null && dimensionJsonArray.size() >0){
                for(Object dimensionJsonObject: dimensionJsonArray){
                    JSONObject dimensionJson = (JSONObject)dimensionJsonObject;
                    selArea = dimensionJson.getString("id");
                }
            }

            //2025-11-25
            if(isWxjy){ //大宽表统计
                //全区
                if(selArea.equals("selAllQuanQu")){
                    sqlStr = getReportCountSqlQuanQuTjkb(reportParam);
                }else if(selArea.equals("selAllDiShi")){ //地市
                    sqlStr = getReportCountSqlDiShiTjkb(reportParam);
                }else if(selArea.equals("selAllQuXian")){ //区县
                    sqlStr = getReportCountSqlQuXianWxjy(reportParam);
                }else if(selArea.equals("selAllWangGe")){ //网格
                    sqlStr = getReportCountSqlWangGeWxjy(reportParam);
                }
            }else{ //非五项集约
                //全区
                if(selArea.equals("selAllQuanQu")){
                    sqlStr = getReportCountSqlQuanQu(reportParam);
                }else if(selArea.equals("selAllDiShi")){ //地市
                    sqlStr = getReportCountSqlDiShi(reportParam);
                }else if(selArea.equals("selAllQuXian")){ //区县
                    sqlStr = getReportCountSqlQuXian(reportParam);
                }else if(selArea.equals("selAllWangGe")){ //网格
                    sqlStr = getReportCountSqlWangGe(reportParam);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return sqlStr;

    }


    /**
     * 获取自动化报表统计SQL语句 全区
     * @param reportParam
     * @return
     */
    private String getReportCountSqlQuanQu(ReportParam reportParam){
        String sqlStr = "";

        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的地市信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_latn_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        for(ReportDimension reportDimension:reportDimensionList){
                            String dimensionName = reportDimension.getDimensionName();
                            if(localCity.equals("")){
                                localCity = "'"+dimensionName+"'";
                            }else{
                                localCity = localCity+","+"'"+dimensionName+"'";
                            }
                        }
                        localCity = "("+localCity+")";
                    }
                }
            }

            if(localCity.equals("")){
                return "";
            }


            String sumColumnStr ="";
            String whereDateStr ="";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                //tableExpr = "("+tableExpr+") ";

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);
                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;
                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }

                                if(sumColumnStr.equals("")){
                                    sumColumnStr = " SUM ("+tableExpr+") AS "+columnName;
                                }else{
                                    sumColumnStr = sumColumnStr+","+ " SUM ("+tableExpr+") AS "+columnName;
                                }

                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlQqColumnStr =" ,'全区' sel_all_quan_qu";   //全区字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQqWhereStr ="";   //全区条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = " group by '全区' ";  //分组信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }

                    //查询条件
                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }

                    //客户类型 2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                    //16个地市
                    sqlQqWhereStr = " and "+key+".hx_latn_name in "+localCity;

                    if(whereDateStr.equals("")){
                        whereDateStr = " "+key+".op_date='{repl_date}' ";
                    }else{
                        whereDateStr = whereDateStr + " and  "+key+".op_date='{repl_date}' ";
                    }

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlQqColumnStr: "+sqlQqColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlQqWhereStr: "+sqlQqWhereStr);
                log.info("------count-whereDateStr: "+whereDateStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){

                    sqlStr = "select " + sqlColumnStr + sqlQqColumnStr+" from " + sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlQqWhereStr + sqlCustTypeStr + sqlRoleTypeStr +sqlGroupByStr;

                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                    }else{

                        sqlStr = "select "+sumColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlQqWhereStr+ " and " +whereDateStr +sqlGroupByStr;

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }


    /**
     * 获取自动化报表统计SQL语句 全区 五项集约
     * @param reportParam
     * @return
     */
    private String getReportCountSqlQuanQuTjkb(ReportParam reportParam){
        String sqlStr = "";

        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                localCity ="('9999')";
            }

            if(localCity.equals("")){
                return "";
            }

            String sumColumnStr ="";
            String whereDateStr ="";

            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);
                                    columnMap.put(tableName, columnName);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;
                                    tableNameMapValue = tableNameMapValue+","+ columnName;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlQqColumnStr =" ,'全区' sel_all_quan_qu";   //全区字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQqWhereStr ="";   //全区条件信息
            String sqlHxTypeStr =" and hx_type= '分公司' ";

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    //16个地市
                    sqlQqWhereStr = " and "+key+".hx_latn_id in "+localCity;

                    if(whereDateStr.equals("")){
                        whereDateStr = " "+key+".op_date='{repl_date}' ";
                    }else{
                        whereDateStr = whereDateStr + " and  "+key+".op_date='{repl_date}' ";
                    }

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlQqColumnStr: "+sqlQqColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlQqWhereStr: "+sqlQqWhereStr);
                log.info("------count-whereDateStr: "+whereDateStr);
                log.info("------count-sqlHxTypeStr: "+sqlHxTypeStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){

                    sqlStr = "select "+sqlColumnStr+ sqlQqColumnStr +" from "+sqlTableStr +" where 1=1 "
                            + sqlQqWhereStr+ " and " +whereDateStr + sqlHxTypeStr;

                    log.info("+++++++++自助报表统计SQL: {}",sqlStr);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }


    /**
     * 获取自动化报表统计SQL语句 地市
     * @param reportParam
     * @return
     */
    private String getReportCountSqlDiShi(ReportParam reportParam){
        String sqlStr = "";

        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的地市信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_latn_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        for(ReportDimension reportDimension:reportDimensionList){
                            String dimensionName = reportDimension.getDimensionName();
                            if(localCity.equals("")){
                                localCity = "'"+dimensionName+"'";
                            }else{
                                localCity = localCity+","+"'"+dimensionName+"'";
                            }
                        }
                        localCity = "("+localCity+")";
                    }
                }
            }

            if(localCity.equals("")){
                return "";
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr()==null?"":reportIndex.getFzBdsEr(); //分子表达式2
                                String fmBdsEr = reportIndex.getFmBdsEr()==null?"":reportIndex.getFmBdsEr(); //分母表达式2

                                //tableExpr = "("+tableExpr+") ";

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableExpr = tableExpr +", "+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableExpr = tableExpr +", "+fmBdsEr;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }


                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //地州
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlDzWhereStr = key+".hx_latn_name in "+localCity;

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    }

                    //客户类型
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }

                    if(whereDateStr.equals("")){
                        whereDateStr = " "+key+".op_date='{repl_date}' ";
                    }else{
                        whereDateStr = whereDateStr + " and  "+key+".op_date='{repl_date}' ";
                    }

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                    sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr + sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);


                    }else{

                        sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + sqlDzOrderByStr;

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }


    /**
     * 获取自动化报表统计SQL语句 地市 五项集约
     * @param reportParam
     * @return
     */
    private String getReportCountSqlDiShiTjkb(ReportParam reportParam){
        String sqlStr = "";

        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的地市信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_latn_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        //String dimensionName = reportDimension.getDimensionName();
                        String dimensionId = reportDimension.getDimensionId();
                        if(dimensionId.indexOf("_f") >=0){
                            dimensionId = dimensionId.replaceAll("_f","");
                        }

                        if(localCity.equals("")){
                            localCity = "'"+dimensionId+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionId+"'";
                        }
                    }
                    localCity = "("+localCity+")";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        for(ReportDimension reportDimension:reportDimensionList){
                            String dimensionId = reportDimension.getDimensionId();
                            if(dimensionId.indexOf("_f") >=0){
                                dimensionId = dimensionId.replaceAll("_f","");
                            }

                            if(localCity.equals("")){
                                localCity = "'"+dimensionId+"'";
                            }else{
                                localCity = localCity+","+"'"+dimensionId+"'";
                            }
                        }
                        localCity = "("+localCity+")";
                    }
                }
            }

            if(localCity.equals("")){
                return "";
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds(); //分母表达式
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue+","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue+","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlDzWhereStr ="";   //地州条件信息
            String sqlDzOrderByStr = ""; //地州排序字段顺序
            String sqlGroupByStr = "";  //分组信息
            String sqlHxTypeStr = " and hx_type='分公司' ";

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    //地州
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlDzWhereStr = key+".hx_latn_id in "+localCity;

                        sqlColumnStr = sqlColumnStr+","+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_latn_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_latn_name");
                        if(sqlGroupByStr.equals("")){
                            sqlGroupByStr = key+".hx_latn_name";
                        }else{
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_latn_name";
                        }

                        //地州排序字段顺序
                        //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                        //2026-1-23
                        String sortLatnName = ReportUtil.getSortLatnName();
                        sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                    }

                    if(whereDateStr.equals("")){
                        whereDateStr = " "+key+".op_date='{repl_date}' ";
                    }else{
                        whereDateStr = whereDateStr + " and  "+key+".op_date='{repl_date}' ";
                    }

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlDzWhereStr: "+sqlDzWhereStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlHxTypeStr: "+sqlHxTypeStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlDzWhereStr.equals("")){
                        sqlDzWhereStr =  " and ("+sqlDzWhereStr+")";
                    }

                        /*sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + sqlHxTypeStr + " group by "+sqlGroupByStr + sqlDzOrderByStr;*/

                    sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlDzWhereStr + " and " +whereDateStr + sqlHxTypeStr + sqlDzOrderByStr;

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }


    /**
     * 获取报表统计数据区县导出
     * @param reportParam
     * @return
     */
    private String  getReportCountSqlQuXian(ReportParam reportParam){
        String sqlStr ="";

        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的区县信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_area_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        /*if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }*/

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionName+"'";
                        }else{
                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                            localCity = localCity+",'"+dimensionName+"'";
                        }

                    }
                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                String dimensionName = quXianDimension.getDimensionName();
                                /*if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }*/

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+dimensionName+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                String dimensionName = quXianDimension.getDimensionName();
                                /*if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }*/

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+dimensionName+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }
                    }
                }
            }

            if(localCity.equals("")){
                return "";
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }

                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }




            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQxWhereStr ="";   //区县条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr = ""; //地市排序

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //区县
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlQxWhereStr = key+".hx_area_name in "+localCity;
                        //sqlQxWhereStr = key+".hx_area_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                    }

                    //客户类型  2025-9-8
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }


                    if(whereDateStr.equals("")){
                        whereDateStr = " "+key+".op_date='{repl_date}' ";
                    }else{
                        whereDateStr = whereDateStr + " and  "+key+".op_date='{repl_date}' ";
                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";
                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlQxWhereStr: "+sqlQxWhereStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlQxWhereStr.equals("")){
                        sqlQxWhereStr =  " and ("+sqlQxWhereStr+")";
                    }

                    sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlQxWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr+sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                    }else{

                        sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr + sqlDzOrderByStr;

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }


    /**
     * 获取报表统计数据区县导出 五项集约
     * @param reportParam
     * @return
     */
    private String  getReportCountSqlQuXianWxjy(ReportParam reportParam){
        String sqlStr ="";

        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的区县信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","hx_area_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        //String dimensionName = reportDimension.getDimensionName();
                        String dimensionId = reportDimension.getDimensionId();
                        if(dimensionId.indexOf("_f") >=0){
                            dimensionId = dimensionId.replaceAll("_f","");
                        }

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionId+"'";
                        }else{
                            localCity = localCity+",'"+dimensionId+"'";
                        }

                    }
                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //String dimensionName = quXianDimension.getDimensionName();
                                String qxDimensionId = quXianDimension.getDimensionId();
                                if(qxDimensionId.indexOf("_f") >=0){
                                    qxDimensionId = qxDimensionId.replaceAll("_f","");
                                }
                                /*if(localCity.equals("")){
                                    localCity = "'"+dimensionName+"'";
                                }else{
                                    localCity = localCity+","+"'"+dimensionName+"'";
                                }*/

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+qxDimensionId+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+qxDimensionId+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //String dimensionName = quXianDimension.getDimensionName();

                                String qxDimensionId = quXianDimension.getDimensionId();
                                if(qxDimensionId.indexOf("_f") >=0){
                                    qxDimensionId = qxDimensionId.replaceAll("_f","");
                                }

                                if(localCity.equals("")){
                                    //localCity = "^"+dimensionName+"$";
                                    localCity = "'"+qxDimensionId+"'";
                                }else{
                                    //localCity = localCity+"|"+"^"+dimensionName+"$";
                                    localCity = localCity+",'"+qxDimensionId+"'";
                                }

                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";
                        }
                    }
                }
            }

            if(localCity.equals("")){
                return "";
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds();
                                String fmBds = reportIndex.getFmBds();
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName+","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName+","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }

                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlQxWhereStr ="";   //区县条件信息
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr = ""; //地市排序
            String sqlHxTypeStr = " and hx_type='县分' "; //划小类型

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    //区县
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlQxWhereStr = key+".hx_area_id in "+localCity;
                        //sqlQxWhereStr = key+".hx_area_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","hx_area_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","hx_area_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".hx_area_name";
                            sqlGroupByStr = key+".hx_area_name,"+key+".hx_latn_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".hx_area_name,"+key+".hx_latn_name";
                        }

                    }


                    if(whereDateStr.equals("")){
                        whereDateStr = " "+key+".op_date='{repl_date}' ";
                    }else{
                        whereDateStr = whereDateStr + " and  "+key+".op_date='{repl_date}' ";
                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlQxWhereStr: "+sqlQxWhereStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);
                log.info("------count-sqlHxTypeStr: "+sqlHxTypeStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlQxWhereStr.equals("")){
                        sqlQxWhereStr =  " and ("+sqlQxWhereStr+")";
                    }

                        /*sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr + " group by "+sqlGroupByStr+ sqlDzOrderByStr;*/

                    sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlQxWhereStr + " and " +whereDateStr + sqlHxTypeStr + sqlDzOrderByStr;

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }

    /**
     * 获取报表统计数据网格 导出
     * @param reportParam
     * @return
     */
    private String getReportCountSqlWangGe(ReportParam reportParam){

        String sqlStr ="";
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的网格信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","x_hx5_bp_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionName = reportDimension.getDimensionName();
                        /*if(localCity.equals("")){
                            localCity = "'"+dimensionName+"'";
                        }else{
                            localCity = localCity+","+"'"+dimensionName+"'";
                        }*/
                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionName+"'";
                        }else{
                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                            localCity = localCity+",'"+dimensionName+"'";
                        }
                    }

                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){ //地市账号
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        String dimensionName = wangGereportDimension.getDimensionName();
                                        /*if(localCity.equals("")){
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            localCity = localCity+","+"'"+dimensionName+"'";
                                        }*/
                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+dimensionName+"'";
                                        }
                                    }
                                }
                            }

                            localCity = "("+localCity+")";

                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        String dimensionName = wangGereportDimension.getDimensionName();
                                        /*if(localCity.equals("")){
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            localCity = localCity+","+"'"+dimensionName+"'";
                                        }*/

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+dimensionName+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+dimensionName+"'";
                                        }
                                    }

                                }
                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";

                        }else{ //网格账号
                            QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                            wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                            wangGeQueryWrapper.eq("dimension_name",deptName);
                            wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                            List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                            if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                    String dimensionName = wangGereportDimension.getDimensionName();

                                    if(localCity.equals("")){
                                        //localCity = "^"+dimensionName+"$";
                                        localCity = "'"+dimensionName+"'";
                                    }else{
                                        //localCity = localCity+"|"+"^"+dimensionName+"$";
                                        localCity = localCity+",'"+dimensionName+"'";
                                    }
                                }
                                localCity = "("+localCity+")";
                            }
                        }
                    }
                }
            }

            if(localCity.equals("")){
                return "";
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String tableExpr = reportIndex.getTableExpr();
                                String fzBds = reportIndex.getFzBds()==null?"":reportIndex.getFzBds(); //分子表达式
                                String fmBds = reportIndex.getFmBds()==null?"":reportIndex.getFmBds(); //分母表达式

                                //tableExpr = "("+tableExpr+") ";

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){
                                    //columnMap.put(tableName, tableExpr +columnName);

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    columnMap.put(tableName, tableExpr);
                                }else{
                                    //tableNameMapValue = tableNameMapValue+","+ tableExpr +columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableExpr = tableExpr +", "+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableExpr = tableExpr +", "+fmBds;
                                    }

                                    tableNameMapValue = tableNameMapValue+","+ tableExpr;
                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            //解析条件信息
            Map<String,String> conditMap = new HashMap<>();
            String whereStr = reportParam.getWhereInfo();
            if(StringUtils.isNotEmpty(whereStr)){
                ObjectMapper mapper = new ObjectMapper();
                String[][] result = mapper.readValue(whereStr, String[][].class);

                for (String[] row : result) {
                    String conditIdStr = row[0];
                    String conditWhereStr = row[1];

                    ReportCondit reportCondit = reportConditMapper.selectById(conditIdStr);
                    if(reportCondit != null){
                        int indexId = reportCondit.getIndexId();
                        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                        if(reportIndex != null){
                            String tableName = reportIndex.getTableName();
                            String coditWhereMapValue = conditMap.get(tableName);
                            if(StringUtil.isEmpty(coditWhereMapValue)){
                                conditMap.put(tableName, tableName+"."+conditWhereStr);
                            }else{
                                coditWhereMapValue = coditWhereMapValue+" and "+tableName+"."+conditWhereStr;
                                conditMap.put(tableName, coditWhereMapValue);
                            }
                        }
                    }
                }
            }


            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlWgWhereStr ="";   //网格条件信息
            String sqlCustTypeStr = "";    //客户类型
            String sqlRoleTypeStr = "";    //分析角色
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr =""; //地市排序

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }


                    String conditWhereStr = conditMap.get(key);
                    if(StringUtil.isNotEmpty(conditWhereStr)){
                        sqlWhereStr = sqlWhereStr +" and " + conditWhereStr;
                    }


                    //网格
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlWgWhereStr = key+".x_hx5_bp_name in "+localCity;
                        //sqlWgWhereStr = key+".x_hx5_bp_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }

                    }

                    //客户类型
                    /*String custZq = reportParam.getCustZq() ==null?"":reportParam.getCustZq();
                    String custGz = reportParam.getCustGz() ==null?"":reportParam.getCustGz();
                    if(custZq.equals("1") && !custGz.equals("1")){ //政企
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='政企客户'";
                    }else if(!custZq.equals("1") && custGz.equals("1")){ //公众
                        sqlCustTypeStr = sqlCustTypeStr +" and "+key+".cust_type='公众客户'";
                    }*/
                    String custType = reportParam.getCustType();
                    if(StringUtil.isNotEmpty(custType)){
                        String[] custTypeArr = custType.split(";");
                        if(custTypeArr.length ==2){
                            sqlCustTypeStr = " and "+key+"."+custTypeArr[1]+"='"+custTypeArr[0]+"'";
                        }
                    }

                    //分析角色
                    String roleType = reportParam.getRoleType() ==null?"":reportParam.getRoleType();
                    if(roleType.equals("customer")){ //客户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".cust_id is not null";
                    }else if(roleType.equals("user")){ //用户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".serv_id is not null";
                    }else if(roleType.equals("account")){ //账户
                        sqlRoleTypeStr = sqlRoleTypeStr +" and "+key+".acct_id is not null";
                    }


                    if(whereDateStr.equals("")){
                        whereDateStr = " "+key+".op_date='{repl_date}' ";
                    }else{
                        whereDateStr = whereDateStr + " and  "+key+".op_date='{repl_date}' ";
                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------count-sqlCustTypeStr: "+sqlCustTypeStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                    sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlWgWhereStr + sqlCustTypeStr + sqlRoleTypeStr
                            + " group by "+sqlGroupByStr + sqlDzOrderByStr;


                    if(sqlStr.indexOf(".op_date") >=0 || sqlStr.indexOf(".OP_DATE") >=0){

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);
                    }else{

                        sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlWgWhereStr + " and " +whereDateStr + sqlDzOrderByStr;

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }


    /**
     * 获取报表统计数据网格 导出 五项集约
     * @param reportParam
     * @return
     */
    private String getReportCountSqlWangGeWxjy(ReportParam reportParam){

        String sqlStr ="";
        try {

            //获取当前登录人所在的地市
            String localCity = ""; //所属地市
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId();
            if(StringUtil.isNotEmpty(deptId) && deptId.equals("1")){ //区公司
                //获取维度表中的网格信息
                QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                dimensionQueryWrapper.eq("field","x_hx5_bp_name");
                dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                if(reportDimensionList != null && reportDimensionList.size() >0){
                    for(ReportDimension reportDimension:reportDimensionList){
                        String dimensionId = reportDimension.getDimensionId();
                        if(dimensionId.indexOf("_f") >=0){
                            dimensionId = dimensionId.replaceAll("_f","");
                        }

                        if(localCity.equals("")){
                            //localCity = "^"+dimensionName+"$";
                            localCity = "'"+dimensionId+"'";
                        }else{
                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                            localCity = localCity+",'"+dimensionId+"'";
                        }
                    }

                    localCity = "("+localCity+")";
                    //localCity = "'"+localCity+"'";
                }
            }else if(StringUtil.isNotEmpty(deptId)){
                //根据部门ID获取部门名称
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    String deptName = sysDept.getDeptName();
                    //通过部门名称查询此部门是否是维度中的地市
                    QueryWrapper<ReportDimension> dimensionQueryWrapper = new QueryWrapper<>();
                    dimensionQueryWrapper.eq("field","hx_latn_name");
                    dimensionQueryWrapper.eq("dimension_name",deptName);
                    dimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                    List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(dimensionQueryWrapper);
                    if(reportDimensionList != null && reportDimensionList.size() >0){ //地市账号
                        String dimensionId = reportDimensionList.get(0).getDimensionId();
                        //根据地市ID获取地市下的区县
                        QueryWrapper<ReportDimension> quXianQueryWrapper = new QueryWrapper<>();
                        quXianQueryWrapper.eq("parent_id",dimensionId);
                        quXianQueryWrapper.eq("field","hx_area_name");
                        quXianQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        //String dimensionName = wangGereportDimension.getDimensionName();
                                        String wgDimensionId = wangGereportDimension.getDimensionId();
                                        if(wgDimensionId.indexOf("_f") >=0){
                                            wgDimensionId = wgDimensionId.replaceAll("_f","");
                                        }

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+wgDimensionId+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+wgDimensionId+"'";
                                        }
                                    }
                                }
                            }

                            localCity = "("+localCity+")";

                        }

                    }else{
                        //查询此账号是否是区县账号
                        QueryWrapper<ReportDimension> quXianDimensionQueryWrapper = new QueryWrapper<>();
                        quXianDimensionQueryWrapper.eq("field","hx_area_name");
                        quXianDimensionQueryWrapper.eq("dimension_name",deptName);
                        quXianDimensionQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                        List<ReportDimension> quXianDimensionList = reportDimensionMapper.selectList(quXianDimensionQueryWrapper);
                        if(quXianDimensionList != null && quXianDimensionList.size() >0){
                            for(ReportDimension quXianDimension : quXianDimensionList){
                                //获取区县下的网格
                                String quXianDimensionId = quXianDimension.getDimensionId();
                                QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                                wangGeQueryWrapper.eq("parent_id",quXianDimensionId);
                                wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                                wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                                List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                                if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                    for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                        //String dimensionName = wangGereportDimension.getDimensionName();
                                        String wgDimensionId = wangGereportDimension.getDimensionId();
                                        if(wgDimensionId.indexOf("_f") >=0){
                                            wgDimensionId = wgDimensionId.replaceAll("_f","");
                                        }

                                        if(localCity.equals("")){
                                            //localCity = "^"+dimensionName+"$";
                                            localCity = "'"+wgDimensionId+"'";
                                        }else{
                                            //localCity = localCity+"|"+"^"+dimensionName+"$";
                                            localCity = localCity+",'"+wgDimensionId+"'";
                                        }
                                    }

                                }
                            }

                            localCity = "("+localCity+")";
                            //localCity = "'"+localCity+"'";

                        }else{ //网格账号
                            QueryWrapper<ReportDimension> wangGeQueryWrapper = new QueryWrapper<>();
                            wangGeQueryWrapper.eq("field","x_hx5_bp_name");
                            wangGeQueryWrapper.eq("dimension_name",deptName);
                            wangGeQueryWrapper.eq("is_non_stand",reportParam.getIsNonNtand());
                            List<ReportDimension> wangGeDimensionList = reportDimensionMapper.selectList(wangGeQueryWrapper);
                            if(wangGeDimensionList != null && wangGeDimensionList.size() >0){
                                for(ReportDimension wangGereportDimension:wangGeDimensionList){
                                    //String dimensionName = wangGereportDimension.getDimensionName();
                                    String wgDimensionId = wangGereportDimension.getDimensionId();
                                    if(wgDimensionId.indexOf("_f") >=0){
                                        wgDimensionId = wgDimensionId.replaceAll("_f","");
                                    }

                                    if(localCity.equals("")){
                                        //localCity = "^"+dimensionName+"$";
                                        localCity = "'"+wgDimensionId+"'";
                                    }else{
                                        //localCity = localCity+"|"+"^"+dimensionName+"$";
                                        localCity = localCity+",'"+wgDimensionId+"'";
                                    }
                                }
                                localCity = "("+localCity+")";
                            }
                        }
                    }
                }
            }

            if(localCity.equals("")){
                return "";
            }


            String whereDateStr ="";
            //解析指标信息
            Map<String, String> columnMap = new HashMap<>();
            String indexStr = reportParam.getIndexInfo();
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                            int indexId = Integer.parseInt(indexIdStr);
                            ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
                            if(reportIndex != null){
                                String tableName = reportIndex.getTableName();
                                String columnName = reportIndex.getColumnName();
                                String fzBds = reportIndex.getFzBds();
                                String fmBds = reportIndex.getFmBds();
                                String fzBdsEr = reportIndex.getFzBdsEr();
                                String fmBdsEr = reportIndex.getFmBdsEr();

                                String tableNameMapValue = columnMap.get(tableName);
                                if(StringUtil.isEmpty(tableNameMapValue)){

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        columnName = columnName +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        columnName = columnName +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        columnName = columnName+","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        columnName = columnName+","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, columnName);
                                }else{

                                    tableNameMapValue = tableNameMapValue+","+ columnName;

                                    if(StringUtil.isNotEmpty(fzBds)){
                                        tableNameMapValue = tableNameMapValue +","+fzBds;
                                    }

                                    if(StringUtil.isNotEmpty(fmBds)){
                                        tableNameMapValue = tableNameMapValue +","+fmBds;
                                    }

                                    if(StringUtil.isNotEmpty(fzBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fzBdsEr;
                                    }

                                    if(StringUtil.isNotEmpty(fmBdsEr)){
                                        tableNameMapValue = tableNameMapValue + ","+fmBdsEr;
                                    }

                                    columnMap.put(tableName, tableNameMapValue);
                                }
                            }
                        }
                    }
                }
            }

            String sqlColumnStr =""; //查询列字段信息
            String sqlTableStr ="";  //查询表信息
            String sqlWhereStr ="";  //查询条件信息
            String sqlWgWhereStr ="";   //网格条件信息
            String sqlGroupByStr = "";  //分组信息
            String sqlDzOrderByStr =""; //地市排序
            String sqlHxTypeStr =" and hx_type='网格' "; //划小类型

            if(columnMap != null && columnMap.size()>0){
                for (String key : columnMap.keySet()) {
                    String columnStr = columnMap.get(key);

                    if(sqlColumnStr.equals("")){
                        sqlColumnStr = columnStr;
                    }else{
                        sqlColumnStr = sqlColumnStr+","+columnStr;
                    }

                    if(sqlTableStr.equals("")){
                        sqlTableStr = key +" "+key;
                    }else{
                        sqlTableStr = sqlTableStr+","+key +" "+key;
                    }

                    //网格
                    if(StringUtil.isNotEmpty(localCity)){

                        sqlWgWhereStr = key+".x_hx5_bp_id in "+localCity;
                        //sqlWgWhereStr = key+".x_hx5_bp_name REGEXP "+localCity;

                        //sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name";
                        sqlColumnStr = sqlColumnStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        sqlColumnStr = sqlColumnStr.replaceAll("\\{area_name}","x_hx5_bp_name");

                        sqlGroupByStr = sqlGroupByStr.replaceAll("\\{area_name}","x_hx5_bp_name");
                        if(sqlGroupByStr.equals("")){
                            //sqlGroupByStr = key+".x_hx5_bp_name";
                            sqlGroupByStr = key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }else{
                            //sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name";
                            sqlGroupByStr = sqlGroupByStr+","+key+".x_hx5_bp_name,"+key+".hx_latn_name,"+key+".hx_area_name";
                        }

                    }


                    if(whereDateStr.equals("")){
                        whereDateStr = " "+key+".op_date='{repl_date}' ";
                    }else{
                        whereDateStr = whereDateStr + " and  "+key+".op_date='{repl_date}' ";
                    }

                    //地州排序字段顺序
                    //sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子')";
                    //2026-1-23
                    String sortLatnName = ReportUtil.getSortLatnName();
                    sqlDzOrderByStr = " ORDER BY FIELD("+key+".hx_latn_name,"+sortLatnName+")";

                }

                log.info("------count-sqlColumnStr: "+sqlColumnStr);
                log.info("------count-sqlTableStr: "+sqlTableStr);
                log.info("------count-sqlWhereStr: "+sqlWhereStr);
                log.info("------count-sqlWgWhereStr: "+sqlWgWhereStr);
                log.info("------count-sqlGroupByStr: "+sqlGroupByStr);
                log.info("------count-sqlDzOrderByStr: "+sqlDzOrderByStr);

                //拼接完整的SQL查询语句
                if(sqlColumnStr.length() >0 && sqlTableStr.length() >0 ){
                    //int offset  = (pageDomain.getPage() - 1) * pageDomain.getLimit();
                    //String sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 and " +sqlWhereStr +" LIMIT "+offset+", "+pageDomain.getLimit();
                    if(!sqlWgWhereStr.equals("")){
                        sqlWgWhereStr =  " and ("+sqlWgWhereStr+")";
                    }

                        /*sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                                +sqlWhereStr + sqlWgWhereStr + " and " +whereDateStr + " group by "+sqlGroupByStr + sqlDzOrderByStr;*/
                        sqlStr = "select "+sqlColumnStr+" from "+sqlTableStr +" where 1=1 "
                            +sqlWhereStr + sqlWgWhereStr + " and " +whereDateStr + sqlHxTypeStr + sqlDzOrderByStr;

                        log.info("+++++++++自助报表统计SQL: {}",sqlStr);

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return sqlStr;
    }

    // 格式化输出时间
    public static String formatTime(long nanoseconds) {
        if (nanoseconds < 1000) {
            return nanoseconds + " ns";
        } else if (nanoseconds < 1_000_000) {
            return (nanoseconds / 1_000.0) + " μs";
        } else if (nanoseconds < 1_000_000_000) {
            return (nanoseconds / 1_000_000.0) + " ms";
        } else {
            return (nanoseconds / 1_000_000_000.0) + " s";
        }
    }


    /**
     * 替换日期,默认前一天
     * @param replaceStr
     * @param tableName
     * @param startDate
     * @param endDate
     * @return
     */
    public String replaceDateLast(String replaceStr,String sourStr,String tableName,String startDate,String endDate){
        if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
            String repStr = tableName+".op_date='"+sourStr+"'";
            String repValue = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";
            //replaceStr = replaceStr.replaceAll(repStr, repValue);
            replaceStr = replaceStr.replace(repStr, repValue);

        }else if(StringUtil.isNotEmpty(startDate)){
            String repStr = "'"+sourStr+"'";
            String repValue = "'"+startDate +"' ";
            //replaceStr = replaceStr.replaceAll(repStr, repValue);
            replaceStr = replaceStr.replace(repStr, repValue);

        }else if(StringUtil.isNotEmpty(endDate)){
            String repStr = "'"+sourStr+"'";
            String repValue = "'"+endDate +"' ";
            //replaceStr = replaceStr.replaceAll(repStr, repValue);
            replaceStr = replaceStr.replace(repStr, repValue);
        }else{
            String strDate = DateTimeUtil.getYesterday("yyyyMMdd");
            //replaceStr = replaceStr.replaceAll(sourStr, strDate);
            replaceStr = replaceStr.replace(sourStr, strDate);
        }

        return replaceStr;
    }


    /**
     * 替换日期前一天,默认前两天
     * @param replaceStr
     * @param sourStr
     * @param startDate
     * @param endDate
     * @return
     */
    public String replaceDateLastTwo(String replaceStr,String sourStr,String startDate,String endDate){
        if(StringUtil.isNotEmpty(startDate)){
            //获取开始日期的前一天
            String befoStartDate = DateTimeUtil.getStrDateBefore(startDate,"yyyyMMdd",1);
            String repStr = "'"+sourStr+"'";
            String repValue = "'"+befoStartDate +"' ";
            //replaceStr = replaceStr.replaceAll(repStr, repValue);
            replaceStr = replaceStr.replace(repStr, repValue);

        }else if(StringUtil.isNotEmpty(endDate)){
            //获取结束日期的前一天
            String befoEndDate = DateTimeUtil.getStrDateBefore(endDate,"yyyyMMdd",1);
            String repStr = "'"+sourStr+"'";
            String repValue = "'"+befoEndDate +"' ";
            //replaceStr = replaceStr.replaceAll(repStr, repValue);
            replaceStr = replaceStr.replace(repStr, repValue);
        }else{
            //获取当前日期的前两天
            String strDate = DateTimeUtil.getCurrDateBefore("yyyyMMdd",-2);
            replaceStr = replaceStr.replace(sourStr, strDate);
        }

        return replaceStr;
    }

    /**
     * 替换日期,默认前一个月
     * @param replaceStr
     * @param tableName
     * @param startDate
     * @param endDate
     * @return
     */
    public String replaceMonthLast(String replaceStr,String sourStr,String tableName,String startDate,String endDate){
        if(StringUtil.isNotEmpty(startDate) && StringUtil.isNotEmpty(endDate)){
            String repStr = tableName+".op_date='"+sourStr+"'";
            String repValue = " ("+tableName+".op_date >='"+startDate +"' and "+tableName+".op_date <='"+endDate+"') ";
            replaceStr = replaceStr.replace(repStr, repValue);

        }else if(StringUtil.isNotEmpty(startDate)){
            String repStr = "'"+sourStr+"'";
            String repValue = "'"+startDate +"' ";
            replaceStr = replaceStr.replace(repStr, repValue);

        }else if(StringUtil.isNotEmpty(endDate)){
            String repStr = "'"+sourStr+"'";
            String repValue = "'"+endDate +"' ";
            //replaceStr = replaceStr.replaceAll(repStr, repValue);
            replaceStr = replaceStr.replace(repStr, repValue);
        }else{
            String strDate = DateTimeUtil.getLastMonth("yyyyMM");
            replaceStr = replaceStr.replace(sourStr, strDate);
        }

        return replaceStr;
    }


    /**
     * 替换日期,默认前两个月
     * @param replaceStr

     * @param startDate
     * @param endDate
     * @return
     */
    public String replaceMonthLastTwo(String replaceStr,String sourStr,String startDate,String endDate){
        if(StringUtil.isNotEmpty(startDate)){
            String beforeDate = DateTimeUtil.getStrMonthBefore(startDate,"yyyyMM",1);
            String repStr = "'"+sourStr+"'";
            String repValue = "'"+beforeDate +"' ";
            replaceStr = replaceStr.replace(repStr, repValue);

        }else if(StringUtil.isNotEmpty(endDate)){
            String beforeDate = DateTimeUtil.getStrMonthBefore(endDate,"yyyyMM",1);
            String repStr = "'"+sourStr+"'";
            String repValue = "'"+beforeDate +"' ";
            replaceStr = replaceStr.replace(repStr, repValue);
        }else{
            String strDate = DateTimeUtil.getCurrMonthBefore("yyyyMM",-2);
            replaceStr = replaceStr.replace(sourStr, strDate);
        }

        return replaceStr;
    }


    /**
     * 获取所选指标的ID，多个用;拼接
     * @param indexStr
     * @return
     */
    public String getSelIndexId(String indexStr){
        String selIndexIds ="";
        try {
            //解析指标信息
            if(StringUtil.isNotEmpty(indexStr)){
                JSONArray indexJsonArray = JSON.parseArray(indexStr);
                if(indexJsonArray !=null && indexJsonArray.size() >0){

                    for(int i = 0; i < indexJsonArray.size();i++){
                        JSONObject indexObj = indexJsonArray.getJSONObject(i);
                        String indexIdStr = indexObj.getString("id");
                        if(!indexIdStr.startsWith("wd_")){
                           if(StringUtil.isEmpty(selIndexIds)){
                               selIndexIds = indexIdStr;
                           }else{
                               selIndexIds = selIndexIds+";"+indexIdStr;
                           }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return selIndexIds;
    }


    /**
     * 获取当前登录人的区域信息
     * @return
     */
    @Override
    public String getCurrAreaInfo() {
        String retStr ="";
        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        String deptId  = currentUser.getDeptId();
        if(StringUtil.isNotEmpty(deptId)){
            if(deptId.equals("1")){ //区公司
                retStr = "qgs";
            }else{
                SysDept sysDept = sysDeptMapper.selectById(deptId);
                if(sysDept != null){
                    QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("dimension_name",sysDept.getDeptName());
                    queryWrapper.eq("is_non_stand","0");
                    queryWrapper.last("limit 1");

                    ReportDimension reportDimension = reportDimensionMapper.selectOne(queryWrapper);
                    if(reportDimension != null){
                        retStr = reportDimension.getField();
                    }
                }

            }
        }

        return retStr;
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
     * 计算合计列
     * @param retReportDataList
     * @return
     */
    public List<Map<String,Object>> getCountListTj(List<Map<String,Object>> retReportDataList){
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
            for(String key : keyset){
                Object objValue = newAllMap.get(key);

                if(key.equalsIgnoreCase("hx_latn_name")){
                    totalMap.put(key,"合计");
                }else if(key.equalsIgnoreCase("hx_area_name") || key.equalsIgnoreCase("x_hx5_bp_name")){
                    totalMap.put(key,"-");
                }else if(objValue != null && objValue instanceof String && objValue.toString().endsWith("%")){
                    //BigDecimal average = calculateAver(retReportDataList, key);
                    BigDecimal average = calculateSumLv(retReportDataList, key);
                    String averageStr = average+"%";
                    totalMap.put(key,averageStr);
                }else if(objValue instanceof Number){ //数值求合
                    BigDecimal totalSum = calculateSum(retReportDataList, key);
                    totalMap.put(key,totalSum);
                }else{
                    totalMap.put(key,"-");
                }
            }
            retReportDataList.add(totalMap);
        }

        return retReportDataList;
    }


}
