package com.example.webapp;

import com.example.frontend.Frontend;

public class Webapp {
    private final Frontend frontend = new Frontend();

    public String handle() {
        return frontend.render();
    }
}
