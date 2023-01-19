package com.liziyi0914.mci.handler;

import com.liziyi0914.mci.Cmd;
import com.liziyi0914.mci.Identifiers;
import com.liziyi0914.mci.TaskExecutor;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.task.*;

public class MinecraftHandler implements Handler {

    @Override
    public boolean canHandle(Cmd cmd) {
        return cmd.getRoot().exists() && cmd.getRoot().isDirectory();
    }

    @Override
    public void handle(Cmd cmd, InstallContext ctx, TaskExecutor executor) {
        ctx.put(Identifiers.VAR_TASK_NAME, "安装Minecraft " + cmd.getMinecraft());

        if (cmd.isMultiThread()) {
            executor
                    .then(new MinecraftVersionManifestTask(new SubTaskInfo("下载版本清单", 0xff)))
                    .then(new MinecraftJsonTask(new SubTaskInfo("下载Minecraft Json", 0xff)))
                    .then(new MinecraftAssetIndexTask(new SubTaskInfo("下载AssetIndex", 0xff)))
                    .thenMulti(
                            new MinecraftJarTask(new SubTaskInfo("下载Minecraft Jar", 0xff)),
                            new MinecraftLibrariesTask(new SubTaskInfo("下载Libraries", 0xff)),
                            new MinecraftAssetsTask(new SubTaskInfo("下载Assets", 0xff))
                    )
                    .then(new RetryTask(new SubTaskInfo("重试下载文件", 0xff)));
        } else {
            executor.then(new MinecraftVersionManifestTask(new SubTaskInfo("下载版本清单", 0xff)))
                    .then(new MinecraftJsonTask(new SubTaskInfo("下载Minecraft Json", 0xff)))
                    .then(new MinecraftJarTask(new SubTaskInfo("下载Minecraft Jar", 0xff)))
                    .then(new MinecraftLibrariesTask(new SubTaskInfo("下载Libraries", 0xff)))
                    .then(new MinecraftAssetIndexTask(new SubTaskInfo("下载AssetIndex", 0xff)))
                    .then(new MinecraftAssetsTask(new SubTaskInfo("下载Assets", 0xff)));
        }


        executor.execute(ctx);
    }

}
