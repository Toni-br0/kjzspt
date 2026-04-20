package com.pearadmin.modules.job.schedule;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.quartz.base.BaseQuartz;
import com.pearadmin.common.tools.DateTimeUtil;
import com.pearadmin.modules.ppt.domain.ParamPojo;
import com.pearadmin.modules.ppt.domain.PptModelConfig;
import com.pearadmin.modules.ppt.mapper.PptModelConfigMapper;
import com.pearadmin.modules.ppt.service.impl.ConvertServiceImpl;
import com.pearadmin.modules.ppt.service.impl.PptServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 数据转换定时任务
 */

@Slf4j
@Component("dataConvertTask")
public class DataConvertTask implements BaseQuartz {

    @Resource
    private PptModelConfigMapper pptModelConfigMapper;

    @Resource
    private ConvertServiceImpl convertService;

    /**
     * 任务实现
     */
    @Override
    public void run(String params) {
        log.info("dataConvertTask-Params === >> " + params);
        log.info("++++++++++++++++dataConvertTask: 开始执行");


        try {
            QueryWrapper<PptModelConfig> modelQuery = new QueryWrapper<>();
            modelQuery.select("model_id","model_level","model_type","ppt_name");
            List<PptModelConfig> modelList = pptModelConfigMapper.selectList(modelQuery);
            if(modelList != null && modelList.size() >0){
                ParamPojo paramPojo = new ParamPojo();
                String accountPeriod = null;
                for(PptModelConfig pptModelConfig : modelList){
                    int modelId = pptModelConfig.getModelId();
                    String modelLevel = pptModelConfig.getModelLevel();
                    //模板类型  1月模板  2周模板  3日模板
                    String modelType = pptModelConfig.getModelType();
                    //模板名称,查询Oracle表用
                    String pptName = pptModelConfig.getPptName() ==null?"":pptModelConfig.getPptName();

                    //月模板
                    if(modelType.equals("1")){
                        //获取当前日期的上一个月
                        accountPeriod = DateTimeUtil.getLastMonth("yyyyMM");

                    }else if(modelType.equals("2")){ //周模板

                    }

                    paramPojo.setModelId(modelId);
                    paramPojo.setDzCode(modelLevel);
                    paramPojo.setAccountPeriod(accountPeriod);
                    paramPojo.setPptName(pptName);

                    JSONObject retJsonObject = convertService.convertData(paramPojo);

                    log.info("++++++++++++++++dataConvertTask: {}",retJsonObject);

                    Thread.sleep(3000l);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
