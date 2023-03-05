package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.mirror.Mirror;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@Data
@Slf4j
@AllArgsConstructor
public class MinecraftVersionManifestTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        Mirror mirror = ctx.get(Ids.VAR_MIRROR);
        Path minecraftRoot = ctx.get(Ids.VAR_MINECRAFT_ROOT);
        String version = ctx.get(Ids.VAR_MINECRAFT_VERSION);
        String id = ctx.get(Ids.VAR_ID);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行版本清单任务");

        log.info("目标版本: {}", version);

        String url = mirror.manifest("http://launchermeta.mojang.com/mc/game/version_manifest_v2.json");

        log.info("版本清单链接: {}",url);

        try {
            Optional<String> jsonOpt = Utils.downloadSync(url);

            if (!jsonOpt.isPresent()) {
                throw new IOException("下载失败");
            }

            log.info("版本清单下载成功");

            JSONObject object = JSONUtil.parseObj(jsonOpt.get());
            Optional<JSONObject> versionOpt = object.getJSONArray("versions")
                    .toList(JSONObject.class)
                    .stream()
                    .filter(obj -> version.equals(obj.getStr("id")))
                    .findFirst();
            if (!versionOpt.isPresent()) {
                throw new IOException("Version not found");
            }
            JSONObject versionObj = versionOpt.get();

            log.info("目标版本JSON: {}", versionObj.getStr("url"));

            ctx.put(
                    Ids.VAR_MINECRAFT_JSON_FILE,
                    FileInfo.builder()
                            .url(mirror.minecraftJson(versionObj.getStr("url")))
                            .hash(versionObj.getStr("sha1"))
                            .file(FileUtil.file(minecraftRoot.toFile(), "versions", id, id + ".json"))
                            .build()
            );
        } catch (IOException e) {
            log.error("版本清单任务执行失败",e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }
        log.info("版本清单任务执行成功");
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
