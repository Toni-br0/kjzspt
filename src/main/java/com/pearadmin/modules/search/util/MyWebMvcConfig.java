package com.pearadmin.modules.search.util;

/**
 * 创建日期：2024-10-22
 **/

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebMvcConfig implements WebMvcConfigurer {

    @Value("${search-upload-path}")
    private String searchUploadPath;

    @Value("${search-file-topdf-path}")
    private String searchFileTopdfPath;

    @Value("${im-file-topdf-path}")
    private String imFileTopdfPath;

    @Value("${ppt-file-topdf-path}")
    private String pptFileTopdfPath;

    @Value("${hdjk-excelToPdf-path}")
    private String hdjkExcelToPdfPath;

    @Value("${report-target-path}")
    private String reportTargetPath;

    @Value("${xf-ppt-file-topdf-path}")
    private String xfPptFileTopdfPath;

    @Value("${wg-ppt-file-topdf-path}")
    private String wgPptFileTopdfPath;


    /**
     * 配置静态资源映射
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            searchUploadPath = "file:"+searchUploadPath;
            searchFileTopdfPath = "file:"+searchFileTopdfPath;

            imFileTopdfPath = "file:"+imFileTopdfPath;

            pptFileTopdfPath = "file:"+pptFileTopdfPath;

            hdjkExcelToPdfPath = "file:"+hdjkExcelToPdfPath;

            reportTargetPath = "file:"+reportTargetPath;

            xfPptFileTopdfPath = "file:"+xfPptFileTopdfPath;

            wgPptFileTopdfPath = "file:"+wgPptFileTopdfPath;

            registry.addResourceHandler("/searchUpload/**").addResourceLocations(searchUploadPath);
            registry.addResourceHandler("/searchFileTopdf/**").addResourceLocations(searchFileTopdfPath);

            registry.addResourceHandler("/imFileTopdf/**").addResourceLocations(imFileTopdfPath);

            registry.addResourceHandler("/pptFileTopdf/**").addResourceLocations(pptFileTopdfPath);

            registry.addResourceHandler("/hdjkExcelToPdf/**").addResourceLocations(hdjkExcelToPdfPath);

            registry.addResourceHandler("/autoReportFile/**").addResourceLocations(reportTargetPath);

            registry.addResourceHandler("/xfPptFileTopdf/**").addResourceLocations(xfPptFileTopdfPath);

            registry.addResourceHandler("/wgPptFileTopdf/**").addResourceLocations(wgPptFileTopdfPath);


        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}