package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.bean.minecraft.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Data
@Slf4j
@AllArgsConstructor
public class DumpVersionJsonTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        String id = ctx.get(Ids.VAR_ID);
        Version json = ctx.get(Ids.VAR_MINECRAFT_JSON);
        Path minecraftRoot = ctx.get(Ids.VAR_MINECRAFT_ROOT);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行版本JSON写入任务");

        try {
            IoUtil.writeUtf8(
                    FileUtil.getOutputStream(
                            FileUtil.file(
                                    minecraftRoot.toFile(),
                                    "versions",
                                    id,
                                    id+".json"
                            )
                    ),
                    true,
                    JSONUtil.toJsonStr(json)
            );
        } catch (Exception e) {
            log.error("版本JSON写入任务执行失败",e);
            subTaskInfo.update(0, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("版本JSON写入任务执行成功");
        subTaskInfo.update(0, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }
}
