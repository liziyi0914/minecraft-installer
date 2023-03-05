package com.liziyi0914.mci.handler;

import cn.hutool.core.comparator.VersionComparator;
import com.liziyi0914.mci.Cmd;
import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.TaskExecutor;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.task.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class DefaultHandler implements Handler {
    @Override
    public boolean canHandle(Cmd cmd) {
        if (!cmd.getRoot().exists() || !cmd.getRoot().isDirectory()) {
            return false;
        }
        if (Objects.nonNull(cmd.getOptifine())) {
            return cmd.getOptifine().split(":").length == 2;
        }
        return true;
    }

    @Override
    public void genSubTasks(Cmd cmd, InstallContext ctx, TaskExecutor executor) {
        String version = ctx.get(Ids.VAR_MINECRAFT_VERSION);
        boolean mix = ctx.get(Ids.VAR_MIX);
        String id = ctx.get(Ids.VAR_ID);
        String minecraftId = mix ? id : cmd.getMinecraft();
        boolean modded = Objects.nonNull(cmd.getLiteloader()) || Objects.nonNull(cmd.getOptifine()) || Objects.nonNull(cmd.getForge());

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
                .then(new DumpVersionJsonTask(new SubTaskInfo("写入版本Json", 0xff)));

        executor.then(new VarTask<>(Ids.VAR_ID, id));

        if (!mix && modded) {
            executor.then(new InheritsTask(new SubTaskInfo("克隆版本文件", 0xff)));
        }

        // OptiFine
        if (Objects.nonNull(cmd.getOptifine())) {
            executor.then(new OptiFineDownloadTask(new SubTaskInfo("下载OptiFineInstaller", 0xff)))
                    .then(new OptiFineInstallTask(new SubTaskInfo("安装OptiFine", 0xff)))
                    .then(new DumpVersionJsonTask(new SubTaskInfo("写入版本Json", 0xff)));
        }

        // LiteLoader
        if (Objects.nonNull(cmd.getLiteloader())) {
            executor.then(new LiteLoaderInstallTask(new SubTaskInfo("安装LiteLoader", 0xff)))
                    .then(new DumpVersionJsonTask(new SubTaskInfo("写入版本Json", 0xff)))
                    .then(new MinecraftLibrariesTask(new SubTaskInfo("下载Libraries", 0xff)))
                    .then(new RetryTask(new SubTaskInfo("重试下载文件", 0xff)));
        }

        // Forge
        if (Objects.nonNull(cmd.getForge())) {
            executor.then(new VarTask<>(Ids.VAR_MIX, mix || Objects.nonNull(cmd.getLiteloader()) || Objects.nonNull(cmd.getOptifine())))
                    .then(new ForgeVersionManifestTask(new SubTaskInfo("下载Forge清单", 0xff)))
                    .then(new ForgeInstallerTask(new SubTaskInfo("下载ForgeInstaller", 0xff)));

            if (VersionComparator.INSTANCE.compare(version, "1.13") >= 0) {
                // 新版安装
                executor
                        // 解包
                        .then(new ForgeNewExtractTask(new SubTaskInfo("安装Forge", 0xff)))
                        .then(new DumpVersionJsonTask(new SubTaskInfo("写入版本Json", 0xff)))
                        // 补全libraries
                        .then(new MinecraftLibrariesTask(new SubTaskInfo("下载Libraries", 0xff)))
                        // 执行模块
                        .then(new ForgeNewProcessTask(new SubTaskInfo("修补Forge", 0xff)))
                        .then(new RetryTask(new SubTaskInfo("重试下载文件", 0xff)));
            } else {
                // 旧版安装
                executor
                        .then(new ForgeOldInstallTask(new SubTaskInfo("安装Forge", 0xff)))
                        .then(new DumpVersionJsonTask(new SubTaskInfo("写入版本Json", 0xff)))
                        .then(new MinecraftLibrariesTask(new SubTaskInfo("下载Libraries", 0xff)))
                        .then(new RetryTask(new SubTaskInfo("重试下载文件", 0xff)));
            }
        }
    }

    @Override
    public void handle(Cmd cmd, InstallContext ctx, TaskExecutor executor) {
        ctx.put(Ids.VAR_TASK_NAME, "安装 " + cmd.getId());
        log.info("Minecraft: {}", cmd.getMinecraft());
        if (Objects.nonNull(cmd.getForge())) {
            log.info("Forge: {}", cmd.getForge());
            ctx.put(Ids.VAR_FORGE_BUILD, Long.valueOf(cmd.getForge()));
        }
        if (Objects.nonNull(cmd.getOptifine())) {
            log.info("OptiFine: {}", cmd.getOptifine());
            String[] optfineVersionString = cmd.getOptifine().split(":");
            if (optfineVersionString.length == 2) {
                ctx.put(Ids.VAR_OPTIFINE_TYPE, optfineVersionString[0]);
                ctx.put(Ids.VAR_OPTIFINE_PATCH, optfineVersionString[1]);
            }
        }
        if (Objects.nonNull(cmd.getLiteloader())) {
            log.info("LiteLoader: {}", cmd.getLiteloader());
            ctx.put(Ids.VAR_LITE_LOADER_VERSION, cmd.getLiteloader());
        }

        genSubTasks(cmd, ctx, executor);

        executor.execute(ctx);
    }
}
