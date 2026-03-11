package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Foo(val x: Int)

val serializer = Foo.serializer()
