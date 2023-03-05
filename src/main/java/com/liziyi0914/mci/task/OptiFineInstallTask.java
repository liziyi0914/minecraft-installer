package com.liziyi0914.mci.task;

import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.JarClassLoader;
import com.liziyi0914.mci.Constants;
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

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Data
@Slf4j
@AllArgsConstructor
public class OptiFineInstallTask implements Task {

    SubTaskInfo info;

    @Override
    public InstallResult execute(InstallContext ctx) {
        Path minecraftRoot = ctx.get(Ids.VAR_MINECRAFT_ROOT);
        FileInfo optiFineInstaller = ctx.get(Ids.VAR_OPTIFINE_INSTALLER_FILE);
        String id = ctx.get(Ids.VAR_ID);
        Mirror mirror = ctx.get(Ids.VAR_MIRROR);
        boolean mix = ctx.get(Ids.VAR_MIX);
        Version version = ctx.get(Ids.VAR_MINECRAFT_JSON);
        String minecraftVersion = ctx.get(Ids.VAR_MINECRAFT_VERSION);
        String optifineType = ctx.get(Ids.VAR_OPTIFINE_TYPE);
        String optifinePatch = ctx.get(Ids.VAR_OPTIFINE_PATCH);
        String optifineVersion = minecraftVersion + "_" + optifineType + "_" + optifinePatch;
        FileInfo jarFile = ctx.get(Ids.VAR_MINECRAFT_JAR_FILE);

        SubTaskInfo subTaskInfo = getInfo();
        subTaskInfo.update(0, "开始执行", SubTaskInfo.STATUS_RUNNING);

        log.info("开始执行OptiFine安装流程");

        File installerFile = optiFineInstaller.getFile();
        String optifineMaven = "optifine:OptiFine:" + optifineVersion;
        File file = FileUtil.file(
                minecraftRoot.toFile(),
                "libraries",
                Utils.mavenPath(optifineMaven),
                Utils.mavenFileName(optifineMaven)
        );

        {
            log.info("开始复制OptiFine安装器到库文件目录");
            String maven = "optifine:OptiFine:" + optifineVersion + ":installer";
            FileUtil.copyFile(
                    installerFile,
                    FileUtil.file(minecraftRoot.toFile(), "libraries", Utils.mavenPath(maven), Utils.mavenFileName(maven)),
                    StandardCopyOption.REPLACE_EXISTING
            );
            log.info("复制OptiFine安装器到库文件目录完成");
        }

        version.getLibraries().add(
                Library.builder()
                        .name(optifineMaven)
                        .build()
        );

        boolean hasLauncherWrapper = false;
        try (ZipFile zipFile = new ZipFile(installerFile)) {
            Optional<ZipEntry> zipEntryOpt;

            zipEntryOpt = Optional.ofNullable(zipFile.getEntry("optifine/Patcher.class"));
            if (zipEntryOpt.isPresent()) {
                log.info("找到OptiFine的Patcher.class");
                try (JarClassLoader loader = JarClassLoader.loadJar(installerFile)) {
                    Class<?> cls = loader.loadClass("optifine.Patcher");
                    Method m = cls.getMethod("main", String[].class);
                    m.invoke(null, (Object) new String[]{
                            jarFile.getFile().getCanonicalPath(),
                            installerFile.getCanonicalPath(),
                            file.getCanonicalPath()
                    });
                    log.info("Patcher执行成功");
                } catch (ClassNotFoundException e) {
                    throw new Exception(e);
                }
            } else {
                FileUtil.copyFile(installerFile, file, StandardCopyOption.REPLACE_EXISTING);
            }

            zipEntryOpt = Optional.ofNullable(zipFile.getEntry("launchwrapper-2.0.jar"));
            if (zipEntryOpt.isPresent()) {
                log.info("找到launchwrapper-2.0.jar");
                hasLauncherWrapper = true;
                InputStream input = zipFile.getInputStream(zipEntryOpt.get());
                FileUtil.writeFromStream(
                        input,
                        FileUtil.file(
                                minecraftRoot.toFile(),
                                "libraries",
                                Utils.mavenPath("optifine:launchwrapper:2.0"),
                                Utils.mavenFileName("optifine:launchwrapper:2.0")
                        )
                );
                version.getLibraries().add(
                        Library.builder()
                                .name("optifine:launchwrapper:2.0")
                                .build()
                );
            }

            zipEntryOpt = Optional.ofNullable(zipFile.getEntry("launchwrapper-of.txt"));
            if (zipEntryOpt.isPresent()) {
                log.info("找到launchwrapper-of.txt");
                InputStream input = zipFile.getInputStream(zipEntryOpt.get());
                String v = IoUtil.readUtf8(input).trim();

                Optional<ZipEntry> zipEntryOptional = Optional.ofNullable(zipFile.getEntry("launchwrapper-of-" + v + ".jar"));
                if (zipEntryOptional.isPresent()) {
                    log.info("找到launchwrapper-of-" + v + ".jar");
                    InputStream input2 = zipFile.getInputStream(zipEntryOptional.get());
                    String maven = "optifine:launchwrapper-of:" + v;
                    FileUtil.writeFromStream(
                            input2,
                            FileUtil.file(
                                    minecraftRoot.toFile(),
                                    "libraries",
                                    Utils.mavenPath(maven),
                                    Utils.mavenFileName(maven)
                            )
                    );
                    version.getLibraries().add(
                            Library.builder()
                                    .name(maven)
                                    .build()
                    );
                    hasLauncherWrapper = true;
                }
            }

            zipEntryOpt = Optional.ofNullable(zipFile.getEntry("buildof.txt"));
            if (zipEntryOpt.isPresent()) {
                InputStream input = zipFile.getInputStream(zipEntryOpt.get());
                String v = IoUtil.readUtf8(input).trim();

                if (Constants.BOOTSTRAP_LAUNCHER_MAIN.equals(version.getMainClass()) &&
                        VersionComparator.INSTANCE.compare(v.replaceAll("-", "."), "20210924.190833") < 0
                ) {
                    log.error("无法安装该版本的OptiFine");
                    subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
                    return InstallResult.failed();
                }
            }

            if (!hasLauncherWrapper) {
                version.getLibraries().add(
                        Library.builder()
                                .name("net.minecraft:launchwrapper:1.12")
                                .build()
                );
            }

            version.addTweakClass("optifine.OptiFineTweaker");

            version.setMainClass(Constants.LAUNCH_WRAPPER_MAIN);
        } catch (Exception e) {
            log.error("optifine installer file解析失败", e);
            subTaskInfo.update(65535, "失败", SubTaskInfo.STATUS_FAIL);
            return InstallResult.failed();
        }

        log.info("OptiFine安装任务执行成功");
        subTaskInfo.update(65535, "成功", SubTaskInfo.STATUS_SUCCESS);

        return InstallResult.success();
    }

}
