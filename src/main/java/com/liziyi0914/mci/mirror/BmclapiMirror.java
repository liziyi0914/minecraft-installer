package com.liziyi0914.mci.mirror;

public class BmclapiMirror extends OfficialMirror {

    String domain() {
        return "bmclapi2.bangbang93.com";
    }

    @Override
    public String manifest(String url) {
        return url.replace("http://launchermeta.mojang.com","https://"+ domain());
    }

    @Override
    public String minecraftJson(String url) {
        return url.replaceAll("https?://.*?\\.mojang\\.com","https://"+ domain());
    }

    @Override
    public String minecraftJar(String url) {
        return url.replaceAll("https?://.*?\\.mojang\\.com","https://"+ domain());
    }

    @Override
    public String minecraftAssetIndex(String url) {
        return url.replaceAll("https?://.*?\\.mojang\\.com","https://"+ domain());
    }

    @Override
    public String minecraftAsset(String url) {
        return url.replace("https://resources.download.minecraft.net","https://"+ domain() +"/assets");
    }

    @Override
    public String minecraftLibrary(String url) {
        return url.replace("https://libraries.minecraft.net","https://"+ domain() +"/maven");
    }

    @Override
    public String minecraftLog4j(String url) {
        return url.replaceAll("https?://.*?\\.mojang\\.com","https://"+ domain());
    }

}
