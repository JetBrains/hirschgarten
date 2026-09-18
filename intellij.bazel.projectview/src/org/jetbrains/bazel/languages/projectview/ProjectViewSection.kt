package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.extensions.forEachExtensionSafeInline
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

@ApiStatus.Internal
class ProjectViewSection<T>(
  val key: ProjectViewSectionKey<T>,
  val type: ProjectViewSectionType<T & Any>,
  @Nls val documentation: String,
) {

  companion object {

    private val byName: Map<String, ProjectViewSection<*>>
      get() = ProjectViewSectionProvider.EP_NAME.computeIfAbsent(ProjectViewSection::class.java) {
        buildMap {
          ProjectViewSectionProvider.EP_NAME.forEachExtensionSafeInline { provider ->
            provider.sections.forEach { putIfAbsent(it.key.name, it) }
          }
        }
      }

    val allRegistered: Sequence<ProjectViewSection<*>>
      get() = byName.values.asSequence()

    fun findByName(name: String): ProjectViewSection<*>? = byName[name]

    fun findByPsi(psi: PsiElement): ProjectViewSection<*>? {
      val section = psi.parentOfType<ProjectViewPsiSection>(withSelf = true) ?: return null
      return section.getKeyword()
        .text
        .trim()
        .let(::findByName)
    }
  }
}
