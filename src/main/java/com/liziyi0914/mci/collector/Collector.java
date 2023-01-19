package com.liziyi0914.mci.collector;

import com.liziyi0914.mci.bean.TaskInfo;

public interface Collector {

    void commit(TaskInfo info);

    void close();

}
