package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.liziyi0914.mci.Identifiers;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.JarFile;

@Data
@Slf4j
@AllArgsConstructor
public class ForgeNewProcessTask implements Task {

    SubTaskInfo info;

    void execProcessor(JSONObject processor, InstallContext ctx) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Path minecraftRoot = ctx.get(Identifiers.VAR_MINECRAFT_ROOT);

        try (JarClassLoader loader = new JarClassLoader()) {
            String jar = processor.getStr("jar");

            log.info("修补器: {}", jar);

            File mainFile = FileUtil.file(
                    minecraftRoot.toFile(),
                    "libraries",
                    Utils.mavenPath(jar),
                    Utils.mavenFileName(jar)
            );
            JarFile mainJarFile = new JarFile(mainFile);
            String mainClass = mainJarFile.getManifest().getMainAttributes().getValue("Main-Class");
            if (Objects.isNull(mainClass)) {
                throw new RuntimeException("找不到主类");
            }
            mainJarFile.close();
            loader.addJar(mainFile);

            processor.getJSONArray("classpath")
                    .toList(String.class)
                    .forEach(maven->{
                        File file = FileUtil.file(
                                minecraftRoot.toFile(),
                                "libraries",
                                Utils.mavenPath(maven),
                                Utils.mavenFileName(maven)
                        );
                        loader.addJar(file);

                        log.info("依赖库: {}", maven);
                    });

            String[] args = processor.getJSONArray("args")
                    .toList(String.class)
                    .stream()
                    .map(arg->{
                        String value = arg;
                        if (value.startsWith("[") && value.endsWith("]")) {
                            String tmp = value.substring(1,value.length() - 1);
                            try {
                                value = FileUtil.file(
                                        minecraftRoot.toFile(),
                                        "libraries",
                                        Utils.mavenPath(tmp),
                                        Utils.mavenFileName(tmp)
                                ).getCanonicalPath();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else if (value.startsWith("{") && value.endsWith("}")) {
                            String tmp = value.substring(1,value.length() - 1);
                            value = ctx.mapGet(Identifiers.VAR_FORGE_VARS, tmp);
                        }
                        return value;
                    }).toArray(String[]::new);

            log.info("主类: {}", mainClass);

            log.info("参数: {}", Arrays.toString(args));

            log.info("开始执行修补器 {}", jar);

            Class<?> cls = loader.loadClass(mainClass);
            Method m = cls.getMethod("main", String[].class);
            m.invoke(null, (Object) args);

            if (processor.containsKey("outputs")) {
                JSONObject outputs = processor.getJSONObject("outputs");
                for (String _key : outputs.keySet()) {
                    String value = outputs.getStr(_key);
                    if (value.startsWith("{") && value.endsWith("}")) {
                        String tmp = value.substring(1,value.length() - 1);
                        value = ctx.mapGet(Identifiers.VAR_FORGE_VARS, tmp);
                    }
                    String key = _key;
                    if (key.startsWith("{") && key.endsWith("}")) {
                        String tmp = key.substring(1,key.length() - 1);
                        key = ctx.mapGet(Identifiers.VAR_FORGE_VARS, tmp);
                    }
                    if (!Utils.checkHash(value, FileUtil.file(key))) {
                        throw new RuntimeException("修补器 " + jar + " 校验失败");
                    }
                }
            }

            log.info("修补器 {} 执行完成", jar);
        }
    }

    @Override
    public InstallResult execute(InstallContext ctx) {
        JSONArray processors = ctx.get(Identifiers.VAR_FORGE_PROCESSORS);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行新版Forge修补流程");

        int total = processors.size();
        int index = 0;
        subTaskInfo.update(0, "执行中 ("+index+"/"+total+")", SubTaskInfo.STATUS_RUNNING);
        for (JSONObject processor : processors.toList(JSONObject.class)) {
            try {
                execProcessor(processor, ctx);
            } catch (Exception e) {
                log.error("Forge修补器执行失败", e);
                subTaskInfo.update(index*65535/total, "Forge修补器执行失败", SubTaskInfo.STATUS_FAIL);
                return InstallResult.failed();
            }

            index++;
            subTaskInfo.update(index*65535/total, "执行中 ("+index+"/"+total+")", SubTaskInfo.STATUS_RUNNING);
        }

        log.info("Forge修补任务执行成功");
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
