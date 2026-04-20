package com.pearadmin.modules.im.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.SensitiveDataUtils;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.*;
import com.pearadmin.modules.im.mapper.*;
import com.pearadmin.modules.im.service.IImDorisConnService;
import com.pearadmin.modules.im.service.IPushObjectManageService;
import com.pearadmin.modules.im.util.ImUtil;
import com.pearadmin.modules.sys.domain.SysConfig;
import com.pearadmin.modules.sys.domain.SysDept;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.mapper.SysConfigMapper;
import com.pearadmin.modules.sys.mapper.SysDeptMapper;
import com.pearadmin.modules.wgppt.domain.VWgzsUserLevelD;
import com.pearadmin.modules.wgppt.domain.WgDtreeData;
import com.pearadmin.modules.wgppt.domain.WgpptPushObjectManage;
import com.pearadmin.modules.wgppt.domain.WgpptPushObjectManageImp;
import com.pearadmin.modules.wgppt.service.WgPptDorisConnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.spring.web.json.Json;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 创建日期：2025-04-27
 * 推送对象管理
 **/

@Service
@Slf4j
public class PushObjectManageServiceImpl implements IPushObjectManageService {

    @Value("${im-batch-import-model-path}")
    private String imBatchImportModelPath;

    @Resource
    private ImPushObjectManageMapper imPushObjectManageMapper;

    @Resource
    private ImFilePushManageMapper imFilePushManageMapper;

    @Resource
    private ImOperateLogMapper imOperateLogMapper;

    @Resource
    private ImUtil imUtil;

    @Resource
    private ImHdjkPushManageMapper imRbFilePushManageMapper;

    @Resource
    private SysDeptMapper sysDeptMapper;

    @Resource
    private WgPptDorisConnService wgPptDorisConnService;

    @Resource
    private IImDorisConnService imDorisConnService;

    @Resource
    private ImPushOrgMapper imPushOrgMapper;

    @Resource
    private ImBindInfoMapper imBindInfoMapper;

    @Resource
    private SysConfigMapper sysConfigMapper;

    /**
     * 获取标签分组推送对象列表
     * @param imPushObjectManage
     * @return
     */
    @Override
    public List<ImPushObjectManage> getPushObjectManageList(ImPushObjectManage imPushObjectManage) {
        List<ImPushObjectManage> list = new ArrayList<>();

        if(StringUtil.isNotEmpty(imPushObjectManage.getObjectAreaId()) && imPushObjectManage.getObjectAreaId().equals("1")){
            imPushObjectManage.setObjectAreaId("");
        }
        list = imPushObjectManageMapper.selectListByWhere(imPushObjectManage);

        if(list != null && list.size() >0){
            list.forEach(thImPushObjectManage -> {
                thImPushObjectManage.setPushObjectId(SensitiveDataUtils.desensitizePhone(thImPushObjectManage.getPushObjectId()));
            });
        }

        return list;
    }


    /**
     * 获取组织维度推送对象列表
     * @param imPushObjectManage
     * @return
     */
    @Override
    public List<VWgzsUserLevelD> getZzwdPushObjectManageList(ImPushObjectManage imPushObjectManage) {
            String dataLevel = imPushObjectManage.getDataLevel();

            List<VWgzsUserLevelD> vWgzsUserLevelDList = imDorisConnService.selectImPushObjectByWhere(imPushObjectManage,dataLevel);
            if(vWgzsUserLevelDList != null && vWgzsUserLevelDList.size() >0){
                for(VWgzsUserLevelD vWgzsUserLevelD : vWgzsUserLevelDList){
                    vWgzsUserLevelD.setPushObjectType("users");
                    vWgzsUserLevelD.setBindState("0");

                    if(imPushObjectManage.getDataLevel().equals("全疆")){
                        String level = vWgzsUserLevelD.getLevel()==null?"":vWgzsUserLevelD.getLevel();
                        if(level.equals("全疆")){
                            vWgzsUserLevelD.setAreaName("全疆");
                        }else if(level.equals("本地网")){
                            vWgzsUserLevelD.setAreaName(vWgzsUserLevelD.getHxLatnName());
                        }else if(level.equals("县分")){
                            vWgzsUserLevelD.setAreaName(vWgzsUserLevelD.getHxAreaName());
                        }else if(level.equals("支局")){
                            vWgzsUserLevelD.setAreaName(vWgzsUserLevelD.getHxRegionName());
                        }else if(level.equals("网格")){
                            vWgzsUserLevelD.setAreaName(vWgzsUserLevelD.getXHx5BpName());
                        }

                    }else if(imPushObjectManage.getDataLevel().equals("本地网")){
                        vWgzsUserLevelD.setAreaName(vWgzsUserLevelD.getHxLatnName());
                    }else if(imPushObjectManage.getDataLevel().equals("县分")){
                        vWgzsUserLevelD.setAreaName(vWgzsUserLevelD.getHxAreaName());
                    }else if(imPushObjectManage.getDataLevel().equals("支局")){
                        vWgzsUserLevelD.setAreaName(vWgzsUserLevelD.getHxRegionName());
                    }else if(imPushObjectManage.getDataLevel().equals("网格")){
                        vWgzsUserLevelD.setAreaName(vWgzsUserLevelD.getXHx5BpName());
                    }

                    //获取绑定时间
                    ImBindInfo imBindInfo = imBindInfoMapper.selectById(vWgzsUserLevelD.getTelephone());
                    if(imBindInfo != null){
                        vWgzsUserLevelD.setBindState("1");
                        vWgzsUserLevelD.setBindTime(imBindInfo.getBindTime());
                    }

                    //对电话号码脱敏
                    SysConfig sysConfig = sysConfigMapper.selectByCode("phone_sensitive");
                    if(sysConfig != null && sysConfig.getConfigValue().equals("1")){
                        // 对结果集进行脱敏处理
                        vWgzsUserLevelD.setTelephone(SensitiveDataUtils.desensitizePhone(vWgzsUserLevelD.getTelephone()));
                    }
                }

                //vWgzsUserLevelDList.removeIf(userLevel -> userLevel.getBindState().equals("0"));
            }

        return vWgzsUserLevelDList;
    }


    /**
     * 保存推送对象
     * @param imPushObjectManage
     * @return
     */
    @Override
    public JSONObject save(ImPushObjectManage imPushObjectManage) {
        JSONObject retJsonObject = new JSONObject();
        int count =0;

        //获取当前登录用户信息
        SysUser currentUser = UserContext.currentUser();

        if(imPushObjectManage.getManageId() != null){

            ImPushObjectManage queryImPushObjectManage = imPushObjectManageMapper.selectById(imPushObjectManage.getManageId());
            if(queryImPushObjectManage != null){

                //修改时检查是否重复的推送对象ID
                QueryWrapper<ImPushObjectManage> checkImQuery = new QueryWrapper<>();
                checkImQuery.eq("push_object_id",imPushObjectManage.getPushObjectId());
                checkImQuery.ne("manage_id",imPushObjectManage.getManageId());
                checkImQuery.last("limit 1");

                ImPushObjectManage checkImPushObjectManage = imPushObjectManageMapper.selectOne(checkImQuery);
                if(checkImPushObjectManage != null){
                    retJsonObject.put("retCode","-1");
                    retJsonObject.put("retMsg","保存失败，推送对象ID已存在");
                    return retJsonObject;
                }

                String pushObjectId = queryImPushObjectManage.getPushObjectId();
                //如果推送对象ID改变，则重置绑定时间
                if(!pushObjectId.equals(imPushObjectManage.getPushObjectId())){
                    queryImPushObjectManage.setBindTime(null);
                }

                queryImPushObjectManage.setPushObjectType(imPushObjectManage.getPushObjectType());
                queryImPushObjectManage.setPushObjectId(imPushObjectManage.getPushObjectId());
                queryImPushObjectManage.setPushObjectName(imPushObjectManage.getPushObjectName());
                queryImPushObjectManage.setObjectAreaId(imPushObjectManage.getObjectAreaId());
                queryImPushObjectManage.setObjectArea(imPushObjectManage.getObjectArea());
                queryImPushObjectManage.setCreateUserId(currentUser.getUserId());
                queryImPushObjectManage.setRemark(imPushObjectManage.getRemark());

                count = imPushObjectManageMapper.updateById(queryImPushObjectManage);
                if(count >0){
                    retJsonObject.put("retCode","0");
                }else{
                    retJsonObject.put("retCode","-1");
                    retJsonObject.put("retMsg","保存失败");
                }
            }

        }else{
            QueryWrapper<ImPushObjectManage>  queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("push_object_id",imPushObjectManage.getPushObjectId());
            ImPushObjectManage queryImPushObjectManage = imPushObjectManageMapper.selectOne(queryWrapper);
            if(queryImPushObjectManage != null){
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","保存失败，推送对象ID已存在");
            }else{
                imPushObjectManage.setCreateTime(LocalDateTime.now());
                imPushObjectManage.setCreateUserId(currentUser.getUserId());
                count = imPushObjectManageMapper.insert(imPushObjectManage);
                if(count >0){
                    retJsonObject.put("retCode","0");
                }else{
                    retJsonObject.put("retCode","-1");
                    retJsonObject.put("retMsg","保存失败");
                }
            }

        }

       return retJsonObject;
    }

    /**
     * 根据ID查询对象信息
     * @param manageId
     * @return
     */
    @Override
    public ImPushObjectManage getById(int manageId) {

        return imPushObjectManageMapper.selectById(manageId);
    }

    /**
     * 删除推送对象信息
     * @param manageId
     * @return
     */
    @Override
    public boolean remove(int manageId) {
        int count = imPushObjectManageMapper.deleteById(manageId);
        if(count >0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 获取推送对象管理下拉框
     * @return
     */
    @Override
    public Object getPushObjectManageSelect() {
        List<JSONObject> retList = new ArrayList<>();
        QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        queryWrapper.isNotNull("bind_time");
        List<ImPushObjectManage> list = imPushObjectManageMapper.selectList(queryWrapper);
        if(list != null && list.size() >0){
            for(ImPushObjectManage imPushObjectManage : list){
                String pushObjectType = imPushObjectManage.getPushObjectType();
                /*if(pushObjectType.equals("users")){
                    pushObjectType = "用户";
                }else if(pushObjectType.equals("group")){
                    pushObjectType = "群";
                }*/

                JSONObject jsonObject = new JSONObject();
                //jsonObject.put("name","["+pushObjectType+"]"+imPushObjectManage.getPushObjectName());
                jsonObject.put("id",imPushObjectManage.getManageId());
                jsonObject.put("name","["+imPushObjectManage.getObjectArea()+"]"+imPushObjectManage.getPushObjectName());
                jsonObject.put("value",imPushObjectManage.getPushObjectId());
                jsonObject.put("type",imPushObjectManage.getPushObjectType());
                retList.add(jsonObject);
            }
        }

        return retList;
    }


    /**
     * 根据当前登录人的部门获取推送对象管理下拉框
     * @return
     */
    @Override
    public Object getPushObjectByAreaSelect() {
        List<JSONObject> retList = new ArrayList<>();

        String areaName ="";
        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        String deptId = currentUser.getDeptId();
        if(!deptId.equals("1")){ //部门不是分公司
            SysDept sysDept = sysDeptMapper.selectById(deptId);
            if(sysDept != null){
                areaName = sysDept.getDeptName();
            }
        }


        QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        queryWrapper.isNotNull("bind_time");

        if(!areaName.equals("")){
            queryWrapper.eq("object_area",areaName);
        }

        List<ImPushObjectManage> list = imPushObjectManageMapper.selectList(queryWrapper);
        if(list != null && list.size() >0){
            for(ImPushObjectManage imPushObjectManage : list){
                String pushObjectType = imPushObjectManage.getPushObjectType();
                /*if(pushObjectType.equals("users")){
                    pushObjectType = "用户";
                }else if(pushObjectType.equals("group")){
                    pushObjectType = "群";
                }*/

                JSONObject jsonObject = new JSONObject();
                //jsonObject.put("name","["+pushObjectType+"]"+imPushObjectManage.getPushObjectName());
                jsonObject.put("id",imPushObjectManage.getManageId());
                jsonObject.put("name","["+imPushObjectManage.getObjectArea()+"]"+imPushObjectManage.getPushObjectName());
                jsonObject.put("value",imPushObjectManage.getPushObjectId());
                jsonObject.put("type",imPushObjectManage.getPushObjectType());
                retList.add(jsonObject);
            }
        }

        return retList;
    }

    /**
     * 获取推送对象管理下拉框（选中值）
     * @param manageId
     * @return
     */
    @Override
    public Object getPushObjectManageSelectSel(String manageId) {
        List<JSONObject> retList = new ArrayList<>();

        ImFilePushManage imFilePushManage = imFilePushManageMapper.selectById(manageId);
        if(imFilePushManage != null){
            String pushObjectId = imFilePushManage.getPushObjectId();

            QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("create_time");
            queryWrapper.isNotNull("bind_time");
            List<ImPushObjectManage> list = imPushObjectManageMapper.selectList(queryWrapper);
            if(list != null && list.size() >0){
                String[] pushObjectIdArr = pushObjectId.split(";");

                for(ImPushObjectManage imPushObjectManage : list){
                    String pushObjectType = imPushObjectManage.getPushObjectType();
                    int queryManageId = imPushObjectManage.getManageId();

                /*if(pushObjectType.equals("users")){
                    pushObjectType = "用户";
                }else if(pushObjectType.equals("group")){
                    pushObjectType = "群";
                }*/

                    JSONObject jsonObject = new JSONObject();
                    //jsonObject.put("name","["+pushObjectType+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("name","["+imPushObjectManage.getObjectArea()+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("value",imPushObjectManage.getPushObjectId());
                    jsonObject.put("type",imPushObjectManage.getPushObjectType());
                    jsonObject.put("id",imPushObjectManage.getManageId());

                    /*if(pushObjectId.indexOf(queryManageId+"") >=0){
                        jsonObject.put("selected",true);
                    }*/


                    if(pushObjectIdArr != null && pushObjectIdArr.length > 0){
                        for(String strPushObjectId :pushObjectIdArr){
                            int iPushObjectId = Integer.parseInt(strPushObjectId);

                            if(iPushObjectId == queryManageId){
                                jsonObject.put("selected",true);
                            }
                        }
                    }

                    retList.add(jsonObject);
                }
            }

        }

        return retList;
    }


    /**
     * 获取推送对象管理领导下拉框（选中值）
     * @param manageId
     * @return
     */
    @Override
    public Object getPushObjectManageLeaderSelectSel(String manageId) {
        List<JSONObject> retList = new ArrayList<>();

        ImFilePushManage imFilePushManage = imFilePushManageMapper.selectById(manageId);
        if(imFilePushManage != null){
            String pushObjectId = imFilePushManage.getPushObjectLeaderId();

            QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("create_time");
            queryWrapper.isNotNull("bind_time");
            List<ImPushObjectManage> list = imPushObjectManageMapper.selectList(queryWrapper);
            if(list != null && list.size() >0){
                String[] pushObjectIdArr = null;
                if(StringUtil.isNotEmpty(pushObjectId)){
                    pushObjectIdArr = pushObjectId.split(";");
                }

                for(ImPushObjectManage imPushObjectManage : list){
                    String pushObjectType = imPushObjectManage.getPushObjectType();
                    int queryManageId = imPushObjectManage.getManageId();

                /*if(pushObjectType.equals("users")){
                    pushObjectType = "用户";
                }else if(pushObjectType.equals("group")){
                    pushObjectType = "群";
                }*/

                    JSONObject jsonObject = new JSONObject();
                    //jsonObject.put("name","["+pushObjectType+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("name","["+imPushObjectManage.getObjectArea()+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("value",imPushObjectManage.getPushObjectId());
                    jsonObject.put("type",imPushObjectManage.getPushObjectType());
                    jsonObject.put("id",imPushObjectManage.getManageId());

                    /*if(pushObjectId.indexOf(queryManageId+"") >=0){
                        jsonObject.put("selected",true);
                    }*/


                    if(pushObjectIdArr != null && pushObjectIdArr.length > 0){
                        for(String strPushObjectId :pushObjectIdArr){
                            int iPushObjectId = Integer.parseInt(strPushObjectId);
                            if(iPushObjectId == queryManageId){
                                jsonObject.put("selected",true);
                            }
                        }
                    }

                    retList.add(jsonObject);
                }
            }

        }

        return retList;
    }


    /**
     * 标签分组一键绑定
     * @param param
     * @return
     */
    @Override
    public JSONObject binding(String param) {
        JSONObject retJson  = new JSONObject();
        ImOperateLog imOperateLog = new ImOperateLog();
        ImPushObjectManage imPushObjectManage = null;
        int manageId =0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            manageId = paramJson.getInteger("manageId");
            imPushObjectManage = imPushObjectManageMapper.selectById(manageId);
            if(imPushObjectManage != null){
                String pushObjectId = imPushObjectManage.getPushObjectId();
                if(StringUtil.isNotEmpty(pushObjectId)){

                    //执行绑定操作
                    String bindingResult = imUtil.relBind(pushObjectId);
                    log.info("++++++++++绑定结果：{}",bindingResult);
                    if(StringUtil.isNotEmpty(bindingResult)){
                        JSONObject bindingJson = JSONObject.parseObject(bindingResult);
                        String status = bindingJson.getString("status");
                        String msg = bindingJson.getString("msg");
                        //绑定成功
                        if(StringUtil.isNotEmpty(bindingResult) && "success".equals(status)){
                            imPushObjectManage.setBindTime(LocalDateTime.now());
                            imPushObjectManage.setBindState("1");
                            imPushObjectManageMapper.updateById(imPushObjectManage);

                            retJson.put("retCode","0");
                            retJson.put("retMsg","绑定成功！");
                            retJson.put("addRetMsg","绑定成功: "+bindingResult);
                        }else{
                            retJson.put("retCode","-1");
                            retJson.put("retMsg", "绑定失败:"+msg);
                            retJson.put("addRetMsg","绑定失败: "+bindingResult);
                        }
                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","IM绑定接口返回空");
                        retJson.put("addRetMsg","绑定接口返回空");
                    }
                }
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","绑定失败，根据["+manageId+"]未查询到相关信息");
                retJson.put("addRetMsg","绑定失败，根据["+manageId+"]未查询到相关信息");
            }
        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常,请稍后再试！");
            retJson.put("addRetMsg","系统异常:"+e.getMessage());
        }finally {
            if(imPushObjectManage != null){
                imOperateLog.setOperateType("bind");
                imOperateLog.setObjectId(imPushObjectManage.getPushObjectId());
                imOperateLog.setObjectName(imPushObjectManage.getPushObjectName());
                imOperateLog.setOperateTime(LocalDateTime.now());
                imOperateLog.setManageId(manageId);

                String retCode = retJson.getString("retCode");
                String retMsg = retJson.getString("addRetMsg");
                if(StringUtil.isNotEmpty(retMsg) && retMsg.length()>1000){
                    retMsg = retMsg.substring(0,1000);
                }

                imOperateLog.setIsSuccess(retCode);
                imOperateLog.setOperateResult(retMsg);

                //获取当前登录用户信息
                SysUser currentUser = UserContext.currentUser();
                String realName = currentUser.getRealName();
                imOperateLog.setOperatePerson(realName);

                imOperateLogMapper.insert(imOperateLog);
            }
        }
        return retJson;
    }

    /**
     * 组织维度一键绑定
     * @param param
     * @return
     */
    @Override
    public JSONObject zzwdBinding(String param) {
        JSONObject retJson  = new JSONObject();
        ImOperateLog imOperateLog = new ImOperateLog();
        String pushObjectId = "";
        String pushObjectName = "";
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            pushObjectId = paramJson.getString("pushObjectId");
            pushObjectName = paramJson.getString("pushObjectName");

            if(StringUtil.isNotEmpty(pushObjectId)){

                //执行绑定操作
                String bindingResult = imUtil.relBind(pushObjectId);
                log.info("++++++++++绑定结果：{}",bindingResult);
                if(StringUtil.isNotEmpty(bindingResult)){
                    JSONObject bindingJson = JSONObject.parseObject(bindingResult);
                    String status = bindingJson.getString("status");
                    String msg = bindingJson.getString("msg");
                    //绑定成功
                    if(StringUtil.isNotEmpty(bindingResult) && "success".equals(status)){
                        retJson.put("retCode","0");
                        retJson.put("retMsg","绑定成功！");
                        retJson.put("addRetMsg","绑定成功: "+bindingResult);

                        addImBindInfo(pushObjectId,pushObjectName);
                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg", "绑定失败:"+msg);
                        retJson.put("addRetMsg","绑定失败: "+bindingResult);
                    }
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","IM绑定接口返回空");
                    retJson.put("addRetMsg","绑定接口返回空");
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常,请稍后再试！");
            retJson.put("addRetMsg","系统异常:"+e.getMessage());
        }finally {

                imOperateLog.setOperateType("bind");
                imOperateLog.setObjectId(pushObjectId);
                imOperateLog.setObjectName(pushObjectName+"["+pushObjectId+"]");
                imOperateLog.setOperateTime(LocalDateTime.now());

                String retCode = retJson.getString("retCode");
                String retMsg = retJson.getString("addRetMsg");
                if(StringUtil.isNotEmpty(retMsg) && retMsg.length()>1000){
                    retMsg = retMsg.substring(0,1000);
                }

                imOperateLog.setIsSuccess(retCode);
                imOperateLog.setOperateResult(retMsg);

                //获取当前登录用户信息
                SysUser currentUser = UserContext.currentUser();
                String realName = currentUser.getRealName();
                imOperateLog.setOperatePerson(realName);

                imOperateLogMapper.insert(imOperateLog);

        }
        return retJson;
    }


    /**
     * 标签分组批量一键绑定
     * @param param
     * @return
     */
    @Override
    public JSONObject batchBinding(String param) {
        JSONObject retJson  = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String manageIds = paramJson.getString("manageIds");
            if(StringUtil.isNotEmpty(manageIds)){
                String[] arrManageIds = manageIds.split(",");
                if(arrManageIds != null && arrManageIds.length >0){
                    for(int i=0;i<arrManageIds.length;i++){
                        String manageId = arrManageIds[i];
                        int iManageId = Integer.parseInt(manageId);
                        ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectById(iManageId);
                        if(imPushObjectManage != null){
                            String pushObjectId = imPushObjectManage.getPushObjectId();
                            if(StringUtil.isNotEmpty(pushObjectId)){

                                String bindingResult ="";
                                //执行绑定操作
                                try{
                                    bindingResult = imUtil.relBind(pushObjectId);
                                }catch (Exception ex){
                                    ex.printStackTrace();
                                }

                                log.info("++++++++++绑定结果：{}",bindingResult);
                                if(StringUtil.isNotEmpty(bindingResult)){
                                    JSONObject bindingJson = JSONObject.parseObject(bindingResult);
                                    String status = bindingJson.getString("status");
                                    String msg = bindingJson.getString("msg");

                                    //绑定成功
                                    if(StringUtil.isNotEmpty(bindingResult) && "success".equals(status)){

                                        imPushObjectManage.setBindTime(LocalDateTime.now());
                                        imPushObjectManage.setBindState("1");
                                        imPushObjectManageMapper.updateById(imPushObjectManage);

                                        retJson.put("retCode","0");
                                        retJson.put("retMsg","绑定成功！");
                                        retJson.put("addRetMsg","绑定成功: "+bindingResult);
                                    }else{
                                        retJson.put("retCode","-1");
                                        retJson.put("retMsg","绑定失败:"+msg);
                                        retJson.put("addRetMsg","绑定失败: "+bindingResult);

                                    }
                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","绑定接口返回空");
                                    retJson.put("addRetMsg","绑定接口返回空");

                                }
                            }
                        }else{
                            retJson.put("retCode","-1");
                            retJson.put("retMsg","根据ID["+iManageId+"]未查询到相关信息！");
                            retJson.put("addRetMsg","根据ID["+iManageId+"]未查询到相关信息！");

                        }

                        //保存操作日志
                        if(imPushObjectManage != null){
                            ImOperateLog imOperateLog = new ImOperateLog();
                            imOperateLog.setOperateType("bind");

                            imOperateLog.setObjectId(imPushObjectManage.getPushObjectId());
                            imOperateLog.setObjectName(imPushObjectManage.getPushObjectName());
                            imOperateLog.setOperateTime(LocalDateTime.now());
                            imOperateLog.setManageId(iManageId);

                            String retCode = retJson.getString("retCode");
                            String retMsg = retJson.getString("addRetMsg");
                            if(StringUtil.isNotEmpty(retMsg) && retMsg.length()>1000){
                                retMsg = retMsg.substring(0,1000);
                            }

                            imOperateLog.setIsSuccess(retCode);
                            imOperateLog.setOperateResult(retMsg);

                            //获取当前登录用户信息
                            SysUser currentUser = UserContext.currentUser();
                            String realName = currentUser.getRealName();
                            imOperateLog.setOperatePerson(realName);

                            imOperateLogMapper.insert(imOperateLog);
                        }
                    }
                }

            }

        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","系统异常，请稍后再试！");
        }
        return retJson;
    }

    /**
     * 组织维度批量一键绑定
     * @param param
     * @return
     */
    @Override
    public JSONObject zzwdBatchBinding(String param) {
        JSONObject retJson  = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String pushObjectIds = paramJson.getString("pushObjectIds");
            if(StringUtil.isNotEmpty(pushObjectIds)){
                String[] arrPushObjectIds = pushObjectIds.split(",");
                if(arrPushObjectIds != null && arrPushObjectIds.length >0){
                    for(int i=0;i<arrPushObjectIds.length;i++){

                        String phshObjectPhone = "";
                        String phshObjectName = "";

                        String pushObjectId = arrPushObjectIds[i];
                        String[] pushObjectInfoArr = pushObjectId.split(";");

                        if(pushObjectInfoArr != null && pushObjectInfoArr.length >1){
                            phshObjectPhone = pushObjectInfoArr[0];
                            phshObjectName = pushObjectInfoArr[1];
                            if(StringUtil.isNotEmpty(phshObjectPhone)){

                                String bindingResult ="";
                                //执行绑定操作
                                try{
                                    bindingResult = imUtil.relBind(phshObjectPhone);
                                }catch (Exception ex){
                                    ex.printStackTrace();
                                }

                                log.info("++++++++++绑定结果：{}",bindingResult);
                                if(StringUtil.isNotEmpty(bindingResult)){
                                    JSONObject bindingJson = JSONObject.parseObject(bindingResult);
                                    String status = bindingJson.getString("status");
                                    String msg = bindingJson.getString("msg");

                                    //绑定成功
                                    if(StringUtil.isNotEmpty(bindingResult) && "success".equals(status)){

                                        retJson.put("retCode","0");
                                        retJson.put("retMsg","绑定成功！");
                                        retJson.put("addRetMsg","绑定成功: "+bindingResult);

                                        addImBindInfo(phshObjectPhone,phshObjectName);
                                    }else{
                                        retJson.put("retCode","-1");
                                        retJson.put("retMsg","绑定失败:"+msg);
                                        retJson.put("addRetMsg","绑定失败: "+bindingResult);

                                    }
                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","绑定接口返回空");
                                    retJson.put("addRetMsg","绑定接口返回空");

                                }
                            }
                        }else{
                            phshObjectPhone = "空";
                            phshObjectName = "空";
                        }


                        //保存操作日志
                        ImOperateLog imOperateLog = new ImOperateLog();
                        imOperateLog.setOperateType("bind");

                        imOperateLog.setObjectId(phshObjectPhone);
                        imOperateLog.setObjectName(phshObjectName+"["+phshObjectPhone+"]");
                        imOperateLog.setOperateTime(LocalDateTime.now());

                        String retCode = retJson.getString("retCode");
                        String retMsg = retJson.getString("addRetMsg");
                        if(StringUtil.isNotEmpty(retMsg) && retMsg.length()>1000){
                            retMsg = retMsg.substring(0,1000);
                        }

                        imOperateLog.setIsSuccess(retCode);
                        imOperateLog.setOperateResult(retMsg);

                        //获取当前登录用户信息
                        SysUser currentUser = UserContext.currentUser();
                        String realName = currentUser.getRealName();
                        imOperateLog.setOperatePerson(realName);

                        imOperateLogMapper.insert(imOperateLog);
                    }
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","系统异常，请稍后再试！");
        }
        return retJson;
    }

    /**
     * 获取日报推送对象管理下拉框（选中值）
     * @param manageId
     * @return
     */
    @Override
    public Object getRbPushObjectManageSelectSel(String manageId) {
        List<JSONObject> retList = new ArrayList<>();

        ImHdjkPushManage imRbFilePushManage = imRbFilePushManageMapper.selectById(manageId);
        if(imRbFilePushManage != null){
            String pushObjectId = imRbFilePushManage.getPushObjectId();

            QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("create_time");
            queryWrapper.isNotNull("bind_time");
            List<ImPushObjectManage> list = imPushObjectManageMapper.selectList(queryWrapper);
            if(list != null && list.size() >0){

                String[] pushObjectIdArr = pushObjectId.split(";");

                for(ImPushObjectManage imPushObjectManage : list){
                    String pushObjectType = imPushObjectManage.getPushObjectType();
                    int queryManageId = imPushObjectManage.getManageId();

                /*if(pushObjectType.equals("users")){
                    pushObjectType = "用户";
                }else if(pushObjectType.equals("group")){
                    pushObjectType = "群";
                }*/

                    JSONObject jsonObject = new JSONObject();
                    //jsonObject.put("name","["+pushObjectType+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("name","["+imPushObjectManage.getObjectArea()+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("value",imPushObjectManage.getPushObjectId());
                    jsonObject.put("type",imPushObjectManage.getPushObjectType());
                    jsonObject.put("id",imPushObjectManage.getManageId());

                    if(pushObjectIdArr != null && pushObjectIdArr.length >0){
                        for(String strPushObjectId :pushObjectIdArr){
                            int iPushObjectId = Integer.parseInt(strPushObjectId);
                            if(iPushObjectId == queryManageId){
                                jsonObject.put("selected",true);
                            }
                        }
                    }

                    retList.add(jsonObject);
                }
            }

        }

        return retList;
    }

    /**
     * 获取日报推送领导管理下拉框（选中值）
     * @param manageId
     * @return
     */
    @Override
    public Object getRbPushLeaderManageSelectSel(String manageId) {
        List<JSONObject> retList = new ArrayList<>();

        ImHdjkPushManage imRbFilePushManage = imRbFilePushManageMapper.selectById(manageId);
        if(imRbFilePushManage != null){
            String pushObjectId = imRbFilePushManage.getPushLeaderId() ==null?"":imRbFilePushManage.getPushLeaderId();

            QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("create_time");
            queryWrapper.isNotNull("bind_time");
            List<ImPushObjectManage> list = imPushObjectManageMapper.selectList(queryWrapper);
            if(list != null && list.size() >0){

                String[] pushObjectIdArr = pushObjectId.split(";");

                for(ImPushObjectManage imPushObjectManage : list){
                    String pushObjectType = imPushObjectManage.getPushObjectType();
                    int queryManageId = imPushObjectManage.getManageId();

                /*if(pushObjectType.equals("users")){
                    pushObjectType = "用户";
                }else if(pushObjectType.equals("group")){
                    pushObjectType = "群";
                }*/

                    JSONObject jsonObject = new JSONObject();
                    //jsonObject.put("name","["+pushObjectType+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("name","["+imPushObjectManage.getObjectArea()+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("value",imPushObjectManage.getPushObjectId());
                    jsonObject.put("type",imPushObjectManage.getPushObjectType());
                    jsonObject.put("id",imPushObjectManage.getManageId());

                    if(pushObjectIdArr != null && pushObjectIdArr.length >0){
                        for(String strPushObjectId :pushObjectIdArr){
                            if(strPushObjectId == null || strPushObjectId.trim().isEmpty()) {
                                continue;
                            }

                            int iPushObjectId = Integer.parseInt(strPushObjectId);
                            if(iPushObjectId == queryManageId){
                                jsonObject.put("selected",true);
                            }
                        }
                    }

                    retList.add(jsonObject);
                }
            }

        }

        return retList;
    }

    /**
     * 获取当前登录人在推送对象中的信息
     * @return
     */
    @Override
    public JSONObject getPushObjectByLogin() {
        JSONObject retJsonObect = new JSONObject();
        try {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();
            String userName = currentUser.getUsername() ==null?"" : currentUser.getUsername();
            if(StringUtil.isNotEmpty(userName)){
                QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("push_object_id",userName);

                ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectOne(queryWrapper);
                if(imPushObjectManage != null){
                    retJsonObect.put("retCode","0");
                    retJsonObect.put("pushObjectId",imPushObjectManage.getPushObjectId());
                    retJsonObect.put("pushObjectName",imPushObjectManage.getPushObjectName());
                    retJsonObect.put("pushManageId",imPushObjectManage.getManageId());
                }else{
                    retJsonObect.put("retCode","-1");
                }
            }else{
                retJsonObect.put("retCode","-1");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJsonObect.put("retCode","-1");
        }
        return retJsonObect;
    }

    /**
     * 根据推送对象ID获取推送对象信息
     * @param objectId
     * @return
     */
    @Override
    public ImPushObjectManage getImPushObjectByObjectId(String objectId) {
        QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("push_object_id",objectId);
        queryWrapper.last("limit 1");
        ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectOne(queryWrapper);
        return imPushObjectManage;
    }


    /**
     * 批量导入
     * @return
     */
    @Override
    public ResponseEntity<InputStreamResource> downloadModelFile() {
        String isSuccess = "0";
        String errMsg ="";
        try {

            String filePath = imBatchImportModelPath;

            FileSystemResource file = new FileSystemResource(filePath);

            HttpHeaders headers = new HttpHeaders();
            // 确保不缓存
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            // 处理特殊字符的文件名
            String encodedFileName = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8.toString());
            log.info("进入下载方法... encodedFilename: {}",encodedFileName);
            headers.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            try {
                return ResponseEntity
                        .ok()
                        .headers(headers)
                        .contentLength(file.contentLength())
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .body(new InputStreamResource(file.getInputStream()));
            }catch (Exception ex){
                ex.printStackTrace();
                isSuccess = "-1";
                if(ex.getMessage() != null){
                    errMsg = ex.getMessage().substring(0,1000);
                }
                return null;
            }

        }catch (Exception e){
            e.printStackTrace();
            isSuccess = "-1";
            if(e.getMessage() != null){
                errMsg = e.getMessage().substring(0,1000);
            }
            return null;
        }
    }


    /**
     * 批量导入数据
     * @param file
     * @return
     */
    @Override
    public boolean batchImportData(MultipartFile file) {
        boolean retResult = false;
        try {
            List<ImPushObjectManageImp> dataList = new ArrayList<>();

            // 使用EasyExcel读取
            EasyExcel.read(file.getInputStream(), ImPushObjectManageImp.class,
                    new ReadListener<ImPushObjectManageImp>() {
                        @Override
                        public void invoke(ImPushObjectManageImp data, AnalysisContext context) {
                            // 每读取一行调用一次
                            dataList.add(data);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            // 读取完成
                            log.info("读取完成，共" + dataList.size() + "条数据");
                        }
                    }).sheet().doRead();


            if(dataList != null && dataList.size() >0){
                //对象转换
                List<ImPushObjectManage> imPushObjectManageList = objectConvert(dataList);
                if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                    // 分批处理，避免单次处理数据量过大
                    List<List<ImPushObjectManage>> batches = partitionList(imPushObjectManageList, 1000);
                    for (List<ImPushObjectManage> batch : batches){
                        //批量保存
                        int addCount = imPushObjectManageMapper.batchInsertWithId(batch);

                        retResult = true;
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return retResult;
    }

    /**
     * 对象转换
     * @param imPushObjectManageImpList
     * @return
     */
    private List<ImPushObjectManage> objectConvert(List<ImPushObjectManageImp> imPushObjectManageImpList){
        List<ImPushObjectManage> imPushObjectManageList = new ArrayList<>();

        if(imPushObjectManageImpList != null && imPushObjectManageImpList.size() >0){
            for(ImPushObjectManageImp imPushObjectManageImp:imPushObjectManageImpList){

                String pushObjectType = imPushObjectManageImp.getPushObjectType()==null?"":imPushObjectManageImp.getPushObjectType();
                pushObjectType = pushObjectType.equals("群")?"group":"users";

                ImPushObjectManage imPushObjectManage = new ImPushObjectManage();
                imPushObjectManage.setPushObjectType(pushObjectType);
                imPushObjectManage.setPushObjectId(imPushObjectManageImp.getPushObjectId());
                imPushObjectManage.setPushObjectName(imPushObjectManageImp.getPushObjectName());
                imPushObjectManage.setObjectArea(imPushObjectManageImp.getObjectArea());
                imPushObjectManage.setCreateTime(LocalDateTime.now());

                imPushObjectManageList.add(imPushObjectManage);
            }

            //去掉重复的XHxBpId
            imPushObjectManageList = imPushObjectManageList.stream()
                    .filter(distinctByKey(ImPushObjectManage::getPushObjectId))
                    .collect(Collectors.toList());
        }
        return  imPushObjectManageList;
    }

    // 通用的去重方法
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    /**
     * 将列表分割成多个批次
     */
    private <T> List<List<T>> partitionList(List<T> originalList, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();

        for (int i = 0; i < originalList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, originalList.size());
            partitions.add(new ArrayList<>(originalList.subList(i, end)));
        }

        return partitions;
    }

    /**
     * Describe: 获取组织维度树
     */
    @Override
    public List<WgDtreeData> zzWdtree(){
        List<WgDtreeData> retDtreeList = new ArrayList<>();

        try {
            //拼接全疆数据
            WgDtreeData qJWgDtreeData = new WgDtreeData();
            qJWgDtreeData.setTreeId("1");
            qJWgDtreeData.setTreeName("全疆");
            qJWgDtreeData.setParentId("0");
            qJWgDtreeData.setDataLevel("全疆");

            JSONObject qjBasicDataJson = new JSONObject();
            qjBasicDataJson.put("dataLevel","全疆");
            qJWgDtreeData.setBasicData(qjBasicDataJson);


            //组装全疆下的16个地州组织数据
            List<VWgzsUserLevelD> bdwList = wgPptDorisConnService.getWgZzLevelDListById("0","本地网");
            if(bdwList != null && bdwList.size() >0){
                JSONObject bdwBasicDataJson = new JSONObject();
                bdwBasicDataJson.put("dataLevel","本地网");
                for(VWgzsUserLevelD bdwWgzsUserLevelD : bdwList){
                    WgDtreeData bdwWgDtreeData = new WgDtreeData();
                    bdwWgDtreeData.setTreeId(bdwWgzsUserLevelD.getHxLatnId());
                    bdwWgDtreeData.setTreeName(bdwWgzsUserLevelD.getHxLatnName());
                    bdwWgDtreeData.setParentId("1");
                    bdwWgDtreeData.setDataLevel("本地网");
                    bdwWgDtreeData.setBasicData(bdwBasicDataJson);
                    retDtreeList.add(bdwWgDtreeData);

                    //根据本地网ID获取县分组织数据
                    List<VWgzsUserLevelD> xfZzList = wgPptDorisConnService.getWgZzLevelDListById(bdwWgzsUserLevelD.getHxLatnId(),"本地网");
                    if(xfZzList != null && xfZzList.size() >0){
                        JSONObject xfBasicDataJson = new JSONObject();
                        xfBasicDataJson.put("dataLevel","县分");
                        for(VWgzsUserLevelD xfWgzsUserLevelD : xfZzList){
                            WgDtreeData xfWgDtreeData = new WgDtreeData();
                            xfWgDtreeData.setTreeId(xfWgzsUserLevelD.getHxAreaId());
                            xfWgDtreeData.setTreeName(xfWgzsUserLevelD.getHxAreaName());
                            xfWgDtreeData.setParentId(xfWgzsUserLevelD.getHxLatnId());
                            xfWgDtreeData.setDataLevel("县分");
                            xfWgDtreeData.setBasicData(xfBasicDataJson);
                            retDtreeList.add(xfWgDtreeData);

                            //根据县分ID获取支局组织数据
                            List<VWgzsUserLevelD> zjZzList = wgPptDorisConnService.getWgZzLevelDListById(xfWgzsUserLevelD.getHxAreaId(),"县分");
                            if(zjZzList != null && zjZzList.size() >0){
                                JSONObject zjBasicDataJson = new JSONObject();
                                zjBasicDataJson.put("dataLevel","支局");
                                for(VWgzsUserLevelD zjZzVWgzsUserLevelD : zjZzList){
                                    WgDtreeData zjWgDtreeData = new WgDtreeData();
                                    zjWgDtreeData.setTreeId(zjZzVWgzsUserLevelD.getHxRegionId());
                                    zjWgDtreeData.setTreeName(zjZzVWgzsUserLevelD.getHxRegionName());
                                    zjWgDtreeData.setParentId(zjZzVWgzsUserLevelD.getHxAreaId());
                                    zjWgDtreeData.setDataLevel("支局");
                                    zjWgDtreeData.setBasicData(zjBasicDataJson);
                                    retDtreeList.add(zjWgDtreeData);

                                    //根据支局ID获取网格组织数据
                                    List<VWgzsUserLevelD> wgZzList = wgPptDorisConnService.getWgZzLevelDListById(zjZzVWgzsUserLevelD.getHxRegionId(),"支局");
                                    if(wgZzList != null && wgZzList.size() >0){
                                        JSONObject wgBasicDataJson = new JSONObject();
                                        wgBasicDataJson.put("dataLevel","网格");
                                        for(VWgzsUserLevelD wgZzVWgzsUserLevelD : wgZzList){
                                            WgDtreeData wgWgDtreeData = new WgDtreeData();
                                            wgWgDtreeData.setTreeId(wgZzVWgzsUserLevelD.getXHx5BpId());
                                            wgWgDtreeData.setTreeName(wgZzVWgzsUserLevelD.getXHx5BpName());
                                            wgWgDtreeData.setParentId(wgZzVWgzsUserLevelD.getHxRegionId());
                                            wgWgDtreeData.setDataLevel("网格");
                                            wgWgDtreeData.setBasicData(wgBasicDataJson);
                                            retDtreeList.add(wgWgDtreeData);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            retDtreeList.add(qJWgDtreeData);

        }catch (Exception e){
            e.printStackTrace();
        }

        return retDtreeList;
    }


    /**
     * Describe: 获取标签分组树
     */
    @Override
    public List<WgDtreeData> bqfzTree(){
        List<WgDtreeData> retDtreeList = new ArrayList<>();

        try {
            //拼接全疆数据
            WgDtreeData qJWgDtreeData = new WgDtreeData();
            qJWgDtreeData.setTreeId("1");
            qJWgDtreeData.setTreeName("全疆");
            qJWgDtreeData.setParentId("0");
            qJWgDtreeData.setDataLevel("全疆");

            JSONObject qjBasicDataJson = new JSONObject();
            qjBasicDataJson.put("dataLevel","全疆");
            qJWgDtreeData.setBasicData(qjBasicDataJson);
            retDtreeList.add(qJWgDtreeData);

            List<ImPushOrg> bdwImPushOrgList = getInfoByParentId("1");
            if(bdwImPushOrgList != null && bdwImPushOrgList.size() >0){
                //组织树子节点数据信息
                retDtreeList = creatTreeNodeData(bdwImPushOrgList,retDtreeList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return retDtreeList;
    }


    /**
     * 根据父节点ID获取子节点信息
     * @param parentId
     * @return
     */
    private List<ImPushOrg> getInfoByParentId(String parentId){
        List<ImPushOrg> retList = new ArrayList<>();
        if(StringUtil.isNotEmpty(parentId)){
            QueryWrapper<ImPushOrg> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_org_id",parentId);
            queryWrapper.orderByAsc("sort_id");
            retList = imPushOrgMapper.selectList(queryWrapper);
        }
        return retList;
    }

    /**
     * 组织树子节点数据信息
     * @param imPushOrgList
     * @param retDtreeList
     * @return
     */
    public List<WgDtreeData>  creatTreeNodeData(List<ImPushOrg> imPushOrgList,List<WgDtreeData> retDtreeList){
        if(imPushOrgList != null && imPushOrgList.size() >0){
            JSONObject basicDataJson = new JSONObject();
            for(ImPushOrg imPushOrg : imPushOrgList){
                WgDtreeData zjWgDtreeData = new WgDtreeData();
                zjWgDtreeData.setTreeId(imPushOrg.getOrgId());
                zjWgDtreeData.setTreeName(imPushOrg.getOrgName());
                zjWgDtreeData.setParentId(imPushOrg.getParentOrgId());
                zjWgDtreeData.setDataLevel(imPushOrg.getLevel());
                basicDataJson.put("dataLevel",imPushOrg.getLevel());
                basicDataJson.put("dataType","orga");
                basicDataJson.put("classType","bqwd");
                zjWgDtreeData.setBasicData(basicDataJson);
                retDtreeList.add(zjWgDtreeData);

                //根据组织ID获取子节点数据
                QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("object_area_id",imPushOrg.getOrgId());
                queryWrapper.eq("class_type","bqwd");
                List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectList(queryWrapper);
                if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                    for(ImPushObjectManage imPushObjectManage : imPushObjectManageList){
                        JSONObject userBasicDataJson = new JSONObject();
                        WgDtreeData userWgDtreeData = new WgDtreeData();
                        userWgDtreeData.setTreeId(imPushObjectManage.getPushObjectId());
                        userWgDtreeData.setTreeName(imPushObjectManage.getPushObjectName());
                        userWgDtreeData.setParentId(imPushOrg.getOrgId());
                        userWgDtreeData.setDataLevel(imPushOrg.getLevel());

                        userBasicDataJson.put("dataLevel",imPushOrg.getLevel());
                        userBasicDataJson.put("dataType","user");
                        userBasicDataJson.put("classType","bqwd");
                        userBasicDataJson.put("manageId",imPushObjectManage.getManageId());

                        userWgDtreeData.setBasicData(userBasicDataJson);
                        retDtreeList.add(userWgDtreeData);
                    }
                }

                List<ImPushOrg> subImPushOrgList = getInfoByParentId(imPushOrg.getOrgId());
                if(subImPushOrgList != null && subImPushOrgList.size() >0){
                    retDtreeList = creatTreeNodeData(subImPushOrgList,retDtreeList);
                }
            }
        }
        return  retDtreeList;
    }

    /**
     * 组织树子节点数据信息
     * @param imPushOrgList
     * @param retDtreeList
     * @return
     */
    public List<WgDtreeData>  creatTreeNodeDataSel(List<ImPushOrg> imPushOrgList,List<WgDtreeData> retDtreeList,String pushObjectId){
        if(imPushOrgList != null && imPushOrgList.size() >0){
            JSONObject basicDataJson = new JSONObject();
            for(ImPushOrg imPushOrg : imPushOrgList){
                WgDtreeData zjWgDtreeData = new WgDtreeData();
                zjWgDtreeData.setTreeId(imPushOrg.getOrgId());
                zjWgDtreeData.setTreeName(imPushOrg.getOrgName());
                zjWgDtreeData.setParentId(imPushOrg.getParentOrgId());
                zjWgDtreeData.setDataLevel(imPushOrg.getLevel());
                basicDataJson.put("dataLevel",imPushOrg.getLevel());
                basicDataJson.put("dataType","orga");
                basicDataJson.put("classType","bqwd");
                zjWgDtreeData.setBasicData(basicDataJson);
                retDtreeList.add(zjWgDtreeData);

                //根据组织ID获取子节点数据
                QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("object_area_id",imPushOrg.getOrgId());
                queryWrapper.eq("class_type","bqwd");
                List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectList(queryWrapper);
                if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                    for(ImPushObjectManage imPushObjectManage : imPushObjectManageList){
                        JSONObject userBasicDataJson = new JSONObject();
                        WgDtreeData userWgDtreeData = new WgDtreeData();
                        userWgDtreeData.setTreeId(imPushObjectManage.getPushObjectId());
                        userWgDtreeData.setTreeName(imPushObjectManage.getPushObjectName());
                        userWgDtreeData.setParentId(imPushOrg.getOrgId());
                        userWgDtreeData.setDataLevel(imPushOrg.getLevel());

                        if(StringUtil.isNotEmpty(pushObjectId) && pushObjectId.equals(imPushObjectManage.getPushObjectId())){
                            userWgDtreeData.setCheckArr("1");
                        }

                        userBasicDataJson.put("dataLevel",imPushOrg.getLevel());
                        userBasicDataJson.put("dataType","user");
                        userBasicDataJson.put("classType","bqwd");
                        userBasicDataJson.put("manageId",imPushObjectManage.getManageId());

                        userWgDtreeData.setBasicData(userBasicDataJson);
                        retDtreeList.add(userWgDtreeData);
                    }
                }

                List<ImPushOrg> subImPushOrgList = getInfoByParentId(imPushOrg.getOrgId());
                if(subImPushOrgList != null && subImPushOrgList.size() >0){
                    retDtreeList = creatTreeNodeData(subImPushOrgList,retDtreeList);
                }
            }
        }
        return  retDtreeList;
    }


    /**
     * Describe: 保存新增标签分组树节点
     */
    @Override
    public JSONObject saveAddBqfzTree(ImPushOrg imPushOrg){
        JSONObject retObject = new JSONObject();
        try {

            if(imPushOrg != null){
                imPushOrg.setCreateTime(LocalDateTime.now());
                //imPushOrg.setOrgId(IdUtil.simpleUUID());
                int addResult = imPushOrgMapper.insert(imPushOrg);
                if(addResult >0){
                    retObject.put("retCode","0");
                    retObject.put("retMsg","保存成功！");
                }else{
                    retObject.put("retCode","-1");
                    retObject.put("retMsg","保存失败！");
                }
            }else{
                retObject.put("retCode","-1");
                retObject.put("retMsg","保存失败！");
            }
        }catch (Exception e){
            e.printStackTrace();
            retObject.put("retCode",500);
            retObject.put("retMsg","保存失败,系统异常！");
        }
        return retObject;
    }

    /**
     * 标签分组树节点删除
     */
    @Override
    public JSONObject bqfzTreeDel(String orgId){
        JSONObject retJson = new JSONObject();
        try {
            QueryWrapper<ImPushOrg> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_org_id",orgId);
            List<ImPushOrg> imPushOrgList = imPushOrgMapper.selectList(queryWrapper);
            if(imPushOrgList != null && imPushOrgList.size() >0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","选择的节点下有子节点，请先删除子节点");
            }else{
                int delResult = imPushOrgMapper.deleteById(orgId);
                if(delResult >0){
                    retJson.put("retCode","0");
                    retJson.put("retMsg","删除成功");
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","删除失败");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败,系统异常");
        }
        return retJson;
    }

    /**
     * 根据ID查询标签分组节点信息
     * @param orgId
     * @return
     */
    @Override
    public ImPushOrg getBqfzTreeNodeById(String orgId){
        return imPushOrgMapper.selectById(orgId);
    }

    /**
     * Describe: 保存修改标签分组树节点
     */
    @Override
    public JSONObject saveEditBqfzTree(ImPushOrg imPushOrg){
        JSONObject retObject = new JSONObject();
        try {

            if(imPushOrg != null){
                imPushOrg.setCreateTime(LocalDateTime.now());
                int addResult = imPushOrgMapper.updateById(imPushOrg);
                if(addResult >0){

                    //更新对应组织人员信息
                    imPushObjectManageMapper.updateAreaNameById(imPushOrg.getOrgId(),imPushOrg.getOrgName());

                    retObject.put("retCode","0");
                    retObject.put("retMsg","保存成功！");
                }else{
                    retObject.put("retCode","-1");
                    retObject.put("retMsg","保存失败！");
                }
            }else{
                retObject.put("retCode","-1");
                retObject.put("retMsg","保存失败！");
            }
        }catch (Exception e){
            e.printStackTrace();
            retObject.put("retCode",500);
            retObject.put("retMsg","保存失败,系统异常！");
        }
        return retObject;
    }

    /**
     * 新增IM推送对象绑定信息
     */
    private void addImBindInfo(String bindId,String bindName){
        try {
            ImBindInfo imBindInfo = imBindInfoMapper.selectById(bindId);
            if(imBindInfo == null){
                imBindInfo = new ImBindInfo();
                imBindInfo.setBindId(bindId);
                imBindInfo.setBindName(bindName);
                imBindInfo.setBindTime(LocalDateTime.now());
                imBindInfoMapper.insert(imBindInfo);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Describe: 获取组织维度树人员信息
     */
    @Override
    public List<WgDtreeData> zzWdtreePerson(){
        List<WgDtreeData> retDtreeList = new ArrayList<>();

        try {
            //拼接全疆数据
            WgDtreeData qJWgDtreeData = new WgDtreeData();
            qJWgDtreeData.setTreeId("1");
            qJWgDtreeData.setTreeName("全疆");
            qJWgDtreeData.setParentId("0");
            qJWgDtreeData.setDataLevel("全疆");

            JSONObject qjBasicDataJson = new JSONObject();
            qjBasicDataJson.put("dataLevel","全疆");
            qjBasicDataJson.put("dataType","orga");
            qjBasicDataJson.put("classType","zzwd");
            qJWgDtreeData.setBasicData(qjBasicDataJson);

            //获取全疆下的人员信息
            List<VWgzsUserLevelD> qjUserWgzsUserLevelDList = wgPptDorisConnService.getWgUserLevelDListById("","全疆");
            if(qjUserWgzsUserLevelDList != null && qjUserWgzsUserLevelDList.size() >0){
                for(VWgzsUserLevelD qjUserVWgzsUserLevelD : qjUserWgzsUserLevelDList){
                    WgDtreeData qjUserWgDtreeData = new WgDtreeData();
                    qjUserWgDtreeData.setTreeId(qjUserVWgzsUserLevelD.getTelephone());
                    qjUserWgDtreeData.setTreeName(qjUserVWgzsUserLevelD.getUserName()+"["+qjUserVWgzsUserLevelD.getTelephone()+"]");
                    qjUserWgDtreeData.setParentId("1");
                    qjUserWgDtreeData.setDataLevel("全疆");

                    JSONObject qjUserBasicDataJson = new JSONObject();
                    qjUserBasicDataJson.put("dataLevel","全疆");
                    qjUserBasicDataJson.put("dataType","user");
                    qjUserBasicDataJson.put("classType","zzwd");
                    qjUserWgDtreeData.setBasicData(qjUserBasicDataJson);

                    retDtreeList.add(qjUserWgDtreeData);
                }
            }

            //组装全疆下的16个地州组织数据
            List<VWgzsUserLevelD> bdwList = wgPptDorisConnService.getWgZzLevelDListById("0","本地网");
            if(bdwList != null && bdwList.size() >0){
                JSONObject bdwBasicDataJson = new JSONObject();
                bdwBasicDataJson.put("dataLevel","本地网");
                for(VWgzsUserLevelD bdwWgzsUserLevelD : bdwList){
                    WgDtreeData bdwWgDtreeData = new WgDtreeData();
                    bdwWgDtreeData.setTreeId(bdwWgzsUserLevelD.getHxLatnId());
                    bdwWgDtreeData.setTreeName(bdwWgzsUserLevelD.getHxLatnName());
                    bdwWgDtreeData.setParentId("1");
                    bdwWgDtreeData.setDataLevel("本地网");
                    bdwWgDtreeData.setBasicData(bdwBasicDataJson);
                    retDtreeList.add(bdwWgDtreeData);

                    //获取16个本地网的人员信息
                    List<VWgzsUserLevelD> bdwUserWgzsUserLevelDList = wgPptDorisConnService.getWgUserLevelDListById(bdwWgzsUserLevelD.getHxLatnId(),"本地网");
                    if(bdwUserWgzsUserLevelDList != null && bdwUserWgzsUserLevelDList.size() >0){
                        for(VWgzsUserLevelD bdwUserVWgzsUserLevelD : bdwUserWgzsUserLevelDList){
                            WgDtreeData bdwUserWgDtreeData = new WgDtreeData();
                            bdwUserWgDtreeData.setTreeId(bdwUserVWgzsUserLevelD.getTelephone());
                            bdwUserWgDtreeData.setTreeName(bdwUserVWgzsUserLevelD.getUserName()+"["+bdwUserVWgzsUserLevelD.getTelephone()+"]");
                            bdwUserWgDtreeData.setParentId(bdwWgzsUserLevelD.getHxLatnId());
                            bdwUserWgDtreeData.setDataLevel("本地网");

                            JSONObject bdwUserBasicDataJson = new JSONObject();
                            bdwUserBasicDataJson.put("dataLevel","本地网");
                            bdwUserBasicDataJson.put("dataType","user");
                            bdwUserBasicDataJson.put("classType","zzwd");
                            bdwUserWgDtreeData.setBasicData(bdwUserBasicDataJson);

                            retDtreeList.add(bdwUserWgDtreeData);
                        }
                    }


                    //根据本地网ID获取县分组织数据
                    List<VWgzsUserLevelD> xfZzList = wgPptDorisConnService.getWgZzLevelDListById(bdwWgzsUserLevelD.getHxLatnId(),"本地网");
                    if(xfZzList != null && xfZzList.size() >0){
                        JSONObject xfBasicDataJson = new JSONObject();
                        xfBasicDataJson.put("dataLevel","县分");
                        for(VWgzsUserLevelD xfWgzsUserLevelD : xfZzList){
                            WgDtreeData xfWgDtreeData = new WgDtreeData();
                            xfWgDtreeData.setTreeId(xfWgzsUserLevelD.getHxAreaId());
                            xfWgDtreeData.setTreeName(xfWgzsUserLevelD.getHxAreaName());
                            xfWgDtreeData.setParentId(xfWgzsUserLevelD.getHxLatnId());
                            xfWgDtreeData.setDataLevel("县分");
                            xfWgDtreeData.setBasicData(xfBasicDataJson);
                            retDtreeList.add(xfWgDtreeData);

                            //获取县分下的的人员信息
                            List<VWgzsUserLevelD> xfUserWgzsUserLevelDList = wgPptDorisConnService.getWgUserLevelDListById(xfWgzsUserLevelD.getHxAreaId(),"县分");
                            if(xfUserWgzsUserLevelDList != null && xfUserWgzsUserLevelDList.size() >0){
                                for(VWgzsUserLevelD xfUserVWgzsUserLevelD : xfUserWgzsUserLevelDList){
                                    WgDtreeData xfUserWgDtreeData = new WgDtreeData();
                                    xfUserWgDtreeData.setTreeId(xfUserVWgzsUserLevelD.getTelephone());
                                    xfUserWgDtreeData.setTreeName(xfUserVWgzsUserLevelD.getUserName()+"["+xfUserVWgzsUserLevelD.getTelephone()+"]");
                                    xfUserWgDtreeData.setParentId(xfUserVWgzsUserLevelD.getHxAreaId());
                                    xfUserWgDtreeData.setDataLevel("县分");

                                    JSONObject xfUserBasicDataJson = new JSONObject();
                                    xfUserBasicDataJson.put("dataLevel","县分");
                                    xfUserBasicDataJson.put("dataType","user");
                                    xfUserBasicDataJson.put("classType","zzwd");
                                    xfUserWgDtreeData.setBasicData(xfUserBasicDataJson);

                                    retDtreeList.add(xfUserWgDtreeData);
                                }
                            }


                            //根据县分ID获取支局组织数据
                            List<VWgzsUserLevelD> zjZzList = wgPptDorisConnService.getWgZzLevelDListById(xfWgzsUserLevelD.getHxAreaId(),"县分");
                            if(zjZzList != null && zjZzList.size() >0){
                                JSONObject zjBasicDataJson = new JSONObject();
                                zjBasicDataJson.put("dataLevel","支局");
                                for(VWgzsUserLevelD zjZzVWgzsUserLevelD : zjZzList){
                                    WgDtreeData zjWgDtreeData = new WgDtreeData();
                                    zjWgDtreeData.setTreeId(zjZzVWgzsUserLevelD.getHxRegionId());
                                    zjWgDtreeData.setTreeName(zjZzVWgzsUserLevelD.getHxRegionName());
                                    zjWgDtreeData.setParentId(zjZzVWgzsUserLevelD.getHxAreaId());
                                    zjWgDtreeData.setDataLevel("支局");
                                    zjWgDtreeData.setBasicData(zjBasicDataJson);
                                    retDtreeList.add(zjWgDtreeData);

                                    //获取支局下的的人员信息
                                    List<VWgzsUserLevelD> zjUserWgzsUserLevelDList = wgPptDorisConnService.getWgUserLevelDListById(zjZzVWgzsUserLevelD.getHxRegionId(),"支局");
                                    if(zjUserWgzsUserLevelDList != null && zjUserWgzsUserLevelDList.size() >0){
                                        for(VWgzsUserLevelD zjUserVWgzsUserLevelD : zjUserWgzsUserLevelDList){
                                            WgDtreeData zjUserWgDtreeData = new WgDtreeData();
                                            zjUserWgDtreeData.setTreeId(zjUserVWgzsUserLevelD.getTelephone());
                                            zjUserWgDtreeData.setTreeName(zjUserVWgzsUserLevelD.getUserName()+"["+zjUserVWgzsUserLevelD.getTelephone()+"]");
                                            zjUserWgDtreeData.setParentId(zjUserVWgzsUserLevelD.getHxRegionId());
                                            zjUserWgDtreeData.setDataLevel("支局");

                                            JSONObject zjUserBasicDataJson = new JSONObject();
                                            zjUserBasicDataJson.put("dataLevel","支局");
                                            zjUserBasicDataJson.put("dataType","user");
                                            zjUserBasicDataJson.put("classType","zzwd");
                                            zjUserWgDtreeData.setBasicData(zjUserBasicDataJson);

                                            retDtreeList.add(zjUserWgDtreeData);
                                        }
                                    }

                                    //根据支局ID获取网格组织数据
                                    List<VWgzsUserLevelD> wgZzList = wgPptDorisConnService.getWgZzLevelDListById(zjZzVWgzsUserLevelD.getHxRegionId(),"支局");
                                    if(wgZzList != null && wgZzList.size() >0){
                                        JSONObject wgBasicDataJson = new JSONObject();
                                        wgBasicDataJson.put("dataLevel","网格");
                                        for(VWgzsUserLevelD wgZzVWgzsUserLevelD : wgZzList){
                                            WgDtreeData wgWgDtreeData = new WgDtreeData();
                                            wgWgDtreeData.setTreeId(wgZzVWgzsUserLevelD.getXHx5BpId());
                                            wgWgDtreeData.setTreeName(wgZzVWgzsUserLevelD.getXHx5BpName());
                                            wgWgDtreeData.setParentId(wgZzVWgzsUserLevelD.getHxRegionId());
                                            wgWgDtreeData.setDataLevel("网格");
                                            wgWgDtreeData.setBasicData(wgBasicDataJson);
                                            retDtreeList.add(wgWgDtreeData);

                                            //获取风格下的的人员信息
                                            List<VWgzsUserLevelD> wgUserWgzsUserLevelDList = wgPptDorisConnService.getWgUserLevelDListById(wgZzVWgzsUserLevelD.getXHx5BpId(),"网格");
                                            if(wgUserWgzsUserLevelDList != null && wgUserWgzsUserLevelDList.size() >0){
                                                for(VWgzsUserLevelD wgUserVWgzsUserLevelD : wgUserWgzsUserLevelDList){
                                                    WgDtreeData wgUserWgDtreeData = new WgDtreeData();
                                                    wgUserWgDtreeData.setTreeId(wgUserVWgzsUserLevelD.getTelephone());
                                                    wgUserWgDtreeData.setTreeName(wgUserVWgzsUserLevelD.getUserName()+"["+wgUserVWgzsUserLevelD.getTelephone()+"]");
                                                    wgUserWgDtreeData.setParentId(wgUserVWgzsUserLevelD.getXHx5BpId());
                                                    wgUserWgDtreeData.setDataLevel("网格");

                                                    JSONObject wgUserBasicDataJson = new JSONObject();
                                                    wgUserBasicDataJson.put("dataLevel","网格");
                                                    wgUserBasicDataJson.put("dataType","user");
                                                    wgUserBasicDataJson.put("classType","zzwd");
                                                    wgUserWgDtreeData.setBasicData(wgUserBasicDataJson);
                                                    retDtreeList.add(wgUserWgDtreeData);
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            retDtreeList.add(qJWgDtreeData);

        }catch (Exception e){
            e.printStackTrace();
        }

        return retDtreeList;
    }


    /**
     * Describe: 获取标签分组树节点人员信息
     */
    @Override
    public List<WgDtreeData> bqfzTreePerson(){
        List<WgDtreeData> retDtreeList = new ArrayList<>();

        try {
            //拼接全疆数据
            WgDtreeData qJWgDtreeData = new WgDtreeData();
            qJWgDtreeData.setTreeId("1");
            qJWgDtreeData.setTreeName("全疆");
            qJWgDtreeData.setParentId("0");
            qJWgDtreeData.setDataLevel("全疆");

            JSONObject qjBasicDataJson = new JSONObject();
            qjBasicDataJson.put("dataLevel","全疆");
            qjBasicDataJson.put("dataType","orga");
            qjBasicDataJson.put("classType","bqwd");

            qJWgDtreeData.setBasicData(qjBasicDataJson);
            retDtreeList.add(qJWgDtreeData);

            List<ImPushOrg> bdwImPushOrgList = getInfoByParentId("1");
            if(bdwImPushOrgList != null && bdwImPushOrgList.size() >0){
                //组织树子节点数据信息
                retDtreeList = creatTreeNodeData(bdwImPushOrgList,retDtreeList);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return retDtreeList;
    }

    /**
     * Describe: 获取标签分组树节点人员信息
     */
    @Override
    public List<WgDtreeData> bqfzTreePersonSel(String pushObjectId){
        List<WgDtreeData> retDtreeList = new ArrayList<>();

        try {
            //拼接全疆数据
            WgDtreeData qJWgDtreeData = new WgDtreeData();
            qJWgDtreeData.setTreeId("1");
            qJWgDtreeData.setTreeName("全疆");
            qJWgDtreeData.setParentId("0");
            qJWgDtreeData.setDataLevel("全疆");

            JSONObject qjBasicDataJson = new JSONObject();
            qjBasicDataJson.put("dataLevel","全疆");
            qjBasicDataJson.put("dataType","orga");
            qjBasicDataJson.put("classType","bqwd");

            qJWgDtreeData.setBasicData(qjBasicDataJson);

            retDtreeList.add(qJWgDtreeData);

            List<ImPushOrg> bdwImPushOrgList = getInfoByParentId("1");
            if(bdwImPushOrgList != null && bdwImPushOrgList.size() >0){
                //组织树子节点数据信息
                retDtreeList = creatTreeNodeDataSel(bdwImPushOrgList,retDtreeList,pushObjectId);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return retDtreeList;
    }

    /**
     * Describe: 绑定所有组织维度下的人员
     */
    @Override
    public JSONObject zzwdBindingAll(){
        JSONObject retJson = new JSONObject();
        try{
            int index =0;
            List<VWgzsUserLevelD> vWgzsUserLevelDList = wgPptDorisConnService.getWgUserLevelDList();
            if(vWgzsUserLevelDList != null && vWgzsUserLevelDList.size() >0){
                for(VWgzsUserLevelD vWgzsUserLevelD : vWgzsUserLevelDList){
                    index++;
                    String telephone = vWgzsUserLevelD.getTelephone();
                    String name = vWgzsUserLevelD.getUserName();
                    if(StringUtil.isNotEmpty(telephone)){
                        ImBindInfo imBindInfo = imBindInfoMapper.selectById(telephone);
                        if(imBindInfo == null){
                            String bindingResult = "";
                            //执行绑定操作
                            try {
                                bindingResult = imUtil.relBind(telephone);
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                            log.info("++++++++++绑定结果：{}",bindingResult);
                            if(StringUtil.isNotEmpty(bindingResult)){
                                JSONObject bindingJson = JSONObject.parseObject(bindingResult);
                                String status = bindingJson.getString("status");
                                String msg = bindingJson.getString("msg");
                                //绑定成功
                                if(StringUtil.isNotEmpty(bindingResult) && "success".equals(status)){

                                    //新增绑定信息
                                    ImBindInfo addImBindInfo = new ImBindInfo();
                                    addImBindInfo.setBindId(telephone);
                                    addImBindInfo.setBindName(name);

                                    imBindInfoMapper.insert(addImBindInfo);

                                    retJson.put("retCode","0");
                                    retJson.put("retMsg","绑定成功！");
                                    retJson.put("addRetMsg","绑定成功: "+bindingResult);
                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg", "绑定失败:"+msg);
                                    retJson.put("addRetMsg","绑定失败: "+bindingResult);
                                }
                            }else{
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","IM绑定接口返回空");
                                retJson.put("addRetMsg","绑定接口返回空");
                            }
                        }
                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","绑定账号为空");
                    }

                    try {
                        if(index%5 ==0){
                            Thread.sleep(3000);
                        }
                    }catch (Exception ex2){
                        ex2.printStackTrace();
                    }
                }
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","未查询到相关信息");
                retJson.put("addRetMsg","未查询到相关信息");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","绑定失败，系统异常！");
        }
        return retJson;
    }


    /**
     * 标签分组一键绑定
     * @return
     */
    @Override
    public JSONObject bqfzYjbd() {
        JSONObject retObject = new JSONObject();
        try {
            QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("bind_state","0");
            List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectList(queryWrapper);
            if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                int index =0;
                for(ImPushObjectManage imPushObjectManage : imPushObjectManageList){
                    index++;
                    String pushObjectId = imPushObjectManage.getPushObjectId();
                    int iManageId = imPushObjectManage.getManageId();
                    if(StringUtil.isNotEmpty(pushObjectId)){

                        String bindingResult ="";
                        //执行绑定操作
                        try{
                            bindingResult = imUtil.relBind(pushObjectId);
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                        log.info("++++++++++绑定结果：{}",bindingResult);
                        if(StringUtil.isNotEmpty(bindingResult)){
                            JSONObject bindingJson = JSONObject.parseObject(bindingResult);
                            String status = bindingJson.getString("status");
                            String msg = bindingJson.getString("msg");

                            //绑定成功
                            if(StringUtil.isNotEmpty(bindingResult) && "success".equals(status)){

                                imPushObjectManage.setBindTime(LocalDateTime.now());
                                imPushObjectManage.setBindState("1");
                                imPushObjectManageMapper.updateById(imPushObjectManage);

                                retObject.put("retCode","0");
                                retObject.put("retMsg","绑定成功！");
                                retObject.put("addRetMsg","绑定成功: "+bindingResult);
                            }else{
                                retObject.put("retCode","-1");
                                retObject.put("retMsg","绑定失败:"+msg);
                                retObject.put("addRetMsg","绑定失败: "+bindingResult);

                            }
                        }else{
                            retObject.put("retCode","-1");
                            retObject.put("retMsg","绑定接口返回空");
                            retObject.put("addRetMsg","绑定接口返回空");

                        }

                        //保存操作日志
                        if(imPushObjectManage != null){
                            ImOperateLog imOperateLog = new ImOperateLog();
                            imOperateLog.setOperateType("bind");

                            imOperateLog.setObjectId(imPushObjectManage.getPushObjectId());
                            imOperateLog.setObjectName(imPushObjectManage.getPushObjectName());
                            imOperateLog.setOperateTime(LocalDateTime.now());
                            imOperateLog.setManageId(iManageId);

                            String retCode = retObject.getString("retCode");
                            String retMsg = retObject.getString("addRetMsg");
                            if(StringUtil.isNotEmpty(retMsg) && retMsg.length()>1000){
                                retMsg = retMsg.substring(0,1000);
                            }

                            imOperateLog.setIsSuccess(retCode);
                            imOperateLog.setOperateResult(retMsg);

                            //获取当前登录用户信息
                            SysUser currentUser = UserContext.currentUser();
                            String realName = currentUser.getRealName();
                            imOperateLog.setOperatePerson(realName);

                            imOperateLogMapper.insert(imOperateLog);
                        }

                    }

                    if(index %5 ==0){
                        try {
                            Thread.sleep(3000);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            retObject.put("retCode","-1");
            retObject.put("retMSg","一键绑定失败："+e.getMessage());
        }
        return retObject;
    }


}
