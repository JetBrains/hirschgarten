package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.TargetKind

fun TargetKind.includesKotlin(): Boolean = languageClasses.contains(LanguageClass.KOTLIN)

fun TargetKind.includesJava(): Boolean = languageClasses.contains(LanguageClass.JAVA)

fun TargetKind.includesScala(): Boolean = languageClasses.contains(LanguageClass.SCALA)

fun TargetKind.includesAndroid(): Boolean = languageClasses.contains(LanguageClass.ANDROID)

fun TargetKind.includesGo(): Boolean = languageClasses.contains(LanguageClass.GO)

fun TargetKind.isJvmTarget(): Boolean = (includesJava() || includesKotlin() || includesScala()) && !includesAndroid()

fun TargetKind.isJvmOrAndroidTarget(): Boolean = includesJava() || includesKotlin() || includesScala() || includesAndroid()
