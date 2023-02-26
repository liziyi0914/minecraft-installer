package com.liziyi0914.mci.task;

import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import lombok.Getter;

import java.util.function.Supplier;

public class SimpleTask implements Task {

    @Getter
    SubTaskInfo info;

    final Supplier<Boolean> f;

    public SimpleTask(Supplier<Boolean> f) {
        this.f = f;
    }

    @Override
    public InstallResult execute(InstallContext ctx) {
        boolean success = f.get();

        return success?InstallResult.success():InstallResult.failed();
    }

}
