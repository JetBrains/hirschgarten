package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.bazel.sync.workspace.languages.java.JavaModule
import org.jetbrains.bazel.sync.workspace.languages.kotlin.KotlinModule
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaModule
import org.jetbrains.bazel.sync.workspace.model.Module

val Module.javaModule: JavaModule?
  get() {
    return when (val data = languageData) {
      is JavaModule -> data
      is ScalaModule -> data.javaModule
      is KotlinModule -> data.javaModule
      else -> null
    }
  }
