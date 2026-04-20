package com.pearadmin.modules.yytjzx.service;

import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.modules.yytjzx.domain.AutoReportCount;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.List;

/**
 * 创建日期：2025-10-29
 * 自助报表统计
 **/

public interface AutoReportCountService {

    /**
     * 获取维度树数据
     * @return
     */
    public Object dimensionTreeload();


    /**
     * 获取整体应用统计数据
     * @return
     */
    public List<AutoReportCount> getAppCount(String area, String startDate, String endDate);


    /**
     *    * 获取整体应用下一级统计数据
     * @return
     */
    public List<AutoReportCount> getAppSubCount(String area,String areaCode, String startDate, String endDate);


    /**
     * 导出自助取数统计文件
     * @param area
     * @return
     * @throws IOException
     */
    public ResponseEntity<InputStreamResource> downloadFile(String area,String areaCode,String startDate,String endDate) throws Exception;

}
