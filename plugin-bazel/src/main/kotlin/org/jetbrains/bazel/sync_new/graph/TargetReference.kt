package org.jetbrains.bazel.sync_new.graph

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetGraph
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import kotlin.reflect.KProperty

interface TargetReference {
  val label: Label

  fun getBuildTarget(): BazelTargetVertex?

  companion object {
    fun ofGraph(label: Label, targetGraph: () -> BazelTargetGraph): TargetReference = object : TargetReference {
      override val label: Label
        get() = label

      override fun getBuildTarget(): BazelTargetVertex? = targetGraph().getVertexByLabel(label)
    }
  }
}

operator fun TargetReference.getValue(thisRef: Any?, property: KProperty<*>): BazelTargetVertex? = getBuildTarget()
