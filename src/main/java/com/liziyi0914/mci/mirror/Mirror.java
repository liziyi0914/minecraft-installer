package com.liziyi0914.mci.mirror;

public abstract class Mirror {
    public abstract String manifest(String url);

    public abstract String minecraftJson(String url);

    public abstract String minecraftJar(String url);

    public abstract String minecraftAssetIndex(String url);

    public abstract String minecraftAsset(String url);

    public abstract String minecraftLibrary(String url);

    public abstract String minecraftLog4j(String url);
}
