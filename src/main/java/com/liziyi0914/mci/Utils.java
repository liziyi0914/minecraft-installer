package com.liziyi0914.mci;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import com.liziyi0914.mci.bean.minecraft.Library;
import com.liziyi0914.mci.bean.minecraft.Version;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class Utils {

    private static OkHttpClient httpClient = null;

    private static final Pattern tweakPattern = Pattern.compile("--tweakClass ((\\w+\\.?)*\\w+)");

    public static OkHttpClient getClient() {
        if (Objects.isNull(httpClient)) {
            httpClient = new OkHttpClient.Builder()
//                    .proxy(new Proxy(Proxy.Type.SOCKS, new java.net.InetSocketAddress("127.0.0.1",7890)))
                    .build();
        }
        return httpClient;
    }

    public static boolean checkHash(String hash, File file) {
        if (Objects.isNull(hash) || !file.exists()) {
            return false;
        }
        if (hash.length() == 32) {
            return hash.equalsIgnoreCase(DigestUtil.md5Hex(file));
        } else if (hash.length() == 40) {
            return hash.equalsIgnoreCase(DigestUtil.sha1Hex(file));
        } else if (hash.length() == 64) {
            return hash.equalsIgnoreCase(DigestUtil.sha256Hex(file));
        } else {
            return false;
        }
    }

    public static boolean checkHashOrExists(String hash, File file) {
        if (Objects.isNull(hash)) {
            return file.exists();
        }
        if (hash.length() == 32) {
            return hash.equalsIgnoreCase(DigestUtil.md5Hex(file));
        } else if (hash.length() == 40) {
            return hash.equalsIgnoreCase(DigestUtil.sha1Hex(file));
        } else if (hash.length() == 64) {
            return hash.equalsIgnoreCase(DigestUtil.sha256Hex(file));
        } else {
            return false;
        }
    }

    public static boolean checkBufferHash(String hash, byte[] buffer) {
        if (Objects.isNull(hash)) {
            return true;
        }
        if (hash.length() == 32) {
            return hash.equalsIgnoreCase(DigestUtil.md5Hex(buffer));
        } else if (hash.length() == 40) {
            return hash.equalsIgnoreCase(DigestUtil.sha1Hex(buffer));
        } else if (hash.length() == 64) {
            return hash.equalsIgnoreCase(DigestUtil.sha256Hex(buffer));
        } else {
            return false;
        }
    }

    public static void downloadSync(String url, DownloadCallback callback) throws Exception {
        OkHttpClient client = getClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        ResponseBody body = response.body();
        if (Objects.isNull(body)) {
            throw new IOException();
        }
        InputStream in = body.byteStream();

        callback.handle(in);

        in.close();
        body.close();
        response.close();
    }

    public static Optional<File> downloadSync(String url, File file) {
//        OkHttpClient client = getClient();
//        Request request = new Request.Builder()
//                .url(url)
//                .get()
//                .build();
//        try {
//            Response response = client.newCall(request).execute();
//            ResponseBody body = response.body();
//            if (Objects.isNull(body)) {
//                return Optional.empty();
//            }
//            InputStream in = body.byteStream();
//            OutputStream out = FileUtil.getOutputStream(file);
//            byte[] buffer = new byte[4 * 1024];
//            int len;
//            while ((len = in.read(buffer)) != -1) {
//                out.write(buffer, 0, len);
//            }
//            in.close();
//            out.close();
//            body.close();
//            response.close();
//        } catch (IOException e) {
//            return Optional.empty();
//        }
//
//        return Optional.of(file);

        try {
            downloadSync(url, in -> {
                OutputStream out = FileUtil.getOutputStream(file);
                byte[] buffer = new byte[4 * 1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.close();
            });
        } catch (Exception e) {
            return Optional.empty();
        }

        return Optional.of(file);
    }

    public static Optional<String> downloadSync(String url) {
        try {
            AtomicReference<String> s = new AtomicReference<>("");
            downloadSync(url, in -> {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4 * 1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                s.set(out.toString("UTF-8"));
                out.close();
            });
            return Optional.of(s.get());
        } catch (Exception e) {
            return Optional.empty();
        }
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

    public static Optional<String> downloadSyncEx(String url) {
        return Observable.just(0)
                .map(__ -> downloadSync(url))
                .timeout(120, TimeUnit.SECONDS, Schedulers.io(), new Observable<Optional<String>>() {
                    @Override
                    protected void subscribeActual(@NonNull Observer<? super Optional<String>> observer) {
                        log.warn("下载超时 [{}]", url);
                        observer.onNext(Optional.empty());
                    }
                })
                .blockingFirst();
    }

    public static String mavenFileName(String name) {
        String[] suffixes = name.split("@");
        String suffix = ".jar";
        if (suffixes.length > 1) {
            suffix = "." + suffixes[1];
        }
        String[] parts = suffixes[0].split(":");
        if (parts.length == 3) {
            return parts[1] + "-" + parts[2] + suffix;
        } else if (parts.length == 4) {
            return parts[1] + "-" + parts[2] + "-" + parts[3] + suffix;
        } else {
            return name;
        }
    }

    public static String mavenPath(String name) {
        String[] parts = name.split("@")[0].split(":");
        if (parts.length >= 3) {
            return String.join(
                    File.separator,
                    parts[0].split("\\.")
            ) + File.separator + parts[1] + File.separator + parts[2];
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

    public static JSONObject mixJson(JSONObject baseJson, JSONObject newJson) {
        Optional.ofNullable(newJson.getStr("minecraftArguments"))
                .ifPresent(value ->
                        baseJson.set(
                                "minecraftArguments",
                                value
                        )
                );
        Optional.ofNullable(newJson.getStr("mainClass"))
                .ifPresent(value -> baseJson.set("mainClass", value));
        Optional.ofNullable(newJson.getJSONObject("arguments"))
                .ifPresent(arguments -> {
                    Optional.ofNullable(arguments.getJSONArray("jvm"))
                            .ifPresent(jvm -> {
                                JSONObject baseArgs = Optional.ofNullable(baseJson.getJSONObject("arguments")).orElse(new JSONObject());
                                JSONArray jvmArray = Optional.ofNullable(baseArgs.getJSONArray("jvm")).orElse(new JSONArray());
                                jvmArray.addAll(jvm);
                                baseArgs.set("jvm", jvmArray);
                            });
                    Optional.ofNullable(arguments.getJSONArray("game"))
                            .ifPresent(game -> {
                                JSONObject baseArgs = Optional.ofNullable(baseJson.getJSONObject("arguments")).orElse(new JSONObject());
                                JSONArray gameArray = Optional.ofNullable(baseArgs.getJSONArray("game")).orElse(new JSONArray());
                                gameArray.addAll(game);
                                baseArgs.set("game", gameArray);
                            });
                });
        Optional.ofNullable(newJson.getJSONArray("libraries"))
                .ifPresent(libraries -> {
                    JSONArray baseLibraries = Optional.ofNullable(baseJson.getJSONArray("libraries")).orElse(new JSONArray());
                    Set<String> names = baseLibraries.toList(JSONObject.class)
                            .stream()
                            .map(lib -> lib.getStr("name"))
                            .collect(Collectors.toSet());
                    libraries.toList(JSONObject.class)
                            .forEach(lib -> {
                                if (!names.contains(lib.getStr("name"))) {
                                    baseLibraries.add(lib);
                                }
                            });
                    baseJson.set("libraries", baseLibraries);
                });
        return baseJson;
    }

    public static Version mixJson(Version baseJson, Version newJson) {
        Optional.ofNullable(newJson.getMinecraftArguments())
                .ifPresent(newArgs -> {
                    String oldArgs = baseJson.getMinecraftArguments();
                    Matcher m = tweakPattern.matcher(oldArgs);
                    StringBuilder args = new StringBuilder(newArgs);
                    while (m.find()) {
                        args.append(" --tweakClass ").append(m.group(1));
                    }
                    baseJson.setMinecraftArguments(args.toString().trim());
                });
        Optional.ofNullable(newJson.getMainClass())
                .ifPresent(baseJson::setMainClass);
        Optional.ofNullable(newJson.getArguments())
                .ifPresent(arguments -> {
                    Optional.ofNullable(arguments.get("jvm"))
                            .ifPresent(jvm -> {
                                Map<String, List<Object>> baseArguments = baseJson.getArguments();
                                if (Objects.isNull(baseArguments)) {
                                    baseArguments = new HashMap<>();
                                    baseJson.setArguments(baseArguments);
                                }
                                List<Object> jvmList = baseArguments.get("jvm");
                                if (Objects.isNull(jvmList)) {
                                    jvmList = new ArrayList<>();
                                    baseArguments.put("jvm", jvmList);
                                }
                                jvmList.addAll(jvm);
                            });
                    Optional.ofNullable(arguments.get("game"))
                            .ifPresent(game -> {
                                Map<String, List<Object>> baseArguments = baseJson.getArguments();
                                if (Objects.isNull(baseArguments)) {
                                    baseArguments = new HashMap<>();
                                    baseJson.setArguments(baseArguments);
                                }
                                List<Object> jvmList = baseArguments.get("game");
                                if (Objects.isNull(jvmList)) {
                                    jvmList = new ArrayList<>();
                                    baseArguments.put("game", jvmList);
                                }
                                jvmList.addAll(game);
                            });
                });
        Optional.ofNullable(newJson.getLibraries())
                .ifPresent(libraries -> {
                    List<Library> libs;
                    if (Objects.isNull(baseJson.getLibraries())) {
                        libs = new ArrayList<>();
                        baseJson.setLibraries(libs);
                    } else {
                        libs = baseJson.getLibraries();
                    }
                    Set<String> names = libs.stream()
                            .map(Library::getName)
                            .collect(Collectors.toSet());
                    libraries.forEach(lib -> {
                        if (!names.contains(lib.getName())) {
                            libs.add(lib);
                        }
                    });
//                    JSONArray baseLibraries = Optional.ofNullable(baseJson.getJSONArray("libraries")).orElse(new JSONArray());
//                    Set<String> names = baseLibraries.toList(JSONObject.class)
//                            .stream()
//                            .map(lib -> lib.getStr("name"))
//                            .collect(Collectors.toSet());
//                    libraries.toList(JSONObject.class)
//                            .forEach(lib->{
//                                if (!names.contains(lib.getStr("name"))) {
//                                    baseLibraries.add(lib);
//                                }
//                            });
//                    baseJson.set("libraries", baseLibraries);
                });
        return baseJson;
    }

    public interface DownloadCallback {
        void handle(InputStream in) throws Exception;
    }
}
