package org.jetbrains.magicmetamodel.impl.workspacemodel

import ch.epfl.scala.bsp4j.BuildTargetIdentifier

public typealias BuildTargetId = String

public typealias LanguageIds = List<String>

public fun LanguageIds.includesPython(): Boolean = contains("python")
public fun LanguageIds.includesKotlin(): Boolean = contains("kotlin")
public fun LanguageIds.includesJava(): Boolean = contains("java")

public fun List<BuildTargetId>.toBsp4JTargetIdentifiers(): List<BuildTargetIdentifier> =
  this.map { it.toBsp4JTargetIdentifier() }

public fun BuildTargetId.toBsp4JTargetIdentifier(): BuildTargetIdentifier =
  BuildTargetIdentifier(this)
