package org.jetbrains.bazel.sync_new.internal

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.intellij.util.text.SemVer
import org.jetbrains.bazel.languages.bazelversion.psi.toSemVer
import org.jetbrains.bazel.sync_new.flow.SyncStoreService

@Service(Service.Level.PROJECT)
class InternalsBridgeService(private val project: Project) {
  private val _bridge = SynchronizedClearableLazy {
    val metadata = project.service<SyncStoreService>()
    val version = metadata.syncMetadata.get().bazelVersion
    InternalsBridge.create(version = version?.toSemVer())
  }

  val bridge: InternalsBridge
    get() = _bridge.value

  fun clear() {
    _bridge.drop()
  }
}

interface InternalsBridge {
  fun createStarlarkParser(): StarlarkParser

  companion object {
    fun create(version: SemVer? = null): InternalsBridge {
      return when {
        // use latest impl
        version == null -> InternalsBridgeImpl_v8_4_2()

        // per-version impl
        version.major == 8 -> InternalsBridgeImpl_v8_4_2()

        // fallback
        else -> InternalsBridgeImpl_v8_4_2()
      }
    }
  }
}

interface StarlarkParser {
  fun parse(content: String, file: String): StarlarkSyntaxFile?
}

interface StarlarkSyntaxFile {
  val stmts: List<StarlarkSyntaxStmt>
}

sealed interface StarlarkSyntaxStmt {
  interface LoadStmt : StarlarkSyntaxStmt {
    val import: String
  }
}
