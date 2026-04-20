package com.pearadmin.modules.hdjk.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.pearadmin.modules.hdjk.mapper.GzZyzGjzMktMapper;
import com.pearadmin.modules.hdjk.service.HdjkConnSecondTableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 创建日期：2025-05-23
 * 日报连接第二个数据库服务类
 **/

@Service
@Slf4j
public class HdjkConnSecondTableServiceImpl implements HdjkConnSecondTableService {

    @Resource
    private GzZyzGjzMktMapper gzZyzGjzMktMapper;

    @Override
    @DS("second")
    public List<Map<String, Object>> zyzGjzMkt(String tableName,String mktType,String acctDay) {
        /*QueryWrapper<GzZyzGjzMkt> qu = new QueryWrapper<>();
        //qu.select("ORDER_ID,HP_PAR_ACCT_DAY,MKT_TYPE,COL_0,COL_1,COL_2,COL_3,COL_4,COL_5,COL_6,COL_7,COL_8,COL_9,COL_10,COL_11,COL_12,COL_13,COL_14,COL_15,COL_16,COL_17 ");
        qu.eq("mkt_type",mktType);
        qu.orderByAsc("order_id");
        List<Map<String, Object>> list = gzZyzGjzMktMapper.selectMaps(qu);*/

        List<Map<String, Object>> list = gzZyzGjzMktMapper.selGzZyzGjzMktList(tableName,mktType,acctDay);
        return list;
    }
}
