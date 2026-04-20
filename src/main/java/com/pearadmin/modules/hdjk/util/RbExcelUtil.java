package com.pearadmin.modules.hdjk.util;

import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson.JSONObject;
import com.aspose.cells.*;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.search.util.FileToPdfUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.*;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建日期：2025-05-16
 **/

@Slf4j
public class RbExcelUtil {

    /**
     * 生成Excel文件
     * @param sourFilePath   源Excel文件路径
     * @param outputFilePath 输出 Excel文件路径
     * @param dataList  数据集
     */
    public static boolean createExcelFile(String sourFilePath,String outputFilePath,List<Map<String,Object>> dataList){
        boolean result = false;
        FileInputStream fis = null;
        FileOutputStream outputStream = null;
        Workbook workbook = null;
        try {
            //excel模板文件路径
            fis = new FileInputStream(sourFilePath);
            // 创建一个工作簿对象
            workbook = new XSSFWorkbook(fis);
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            // 获取总行数
            int totalRowNum = sheet.getLastRowNum();

            int xhRowNum = findRowByCellValue(sheet, "xh1");
            log.info("+++++xhRowNum: {}",xhRowNum);

            //获取总列数
            //int totalColNum = sheet.getRow(0).getLastCellNum();
            int totalColNum = sheet.getRow(xhRowNum).getLastCellNum();

            log.info("+++++totalRowNum: {}",totalRowNum);
            log.info("+++++totalColNum: {}",totalColNum);

            boolean isNext = true;
            int rowIndex = 0;
            int columnIndex = 0;

            //循环获取模板“序号”所在的行和列索引
            for (int i = 0; i <= totalRowNum && isNext; i++){
                Row row = sheet.getRow(i);
                if (row != null){
                    for(int j=0;j <= totalColNum;j++){
                        String cellValue = getCellValue(row.getCell(j));
                        if(StringUtil.isNotEmpty(cellValue) && cellValue.trim().equals("xh1")){
                            rowIndex = i;
                            columnIndex = j;
                            isNext = false;
                            break;
                        }
                    }
                }
            }

            log.info("+++++rowIndex: {}",rowIndex);
            log.info("+++++columnIndex: {}",columnIndex);

            //拼接查询语句所需的字段信息，并保存字段信息对应的列索引到map中
            Map<String,Integer> columnIndexMap = new HashMap<String,Integer>();
            String selName ="";
            Row row = sheet.getRow(rowIndex);
            if(row != null){
                for(int i=columnIndex+1;i<=totalColNum;i++){
                    String cellValue = getCellValue(row.getCell(i));
                    if(StringUtil.isNotEmpty(cellValue)){

                        columnIndexMap.put(cellValue,i);

                        if(selName.equals("")){
                            selName = cellValue;
                        }else{
                            selName = selName+","+cellValue;
                        }
                    }
                }
            }

            log.info("+++++selName: {}",selName);
            log.info("+++++columnIndexMap: {}",columnIndexMap);

            //设置数据到Excel中
            if(dataList != null && dataList.size() >0){
                for(int i=0;i<dataList.size();i++){

                    Map<String,Object> dataMap = dataList.get(i);
                    Row dataRow = sheet.getRow(rowIndex+i);
                    if(dataRow == null){
                        dataRow = sheet.createRow(rowIndex+i);
                    }

                    //创建“序号”左边单元格及背景色
                    Cell leftCellXh = dataRow.getCell(columnIndex-1);
                    if (leftCellXh == null) {
                        leftCellXh = dataRow.createCell(columnIndex-1); // 如果不存在则创建第一列的单元格
                    }
                    leftCellXh.setCellValue("");
                    leftCellXh.setCellStyle(backCellStyle(workbook));

                    //创建右边单元格及背景色
                    Cell rightCellXh = dataRow.getCell(totalColNum-1);
                    if (rightCellXh == null) {
                        rightCellXh = dataRow.createCell(totalColNum-1); // 如果不存在则创建第一列的单元格
                    }
                    rightCellXh.setCellValue("");
                    rightCellXh.setCellStyle(backCellStyle(workbook));


                    //创建序号列
                    Cell cellXh = dataRow.getCell(columnIndex);
                    if (cellXh == null) {
                        cellXh = dataRow.createCell(columnIndex); // 如果不存在则创建第一列的单元格
                    }
                    cellXh.setCellValue(i+1);
                    cellXh.setCellStyle(contentCellStyle(workbook,"",""));

                    //根据数据值及对应的列索引设置数据
                    for(String key : dataMap.keySet()){

                        if(columnIndexMap.get(key) != null){
                            Cell cell = dataRow.getCell(columnIndexMap.get(key));
                            if (cell == null) {
                                cell = dataRow.createCell(columnIndexMap.get(key)); // 如果不存在则创单元格
                            }

                            Object cellObject = dataMap.get(key);
                            if (cellObject instanceof String) {
                                // 对象是String类型
                                cell.setCellValue(cellObject.toString());
                            } else if (cellObject instanceof Integer) {
                                // 对象是Integer类型
                                cell.setCellValue(Integer.valueOf(cellObject.toString()));
                            } else if (cellObject instanceof Double) {
                                // 对象是Integer类型
                                cell.setCellValue(Double.valueOf(cellObject.toString()));
                            } else {
                                // 对象是其他类型
                                cell.setCellValue(dataMap.get(key).toString());
                            }

                            cell.setCellStyle(contentCellStyle(workbook,"",""));
                        }

                    }

                }

                //创建最后一行
                Row lastRow = sheet.getRow(totalRowNum+dataList.size()-1);
                if(lastRow == null){
                    lastRow = sheet.createRow(totalRowNum+dataList.size()-1);
                }

                //创建最后一行单元格及背景色
                for(int i=0;i<totalColNum;i++){
                    Cell lastCellXh = lastRow.getCell(i);
                    if (lastCellXh == null) {
                        lastCellXh = lastRow.createCell(i);
                    }
                    lastCellXh.setCellValue("");
                    lastCellXh.setCellStyle(backCellStyle(workbook));
                }

                outputStream = new FileOutputStream(outputFilePath);
                workbook.write(outputStream); // 写入到输出流中

                result = true;
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(fis !=null){
                    fis.close();
                }

                if(workbook !=null){
                    workbook.close();
                }
                if(outputStream != null){
                    outputStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        return result;

    }


    /**
     * 生成Excel文件
     * @param sourFilePath   源Excel文件路径
     * @param outputFilePath 输出 Excel文件路径
     * @param dataListMap  数据集
     */
    public static boolean createExcelFile2(String sourFilePath,String outputFilePath,Map<String,List<Map<String,Object>>> dataListMap,String backgroundColor,String lineColor){
        boolean result = false;
        FileInputStream fis = null;
        FileOutputStream outputStream = null;
        Workbook workbook = null;
        try {
            //excel模板文件路径
            fis = new FileInputStream(sourFilePath);
            // 创建一个工作簿对象
            workbook = new XSSFWorkbook(fis);
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);

            for(int m=1;m<=dataListMap.size();m++){

                List<Map<String,Object>> dataList = dataListMap.get("xh"+m);

                // 获取总行数
                int totalRowNum = sheet.getLastRowNum();

                //获取“xh1”所在的行
                int xhRowNum = findRowByCellValue(sheet, "xh1");
                log.info("+++++xhRowNum: {}",xhRowNum);

                //获取总列数
                //int totalColNum = sheet.getRow(0).getLastCellNum();
                int totalColNum = sheet.getRow(xhRowNum).getLastCellNum();

                log.info("+++++totalRowNum: {}",totalRowNum);
                log.info("+++++totalColNum: {}",totalColNum);

                boolean isNext = true;
                int rowIndex = 0;
                int columnIndex = 0;

                //循环获取模板“序号”所在的行和列索引
                for (int i = 0; i <= totalRowNum && isNext; i++){
                    Row row = sheet.getRow(i);
                    if (row != null){
                        for(int j=0;j <= totalColNum;j++){
                            String cellValue = getCellValue(row.getCell(j));
                            if(StringUtil.isNotEmpty(cellValue) && cellValue.trim().equals("xh"+m)){
                                rowIndex = i-1;
                                columnIndex = j-1;
                                isNext = false;
                                break;
                            }
                        }
                    }
                }

                log.info("+++++rowIndex: {}",rowIndex);
                log.info("+++++columnIndex: {}",columnIndex);

                //拼接查询语句所需的字段信息，并保存字段信息对应的列索引到map中
                Map<String,Integer> columnIndexMap = new HashMap<String,Integer>();
                String selName ="";
                Row row = sheet.getRow(rowIndex+1);
                if(row != null){
                    for(int i=columnIndex+1;i<=totalColNum;i++){
                        String cellValue = getCellValue(row.getCell(i));
                        if(StringUtil.isNotEmpty(cellValue)){

                            columnIndexMap.put(cellValue,i);

                            if(selName.equals("")){
                                selName = cellValue;
                            }else{
                                selName = selName+","+cellValue;
                            }
                        }
                    }
                }

                //log.info("+++++selName: {}",selName);
                log.info("+++++columnIndexMap: {}",columnIndexMap);

                //设置数据到Excel中
                if(dataList != null && dataList.size() >0){
                    for(int i=0;i<dataList.size();i++){

                        Map<String,Object> dataMap = dataList.get(i);
                        Row dataRow = sheet.getRow(rowIndex+1+i);
                        if(dataRow == null){
                            dataRow = sheet.createRow(rowIndex+1+i);
                        }

                        //创建序号列
                        /*Cell cellXh = dataRow.getCell(columnIndex+1);
                        if (cellXh == null) {
                            cellXh = dataRow.createCell(columnIndex+1); // 如果不存在则创建第一列的单元格
                        }
                        cellXh.setCellValue(i+1);
                        cellXh.setCellStyle(contentCellStyle(workbook,backgroundColor,lineColor));*/

                        //根据数据值及对应的列索引设置数据
                        String xhName = "xh"+m;
                        for(String key : columnIndexMap.keySet()){
                            //System.out.println("+++++key: " + key);
                            Cell cell = dataRow.getCell(columnIndexMap.get(key));
                            if (cell == null) {
                                cell = dataRow.createCell(columnIndexMap.get(key)); // 如果不存在则创单元格
                            }

                            Object cellObject = null;
                            if(key.equals(xhName)){
                                cellObject = i+1;
                            }else{
                                cellObject = dataMap.get(key);
                            }

                            //System.out.println("+++++cellObject: " + cellObject);

                            if(cellObject != null){
                                if (cellObject instanceof String) {

                                    String strCellObject = cellObject.toString();
                                    //验证是否数字
                                    if(isValidNumber(strCellObject)){
                                        cell.setCellValue(Double.valueOf(strCellObject));
                                    }else{
                                        // 对象是String类型
                                        cell.setCellValue(cellObject.toString());
                                    }

                                } else if (cellObject instanceof Integer) {
                                    // 对象是Integer类型
                                    cell.setCellValue(Integer.valueOf(cellObject.toString()));
                                }else if (cellObject instanceof Long) {
                                    // 对象是Long类型
                                    cell.setCellValue(Long.valueOf(cellObject.toString()));
                                } else if (cellObject instanceof Double) {
                                    // 对象是Integer类型
                                    cell.setCellValue(Double.valueOf(cellObject.toString()));
                                } else if (cellObject instanceof BigDecimal) {
                                    // 对象是Integer类型
                                    cell.setCellValue(Double.valueOf(cellObject.toString()));
                                } else if (cellObject instanceof Float) {
                                    // 对象是Integer类型
                                    cell.setCellValue(Float.valueOf(cellObject.toString()));
                                }else {
                                    // 对象是其他类型
                                    //cell.setCellValue(dataMap.get(key).toString());
                                    String strCellObject = cellObject.toString();
                                    //验证是否数字
                                    if(isValidNumber(strCellObject)){
                                        cell.setCellValue(Double.valueOf(strCellObject));
                                    }else{
                                        // 对象是String类型
                                        cell.setCellValue(cellObject.toString());
                                    }
                                }

                                cell.setCellStyle(contentCellStyle(workbook,backgroundColor,lineColor));
                            }else{
                                cell.setCellValue("");
                                cell.setCellStyle(contentCellStyle(workbook,backgroundColor,lineColor));
                            }

                        }

                    }
                }
            }

            workbook.setForceFormulaRecalculation(true); // 确保公式被重新计算
            outputStream = new FileOutputStream(outputFilePath);
            workbook.write(outputStream); // 写入到输出流中

            result = true;

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(fis !=null){
                    fis.close();
                }

                if(workbook !=null){
                    workbook.close();
                }
                if(outputStream != null){
                    outputStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        return result;

    }


    /**
     * 生成Excel文件
     * @param sourFilePath   源Excel文件路径
     * @param outputFilePath 输出 Excel文件路径
     * @param dataListMap  数据集
     */
    public static boolean createBigDataExcelFile(String sourFilePath,String outputFilePath,Map<String,List<Map<String,Object>>> dataListMap,String backgroundColor,String lineColor){

        boolean result = false;
        FileInputStream fis = null;
        FileOutputStream outputStream = null;
        Workbook workbook = null;

        // 添加样式缓存
        CellStyle contentStyle = null;

        Map<String, CellStyle> styleCache = new HashMap<>();

        try {
            //excel模板文件路径
            fis = new FileInputStream(sourFilePath);
            // 创建一个工作簿对象
            workbook = new XSSFWorkbook(fis);
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);

            // 预先创建并缓存样式
            contentStyle = createAndCacheCellStyle(workbook, backgroundColor, lineColor,styleCache);

            for(int m=1; m<=dataListMap.size(); m++){
                List<Map<String,Object>> dataList = dataListMap.get("xh"+m);

                // 获取总行数
                int totalRowNum = sheet.getLastRowNum();

                //获取"xh1"所在的行
                int xhRowNum = findRowByCellValue(sheet, "xh1");
                log.info("+++++xhRowNum: {}", xhRowNum);

                //获取总列数
                int totalColNum = sheet.getRow(xhRowNum).getLastCellNum();

                log.info("+++++totalRowNum: {}", totalRowNum);
                log.info("+++++totalColNum: {}", totalColNum);

                boolean isNext = true;
                int rowIndex = 0;
                int columnIndex = 0;

                //循环获取模板"序号"所在的行和列索引
                for (int i = 0; i <= totalRowNum && isNext; i++){
                    Row row = sheet.getRow(i);
                    if (row != null){
                        for(int j=0; j <= totalColNum; j++){
                            String cellValue = getCellValue(row.getCell(j));
                            if(StringUtil.isNotEmpty(cellValue) && cellValue.trim().equals("xh"+m)){
                                rowIndex = i-1;
                                columnIndex = j-1;
                                isNext = false;
                                break;
                            }
                        }
                    }
                }

                log.info("+++++rowIndex: {}", rowIndex);
                log.info("+++++columnIndex: {}", columnIndex);

                //拼接查询语句所需的字段信息，并保存字段信息对应的列索引到map中
                Map<String,Integer> columnIndexMap = new HashMap<String,Integer>();
                String selName ="";
                Row row = sheet.getRow(rowIndex+1);
                if(row != null){
                    for(int i=columnIndex+1; i<=totalColNum; i++){
                        String cellValue = getCellValue(row.getCell(i));
                        if(StringUtil.isNotEmpty(cellValue)){
                            columnIndexMap.put(cellValue, i);

                            if(selName.equals("")){
                                selName = cellValue;
                            }else{
                                selName = selName + "," + cellValue;
                            }
                        }
                    }
                }

                log.info("+++++columnIndexMap: {}", columnIndexMap);

                //设置数据到Excel中
                if(dataList != null && dataList.size() > 0){
                    // 批量创建行（优化性能）
                    for(int i=0; i<dataList.size(); i++){
                        Map<String,Object> dataMap = dataList.get(i);
                        int currentRowNum = rowIndex + 1 + i;

                        Row dataRow = sheet.getRow(currentRowNum);
                        if(dataRow == null){
                            dataRow = sheet.createRow(currentRowNum);
                        }

                        //根据数据值及对应的列索引设置数据
                        String xhName = "xh"+m;
                        for(String key : columnIndexMap.keySet()){
                            int colIdx = columnIndexMap.get(key);
                            Cell cell = dataRow.getCell(colIdx);
                            if (cell == null) {
                                cell = dataRow.createCell(colIdx);
                            }

                            Object cellObject = null;
                            if(key.equals(xhName)){
                                cellObject = i+1;
                            }else{
                                cellObject = dataMap.get(key);
                            }

                            if(cellObject != null){
                                setCellValueBasedOnType(cell, cellObject);
                            }else{
                                cell.setCellValue("");
                            }

                            // 使用缓存的样式，而不是每次都创建新样式
                            cell.setCellStyle(contentStyle);
                        }
                    }
                }
            }

            workbook.setForceFormulaRecalculation(true);
            outputStream = new FileOutputStream(outputFilePath);
            workbook.write(outputStream);

            result = true;

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if(fis != null) fis.close();
                if(workbook != null) workbook.close();
                if(outputStream != null) outputStream.close();

                // 清理缓存
                styleCache.clear();
                contentStyle = null;
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return result;

    }

    /**
     * 生成Excel文件
     * @param sourFilePath   源Excel文件路径
     * @param outputFilePath 输出 Excel文件路径
     * @param dataListMap  数据集
     */
    public static JSONObject createBigDataExcelFileAuto(String sourFilePath, String outputFilePath, Map<String,List<Map<String,Object>>> dataListMap, String backgroundColor, String lineColor){
        JSONObject retJsonObject = new JSONObject();
        retJsonObject.put("retCode","0");
        retJsonObject.put("retMsg","文件生成失败");

        FileInputStream fis = null;
        FileOutputStream outputStream = null;
        Workbook workbook = null;

        // 添加样式缓存
        CellStyle contentStyle = null;

        Map<String, CellStyle> styleCache = new HashMap<>();

        try {
            //excel模板文件路径
            fis = new FileInputStream(sourFilePath);
            // 创建一个工作簿对象
            workbook = new XSSFWorkbook(fis);
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);

            // 预先创建并缓存样式
            contentStyle = createAndCacheCellStyle(workbook, backgroundColor, lineColor,styleCache);

            for(int m=1; m<=dataListMap.size(); m++){
                List<Map<String,Object>> dataList = dataListMap.get("xh"+m);

                // 获取总行数
                int totalRowNum = sheet.getLastRowNum();

                //获取"xh1"所在的行
                int xhRowNum = findRowByCellValue(sheet, "xh1");
                log.info("+++++xhRowNum: {}", xhRowNum);

                //获取总列数
                int totalColNum = sheet.getRow(xhRowNum).getLastCellNum();

                log.info("+++++totalRowNum: {}", totalRowNum);
                log.info("+++++totalColNum: {}", totalColNum);

                boolean isNext = true;
                int rowIndex = 0;
                int columnIndex = 0;

                //循环获取模板"序号"所在的行和列索引
                for (int i = 0; i <= totalRowNum && isNext; i++){
                    Row row = sheet.getRow(i);
                    if (row != null){
                        for(int j=0; j <= totalColNum; j++){
                            String cellValue = getCellValue(row.getCell(j));
                            if(StringUtil.isNotEmpty(cellValue) && cellValue.trim().equals("xh"+m)){
                                rowIndex = i-1;
                                columnIndex = j-1;
                                isNext = false;
                                break;
                            }
                        }
                    }
                }

                log.info("+++++rowIndex: {}", rowIndex);
                log.info("+++++columnIndex: {}", columnIndex);

                //拼接查询语句所需的字段信息，并保存字段信息对应的列索引到map中
                Map<String,Integer> columnIndexMap = new HashMap<String,Integer>();
                String selName ="";
                Row row = sheet.getRow(rowIndex+1);
                if(row != null){
                    for(int i=columnIndex+1; i<=totalColNum; i++){
                        String cellValue = getCellValue(row.getCell(i));
                        if(StringUtil.isNotEmpty(cellValue)){
                            columnIndexMap.put(cellValue, i);

                            if(selName.equals("")){
                                selName = cellValue;
                            }else{
                                selName = selName + "," + cellValue;
                            }
                        }
                    }
                }

                log.info("+++++columnIndexMap: {}", columnIndexMap);

                //设置数据到Excel中
                if(dataList != null && dataList.size() > 0){
                    // 批量创建行（优化性能）
                    for(int i=0; i<dataList.size(); i++){
                        Map<String,Object> dataMap = dataList.get(i);
                        int currentRowNum = rowIndex + 1 + i;

                        Row dataRow = sheet.getRow(currentRowNum);
                        if(dataRow == null){
                            dataRow = sheet.createRow(currentRowNum);
                        }

                        //根据数据值及对应的列索引设置数据
                        String xhName = "xh"+m;
                        for(String key : columnIndexMap.keySet()){
                            int colIdx = columnIndexMap.get(key);
                            Cell cell = dataRow.getCell(colIdx);
                            if (cell == null) {
                                cell = dataRow.createCell(colIdx);
                            }

                            Object cellObject = null;
                            if(key.equals(xhName)){
                                cellObject = i+1;
                            }else{
                                cellObject = dataMap.get(key);
                            }

                            if(cellObject != null){
                                setCellValueBasedOnType(cell, cellObject);
                            }else{
                                cell.setCellValue("");
                            }

                            // 使用缓存的样式，而不是每次都创建新样式
                            cell.setCellStyle(contentStyle);
                        }
                    }
                }
            }

            workbook.setForceFormulaRecalculation(true);
            outputStream = new FileOutputStream(outputFilePath);
            workbook.write(outputStream);

            retJsonObject.put("retCode","0");
            retJsonObject.put("retMsg","文件生成成功");

        } catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","文件生成失败："+e.toString());
        } finally {
            try {
                if(fis != null) fis.close();
                if(workbook != null) workbook.close();
                if(outputStream != null) outputStream.close();

                // 清理缓存
                styleCache.clear();
                contentStyle = null;
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        return retJsonObject;

    }

    // 样式创建和缓存方法
    private static CellStyle createAndCacheCellStyle(Workbook workbook, String backgroundColor, String lineColor,Map<String, CellStyle> styleCache) throws Exception {
        String cacheKey = backgroundColor + "_" + lineColor;

        if(styleCache.containsKey(cacheKey)) {
            return styleCache.get(cacheKey);
        }

        CellStyle style = workbook.createCellStyle();

        // 设置背景色
        if(StringUtil.isNotEmpty(backgroundColor)) {
            try {
                // 如果是16进制颜色
                if(backgroundColor.startsWith("#")) {
                    // XSSFWorkbook支持16进制颜色
                    if(workbook instanceof XSSFWorkbook) {
                        XSSFCellStyle xssfStyle = (XSSFCellStyle) style;
                        xssfStyle.setFillForegroundColor(new XSSFColor(
                                Color.decode(backgroundColor), new DefaultIndexedColorMap()));
                    } else {
                        // HSSFWorkbook使用索引颜色
                        //style.setFillForegroundColor(getNearestColor(backgroundColor));
                    }
                } else {
                    // 使用预定义颜色
                    /*IndexedColors color = IndexedColors.valueOf(backgroundColor.toUpperCase());
                    style.setFillForegroundColor(color.getIndex());*/

                    if(StringUtil.isNotEmpty(backgroundColor)){
                        //XSSFColor color = new XSSFColor(new java.awt.Color(237,237,237), null);
                        String[] colorStr = backgroundColor.split(",");
                        int color1 = Integer.parseInt(colorStr[0]);
                        int color2 = Integer.parseInt(colorStr[1]);
                        int color3 = Integer.parseInt(colorStr[2]);

                        XSSFColor color = new XSSFColor(new java.awt.Color(color1,color2,color3), null);
                        style.setFillForegroundColor(color);
                        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    }else{
                        XSSFColor color = new XSSFColor(new java.awt.Color(255,255,255), null);
                        style.setFillForegroundColor(color);
                        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    }

                }
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } catch (Exception e) {
                log.warn("设置背景色失败: {}", backgroundColor, e);
                e.printStackTrace();
                throw e;
            }
        }

        // 设置边框
        if(StringUtil.isNotEmpty(lineColor)) {
            BorderStyle borderStyle = BorderStyle.THIN;
            try {
                // 边框颜色
                if(lineColor.startsWith("#")) {
                    if(workbook instanceof XSSFWorkbook) {
                        XSSFColor borderColor = new XSSFColor(
                                java.awt.Color.decode(lineColor), new DefaultIndexedColorMap());
                        // 这里需要特殊处理，因为XSSF的边框颜色设置方式不同
                    }
                } else {
                    IndexedColors color = IndexedColors.valueOf(lineColor.toUpperCase());
                    short colorIndex = color.getIndex();
                    style.setBorderTop(borderStyle);
                    style.setTopBorderColor(colorIndex);
                    style.setBorderRight(borderStyle);
                    style.setRightBorderColor(colorIndex);
                    style.setBorderBottom(borderStyle);
                    style.setBottomBorderColor(colorIndex);
                    style.setBorderLeft(borderStyle);
                    style.setLeftBorderColor(colorIndex);

                }
            } catch (Exception e) {
                e.printStackTrace();
                log.warn("设置边框颜色失败: {}", lineColor, e);
                // 使用默认边框
                setDefaultBorder(style);
                throw e;
            }
        } else {
            // 设置默认边框
            setDefaultBorder(style);
        }

        // 缓存样式
        styleCache.put(cacheKey, style);
        return style;
    }

    // 设置默认边框
    private static void setDefaultBorder(CellStyle style) {
        BorderStyle thin = BorderStyle.THIN;
        style.setBorderTop(thin);
        style.setBorderRight(thin);
        style.setBorderBottom(thin);
        style.setBorderLeft(thin);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
    }

    // 根据类型设置单元格值（优化版本）
    private static void setCellValueBasedOnType(Cell cell, Object value) {
        if(value == null) {
            cell.setCellValue("");
            return;
        }

        try {
            if(value instanceof Number) {
                if(value instanceof Integer || value instanceof Long ||
                        value instanceof Short || value instanceof Byte) {
                    cell.setCellValue(((Number)value).doubleValue());
                } else if(value instanceof Double) {
                    cell.setCellValue((Double)value);
                } else if(value instanceof Float) {
                    cell.setCellValue((Float)value);
                } else if(value instanceof BigDecimal) {
                    cell.setCellValue(((BigDecimal)value).doubleValue());
                    // 设置数字格式（可选）
                    cell.getCellStyle().setDataFormat(
                            cell.getSheet().getWorkbook().createDataFormat().getFormat("#,##0.00"));
                }
            } else if(value instanceof String) {
                String strValue = (String)value;
                // 尝试解析为数字
                if(isNumeric(strValue)) {
                    try {
                        cell.setCellValue(Double.parseDouble(strValue));
                    } catch (Exception e) {
                        cell.setCellValue(strValue);
                    }
                } else if(isDate(strValue)) {
                    // 如果是日期格式
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = sdf.parse(strValue);
                        cell.setCellValue(date);
                        CellStyle dateStyle = cell.getSheet().getWorkbook().createCellStyle();
                        dateStyle.cloneStyleFrom(cell.getCellStyle());
                        dateStyle.setDataFormat(
                                cell.getSheet().getWorkbook().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
                        cell.setCellStyle(dateStyle);
                    } catch (ParseException e) {
                        cell.setCellValue(strValue);
                    }
                } else {
                    cell.setCellValue(strValue);
                }
            } else if(value instanceof Date) {
                cell.setCellValue((Date)value);
                // 设置日期格式
                CellStyle dateStyle = cell.getSheet().getWorkbook().createCellStyle();
                dateStyle.cloneStyleFrom(cell.getCellStyle());
                dateStyle.setDataFormat(
                        cell.getSheet().getWorkbook().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
                cell.setCellStyle(dateStyle);
            } else if(value instanceof Boolean) {
                cell.setCellValue((Boolean)value);
            } else {
                cell.setCellValue(value.toString());
            }
        } catch (Exception e) {
            log.warn("设置单元格值失败: {}, 值类型: {}", value, value.getClass().getName(), e);
            cell.setCellValue(value != null ? value.toString() : "");
        }
    }

    // 判断是否为数字
    private static boolean isNumeric(String str) {
        if(str == null || str.trim().isEmpty()) {
            return false;
        }
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    // 判断是否为日期字符串
    private static boolean isDate(String str) {
        if(str == null || str.trim().isEmpty()) {
            return false;
        }
        // 简单的日期格式判断
        return str.matches("\\d{4}-\\d{2}-\\d{2}( \\d{2}:\\d{2}:\\d{2})?");
    }


    /**
     * 创建单元格样式
     * @param workbook
     * @return
     */
    private static CellStyle contentCellStyle(Workbook workbook,String backgroundColor,String lineColor){
        // 创建单元格样式
        CellStyle style = workbook.createCellStyle();
        // 设置字体样式
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 10); // 字体大小
        //font.setBold(true); // 粗体
        font.setColor(IndexedColors.BLACK.getIndex()); // 字体颜色
        style.setFont(font);
        // 设置背景颜色和模式（可选）
        //style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        if(StringUtil.isNotEmpty(backgroundColor)){
            //XSSFColor color = new XSSFColor(new java.awt.Color(237,237,237), null);
            String[] colorStr = backgroundColor.split(",");
            int color1 = Integer.parseInt(colorStr[0]);
            int color2 = Integer.parseInt(colorStr[1]);
            int color3 = Integer.parseInt(colorStr[2]);

            XSSFColor color = new XSSFColor(new java.awt.Color(color1,color2,color3), null);
            style.setFillForegroundColor(color);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }else{
            XSSFColor color = new XSSFColor(new java.awt.Color(255,255,255), null);
            style.setFillForegroundColor(color);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }


        // 设置边框样式
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);

        // 设置边框颜色
        if(StringUtil.isNotEmpty(lineColor) && lineColor.equals("white")){
            style.setBottomBorderColor(IndexedColors.WHITE1.getIndex());
            style.setTopBorderColor(IndexedColors.WHITE1.getIndex());
            style.setLeftBorderColor(IndexedColors.WHITE1.getIndex());
            style.setRightBorderColor(IndexedColors.WHITE1.getIndex());
        }else if(StringUtil.isNotEmpty(lineColor) && lineColor.equals("blue")){
            style.setBottomBorderColor(IndexedColors.BLUE.getIndex());
            style.setTopBorderColor(IndexedColors.BLUE.getIndex());
            style.setLeftBorderColor(IndexedColors.BLUE.getIndex());
            style.setRightBorderColor(IndexedColors.BLUE.getIndex());
        }else if(StringUtil.isNotEmpty(lineColor) && lineColor.equals("red")){
            style.setBottomBorderColor(IndexedColors.RED.getIndex());
            style.setTopBorderColor(IndexedColors.RED.getIndex());
            style.setLeftBorderColor(IndexedColors.RED.getIndex());
            style.setRightBorderColor(IndexedColors.RED.getIndex());
        }else{
            style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            style.setTopBorderColor(IndexedColors.BLACK.getIndex());
            style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        }

        // 设置垂直居中
        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }

    /**
     * 创建背景单元格样式
     * @param workbook
     * @return
     */
    private static CellStyle backCellStyle(Workbook workbook){
        // 创建单元格样式（背景）
        CellStyle backStyle = workbook.createCellStyle();
        XSSFColor backColor = new XSSFColor(new java.awt.Color(190,62,23), null);
        backStyle.setFillForegroundColor(backColor);
        backStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return backStyle;
    }


    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    /**
     * excel文件另存为图片
     * @param sourFilePath
     * @param outputFilePath
     * @return
     */
    public static boolean excelToImg(String sourFilePath,String outputFilePath){
        boolean result = false;

        if (!getExcelLicense()) {
            return result;
        }

        try {

            OsInfo osInfo = SystemUtil.getOsInfo();
            if(osInfo.isLinux()){
                //FontSettings.setFontsFolder("/app/tools/Fonts/chinese", true);
                // 设置字体文件夹路径
                FontConfigs.setFontFolder("/app/server/fonts/chinese", true);
            }

            // 加载 Excel 文件
            com.aspose.cells.Workbook workbook = new com.aspose.cells.Workbook(sourFilePath);

            // 手动触发公式重新计算
            workbook.calculateFormula();

            // 获取第一个工作表
            Worksheet worksheet = workbook.getWorksheets().get(0);

            // 设置图片选项
            ImageOrPrintOptions imgOptions = new ImageOrPrintOptions();
            imgOptions.setImageFormat(ImageFormat.getPng());
            imgOptions.setImageType(ImageType.PNG);
            imgOptions.setHorizontalResolution(350); // 水平DPI（默认96）
            imgOptions.setVerticalResolution(350); // 垂直DPI

            imgOptions.setCellAutoFit(true);
            imgOptions.setOnePagePerSheet(true); // 将整个工作表渲染为一张图片
            imgOptions.setOnlyArea(true);        // 只导出有数据的区域（忽略空白）
            imgOptions.setTiffCompression(TiffCompression.COMPRESSION_LZW); // 压缩算法


            // 创建工作表渲染对象
            SheetRender sr = new SheetRender(worksheet, imgOptions);
            sr.toImage(0, outputFilePath);
            log.info("++++++++Excel文件另存为图片成功。");
            result = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * excel文件另存为图片
     * @param sourFilePath
     * @param outputFilePath
     * @return
     */
    public static boolean excelBigDataToImg(String sourFilePath,String outputFilePath){
        boolean result = false;

        if (!getExcelLicense()) {
            return result;
        }

        try {
            // 设置内存优化

            OsInfo osInfo = SystemUtil.getOsInfo();
            if(osInfo.isLinux()){
                //FontSettings.setFontsFolder("/app/tools/Fonts/chinese", true);
                // 设置字体文件夹路径
                FontConfigs.setFontFolder("/app/server/fonts/chinese", true);
            }

            // 加载Excel文件时使用优化选项
            LoadOptions loadOptions = new LoadOptions(LoadFormat.AUTO);
            loadOptions.setMemorySetting(MemorySetting.MEMORY_PREFERENCE);
            loadOptions.setCheckExcelRestriction(false); // 关闭Excel限制检查

            // 加载 Excel 文件
            com.aspose.cells.Workbook workbook = new com.aspose.cells.Workbook(sourFilePath);

            // 手动触发公式重新计算
            //workbook.calculateFormula();

            // 获取第一个工作表
            Worksheet worksheet = workbook.getWorksheets().get(0);

            // 设置图片选项
            ImageOrPrintOptions imgOptions = new ImageOrPrintOptions();
            imgOptions.setImageFormat(ImageFormat.getPng());
            imgOptions.setImageType(ImageType.PNG);
            imgOptions.setHorizontalResolution(96); // 水平DPI（默认96）
            imgOptions.setVerticalResolution(96); // 垂直DPI

            imgOptions.setCellAutoFit(true);
            imgOptions.setCellAutoFit(true);
            imgOptions.setOnePagePerSheet(false); // 将整个工作表渲染为一张图片
            imgOptions.setOnlyArea(true);        // 只导出有数据的区域（忽略空白）
            imgOptions.setTiffCompression(TiffCompression.COMPRESSION_LZW); // 压缩算法

            // 创建工作表渲染对象
            SheetRender sr = new SheetRender(worksheet, imgOptions);
            sr.toImage(0, outputFilePath);
            log.info("++++++++Excel文件另存为图片成功。");
            result = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取密钥
     * @return
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
     * 判断给定字符串是否是一个有效的数字。
     * 规则：仅允许一个小数点，其余必须为数字字符，不支持符号位或科学计数法。
     *
     * @param str 待验证的字符串
     * @return 如果是合法数字格式，返回 true；否则返回 false
     */
    public static boolean isValidNumber(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        //String regex = "^[-+]?((\\d+\\.?\\d*)|(\\.\\d+))([eE][-+]?\\d+)?$";
        String regex = "^[-+]?((\\d+\\.?\\d*)|(\\.\\d+))$";
        return str.trim().matches(regex);
    }

    public static int findRowByCellValue(Sheet sheet, String targetValue) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().equals(targetValue)) {
                    return row.getRowNum();  // 获取行号
                }
            }
        }
        return 0;  // 未找到
    }

}
