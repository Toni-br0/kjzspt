/*
package com.pearadmin.modules.search.config;



import com.pearadmin.modules.search.domain.FileInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.HighlightParameters;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends ElasticsearchRepository<FileInfo, String> {

*/
/**
   * 关键字查询
   *
   * @return*//*




  @Highlight(fields = {@HighlightField(name = "fileName"), @HighlightField(name = "fileContent")},
          parameters = @HighlightParameters(preTags = {"<span style='color:red'>"}, postTags = {"</span>"}, numberOfFragments = 0))
  List<SearchHit<FileInfo>> findByFileNameOrFileContent(String fileName, String fileContent, Pageable pageable);
}
*/
