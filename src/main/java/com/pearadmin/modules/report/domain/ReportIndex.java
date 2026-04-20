package com.pearadmin.modules.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-07-03
 * 自助报表指标实体类
 *
 **/

@Data
@Alias("ReportIndex")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportIndex {

  @TableId(value = "index_id", type = IdType.AUTO)
  private Integer indexId;

  /**
   * 指标名称
   */
  private String indexName;

  /**
   * 指标类型 count 统计  details 明细
   */
  private String indexType;

  /**
   * 数据周期 day 日  month 月
   */
  private String dataCycle;

  /**
   * 分析角色 customer 客户  user 用户 account 账户
   */
  private String analRole;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 创建人
   */
  private String createUserId;

  /**
   * 分类ID
   */
  private Integer classifyId;

  /**
   * 父节点ID
   */
  private Integer parentId;

  /**
   * 排序ID
   */
  private Integer sortId;

  /**
   * 表名
   */
  private String tableName;

  /**
   * 列名
   */
  private String columnName;

  /**
   * 表达式
   */
  private String tableExpr;

  /**
   * 是否查询条件  1是  0否
   */
  private String isQuery;

  /**
   * 指标分类  lv率 tb同比  hb环比 zb占比
   */
  private String indexClass;

  /**
   * 分子表达式
   */
  private String fzBds;

  /**
   * 分每表达式
   */
  private String fmBds;

  /**
   * 分子表达式2
   */
  private String fzBdsEr;

  /**
   * 分每表达式2
   */
  private String fmBdsEr;

  /**
   * 是否包含非标准维度  0不包含 1包含
   */
  private String isIncludeNoStand;

  /**
   * 是否五项集约统计 1是 0否
   */
  private String isFiveInten;


  /**
   * 创建人姓名
   */
  @TableField(exist = false)
  private String realName;

  /**
   * 分类名称
   */
  @TableField(exist = false)
  private String classifyName;

  /**
   * 父指标名称
   */
  @TableField(exist = false)
  private String parentIndexName;

}
