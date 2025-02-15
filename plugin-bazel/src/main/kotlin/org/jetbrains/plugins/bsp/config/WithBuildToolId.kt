package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.extensions.ExtensionPointName

data class BuildToolId(val id: String)

interface WithBuildToolId {
  val buildToolId: BuildToolId
}

val bspBuildToolId: BuildToolId = BuildToolId("bsp")

fun <T : WithBuildToolId> ExtensionPointName<T>.withBuildToolId(buildToolId: BuildToolId): T? =
  this.extensions.find { it.buildToolId == buildToolId }

fun <T : WithBuildToolId> ExtensionPointName<T>.withBuildToolIdOrDefault(buildToolId: BuildToolId): T =
  this.withBuildToolId(buildToolId)
    ?: default()

fun <T : WithBuildToolId> ExtensionPointName<T>.default(): T =
  withBuildToolId(bspBuildToolId)
    ?: error("Missing default implementation (BSP) for extension: ${this.javaClass.name}. Something is wrong.")

fun <T : WithBuildToolId> ExtensionPointName<T>.allWithBuildToolId(buildToolId: BuildToolId): List<T> =
  this.extensions.filter { it.buildToolId == buildToolId }
