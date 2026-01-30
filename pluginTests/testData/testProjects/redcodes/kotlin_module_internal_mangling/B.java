package com.example;

import com.example.A;

public class B {

  public void b() {
    A a = new A();
    a.getProperty$a_module();
    a.setProperty$a_module("");
    a.method$a_module(1);
    a.<error descr="Cannot resolve method 'method$wrong_module' in 'A'">method$wrong_module</error>(1);
  }
}