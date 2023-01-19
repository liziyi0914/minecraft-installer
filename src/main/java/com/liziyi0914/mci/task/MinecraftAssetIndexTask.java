package com.liziyi0914.mci.task;

import cn.hutool.core.annotation.Alias;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.liziyi0914.mci.Identifiers;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.mirror.Mirror;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Data
@Slf4j
@AllArgsConstructor
public class MinecraftAssetIndexTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        Mirror mirror = ctx.get(Identifiers.VAR_MIRROR);
        Path minecraftRoot = ctx.get(Identifiers.VAR_MINECRAFT_ROOT);
        FileInfo jsonFile = ctx.get(Identifiers.VAR_MINECRAFT_ASSET_INDEX_FILE);
        String url = jsonFile.getUrl();
        File file = jsonFile.getFile();

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "下载中", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行Minecraft AssetIndex任务");

        log.info("Minecraft AssetIndex文件链接: {}", url);

        try {
            Optional<String> jsonOpt = Utils.downloadSync(url);
            if (!jsonOpt.isPresent()) {
                throw new IOException("下载失败");
            }

            log.info("Minecraft AssetIndex文件下载完成");
            log.info("开始解析Minecraft AssetIndex文件");

            subTaskInfo.update(39321, "解析中", SubTaskInfo.STATUS_RUNNING);

            String jsonString = jsonOpt.get();
            AssetIndex assetIndex = JSONUtil.toBean(jsonString, AssetIndex.class);

            IoUtil.writeUtf8(
                    FileUtil.getOutputStream(file),
                    true,
                    jsonString
            );

            log.info("Minecraft AssetIndex文件写入完成");
            if (!Utils.checkHashOrExists(jsonFile.getHash(),file)) {
                throw new IOException("文件校验失败");
            }

            subTaskInfo.update(52428, "分析中", SubTaskInfo.STATUS_RUNNING);

            ctx.put(Identifiers.VAR_MINECRAFT_ASSET_INDEX_VIRTUAL,assetIndex.getVirtual() || assetIndex.getMapToResources());
            Function<AssetItem,File> getAssetFile;
            if (assetIndex.getVirtual() || assetIndex.getMapToResources()) {
                getAssetFile = (item) -> FileUtil.file(
                        minecraftRoot.toFile(),
                        "assets",
                        "virtual",
                        "legacy",
                        item.getName()
                );
            } else {
                getAssetFile = (item) -> FileUtil.file(
                        minecraftRoot.toFile(),
                        "assets",
                        "objects",
                        item.getHash().substring(0, 2),
                        item.getHash()
                );
            }
            List<FileInfo> assetFiles = new ArrayList<>();
            assetIndex.objects.forEach((fileName,item)->{
                log.info("{}: {}",item.getHash(),fileName);
                item.setName(fileName);
                assetFiles.add(
                        FileInfo.builder()
                                .name(fileName)
                                .hash(item.getHash())
                                .size(Long.valueOf(item.getSize()))
                                .url(mirror.minecraftAsset("https://resources.download.minecraft.net/" + item.getHash().substring(0, 2) + "/" + item.getHash()))
                                .file(getAssetFile.apply(item))
                                .build()
                );
            });
            ctx.put(Identifiers.VAR_MINECRAFT_ASSET_FILES,assetFiles);
        } catch (IOException e) {
            log.error("Minecraft AssetIndex任务执行失败",e);
            subTaskInfo.update(65535,"失败",SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("Minecraft AssetIndex任务执行成功");

        subTaskInfo.update(65535,"完成",SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetIndex {

        Boolean virtual = false;

        @Alias("map_to_resources")
        Boolean mapToResources = false;

        Map<String,AssetItem> objects;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetItem {

        String name;

        String hash;

        Integer size;

    }

}
