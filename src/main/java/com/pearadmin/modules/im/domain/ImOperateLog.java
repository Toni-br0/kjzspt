package com.pearadmin.modules.im.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-04-30
 * IM操作日志
 **/

@Data
@Alias("ImOperateLog")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImOperateLog {
    /**
     * 管理ID
     */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Integer logId;

    /**
     * bind 绑定 push 推送
     */
    private String operateType;

    /**
     * 模板ID
     */
    private Integer modelId;

    /**
     * 模板ID
     */
    private String modelName;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 对象ID
     */
    private String objectId;

    /**
     * 对象名称
     */
    private String objectName;

    /**
     * 文件生成时间
     */
    private LocalDateTime fileCreatTime;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 是否成功 0 成功 -1失败
     */
    private String isSuccess;

    /**
     * 执行结果
     */
    private String operateResult;

    /**
     * 操作人
     */
    private String operatePerson;

    /**
     * IM文件推送管理ID
     */
    private Integer manageId;

    /**
     * 查询操作日期
     */
    @TableField(exist = false)
    private String queryOperateTime;

}
