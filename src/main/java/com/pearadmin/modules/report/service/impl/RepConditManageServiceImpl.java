package com.pearadmin.modules.report.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportCondit;
import com.pearadmin.modules.report.mapper.ReportConditMapper;
import com.pearadmin.modules.report.service.RepConditManageService;
import com.pearadmin.modules.sys.domain.SysUser;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建日期：2025-07-02
 * 自助报表条件管理
 **/

@Slf4j
@Service
public class RepConditManageServiceImpl implements RepConditManageService {

    @Resource
    private ReportConditMapper reportConditMapper;

    /**
     * 获取条件管理列表数据
     * @param reportCondit
     * @return
     */
    @Override
    public List<ReportCondit> getConditList(ReportCondit reportCondit) {
        return reportConditMapper.selConditList(reportCondit);
    }

    /**
     * 保存条件数据
     * @param reportCondit
     * @return
     */
    @Override
    public JSONObject saveConditInfo(ReportCondit reportCondit){
        JSONObject retJson = new JSONObject();
        try {
            boolean isAdd = true;
            Integer conditId = reportCondit.getConditId();
            if(conditId !=null && conditId !=0){
                isAdd = false;
            }

            //判断条件编码是否存在
            /*QueryWrapper<ReportCondit> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("condit_code",reportCondit.getConditCode());

            ReportCondit queryReportCondit = reportConditMapper.selectOne(queryWrapper);
            if(queryReportCondit != null){
                if(isAdd){
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","新增失败，编码已存在");
                    return retJson;
                }else{
                    if(queryReportCondit.getConditId() != conditId){
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","修改失败，编码已存在");
                        return retJson;
                    }

                }
            }*/

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            reportCondit.setCreateTime(LocalDateTime.now());
            reportCondit.setCreateUserId(currentUser.getUserId());

            int result = 0;
            if(isAdd){
                result = reportConditMapper.insert(reportCondit);
                if(result > 0){
                    retJson.put("retCode","0");
                    retJson.put("retMsg","新增成功");
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","新增失败");
                }

            }else{
                result = reportConditMapper.updateById(reportCondit);
                if(result > 0){
                    retJson.put("retCode","0");
                    retJson.put("retMsg","修改成功");
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","修改失败");
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","保存失败");
        }
        return retJson;
    }

    /**
     * 根据条件ID查询条件数据
     * @param conditId
     * @return
     */
    public ReportCondit getConditById(int conditId){
        return reportConditMapper.selectById(conditId);
    }

    /**
     * 单个删除条件数据
     * Param: id
     * Return: 文件
     */
    @Override
    public JSONObject remove(int conditId){
        JSONObject retJson = new JSONObject();
        int result = reportConditMapper.deleteById(conditId);
        if(result > 0){
            retJson.put("retCode","0");
            retJson.put("retMsg","删除成功");
        }else{
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败");
        }

        return retJson;
    }


    /**
     * 批量删除条件数据
     * @param conditIds
     * @return
     */
    @Override
    public JSONObject batchRemove(String conditIds){
        JSONObject retJson = new JSONObject();
        try {
            List<Integer> delList = new ArrayList<>();
            for (String conditId : conditIds.split(CommonConstant.COMMA)) {
                int iConditId = Integer.parseInt(conditId);
                delList.add(iConditId);
            }

            int result = reportConditMapper.deleteBatchIds(delList);
            if(result >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","删除成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败,系统异常,请联系管理员");
        }
        return retJson;
    }

}
