package com.liziyi0914.mci.bean.minecraft;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Version {

    String id;
    String assets;
    String mainClass;
    int minimumLauncherVersion;
    String releaseTime;
    String time;
    String type;
    String clientVersion;
    String minecraftArguments;
    String jar;
    String inheritsFrom;

    Map<String, List<Object>> arguments;
    _AssetIndex assetIndex;

    JSONObject logging;

    Map<String,Download> downloads;

    Map<String,Object> javaVersion;

    List<Library> libraries;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class _AssetIndex {

        String id;
        String sha1;
        int size;
        int totalSize;
        String url;

    }
}
