package com.example.client;

import com.example.common.Common;

public class Client {
    private final Common common = new Common();

    public String connect() {
        return common.getName();
    }
}
