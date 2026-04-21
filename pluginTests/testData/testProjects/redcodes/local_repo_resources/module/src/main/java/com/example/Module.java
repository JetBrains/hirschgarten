package com.example;

import org.jetbrains.annotations.PropertyKey;

class Module {

  void bundle(@PropertyKey(resourceBundle = "messages.Bundle") String key) {

  }

  void incorrectBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.IncorrectBundle'">"messages.IncorrectBundle"</error>) String key) {

  }

  void fooBundle(@PropertyKey(resourceBundle = "messages.FooBundle") String key) {

  }

  void barBundle(@PropertyKey(resourceBundle = "messages.BarBundle") String key) {

  }

  void standardBundle(@PropertyKey(resourceBundle = "messages.StandardBundle") String key) {

  }

  void test() {
    bundle("key");
    bundle(<error descr="'keyStandard' doesn't appear to be a valid property key">"keyStandard"</error>);
    bundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>);
    bundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>);

    fooBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>);
    fooBundle(<error descr="'keyStandard' doesn't appear to be a valid property key">"keyStandard"</error>);
    fooBundle("keyFoo");
    fooBundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>);

    barBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>);
    barBundle(<error descr="'keyStandard' doesn't appear to be a valid property key">"keyStandard"</error>);
    barBundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>);
    barBundle("keyBar");

    standardBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>);
    standardBundle("keyStandard");
    standardBundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>);
    standardBundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>);
  }
}
