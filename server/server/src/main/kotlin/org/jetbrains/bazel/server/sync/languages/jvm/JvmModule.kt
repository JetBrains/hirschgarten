package org.jetbrains.bazel.server.sync.languages.jvm

import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.sync.languages.android.AndroidModule
import org.jetbrains.bazel.server.sync.languages.java.JavaModule
import org.jetbrains.bazel.server.sync.languages.kotlin.KotlinModule
import org.jetbrains.bazel.server.sync.languages.scala.ScalaModule

val Module.javaModule: JavaModule?
  get() {
    return when (val data = languageData) {
      is JavaModule -> data
      is ScalaModule -> data.javaModule
      is KotlinModule -> data.javaModule
      is AndroidModule -> data.javaModule
      else -> null
    }
  }
