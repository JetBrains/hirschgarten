package org.jetbrains.bsp.bazel.server.sync.languages.android

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.bazel.server.model.LanguageData
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaModule
import org.jetbrains.bsp.bazel.server.sync.languages.kotlin.KotlinModule
import org.jetbrains.bsp.protocol.AndroidTargetType
import java.net.URI

data class AndroidModule(
  val androidJar: URI,
  val androidTargetType: AndroidTargetType,
  val manifest: URI?,
  val manifestOverrides: Map<String, String>,
  val resourceDirectories: List<URI>,
  val resourceJavaPackage: String?,
  val assetsDirectories: List<URI>,
  val apk: URI?,
  val javaModule: JavaModule?,
  val kotlinModule: KotlinModule?,
  val correspondingKotlinTarget: Label?,
) : LanguageData
