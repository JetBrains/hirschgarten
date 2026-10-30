package com.example.project;

import com.example.project.Internal;
import java.lang.String;

class Heavy {
    public static String notWorthInspecting(String s) {
        Internal.doSomething();
        return s.toUpperCase();
    }
}