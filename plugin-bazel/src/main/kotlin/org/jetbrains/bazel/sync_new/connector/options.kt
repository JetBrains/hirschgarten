package org.jetbrains.bazel.sync_new.connector

import org.jetbrains.bazel.commons.constants.Constants

sealed interface Arg {
  data class Named(val name: String, val value: Value) : Arg
  data class Positional(val value: Value) : Arg
}

interface Args {
  fun add(arg: Arg)
}

interface StartupOptions : Args {
}

fun Args.defaults() {
  add(Arg.Named("tool_tag", argValueOf("${Constants.NAME}:${Constants.VERSION}")))
  add(Arg.Named("curses", argValueOf("no")))
  add(Arg.Named("color", argValueOf("yes")))
  add(Arg.Named("progress_in_terminal_title", argValueOf(false)))
}

fun Args.add(name: String, value: Value): Unit = add(Arg.Named(name, value))
fun Args.add(value: Value): Unit = add(Arg.Positional(value))

fun Args.keepGoing(): Unit = add("keep_going", argValueOf(true))
fun Args.injectRepository(repo: String): Unit = add("inject_repository", argValueOf(repo))
