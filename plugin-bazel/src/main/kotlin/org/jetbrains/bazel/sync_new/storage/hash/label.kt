package org.jetbrains.bazel.sync_new.storage.hash

import com.dynatrace.hash4j.hashing.HashSink
import com.dynatrace.hash4j.hashing.HashStream128
import com.dynatrace.hash4j.hashing.HashValue128
import org.jetbrains.bazel.label.AllPackagesBeneath
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.AllRuleTargetsAndFiles
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget

internal fun HashStream128.putResolvedLabel(label: ResolvedLabel) {
  hashLabelRepo(label)
  hashLabelPackage(label)
  hashLabelTarget(label)
}

private fun HashSink.hashLabelRepo(label: ResolvedLabel) {
  when (val repo = label.repo) {
    Main -> putByte(0)
    is Canonical -> {
      putByte(1)
      putByteArray(repo.repoName.toByteArray())
    }

    is Apparent -> {
      putByte(2)
      putByteArray(repo.repoName.toByteArray())
    }
  }
}

private fun HashSink.hashLabelPackage(label: ResolvedLabel) {
  val packagePath = label.packagePath
  when (packagePath) {
    is AllPackagesBeneath -> putByte(0)
    is Package -> putByte(1)
  }
  for (string in packagePath.pathSegments) {
    putByteArray(string.toByteArray())
  }
  putInt(packagePath.pathSegments.size)
}

private fun HashSink.hashLabelTarget(label: ResolvedLabel) {
  when (val target = label.target) {
    AmbiguousEmptyTarget -> putByte(0)
    AllRuleTargets -> putByte(1)
    AllRuleTargetsAndFiles -> putByte(2)
    is SingleTarget -> {
      putByte(3)
      putByteArray(target.targetName.toByteArray())
    }
  }
}

