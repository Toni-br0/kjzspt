package com.pearadmin.modules.report.domain;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-07-09
 * 自助报表查询接口返回对象
 **/

@Data
@Alias("RetReportData")
public class RetReportData {
  //khjy_sp_mobcj_mkt_d
    /**
     * 账期
      */
  private String opDate;
    /**
     * 地州名称
     */
  private String hxLatnName;
    /**
     * 县分
     */
  private String hxAreaName;
    /**
     * 网格
     */
  private String xHx5BpName;
    /**
     * 城乡标识
     */
  private String regionType;
    /**
     * 客户id
     */
  private Long custId;
    /**
     * 账户id
     */
  private Long acctId;
    /**
     * 用户id
     */
  private Long servId;
  private String removeDate;
  private String custType;
  private String isGr;
  private String isGrF;
  private String salesnameZ;
  private String salesname;
  private String packageexesZ;
    /**
     * 高价值客户数
     */
  private String isMq;
    /**
     * 非高价值客户数
     */
  private String isMqF;
    /**
     * 超时数
     */
  private String isCs;
    /**
     * 未超时数
     */
  private String isCsF;
    /**
     * 接触执行量
     */
  private String isJczx;
    /**
     * 未接触执行量
     */
  private String isJczxF;
    /**
     * 预约上门量
     */
  private String isYysm;
    /**
     * 非预约上门量
     */
  private String isYysmF;
  private String yysmsj;
  private String effectiveTime;
  private String executedDate;
  private String contactResult;
  private String contactResultF;
  private String contactRemark;
  private String address;
    /**
     * 宽带分类
     */
  private String kdFenlei;
  private String kdFenlei1;
  private String kdFenlei2;
  private String kdFenlei3;
  private String kdFenlei4;
  private String kdFenlei5;

  //khjy_sp_mobcj_mkt_m
  private String contactResultDetails;

  //khjy_yycj_xt_d
    /**
     * 支局
      */
  private String hxRegionName;
  private String msAreaType ;
  private String state ;
  private String areaName ;
  private String msAreaId ;
  private String locId ;
  private String sysUserCode ;
  private String chnlName ;
  private String isYy ;
  private String isYyF ;
  private String isPd ;
  private String isPdF ;
  private Long isJC ;
  private Long isJCF ;
  private String cjfl ;


  //基础数据 khjy_dwd_new_cust_kb_m
  private Long isPerson;
  private Long isPersonF;
  private String custJt;
  private String custType2;
  private String custType4;
  private Object isPm;
  private Object isPmF;
  private String partyNbrSn;
  private String depName;
  private String isCustLog;
  private String isCustLogF;
  private String industryTypeTwo;
  //private String custCretaeDate;
  private Object custCretaeDate;
  private Long isGjz;
  private Long isGjzF;
  private String nation;
  private Long starLevelCd;
  private Long productOfferInstanceId;

  private String accNbr;
  private String cp;
  private String cz;
  private String hy;
  private Long ydcpAll;
  private Long kdcpAll;
  private Long itvcpAll;
  private Long yxcps;
  private Long ydcp;
  private Long kdcp;
  private Long itvcp;
  private Long zjcp;
  private Long ydcz;
  private Long kdcz;
  private Long itvjf;
  private Long ydhy;
  private Long kdhy;
  private Long ydhyJt;
  private Long kdhyJt;
  private Integer packageexes;
  private Long brdSpeedM3Mb;
  private Long isQf;
  private Long isQfF;
  private float qfje;
  private Long qfzq;
  private Long is5gzd;
  private Long is5gzdF;
  private Long isHy;
  private Long isHyF;
  private Long is5g;
  private Long is5gF;
  private Long is5gb;
  private Long is5gbF;
  private Long is5gtc;
  private Long is5gtcF;
  private Long isFmb5g;
  private Long isFmb5gF;
  private Long isFmb5gb;
  private Long isFmb5gbF;
  private Long isKj;
  private Long isKjF;
  private Long isWf;
  private Long isWfF;
  //isWfzn
  private Long isDljs;
  private Long isDljsF;
  private Long isQzxsp;
  private Long isQzxspF;
  private Long isQzsl;
  private Long isQzslF;
  private Long isFttr;
  private Long isFttrF;
  private Long is10gpon;
  private Long is10gponF;
  private Long isSfllb;
  private Long isSfllbF;
  private float avgCharge;
  private float fCharge1;
  private float fCharge2;
  private float fCharge3;
  private float shBillingCharge;
  private float avgLlycfy;
  private float llycfy1;
  private float llycfy2;
  private float llycfy3;
  private float avgYyycfy;
  private float yyycfy1;
  private float yyycfy2;
  private float yyycfy3;
  private float useFlux;
  private float voiceDur;
  private float callingDur;


  //流量深耕 收费流量包办理 khjy_dwd_llsg_sfllb_m
  private String llbfl;
  private float sfCon;
  private float sfSr;
  private float qyCon;
  private float qySr;
  private String bossId;
  private String applyDate;


  //高值拓保  高价值月到达 khjy_dwd_gjz_ydd_m
  private String lx;
  private int pzkhs;
  private int ddkhs;
  private int xlkhs;
  private int clkhs;
  private int clzldkhs;
  private float pzbyl;


  //高值拓保  高价值到达流失 khjy_dwd_gjz_ljls_m
  private String lsfl;
  private int ljlskhs;
  private int ljjdkhs;
  private int ljzfkhs;
  private int ljlwkhs;
  private int dylskhs;
  private int dyjdkhs;
  private int dyzfkhs;
  private int dylwkhs;

  //融合重耕月表 khjy_dwd_rhcg_yjxsp_m
  private String srfd;
  private long khl;
  private long xspl;
  private float xspj;
  private long qzkhl;
  private long qzxspl;
  private float qzxspj;
  private long G5khl;
  private long G5xspl;
  private float G5xspj;
  private long xhykhl;
  private long xhyxspl;
  private float xhyxspj;


 //退惠回流 100273退恵回流 khjy_dwd_100273_tfhl
  private String infoVal;
  private String offerName;
  private long offerGrade;
  private LocalDateTime offerEffDate;
  private LocalDateTime offerExpDate;
  private String servState;
  private long billingArriveFlag;
  private String is100273;
  private String is100273F;
  private String srTs;


 //退惠回流 非100273退恵回流 khjy_dwd_100273_other_zk
 private String  offerId;
 private long  isExistTqct;
  private long  isExistTqctF;
 private float discountAmount;
 //private float srTs;

  //宽带拆晚（控流失） khjy_sp_mobcj_mkt_d
  //private int packageexesZ;

  //宽带拆晚（控流失） 宽带拆挽入系统日表  khjy_yycj_xt_d


}
