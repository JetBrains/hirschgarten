package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName

public data class BuildToolId(public val id: String)

public interface WithBuildToolId {
  public val buildToolId: BuildToolId
}

val bspBuildToolId = BuildToolId("bsp")

fun <T : WithBuildToolId> ExtensionPointName<T>.withBuildToolId(buildToolId: BuildToolId): T? =
  this.extensions.find { it.buildToolId == buildToolId }

fun <T : WithBuildToolId> ExtensionPointName<T>.withBuildToolIdOrDefault(buildToolId: BuildToolId): T =
  this.withBuildToolId(buildToolId)
    ?: withBuildToolId(bspBuildToolId)
    ?: error("Missing default implementation (BSP) for extension: ${this.javaClass.name}. Something is wrong.")

internal fun <T : WithBuildToolId> ExtensionPointName<T>.allWithBuildToolId(buildToolId: BuildToolId): List<T> =
  this.extensions.filter { it.buildToolId == buildToolId }
