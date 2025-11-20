package org.jetbrains.bazel.sync_new.connector

sealed interface Arg {
  data class Named(val name: String, val value: Value) : Arg
  data class Positional(val value: Value) : Arg
}

interface StartupOptions : Args {

}

interface Args {
  fun add(arg: Arg)
}

fun Args.add(name: String, value: Value): Unit = add(Arg.Named(name, value))
fun Args.add(value: Value): Unit = add(Arg.Positional(value))
