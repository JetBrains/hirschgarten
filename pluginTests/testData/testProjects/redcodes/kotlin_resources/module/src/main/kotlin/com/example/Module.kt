package com.example

import org.jetbrains.annotations.PropertyKey

class Module {

  fun bundle(@PropertyKey(resourceBundle = "messages.Bundle") key: String) {

  }

  fun kotlinBundle(@PropertyKey(resourceBundle = "messages.KotlinBundle") key: String) {

  }

  fun incorrectBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.IncorrectBundle'">"messages.IncorrectBundle"</error>) key: String) {

  }

  fun fooBundle(@PropertyKey(resourceBundle = "messages.FooBundle") key: String) {

  }

  fun barBundle(@PropertyKey(resourceBundle = "messages.BarBundle") key: String) {

  }

  fun test() {
    bundle("key")
    bundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>)
    bundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>)

    fooBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>)
    fooBundle("keyFoo")
    fooBundle(<error descr="'keyBar' doesn't appear to be a valid property key">"keyBar"</error>)

    barBundle(<error descr="'key' doesn't appear to be a valid property key">"key"</error>)
    barBundle(<error descr="'keyFoo' doesn't appear to be a valid property key">"keyFoo"</error>)
    barBundle("keyBar")
  }
}

