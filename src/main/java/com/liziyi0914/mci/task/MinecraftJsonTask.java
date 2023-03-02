package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.liziyi0914.mci.Identifiers;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.bean.minecraft.Download;
import com.liziyi0914.mci.bean.minecraft.Library;
import com.liziyi0914.mci.bean.minecraft.Version;
import com.liziyi0914.mci.mirror.Mirror;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Slf4j
@AllArgsConstructor
public class MinecraftJsonTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        Mirror mirror = ctx.get(Identifiers.VAR_MIRROR);
        Path minecraftRoot = ctx.get(Identifiers.VAR_MINECRAFT_ROOT);
        String id = ctx.get(Identifiers.VAR_ID);
        FileInfo jsonFile = ctx.get(Identifiers.VAR_MINECRAFT_JSON_FILE);
        String url = jsonFile.getUrl();
        File file = jsonFile.getFile();

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行Minecraft JSON任务");

        log.info("Minecraft JSON文件链接: {}", url);

        try {
            Optional<String> jsonOpt = Utils.downloadSync(url);

            if (!jsonOpt.isPresent()) {
                throw new IOException("下载失败");
            }

            log.info("Minecraft JSON文件下载完成");

            log.info("开始解析Minecraft JSON文件");

            String jsonString = jsonOpt.get();

            if (!Utils.checkBufferHash(jsonFile.getHash(),jsonString.getBytes(StandardCharsets.UTF_8))) {
                throw new IOException("文件校验失败");
            }

            Version json = JSONUtil.toBean(jsonString, Version.class);
            json.setId(id);
            ctx.put(Identifiers.VAR_MINECRAFT_JSON, json);

            log.info("Minecraft JSON文件写入完成");

            subTaskInfo.update(32767, "解析中", SubTaskInfo.STATUS_RUNNING);

            {
                Download obj = json.getDownloads().get("client");
                ctx.put(
                        Identifiers.VAR_MINECRAFT_JAR_FILE,
                        FileInfo.builder()
                                .url(mirror.minecraftJar(obj.getUrl()))
                                .hash(obj.getSha1())
                                .file(FileUtil.file(minecraftRoot.toFile(), "versions", id, id + ".jar"))
                                .build()
                );

                log.info("Minecraft JAR文件链接: {}", obj.getUrl());
            }

            subTaskInfo.update(40960, "解析中", SubTaskInfo.STATUS_RUNNING);

            {
                Version._AssetIndex obj = json.getAssetIndex();
                ctx.put(
                        Identifiers.VAR_MINECRAFT_ASSET_INDEX_FILE,
                        FileInfo.builder()
                                .name(obj.getId() + ".json")
                                .url(mirror.minecraftAssetIndex(obj.getUrl()))
                                .hash(obj.getSha1())
                                .file(FileUtil.file(minecraftRoot.toFile(), "assets", "indexes", obj.getId() + ".json"))
                                .build()
                );

                log.info("Minecraft AssetIndex文件链接: {}", obj.getUrl());
            }

            subTaskInfo.update(49151, "解析中", SubTaskInfo.STATUS_RUNNING);

            {
                List<Library> libs = json.getLibraries();
                List<FileInfo> list = libs.stream()
                        .flatMap(o -> {
                            Library._Download downloads = o.getDownloads();
                            Map<String, Download> classifiers = downloads.getClassifiers();
                            Download artifact = downloads.getArtifact();

                            List<FileInfo> files = new ArrayList<>();
                            if (Objects.nonNull(artifact)) {
                                files.add(
                                        FileInfo.builder()
                                                .id(o.getName())
                                                .name(Utils.mavenFileName(o.getName()))
                                                .url(mirror.minecraftLibrary(artifact.getUrl()))
                                                .hash(artifact.getSha1())
                                                .file(FileUtil.file(
                                                        minecraftRoot.toFile(),
                                                        "libraries",
                                                        Utils.mavenPath(o.getName()),
                                                        Utils.mavenFileName(o.getName())
                                                ))
                                                .build()
                                );
                            }

                            if (Objects.nonNull(classifiers)) {
                                String os = Utils.getOs();
                                Optional.ofNullable(o.getNatives())
                                        .map(obj->obj.get(os))
                                        .ifPresent(os2 -> {
                                            Download osObj = classifiers.get(os2);
                                            if (!Objects.isNull(osObj)) {
                                                files.add(
                                                        FileInfo.builder()
                                                                .id(o.getName())
                                                                .name(Utils.mavenFileName(o.getName()+":"+os2))
                                                                .url(mirror.minecraftLibrary(osObj.getUrl()))
                                                                .hash(osObj.getSha1())
                                                                .file(FileUtil.file(
                                                                        minecraftRoot.toFile(),
                                                                        "libraries",
                                                                        Utils.mavenPath(o.getName()),
                                                                        Utils.mavenFileName(o.getName()+":"+os2)
                                                                ))
                                                                .build()
                                                );
                                            }
                                        });
                            }

                            return files.stream();
                        })
                        .distinct()
                        .peek(fileInfo -> log.info("Minecraft Library文件: {}", fileInfo.getName()))
                        .collect(Collectors.toList());
                ctx.put(Identifiers.VAR_LIBRARY_FILES, list);
            }

            subTaskInfo.update(57343, "解析中", SubTaskInfo.STATUS_RUNNING);

            {
                Optional.ofNullable(json.getLogging())
                        .map(obj -> obj.getJSONObject("client"))
                        .map(obj -> obj.getJSONObject("file"))
                        .ifPresent(obj -> {
                            ctx.put(
                                    Identifiers.VAR_MINECRAFT_LOG4J_FILE,
                                    FileInfo.builder()
                                            .name(obj.getStr("id"))
                                            .url(mirror.minecraftLog4j(obj.getStr("url")))
                                            .hash(obj.getStr("sha1"))
                                            .file(FileUtil.file(minecraftRoot.toFile(), "assets", "log_configs", obj.getStr("id")))
                                            .build()
                            );

                            log.info("Minecraft Log4j文件链接: {}", obj.getStr("url"));
                        });
            }
        } catch (IOException e) {
            log.error("Minecraft JSON任务执行失败",e);
            subTaskInfo.update(0, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }
        log.info("Minecraft JSON任务执行成功");
        subTaskInfo.update(0, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
