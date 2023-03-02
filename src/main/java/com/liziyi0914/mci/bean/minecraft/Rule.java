package com.liziyi0914.mci.bean.minecraft;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Rule {

    String action;
    Map<String,String> os;
    /*
    name
    version
     */

}
