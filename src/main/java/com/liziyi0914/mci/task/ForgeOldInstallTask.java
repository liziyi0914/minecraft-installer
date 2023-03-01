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
import com.liziyi0914.mci.mirror.Mirror;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

@Data
@Slf4j
@AllArgsConstructor
public class ForgeOldInstallTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        Path minecraftRoot = ctx.get(Identifiers.VAR_MINECRAFT_ROOT);
        FileInfo forgeInstaller = ctx.get(Identifiers.VAR_FORGE_INSTALLER_FILE);
        String forgeVersion = ctx.get(Identifiers.VAR_FORGE_VERSION);
        String version = ctx.get(Identifiers.VAR_MINECRAFT_VERSION);
        String id = ctx.get(Identifiers.VAR_ID);
        Mirror mirror = ctx.get(Identifiers.VAR_MIRROR);
        boolean mix = ctx.get(Identifiers.VAR_MIX);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行旧版Forge安装流程");

        File file = forgeInstaller.getFile();
        try (ZipFile zipFile = new ZipFile(file)) {
            InputStream profileStream = zipFile.getInputStream(zipFile.getEntry("install_profile.json"));
            if (profileStream == null)
                throw new RuntimeException("找不到install_profile.json");
            JSONObject installProfile = JSONUtil.parseObj(IoUtil.readUtf8(profileStream));

            // 释放json
            log.info("开始释放install_profile.json");
            subTaskInfo.update(16384, "释放install_profile.json", SubTaskInfo.STATUS_RUNNING);
            JSONObject forgeJson = installProfile.getJSONObject("versionInfo");
            File jsonFile = FileUtil.file(
                    minecraftRoot.toFile(),
                    "versions",
                    id,
                    id + ".json"
            );
            if (mix) {
                // 合并json
                JSONObject baseJson = JSONUtil.parseObj(FileUtil.readUtf8String(jsonFile));
                forgeJson = Utils.mixJson(baseJson, forgeJson);
            } else {
                forgeJson.set("id", id);
            }
            IoUtil.writeUtf8(
                    FileUtil.getOutputStream(jsonFile),
                    true,
                    forgeJson.toString()
            );
            log.info("install_profile.json释放完成");

            // 释放jar
            log.info("开始释放forge.jar");
            subTaskInfo.update(32768, "释放forge.jar", SubTaskInfo.STATUS_RUNNING);
            InputStream jarStream = zipFile.getInputStream(zipFile.getEntry(installProfile.getJSONObject("install").getStr("filePath")));
            if (jarStream == null)
                throw new RuntimeException("找不到universal jar");
            String maven = installProfile.getJSONObject("install").getStr("path");
            OutputStream out = FileUtil.getOutputStream(FileUtil.file(
                    minecraftRoot.toFile(),
                    "libraries",
                    Utils.mavenPath(maven),
                    Utils.mavenFileName(maven)
            ));
            IoUtil.copy(jarStream, out);
            out.close();
            jarStream.close();
            log.info("forge.jar释放完成");

            // 添加libraries
            log.info("开始添加libraries");
            subTaskInfo.update(49152, "添加libraries", SubTaskInfo.STATUS_RUNNING);
            List<FileInfo> libs = forgeJson.getJSONArray("libraries")
                    .toList(JSONObject.class)
                    .stream()
                    .filter(obj -> !maven.equals(obj.getStr("name")))
                    .map(obj -> {
                        String name = obj.getStr("name");
                        String url = obj.getStr("url");

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
                                .build();
                    })
                    .collect(Collectors.toList());
            ctx.addAll(Identifiers.VAR_LIBRARY_FILES, libs);
        } catch (IOException e) {
            log.error("forge installer file解析失败", e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("Forge安装任务执行成功");
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
