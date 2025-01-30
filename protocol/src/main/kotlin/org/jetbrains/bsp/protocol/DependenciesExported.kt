package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildTargetIdentifier

data class DependenciesExportedParams(val targets: List<BuildTargetIdentifier>)

data class DependenciesExportedItem(val target: BuildTargetIdentifier, val dependenciesExported: List<Boolean>)

data class DependenciesExportedResult(val items: List<DependenciesExportedItem>)
