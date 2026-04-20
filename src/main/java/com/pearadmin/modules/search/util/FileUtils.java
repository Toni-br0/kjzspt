package com.pearadmin.modules.search.util;


/**
 * 创建日期：2024-11-26
 **/


import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import javax.imageio.ImageIO;

@Slf4j
public class FileUtils {

    private static final List<String> FILE_TYPE;

    static {
        FILE_TYPE = Arrays.asList("pdf", "doc", "docx", "text","txt","xlsx","xls","jpg","png","gif","jpeg");
    }

    public static String readFileContent(InputStream inputStream, String fileType, Tesseract tesseract) throws Exception{
        if (!FILE_TYPE.contains(fileType)) {
            return null;
        }
        // 使用PdfBox读取pdf文件内容
        if ("pdf".equalsIgnoreCase(fileType)) {
            return readPdfContent(inputStream);
        } else if ("doc".equalsIgnoreCase(fileType) || "docx".equalsIgnoreCase(fileType)) {
            return readDocOrDocxContent(inputStream);
        } else if ("text".equalsIgnoreCase(fileType)) {
            return readTextContent(inputStream);
        }else if ("txt".equalsIgnoreCase(fileType)) {
            return readTxtContent(inputStream);
        }else if ("xlsx".equalsIgnoreCase(fileType) || "xls".equalsIgnoreCase(fileType)) {
            return readExcelContent(inputStream);
        }else if ("jpg".equalsIgnoreCase(fileType) || "png".equalsIgnoreCase(fileType) || "gif".equalsIgnoreCase(fileType) || "jpeg".equalsIgnoreCase(fileType)) {

            return readImgContent(inputStream,tesseract);
        }

        return null;
    }


    private static String readPdfContent(InputStream inputStream) throws Exception {
        // 加载PDF文档
        PDDocument pdDocument = PDDocument.load(inputStream);

        // 创建PDFTextStripper对象, 提取文本
        PDFTextStripper textStripper = new PDFTextStripper();

        // 提取文本
        String content = textStripper.getText(pdDocument);
        // 关闭PDF文档
        pdDocument.close();
        return content;
    }


    private static String readDocOrDocxContent(InputStream inputStream) {
        try {
            // 加载DOC文档
            XWPFDocument document = new XWPFDocument(inputStream);

            // 2. 提取文本内容
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private static String readTextContent(InputStream inputStream) {
        StringBuilder content = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8")) {
            int ch;
            while ((ch = isr.read()) != -1) {
                content.append((char) ch);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return content.toString();
    }

    private static String readTxtContent(InputStream inputStream) {
        String retStr = "";
        StringBuilder content = new StringBuilder();
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line); // 将每一行内容添加到StringBuilder中
            }

            retStr = content.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return retStr;

        /*// 声明BufferedReader对象
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();

        try {
            // 创建FileReader对象，并指定文件路径和字符编码
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            String line;
            // 逐行读取文件内容
            while ((line = reader.readLine()) != null) {
                // 输出读取的每一行内容
                System.out.println(line);
                content.append(line); // 将每一行内容添加到StringBuilder中
            }
        } catch (IOException e) {
            // 捕获并打印IO异常
            e.printStackTrace();
        } finally {
            // 关闭BufferedReader，释放资源
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return content.toString();*/
    }

    private static String readExcelContent(InputStream inputStream){
        Workbook workbook = null;
        StringBuilder content = new StringBuilder();

        try {
            // 读取 Excel 文件
            workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表

            // 遍历每一行
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {

                Row row = rowIterator.next();
                // 遍历每一列
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {

                    Cell cell = cellIterator.next();
                    String cellValue = getCellValue(cell);
                    //content.append(cellValue+"<br>");
                    content.append("&nbsp;&nbsp;").append(cellValue); // 开始新的一列
                }
                content.append("<br>"); // 开始新的一行
            }

            return content.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }finally {
            if(workbook != null){
                try {
                    workbook.close();
                }catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    private static String readImgContent(InputStream inputStream, Tesseract tesseract){

        try {

            // 转换
            //InputStream sbs = new ByteArrayInputStream(multFile.getBytes());

            OsInfo osInfo = SystemUtil.getOsInfo();
            if(osInfo.isLinux()){
                tesseract.setDatapath("/app/server/fonts/tessdata");
            }else if(osInfo.isWindows()){
                tesseract.setDatapath("D:\\tmp");
            }

            tesseract.setLanguage("chi_sim");
            tesseract.setPageSegMode(2);

            BufferedImage bufferedImage = ImageIO.read(inputStream);

            // 对图片进行文字识别
            String result = tesseract.doOCR(bufferedImage);
            result = new String(result.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            result = result.replaceAll(" ","");
            return result;


        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    private static String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }


}

