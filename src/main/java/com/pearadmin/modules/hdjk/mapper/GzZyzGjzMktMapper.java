package com.pearadmin.modules.hdjk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.hdjk.domain.GzZyzGjzMkt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 创建日期：2025-05-22
 **/

@Mapper
public interface GzZyzGjzMktMapper extends BaseMapper<GzZyzGjzMkt> {

    /**
     * 通过类型查询数据信息
     * @param mktType
     * @return
     */
    @Select({"<script> select * from  ${tableName}  where 1=1 "+
            " <when test='mktType !=null and mktType != \"\"'> and mkt_type = #{mktType} </when>" +
            " and hp_par_acct_day = #{acctDay} " +
            " order by order_id asc </script>"})
    public List<Map<String, Object>> selGzZyzGjzMktList(@Param("tableName") String tableName,@Param("mktType") String mktType,@Param("acctDay") String acctDay);

}
