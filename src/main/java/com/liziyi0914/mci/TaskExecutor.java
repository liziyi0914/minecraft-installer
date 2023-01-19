package com.liziyi0914.mci;

import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.SubTaskInfo;
import com.liziyi0914.mci.bean.TaskInfo;
import com.liziyi0914.mci.collector.Collector;
import com.liziyi0914.mci.task.MultiTask;
import com.liziyi0914.mci.task.Task;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TaskExecutor {

    List<Task> tasks = new LinkedList<>();

    int count = 0;

    Collector collector = null;

    public void collector(Collector collector) {
        this.collector = collector;
    }

    public TaskExecutor then(Task task) {
        task.getInfo().setIndex(count);
        tasks.add(task);
        count++;
        return this;
    }

    public TaskExecutor thenMulti(Task... task) {
        for (Task t : task) {
            t.getInfo().setIndex(count);
            count++;
        }
        tasks.add(new MultiTask(task));
        return this;
    }

    void commitTaskInfo(TaskInfo taskInfo) {
        Optional.ofNullable(this.collector).ifPresent(c->c.commit(taskInfo));
    }

    public void execute(InstallContext context) {
        List<SubTaskInfo> subTaskInfos = new ArrayList<>();
        for (Task task : tasks) {
            subTaskInfos.addAll(Arrays.asList(task.getSubTaskInfos()));
        }
        long taskId = context.get(Identifiers.VAR_TASK_ID);
        String taskName = context.get(Identifiers.VAR_TASK_NAME);
        TaskInfo taskInfo = new TaskInfo(taskId,taskName, "minecraft_install",subTaskInfos, TaskInfo.STATUS_PENDING);

        Disposable disposable = Flowable.interval(50, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(t-> commitTaskInfo(taskInfo));

        taskInfo.setStatus(TaskInfo.STATUS_RUNNING);
        for (Task task : tasks) {
            if (!task.execute(context).isSuccess()) {
                log.error("任务执行失败: {}", task.getClass().getSimpleName());
                disposable.dispose();
                taskInfo.setStatus(TaskInfo.STATUS_FAIL);
                commitTaskInfo(taskInfo);
                Optional.ofNullable(this.collector).ifPresent(Collector::close);
                return;
            }
        }

        disposable.dispose();
        taskInfo.setStatus(TaskInfo.STATUS_SUCCESS);
        commitTaskInfo(taskInfo);
        Optional.ofNullable(this.collector).ifPresent(Collector::close);
        Utils.getClient().dispatcher().executorService().shutdown();
    }

}
