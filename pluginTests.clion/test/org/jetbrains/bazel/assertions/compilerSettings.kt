package org.jetbrains.bazel.assertions

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.config.rootDir

internal fun Project.findCompilerSettings(relativePath: String, language: OCLanguageKind = CLanguageKind.CPP): List<OCCompilerSettings> {
  val file = rootDir.findFileByRelativePath(relativePath).assertNotNull()

  val configurations = OCWorkspace.getInstance(this).getConfigurationsForFile(file)
  assertThat(configurations).isNotEmpty()

  return configurations.map { it.getCompilerSettings(language, file) }
}

internal fun Project.findCompilerSetting(relativePath: String, language: OCLanguageKind = CLanguageKind.CPP): OCCompilerSettings {
  val settings = findCompilerSettings(relativePath, language)
  assertThat(settings).hasSize(1)

  return settings.single()
}

internal fun Project.findResolveConfigurations(relativePath: String): List<OCResolveConfiguration> {
  val file = rootDir.findFileByRelativePath(relativePath).assertNotNull()

  val configurations = OCWorkspace.getInstance(this).getConfigurationsForFile(file)
  assertThat(configurations).isNotEmpty()

  return configurations
}

internal fun Project.findResolveConfiguration(relativePath: String): OCResolveConfiguration {
  val configurations = findResolveConfigurations(relativePath)
  assertThat(configurations).hasSize(1)

  return configurations.single()
}
