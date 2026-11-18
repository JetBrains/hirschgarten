package org.jetbrains.bazel.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.relativeNioPath
import kotlin.io.path.extension

@ApiStatus.Internal
object JavaLanguageClass {
  val JAVA = LanguageClass("java", setOf("java"))
  val SCALA = LanguageClass("scala", setOf("scala"), fusOverride = JAVA)
  val KOTLIN = LanguageClass("kotlin", setOf("kt"), fusOverride = JAVA)

  private val jvmSourceExtensions =
    buildSet {
      addAll(this@JavaLanguageClass.JAVA.recognizedFilenameExtensions)
      addAll(this@JavaLanguageClass.KOTLIN.recognizedFilenameExtensions)
      addAll(this@JavaLanguageClass.SCALA.recognizedFilenameExtensions)
    }

  fun OutputLocation.hasJvmSourceExtension(): Boolean = relativeNioPath.extension in jvmSourceExtensions
}

internal class JavaLanguageClassProvider: LanguageClassProvider {
  override val languages: List<LanguageClass>
    get() = listOf(JavaLanguageClass.JAVA, JavaLanguageClass.SCALA, JavaLanguageClass.KOTLIN)
}

@ApiStatus.Internal
fun TargetKind.includesKotlin(): Boolean = languageClasses.contains(JavaLanguageClass.KOTLIN)

@ApiStatus.Internal
fun TargetKind.includesJava(): Boolean = languageClasses.contains(JavaLanguageClass.JAVA)

@ApiStatus.Internal
fun TargetKind.includesScala(): Boolean = languageClasses.contains(JavaLanguageClass.SCALA)

@ApiStatus.Internal
fun TargetKind.isJvmTarget(): Boolean = includesJava() || includesKotlin() || includesScala()
