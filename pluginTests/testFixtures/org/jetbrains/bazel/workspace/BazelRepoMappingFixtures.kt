package org.jetbrains.bazel.workspace

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import java.nio.file.Path

class TestRepoMappingBuilder internal constructor() {
  private val canonicalRepoNameToPath = mutableMapOf<String, Path>()
  private val apparentRepoNameToCanonicalName = mutableMapOf<String, String>()

  fun mainRepo(path: Path) {
    repo(canonicalName = "", path = path, apparentName = "")
  }

  fun repo(canonicalName: String, path: Path, apparentName: String = canonicalName) {
    canonicalRepoNameToPath[canonicalName] = path
    apparentRepoNameToCanonicalName[apparentName] = canonicalName
  }

  internal fun build(): BzlmodRepoMapping =
    BzlmodRepoMapping(
      canonicalRepoNameToLocalPath = canonicalRepoNameToPath.toMap(),
      apparentRepoNameToCanonicalName = apparentRepoNameToCanonicalName.toMap(),
      canonicalRepoNameToPath = canonicalRepoNameToPath.toMap(),
      nonLocalCanonicalRepoNames = emptySet(),
    )
}

@TestOnly
fun Project.setRepoMapping(builder: TestRepoMappingBuilder.() -> Unit) {
  val repoMapping = TestRepoMappingBuilder().apply(builder).build()
  runBlocking {
    service<WorkspaceSnapshotService>().update { snapshot -> snapshot.copy(repoMapping = repoMapping) to Unit }
  }
}
