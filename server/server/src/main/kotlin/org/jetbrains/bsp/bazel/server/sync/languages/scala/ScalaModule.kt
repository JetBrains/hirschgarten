package org.jetbrains.bsp.bazel.server.sync.languages.scala

import java.net.URI
import org.jetbrains.bsp.bazel.server.model.LanguageData
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaModule

data class ScalaSdk(
    val organization: String,
    val version: String,
    val binaryVersion: String,
    val compilerJars: List<URI>
)

data class ScalaModule(
    val sdk: ScalaSdk,
    val scalacOpts: List<String>,
    val javaModule: JavaModule?
) : LanguageData
