package com.pearadmin.modules.hdjk.controller;

import com.pearadmin.modules.hdjk.util.RbExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

/**
 * 创建日期：2025-05-16
 **/

public class MyExcelTest {
  public static void main(String[] args) {
    String sourFilePath = "D:\\testPpt\\testExcel\\0523.xlsx";
    String outputFilePath = "D:\\testPpt\\testExcel\\0523_1.xlsx";

    //查询数据库，获取数据信息
    List<Map<String,Object>> dataList = new ArrayList<Map<String,Object>>();

    Map<String,Object> map = new HashMap<>();
    map.put("dz","乌鲁木齐");
    map.put("jdl",13565);
    map.put("zxl",521);
    map.put("jcl",492);
    map.put("zxjcl","94.43%");
    map.put("rmb",175);
    map.put("rcgl",46);
    map.put("rwcl","26.29%");
    map.put("ymb",5423);
    map.put("ycgl",112);
    map.put("ycg_lv","22.76%");
    map.put("ywcl","2.06%");

    Map<String,Object> map1 = new HashMap<>();
    map1.put("dz","伊犁");
    map1.put("jdl",7103);
    map1.put("zxl",547);
    map1.put("jcl",368);
    map1.put("zxjcl","67.28%");
    map1.put("rmb",49);
    map1.put("rcgl",12);
    map1.put("rwcl","24.49%");
    map1.put("ymb",1511);
    map1.put("ycgl",59);
    map1.put("ycg_lv","16.03%");
    map1.put("ywcl","3.9%");

    Map<String,Object> map2 = new HashMap<>();
    map2.put("dz","克州");
    map2.put("jdl",7103);
    map2.put("zxl",547);
    map2.put("jcl",368);
    map2.put("zxjcl","67.28%");
    map2.put("rmb",49);
    map2.put("rcgl",12);
    map2.put("rwcl","24.49%");
    map2.put("ymb",1511);
    map2.put("ycgl",59);
    map2.put("ycg_lv","16.03%");
    map2.put("ywcl","3.9%");

    Map<String,Object> map3 = new HashMap<>();
    map3.put("dz","克拉玛依");
    map3.put("jdl",7103);
    map3.put("zxl",547);
    map3.put("jcl",368);
    map3.put("zxjcl","67.28%");
    map3.put("rmb",49);
    map3.put("rcgl",12);
    map3.put("rwcl","24.49%");
    map3.put("ymb",1511);
    map3.put("ycgl",59);
    map3.put("ycg_lv","16.03%");
    map3.put("ywcl","3.9%");

    Map<String,Object> map4 = new HashMap<>();
    map4.put("dz","博州");
    map4.put("jdl",7103);
    map4.put("zxl",547);
    map4.put("jcl",368);
    map4.put("zxjcl","67.28%");
    map4.put("rmb",49);
    map4.put("rcgl",12);
    map4.put("rwcl","24.49%");
    map4.put("ymb",1511);
    map4.put("ycgl",59);
    map4.put("ycg_lv","16.03%");
    map4.put("ywcl","3.9%");

    dataList.add(map);
    dataList.add(map1);
    dataList.add(map2);
    dataList.add(map3);
    dataList.add(map4);

    Map<String,List<Map<String,Object>>> dataListMap = new HashMap<>();
    dataListMap.put("xh1",dataList);
    dataListMap.put("xh2",dataList);

    boolean creatFileReslut = RbExcelUtil.createExcelFile(sourFilePath,outputFilePath,dataList);
    //boolean creatFileReslut = RbExcelUtil.createExcelFile2(sourFilePath,outputFilePath,dataListMap);
    System.out.println("++++++creatFileReslut: "+creatFileReslut);
    if(creatFileReslut){
      sourFilePath ="D:\\testPpt\\testExcel\\0523_1.xlsx";
      outputFilePath = "D:\\testPpt\\testExcel\\0523_1.jpeg";
      boolean toImgResult = RbExcelUtil.excelToImg(sourFilePath,outputFilePath);
      System.out.println("++++++toImgResult: "+toImgResult);
    }

    try {
      FileInputStream file = new FileInputStream(new File("D:\\testPpt\\testExcel\\0523.xlsx")); // 可以是 .xls 或 .xlsx 文件
      Workbook workbook = WorkbookFactory.create(file); // 自动检测文件类型
      int numberOfSheets = workbook.getNumberOfSheets();
      System.out.println("Number of sheets: " + numberOfSheets);
      file.close();
    } catch (Exception e) {
      e.printStackTrace();
    }


  }
}
