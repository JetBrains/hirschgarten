package org.jetbrains.plugins.bsp.workspacemodel.entities

public typealias LanguageIds = List<String>

public fun LanguageIds.includesKotlin(): Boolean = contains("kotlin")

public fun LanguageIds.includesJava(): Boolean = contains("java")

public fun LanguageIds.includesScala(): Boolean = contains("scala")

public fun LanguageIds.includesAndroid(): Boolean = contains("android")

public fun LanguageIds.includesGo(): Boolean = contains("go")

public fun LanguageIds.includesJavaOrScala(): Boolean = includesJava() || includesScala()

public fun LanguageIds.isJvmTarget(): Boolean = (includesJava() || includesKotlin() || includesScala()) && !includesAndroid()

public fun LanguageIds.isJvmOrAndroidTarget(): Boolean = includesJava() || includesKotlin() || includesScala() || includesAndroid()
