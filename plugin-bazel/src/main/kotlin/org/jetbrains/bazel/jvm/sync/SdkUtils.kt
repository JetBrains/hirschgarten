package org.jetbrains.bazel.jvm.sync

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToBaseJdkName
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import java.nio.file.Path

object SdkUtils {
  suspend fun addJdkIfNeeded(projectName: String, javaHome: Path) {
    val jdkName = projectName.projectNameToJdkName(javaHome)
    // Normalize the JDK path, because some code in the platform compares paths using `startsWith`, e.g.
    // https://github.com/JetBrains/intellij-community/blob/b41a4084da5521effedd334e28896fd9d07410da/java/codeserver/core/src/com/intellij/java/codeserver/core/JpmsModuleAccessInfo.kt#L216
    val path = javaHome.normalize().toString()
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
    return javaSdkInstance.isValidSdkHome(homePath) &&
      (sdk as? ProjectJdkImpl)?.rootProvider?.getUrls(OrderRootType.CLASSES)?.size != 0
  }

  fun getProjectJdkOrMostRecentJdk(project: Project): Sdk? =
    ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.sdkType == javaSdkInstance }
      ?: getMostRecentJdk()

  private fun getMostRecentJdk(): Sdk? = getAllAvailableJdks().maxWithOrNull(javaSdkInstance.comparator)

  private fun getAllAvailableJdks(): List<Sdk> = ProjectJdkTable.getInstance().getSdksOfType(javaSdkInstance)

  private val javaSdkInstance: JavaSdk
    get() = JavaSdk.getInstance()
}
