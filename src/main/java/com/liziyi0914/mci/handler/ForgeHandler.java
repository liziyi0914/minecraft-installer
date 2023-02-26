package com.liziyi0914.mci.handler;

import cn.hutool.core.comparator.VersionComparator;
import com.liziyi0914.mci.Cmd;
import com.liziyi0914.mci.Identifiers;
import com.liziyi0914.mci.TaskExecutor;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.task.*;

import java.util.Arrays;
import java.util.Objects;

public class ForgeHandler implements Handler {
    @Override
    public boolean canHandle(Cmd cmd) {
        return cmd.getRoot().exists() &&
                cmd.getRoot().isDirectory() &&
                Objects.nonNull(cmd.getForge());
    }

    @Override
    public void genSubTasks(Cmd cmd, InstallContext ctx, TaskExecutor executor) {
        String version = ctx.get(Identifiers.VAR_MINECRAFT_VERSION);

        executor
                .thenShadow(
                        ctx,
                        Arrays.asList(Identifiers.VAR_ID),
                        context -> {
                            context.put(Identifiers.VAR_ID, cmd.getMinecraft());
                        },
                        exec -> {
                            exec
                                    .then(new MinecraftVersionManifestTask(new SubTaskInfo("下载版本清单", 0xff)))
                                    .then(new MinecraftJsonTask(new SubTaskInfo("下载Minecraft Json", 0xff)))
                                    .then(new MinecraftAssetIndexTask(new SubTaskInfo("下载AssetIndex", 0xff)))
                                    .thenMulti(
                                            new MinecraftJarTask(new SubTaskInfo("下载Minecraft Jar", 0xff)),
                                            new MinecraftAssetsTask(new SubTaskInfo("下载Assets", 0xff))
                                    );
                        }
                )
                .then(new ForgeVersionManifestTask(new SubTaskInfo("下载Forge清单", 0xff)))
                .then(new ForgeInstallerTask(new SubTaskInfo("下载ForgeInstaller", 0xff)));

        if (VersionComparator.INSTANCE.compare(version, "1.13") >= 0) {
            // 新版安装
            executor
                    // 解包
                    .then(new ForgeNewExtractTask(new SubTaskInfo("安装Forge", 0xff)))
                    // 补全libraries
                    .then(new MinecraftLibrariesTask(new SubTaskInfo("下载Libraries", 0xff)))
                    // 执行模块
                    .then(new ForgeNewProcessTask(new SubTaskInfo("修补Forge", 0xff)))
                    .then(new RetryTask(new SubTaskInfo("重试下载文件", 0xff)));
        } else {
            // 旧版安装
            executor
                    .then(new ForgeOldInstallTask(new SubTaskInfo("安装Forge", 0xff)))
                    .then(new MinecraftLibrariesTask(new SubTaskInfo("下载Libraries", 0xff)))
                    .then(new RetryTask(new SubTaskInfo("重试下载文件", 0xff)));
        }
    }

    @Override
    public void handle(Cmd cmd, InstallContext ctx, TaskExecutor executor) {
        ctx.put(Identifiers.VAR_TASK_NAME, "安装Forge " + cmd.getMinecraft());
        ctx.put(Identifiers.VAR_FORGE_BUILD, Long.valueOf(cmd.getForge()));

        genSubTasks(cmd, ctx, executor);

        executor.execute(ctx);
    }
}
