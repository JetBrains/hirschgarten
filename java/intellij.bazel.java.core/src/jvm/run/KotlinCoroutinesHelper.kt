package org.jetbrains.bazel.jvm.run

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

// TODO: remove this, it's only being used to get rid of
//   `intellij.kotlin.jvm.debugger.coroutines` dependency from `bazel.java.common`
@ApiStatus.Internal
interface KotlinCoroutinesHelper {
  fun attachCoroutinesDebuggerConnection(project: Project, runConfiguration: RunConfigurationBase<*>)

  companion object {
    val ep: ExtensionPointName<KotlinCoroutinesHelper> = ExtensionPointName.create("org.jetbrains.bazel.coroutinesHelper")
  }
}
