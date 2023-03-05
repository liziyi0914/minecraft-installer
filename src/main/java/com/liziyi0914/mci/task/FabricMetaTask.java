package com.liziyi0914.mci.task;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.Utils;
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
public class FabricMetaTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        Mirror mirror = ctx.get(Ids.VAR_MIRROR);
        Path minecraftRoot = ctx.get(Ids.VAR_MINECRAFT_ROOT);
        String version = ctx.get(Ids.VAR_MINECRAFT_VERSION);
        String fabricVersion = ctx.get(Ids.VAR_FABRIC_VERSION);
        String id = ctx.get(Ids.VAR_ID);
        boolean isQuilt = ctx.get(Ids.VAR_QUILT);
        String fabricName = isQuilt?"Quilt":"Fabric";

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行{} Meta任务",fabricName);

        String url = isQuilt?
                mirror.quilt("https://meta.quiltmc.org/v3/versions/loader/"+version+"/" + fabricVersion):
                mirror.fabric("https://meta.fabricmc.net/v2/versions/loader/"+version+"/" + fabricVersion);

        log.info("{} Meta链接: {}",fabricName, url);

        try {
            Optional<String> jsonOpt = Utils.downloadSync(url);

            if (!jsonOpt.isPresent()) {
                throw new IOException("下载失败");
            }

            log.info("{} Meta下载成功",fabricName);

            JSONObject jsonObject = JSONUtil.parseObj(jsonOpt.get());
            ctx.put(Ids.VAR_FABRIC_META, jsonObject);
        } catch (IOException e) {
            log.error("{} Meta任务执行失败",fabricName, e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }
        log.info("{} Meta任务执行成功",fabricName);
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
