package com.pearadmin.modules.report.domain;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("reportHead")
public class ReportHead {
    private String id;
    private String title;
    private String field;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
