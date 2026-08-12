package com.example.other;

import com.example.common.Common;

public class Other {
    private final Common common = new Common();

    public String run() {
        return common.getName();
    }
}
