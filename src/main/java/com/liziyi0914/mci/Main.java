package com.liziyi0914.mci;

import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.task.MinecraftJarTask;
import com.liziyi0914.mci.task.MinecraftJsonTask;
import com.liziyi0914.mci.task.MinecraftLibrariesTask;
import com.liziyi0914.mci.task.MinecraftVersionManifestTask;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello world!");

        InstallContext ctx = new InstallContext();
        ctx.put(Constants.VAR_MINECRAFT_ROOT, Paths.get("D:\\.minecraft"));
        ctx.put(Constants.VAR_MINECRAFT_VERSION, "1.16.5");
        ctx.put(Constants.VAR_ID, "1.16.5-2333333");

        new MinecraftVersionManifestTask().execute(ctx);
        new MinecraftJsonTask().execute(ctx);
        new MinecraftJarTask().execute(ctx);
        new MinecraftLibrariesTask().execute(ctx);

        System.out.println(ctx);

        Utils.getClient().dispatcher().executorService().shutdown();
    }
}