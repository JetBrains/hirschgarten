package org.jetbrains.bazel.test.framework.testproject

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import java.nio.file.Path
import kotlin.io.path.Path

class TestRepositoriesBuilder internal constructor() {
  private val canonicalToApparentName = LinkedHashMap<String, String>()
  private val canonicalToLocalPath = LinkedHashMap<String, Path>()

  fun external(canonicalName: String, apparentName: String = canonicalName) {
    canonicalToApparentName[canonicalName] = apparentName
    canonicalToLocalPath.remove(canonicalName)
  }

  fun local(canonicalName: String, path: String, apparentName: String = canonicalName) {
    canonicalToApparentName[canonicalName] = apparentName
    canonicalToLocalPath[canonicalName] = Path(path)
  }

  internal fun build(bazelInfo: BazelInfo): BzlmodRepoMapping {
    val apparentToCanonicalName = linkedMapOf("" to "")
    val canonicalNameToPath = linkedMapOf("" to bazelInfo.workspaceRoot)
    for ((canonicalName, apparentName) in canonicalToApparentName) {
      apparentToCanonicalName[apparentName] = canonicalName
      val localPath = canonicalToLocalPath[canonicalName]
      canonicalNameToPath[canonicalName] =
        if (localPath != null) bazelInfo.workspaceRoot.resolve(localPath) else bazelInfo.externalRepositoryPath(canonicalName)
    }
    return BzlmodRepoMapping(
      canonicalRepoNameToLocalPath = canonicalToLocalPath.toMap(),
      apparentRepoNameToCanonicalName = apparentToCanonicalName,
      canonicalRepoNameToPath = canonicalNameToPath,
      nonLocalCanonicalRepoNames = canonicalToApparentName.keys - canonicalToLocalPath.keys,
    )
  }
}

fun testRepoMapping(bazelInfo: BazelInfo, declare: TestRepositoriesBuilder.() -> Unit = {}): BzlmodRepoMapping =
  TestRepositoriesBuilder().apply(declare).build(bazelInfo)
