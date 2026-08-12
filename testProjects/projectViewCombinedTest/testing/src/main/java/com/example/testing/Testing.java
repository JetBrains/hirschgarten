package com.example.testing;

import com.example.common.Common;

public class Testing {
    private final Common common = new Common();

    public boolean verify() {
        return common.getName() != null;
    }
}
