package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel

import ch.epfl.scala.bsp4j.BuildTargetIdentifier

public typealias BuildTargetId = String

public typealias LanguageIds = List<String>

public fun LanguageIds.includesPython(): Boolean = contains("python")
public fun LanguageIds.includesKotlin(): Boolean = contains("kotlin")
public fun LanguageIds.includesJava(): Boolean = contains("java")
public fun LanguageIds.includesScala(): Boolean = contains("scala")
public fun LanguageIds.includesAndroid(): Boolean = contains("android")

public fun LanguageIds.includesJavaOrScala(): Boolean = includesJava() || includesScala()
public fun LanguageIds.isJvmTarget(): Boolean = (includesJava() || includesKotlin()) && !includesAndroid()

public fun List<BuildTargetId>.toBsp4JTargetIdentifiers(): List<BuildTargetIdentifier> =
  this.map { it.toBsp4JTargetIdentifier() }

public fun BuildTargetId.toBsp4JTargetIdentifier(): BuildTargetIdentifier =
  BuildTargetIdentifier(this)
