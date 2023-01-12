package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson2.JSON;
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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class MinecraftJarTask implements Task {

    @Override
    public InstallResult execute(InstallContext ctx) throws IOException {
        Path minecraftRoot = ctx.get(Constants.VAR_MINECRAFT_ROOT);
        String id = ctx.get(Constants.VAR_ID);
        String version = ctx.get(Constants.VAR_MINECRAFT_VERSION);
        FileInfo jarFile = ctx.get(Constants.VAR_MINECRAFT_JAR_FILE);
        String url = jarFile.getUrl();
        String hash = jarFile.getHash();

        log.info("开始执行Minecraft JAR任务");

        log.info("Minecraft JAR文件链接: {}", url);

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

            InputStream in = body.byteStream();

            IoUtil.copy(
                    in,
                    FileUtil.getOutputStream(
                            FileUtil.file(minecraftRoot.toFile(), "versions", id, id + ".jar")
                    )
            );

            in.close();

            log.info("Minecraft JAR文件写入完成");
        } catch (IOException e) {
            log.error("Minecraft JAR任务执行失败",e);
            throw new RuntimeException(e);
        }

        log.info("Minecraft JAR任务执行成功");

        return InstallResult.success();
    }

}
