/*
package com.pearadmin.modules.search.service.impl;


*/
/**
 * 创建日期：2024-11-26
 **//*




import cn.hutool.core.util.IdUtil;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.modules.search.config.FileRepository;
import com.pearadmin.modules.search.domain.FileInfo;
import com.pearadmin.modules.search.domain.FileDTO;
import com.pearadmin.modules.search.service.IFileService;
import com.pearadmin.modules.search.util.FileUtils;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;


*/
/**
 * description: ES文件服务
 *
 * @author yangfeng
 * @version V1.0
 * @date 2023-02-21*//*




@Slf4j
@Service
public class FileServiceImpl implements IFileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private SysUserService sysUserService;


*/
/**
     * 保存文件*//*




    @Override
    public void saveFile(MultipartFile multFile) throws Exception {
        if (Objects.isNull(multFile)) {
            throw new IOException("文件不存在");
        }

        //获取当前登录用户信息
        SysUser currentUser = UserContext.currentUser();
        if(currentUser == null){
            //取管理员信息
            currentUser = sysUserService.getById("1309861917694623744");
        }

        String fileName = multFile.getOriginalFilename();
        String fileType = StringUtils.isNotBlank(fileName) ? fileName.substring(fileName.lastIndexOf(".") + 1) : null;

        InputStream inputStream = multFile.getInputStream();

        // 读取文件内容，上传到es，方便后续的检索
        String fileContent = FileUtils.readFileContent(inputStream, fileType,null);
        FileInfo file = new FileInfo();
        file.setFileId(IdUtil.getSnowflake(1, 1).nextIdStr());
        file.setFileContent(fileContent);
        file.setFileName(fileName);
        file.setFilePath("D:\\zswdFile\\uploadFile\\123.docx");
        file.setFileType(fileType);
        file.setFileCategory(fileType);
        file.setCreateBy(currentUser.getRealName());
        file.setCreateTime(new Date());
        fileRepository.save(file);

    }



*/
/**
     * 关键字查询
     *
     * @return*//*




    @Override
    public List<SearchHit<FileInfo>> search(FileDTO dto) {
        Pageable pageable = PageRequest.of(dto.getPageNo() - 1, dto.getPageSize(), Sort.Direction.DESC, "createTime");
        return fileRepository.findByFileNameOrFileContent(dto.getFileContent(), dto.getFileContent(), pageable);
    }


    @Override
    public SearchHits<FileInfo> searchPage(FileDTO dto) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.multiMatchQuery(dto.getFileContent(), "fileName", "fileContent"));
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

        return elasticsearchRestTemplate.search(nativeSearchQuery, FileInfo.class);
    }

*/
/**
     * 根据ID删除文件
     * @param id
     * @return*//*



    @Override
    public String deleteById(String id) {
        fileRepository.deleteById(id);
        return "删除文件成功";
    }

}

*/
