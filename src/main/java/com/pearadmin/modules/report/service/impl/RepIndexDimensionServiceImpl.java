package com.pearadmin.modules.report.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.modules.ppt.util.ToolUtil;
import com.pearadmin.modules.report.domain.ReportDimension;
import com.pearadmin.modules.report.mapper.ReportDimensionMapper;
import com.pearadmin.modules.report.service.RepIndexDimensionService;
import com.pearadmin.modules.sys.domain.SysDept;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.mapper.SysDeptMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.expression.Ids;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建日期：2025-11-07
 * 自助取数维度管理
 **/

@Slf4j
@Service
public class RepIndexDimensionServiceImpl implements RepIndexDimensionService {

    @Resource
    private ReportDimensionMapper reportDimensionMapper;

    @Resource
    private SysDeptMapper sysDeptMapper;


    /**
     * 获取维度树数据
     * @return
     */
    @Override
    public Object dimensionTreeload(String isNonStand) {
        JSONObject retArr = new JSONObject();
        QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", "0");
        //queryWrapper.eq("is_non_stand", isNonStand);
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
                retArr.put("spread",true);

                retArr.put("isParent",true);

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
     * 保存自助取数维度管理数据
     * @param reportDimension
     * @return
     */
    @Override
    public JSONObject saveDimensionInfo(ReportDimension reportDimension) {
        JSONObject retJsonObject = new JSONObject();
        try {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            ReportDimension queryReportDimension = reportDimensionMapper.selectById(reportDimension.getDimensionId());
            if(queryReportDimension != null){
                retJsonObject.put("retCode", "-1");
                retJsonObject.put("retMsg", "新增失败，维度ID已存在");
                return retJsonObject;
            }

            //reportDimension.setDimensionId(RandomUtil.randomString(20));
            reportDimension.setDimensionId(reportDimension.getDimensionId());
            reportDimension.setCreateTime(LocalDateTime.now());
            reportDimension.setCreateBy(currentUser.getUserId());
            reportDimension.setState("0");

            String level = "";
            String field = reportDimension.getField() ==null?"":reportDimension.getField();
            if(field.equals("hx_latn_name")){ //地市
                level ="1";
            }else if(field.equals("hx_area_name")){ //区县
                level ="2";
            }else if(field.equals("x_hx5_bp_name")){ //网络
                level ="3";
            }

            reportDimension.setLevel(level);

            int addResult = reportDimensionMapper.insert(reportDimension);
            if(addResult > 0){
                retJsonObject.put("retCode", "0");
                retJsonObject.put("retMsg", "新增成功");
            }else{
                retJsonObject.put("retCode", "-1");
                retJsonObject.put("retMsg", "新增失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "新增失败，请联系管理员");
        }
        return retJsonObject;
    }


    /**
     * 删除自助取数维度数据
     * @param dimensionId
     * @return
     */
    @Override
    public JSONObject remove(String dimensionId) {
        JSONObject retJsonObject = new JSONObject();
        try {
            //查询此维度下是否有子维度
            QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id", dimensionId);
            queryWrapper.last("limit 1");

            ReportDimension reportDimension = reportDimensionMapper.selectOne(queryWrapper);
            if(reportDimension != null){
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","此维度下有子维度，请先删除子维度");
                return retJsonObject;
            }

            int deleteResult = reportDimensionMapper.deleteById(dimensionId);
            if(deleteResult >0){
                retJsonObject.put("retCode","0");
                retJsonObject.put("retMsg","删除成功");
            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","删除失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","删除失败");
        }
        return retJsonObject;
    }


    /**
     * 根据ID获取数据信息
     * @param dimensionId
     * @return
     */
    @Override
    public ReportDimension getById(String dimensionId) {
        return reportDimensionMapper.selectById(dimensionId);
    }


    /**
     * 根据ID获取父节点数据信息
     * @param dimensionId
     * @return
     */
    @Override
    public ReportDimension getParentById(String dimensionId) {

        ReportDimension parentReportDimension = null;

        ReportDimension reportDimension = reportDimensionMapper.selectById(dimensionId);
        if(reportDimension != null){
            QueryWrapper<ReportDimension> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("dimension_id", reportDimension.getParentId());
            parentReportDimension = reportDimensionMapper.selectOne(queryWrapper);

        }
        return parentReportDimension;
    }


    /**
     * 保存修改自助取数维度管理数据
     * @param reportDimension
     * @return
     */
    @Override
    public JSONObject updateDimensionInfo(ReportDimension reportDimension) {
        JSONObject retJsonObject = new JSONObject();
        try {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            reportDimension.setCreateTime(LocalDateTime.now());
            reportDimension.setCreateBy(currentUser.getUserId());
            reportDimension.setState("0");

            String level = "";
            String field = reportDimension.getField() ==null?"":reportDimension.getField();
            if(field.equals("hx_latn_name")){ //地市
                level ="1";
            }else if(field.equals("hx_area_name")){ //区县
                level ="2";
            }else if(field.equals("x_hx5_bp_name")){ //网络
                level ="3";
            }

            reportDimension.setLevel(level);

            int addResult = reportDimensionMapper.updateById(reportDimension);
            if(addResult > 0){
                retJsonObject.put("retCode", "0");
                retJsonObject.put("retMsg", "修改成功");
            }else{
                retJsonObject.put("retCode", "-1");
                retJsonObject.put("retMsg", "修改失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "修改失败，请联系管理员");
        }
        return retJsonObject;
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

                childObj.put("isParent",true);

//                childObj.put("spread",true);
                childObj.put("children",getChildTreeNode(reportDimension.getDimensionId(),isNonStand));
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
//                childObj.put("spread",true);
                childObj.put("children",getChildTreeNode(reportDimension.getDimensionId(),isNonStand)) ;

                childObj.put("isParent",true);

                retArr.add(childObj);
            }
        }

        return retArr;

    }

}
