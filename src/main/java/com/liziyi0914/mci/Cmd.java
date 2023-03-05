package com.liziyi0914.mci;

import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.collector.WebSocketCollector;
import com.liziyi0914.mci.handler.Handlers;
import com.liziyi0914.mci.mirror.Mirrors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Data
@Slf4j
@Command(name = "MinecraftInstaller.jar", version = "1.0")
public class Cmd implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, description = "帮助", usageHelp = true)
    boolean help;

    @Option(names = {"--parse"}, description = "仅解析参数")
    boolean parseOnly = false;

    @Option(names = {"--override"}, description = "覆盖已有文件")
    boolean override = false;

    @Option(names = {"--multi-thread"}, description = "多线程")
    boolean multiThread = true;

    @Option(names = {"--mix"}, description = "合并版本")
    boolean mix = false;

    @Option(names = {"--root"}, description = "Minecraft根目录", required = true)
    File root;

    @Option(names = {"--id"}, description = "实例ID", required = true)
    String id;

    @Option(names = {"--mc"}, description = "Minecraft版本", required = true)
    String minecraft;

    @Option(names = {"--forge"}, description = "Forge build", required = false)
    String forge;

    @Option(names = {"--optifine"}, description = "OptiFine版本（type:patch，如：\"HD_U:E4\"）", required = false)
    String optifine;

    @Option(names = {"--liteloader"}, description = "LiteLoader版本", required = false)
    String liteloader;

    @Option(names = {"--ws-url"}, description = "WebSocket地址")
    String ws;

    @Option(names = {"--ws-token"}, description = "WebSocket Token")
    String wsToken = "";

    @Option(names = {"--mirror"}, description = "镜像站")
    String mirror = "official";

    @Override
    public Integer call() {
        if (isParseOnly()) {
            log.info("{}",this);
            return 0;
        }

        InstallContext ctx = new InstallContext();

        switch (getMirror().toLowerCase()) {
            case "bmclapi":
                ctx.put(Identifiers.VAR_MIRROR, Mirrors.BMCLAPI);
                break;
            case "mcbbs":
                ctx.put(Identifiers.VAR_MIRROR, Mirrors.MCBBS);
                break;
            default:
                ctx.put(Identifiers.VAR_MIRROR, Mirrors.OFFICIAL);
        }

        ctx.put(Identifiers.VAR_MINECRAFT_ROOT, getRoot().toPath());
        ctx.put(Identifiers.VAR_MINECRAFT_VERSION, getMinecraft());
        ctx.put(Identifiers.VAR_ID, getId());

        ctx.put(Identifiers.VAR_OVERRIDE_EXISTS, isOverride());

        ctx.put(Identifiers.VAR_TASK_ID, new Random().nextLong());

        ctx.put(Identifiers.VAR_MULTI_THREAD, isMultiThread());

        ctx.put(Identifiers.VAR_MIX, isMix());

        TaskExecutor executor = new TaskExecutor();

        if (Objects.nonNull(ws)) {
            WebSocketCollector webSocketCollector = WebSocketCollector.getInstance();
            webSocketCollector.init(ws, wsToken);
            executor.collector(webSocketCollector);
        }

        if (Handlers.DEFAULT.canHandle(this)) {
            Handlers.DEFAULT.handle(this, ctx, executor);
        } else {
            log.error("找不到安装流程");
        }

        return 0;
    }
}
