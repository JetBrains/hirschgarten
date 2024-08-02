package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.base.projectStructure.ModuleSourceRootGroup
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinStatus
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.plugins.bsp.config.isBspProject

private const val BSP_KOTLIN_PROJECT_CONFIGURATOR_UNSUPPORTED_MESSAGE =
  """
  The BSP configurator should never be called to configure Kotlin,
  this can only be done properly by re-syncing the project.
  """

class BspKotlinProjectConfigurator : KotlinProjectConfigurator {
  override val name: String = "BSP Kotlin configurator"

  override val presentableText: String
    get() =
      throw UnsupportedOperationException(BSP_KOTLIN_PROJECT_CONFIGURATOR_UNSUPPORTED_MESSAGE)

  override val targetPlatform: TargetPlatform
    get() =
      throw UnsupportedOperationException(BSP_KOTLIN_PROJECT_CONFIGURATOR_UNSUPPORTED_MESSAGE)

  override fun changeGeneralFeatureConfiguration(
    module: Module,
    feature: LanguageFeature,
    state: LanguageFeature.State,
    forTests: Boolean,
  ) {}

  override fun configure(project: Project, excludeModules: Collection<Module>): Unit =
    throw UnsupportedOperationException(BSP_KOTLIN_PROJECT_CONFIGURATOR_UNSUPPORTED_MESSAGE)

  override fun getStatus(moduleSourceRootGroup: ModuleSourceRootGroup): ConfigureKotlinStatus = ConfigureKotlinStatus.CONFIGURED

  override fun isApplicable(module: Module): Boolean = module.project.isBspProject

  override fun updateLanguageVersion(
    module: Module,
    languageVersion: String?,
    apiVersion: String?,
    requiredStdlibVersion: ApiVersion,
    forTests: Boolean,
  ) {}
}
