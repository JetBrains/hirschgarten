package com.example.app;

import com.example.lib.Leaf;
import gen.Extra;

public class App {
    public String run() {
        return new Leaf().greeting() + Extra.tag();
    }
}
