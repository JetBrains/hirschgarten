package com.example.a;

import com.example.b.ClassB;

public class ClassA {
    public <caret>ClassB createB() {
        return new ClassB();
    }
}
