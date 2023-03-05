package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.bean.minecraft.Version;
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
        Path minecraftRoot = ctx.get(Ids.VAR_MINECRAFT_ROOT);
        FileInfo forgeInstaller = ctx.get(Ids.VAR_FORGE_INSTALLER_FILE);
        String forgeVersion = ctx.get(Ids.VAR_FORGE_VERSION);
        Version version = ctx.get(Ids.VAR_MINECRAFT_JSON);
        String id = ctx.get(Ids.VAR_ID);
        Mirror mirror = ctx.get(Ids.VAR_MIRROR);
        boolean mix = ctx.get(Ids.VAR_MIX);

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
            Version forgeJson = installProfile.getJSONObject("versionInfo").toBean(Version.class);
            version = Utils.mixJson(version, forgeJson);
            ctx.put(Ids.VAR_MINECRAFT_JSON, version);
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
            List<FileInfo> libs = forgeJson.getLibraries()
                    .stream()
                    .filter(obj -> !maven.equals(obj.getName()))
                    .map(obj -> {
                        String name = obj.getName();
                        String url = obj.getUrl();

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
            ctx.addAll(Ids.VAR_LIBRARY_FILES, libs);
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
