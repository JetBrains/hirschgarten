package com.example.foo

import org.scalatest.flatspec.AnyFlatSpec

class FooTest extends AnyFlatSpec {
  "things" should "work" in {
    assert(Foo.text == "Hello foo")
  }
}
