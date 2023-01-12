package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.liziyi0914.mci.Constants;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MinecraftJsonTask implements Task {

    @Override
    public InstallResult execute(InstallContext ctx) throws IOException {
        Path minecraftRoot = ctx.get(Constants.VAR_MINECRAFT_ROOT);
        String id = ctx.get(Constants.VAR_ID);
        String version = ctx.get(Constants.VAR_MINECRAFT_VERSION);
        FileInfo jsonFile = ctx.get(Constants.VAR_MINECRAFT_JSON_FILE);
        String url = jsonFile.getUrl();
        String hash = jsonFile.getHash();

        log.info("开始执行Minecraft JSON任务");

        log.info("Minecraft JSON文件链接: {}", url);

        OkHttpClient client = Utils.getClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code());
            }

            ResponseBody body = response.body();

            if (Objects.isNull(body)) {
                throw new IOException("Unexpected code " + response.code());
            }

            log.info("Minecraft JSON文件下载完成");

            log.info("开始解析Minecraft JSON文件");

            String jsonString = body.string();
            JSONObject json = JSON.parseObject(jsonString);
            json.put("id", id);

            IoUtil.writeUtf8(
                    FileUtil.getOutputStream(
                            FileUtil.file(minecraftRoot.toFile(), "versions", id, id + ".json")
                    ),
                    true,
                    json.toString()
            );

            log.info("Minecraft JSON文件写入完成");

            {
                JSONObject obj = json.getJSONObject("downloads").getJSONObject("client");
                ctx.put(
                        Constants.VAR_MINECRAFT_JAR_FILE,
                        FileInfo.builder()
                                .url(obj.getString("url"))
                                .hash(obj.getString("sha1"))
                                .build()
                );

                log.info("Minecraft JAR文件链接: {}", obj.getString("url"));
            }

            {
                JSONObject obj = json.getJSONObject("assetIndex");
                ctx.put(
                        Constants.VAR_MINECRAFT_ASSET_INDEX_FILE,
                        FileInfo.builder()
                                .name(obj.getString("id") + ".json")
                                .url(obj.getString("url"))
                                .hash(obj.getString("sha1"))
                                .build()
                );

                log.info("Minecraft AssetIndex文件链接: {}", obj.getString("url"));
            }

            {
                JSONArray libs = json.getJSONArray("libraries");
                List<FileInfo> list = libs.stream()
                        .map(o -> (JSONObject) o)
                        .flatMap(o -> {
                            JSONObject downloads = o.getJSONObject("downloads");
                            JSONObject classifiers = downloads.getJSONObject("classifiers");
                            JSONObject artifact = downloads.getJSONObject("artifact");

                            List<FileInfo> files = new ArrayList<>();
                            files.add(
                                    FileInfo.builder()
                                            .id(o.getString("name"))
                                            .name(Utils.mavenFileName(o.getString("name")))
                                            .url(artifact.getString("url"))
                                            .hash(artifact.getString("sha1"))
                                            .build()
                            );

                            if (!Objects.isNull(classifiers)) {
                                String os = Utils.getOs();
                                Optional.ofNullable(o.getJSONObject("natives"))
                                        .map(obj->obj.getString(os))
                                        .ifPresent(os2 -> {
                                            JSONObject osObj = classifiers.getJSONObject(os2);
                                            if (!Objects.isNull(osObj)) {
                                                files.add(
                                                        FileInfo.builder()
                                                                .id(o.getString("name"))
                                                                .name(Utils.mavenFileName(o.getString("name")+":"+os2))
                                                                .url(osObj.getString("url"))
                                                                .hash(osObj.getString("sha1"))
                                                                .build()
                                                );
                                            }
                                        });
                            }

                            return files.stream();
                        })
                        .distinct()
                        .peek(fileInfo -> log.info("Minecraft Library文件: {}", fileInfo.getName()))
                        .collect(Collectors.toList());
                ctx.put(Constants.VAR_MINECRAFT_LIBRARY_FILES, list);
            }

            {
                Optional.ofNullable(json.getJSONObject("logging"))
                        .map(obj -> obj.getJSONObject("client"))
                        .map(obj -> obj.getJSONObject("file"))
                        .ifPresent(obj -> {
                            ctx.put(
                                    Constants.VAR_MINECRAFT_LOG4J_FILE,
                                    FileInfo.builder()
                                            .name(obj.getString("id"))
                                            .url(obj.getString("url"))
                                            .hash(obj.getString("sha1"))
                                            .build()
                            );

                            log.info("Minecraft Log4j文件链接: {}", obj.getString("url"));
                        });
            }
        } catch (IOException e) {
            log.error("Minecraft JSON任务执行失败",e);
            throw new RuntimeException(e);
        }
        log.info("Minecraft JSON任务执行成功");

        return InstallResult.success();
    }

}
