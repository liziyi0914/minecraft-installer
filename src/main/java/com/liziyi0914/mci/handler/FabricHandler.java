package com.liziyi0914.mci.handler;

import com.liziyi0914.mci.Cmd;
import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.TaskExecutor;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.task.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class FabricHandler implements Handler {
    @Override
    public boolean canHandle(Cmd cmd) {
        if (!cmd.getRoot().exists() || !cmd.getRoot().isDirectory()) {
            return false;
        }
        if (Objects.isNull(cmd.getFabric()) && Objects.isNull(cmd.getQuilt())) {
            return false;
        }
        if (Objects.nonNull(cmd.getFabric()) && Objects.nonNull(cmd.getQuilt())) {
            return false;
        }
        if (Objects.nonNull(cmd.getForge()) || Objects.nonNull(cmd.getOptifine()) || Objects.nonNull(cmd.getLiteloader())) {
            return false;
        }
        return true;
    }

    @Override
    public void genSubTasks(Cmd cmd, InstallContext ctx, TaskExecutor executor) {
        String version = ctx.get(Ids.VAR_MINECRAFT_VERSION);
        boolean mix = ctx.get(Ids.VAR_MIX);
        String id = ctx.get(Ids.VAR_ID);
        String minecraftId = mix ? id : cmd.getMinecraft();
        boolean modded = Objects.nonNull(cmd.getFabric()) || Objects.nonNull(cmd.getQuilt());
        boolean isQuilt = ctx.get(Ids.VAR_QUILT);

        // Vanilla
        executor
                .then(new VarTask<>(Ids.VAR_ID, minecraftId))
                .then(new MinecraftVersionManifestTask(new SubTaskInfo("下载版本清单", 0xff)))
                .then(new MinecraftJsonTask(new SubTaskInfo("下载Minecraft Json", 0xff)))
                .then(new MinecraftAssetIndexTask(new SubTaskInfo("下载AssetIndex", 0xff)))
                .thenMulti(
                        new MinecraftJarTask(new SubTaskInfo("下载Minecraft Jar", 0xff)),
                        new MinecraftAssetsTask(new SubTaskInfo("下载Assets", 0xff)),
                        new MinecraftLibrariesTask(new SubTaskInfo("下载Libraries", 0xff))
                )
                .then(new DumpVersionJsonTask(new SubTaskInfo("写入版本Json", 0xff)))
                .then(new RetryTask(new SubTaskInfo("重试下载文件", 0xff)));

        executor.then(new VarTask<>(Ids.VAR_ID, id));

        if (!mix && modded) {
            executor.then(new InheritsTask(new SubTaskInfo("克隆版本文件", 0xff)));
        }

        // Fabric & Quilt
        if (Objects.nonNull(cmd.getFabric()) || Objects.nonNull(cmd.getQuilt())) {
            String name = isQuilt?"Quilt":"Fabric";
            executor.then(new FabricMetaTask(new SubTaskInfo("下载"+name+" Meta", 0xff)))
                    .then(new FabricInstallTask(new SubTaskInfo("安装"+name, 0xff)))
                    .then(new DumpVersionJsonTask(new SubTaskInfo("写入版本Json", 0xff)))
                    .then(new MinecraftLibrariesTask(new SubTaskInfo("下载Libraries", 0xff)))
                    .then(new RetryTask(new SubTaskInfo("重试下载文件", 0xff)));
        }
    }

    @Override
    public void handle(Cmd cmd, InstallContext ctx, TaskExecutor executor) {
        ctx.put(Ids.VAR_TASK_NAME, "安装 " + cmd.getId());
        log.info("Minecraft: {}", cmd.getMinecraft());
        if (Objects.nonNull(cmd.getFabric())) {
            log.info("Fabric: {}", cmd.getFabric());
            ctx.put(Ids.VAR_QUILT, false);
            ctx.put(Ids.VAR_FABRIC_VERSION, cmd.getFabric());
        }
        if (Objects.nonNull(cmd.getQuilt())) {
            log.info("Quilt: {}", cmd.getQuilt());
            ctx.put(Ids.VAR_QUILT, true);
            ctx.put(Ids.VAR_FABRIC_VERSION, cmd.getQuilt());
        }

        genSubTasks(cmd, ctx, executor);

        executor.execute(ctx);
    }
}
