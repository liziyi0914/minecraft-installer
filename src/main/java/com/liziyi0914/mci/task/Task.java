package com.liziyi0914.mci.task;

import com.liziyi0914.mci.bean.InstallContext;
import com.liziyi0914.mci.bean.InstallResult;

import java.io.IOException;

public interface Task {

    InstallResult execute(InstallContext ctx) throws Exception;

}
