package com.example;

import org.jetbrains.annotations.PropertyKey;

class A {

  void bundle(@PropertyKey(resourceBundle = "Bundle") String key) {

  }

  void incorrectBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'IncorrectBundle'">"IncorrectBundle"</error>) String key) {

  }

  void test() {
    bundle("foo");
    bundle(<error descr="'wrongKey' doesn't appear to be a valid property key">"wrongKey"</error>);
  }
}

