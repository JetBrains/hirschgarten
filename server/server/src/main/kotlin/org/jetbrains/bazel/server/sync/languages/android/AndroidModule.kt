package org.jetbrains.bazel.server.sync.languages.android

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.server.model.LanguageData
import org.jetbrains.bazel.server.sync.languages.java.JavaModule
import org.jetbrains.bazel.server.sync.languages.kotlin.KotlinModule
import org.jetbrains.bsp.protocol.AndroidTargetType
import java.nio.file.Path

data class AndroidModule(
  val androidJar: Path,
  val androidTargetType: AndroidTargetType,
  val manifest: Path?,
  val manifestOverrides: Map<String, String>,
  val resourceDirectories: List<Path>,
  val resourceJavaPackage: String?,
  val assetsDirectories: List<Path>,
  val apk: Path?,
  val javaModule: JavaModule?,
  val kotlinModule: KotlinModule?,
  val correspondingKotlinTarget: CanonicalLabel?,
) : LanguageData
