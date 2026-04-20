package com.pearadmin.modules.hdjk.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 创建日期：2025-05-23
 **/

@Data
@Alias("GzZyzGjzMkt")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GzZyzGjzMkt {

    private Integer orderId;

    /**
     * 账期
     */
    private String hpParAcctDay;

    /**
     * 类型
     */
    private String mktType;

    /**
     * 地州
     */
    private String _col0;


    /**
     * 接单量
     */
    private String _col1;

    /**
     * 执行量
     */
    private String _col2;

    /**
     * 接触量
     */
    private String _col3;

    /**
     * 执行接触率
     */
    private String _col4;

    /**
     * 日目标
     */
    private String _col5;

    /**
     * 日成功量
     */
    private String _col6;

    /**
     * 日完成率
     */
    private String _col7;

    /**
     * 月目标
     */
    private String _col8;

    /**
     * 月成功量
     */
    private String _col9;

    /**
     * 月成功率
     */
    private String _col10;

    /**
     * 月完成率
     */
    private String _col11;

    /**
     * 地州1
     */
    private String _col12;

    /**
     * 销售能手
     */
    private String _col13;

    /**
     * 人匀日外呼量
     */
    private String _col14;

    /**
     * 当日成功量破零销售能手
     */
    private String _col15;

    /**
     * 达标销售助手
     */
    private String _col16;

    /**
     * 优秀销售能手
     */
    private String _col17;

}
