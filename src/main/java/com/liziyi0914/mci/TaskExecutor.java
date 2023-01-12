package com.liziyi0914.mci;

import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.task.Task;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class TaskExecutor {

    List<Observable<InstallResult>> tasks = new LinkedList<>();

    public void add(Task task) {
    }

}
