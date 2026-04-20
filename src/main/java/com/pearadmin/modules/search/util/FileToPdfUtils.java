package com.pearadmin.modules.search.util;

/**
 * 创建日期：2025-01-17
 * 文件转pdf工具类  linux 服务器需要安装字体不然是乱码
 **/

import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import com.aspose.cells.*;
import com.aspose.slides.PdfOptions;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
import com.aspose.words.Document;
import com.aspose.words.FontSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.FontUnderline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


@Slf4j
public class FileToPdfUtils {
    /**
     * 文件转换
     *
     * @param source:源文件地址 如：C://test/test.doc,target:转换后文件路径  如 C://test/pdf
     * @return
     */
    public static String officeToPdf(String source, String target) {
        File file = new File(source);
        // 文件名字
        String fileName = file.getName();
        //office文档转pdf
        String fileExt = source.substring(source.lastIndexOf(".") + 1);
        if ("doc".equals(fileExt) || "docx".equals(fileExt)) {
            doc2pdf(source, target, fileExt);
        }
        if ("xls".equals(fileExt) || "xlsx".equals(fileExt)) {
            excel2pdf(source, target, fileExt);
        }
        if ("ppt".equals(fileExt) || "pptx".equals(fileExt)) {
            ppt2pdf(source, target, fileExt);
        }
        if ("txt".equals(fileExt)) {
            txt2pdf(source, target, fileExt);
        }

        if ("doc,docx,xls,xlsx,ppt,pptx,txt".indexOf(fileExt) > 0) {
            return target +  File.separator + (fileName.replace(fileExt, "pdf"));
        }
        return null;
    }

    public static String officeToPdfFileName(String source, String target) {
        File file = new File(source);
        // 文件名字
        String fileName = file.getName();
        //office文档转pdf
        String fileExt = source.substring(source.lastIndexOf(".") + 1);
        if ("doc".equals(fileExt) || "docx".equals(fileExt)) {
            doc2pdf(source, target, fileExt);
        }
        if ("xls".equals(fileExt) || "xlsx".equals(fileExt)) {
            excel2pdf(source, target, fileExt);
        }
        if ("ppt".equals(fileExt) || "pptx".equals(fileExt)) {
            ppt2pdf(source, target, fileExt);
        }
        if ("txt".equals(fileExt)) {
            txt2pdf(source, target, fileExt);
        }

        if ("doc,docx,xls,xlsx,ppt,pptx,txt".indexOf(fileExt) > 0) {
            return fileName.replace(fileExt, "pdf");
        }
        return null;
    }

    /**
     * @description: 验证ExcelLicense
     * @params:
     * @return:
     * @author: com.liuhm
     * @Date: 2019/10/10 13:40
     */
    public static boolean getExcelLicense() {
        boolean result = false;
        try {
            //  license.xml应放在..\WebRoot\WEB-INF\classes路径下
            //InputStream is = FileToPdfUtils.class.getClassLoader().getResourceAsStream("license.xml");
            /*ClassPathResource classPathResource = new ClassPathResource("license/license.xml");
            InputStream is = classPathResource.getInputStream();*/
            InputStream is = FileToPdfUtils.class.getClassLoader().getResourceAsStream("license/license.xml");
            com.aspose.cells.License aposeLic = new com.aspose.cells.License();
            aposeLic.setLicense(is);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @description: 验证PPTtlLicense
     * @params:
     * @return:
     * @author: com.liuhm
     * @Date: 2019/10/10 13:40
     */
    public static boolean getPPTLicense() {
        boolean result = false;
        try {
            //  license.xml应放在..\WebRoot\WEB-INF\classes路径下
            //InputStream is = FileToPdfUtils.class.getClassLoader().getResourceAsStream("license.xml");
            /*ClassPathResource classPathResource = new ClassPathResource("license/license.xml");
            InputStream is = classPathResource.getInputStream();*/
            InputStream is = FileToPdfUtils.class.getClassLoader().getResourceAsStream("license/license.xml");
            com.aspose.slides.License aposeLic = new com.aspose.slides.License();
            aposeLic.setLicense(is);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @description: 验证License
     * @params:
     * @return:
     * @author: com.liuhm
     * @Date: 2019/10/10 13:40
     */
    public static boolean getDocLicense() {
        boolean result = false;
        try {
            //  license.xml应放在..\WebRoot\WEB-INF\classes路径下
            //InputStream is = FileToPdfUtils.class.getClassLoader().getResourceAsStream("license.xml");
            /*ClassPathResource classPathResource = new ClassPathResource("license/license.xml");
            InputStream is = classPathResource.getInputStream();*/
            InputStream is = FileToPdfUtils.class.getClassLoader().getResourceAsStream("license/license.xml");
            com.aspose.words.License aposeLic = new com.aspose.words.License();
            aposeLic.setLicense(is);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @description: excel转pdf
     * @params: source:源文件地址,target:转换后文件路径,fileExt:后缀
     * @return:
     * @author: com.liuhm
     * @Date: 2019/10/10 13:41
     */
    public static void excel2pdf(String source, String target, String fileExt) {

        // 验证License 若不验证则转化出的pdf文档会有水印产生
        if (!getExcelLicense()) {
            return;
        }

        try {

            //验证路径
            try {
                if (!(new File(target).isDirectory())) {
                    new File(target).mkdirs();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            // 文件名字
            String fileName = new File(source).getName();

            String dest = target + fileName.replace(fileExt, "pdf");

            OsInfo osInfo = SystemUtil.getOsInfo();
            if(osInfo.isLinux()){
                //FontSettings.setFontsFolder("/app/tools/Fonts/chinese", true);
                // 设置字体文件夹路径
                FontConfigs.setFontFolder("/app/server/fonts/chinese", true);
            }

            // 原始excel路径
            Workbook wb = new Workbook(source);

            // 手动触发公式重新计算
            wb.calculateFormula();

            PdfSaveOptions pdfSaveOptions = new PdfSaveOptions();

            pdfSaveOptions.setFontSubstitutionCharGranularity(true);
            //pdfSaveOptions.setDefaultFont("SimSun"); // 设置为宋体


            //缩放到一个页面（如果列太多 太长）
            pdfSaveOptions.setOnePagePerSheet(true);
            //重点，设置所有列放在一页里，会自动适应宽度
            pdfSaveOptions.setAllColumnsInOnePagePerSheet(true);
            wb.save(dest, pdfSaveOptions);

            log.info("++++++++++++Excel文件已成功转换为PDF！");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @description: ppt转pdf
     * @params: source:源文件地址,target:转换后文件路径,fileExt:后缀
     * @return:
     * @author: com.liuhm
     * @Date: 2019/10/10 13:46
     */
    public static void ppt2pdf(String source, String target, String fileExt) {
        // 验证License 若不验证则转化出的pdf文档会有水印产生
        if (!getPPTLicense()) {
            return;
        }

        log.info("++++++++++excel2pdf_source: {} - target: {}",source,target);

        FileOutputStream fileOS = null;
        Presentation pres = null;

        try {
            //验证路径
            try {
                if (!(new File(target).isDirectory())) {
                    new File(target).mkdirs();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            // 文件名字
            String fileName = new File(source).getName();
            //新建一个空白pdf文档
            File file = new File(target +  File.separator + (fileName.replace(fileExt, "pdf")));

            OsInfo osInfo = SystemUtil.getOsInfo();
            if(osInfo.isLinux()){
                log.info("++++++++++linux环境---ppt2pdf 加载字体路径+++++++++++");
                //FontSettings.setFontsFolder("/app/tools/Fonts/chinese", true);
                // 设置字体文件夹路径
                FontConfigs.setFontFolder("/app/server/fonts/chinese", true);
                FontSettings.setFontsFolder("/app/server/fonts/chinese", true);

                //ppt
                com.aspose.slides.FontsLoader.loadExternalFonts(new String[]{"/app/server/fonts/chinese/"});
            }

            //输入ppt路径
            pres = new Presentation(source);

            // 配置 PDF 选项（嵌入字体）
            PdfOptions pdfOptions = new PdfOptions();
            pdfOptions.setEmbedTrueTypeFontsForASCII(true);  // 嵌入 ASCII 范围内的字体:cite[1]:cite[7]

            fileOS = new FileOutputStream(file);
            pres.save(fileOS, SaveFormat.Pdf,pdfOptions);

        } catch (Exception e) {
            e.printStackTrace();
        }finally {

            if(fileOS != null){
                try {
                    fileOS.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

            if(pres != null){
                pres.dispose();
            }
        }
    }

    /**
     * @description: ppt转pdf
     * @params: source:源文件地址,target:转换后文件路径,fileExt:后缀
     * @return:
     * @author: com.liuhm
     * @Date: 2019/10/10 13:46
     */
    public static void txt2pdf(String source, String target, String fileExt) {
        // 验证License 若不验证则转化出的pdf文档会有水印产生
        if (!getPPTLicense()) {
            return;
        }
        try {
            //验证路径
            try {
                if (!(new File(target).isDirectory())) {
                    new File(target).mkdirs();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            // 文件名字
            String fileName = new File(source).getName();

            String dest = target + fileName.replace(fileExt, "pdf");

            OsInfo osInfo = SystemUtil.getOsInfo();
            if(osInfo.isLinux()){
                //FontSettings.setFontsFolder("/app/tools/Fonts/chinese", true);
                // 设置字体文件夹路径
                FontConfigs.setFontFolder("/app/server/fonts/chinese", true);
                FontSettings.setFontsFolder("/app/server/fonts/chinese", true);
            }

            // 加载TXT文件
            Document doc = new Document(source);

            // 保存为PDF
            doc.save(dest, com.aspose.words.SaveFormat.PDF);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @description: doc转pdf
     * @params: source:源文件地址,target:转换后文件路径,fileExt:后缀
     * @return:
     * @author: com.liuhm
     * @Date: 2019/10/10 13:46
     */
    public static void doc2pdf(String source, String target, String fileExt) {
        // 验证License 若不验证则转化出的pdf文档会有水印产生
        if (!getDocLicense()) {
            return;
        }
        try {
            //新建一个空白pdf文档
            try {
                if (!(new File(target).isDirectory())) {
                    new File(target).mkdirs();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            // 文件名字

            String fileName = new File(source).getName();
            // 输出路径
            File file = new File(target +  File.separator + (fileName.replace(fileExt, "pdf")));
            FileOutputStream os = new FileOutputStream(file);

            OsInfo osInfo = SystemUtil.getOsInfo();
            if(osInfo.isLinux()){
                // 设置字体文件夹路径
                FontSettings.setFontsFolder("/app/server/fonts/chinese", true);
                FontConfigs.setFontFolder("/app/server/fonts/chinese", true);
            }

            Document doc = new Document(source);
            doc.save(os, com.aspose.words.SaveFormat.PDF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
