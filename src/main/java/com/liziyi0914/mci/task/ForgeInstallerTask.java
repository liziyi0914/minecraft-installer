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
import java.util.Optional;

@Data
@Slf4j
@AllArgsConstructor
public class ForgeInstallerTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        FileInfo forgeInstaller = ctx.get(Identifiers.VAR_FORGE_INSTALLER_FILE);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        String url = forgeInstaller.getUrl();

        log.info("开始执行Forge Installer任务");

        log.info("Forge Installer文件链接: {}", url);

        File file = forgeInstaller.getFile();

        try {
            Optional<File> jsonOpt = Utils.downloadSync(url,file);

            if (!jsonOpt.isPresent()) {
                throw new IOException("下载失败");
            }

            log.info("Forge Installer文件写入完成");

            if (!Utils.checkHashOrExists(forgeInstaller.getHash(),file)) {
                throw new IOException("文件校验失败");
            }
        } catch (IOException e) {
            log.error("Forge Installer任务执行失败",e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("Forge Installer任务执行成功");
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
