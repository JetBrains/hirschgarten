package com.example.a;

import com.example.b.B;

public class A {
    public void usesB() {
        B b = null;
        var c1 = b.getC();  // Compiles because var
        b.getC().fooBar(); // No error
        <error descr="Using type com.example.c.C from an indirect dependency @//src/main/com/example/c:C">com.example.c.C</error> c2 = null;
    }
}
