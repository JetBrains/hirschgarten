package com.example;

import org.jetbrains.annotations.PropertyKey;

class Module {

  void bundle(@PropertyKey(resourceBundle = "messages.Bundle") String key) {

  }

  void javaBundle(@PropertyKey(resourceBundle = "messages.JavaBundle") String key) {

  }

  void incorrectBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.IncorrectBundle'">"messages.IncorrectBundle"</error>) String key) {

  }

  void fooBundle(@PropertyKey(resourceBundle = "messages.FooBundle") String key) {

  }

  void barBundle(@PropertyKey(resourceBundle = "messages.BarBundle") String key) {

  }

  void test() {
    bundle("key");
    bundle(<error descr="'keyJava' doesn't appear to be a valid property key">"keyJava"</error>);
    bundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>);
    bundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>);

    javaBundle("keyJava");
    javaBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>);
    javaBundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>);
    javaBundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>);

    fooBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>);
    fooBundle(<error descr="'keyJava' doesn't appear to be a valid property key">"keyJava"</error>);
    fooBundle("keyFoo");
    fooBundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>);

    barBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>);
    barBundle(<error descr="'keyJava' doesn't appear to be a valid property key">"keyJava"</error>);
    barBundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>);
    barBundle("keyBar");
  }
}

