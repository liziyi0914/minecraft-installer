package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONObject;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Data
@Slf4j
@AllArgsConstructor
public class ForgeNewExtractTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        Path minecraftRoot = ctx.get(Identifiers.VAR_MINECRAFT_ROOT);
        FileInfo forgeInstaller = ctx.get(Identifiers.VAR_FORGE_INSTALLER_FILE);
        FileInfo mcJarFile = ctx.get(Identifiers.VAR_MINECRAFT_JAR_FILE);
        String id = ctx.get(Identifiers.VAR_ID);
        Mirror mirror = ctx.get(Identifiers.VAR_MIRROR);
        boolean mix = ctx.get(Identifiers.VAR_MIX);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行新版Forge解包流程");

        File file = forgeInstaller.getFile();
        try (ZipFile zipFile = new ZipFile(file)) {
            JSONObject installProfile;
            {
                InputStream stream = zipFile.getInputStream(zipFile.getEntry("install_profile.json"));
                if (stream == null)
                    throw new RuntimeException("找不到install_profile.json");
                installProfile = JSONUtil.parseObj(IoUtil.readUtf8(stream));
                stream.close();
            }

            // 释放json
            log.info("开始释放version.json");
            subTaskInfo.update(8192, "释放version.json", SubTaskInfo.STATUS_RUNNING);
            Version forgeJson;
            {
                InputStream stream = zipFile.getInputStream(zipFile.getEntry("version.json"));
                if (stream == null)
                    throw new RuntimeException("找不到version.json");
                forgeJson = JSONUtil.toBean(IoUtil.readUtf8(stream), Version.class);
                stream.close();
            }
            File jsonFile = FileUtil.file(
                    minecraftRoot.toFile(),
                    "versions",
                    id,
                    id + ".json"
            );
            if (mix) {
                // 合并json
//                JSONObject baseJson = JSONUtil.parseObj(FileUtil.readUtf8String(jsonFile));
//                forgeJson = Utils.mixJson(baseJson, forgeJson);
                Version baseJson = ctx.get(Identifiers.VAR_MINECRAFT_JSON);
                forgeJson = Utils.mixJson(baseJson, forgeJson);
            } else {
                forgeJson.setId(id);
            }
            ctx.put(Identifiers.VAR_MINECRAFT_JSON, forgeJson);
            ctx.put(Identifiers.VAR_MINECRAFT_JSON_FILE, FileInfo
                    .builder()
                    .file(jsonFile)
                    .build()
            );
            log.info("version.json释放完成");

            // 释放jar
            log.info("开始释放maven文件夹");
            subTaskInfo.update(16384, "释放maven", SubTaskInfo.STATUS_RUNNING);
            {
                Path targetPath = FileUtil.file(
                        minecraftRoot.toFile(),
                        "libraries"
                ).toPath();
                String sourceDirectoryPath = "maven/";
                try (ZipInputStream zis = new ZipInputStream(FileUtil.getInputStream(file))) {
                    ZipEntry entry = null;
                    while ((entry = zis.getNextEntry()) != null) {
                        // 如果entry不是指定目录下的项，则跳过
                        if (!entry.getName().startsWith(sourceDirectoryPath)) {
                            continue;
                        }
                        // 如果entry是目录，则在目标文件夹中创建相应的目录
                        if (entry.isDirectory()) {
                            Path directoryPath = targetPath.resolve(entry.getName().substring(sourceDirectoryPath.length()));
                            Files.createDirectories(directoryPath);
                        } else {
                            // 如果entry是文件，则将文件写入目标文件夹中相应的路径下
                            Path filePath = targetPath.resolve(entry.getName().substring(sourceDirectoryPath.length()));
                            FileOutputStream fos = new FileOutputStream(filePath.toFile());
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                            fos.close();
                        }
                    }
                }
            }
            log.info("maven文件夹释放完成");

            // 释放lzma
            log.info("开始释放lzma");
            subTaskInfo.update(24576, "释放lzma", SubTaskInfo.STATUS_RUNNING);
            File lzmaFile = File.createTempFile("ln-minecraft-installer", ".lzma");
            {
                InputStream stream = zipFile.getInputStream(zipFile.getEntry("data/client.lzma"));
                if (stream == null)
                    throw new RuntimeException("找不到client.lzma");
                OutputStream out = FileUtil.getOutputStream(lzmaFile);
                IoUtil.copy(stream, out);
                out.close();
                stream.close();
            }
            log.info("lzma释放完成");

            // 添加libraries
            log.info("开始添加libraries");
            subTaskInfo.update(32768, "添加libraries", SubTaskInfo.STATUS_RUNNING);
            {
                List<FileInfo> libs = forgeJson.getLibraries()
                        .stream()
//                    .filter(obj -> !maven.equals(obj.getStr("name")))
                        .map(obj -> {
                            String name = obj.getName();
                            Download artifact = Optional.ofNullable(obj.getDownloads()).map(Library._Download::getArtifact).orElse(null);
                            if (
                                    Objects.isNull(artifact) &&
                                            FileUtil.exist(FileUtil.file(
                                                            minecraftRoot.toFile(),
                                                            "libraries",
                                                            Utils.mavenPath(name),
                                                            Utils.mavenFileName(name)
                                                    )
                                            )) {
                                return null;
                            }
                            String url = Optional.ofNullable(artifact).map(Download::getUrl).orElse(null);
                            String hash = Optional.ofNullable(artifact).map(Download::getSha1).orElse(null);

                            if (Optional.ofNullable(url).orElse("").contains("forge")) {
                                url = mirror.forge("https://maven.minecraftforge.net/" + Utils.mavenPath(name) + "/" + Utils.mavenFileName(name));
                            } else {
                                url = mirror.minecraftLibrary("https://libraries.minecraft.net/" + Utils.mavenPath(name) + "/" + Utils.mavenFileName(name));
                            }

                            log.info("找到Library: {}，下载链接: {}", name, url);

                            return FileInfo.builder()
                                    .id(name)
                                    .name(Utils.mavenFileName(name))
                                    .url(url)
                                    .file(FileUtil.file(
                                            minecraftRoot.toFile(),
                                            "libraries",
                                            Utils.mavenPath(name),
                                            Utils.mavenFileName(name)
                                    ))
                                    .hash(Optional.ofNullable(hash).filter(s -> !s.isEmpty()).orElse(null))
                                    .build();
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                ctx.addAll(Identifiers.VAR_LIBRARY_FILES, libs);
            }
            log.info("libraries添加完成");

            // 添加libraries
            log.info("开始添加installer libraries");
            subTaskInfo.update(40960, "添加libraries", SubTaskInfo.STATUS_RUNNING);
            {
                List<FileInfo> libs = installProfile.getJSONArray("libraries")
                        .toList(JSONObject.class)
                        .stream()
//                    .filter(obj -> !maven.equals(obj.getStr("name")))
                        .map(obj -> {
                            String name = obj.getStr("name");
                            JSONObject artifact = obj.getJSONObject("downloads").getJSONObject("artifact");
                            String url = artifact.getStr("url");
                            String hash = artifact.getStr("sha1");

                            if (Optional.ofNullable(url).orElse("").contains("forge")) {
                                url = mirror.forge("https://maven.minecraftforge.net/" + Utils.mavenPath(name) + "/" + Utils.mavenFileName(name));
                            } else {
                                url = mirror.minecraftLibrary("https://libraries.minecraft.net/" + Utils.mavenPath(name) + "/" + Utils.mavenFileName(name));
                            }

                            log.info("找到Library: {}，下载链接: {}", name, url);

                            return FileInfo.builder()
                                    .id(name)
                                    .name(Utils.mavenFileName(name))
                                    .url(url)
                                    .file(FileUtil.file(
                                            minecraftRoot.toFile(),
                                            "libraries",
                                            Utils.mavenPath(name),
                                            Utils.mavenFileName(name)
                                    ))
                                    .hash(Optional.ofNullable(hash).filter(s -> !s.isEmpty()).orElse(null))
                                    .build();
                        })
                        .collect(Collectors.toList());
                ctx.addAll(Identifiers.VAR_LIBRARY_FILES, libs);
            }
            log.info("libraries添加完成");

            // 保存变量表
            log.info("开始保存变量表");
            subTaskInfo.update(49152, "保存变量表", SubTaskInfo.STATUS_RUNNING);
            JSONObject dataObj = installProfile.getJSONObject("data");
            dataObj.keySet().forEach(key -> {
                String rawValue = dataObj.getJSONObject(key).getStr("client");

                String value = null;
                if (rawValue.startsWith("[") && rawValue.endsWith("]")) {
                    String tmp = rawValue.substring(1, rawValue.length() - 1);
                    try {
                        value = FileUtil.file(
                                minecraftRoot.toFile(),
                                "libraries",
                                Utils.mavenPath(tmp),
                                Utils.mavenFileName(tmp)
                        ).getCanonicalPath();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (rawValue.startsWith("'") && rawValue.endsWith("'")) {
                    value = rawValue.substring(1, rawValue.length() - 1);
                } else if (rawValue.equals("/data/client.lzma")) {
                    try {
                        value = lzmaFile.getCanonicalPath();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                log.info("找到变量: {}, 原始值: {}, 解析值: {}", key, rawValue, value);

                ctx.mapPut(Identifiers.VAR_FORGE_VARS, key, value);
            });
            ctx.mapPut(Identifiers.VAR_FORGE_VARS, "MINECRAFT_JAR", mcJarFile.getFile().getCanonicalPath());
            log.info("变量表保存完成");

            // 保存修补器列表
            log.info("开始保存修补器列表");
            subTaskInfo.update(57344, "保存修补器", SubTaskInfo.STATUS_RUNNING);
            ctx.put(Identifiers.VAR_FORGE_PROCESSORS, installProfile.getJSONArray("processors"));
        } catch (IOException e) {
            log.error("forge installer file解析失败", e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("Forge解包任务执行成功");
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
