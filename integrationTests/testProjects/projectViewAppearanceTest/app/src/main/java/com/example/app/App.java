package com.example.app;

import com.example.common.Common;

public class App {
    private final Common common = new Common();

    public String run() {
        return common.getName();
    }
}
