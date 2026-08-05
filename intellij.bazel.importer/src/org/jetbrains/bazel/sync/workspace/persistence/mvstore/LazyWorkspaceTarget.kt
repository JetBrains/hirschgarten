package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.intellij.util.SlowOperations
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path

internal class LazyWorkspaceTarget(
  override val key: WorkspaceTargetKey,
  override val kind: TargetKind,
  override val baseDirectory: Path,
  override val generatorName: String?,
  override val isManual: Boolean,
  override val isWorkspace: Boolean,
  override val isTestOnly: Boolean,
  override val tags: List<String>,
  override val loaded: TargetLoadOptions,
  private val depsSection: LazyTargetSection<List<DependencyLabel>>,
  private val fileSetsSection: LazyTargetSection<HeavyWorkspaceTarget>,
  private val dataSection: LazyTargetSection<List<BuildTargetData>>,
) : BuildTarget {
  override val dependencies: List<DependencyLabel> get() = depsSection.value

  override val sources: SourceFileCollection get() = fileSetsSection.value.sources
  override val generatedSources: SourceFileCollection get() = fileSetsSection.value.generatedSources
  override val resources: SourceFileCollection get() = fileSetsSection.value.resources

  override val data: List<BuildTargetData> get() = dataSection.value
}

internal class LazyTargetSection<T>(isLoaded: Boolean, loader: () -> T) {
  private val section: Lazy<T> =
    if (isLoaded) {
      // evaluate immediately
      lazyOf(loader())
    }
    else {
      // keep it unloaded until explicitly requested and then cache
      lazy {
        SlowOperations.assertSlowOperationsAreAllowed()
        loader()
      }
    }

  val value: T get() = section.value
}
