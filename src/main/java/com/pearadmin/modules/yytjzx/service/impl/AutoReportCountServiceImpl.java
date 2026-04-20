package com.pearadmin.modules.yytjzx.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.ppt.util.ToolUtil;
import com.pearadmin.modules.report.domain.ReportDimension;
import com.pearadmin.modules.report.mapper.ReportDimensionMapper;
import com.pearadmin.modules.sys.domain.SysDept;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.mapper.SysDeptMapper;
import com.pearadmin.modules.yytjzx.domain.AutoReportCount;
import com.pearadmin.modules.yytjzx.domain.AutoReportIndexParam;
import com.pearadmin.modules.yytjzx.mapper.AutoReportCountMapper;
import com.pearadmin.modules.yytjzx.service.AutoReportCountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 创建日期：2025-10-29
 * 自助报表统计
 **/

@Slf4j
@Service
public class AutoReportCountServiceImpl implements AutoReportCountService {

    @Value("${yytjzx-report-count-export-path}")
    private String yytjzxReportCountExportPath;

    @Resource
    private ReportDimensionMapper reportDimensionMapper;

    @Resource
    private SysDeptMapper sysDeptMapper;

    @Resource
    private AutoReportCountMapper autoReportCountMapper;

    /**
     * 获取维度树数据
     * @return
     */
    @Override
    public Object dimensionTreeload() {
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

            ReportDimension queryReportDimension =null;
            if(deptName.equals("全疆")){
                queryReportDimension = reportDimensionMapper.selectOne(new QueryWrapper<ReportDimension>().eq("dimension_name", deptName));
            }else{
                queryReportDimension = reportDimensionMapper.selectOne(new QueryWrapper<ReportDimension>().eq("dimension_name", deptName).eq("is_non_stand","0"));
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
                retArr.put("spread",true);

                //retArr.put("children",getChildTreeNode(reportDimension.getDimensionId()));
                if(wdId.equals("1")){
                    retArr.put("children",getChildTreeNode(reportDimension.getDimensionId()));
                }else{
                    retArr.put("children",getCurrentTreeNode(wdId));
                }

            }
        }

        String resultStr = "["+retArr.toJSONString()+"]";
        return resultStr;
    }


    private JSONArray getChildTreeNode(String parentId){
        JSONArray retArr = new JSONArray();
        QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("is_non_stand", "0");
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

                //childObj.put("spread",true);
                childObj.put("children",getChildTreeNode(reportDimension.getDimensionId())) ;
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
    private JSONArray getCurrentTreeNode(String dimensionId){
        JSONArray retArr = new JSONArray();
        QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
        //queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("dimension_id", dimensionId);
        queryWrapper.eq("is_non_stand", "0");
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
                childObj.put("spread",true);
                childObj.put("children",getChildTreeNode(reportDimension.getDimensionId())) ;
                retArr.add(childObj);
            }
        }

        return retArr;

    }


    /**
     * 获取整体应用统计数据
     * @return
     */
    @Override
    public List<AutoReportCount> getAppCount(String area,String startDate,String endDate) {
        List<AutoReportCount> autoReportCountList = new ArrayList<>();
        try {

            String deptId = "-1";

            if(StringUtil.isEmpty(area)){
                //当前登录人信息
                SysUser currentUser = UserContext.currentUser();
                String currentDeptId = currentUser.getDeptId();
                //不是区公司账号
                if(!currentDeptId.equals("1")){
                    //获取当前登录人所在的部门名称
                    SysDept sysDept = sysDeptMapper.selectById(currentUser.getDeptId());
                    if(sysDept != null){
                        area = sysDept.getDeptName();
                        deptId = sysDept.getDeptId();
                    }
                }
            }else{
                if(area.equals("全疆")){
                    deptId = "";
                    area ="";
                }else{
                    QueryWrapper<SysDept> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("dept_name", area);
                    SysDept sysDept = sysDeptMapper.selectOne(queryWrapper);
                    if(sysDept != null){
                        deptId = sysDept.getDeptId();
                    }
                }
            }

            //获取自助取数用户部门数据
            AutoReportCount autoReportCount = getAutoReportUserCount(deptId,startDate,endDate);

            autoReportCount = getAutoReportIndexCount(autoReportCount,startDate,endDate);

            autoReportCount = getAutoReportCreatPushReportCount(autoReportCount,deptId,startDate,endDate);

            autoReportCount = getAutoReportWarningCount(autoReportCount,area,startDate,endDate);

            autoReportCountList.add(autoReportCount);

        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCountList;
    }

    /**
     * 获取整体应用下一级统计数据
     *
     * @param area
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public List<AutoReportCount> getAppSubCount(String area,String areaCode, String startDate, String endDate) {

        List<AutoReportCount> autoReportCountList = new ArrayList<>();
        try {

            String deptId = "-1";

            if(StringUtil.isEmpty(area)){
                //当前登录人信息
                SysUser currentUser = UserContext.currentUser();
                String currentDeptId = currentUser.getDeptId();
                //不是区公司账号
                if(!currentDeptId.equals("1")){
                    //获取当前登录人所在的部门名称
                    SysDept sysDept = sysDeptMapper.selectById(currentUser.getDeptId());
                    if(sysDept != null){
                        area = sysDept.getDeptName();
                        deptId = sysDept.getDeptId();
                    }
                }
            }else{
                if(area.equals("全疆")){
                    deptId = "1";
                    area ="全疆";
                }else{
                    QueryWrapper<SysDept> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("dept_name", area);
                    SysDept sysDept = sysDeptMapper.selectOne(queryWrapper);
                    if(sysDept != null){
                        deptId = sysDept.getDeptId();
                    }
                }
            }

            //获取自助取数用户部门数据
            autoReportCountList = getAutoReportSubUserCount(deptId,startDate,endDate);

            /*autoReportCountList = getAutoReportSubIndexCount(autoReportCountList,startDate,endDate);*/

            autoReportCountList = getAutoReportSubCreatPushReportCount(autoReportCountList,deptId,startDate,endDate);

            autoReportCountList = getAutoReportSubWarningCount(autoReportCountList,area,startDate,endDate);


        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCountList;

    }

    /**
     * 导出自助取数统计文件
     * @param area
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public ResponseEntity<InputStreamResource> downloadFile(String area,String areaCode, String startDate, String endDate) throws Exception{

        //获取总体的数据
        List<AutoReportCount> autoReportCountList = getAppCount(area,startDate,endDate);

        //获取总体下一级的数据
        List<AutoReportCount> autoReportSubCountList = getAppSubCount(area,areaCode,startDate,endDate);

        String filePath ="";
        if(autoReportCountList != null && autoReportCountList.size() >0){
            filePath = creatFile(autoReportCountList,autoReportSubCountList,area);
        }

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
            return null;
        }

    }

    /**
     * 获取区域下的子区域信息
     * @param area
     * @return
     */
    public String getSubAreaInfo(String area,String areaCode){
        String retAreaStr ="";
        try {
             String field ="";
             if(area.equals("全疆")){ //获取地州
                 field = "hx_latn_name";
             }else{
                if(StringUtil.isNotEmpty(areaCode) && areaCode.equals("hx_latn_name")){ //获取区县
                    field = "hx_area_name";
                }else if(StringUtil.isNotEmpty(areaCode) && areaCode.equals("hx_area_name")){ //获取网格
                    field = "x_hx5_bp_name";
                }
             }

            QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("field", field);
            List<ReportDimension> reportDimensionList = reportDimensionMapper.selectList(queryWrapper);
            if(reportDimensionList != null && reportDimensionList.size() >0){
                for(ReportDimension reportDimension : reportDimensionList){
                    if(StringUtil.isEmpty(retAreaStr)){
                        retAreaStr = reportDimension.getDimensionName();
                    }else{
                        retAreaStr = retAreaStr+";"+reportDimension.getDimensionName();
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return  retAreaStr;
    }

    /**
     * 根据部门ID获取子部门信息
     * @param deptId
     * @return
     */
    public String getSubDeptInfo(String deptId){
        String retDeptId ="";
        try {
            QueryWrapper<SysDept> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id",deptId);
            List<SysDept> sysDeptList = sysDeptMapper.selectList(queryWrapper);
            if(sysDeptList != null && sysDeptList.size() >0){
                for(SysDept sysDept : sysDeptList){
                    if(StringUtil.isEmpty(retDeptId)){
                        retDeptId = sysDept.getDeptId();
                    }else {
                        retDeptId = retDeptId+";"+sysDept.getDeptId();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return retDeptId;
    }

    /**
     * 获取自助取数中用户部分信息
     * @param deptId
     * @param startDate
     * @param endDate
     * @return
     */
    public AutoReportCount getAutoReportUserCount(String deptId,String startDate,String endDate){
        AutoReportCount autoReportCount = new AutoReportCount();
        try {
            startDate = startDate+" 00:00:00";
            endDate = endDate+" 23:59:59";
            String userCountSql ="";
            if(StringUtil.isNotEmpty(deptId)){
                /*userCountSql = "SELECT " +
                        "  (SELECT COUNT(*) FROM sys_user su WHERE su.dept_id in( " +
                        "  SELECT au.dept_id " +
                        "  FROM (SELECT * FROM sys_dept WHERE parent_id IS NOT NULL) au, " +
                        "       (SELECT @pid := '"+deptId+"') pd " +
                        " WHERE FIND_IN_SET(parent_id, @pid) > 0 " +
                        "   AND @pid := concat(@pid, ',', dept_id) " +
                        "UNION " +
                        "SELECT dept_id " +
                        "  FROM sys_dept sd " +
                        " WHERE dept_id = '"+deptId+"' " +
                        "  )) AS yh_fwyhs, " +
                        "  (SELECT COUNT(DISTINCT sl.operate_name) " +
                        "  FROM sys_log sl, sys_user su " +
                        "  WHERE sl.operate_name = su.username " +
                        "  AND su.dept_id in ( " +
                        "   SELECT au.dept_id " +
                        "  FROM (SELECT * FROM sys_dept WHERE parent_id IS NOT NULL) au, " +
                        "       (SELECT @pid := '"+deptId+"') pd " +
                        " WHERE FIND_IN_SET(parent_id, @pid) > 0 " +
                        "   AND @pid := concat(@pid, ',', dept_id) " +
                        "UNION " +
                        "SELECT dept_id " +
                        "  FROM sys_dept sd " +
                        " WHERE dept_id = '"+deptId+"' " +
                        "  ) " +
                        "  AND sl.logging_type = 'LOGIN' " +
                        "  AND sl.create_time >= '"+startDate+"' " +
                        "  AND sl.create_time <= '"+endDate+"') AS yh_hyyhs, " +
                        "  CONCAT(ROUND(" +
                        "  (SELECT COUNT(DISTINCT sl.operate_name)      FROM sys_log sl, sys_user su  WHERE sl.operate_name = su.username  AND su.dept_id in(SELECT au.dept_id " +
                        "  FROM (SELECT * FROM sys_dept WHERE parent_id IS NOT NULL) au, " +
                        "       (SELECT @pid := '"+deptId+"') pd " +
                        " WHERE FIND_IN_SET(parent_id, @pid) > 0 " +
                        "   AND @pid := concat(@pid, ',', dept_id) " +
                        "UNION " +
                        "SELECT dept_id " +
                        "  FROM sys_dept sd " +
                        " WHERE dept_id = '"+deptId+"') AND sl.logging_type = 'LOGIN' AND sl.create_time >= '"+startDate+"' AND sl.create_time <= '"+endDate+"') " +
                        "  / NULLIF((SELECT COUNT(*) FROM sys_user su WHERE su.dept_id in (SELECT au.dept_id " +
                        "  FROM (SELECT * FROM sys_dept WHERE parent_id IS NOT NULL) au, " +
                        "       (SELECT @pid := '"+deptId+"') pd " +
                        " WHERE FIND_IN_SET(parent_id, @pid) > 0 " +
                        "   AND @pid := concat(@pid, ',', dept_id) " +
                        "UNION\n" +
                        "SELECT dept_id " +
                        "  FROM sys_dept sd " +
                        " WHERE dept_id = '"+deptId+"')), 0) * 100, 2), '%') AS yh_yhhylv";*/

                userCountSql ="SELECT" +
                        "(SELECT COUNT(*) FROM sys_user su WHERE su.dept_id ='"+deptId+"') AS yh_fwyhs," +
                        " (SELECT COUNT(DISTINCT sl.operate_name)   FROM sys_log sl, sys_user su   WHERE sl.operate_name = su.username   AND su.dept_id ='"+deptId+"' " +
                        "  AND sl.logging_type = 'LOGIN'   AND sl.create_time >= '"+startDate+"'   AND sl.create_time <= '"+endDate+"') AS yh_hyyhs, " +
                        "  CONCAT(ROUND((SELECT COUNT(DISTINCT sl.operate_name) FROM sys_log sl, sys_user su  WHERE sl.operate_name = su.username  AND su.dept_id ='"+deptId+"' " +
                        " AND sl.logging_type = 'LOGIN' AND sl.create_time >= '"+startDate+"' AND sl.create_time <= '"+endDate+"') / NULLIF((SELECT COUNT(*) FROM sys_user su WHERE su.dept_id='"+deptId+"'), 0) * 100, 2), '%') AS yh_yhhylv";

            }else{
                userCountSql = "SELECT " +
                        "  (SELECT COUNT(*) FROM sys_user su ) AS yh_fwyhs, " +
                        "  (SELECT COUNT(DISTINCT sl.operate_name) " +
                        "   FROM sys_log sl, sys_user su " +
                        "   WHERE sl.operate_name = su.username " +
                        "     AND sl.logging_type = 'LOGIN' " +
                        "     AND sl.create_time >= '"+startDate+"' " +
                        "     AND sl.create_time <= '"+endDate+"') AS yh_hyyhs, " +
                        "  CONCAT(ROUND(" +
                        "    (SELECT COUNT(DISTINCT sl.operate_name) " +
                        "     FROM sys_log sl, sys_user su " +
                        "     WHERE sl.operate_name = su.username " +
                        "       AND sl.logging_type = 'LOGIN' " +
                        "       AND sl.create_time >= '"+startDate+"' " +
                        "       AND sl.create_time <= '"+endDate+"') " +
                        "    / NULLIF((SELECT COUNT(*) FROM sys_user su ), 0) * 100, 2), '%') AS yh_yhhylv";
            }

            log.info("+++++++++++++userCountSql: {}",userCountSql);
            List<AutoReportCount> autoReportCountList = autoReportCountMapper.getAutoReportCountList(userCountSql);
            if(autoReportCountList != null && autoReportCountList.size() > 0){
                autoReportCount = autoReportCountList.get(0);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCount;
    }


    /**
     * 获取自助取数中用户部分信息
     * @param deptId
     * @param startDate
     * @param endDate
     * @return
     */
    public List<AutoReportCount> getAutoReportSubUserCount(String deptId,String startDate,String endDate){
        List<AutoReportCount> autoReportCountList = new ArrayList<>();
        try {
            startDate = startDate+" 00:00:00";
            endDate = endDate+" 23:59:59";
            String userSubCountSql ="";
            if(StringUtil.isNotEmpty(deptId)){
                userSubCountSql = "SELECT " +
                        "    sd.dept_name area_name, " +
                        "    COUNT(DISTINCT su.username) as yh_fwyhs, " +
                        "    COUNT(DISTINCT " +
                        "        CASE WHEN sl.logging_type = 'LOGIN' " +
                        "             AND sl.create_time >= '"+startDate+"' " +
                        "             AND sl.create_time <= '"+endDate+"' " +
                        "        THEN sl.operate_name END " +
                        "    ) as yh_hyyhs, " +
                        "    CASE " +
                        "        WHEN COUNT(DISTINCT su.username) > 0 THEN " +
                        "            CONCAT(ROUND(COUNT(DISTINCT " +
                        "                CASE WHEN sl.logging_type = 'LOGIN' " +
                        "                     AND sl.create_time >= '"+startDate+"' " +
                        "                     AND sl.create_time <= '"+endDate+"' " +
                        "                THEN sl.operate_name END " +
                        "            ) / COUNT(DISTINCT su.username)*100,2),'%') " +
                        "        ELSE 0 " +
                        "    END as yh_yhhylv " +
                        "FROM sys_dept sd " +
                        "INNER JOIN sys_user su ON sd.dept_id = su.dept_id " +
                        "LEFT JOIN sys_log sl ON sl.operate_name = su.username " +
                        "    AND sl.logging_type = 'LOGIN' " +
                        "    AND sl.create_time >= '"+startDate+"' " +
                        "    AND sl.create_time <= '"+endDate+"' " +
                        " WHERE sd.dept_id IN ( select sd2.dept_id from sys_dept sd2 where sd2.parent_id ='"+deptId+"') " +
                        "GROUP BY sd.dept_id, sd.dept_name " +
                        "ORDER BY sd.dept_name";
            }

            log.info("+++++++++++++userSubCountSql: {}",userSubCountSql);
            autoReportCountList = autoReportCountMapper.getAutoReportCountList(userSubCountSql);


        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCountList;
    }



    /**
     * 获取自助取数中指标部分信息
     * @param startDate
     * @param endDate
     * @return
     */
    public AutoReportCount getAutoReportIndexCount(AutoReportCount autoReportCount,String startDate,String endDate){
        try {
            startDate = startDate+" 00:00:00";
            endDate = endDate+" 23:59:59";
            /*String indexCountSql ="SELECT " +
                    "    rc.classify_name, " +
                    "    COUNT(rc.classify_id) as index_count " +
                    "FROM report_index ri " +
                    "JOIN report_classify rc ON rc.classify_id = ri.classify_id " +
                    "where ri.create_time >='"+startDate+"' and ri.create_time <='"+endDate+"' " +
                    "and rc.create_time >='"+startDate+"' and rc.create_time <='"+endDate+"' " +
                    "and ri.is_query ='0' "+
                    "GROUP BY rc.classify_id, rc.classify_name ";*/
            String indexCountSql ="SELECT " +
                    "    rc.classify_name,rc.classify_code," +
                    "    COUNT(rc.classify_id) as index_count " +
                    "FROM report_index ri " +
                    "JOIN report_classify rc ON rc.classify_id = ri.classify_id " +
                    "where ri.is_query ='0' " +
                    " GROUP BY rc.classify_id, rc.classify_name,rc.classify_code ";

            log.info("+++++++++++++indexCountSql: {}",indexCountSql);
            List<AutoReportIndexParam> autoReportCountList = autoReportCountMapper.getAutoReportCountMap(indexCountSql);

            int zbJcsj =0;
            int zbKdkls =0;
            int zbGzbt =0;
            int zbLlsg =0;
            int zbRhcg =0;
            int zbYdkls =0;

            if(autoReportCountList != null && autoReportCountList.size() > 0){
                for(AutoReportIndexParam autoReportIndexParam: autoReportCountList){
                    String classifyName = autoReportIndexParam.getClassifyName();
                    String classifyCode = autoReportIndexParam.getClassifyCode();

                    int indexCount = autoReportIndexParam.getIndexCount();
                    /*if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("基础数据")){
                        zbJcsj = indexCount;
                    }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("宽带控流失")){
                        zbKdkls = indexCount;
                    }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("高值保拓")){
                        zbGzbt =indexCount;
                    }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("流量深耕")){
                        zbLlsg =indexCount;
                    }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("融合重耕")){
                        zbRhcg =indexCount;
                    }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("移动控流失")){
                        zbYdkls =indexCount;
                    }*/

                    if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("jcsj")){ //基础数据
                        zbJcsj = indexCount;
                    }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("kls")){ //宽带控流失
                        zbKdkls = indexCount;
                    }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("gztb")){ //高值拓保
                        zbGzbt =indexCount;
                    }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("llsg")){ //流量深耕
                        zbLlsg =indexCount;
                    }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("rhcg")){ //融合重耕
                        zbRhcg =indexCount;
                    }
                    if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("ydkls")){ //移动控流失
                        zbYdkls =indexCount;
                    }

                }
            }

            autoReportCount.setZbJcsj(zbJcsj);
            autoReportCount.setZbKdkls(zbKdkls);
            autoReportCount.setZbGzbt(zbGzbt);
            autoReportCount.setZbLlsg(zbLlsg);
            autoReportCount.setZbRhcg(zbRhcg);
            autoReportCount.setZbYdkls(zbYdkls);

        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCount;
    }


    /**
     * 获取自助取数中指标部分信息
     * @param startDate
     * @param endDate
     * @return
     */
    public List<AutoReportCount> getAutoReportSubIndexCount(List<AutoReportCount> autoReportCountList,String startDate,String endDate){
        try {
            startDate = startDate+" 00:00:00";
            endDate = endDate+" 23:59:59";
            /*String indexSubCountSql ="SELECT " +
                    "    rc.classify_name, " +
                    "    COUNT(rc.classify_id) as index_count " +
                    "FROM report_index ri " +
                    "JOIN report_classify rc ON rc.classify_id = ri.classify_id " +
                    "where ri.create_time >='"+startDate+"' and ri.create_time <='"+endDate+"' " +
                    "and rc.create_time >='"+startDate+"' and rc.create_time <='"+endDate+"' " +
                    "and ri.is_query ='0' "+
                    "GROUP BY rc.classify_id, rc.classify_name ";*/

            String indexSubCountSql ="SELECT " +
                    "    rc.classify_name, " +
                    "    COUNT(rc.classify_id) as index_count " +
                    "FROM report_index ri " +
                    "JOIN report_classify rc ON rc.classify_id = ri.classify_id " +
                    "where  ri.is_query ='0' " +
                    " GROUP BY rc.classify_id, rc.classify_name ";

            log.info("+++++++++++++indexSubCountSql: {}",indexSubCountSql);
            List<AutoReportIndexParam> autoReportIndexParamList = autoReportCountMapper.getAutoReportCountMap(indexSubCountSql);

            int zbJcsj =0;
            int zbKdkls =0;
            int zbGzbt =0;
            int zbLlsg =0;
            int zbRhcg =0;
            int zbYdkls =0;

            if(autoReportIndexParamList != null && autoReportIndexParamList.size() > 0){
                for(AutoReportIndexParam autoReportIndexParam: autoReportIndexParamList){
                    String classifyName = autoReportIndexParam.getClassifyName();
                    int indexCount = autoReportIndexParam.getIndexCount();
                    if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("基础数据")){
                        zbJcsj = indexCount;
                    }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("宽带控流失")){
                        zbKdkls = indexCount;
                    }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("高值拓保")){
                        zbGzbt =indexCount;
                    }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("流量深耕")){
                        zbLlsg =indexCount;
                    }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("融合重耕")){
                        zbRhcg =indexCount;
                    }
                    if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("移动控流失")){
                        zbYdkls =indexCount;
                    }
                }
            }

            if(autoReportCountList != null && autoReportCountList.size() >0){
                for(AutoReportCount autoReportCount: autoReportCountList){
                    autoReportCount.setZbJcsj(zbJcsj);
                    autoReportCount.setZbKdkls(zbKdkls);
                    autoReportCount.setZbGzbt(zbGzbt);
                    autoReportCount.setZbLlsg(zbLlsg);
                    autoReportCount.setZbRhcg(zbRhcg);
                    autoReportCount.setZbYdkls(zbYdkls);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCountList;
    }


    /**
     * 获取自助取数中生成推送报表统计信息
     * @param deptId
     * @param startDate
     * @param endDate
     * @return
     */
    public AutoReportCount getAutoReportCreatPushReportCount(AutoReportCount autoReportCount,String deptId,String startDate,String endDate){
        try {
            startDate = startDate+" 00:00:00";
            endDate = endDate+" 23:59:59";
            String reportCountSql ="";
            if(StringUtil.isNotEmpty(deptId)){
                /*reportCountSql = "select " +
                        " racl.index_id as index_ids " +
                        " from report_auto_create_log racl " +
                        " where 1=1 and racl.create_user_id in( " +
                        "  select su.user_id from sys_user su where su.dept_id in ( " +
                        "   SELECT au.dept_id " +
                        "  FROM (SELECT * FROM sys_dept WHERE parent_id IS NOT NULL) au, " +
                        "       (SELECT @pid := '"+deptId+"') pd " +
                        " WHERE FIND_IN_SET(parent_id, @pid) > 0 " +
                        "   AND @pid := concat(@pid, ',', dept_id) " +
                        "UNION " +
                        "SELECT dept_id " +
                        "  FROM sys_dept sd " +
                        " WHERE dept_id = '"+deptId+"' " +
                        "  ) " +
                        " ) " +
                        " and racl.create_time >='"+startDate+"' " +
                        " and racl.create_time <='"+endDate+"' " +
                        " and racl.index_id is not null and racl.index_id != ''";*/
                reportCountSql ="select  racl.index_id as index_ids " +
                        "from report_auto_create_log racl  where 1=1 " +
                        "and racl.create_user_id in(" +
                        " select su.user_id from sys_user su where su.dept_id ='"+deptId+"' " +
                        " )  and racl.create_time >='"+startDate+"'  and racl.create_time <='"+endDate+"'  and racl.index_id is not null and racl.index_id != ''";
            }else{
                reportCountSql = "select racl.index_id as index_ids from report_auto_create_log racl " +
                        " where 1=1  and racl.create_time >='"+startDate+"' " +
                        " and racl.create_time <='"+endDate+"' and racl.index_id is not null and racl.index_id != ''";
            }

            log.info("+++++++++++++reportCountSql: {}",reportCountSql);

            int scbbJcsj =0;
            int scbbKdkls =0;
            int scbbGzbt =0;
            int scbbLlsg =0;
            int scbbRhcg =0;
            int scbbYdkls =0;

            List<AutoReportIndexParam> autoReportCountList = autoReportCountMapper.getAutoReportCountMap(reportCountSql);
            if(autoReportCountList != null && autoReportCountList.size() > 0){
                for(AutoReportIndexParam autoReportIndexParam: autoReportCountList){
                    String indexIds = autoReportIndexParam.getIndexIds();

                    String queryIndexIds = indexIds.replaceAll(";",",");

                    String indexClassSql = "select rc.classify_name as classify_name, rc.classify_code as classify_code from report_classify rc where rc.classify_id in ( " +
                            "  select ri.classify_id from report_index ri where ri.index_id in ("+queryIndexIds+") " +
                            ")";

                    log.info("+++++++++++++indexClassSql: {}",indexClassSql);
                    List<AutoReportIndexParam> indexClassList = autoReportCountMapper.getAutoReportCountMap(indexClassSql);
                    if(indexClassList != null && indexClassList.size() > 0){
                        for(AutoReportIndexParam indexClassAutoReportIndexParam: indexClassList){
                            /*String classifyName = indexClassAutoReportIndexParam.getClassifyName();
                            if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("基础数据")){
                                scbbJcsj = scbbJcsj+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("宽带控流失")){
                                scbbKdkls = scbbKdkls+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("高值保拓")){
                                scbbGzbt =scbbGzbt+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("流量深耕")){
                                scbbLlsg =scbbLlsg+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("融合重耕")){
                                scbbRhcg =scbbRhcg+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("移动控流失")){
                                scbbYdkls =scbbYdkls+1;
                            }*/

                            String classifyCode = indexClassAutoReportIndexParam.getClassifyCode();
                            if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("jcsj")){ //基础数据
                                scbbJcsj = scbbJcsj+1;
                            }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("kls")){ //宽带控流失
                                scbbKdkls = scbbKdkls+1;
                            }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("gztb")){ //高值拓保
                                scbbGzbt =scbbGzbt+1;
                            }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("llsg")){ //流量深耕
                                scbbLlsg =scbbLlsg+1;
                            }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("rhcg")){ //融合重耕
                                scbbRhcg =scbbRhcg+1;
                            }
                            if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("ydkls")){ //移动控流失
                                scbbYdkls =scbbYdkls+1;
                            }
                        }
                    }
                }
            }

            autoReportCount.setScbbJcsj(scbbJcsj);
            autoReportCount.setScbbKdkls(scbbKdkls);
            autoReportCount.setScbbGzbt(scbbGzbt);
            autoReportCount.setScbbLlsg(scbbLlsg);
            autoReportCount.setScbbRhcg(scbbRhcg);
            autoReportCount.setScbbYdkls(scbbYdkls);

            autoReportCount.setZdtsbbJcsj(scbbJcsj);
            autoReportCount.setZdtsbbKdkls(scbbKdkls);
            autoReportCount.setZdtsbbGzbt(scbbGzbt);
            autoReportCount.setZdtsbbLlsg(scbbLlsg);
            autoReportCount.setZdtsbbRhcg(scbbRhcg);
            autoReportCount.setZdtsbbYdkls(scbbYdkls);

        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCount;
    }


    /**
     * 获取自助取数中生成推送报表统计信息
     * @param deptId
     * @param startDate
     * @param endDate
     * @return
     */
    public List<AutoReportCount> getAutoReportSubCreatPushReportCount(List<AutoReportCount> autoReportCountList,String deptId,String startDate,String endDate){
        try {
            startDate = startDate+" 00:00:00";
            endDate = endDate+" 23:59:59";
            String reportSubCountSql ="";
            if(StringUtil.isNotEmpty(deptId)){
                reportSubCountSql = "select  racl.index_id as index_ids,sd.dept_name area_name " +
                        " from report_auto_create_log racl,sys_user su,sys_dept sd " +
                        " where 1=1 " +
                        " and su.user_id =racl.create_user_id " +
                        " and su.dept_id = sd.dept_id " +
                        " and racl.create_user_id in( " +
                        "  select su.user_id from sys_user su where su.dept_id in ( " +
                        "    select sd2.dept_id from sys_dept sd2 where sd2.parent_id ='"+deptId+"' " +
                        "   ) ) " +
                        "   and racl.create_time >='"+startDate+"'  and racl.create_time <='"+endDate+"' " +
                        "   and racl.index_id is not null and racl.index_id != ''";
            }

            log.info("+++++++++++++reportSubCountSql: {}",reportSubCountSql);


            List<AutoReportIndexParam> autoReportIndexParamList = autoReportCountMapper.getAutoReportCountMap(reportSubCountSql);
            if(autoReportIndexParamList != null && autoReportIndexParamList.size() > 0){
                for(AutoReportIndexParam autoReportIndexParam: autoReportIndexParamList){

                    int scbbJcsj =0;
                    int scbbKdkls =0;
                    int scbbGzbt =0;
                    int scbbLlsg =0;
                    int scbbRhcg =0;
                    int scbbYdkls =0;

                    String indexIds = autoReportIndexParam.getIndexIds();

                    String queryIndexIds = indexIds.replaceAll(";",",");

                    String areaNameParam = autoReportIndexParam.getAreaName();

                    String indexClassSql = "select rc.classify_name as classify_name, rc.classify_code as classify_code from report_classify rc where rc.classify_id in ( " +
                            "  select ri.classify_id from report_index ri where ri.index_id in ("+queryIndexIds+") " +
                            ")";

                    log.info("+++++++++++++indexClassSql: {}",indexClassSql);
                    List<AutoReportIndexParam> indexClassList = autoReportCountMapper.getAutoReportCountMap(indexClassSql);
                    if(indexClassList != null && indexClassList.size() > 0){
                        for(AutoReportIndexParam indexClassAutoReportIndexParam: indexClassList){
                            /*String classifyName = indexClassAutoReportIndexParam.getClassifyName();
                            if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("基础数据")){
                                scbbJcsj = scbbJcsj+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("宽带控流失")){
                                scbbKdkls = scbbKdkls+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("高值保拓")){
                                scbbGzbt =scbbGzbt+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("流量深耕")){
                                scbbLlsg =scbbLlsg+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("融合重耕")){
                                scbbRhcg =scbbRhcg+1;
                            }else if(StringUtil.isNotEmpty(classifyName) && classifyName.contains("移动控流失")){
                                scbbYdkls =scbbYdkls+1;
                            }*/

                            String classifyCode = indexClassAutoReportIndexParam.getClassifyCode();
                            if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("jcsj")){ //基础数据
                                scbbJcsj = scbbJcsj+1;
                            }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("kls")){ //宽带控流失
                                scbbKdkls = scbbKdkls+1;
                            }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("gztb")){ //高值拓保
                                scbbGzbt =scbbGzbt+1;
                            }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("llsg")){ //流量深耕
                                scbbLlsg =scbbLlsg+1;
                            }else if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("rhcg")){ //融合重耕
                                scbbRhcg =scbbRhcg+1;
                            }
                            if(StringUtil.isNotEmpty(classifyCode) && classifyCode.contains("ydkls")){ //移动控流失
                                scbbYdkls =scbbYdkls+1;
                            }

                        }
                    }

                    for(AutoReportCount autoReportCount: autoReportCountList){
                        String areaName = autoReportCount.getAreaName();
                        if(areaNameParam.equals(areaName)){
                            if(scbbJcsj >0){
                                autoReportCount.setScbbJcsj(autoReportCount.getScbbJcsj()+1);
                                autoReportCount.setZdtsbbJcsj(autoReportCount.getScbbJcsj()+1);
                            }
                            if(scbbKdkls >0){
                                autoReportCount.setScbbKdkls(autoReportCount.getScbbKdkls()+1);
                                autoReportCount.setZdtsbbKdkls(autoReportCount.getScbbKdkls()+1);
                            }
                            if(scbbGzbt >0){
                                autoReportCount.setScbbGzbt(autoReportCount.getScbbGzbt()+1);
                                autoReportCount.setZdtsbbGzbt(autoReportCount.getScbbGzbt()+1);
                            }
                            if(scbbLlsg >0){
                                autoReportCount.setScbbLlsg(autoReportCount.getScbbLlsg()+1);
                                autoReportCount.setZdtsbbLlsg(autoReportCount.getScbbLlsg()+1);
                            }
                            if(scbbRhcg >0){
                                autoReportCount.setScbbRhcg(autoReportCount.getScbbRhcg()+1);
                                autoReportCount.setZdtsbbRhcg(autoReportCount.getScbbRhcg()+1);
                            }
                            if(scbbYdkls >0){
                                autoReportCount.setScbbYdkls(autoReportCount.getScbbYdkls()+1);
                                autoReportCount.setZdtsbbYdkls(autoReportCount.getScbbYdkls()+1);
                            }
                        }
                    }
                }
            }

            /*autoReportCount.setScbbJcsj(scbbJcsj);
            autoReportCount.setScbbKdkls(scbbKdkls);
            autoReportCount.setScbbGzbt(scbbGzbt);
            autoReportCount.setScbbLlsg(scbbLlsg);
            autoReportCount.setScbbRhcg(scbbRhcg);
            autoReportCount.setScbbYdkls(scbbYdkls);

            autoReportCount.setZdtsbbJcsj(scbbJcsj);
            autoReportCount.setZdtsbbKdkls(scbbKdkls);
            autoReportCount.setZdtsbbGzbt(scbbGzbt);
            autoReportCount.setZdtsbbLlsg(scbbLlsg);
            autoReportCount.setZdtsbbRhcg(scbbRhcg);
            autoReportCount.setZdtsbbYdkls(scbbYdkls);*/

        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCountList;
    }


    /**
     * 获取自助取数中预警处置统计信息
     * @param deptName
     * @param startDate
     * @param endDate
     * @return
     */
    public AutoReportCount getAutoReportWarningCount(AutoReportCount autoReportCount,String deptName,String startDate,String endDate){
        try {
            startDate = startDate+" 00:00:00";
            endDate = endDate+" 23:59:59";
            String warningCountSql ="";
            if(StringUtil.isNotEmpty(deptName)){
                /*warningCountSql = "SELECT " +
                        "  COUNT(*) AS warning_index_count, " +
                        "  SUM(CASE WHEN warning_level = 'urgent' THEN 1 ELSE 0 END) AS warning_index_urgent_count," +
                        "  SUM(CASE WHEN warning_level = 'alarm' THEN 1 ELSE 0 END) AS warning_index_alarm_count," +
                        "  SUM(CASE WHEN warning_level = 'ordinary' THEN 1 ELSE 0 END) AS warning_index_ordinary_count," +
                        "  (SELECT COUNT(*) FROM report_index_warning_log WHERE local_city in ( " +
                        "    SELECT au.dept_name " +
                        "  FROM (SELECT * FROM sys_dept WHERE parent_id IS NOT NULL) au, " +
                        "       (SELECT @deptname := '"+deptName+"') pd " +
                        " WHERE FIND_IN_SET(parent_id, @pid) > 0 " +
                        "   AND @pid := concat(@pid, ',', dept_id) " +
                        " UNION " +
                        "SELECT  dept_name " +
                        "  FROM sys_dept sd " +
                        " WHERE dept_name = '"+deptName+"' " +
                        "   )  AND create_time >= '"+startDate+"'  AND create_time <= '"+endDate+"') AS warning_total_count " +
                        "  FROM report_index_warning WHERE 1=1 and local_city in ( " +
                        "   SELECT au.dept_name " +
                        "  FROM (SELECT * FROM sys_dept WHERE parent_id IS NOT NULL) au, " +
                        "       (SELECT @deptname := '"+deptName+"') pd " +
                        " WHERE FIND_IN_SET(parent_id, @pid) > 0 " +
                        "   AND @pid := concat(@pid, ',', dept_id) " +
                        "UNION " +
                        "SELECT  dept_name " +
                        "  FROM sys_dept sd " +
                        " WHERE dept_name = '"+deptName+"')";*/
                warningCountSql ="SELECT COUNT(*) AS warning_index_count, " +
                        "SUM(CASE WHEN warning_level = 'urgent' THEN 1 ELSE 0 END) AS warning_index_urgent_count, " +
                        "SUM(CASE WHEN warning_level = 'alarm' THEN 1 ELSE 0 END) AS warning_index_alarm_count, " +
                        "SUM(CASE WHEN warning_level = 'ordinary' THEN 1 ELSE 0 END) AS warning_index_ordinary_count, " +
                        "(SELECT COUNT(*) FROM report_index_warning_log WHERE local_city ='"+deptName+"'  AND create_time >= '"+startDate+"'  AND create_time <= '"+endDate+"') AS warning_total_count " +
                        " FROM report_index_warning WHERE 1=1 and local_city ='"+deptName+"'";
            }else{
                warningCountSql = "SELECT " +
                        "    COUNT(*) AS warning_index_count, " +
                        "    SUM(CASE WHEN warning_level = 'urgent' THEN 1 ELSE 0 END) AS warning_index_urgent_count, " +
                        "    SUM(CASE WHEN warning_level = 'alarm' THEN 1 ELSE 0 END) AS warning_index_alarm_count, " +
                        "    SUM(CASE WHEN warning_level = 'ordinary' THEN 1 ELSE 0 END) AS warning_index_ordinary_count, " +
                        "    (SELECT COUNT(*) " +
                        "     FROM report_index_warning_log " +
                        "     WHERE 1=1 " +
                        "       AND create_time >= '"+startDate+"' " +
                        "       AND create_time <= '"+endDate+"') AS warning_total_count " +
                        "FROM report_index_warning ";
            }

            log.info("+++++++++++++warningCountSql: {}",warningCountSql);

            int warningIndexCount =0;
            int warningIndexUrgentCount =0;
            int warningIndexAlarmCount =0;
            int warningIndexOrdinaryCount =0;
            int warningTotalCount =0;

            List<AutoReportIndexParam> autoReportCountList = autoReportCountMapper.getAutoReportCountMap(warningCountSql);
            if(autoReportCountList != null && autoReportCountList.size() > 0){
                for(AutoReportIndexParam autoReportIndexParam: autoReportCountList){
                    warningIndexCount = autoReportIndexParam.getWarningIndexCount();
                    warningIndexUrgentCount = autoReportIndexParam.getWarningIndexUrgentCount();
                    warningIndexAlarmCount = autoReportIndexParam.getWarningIndexAlarmCount();
                    warningIndexOrdinaryCount = autoReportIndexParam.getWarningIndexOrdinaryCount();
                    warningTotalCount = autoReportIndexParam.getWarningTotalCount();
                }
            }

            autoReportCount.setWarningIndexCount(warningIndexCount);
            autoReportCount.setWarningIndexUrgentCount(warningIndexUrgentCount);
            autoReportCount.setWarningIndexAlarmCount(warningIndexAlarmCount);
            autoReportCount.setWarningIndexOrdinaryCount(warningIndexOrdinaryCount);
            autoReportCount.setWarningTotalCount(warningTotalCount);

        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCount;
    }


    /**
     * 获取自助取数中预警处置统计信息
     * @param deptName
     * @param startDate
     * @param endDate
     * @return
     */
    public List<AutoReportCount> getAutoReportSubWarningCount(List<AutoReportCount> autoReportCountList,String deptName,String startDate,String endDate){
        try {
            startDate = startDate+" 00:00:00";
            endDate = endDate+" 23:59:59";
            String warningSubCountSql ="";
            if(StringUtil.isNotEmpty(deptName)){
                if(deptName.equals("全疆")){
                    deptName = "新疆分公司";
                }

                warningSubCountSql = "SELECT " +
                        "  riw.local_city area_name, " +
                        "  COUNT(*) AS warning_index_count," +
                        "  SUM(CASE WHEN riw.warning_level = 'urgent' THEN 1 ELSE 0 END) AS warning_index_urgent_count," +
                        "  SUM(CASE WHEN riw.warning_level = 'alarm' THEN 1 ELSE 0 END) AS warning_index_alarm_count," +
                        "  SUM(CASE WHEN riw.warning_level = 'ordinary' THEN 1 ELSE 0 END) AS warning_index_ordinary_count," +
                        "  total.warning_total_count " +
                        "FROM " +
                        "  report_index_warning riw " +
                        "  CROSS JOIN (" +
                        "    SELECT COUNT(*) AS warning_total_count " +
                        "    FROM report_index_warning_log rlog " +
                        "    WHERE rlog.local_city IN ( " +
                        "      SELECT sd.dept_name " +
                        "      FROM sys_dept sd " +
                        "      WHERE sd.parent_id IN ( " +
                        "        SELECT sd3.dept_id  FROM sys_dept sd3  WHERE sd3.dept_name = '"+deptName+"' " +
                        "      ) " +
                        "    ) " +
                        "    AND rlog.create_time >= '"+startDate+"' " +
                        "    AND rlog.create_time <= '"+endDate+"' " +
                        "  ) AS total " +
                        "WHERE " +
                        "  riw.local_city IN ( " +
                        "    SELECT sd.dept_name " +
                        "    FROM sys_dept sd " +
                        "    WHERE sd.parent_id IN ( " +
                        "      SELECT sd3.dept_id  FROM sys_dept sd3  WHERE sd3.dept_name = '"+deptName+"' " +
                        "    ) " +
                        "  ) " +
                        "GROUP BY" +
                        "  riw.local_city, total.warning_total_count";
            }

            log.info("+++++++++++++warningSubCountSql: {}",warningSubCountSql);



            List<AutoReportIndexParam> autoReportIndexParamList = autoReportCountMapper.getAutoReportCountMap(warningSubCountSql);
            if(autoReportIndexParamList != null && autoReportIndexParamList.size() > 0){
                for(AutoReportIndexParam autoReportIndexParam: autoReportIndexParamList){
                    int warningIndexCount =0;
                    int warningIndexUrgentCount =0;
                    int warningIndexAlarmCount =0;
                    int warningIndexOrdinaryCount =0;
                    int warningTotalCount =0;
                    warningIndexCount = autoReportIndexParam.getWarningIndexCount();
                    warningIndexUrgentCount = autoReportIndexParam.getWarningIndexUrgentCount();
                    warningIndexAlarmCount = autoReportIndexParam.getWarningIndexAlarmCount();
                    warningIndexOrdinaryCount = autoReportIndexParam.getWarningIndexOrdinaryCount();
                    warningTotalCount = autoReportIndexParam.getWarningTotalCount();

                    String areaNameParam = autoReportIndexParam.getAreaName()==null?"":autoReportIndexParam.getAreaName();

                    for(AutoReportCount autoReportCount : autoReportCountList){
                        String areaName = autoReportCount.getAreaName() ==null?"":autoReportCount.getAreaName();
                        if(areaName.equals(areaNameParam)){
                            autoReportCount.setWarningIndexCount(warningIndexCount);
                            autoReportCount.setWarningIndexUrgentCount(warningIndexUrgentCount);
                            autoReportCount.setWarningIndexAlarmCount(warningIndexAlarmCount);
                            autoReportCount.setWarningIndexOrdinaryCount(warningIndexOrdinaryCount);
                            autoReportCount.setWarningTotalCount(warningTotalCount);
                        }
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return autoReportCountList;
    }


    /**
     * 生成文件
     * @return
     */
    public String creatFile(List<AutoReportCount> autoReportCountList,List<AutoReportCount> autoReportSubCountList,String area){
        String retStr ="";
        try {

            // 创建工作簿和工作表
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("自助取数统计");

            // 创建居中样式，用于美观显示

            CellStyle style = createBorderedStyle(workbook, true);
            CellStyle contentStyle = createBorderedStyle(workbook, false);


            // -----------------------------
            // 示例1：合并列（同一行，多个列）
            // 合并 A1 到 C1
            // -----------------------------
            Row row1 = sheet.createRow(0);
            Cell hxmkCell = row1.createCell(0);
            hxmkCell.setCellValue("核心模块");
            hxmkCell.setCellStyle(style);

            Cell yhfgCell = row1.createCell(1);
            yhfgCell.setCellValue("用户覆盖");
            yhfgCell.setCellStyle(style);


            row1.createCell(3).setCellStyle(style);

            Row row2 = sheet.createRow(1);
            Cell tjwdCel = row2.createCell(0);
            tjwdCel.setCellValue("统计维度");
            tjwdCel.setCellStyle(style);


            row2.createCell(3).setCellStyle(style);

            sheet.addMergedRegion(new CellRangeAddress(0, 1, 1, 3)); // 第1行，A-C列

            Cell zbgsCel = row1.createCell(4);
            zbgsCel.setCellValue("指标个数");
            zbgsCel.setCellStyle(style);

            row2.createCell(9).setCellStyle(style);

            sheet.addMergedRegion(new CellRangeAddress(0, 1, 4, 9)); // 第1行，A-C列

            Cell scbbsCell = row1.createCell(10);
            scbbsCell.setCellValue("生成报表数");
            scbbsCell.setCellStyle(style);

            row2.createCell(15).setCellStyle(style);

            sheet.addMergedRegion(new CellRangeAddress(0, 1, 10, 15)); // 第1行，A-C列

            Cell zdtsbbsCel = row1.createCell(16);
            zdtsbbsCel.setCellValue("自动推送报表数");
            zdtsbbsCel.setCellStyle(style);

            row2.createCell(21).setCellStyle(style);

            sheet.addMergedRegion(new CellRangeAddress(0, 1, 16, 21)); // 第1行，A-C列

            Cell yjczCel = row1.createCell(22);
            yjczCel.setCellValue("预警处置");
            yjczCel.setCellStyle(style);

            row1.createCell(26).setCellStyle(style);
            row2.createCell(26).setCellStyle(style);

            sheet.addMergedRegion(new CellRangeAddress(0, 1, 22, 26)); // 第1行，A-C列


            Row row3 = sheet.createRow(2);
            Cell ywlxCel = row3.createCell(0);
            ywlxCel.setCellValue("业务类型");
            ywlxCel.setCellStyle(style);

            Cell fwyhsCel = row3.createCell(1);
            fwyhsCel.setCellValue("服务用户数(人)");
            fwyhsCel.setCellStyle(style);

            Cell hyyhsCel = row3.createCell(2);
            hyyhsCel.setCellValue("活跃用户数(人)");
            hyyhsCel.setCellStyle(style);

            Cell yhhlvCel = row3.createCell(3);
            yhhlvCel.setCellValue("用户活跃率");
            yhhlvCel.setCellStyle(style);

            Cell zbJcsjCel = row3.createCell(4);
            zbJcsjCel.setCellValue("基础数据(个)");
            zbJcsjCel.setCellStyle(style);

            Cell zbKdklsCel = row3.createCell(5);
            zbKdklsCel.setCellValue("宽带控流失(个)");
            zbKdklsCel.setCellStyle(style);

            Cell zbGzbtCel = row3.createCell(6);
            zbGzbtCel.setCellValue("高值拓保(个)");
            zbGzbtCel.setCellStyle(style);

            Cell zbLlsgCel = row3.createCell(7);
            zbLlsgCel.setCellValue("流量深耕(个)");
            zbLlsgCel.setCellStyle(style);

            Cell zbRhcgCel = row3.createCell(8);
            zbRhcgCel.setCellValue("融合重耕(个)");
            zbRhcgCel.setCellStyle(style);

            Cell zbYdklsCel = row3.createCell(9);
            zbYdklsCel.setCellValue("移动控流失(个)");
            zbYdklsCel.setCellStyle(style);

            Cell scbbsJcsjCel = row3.createCell(10);
            scbbsJcsjCel.setCellValue("基础数据(份)");
            scbbsJcsjCel.setCellStyle(style);

            Cell scbbsKdklsCel = row3.createCell(11);
            scbbsKdklsCel.setCellValue("宽带控流失(份)");
            scbbsKdklsCel.setCellStyle(style);

            Cell scbbsGzbtCel = row3.createCell(12);
            scbbsGzbtCel.setCellValue("高值拓保(份)");
            scbbsGzbtCel.setCellStyle(style);

            Cell scbbsLlsgCel = row3.createCell(13);
            scbbsLlsgCel.setCellValue("流量深耕(份)");
            scbbsLlsgCel.setCellStyle(style);

            Cell scbbsRhcgCel = row3.createCell(14);
            scbbsRhcgCel.setCellValue("融合重耕(份)");
            scbbsRhcgCel.setCellStyle(style);

            Cell scbbsYdklsCel = row3.createCell(15);
            scbbsYdklsCel.setCellValue("移动控流失(份)");
            scbbsYdklsCel.setCellStyle(style);

            Cell zdtsbbsJcsjCel = row3.createCell(16);
            zdtsbbsJcsjCel.setCellValue("基础数据(份)");
            zdtsbbsJcsjCel.setCellStyle(style);

            Cell zdtsbbsKdklsCel = row3.createCell(17);
            zdtsbbsKdklsCel.setCellValue("宽带控流失(份)");
            zdtsbbsKdklsCel.setCellStyle(style);

            Cell zdtsbbsGzbtCel = row3.createCell(18);
            zdtsbbsGzbtCel.setCellValue("高值拓保(份)");
            zdtsbbsGzbtCel.setCellStyle(style);

            Cell zdtsbbsLlsgCel = row3.createCell(19);
            zdtsbbsLlsgCel.setCellValue("流量深耕(份)");
            zdtsbbsLlsgCel.setCellStyle(style);

            Cell zdtsbbsRhcgCel = row3.createCell(20);
            zdtsbbsRhcgCel.setCellValue("融合重耕(份)");
            zdtsbbsRhcgCel.setCellStyle(style);

            Cell zdtsbbsYdklsCel = row3.createCell(21);
            zdtsbbsYdklsCel.setCellValue("移动控流失(份)");
            zdtsbbsYdklsCel.setCellStyle(style);

            Cell yjczYjzbzsCel = row3.createCell(22);
            yjczYjzbzsCel.setCellValue("预警指标总数(个)");
            yjczYjzbzsCel.setCellStyle(style);

            Cell yjczYjzbzsJjCel = row3.createCell(23);
            yjczYjzbzsJjCel.setCellValue("预警指标数-紧急(个)");
            yjczYjzbzsJjCel.setCellStyle(style);

            Cell yjczYjzbzsGjCel = row3.createCell(24);
            yjczYjzbzsGjCel.setCellValue("预警指标数-告警(个)");
            yjczYjzbzsGjCel.setCellStyle(style);

            Cell yjczYjzbzsPtCel = row3.createCell(25);
            yjczYjzbzsPtCel.setCellValue("预警指标数-普通(个)");
            yjczYjzbzsPtCel.setCellStyle(style);

            Cell yjczYjzcsCel = row3.createCell(26);
            yjczYjzcsCel.setCellValue("预警总次数(次)");
            yjczYjzcsCel.setCellStyle(style);


            Row row4 = sheet.createRow(3);
            Cell slCel = row4.createCell(0);
            slCel.setCellValue("数量");
            slCel.setCellStyle(contentStyle);

            //数据
            if(autoReportCountList != null && autoReportCountList.size() >0){
                AutoReportCount autoReportCount = autoReportCountList.get(0);

                Cell fwyhrwValueCel = row4.createCell(1);
                fwyhrwValueCel.setCellValue(autoReportCount.getYhFwyhs());
                fwyhrwValueCel.setCellStyle(contentStyle);

                Cell hyyhsValueCel = row4.createCell(2);
                hyyhsValueCel.setCellValue(autoReportCount.getYhHyyhs());
                hyyhsValueCel.setCellStyle(contentStyle);

                Cell yhhylvValueCel = row4.createCell(3);
                yhhylvValueCel.setCellValue(autoReportCount.getYhYhhylv());
                yhhylvValueCel.setCellStyle(contentStyle);

                Cell zbJcsjValueCel = row4.createCell(4);
                zbJcsjValueCel.setCellValue(autoReportCount.getZbJcsj());
                zbJcsjValueCel.setCellStyle(contentStyle);

                Cell zbKdklsValueCel = row4.createCell(5);
                zbKdklsValueCel.setCellValue(autoReportCount.getZbKdkls());
                zbKdklsValueCel.setCellStyle(contentStyle);

                Cell zbGzbtValueCel = row4.createCell(6);
                zbGzbtValueCel.setCellValue(autoReportCount.getZbGzbt());
                zbGzbtValueCel.setCellStyle(contentStyle);

                Cell zbLlsgValueCel = row4.createCell(7);
                zbLlsgValueCel.setCellValue(autoReportCount.getZbLlsg());
                zbLlsgValueCel.setCellStyle(contentStyle);

                Cell zbRhcgValueCel = row4.createCell(8);
                zbRhcgValueCel.setCellValue(autoReportCount.getZbRhcg());
                zbRhcgValueCel.setCellStyle(contentStyle);

                Cell zbYdklsValueCel = row4.createCell(9);
                zbYdklsValueCel.setCellValue(autoReportCount.getZbYdkls());
                zbYdklsValueCel.setCellStyle(contentStyle);

                Cell scbbsJcsjValueCel = row4.createCell(10);
                scbbsJcsjValueCel.setCellValue(autoReportCount.getScbbJcsj());
                scbbsJcsjValueCel.setCellStyle(contentStyle);

                Cell scbbsKdklsValueCel = row4.createCell(11);
                scbbsKdklsValueCel.setCellValue(autoReportCount.getScbbKdkls());
                scbbsKdklsValueCel.setCellStyle(contentStyle);

                Cell scbbsGzbtValueCel = row4.createCell(12);
                scbbsGzbtValueCel.setCellValue(autoReportCount.getScbbGzbt());
                scbbsGzbtValueCel.setCellStyle(contentStyle);

                Cell scbbsLlsgValueCel = row4.createCell(13);
                scbbsLlsgValueCel.setCellValue(autoReportCount.getScbbLlsg());
                scbbsLlsgValueCel.setCellStyle(contentStyle);

                Cell scbbsRhcgValueCel = row4.createCell(14);
                scbbsRhcgValueCel.setCellValue(autoReportCount.getScbbRhcg());
                scbbsRhcgValueCel.setCellStyle(contentStyle);

                Cell scbbsYdklsValueCel = row4.createCell(15);
                scbbsYdklsValueCel.setCellValue(autoReportCount.getScbbYdkls());
                scbbsYdklsValueCel.setCellStyle(contentStyle);

                Cell zdtsbbJcsjValueCel = row4.createCell(16);
                zdtsbbJcsjValueCel.setCellValue(autoReportCount.getZdtsbbJcsj());
                zdtsbbJcsjValueCel.setCellStyle(contentStyle);

                Cell zdtsbbKdklsValueCel = row4.createCell(17);
                zdtsbbKdklsValueCel.setCellValue(autoReportCount.getZdtsbbKdkls());
                zdtsbbKdklsValueCel.setCellStyle(contentStyle);

                Cell zdtsbbGzbtValueCel = row4.createCell(18);
                zdtsbbGzbtValueCel.setCellValue(autoReportCount.getZdtsbbGzbt());
                zdtsbbGzbtValueCel.setCellStyle(contentStyle);

                Cell zdtsbbLlsgValueCel = row4.createCell(19);
                zdtsbbLlsgValueCel.setCellValue(autoReportCount.getZdtsbbLlsg());
                zdtsbbLlsgValueCel.setCellStyle(contentStyle);

                Cell zdtsbbRhcgValueCel = row4.createCell(20);
                zdtsbbRhcgValueCel.setCellValue(autoReportCount.getZdtsbbRhcg());
                zdtsbbRhcgValueCel.setCellStyle(contentStyle);

                Cell zdtsbbYdklsValueCel = row4.createCell(21);
                zdtsbbYdklsValueCel.setCellValue(autoReportCount.getZdtsbbYdkls());
                zdtsbbYdklsValueCel.setCellStyle(contentStyle);

                Cell yjczYjzbzsValueCel = row4.createCell(22);
                yjczYjzbzsValueCel.setCellValue(autoReportCount.getWarningIndexCount());
                yjczYjzbzsValueCel.setCellStyle(contentStyle);

                Cell yjczYjzbsJjValueCel = row4.createCell(23);
                yjczYjzbsJjValueCel.setCellValue(autoReportCount.getWarningIndexUrgentCount());
                yjczYjzbsJjValueCel.setCellStyle(contentStyle);

                Cell yjczYjzbsGjValueCel = row4.createCell(24);
                yjczYjzbsGjValueCel.setCellValue(autoReportCount.getWarningIndexAlarmCount());
                yjczYjzbsGjValueCel.setCellStyle(contentStyle);

                Cell yjczYjzbsPtValueCel = row4.createCell(25);
                yjczYjzbsPtValueCel.setCellValue(autoReportCount.getWarningIndexOrdinaryCount());
                yjczYjzbsPtValueCel.setCellStyle(contentStyle);

                Cell yjczYjzcsValueCel = row4.createCell(26);
                yjczYjzcsValueCel.setCellValue(autoReportCount.getWarningTotalCount());
                yjczYjzcsValueCel.setCellStyle(contentStyle);

            }

            //第二个表格
            Row row8 = sheet.createRow(8);
            Cell hxmkCellSub = row8.createCell(0);
            hxmkCellSub.setCellValue("核心模块");
            hxmkCellSub.setCellStyle(style);

            Cell yhfgCellSub = row8.createCell(1);
            yhfgCellSub.setCellValue("用户覆盖");
            yhfgCellSub.setCellStyle(style);

            Row row9 = sheet.createRow(9);
            Cell tjwdCelSub = row9.createCell(0);
            tjwdCelSub.setCellValue("统计维度");
            tjwdCelSub.setCellStyle(style);

            row8.createCell(2).setCellStyle(style);
            row8.createCell(3).setCellStyle(style);
            row9.createCell(2).setCellStyle(style);
            row9.createCell(3).setCellStyle(style);

            sheet.addMergedRegion(new CellRangeAddress(8, 9, 1, 3)); // 第1行，A-C列

            Cell zbgsCelSub = row8.createCell(4);
            zbgsCelSub.setCellValue("生成报表数");
            zbgsCelSub.setCellStyle(style);

            row8.createCell(5).setCellStyle(style);
            row8.createCell(6).setCellStyle(style);
            row8.createCell(7).setCellStyle(style);
            row8.createCell(8).setCellStyle(style);
            row8.createCell(9).setCellStyle(style);
            row9.createCell(5).setCellStyle(style);
            row9.createCell(6).setCellStyle(style);
            row9.createCell(7).setCellStyle(style);
            row9.createCell(8).setCellStyle(style);
            row9.createCell(9).setCellStyle(style);

            sheet.addMergedRegion(new CellRangeAddress(8, 9, 4, 9)); // 第1行，A-C列

            Cell scbbsCellSub = row8.createCell(10);
            scbbsCellSub.setCellValue("自动推送报表数");
            scbbsCellSub.setCellStyle(style);

            row8.createCell(11).setCellStyle(style);
            row8.createCell(12).setCellStyle(style);
            row8.createCell(13).setCellStyle(style);
            row8.createCell(14).setCellStyle(style);
            row8.createCell(15).setCellStyle(style);
            row9.createCell(11).setCellStyle(style);
            row9.createCell(12).setCellStyle(style);
            row9.createCell(13).setCellStyle(style);
            row9.createCell(14).setCellStyle(style);
            row9.createCell(15).setCellStyle(style);

            sheet.addMergedRegion(new CellRangeAddress(8, 9, 10, 15)); // 第1行，A-C列


            Cell zdtsbbsCelSub = row8.createCell(16);
            zdtsbbsCelSub.setCellValue("预警处置");
            zdtsbbsCelSub.setCellStyle(style);

            row8.createCell(17).setCellStyle(style);
            row8.createCell(18).setCellStyle(style);
            row8.createCell(19).setCellStyle(style);
            row8.createCell(20).setCellStyle(style);
            row9.createCell(17).setCellStyle(style);
            row9.createCell(18).setCellStyle(style);
            row9.createCell(19).setCellStyle(style);
            row9.createCell(20).setCellStyle(style);
            /*row9.createCell(21).setCellStyle(style);*/

            sheet.addMergedRegion(new CellRangeAddress(8, 9, 16, 20)); // 第1行，A-C列

            Row row10 = sheet.createRow(10);
            Cell mcCelSub = row10.createCell(0);
            mcCelSub.setCellValue("名称");
            mcCelSub.setCellStyle(style);

            //用户覆盖开始
            Cell fwyhsCelSub = row10.createCell(1);
            fwyhsCelSub.setCellValue("服务用户数(人)");
            fwyhsCelSub.setCellStyle(style);

            Cell hyyhsCelSub = row10.createCell(2);
            hyyhsCelSub.setCellValue("活跃用户数(人)");
            hyyhsCelSub.setCellStyle(style);

            Cell yhhlvCelSub = row10.createCell(3);
            yhhlvCelSub.setCellValue("用户活跃率");
            yhhlvCelSub.setCellStyle(style);
            //用户覆盖结束

            //生成报表数开始
            Cell zbJcsjCelSub = row10.createCell(4);
            zbJcsjCelSub.setCellValue("基础数据(份)");
            zbJcsjCelSub.setCellStyle(style);

            Cell zbKdklsCelSub = row10.createCell(5);
            zbKdklsCelSub.setCellValue("宽带控流失(份)");
            zbKdklsCelSub.setCellStyle(style);

            Cell zbGzbtCelSub = row10.createCell(6);
            zbGzbtCelSub.setCellValue("高值拓保(份)");
            zbGzbtCelSub.setCellStyle(style);

            Cell zbLlsgCelSub = row10.createCell(7);
            zbLlsgCelSub.setCellValue("流量深耕(份)");
            zbLlsgCelSub.setCellStyle(style);

            Cell zbRhcgCelSub = row10.createCell(8);
            zbRhcgCelSub.setCellValue("融合重耕(份)");
            zbRhcgCelSub.setCellStyle(style);

            Cell zbYdklsCelSub = row10.createCell(9);
            zbYdklsCelSub.setCellValue("移动控流失(份)");
            zbYdklsCelSub.setCellStyle(style);
            //生成报表数结束

            //自动推送报表数开始
            Cell scbbsJcsjCelSub = row10.createCell(10);
            scbbsJcsjCelSub.setCellValue("基础数据(份)");
            scbbsJcsjCelSub.setCellStyle(style);

            Cell scbbsKdklsCelSub = row10.createCell(11);
            scbbsKdklsCelSub.setCellValue("宽带控流失(份)");
            scbbsKdklsCelSub.setCellStyle(style);

            Cell scbbsGzbtCelSub = row10.createCell(12);
            scbbsGzbtCelSub.setCellValue("高值拓保(份)");
            scbbsGzbtCelSub.setCellStyle(style);

            Cell scbbsLlsgCelSub = row10.createCell(13);
            scbbsLlsgCelSub.setCellValue("流量深耕(份)");
            scbbsLlsgCelSub.setCellStyle(style);

            Cell scbbsRhcgCelSub = row10.createCell(14);
            scbbsRhcgCelSub.setCellValue("融合重耕(份)");
            scbbsRhcgCelSub.setCellStyle(style);

            Cell scbbsYdklsCelSub = row10.createCell(15);
            scbbsYdklsCelSub.setCellValue("移动控流失(份)");
            scbbsYdklsCelSub.setCellStyle(style);
            //自动推送报表数结束

            //预警处置开始
            Cell zdtsbbsJcsjCelSub = row10.createCell(16);
            zdtsbbsJcsjCelSub.setCellValue("预警指标总数(个)");
            zdtsbbsJcsjCelSub.setCellStyle(style);

            Cell zdtsbbsKdklsCelSub = row10.createCell(17);
            zdtsbbsKdklsCelSub.setCellValue("预警指标数-紧急(个)");
            zdtsbbsKdklsCelSub.setCellStyle(style);

            Cell zdtsbbsGzbtCelSub = row10.createCell(18);
            zdtsbbsGzbtCelSub.setCellValue("预警指标数-告警(个)");
            zdtsbbsGzbtCelSub.setCellStyle(style);

            Cell zdtsbbsLlsgCelSub = row10.createCell(19);
            zdtsbbsLlsgCelSub.setCellValue("预警指标数-普通(个)");
            zdtsbbsLlsgCelSub.setCellStyle(style);

            Cell zdtsbbsRhcgCelSub = row10.createCell(20);
            zdtsbbsRhcgCelSub.setCellValue("预警总次数(次)");
            zdtsbbsRhcgCelSub.setCellStyle(style);
            //预警处置结束


            /*Row row11 = sheet.createRow(11);
            Cell mcCelSub1 = row11.createCell(0);
            mcCelSub1.setCellValue("aaa");
            mcCelSub1.setCellStyle(style);*/
            //数据
            if(autoReportSubCountList != null && autoReportSubCountList.size()>0){
                int addRowIndex =11;
                for(AutoReportCount autoReportCount: autoReportSubCountList){
                    Row addrow = sheet.createRow(addRowIndex);
                    Cell mcValueCelSub = addrow.createCell(0);
                    mcValueCelSub.setCellValue(autoReportCount.getAreaName());
                    mcValueCelSub.setCellStyle(style);

                    //用户覆盖开始
                    Cell fwyhrwValueCel = addrow.createCell(1);
                    fwyhrwValueCel.setCellValue(autoReportCount.getYhFwyhs());
                    fwyhrwValueCel.setCellStyle(contentStyle);

                    Cell hyyhsValueCel = addrow.createCell(2);
                    hyyhsValueCel.setCellValue(autoReportCount.getYhHyyhs());
                    hyyhsValueCel.setCellStyle(contentStyle);

                    Cell yhhylvValueCel = addrow.createCell(3);
                    yhhylvValueCel.setCellValue(autoReportCount.getYhYhhylv());
                    yhhylvValueCel.setCellStyle(contentStyle);
                    //用户覆盖结束

                    //生成报表数开始
                    Cell zbJcsjValueCel = addrow.createCell(4);
                    zbJcsjValueCel.setCellValue(autoReportCount.getScbbJcsj());
                    zbJcsjValueCel.setCellStyle(contentStyle);

                    Cell zbKdklsValueCel = addrow.createCell(5);
                    zbKdklsValueCel.setCellValue(autoReportCount.getScbbKdkls());
                    zbKdklsValueCel.setCellStyle(contentStyle);

                    Cell zbGzbtValueCel = addrow.createCell(6);
                    zbGzbtValueCel.setCellValue(autoReportCount.getScbbGzbt());
                    zbGzbtValueCel.setCellStyle(contentStyle);

                    Cell zbLlsgValueCel = addrow.createCell(7);
                    zbLlsgValueCel.setCellValue(autoReportCount.getScbbLlsg());
                    zbLlsgValueCel.setCellStyle(contentStyle);

                    Cell zbRhcgValueCel = addrow.createCell(8);
                    zbRhcgValueCel.setCellValue(autoReportCount.getScbbRhcg());
                    zbRhcgValueCel.setCellStyle(contentStyle);

                    Cell zbYdklsValueCel = addrow.createCell(9);
                    zbYdklsValueCel.setCellValue(autoReportCount.getScbbYdkls());
                    zbYdklsValueCel.setCellStyle(contentStyle);
                    //生成报表数结束

                    //自动推送报表数开始
                    Cell scbbsJcsjValueCel = addrow.createCell(10);
                    scbbsJcsjValueCel.setCellValue(autoReportCount.getZdtsbbJcsj());
                    scbbsJcsjValueCel.setCellStyle(contentStyle);

                    Cell scbbsKdklsValueCel = addrow.createCell(11);
                    scbbsKdklsValueCel.setCellValue(autoReportCount.getZdtsbbKdkls());
                    scbbsKdklsValueCel.setCellStyle(contentStyle);

                    Cell scbbsGzbtValueCel = addrow.createCell(12);
                    scbbsGzbtValueCel.setCellValue(autoReportCount.getZdtsbbGzbt());
                    scbbsGzbtValueCel.setCellStyle(contentStyle);

                    Cell scbbsLlsgValueCel = addrow.createCell(13);
                    scbbsLlsgValueCel.setCellValue(autoReportCount.getZdtsbbLlsg());
                    scbbsLlsgValueCel.setCellStyle(contentStyle);

                    Cell scbbsRhcgValueCel = addrow.createCell(14);
                    scbbsRhcgValueCel.setCellValue(autoReportCount.getZdtsbbRhcg());
                    scbbsRhcgValueCel.setCellStyle(contentStyle);

                    Cell scbbsYdklsValueCel = addrow.createCell(15);
                    scbbsYdklsValueCel.setCellValue(autoReportCount.getZdtsbbYdkls());
                    scbbsYdklsValueCel.setCellStyle(contentStyle);
                    //自动推送报表数结束

                    //预警处置开始
                    Cell zdtsbbJcsjValueCel = addrow.createCell(16);
                    zdtsbbJcsjValueCel.setCellValue(autoReportCount.getWarningIndexCount());
                    zdtsbbJcsjValueCel.setCellStyle(contentStyle);

                    Cell zdtsbbKdklsValueCel = addrow.createCell(17);
                    zdtsbbKdklsValueCel.setCellValue(autoReportCount.getWarningIndexUrgentCount());
                    zdtsbbKdklsValueCel.setCellStyle(contentStyle);

                    Cell zdtsbbGzbtValueCel = addrow.createCell(18);
                    zdtsbbGzbtValueCel.setCellValue(autoReportCount.getWarningIndexAlarmCount());
                    zdtsbbGzbtValueCel.setCellStyle(contentStyle);

                    Cell zdtsbbLlsgValueCel = addrow.createCell(19);
                    zdtsbbLlsgValueCel.setCellValue(autoReportCount.getWarningIndexOrdinaryCount());
                    zdtsbbLlsgValueCel.setCellStyle(contentStyle);

                    Cell zdtsbbRhcgValueCel = addrow.createCell(20);
                    zdtsbbRhcgValueCel.setCellValue(autoReportCount.getWarningTotalCount());
                    zdtsbbRhcgValueCel.setCellStyle(contentStyle);

                    addRowIndex++;
                }
            }

            // -----------------------------
            // 写入文件
            // -----------------------------
            Path folderPath = Paths.get(yytjzxReportCountExportPath);
            if (!Files.isDirectory(folderPath)) {
                Files.createDirectories(folderPath);
            }

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            String filePath = yytjzxReportCountExportPath+"自助取数统计_"+area+"_"+currentUser.getRealName()+".xlsx";

            retStr = filePath;
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return retStr;
    }

    private static CellStyle createBorderedStyle(Workbook workbook, boolean backColor) {
        CellStyle style = workbook.createCellStyle();

        // 设置边框样式（可选：THIN, MEDIUM, DASHED, DOTTED, THICK 等）
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        //背景颜色
        if (backColor) {

            java.awt.Color  awtLightBlue = new java.awt.Color(242,242,242);
            XSSFColor customColor = new XSSFColor(awtLightBlue, new DefaultIndexedColorMap());

            style.setFillForegroundColor(customColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        }

        return style;
    }

}
