package com.liziyi0914.mci.bean.minecraft;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Library {

    String name;
    String url;
    Map<String,String> natives;
    List<Rule> rules;
    _Download downloads;

    JSONObject extract;

    Boolean serverreq;
    Boolean clientreq;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class _Download {

        Download artifact;
        Map<String,Download> classifiers;

    }

}
