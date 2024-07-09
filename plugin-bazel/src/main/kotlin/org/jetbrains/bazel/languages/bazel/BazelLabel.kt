package org.jetbrains.bazel.languages.bazel

data class BazelLabel(val repoName: String, val packageName: String, val targetName: String) {
  val qualifiedPackageName = "$repoName//$packageName"
  val qualifiedTargetName = "$qualifiedPackageName:$targetName"

  companion object {
    fun ofString(label: String): BazelLabel {
      val (repoName, packageAndTarget) = label.getRepoNameAndPackageWithTarget()
      val (packageName, targetName) = packageAndTarget.getPackageNameAndTargetName(label)
      return BazelLabel(repoName, packageName, targetName)
    }

    private fun String.getRepoNameAndPackageWithTarget(): Pair<String, String> =
      this.split("//", limit = 2).let { repoAndPackage ->
        if (repoAndPackage.size == 1) Pair("", repoAndPackage[0])
        else Pair(repoAndPackage[0].dropWhile { it == '@' }, repoAndPackage[1])
      }

    private fun String.getPackageNameAndTargetName(label: String): Pair<String, String> =
      this.split(":", limit = 2).let { packageAndTarget ->
        if (packageAndTarget.size == 1)
          if (label.contains("//"))
            Pair(packageAndTarget[0], packageAndTarget[0].split("/").last())
          else
            Pair("", packageAndTarget[0])
        else Pair(packageAndTarget[0], packageAndTarget[1])
      }
  }
}