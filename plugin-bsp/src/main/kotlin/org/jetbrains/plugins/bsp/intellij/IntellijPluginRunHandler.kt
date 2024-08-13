package org.jetbrains.plugins.bsp.intellij

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.PathManager
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
import org.jetbrains.idea.devkit.run.resolveIdeHomeVariable
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.Path

internal val INTELLIJ_PLUGIN_SANDBOX_KEY: Key<Path> = Key.create("INTELLIJ_PLUGIN_SANDBOX_KEY")

class IntellijPluginRunHandler(private val configuration: BspRunConfiguration) : BspRunHandler {
  init {
    configuration.beforeRunTasks =
      listOfNotNull(
        BuildPluginBeforeRunTaskProvider().createTask(configuration),
        CopyPluginToSandboxBeforeRunTaskProvider().createTask(configuration),
      )
  }

  override val state: IntellijPluginRunHandlerState = IntellijPluginRunHandlerState()

  override val name: String = "IntelliJ Plugin Run Handler"

  // Mostly copied from org.jetbrains.idea.devkit.run.PluginRunConfiguration
  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val ideaJdk =
      state.intellijSdkName?.let { findIdeaJdk(it) } ?: throw ExecutionException(
        BspPluginBundle.message(
          "console.task.exception.no.intellij.platform.plugin.sdk",
        ),
      )

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
        fillParameterList(params.programParametersList, state.programArguments)

        val vm = params.vmParametersList

        fillParameterList(vm, state.javaVmOptions)

        vm.defineProperty(PathManager.PROPERTY_CONFIG_PATH, canonicalSandbox + File.separator + "config")
        vm.defineProperty(PathManager.PROPERTY_SYSTEM_PATH, canonicalSandbox + File.separator + "system")
        vm.defineProperty(PathManager.PROPERTY_PLUGINS_PATH, canonicalSandbox + File.separator + "plugins")

        if (!vm.hasProperty("jdk.module.illegalAccess.silent")) {
          vm.defineProperty("jdk.module.illegalAccess.silent", "true")
        }

        // use product-info.json values if found, otherwise fallback to defaults
        val productInfo = loadProductInfo(ideaJdkHome) ?: throw ExecutionException("IDEA product info is null")
        productInfo.getCurrentLaunch().additionalJvmArguments.forEach { item ->
          vm.add(resolveIdeHomeVariable(item, ideaJdkHome))
        }

        if (SystemInfo.isMac) {
          vm.defineProperty("apple.awt.fileDialogForDirectories", "true")
        }

        vm.defineProperty(SlowOperations.IDEA_PLUGIN_SANDBOX_MODE, "true")

        params.workingDirectory = ideaJdkHome + File.separator + "bin" + File.separator
        params.jdk = ideaJdk

        for (path in productInfo.getCurrentLaunch().bootClassPathJarNames) {
          params.classPath.add(ideaJdkHome + FileUtil.toSystemDependentName("/lib/$path"))
        }

        for (moduleJarPath in productInfo.getProductModuleJarPaths()) {
          params.classPath.add(ideaJdkHome + FileUtil.toSystemIndependentName("/$moduleJarPath"))
        }

        params.classPath.addFirst((ideaJdk.sdkType as JavaSdkType).getToolsPath(ideaJdk))

        params.mainClass = "com.intellij.idea.Main"

        return params
      }
    }
  }

  private fun fillParameterList(parameterList: ParametersList, parameters: String?) {
    if (!parameters.isNullOrEmpty()) {
      for (parameter in parameters.split(" ")) {
        if (parameter.isNotEmpty()) {
          parameterList.add(parameter)
        }
      }
    }
  }

  private fun findIdeaJdk(name: String): Sdk? {
    val jdkType = IdeaJdk.getInstance()
    val jdks = ProjectJdkTable.getInstance().getSdksOfType(jdkType)
    return jdks.firstOrNull { it.name == name }
  }
}
