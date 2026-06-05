package com.example

import org.jetbrains.annotations.PropertyKey

class Module {

  fun mainResourcesBundle(@PropertyKey(resourceBundle = "messages.Bundle") key: String) {}

  fun javaPrefixBundle(@PropertyKey(resourceBundle = "utils.UtilBundle") key: String) {}

  fun kotlinMessagesBundle(@PropertyKey(resourceBundle = "messages.KotlinBundle") key: String) {}

  fun kotlinMessagesBundle1(@PropertyKey(resourceBundle = "messages.KotlinBundle1") key: String) {}

  fun kotlinMessagesBundle2(@PropertyKey(resourceBundle = "messages.KotlinBundle2") key: String) {}

  fun adjacentBundle(@PropertyKey(resourceBundle = "com.example.Adjacent") key: String) {}

  fun fooBundle(@PropertyKey(resourceBundle = "messages.FooBundle") key: String) {}

  fun standaloneBundle(@PropertyKey(resourceBundle = "StandaloneBundle") key: String) {}

  fun missingBundle(@PropertyKey(resourceBundle = <error descr="Invalid resource bundle reference 'messages.DoesNotExist'">"messages.DoesNotExist"</error>) key: String) {}

  fun test() {
    mainResourcesBundle("key")
    mainResourcesBundle(<error descr="'wrongKey' doesn't appear to be a valid property key">"wrongKey"</error>)

    javaPrefixBundle("utilKey")
    javaPrefixBundle(<error descr="'wrongKey' doesn't appear to be a valid property key">"wrongKey"</error>)

    kotlinMessagesBundle("keyKotlin")
    kotlinMessagesBundle1("keyKotlin1")
    kotlinMessagesBundle2("keyKotlin2")
    kotlinMessagesBundle(<error descr="'wrongKey' doesn't appear to be a valid property key">"wrongKey"</error>)

    adjacentBundle("adjacentKey")
    adjacentBundle(<error descr="'wrongKey' doesn't appear to be a valid property key">"wrongKey"</error>)

    fooBundle("keyFoo")
    fooBundle(<error descr="'wrongKey' doesn't appear to be a valid property key">"wrongKey"</error>)

    standaloneBundle("standaloneKey")
    standaloneBundle(<error descr="'wrongKey' doesn't appear to be a valid property key">"wrongKey"</error>)
  }
}
