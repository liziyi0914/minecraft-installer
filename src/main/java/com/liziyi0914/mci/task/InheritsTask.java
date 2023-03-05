package com.liziyi0914.mci.task;

import com.liziyi0914.mci.Identifiers;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.bean.minecraft.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.*;

@Data
@Slf4j
@AllArgsConstructor
public class InheritsTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        FileInfo jarFile = ctx.get(Identifiers.VAR_MINECRAFT_JAR_FILE);
        Version json = ctx.get(Identifiers.VAR_MINECRAFT_JSON);
        String id = ctx.get(Identifiers.VAR_ID);
        String minecraftId = ctx.get(Identifiers.VAR_MINECRAFT_VERSION);
        Path minecraftRoot = ctx.get(Identifiers.VAR_MINECRAFT_ROOT);
        Version version = ctx.get(Identifiers.VAR_MINECRAFT_JSON);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行版本继承任务");

        Version newVersion = new Version();

        try {
            newVersion.setId(id);
            newVersion.setInheritsFrom(minecraftId);
            newVersion.setJar(minecraftId);

            if (Objects.nonNull(version.getArguments())) {
                Map<String, List<Object>> args = new HashMap<>();
                args.put("game", new ArrayList<>());
                args.put("jvm", new ArrayList<>());
                newVersion.setArguments(args);
            } else {
                newVersion.setMinecraftArguments(version.getMinecraftArguments());
            }

            newVersion.setLibraries(new ArrayList<>());

            ctx.put(Identifiers.VAR_MINECRAFT_JSON,newVersion);
        } catch (Exception e) {
            log.error("版本继承任务执行失败",e);
            subTaskInfo.update(0, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("版本继承任务执行成功");
        subTaskInfo.update(0, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }
}
