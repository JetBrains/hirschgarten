package org.jetbrains.bazel.sync_new.graph

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.graph.impl.BazelFastTargetGraph
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex

interface TargetReference {
  val label: Label

  fun getBuildTarget(): BazelTargetVertex?

  companion object {
    fun ofGraphLazy(label: Label, targetGraph: BazelFastTargetGraph): TargetReference = object : TargetReference {
      private val target by lazy { targetGraph.getVertexByLabel(label) }

      override val label: Label
        get() = label

      override fun getBuildTarget(): BazelTargetVertex? = target
    }

    fun ofGraphNow(label: Label, targetGraph: BazelFastTargetGraph): TargetReference = object : TargetReference {
      val target = targetGraph.getVertexByLabel(label)

      override val label: Label
        get() = label

      // if target is not available now, try again
      override fun getBuildTarget(): BazelTargetVertex? = target ?: targetGraph.getVertexByLabel(label)
    }
  }
}
