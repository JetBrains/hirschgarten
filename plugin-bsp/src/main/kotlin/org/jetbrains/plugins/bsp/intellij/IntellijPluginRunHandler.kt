// Suppress to be able to import org.jetbrains.idea.devkit.run.loadProductInfo
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.plugins.bsp.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SlowOperations
import org.jetbrains.idea.devkit.DevKitBundle
import org.jetbrains.idea.devkit.projectRoots.IdeaJdk
import org.jetbrains.idea.devkit.projectRoots.Sandbox
import org.jetbrains.idea.devkit.run.IdeaLicenseHelper
import org.jetbrains.idea.devkit.run.loadProductInfo
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandler
import java.io.File
import java.io.IOException
import java.nio.file.Path

internal val INTELLIJ_PLUGIN_SANDBOX_KEY: Key<Path> = Key.create("INTELLIJ_PLUGIN_SANDBOX_KEY")
private const val INTELLIJ_PLUGIN_TAG = "intellij-plugin"

public class IntellijPluginRunHandler : BspRunHandler {
  override fun canRun(targets: List<BuildTargetInfo>): Boolean = targets.all { it.tags.contains(INTELLIJ_PLUGIN_TAG) }

  override fun getBeforeRunTasks(configuration: BspRunConfigurationBase): List<BeforeRunTask<*>> =
    listOf(
      BuildPluginBeforeRunTaskProvider().createTask(configuration),
      CopyPluginToSandboxBeforeRunTaskProvider().createTask(configuration),
    ).map { checkNotNull(it) { "Couldn't create before run task" } }

  // Mostly copied from org.jetbrains.idea.devkit.run.PluginRunConfiguration
  override fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    configuration: BspRunConfigurationBase,
  ): RunProfileState {
    val ideaJdk =
      findNewestIdeaJdk()
        ?: throw ExecutionException(BspPluginBundle.message("console.task.exception.no.intellij.platform.plugin.sdk"))

    var sandboxHome =
      (ideaJdk.sdkAdditionalData as Sandbox).sandboxHome
        ?: throw ExecutionException(DevKitBundle.message("sandbox.no.configured"))

    try {
      sandboxHome = File(sandboxHome).canonicalPath
    } catch (e: IOException) {
      throw ExecutionException(DevKitBundle.message("sandbox.no.configured"))
    }
    val canonicalSandbox = sandboxHome

    environment.putUserData(INTELLIJ_PLUGIN_SANDBOX_KEY, Path.of(canonicalSandbox).resolve("plugins"))

    // copy license from running instance of idea
    IdeaLicenseHelper.copyIDEALicense(sandboxHome)

    return object : JavaCommandLineState(environment) {
      override fun createJavaParameters(): JavaParameters {
        val ideaJdkHome = checkNotNull(ideaJdk.homePath)

        val params = JavaParameters()

        val vm = params.vmParametersList

        // TODO add parameters from run config UI here

        vm.defineProperty(PathManager.PROPERTY_CONFIG_PATH, canonicalSandbox + File.separator + "config")
        vm.defineProperty(PathManager.PROPERTY_SYSTEM_PATH, canonicalSandbox + File.separator + "system")
        vm.defineProperty(PathManager.PROPERTY_PLUGINS_PATH, canonicalSandbox + File.separator + "plugins")

        if (!vm.hasProperty("jdk.module.illegalAccess.silent")) {
          vm.defineProperty("jdk.module.illegalAccess.silent", "true")
        }

        // use product-info.json values if found, otherwise fallback to defaults
        val productInfo = loadProductInfo(ideaJdkHome) ?: throw ExecutionException("IDEA product info is null")
        productInfo.additionalJvmArguments.forEach(vm::add)

        if (SystemInfo.isMac) {
          vm.defineProperty("apple.awt.fileDialogForDirectories", "true")
        }

        vm.defineProperty(SlowOperations.IDEA_PLUGIN_SANDBOX_MODE, "true")

        params.workingDirectory = ideaJdkHome + File.separator + "bin" + File.separator
        params.setJdk(ideaJdk)

        for (path in productInfo.bootClassPathJarNames) {
          params.classPath.add(ideaJdkHome + FileUtil.toSystemDependentName("/lib/$path"))
        }

        params.classPath.addFirst((ideaJdk.sdkType as JavaSdkType).getToolsPath(ideaJdk))

        params.mainClass = "com.intellij.idea.Main"

        return params
      }
    }
  }

  // TODO select Idea JDK via run config UI
  private fun findNewestIdeaJdk(): Sdk? {
    val jdkType = IdeaJdk.getInstance()
    val jdks = ProjectJdkTable.getInstance().getSdksOfType(jdkType)
    return jdks.maxWithOrNull(jdkType.comparator)
  }
}
