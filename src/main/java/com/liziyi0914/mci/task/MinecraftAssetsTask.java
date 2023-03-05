package com.liziyi0914.mci.task;

import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Data
@Slf4j
@AllArgsConstructor
public class MinecraftAssetsTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        List<FileInfo> assetFiles = ctx.get(Ids.VAR_MINECRAFT_ASSET_FILES);
        boolean override = ctx.get(Ids.VAR_OVERRIDE_EXISTS);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行Minecraft Assets任务");

        int total = assetFiles.size();

        log.info("共有{}个资源文件需要下载", total);

        try {
            AtomicInteger _current = new AtomicInteger();
            double per = 1.0d / ((double)total) * 65535.0;
            List<FileInfo> fails = Flowable.fromIterable(assetFiles)
                    .flatMap((Function<FileInfo, Publisher<Optional<FileInfo>>>) info -> {
                        int current = _current.getAndIncrement();
                        subTaskInfo.update((int)(current * per), "下载 "+info.getName(), SubTaskInfo.STATUS_RUNNING);

                        File file = info.getFile();

                        String hash = info.getHash();
                        if (Utils.checkHash(hash,file) && !override) {
                            log.info("[{}] 已存在", file.getName());
                            return Flowable.just(Optional.empty());
                        }

                        return Flowable.just(file)
                                .observeOn(Schedulers.io())
                                .map(f -> {
                                    log.info("[{}] 开始下载", f.getName());
                                    String url = info.getUrl();
                                    Optional<File> fileOpt = Utils.downloadSyncEx(url, f);
                                    if (fileOpt.isPresent()) {
                                        log.info("[{}] 下载完成", info.getHash());
                                    } else {
                                        log.error("[{}] 下载失败", info.getHash());
                                        for (int i = 0; i < 3; i++) {
                                            log.info("[{}] 第{}次重试", info.getHash(), i+1);
                                            fileOpt = Utils.downloadSyncEx(url, file);
                                            if (fileOpt.isPresent()) {
                                                log.info("[{}] 下载完成", info.getHash());
                                                break;
                                            } else {
                                                log.error("[{}] 下载失败", info.getHash());
                                            }
                                        }
                                    }

                                    if (fileOpt.isPresent() && Utils.checkHashOrExists(hash, fileOpt.get())) {
                                        return Optional.empty();
                                    } else {
                                        return Optional.of(info);
                                    }
                                });
                    },16)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList())
                    .blockingGet();

            log.info("下载完成，共有{}个资源文件下载失败", fails.size());

            ctx.addAll(Ids.VAR_FILES_FAILED,fails);
        } catch (RuntimeException e) {
            log.error("Minecraft Assets任务执行失败",e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("Minecraft Assets任务执行成功");

        subTaskInfo.update(65535, "完成", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
