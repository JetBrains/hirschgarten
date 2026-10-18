package org.jetbrains.bazel.test.framework.testproject

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.StringsCollection

private val JAVA_LIBRARY = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.JAVA))

private val JAVA_BINARY = TargetKind(kind = "java_binary", ruleType = RuleType.BINARY, languageClasses = setOf(JavaLanguageClass.JAVA))

private val KOTLIN_LIBRARY =
  TargetKind(kind = "kt_jvm_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(JavaLanguageClass.KOTLIN))

fun javaLibrary(label: String, declare: TestTargetBuilder.() -> Unit = {}): TestBuildTarget =
  buildTarget(label, JAVA_LIBRARY) {
    data(JvmBuildTarget())
    declare()
  }

fun javaBinary(label: String, declare: TestTargetBuilder.() -> Unit = {}): TestBuildTarget =
  buildTarget(label, JAVA_BINARY) {
    data(JvmBuildTarget())
    declare()
  }

fun kotlinLibrary(label: String, declare: TestTargetBuilder.() -> Unit = {}): TestBuildTarget =
  buildTarget(label, KOTLIN_LIBRARY) {
    data(
      KotlinBuildTarget(languageVersion = "1.8", apiVersion = "1.8", kotlincOptions = StringsCollection.EMPTY, associates = listOf()),
      JvmBuildTarget(),
    )
    declare()
  }
