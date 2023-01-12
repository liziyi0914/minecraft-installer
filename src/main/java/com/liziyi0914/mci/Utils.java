package com.liziyi0914.mci;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import com.google.gson.Gson;
import com.liziyi0914.mci.bean.DownloadBuffer;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class Utils {

    private static OkHttpClient httpClient = null;

    private static final Gson gson = new Gson();

    public static OkHttpClient getClient() {
        if (Objects.isNull(httpClient)) {
            httpClient = new OkHttpClient.Builder()
                    .build();
        }
        return httpClient;
    }

    public static Gson gson() {
        return gson;
    }

    public static Optional<File> downloadSync(String url, File file) {
        OkHttpClient client = getClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try {
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (Objects.isNull(body)) {
                return Optional.empty();
            }
            InputStream in = body.byteStream();
            OutputStream out = FileUtil.getOutputStream(file);
            byte[] buffer = new byte[4 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
            body.close();
            response.close();
        } catch (IOException e) {
            return Optional.empty();
        }

        return Optional.of(file);
    }

    public static Optional<File> downloadSyncEx(String url, File file) {
        return Observable.just(0)
                .map(__ -> downloadSync(url, file))
                .timeout(120, TimeUnit.SECONDS, Schedulers.io(), new Observable<Optional<File>>() {
                    @Override
                    protected void subscribeActual(@NonNull Observer<? super Optional<File>> observer) {
                        log.warn("[{}] 下载超时", file.getName());
                        observer.onNext(Optional.empty());
                    }
                })
                .blockingFirst();
    }

    public static String mavenFileName(String name) {
        String[] parts = name.split(":");
        if (parts.length == 3) {
            return parts[1] + "-" + parts[2] + ".jar";
        } else if (parts.length == 4) {
            return parts[1] + "-" + parts[2] + "-" + parts[3] + ".jar";
        } else {
            return name;
        }
    }

    public static String mavenPath(String name) {
        String[] parts = name.split(":");
        if (parts.length >= 3) {
            return String.join(File.separator, parts[0].split("\\.")) + File.separator +
                    parts[1] + File.separator +
                    parts[2];
        } else {
            return name;
        }
    }

    public static String getOs() {
        String os;
        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isWindows()) {
            os = "windows";
        } else if (osInfo.isLinux()) {
            os = "linux";
        } else if (osInfo.isMac()) {
            os = "osx";
        } else {
            os = "UNKNOWN";
        }
        return os;
    }
}
