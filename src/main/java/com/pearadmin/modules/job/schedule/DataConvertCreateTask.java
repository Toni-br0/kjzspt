package com.pearadmin.modules.job.schedule;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.quartz.base.BaseQuartz;
import com.pearadmin.modules.ppt.domain.ParamPojo;
import com.pearadmin.modules.ppt.domain.PptModelConfig;
import com.pearadmin.modules.ppt.mapper.PptModelConfigMapper;
import com.pearadmin.modules.ppt.service.impl.AutoValueServiceImpl;
import com.pearadmin.modules.ppt.service.impl.ConvertServiceImpl;
import com.pearadmin.modules.ppt.service.impl.DorisToMysqlServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 创建日期：2025-04-07
 * 数据转换生成任务
 **/

@Slf4j
@Component("dataConvertCreateTask")
public class DataConvertCreateTask implements BaseQuartz {

    @Resource
    private DorisToMysqlServiceImpl dorisToMysqlService;

    @Resource
    private PptModelConfigMapper pptModelConfigMapper;

    @Resource
    private ConvertServiceImpl convertService;

    @Resource
    private AutoValueServiceImpl autoValueService;

    @Override
    public void run(String params) throws Exception {
        log.info("+++++++++++++++++++ dorisToMysqlTask_run +++++++++++++++++++");

        JSONObject paramJson = new JSONObject();
        String accountPeriod ="";

        if(params.equals("week")){ //周PPT
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd"); // 定义日期格式
            LocalDate currentDate = LocalDate.now();
            // 找到本周的周四
            LocalDate thursday = currentDate.with(DayOfWeek.THURSDAY);
            accountPeriod = thursday.format(formatter);

            //accountPeriod ="20250417";

            paramJson.put("zqValue", "week");
            paramJson.put("accountPeriod", accountPeriod);

        }else if(params.equals("month")){ //月PPT

            // 获取当前日期
            Calendar calendar = Calendar.getInstance();
            // 获取当前日期的上个月
            calendar.add(Calendar.MONTH, -1);
            Date lastMonth = calendar.getTime();

            // 格式化日期为字符串
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
            accountPeriod = sdf.format(lastMonth);

            //accountPeriod ="202503";

            paramJson.put("zqValue", "month");
            paramJson.put("accountPeriod", accountPeriod);

        }

        paramJson.put("dzCode", "0000"); //全疆+ 16个地州数据
        paramJson.put("qxCode", "");

        log.info("++++++++++++++++dorisToMysqlTask_paramJson: {}",paramJson);

        JSONObject retDorisToMysqlJsonObject = dorisToMysqlService.dorisToMysql(paramJson.toString());

        log.info("++++++++++++++++dorisToMysqlTask_retDorisToMysqlJsonObject: {}",retDorisToMysqlJsonObject);

        String retCode = retDorisToMysqlJsonObject.getString("retCode");
        if(retCode.equals("0")){ //doris转Mysql数据成功
            //执行数据转换功能
            if(params.equals("week")){ //周PPT
                //查询周模板数据信息
                QueryWrapper<PptModelConfig> modelConfigQuery = new QueryWrapper<>();
                modelConfigQuery.eq("model_type", "2");
                List<PptModelConfig> pptModelConfigList = pptModelConfigMapper.selectList(modelConfigQuery);
                if(pptModelConfigList != null && pptModelConfigList.size() >0){
                    for(PptModelConfig pptModelConfig : pptModelConfigList){
                        String pptName = pptModelConfig.getPptName();
                        String modelLevel = pptModelConfig.getModelLevel();
                        int modelId = pptModelConfig.getModelId();

                        ParamPojo paramPojo = new ParamPojo();
                        paramPojo.setModelId(modelId);
                        paramPojo.setAccountPeriod(accountPeriod);
                        paramPojo.setDzCode(modelLevel);
                        paramPojo.setPptName(pptName);

                        //数据转换
                        JSONObject retConvertDataJsonObject = convertService.convertData(paramPojo);
                        log.info("+++++++++++++++week_retConvertDataJsonObject:{}",retConvertDataJsonObject);

                        String retConvertDataCode = retConvertDataJsonObject.getString("retCode");
                        if(retConvertDataCode.equals("0")){
                            //模板内容生成
                            log.info("+++++++++++++++week_DataConvertCreateTask_week_模板内容生成参数:{}",paramPojo);
                            JSONObject retAutoValueJsonObject = autoValueService.autoValue(paramPojo);
                            log.info("+++++++++++++++week_retAutoValueJsonObject:{}",retAutoValueJsonObject);
                            String retAutoValueCode = retAutoValueJsonObject.getString("retCode");
                            if(!retAutoValueCode.equals("0")){
                                throw  new Exception("模板内容生成失败! "+retConvertDataJsonObject.toJSONString());
                            }

                        }else{
                            throw  new Exception("数据转换失败! "+retConvertDataJsonObject.toJSONString());
                        }
                    }
                }
            }else if(params.equals("month")){ //月PPT
                //查询周模板数据信息
                QueryWrapper<PptModelConfig> modelConfigQuery = new QueryWrapper<>();
                modelConfigQuery.eq("model_type", "1");
                List<PptModelConfig> pptModelConfigList = pptModelConfigMapper.selectList(modelConfigQuery);
                if(pptModelConfigList != null && pptModelConfigList.size() >0){
                   for(PptModelConfig pptModelConfig : pptModelConfigList){
                       int modelId = pptModelConfig.getModelId();
                       String dzCode = pptModelConfig.getModelLevel();

                       ParamPojo paramPojo = new ParamPojo();
                       paramPojo.setModelId(modelId);
                       paramPojo.setDzCode(dzCode);
                       paramPojo.setAccountPeriod(accountPeriod);

                       //数据转换
                       JSONObject retConvertDataJsonObject = convertService.convertData(paramPojo);
                       log.info("+++++++++++++++month_retConvertDataJsonObject:{}",retConvertDataJsonObject);

                       String retConvertDataCode = retConvertDataJsonObject.getString("retCode");
                       if(retConvertDataCode.equals("0")){
                           //模板内容生成
                           JSONObject retAutoValueJsonObject = autoValueService.autoValue(paramPojo);
                           log.info("+++++++++++++++month_retAutoValueJsonObject:{}",retAutoValueJsonObject);
                           /*String retAutoValueCode = retAutoValueJsonObject.getString("retCode");
                           if(!retAutoValueCode.equals("0")){
                               throw  new Exception("模板内容生成失败! "+retConvertDataJsonObject.toJSONString());
                           }*/

                       }
                       /*else{
                           throw  new Exception("数据转换失败! "+retConvertDataJsonObject.toJSONString());
                       }*/

                   }
                }
            }
        }else{ //doris转Mysql数据失败
            throw  new Exception("doris转Mysql数据失败! "+retDorisToMysqlJsonObject.toJSONString());
        }
    }
}
