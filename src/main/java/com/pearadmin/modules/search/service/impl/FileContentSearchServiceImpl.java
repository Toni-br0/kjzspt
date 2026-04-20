/*
package com.pearadmin.modules.search.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.search.domain.*;
import com.pearadmin.modules.search.mapper.SearchFileClassifyMapper;
import com.pearadmin.modules.search.mapper.SearchFileManageMapper;
import com.pearadmin.modules.search.service.IFileContentSearchService;
import com.pearadmin.modules.search.util.FileToPdfUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

*/
/**
 * 创建日期：2024-12-10
 **//*


@Slf4j
@Service
public class FileContentSearchServiceImpl implements IFileContentSearchService {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Value("${search-upload-path}")
    private String searchUploadPath;

    @Value("${search-file-topdf-path}")
    private String searchFileTopdfPath;

    @Resource
    private SearchFileClassifyMapper searchFileClassifyMapper;

    @Resource
    private SearchFileManageMapper searchFileManageMapper;

    @Resource
    private Environment env;

*/
/**
     * 文件内容搜索
     * @param dto
     * @return*//*



    @Override
    public JSONObject searchPage(FileDTO dto) {
        if(dto.getPageNo() ==null){
            dto.setPageNo(1);
        }
        if(dto.getPageSize() ==null){
            dto.setPageNo(100);
        }

        JSONObject retJson = new JSONObject();
        retJson.put("retCode", "200");
        retJson.put("retMsg", "成功");


        StringBuffer strFileInfo = new StringBuffer();

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //queryBuilder.withQuery(QueryBuilders.multiMatchQuery(dto.getKeyword(), "fileName", "fileContent"));
        //queryBuilder.withQuery(QueryBuilders.matchQuery("fileContent", dto.getKeyword()).fuzziness(Fuzziness.AUTO));
        //queryBuilder.withQuery(QueryBuilders.fuzzyQuery("fileCategory", dto.getKeyword()).fuzziness(Fuzziness.AUTO));
        //queryBuilder.withQuery(QueryBuilders.termQuery("fileContent", dto.getKeyword()));
        //queryBuilder.withQuery(QueryBuilders.matchPhraseQuery("fileContent", dto.getKeyword()));

        // 创建一个MatchPhraseQueryBuilder来查询第一个字段fieldName1
        MatchPhraseQueryBuilder firstFieldQuery = QueryBuilders.matchPhraseQuery("fileName", dto.getFileContent());

        // 创建另一个MatchPhraseQueryBuilder来查询第二个字段fieldName2
        MatchPhraseQueryBuilder secondFieldQuery = QueryBuilders.matchPhraseQuery("fileContent", dto.getFileContent());

        // 创建另一个MatchPhraseQueryBuilder来查询第二个字段fieldName3
        MatchPhraseQueryBuilder threeFieldQuery = QueryBuilders.matchPhraseQuery("fileClassifyName", dto.getFileContent());

        // 使用BoolQueryBuilder将两个查询合并成一个布尔AND查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .should(firstFieldQuery)
                .should(secondFieldQuery)
                .should(threeFieldQuery)
                .minimumShouldMatch(1);

        queryBuilder.withQuery(boolQuery);


        // 设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        String[] fieldNames = {"fileName", "fileContent"};
        for (String fieldName : fieldNames) {
            highlightBuilder.field(fieldName);
        }
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        highlightBuilder.order();
        queryBuilder.withHighlightBuilder(highlightBuilder);

        // 也可以添加分页和排序
        queryBuilder.withSorts(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(dto.getPageNo() - 1, dto.getPageSize()));

        NativeSearchQuery nativeSearchQuery = queryBuilder.build();
        SearchHits<FileInfo> searchHitsList = elasticsearchRestTemplate.search(nativeSearchQuery, FileInfo.class);
        if(searchHitsList != null && searchHitsList.getTotalHits() >0){
            List<SearchHit<FileInfo>> subSearchHitslist = searchHitsList.getSearchHits();
            if(subSearchHitslist != null && subSearchHitslist.size() >0){
                for(SearchHit<FileInfo> searchHit : subSearchHitslist){
                    FileInfo file = searchHit.getContent();
                    String fileId = file.getFileId();
                    String filePath = file.getFilePath();
                    String fileName = file.getFileName();
                    String downloadFileName = fileName;
                    String fileContent = file.getFileContent();

                    Map<String, List<String>> highMap = searchHit.getHighlightFields();
                    if(highMap.get("fileName") != null && highMap.get("fileName").toString().length() > 0){
                        fileName = highMap.get("fileName").toString();
                        fileName = fileName.substring(1,fileName.length()-1);
                    }
                    if(highMap.get("fileContent") != null && highMap.get("fileContent").toString().length() > 0){
                        fileContent = highMap.get("fileContent").toString();
                    }
                    fileContent = fileContent.substring(1,fileContent.length()-1);

                    strFileInfo.append("<li class='fileName'><a href='/search/fileManage/downloadFile?fileId=").append(fileId).append("' download='").append(downloadFileName).append("'>").append(fileName).append("</a></li>");
                    strFileInfo.append("<li class='fileContent'>").append(fileContent).append("</li>");
                    strFileInfo.append("<li class='lineLi'></li>");
                }
            }else{
                strFileInfo.append("<li class='fileEmpty'>").append("未查询到相关信息").append("</li>");
            }
        }else{
            strFileInfo.append("<li class='fileEmpty'>").append("未查询到相关信息").append("</li>");
        }

        retJson.put("retData", strFileInfo.toString());

        return retJson;
    }

*/
/**
     * 文件内容搜索
     * @param dto
     * @return*//*



    @Override
    public List<FileResult> searchTable(FileDTO dto) {
        if(StringUtil.isNotEmpty(dto.getFileClassifyName()) && dto.getFileClassifyName().equals("全部")){
            dto.setFileClassifyName("");
        }

        List<FileResult> retList = new ArrayList<>();
        if(StringUtil.isEmpty(dto.getFileContent()) && StringUtil.isEmpty(dto.getFileClassifyName())){
            return retList;
        }

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // 创建一个MatchPhraseQueryBuilder来查询第一个字段fieldName1
        MatchPhraseQueryBuilder firstFieldQuery = QueryBuilders.matchPhraseQuery("fileName", dto.getFileContent());

        // 创建另一个MatchPhraseQueryBuilder来查询第二个字段fieldName2
        MatchPhraseQueryBuilder secondFieldQuery = QueryBuilders.matchPhraseQuery("fileContent", dto.getFileContent());

        // 创建另一个MatchPhraseQueryBuilder来查询第二个字段fieldName3
        MatchPhraseQueryBuilder threeFieldQuery = QueryBuilders.matchPhraseQuery("fileClassifyName", dto.getFileClassifyName());

        // 使用BoolQueryBuilder将两个查询合并成一个布尔AND查询
        BoolQueryBuilder boolQuery = null;
        if(StringUtil.isNotEmpty(dto.getFileContent()) && StringUtil.isNotEmpty(dto.getFileClassifyName())){
            boolQuery = QueryBuilders.boolQuery()
                    .should(firstFieldQuery)
                    .should(secondFieldQuery)
                    .must(threeFieldQuery)
                    .minimumShouldMatch(1);
        }if(StringUtil.isNotEmpty(dto.getFileContent()) && StringUtil.isEmpty(dto.getFileClassifyName())){
            boolQuery = QueryBuilders.boolQuery()
                    .should(firstFieldQuery)
                    .should(secondFieldQuery)
                    .minimumShouldMatch(1);
        }if(StringUtil.isEmpty(dto.getFileContent()) && StringUtil.isNotEmpty(dto.getFileClassifyName())){
            boolQuery = QueryBuilders.boolQuery()
                    .should(threeFieldQuery)
                    .minimumShouldMatch(1);
        }

        queryBuilder.withQuery(boolQuery);


        // 设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        String[] fieldNames = {"fileName", "fileContent"};
        for (String fieldName : fieldNames) {
            highlightBuilder.field(fieldName);
        }
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        highlightBuilder.order();
        queryBuilder.withHighlightBuilder(highlightBuilder);

        // 也可以添加分页和排序
        queryBuilder.withSorts(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));

        NativeSearchQuery nativeSearchQuery = queryBuilder.build();
        SearchHits<FileInfo> searchHitsList = elasticsearchRestTemplate.search(nativeSearchQuery, FileInfo.class);
        if(searchHitsList != null && searchHitsList.getTotalHits() >0){
            List<SearchHit<FileInfo>> subSearchHitslist = searchHitsList.getSearchHits();
            if(subSearchHitslist != null && subSearchHitslist.size() >0){
                for(SearchHit<FileInfo> searchHit : subSearchHitslist){
                    System.out.println("=======================");
                    FileInfo file = searchHit.getContent();
                    String fileId = file.getFileId();
                    String filePath = file.getFilePath();
                    String fileName = file.getFileName();
                    String downloadFileName = fileName;
                    String fileSize = file.getFileSize();
                    String flieType = file.getFileType();
                    String fileClassifyName = file.getFileClassifyName();
                    String createBy = file.getCreateBy();
                    Date createTime = file.getCreateTime();
                    String fileContent = file.getFileContent();


                    Map<String, List<String>> highMap = searchHit.getHighlightFields();
                    if(highMap.get("fileName") != null && highMap.get("fileName").toString().length() > 0){
                        fileName = highMap.get("fileName").toString();
                        fileName = fileName.substring(1,fileName.length()-1);
                    }
                    if(highMap.get("fileContent") != null && highMap.get("fileContent").toString().length() > 0){
                        fileContent = highMap.get("fileContent").toString();
                    }
                    fileContent = fileContent.substring(1,fileContent.length()-1);

                    FileResult fileResult = new FileResult();
                    fileResult.setFileId(fileId);
                    fileResult.setFileName(fileName);
                    fileResult.setFileSize(fileSize);
                    fileResult.setFileType(flieType);
                    fileResult.setFileClassifyName(fileClassifyName);
                    fileResult.setCreateBy(createBy);
                    fileResult.setCreateTime(createTime);
                    fileResult.setResult(fileContent);
                    fileResult.setFilePath(filePath);
                    retList.add(fileResult);
                }
            }
        }

        return retList;
    }

*/
/**
     * 文档预览
     * @param param
     * @return*//*



    @Override
    public String fileView(String param) {
        JSONObject retJson = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String fileId = paramJson.getString("fileId");

*/
/*QueryWrapper<SearchFileManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_id", fileId);
            queryWrapper.last("limit 1");
            SearchFileManage searchFileManage = searchFileManageMapper.selectOne(queryWrapper);*//*


            SearchFileManage searchFileManage = searchFileManageMapper.selectById(fileId);
            if(searchFileManage != null){
                String fileName = searchFileManage.getFileName();
                String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1);

                String retPdfPath ="";
                if ("doc".equals(fileExt) || "docx".equals(fileExt) || "xls".equals(fileExt) || "xlsx".equals(fileExt) || "ppt".equals(fileExt) || "pptx".equals(fileExt) || "txt".equals(fileExt)) {

                    String filePaht =  searchFileManage.getFilePath();
                    //文件转PDF格式
                    FileToPdfUtils.officeToPdf(filePaht,searchFileTopdfPath);

                    String pdfName = fileName.substring(0,fileName.lastIndexOf(".")+1)+"pdf";

                    String port = env.getProperty("server.port");
                    String serverIp = env.getProperty("server.server-ip");

                    retPdfPath =serverIp+":"+port+"/searchFileTopdf/"+pdfName;
                }else{
                    String port = env.getProperty("server.port");
                    String serverIp = env.getProperty("server.server-ip");
                    retPdfPath =serverIp+":"+port+"/searchUpload/"+fileName;
                }

                log.info("++++++++++++retPdfPath: {}",retPdfPath);

                retJson.put("retCode","0");
                retJson.put("retMsg",retPdfPath);
            }else{
                retJson.put("retCode","0");
                retJson.put("retMsg","根据文件ID【"+fileId+"】未找到对应的文件信息！");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","0");
            retJson.put("retMsg","预览文件失败！");
        }

        return retJson.toJSONString();
    }


}
*/
