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

}
