package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.liziyi0914.mci.Constants;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableAll;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.parallel.ParallelTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class MinecraftLibrariesTask implements Task {

    @Override
    public InstallResult execute(InstallContext ctx) throws IOException {
        Path minecraftRoot = ctx.get(Constants.VAR_MINECRAFT_ROOT);
        String id = ctx.get(Constants.VAR_ID);
        String version = ctx.get(Constants.VAR_MINECRAFT_VERSION);
        List<FileInfo> libraryFiles = ctx.get(Constants.VAR_MINECRAFT_LIBRARY_FILES);

        log.info("开始执行Minecraft Libraries任务");

        log.info("共有{}个库文件需要下载", libraryFiles.size());

        try {
            List<FileInfo> fails = Flowable.fromIterable(libraryFiles)
                    .flatMap((Function<FileInfo, Publisher<Optional<FileInfo>>>) info -> {
                        File file = FileUtil.file(
                                minecraftRoot.toFile(),
                                "libraries",
                                Utils.mavenPath(info.getId()),
                                info.getName()
                        );
                        return Flowable.just(file)
                                .observeOn(Schedulers.io())
                                .map(f -> {
                                    log.info("[{}] 开始下载", f.getName());
                                    Optional<File> fileOpt = Utils.downloadSyncEx(info.getUrl(), f);
                                    if (fileOpt.isPresent()) {
                                        log.info("[{}] 下载完成", fileOpt.get().getName());
                                    } else {
                                        log.error("[{}] 下载失败", info.getName());
                                        for (int i = 0; i < 3; i++) {
                                            log.info("[{}] 第{}次重试", info.getName(), i+1);
                                            fileOpt = Utils.downloadSyncEx(info.getUrl(), file);
                                            if (fileOpt.isPresent()) {
                                                log.info("[{}] 下载完成", fileOpt.get().getName());
                                                break;
                                            } else {
                                                log.error("[{}] 下载失败", info.getName());
                                            }
                                        }
                                    }

                                    if (fileOpt.isPresent()) {
                                        return Optional.empty();
                                    } else {
                                        return Optional.of(info);
                                    }
                                });
                    },3)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList())
                    .blockingGet();

            log.info("下载完成，共有{}个库文件下载失败", fails.size());

            ctx.put(Constants.VAR_MINECRAFT_LIBRARY_FILES_FAILED,fails);
        } catch (RuntimeException e) {
            log.error("Minecraft Libraries任务执行失败",e);
            throw e;
        }

        log.info("Minecraft Libraries任务执行成功");

        return InstallResult.success();
    }

}
