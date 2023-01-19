package com.liziyi0914.mci.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallResult {

    int code;

    Object data;

    public static InstallResult success() {
        return new InstallResult(200, null);
    }

    public static InstallResult failed() {
        return new InstallResult(400, null);
    }

    public boolean isSuccess() {
        return code==200;
    }
}
