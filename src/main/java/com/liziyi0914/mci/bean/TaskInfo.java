package com.liziyi0914.mci.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskInfo {

    public static final int STATUS_PENDING = 0;

    public static final int STATUS_RUNNING = 1;

    public static final int STATUS_SUCCESS = 2;

    public static final int STATUS_FAIL = 3;

    long id;

    String name;

    String type;

    List<SubTaskInfo> subTasks;

    int status;

}
