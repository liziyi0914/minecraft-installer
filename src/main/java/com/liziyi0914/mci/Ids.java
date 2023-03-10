package com.liziyi0914.mci;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.minecraft.Version;
import com.liziyi0914.mci.mirror.Mirror;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Ids {

    /** 任务ID
     */
    public static final Identifier<Long> VAR_TASK_ID = Identifier.of(Long.class);

    /** 开启多线程模式
     */
    public static final Identifier<Boolean> VAR_MULTI_THREAD = Identifier.of(Boolean.class);

    /** 合并版本
     */
    public static final Identifier<Boolean> VAR_MIX = Identifier.of(Boolean.class);

    /** 任务名称
     */
    public static final Identifier<String> VAR_TASK_NAME = Identifier.of(String.class);

    /** 镜像站
     */
    public static final Identifier<Mirror> VAR_MIRROR = Identifier.of(Mirror.class);

    /** 覆盖已有文件
     */
    public static final Identifier<Boolean> VAR_OVERRIDE_EXISTS = Identifier.of(Boolean.class);

    /** 版本id
     */
    public static final Identifier<String> VAR_ID = Identifier.of(String.class);

    /** Minecraft根目录
     */
    public static final Identifier<Path> VAR_MINECRAFT_ROOT = Identifier.of(Path.class);

    /** Minecraft版本
     */
    public static final Identifier<String> VAR_MINECRAFT_VERSION = Identifier.of(String.class);

    /** Minecraft版本json file
     */
    public static final Identifier<FileInfo> VAR_MINECRAFT_JSON_FILE = Identifier.of(FileInfo.class);

    /** Minecraft版本json data
     */
    public static final Identifier<Version> VAR_MINECRAFT_JSON = Identifier.of(Version.class);

    /** Minecraft版本jar file
     */
    public static final Identifier<FileInfo> VAR_MINECRAFT_JAR_FILE = Identifier.of(FileInfo.class);

    /** Minecraft AssetIndex file
     */
    public static final Identifier<FileInfo> VAR_MINECRAFT_ASSET_INDEX_FILE = Identifier.of(FileInfo.class);

    /** Minecraft Log4j file
     */
    public static final Identifier<FileInfo> VAR_MINECRAFT_LOG4J_FILE = Identifier.of(FileInfo.class);

    /** Library files
     */
    public static final Identifier<List<FileInfo>> VAR_LIBRARY_FILES = Identifier.list(FileInfo.class);

    /** 失败文件清单
     */
    public static final Identifier<List<FileInfo>> VAR_FILES_FAILED = Identifier.list(FileInfo.class);

    /** Minecraft Assets是否存入virtual文件夹
     */
    public static final Identifier<Boolean> VAR_MINECRAFT_ASSET_INDEX_VIRTUAL = Identifier.of(Boolean.class);

    /** Minecraft Asset files
     */
    public static final Identifier<List<FileInfo>> VAR_MINECRAFT_ASSET_FILES = Identifier.list(FileInfo.class);



    /** Forge Version
     */
    public static final Identifier<String> VAR_FORGE_VERSION = Identifier.of(String.class);

    /** Forge Branch
     */
    public static final Identifier<String> VAR_FORGE_BRANCH = Identifier.of(String.class);

    /** Forge Build
     */
    public static final Identifier<Long> VAR_FORGE_BUILD = Identifier.of(Long.class);

    /** Forge installer file
     */
    public static final Identifier<FileInfo> VAR_FORGE_INSTALLER_FILE = Identifier.of(FileInfo.class);

    /** Forge 变量表
     */
    public static final Identifier<Map<String,String>> VAR_FORGE_VARS = Identifier.map(String.class,String.class);

    /** Forge 变量表
     */
    public static final Identifier<JSONArray> VAR_FORGE_PROCESSORS = Identifier.of(JSONArray.class);



    /** OptiFine Type
     */
    public static final Identifier<String> VAR_OPTIFINE_TYPE = Identifier.of(String.class);

    /** OptiFine Patch
     */
    public static final Identifier<String> VAR_OPTIFINE_PATCH = Identifier.of(String.class);

    /** OptiFine installer file
     */
    public static final Identifier<FileInfo> VAR_OPTIFINE_INSTALLER_FILE = Identifier.of(FileInfo.class);



    /** LityLoader Version
     */
    public static final Identifier<String> VAR_LITE_LOADER_VERSION = Identifier.of(String.class);



    /** Fabric Version
     */
    public static final Identifier<String> VAR_FABRIC_VERSION = Identifier.of(String.class);

    /** Fabric Meta
     */
    public static final Identifier<JSONObject> VAR_FABRIC_META = Identifier.of(JSONObject.class);

    /** isQuilt
     */
    public static final Identifier<Boolean> VAR_QUILT = Identifier.of(Boolean.class);

}
