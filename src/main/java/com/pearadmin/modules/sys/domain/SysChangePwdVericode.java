package com.pearadmin.modules.sys.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-08-28
 * 修改密码验证码
 **/

@Data
@Alias("SysChangePwdVericode")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysChangePwdVericode {

    /**
     * 验证码ID
     */
    @TableId
    private String vericodeId;


    /**
     * 用户名（电话号码）
     */
    private String userName;


    /**
     * 验证码
     */
    private String veriCode;

    /**
     * 消息内容
     */
    private String notticeContent;


    /**
     * 开始时间
     */
    private LocalDateTime startDate;

    /**
     * 结束时间
     */
    private LocalDateTime endDate;

    /**
     * 接口返回内容
     */
    private String retContent;

    /**
     * 是否成功 0成功  -1失败
     */
    private String isSuccess;

}
