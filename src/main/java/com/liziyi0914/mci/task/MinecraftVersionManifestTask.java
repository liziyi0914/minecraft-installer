package com.liziyi0914.mci.task;

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
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class MinecraftVersionManifestTask implements Task {

    @Override
    public InstallResult execute(InstallContext ctx) {
        String version = ctx.get(Constants.VAR_MINECRAFT_VERSION);

        log.info("开始执行版本清单任务");

        log.info("目标版本: {}", version);

        String url = "https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json";

        log.info("版本清单链接: {}",url);

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

            log.info("版本清单下载成功");

            JSONObject object = JSON.parseObject(body.string());
            Optional<JSONObject> versionOpt = object.getJSONArray("versions")
                    .stream()
                    .map(obj -> (JSONObject) obj)
                    .filter(obj -> version.equals(obj.getString("id")))
                    .findFirst();
            if (!versionOpt.isPresent()) {
                throw new IOException("Version not found");
            }
            JSONObject versionObj = versionOpt.get();

            log.info("目标版本JSON: {}", versionObj.getString("url"));

            ctx.put(
                    Constants.VAR_MINECRAFT_JSON_FILE,
                    FileInfo.builder()
                            .url(versionObj.getString("url"))
                            .hash(versionObj.getString("sha1"))
                            .build()
            );
        } catch (IOException e) {
            log.error("版本清单任务执行失败",e);
            throw new RuntimeException(e);
        }
        log.info("版本清单任务执行成功");

        return InstallResult.success();
    }

}
