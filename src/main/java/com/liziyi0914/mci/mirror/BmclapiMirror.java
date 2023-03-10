package com.liziyi0914.mci.mirror;

public class BmclapiMirror extends OfficialMirror {

    String domain() {
        return "bmclapi2.bangbang93.com";
    }

    @Override
    public String manifest(String url) {
        return url.replace("http://launchermeta.mojang.com", "https://" + domain());
    }

    @Override
    public String minecraftJson(String url) {
        return url.replaceAll("https?://.*?\\.mojang\\.com", "https://" + domain());
    }

    @Override
    public String minecraftJar(String url) {
        return url.replaceAll("https?://.*?\\.mojang\\.com", "https://" + domain());
    }

    @Override
    public String minecraftAssetIndex(String url) {
        return url.replaceAll("https?://.*?\\.mojang\\.com", "https://" + domain());
    }

    @Override
    public String minecraftAsset(String url) {
        return url.replace("https://resources.download.minecraft.net", "https://" + domain() + "/assets");
    }

    @Override
    public String minecraftLibrary(String url) {
        return url.replace("https://libraries.minecraft.net", "https://" + domain() + "/maven");
    }

    @Override
    public String minecraftLog4j(String url) {
        return url.replaceAll("https?://.*?\\.mojang\\.com", "https://" + domain());
    }

    @Override
    public String forge(String url) {
        return url
                .replace("https://maven.minecraftforge.net", "https://" + domain() + "/maven")
                .replace("https://files.minecraftforge.net/maven", "https://" + domain() + "/maven");
    }

    @Override
    public String liteLoader(String url) {
//        return url.replace("http://dl.liteloader.com/versions/versions.json","https://"+domain()+"/maven/com/mumfrey/liteloader/versions.json")
//                .replace("http://dl.liteloader.com/versions/com/mumfrey/liteloader/","https://"+domain()+"/maven/com/mumfrey/liteloader/");
        return super.liteLoader(url);
    }

    @Override
    public String fabric(String url) {
        return url.replace("https://meta.fabricmc.net", "https://" + domain() + "/fabric-meta")
                .replace("https://maven.fabricmc.net", "https://" + domain() + "/maven");
    }
}
