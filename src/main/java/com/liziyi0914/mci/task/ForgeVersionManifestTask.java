package com.liziyi0914.mci.task;

import cn.hutool.json.JSONArray;
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
import java.nio.file.Path;
import java.util.Optional;

@Data
@Slf4j
@AllArgsConstructor
public class ForgeVersionManifestTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        Mirror mirror = ctx.get(Identifiers.VAR_MIRROR);
        Path minecraftRoot = ctx.get(Identifiers.VAR_MINECRAFT_ROOT);
        String version = ctx.get(Identifiers.VAR_MINECRAFT_VERSION);
        long forgeBuild = ctx.get(Identifiers.VAR_FORGE_BUILD);
        String id = ctx.get(Identifiers.VAR_ID);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行Forge清单任务");

        log.info("目标Minecraft版本: {}", version);
        log.info("目标Forge Build: {}", forgeBuild);

        String url = "https://bmclapi2.bangbang93.com/forge/minecraft/" + version;

        log.info("Forge清单链接: {}", url);

        try {
            Optional<String> jsonOpt = Utils.downloadSync(url);

            if (!jsonOpt.isPresent()) {
                throw new IOException("下载失败");
            }

            log.info("Forge清单下载成功");

            JSONArray array = JSONUtil.parseArray(jsonOpt.get());
            Optional<JSONObject> versionOpt = array.toList(JSONObject.class)
                    .stream()
                    .filter(obj -> obj.getLong("build", 0L) == forgeBuild)
                    .findFirst();
            if (!versionOpt.isPresent()) {
                throw new IOException("Forge Version not found");
            }
            JSONObject versionObj = versionOpt.get();

            String forgeVersion = versionObj.getStr("version");
            String forgeBranch = versionObj.getStr("branch");

            log.info("目标Forge版本: {}", forgeVersion);
            log.info("目标Forge分支: {}", forgeBranch);

            ctx.put(Identifiers.VAR_FORGE_VERSION, forgeVersion);
            ctx.put(Identifiers.VAR_FORGE_BRANCH, forgeBranch);

            Optional<JSONObject> installerOpt = versionObj.getJSONArray("files")
                    .toList(JSONObject.class)
                    .stream()
                    .filter(obj -> "installer".equals(obj.getStr("category")))
                    .findFirst();
            if (!installerOpt.isPresent()) {
                throw new IOException("Forge Installer not found");
            }
            JSONObject installerObj = installerOpt.get();

            String installerUrl = mirror.forge("https://maven.minecraftforge.net/net/minecraftforge/forge/" +
                    version + "-" + forgeVersion + Optional.ofNullable(forgeBranch).map(b -> "-" + b).orElse("") +
                    "/forge-" + version + "-" + forgeVersion + Optional.ofNullable(forgeBranch).map(b -> "-" + b).orElse("") + "-installer.jar");

            log.info("Forge installer url: {}", installerUrl);

            ctx.put(
                    Identifiers.VAR_FORGE_INSTALLER_FILE,
                    FileInfo.builder()
                            .url(mirror.forge(installerUrl))
                            .hash(installerObj.getStr("hash"))
                            .file(File.createTempFile("ln-minecraft-installer", ".tmp"))
                            .build()
            );
        } catch (IOException e) {
            log.error("Forge清单任务执行失败", e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }
        log.info("Forge清单任务执行成功");
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
