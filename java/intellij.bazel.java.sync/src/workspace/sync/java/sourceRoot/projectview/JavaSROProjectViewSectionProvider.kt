package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview

import org.jetbrains.bazel.config.BazelJavaBackendBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionProvider
import org.jetbrains.bazel.languages.projectview.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionType
import org.jetbrains.bazel.languages.projectview.list

internal class JavaSROProjectViewSectionProvider : ProjectViewSectionProvider {
  override val sections: List<ProjectViewSection<*>> = listOf(
    JavaSROEnableSection,
    JavaSROPatternsSection,
  )
}

private val JavaSROEnableSection: ProjectViewSection<Boolean> = ProjectViewSection(
  key = JAVA_SOURCE_ROOT_OPTIMIZATION_KEY,
  type = ProjectViewSectionType.boolean,
  documentation = BazelJavaBackendBundle.message("bazel.language.projectview.docs.java_source_root_optimization"),
)

private val JavaSROPatternsSection: ProjectViewSection<List<String>> = ProjectViewSection(
  key = JAVA_SOURCE_ROOT_OPTIMIZATION_PATTERNS_KEY,
  type = ProjectViewSectionType.string().list(),
  documentation = BazelJavaBackendBundle.message("bazel.language.projectview.docs.java_source_root_optimization_patterns"),
)
