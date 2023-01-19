package com.liziyi0914.mci.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    String id;

    String name;

    Long size;

    String url;

    String hash;

    File file;

}
