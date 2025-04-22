package org.jetbrains.bazel.kotlin.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.base.projectStructure.ModuleSourceRootGroup
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinStatus
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.platform.TargetPlatform

private const val BAZEL_KOTLIN_PROJECT_CONFIGURATOR_UNSUPPORTED_MESSAGE =
  """
  The Bazel configurator should never be called to configure Kotlin,
  this can only be done properly by re-syncing the project.
  """

class BazelKotlinProjectConfigurator : KotlinProjectConfigurator {
  override val name: String = "Bazel Kotlin configurator"

  override val presentableText: String
    get() =
      throw UnsupportedOperationException(BAZEL_KOTLIN_PROJECT_CONFIGURATOR_UNSUPPORTED_MESSAGE)

  override val targetPlatform: TargetPlatform
    get() =
      throw UnsupportedOperationException(BAZEL_KOTLIN_PROJECT_CONFIGURATOR_UNSUPPORTED_MESSAGE)

  override fun changeGeneralFeatureConfiguration(
    module: Module,
    feature: LanguageFeature,
    state: LanguageFeature.State,
    forTests: Boolean,
  ) {
  }

  override fun configure(project: Project, excludeModules: Collection<Module>): Unit =
    throw UnsupportedOperationException(BAZEL_KOTLIN_PROJECT_CONFIGURATOR_UNSUPPORTED_MESSAGE)

  override fun getStatus(moduleSourceRootGroup: ModuleSourceRootGroup): ConfigureKotlinStatus = ConfigureKotlinStatus.CONFIGURED

  override fun isApplicable(module: Module): Boolean = module.project.isBazelProject

  override fun updateLanguageVersion(
    module: Module,
    languageVersion: String?,
    apiVersion: String?,
    requiredStdlibVersion: ApiVersion,
    forTests: Boolean,
  ) {
  }
}
