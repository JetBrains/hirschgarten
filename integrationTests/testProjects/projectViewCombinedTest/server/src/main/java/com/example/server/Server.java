package com.example.server;

import com.example.common.Common;

public class Server {
    private final Common common = new Common();

    public String serve() {
        return common.getName();
    }
}
