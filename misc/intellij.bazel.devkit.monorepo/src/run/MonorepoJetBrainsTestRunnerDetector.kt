package com.intellij.bazel.devkit.monorepo.run

import com.intellij.monorepo.devkit.bazel.BazelTargetsInfo
import com.intellij.monorepo.devkit.bazel.BazelTargetsInfoCache
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.jvm.run.JetBrainsTestRunner
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.languages.starlark.repomapping.toApparentLabel

internal class MonorepoJetBrainsTestRunnerDetector : JetBrainsTestRunner.Detector {
  override fun usesJetBrainsTestRunner(project: Project, label: Label): Boolean {
    if (!MonorepoRunLineMarkerContributorUtil.isProjectApplicable(project)) return false
    if (label.target is AllRuleTargets) return true  // Wildcards are not in BazelTargetsInfoCache, best effort guess
    val targetsInfo = BazelTargetsInfoCache.getInstance(project).targetsInfo
    return targetsInfo.filePresent && targetsInfo.hasTestTarget(project, label)
  }
}

private fun BazelTargetsInfo.hasTestTarget(project: Project, label: Label): Boolean {
  val entry = label.toTargetsFileEntry(project) ?: return false
  return getAllModules().any { module ->
    getModuleDescription(module).testTargets.contains(entry)
  }
}

private fun Label.toTargetsFileEntry(project: Project): String? {
  val label = toApparentLabel(project) ?: return null
  val repoPrefix = if (label.repo is Main) "" else label.repo.toString()
  return "$repoPrefix//${label.packagePath}:${label.targetName}${MonorepoRunLineMarkerContributorUtil.JAR_SUFFIX}"
}
