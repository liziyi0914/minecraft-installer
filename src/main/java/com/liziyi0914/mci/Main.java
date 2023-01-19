package com.liziyi0914.mci;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import static picocli.CommandLine.MissingParameterException;

@Slf4j
public class Main {
    public static void main(String[] args) {
        CommandLine cmdLine = new CommandLine(new Cmd());
        try {
            cmdLine.execute(args);
        } catch (MissingParameterException e) {
            cmdLine.usage(System.out);
        }
    }
}