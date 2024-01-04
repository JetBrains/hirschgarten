package org.jetbrains.plugins.bsp.utils

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.createJavaHomeHash
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import java.net.URI
import kotlin.io.path.toPath

internal object SdkUtils {
  suspend fun addJdkIfNeeded(projectName: String, javaHomeUri: String) {
    val jdkName = projectName.projectNameToJdkName(javaHomeUri.createJavaHomeHash())
    val path = URI.create(javaHomeUri).toPath().toString()
    val jdk = ExternalSystemJdkProvider.getInstance().createJdk(jdkName, path)
    addSdkIfNeeded(jdk)
  }

  suspend fun addSdkIfNeeded(sdk: Sdk) {
    val sdkTable = ProjectJdkTable.getInstance()
    val existingSdk = sdkTable.findJdk(sdk.name, sdk.sdkType.name)
    if (existingSdk == null || existingSdk.homePath != sdk.homePath) {
      writeAction {
        existingSdk?.let { sdkTable.removeJdk(existingSdk) }
        sdkTable.addJdk(sdk)
      }
    }
  }

  fun getProjectJdkOrMostRecentJdk(project: Project): Sdk? =
    ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.sdkType == JavaSdk.getInstance() }
      ?: getMostRecentJdk()

  private fun getMostRecentJdk(): Sdk? {
    val jdkType = JavaSdk.getInstance()
    val jdks = ProjectJdkTable.getInstance().getSdksOfType(jdkType)
    return jdks.maxWithOrNull(jdkType.comparator)
  }
}
