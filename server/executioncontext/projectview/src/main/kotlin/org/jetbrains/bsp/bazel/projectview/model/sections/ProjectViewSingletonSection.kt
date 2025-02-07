package org.jetbrains.bsp.bazel.projectview.model.sections

import java.nio.file.Path

sealed class ProjectViewSingletonSection<T>(sectionName: String) : ProjectViewSection(sectionName) {
  abstract val value: T
}

data class ProjectViewDeriveTargetsFromDirectoriesSection(override val value: Boolean) :
  ProjectViewSingletonSection<Boolean>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "derive_targets_from_directories"
  }
}

data class ProjectViewBazelBinarySection(override val value: Path) : ProjectViewSingletonSection<Path>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "bazel_binary"
  }
}

data class ProjectViewAllowManualTargetsSyncSection(override val value: Boolean) : ProjectViewSingletonSection<Boolean>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "allow_manual_targets_sync"
  }
}

data class ProjectViewIdeJavaHomeOverrideSection(override val value: Path) : ProjectViewSingletonSection<Path>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "ide_java_home_override"
  }
}

data class ProjectViewImportDepthSection(override val value: Int) : ProjectViewSingletonSection<Int>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "import_depth"
  }
}

data class ExperimentalAddTransitiveCompileTimeJarsSection(override val value: Boolean) :
  ProjectViewSingletonSection<Boolean>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "experimental_add_transitive_compile_time_jars"
  }
}

data class EnableNativeAndroidRulesSection(override val value: Boolean) : ProjectViewSingletonSection<Boolean>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "enable_native_android_rules"
  }
}

data class AndroidMinSdkSection(override val value: Int) : ProjectViewSingletonSection<Int>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "android_min_sdk"
  }
}

data class ShardSyncSection(override val value: Boolean) : ProjectViewSingletonSection<Boolean>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "shard_sync"
  }
}

data class TargetShardSizeSection(override val value: Int) : ProjectViewSingletonSection<Int>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "target_shard_size"
  }
}

data class ShardingApproachSection(override val value: String) : ProjectViewSingletonSection<String>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "shard_approach"
  }
}
