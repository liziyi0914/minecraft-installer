package com.liziyi0914.mci.task;

import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;
import com.liziyi0914.mci.bean.SubTaskInfo;

public interface Task {

    InstallResult execute(InstallContext ctx);

    SubTaskInfo getInfo();

    default SubTaskInfo[] getSubTaskInfos() {
        if (this instanceof MultiTask) {
            return ((MultiTask) this).tasks.stream().map(Task::getInfo).toArray(SubTaskInfo[]::new);
        } else {
            return new SubTaskInfo[]{getInfo()};
        }
    }

}
