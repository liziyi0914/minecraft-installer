package com.liziyi0914.mci.handler;

import com.liziyi0914.mci.Cmd;
import com.liziyi0914.mci.TaskExecutor;
import com.liziyi0914.mci.bean.InstallContext;

public interface Handler {

    boolean canHandle(Cmd cmd);

    void handle(Cmd cmd, InstallContext ctx, TaskExecutor executor);

}
