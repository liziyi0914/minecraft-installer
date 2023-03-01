package com.liziyi0914.mci.task;

import com.liziyi0914.mci.Identifier;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import lombok.Getter;

public class VarTask<T> implements Task {

    @Getter
    SubTaskInfo info;

    final Identifier<T> identifier;
    final T value;

    public VarTask(Identifier<T> identifier, T value) {
        this.identifier = identifier;
        this.value = value;
    }

    @Override
    public InstallResult execute(InstallContext ctx) {
        ctx.put(identifier, value);

        return InstallResult.success();
    }

}
