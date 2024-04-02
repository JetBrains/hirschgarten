package org.jetbrains.plugins.bsp.extension.points

import ch.epfl.scala.bsp4j.BuildTarget
import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.vgo.project.workspaceModel.VgoWorkspaceModelUpdater
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails

public interface GoSdkGetterExtension {
  public suspend fun addGoSdks(
    project: Project,
  )

  public fun calculateAllGoSdkInfos(
    projectDetails: ProjectDetails,
  )

  public fun enableGoSupportForModule(
    module: Module,
  )

  public fun restoreGoModulesRegistry(
    project: Project,
  )
}

private val ep = ExtensionPointName.create<GoSdkGetterExtension>(
  "org.jetbrains.bsp.goSdkGetterExtension",
)

public fun goSdkExtension(): GoSdkGetterExtension? = ep.extensionList.firstOrNull()

public fun goSdkExtensionExists(): Boolean = ep.extensionList.isNotEmpty()

public class GoSdkGetter: GoSdkGetterExtension {
  private var goSdks: Set<GoSdk> = emptySet()

  override suspend fun addGoSdks(
    project: Project,
  ) {
    val goSdkService = GoSdkService.getInstance(project)
    goSdks.forEach {
      writeAction {
        goSdkService.setSdk(it)
      }
    }
  }

  override fun calculateAllGoSdkInfos(
    projectDetails: ProjectDetails,
  ) {
    goSdks = projectDetails.targets
      .mapNotNull {
        createGoSdk(it)
      }
      .toSet()
  }

  override fun enableGoSupportForModule(
    module: Module,
  ) {
    GoModuleSettings.getInstance(module).isGoSupportEnabled = true
  }

  override fun restoreGoModulesRegistry(
    project: Project,
  ) {
    VgoWorkspaceModelUpdater(project).restoreModulesRegistry()
  }

  private fun createGoSdk(target: BuildTarget): GoSdk? =
    extractGoBuildTarget(target)?.let {
      if (it.sdkHomePath == null) {
        GoSdk.NULL
      } else {
        GoSdk.fromHomePath(it.sdkHomePath?.path)
      }
    }
}
