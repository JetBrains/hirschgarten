package com.example.b;

import com.example.a.A;

public class B {
  public void useA() {
    // Using A (provided transitively via M's `exports`) makes javac record A's header jar
    // in B's .jdeps, which is what leaks back in as a sourceless library.
    A a = new A();
    a.hello();
  }
}
