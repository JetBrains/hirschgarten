package org.jetbrains.bazel.sync.workspace.persistence

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KClass

@ApiStatus.Internal
interface WorkspaceTypeContributor {
  fun contribute(project: Project): List<WorkspaceTypeEntry>

  companion object {
    val EP_NAME: ExtensionPointName<WorkspaceTypeContributor> = ExtensionPointName("org.jetbrains.bazel.workspaceTypeContributor")
  }
}

@ApiStatus.Internal
data class WorkspaceTypeEntry(val fqn: String, val type: Class<*>)

@ApiStatus.Internal
fun <T : Any> MutableList<WorkspaceTypeEntry>.type(type: KClass<T>) {
  this += WorkspaceTypeEntry(fqn = type.qualifiedName ?: error("`${type}` have no valid qualified name"), type.java)
}

@ApiStatus.Internal
inline fun <reified T : Any> MutableList<WorkspaceTypeEntry>.type(): Unit = this.type(T::class)

@ApiStatus.Internal
inline fun <reified T : Any> MutableList<WorkspaceTypeEntry>.sealed() {
  require(T::class.isSealed) { "type ${T::class} must be sealed" }
  type<T>()
  T::class.sealedSubclasses.forEach { type(it) }
}
