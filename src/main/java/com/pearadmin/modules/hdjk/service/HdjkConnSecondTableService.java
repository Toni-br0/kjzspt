package com.pearadmin.modules.hdjk.service;

import java.util.List;
import java.util.Map;

/**
 * 创建日期：2025-05-23
 * 日报连接第二个数据库服务类
 **/

public interface HdjkConnSecondTableService {

  /**
   * 获取zyz_gjz_mkt表数据
   * @return
   */
  public List<Map<String,Object>>  zyzGjzMkt(String tableName,String mktType,String acctDay);

}
