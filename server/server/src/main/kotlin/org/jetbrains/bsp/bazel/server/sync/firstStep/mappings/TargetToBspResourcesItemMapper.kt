package org.jetbrains.bsp.bazel.server.sync.firstStep.mappings

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.ResourcesItem
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import java.nio.file.Path

fun Target.toBspResourcesItem(workspaceRoot: Path): ResourcesItem =
  ResourcesItem(
    BuildTargetIdentifier(rule.name),
    getListAttribute("resources"),
  )
