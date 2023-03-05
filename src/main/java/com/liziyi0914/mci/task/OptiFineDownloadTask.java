package com.liziyi0914.mci.task;

import com.liziyi0914.mci.Identifiers;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Data
@Slf4j
@AllArgsConstructor
public class OptiFineDownloadTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        String minecraftVersion = ctx.get(Identifiers.VAR_MINECRAFT_VERSION);
        String optifineType = ctx.get(Identifiers.VAR_OPTIFINE_TYPE);
        String optifinePatch = ctx.get(Identifiers.VAR_OPTIFINE_PATCH);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        String url = "https://bmclapi2.bangbang93.com/optifine/"+minecraftVersion+"/"+optifineType+"/"+optifinePatch;

        log.info("开始执行OptiFine下载任务");

        log.info("OptiFine Installer文件链接: {}", url);

        try {
            File file = Files.createTempFile("ln-minecraft-installer", ".jar").toFile();
            Optional<File> jsonOpt = Utils.downloadSync(url,file);

            if (!jsonOpt.isPresent()) {
                throw new IOException("下载失败");
            }

            ctx.put(
                    Identifiers.VAR_OPTIFINE_INSTALLER_FILE,
                    FileInfo.builder()
                            .file(file)
                            .build()
            );

            log.info("OptiFine Installer文件写入完成");
        } catch (IOException e) {
            log.error("OptiFine下载任务执行失败",e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("OptiFine下载任务执行成功");
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
