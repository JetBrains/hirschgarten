package org.jetbrains.bazel.sync_new.connector

interface StartupOptions : Args {

}

interface Args {
  fun add(name: String, value: Value)
}
