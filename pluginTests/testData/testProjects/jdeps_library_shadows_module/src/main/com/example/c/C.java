package com.example.c;

import com.example.a.A;
import com.example.b.B;

// Demo of how a dependent of B sees A. See the BUILD file in this directory.
public class C {
  public void use() {
    // B is a direct dependency -> resolves cleanly.
    B b = new B();
    b.useA();

    // A reaches C only indirectly and B does not re-export it, so A is NOT on C's classpath:
    // `new A()` is unresolved. IntelliJ then offers "Add dependency on module 'A'", which adds
    // //src/main/com/example/a:A to this target's deps in the BUILD file.
    A a = new A();
    a.hello();
  }
}
