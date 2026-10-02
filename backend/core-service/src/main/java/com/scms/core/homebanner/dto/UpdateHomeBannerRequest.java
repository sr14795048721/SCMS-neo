package com.scms.core.homebanner.dto;

import jakarta.validation.constraints.Size;

public class UpdateHomeBannerRequest {

    @Size(max = 180, message = "title length must be at most 180")
    private String title;

    @Size(max = 1000, message = "description length must be at most 1000")
    private String desc;

    private String type;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
