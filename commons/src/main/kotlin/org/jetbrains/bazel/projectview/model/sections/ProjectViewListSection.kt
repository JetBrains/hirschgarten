package org.jetbrains.bazel.projectview.model.sections

sealed class ProjectViewListSection<T>(sectionName: String) : ProjectViewSection(sectionName) {
  abstract val values: List<T>
}

data class ProjectViewBuildFlagsSection(override val values: List<String>) : ProjectViewListSection<String>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "build_flags"
  }
}

data class ProjectViewSyncFlagsSection(override val values: List<String>) : ProjectViewListSection<String>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "sync_flags"
  }
}

data class ProjectViewDebugFlagsSection(override val values: List<String>) : ProjectViewListSection<String>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "debug_flags"
  }
}

data class ProjectViewEnabledRulesSection(override val values: List<String>) : ProjectViewListSection<String>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "enabled_rules"
  }
}

data class ImportRunConfigurationsSection(override val values: List<String>) : ProjectViewListSection<String>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "import_run_configurations"
  }
}

data class PythonCodeGeneratorRuleNamesSection(override val values: List<String>) : ProjectViewListSection<String>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "python_code_generator_rule_names"
  }
}
