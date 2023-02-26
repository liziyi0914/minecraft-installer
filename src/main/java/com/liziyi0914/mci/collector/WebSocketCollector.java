package com.liziyi0914.mci.collector;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.liziyi0914.mci.Utils;
import com.liziyi0914.mci.bean.TaskInfo;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Predicate;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WebSocketCollector extends WebSocketListener implements Collector {

    private static final WebSocketCollector INSTANCE = new WebSocketCollector();

    private static final OkHttpClient client = Utils.getClient();

    private static WebSocket ws;

    String url;

    String token;

    int heartbeatCache = 0;

    int state = 0;

    Disposable heartbeat;

    TaskInfo taskInfo;

    private WebSocketCollector() {
    }

    public static WebSocketCollector getInstance() {
        return INSTANCE;
    }

    public void init(String url, String token) {
        this.url = url;
        this.token = token;

        ws = client.newWebSocket(
                new Request.Builder()
                        .url(url)
                        .build(),
                this
        );
    }

    @Override
    public void commit(TaskInfo info) {
        this.taskInfo = info;
        JSONObject json = new JSONObject();
        json.set("op",2);
        json.set("d", info);
        ws.send(json.toString());
    }

    @Override
    public void close() {
        JSONObject json = new JSONObject();
        json.set("op",2);
        json.set("d", this.taskInfo);
        ws.send(json.toString());

        ws.close(1000, "close");
        Optional.ofNullable(this.heartbeat).ifPresent(Disposable::dispose);
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        webSocket.send("{\"op\":0,\"token\":\""+token+"\"}");

        this.heartbeat = Observable.interval(15, TimeUnit.SECONDS)
                .takeUntil((Predicate<? super Long>) t->!webSocket.send("{\"op\":1}"))
                .subscribe(t->{
                    if (state!=1) {
                        log.error("WebSocket认证失败");
                        webSocket.close(1000, "认证失败");
                    }

                    if (heartbeatCache>=4) {
                        log.error("WebSocket心跳超时");
                        webSocket.close(1000, "Heartbeat timeout");
                    }

                    log.info("发送WebSocket心跳包");
                    heartbeatCache++;
                });
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        JSONObject json = JSONUtil.parseObj(text);

        int op = json.getInt("op");
        switch (op) {
            case 0: {
                log.info("WebSocket连接成功");
                state = 1;
                break;
            }
            case 1: {
                log.info("收到WebSocket心跳包");
                heartbeatCache = 0;
                break;
            }
            case 3: {
                log.info("收到WebSocket指令");
                break;
            }
            default: {
                break;
            }
        }
    }
}
