package org.jetbrains.bazel.sdkcompat

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator

interface ProjectViewNodeDecoratorCompat : ProjectViewNodeDecorator {
  fun decorateCompat(node: ProjectViewNode<*>?, data: PresentationData?)

  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    decorateCompat(node, data)
  }
}
