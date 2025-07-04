package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.BspMappings
import org.jetbrains.bazel.server.sync.languages.android.AndroidModule
import org.jetbrains.bazel.server.sync.languages.android.KotlinAndroidModulesMerger

/**
 * Every Kotlin Android target in rules_kotlin actually produces three targets, which we merge inside [KotlinAndroidModulesMerger].
 * However, in order for all the dependent libraries to be unpacked properly (e.g. for Jetpack Compose preview),
 * we still have to pass the dependent Kotlin target explicitly during build (and not just the merged target).
 */
class AdditionalAndroidBuildTargetsProvider(private val projectProvider: ProjectProvider) {
  suspend fun getAdditionalBuildTargets(targets: List<CanonicalLabel>): List<CanonicalLabel> {
    val project = projectProvider.get() as? AspectSyncProject ?: return emptyList()
    val modules = BspMappings.getModules(project, targets)
    return modules
      .mapNotNull { (it.languageData as? AndroidModule)?.correspondingKotlinTarget }
  }
}
