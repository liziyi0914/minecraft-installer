package com.liziyi0914.mci.bean.minecraft;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Library {

    String name;
    String url;
    HashMap<String,String> natives;
    ArrayList<Rule> rules;
    _Download downloads;

    JSONObject extract;

    boolean serverreq;
    boolean clientreq;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class _Download {

        Download artifact;
        HashMap<String,Download> classifiers;

    }

}
