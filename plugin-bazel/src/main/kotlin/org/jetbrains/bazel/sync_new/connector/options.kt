package org.jetbrains.bazel.sync_new.connector

import org.jetbrains.bazel.commons.constants.Constants
import java.nio.file.Path

sealed interface Arg {
  data class Named(val name: String, val value: Value) : Arg
  data class Positional(val value: Value, val last: Boolean) : Arg
}

interface Args {
  fun add(arg: Arg)
}

interface StartupOptions : Args {
}

fun StartupOptions.overrideWorkspace(path: Path): Unit = add("workspace_override", argValueOf(path))

fun Args.defaults() {
  add(Arg.Named("tool_tag", argValueOf("${Constants.NAME}:${Constants.VERSION}")))
  add(Arg.Named("curses", argValueOf("no")))
  add(Arg.Named("color", argValueOf("yes")))
  add(Arg.Named("progress_in_terminal_title", argValueOf(false)))
}

fun Args.add(name: String, value: Value): Unit = add(Arg.Named(name, value))
fun Args.add(value: Value, last: Boolean = false): Unit = add(Arg.Positional(value, last))

fun Args.keepGoing(): Unit = add("keep_going", argValueOf(true))
