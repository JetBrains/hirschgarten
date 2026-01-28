package com.example;

import org.jetbrains.annotations.PropertyKey;

class Module {

  void bundle(@PropertyKey(resourceBundle = "messages.Bundle") String key) {

  }

  void incorrectBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.IncorrectBundle'">"messages.IncorrectBundle"</error>) String key) {

  }

  void excludedBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.ExcludedBundle'">"messages.ExcludedBundle"</error>) String key) {

  }

  void fooBundle(@PropertyKey(resourceBundle = "messages.FooBundle") String key) {

  }

  void excludedFooBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.ExcludedFooBundle'">"messages.ExcludedFooBundle"</error>) String key) {

  }

  void barBundle(@PropertyKey(resourceBundle = "messages.BarBundle") String key) {

  }

  void excludedBarBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.ExcludedBarBundle'">"messages.ExcludedBarBundle"</error>) String key) {

  }

  void test() {
    bundle("key");
    bundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>);
    bundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>);
    excludedBundle(<error descr="'keyExcluded' doesn't appear to be a valid property key">"keyExcluded"</error>);

    fooBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>);
    fooBundle("keyFoo");
    fooBundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>);
    excludedFooBundle(<error descr="'keyFooExcluded' doesn't appear to be a valid property key">"keyFooExcluded"</error>);

    barBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>);
    barBundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>);
    barBundle("keyBar");
    excludedBarBundle(<error descr="'keyBarExcluded' doesn't appear to be a valid property key">"keyBarExcluded"</error>);
  }
}

