package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.liziyi0914.mci.Constants;
import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.bean.minecraft.Library;
import com.liziyi0914.mci.bean.minecraft.Version;
import com.liziyi0914.mci.mirror.Mirror;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Slf4j
@AllArgsConstructor
public class LiteLoaderInstallTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        String minecraftVersion = ctx.get(Ids.VAR_MINECRAFT_VERSION);
        String liteLoaderVersion = ctx.get(Ids.VAR_LITE_LOADER_VERSION);
        Mirror mirror = ctx.get(Ids.VAR_MIRROR);
        Path minecraftRoot = ctx.get(Ids.VAR_MINECRAFT_ROOT);
        Version version = ctx.get(Ids.VAR_MINECRAFT_JSON);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        String url = "https://bmclapi2.bangbang93.com/liteloader/list";

        log.info("开始执行LiteLoader安装任务");

        log.info("LiteLoader版本清单: {}", url);

        try {
            Optional<String> jsonOpt = Utils.downloadSync(url);

            if (!jsonOpt.isPresent()) {
                throw new IOException("下载失败");
            }

            JSONArray array = JSONUtil.parseArray(jsonOpt.get());

            Optional<JSONObject> objOpt = array.toList(JSONObject.class)
                    .stream()
                    .filter(_obj->minecraftVersion.equals(_obj.getStr("mcversion"))&&liteLoaderVersion.equals(_obj.getStr("version")))
                    .findFirst();
            if (!objOpt.isPresent()) {
                throw new IOException("找不到该版本的LiteLoader");
            }
            JSONObject obj = objOpt.get();

            if ("RELEASE".equals(obj.getStr("type"))) {
                url = "http://dl.liteloader.com/versions/com/mumfrey/liteloader/"+minecraftVersion+"/liteloader-"+liteLoaderVersion+".jar";
            } else {// SNAPSHOT
                url = "http://dl.liteloader.com/versions/com/mumfrey/liteloader/"+liteLoaderVersion+"/liteloader-"+liteLoaderVersion+".jar";
            }

            log.info("LiteLoader链接: {}", url);

            String maven = "com.mumfrey:liteloader:"+liteLoaderVersion;
            File file = FileUtil.file(
                    minecraftRoot.toFile(),
                    "libraries",
                    Utils.mavenPath(maven),
                    Utils.mavenFileName(maven)
            );

            log.info("开始下载LiteLoader");

            Optional<File> liteLoaderOpt = Utils.downloadSync(url,file);

            if (!liteLoaderOpt.isPresent()) {
                throw new IOException("下载失败");
            }

            log.info("LiteLoader文件写入完成");

            if (!Utils.checkHashOrExists(obj.getStr("hash"),file)) {
                throw new IOException("文件校验失败");
            }

            version.getLibraries().add(
                    Library.builder()
                            .name(maven)
                            .build()
            );

            JSONArray libArray = Optional.ofNullable(obj.getJSONObject("build"))
                    .map(o->o.getJSONArray("libraries"))
                    .orElse(null);
            if (Objects.nonNull(libArray)) {
                version.getLibraries().addAll(
                        libArray
                                .toList(JSONObject.class)
                                .stream()
                                .map(o->o.getStr("name"))
                                .filter(Objects::nonNull)
                                .map(name->Library.builder().name(name).build())
                                .collect(Collectors.toList())
                );
            }

            version.addTweakClass("com.mumfrey.liteloader.launch.LiteLoaderTweaker");

            version.setMainClass(Constants.LAUNCH_WRAPPER_MAIN);
        } catch (IOException e) {
            log.error("LiteLoader安装任务执行失败",e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("LiteLoader安装任务执行成功");
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
