package com.example

import org.jetbrains.annotations.PropertyKey

class Module {

  fun incorrectBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.IncorrectBundle'">"messages.IncorrectBundle"</error>) key: String) {

  }

  fun fooBundle(@PropertyKey(resourceBundle = "messages.FooBundle") key: String) {

  }

  fun barBundle(@PropertyKey(resourceBundle = "messages.BarBundle") key: String) {

  }

  fun test() {

    fooBundle("keyFoo")
    fooBundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>)

    barBundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>)
    barBundle("keyBar")
  }
}

