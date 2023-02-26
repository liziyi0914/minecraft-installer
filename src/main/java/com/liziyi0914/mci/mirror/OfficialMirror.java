package com.liziyi0914.mci.mirror;

public class OfficialMirror extends Mirror {

    @Override
    public String manifest(String url) {
        return url;
    }

    @Override
    public String minecraftJson(String url) {
        return url;
    }

    @Override
    public String minecraftJar(String url) {
        return url;
    }

    @Override
    public String minecraftAssetIndex(String url) {
        return url;
    }

    @Override
    public String minecraftAsset(String url) {
        return url;
    }

    @Override
    public String minecraftLibrary(String url) {
        return url;
    }

    @Override
    public String minecraftLog4j(String url) {
        return url;
    }

    @Override
    public String forge(String url) {
        return url;
    }

}
