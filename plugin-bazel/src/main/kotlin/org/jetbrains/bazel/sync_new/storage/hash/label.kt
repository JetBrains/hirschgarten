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
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.PackageType
import org.jetbrains.bazel.label.RelativeLabel
import org.jetbrains.bazel.label.RepoType
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.label.SyntheticLabel
import org.jetbrains.bazel.label.TargetType

internal fun HashStream128.putLabel(label: Label) {
  when (label) {
    is RelativeLabel -> {
      putByte(1)
      hashLabelPackage(label.packagePath)
      hashLabelTarget(label.target)
    }
    is ResolvedLabel -> {
      putByte(2)
      putResolvedLabel(label)
    }
    is SyntheticLabel -> {
      putByte(3)
      hashLabelTarget(label.target)
      hashLabelPackage(label.packagePath)
    }
  }
}

internal fun HashStream128.putResolvedLabel(label: ResolvedLabel) {
  hashLabelRepo(label.repo)
  hashLabelPackage(label.packagePath)
  hashLabelTarget(label.target)
}

private fun HashSink.hashLabelRepo(repo: RepoType) {
  when (repo) {
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

private fun HashSink.hashLabelPackage(packagePath: PackageType) {
  when (packagePath) {
    is AllPackagesBeneath -> putByte(0)
    is Package -> putByte(1)
  }
  for (string in packagePath.pathSegments) {
    putByteArray(string.toByteArray())
  }
  putInt(packagePath.pathSegments.size)
}

private fun HashSink.hashLabelTarget(target: TargetType) {
  when (target) {
    AmbiguousEmptyTarget -> putByte(0)
    AllRuleTargets -> putByte(1)
    AllRuleTargetsAndFiles -> putByte(2)
    is SingleTarget -> {
      putByte(3)
      putByteArray(target.targetName.toByteArray())
    }
  }
}

