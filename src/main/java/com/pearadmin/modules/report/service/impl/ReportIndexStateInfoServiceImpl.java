package com.pearadmin.modules.report.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportIndexStateInfo;
import com.pearadmin.modules.report.mapper.ReportIndexStateInfoMapper;
import com.pearadmin.modules.report.service.ReportIndexStateInfoService;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建日期：2025-08-15
 * 自助取数指标口径查询
 **/

@Slf4j
@Service
public class ReportIndexStateInfoServiceImpl implements ReportIndexStateInfoService {

    @Value("${report-target-path}")
    private String reportTargetPath;

    @Resource
    private Environment env;

    @Resource
    private ReportIndexStateInfoMapper reportIndexStateInfoMapper;

    /**
     * 获取指标口径查询列表数据
     * @param reportIndexStateInfo
     * @return
     */
    @Override
    public List<ReportIndexStateInfo> getIndexStateList(ReportIndexStateInfo reportIndexStateInfo) {
        return reportIndexStateInfoMapper.selIndexStateList(reportIndexStateInfo);
    }

    /**
     * 保存自助取数指标口径查询数据
     * @param reportIndexStateInfo
     * @return
     */
    @Override
    public JSONObject saveIndexStateInfo(ReportIndexStateInfo reportIndexStateInfo) {
        JSONObject retJsonObject = new JSONObject();
        try {
            reportIndexStateInfo.setCreateTime(LocalDateTime.now());
           int addResult = reportIndexStateInfoMapper.insert(reportIndexStateInfo);
           if(addResult >0){
               retJsonObject.put("retCode","0");
               retJsonObject.put("retMsg","保存成功");
           }else{
               retJsonObject.put("retCode","-1");
               retJsonObject.put("retMsg","保存失败");
           }
        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "保存失败");
        }
        return retJsonObject;
    }


    /**
     * 修改自助取数指标口径查询数据
     * @param reportIndexStateInfo
     * @return
     */
    @Override
    public JSONObject updateIndexStateInfo(ReportIndexStateInfo reportIndexStateInfo) {
        JSONObject retJsonObject = new JSONObject();
        try {

            int addResult = reportIndexStateInfoMapper.updateById(reportIndexStateInfo);
            if(addResult >0){
                retJsonObject.put("retCode","0");
                retJsonObject.put("retMsg","修改成功");
            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","修改失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "修改失败");
        }
        return retJsonObject;
    }


    /**
     * 单个删除自助取数指标口径查询数据
     * @param infoId
     * @return
     */
    @Override
    public JSONObject remove(int infoId) {
          JSONObject retJsonObject = new JSONObject();
         try {
            int deleteResult = reportIndexStateInfoMapper.deleteById(infoId);
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
     * 批量删除自助取数指标口径查询数据s
     * @param infoIds
     * @return
     */
    @Override
    public JSONObject batchRemove(String infoIds) {
        JSONObject retJsonObject = new JSONObject();
        try {
            List<Integer> delList = new ArrayList<>();
            for (String infoId : infoIds.split(CommonConstant.COMMA)) {
                int iInfoId = Integer.parseInt(infoId);
                delList.add(iInfoId);
            }

            int deleteResult = reportIndexStateInfoMapper.deleteBatchIds(delList);
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
     * 根据ID查询自助取数指标口径查询数据
     * @param infoId
     * @return
     */
    @Override
    public ReportIndexStateInfo getIndexStateInfoById(int infoId) {
        return reportIndexStateInfoMapper.selectById(infoId);
    }


    /**
     * 导出报表
     * @param reportIndexStateInfo
     * @return
     */
    @Override
    public JSONObject fileDownload(ReportIndexStateInfo reportIndexStateInfo) {
        JSONObject retJsonObject = new JSONObject();
        try {

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            List<ReportIndexStateInfo> list = reportIndexStateInfoMapper.selIndexStateList(reportIndexStateInfo);
            if(list != null && list.size() >0){

                // 确保目录存在
                File dir = new File(reportTargetPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String fileName = "指标口径说明_"+currentUser.getUsername() + ".xlsx";
                // 完整文件路径
                String filePath = reportTargetPath + fileName;

                // 核心API：EasyExcel.write() 构建写入对象
                EasyExcel.write(filePath, ReportIndexStateInfo.class)
                        .sheet("商品列表") // 指定工作表名称
                        .doWrite(list); // 写入数据

                String port = env.getProperty("server.port");
                String serverIp = env.getProperty("server.server-ip");

                String retFilePath =serverIp+":"+port+"/autoReportFile/"+fileName;

                retJsonObject.put("retCode","0");
                retJsonObject.put("filePath",retFilePath);
                retJsonObject.put("fileName",fileName);

            }
        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "导出失败");
        }

        return retJsonObject;
    }
}
