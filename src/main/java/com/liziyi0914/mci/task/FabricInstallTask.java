package com.liziyi0914.mci.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.FileInfo;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.bean.minecraft.Library;
import com.liziyi0914.mci.bean.minecraft.Version;
import com.liziyi0914.mci.mirror.Mirror;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Slf4j
@AllArgsConstructor
public class FabricInstallTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        Path minecraftRoot = ctx.get(Ids.VAR_MINECRAFT_ROOT);
        String id = ctx.get(Ids.VAR_ID);
        Mirror mirror = ctx.get(Ids.VAR_MIRROR);
        boolean mix = ctx.get(Ids.VAR_MIX);
        Version version = ctx.get(Ids.VAR_MINECRAFT_JSON);
        String minecraftVersion = ctx.get(Ids.VAR_MINECRAFT_VERSION);
        FileInfo jarFile = ctx.get(Ids.VAR_MINECRAFT_JAR_FILE);
        JSONObject meta = ctx.get(Ids.VAR_FABRIC_META);
        boolean isQuilt = ctx.get(Ids.VAR_QUILT);
        String fabricName = isQuilt?"Quilt":"Fabric";

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行{}安装流程",fabricName);

        JSONObject launcherMeta = meta.getJSONObject("launcherMeta");

        String mainClass;
        Object mainClassObj = launcherMeta.getObj("mainClass");
        if (mainClassObj instanceof String) {
            mainClass = (String) mainClassObj;
        } else {
            JSONObject mainClassJson = (JSONObject) mainClassObj;
            mainClass = mainClassJson.getStr("client");
        }
        version.setMainClass(mainClass);

        if (launcherMeta.containsKey("launchwrapper")) {
            String tweak = Optional.ofNullable(launcherMeta.getJSONObject("launchwrapper"))
                    .map(opt->opt.getJSONObject("tweakers"))
                    .map(opt->opt.getJSONArray("client"))
                    .map(opt->opt.get(0))
                    .map(Object::toString)
                    .orElse(null);
            version.addTweakClass(tweak);
        }

        JSONObject launcherMetaLibraries = launcherMeta.getJSONObject("libraries");

        List<Library> libraries = new ArrayList<>();
        libraries.addAll(
                launcherMetaLibraries.getJSONArray("common").toList(Library.class)
        );
        libraries.addAll(
                launcherMetaLibraries.getJSONArray("server").toList(Library.class)
        );

        libraries.add(
                Library.builder()
                        .name(meta.getJSONObject("loader").getStr("maven"))
                        .build()
        );
        libraries.add(
                Library.builder()
                        .name(meta.getJSONObject("intermediary").getStr("maven"))
                        .build()
        );

        version.getLibraries().addAll(libraries);
        ctx.addAll(
                Ids.VAR_LIBRARY_FILES,
                libraries.stream().map(library ->
                        FileInfo.builder()
                                .id(library.getName())
                                .name(Utils.mavenFileName(library.getName()))
                                .url(
                                        library.getName().contains("org.quiltmc:")?
                                                mirror.quilt("https://maven.quiltmc.org/repository/release/"+Utils.mavenPath(library.getName())+"/"+Utils.mavenFileName(library.getName())):
                                                mirror.fabric("https://maven.fabricmc.net/"+Utils.mavenPath(library.getName())+"/"+Utils.mavenFileName(library.getName()))
                                )
                                .file(FileUtil.file(
                                        minecraftRoot.toFile(),
                                        "libraries",
                                        Utils.mavenPath(library.getName()),
                                        Utils.mavenFileName(library.getName())
                                ))
                                .build()
                ).collect(Collectors.toList())
        );

        log.info("{}安装任务执行成功",fabricName);
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
