package org.jetbrains.bazel.languages.projectview.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

class ProjectViewElementType(debugName: String) : IElementType(debugName, ProjectViewLanguage)
