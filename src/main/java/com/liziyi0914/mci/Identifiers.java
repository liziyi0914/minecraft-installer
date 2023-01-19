package com.liziyi0914.mci;

import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.mirror.Mirror;

import java.nio.file.Path;
import java.util.List;

public class Identifiers {

    /** 任务ID
     */
    public static final Identifier<Long> VAR_TASK_ID = Identifier.of(Long.class);

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

    /** Minecraft版本jar file
     */
    public static final Identifier<FileInfo> VAR_MINECRAFT_JAR_FILE = Identifier.of(FileInfo.class);

    /** Minecraft AssetIndex file
     */
    public static final Identifier<FileInfo> VAR_MINECRAFT_ASSET_INDEX_FILE = Identifier.of(FileInfo.class);

    /** Minecraft Log4j file
     */
    public static final Identifier<FileInfo> VAR_MINECRAFT_LOG4J_FILE = Identifier.of(FileInfo.class);

    /** Minecraft Library files
     */
    public static final Identifier<List<FileInfo>> VAR_MINECRAFT_LIBRARY_FILES = Identifier.list(FileInfo.class);

    /** 失败文件清单
     */
    public static final Identifier<List<FileInfo>> VAR_FILES_FAILED = Identifier.list(FileInfo.class);

    /** Minecraft Assets是否存入virtual文件夹
     */
    public static final Identifier<Boolean> VAR_MINECRAFT_ASSET_INDEX_VIRTUAL = Identifier.of(Boolean.class);

    /** Minecraft Asset files
     */
    public static final Identifier<List<FileInfo>> VAR_MINECRAFT_ASSET_FILES = Identifier.list(FileInfo.class);

}