package com.pearadmin.modules.knowBase.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-06-23
 * 知识库知识信息表
 **/

@Data
@Alias("KnowbaseKnowInfo")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowbaseKnowInfo {
  /**
   * 知识ID
   */
  @TableId(value = "info_id", type = IdType.AUTO)
  private Integer infoId;

  /**
   * 知识标题
   */
  private String knowTitle;

  /**
   * 知识类型
   */
  private String knowType;

  /**
   * 知识类别
   */
  private String knowClass;

  /**
   * 知识内容
   */
  private String knowContent;

  /**
   * 附件地址
   */
  private String fjUrl;

  /**
   * 封面地址
   */
  private String fmUrl;

  /**
   * 视频地址
   */
  private String spUrl;

  /**
   * 分享权限
   */
  private String sharePurv;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 创建人ID
   */
  private String createUserId;

  /**
   * 创建人名称
   */
  private String createUserName;

  /**
   * 点赞量
   */
  private Integer dzCount;

  /**
   * 收藏量
   */
  private Integer scCount;

  /**
   * 访问量
   */
  private Integer fwCount;

  /**
   * 类别名称
   */
  @TableField(exist = false)
  private String knowClassName;

  /**
   * 查询日期
   */
  @TableField(exist = false)
  private String queryDate;

  /**
   * 查询类型
   */
  @TableField(exist = false)
  private String queryType;


}
