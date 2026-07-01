package org.jetbrains.bazel.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider
import org.jetbrains.bazel.commons.TargetKind

@ApiStatus.Internal
object JavaLanguageClass {
  val JAVA = LanguageClass("java", setOf("java"))
  val SCALA = LanguageClass("scala", setOf("scala"), fusOverride = JAVA)
  val KOTLIN = LanguageClass("kotlin", setOf("kt"), fusOverride = JAVA)
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
