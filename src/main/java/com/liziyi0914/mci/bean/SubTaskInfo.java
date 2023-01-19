package com.liziyi0914.mci.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubTaskInfo {

    public static final int STATUS_PENDING = 0;

    public static final int STATUS_RUNNING = 1;

    public static final int STATUS_SUCCESS = 2;

    public static final int STATUS_FAIL = 3;

    long index;

    String name;

    int weight;

    int progress;

    String message;

    int status;

    public SubTaskInfo(String name,int weight) {
        this(0, name, weight, 0, "等待中", STATUS_PENDING);
    }

    public void update(int progress, String message, int status) {
        this.progress = progress;
        this.message = message;
        this.status = status;
    }

}
