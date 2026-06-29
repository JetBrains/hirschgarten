package org.jetbrains.bazel.languages.starlark.bazel

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.autolink.mapExtensionSafe
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class Environment {
  BZL,
  BUILD,
  MODULE,
  REPO,
  VENDOR,
}

@ApiStatus.Internal
data class BazelGlobalFunctionParameter(
  val name: String,
  val doc: String?,
  val defaultValue: String?,
  val named: Boolean,
  val positional: Boolean,
  val required: Boolean,
)

internal fun BazelGlobalFunctionParameter.isKwArgs() = name.startsWith("**")

internal fun BazelGlobalFunctionParameter.isVarArgs() = name.startsWith("*")

@ApiStatus.Internal
data class BazelGlobalFunction(
  val name: String,
  val doc: String?,
  val environment: List<Environment>,
  val params: List<BazelGlobalFunctionParameter>,
  val returnType: String? = null,
)

@ApiStatus.Internal
interface StarlarkGlobalFunctionProvider {

  fun functions(project: Project): List<BazelGlobalFunction>

  companion object {
    val extensionPoint = ExtensionPointName<StarlarkGlobalFunctionProvider>("org.jetbrains.bazel.starlarkGlobalFunctionProvider")
  }
}

@ApiStatus.Internal
class BazelGlobalFunctions {
  companion object {
    // These properties read from the extension point dynamically to support test isolation.
    // Extension points can be masked/replaced during tests, so caching the initial value
    // would cause subsequent tests to see stale data.
    fun globalFunctions(project: Project): Map<String, BazelGlobalFunction> =
      StarlarkGlobalFunctionProvider.extensionPoint
        .mapExtensionSafe { it.functions(project) }
        .flatten()
        .associateBy { it.name }

    internal fun buildGlobalFunctions(project: Project): Map<String, BazelGlobalFunction> =
      globalFunctions(project).filter { it.value.environment.contains(Environment.BUILD) }

    fun moduleGlobalFunctions(project: Project): Map<String, BazelGlobalFunction> =
      globalFunctions(project).filter { it.value.environment.contains(Environment.MODULE) }

    internal fun extensionGlobalFunctions(project: Project): Map<String, BazelGlobalFunction> =
      globalFunctions(project).filter { it.value.environment.contains(Environment.BZL) }

    internal fun starlarkGlobalFunctions(project: Project): Map<String, BazelGlobalFunction> =
      globalFunctions(project).filter { it.value.environment.containsAll(Environment.entries) }

    fun getFunctionByName(name: String, project: Project): BazelGlobalFunction? = globalFunctions(project)[name]
  }
}
