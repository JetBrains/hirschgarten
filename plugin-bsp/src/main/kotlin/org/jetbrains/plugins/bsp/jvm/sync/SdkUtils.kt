package org.jetbrains.plugins.bsp.jvm.sync

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToBaseJdkName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import kotlin.io.path.toPath

object SdkUtils {
  suspend fun addJdkIfNeeded(projectName: String, javaHomeUri: String) {
    val jdkName = projectName.projectNameToJdkName(javaHomeUri)
    val path = javaHomeUri.safeCastToURI().toPath().toString()
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

  suspend fun cleanUpInvalidJdks(projectName: String) {
    val sdkTable = ProjectJdkTable.getInstance()
    val jdkPrefix = projectName.projectNameToBaseJdkName()
    getAllAvailableJdks()
      .filter { it.name.startsWith(jdkPrefix) && !isValidJdk(it) }
      .let { invalidJdks ->
        writeAction {
          invalidJdks.forEach { sdkTable.removeJdk(it) }
        }
      }
  }

  private fun isValidJdk(sdk: Sdk): Boolean {
    val homePath = sdk.homePath ?: return false
    return javaSdkInstance.isValidSdkHome(homePath)
  }

  fun getProjectJdkOrMostRecentJdk(project: Project): Sdk? =
    ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.sdkType == javaSdkInstance }
      ?: getMostRecentJdk()

  private fun getMostRecentJdk(): Sdk? = getAllAvailableJdks().maxWithOrNull(javaSdkInstance.comparator)

  private fun getAllAvailableJdks(): List<Sdk> = ProjectJdkTable.getInstance().getSdksOfType(javaSdkInstance)

  private val javaSdkInstance: JavaSdk
    get() = JavaSdk.getInstance()
}
