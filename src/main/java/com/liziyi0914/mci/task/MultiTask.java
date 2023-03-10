package com.liziyi0914.mci.task;

import com.liziyi0914.mci.Ids;
import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiTask implements Task {

    @Getter
    SubTaskInfo info;

    final List<Task> tasks;

    public MultiTask(Task... tasks) {
        this.tasks = new ArrayList<>(Arrays.asList(tasks));
    }

    @Override
    public InstallResult execute(InstallContext ctx) {
        boolean multiThread = ctx.get(Ids.VAR_MULTI_THREAD);

        boolean success;
        if (multiThread) {
            success = Flowable.fromIterable(tasks)
                    .flatMap(task -> Flowable.just(task)
                                    .observeOn(Schedulers.io())
                                    .map(t -> t.execute(ctx)),
                            3
                    )
                    .all(InstallResult::isSuccess)
                    .blockingGet();
        } else {
            success = tasks.stream().allMatch(task -> task.execute(ctx).isSuccess());
        }

        return success?InstallResult.success():InstallResult.failed();
    }

}
