package com.example;

import org.jetbrains.annotations.PropertyKey;

class A {
  void includedBundle(@PropertyKey(resourceBundle = "messages.IncludedBundle") String key) {

  }

  void excludedBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.ExcludedBundle'">"messages.ExcludedBundle"</error>) String key) {

  }

  void incorrectBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.IncorrectBundle'">"messages.IncorrectBundle"</error>) String key) {

  }

  void test() {
    includedBundle("foo");
    includedBundle(<error descr="'wrongKey' doesn't appear to be a valid property key">"wrongKey"</error>);
  }
}

