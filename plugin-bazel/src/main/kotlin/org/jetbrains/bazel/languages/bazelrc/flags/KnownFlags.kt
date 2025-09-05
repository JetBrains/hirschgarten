package org.jetbrains.bazel.languages.bazelrc.flags

internal object KnownFlags {
  @Option(
    name = "action_cache",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Dump action cache content.",
    valueHelp = "a boolean",
    commands = ["dump"],
  )
  @JvmField
  @Suppress("unused")
  val actionCache = Flag.Boolean("actionCache")

  @Option(
    name = "action_cache_store_output_metadata",
    oldName = "experimental_action_cache_store_output_metadata",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "no-op",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val actionCacheStoreOutputMetadata = Flag.Boolean("actionCacheStoreOutputMetadata")

  @Option(
    name = "action_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """
      Specifies the set of environment variables available to actions with target configuration. Variables can be
      either specified by name, in which case the value will be taken from the invocation environment, or by the
      name=value pair which sets the value independent of the invocation environment. This option can be used
      multiple times; for options given for the same variable, the latest wins, options for different variables
      accumulate.
    """,
    valueHelp = "a 'name=value' assignment with an optional value part",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val actionEnv = Flag.Unknown("actionEnv")

  @Option(
    name = "adb",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      adb binary to use for the 'mobile-install' command. If unspecified, the one in the Android SDK specified by
      the --android_sdk_channel command line option (or  the default SDK if --android_sdk_channel is not specified)
      is used.
    """,
    valueHelp = "a string",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val adb = Flag.Str("adb")

  @Option(
    name = "adb_arg",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = "Extra arguments to pass to adb. Usually used to designate a device to install to.",
    valueHelp = "a string",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val adbArg = Flag.Str("adbArg")

  @Option(
    name = "all",
    defaultValue = "false",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      Fetches all external repositories necessary for building any target or repository. This is the default if no
      other flags and arguments are provided. Only works when --enable_bzlmod is on.
    """,
    valueHelp = "a boolean",
    commands = ["fetch"],
  )
  @JvmField
  @Suppress("unused")
  val all = Flag.Boolean("all")

  @Option(
    name = "allow_analysis_cache_discard",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      If discarding the analysis cache due to a change in the build system, setting this option to false will cause
      bazel to exit, rather than continuing with the build. This option has no effect when 'discard_analysis_cache'
      is also set.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val allowAnalysisCacheDiscard = Flag.Boolean("allowAnalysisCacheDiscard")

  @Option(
    name = "allow_analysis_failures",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, an analysis failure of a rule target results in the target's propagation of an instance of
      AnalysisFailureInfo containing the error description, instead of resulting in a build failure.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val allowAnalysisFailures = Flag.Boolean("allowAnalysisFailures")

  @Option(
    name = "allow_local_tests",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION],
    help = "If true, Bazel will allow local tests to run.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val allowLocalTests = Flag.Boolean("allowLocalTests")

  @Option(
    name = "allow_unresolved_symlinks",
    oldName = "experimental_allow_unresolved_symlinks",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      If enabled, Bazel allows the use of ctx.action.declare_symlink() and the use of ctx.actions.symlink() without
      a target file, thus allowing the creation of unresolved symlinks. Unresolved symlinks inside tree artifacts
      are not currently supported.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val allowUnresolvedSymlinks = Flag.Boolean("allowUnresolvedSymlinks")

  @Option(
    name = "allow_yanked_versions",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Specified the module versions in the form of `<module1>@<version1>,<module2>@<version2>` that will be allowed
      in the resolved dependency graph even if they are declared yanked in the registry where they come from (if
      they are not coming from a NonRegistryOverride). Otherwise, yanked versions will cause the resolution to
      fail. You can also define allowed yanked version with the `BZLMOD_ALLOW_YANKED_VERSIONS` environment
      variable. You can disable this check by using the keyword 'all' (not recommended).
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val allowYankedVersions = Flag.Str("allowYankedVersions")

  @Option(
    name = "allowed_cpu_values",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Allowed values for the --cpu flag.",
    valueHelp = "comma-separated set of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val allowedCpuValues = Flag.Unknown("allowedCpuValues")

  @Option(
    name = "allowed_local_actions_regex",
    help = """
      A regex whitelist for action types which may be run locally. If unset, all actions are allowed to execute
      locally
    """,
    valueHelp = "a valid Java regular expression",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val allowedLocalActionsRegex = Flag.Unknown("allowedLocalActionsRegex")

  @Option(
    name = "always_profile_slow_operations",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "Whether profiling slow operations is always turned on",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val alwaysProfileSlowOperations = Flag.Boolean("alwaysProfileSlowOperations")

  @Option(
    name = "analysis_testing_deps_limit",
    defaultValue = "2000",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Sets the maximum number of transitive dependencies through a rule attribute with a for_analysis_testing
      configuration transition. Exceeding this limit will result in a rule error.
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val analysisTestingDepsLimit = Flag.Integer("analysisTestingDepsLimit")

  @Option(
    name = "analyze",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Execute the loading/analysis phase; this is the usual behaviour. Specifying --noanalyzecauses the build to
      stop before starting the loading/analysis phase, just doing target pattern parsing and returning zero iff
      that completed successfully; this mode is useful for testing.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val analyze = Flag.Boolean("analyze")

  @Option(
    name = "android_compiler",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    help = "The Android target compiler.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidCompiler = Flag.Str("androidCompiler")

  @Option(
    name = "android_cpu",
    defaultValue = "",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidCpu = Flag.Str("androidCpu")

  @Option(
    name = "android_crosstool_top",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidCrosstoolTop = Flag.Str("androidCrosstoolTop")

  @Option(
    name = "android_databinding_use_androidx",
    defaultValue = "true",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Generate AndroidX-compatible data-binding files. This is only used with databinding v2. This flag is a no-op.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidDatabindingUseAndroidx = Flag.Boolean("androidDatabindingUseAndroidx")

  @Option(
    name = "android_databinding_use_v3_4_args",
    defaultValue = "true",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Use android databinding v2 with 3.4.0 argument. This flag is a no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidDatabindingUseV3_4Args = Flag.Boolean("androidDatabindingUseV3_4Args")

  @Option(
    name = "android_dynamic_mode",
    defaultValue = "off",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Determines whether C++ deps of Android rules will be linked dynamically when a cc_binary does not explicitly
      create a shared library. 'default' means bazel will choose whether to link dynamically.  'fully' means all
      libraries will be linked dynamically. 'off' means that all libraries will be linked in mostly static mode.
    """,
    valueHelp = "off, default or fully",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidDynamicMode = Flag.OneOf("androidDynamicMode")

  @Option(
    name = "android_fixed_resource_neverlinking",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      If true, resources will properly not get propagated through neverlinked libraries. Otherwise, the old
      behavior of propagating those resources if no resource-related attributes are specified in the neverlink
      library will be preserved.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidFixedResourceNeverlinking = Flag.Boolean("androidFixedResourceNeverlinking")

  @Option(
    name = "android_grte_top",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidGrteTop = Flag.Str("androidGrteTop")

  @Option(
    name = "android_manifest_merger",
    defaultValue = "android",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    help = """
      Selects the manifest merger to use for android_binary rules. Flag to help thetransition to the Android
      manifest merger from the legacy merger.
    """,
    valueHelp = "legacy, android or force_android",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidManifestMerger = Flag.OneOf("androidManifestMerger")

  @Option(
    name = "android_manifest_merger_order",
    defaultValue = "alphabetical",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.EXECUTION],
    help = """
      Sets the order of manifests passed to the manifest merger for Android binaries. ALPHABETICAL means manifests
      are sorted by path relative to the execroot. ALPHABETICAL_BY_CONFIGURATION means manifests are sorted by
      paths relative to the configuration directory within the output directory. DEPENDENCY means manifests are
      ordered with each library's manifest coming before the manifests of its dependencies.
    """,
    valueHelp = "alphabetical, alphabetical_by_configuration or dependency",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidManifestMergerOrder = Flag.OneOf("androidManifestMergerOrder")

  @Option(
    name = "android_migration_tag_check",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      If enabled, strict usage of the Starlark migration tag is enabled for android rules. Prefer using
      --incompatible_disable_native_android_rules.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidMigrationTagCheck = Flag.Boolean("androidMigrationTagCheck")

  @Option(
    name = "android_platforms",
    defaultValue = "",
    effectTags = [
      OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    help = """
      Sets the platforms that android_binary targets use. If multiple platforms are specified, then the binary is a
      fat APKs, which contains native binaries for each specified target platform.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidPlatforms = Flag.Label("androidPlatforms")

  @Option(
    name = "android_resource_shrinking",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Enables resource shrinking for android_binary APKs that use ProGuard.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidResourceShrinking = Flag.Boolean("androidResourceShrinking")

  @Option(
    name = "android_sdk",
    defaultValue = "",
    help = "No-op",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val androidSdk = Flag.Str("androidSdk")

  @Option(
    name = "announce",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated. No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val announce = Flag.Boolean("announce")

  @Option(
    name = "announce_rc",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Whether to announce rc options.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val announceRc = Flag.Boolean("announceRc")

  @Option(
    name = "apk_signing_method",
    defaultValue = "v1_v2",
    effectTags = [
      OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    help = "Implementation to use to sign APKs",
    valueHelp = "v1, v2, v1_v2 or v4",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val apkSigningMethod = Flag.OneOf("apkSigningMethod")

  @Option(
    name = "apple_crosstool_top",
    defaultValue = "@bazel_tools//tools/cpp:toolchain",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.CHANGES_INPUTS],
    help = "The label of the crosstool package to be used in Apple and Objc rules and their dependencies.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val appleCrosstoolTop = Flag.Label("appleCrosstoolTop")

  @Option(
    name = "apple_generate_dsym",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.ACTION_COMMAND_LINES],
    help = "Whether to generate debug symbol(.dSYM) file(s).",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val appleGenerateDsym = Flag.Boolean("appleGenerateDsym")

  @Option(
    name = "apple_platform_type",
    defaultValue = "macos",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      Don't set this value from the command line - it is derived from other flags and configuration transitions
      derived from rule attributes
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val applePlatformType = Flag.Str("applePlatformType")

  @Option(
    name = "apple_platforms",
    defaultValue = "",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Comma-separated list of platforms to use when building Apple binaries.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val applePlatforms = Flag.Label("applePlatforms")

  @Option(
    name = "apple_split_cpu",
    defaultValue = "",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      Don't set this value from the command line - it is derived from other flags and configuration transitions
      derived from rule attributes
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val appleSplitCpu = Flag.Str("appleSplitCpu")

  @Option(
    name = "archived_tree_artifact_mnemonics_filter",
    defaultValue = "-.*",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    help = """
      Regex filter for mnemonics of actions for which we should create archived tree artifacts. This option is a
      no-op for actions which do not generate tree artifacts.
    """,
    valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val archivedTreeArtifactMnemonicsFilter = Flag.Unknown("archivedTreeArtifactMnemonicsFilter")

  @Option(
    name = "aspect_deps",
    defaultValue = "conservative",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """
      How to resolve aspect dependencies when the output format is one of {xml,proto,record}. 'off' means no aspect
      dependencies are resolved, 'conservative' (the default) means all declared aspect dependencies are added
      regardless of whether they are given the rule class of direct dependencies, 'precise' means that only those
      aspects are added that are possibly active given the rule class of the direct dependencies. Note that precise
      mode requires loading other packages to evaluate a single target thus making it slower than the other modes.
      Also note that even precise mode is not completely precise: the decision whether to compute an aspect is
      decided in the analysis phase, which is not run during 'bazel query'.
    """,
    valueHelp = "off, conservative or precise",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val aspectDeps = Flag.OneOf("aspectDeps")

  @Option(
    name = "aspects",
    allowMultiple = true,
    help = """
      Comma-separated list of aspects to be applied to top-level targets. In the list, if aspect some_aspect
      specifies required aspect providers via required_aspect_providers, some_aspect will run after every aspect
      that was mentioned before it in the aspects list whose advertised providers satisfy some_aspect required
      aspect providers. Moreover, some_aspect will run after all its required aspects specified by requires
      attribute. some_aspect will then have access to the values of those aspects' providers.
      <bzl-file-label>%<aspect_name>, for example '//tools:my_def.bzl%my_aspect', where 'my_aspect' is a top-level
      value from a file tools/my_def.bzl
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val aspects = Flag.Unknown("aspects")

  @Option(
    name = "aspects_parameters",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Specifies the values of the command-line aspects parameters. Each parameter value is specified via
      <param_name>=<param_value>, for example 'my_param=my_val' where 'my_param' is a parameter of some aspect in
      --aspects list or required by an aspect in the list. This option can be used multiple times. However, it is
      not allowed to assign values to the same parameter more than once.
    """,
    valueHelp = "a 'name=value' assignment",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val aspectsParameters = Flag.Unknown("aspectsParameters")

  @Option(
    name = "async",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      If true, output cleaning is asynchronous. When this command completes, it will be safe to execute new
      commands in the same client, even though the deletion may continue in the background.
    """,
    valueHelp = "a boolean",
    commands = ["clean"],
  )
  @JvmField
  @Suppress("unused")
  val async = Flag.Boolean("async")

  @Option(
    name = "attempt_to_print_relative_paths",
    oldName = "experimental_ui_attempt_to_print_relative_paths",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      When printing the location part of messages, attempt to use a path relative to the workspace directory or one
      of the directories specified by --package_path.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val attemptToPrintRelativePaths = Flag.Boolean("attemptToPrintRelativePaths")

  @Option(
    name = "auto_cpu_environment_group",
    defaultValue = "",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "No-op",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val autoCpuEnvironmentGroup = Flag.Str("autoCpuEnvironmentGroup")

  @Option(
    name = "auto_output_filter",
    defaultValue = "none",
    help = """
      If --output_filter is not specified, then the value for this option is used create a filter automatically.
      Allowed values are 'none' (filter nothing / show everything), 'all' (filter everything / show nothing),
      'packages' (include output from rules in packages mentioned on the Blaze command line), and 'subpackages'
      (like 'packages', but also include subpackages). For the 'packages' and 'subpackages' values //java/foo and
      //javatests/foo are treated as one package)'.
    """,
    valueHelp = "none, all, packages or subpackages",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val autoOutputFilter = Flag.OneOf("autoOutputFilter")

  @Option(
    name = "autodetect_server_javabase",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      When --noautodetect_server_javabase is passed, Bazel does not fall back to the local JDK for running the
      bazel server and instead exits.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val autodetectServerJavabase = Flag.Boolean("autodetectServerJavabase")

  @Option(
    name = "base_module",
    defaultValue = "<root>",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Specify a module relative to which the specified target repos will be interpreted.",
    valueHelp = """
      "<root>" for the root module; <module>@<version> for a specific version of a module; <module> for
      all versions of a module; @<name> for a repo with the given apparent name; or @@<name> for a repo
      with the given canonical name
    """,
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val baseModule = Flag.Unknown("baseModule")

  @Option(
    name = "batch",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.DEPRECATED],
    help = """
      If set, Bazel will be run as just a client process without a server, instead of in the standard client/server
      mode. This is deprecated and will be removed, please prefer shutting down the server explicitly if you wish
      to avoid lingering servers.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val batch = Flag.Boolean("batch")

  @Option(
    name = "batch_cpu_scheduling",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Only on Linux; use 'batch' CPU scheduling for Blaze. This policy is useful for workloads that are
      non-interactive, but do not want to lower their nice value. See 'man 2 sched_setscheduler'. If false, then
      Bazel does not perform a system call.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val batchCpuScheduling = Flag.Boolean("batchCpuScheduling")

  @Option(
    name = "bazelrc",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      The location of the user .bazelrc file containing default values of Bazel options. /dev/null indicates that
      all further `--bazelrc`s will be ignored, which is useful to disable the search for a user rc file, e.g. in
      release builds.
      This option can also be specified multiple times.
      E.g. with `--bazelrc=x.rc --bazelrc=y.rc --bazelrc=/dev/null --bazelrc=z.rc`,
        1) x.rc and y.rc are read.
        2) z.rc is ignored due to the prior /dev/null.
      If unspecified, Bazel uses the first .bazelrc file it finds in the following two locations: the workspace
      directory, then the user's home directory.
      Note: command line options will always supersede any option in bazelrc.
    """,
    valueHelp = "a string",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val bazelrc = Flag.Str("bazelrc")

  @Option(
    name = "bep_maximum_open_remote_upload_files",
    defaultValue = "-1",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Maximum number of open files allowed during BEP artifact upload.",
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val bepMaximumOpenRemoteUploadFiles = Flag.Integer("bepMaximumOpenRemoteUploadFiles")

  @Option(
    name = "bes_backend",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specifies the build event service (BES) backend endpoint in the form [SCHEME://]HOST[:PORT]. The default is
      to disable BES uploads. Supported schemes are grpc and grpcs (grpc with TLS enabled). If no scheme is
      provided, Bazel assumes grpcs.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besBackend = Flag.Str("besBackend")

  @Option(
    name = "bes_best_effort",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besBestEffort = Flag.Boolean("besBestEffort")

  @Option(
    name = "bes_check_preceding_lifecycle_events",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Sets the field check_preceding_lifecycle_events_present on PublishBuildToolEventStreamRequest which tells BES
      to check whether it previously received InvocationAttemptStarted and BuildEnqueued events matching the
      current tool event.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besCheckPrecedingLifecycleEvents = Flag.Boolean("besCheckPrecedingLifecycleEvents")

  @Option(
    name = "bes_header",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specify a header in NAME=VALUE form that will be included in BES requests. Multiple headers can be passed by
      specifying the flag multiple times. Multiple values for the same name will be converted to a comma-separated
      list.
    """,
    valueHelp = "a 'name=value' assignment",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besHeader = Flag.Unknown("besHeader")

  @Option(
    name = "bes_instance_name",
    oldName = "project_id",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Specifies the instance name under which the BES will persist uploaded BEP. Defaults to null.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besInstanceName = Flag.Str("besInstanceName")

  @Option(
    name = "bes_keywords",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specifies a list of notification keywords to be added the default set of keywords published to BES
      ("command_name=<command_name> ", "protocol_name=BEP"). Defaults to none.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besKeywords = Flag.Unknown("besKeywords")

  @Option(
    name = "bes_lifecycle_events",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Specifies whether to publish BES lifecycle events. (defaults to 'true').",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besLifecycleEvents = Flag.Boolean("besLifecycleEvents")

  @Option(
    name = "bes_oom_finish_upload_timeout",
    defaultValue = "10m",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      Specifies how long bazel should wait for the BES/BEP upload to complete while OOMing. This flag ensures
      termination when the JVM is severely GC thrashing and cannot make progress on any user thread.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besOomFinishUploadTimeout = Flag.Duration("besOomFinishUploadTimeout")

  @Option(
    name = "bes_outerr_buffer_size",
    defaultValue = "10240",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specifies the maximal size of stdout or stderr to be buffered in BEP, before it is reported as a progress
      event. Individual writes are still reported in a single event, even if larger than the specified value up to
      --bes_outerr_chunk_size.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besOuterrBufferSize = Flag.Integer("besOuterrBufferSize")

  @Option(
    name = "bes_outerr_chunk_size",
    defaultValue = "1048576",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Specifies the maximal size of stdout or stderr to be sent to BEP in a single message.",
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besOuterrChunkSize = Flag.Integer("besOuterrChunkSize")

  @Option(
    name = "bes_proxy",
    help = """
      Connect to the Build Event Service through a proxy. Currently this flag can only be used to configure a Unix
      domain socket (unix:/path/to/socket).
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besProxy = Flag.Str("besProxy")

  @Option(
    name = "bes_results_url",
    defaultValue = "",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Specifies the base URL where a user can view the information streamed to the BES backend. Bazel will output
      the URL appended by the invocation id to the terminal.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besResultsUrl = Flag.Str("besResultsUrl")

  @Option(
    name = "bes_system_keywords",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specifies a list of notification keywords to be included directly, without the "user_keyword=" prefix
      included for keywords supplied via --bes_keywords. Intended for Build service operators that set
      --bes_lifecycle_events=false and include keywords when calling PublishLifecycleEvent. Build service operators
      using this flag should prevent users from overriding the flag value.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besSystemKeywords = Flag.Unknown("besSystemKeywords")

  @Option(
    name = "bes_timeout",
    defaultValue = "0s",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specifies how long bazel should wait for the BES/BEP upload to complete after the build and tests have
      finished. A valid timeout is a natural number followed by a unit: Days (d), hours (h), minutes (m), seconds
      (s), and milliseconds (ms). The default value is '0' which means that there is no timeout.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besTimeout = Flag.Duration("besTimeout")

  @Option(
    name = "bes_upload_mode",
    defaultValue = "wait_for_upload_complete",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      Specifies whether the Build Event Service upload should block the build completion or should end the
      invocation immediately and finish the upload in the background. Either 'wait_for_upload_complete' (default),
      'nowait_for_upload_complete', or 'fully_async'.
    """,
    valueHelp = "wait_for_upload_complete, nowait_for_upload_complete or fully_async",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val besUploadMode = Flag.OneOf("besUploadMode")

  @Option(
    name = "binary_path",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "The absolute path of the bazel binary.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val binaryPath = Flag.Str("binaryPath")

  @Option(
    name = "block_for_lock",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      When --noblock_for_lock is passed, Bazel does not wait for a running command to complete, but instead exits
      immediately.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val blockForLock = Flag.Boolean("blockForLock")

  @Option(
    name = "break_build_on_parallel_dex2oat_failure",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true dex2oat action failures will cause the build to break instead of executing dex2oat during test
      runtime.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val breakBuildOnParallelDex2oatFailure = Flag.Boolean("breakBuildOnParallelDex2oatFailure")

  @Option(
    name = "build",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Execute the build; this is the usual behaviour. Specifying --nobuild causes the build to stop before
      executing the build actions, returning zero iff the package loading and analysis phases completed
      successfully; this mode is useful for testing those phases.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val build = Flag.Boolean("build")

  @Option(
    name = "build_event_binary_file",
    oldName = "experimental_build_event_binary_file",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If non-empty, write a varint delimited binary representation of representation of the build event protocol to
      that file. This option implies --bes_upload_mode=wait_for_upload_complete.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventBinaryFile = Flag.Str("buildEventBinaryFile")

  @Option(
    name = "build_event_binary_file_path_conversion",
    oldName = "experimental_build_event_binary_file_path_conversion",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Convert paths in the binary file representation of the build event protocol to more globally valid URIs
      whenever possible; if disabled, the file:// uri scheme will always be used
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventBinaryFilePathConversion = Flag.Boolean("buildEventBinaryFilePathConversion")

  @Option(
    name = "build_event_binary_file_upload_mode",
    defaultValue = "wait_for_upload_complete",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      Specifies whether the Build Event Service upload for --build_event_binary_file should block the build
      completion or should end the invocation immediately and finish the upload in the background. Either
      'wait_for_upload_complete' (default), 'nowait_for_upload_complete', or 'fully_async'.
    """,
    valueHelp = "wait_for_upload_complete, nowait_for_upload_complete or fully_async",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventBinaryFileUploadMode = Flag.OneOf("buildEventBinaryFileUploadMode")

  @Option(
    name = "build_event_json_file",
    oldName = "experimental_build_event_json_file",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If non-empty, write a JSON serialisation of the build event protocol to that file. This option implies
      --bes_upload_mode=wait_for_upload_complete.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventJsonFile = Flag.Str("buildEventJsonFile")

  @Option(
    name = "build_event_json_file_path_conversion",
    oldName = "experimental_build_event_json_file_path_conversion",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Convert paths in the json file representation of the build event protocol to more globally valid URIs
      whenever possible; if disabled, the file:// uri scheme will always be used
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventJsonFilePathConversion = Flag.Boolean("buildEventJsonFilePathConversion")

  @Option(
    name = "build_event_json_file_upload_mode",
    defaultValue = "wait_for_upload_complete",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      Specifies whether the Build Event Service upload for --build_event_json_file should block the build
      completion or should end the invocation immediately and finish the upload in the background. Either
      'wait_for_upload_complete' (default), 'nowait_for_upload_complete', or 'fully_async'.
    """,
    valueHelp = "wait_for_upload_complete, nowait_for_upload_complete or fully_async",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventJsonFileUploadMode = Flag.OneOf("buildEventJsonFileUploadMode")

  @Option(
    name = "build_event_max_named_set_of_file_entries",
    defaultValue = "5000",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      The maximum number of entries for a single named_set_of_files event; values smaller than 2 are ignored and no
      event splitting is performed. This is intended for limiting the maximum event size in the build event
      protocol, although it does not directly control event size. The total event size is a function of the
      structure of the set as well as the file and uri lengths, which may in turn depend on the hash function.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventMaxNamedSetOfFileEntries = Flag.Integer("buildEventMaxNamedSetOfFileEntries")

  @Option(
    name = "build_event_publish_all_actions",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Whether all actions should be published.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventPublishAllActions = Flag.Boolean("buildEventPublishAllActions")

  @Option(
    name = "build_event_text_file",
    oldName = "experimental_build_event_text_file",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "If non-empty, write a textual representation of the build event protocol to that file",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventTextFile = Flag.Str("buildEventTextFile")

  @Option(
    name = "build_event_text_file_path_conversion",
    oldName = "experimental_build_event_text_file_path_conversion",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Convert paths in the text file representation of the build event protocol to more globally valid URIs
      whenever possible; if disabled, the file:// uri scheme will always be used
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventTextFilePathConversion = Flag.Boolean("buildEventTextFilePathConversion")

  @Option(
    name = "build_event_text_file_upload_mode",
    defaultValue = "wait_for_upload_complete",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      Specifies whether the Build Event Service upload for --build_event_text_file should block the build
      completion or should end the invocation immediately and finish the upload in the background. Either
      'wait_for_upload_complete' (default), 'nowait_for_upload_complete', or 'fully_async'.
    """,
    valueHelp = "wait_for_upload_complete, nowait_for_upload_complete or fully_async",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventTextFileUploadMode = Flag.OneOf("buildEventTextFileUploadMode")

  @Option(
    name = "build_event_upload_max_retries",
    oldName = "experimental_build_event_upload_max_retries",
    defaultValue = "4",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "The maximum number of times Bazel should retry uploading a build event.",
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildEventUploadMaxRetries = Flag.Integer("buildEventUploadMaxRetries")

  @Option(
    name = "build_manual_tests",
    defaultValue = "false",
    help = """
      Forces test targets tagged 'manual' to be built. 'manual' tests are excluded from processing. This option
      forces them to be built (but not executed).
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildManualTests = Flag.Boolean("buildManualTests")

  @Option(
    name = "build_metadata",
    allowMultiple = true,
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Custom key-value string pairs to supply in a build event.",
    valueHelp = "a 'name=value' assignment",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildMetadata = Flag.Unknown("buildMetadata")

  @Option(
    name = "build_python_zip",
    defaultValue = "auto",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Build python executable zip; on on Windows, off on other platforms",
    valueHelp = "a tri-state (auto, yes, no)",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildPythonZip = Flag.TriState("buildPythonZip")

  @Option(
    name = "build_request_id",
    defaultValue = "",
    effectTags = [OptionEffectTag.BAZEL_MONITORING, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "Unique string identifier for the build being run.",
    valueHelp = "An optionally prefixed UUID. The last 36 characters will be verified as a UUID.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildRequestId = Flag.Unknown("buildRequestId")

  @Option(
    name = "build_runfile_links",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If true, build runfiles symlink forests for all targets.  If false, write them only when required by a local
      action, test or run command.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildRunfileLinks = Flag.Boolean("buildRunfileLinks")

  @Option(
    name = "build_runfile_manifests",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If true, write runfiles manifests for all targets. If false, omit them. Local tests will fail to run when
      false.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildRunfileManifests = Flag.Boolean("buildRunfileManifests")

  @Option(
    name = "build_tag_filters",
    defaultValue = "",
    help = """
      Specifies a comma-separated list of tags. Each tag can be optionally preceded with '-' to specify excluded
      tags. Only those targets will be built that contain at least one included tag and do not contain any excluded
      tags. This option does not affect the set of tests executed with the 'test' command; those are be governed by
      the test filtering options, for example '--test_tag_filters'
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildTagFilters = Flag.Unknown("buildTagFilters")

  @Option(
    name = "build_test_dwp",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If enabled, when building C++ tests statically and with fission the .dwp file  for the test binary will be
      automatically built as well.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildTestDwp = Flag.Boolean("buildTestDwp")

  @Option(
    name = "build_tests_only",
    defaultValue = "false",
    help = """
      If specified, only *_test and test_suite rules will be built and other targets specified on the command line
      will be ignored. By default everything that was requested will be built.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val buildTestsOnly = Flag.Boolean("buildTestsOnly")

  @Option(
    name = "bytecode_optimization_pass_actions",
    defaultValue = "1",
    help = "Do not use.",
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val bytecodeOptimizationPassActions = Flag.Integer("bytecodeOptimizationPassActions")

  @Option(
    name = "cache_computed_file_digests",
    defaultValue = "50000",
    help = """
      If greater than 0, configures Bazel to cache file digests in memory based on their metadata instead of
      recomputing the digests from disk every time they are needed. Setting this to 0 ensures correctness because
      not all file changes can be noted from file metadata. When not 0, the number indicates the size of the cache
      as the number of file digests to be cached.
    """,
    valueHelp = "a long integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val cacheComputedFileDigests = Flag.Unknown("cacheComputedFileDigests")

  @Option(
    name = "cache_test_results",
    abbrev = 't',
    defaultValue = "auto",
    help = """
      If set to 'auto', Bazel reruns a test if and only if: (1) Bazel detects changes in the test or its
      dependencies, (2) the test is marked as external, (3) multiple test runs were requested with --runs_per_test,
      or(4) the test previously failed. If set to 'yes', Bazel caches all test results except for tests marked as
      external. If set to 'no', Bazel does not cache any test results.
    """,
    valueHelp = "a tri-state (auto, yes, no)",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val cacheTestResults = Flag.TriState("cacheTestResults")

  @Option(
    name = "canonicalize_policy",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Output the canonical policy, after expansion and filtering. To keep the output clean, the canonicalized
      command arguments will NOT be shown when this option is set to true. Note that the command specified by
      --for_command affects the filtered policy, and if none is specified, the default command is 'build'.
    """,
    valueHelp = "a boolean",
    commands = ["canonicalize-flags"],
  )
  @JvmField
  @Suppress("unused")
  val canonicalizePolicy = Flag.Boolean("canonicalizePolicy")

  @Option(
    name = "catalyst_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Comma-separated list of architectures for which to build Apple Catalyst binaries.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val catalystCpus = Flag.Unknown("catalystCpus")

  @Option(
    name = "cc_output_directory_tag",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Specifies a suffix to be added to the configuration directory.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val ccOutputDirectoryTag = Flag.Str("ccOutputDirectoryTag")

  @Option(
    name = "cc_proto_library_header_suffixes",
    defaultValue = ".pb.h",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Sets the suffixes of header files that a cc_proto_library creates.",
    valueHelp = "comma-separated set of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val ccProtoLibraryHeaderSuffixes = Flag.Unknown("ccProtoLibraryHeaderSuffixes")

  @Option(
    name = "cc_proto_library_source_suffixes",
    defaultValue = ".pb.cc",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Sets the suffixes of source files that a cc_proto_library creates.",
    valueHelp = "comma-separated set of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val ccProtoLibrarySourceSuffixes = Flag.Unknown("ccProtoLibrarySourceSuffixes")

  @Option(
    name = "charset",
    defaultValue = "utf8",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Chooses the character set to use for the tree. Only affects text output. Valid values are "utf8" or "ascii".
      Default is "utf8"
    """,
    valueHelp = "utf8 or ascii",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val charset = Flag.OneOf("charset")

  @Option(
    name = "check_bazel_compatibility",
    defaultValue = "error",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Check bazel version compatibility of Bazel modules. Valid values are `error` to escalate it to a resolution
      failure, `off` to disable the check, or `warning` to print a warning when mismatch detected.
    """,
    valueHelp = "error, warning or off",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val checkBazelCompatibility = Flag.OneOf("checkBazelCompatibility")

  @Option(
    name = "check_bzl_visibility",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = "If disabled, .bzl load visibility errors are demoted to warnings.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val checkBzlVisibility = Flag.Boolean("checkBzlVisibility")

  @Option(
    name = "check_direct_dependencies",
    defaultValue = "warning",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Check if the direct `bazel_dep` dependencies declared in the root module are the same versions you get in the
      resolved dependency graph. Valid values are `off` to disable the check, `warning` to print a warning when
      mismatch detected or `error` to escalate it to a resolution failure.
    """,
    valueHelp = "off, warning or error",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val checkDirectDependencies = Flag.OneOf("checkDirectDependencies")

  @Option(
    name = "check_fileset_dependencies_recursively",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val checkFilesetDependenciesRecursively = Flag.Boolean("checkFilesetDependenciesRecursively")

  @Option(
    name = "check_licenses",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """
      Check that licensing constraints imposed by dependent packages do not conflict with distribution modes of the
      targets being built. By default, licenses are not checked.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val checkLicenses = Flag.Boolean("checkLicenses")

  @Option(
    name = "check_tests_up_to_date",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Don't run tests, just check if they are up-to-date.  If all tests results are up-to-date, the testing
      completes successfully.  If any test needs to be built or executed, an error is reported and the testing
      fails.  This option implies --check_up_to_date behavior.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val checkTestsUpToDate = Flag.Boolean("checkTestsUpToDate")

  @Option(
    name = "check_up_to_date",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Don't perform the build, just check if it is up-to-date.  If all targets are up-to-date, the build completes
      successfully.  If any step needs to be executed an error is reported and the build fails.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val checkUpToDate = Flag.Boolean("checkUpToDate")

  @Option(
    name = "check_visibility",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = "If disabled, visibility errors in target dependencies are demoted to warnings.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val checkVisibility = Flag.Boolean("checkVisibility")

  @Option(
    name = "client_cwd",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "A system-generated parameter which specifies the client's working directory",
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val clientCwd = Flag.Path("clientCwd")

  @Option(
    name = "client_debug",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    help = """
      If true, log debug information from the client to stderr. Changing this option will not cause the server to
      restart.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val clientDebug = Flag.Boolean("clientDebug")

  @Option(
    name = "client_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "A system-generated parameter which specifies the client's environment",
    valueHelp = "a 'name=value' assignment",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val clientEnv = Flag.Unknown("clientEnv")

  @Option(
    name = "collapse_duplicate_defines",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "no-op",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val collapseDuplicateDefines = Flag.Boolean("collapseDuplicateDefines")

  @Option(
    name = "collect_code_coverage",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If specified, Bazel will instrument code (using offline instrumentation where possible) and will collect
      coverage information during tests. Only targets that  match --instrumentation_filter will be affected.
      Usually this option should  not be specified directly - 'bazel coverage' command should be used instead.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val collectCodeCoverage = Flag.Boolean("collectCodeCoverage")

  @Option(
    name = "color",
    defaultValue = "auto",
    help = "Use terminal controls to colorize output.",
    valueHelp = "yes, no or auto",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val color = Flag.OneOf("color")

  @Option(
    name = "combined_report",
    defaultValue = "none",
    help = "Specifies desired cumulative coverage report type. At this point only LCOV is supported.",
    valueHelp = "none or lcov",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val combinedReport = Flag.OneOf("combinedReport")

  @Option(
    name = "command_port",
    defaultValue = "0",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "Port to start up the gRPC command server on. If 0, let the kernel choose.",
    valueHelp = "an integer",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val commandPort = Flag.Integer("commandPort")

  @Option(
    name = "command_wait_time",
    defaultValue = "0",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "The time in ms a command had to wait on a busy Bazel server process.",
    valueHelp = "a long integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val commandWaitTime = Flag.Unknown("commandWaitTime")

  @Option(
    name = "compilation_mode",
    abbrev = 'c',
    defaultValue = "fastbuild",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.ACTION_COMMAND_LINES],
    help = "Specify the mode the binary will be built in. Values: 'fastbuild', 'dbg', 'opt'.",
    valueHelp = "fastbuild, dbg or opt",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val compilationMode = Flag.OneOf("compilationMode")

  @Option(
    name = "compile_one_dependency",
    defaultValue = "false",
    help = """
      Compile a single dependency of the argument files. This is useful for syntax checking source files in IDEs,
      for example, by rebuilding a single target that depends on the source file to detect errors as early as
      possible in the edit/build/test cycle. This argument affects the way all non-flag arguments are interpreted;
      instead of being targets to build they are source filenames.  For each source filename an arbitrary target
      that depends on it will be built.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val compileOneDependency = Flag.Boolean("compileOneDependency")

  @Option(
    name = "compiler",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    help = "The C++ compiler to use for compiling the target.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val compiler = Flag.Str("compiler")

  @Option(
    name = "config",
    allowMultiple = true,
    help = """
      Selects additional config sections from the rc files; for every <command>, it also pulls in the options from
      <command>:<config> if such a section exists; if this section doesn't exist in any .rc file, Blaze fails with
      an error. The config sections and flag combinations they are equivalent to are located in the tools/*.blazerc
      config files.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val config = Flag.Str("config")

  @Option(
    name = "configure",
    defaultValue = "false",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      Only fetches repositories marked as 'configure' for system-configuration purpose. Only works when
      --enable_bzlmod is on.
    """,
    valueHelp = "a boolean",
    commands = ["fetch", "sync"],
  )
  @JvmField
  @Suppress("unused")
  val configure = Flag.Boolean("configure")

  @Option(
    name = "conlyopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional option to pass to gcc when compiling C source files.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val conlyopt = Flag.Str("conlyopt")

  @Option(
    name = "connect_timeout_secs",
    defaultValue = "30",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "The amount of time the client waits for each attempt to connect to the server",
    valueHelp = "an integer",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val connectTimeoutSecs = Flag.Integer("connectTimeoutSecs")

  @Option(
    name = "consistent_labels",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      If enabled, every query command emits labels as if by the Starlark <code>str</code> function applied to a
      <code>Label</code> instance. This is useful for tools that need to match the output of different query
      commands and/or labels emitted by rules. If not enabled, output formatters are free to emit apparent
      repository names (relative to the main repository) instead to make the output more readable.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val consistentLabels = Flag.Boolean("consistentLabels")

  @Option(
    name = "copt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional options to pass to gcc.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val copt = Flag.Str("copt")

  @Option(
    name = "coverage_output_generator",
    defaultValue = "@bazel_tools//tools/test:lcov_merger",
    effectTags = [
      OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    help = """
      Location of the binary that is used to postprocess raw coverage reports. This must currently be a filegroup
      that contains a single file, the binary. Defaults to '//tools/test:lcov_merger'.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val coverageOutputGenerator = Flag.Label("coverageOutputGenerator")

  @Option(
    name = "coverage_report_generator",
    defaultValue = "@bazel_tools//tools/test:coverage_report_generator",
    effectTags = [
      OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    help = """
      Location of the binary that is used to generate coverage reports. This must currently be a filegroup that
      contains a single file, the binary. Defaults to '//tools/test:coverage_report_generator'.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val coverageReportGenerator = Flag.Label("coverageReportGenerator")

  @Option(
    name = "coverage_support",
    defaultValue = "@bazel_tools//tools/test:coverage_support",
    effectTags = [
      OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    help = """
      Location of support files that are required on the inputs of every test action that collects code coverage.
      Defaults to '//tools/test:coverage_support'.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val coverageSupport = Flag.Label("coverageSupport")

  @Option(
    name = "cpu",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "The target CPU.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val cpu = Flag.Str("cpu")

  @Option(
    name = "credential_helper",
    oldName = "experimental_credential_helper",
    allowMultiple = true,
    help = """
      Configures a credential helper conforming to the <a
      href="https://github.com/EngFlow/credential-helper-spec">Credential Helper Specification</a> to use for
      retrieving authorization credentials for  repository fetching, remote caching and execution, and the build
      event service.
      
      Credentials supplied by a helper take precedence over credentials supplied by `--google_default_credentials`,
      `--google_credentials`, a `.netrc` file, or the auth parameter to `repository_ctx.download()` and
      `repository_ctx.download_and_extract()`.
      
      May be specified multiple times to set up multiple helpers.
      
      See https://blog.engflow.com/2023/10/09/configuring-bazels-credential-helper/ for instructions.
    """,
    valueHelp = """
      Path to a credential helper. It may be absolute, relative to the PATH environment variable, or
      %workspace%-relative. The path be optionally prefixed by a scope  followed by an '='. The scope is
      a domain name, optionally with a single leading '*' wildcard component. A helper applies to URIs
      matching its scope, with more specific scopes preferred. If a helper has no scope, it applies to
      every URI.
    """,
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val credentialHelper = Flag.Unknown("credentialHelper")

  @Option(
    name = "credential_helper_cache_duration",
    oldName = "experimental_credential_helper_cache_duration",
    defaultValue = "30m",
    help = """
      The default duration for which credentials supplied by a credential helper are cached if the helper does not
      provide when the credentials expire.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val credentialHelperCacheDuration = Flag.Duration("credentialHelperCacheDuration")

  @Option(
    name = "credential_helper_timeout",
    oldName = "experimental_credential_helper_timeout",
    defaultValue = "10s",
    help = """
      Configures the timeout for a credential helper.
      
      Credential helpers failing to respond within this timeout will fail the invocation.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val credentialHelperTimeout = Flag.Duration("credentialHelperTimeout")

  @Option(
    name = "crosstool_top",
    defaultValue = "@bazel_tools//tools/cpp:toolchain",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op flag. Will be removed in a future release.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val crosstoolTop = Flag.Label("crosstoolTop")

  @Option(
    name = "cs_fdo_absolute_path",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Use CSFDO profile information to optimize compilation. Specify the absolute path name of the zip file
      containing the profile file, a raw or an indexed LLVM profile file.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val csFdoAbsolutePath = Flag.Str("csFdoAbsolutePath")

  @Option(
    name = "cs_fdo_instrument",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Generate binaries with context sensitive FDO instrumentation. With Clang/LLVM compiler, it also accepts the
      directory name under which the raw profile file(s) will be dumped at runtime.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val csFdoInstrument = Flag.Str("csFdoInstrument")

  @Option(
    name = "cs_fdo_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "The cs_fdo_profile representing the context sensitive profile to be used for optimization.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val csFdoProfile = Flag.Label("csFdoProfile")

  @Option(
    name = "curses",
    defaultValue = "auto",
    help = "Use terminal cursor controls to minimize scrolling output.",
    valueHelp = "yes, no or auto",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val curses = Flag.OneOf("curses")

  @Option(
    name = "custom_malloc",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specifies a custom malloc implementation. This setting overrides malloc attributes in build rules.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val customMalloc = Flag.Label("customMalloc")

  @Option(
    name = "cxxopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional option to pass to gcc when compiling C++ source files.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val cxxopt = Flag.Str("cxxopt")

  @Option(
    name = "cycles",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Points out dependency cycles inside the displayed tree, which are normally ignored by default.",
    valueHelp = "a boolean",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val cycles = Flag.Boolean("cycles")

  @Option(
    name = "debug_app",
    effectTags = [OptionEffectTag.EXECUTION],
    expandsTo = ["--start=DEBUG"],
    help = "Whether to wait for the debugger before starting the app.",
    valueHelp = "",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val debugApp = Flag.Unknown("debugApp")

  @Option(
    name = "debug_spawn_scheduler",
    oldName = "experimental_debug_spawn_scheduler",
    defaultValue = "false",
    help = "",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val debugSpawnScheduler = Flag.Boolean("debugSpawnScheduler")

  @Option(
    name = "default_override",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "",
    valueHelp = "blazerc option override",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val defaultOverride = Flag.Unknown("defaultOverride")

  @Option(
    name = "default_system_javabase",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = """
      The root of the user's local JDK install, to be used as the default target javabase and as a fall-back
      host_javabase. This is not the embedded JDK.
    """,
    valueHelp = "a path",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val defaultSystemJavabase = Flag.Path("defaultSystemJavabase")

  @Option(
    name = "default_test_resources",
    allowMultiple = true,
    help = """
      Override the default resources amount for tests. The expected format is <resource>=<value>. If a single
      positive number is specified as <value> it will override the default resources for all test sizes. If 4
      comma-separated numbers are specified, they will override the resource amount for respectively the small,
      medium, large, enormous test sizes. Values can also be HOST_RAM/HOST_CPU, optionally followed by [-|*]<float>
      (eg. memory=HOST_RAM*.1,HOST_RAM*.2,HOST_RAM*.3,HOST_RAM*.4). The default test resources specified by this
      flag are overridden by explicit resources specified in tags.
    """,
    valueHelp = "a resource name followed by equal and 1 float or 4 float, e.g memory=10,30,60,100",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val defaultTestResources = Flag.Unknown("defaultTestResources")

  @Option(
    name = "default_visibility",
    defaultValue = "private",
    help = "Default visibility for packages that don't set it explicitly ('public' or 'private').",
    valueHelp = "default visibility",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val defaultVisibility = Flag.Unknown("defaultVisibility")

  @Option(
    name = "defer_param_files",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "This option is deprecated and has no effect and will be removed in the future.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val deferParamFiles = Flag.Boolean("deferParamFiles")

  @Option(
    name = "define",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Each --define option specifies an assignment for a build variable. In case of multiple values for a variable,
      the last one wins.
    """,
    valueHelp = "a 'name=value' assignment",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val define = Flag.Unknown("define")

  @Option(
    name = "deleted_packages",
    allowMultiple = true,
    help = """
      A comma-separated list of names of packages which the build system will consider non-existent, even if they
      are visible somewhere on the package path.
      Use this option when deleting a subpackage 'x/y' of an existing package 'x'.  For example, after deleting
      x/y/BUILD in your client, the build system may complain if it encounters a label '//x:y/z' if that is still
      provided by another package_path entry.  Specifying --deleted_packages x/y avoids this problem.
    """,
    valueHelp = "comma-separated list of package names",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val deletedPackages = Flag.Unknown("deletedPackages")

  @Option(
    name = "depth",
    defaultValue = "-1",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Maximum display depth of the dependency tree. A depth of 1 displays the direct dependencies, for example. For
      tree, path and all_paths it defaults to Integer.MAX_VALUE, while for deps and explain it defaults to 1 (only
      displays direct deps of the root besides the target leaves and their parents).
      
    """,
    valueHelp = "an integer",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val depth = Flag.Integer("depth")

  @Option(
    name = "desugar_for_android",
    oldName = "experimental_desugar_for_android",
    defaultValue = "true",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    help = "Whether to desugar Java 8 bytecode before dexing.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val desugarForAndroid = Flag.Boolean("desugarForAndroid")

  @Option(
    name = "desugar_java8_libs",
    oldName = "experimental_desugar_java8_libs",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Whether to include supported Java 8 libraries in apps for legacy devices.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val desugarJava8Libs = Flag.Boolean("desugarJava8Libs")

  @Option(
    name = "device",
    defaultValue = "",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = "The adb device serial number. If not specified, the first device will be used.",
    valueHelp = "a string",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val device = Flag.Str("device")

  @Option(
    name = "device_debug_entitlements",
    defaultValue = "true",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      If set, and compilation mode is not 'opt', objc apps will include debug entitlements when signing.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val deviceDebugEntitlements = Flag.Boolean("deviceDebugEntitlements")

  @Option(
    name = "dexopts_supported_in_dexmerger",
    defaultValue = "--minimal-main-dex,--set-max-idx-number",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "dx flags supported in tool that merges dex archives into final classes.dex files.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val dexoptsSupportedInDexmerger = Flag.Unknown("dexoptsSupportedInDexmerger")

  @Option(
    name = "dexopts_supported_in_dexsharder",
    defaultValue = "--minimal-main-dex",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "dx flags supported in tool that groups classes for inclusion in final .dex files.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val dexoptsSupportedInDexsharder = Flag.Unknown("dexoptsSupportedInDexsharder")

  @Option(
    name = "dexopts_supported_in_incremental_dexing",
    defaultValue = "--no-optimize,--no-locals",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "dx flags supported when converting Jars to dex archives incrementally.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val dexoptsSupportedInIncrementalDexing = Flag.Unknown("dexoptsSupportedInIncrementalDexing")

  @Option(
    name = "digest_function",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "The hash function to use when computing file digests.",
    valueHelp = "hash function",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val digestFunction = Flag.Unknown("digestFunction")

  @Option(
    name = "directory_creation_cache",
    defaultValue = "maximumSize=100000",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Describes the cache used to store known regular directories as they're created. Parent directories of output
      files are created on-demand during action execution.
    """,
    valueHelp = "Converts to a CaffeineSpec, or null if the input is empty",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val directoryCreationCache = Flag.Unknown("directoryCreationCache")

  @Option(
    name = "disallow_strict_deps_for_jpl",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = "No-op, kept only for backwards compatibility.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val disallowStrictDepsForJpl = Flag.Boolean("disallowStrictDepsForJpl")

  @Option(
    name = "discard_actions_after_execution",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "This option is deprecated and has no effect.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val discardActionsAfterExecution = Flag.Boolean("discardActionsAfterExecution")

  @Option(
    name = "discard_analysis_cache",
    defaultValue = "false",
    help = """
      Discard the analysis cache immediately after the analysis phase completes. Reduces memory usage by ~10%, but
      makes further incremental builds slower.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val discardAnalysisCache = Flag.Boolean("discardAnalysisCache")

  @Option(
    name = "disk_cache",
    help = """
      A path to a directory where Bazel can read and write actions and action outputs. If the directory does not
      exist, it will be created.
    """,
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val diskCache = Flag.Path("diskCache")

  @Option(
    name = "distdir",
    oldName = "experimental_distdir",
    allowMultiple = true,
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "Additional places to search for archives before accessing the network to download them.",
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val distdir = Flag.Path("distdir")

  @Option(
    name = "downloader_config",
    help = """
      Specify a file to configure the remote downloader with. This file consists of lines, each of which starts
      with a directive (`allow`, `block` or `rewrite`) followed by either a host name (for `allow` and `block`) or
      two patterns, one to match against, and one to use as a substitute URL, with back-references starting from
      `${'$'}1`. It is possible for multiple `rewrite` directives for the same URL to be give, and in this case
      multiple URLs will be returned.
    """,
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val downloaderConfig = Flag.Path("downloaderConfig")

  @Option(
    name = "dump",
    abbrev = 'd',
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      output full profile data dump either in human-readable 'text' format or script-friendly 'raw' format.
    """,
    valueHelp = "text or raw",
    commands = ["analyze-profile"],
  )
  @JvmField
  @Suppress("unused")
  val dump = Flag.OneOf("dump")

  @Option(
    name = "dump_all",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "If set, dump all known configurations instead of just the ids.",
    valueHelp = "a boolean",
    commands = ["config"],
  )
  @JvmField
  @Suppress("unused")
  val dumpAll = Flag.Boolean("dumpAll")

  @Option(
    name = "dynamic_local_execution_delay",
    oldName = "experimental_local_execution_delay",
    defaultValue = "1000",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      How many milliseconds should local execution be delayed, if remote execution was faster during a build at
      least once?
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val dynamicLocalExecutionDelay = Flag.Integer("dynamicLocalExecutionDelay")

  @Option(
    name = "dynamic_local_strategy",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      The local strategies, in order, to use for the given mnemonic - the first applicable strategy is used. For
      example, `worker,sandboxed` runs actions that support persistent workers using the worker strategy, and all
      others using the sandboxed strategy. If no mnemonic is given, the list of strategies is used as the fallback
      for all mnemonics. The default fallback list is `worker,sandboxed`, or`worker,sandboxed,standalone` if
      `experimental_local_lockfree_output` is set. Takes [mnemonic=]local_strategy[,local_strategy,...]
    """,
    valueHelp = "a '[name=]value1[,..,valueN]' assignment",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val dynamicLocalStrategy = Flag.Unknown("dynamicLocalStrategy")

  @Option(
    name = "dynamic_mode",
    defaultValue = "default",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Determines whether C++ binaries will be linked dynamically.  'default' means Bazel will choose whether to
      link dynamically.  'fully' means all libraries will be linked dynamically. 'off' means that all libraries
      will be linked in mostly static mode.
    """,
    valueHelp = "off, default or fully",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val dynamicMode = Flag.OneOf("dynamicMode")

  @Option(
    name = "dynamic_remote_strategy",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      The remote strategies, in order, to use for the given mnemonic - the first applicable strategy is used. If no
      mnemonic is given, the list of strategies is used as the fallback for all mnemonics. The default fallback
      list is `remote`, so this flag usually does not need to be set explicitly. Takes
      [mnemonic=]remote_strategy[,remote_strategy,...]
    """,
    valueHelp = "a '[name=]value1[,..,valueN]' assignment",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val dynamicRemoteStrategy = Flag.Unknown("dynamicRemoteStrategy")

  @Option(
    name = "emacs",
    defaultValue = "false",
    help = """
      A system-generated parameter which is true iff EMACS=t or INSIDE_EMACS is set in the environment of the
      client.  This option controls certain display features.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val emacs = Flag.Boolean("emacs")

  @Option(
    name = "embed_label",
    defaultValue = "",
    help = "Embed source control revision or release label in binary",
    valueHelp = "a one-line string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val embedLabel = Flag.Unknown("embedLabel")

  @Option(
    name = "emit_script_path_in_exec_request",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If true, emits the ExecRequest with --script_path file value and script contents instead of writing the
      script.
    """,
    valueHelp = "a boolean",
    commands = ["run"],
  )
  @JvmField
  @Suppress("unused")
  val emitScriptPathInExecRequest = Flag.Boolean("emitScriptPathInExecRequest")

  @Option(
    name = "enable_bzlmod",
    oldName = "experimental_enable_bzlmod",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      If true, enables the Bzlmod dependency management system, taking precedence over WORKSPACE. See
      https://bazel.build/docs/bzlmod for more information.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enableBzlmod = Flag.Boolean("enableBzlmod")

  @Option(
    name = "enable_fdo_profile_absolute_path",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated. No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enableFdoProfileAbsolutePath = Flag.Boolean("enableFdoProfileAbsolutePath")

  @Option(
    name = "enable_platform_specific_config",
    defaultValue = "false",
    help = """
      If true, Bazel picks up host-OS-specific config lines from bazelrc files. For example, if the host OS is
      Linux and you run bazel build, Bazel picks up lines starting with build:linux. Supported OS identifiers are
      linux, macos, windows, freebsd, and openbsd. Enabling this flag is equivalent to using --config=linux on
      Linux, --config=windows on Windows, etc.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enablePlatformSpecificConfig = Flag.Boolean("enablePlatformSpecificConfig")

  @Option(
    name = "enable_propeller_optimize_absolute_paths",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "If set, any use of absolute paths for propeller optimize will raise an error.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enablePropellerOptimizeAbsolutePaths = Flag.Boolean("enablePropellerOptimizeAbsolutePaths")

  @Option(
    name = "enable_remaining_fdo_absolute_paths",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "If set, any use of absolute paths for FDO will raise an error.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enableRemainingFdoAbsolutePaths = Flag.Boolean("enableRemainingFdoAbsolutePaths")

  @Option(
    name = "enable_runfiles",
    defaultValue = "auto",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Enable runfiles symlink tree; By default, it's off on Windows, on on other platforms.",
    valueHelp = "a tri-state (auto, yes, no)",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enableRunfiles = Flag.TriState("enableRunfiles")

  @Option(
    name = "enable_workspace",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      If true, enables the legacy WORKSPACE system for external dependencies. See
      https://bazel.build/external/overview for more information.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enableWorkspace = Flag.Boolean("enableWorkspace")

  @Option(
    name = "enforce_constraints",
    oldName = "experimental_enforce_constraints",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """
      Checks the environments each target is compatible with and reports errors if any target has dependencies that
      don't support the same environments
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enforceConstraints = Flag.Boolean("enforceConstraints")

  @Option(
    name = "enforce_proguard_file_extension",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      If enabled, requires that ProGuard configuration files outside of third_party/ use the *.pgcfg file extension.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enforceProguardFileExtension = Flag.Boolean("enforceProguardFileExtension")

  @Option(
    name = "enforce_project_configs",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, interactive builds may only pass the --scl_config build flag; they may not use any other build
      flags. --scl_config must be set to an officially suported project configuration. Supported configurations are
      defined in the target's PROJECT.scl, which can be found by walking up the target's packagge path. See
      b/324126745.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enforceProjectConfigs = Flag.Boolean("enforceProjectConfigs")

  @Option(
    name = "enforce_transitive_configs_for_config_feature_flag",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.AFFECTS_OUTPUTS,
      OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    help = "",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val enforceTransitiveConfigsForConfigFeatureFlag = Flag.Boolean("enforceTransitiveConfigsForConfigFeatureFlag")

  @Option(
    name = "execution_log_binary_file",
    help = """
      Log the executed spawns into this file as length-delimited SpawnExec protos, according to
      src/main/protobuf/spawn.proto. Prefer --execution_log_compact_file, which is significantly smaller and
      cheaper to produce. Related flags: --execution_log_compact_file (compact format; mutually exclusive),
      --execution_log_json_file (text JSON format; mutually exclusive), --execution_log_sort (whether to sort the
      execution log), --subcommands (for displaying subcommands in terminal output).
    """,
    valueHelp = "a path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val executionLogBinaryFile = Flag.Path("executionLogBinaryFile")

  @Option(
    name = "execution_log_compact_file",
    oldName = "experimental_execution_log_compact_file",
    help = """
      Log the executed spawns into this file as length-delimited ExecLogEntry protos, according to
      src/main/protobuf/spawn.proto. The entire file is zstd compressed. Related flags: --execution_log_binary_file
      (binary protobuf format; mutually exclusive), --execution_log_json_file (text JSON format; mutually
      exclusive), --subcommands (for displaying subcommands in terminal output).
    """,
    valueHelp = "a path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val executionLogCompactFile = Flag.Path("executionLogCompactFile")

  @Option(
    name = "execution_log_json_file",
    help = """
      Log the executed spawns into this file as newline-delimited JSON representations of SpawnExec protos,
      according to src/main/protobuf/spawn.proto. Prefer --execution_log_compact_file, which is significantly
      smaller and cheaper to produce. Related flags: --execution_log_compact_file (compact format; mutually
      exclusive), --execution_log_binary_file (binary protobuf format; mutually exclusive), --execution_log_sort
      (whether to sort the execution log), --subcommands (for displaying subcommands in terminal output).
    """,
    valueHelp = "a path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val executionLogJsonFile = Flag.Path("executionLogJsonFile")

  @Option(
    name = "execution_log_sort",
    defaultValue = "true",
    help = """
      Whether to sort the execution log, making it easier to compare logs across invocations. Set to false to avoid
      potentially significant CPU and memory usage at the end of the invocation, at the cost of producing the log
      in nondeterministic execution order. Only applies to the binary and JSON formats; the compact format is never
      sorted.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val executionLogSort = Flag.Boolean("executionLogSort")

  @Option(
    name = "expand_test_suites",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Expand test_suite targets into their constituent tests before analysis. When this flag is turned on (the
      default), negative target patterns will apply to the tests belonging to the test suite, otherwise they will
      not. Turning off this flag is useful when top-level aspects are applied at command line: then they can
      analyze test_suite targets.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val expandTestSuites = Flag.Boolean("expandTestSuites")

  @Option(
    name = "experimental_action_listener",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Deprecated in favor of aspects. Use action_listener to attach an extra_action to existing build actions.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalActionListener = Flag.Label("experimentalActionListener")

  @Option(
    name = "experimental_action_resource_set",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalActionResourceSet = Flag.Boolean("experimentalActionResourceSet")

  @Option(
    name = "experimental_add_exec_constraints_to_targets",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      List of comma-separated regular expressions, each optionally prefixed by - (negative expression), assigned
      (=) to a list of comma-separated constraint value targets. If a target matches no negative expression and at
      least one positive expression its toolchain resolution will be performed as if it had declared the constraint
      values as execution constraints. Example: //demo,-test=@platforms//cpus:x86_64 will add 'x86_64' to any
      target under //demo except for those whose name contains 'test'.
    """,
    valueHelp = "a '<RegexFilter>=<label1>[,<label2>,...]' assignment",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAddExecConstraintsToTargets = Flag.Unknown("experimentalAddExecConstraintsToTargets")

  @Option(
    name = "experimental_add_test_support_to_compile_time_deps",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Flag to help transition away from adding test support libraries to the compile-time deps of Java test rules.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAddTestSupportToCompileTimeDeps = Flag.Boolean("experimentalAddTestSupportToCompileTimeDeps")

  @Option(
    name = "experimental_allow_android_library_deps_without_srcs",
    defaultValue = "false",
    help = "No-op. Kept here for backwards compatibility.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAllowAndroidLibraryDepsWithoutSrcs = Flag.Boolean("experimentalAllowAndroidLibraryDepsWithoutSrcs")

  @Option(
    name = "experimental_allow_project_files",
    defaultValue = "false",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL, OptionMetadataTag.HIDDEN],
    help = "Enable processing of +<file> parameters.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAllowProjectFiles = Flag.Boolean("experimentalAllowProjectFiles")

  @Option(
    name = "experimental_allow_runtime_deps_on_neverlink",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "No-op, kept only for backwards compatibility",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAllowRuntimeDepsOnNeverlink = Flag.Boolean("experimentalAllowRuntimeDepsOnNeverlink")

  @Option(
    name = "experimental_always_filter_duplicate_classes_from_android_test",
    defaultValue = "false",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If enabled and the android_test defines a binary_under_test, the class filterering applied to the test's
      deploy jar will always filter duplicate classes based solely on matching class and package name, ignoring
      hash values.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAlwaysFilterDuplicateClassesFromAndroidTest =
    Flag.Boolean("experimentalAlwaysFilterDuplicateClassesFromAndroidTest")

  @Option(
    name = "experimental_android_assume_minsdkversion",
    defaultValue = "false",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      When enabled, the minSdkVersion is parsed from the merged AndroidManifest and used to instruct Proguard on
      valid Android build versions.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidAssumeMinsdkversion = Flag.Boolean("experimentalAndroidAssumeMinsdkversion")

  @Option(
    name = "experimental_android_compress_java_resources",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Compress Java resources in APKs",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidCompressJavaResources = Flag.Boolean("experimentalAndroidCompressJavaResources")

  @Option(
    name = "experimental_android_databinding_v2",
    defaultValue = "true",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Use android databinding v2. This flag is a no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidDatabindingV2 = Flag.Boolean("experimentalAndroidDatabindingV2")

  @Option(
    name = "experimental_android_library_exports_manifest_default",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "The default value of the exports_manifest attribute on android_library.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidLibraryExportsManifestDefault = Flag.Boolean("experimentalAndroidLibraryExportsManifestDefault")

  @Option(
    name = "experimental_android_resource_cycle_shrinking",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Enables more shrinking of code and resources by instructing AAPT2 to emit conditional Proguard keep rules.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidResourceCycleShrinking = Flag.Boolean("experimentalAndroidResourceCycleShrinking")

  @Option(
    name = "experimental_android_resource_name_obfuscation",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Enables obfuscation of resource names within android_binary APKs.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidResourceNameObfuscation = Flag.Boolean("experimentalAndroidResourceNameObfuscation")

  @Option(
    name = "experimental_android_resource_path_shortening",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Enables shortening of resource file paths within android_binary APKs.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidResourcePathShortening = Flag.Boolean("experimentalAndroidResourcePathShortening")

  @Option(
    name = "experimental_android_resource_shrinking",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Enables resource shrinking for android_binary APKs that use ProGuard.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidResourceShrinking = Flag.Boolean("experimentalAndroidResourceShrinking")

  @Option(
    name = "experimental_android_rewrite_dexes_with_rex",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "use rex tool to rewrite dex files",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidRewriteDexesWithRex = Flag.Boolean("experimentalAndroidRewriteDexesWithRex")

  @Option(
    name = "experimental_android_use_parallel_dex2oat",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Use dex2oat in parallel to possibly speed up android_test.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidUseParallelDex2oat = Flag.Boolean("experimentalAndroidUseParallelDex2oat")

  @Option(
    name = "experimental_announce_profile_path",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAnnounceProfilePath = Flag.Boolean("experimentalAnnounceProfilePath")

  @Option(
    name = "experimental_aquery_dump_after_build_format",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Writes the state of Skyframe (which includes previous invocations on this blaze instance as well) after a
      build. Output is streamed remotely unless local output is requested with
      --experimental_aquery_dump_after_build_output_file.  Does not honor aquery flags for --include_*, but uses
      the same defaults, except for --include_commandline=false. Possible output formats:
      proto|streamed_proto|textproto|jsonproto. Using this will disable Skymeld.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAqueryDumpAfterBuildFormat = Flag.Str("experimentalAqueryDumpAfterBuildFormat")

  @Option(
    name = "experimental_aquery_dump_after_build_output_file",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specify the output file for the aquery dump after a build. Use in conjunction with
      --experimental_aquery_dump_after_build_format. The path provided is relative to Bazel's output base, unless
      it's an absolute path. Using this will disable Skymeld.
    """,
    valueHelp = "a path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAqueryDumpAfterBuildOutputFile = Flag.Path("experimentalAqueryDumpAfterBuildOutputFile")

  @Option(
    name = "experimental_async_execution",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, Bazel is allowed to run action in a virtual thread. The number of actions in flight is still
      capped with --jobs.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalAsyncExecution = Flag.Boolean("experimentalAsyncExecution")

  @Option(
    name = "experimental_bep_target_summary",
    defaultValue = "false",
    help = "Whether to publish TargetSummary events.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBepTargetSummary = Flag.Boolean("experimentalBepTargetSummary")

  @Option(
    name = "experimental_build_event_expand_filesets",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "If true, expand Filesets in the BEP when presenting output files.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventExpandFilesets = Flag.Boolean("experimentalBuildEventExpandFilesets")

  @Option(
    name = "experimental_build_event_fully_resolve_fileset_symlinks",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If true, fully resolve relative Fileset symlinks in the BEP when presenting output files. Requires
      --experimental_build_event_expand_filesets.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventFullyResolveFilesetSymlinks = Flag.Boolean("experimentalBuildEventFullyResolveFilesetSymlinks")

  @Option(
    name = "experimental_build_event_output_group_mode",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specify how an output group's files will be represented in TargetComplete/AspectComplete BEP events. Values
      are an assignment of an output group name to one of 'NAMED_SET_OF_FILES_ONLY', 'INLINE_ONLY', or 'BOTH'. The
      default value is 'NAMED_SET_OF_FILES_ONLY'. If an output group is repeated, the final value to appear is
      used. The default value sets the mode for coverage artifacts to BOTH:
      --experimental_build_event_output_group_mode=baseline.lcov=both
    """,
    valueHelp = "an output group name followed by an OutputGroupFileMode, e.g. default=both",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventOutputGroupMode = Flag.Unknown("experimentalBuildEventOutputGroupMode")

  @Option(
    name = "experimental_build_event_upload_retry_minimum_delay",
    defaultValue = "1s",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "Initial, minimum delay for exponential backoff retries when BEP upload fails. (exponent: 1.6)",
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventUploadRetryMinimumDelay = Flag.Duration("experimentalBuildEventUploadRetryMinimumDelay")

  @Option(
    name = "experimental_build_event_upload_strategy",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Selects how to upload artifacts referenced in the build event protocol.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventUploadStrategy = Flag.Str("experimentalBuildEventUploadStrategy")

  @Option(
    name = "experimental_build_transitive_python_runfiles",
    oldName = "incompatible_build_transitive_python_runfiles",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildTransitivePythonRunfiles = Flag.Boolean("experimentalBuildTransitivePythonRunfiles")

  @Option(
    name = "experimental_builtins_bzl_path",
    defaultValue = "%bundled%",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      This flag tells Bazel how to find the "@_builtins" .bzl files that govern how predeclared symbols for BUILD
      and .bzl files are defined. This flag is only intended for Bazel developers, to help when writing @_builtins
      .bzl code. Ordinarily this value is set to "%bundled%", which means to use the builtins_bzl/ directory
      packaged in the Bazel binary. However, it can be set to the path (relative to the root of the current
      workspace) of an alternate builtins_bzl/ directory, such as one in a Bazel source tree workspace. A literal
      value of "%workspace%" is equivalent to the relative package path of builtins_bzl/ within a Bazel source
      tree; this should only be used when running Bazel within its own source tree. Finally, a value of the empty
      string disables the builtins injection mechanism entirely.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuiltinsBzlPath = Flag.Str("experimentalBuiltinsBzlPath")

  @Option(
    name = "experimental_builtins_dummy",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Enables an internal dummy symbol used to test builtins injection.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuiltinsDummy = Flag.Boolean("experimentalBuiltinsDummy")

  @Option(
    name = "experimental_builtins_injection_override",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      A comma-separated list of symbol names prefixed by a + or - character, indicating which symbols from
      `@_builtins//:exports.bzl` to inject, overriding their default injection status. Precisely, this works as
      follows. Each dict key of `exported_toplevels` or `exported_rules` has the form `foo`, `+foo`, or `-foo`. The
      first two forms mean it gets injected by default, while the last form means it does not get injected by
      default. In the first case (unprefixed), the default is absolute and cannot be overridden. Otherwise, we then
      consult this options list, and if we see foo occur here, we take the prefix of its last occurrence and use
      that to decide whether or not to inject. It is a no-op to specify an unknown symbol, or to attempt to not
      inject a symbol that occurs unprefixed in a dict key.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuiltinsInjectionOverride = Flag.Unknown("experimentalBuiltinsInjectionOverride")

  @Option(
    name = "experimental_bytecode_optimizers",
    defaultValue = "Proguard",
    help = "Do not use.",
    valueHelp = "a comma-separated list of keys optionally followed by '=' and a label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBytecodeOptimizers = Flag.Unknown("experimentalBytecodeOptimizers")

  @Option(
    name = "experimental_bzl_visibility",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If enabled, adds a `visibility()` function that .bzl files may call during top-level evaluation to set their
      visibility for the purpose of load() statements.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalBzlVisibility = Flag.Boolean("experimentalBzlVisibility")

  @Option(
    name = "experimental_cancel_concurrent_tests",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, then Blaze will cancel concurrently running tests on the first successful run. This is only useful
      in combination with --runs_per_test_detects_flakes.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCancelConcurrentTests = Flag.Boolean("experimentalCancelConcurrentTests")

  @Option(
    name = "experimental_cc_implementation_deps",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If enabled, cc_library targets can use attribute `implementation_deps`.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCcImplementationDeps = Flag.Boolean("experimentalCcImplementationDeps")

  @Option(
    name = "experimental_cc_shared_library",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If set to true, rule attributes and Starlark API methods needed for the rule cc_shared_library will be
      available
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCcSharedLibrary = Flag.Boolean("experimentalCcSharedLibrary")

  @Option(
    name = "experimental_cc_skylark_api_enabled_packages",
    defaultValue = "",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Passes list of packages that can use the C++ Starlark API. Don't enable this flag yet, we will be making
      breaking changes.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCcSkylarkApiEnabledPackages = Flag.Unknown("experimentalCcSkylarkApiEnabledPackages")

  @Option(
    name = "experimental_cc_static_library",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If set to true, rule attributes and Starlark API methods needed for the rule cc_static_library will be
      available
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCcStaticLibrary = Flag.Boolean("experimentalCcStaticLibrary")

  @Option(
    name = "experimental_cgroup_parent",
    effectTags = [OptionEffectTag.BAZEL_MONITORING, OptionEffectTag.EXECUTION],
    help = """
      The cgroup where to start the bazel server as an absolute path. The server process will be started in the
      specified cgroup for each supported controller. For example, if the value of this flag is /build/bazel and
      the cpu and memory controllers are mounted respectively on /sys/fs/cgroup/cpu and /sys/fs/cgroup/memory, the
      server will be started in the cgroups /sys/fs/cgroup/cpu/build/bazel and /sys/fs/cgroup/memory/build/bazel.It
      is not an error if the specified cgroup is not writable for one or more of the controllers. This options does
      not have any effect on platforms that do not support cgroups.
    """,
    valueHelp = "a string",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCgroupParent = Flag.Str("experimentalCgroupParent")

  @Option(
    name = "experimental_check_desugar_deps",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Whether to double-check correct desugaring at Android binary level.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCheckDesugarDeps = Flag.Boolean("experimentalCheckDesugarDeps")

  @Option(
    name = "experimental_check_external_repository_files",
    defaultValue = "true",
    help = """
      Check for modifications to files in external repositories. Consider setting this flag to false if you don't
      expect these files to change outside of bazel since it will speed up subsequent runs as they won't have to
      check a previous run's cache.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCheckExternalRepositoryFiles = Flag.Boolean("experimentalCheckExternalRepositoryFiles")

  @Option(
    name = "experimental_check_output_files",
    defaultValue = "true",
    help = """
      Check for modifications made to the output files of a build. Consider setting this flag to false if you don't
      expect these files to change outside of bazel since it will speed up subsequent runs as they won't have to
      check a previous run's cache.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCheckOutputFiles = Flag.Boolean("experimentalCheckOutputFiles")

  @Option(
    name = "experimental_circuit_breaker_strategy",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Specifies the strategy for the circuit breaker to use. Available strategies are "failure". On invalid value
      for the option the behavior same as the option is not set.
    """,
    valueHelp = "failure",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCircuitBreakerStrategy = Flag.Unknown("experimentalCircuitBreakerStrategy")

  @Option(
    name = "experimental_collect_code_coverage_for_generated_files",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If specified, Bazel will also generate collect coverage information for generated files.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectCodeCoverageForGeneratedFiles = Flag.Boolean("experimentalCollectCodeCoverageForGeneratedFiles")

  @Option(
    name = "experimental_collect_load_average_in_profiler",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "If enabled, the profiler collects the system's overall load average.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectLoadAverageInProfiler = Flag.Boolean("experimentalCollectLoadAverageInProfiler")

  @Option(
    name = "experimental_collect_local_action_metrics",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectLocalActionMetrics = Flag.Boolean("experimentalCollectLocalActionMetrics")

  @Option(
    name = "experimental_collect_local_sandbox_action_metrics",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectLocalSandboxActionMetrics = Flag.Boolean("experimentalCollectLocalSandboxActionMetrics")

  @Option(
    name = "experimental_collect_pressure_stall_indicators",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "If enabled, the profiler collects the Linux PSI data.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectPressureStallIndicators = Flag.Boolean("experimentalCollectPressureStallIndicators")

  @Option(
    name = "experimental_collect_resource_estimation",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "If enabled, the profiler collects CPU and memory usage estimation for local actions.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectResourceEstimation = Flag.Boolean("experimentalCollectResourceEstimation")

  @Option(
    name = "experimental_collect_skyframe_counts_in_profiler",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      If enabled, the profiler collects SkyFunction counts in the Skyframe graph over time for key function types,
      like configured targets and action executions. May have a performance hit as this visits the ENTIRE Skyframe
      graph at every profiling time unit. Do not use this flag with performance-critical measurements.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectSkyframeCountsInProfiler = Flag.Boolean("experimentalCollectSkyframeCountsInProfiler")

  @Option(
    name = "experimental_collect_system_network_usage",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "If enabled, the profiler collects the system's network usage.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectSystemNetworkUsage = Flag.Boolean("experimentalCollectSystemNetworkUsage")

  @Option(
    name = "experimental_collect_worker_data_in_profiler",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "If enabled, the profiler collects worker's aggregated resource data.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectWorkerDataInProfiler = Flag.Boolean("experimentalCollectWorkerDataInProfiler")

  @Option(
    name = "experimental_command_profile",
    help = """
      Records a Java Flight Recorder profile for the duration of the command. One of the supported profiling event
      types (cpu, wall, alloc or lock) must be given as an argument. The profile is written to a file named after
      the event type under the output base directory. The syntax and semantics of this flag might change in the
      future to support additional profile types or output formats; use at your own risk.
    """,
    valueHelp = "cpu, wall, alloc or lock",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCommandProfile = Flag.OneOf("experimentalCommandProfile")

  @Option(
    name = "experimental_convenience_symlinks",
    defaultValue = "normal",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      This flag controls how the convenience symlinks (the symlinks that appear in the workspace after the build)
      will be managed. Possible values:
        normal (default): Each kind of convenience symlink will be created or deleted, as determined by the build.
        clean: All symlinks will be unconditionally deleted.
        ignore: Symlinks will not be created or cleaned up.
        log_only: Generate log messages as if 'normal' were passed, but don't actually perform any filesystem
      operations (useful for tools).
      Note that only symlinks whose names are generated by the current value of --symlink_prefix can be affected;
      if the prefix changes, any pre-existing symlinks will be left alone.
    """,
    valueHelp = "normal, clean, ignore or log_only",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalConvenienceSymlinks = Flag.OneOf("experimentalConvenienceSymlinks")

  @Option(
    name = "experimental_convenience_symlinks_bep_event",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      This flag controls whether or not we will post the build eventConvenienceSymlinksIdentified to the
      BuildEventProtocol. If the value is true, the BuildEventProtocol will have an entry for
      convenienceSymlinksIdentified, listing all of the convenience symlinks created in your workspace. If false,
      then the convenienceSymlinksIdentified entry in the BuildEventProtocol will be empty.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalConvenienceSymlinksBepEvent = Flag.Boolean("experimentalConvenienceSymlinksBepEvent")

  @Option(
    name = "experimental_correct_runfiles_middleman_paths",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If set, the path of runfiles middlemen represents the real path of the runfiles tree.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCorrectRunfilesMiddlemanPaths = Flag.Boolean("experimentalCorrectRunfilesMiddlemanPaths")

  @Option(
    name = "experimental_cpp_compile_resource_estimation",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If enabled, will estimate precise resource usage for local execution of CppCompileAction.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCppCompileResourceEstimation = Flag.Boolean("experimentalCppCompileResourceEstimation")

  @Option(
    name = "experimental_cpp_modules",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Enables experimental C++20 modules support. Use it with `module_interfaces` attribute on `cc_binary` and
      `cc_library`. While the support is behind the experimental flag, there are no guarantees about incompatible
      changes to it or even keeping the support in the future. Consider those risks when using it.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCppModules = Flag.Boolean("experimentalCppModules")

  @Option(
    name = "experimental_cpu_load_scheduling",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Enables the experimental local execution scheduling based on CPU load, not estimation of actions one by one.
      Experimental scheduling have showed the large benefit on a large local builds on a powerful machines with the
      large number of cores. Reccommended to use with --local_resources=cpu=HOST_CPUS
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCpuLoadScheduling = Flag.Boolean("experimentalCpuLoadScheduling")

  @Option(
    name = "experimental_cpu_load_scheduling_window_size",
    defaultValue = "5000ms",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      The size of window during experimental scheduling of action based on CPU load. Make sense to define only when
      flag --experimental_cpu_load_scheduling is enabled.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalCpuLoadSchedulingWindowSize = Flag.Duration("experimentalCpuLoadSchedulingWindowSize")

  @Option(
    name = "experimental_debug_selects_always_succeed",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      When set, select functions with no matching clause will return an empty value, instead of failing. This is to
      help use cquery diagnose failures in select.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDebugSelectsAlwaysSucceed = Flag.Boolean("experimentalDebugSelectsAlwaysSucceed")

  @Option(
    name = "experimental_disable_external_package",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If set to true, the auto-generated //external package will not be available anymore. Bazel will still be
      unable to parse the file 'external/BUILD', but globs reaching into external/ from the unnamed package will
      work.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDisableExternalPackage = Flag.Boolean("experimentalDisableExternalPackage")

  @Option(
    name = "experimental_disable_instrumentation_manifest_merge",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Disables manifest merging when an android_binary has instruments set (i.e. is used for instrumentation
      testing).
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDisableInstrumentationManifestMerge = Flag.Boolean("experimentalDisableInstrumentationManifestMerge")

  @Option(
    name = "experimental_disallow_legacy_java_toolchain_flags",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If enabled, disallow legacy Java toolchain flags (--javabase, --host_javabase, --java_toolchain,
      --host_java_toolchain) and require the use of --platforms instead; see #7849
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDisallowLegacyJavaToolchainFlags = Flag.Boolean("experimentalDisallowLegacyJavaToolchainFlags")

  @Option(
    name = "experimental_disk_cache_gc_idle_delay",
    defaultValue = "5m",
    help = """
      How long the server must remain idle before a garbage collection of the disk cache occurs. To specify the
      garbage collection policy, set --experimental_disk_cache_gc_max_size and/or
      --experimental_disk_cache_gc_max_age.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDiskCacheGcIdleDelay = Flag.Duration("experimentalDiskCacheGcIdleDelay")

  @Option(
    name = "experimental_disk_cache_gc_max_age",
    defaultValue = "0",
    help = """
      If set to a positive value, the disk cache will be periodically garbage collected to remove entries older
      than this age. If set in conjunction with --experimental_disk_cache_gc_max_size, both criteria are applied.
      Garbage collection occurrs in the background once the server has become idle, as determined by the
      --experimental_disk_cache_gc_idle_delay flag.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDiskCacheGcMaxAge = Flag.Duration("experimentalDiskCacheGcMaxAge")

  @Option(
    name = "experimental_disk_cache_gc_max_size",
    defaultValue = "0",
    help = """
      If set to a positive value, the disk cache will be periodically garbage collected to stay under this size. If
      set in conjunction with --experimental_disk_cache_gc_max_age, both criteria are applied. Garbage collection
      occurrs in the background once the server has become idle, as determined by the
      --experimental_disk_cache_gc_idle_delay flag.
    """,
    valueHelp = "a size in bytes, optionally followed by a K, M, G or T multiplier",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDiskCacheGcMaxSize = Flag.Unknown("experimentalDiskCacheGcMaxSize")

  @Option(
    name = "experimental_docker_image",
    defaultValue = "",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Specify a Docker image name (e.g. "ubuntu:latest") that should be used to execute a sandboxed action when
      using the docker strategy and the action itself doesn't already have a container-image attribute in its
      remote_execution_properties in the platform description. The value of this flag is passed verbatim to 'docker
      run', so it supports the same syntax and mechanisms as Docker itself.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDockerImage = Flag.Str("experimentalDockerImage")

  @Option(
    name = "experimental_docker_privileged",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If enabled, Bazel will pass the --privileged flag to 'docker run' when running actions. This might be
      required by your build, but it might also result in reduced hermeticity.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDockerPrivileged = Flag.Boolean("experimentalDockerPrivileged")

  @Option(
    name = "experimental_docker_use_customized_images",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If enabled, injects the uid and gid of the current user into the Docker image before using it. This is
      required if your build / tests depend on the user having a name and home directory inside the container. This
      is on by default, but you can disable it in case the automatic image customization feature doesn't work in
      your case or you know that you don't need it.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDockerUseCustomizedImages = Flag.Boolean("experimentalDockerUseCustomizedImages")

  @Option(
    name = "experimental_docker_verbose",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = "If enabled, Bazel will print more verbose messages about the Docker sandbox strategy.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDockerVerbose = Flag.Boolean("experimentalDockerVerbose")

  @Option(
    name = "experimental_dormant_deps",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
       If set to true, attr.label(materializer=), attr(for_dependency_resolution=), attr.dormant_label(),
      attr.dormant_label_list() and rule(for_dependency_resolution=) are allowed.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDormantDeps = Flag.Boolean("experimentalDormantDeps")

  @Option(
    name = "experimental_downloader_config",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Specify a file to configure the remote downloader with. This file consists of lines, each of which starts
      with a directive (`allow`, `block` or `rewrite`) followed by either a host name (for `allow` and `block`) or
      two patterns, one to match against, and one to use as a substitute URL, with back-references starting from
      `${'$'}1`. It is possible for multiple `rewrite` directives for the same URL to be give, and in this case
      multiple URLs will be returned.
    """,
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDownloaderConfig = Flag.Path("experimentalDownloaderConfig")

  @Option(
    name = "experimental_dynamic_exclude_tools",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      When set, targets that are build "for tool" are not subject to dynamic execution. Such targets are extremely
      unlikely to be built incrementally and thus not worth spending local cycles on.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDynamicExcludeTools = Flag.Boolean("experimentalDynamicExcludeTools")

  @Option(
    name = "experimental_dynamic_ignore_local_signals",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Takes a list of OS signal numbers. If a local branch of dynamic execution gets killed with any of these
      signals, the remote branch will be allowed to finish instead. For persistent workers, this only affects
      signals that kill the worker process.
    """,
    valueHelp = "a comma-separated list of signal numbers",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDynamicIgnoreLocalSignals = Flag.Unknown("experimentalDynamicIgnoreLocalSignals")

  @Option(
    name = "experimental_dynamic_local_load_factor",
    defaultValue = "0",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Controls how much load from dynamic execution to put on the local machine. This flag adjusts how many actions
      in dynamic execution we will schedule concurrently. It is based on the number of CPUs Blaze thinks is
      available, which can be controlled with the --local_cpu_resources flag.
      If this flag is 0, all actions are scheduled locally immediately. If > 0, the amount of actions scheduled
      locally is limited by the number of CPUs available. If < 1, the load factor is used to reduce the number of
      locally scheduled actions when the number of actions waiting to schedule is high. This lessens the load on
      the local machine in the clean build case, where the local machine does not contribute much.
    """,
    valueHelp = "a double",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDynamicLocalLoadFactor = Flag.Double("experimentalDynamicLocalLoadFactor")

  @Option(
    name = "experimental_dynamic_slow_remote_time",
    defaultValue = "0",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      If >0, the time a dynamically run action must run remote-only before we prioritize its local execution to
      avoid remote timeouts. This may hide some problems on the remote execution system. Do not turn this on
      without monitoring of remote execution issues.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalDynamicSlowRemoteTime = Flag.Duration("experimentalDynamicSlowRemoteTime")

  @Option(
    name = "experimental_enable_android_migration_apis",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = "If set to true, enables the APIs required to support the Android Starlark migration.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableAndroidMigrationApis = Flag.Boolean("experimentalEnableAndroidMigrationApis")

  @Option(
    name = "experimental_enable_aspect_hints",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableAspectHints = Flag.Boolean("experimentalEnableAspectHints")

  @Option(
    name = "experimental_enable_critical_path_profiling",
    defaultValue = "true",
    help = """
      If set (the default), critical path profiling is enabled for the execution phase. This has a slight overhead
      in RAM and CPU, and may prevent Bazel from making certain aggressive RAM optimizations in some cases.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableCriticalPathProfiling = Flag.Boolean("experimentalEnableCriticalPathProfiling")

  @Option(
    name = "experimental_enable_docker_sandbox",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = "Enable Docker-based sandboxing. This option has no effect if Docker is not installed.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableDockerSandbox = Flag.Boolean("experimentalEnableDockerSandbox")

  @Option(
    name = "experimental_enable_execution_graph_log",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Enabling this flag makes Blaze write a file of all actions executed during a build. Note that this dump may
      use a different granularity of actions than other APIs, and may also contain additional information as
      necessary to reconstruct the full dependency graph in combination with other sources of data.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableExecutionGraphLog = Flag.Boolean("experimentalEnableExecutionGraphLog")

  @Option(
    name = "experimental_enable_first_class_macros",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = "If set to true, enables the `macro()` construct for defining symbolic macros.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableFirstClassMacros = Flag.Boolean("experimentalEnableFirstClassMacros")

  @Option(
    name = "experimental_enable_jspecify",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Enable experimental jspecify integration.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableJspecify = Flag.Boolean("experimentalEnableJspecify")

  @Option(
    name = "experimental_enable_scl_dialect",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = "If set to true, .scl files may be used in load() statements.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableSclDialect = Flag.Boolean("experimentalEnableSclDialect")

  @Option(
    name = "experimental_enable_skyfocus",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      If true, enable the use of --experimental_working_set to reduce Bazel's memory footprint for incremental
      builds. This feature is known as Skyfocus.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableSkyfocus = Flag.Boolean("experimentalEnableSkyfocus")

  @Option(
    name = "experimental_enable_starlark_doc_extract",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Deprecated no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableStarlarkDocExtract = Flag.Boolean("experimentalEnableStarlarkDocExtract")

  @Option(
    name = "experimental_enable_starlark_set",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If true, enable the set data type and set() constructor in Starlark.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableStarlarkSet = Flag.Boolean("experimentalEnableStarlarkSet")

  @Option(
    name = "experimental_exclude_defines_from_exec_config",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, don't propagate '--define's to the exec transition at default; only propagate defines specified by
      `--experimental_propagate_custom_flag`.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExcludeDefinesFromExecConfig = Flag.Boolean("experimentalExcludeDefinesFromExecConfig")

  @Option(
    name = "experimental_exclude_starlark_flags_from_exec_config",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, don't propagate starlark flags to the exec transition at default; only propagate starlark flags
      specified in `--experimental_propagate_custom_flag`.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExcludeStarlarkFlagsFromExecConfig = Flag.Boolean("experimentalExcludeStarlarkFlagsFromExecConfig")

  @Option(
    name = "experimental_exec_config",
    defaultValue = "@_builtins//:common/builtin_exec_platforms.bzl%bazel_exec_transition",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If set to '//some:label:my.bzl%my_transition', uses my_transition for 'cfg = "exec"' semantics instead of
      Bazel's internal exec transition logic.  Else uses Bazel's internal logic.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExecConfig = Flag.Str("experimentalExecConfig")

  @Option(
    name = "experimental_exec_configuration_distinguisher",
    defaultValue = "off",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Please only use this flag as part of a suggested migration or testing strategy due to potential for action
      conflicts. Controls how the execution transition changes the platform_suffix flag. In legacy mode, sets it to
      a hash of the execution platform. In fullhash mode, sets it to a hash of the entire configuration. In off
      mode, does not touch it.
    """,
    valueHelp = "legacy, off, full_hash or diff_to_affected",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExecConfigurationDistinguisher = Flag.OneOf("experimentalExecConfigurationDistinguisher")

  @Option(
    name = "experimental_execution_graph_enable_edges_from_filewrite_actions",
    defaultValue = "true",
    help = "Handle edges from filewrite actions to their inputs correctly.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExecutionGraphEnableEdgesFromFilewriteActions =
    Flag.Boolean("experimentalExecutionGraphEnableEdgesFromFilewriteActions")

  @Option(
    name = "experimental_execution_graph_log_dep_type",
    defaultValue = "none",
    help = """
      Selects what kind of dependency information is reported in the action dump. If 'all', every inter-action edge
      will be reported.
    """,
    valueHelp = "none, runfiles or all",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExecutionGraphLogDepType = Flag.OneOf("experimentalExecutionGraphLogDepType")

  @Option(
    name = "experimental_execution_graph_log_middleman",
    defaultValue = "false",
    help = "Subscribe to ActionMiddlemanEvent in ExecutionGraphModule.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExecutionGraphLogMiddleman = Flag.Boolean("experimentalExecutionGraphLogMiddleman")

  @Option(
    name = "experimental_execution_graph_log_path",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Local path at which the execution path will be written. If this is set, the log will only be written locally,
      and not to BEP. If this is set when experimental_enable_execution_graph_log is disabled, there will be an
      error. If this is unset while BEP uploads are disabled and experimental_enable_execution_graph_log is
      enabled, the log will be written to a local default.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExecutionGraphLogPath = Flag.Str("experimentalExecutionGraphLogPath")

  @Option(
    name = "experimental_execution_graph_log_queue_size",
    defaultValue = "-1",
    help = """
      The size of the action dump queue, where actions are kept before writing. Larger sizes will increase peak
      memory usage, but should decrease queue blocking. -1 means unbounded
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExecutionGraphLogQueueSize = Flag.Integer("experimentalExecutionGraphLogQueueSize")

  @Option(
    name = "experimental_explicit_aspects",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      aquery, cquery: whether to include aspect-generated actions in the output. query: no-op (aspects are always
      followed).
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExplicitAspects = Flag.Boolean("experimentalExplicitAspects")

  @Option(
    name = "experimental_extended_sanity_checks",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Enables internal validation checks to make sure that configured target implementations only access things
      they should. Causes a performance hit.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExtendedSanityChecks = Flag.Boolean("experimentalExtendedSanityChecks")

  @Option(
    name = "experimental_extra_action_filter",
    defaultValue = "",
    help = "Deprecated in favor of aspects. Filters set of targets to schedule extra_actions for.",
    valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExtraActionFilter = Flag.Unknown("experimentalExtraActionFilter")

  @Option(
    name = "experimental_extra_action_top_level_only",
    defaultValue = "false",
    help = "Deprecated in favor of aspects. Only schedules extra_actions for top level targets.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalExtraActionTopLevelOnly = Flag.Boolean("experimentalExtraActionTopLevelOnly")

  @Option(
    name = "experimental_fetch_all_coverage_outputs",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, then Bazel fetches the entire coverage data directory for each test during a coverage run.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalFetchAllCoverageOutputs = Flag.Boolean("experimentalFetchAllCoverageOutputs")

  @Option(
    name = "experimental_filter_library_jar_with_program_jar",
    defaultValue = "false",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Filter the ProGuard ProgramJar to remove any classes also present in the LibraryJar.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalFilterLibraryJarWithProgramJar = Flag.Boolean("experimentalFilterLibraryJarWithProgramJar")

  @Option(
    name = "experimental_filter_r_jars_from_android_test",
    defaultValue = "false",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If enabled, R Jars will be filtered from the test apk built by android_test.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalFilterRJarsFromAndroidTest = Flag.Boolean("experimentalFilterRJarsFromAndroidTest")

  @Option(
    name = "experimental_fix_deps_tool",
    defaultValue = "add_dep",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = "Specifies which tool should be used to resolve missing dependencies.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalFixDepsTool = Flag.Str("experimentalFixDepsTool")

  @Option(
    name = "experimental_force_gc_after_build",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "If true calls System.gc() after a build to try and get a post-gc peak heap measurement.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalForceGcAfterBuild = Flag.Boolean("experimentalForceGcAfterBuild")

  @Option(
    name = "experimental_fsvc_threads",
    defaultValue = "200",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "The number of threads that are used by the FileSystemValueChecker.",
    valueHelp = """
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation
      ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalFsvcThreads = Flag.Unknown("experimentalFsvcThreads")

  @Option(
    name = "experimental_generate_llvm_lcov",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If true, coverage for clang will generate an LCOV report.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalGenerateLlvmLcov = Flag.Boolean("experimentalGenerateLlvmLcov")

  @Option(
    name = "experimental_genquery_use_graphless_query",
    defaultValue = "auto",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated. No-op.",
    valueHelp = "a tri-state (auto, yes, no)",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalGenqueryUseGraphlessQuery = Flag.TriState("experimentalGenqueryUseGraphlessQuery")

  @Option(
    name = "experimental_get_android_java_resources_from_optimized_jar",
    defaultValue = "false",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Get Java resources from _proguard.jar instead of _deploy.jar in android_binary when bundling the final APK.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalGetAndroidJavaResourcesFromOptimizedJar = Flag.Boolean("experimentalGetAndroidJavaResourcesFromOptimizedJar")

  @Option(
    name = "experimental_google_legacy_api",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If set to true, exposes a number of experimental pieces of Starlark build API pertaining to Google legacy
      code.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalGoogleLegacyApi = Flag.Boolean("experimentalGoogleLegacyApi")

  @Option(
    name = "experimental_graphless_query",
    defaultValue = "auto",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      If true, uses a Query implementation that does not make a copy of the graph. The new implementation only
      supports --order_output=no, as well as only a subset of output formatters.
    """,
    valueHelp = "a tri-state (auto, yes, no)",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val experimentalGraphlessQuery = Flag.TriState("experimentalGraphlessQuery")

  @Option(
    name = "experimental_guard_against_concurrent_changes",
    defaultValue = "false",
    help = """
      Turn this off to disable checking the ctime of input files of an action before uploading it to a remote
      cache. There may be cases where the Linux kernel delays writing of files, which could cause false positives.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalGuardAgainstConcurrentChanges = Flag.Boolean("experimentalGuardAgainstConcurrentChanges")

  @Option(
    name = "experimental_import_deps_checking",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "No-op, kept only for backwards compatibility",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalImportDepsChecking = Flag.Str("experimentalImportDepsChecking")

  @Option(
    name = "experimental_include_default_values",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.TERMINAL_OUTPUT],
    help = "Whether Starlark options set to their default values are included in the output.",
    valueHelp = "a boolean",
    commands = ["canonicalize-flags"],
  )
  @JvmField
  @Suppress("unused")
  val experimentalIncludeDefaultValues = Flag.Boolean("experimentalIncludeDefaultValues")

  @Option(
    name = "experimental_include_scanning_parallelism",
    defaultValue = "80",
    effectTags = [
      OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.EXECUTION,
      OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS,
    ],
    help = """
      Configures the size of the thread pool used for include scanning. Takes an integer, or a keyword ("auto",
      "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". 0
      means to disable parallelism and to just rely on the build graph parallelism for concurrency.  "auto" means
      to use a reasonable value derived from the machine's hardware profile (e.g. the number of processors).
    """,
    valueHelp = """
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation
      ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalIncludeScanningParallelism = Flag.Unknown("experimentalIncludeScanningParallelism")

  @Option(
    name = "experimental_include_xcode_execution_requirements",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.EXECUTION,
    ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If set, add a "requires-xcode:{version}" execution requirement to every Xcode action.  If the Xcode version
      has a hyphenated label,  also add a "requires-xcode-label:{version_label}" execution requirement.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalIncludeXcodeExecutionRequirements = Flag.Boolean("experimentalIncludeXcodeExecutionRequirements")

  @Option(
    name = "experimental_incremental_dexing_after_proguard",
    defaultValue = "50",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      Whether to use incremental dexing tools when building proguarded Android binaries.  Values > 0 turn the
      feature on, values > 1 run that many dexbuilder shards.
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalIncrementalDexingAfterProguard = Flag.Integer("experimentalIncrementalDexingAfterProguard")

  @Option(
    name = "experimental_incremental_dexing_after_proguard_by_default",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Whether to use incremental dexing for proguarded Android binaries by default.  Use incremental_dexing
      attribute to override default for a particular android_binary.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalIncrementalDexingAfterProguardByDefault = Flag.Boolean("experimentalIncrementalDexingAfterProguardByDefault")

  @Option(
    name = "experimental_inmemory_dotd_files",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If enabled, C++ .d files will be passed through in memory directly from the remote build nodes instead of
      being written to disk.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalInmemoryDotdFiles = Flag.Boolean("experimentalInmemoryDotdFiles")

  @Option(
    name = "experimental_inmemory_dotincludes_files",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.EXECUTION,
      OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS,
    ],
    help = """
      If enabled, searching for '#include' lines in generated header files will not touch local disk. This makes
      include scanning of C++ files less disk-intensive.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalInmemoryDotincludesFiles = Flag.Boolean("experimentalInmemoryDotincludesFiles")

  @Option(
    name = "experimental_inmemory_jdeps_files",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If enabled, the dependency (.jdeps) files generated from Java compilations will be passed through in memory
      directly from the remote build nodes instead of being written to disk.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalInmemoryJdepsFiles = Flag.Boolean("experimentalInmemoryJdepsFiles")

  @Option(
    name = "experimental_inmemory_sandbox_stashes",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """
      If set to true, the contents of stashed sandboxes for reuse_sandbox_directories will be tracked in memory.
      This reduces the amount of I/O needed during reuse. Depending on the build this flag may improve wall time.
      Depending on the build as well this flag may use a significant amount of additional memory.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalInmemorySandboxStashes = Flag.Boolean("experimentalInmemorySandboxStashes")

  @Option(
    name = "experimental_inprocess_symlink_creation",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Whether to make direct filesystem calls to create symlink trees instead of delegating to a helper process.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalInprocessSymlinkCreation = Flag.Boolean("experimentalInprocessSymlinkCreation")

  @Option(
    name = "experimental_install_base_gc_max_age",
    defaultValue = "30d",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      How long an install base must go unused before it's eligible for garbage collection. If nonzero, the server
      will attempt to garbage collect other install bases when idle.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalInstallBaseGcMaxAge = Flag.Duration("experimentalInstallBaseGcMaxAge")

  @Option(
    name = "experimental_isolated_extension_usages",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      If true, enables the <code>isolate</code> parameter in the <a
      href="https://bazel.build/rules/lib/globals/module#use_extension"><code>use_extension</code></a> function.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalIsolatedExtensionUsages = Flag.Boolean("experimentalIsolatedExtensionUsages")

  @Option(
    name = "experimental_j2objc_header_map",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Whether to generate J2ObjC header map in parallel of J2ObjC transpilation.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalJ2objcHeaderMap = Flag.Boolean("experimentalJ2objcHeaderMap")

  @Option(
    name = "experimental_j2objc_shorter_header_path",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Whether to generate with shorter header path (uses "_ios" instead of "_j2objc").""",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalJ2objcShorterHeaderPath = Flag.Boolean("experimentalJ2objcShorterHeaderPath")

  @Option(
    name = "experimental_java_classpath",
    oldName = "java_classpath",
    defaultValue = "javabuilder",
    help = "Enables reduced classpaths for Java compilations.",
    valueHelp = "off, javabuilder or bazel",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalJavaClasspath = Flag.OneOf("experimentalJavaClasspath")

  @Option(
    name = "experimental_java_header_input_pruning",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "No-op, kept only for backwards compatibility",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalJavaHeaderInputPruning = Flag.Boolean("experimentalJavaHeaderInputPruning")

  @Option(
    name = "experimental_java_library_export",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If enabled, experimental_java_library_export_do_not_use module is available.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalJavaLibraryExport = Flag.Boolean("experimentalJavaLibraryExport")

  @Option(
    name = "experimental_java_proto_add_allowed_public_imports",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "This flag is a noop and scheduled for removal.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalJavaProtoAddAllowedPublicImports = Flag.Boolean("experimentalJavaProtoAddAllowedPublicImports")

  @Option(
    name = "experimental_java_test_auto_create_deploy_jar",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "DO NOT USE",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalJavaTestAutoCreateDeployJar = Flag.Boolean("experimentalJavaTestAutoCreateDeployJar")

  @Option(
    name = "experimental_limit_android_lint_to_android_constrained_java",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "No-op, kept only for backwards compatibility",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalLimitAndroidLintToAndroidConstrainedJava = Flag.Boolean("experimentalLimitAndroidLintToAndroidConstrainedJava")

  @Option(
    name = "experimental_link_static_libraries_once",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE, OptionMetadataTag.EXPERIMENTAL],
    help = """
      If enabled, cc_shared_library will link all libraries statically linked into it, that should only be linked
      once.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalLinkStaticLibrariesOnce = Flag.Boolean("experimentalLinkStaticLibrariesOnce")

  @Option(
    name = "experimental_local_java_optimization_configuration",
    help = "Do not use.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalLocalJavaOptimizationConfiguration = Flag.Label("experimentalLocalJavaOptimizationConfiguration")

  @Option(
    name = "experimental_local_java_optimizations",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Do not use.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalLocalJavaOptimizations = Flag.Boolean("experimentalLocalJavaOptimizations")

  @Option(
    name = "experimental_local_lockfree_output",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      When true, the local spawn runner doesn't lock the output tree during dynamic execution. Instead, spawns are
      allowed to execute until they are explicitly interrupted by a faster remote action.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalLocalLockfreeOutput = Flag.Boolean("experimentalLocalLockfreeOutput")

  @Option(
    name = "experimental_local_retries_on_crash",
    defaultValue = "0",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Number of times to retry a local action when we detect that it crashed. This exists to workaround a bug in
      OSXFUSE which is tickled by the use of the dynamic scheduler and --experimental_local_lockfree_output due to
      constant process churn. The bug can be triggered by a cancelled process that ran *before* the process we are
      trying to run, introducing corruption in its file reads.
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalLocalRetriesOnCrash = Flag.Integer("experimentalLocalRetriesOnCrash")

  @Option(
    name = "experimental_materialize_param_files_directly",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = "If materializing param files, do so with direct writes to disk.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalMaterializeParamFilesDirectly = Flag.Boolean("experimentalMaterializeParamFilesDirectly")

  @Option(
    name = "experimental_max_directories_to_eagerly_visit_in_globbing",
    defaultValue = "-1",
    help = """
      If non-negative, the first time a glob is evaluated in a package, the subdirectories of the package will be
      traversed in order to warm filesystem caches and compensate for lack of parallelism in globbing. At most this
      many directories will be visited.
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalMaxDirectoriesToEagerlyVisitInGlobbing = Flag.Integer("experimentalMaxDirectoriesToEagerlyVisitInGlobbing")

  @Option(
    name = "experimental_merged_skyframe_analysis_execution",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If this flag is set, the analysis and execution phases of Skyframe are merged.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalMergedSkyframeAnalysisExecution = Flag.Boolean("experimentalMergedSkyframeAnalysisExecution")

  @Option(
    name = "experimental_objc_fastbuild_options",
    defaultValue = "-O0,-DDEBUG=1",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = "Uses these strings as objc fastbuild compiler options.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalObjcFastbuildOptions = Flag.Unknown("experimentalObjcFastbuildOptions")

  @Option(
    name = "experimental_objc_provider_from_linked",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "No-op. Kept here for backwards compatibility. This field will be removed in a future release.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalObjcProviderFromLinked = Flag.Boolean("experimentalObjcProviderFromLinked")

  @Option(
    name = "experimental_omit_resources_info_provider_from_android_binary",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Omit AndroidResourcesInfo provider from android_binary rules. Propagating resources out to other binaries is
      usually unintentional.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalOmitResourcesInfoProviderFromAndroidBinary = Flag.Boolean("experimentalOmitResourcesInfoProviderFromAndroidBinary")

  @Option(
    name = "experimental_omitfp",
    defaultValue = "false",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, use libunwind for stack unwinding, and compile with -fomit-frame-pointer and
      -fasynchronous-unwind-tables.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalOmitfp = Flag.Boolean("experimentalOmitfp")

  @Option(
    name = "experimental_one_version_enforcement",
    defaultValue = "OFF",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      When enabled, enforce that a java_binary rule can't contain more than one version of the same class file on
      the classpath. This enforcement can break the build, or can just result in warnings.
    """,
    valueHelp = "off, warning or error",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalOneVersionEnforcement = Flag.OneOf("experimentalOneVersionEnforcement")

  @Option(
    name = "experimental_one_version_enforcement_use_transitive_jars_for_binary_under_test",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.ACTION_COMMAND_LINES],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If enabled, one version enforcement for android_test uses the binary_under_test's transitive classpath,
      otherwise it uses the deploy jar
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalOneVersionEnforcementUseTransitiveJarsForBinaryUnderTest =
    Flag.Boolean("experimentalOneVersionEnforcementUseTransitiveJarsForBinaryUnderTest")

  @Option(
    name = "experimental_oom_sensitive_skyfunctions_semaphore_size",
    defaultValue = "HOST_CPUS",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Sets the size of the semaphore used to prevent SkyFunctions with large peak memory requirement from OOM-ing
      blaze. A value of 0 indicates that no semaphore should be used. Example value: "HOST_CPUS*0.5".
    """,
    valueHelp = """an integer, or "HOST_CPUS", optionally followed by [-|*]<float>.""",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalOomSensitiveSkyfunctionsSemaphoreSize = Flag.Unknown("experimentalOomSensitiveSkyfunctionsSemaphoreSize")

  @Option(
    name = "experimental_output_directory_naming_scheme",
    defaultValue = "diff_against_dynamic_baseline",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Please only use this flag as part of a suggested migration or testing strategy. In legacy mode, transitions
      (generally only Starlark) set and use `affected by Starlark transition` to determine the ST hash. In
      diff_against_baseline mode, `affected by Starlark transition` is ignored and instead ST hash is determined,
      for all configuration, by diffing against the top-level configuration.
    """,
    valueHelp = "legacy, diff_against_baseline or diff_against_dynamic_baseline",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalOutputDirectoryNamingScheme = Flag.OneOf("experimentalOutputDirectoryNamingScheme")

  @Option(
    name = "experimental_output_paths",
    defaultValue = "off",
    effectTags = [
      OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION,
    ],
    help = """
      Which model to use for where in the output tree rules write their outputs, particularly for multi-platform /
      multi-configuration builds. This is highly experimental. See https://github.com/bazelbuild/bazel/issues/6526
      for details. Starlark actions canopt into path mapping by adding the key 'supports-path-mapping' to the
      'execution_requirements' dict.
    """,
    valueHelp = "off, content or strip",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalOutputPaths = Flag.OneOf("experimentalOutputPaths")

  @Option(
    name = "experimental_override_name_platform_in_output_dir",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Each entry should be of the form label=value where label refers to a platform and values is the desired
      shortname to use in the output path. Only used when --experimental_platform_in_output_dir is true. Has
      highest naming priority.
    """,
    valueHelp = "a 'label=value' assignment",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalOverrideNamePlatformInOutputDir = Flag.Unknown("experimentalOverrideNamePlatformInOutputDir")

  @Option(
    name = "experimental_parallel_aquery_output",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalParallelAqueryOutput = Flag.Boolean("experimentalParallelAqueryOutput")

  @Option(
    name = "experimental_parse_headers_skipped_if_corresponding_srcs_found",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalParseHeadersSkippedIfCorrespondingSrcsFound =
    Flag.Boolean("experimentalParseHeadersSkippedIfCorrespondingSrcsFound")

  @Option(
    name = "experimental_persistent_aar_extractor",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Enable persistent aar extractor by using workers.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPersistentAarExtractor = Flag.Boolean("experimentalPersistentAarExtractor")

  @Option(
    name = "experimental_platform_cc_test",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If enabled, a Starlark version of cc_test can be used which will use platform-based toolchain() resolution to
      choose a test runner.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPlatformCcTest = Flag.Boolean("experimentalPlatformCcTest")

  @Option(
    name = "experimental_platform_in_output_dir",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, a shortname for the target platform is used in the output directory name instead of the CPU. The
      exact scheme is experimental and subject to change: First, in the rare case the --platforms option does not
      have exactly one value, a hash of the platforms option is used. Next, if any shortname for the current
      platform was registered by --experimental_override_name_platform_in_output_dir, then that shortname is used.
      Then, if --experimental_use_platforms_in_output_dir_legacy_heuristic is set, use a shortname based off the
      current platform Label. Finally, a hash of the platform option is used as a last resort.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPlatformInOutputDir = Flag.Boolean("experimentalPlatformInOutputDir")

  @Option(
    name = "experimental_platforms_api",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If set to true, enables a number of platform-related Starlark APIs useful for debugging.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPlatformsApi = Flag.Boolean("experimentalPlatformsApi")

  @Option(
    name = "experimental_post_profile_started_event",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPostProfileStartedEvent = Flag.Boolean("experimentalPostProfileStartedEvent")

  @Option(
    name = "experimental_prefer_mutual_xcode",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, use the most recent Xcode that is available both locally and remotely. If false, or if there are no
      mutual available versions, use the local Xcode version selected via xcode-select.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPreferMutualXcode = Flag.Boolean("experimentalPreferMutualXcode")

  @Option(
    name = "experimental_process_wrapper_graceful_sigterm",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      When true, make the process-wrapper propagate SIGTERMs (used by the dynamic scheduler to stop process trees)
      to the subprocesses themselves, giving them the grace period in --local_termination_grace_seconds before
      forcibly sending a SIGKILL.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalProcessWrapperGracefulSigterm = Flag.Boolean("experimentalProcessWrapperGracefulSigterm")

  @Option(
    name = "experimental_profile_action_counts",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalProfileActionCounts = Flag.Boolean("experimentalProfileActionCounts")

  @Option(
    name = "experimental_profile_additional_tasks",
    allowMultiple = true,
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Specifies additional profile tasks to be included in the profile.",
    valueHelp = """
      phase, action, discover_inputs, action_check, action_lock, action_update, action_complete,
      action_rewinding, bzlmod, info, create_package, remote_execution, local_execution, scanner,
      local_parse, upload_time, remote_process_time, remote_queue, remote_setup, fetch,
      local_process_time, vfs_stat, vfs_dir, vfs_readlink, vfs_md5, vfs_xattr, vfs_delete, vfs_open,
      vfs_read, vfs_write, vfs_glob, vfs_vmfs_stat, vfs_vmfs_dir, vfs_vmfs_read, wait, thread_name,
      thread_sort_index, skyframe_eval, skyfunction, critical_path, critical_path_component,
      handle_gc_notification, local_action_counts, starlark_parser, starlark_user_fn,
      starlark_builtin_fn, starlark_user_compiled_fn, starlark_repository_fn, action_fs_staging,
      remote_cache_check, remote_download, remote_network, filesystem_traversal, worker_execution,
      worker_setup, worker_borrow, worker_working, worker_copying_outputs, credential_helper,
      conflict_check, dynamic_lock, repository_fetch, repository_vendor or unknown
    """,
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalProfileAdditionalTasks = Flag.OneOf("experimentalProfileAdditionalTasks")

  @Option(
    name = "experimental_profile_include_primary_output",
    oldName = "experimental_include_primary_output",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      Includes the extra "out" attribute in action events that contains the exec path to the action's primary
      output.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalProfileIncludePrimaryOutput = Flag.Boolean("experimentalProfileIncludePrimaryOutput")

  @Option(
    name = "experimental_profile_include_target_configuration",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Includes target configuration hash in action events' JSON profile data.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalProfileIncludeTargetConfiguration = Flag.Boolean("experimentalProfileIncludeTargetConfiguration")

  @Option(
    name = "experimental_profile_include_target_label",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Includes target label in action events' JSON profile data.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalProfileIncludeTargetLabel = Flag.Boolean("experimentalProfileIncludeTargetLabel")

  @Option(
    name = "experimental_propagate_custom_flag",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Which custom flags (starlark flags or defines) to propagate to the exec transition, by key. e.g. if
      '--define=a=b' should be propagated, set `--experimental_propagate_custom_flag=a`
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPropagateCustomFlag = Flag.Str("experimentalPropagateCustomFlag")

  @Option(
    name = "experimental_proto_descriptor_sets_include_source_info",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Run extra actions for alternative Java api versions in a proto_library.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalProtoDescriptorSetsIncludeSourceInfo = Flag.Boolean("experimentalProtoDescriptorSetsIncludeSourceInfo")

  @Option(
    name = "experimental_proto_extra_actions",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Deprecated. No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalProtoExtraActions = Flag.Boolean("experimentalProtoExtraActions")

  @Option(
    name = "experimental_publish_package_metrics_in_bep",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Whether to publish package metrics in the BEP.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPublishPackageMetricsInBep = Flag.Boolean("experimentalPublishPackageMetricsInBep")

  @Option(
    name = "experimental_py_binaries_include_label",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "py_binary targets include their label even when stamping is disabled.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPyBinariesIncludeLabel = Flag.Boolean("experimentalPyBinariesIncludeLabel")

  @Option(
    name = "experimental_python_import_all_repositories",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, the roots of repositories in the runfiles tree are added to PYTHONPATH, so that imports like `import
      mytoplevelpackage.package.module` are valid. Regardless of whether this flag is true, the runfiles root
      itself is also added to the PYTHONPATH, so `import myreponame.mytoplevelpackage.package.module` is valid. The
      latter form is less likely to experience import name collisions.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalPythonImportAllRepositories = Flag.Boolean("experimentalPythonImportAllRepositories")

  @Option(
    name = "experimental_record_metrics_for_all_mnemonics",
    defaultValue = "false",
    help = """
      Controls the output of BEP ActionSummary and BuildGraphMetrics, limiting the number of mnemonics in
      ActionData and number of entries reported in BuildGraphMetrics.AspectCount/RuleClassCount. By default the
      number of types is limited to the top 20, by number of executed actions for ActionData, and instances for
      RuleClass and Asepcts. Setting this option will write statistics for all mnemonics, rule classes and aspects.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRecordMetricsForAllMnemonics = Flag.Boolean("experimentalRecordMetricsForAllMnemonics")

  @Option(
    name = "experimental_record_skyframe_metrics",
    defaultValue = "false",
    help = """
      Controls the output of BEP BuildGraphMetrics, including expensiveto compute skyframe metrics about Skykeys,
      RuleClasses and Aspects.With this flag set to false BuildGraphMetrics.rule_count and aspectfields will not be
      populated in the BEP.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRecordSkyframeMetrics = Flag.Boolean("experimentalRecordSkyframeMetrics")

  @Option(
    name = "experimental_remotable_source_manifests",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Whether to make source manifest actions remotable",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemotableSourceManifests = Flag.Boolean("experimentalRemotableSourceManifests")

  @Option(
    name = "experimental_remote_analysis_cache",
    defaultValue = "",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "The URL for the remote analysis caching backend.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteAnalysisCache = Flag.Str("experimentalRemoteAnalysisCache")

  @Option(
    name = "experimental_remote_analysis_cache_concurrency",
    defaultValue = "4",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "Target concurrency for remote analysis caching RPCs.",
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteAnalysisCacheConcurrency = Flag.Integer("experimentalRemoteAnalysisCacheConcurrency")

  @Option(
    name = "experimental_remote_analysis_cache_deadline",
    defaultValue = "120s",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "Deadline to use for remote analysis cache operations.",
    valueHelp = "An immutable length of time.",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteAnalysisCacheDeadline = Flag.Duration("experimentalRemoteAnalysisCacheDeadline")

  @Option(
    name = "experimental_remote_analysis_cache_mode",
    defaultValue = "off",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "The transport direction for the remote analysis cache.",
    valueHelp = "upload, download or off",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteAnalysisCacheMode = Flag.OneOf("experimentalRemoteAnalysisCacheMode")

  @Option(
    name = "experimental_remote_cache_compression_threshold",
    defaultValue = "100",
    help = """
      The minimum blob size required to compress/decompress with zstd. Ineffectual unless
      --remote_cache_compression is set.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCacheCompressionThreshold = Flag.Integer("experimentalRemoteCacheCompressionThreshold")

  @Option(
    name = "experimental_remote_cache_eviction_retries",
    defaultValue = "5",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      The maximum number of attempts to retry if the build encountered a transient remote cache error that would
      otherwise fail the build. Applies for example when artifacts are evicted from the remote cache, or in certain
      cache failure conditions. A non-zero value will implicitly set
      --incompatible_remote_use_new_exit_code_for_lost_inputs to true. A new invocation id will be generated for
      each attempt. If you generate invocation id and provide it to Bazel with --invocation_id, you should not use
      this flag. Instead, set flag --incompatible_remote_use_new_exit_code_for_lost_inputs and check for the exit
      code 39.
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCacheEvictionRetries = Flag.Integer("experimentalRemoteCacheEvictionRetries")

  @Option(
    name = "experimental_remote_cache_lease_extension",
    defaultValue = "false",
    help = """
      If set to true, Bazel will extend the lease for outputs of remote actions during the build by sending
      `FindMissingBlobs` calls periodically to remote cache. The frequency is based on the value of
      `--experimental_remote_cache_ttl`.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCacheLeaseExtension = Flag.Boolean("experimentalRemoteCacheLeaseExtension")

  @Option(
    name = "experimental_remote_cache_ttl",
    defaultValue = "3h",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      The guaranteed minimal TTL of blobs in the remote cache after their digests are recently referenced e.g. by
      an ActionResult or FindMissingBlobs. Bazel does several optimizations based on the blobs' TTL e.g. doesn't
      repeatedly call GetActionResult in an incremental build. The value should be set slightly less than the real
      TTL since there is a gap between when the server returns the digests and when Bazel receives them.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCacheTtl = Flag.Duration("experimentalRemoteCacheTtl")

  @Option(
    name = "experimental_remote_capture_corrupted_outputs",
    help = "A path to a directory where the corrupted outputs will be captured to.",
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCaptureCorruptedOutputs = Flag.Path("experimentalRemoteCaptureCorruptedOutputs")

  @Option(
    name = "experimental_remote_discard_merkle_trees",
    defaultValue = "true",
    help = """
      If set to true, discard in-memory copies of the input root's Merkle tree and associated input mappings during
      calls to GetActionResult() and Execute(). This reduces memory usage significantly, but does require Bazel to
      recompute them upon remote cache misses and retries.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteDiscardMerkleTrees = Flag.Boolean("experimentalRemoteDiscardMerkleTrees")

  @Option(
    name = "experimental_remote_downloader",
    help = """
      A Remote Asset API endpoint URI, to be used as a remote download proxy. The supported schemas are grpc, grpcs
      (grpc with TLS enabled) and unix (local UNIX sockets). If no schema is provided Bazel will default to grpcs.
      See: https://github.com/bazelbuild/remote-apis/blob/master/build/bazel/remote/asset/v1/remote_asset.proto
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteDownloader = Flag.Str("experimentalRemoteDownloader")

  @Option(
    name = "experimental_remote_downloader_local_fallback",
    defaultValue = "false",
    help = "Whether to fall back to the local downloader if remote downloader fails.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteDownloaderLocalFallback = Flag.Boolean("experimentalRemoteDownloaderLocalFallback")

  @Option(
    name = "experimental_remote_downloader_propagate_credentials",
    defaultValue = "false",
    help = """
      Whether to propagate credentials from netrc and credential helper to the remote downloader server. The server
      implementation needs to support the new `http_header_url:<url-index>:<header-key>` qualifier where the
      `<url-index>` is a 0-based position of the URL inside the FetchBlobRequest's `uris` field. The URL-specific
      headers should take precedence over the global headers.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteDownloaderPropagateCredentials = Flag.Boolean("experimentalRemoteDownloaderPropagateCredentials")

  @Option(
    name = "experimental_remote_execution_keepalive",
    defaultValue = "false",
    help = "Whether to use keepalive for remote execution calls.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteExecutionKeepalive = Flag.Boolean("experimentalRemoteExecutionKeepalive")

  @Option(
    name = "experimental_remote_failure_rate_threshold",
    defaultValue = "10",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Sets the allowed number of failure rate in percentage for a specific time window after which it stops calling
      to the remote cache/executor. By default the value is 10. Setting this to 0 means no limitation.
    """,
    valueHelp = "an integer in 0-100 range",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteFailureRateThreshold = Flag.Unknown("experimentalRemoteFailureRateThreshold")

  @Option(
    name = "experimental_remote_failure_window_interval",
    defaultValue = "60s",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      The interval in which the failure rate of the remote requests are computed. On zero or negative value the
      failure duration is computed the whole duration of the execution.Following units can be used: Days (d), hours
      (h), minutes (m), seconds (s), and milliseconds (ms). If the unit is omitted, the value is interpreted as
      seconds.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteFailureWindowInterval = Flag.Duration("experimentalRemoteFailureWindowInterval")

  @Option(
    name = "experimental_remote_include_extraction_size_threshold",
    defaultValue = "1000000",
    effectTags = [
      OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.EXECUTION,
      OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS,
    ],
    help = "Run remotable C++ include extraction remotely if the file size in bytes exceeds this.",
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteIncludeExtractionSizeThreshold = Flag.Integer("experimentalRemoteIncludeExtractionSizeThreshold")

  @Option(
    name = "experimental_remote_mark_tool_inputs",
    defaultValue = "false",
    help = """
      If set to true, Bazel will mark inputs as tool inputs for the remote executor. This can be used to implement
      remote persistent workers.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteMarkToolInputs = Flag.Boolean("experimentalRemoteMarkToolInputs")

  @Option(
    name = "experimental_remote_merkle_tree_cache",
    defaultValue = "false",
    help = """
      If set to true, Merkle tree calculations will be memoized to improve the remote cache hit checking speed. The
      memory foot print of the cache is controlled by --experimental_remote_merkle_tree_cache_size.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteMerkleTreeCache = Flag.Boolean("experimentalRemoteMerkleTreeCache")

  @Option(
    name = "experimental_remote_merkle_tree_cache_size",
    defaultValue = "1000",
    help = """
      The number of Merkle trees to memoize to improve the remote cache hit checking speed. Even though the cache
      is automatically pruned according to Java's handling of soft references, out-of-memory errors can occur if
      set too high. If set to 0  the cache size is unlimited. Optimal value varies depending on project's size.
      Default to 1000.
    """,
    valueHelp = "a long integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteMerkleTreeCacheSize = Flag.Unknown("experimentalRemoteMerkleTreeCacheSize")

  @Option(
    name = "experimental_remote_output_service",
    help = """
      HOST or HOST:PORT of a remote output service endpoint. The supported schemas are grpc, grpcs (grpc with TLS
      enabled) and unix (local UNIX sockets). If no schema is provided Bazel will default to grpcs. Specify grpc://
      or unix: schema to disable TLS.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteOutputService = Flag.Str("experimentalRemoteOutputService")

  @Option(
    name = "experimental_remote_output_service_output_path_prefix",
    defaultValue = "",
    help = """
      The path under which the contents of output directories managed by the --experimental_remote_output_service
      are placed. The actual output directory used by a build will be a descendant of this path and determined by
      the output service.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteOutputServiceOutputPathPrefix = Flag.Str("experimentalRemoteOutputServiceOutputPathPrefix")

  @Option(
    name = "experimental_remote_require_cached",
    defaultValue = "false",
    help = """
      If set to true, enforce that all actions that can run remotely are cached, or else fail the build. This is
      useful to troubleshoot non-determinism issues as it allows checking whether actions that should be cached are
      actually cached without spuriously injecting new results into the cache.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteRequireCached = Flag.Boolean("experimentalRemoteRequireCached")

  @Option(
    name = "experimental_remote_scrubbing_config",
    help = """
      Enables remote cache key scrubbing with the supplied configuration file, which must be a protocol buffer in
      text format (see src/main/protobuf/remote_scrubbing.proto).
      
      This feature is intended to facilitate sharing a remote/disk cache between actions executing on different
      platforms but targeting the same platform. It should be used with extreme care, as improper settings may
      cause accidental sharing of cache entries and result in incorrect builds.
      
      Scrubbing does not affect how an action is executed, only how its remote/disk cache key is computed for the
      purpose of retrieving or storing an action result. Scrubbed actions are incompatible with remote execution,
      and will always be executed locally instead.
      
      Modifying the scrubbing configuration does not invalidate outputs present in the local filesystem or internal
      caches; a clean build is required to reexecute affected actions.
      
      In order to successfully use this feature, you likely want to set a custom --host_platform together with
      --experimental_platform_in_output_dir (to normalize output prefixes) and --incompatible_strict_action_env (to
      normalize environment variables).
    """,
    valueHelp = "Converts to a Scrubber",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteScrubbingConfig = Flag.Unknown("experimentalRemoteScrubbingConfig")

  @Option(
    name = "experimental_remove_r_classes_from_instrumentation_test_jar",
    defaultValue = "true",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If enabled and the test instruments an application, all the R classes from the test's deploy jar will be
      removed.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoveRClassesFromInstrumentationTestJar = Flag.Boolean("experimentalRemoveRClassesFromInstrumentationTestJar")

  @Option(
    name = "experimental_repo_remote_exec",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If set to true, repository_rule gains some remote execution capabilities.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRepoRemoteExec = Flag.Boolean("experimentalRepoRemoteExec")

  @Option(
    name = "experimental_repository_cache_hardlinks",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      If set, the repository cache will hardlink the file in case of a cache hit, rather than copying. This is
      intended to save disk space.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRepositoryCacheHardlinks = Flag.Boolean("experimentalRepositoryCacheHardlinks")

  @Option(
    name = "experimental_repository_downloader_retries",
    defaultValue = "5",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "The maximum number of attempts to retry a download error. If set to 0, retries are disabled.",
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRepositoryDownloaderRetries = Flag.Integer("experimentalRepositoryDownloaderRetries")

  @Option(
    name = "experimental_repository_resolved_file",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If non-empty, write a Starlark value with the resolved information of all Starlark repository rules that were
      executed.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRepositoryResolvedFile = Flag.Str("experimentalRepositoryResolvedFile")

  @Option(
    name = "experimental_require_availability_info",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRequireAvailabilityInfo = Flag.Boolean("experimentalRequireAvailabilityInfo")

  @Option(
    name = "experimental_resolved_file_instead_of_workspace",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = "If non-empty read the specified resolved file instead of the WORKSPACE file",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalResolvedFileInsteadOfWorkspace = Flag.Str("experimentalResolvedFileInsteadOfWorkspace")

  @Option(
    name = "experimental_retain_test_configuration_across_testonly",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      When enabled, --trim_test_configuration will not trim the test configuration for rules marked testonly=1.
      This is meant to reduce action conflict issues when non-test rules depend on cc_test rules. No effect if
      --trim_test_configuration is false.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRetainTestConfigurationAcrossTestonly = Flag.Boolean("experimentalRetainTestConfigurationAcrossTestonly")

  @Option(
    name = "experimental_reuse_include_scanning_threads",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.EXECUTION,
      OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS,
    ],
    help = "If enabled core threads of include scanner pool will not die during execution.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalReuseIncludeScanningThreads = Flag.Boolean("experimentalReuseIncludeScanningThreads")

  @Option(
    name = "experimental_rule_extension_api",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Enable experimental rule extension API and subrule APIs",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRuleExtensionApi = Flag.Boolean("experimentalRuleExtensionApi")

  @Option(
    name = "experimental_run_android_lint_on_java_rules",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Whether to validate java_* sources.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRunAndroidLintOnJavaRules = Flag.Boolean("experimentalRunAndroidLintOnJavaRules")

  @Option(
    name = "experimental_run_bep_event_include_residue",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Whether to include the command-line residue in run build events which could contain the residue. By default,
      the residue is not included in run command build events that could contain the residue.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalRunBepEventIncludeResidue = Flag.Boolean("experimentalRunBepEventIncludeResidue")

  @Option(
    name = "experimental_sandbox_async_tree_delete_idle_threads",
    defaultValue = "4",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """
      If 0, delete sandbox trees as soon as an action completes (causing completion of the action to be delayed).
      If greater than zero, execute the deletion of such threes on an asynchronous thread pool that has size 1 when
      the build is running and grows to the size specified by this flag when the server is idle.
    """,
    valueHelp = """
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation
      ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSandboxAsyncTreeDeleteIdleThreads = Flag.Unknown("experimentalSandboxAsyncTreeDeleteIdleThreads")

  @Option(
    name = "experimental_sandbox_enforce_resources_regexp",
    defaultValue = "",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If true, actions whose mnemonic matches the input regex will have their resources request enforced as limits,
      overriding the value of --experimental_sandbox_limits, if the resource type supports it. For example a test
      that declares cpu:3 and resources:memory:10, will run with at most 3 cpus and 10 megabytes of memory.
    """,
    valueHelp = "a valid Java regular expression",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSandboxEnforceResourcesRegexp = Flag.Unknown("experimentalSandboxEnforceResourcesRegexp")

  @Option(
    name = "experimental_sandbox_limits",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If > 0, each Linux sandbox will be limited to the given amount for the specified resource. Requires
      --incompatible_use_new_cgroup_implementation and overrides --experimental_sandbox_memory_limit_mb. Requires
      cgroups v1 or v2 and permissions for the users to the cgroups dir.
    """,
    valueHelp = """
      a named double, 'name=value', where value is an integer, or a keyword ("auto", "HOST_CPUS",
      "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSandboxLimits = Flag.Unknown("experimentalSandboxLimits")

  @Option(
    name = "experimental_sandbox_memory_limit_mb",
    defaultValue = "0",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If > 0, each Linux sandbox will be limited to the given amount of memory (in MB). Requires cgroups v1 or v2
      and permissions for the users to the cgroups dir.
    """,
    valueHelp = """an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.""",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSandboxMemoryLimitMb = Flag.Unknown("experimentalSandboxMemoryLimitMb")

  @Option(
    name = "experimental_sandboxfs_map_symlink_targets",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = "No-op",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSandboxfsMapSymlinkTargets = Flag.Boolean("experimentalSandboxfsMapSymlinkTargets")

  @Option(
    name = "experimental_save_feature_state",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Save the state of enabled and requested feautres as an output of compilation.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSaveFeatureState = Flag.Boolean("experimentalSaveFeatureState")

  @Option(
    name = "experimental_scale_timeouts",
    defaultValue = "1.0",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Scale all timeouts in Starlark repository rules by this factor. In this way, external repositories can be
      made working on machines that are slower than the rule author expected, without changing the source code
    """,
    valueHelp = "a double",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalScaleTimeouts = Flag.Double("experimentalScaleTimeouts")

  @Option(
    name = "experimental_show_artifacts",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalShowArtifacts = Flag.Boolean("experimentalShowArtifacts")

  @Option(
    name = "experimental_shrink_worker_pool",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      If enabled, could shrink worker pool if worker memory pressure is high. This flag works only when flag
      experimental_total_worker_memory_limit_mb is enabled.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalShrinkWorkerPool = Flag.Boolean("experimentalShrinkWorkerPool")

  @Option(
    name = "experimental_sibling_repository_layout",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
      OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If set to true, non-main repositories are planted as symlinks to the main repository in the execution root.
      That is, all repositories are direct children of the ${'$'}output_base/execution_root directory. This has the
      side effect of freeing up ${'$'}output_base/execution_root/__main__/external for the real top-level
      'external' directory.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSiblingRepositoryLayout = Flag.Boolean("experimentalSiblingRepositoryLayout")

  @Option(
    name = "experimental_single_package_toolchain_binding",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If enabled, the register_toolchain function may not include target patterns which may refer to more than one
      package.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSinglePackageToolchainBinding = Flag.Boolean("experimentalSinglePackageToolchainBinding")

  @Option(
    name = "experimental_skip_ttvs_for_genquery",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, genquery loads its scope's transitive closure directly instead of by using 'TransitiveTargetValue'
      Skyframe work.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkipTtvsForGenquery = Flag.Boolean("experimentalSkipTtvsForGenquery")

  @Option(
    name = "experimental_skyfocus_dump_keys",
    defaultValue = "none",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "For debugging Skyfocus. Dump the focused SkyKeys (roots, leafs, focused deps, focused rdeps).",
    valueHelp = "none, count or verbose",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkyfocusDumpKeys = Flag.OneOf("experimentalSkyfocusDumpKeys")

  @Option(
    name = "experimental_skyfocus_dump_post_gc_stats",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      For debugging Skyfocus. If enabled, trigger manual GC before/after focusing to report heap sizes reductions.
      This will increase the Skyfocus latency.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkyfocusDumpPostGcStats = Flag.Boolean("experimentalSkyfocusDumpPostGcStats")

  @Option(
    name = "experimental_skyfocus_handling_strategy",
    defaultValue = "strict",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = "Strategies for Skyfocus to handle changes outside of the working set.",
    valueHelp = "strict or warn",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkyfocusHandlingStrategy = Flag.OneOf("experimentalSkyfocusHandlingStrategy")

  @Option(
    name = "experimental_skyframe_cpu_heavy_skykeys_thread_pool_size",
    defaultValue = "HOST_CPUS",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If set to a positive value (e.g. "HOST_CPUS*1.5"), Skyframe will run the loading/analysis phase with 2
      separate thread pools: 1 with <value> threads (ideally close to HOST_CPUS) reserved for CPU-heavy SkyKeys,
      and 1 "standard" thread pool (whose size is controlled by --loading_phase_threads) for the rest.
    """,
    valueHelp = """an integer, or "HOST_CPUS", optionally followed by [-|*]<float>.""",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkyframeCpuHeavySkykeysThreadPoolSize = Flag.Unknown("experimentalSkyframeCpuHeavySkykeysThreadPoolSize")

  @Option(
    name = "experimental_skyframe_memory_dump",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Dump the memory use of individual nodes in the Skyframe graph after the build. This option takes a number of
      flags separated by commas: 'json' (no-op, that's the only format), 'notransient' (don't traverse transient
      fields), 'noconfig' (ignore objects related to configurations), 'noprecomputed' (ignore precomputed values)
      and 'noworkspacestatus' (ignore objects related to the workspace status machinery)
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkyframeMemoryDump = Flag.Str("experimentalSkyframeMemoryDump")

  @Option(
    name = "experimental_skyframe_native_filesets",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkyframeNativeFilesets = Flag.Boolean("experimentalSkyframeNativeFilesets")

  @Option(
    name = "experimental_skyframe_prepare_analysis",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated. No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkyframePrepareAnalysis = Flag.Boolean("experimentalSkyframePrepareAnalysis")

  @Option(
    name = "experimental_skyframe_target_pattern_evaluator",
    defaultValue = "true",
    help = """
      Use the Skyframe-based target pattern evaluator; implies --experimental_interleave_loading_and_analysis.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkyframeTargetPatternEvaluator = Flag.Boolean("experimentalSkyframeTargetPatternEvaluator")

  @Option(
    name = "experimental_skylark_debug",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, Blaze will open the Starlark debug server at the start of the build invocation, and wait for a
      debugger to attach before running the build.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkylarkDebug = Flag.Boolean("experimentalSkylarkDebug")

  @Option(
    name = "experimental_skylark_debug_reset_analysis",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If true, resets analysis before executing the build. Has no effect without --experimental_skylark_debug
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkylarkDebugResetAnalysis = Flag.Boolean("experimentalSkylarkDebugResetAnalysis")

  @Option(
    name = "experimental_skylark_debug_server_port",
    defaultValue = "7300",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "The port on which the Starlark debug server will listen for connections.",
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkylarkDebugServerPort = Flag.Integer("experimentalSkylarkDebugServerPort")

  @Option(
    name = "experimental_skylark_debug_verbose_logging",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Show verbose logs for the debugger.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkylarkDebugVerboseLogging = Flag.Boolean("experimentalSkylarkDebugVerboseLogging")

  @Option(
    name = "experimental_skymeld_analysis_overlap_percentage",
    defaultValue = "100",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      The value represents the % of the analysis phase which will be overlapped with the execution phase. A value
      of x means Skyframe will queue up execution tasks and wait until there's x% of the top level target left to
      be analyzed before allowing them to launch. When the value is 0%, we'd wait for all analysis to finish before
      executing (no overlap). When it's 100%, the phases are free to overlap as much as they can.
    """,
    valueHelp = "an integer in 0-100 range",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSkymeldAnalysisOverlapPercentage = Flag.Unknown("experimentalSkymeldAnalysisOverlapPercentage")

  @Option(
    name = "experimental_spawn_scheduler",
    expandsTo = ["--internal_spawn_scheduler", "--spawn_strategy=dynamic"],
    help = """
      Enable dynamic execution by running actions locally and remotely in parallel. Bazel spawns each action
      locally and remotely and picks the one that completes first. If an action supports workers, the local action
      will be run in the persistent worker mode. To enable dynamic execution for an individual action mnemonic, use
      the `--internal_spawn_scheduler` and `--strategy=<mnemonic>=dynamic` flags instead.
    """,
    valueHelp = "",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSpawnScheduler = Flag.Unknown("experimentalSpawnScheduler")

  @Option(
    name = "experimental_split_coverage_postprocessing",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If true, then Bazel will run coverage postprocessing for test in a new spawn.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSplitCoveragePostprocessing = Flag.Boolean("experimentalSplitCoveragePostprocessing")

  @Option(
    name = "experimental_split_xml_generation",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If this flag is set, and a test action does not generate a test.xml file, then Bazel uses a separate action
      to generate a dummy test.xml file containing the test log. Otherwise, Bazel generates a test.xml as part of
      the test action.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalSplitXmlGeneration = Flag.Boolean("experimentalSplitXmlGeneration")

  @Option(
    name = "experimental_starlark_cc_import",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If enabled, the Starlark version of cc_import can be used.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalStarlarkCcImport = Flag.Boolean("experimentalStarlarkCcImport")

  @Option(
    name = "experimental_stats_summary",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Enable a modernized summary of the build stats.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalStatsSummary = Flag.Boolean("experimentalStatsSummary")

  @Option(
    name = "experimental_stream_log_file_uploads",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Stream log file uploads directly to the remote storage rather than writing them to disk.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalStreamLogFileUploads = Flag.Boolean("experimentalStreamLogFileUploads")

  @Option(
    name = "experimental_strict_fileset_output",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If this option is enabled, filesets will treat all output artifacts as regular files. They will not traverse
      directories or be sensitive to symlinks.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalStrictFilesetOutput = Flag.Boolean("experimentalStrictFilesetOutput")

  @Option(
    name = "experimental_strict_java_deps",
    oldName = "strict_java_deps",
    defaultValue = "default",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      If true, checks that a Java target explicitly declares all directly used targets as dependencies.
    """,
    valueHelp = "off, warn, error, strict or default",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalStrictJavaDeps = Flag.OneOf("experimentalStrictJavaDeps")

  @Option(
    name = "experimental_throttle_action_cache_check",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Whether to throttle the check whether an action is cached.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalThrottleActionCacheCheck = Flag.Boolean("experimentalThrottleActionCacheCheck")

  @Option(
    name = "experimental_throttle_remote_action_building",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Whether to throttle the building of remote action to avoid OOM. Defaults to true.
      
      This is a temporary flag to allow users switch off the behaviour. Once Bazel is smart enough about the
      RAM/CPU usages, this flag will be removed.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalThrottleRemoteActionBuilding = Flag.Boolean("experimentalThrottleRemoteActionBuilding")

  @Option(
    name = "experimental_tool_command_line",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL, OptionMetadataTag.HIDDEN],
    help = """
      An extra command line to report with this invocation's command line. Useful for tools that invoke Bazel and
      want the original information that the tool received to be logged with the rest of the Bazel invocation.
    """,
    valueHelp = "A command line, either as a simple string, or as a base64-encoded binary form of a CommandLine proto",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalToolCommandLine = Flag.Unknown("experimentalToolCommandLine")

  @Option(
    name = "experimental_total_worker_memory_limit_mb",
    defaultValue = "0",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      If this limit is greater than zero idle workers might be killed if the total memory usage of all  workers
      exceed the limit.
    """,
    valueHelp = """an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.""",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalTotalWorkerMemoryLimitMb = Flag.Unknown("experimentalTotalWorkerMemoryLimitMb")

  @Option(
    name = "experimental_turbine_annotation_processing",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If enabled, turbine is used for all annotation processing",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalTurbineAnnotationProcessing = Flag.Boolean("experimentalTurbineAnnotationProcessing")

  @Option(
    name = "experimental_ui_debug_all_events",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "Report all events known to the Bazel UI.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUiDebugAllEvents = Flag.Boolean("experimentalUiDebugAllEvents")

  @Option(
    name = "experimental_ui_max_stdouterr_bytes",
    defaultValue = "1048576",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      The maximum size of the stdout / stderr files that will be printed to the console. -1 implies no limit.
    """,
    valueHelp = "an integer in (-1)-1073741819 range",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUiMaxStdouterrBytes = Flag.Unknown("experimentalUiMaxStdouterrBytes")

  @Option(
    name = "experimental_unsupported_and_brittle_include_scanning",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Whether to narrow inputs to C/C++ compilation by parsing #include lines from input files. This can improve
      performance and incrementality by decreasing the size of compilation input trees. However, it can also break
      builds because the include scanner does not fully implement C preprocessor semantics. In particular, it does
      not understand dynamic #include directives and ignores preprocessor conditional logic. Use at your own risk.
      Any issues relating to this flag that are filed will be closed.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUnsupportedAndBrittleIncludeScanning = Flag.Boolean("experimentalUnsupportedAndBrittleIncludeScanning")

  @Option(
    name = "experimental_use_cpp_compile_action_args_params_file",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If enabled, write CppCompileAction exposed action.args to parameters file.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseCppCompileActionArgsParamsFile = Flag.Boolean("experimentalUseCppCompileActionArgsParamsFile")

  @Option(
    name = "experimental_use_dex_splitter_for_incremental_dexing",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Do not use.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseDexSplitterForIncrementalDexing = Flag.Boolean("experimentalUseDexSplitterForIncrementalDexing")

  @Option(
    name = "experimental_use_event_based_build_completion_status",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "No-op",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseEventBasedBuildCompletionStatus = Flag.Boolean("experimentalUseEventBasedBuildCompletionStatus")

  @Option(
    name = "experimental_use_hermetic_linux_sandbox",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If set to true, do not mount root, only mount whats provided with sandbox_add_mount_pair. Input files will be
      hardlinked to the sandbox instead of symlinked to from the sandbox. If action input files are located on a
      filesystem different from the sandbox, then the input files will be copied instead.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseHermeticLinuxSandbox = Flag.Boolean("experimentalUseHermeticLinuxSandbox")

  @Option(
    name = "experimental_use_llvm_covmap",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If specified, Bazel will generate llvm-cov coverage map information rather than gcov when
      collect_code_coverage is enabled.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseLlvmCovmap = Flag.Boolean("experimentalUseLlvmCovmap")

  @Option(
    name = "experimental_use_new_worker_pool",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Uses a new worker pool implementation (no change in behavior, reimplementation of  the worker pool in order
      to deprecate the use of a third party tool).
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseNewWorkerPool = Flag.Boolean("experimentalUseNewWorkerPool")

  @Option(
    name = "experimental_use_platforms_in_output_dir_legacy_heuristic",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Please only use this flag as part of a suggested migration or testing strategy. Note that the heuristic has
      known deficiencies and it is suggested to migrate to relying on just
      --experimental_override_name_platform_in_output_dir.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUsePlatformsInOutputDirLegacyHeuristic = Flag.Boolean("experimentalUsePlatformsInOutputDirLegacyHeuristic")

  @Option(
    name = "experimental_use_rtxt_from_merged_resources",
    defaultValue = "false",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "Use R.txt from the merging action, instead of from the validation action.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseRtxtFromMergedResources = Flag.Boolean("experimentalUseRtxtFromMergedResources")

  @Option(
    name = "experimental_use_scheduling_middlemen",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated. No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseSchedulingMiddlemen = Flag.Boolean("experimentalUseSchedulingMiddlemen")

  @Option(
    name = "experimental_use_semaphore_for_jobs",
    defaultValue = "true",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = "If set to true, additionally use semaphore to limit number of concurrent jobs.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseSemaphoreForJobs = Flag.Boolean("experimentalUseSemaphoreForJobs")

  @Option(
    name = "experimental_use_validation_aspect",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Whether to run validation actions using aspect (for parallelism with tests).",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseValidationAspect = Flag.Boolean("experimentalUseValidationAspect")

  @Option(
    name = "experimental_use_windows_sandbox",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Use Windows sandbox to run actions. If "yes", the binary provided by --experimental_windows_sandbox_path must
      be valid and correspond to a supported version of sandboxfs. If "auto", the binary may be missing or not
      compatible.
    """,
    valueHelp = "a tri-state (auto, yes, no)",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseWindowsSandbox = Flag.TriState("experimentalUseWindowsSandbox")

  @Option(
    name = "experimental_windows_sandbox_path",
    defaultValue = "BazelSandbox.exe",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Path to the Windows sandbox binary to use when --experimental_use_windows_sandbox is true. If a bare name,
      use the first binary of that name found in the PATH.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWindowsSandboxPath = Flag.Str("experimentalWindowsSandboxPath")

  @Option(
    name = "experimental_windows_watchfs",
    defaultValue = "false",
    help = """
      If true, experimental Windows support for --watchfs is enabled. Otherwise --watchfsis a non-op on Windows.
      Make sure to also enable --watchfs.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWindowsWatchfs = Flag.Boolean("experimentalWindowsWatchfs")

  @Option(
    name = "experimental_worker_allowlist",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = "If non-empty, only allow using persistent workers with the given worker key mnemonic.",
    valueHelp = "comma-separated set of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerAllowlist = Flag.Unknown("experimentalWorkerAllowlist")

  @Option(
    name = "experimental_worker_as_resource",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op, will be removed soon.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerAsResource = Flag.Boolean("experimentalWorkerAsResource")

  @Option(
    name = "experimental_worker_cancellation",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = "If enabled, Bazel may send cancellation requests to workers that support them.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerCancellation = Flag.Boolean("experimentalWorkerCancellation")

  @Option(
    name = "experimental_worker_for_repo_fetching",
    defaultValue = "auto",
    help = """
      The threading mode to use for repo fetching. If set to 'off', no worker thread is used, and the repo fetching
      is subject to restarts. Otherwise, uses a virtual worker thread.
    """,
    valueHelp = "off, platform, virtual or auto",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerForRepoFetching = Flag.OneOf("experimentalWorkerForRepoFetching")

  @Option(
    name = "experimental_worker_memory_limit_mb",
    defaultValue = "0",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      If this limit is greater than zero, workers might be killed if the memory usage of the worker exceeds the
      limit. If not used together with dynamic execution and `--experimental_dynamic_ignore_local_signals=9`, this
      may crash your build.
    """,
    valueHelp = """an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.""",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerMemoryLimitMb = Flag.Unknown("experimentalWorkerMemoryLimitMb")

  @Option(
    name = "experimental_worker_metrics_poll_interval",
    defaultValue = "5s",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      The interval between collecting worker metrics and possibly attempting evictions. Cannot effectively be less
      than 1s for performance reasons.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerMetricsPollInterval = Flag.Duration("experimentalWorkerMetricsPollInterval")

  @Option(
    name = "experimental_worker_multiplex_sandboxing",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If enabled, multiplex workers with a 'supports-multiplex-sandboxing' execution requirement will run in a
      sandboxed environment, using a separate sandbox directory per work request. Multiplex workers with the
      execution requirement are always sandboxed when running under the dynamic execution strategy, irrespective of
      this flag.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerMultiplexSandboxing = Flag.Boolean("experimentalWorkerMultiplexSandboxing")

  @Option(
    name = "experimental_worker_sandbox_hardening",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If enabled, workers are run in a hardened sandbox, if the implementation allows it. If hardening is enabled
      then tmp directories are distinct for different workers.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerSandboxHardening = Flag.Boolean("experimentalWorkerSandboxHardening")

  @Option(
    name = "experimental_worker_sandbox_inmemory_tracking",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      A worker key mnemonic for which the contents of the sandbox directory are tracked in memory. This may improve
      build performance at the cost of additional memory usage. Only affects sandboxed workers. May be specified
      multiple times for different mnemonics.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerSandboxInmemoryTracking = Flag.Str("experimentalWorkerSandboxInmemoryTracking")

  @Option(
    name = "experimental_worker_strict_flagfiles",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If enabled, actions arguments for workers that do not follow the worker specification will cause an error.
      Worker arguments must have exactly one @flagfile argument as the last of its list of arguments.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerStrictFlagfiles = Flag.Boolean("experimentalWorkerStrictFlagfiles")

  @Option(
    name = "experimental_worker_use_cgroups_on_linux",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      On linux, run all workers in its own cgroup (without any limits set) and use the cgroup's own resource
      accounting for memory measurements. This is overridden by --experimental_worker_sandbox_hardening for
      sandboxed workers.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerUseCgroupsOnLinux = Flag.Boolean("experimentalWorkerUseCgroupsOnLinux")

  @Option(
    name = "experimental_working_set",
    defaultValue = "",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      The working set for Skyfocus. Specify as comma-separated workspace root-relative paths. This is a stateful
      flag. Defining a working set persists it for subsequent invocations, until it is redefined with a new set.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkingSet = Flag.Unknown("experimentalWorkingSet")

  @Option(
    name = "experimental_workspace_rules_log_file",
    help = "Log certain Workspace Rules events into this file as delimited WorkspaceEvent protos.",
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkspaceRulesLogFile = Flag.Path("experimentalWorkspaceRulesLogFile")

  @Option(
    name = "experimental_writable_outputs",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = "If true, the file permissions of action outputs are set to 0755 instead of 0555",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val experimentalWritableOutputs = Flag.Boolean("experimentalWritableOutputs")

  @Option(
    name = "explain",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Causes the build system to explain each executed step of the build. The explanation is written to the
      specified log file.
    """,
    valueHelp = "a path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val explain = Flag.Path("explain")

  @Option(
    name = "explicit_java_test_deps",
    defaultValue = "false",
    help = """
      Explicitly specify a dependency to JUnit or Hamcrest in a java_test instead of  accidentally obtaining from
      the TestRunner's deps. Only works for bazel right now.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val explicitJavaTestDeps = Flag.Boolean("explicitJavaTestDeps")

  @Option(
    name = "expunge",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      If true, clean removes the entire working tree for this %{product} instance, which includes all
      %{product}-created temporary and build output files, and stops the %{product} server if it is running.
    """,
    valueHelp = "a boolean",
    commands = ["clean"],
  )
  @JvmField
  @Suppress("unused")
  val expunge = Flag.Boolean("expunge")

  @Option(
    name = "expunge_async",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    expandsTo = ["--expunge", "--async"],
    help = """
      If specified, clean asynchronously removes the entire working tree for this %{product} instance, which
      includes all %{product}-created temporary and build output files, and stops the %{product} server if it is
      running. When this command completes, it will be safe to execute new commands in the same client, even though
      the deletion may continue in the background.
    """,
    valueHelp = "",
    commands = ["clean"],
  )
  @JvmField
  @Suppress("unused")
  val expungeAsync = Flag.Unknown("expungeAsync")

  @Option(
    name = "extension_filter",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Only display the usages of these module extensions and the repos generated by them if their respective flags
      are set. If set, the result graph will only include paths that contain modules using the specified
      extensions. An empty list disables the filter, effectively specifying all possible extensions.
    """,
    valueHelp = "a comma-separated list of <extension>s",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val extensionFilter = Flag.Unknown("extensionFilter")

  @Option(
    name = "extension_info",
    defaultValue = "hidden",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Specify how much detail about extension usages to include in the query result. "Usages" will only show the
      extensions names, "repos" will also include repos imported with use_repo, and "all" will also show the other
      repositories generated by extensions.
      
    """,
    valueHelp = "hidden, usages, repos or all",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val extensionInfo = Flag.OneOf("extensionInfo")

  @Option(
    name = "extension_usages",
    defaultValue = "",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Specify modules whose extension usages will be displayed in the show_extension query.",
    valueHelp = "a comma-separated list of <module>s",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val extensionUsages = Flag.Unknown("extensionUsages")

  @Option(
    name = "extra_execution_platforms",
    defaultValue = "",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      The platforms that are available as execution platforms to run actions. Platforms can be specified by exact
      target, or as a target pattern. These platforms will be considered before those declared in the WORKSPACE
      file by register_execution_platforms(). This option may only be set once; later instances will override
      earlier flag settings.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val extraExecutionPlatforms = Flag.Unknown("extraExecutionPlatforms")

  @Option(
    name = "extra_toolchains",
    allowMultiple = true,
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    help = """
      The toolchain rules to be considered during toolchain resolution. Toolchains can be specified by exact
      target, or as a target pattern. These toolchains will be considered before those declared in the WORKSPACE
      file by register_toolchains().
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val extraToolchains = Flag.Unknown("extraToolchains")

  @Option(
    name = "extract_data_time",
    defaultValue = "0",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "The time in ms spent on extracting the new bazel version.",
    valueHelp = "a long integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val extractDataTime = Flag.Unknown("extractDataTime")

  @Option(
    name = "failure_detail_out",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      If set, specifies a location to write a failure_detail protobuf message if the server experiences a failure
      and cannot report it via gRPC, as normal. Otherwise, the location will be
      ${'$'}{OUTPUT_BASE}/failure_detail.rawproto.
    """,
    valueHelp = "a path",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val failureDetailOut = Flag.Path("failureDetailOut")

  @Option(
    name = "fat_apk_cpu",
    defaultValue = "armeabi-v7a",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op",
    valueHelp = "comma-separated set of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val fatApkCpu = Flag.Unknown("fatApkCpu")

  @Option(
    name = "fat_apk_hwasan",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op flag. Will be removed in a future release.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val fatApkHwasan = Flag.Boolean("fatApkHwasan")

  @Option(
    name = "fatal_event_bus_exceptions",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      Whether or not to exit if an exception is thrown by an internal EventBus handler. No-op if
      --fatal_async_exceptions_exclusions is available; that flag's behavior is preferentially used.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val fatalEventBusExceptions = Flag.Boolean("fatalEventBusExceptions")

  @Option(
    name = "fdo_instrument",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Generate binaries with FDO instrumentation. With Clang/LLVM compiler, it also accepts the directory name
      under which the raw profile file(s) will be dumped at runtime.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val fdoInstrument = Flag.Str("fdoInstrument")

  @Option(
    name = "fdo_optimize",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Use FDO profile information to optimize compilation. Specify the name of a zip file containing a .gcda file
      tree, an afdo file containing an auto profile, or an LLVM profile file. This flag also accepts files
      specified as labels (e.g. `//foo/bar:file.afdo` - you may need to add an `exports_files` directive to the
      corresponding package) and labels pointing to `fdo_profile` targets. This flag will be superseded by the
      `fdo_profile` rule.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val fdoOptimize = Flag.Str("fdoOptimize")

  @Option(
    name = "fdo_prefetch_hints",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Use cache prefetch hints.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val fdoPrefetchHints = Flag.Label("fdoPrefetchHints")

  @Option(
    name = "fdo_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "The fdo_profile representing the profile to be used for optimization.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val fdoProfile = Flag.Label("fdoProfile")

  @Option(
    name = "features",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      The given features will be enabled or disabled by default for targets built in the target configuration.
      Specifying -<feature> will disable the feature. Negative features always override positive ones. See also
      --host_features
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val features = Flag.Str("features")

  @Option(
    name = "fetch",
    defaultValue = "true",
    help = """
      Allows the command to fetch external dependencies. If set to false, the command will utilize any cached
      version of the dependency, and if none exists, the command will result in failure.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val fetch = Flag.Boolean("fetch")

  @Option(
    name = "fission",
    defaultValue = "no",
    effectTags = [
      OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.ACTION_COMMAND_LINES,
      OptionEffectTag.AFFECTS_OUTPUTS,
    ],
    help = """
      Specifies which compilation modes use fission for C++ compilations and links.  May be any combination of
      {'fastbuild', 'dbg', 'opt'} or the special values 'yes'  to enable all modes and 'no' to disable all modes.
    """,
    valueHelp = "a set of compilation modes",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val fission = Flag.Unknown("fission")

  @Option(
    name = "flag_alias",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      Sets a shorthand name for a Starlark flag. It takes a single key-value pair in the form "<key>=<value>" as an
      argument.
    """,
    valueHelp = "a 'name=value' flag alias",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val flagAlias = Flag.Unknown("flagAlias")

  @Option(
    name = "flaky_test_attempts",
    defaultValue = "default",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Each test will be retried up to the specified number of times in case of any test failure. Tests that
      required more than one attempt to pass are marked as 'FLAKY' in the test summary. Normally the value
      specified is just an integer or the string 'default'. If an integer, then all tests will be run up to N
      times. If 'default', then only a single test attempt will be made for regular tests and three for tests
      marked explicitly as flaky by their rule (flaky=1 attribute). Alternate syntax:
      regex_filter@flaky_test_attempts. Where flaky_test_attempts is as above and regex_filter stands for a list of
      include and exclude regular expression patterns (Also see --runs_per_test). Example:
      --flaky_test_attempts=//foo/.*,-//foo/bar/.*@3 deflakes all tests in //foo/ except those under foo/bar three
      times. This option can be passed multiple times. The most recently passed argument that matches takes
      precedence. If nothing matches, behavior is as if 'default' above.
    """,
    valueHelp = """
      a positive integer, the string "default", or test_regex@attempts. This flag may be passed more than
      once
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val flakyTestAttempts = Flag.Unknown("flakyTestAttempts")

  @Option(
    name = "for_command",
    defaultValue = "build",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.TERMINAL_OUTPUT],
    help = "The command for which the options should be canonicalized.",
    valueHelp = "a string",
    commands = ["canonicalize-flags"],
  )
  @JvmField
  @Suppress("unused")
  val forCommand = Flag.Str("forCommand")

  @Option(
    name = "force",
    defaultValue = "false",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      Ignore existing repository if any and force fetch the repository again. Only works when --enable_bzlmod is on.
    """,
    valueHelp = "a boolean",
    commands = ["fetch"],
  )
  @JvmField
  @Suppress("unused")
  val force = Flag.Boolean("force")

  @Option(
    name = "force_ignore_dash_static",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    help = "noop",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val forceIgnoreDashStatic = Flag.Boolean("forceIgnoreDashStatic")

  @Option(
    name = "force_pic",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If enabled, all C++ compilations produce position-independent code ("-fPIC"), links prefer PIC pre-built
      libraries over non-PIC libraries, and links produce position-independent executables ("-pie").
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val forcePic = Flag.Boolean("forcePic")

  @Option(
    name = "force_python",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "No-op, will be removed soon.",
    valueHelp = "PY2 or PY3",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val forcePython = Flag.OneOf("forcePython")

  @Option(
    name = "from",
    defaultValue = "<root>",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      The module(s) starting from which the dependency graph query will be displayed. Check each query’s
      description for the exact semantics. Defaults to <root>.
      
    """,
    valueHelp = "a comma-separated list of <module>s",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val from = Flag.Unknown("from")

  @Option(
    name = "gc_thrashing_limits",
    defaultValue = "1s:2,20s:3,1m:5",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Limits which, if reached, cause GcThrashingDetector to crash Bazel with an OOM. Each limit is specified as
      <period>:<count> where period is a duration and count is a positive integer. If more than
      --gc_thrashing_threshold percent of tenured space (old gen heap) remains occupied after <count> consecutive
      full GCs within <period>, an OOM is triggered. Multiple limits can be specified separated by commas.
    """,
    valueHelp = "comma separated pairs of <period>:<count>",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val gcThrashingLimits = Flag.Unknown("gcThrashingLimits")

  @Option(
    name = "gc_thrashing_threshold",
    oldName = "experimental_oom_more_eagerly_threshold",
    defaultValue = "100",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      The percent of tenured space occupied (0-100) above which GcThrashingDetector considers memory pressure
      events against its limits (--gc_thrashing_limits). If set to 100, GcThrashingDetector is disabled.
    """,
    valueHelp = "an integer in 0-100 range",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val gcThrashingThreshold = Flag.Unknown("gcThrashingThreshold")

  @Option(
    name = "generate_json_trace_profile",
    oldName = "experimental_generate_json_trace_profile",
    defaultValue = "auto",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      If enabled, Bazel profiles the build and writes a JSON-format profile into a file in the output base. View
      profile by loading into chrome://tracing. By default Bazel writes the profile for all build-like commands and
      query.
    """,
    valueHelp = "a tri-state (auto, yes, no)",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val generateJsonTraceProfile = Flag.TriState("generateJsonTraceProfile")

  @Option(
    name = "genrule_strategy",
    defaultValue = "",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Specify how to execute genrules. This flag will be phased out. Instead, use --spawn_strategy=<value> to
      control all actions or --strategy=Genrule=<value> to control genrules only.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val genruleStrategy = Flag.Unknown("genruleStrategy")

  @Option(
    name = "gnu_format",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION],
    help = "If set, write the version to stdout using the conventions described in the GNU standards.",
    valueHelp = "a boolean",
    commands = ["version"],
  )
  @JvmField
  @Suppress("unused")
  val gnuFormat = Flag.Boolean("gnuFormat")

  @Option(
    name = "google_auth_scopes",
    oldName = "auth_scope",
    defaultValue = "https://www.googleapis.com/auth/cloud-platform",
    help = "A comma-separated list of Google Cloud authentication scopes.",
    valueHelp = "comma-separated list of options",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val googleAuthScopes = Flag.Unknown("googleAuthScopes")

  @Option(
    name = "google_credentials",
    oldName = "auth_credentials",
    help = """
      Specifies the file to get authentication credentials from. See https://cloud.google.com/docs/authentication
      for details.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val googleCredentials = Flag.Str("googleCredentials")

  @Option(
    name = "google_default_credentials",
    oldName = "auth_enabled",
    defaultValue = "false",
    help = """
      Whether to use 'Google Application Default Credentials' for authentication. See
      https://cloud.google.com/docs/authentication for details. Disabled by default.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val googleDefaultCredentials = Flag.Boolean("googleDefaultCredentials")

  @Option(
    name = "graph:conditional_edges_limit",
    defaultValue = "4",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      The maximum number of condition labels to show. -1 means no truncation and 0 means no annotation. This option
      is only applicable to --output=graph.
    """,
    valueHelp = "an integer",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val graph_conditionalEdgesLimit = Flag.Integer("graph_conditionalEdgesLimit")

  @Option(
    name = "graph:factored",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      If true, then the graph will be emitted 'factored', i.e. topologically-equivalent nodes will be merged
      together and their labels concatenated. This option is only applicable to --output=graph.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val graph_factored = Flag.Boolean("graph_factored")

  @Option(
    name = "graph:node_limit",
    defaultValue = "512",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      The maximum length of the label string for a graph node in the output.  Longer labels will be truncated; -1
      means no truncation.  This option is only applicable to --output=graph.
    """,
    valueHelp = "an integer",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val graph_nodeLimit = Flag.Integer("graph_nodeLimit")

  @Option(
    name = "grpc_keepalive_time",
    help = """
      Configures keep-alive pings for outgoing gRPC connections. If this is set, then Bazel sends pings after this
      much time of no read operations on the connection, but only if there is at least one pending gRPC call. Times
      are treated as second granularity; it is an error to set a value less than one second. By default, keep-alive
      pings are disabled. You should coordinate with the service owner before enabling this setting. For example to
      set a value of 30 seconds to this flag, it should be done as this --grpc_keepalive_time=30s
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val grpcKeepaliveTime = Flag.Duration("grpcKeepaliveTime")

  @Option(
    name = "grpc_keepalive_timeout",
    defaultValue = "20s",
    help = """
      Configures a keep-alive timeout for outgoing gRPC connections. If keep-alive pings are enabled with
      --grpc_keepalive_time, then Bazel times out a connection if it does not receive a ping reply after this much
      time. Times are treated as second granularity; it is an error to set a value less than one second. If
      keep-alive pings are disabled, then this setting is ignored.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val grpcKeepaliveTimeout = Flag.Duration("grpcKeepaliveTimeout")

  @Option(
    name = "grte_top",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      A label to a checked-in libc library. The default value is selected by the crosstool toolchain, and you
      almost never need to override it.
    """,
    valueHelp = "a label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val grteTop = Flag.Unknown("grteTop")

  @Option(
    name = "heap_dump_on_oom",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      Whether to manually output a heap dump if an OOM is thrown (including manual OOMs due to reaching
      --gc_thrashing_limits). The dump will be written to <output_base>/<invocation_id>.heapdump.hprof. This option
      effectively replaces -XX:+HeapDumpOnOutOfMemoryError, which has no effect for manual OOMs.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val heapDumpOnOom = Flag.Boolean("heapDumpOnOom")

  @Option(
    name = "help_verbosity",
    defaultValue = "medium",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Select the verbosity of the help command.",
    valueHelp = "long, medium or short",
    commands = ["help"],
  )
  @JvmField
  @Suppress("unused")
  val helpVerbosity = Flag.OneOf("helpVerbosity")

  @Option(
    name = "heuristically_drop_nodes",
    oldName = "experimental_heuristically_drop_nodes",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      If true, Blaze will remove FileState and DirectoryListingState nodes after related File and DirectoryListing
      node is done to save memory. We expect that it is less likely that these nodes will be needed again. If so,
      the program will re-evaluate them.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val heuristicallyDropNodes = Flag.Boolean("heuristicallyDropNodes")

  @Option(
    name = "hide_aspect_results",
    defaultValue = "",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Comma-separated list of aspect names to not display in results (see --show_result). Useful for keeping
      aspects added by wrappers which are typically not interesting to end users out of console output.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hideAspectResults = Flag.Unknown("hideAspectResults")

  @Option(
    name = "high_priority_workers",
    allowMultiple = true,
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op, will be removed soon.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val highPriorityWorkers = Flag.Str("highPriorityWorkers")

  @Option(
    name = "home_rc",
    defaultValue = "true",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = "Whether or not to look for the home bazelrc file at ${'$'}HOME/.bazelrc",
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val homeRc = Flag.Boolean("homeRc")

  @Option(
    name = "host_action_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """
      Specifies the set of environment variables available to actions with execution configurations. Variables can
      be either specified by name, in which case the value will be taken from the invocation environment, or by the
      name=value pair which sets the value independent of the invocation environment. This option can be used
      multiple times; for options given for the same variable, the latest wins, options for different variables
      accumulate.
    """,
    valueHelp = "a 'name=value' assignment with an optional value part",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostActionEnv = Flag.Unknown("hostActionEnv")

  @Option(
    name = "host_compilation_mode",
    defaultValue = "opt",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.ACTION_COMMAND_LINES],
    help = """
      Specify the mode the tools used during the build will be built in. Values: 'fastbuild', 'dbg', 'opt'.
    """,
    valueHelp = "fastbuild, dbg or opt",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostCompilationMode = Flag.OneOf("hostCompilationMode")

  @Option(
    name = "host_compiler",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    help = "No-op flag. Will be removed in a future release.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostCompiler = Flag.Str("hostCompiler")

  @Option(
    name = "host_conlyopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Additional option to pass to the C compiler when compiling C (but not C++) source files in the exec
      configurations.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostConlyopt = Flag.Str("hostConlyopt")

  @Option(
    name = "host_copt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional options to pass to the C compiler for tools built in the exec configurations.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostCopt = Flag.Str("hostCopt")

  @Option(
    name = "host_cpu",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "The host CPU.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostCpu = Flag.Str("hostCpu")

  @Option(
    name = "host_crosstool_top",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op flag. Will be removed in a future release.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostCrosstoolTop = Flag.Label("hostCrosstoolTop")

  @Option(
    name = "host_cxxopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional options to pass to C++ compiler for tools built in the exec configurations.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostCxxopt = Flag.Str("hostCxxopt")

  @Option(
    name = "host_features",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      The given features will be enabled or disabled by default for targets built in the exec configuration.
      Specifying -<feature> will disable the feature. Negative features always override positive ones.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostFeatures = Flag.Str("hostFeatures")

  @Option(
    name = "host_force_python",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Overrides the Python version for the exec configuration. Can be "PY2" or "PY3".""",
    valueHelp = "PY2 or PY3",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostForcePython = Flag.OneOf("hostForcePython")

  @Option(
    name = "host_grte_top",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If specified, this setting overrides the libc top-level directory (--grte_top) for the exec configuration.
    """,
    valueHelp = "a label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostGrteTop = Flag.Unknown("hostGrteTop")

  @Option(
    name = "host_java_launcher",
    help = "The Java launcher used by tools that are executed during a build.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostJavaLauncher = Flag.Label("hostJavaLauncher")

  @Option(
    name = "host_java_toolchain",
    help = "No-op. Kept here for backwards compatibility.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostJavaToolchain = Flag.Label("hostJavaToolchain")

  @Option(
    name = "host_javabase",
    help = "No-op.  Kept here for backwards compatibility.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostJavabase = Flag.Label("hostJavabase")

  @Option(
    name = "host_javacopt",
    allowMultiple = true,
    help = "Additional options to pass to javac when building tools that are executed during a build.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostJavacopt = Flag.Str("hostJavacopt")

  @Option(
    name = "host_jvm_args",
    allowMultiple = true,
    help = "Flags to pass to the JVM executing Blaze.",
    valueHelp = "a string",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val hostJvmArgs = Flag.Str("hostJvmArgs")

  @Option(
    name = "host_jvm_debug",
    expandsTo = ["--host_jvm_args=-Xdebug", "--host_jvm_args=-Xrunjdwp:transport=dt_socket,server=y,address=5005"],
    help = """
      Convenience option to add some additional JVM startup flags, which cause the JVM to wait during startup until
      you connect from a JDWP-compliant debugger (like Eclipse) to port 5005.
    """,
    valueHelp = "",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val hostJvmDebug = Flag.Unknown("hostJvmDebug")

  @Option(
    name = "host_jvmopt",
    allowMultiple = true,
    help = """
      Additional options to pass to the Java VM when building tools that are executed during  the build. These
      options will get added to the VM startup options of each  java_binary target.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostJvmopt = Flag.Str("hostJvmopt")

  @Option(
    name = "host_linkopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional option to pass to linker when linking tools in the exec configurations.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostLinkopt = Flag.Str("hostLinkopt")

  @Option(
    name = "host_macos_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = "Minimum compatible macOS version for host targets. If unspecified, uses 'macos_sdk_version'.",
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostMacosMinimumOs = Flag.Unknown("hostMacosMinimumOs")

  @Option(
    name = "host_per_file_copt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Additional options to selectively pass to the C/C++ compiler when compiling certain files in the exec
      configurations. This option can be passed multiple times. Syntax:
      regex_filter@option_1,option_2,...,option_n. Where regex_filter stands for a list of include and exclude
      regular expression patterns (Also see --instrumentation_filter). option_1 to option_n stand for arbitrary
      command line options. If an option contains a comma it has to be quoted with a backslash. Options can contain
      @. Only the first @ is used to split the string. Example:
      --host_per_file_copt=//foo/.*\.cc,-//foo/bar\.cc@-O0 adds the -O0 command line option to the gcc command line
      of all cc files in //foo/ except bar.cc.
    """,
    valueHelp = """
      a comma-separated list of regex expressions with prefix '-' specifying excluded paths followed by
      an @ and a comma separated list of options
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostPerFileCopt = Flag.Unknown("hostPerFileCopt")

  @Option(
    name = "host_platform",
    oldName = "experimental_host_platform",
    defaultValue = "@bazel_tools//tools:host_platform",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    help = "The label of a platform rule that describes the host system.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val hostPlatform = Flag.Label("hostPlatform")

  @Option(
    name = "http_connector_attempts",
    defaultValue = "8",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "The maximum number of attempts for http downloads.",
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val httpConnectorAttempts = Flag.Integer("httpConnectorAttempts")

  @Option(
    name = "http_connector_retry_max_timeout",
    defaultValue = "0s",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      The maximum timeout for http download retries. With a value of 0, no timeout maximum is defined.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val httpConnectorRetryMaxTimeout = Flag.Duration("httpConnectorRetryMaxTimeout")

  @Option(
    name = "http_max_parallel_downloads",
    defaultValue = "8",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "The maximum number parallel http downloads.",
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val httpMaxParallelDownloads = Flag.Integer("httpMaxParallelDownloads")

  @Option(
    name = "http_timeout_scaling",
    defaultValue = "1.0",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "Scale all timeouts related to http downloads by the given factor",
    valueHelp = "a double",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val httpTimeoutScaling = Flag.Double("httpTimeoutScaling")

  @Option(
    name = "idle_server_tasks",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = "Run System.gc() when the server is idle",
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val idleServerTasks = Flag.Boolean("idleServerTasks")

  @Option(
    name = "iff_heap_size_greater_than",
    defaultValue = "0",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      Iff non-zero, then shutdown will only shut down the server if the total memory (in MB) consumed by the JVM
      exceeds this value.
    """,
    valueHelp = "an integer",
    commands = ["shutdown"],
  )
  @JvmField
  @Suppress("unused")
  val iffHeapSizeGreaterThan = Flag.Integer("iffHeapSizeGreaterThan")

  @Option(
    name = "ignore_all_rc_files",
    defaultValue = "false",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      Disables all rc files, regardless of the values of other rc-modifying flags, even if these flags come later
      in the list of startup options.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val ignoreAllRcFiles = Flag.Boolean("ignoreAllRcFiles")

  @Option(
    name = "ignore_dev_dependency",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      If true, Bazel ignores `bazel_dep` and `use_extension` declared as `dev_dependency` in the MODULE.bazel of
      the root module. Note that, those dev dependencies are always ignored in the MODULE.bazel if it's not the
      root module regardless of the value of this flag.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val ignoreDevDependency = Flag.Boolean("ignoreDevDependency")

  @Option(
    name = "ignore_unsupported_sandboxing",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Do not print a warning when sandboxed execution is not supported on this system.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val ignoreUnsupportedSandboxing = Flag.Boolean("ignoreUnsupportedSandboxing")

  @Option(
    name = "implicit_deps",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """
      If enabled, implicit dependencies will be included in the dependency graph over which the query operates. An
      implicit dependency is one that is not explicitly specified in the BUILD file but added by bazel. For cquery,
      this option controls filtering resolved toolchains.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val implicitDeps = Flag.Boolean("implicitDeps")

  @Option(
    name = "include_artifacts",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Includes names of the action inputs and outputs in the output (potentially large).",
    valueHelp = "a boolean",
    commands = ["aquery"],
  )
  @JvmField
  @Suppress("unused")
  val includeArtifacts = Flag.Boolean("includeArtifacts")

  @Option(
    name = "include_aspects",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      aquery, cquery: whether to include aspect-generated actions in the output. query: no-op (aspects are always
      followed).
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val includeAspects = Flag.Boolean("includeAspects")

  @Option(
    name = "include_builtin",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Include built-in modules in the dependency graph. Disabled by default because it is quite noisy.
    """,
    valueHelp = "a boolean",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val includeBuiltin = Flag.Boolean("includeBuiltin")

  @Option(
    name = "include_commandline",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Includes the content of the action command lines in the output (potentially large).",
    valueHelp = "a boolean",
    commands = ["aquery"],
  )
  @JvmField
  @Suppress("unused")
  val includeCommandline = Flag.Boolean("includeCommandline")

  @Option(
    name = "include_config_fragments_provider",
    defaultValue = "off",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = """
      INTERNAL BLAZE DEVELOPER FEATURE: If "direct", all configured targets expose RequiredConfigFragmentsProvider
      with the configuration fragments they directly require. If "transitive", they do the same but also include
      the fragments their transitive dependencies require. If "off", the provider is omitted. If not "off", this
      also populates config_setting's ConfigMatchingProvider.requiredFragmentOptions with the fragment options the
      config_setting requires.Be careful using this feature: it adds memory to every configured target in the build.
    """,
    valueHelp = "off, direct or transitive",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val includeConfigFragmentsProvider = Flag.OneOf("includeConfigFragmentsProvider")

  @Option(
    name = "include_file_write_contents",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Include the file contents for the FileWrite, SourceSymlinkManifest, and RepoMappingManifest actions
      (potentially large).
    """,
    valueHelp = "a boolean",
    commands = ["aquery"],
  )
  @JvmField
  @Suppress("unused")
  val includeFileWriteContents = Flag.Boolean("includeFileWriteContents")

  @Option(
    name = "include_param_files",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Include the content of the param files used in the command (potentially large). Note: Enabling this flag will
      automatically enable the --include_commandline flag.
    """,
    valueHelp = "a boolean",
    commands = ["aquery"],
  )
  @JvmField
  @Suppress("unused")
  val includeParamFiles = Flag.Boolean("includeParamFiles")

  @Option(
    name = "include_pruned_inputs",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Includes action inputs that were pruned during action execution. Only affects actions that discover inputs
      and have been executed in a previous invocation. Only takes effect if --include_artifacts is also set.
    """,
    valueHelp = "a boolean",
    commands = ["aquery"],
  )
  @JvmField
  @Suppress("unused")
  val includePrunedInputs = Flag.Boolean("includePrunedInputs")

  @Option(
    name = "include_unused",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      The queries will also take into account and display the unused modules, which are not present in the module
      resolution graph after selection (due to the Minimal-Version Selection or override rules). This can have
      different effects for each of the query types i.e. include new paths in the all_paths command, or extra
      dependants in the explain command.
    """,
    valueHelp = "a boolean",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val includeUnused = Flag.Boolean("includeUnused")

  @Option(
    name = "incompatible_allow_python_version_transitions",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op, will be removed soon.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAllowPythonVersionTransitions = Flag.Boolean("incompatibleAllowPythonVersionTransitions")

  @Option(
    name = "incompatible_allow_tags_propagation",
    oldName = "experimental_allow_tags_propagation",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      If set to true, tags will be propagated from a target to the actions' execution requirements; otherwise tags
      are not propagated. See https://github.com/bazelbuild/bazel/issues/8830 for details.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAllowTagsPropagation = Flag.Boolean("incompatibleAllowTagsPropagation")

  @Option(
    name = "incompatible_always_check_depset_elements",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      Check the validity of elements added to depsets, in all constructors. Elements must be immutable, but
      historically the depset(direct=...) constructor forgot to check. Use tuples instead of lists in depset
      elements. See https://github.com/bazelbuild/bazel/issues/10313 for details.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAlwaysCheckDepsetElements = Flag.Boolean("incompatibleAlwaysCheckDepsetElements")

  @Option(
    name = "incompatible_always_include_files_in_data",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, native rules add <code>DefaultInfo.files</code> of data dependencies to their runfiles, which
      matches the recommended behavior for Starlark rules
      (https://bazel.build/extending/rules#runfiles_features_to_avoid).
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAlwaysIncludeFilesInData = Flag.Boolean("incompatibleAlwaysIncludeFilesInData")

  @Option(
    name = "incompatible_auto_configure_host_platform",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "This option is deprecated and has no effect.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAutoConfigureHostPlatform = Flag.Boolean("incompatibleAutoConfigureHostPlatform")

  @Option(
    name = "incompatible_auto_exec_groups",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      When enabled, an exec groups is automatically created for each toolchain used by a rule. For this to work
      rule needs to specify `toolchain` parameter on its actions. For more information, see
      https://github.com/bazelbuild/bazel/issues/17134.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAutoExecGroups = Flag.Boolean("incompatibleAutoExecGroups")

  @Option(
    name = "incompatible_autoload_externally",
    defaultValue = """
      +@rules_python,+java_common,+JavaInfo,+JavaPluginInfo,ProguardSpecProvider,java_binary,
      java_import,java_library,java_plugin,java_test,java_runtime,java_toolchain,java_package_configuration,
      @com_google_protobuf,@rules_shell,+@rules_android
    """,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      A comma-separated list of rules (or other symbols) that were previously part of Bazel and which are now to be
      retrieved from their respective external repositories. This flag is intended to be used to facilitate
      migration of rules out of Bazel. See also https://github.com/bazelbuild/bazel/issues/23043.
      A symbol that is autoloaded within a file behaves as if its built-into-Bazel definition were replaced by its
      canonical new definition in an external repository. For a BUILD file, this essentially means implicitly
      adding a load() statement. For a .bzl file, it's either a load() statement or a change to a field of the
      `native` object, depending on whether the autoloaded symbol is a rule.
      Bazel maintains a hardcoded list of all symbols that may be autoloaded; only those symbols may appear in this
      flag. For each symbol, Bazel knows the new definition location in an external repository, as well as a set of
      special-cased repositories that must not autoload it to avoid creating cycles.
      A list item of "+foo" in this flag causes symbol foo to be autoloaded, except in foo's exempt repositories,
      within which the Bazel-defined version of foo is still available.
      A list item of "foo" triggers autoloading as above, but the Bazel-defined version of foo is not made
      available to the excluded repositories. This ensures that foo's external repository does not depend on the
      old Bazel implementation of foo
      A list item of "-foo" does not trigger any autoloading, but makes the Bazel-defined version of foo
      inaccessible throughout the workspace. This is used to validate that the workspace is ready for foo's
      definition to be deleted from Bazel.
      If a symbol is not named in this flag then it continues to work as normal -- no autoloading is done, nor is
      the Bazel-defined version suppressed. For configuration see
      https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/packages/AutoloadSymbols.java
      As a shortcut also whole repository may be used, for example +@rules_python will autoload all Python rules.
    """,
    valueHelp = "comma-separated set of options",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAutoloadExternally = Flag.Unknown("incompatibleAutoloadExternally")

  @Option(
    name = "incompatible_avoid_hardcoded_objc_compilation_flags",
    defaultValue = "false",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
      OptionEffectTag.EXECUTION, OptionEffectTag.ACTION_COMMAND_LINES,
    ],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      Prevents Bazel from adding compiler options to Objective-C compilation actions. Options set in the crosstool
      are still applied.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAvoidHardcodedObjcCompilationFlags = Flag.Boolean("incompatibleAvoidHardcodedObjcCompilationFlags")

  @Option(
    name = "incompatible_bazel_test_exec_run_under",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If enabled, "bazel test --run_under=//:runner" builds "//:runner" in the exec configuration. If disabled, it
      builds "//:runner" in the target configuration. Bazel executes tests on exec machines, so the former is more
      correct. This doesn't affect "bazel run", which always builds "`--run_under=//foo" in the target
      configuration.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleBazelTestExecRunUnder = Flag.Boolean("incompatibleBazelTestExecRunUnder")

  @Option(
    name = "incompatible_check_sharding_support",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, Bazel will fail a sharded test if the test runner does not indicate that it supports sharding by
      touching the file at the path in TEST_SHARD_STATUS_FILE. If false, a test runner that does not support
      sharding will lead to all tests running in each shard.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleCheckShardingSupport = Flag.Boolean("incompatibleCheckShardingSupport")

  @Option(
    name = "incompatible_check_testonly_for_output_files",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If enabled, check testonly for prerequisite targets that are output files by looking up the testonly of the
      generating rule. This matches visibility checking.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleCheckTestonlyForOutputFiles = Flag.Boolean("incompatibleCheckTestonlyForOutputFiles")

  @Option(
    name = "incompatible_check_visibility_for_toolchains",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If enabled, visibility checking also applies to toolchain implementations.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleCheckVisibilityForToolchains = Flag.Boolean("incompatibleCheckVisibilityForToolchains")

  @Option(
    name = "incompatible_config_setting_private_default_visibility",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If incompatible_enforce_config_setting_visibility=false, this is a noop. Else, if this flag is false, any
      config_setting without an explicit visibility attribute is //visibility:public. If this flag is true,
      config_setting follows the same visibility logic as all other rules. See
      https://github.com/bazelbuild/bazel/issues/12933.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleConfigSettingPrivateDefaultVisibility = Flag.Boolean("incompatibleConfigSettingPrivateDefaultVisibility")

  @Option(
    name = "incompatible_default_to_explicit_init_py",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      This flag changes the default behavior so that __init__.py files are no longer automatically created in the
      runfiles of Python targets. Precisely, when a py_binary or py_test target has legacy_create_init set to
      "auto" (the default), it is treated as false if and only if this flag is set. See
      https://github.com/bazelbuild/bazel/issues/10076.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDefaultToExplicitInitPy = Flag.Boolean("incompatibleDefaultToExplicitInitPy")

  @Option(
    name = "incompatible_depset_for_java_output_source_jars",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDepsetForJavaOutputSourceJars = Flag.Boolean("incompatibleDepsetForJavaOutputSourceJars")

  @Option(
    name = "incompatible_depset_for_libraries_to_link_getter",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      When true, Bazel no longer returns a list from linking_context.libraries_to_link but returns a depset instead.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDepsetForLibrariesToLinkGetter = Flag.Boolean("incompatibleDepsetForLibrariesToLinkGetter")

  @Option(
    name = "incompatible_disable_legacy_cc_provider",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op flag. Will be removed in a future release.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableLegacyCcProvider = Flag.Boolean("incompatibleDisableLegacyCcProvider")

  @Option(
    name = "incompatible_disable_legacy_flags_cc_toolchain_api",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE, OptionMetadataTag.DEPRECATED],
    help = "Flag for disabling the legacy cc_toolchain Starlark API for accessing legacy CROSSTOOL fields.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableLegacyFlagsCcToolchainApi = Flag.Boolean("incompatibleDisableLegacyFlagsCcToolchainApi")

  @Option(
    name = "incompatible_disable_native_android_rules",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If enabled, direct usage of the native Android rules is disabled. Please use the Starlark Android rules from
      https://github.com/bazelbuild/rules_android
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableNativeAndroidRules = Flag.Boolean("incompatibleDisableNativeAndroidRules")

  @Option(
    name = "incompatible_disable_native_apple_binary_rule",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op. Kept here for backwards compatibility.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableNativeAppleBinaryRule = Flag.Boolean("incompatibleDisableNativeAppleBinaryRule")

  @Option(
    name = "incompatible_disable_native_repo_rules",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      If false, native repo rules can be used in WORKSPACE; otherwise, Starlark repo rules must be used instead.
      Native repo rules include local_repository, new_local_repository, local_config_platform, and
      android_sdk_repository.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableNativeRepoRules = Flag.Boolean("incompatibleDisableNativeRepoRules")

  @Option(
    name = "incompatible_disable_nocopts",
    defaultValue = "true",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      When enabled, it removes nocopts attribute from C++ rules. See
      https://github.com/bazelbuild/bazel/issues/8706 for details.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableNocopts = Flag.Boolean("incompatibleDisableNocopts")

  @Option(
    name = "incompatible_disable_non_executable_java_binary",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If true, java_binary is always executable. create_executable attribute is removed.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableNonExecutableJavaBinary = Flag.Boolean("incompatibleDisableNonExecutableJavaBinary")

  @Option(
    name = "incompatible_disable_objc_library_transition",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      Disable objc_library's custom transition and inherit from the top level target instead (No-op in Bazel)
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableObjcLibraryTransition = Flag.Boolean("incompatibleDisableObjcLibraryTransition")

  @Option(
    name = "incompatible_disable_starlark_host_transitions",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, rule attributes cannot set 'cfg = "host"'. Rules should set 'cfg = "exec"' instead.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableStarlarkHostTransitions = Flag.Boolean("incompatibleDisableStarlarkHostTransitions")

  @Option(
    name = "incompatible_disable_target_default_provider_fields",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, disable the ability to access providers on 'target' objects via field syntax. Use
      provider-key syntax instead. For example, instead of using `ctx.attr.dep.my_info` to access `my_info` from
      inside a rule implementation function, use `ctx.attr.dep[MyInfo]`. See
      https://github.com/bazelbuild/bazel/issues/9014 for details.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableTargetDefaultProviderFields = Flag.Boolean("incompatibleDisableTargetDefaultProviderFields")

  @Option(
    name = "incompatible_disable_target_provider_fields",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, disable the ability to utilize the default provider via field syntax. Use provider-key syntax
      instead. For example, instead of using `ctx.attr.dep.files` to access `files`, utilize
      `ctx.attr.dep[DefaultInfo].files See https://github.com/bazelbuild/bazel/issues/9014 for details.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableTargetProviderFields = Flag.Boolean("incompatibleDisableTargetProviderFields")

  @Option(
    name = "incompatible_disallow_ctx_resolve_tools",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, calling the deprecated ctx.resolve_tools API always fails. Uses of this API should be
      replaced by an executable or tools argument to ctx.actions.run or ctx.actions.run_shell.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowCtxResolveTools = Flag.Boolean("incompatibleDisallowCtxResolveTools")

  @Option(
    name = "incompatible_disallow_empty_glob",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If set to true, the default value of the `allow_empty` argument of glob() is False.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowEmptyGlob = Flag.Boolean("incompatibleDisallowEmptyGlob")

  @Option(
    name = "incompatible_disallow_java_import_empty_jars",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "When enabled, empty java_import.jars is not supported.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowJavaImportEmptyJars = Flag.Boolean("incompatibleDisallowJavaImportEmptyJars")

  @Option(
    name = "incompatible_disallow_java_import_exports",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "When enabled, java_import.exports is not supported.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowJavaImportExports = Flag.Boolean("incompatibleDisallowJavaImportExports")

  @Option(
    name = "incompatible_disallow_legacy_py_provider",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op, will be removed soon.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowLegacyPyProvider = Flag.Boolean("incompatibleDisallowLegacyPyProvider")

  @Option(
    name = "incompatible_disallow_resource_jars",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op, kept only for backwards compatibility",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowResourceJars = Flag.Boolean("incompatibleDisallowResourceJars")

  @Option(
    name = "incompatible_disallow_sdk_frameworks_attributes",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, disallow sdk_frameworks and weak_sdk_frameworks attributes in objc_library andobjc_import.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowSdkFrameworksAttributes = Flag.Boolean("incompatibleDisallowSdkFrameworksAttributes")

  @Option(
    name = "incompatible_disallow_struct_provider_syntax",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, rule implementation functions may not return a struct. They must instead return a list of
      provider instances.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowStructProviderSyntax = Flag.Boolean("incompatibleDisallowStructProviderSyntax")

  @Option(
    name = "incompatible_do_not_split_linking_cmdline",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      When true, Bazel no longer modifies command line flags used for linking, and also doesn't selectively decide
      which flags go to the param file and which don't.  See https://github.com/bazelbuild/bazel/issues/7670 for
      details.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDoNotSplitLinkingCmdline = Flag.Boolean("incompatibleDoNotSplitLinkingCmdline")

  @Option(
    name = "incompatible_dont_collect_native_libraries_in_data",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "This flag is a noop and scheduled for removal.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDontCollectNativeLibrariesInData = Flag.Boolean("incompatibleDontCollectNativeLibrariesInData")

  @Option(
    name = "incompatible_dont_enable_host_nonhost_crosstool_features",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, Bazel will not enable 'host' and 'nonhost' features in the c++ toolchain (see
      https://github.com/bazelbuild/bazel/issues/7407 for more information).
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDontEnableHostNonhostCrosstoolFeatures = Flag.Boolean("incompatibleDontEnableHostNonhostCrosstoolFeatures")

  @Option(
    name = "incompatible_dont_use_javasourceinfoprovider",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDontUseJavasourceinfoprovider = Flag.Boolean("incompatibleDontUseJavasourceinfoprovider")

  @Option(
    name = "incompatible_enable_android_toolchain_resolution",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableAndroidToolchainResolution = Flag.Boolean("incompatibleEnableAndroidToolchainResolution")

  @Option(
    name = "incompatible_enable_apple_toolchain_resolution",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "Use toolchain resolution to select the Apple SDK for apple rules (Starlark and native)",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableAppleToolchainResolution = Flag.Boolean("incompatibleEnableAppleToolchainResolution")

  @Option(
    name = "incompatible_enable_cc_test_feature",
    defaultValue = "false",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      When enabled, it switches Crosstool to use feature 'is_cc_test' rather than the link-time build variable of
      the same name.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableCcTestFeature = Flag.Boolean("incompatibleEnableCcTestFeature")

  @Option(
    name = "incompatible_enable_cc_toolchain_resolution",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op flag. Will be removed in a future release.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableCcToolchainResolution = Flag.Boolean("incompatibleEnableCcToolchainResolution")

  @Option(
    name = "incompatible_enable_deprecated_label_apis",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      If enabled, certain deprecated APIs (native.repository_name, Label.workspace_name, Label.relative) can be
      used.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableDeprecatedLabelApis = Flag.Boolean("incompatibleEnableDeprecatedLabelApis")

  @Option(
    name = "incompatible_enable_profile_by_default",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableProfileByDefault = Flag.Boolean("incompatibleEnableProfileByDefault")

  @Option(
    name = "incompatible_enable_proto_toolchain_resolution",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If true, proto lang rules define toolchains from protobuf repository.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableProtoToolchainResolution = Flag.Boolean("incompatibleEnableProtoToolchainResolution")

  @Option(
    name = "incompatible_enforce_config_setting_visibility",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, enforce config_setting visibility restrictions. If false, every config_setting is visible to every
      target. See https://github.com/bazelbuild/bazel/issues/12932.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnforceConfigSettingVisibility = Flag.Boolean("incompatibleEnforceConfigSettingVisibility")

  @Option(
    name = "incompatible_enforce_starlark_utf8",
    defaultValue = "warning",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If enabled (or set to 'error'), fail if Starlark files are not UTF-8 encoded. If set to 'warning', emit a
      warning instead. If set to 'off', Bazel assumes that Starlark files are UTF-8 encoded but does not verify
      this assumption. Note that Starlark files which are not UTF-8 encoded can cause Bazel to behave
      inconsistently.
    """,
    valueHelp = "off, warning or error",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnforceStarlarkUtf8 = Flag.OneOf("incompatibleEnforceStarlarkUtf8")

  @Option(
    name = "incompatible_exclusive_test_sandboxed",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, exclusive tests will run with sandboxed strategy. Add 'local' tag to force an exclusive test run
      locally
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleExclusiveTestSandboxed = Flag.Boolean("incompatibleExclusiveTestSandboxed")

  @Option(
    name = "incompatible_existing_rules_immutable_view",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleExistingRulesImmutableView = Flag.Boolean("incompatibleExistingRulesImmutableView")

  @Option(
    name = "incompatible_fail_on_unknown_attributes",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If enabled, targets that have unknown attributes set to None fail.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleFailOnUnknownAttributes = Flag.Boolean("incompatibleFailOnUnknownAttributes")

  @Option(
    name = "incompatible_fix_package_group_reporoot_syntax",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      In package_group's `packages` attribute, changes the meaning of the value "//..." to refer to all packages in
      the current repository instead of all packages in any repository. You can use the special value "public" in
      place of "//..." to obtain the old behavior. This flag requires that
      --incompatible_package_group_has_public_syntax also be enabled.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleFixPackageGroupReporootSyntax = Flag.Boolean("incompatibleFixPackageGroupReporootSyntax")

  @Option(
    name = "incompatible_j2objc_library_migration",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If enabled, direct usage of the native j2objc_library rules is disabled. Please use the Starlark rule instead.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleJ2objcLibraryMigration = Flag.Boolean("incompatibleJ2objcLibraryMigration")

  @Option(
    name = "incompatible_java_common_parameters",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, the output_jar, and host_javabase parameters in pack_sources and host_javabase in compile
      will all be removed.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleJavaCommonParameters = Flag.Boolean("incompatibleJavaCommonParameters")

  @Option(
    name = "incompatible_java_info_merge_runtime_module_flags",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, the JavaInfo constructor will merge add_exports and add_opens of runtime_deps in addition to
      deps and exports.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleJavaInfoMergeRuntimeModuleFlags = Flag.Boolean("incompatibleJavaInfoMergeRuntimeModuleFlags")

  @Option(
    name = "incompatible_legacy_local_fallback",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, enables the legacy implicit fallback from sandboxed to local strategy. This flag will
      eventually default to false and then become a no-op. Use --strategy, --spawn_strategy, or
      --dynamic_local_strategy to configure fallbacks instead.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleLegacyLocalFallback = Flag.Boolean("incompatibleLegacyLocalFallback")

  @Option(
    name = "incompatible_lexicographical_output",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If this option is set, sorts --order_output=auto output in lexicographical order.",
    valueHelp = "a boolean",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleLexicographicalOutput = Flag.Boolean("incompatibleLexicographicalOutput")

  @Option(
    name = "incompatible_load_java_rules_from_bzl",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "Deprecated no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleLoadJavaRulesFromBzl = Flag.Boolean("incompatibleLoadJavaRulesFromBzl")

  @Option(
    name = "incompatible_load_proto_rules_from_bzl",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "Deprecated no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleLoadProtoRulesFromBzl = Flag.Boolean("incompatibleLoadProtoRulesFromBzl")

  @Option(
    name = "incompatible_load_python_rules_from_bzl",
    defaultValue = "false",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "Deprecated no-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleLoadPythonRulesFromBzl = Flag.Boolean("incompatibleLoadPythonRulesFromBzl")

  @Option(
    name = "incompatible_locations_prefers_executable",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      Whether a target that provides an executable expands to the executable rather than the files in
      <code>DefaultInfo.files</code> under ${'$'}(locations ...) expansion if the number of files is not 1.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleLocationsPrefersExecutable = Flag.Boolean("incompatibleLocationsPrefersExecutable")

  @Option(
    name = "incompatible_macos_set_install_name",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      Whether to explicitly set `-install_name` when creating dynamic libraries. See
      https://github.com/bazelbuild/bazel/issues/12370
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleMacosSetInstallName = Flag.Boolean("incompatibleMacosSetInstallName")

  @Option(
    name = "incompatible_make_thinlto_command_lines_standalone",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "This flag is a noop and scheduled for removal.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleMakeThinltoCommandLinesStandalone = Flag.Boolean("incompatibleMakeThinltoCommandLinesStandalone")

  @Option(
    name = "incompatible_merge_fixed_and_default_shell_env",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If enabled, actions registered with ctx.actions.run and ctx.actions.run_shell with both 'env' and
      'use_default_shell_env = True' specified will use an environment obtained from the default shell environment
      by overriding with the values passed in to 'env'. If disabled, the value of 'env' is completely ignored in
      this case.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleMergeFixedAndDefaultShellEnv = Flag.Boolean("incompatibleMergeFixedAndDefaultShellEnv")

  @Option(
    name = "incompatible_merge_genfiles_directory",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If true, the genfiles directory is folded into the bin directory.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleMergeGenfilesDirectory = Flag.Boolean("incompatibleMergeGenfilesDirectory")

  @Option(
    name = "incompatible_modify_execution_info_additive",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      When enabled, passing multiple --modify_execution_info flags is additive. When disabled, only the last flag
      is taken into account.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleModifyExecutionInfoAdditive = Flag.Boolean("incompatibleModifyExecutionInfoAdditive")

  @Option(
    name = "incompatible_multi_release_deploy_jars",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "When enabled, java_binary creates Multi-Release deploy jars.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleMultiReleaseDeployJars = Flag.Boolean("incompatibleMultiReleaseDeployJars")

  @Option(
    name = "incompatible_new_actions_api",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNewActionsApi = Flag.Boolean("incompatibleNewActionsApi")

  @Option(
    name = "incompatible_no_attr_license",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If set to true, disables the function `attr.license`.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNoAttrLicense = Flag.Boolean("incompatibleNoAttrLicense")

  @Option(
    name = "incompatible_no_implicit_file_export",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set, (used) source files are are package private unless exported explicitly. See
      https://github.com/bazelbuild/proposals/blob/master/designs/2019-10-24-file-visibility.md
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNoImplicitFileExport = Flag.Boolean("incompatibleNoImplicitFileExport")

  @Option(
    name = "incompatible_no_implicit_watch_label",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, then methods on <code>repository_ctx</code> that are passed a Label will no longer automatically
      watch the file under that label for changes even if <code>watch = "no"</code>, and
      <code>repository_ctx.path</code> no longer causes the returned path to be watched. Use
      <code>repository_ctx.watch</code> instead.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNoImplicitWatchLabel = Flag.Boolean("incompatibleNoImplicitWatchLabel")

  @Option(
    name = "incompatible_no_package_distribs",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If set to true, disables the `package(distribs=...)`.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNoPackageDistribs = Flag.Boolean("incompatibleNoPackageDistribs")

  @Option(
    name = "incompatible_no_rule_outputs_param",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If set to true, disables the `outputs` parameter of the `rule()` Starlark function.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNoRuleOutputsParam = Flag.Boolean("incompatibleNoRuleOutputsParam")

  @Option(
    name = "incompatible_objc_alwayslink_by_default",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If true, make the default value true for alwayslink attributes in objc_library and objc_import.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleObjcAlwayslinkByDefault = Flag.Boolean("incompatibleObjcAlwayslinkByDefault")

  @Option(
    name = "incompatible_override_toolchain_transition",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated, this is no longer in use and should be removed.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleOverrideToolchainTransition = Flag.Boolean("incompatibleOverrideToolchainTransition")

  @Option(
    name = "incompatible_package_group_has_public_syntax",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      In package_group's `packages` attribute, allows writing "public" or "private" to refer to all packages or no
      packages respectively.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePackageGroupHasPublicSyntax = Flag.Boolean("incompatiblePackageGroupHasPublicSyntax")

  @Option(
    name = "incompatible_package_group_includes_double_slash",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If enabled, when outputting package_group's `packages` attribute, the leading `//` will not be omitted.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePackageGroupIncludesDoubleSlash = Flag.Boolean("incompatiblePackageGroupIncludesDoubleSlash")

  @Option(
    name = "incompatible_py2_outputs_are_suffixed",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, targets built in the Python 2 configuration will appear under an output root that includes the
      suffix '-py2', while targets built for Python 3 will appear in a root with no Python-related suffix. This
      means that the `bazel-bin` convenience symlink will point to Python 3 targets rather than Python 2. If you
      enable this option it is also recommended to enable `--incompatible_py3_is_default`.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePy2OutputsAreSuffixed = Flag.Boolean("incompatiblePy2OutputsAreSuffixed")

  @Option(
    name = "incompatible_py3_is_default",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, `py_binary` and `py_test` targets that do not set their `python_version` (or
      `default_python_version`) attribute will default to PY3 rather than to PY2. If you set this flag it is also
      recommended to set `--incompatible_py2_outputs_are_suffixed`.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePy3IsDefault = Flag.Boolean("incompatiblePy3IsDefault")

  @Option(
    name = "incompatible_python_disable_py2",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, using Python 2 settings will cause an error. This includes python_version=PY2, srcs_version=PY2, and
      srcs_version=PY2ONLY. See https://github.com/bazelbuild/bazel/issues/15684 for more information.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePythonDisablePy2 = Flag.Boolean("incompatiblePythonDisablePy2")

  @Option(
    name = "incompatible_python_disallow_native_rules",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      When true, an error occurs when using the builtin py_* rules; instead the rule_python rules should be used.
      See https://github.com/bazelbuild/bazel/issues/17773 for more information and migration instructions.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePythonDisallowNativeRules = Flag.Boolean("incompatiblePythonDisallowNativeRules")

  @Option(
    name = "incompatible_remote_use_new_exit_code_for_lost_inputs",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, Bazel will use new exit code 39 instead of 34 if remote cacheerrors, including cache
      evictions, cause the build to fail.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoteUseNewExitCodeForLostInputs = Flag.Boolean("incompatibleRemoteUseNewExitCodeForLostInputs")

  @Option(
    name = "incompatible_remove_binary_profile",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoveBinaryProfile = Flag.Boolean("incompatibleRemoveBinaryProfile")

  @Option(
    name = "incompatible_remove_legacy_whole_archive",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, Bazel will not link library dependencies as whole archive by default (see
      https://github.com/bazelbuild/bazel/issues/7362 for migration instructions).
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoveLegacyWholeArchive = Flag.Boolean("incompatibleRemoveLegacyWholeArchive")

  @Option(
    name = "incompatible_remove_old_python_version_api",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op, will be removed soon.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoveOldPythonVersionApi = Flag.Boolean("incompatibleRemoveOldPythonVersionApi")

  @Option(
    name = "incompatible_require_ctx_in_configure_features",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "This flag is a noop and scheduled for removal.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRequireCtxInConfigureFeatures = Flag.Boolean("incompatibleRequireCtxInConfigureFeatures")

  @Option(
    name = "incompatible_require_javaplugininfo_in_javacommon",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "When enabled java_common.compile only accepts JavaPluginInfo for plugins.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRequireJavaplugininfoInJavacommon = Flag.Boolean("incompatibleRequireJavaplugininfoInJavacommon")

  @Option(
    name = "incompatible_require_linker_input_cc_api",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, rule create_linking_context will require linker_inputs instead of libraries_to_link. The old
      getters of linking_context will also be disabled and just linker_inputs will be available.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRequireLinkerInputCcApi = Flag.Boolean("incompatibleRequireLinkerInputCcApi")

  @Option(
    name = "incompatible_run_shell_command_string",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If set to true, the command parameter of actions.run_shell will only accept string",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRunShellCommandString = Flag.Boolean("incompatibleRunShellCommandString")

  @Option(
    name = "incompatible_sandbox_hermetic_tmp",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If set to true, each Linux sandbox will have its own dedicated empty directory mounted as /tmp rather than
      sharing /tmp with the host filesystem. Use --sandbox_add_mount_pair=/tmp to keep seeing the host's /tmp in
      all sandboxes.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleSandboxHermeticTmp = Flag.Boolean("incompatibleSandboxHermeticTmp")

  @Option(
    name = "incompatible_simplify_unconditional_selects_in_rule_attrs",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, simplify configurable rule attributes which contain only unconditional selects; for example, if
      ["a"] + select("//conditions:default", ["b"]) is assigned to a rule attribute, it is stored as ["a", "b"].
      This option does not affect attributes of symbolic macros or attribute default values.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleSimplifyUnconditionalSelectsInRuleAttrs = Flag.Boolean("incompatibleSimplifyUnconditionalSelectsInRuleAttrs")

  @Option(
    name = "incompatible_skip_genfiles_symlink",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, the genfiles symlink will not be created. For more information, see
      https://github.com/bazelbuild/bazel/issues/8651
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleSkipGenfilesSymlink = Flag.Boolean("incompatibleSkipGenfilesSymlink")

  @Option(
    name = "incompatible_stop_exporting_build_file_path",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, deprecated ctx.build_file_path will not be available. ctx.label.package + '/BUILD' can be
      used instead.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleStopExportingBuildFilePath = Flag.Boolean("incompatibleStopExportingBuildFilePath")

  @Option(
    name = "incompatible_stop_exporting_language_modules",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If enabled, certain language-specific modules (such as `cc_common`) are unavailable in user .bzl files and
      may only be called from their respective rules repositories.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleStopExportingLanguageModules = Flag.Boolean("incompatibleStopExportingLanguageModules")

  @Option(
    name = "incompatible_strict_action_env",
    oldName = "experimental_strict_action_env",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, Bazel uses an environment with a static value for PATH and does not inherit LD_LIBRARY_PATH. Use
      --action_env=ENV_VARIABLE if you want to inherit specific environment variables from the client, but note
      that doing so can prevent cross-user caching if a shared cache is used.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleStrictActionEnv = Flag.Boolean("incompatibleStrictActionEnv")

  @Option(
    name = "incompatible_strip_executable_safely",
    defaultValue = "false",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If true, strip action for executables will use flag -x, which does not break dynamic symbol resolution.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleStripExecutableSafely = Flag.Boolean("incompatibleStripExecutableSafely")

  @Option(
    name = "incompatible_top_level_aspects_require_providers",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, the top level aspect will honor its required providers and only run on top level targets
      whose rules' advertised providers satisfy the required providers of the aspect.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleTopLevelAspectsRequireProviders = Flag.Boolean("incompatibleTopLevelAspectsRequireProviders")

  @Option(
    name = "incompatible_unambiguous_label_stringification",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      When true, Bazel will stringify the label @//foo:bar to @//foo:bar, instead of //foo:bar. This only affects
      the behavior of str(), the % operator, and so on; the behavior of repr() is unchanged. See
      https://github.com/bazelbuild/bazel/issues/15916 for more information.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUnambiguousLabelStringification = Flag.Boolean("incompatibleUnambiguousLabelStringification")

  @Option(
    name = "incompatible_use_cc_configure_from_rules_cc",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      When true, Bazel will no longer allow using cc_configure from @bazel_tools. Please see
      https://github.com/bazelbuild/bazel/issues/10134 for details and migration instructions.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUseCcConfigureFromRulesCc = Flag.Boolean("incompatibleUseCcConfigureFromRulesCc")

  @Option(
    name = "incompatible_use_cpp_compile_header_mnemonic",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "If enabled, give distinguishing mnemonic to header processing actions",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUseCppCompileHeaderMnemonic = Flag.Boolean("incompatibleUseCppCompileHeaderMnemonic")

  @Option(
    name = "incompatible_use_new_cgroup_implementation",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If true, use the new implementation for cgroups. The old implementation only supports the memory controller
      and ignores the value of --experimental_sandbox_limits.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUseNewCgroupImplementation = Flag.Boolean("incompatibleUseNewCgroupImplementation")

  @Option(
    name = "incompatible_use_plus_in_repo_names",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "No-op.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUsePlusInRepoNames = Flag.Boolean("incompatibleUsePlusInRepoNames")

  @Option(
    name = "incompatible_use_python_toolchains",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      If set to true, executable native Python rules will use the Python runtime specified by the Python toolchain,
      rather than the runtime given by legacy flags like --python_top.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUsePythonToolchains = Flag.Boolean("incompatibleUsePythonToolchains")

  @Option(
    name = "incompatible_use_specific_tool_files",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      Use cc toolchain's compiler_files, as_files, and ar_files as inputs to appropriate actions. See
      https://github.com/bazelbuild/bazel/issues/8531
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUseSpecificToolFiles = Flag.Boolean("incompatibleUseSpecificToolFiles")

  @Option(
    name = "incompatible_use_toolchain_resolution_for_java_rules",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op. Kept here for backwards compatibility.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUseToolchainResolutionForJavaRules = Flag.Boolean("incompatibleUseToolchainResolutionForJavaRules")

  @Option(
    name = "incompatible_validate_top_level_header_inclusions",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "This flag is a noop and scheduled for removal.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleValidateTopLevelHeaderInclusions = Flag.Boolean("incompatibleValidateTopLevelHeaderInclusions")

  @Option(
    name = "incompatible_visibility_private_attributes_at_definition",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incompatibleVisibilityPrivateAttributesAtDefinition = Flag.Boolean("incompatibleVisibilityPrivateAttributesAtDefinition")

  @Option(
    name = "incremental",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Whether to do an incremental install. If true, try to avoid unnecessary additional work by reading the state
      of the device the code is to be installed on and using that information to avoid unnecessary work. If false
      (the default), always do a full install.
    """,
    valueHelp = "a boolean",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val incremental = Flag.Boolean("incremental")

  @Option(
    name = "incremental_dexing",
    defaultValue = "true",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
      OptionEffectTag.LOSES_INCREMENTAL_STATE,
    ],
    help = "Does most of the work for dexing separately for each Jar file.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val incrementalDexing = Flag.Boolean("incrementalDexing")

  @Option(
    name = "incremental_install_verbosity",
    defaultValue = "",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "The verbosity for incremental install. Set to 1 for debug logging.",
    valueHelp = "a string",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val incrementalInstallVerbosity = Flag.Str("incrementalInstallVerbosity")

  @Option(
    name = "infer_universe_scope",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      If set and --universe_scope is unset, then a value of --universe_scope will be inferred as the list of unique
      target patterns in the query expression. Note that the --universe_scope value inferred for a query expression
      that uses universe-scoped functions (e.g.`allrdeps`) may not be what you want, so you should use this option
      only if you know what you are doing. See https://bazel.build/reference/query#sky-query for details and
      examples. If --universe_scope is set, then this option's value is ignored. Note: this option applies only to
      `query` (i.e. not `cquery`).
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val inferUniverseScope = Flag.Boolean("inferUniverseScope")

  @Option(
    name = "inject_repository",
    allowMultiple = true,
    help = """
      Adds a new repository with a local path in the form of <repository name>=<path>. This only takes effect with
      --enable_bzlmod and is equivalent to adding a corresponding `local_repository` to the root module's
      MODULE.bazel file via `use_repo_rule`. If the given path is an absolute path, it will be used as it is. If
      the given path is a relative path, it is relative to the current working directory. If the given path starts
      with '%workspace%', it is relative to the workspace root, which is the output of `bazel info workspace`. If
      the given path is empty, then remove any previous injections.
    """,
    valueHelp = "an equals-separated mapping of repository name to path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val injectRepository = Flag.Unknown("injectRepository")

  @Option(
    name = "install_base",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "This launcher option is intended for use only by tests.",
    valueHelp = "a path",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val installBase = Flag.Path("installBase")

  @Option(
    name = "install_md5",
    defaultValue = "",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_MONITORING],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "This launcher option is intended for use only by tests.",
    valueHelp = "a string",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val installMd5 = Flag.Str("installMd5")

  @Option(
    name = "instrument_test_targets",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      When coverage is enabled, specifies whether to consider instrumenting test rules. When set, test rules
      included by --instrumentation_filter are instrumented. Otherwise, test rules are always excluded from
      coverage instrumentation.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val instrumentTestTargets = Flag.Boolean("instrumentTestTargets")

  @Option(
    name = "instrumentation_filter",
    defaultValue = "-/javatests[/:],-/test/java[/:]",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      When coverage is enabled, only rules with names included by the specified regex-based filter will be
      instrumented. Rules prefixed with '-' are excluded instead. Note that only non-test rules are instrumented
      unless --instrument_test_targets is enabled.
    """,
    valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val instrumentationFilter = Flag.Unknown("instrumentationFilter")

  @Option(
    name = "interface_shared_objects",
    defaultValue = "true",
    effectTags = [
      OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS,
      OptionEffectTag.AFFECTS_OUTPUTS,
    ],
    help = """
      Use interface shared objects if supported by the toolchain. All ELF toolchains currently support this setting.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val interfaceSharedObjects = Flag.Boolean("interfaceSharedObjects")

  @Option(
    name = "internal_persistent_android_dex_desugar",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = "Tracking flag for when dexing and desugaring workers are enabled.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val internalPersistentAndroidDexDesugar = Flag.Boolean("internalPersistentAndroidDexDesugar")

  @Option(
    name = "internal_persistent_busybox_tools",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = "Tracking flag for when busybox workers are enabled.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val internalPersistentBusyboxTools = Flag.Boolean("internalPersistentBusyboxTools")

  @Option(
    name = "internal_persistent_multiplex_android_dex_desugar",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = "Tracking flag for when multiplexed dexing and desugaring workers are enabled.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val internalPersistentMultiplexAndroidDexDesugar = Flag.Boolean("internalPersistentMultiplexAndroidDexDesugar")

  @Option(
    name = "internal_persistent_multiplex_busybox_tools",
    defaultValue = "false",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = "Tracking flag for when multiplexed busybox workers are enabled.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val internalPersistentMultiplexBusyboxTools = Flag.Boolean("internalPersistentMultiplexBusyboxTools")

  @Option(
    name = "internal_spawn_scheduler",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = "Placeholder option so that we can tell in Blaze whether the spawn scheduler was enabled.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val internalSpawnScheduler = Flag.Boolean("internalSpawnScheduler")

  @Option(
    name = "internal_starlark_flag_test_canary",
    defaultValue = "false",
    help = "",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val internalStarlarkFlagTestCanary = Flag.Boolean("internalStarlarkFlagTestCanary")

  @Option(
    name = "invocation_id",
    defaultValue = "",
    effectTags = [OptionEffectTag.BAZEL_MONITORING, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      Unique identifier, in UUID format, for the command being run. If explicitly specified uniqueness must be
      ensured by the caller. The UUID is printed to stderr, the BEP and remote execution protocol.
    """,
    valueHelp = "a UUID",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val invocationId = Flag.Unknown("invocationId")

  @Option(
    name = "invocation_policy",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      A base64-encoded-binary-serialized or text-formated invocation_policy.InvocationPolicy proto. Unlike other
      options, it is an error to specify --invocation_policy multiple times.
    """,
    valueHelp = "a string",
    commands = ["startup", "canonicalize-flags"],
  )
  @JvmField
  @Suppress("unused")
  val invocationPolicy = Flag.Str("invocationPolicy")

  @Option(
    name = "io_nice_level",
    defaultValue = "-1",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Only on Linux; set a level from 0-7 for best-effort IO scheduling using the sys_ioprio_set system call. 0 is
      highest priority, 7 is lowest. The anticipatory scheduler may only honor up to priority 4. If set to a
      negative value, then Bazel does not perform a system call.
    """,
    valueHelp = "an integer",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val ioNiceLevel = Flag.Integer("ioNiceLevel")

  @Option(
    name = "ios_memleaks",
    defaultValue = "false",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = "Enable checking for memory leaks in ios_test targets.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val iosMemleaks = Flag.Boolean("iosMemleaks")

  @Option(
    name = "ios_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      Minimum compatible iOS version for target simulators and devices. If unspecified, uses 'ios_sdk_version'.
    """,
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val iosMinimumOs = Flag.Unknown("iosMinimumOs")

  @Option(
    name = "ios_multi_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Comma-separated list of architectures to build an ios_application with. The result is a universal binary
      containing all specified architectures.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val iosMultiCpus = Flag.Unknown("iosMultiCpus")

  @Option(
    name = "ios_sdk_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      Specifies the version of the iOS SDK to use to build iOS applications. If unspecified, uses the default iOS
      SDK version from 'xcode_version'.
    """,
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val iosSdkVersion = Flag.Unknown("iosSdkVersion")

  @Option(
    name = "ios_signing_cert_name",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """
      Certificate name to use for iOS signing. If not set will fall back to provisioning profile. May be the
      certificate's keychain identity preference or (substring) of the certificate's common name, as per codesign's
      man page (SIGNING IDENTITIES).
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val iosSigningCertName = Flag.Str("iosSigningCertName")

  @Option(
    name = "ios_simulator_device",
    effectTags = [OptionEffectTag.TEST_RUNNER],
    help = """
      The device to simulate when running an iOS application in the simulator, e.g. 'iPhone 6'. You can get a list
      of devices by running 'xcrun simctl list devicetypes' on the machine the simulator will be run on.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val iosSimulatorDevice = Flag.Str("iosSimulatorDevice")

  @Option(
    name = "ios_simulator_version",
    effectTags = [OptionEffectTag.TEST_RUNNER],
    help = """
      The version of iOS to run on the simulator when running or testing. This is ignored for ios_test rules if a
      target device is specified in the rule.
    """,
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val iosSimulatorVersion = Flag.Unknown("iosSimulatorVersion")

  @Option(
    name = "isatty",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = """
      A system-generated parameter which is used to notify the server whether this client is running in a terminal.
      If this is set to false, then '--color=auto' will be treated as '--color=no'. If this is set to true, then
      '--color=auto' will be treated as '--color=yes'.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val isatty = Flag.Boolean("isatty")

  @Option(
    name = "j2objc_dead_code_removal",
    defaultValue = "false",
    help = "Whether to perform J2ObjC dead code removal to strip unused code from the final app bundle.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val j2objcDeadCodeRemoval = Flag.Boolean("j2objcDeadCodeRemoval")

  @Option(
    name = "j2objc_dead_code_report",
    help = """
      Allows J2ObjC to strip dead code reported by ProGuard. Takes a label that can generate a dead code report as
      argument.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val j2objcDeadCodeReport = Flag.Label("j2objcDeadCodeReport")

  @Option(
    name = "j2objc_translation_flags",
    allowMultiple = true,
    help = "Additional options to pass to the J2ObjC tool.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val j2objcTranslationFlags = Flag.Unknown("j2objcTranslationFlags")

  @Option(
    name = "java_debug",
    expandsTo = [
      "--test_arg=--wrapper_script_flag=--debug", "--test_output=streamed", "--test_strategy=exclusive",
      "--test_timeout=9999", "--nocache_test_results",
    ],
    help = """
      Causes the Java virtual machine of a java test to wait for a connection from a JDWP-compliant debugger (such
      as jdb) before starting the test. Implies -test_output=streamed.
    """,
    valueHelp = "",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javaDebug = Flag.Unknown("javaDebug")

  @Option(
    name = "java_deps",
    defaultValue = "true",
    help = "Generate dependency information (for now, compile-time classpath) per Java target.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javaDeps = Flag.Boolean("javaDeps")

  @Option(
    name = "java_header_compilation",
    oldName = "experimental_java_header_compilation",
    defaultValue = "true",
    help = "Compile ijars directly from source.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javaHeaderCompilation = Flag.Boolean("javaHeaderCompilation")

  @Option(
    name = "java_language_version",
    defaultValue = "",
    help = "The Java language version",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javaLanguageVersion = Flag.Str("javaLanguageVersion")

  @Option(
    name = "java_launcher",
    help = """
      The Java launcher to use when building Java binaries.  If this flag is set to the empty string, the JDK
      launcher is used. The "launcher" attribute overrides this flag.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javaLauncher = Flag.Label("javaLauncher")

  @Option(
    name = "java_optimization_mode",
    defaultValue = "",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Do not use.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javaOptimizationMode = Flag.Str("javaOptimizationMode")

  @Option(
    name = "java_runtime_version",
    defaultValue = "local_jdk",
    help = "The Java runtime version",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javaRuntimeVersion = Flag.Str("javaRuntimeVersion")

  @Option(
    name = "java_toolchain",
    help = "No-op. Kept here for backwards compatibility.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javaToolchain = Flag.Label("javaToolchain")

  @Option(
    name = "javabase",
    help = "No-op. Kept here for backwards compatibility.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javabase = Flag.Label("javabase")

  @Option(
    name = "javacopt",
    allowMultiple = true,
    help = "Additional options to pass to javac.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val javacopt = Flag.Str("javacopt")

  @Option(
    name = "jobs",
    abbrev = 'j',
    defaultValue = "auto",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """
      The number of concurrent jobs to run. Takes an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"),
      optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". Values must be between 1 and
      5000. Values above 2500 may cause memory issues. "auto" calculates a reasonable default based on host
      resources.
    """,
    valueHelp = """
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation
      ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val jobs = Flag.Unknown("jobs")

  @Option(
    name = "jplPropagateCcLinkParamsStore",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "No-op, kept only for backwards compatibility",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val jplPropagateCcLinkParamsStore = Flag.Boolean("jplPropagateCcLinkParamsStore")

  @Option(
    name = "jvm_heap_histogram_internal_object_pattern",
    defaultValue = """jdk\.internal\.vm\.Filler.+""",
    help = """
      Regex for overriding the matching logic for JDK21+ JVM heap memory collection. We are relying on volatile
      internal G1 GC implemenation details to get a clean memory metric, this option allows us to adapt to changes
      in that internal implementation without having to wait for a binary release.  Passed to JDK Matcher.find()
    """,
    valueHelp = "a valid Java regular expression",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val jvmHeapHistogramInternalObjectPattern = Flag.Unknown("jvmHeapHistogramInternalObjectPattern")

  @Option(
    name = "jvmopt",
    allowMultiple = true,
    help = """
      Additional options to pass to the Java VM. These options will get added to the VM startup options of each
      java_binary target.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val jvmopt = Flag.Str("jvmopt")

  @Option(
    name = "keep_backend_build_event_connections_alive",
    defaultValue = "true",
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "If enabled, keep connections to build event backend connections alive across builds.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val keepBackendBuildEventConnectionsAlive = Flag.Boolean("keepBackendBuildEventConnectionsAlive")

  @Option(
    name = "keep_going",
    abbrev = 'k',
    defaultValue = "false",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      Continue as much as possible after an error.  While the target that failed and those that depend on it cannot
      be analyzed, other prerequisites of these targets can be.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val keepGoing = Flag.Boolean("keepGoing")

  @Option(
    name = "keep_state_after_build",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      If false, Blaze will discard the inmemory state from this build when the build finishes. Subsequent builds
      will not have any incrementality with respect to this one.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val keepStateAfterBuild = Flag.Boolean("keepStateAfterBuild")

  @Option(
    name = "legacy_bazel_java_test",
    defaultValue = "false",
    help = "No-op, kept only for backwards compatibility",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val legacyBazelJavaTest = Flag.Boolean("legacyBazelJavaTest")

  @Option(
    name = "legacy_external_runfiles",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If true, build runfiles symlink forests for external repositories under .runfiles/wsname/external/repo (in
      addition to .runfiles/repo).
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val legacyExternalRunfiles = Flag.Boolean("legacyExternalRunfiles")

  @Option(
    name = "legacy_globbing_threads",
    defaultValue = "100",
    help = """
      Number of threads to use for glob evaluation. Takes an integer, or a keyword ("auto", "HOST_CPUS",
      "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". "auto" means to
      use a reasonable value derived from the machine's hardware profile (e.g. the number of processors).
    """,
    valueHelp = """
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation
      ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val legacyGlobbingThreads = Flag.Unknown("legacyGlobbingThreads")

  @Option(
    name = "legacy_important_outputs",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Use this to suppress generation of the legacy important_outputs field in the TargetComplete event.
      important_outputs are required for Bazel to ResultStore/BTX integration.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val legacyImportantOutputs = Flag.Boolean("legacyImportantOutputs")

  @Option(
    name = "legacy_main_dex_list_generator",
    help = """
      Specifies a binary to use to generate the list of classes that must be in the main dex when compiling legacy
      multidex.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val legacyMainDexListGenerator = Flag.Label("legacyMainDexListGenerator")

  @Option(
    name = "legacy_whole_archive",
    defaultValue = "true",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.DEPRECATED],
    help = """
      Deprecated, superseded by --incompatible_remove_legacy_whole_archive (see
      https://github.com/bazelbuild/bazel/issues/7362 for details). When on, use --whole-archive for cc_binary
      rules that have linkshared=True and either linkstatic=True or '-static' in linkopts. This is for backwards
      compatibility only. A better alternative is to use alwayslink=1 where required.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val legacyWholeArchive = Flag.Boolean("legacyWholeArchive")

  @Option(
    name = "line_terminator_null",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """Whether each format is terminated with \0 instead of newline.""",
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val lineTerminatorNull = Flag.Boolean("lineTerminatorNull")

  @Option(
    name = "linkopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional option to pass to gcc when linking.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val linkopt = Flag.Str("linkopt")

  @Option(
    name = "loading_phase_threads",
    defaultValue = "auto",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      Number of parallel threads to use for the loading/analysis phase.Takes an integer, or a keyword ("auto",
      "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5".
      "auto" sets a reasonable default based on host resources. Must be at least 1.
    """,
    valueHelp = """
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation
      ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val loadingPhaseThreads = Flag.Unknown("loadingPhaseThreads")

  @Option(
    name = "local_cpu_resources",
    defaultValue = "HOST_CPUS",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Explicitly set the total number of local CPU cores available to Bazel to spend on build actions executed
      locally. Takes an integer, or "HOST_CPUS", optionally followed by [-|*]<float> (eg. HOST_CPUS*.5 to use half
      the available CPU cores). By default, ("HOST_CPUS"), Bazel will query system configuration to estimate the
      number of CPU cores available.
    """,
    valueHelp = """an integer, or "HOST_CPUS", optionally followed by [-|*]<float>.""",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val localCpuResources = Flag.Unknown("localCpuResources")

  @Option(
    name = "local_extra_resources",
    allowMultiple = true,
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Set the number of extra resources available to Bazel. Takes in a string-float pair. Can be used multiple
      times to specify multiple types of extra resources. Bazel will limit concurrently running actions based on
      the available extra resources and the extra resources required. Tests can declare the amount of extra
      resources they need by using a tag of the "resources:<resoucename>:<amount>" format. Available CPU, RAM and
      resources cannot be set with this flag.
    """,
    valueHelp = "a named float, 'name=value'",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val localExtraResources = Flag.Unknown("localExtraResources")

  @Option(
    name = "local_ram_resources",
    defaultValue = "HOST_RAM*.67",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Explicitly set the total amount of local host RAM (in MB) available to Bazel to spend on build actions
      executed locally. Takes an integer, or "HOST_RAM", optionally followed by [-|*]<float> (eg. HOST_RAM*.5 to
      use half the available RAM). By default, ("HOST_RAM*.67"), Bazel will query system configuration to estimate
      the amount of RAM available and will use 67% of it.
    """,
    valueHelp = """an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.""",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val localRamResources = Flag.Unknown("localRamResources")

  @Option(
    name = "local_resources",
    allowMultiple = true,
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Set the number of resources available to Bazel. Takes in an assignment to a float or HOST_RAM/HOST_CPUS,
      optionally followed by [-|*]<float> (eg. memory=HOST_RAM*.5 to use half the available RAM). Can be used
      multiple times to specify multiple types of resources. Bazel will limit concurrently running actions based on
      the available resources and the resources required. Tests can declare the amount of resources they need by
      using a tag of the "resources:<resource name>:<amount>" format. Overrides resources specified by
      --local_{cpu|ram|extra}_resources.
    """,
    valueHelp = """
      a named double, 'name=value', where value is an integer, or a keyword ("auto", "HOST_CPUS",
      "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val localResources = Flag.Unknown("localResources")

  @Option(
    name = "local_startup_timeout_secs",
    defaultValue = "120",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = "The maximum amount of time the client waits to connect to the server",
    valueHelp = "an integer",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val localStartupTimeoutSecs = Flag.Integer("localStartupTimeoutSecs")

  @Option(
    name = "local_termination_grace_seconds",
    oldName = "local_sigkill_grace_seconds",
    defaultValue = "15",
    help = """
      Time to wait between terminating a local process due to timeout and forcefully shutting it down.
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val localTerminationGraceSeconds = Flag.Integer("localTerminationGraceSeconds")

  @Option(
    name = "local_test_jobs",
    defaultValue = "auto",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      The max number of local test jobs to run concurrently. Takes an integer, or a keyword ("auto", "HOST_CPUS",
      "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". 0 means local
      resources will limit the number of local test jobs to run concurrently instead. Setting this greater than the
      value for --jobs is ineffectual.
    """,
    valueHelp = """
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation
      ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val localTestJobs = Flag.Unknown("localTestJobs")

  @Option(
    name = "lock_install_base",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = """
      Whether the server should hold a lock on the install base while running, to prevent another server from
      attempting to garbage collect it.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val lockInstallBase = Flag.Boolean("lockInstallBase")

  @Option(
    name = "lockfile_mode",
    defaultValue = "update",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Specifies how and whether or not to use the lockfile. Valid values are `update` to use the lockfile and
      update it if there are changes, `refresh` to additionally refresh mutable information (yanked versions and
      previously missing modules) from remote registries from time to time, `error` to use the lockfile but throw
      an error if it's not up-to-date, or `off` to neither read from or write to the lockfile.
    """,
    valueHelp = "off, update, refresh or error",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val lockfileMode = Flag.OneOf("lockfileMode")

  @Option(
    name = "log_top_n_packages",
    defaultValue = "10",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Configures number of packages included in top-package INFO logging, <= 0 disables.",
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val logTopNPackages = Flag.Integer("logTopNPackages")

  @Option(
    name = "logging",
    defaultValue = "3",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "The logging level.",
    valueHelp = "0 <= an integer <= 6",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val logging = Flag.Unknown("logging")

  @Option(
    name = "long",
    abbrev = 'l',
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    expandsTo = ["--help_verbosity=long"],
    help = "Show full description of each option, instead of just its name.",
    valueHelp = "",
    commands = ["help"],
  )
  @JvmField
  @Suppress("unused")
  val long = Flag.Unknown("long")

  @Option(
    name = "ltobackendopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional option to pass to the LTO backend step (under --features=thin_lto).",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val ltobackendopt = Flag.Str("ltobackendopt")

  @Option(
    name = "ltoindexopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional option to pass to the LTO indexing step (under --features=thin_lto).",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val ltoindexopt = Flag.Str("ltoindexopt")

  @Option(
    name = "macos_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Comma-separated list of architectures for which to build Apple macOS binaries.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val macosCpus = Flag.Unknown("macosCpus")

  @Option(
    name = "macos_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = "Minimum compatible macOS version for targets. If unspecified, uses 'macos_sdk_version'.",
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val macosMinimumOs = Flag.Unknown("macosMinimumOs")

  @Option(
    name = "macos_qos_class",
    defaultValue = "default",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Sets the QoS service class of the %{product} server when running on macOS. This flag has no effect on all
      other platforms but is supported to ensure rc files can be shared among them without changes. Possible values
      are: user-interactive, user-initiated, default, utility, and background.
    """,
    valueHelp = "a string",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val macosQosClass = Flag.Str("macosQosClass")

  @Option(
    name = "macos_sdk_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      Specifies the version of the macOS SDK to use to build macOS applications. If unspecified, uses the default
      macOS SDK version from 'xcode_version'.
    """,
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val macosSdkVersion = Flag.Unknown("macosSdkVersion")

  @Option(
    name = "make_variables_source",
    defaultValue = "configuration",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.HIDDEN, OptionMetadataTag.DEPRECATED],
    help = "",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val makeVariablesSource = Flag.Str("makeVariablesSource")

  @Option(
    name = "materialize_param_files",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Writes intermediate parameter files to output tree even when using remote action execution. Useful when
      debugging actions. This is implied by --subcommands and --verbose_failures.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val materializeParamFiles = Flag.Boolean("materializeParamFiles")

  @Option(
    name = "max_computation_steps",
    defaultValue = "0",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """
      The maximum number of Starlark computation steps that may be executed by a BUILD file (zero means no limit).
    """,
    valueHelp = "a long integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val maxComputationSteps = Flag.Unknown("maxComputationSteps")

  @Option(
    name = "max_config_changes_to_show",
    defaultValue = "3",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      When discarding the analysis cache due to a change in the build options, displays up to the given number of
      changed option names. If the number given is -1, all changed options will be displayed.
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val maxConfigChangesToShow = Flag.Integer("maxConfigChangesToShow")

  @Option(
    name = "max_idle_secs",
    defaultValue = "10800",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      The number of seconds the build server will wait idling before shutting down. Zero means that the server will
      never shutdown. This is only read on server-startup, changing this option will not cause the server to
      restart.
    """,
    valueHelp = "an integer",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val maxIdleSecs = Flag.Integer("maxIdleSecs")

  @Option(
    name = "max_test_output_bytes",
    defaultValue = "-1",
    effectTags = [OptionEffectTag.TEST_RUNNER, OptionEffectTag.TERMINAL_OUTPUT, OptionEffectTag.EXECUTION],
    help = """
      Specifies maximum per-test-log size that can be emitted when --test_output is 'errors' or 'all'. Useful for
      avoiding overwhelming the output with excessively noisy test output. The test header is included in the log
      size. Negative values imply no limit. Output is all or nothing.
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val maxTestOutputBytes = Flag.Integer("maxTestOutputBytes")

  @Option(
    name = "memory",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Dump the memory use of the given Skyframe node.",
    valueHelp = "memory mode",
    commands = ["dump"],
  )
  @JvmField
  @Suppress("unused")
  val memory = Flag.Unknown("memory")

  @Option(
    name = "memory_profile",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      If set, write memory usage data to the specified file at phase ends and stable heap to master log at end of
      build.
    """,
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val memoryProfile = Flag.Path("memoryProfile")

  @Option(
    name = "memory_profile_stable_heap_parameters",
    defaultValue = "1,0",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      Tune memory profile's computation of stable heap at end of build. Should be and even number of  integers
      separated by commas. In each pair the first integer is the number of GCs to perform. The second integer in
      each pair is the number of seconds to wait between GCs. Ex: 2,4,4,0 would 2 GCs with a 4sec pause, followed
      by 4 GCs with zero second pause
    """,
    valueHelp = "integers, separated by a comma expected in pairs",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val memoryProfileStableHeapParameters = Flag.Unknown("memoryProfileStableHeapParameters")

  @Option(
    name = "memprof_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Use memprof profile.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val memprofProfile = Flag.Label("memprofProfile")

  @Option(
    name = "merge_android_manifest_permissions",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If enabled, the manifest merger will merge uses-permission and uses-permission-sdk-23 attributes.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val mergeAndroidManifestPermissions = Flag.Boolean("mergeAndroidManifestPermissions")

  @Option(
    name = "min_param_file_size",
    defaultValue = "32768",
    effectTags = [
      OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION,
      OptionEffectTag.ACTION_COMMAND_LINES,
    ],
    help = "Minimum command line length before creating a parameter file.",
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val minParamFileSize = Flag.Integer("minParamFileSize")

  @Option(
    name = "minimum_os_version",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "The minimum OS version which your compilation targets.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val minimumOsVersion = Flag.Str("minimumOsVersion")

  @Option(
    name = "mobile_install_aspect",
    defaultValue = "@android_test_support//tools/android/mobile_install:mobile-install.bzl",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.CHANGES_INPUTS],
    help = "The aspect to use for mobile-install.",
    valueHelp = "a string",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val mobileInstallAspect = Flag.Str("mobileInstallAspect")

  @Option(
    name = "mobile_install_run_deployer",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION],
    help = "Whether to run the mobile-install deployer after building all artifacts.",
    valueHelp = "a boolean",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val mobileInstallRunDeployer = Flag.Boolean("mobileInstallRunDeployer")

  @Option(
    name = "mobile_install_supported_rules",
    defaultValue = "",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "The supported rules for mobile-install.",
    valueHelp = "comma-separated list of options",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val mobileInstallSupportedRules = Flag.Unknown("mobileInstallSupportedRules")

  @Option(
    name = "mode",
    defaultValue = "skylark",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = "Deprecated no-effect flag. Only skylark mode is still supported.",
    valueHelp = "classic, classic_internal_test_do_not_use or skylark",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val mode = Flag.OneOf("mode")

  @Option(
    name = "modify_execution_info",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Add or remove keys from an action's execution info based on action mnemonic.  Applies only to actions which
      support execution info. Many common actions support execution info, e.g. Genrule, CppCompile, Javac,
      StarlarkAction, TestRunner. When specifying multiple values, order matters because many regexes may apply to
      the same mnemonic.
      
      Syntax: "regex=[+-]key,regex=[+-]key,...".
      
      Examples:
        '.*=+x,.*=-y,.*=+z' adds 'x' and 'z' to, and removes 'y' from, the execution info for all actions.
        'Genrule=+requires-x' adds 'requires-x' to the execution info for all Genrule actions.
        '(?!Genrule).*=-requires-x' removes 'requires-x' from the execution info for all non-Genrule actions.
      
    """,
    valueHelp = "regex=[+-]key,regex=[+-]key,...",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val modifyExecutionInfo = Flag.Unknown("modifyExecutionInfo")

  @Option(
    name = "nested_set_depth_limit",
    defaultValue = "3500",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      The maximum depth of the graph internal to a depset (also known as NestedSet), above which the depset()
      constructor will fail.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val nestedSetDepthLimit = Flag.Integer("nestedSetDepthLimit")

  @Option(
    name = "nodep_deps",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """
      If enabled, deps from "nodep" attributes will be included in the dependency graph over which the query
      operates. A common example of a "nodep" attribute is "visibility". Run and parse the output of `info
      build-language` to learn about all the "nodep" attributes in the build language.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val nodepDeps = Flag.Boolean("nodepDeps")

  @Option(
    name = "non_incremental_per_target_dexopts",
    defaultValue = "--positions",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      dx flags that that prevent incremental dexing for binary targets that list any of the flags listed here in
      their 'dexopts' attribute, which are ignored with incremental dexing (superseding
      --dexopts_supported_in_incremental_dexing).  Defaults to --positions for safety but can in general be used to
      make sure the listed dx flags are honored, with additional build latency.  Please notify us if you find
      yourself needing this flag.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val nonIncrementalPerTargetDexopts = Flag.Unknown("nonIncrementalPerTargetDexopts")

  @Option(
    name = "noorder_results",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    expandsTo = ["--order_output=no"],
    help = """
      Output the results in dependency-ordered (default) or unordered fashion. The unordered output is faster but
      only supported when --output is not minrank, maxrank, or graph.
    """,
    valueHelp = "",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val noorderResults = Flag.Unknown("noorderResults")

  @Option(
    name = "null",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    expandsTo = ["--line_terminator_null=true"],
    help = """Whether each format is terminated with \0 instead of newline.""",
    valueHelp = "",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val `null` = Flag.Unknown("null")

  @Option(
    name = "objc_debug_with_GLIBCXX",
    defaultValue = "false",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """
      If set, and compilation mode is set to 'dbg', define GLIBCXX_DEBUG,  GLIBCXX_DEBUG_PEDANTIC and
      GLIBCPP_CONCEPT_CHECKS.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val objcDebugWith_GLIBCXX = Flag.Boolean("objcDebugWith_GLIBCXX")

  @Option(
    name = "objc_enable_binary_stripping",
    defaultValue = "false",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """
      Whether to perform symbol and dead-code strippings on linked binaries. Binary strippings will be performed if
      both this flag and --compilation_mode=opt are specified.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val objcEnableBinaryStripping = Flag.Boolean("objcEnableBinaryStripping")

  @Option(
    name = "objc_generate_linkmap",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Specifies whether to generate a linkmap file.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val objcGenerateLinkmap = Flag.Boolean("objcGenerateLinkmap")

  @Option(
    name = "objc_use_dotd_pruning",
    defaultValue = "true",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      If set, .d files emitted by clang will be used to prune the set of inputs passed into objc compiles.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val objcUseDotdPruning = Flag.Boolean("objcUseDotdPruning")

  @Option(
    name = "objccopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = "Additional options to pass to gcc when compiling Objective-C/C++ source files.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val objccopt = Flag.Str("objccopt")

  @Option(
    name = "one_version_enforcement_on_java_tests",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      When enabled, and with experimental_one_version_enforcement set to a non-NONE value, enforce one version on
      java_test targets. This flag can be disabled to improve incremental test performance at the expense of
      missing potential one version violations.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val oneVersionEnforcementOnJavaTests = Flag.Boolean("oneVersionEnforcementOnJavaTests")

  @Option(
    name = "only",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      If this option is given, only sync the repositories specified with this option. Still consider all (or all
      configure-like, of --configure is given) outdated.
    """,
    valueHelp = "a string",
    commands = ["sync"],
  )
  @JvmField
  @Suppress("unused")
  val only = Flag.Str("only")

  @Option(
    name = "oom_message",
    defaultValue = "",
    effectTags = [OptionEffectTag.BAZEL_MONITORING, OptionEffectTag.TERMINAL_OUTPUT],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "Custom message to be emitted on an out of memory failure.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val oomMessage = Flag.Str("oomMessage")

  @Option(
    name = "optimizing_dexer",
    help = "Specifies a binary to use to do dexing without sharding.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val optimizingDexer = Flag.Label("optimizingDexer")

  @Option(
    name = "option_sources",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "",
    valueHelp = "a list of option-source pairs",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val optionSources = Flag.Unknown("optionSources")

  @Option(
    name = "order_output",
    defaultValue = "auto",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Output the results unordered (no), dependency-ordered (deps), or fully ordered (full). The default is 'auto',
      meaning that results are output either dependency-ordered or fully ordered, depending on the output formatter
      (dependency-ordered for proto, minrank, maxrank, and graph, fully ordered for all others). When output is
      fully ordered, nodes are printed in a fully deterministic (total) order. First, all nodes are sorted
      alphabetically. Then, each node in the list is used as the start of a post-order depth-first search in which
      outgoing edges to unvisited nodes are traversed in alphabetical order of the successor nodes. Finally, nodes
      are printed in the reverse of the order in which they were visited.
    """,
    valueHelp = "no, deps, auto or full",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val orderOutput = Flag.OneOf("orderOutput")

  @Option(
    name = "order_results",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    expandsTo = ["--order_output=auto"],
    help = """
      Output the results in dependency-ordered (default) or unordered fashion. The unordered output is faster but
      only supported when --output is not minrank, maxrank, or graph.
    """,
    valueHelp = "",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val orderResults = Flag.Unknown("orderResults")

  @Option(
    name = "output",
    defaultValue = "text",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      The format in which the aquery results should be printed. Allowed values for aquery are: text, textproto,
      proto, streamed_proto, jsonproto.
    """,
    valueHelp = "a string",
    commands = ["aquery", "config", "cquery", "mod", "query"],
  )
  @JvmField
  @Suppress("unused")
  val output = Flag.Str("output")

  @Option(
    name = "output_base",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      If set, specifies the output location to which all build output will be written. Otherwise, the location will
      be ${'$'}{OUTPUT_ROOT}/_blaze_${'$'}{USER}/${'$'}{MD5_OF_WORKSPACE_ROOT}. Note: If you specify a different
      option from one to the next Bazel invocation for this value, you'll likely start up a new, additional Bazel
      server. Bazel starts exactly one server per specified output base. Typically there is one output base per
      workspace - however, with this option you may have multiple output bases per workspace and thereby run
      multiple builds for the same client on the same machine concurrently. See 'bazel help shutdown' on how to
      shutdown a Bazel server.
    """,
    valueHelp = "a path",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val outputBase = Flag.Path("outputBase")

  @Option(
    name = "output_filter",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Only shows warnings and action outputs for rules with a name matching the provided regular expression.
    """,
    valueHelp = "a valid Java regular expression",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val outputFilter = Flag.Unknown("outputFilter")

  @Option(
    name = "output_groups",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      A list of comma-separated output group names, each of which optionally prefixed by a + or a -. A group
      prefixed by + is added to the default set of output groups, while a group prefixed by - is removed from the
      default set. If at least one group is not prefixed, the default set of output groups is omitted. For example,
      --output_groups=+foo,+bar builds the union of the default set, foo, and bar, while --output_groups=foo,bar
      overrides the default set such that only foo and bar are built.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val outputGroups = Flag.Unknown("outputGroups")

  @Option(
    name = "output_library_merged_assets",
    defaultValue = "true",
    help = "If disabled, does not produce merged asset.zip outputs for library targets",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val outputLibraryMergedAssets = Flag.Boolean("outputLibraryMergedAssets")

  @Option(
    name = "output_tree_tracking",
    oldName = "experimental_output_tree_tracking",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      If set, tell the output service (if any) to track when files in the output tree have been modified externally
      (not by the build system). This should improve incremental build speed when an appropriate output service is
      enabled.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val outputTreeTracking = Flag.Boolean("outputTreeTracking")

  @Option(
    name = "output_user_root",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      The user-specific directory beneath which all build outputs are written; by default, this is a function of
      ${'$'}USER, but by specifying a constant, build outputs can be shared between collaborating users.
    """,
    valueHelp = "a path",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val outputUserRoot = Flag.Path("outputUserRoot")

  @Option(
    name = "override_module",
    allowMultiple = true,
    help = """
      Override a module with a local path in the form of <module name>=<path>. If the given path is an absolute
      path, it will be used as it is. If the given path is a relative path, it is relative to the current working
      directory. If the given path starts with '%workspace%, it is relative to the workspace root, which is the
      output of `bazel info workspace`. If the given path is empty, then remove any previous overrides.
    """,
    valueHelp = "an equals-separated mapping of module name to path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val overrideModule = Flag.Unknown("overrideModule")

  @Option(
    name = "override_repository",
    allowMultiple = true,
    help = """
      Override a repository with a local path in the form of <repository name>=<path>. If the given path is an
      absolute path, it will be used as it is. If the given path is a relative path, it is relative to the current
      working directory. If the given path starts with '%workspace%, it is relative to the workspace root, which is
      the output of `bazel info workspace`. If the given path is empty, then remove any previous overrides.
    """,
    valueHelp = "an equals-separated mapping of repository name to path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val overrideRepository = Flag.Unknown("overrideRepository")

  @Option(
    name = "package_path",
    defaultValue = "%workspace%",
    help = """
      A colon-separated list of where to look for packages. Elements beginning with '%workspace%' are relative to
      the enclosing workspace. If omitted or empty, the default is the output of 'bazel info default-package-path'.
    """,
    valueHelp = "colon-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val packagePath = Flag.Unknown("packagePath")

  @Option(
    name = "packages",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Dump package cache content.",
    valueHelp = "a boolean",
    commands = ["dump"],
  )
  @JvmField
  @Suppress("unused")
  val packages = Flag.Boolean("packages")

  @Option(
    name = "per_file_copt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Additional options to selectively pass to gcc when compiling certain files. This option can be passed
      multiple times. Syntax: regex_filter@option_1,option_2,...,option_n. Where regex_filter stands for a list of
      include and exclude regular expression patterns (Also see --instrumentation_filter). option_1 to option_n
      stand for arbitrary command line options. If an option contains a comma it has to be quoted with a backslash.
      Options can contain @. Only the first @ is used to split the string. Example:
      --per_file_copt=//foo/.*\.cc,-//foo/bar\.cc@-O0 adds the -O0 command line option to the gcc command line of
      all cc files in //foo/ except bar.cc.
    """,
    valueHelp = """
      a comma-separated list of regex expressions with prefix '-' specifying excluded paths followed by
      an @ and a comma separated list of options
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val perFileCopt = Flag.Unknown("perFileCopt")

  @Option(
    name = "per_file_ltobackendopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Additional options to selectively pass to LTO backend (under --features=thin_lto) when compiling certain
      backend objects. This option can be passed multiple times. Syntax:
      regex_filter@option_1,option_2,...,option_n. Where regex_filter stands for a list of include and exclude
      regular expression patterns. option_1 to option_n stand for arbitrary command line options. If an option
      contains a comma it has to be quoted with a backslash. Options can contain @. Only the first @ is used to
      split the string. Example: --per_file_ltobackendopt=//foo/.*\.o,-//foo/bar\.o@-O0 adds the -O0 command line
      option to the LTO backend command line of all o files in //foo/ except bar.o.
    """,
    valueHelp = """
      a comma-separated list of regex expressions with prefix '-' specifying excluded paths followed by
      an @ and a comma separated list of options
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val perFileLtobackendopt = Flag.Unknown("perFileLtobackendopt")

  @Option(
    name = "persistent_android_dex_desugar",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    expandsTo = [
      "--internal_persistent_android_dex_desugar", "--strategy=Desugar=worker",
      "--strategy=DexBuilder=worker",
    ],
    help = "Enable persistent Android dex and desugar actions by using workers.",
    valueHelp = "",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val persistentAndroidDexDesugar = Flag.Unknown("persistentAndroidDexDesugar")

  @Option(
    name = "persistent_android_resource_processor",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    expandsTo = [
      "--internal_persistent_busybox_tools", "--strategy=AaptPackage=worker",
      "--strategy=AndroidResourceParser=worker", "--strategy=AndroidResourceValidator=worker",
      "--strategy=AndroidResourceCompiler=worker", "--strategy=RClassGenerator=worker",
      "--strategy=AndroidResourceLink=worker", "--strategy=AndroidAapt2=worker",
      "--strategy=AndroidAssetMerger=worker", "--strategy=AndroidResourceMerger=worker",
      "--strategy=AndroidCompiledResourceMerger=worker", "--strategy=ManifestMerger=worker",
      "--strategy=AndroidManifestMerger=worker", "--strategy=Aapt2Optimize=worker",
      "--strategy=AARGenerator=worker", "--strategy=ProcessDatabinding=worker",
      "--strategy=GenerateDataBindingBaseClasses=worker",
    ],
    help = "Enable persistent Android resource processor by using workers.",
    valueHelp = "",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val persistentAndroidResourceProcessor = Flag.Unknown("persistentAndroidResourceProcessor")

  @Option(
    name = "persistent_multiplex_android_dex_desugar",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    expandsTo = ["--persistent_android_dex_desugar", "--internal_persistent_multiplex_android_dex_desugar"],
    help = "Enable persistent multiplexed Android dex and desugar actions by using workers.",
    valueHelp = "",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val persistentMultiplexAndroidDexDesugar = Flag.Unknown("persistentMultiplexAndroidDexDesugar")

  @Option(
    name = "persistent_multiplex_android_resource_processor",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    expandsTo = [
      "--persistent_android_resource_processor",
      "--modify_execution_info=AaptPackage=+supports-multiplex-workers",
      "--modify_execution_info=AndroidResourceParser=+supports-multiplex-workers",
      "--modify_execution_info=AndroidResourceValidator=+supports-multiplex-workers",
      "--modify_execution_info=AndroidResourceCompiler=+supports-multiplex-workers",
      "--modify_execution_info=RClassGenerator=+supports-multiplex-workers",
      "--modify_execution_info=AndroidResourceLink=+supports-multiplex-workers",
      "--modify_execution_info=AndroidAapt2=+supports-multiplex-workers",
      "--modify_execution_info=AndroidAssetMerger=+supports-multiplex-workers",
      "--modify_execution_info=AndroidResourceMerger=+supports-multiplex-workers",
      "--modify_execution_info=AndroidCompiledResourceMerger=+supports-multiplex-workers",
      "--modify_execution_info=ManifestMerger=+supports-multiplex-workers",
      "--modify_execution_info=AndroidManifestMerger=+supports-multiplex-workers",
      "--modify_execution_info=Aapt2Optimize=+supports-multiplex-workers",
      "--modify_execution_info=AARGenerator=+supports-multiplex-workers",
    ],
    help = "Enable persistent multiplexed Android resource processor by using workers.",
    valueHelp = "",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val persistentMultiplexAndroidResourceProcessor = Flag.Unknown("persistentMultiplexAndroidResourceProcessor")

  @Option(
    name = "persistent_multiplex_android_tools",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    expandsTo = [
      "--internal_persistent_multiplex_busybox_tools",
      "--persistent_multiplex_android_resource_processor", "--persistent_multiplex_android_dex_desugar",
    ],
    help = "Enable persistent and multiplexed Android tools (dexing, desugaring, resource processing).",
    valueHelp = "",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val persistentMultiplexAndroidTools = Flag.Unknown("persistentMultiplexAndroidTools")

  @Option(
    name = "platform_mappings",
    defaultValue = "",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    metadataTags = [OptionMetadataTag.IMMUTABLE],
    help = """
      The location of a mapping file that describes which platform to use if none is set or which flags to set when
      a platform already exists. Must be relative to the main workspace root. Defaults to 'platform_mappings' (a
      file directly under the workspace root).
    """,
    valueHelp = "a main workspace-relative path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val platformMappings = Flag.Unknown("platformMappings")

  @Option(
    name = "platform_suffix",
    effectTags = [
      OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.AFFECTS_OUTPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    help = "Specifies a suffix to be added to the configuration directory.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val platformSuffix = Flag.Str("platformSuffix")

  @Option(
    name = "platforms",
    oldName = "experimental_platforms",
    defaultValue = "",
    effectTags = [
      OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS,
      OptionEffectTag.LOADING_AND_ANALYSIS,
    ],
    help = "The labels of the platform rules describing the target platforms for the current command.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val platforms = Flag.Label("platforms")

  @Option(
    name = "plugin",
    allowMultiple = true,
    help = "Plugins to use in the build. Currently works with java_plugin.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val plugin = Flag.Label("plugin")

  @Option(
    name = "portable_paths",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "If true, includes paths to replace in ExecRequest to make the resulting paths portable.",
    valueHelp = "a boolean",
    commands = ["run"],
  )
  @JvmField
  @Suppress("unused")
  val portablePaths = Flag.Boolean("portablePaths")

  @Option(
    name = "preemptible",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = "If true, the command can be preempted if another command is started.",
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val preemptible = Flag.Boolean("preemptible")

  @Option(
    name = "print_action_mnemonics",
    allowMultiple = true,
    help = "Lists which mnemonics to filter print_action data by, no filtering takes place when left empty.",
    valueHelp = "a string",
    commands = ["print_action"],
  )
  @JvmField
  @Suppress("unused")
  val printActionMnemonics = Flag.Str("printActionMnemonics")

  @Option(
    name = "print_relative_test_log_paths",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If true, when printing the path to a test log, use relative path that makes use of the 'testlogs' convenience
      symlink. N.B. - A subsequent 'build'/'test'/etc invocation with a different configuration can cause the
      target of this symlink to change, making the path printed previously no longer useful.
    """,
    valueHelp = "a boolean",
    commands = ["coverage", "cquery", "fetch", "test", "vendor"],
  )
  @JvmField
  @Suppress("unused")
  val printRelativeTestLogPaths = Flag.Boolean("printRelativeTestLogPaths")

  @Option(
    name = "process_headers_in_dependencies",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      When building a target //a:a, process headers in all targets that //a:a depends on (if header processing is
      enabled for the toolchain).
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val processHeadersInDependencies = Flag.Boolean("processHeadersInDependencies")

  @Option(
    name = "product_name",
    defaultValue = "bazel",
    effectTags = [
      OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.AFFECTS_OUTPUTS,
      OptionEffectTag.BAZEL_MONITORING,
    ],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = """
      The name of the build system. It is used as part of the name of the generated directories (e.g.
      productName-bin for binaries) as well as for printing error messages and logging
    """,
    valueHelp = "a string",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val productName = Flag.Str("productName")

  @Option(
    name = "profile",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      If set, profile Bazel and write data to the specified file. Use bazel analyze-profile to analyze the profile.
    """,
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val profile = Flag.Path("profile")

  @Option(
    name = "profiles_to_retain",
    defaultValue = "5",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      Number of profiles to retain in the output base. If there are more than this number of profiles in the output
      base, the oldest are deleted until the total is under the limit.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val profilesToRetain = Flag.Integer("profilesToRetain")

  @Option(
    name = "progress_in_terminal_title",
    defaultValue = "false",
    help = """
      Show the command progress in the terminal title. Useful to see what bazel is doing when having multiple
      terminal tabs.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val progressInTerminalTitle = Flag.Boolean("progressInTerminalTitle")

  @Option(
    name = "progress_report_interval",
    defaultValue = "0",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      The number of seconds to wait between reports on still running jobs. The default value 0 means the first
      report will be printed after 10 seconds, then 30 seconds and after that progress is reported once every
      minute. When --curses is enabled, progress is reported every second.
    """,
    valueHelp = "an integer in 0-3600 range",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val progressReportInterval = Flag.Unknown("progressReportInterval")

  @Option(
    name = "proguard_top",
    help = "Specifies which version of ProGuard to use for code removal when building a Java binary.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val proguardTop = Flag.Label("proguardTop")

  @Option(
    name = "propeller_optimize",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Use Propeller profile information to optimize the build target.A propeller profile must consist of at least
      one of two files, a cc profile and a ld profile.  This flag accepts a build label which must refer to the
      propeller profile input files. For example, the BUILD file that defines the label, in
      a/b/BUILD:propeller_optimize(    name = "propeller_profile",    cc_profile = "propeller_cc_profile.txt",
      ld_profile = "propeller_ld_profile.txt",)An exports_files directive may have to be added to the corresponding
      package to make these files visible to Bazel. The option must be used as:
      --propeller_optimize=//a/b:propeller_profile
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val propellerOptimize = Flag.Label("propellerOptimize")

  @Option(
    name = "propeller_optimize_absolute_cc_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Absolute path name of cc_profile file for Propeller Optimized builds.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val propellerOptimizeAbsoluteCcProfile = Flag.Str("propellerOptimizeAbsoluteCcProfile")

  @Option(
    name = "propeller_optimize_absolute_ld_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Absolute path name of ld_profile file for Propeller Optimized builds.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val propellerOptimizeAbsoluteLdProfile = Flag.Str("propellerOptimizeAbsoluteLdProfile")

  @Option(
    name = "proto:default_values",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      If true, attributes whose value is not explicitly specified in the BUILD file are included; otherwise they
      are omitted. This option is applicable to --output=proto
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_defaultValues = Flag.Boolean("proto_defaultValues")

  @Option(
    name = "proto:definition_stack",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Populate the definition_stack proto field, which records for each rule instance the Starlark call stack at
      the moment the rule's class was defined.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_definitionStack = Flag.Boolean("proto_definitionStack")

  @Option(
    name = "proto:flatten_selects",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """
      If enabled, configurable attributes created by select() are flattened. For list types the flattened
      representation is a list containing each value of the select map exactly once. Scalar types are flattened to
      null.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_flattenSelects = Flag.Boolean("proto_flattenSelects")

  @Option(
    name = "proto:include_attribute_source_aspects",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Populate the source_aspect_name proto field of each Attribute with the source aspect that the attribute came
      from (empty string if it did not).
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_includeAttributeSourceAspects = Flag.Boolean("proto_includeAttributeSourceAspects")

  @Option(
    name = "proto:include_configurations",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      if enabled, proto output will include information about configurations. When disabled,cquery proto output
      format resembles query output format.
    """,
    valueHelp = "a boolean",
    commands = ["cquery"],
  )
  @JvmField
  @Suppress("unused")
  val proto_includeConfigurations = Flag.Boolean("proto_includeConfigurations")

  @Option(
    name = "proto:include_synthetic_attribute_hash",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Whether or not to calculate and populate the ${'$'}internal_attr_hash attribute.",
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_includeSyntheticAttributeHash = Flag.Boolean("proto_includeSyntheticAttributeHash")

  @Option(
    name = "proto:instantiation_stack",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Populate the instantiation call stack of each rule. Note that this requires the stack to be present
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_instantiationStack = Flag.Boolean("proto_instantiationStack")

  @Option(
    name = "proto:locations",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Whether to output location information in proto output at all.",
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_locations = Flag.Boolean("proto_locations")

  @Option(
    name = "proto:output_rule_attrs",
    defaultValue = "all",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Comma separated list of attributes to include in output. Defaults to all attributes. Set to empty string to
      not output any attribute. This option is applicable to --output=proto.
    """,
    valueHelp = "comma-separated list of options",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_outputRuleAttrs = Flag.Unknown("proto_outputRuleAttrs")

  @Option(
    name = "proto:rule_classes",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Populate the rule_class_key field of each rule; and for the first rule with a given rule_class_key, also
      populate its rule_class_info proto field. The rule_class_key field uniquely identifies a rule class, and the
      rule_class_info field is a Stardoc-format rule class API definition.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_ruleClasses = Flag.Boolean("proto_ruleClasses")

  @Option(
    name = "proto:rule_inputs_and_outputs",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "Whether or not to populate the rule_input and rule_output fields.",
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val proto_ruleInputsAndOutputs = Flag.Boolean("proto_ruleInputsAndOutputs")

  @Option(
    name = "proto_compiler",
    defaultValue = "@bazel_tools//tools/proto:protoc",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "The label of the proto-compiler.",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val protoCompiler = Flag.Label("protoCompiler")

  @Option(
    name = "proto_profile",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Whether to pass profile_path to the proto compiler.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val protoProfile = Flag.Boolean("protoProfile")

  @Option(
    name = "proto_profile_path",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      The profile to pass to the proto compiler as profile_path. If unset, but  --proto_profile is true (the
      default), infers the path from --fdo_optimize.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val protoProfilePath = Flag.Label("protoProfilePath")

  @Option(
    name = "proto_toolchain_for_cc",
    defaultValue = "@bazel_tools//tools/proto:cc_toolchain",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Label of proto_lang_toolchain() which describes how to compile C++ protos",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val protoToolchainForCc = Flag.Label("protoToolchainForCc")

  @Option(
    name = "proto_toolchain_for_j2objc",
    defaultValue = "@bazel_tools//tools/j2objc:j2objc_proto_toolchain",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Label of proto_lang_toolchain() which describes how to compile j2objc protos",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val protoToolchainForJ2objc = Flag.Label("protoToolchainForJ2objc")

  @Option(
    name = "proto_toolchain_for_java",
    defaultValue = "@bazel_tools//tools/proto:java_toolchain",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Label of proto_lang_toolchain() which describes how to compile Java protos",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val protoToolchainForJava = Flag.Label("protoToolchainForJava")

  @Option(
    name = "proto_toolchain_for_javalite",
    defaultValue = "@bazel_tools//tools/proto:javalite_toolchain",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Label of proto_lang_toolchain() which describes how to compile JavaLite protos",
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val protoToolchainForJavalite = Flag.Label("protoToolchainForJavalite")

  @Option(
    name = "protocopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional options to pass to the protobuf compiler.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val protocopt = Flag.Str("protocopt")

  @Option(
    name = "python_native_rules_allowlist",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      An allowlist (package_group target) to use when enforcing --incompatible_python_disallow_native_rules.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val pythonNativeRulesAllowlist = Flag.Label("pythonNativeRulesAllowlist")

  @Option(
    name = "python_path",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      The absolute path of the Python interpreter invoked to run Python targets on the target platform. Deprecated;
      disabled by --incompatible_use_python_toolchains.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val pythonPath = Flag.Str("pythonPath")

  @Option(
    name = "python_top",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      The label of a py_runtime representing the Python interpreter invoked to run Python targets on the target
      platform. Deprecated; disabled by --incompatible_use_python_toolchains.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val pythonTop = Flag.Label("pythonTop")

  @Option(
    name = "python_version",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      The Python major version mode, either `PY2` or `PY3`. Note that this is overridden by `py_binary` and
      `py_test` targets (even if they don't explicitly specify a version) so there is usually not much reason to
      supply this flag.
    """,
    valueHelp = "PY2 or PY3",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val pythonVersion = Flag.OneOf("pythonVersion")

  @Option(
    name = "query_file",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      If set, query will read the query from the file named here, rather than on the command line. It is an error
      to specify a file here as well as a command-line query.
    """,
    valueHelp = "a string",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val queryFile = Flag.Str("queryFile")

  @Option(
    name = "quiet",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    help = """
      If true, no informational messages are emitted on the console, only errors. Changing this option will not
      cause the server to restart.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val quiet = Flag.Boolean("quiet")

  @Option(
    name = "rc_source",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val rcSource = Flag.Str("rcSource")

  @Option(
    name = "record_full_profiler_data",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      By default, Bazel profiler will record only aggregated data for fast but numerous events (such as statting
      the file). If this option is enabled, profiler will record each event - resulting in more precise profiling
      data but LARGE performance hit. Option only has effect if --profile used as well.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val recordFullProfilerData = Flag.Boolean("recordFullProfilerData")

  @Option(
    name = "record_metrics_for_all_packages",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Configures PackageMetrics to record all metrics for all packages. Disables Top-n INFO logging.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val recordMetricsForAllPackages = Flag.Boolean("recordMetricsForAllPackages")

  @Option(
    name = "redirect_local_instrumentation_output_writes",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      If true and supported, instrumentation output is redirected to be written locally on a different machine than
      where bazel is running on.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val redirectLocalInstrumentationOutputWrites = Flag.Boolean("redirectLocalInstrumentationOutputWrites")

  @Option(
    name = "registry",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      Specifies the registries to use to locate Bazel module dependencies. The order is important: modules will be
      looked up in earlier registries first, and only fall back to later registries when they're missing from the
      earlier ones.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val registry = Flag.Str("registry")

  @Option(
    name = "relative_locations",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      If true, the location of BUILD files in xml and proto outputs will be relative. By default, the location
      output is an absolute path and will not be consistent across machines. You can set this option to true to
      have a consistent result across machines.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val relativeLocations = Flag.Boolean("relativeLocations")

  @Option(
    name = "remote_accept_cached",
    defaultValue = "true",
    help = "Whether to accept remotely cached action results.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteAcceptCached = Flag.Boolean("remoteAcceptCached")

  @Option(
    name = "remote_build_event_upload",
    oldName = "experimental_remote_build_event_upload",
    defaultValue = "minimal",
    help = """
      If set to 'all', all local outputs referenced by BEP are uploaded to remote cache.
      If set to 'minimal', local outputs referenced by BEP are not uploaded to the remote cache, except for files
      that are important to the consumers of BEP (e.g. test logs and timing profile). bytestream:// scheme is
      always used for the uri of files even if they are missing from remote cache.
      Default to 'minimal'.
    """,
    valueHelp = "all or minimal",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteBuildEventUpload = Flag.OneOf("remoteBuildEventUpload")

  @Option(
    name = "remote_bytestream_uri_prefix",
    help = """
      The hostname and instance name to be used in bytestream:// URIs that are written into build event streams.
      This option can be set when builds are performed using a proxy, which causes the values of --remote_executor
      and --remote_instance_name to no longer correspond to the canonical name of the remote execution service.
      When not set, it will default to "${'$'}{hostname}/${'$'}{instance_name}".
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteBytestreamUriPrefix = Flag.Str("remoteBytestreamUriPrefix")

  @Option(
    name = "remote_cache",
    oldName = "remote_http_cache",
    help = """
      A URI of a caching endpoint. The supported schemas are http, https, grpc, grpcs (grpc with TLS enabled) and
      unix (local UNIX sockets). If no schema is provided Bazel will default to grpcs. Specify grpc://, http:// or
      unix: schema to disable TLS. See https://bazel.build/remote/caching
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteCache = Flag.Str("remoteCache")

  @Option(
    name = "remote_cache_async",
    oldName = "experimental_remote_cache_async",
    defaultValue = "true",
    help = """
      If true, uploading of action results to a disk or remote cache will happen in the background instead of
      blocking the completion of an action. Some actions are incompatible with background uploads, and may still
      block even when this flag is set.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteCacheAsync = Flag.Boolean("remoteCacheAsync")

  @Option(
    name = "remote_cache_compression",
    oldName = "experimental_remote_cache_compression",
    defaultValue = "false",
    help = """
      If enabled, compress/decompress cache blobs with zstd when their size is at least
      --experimental_remote_cache_compression_threshold.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteCacheCompression = Flag.Boolean("remoteCacheCompression")

  @Option(
    name = "remote_cache_header",
    allowMultiple = true,
    help = """
      Specify a header that will be included in cache requests: --remote_cache_header=Name=Value. Multiple headers
      can be passed by specifying the flag multiple times. Multiple values for the same name will be converted to a
      comma-separated list.
    """,
    valueHelp = "a 'name=value' assignment",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteCacheHeader = Flag.Unknown("remoteCacheHeader")

  @Option(
    name = "remote_default_exec_properties",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Set the default exec properties to be used as the remote execution platform if an execution platform does not
      already set exec_properties.
    """,
    valueHelp = "a 'name=value' assignment",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteDefaultExecProperties = Flag.Unknown("remoteDefaultExecProperties")

  @Option(
    name = "remote_default_platform_properties",
    oldName = "host_platform_remote_properties_override",
    defaultValue = "",
    help = """
      Set the default platform properties to be set for the remote execution API, if the execution platform does
      not already set remote_execution_properties. This value will also be used if the host platform is selected as
      the execution platform for remote execution.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteDefaultPlatformProperties = Flag.Str("remoteDefaultPlatformProperties")

  @Option(
    name = "remote_download_all",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    expandsTo = ["--remote_download_outputs=all"],
    help = """
      Downloads all remote outputs to the local machine. This flag is an alias for --remote_download_outputs=all.
    """,
    valueHelp = "",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadAll = Flag.Unknown("remoteDownloadAll")

  @Option(
    name = "remote_download_minimal",
    oldName = "experimental_remote_download_minimal",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    expandsTo = ["--remote_download_outputs=minimal"],
    help = """
      Does not download any remote build outputs to the local machine. This flag is an alias for
      --remote_download_outputs=minimal.
    """,
    valueHelp = "",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadMinimal = Flag.Unknown("remoteDownloadMinimal")

  @Option(
    name = "remote_download_outputs",
    oldName = "experimental_remote_download_outputs",
    defaultValue = "toplevel",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If set to 'minimal' doesn't download any remote build outputs to the local machine, except the ones required
      by local actions. If set to 'toplevel' behaves like'minimal' except that it also downloads outputs of top
      level targets to the local machine. Both options can significantly reduce build times if network bandwidth is
      a bottleneck.
    """,
    valueHelp = "all, minimal or toplevel",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadOutputs = Flag.OneOf("remoteDownloadOutputs")

  @Option(
    name = "remote_download_regex",
    oldName = "experimental_remote_download_regex",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Force remote build outputs whose path matches this pattern to be downloaded, irrespective of
      --remote_download_outputs. Multiple patterns may be specified by repeating this flag.
    """,
    valueHelp = "a valid Java regular expression",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadRegex = Flag.Unknown("remoteDownloadRegex")

  @Option(
    name = "remote_download_symlink_template",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Instead of downloading remote build outputs to the local machine, create symbolic links. The target of the
      symbolic links can be specified in the form of a template string. This template string may contain {hash} and
      {size_bytes} that expand to the hash of the object and the size in bytes, respectively. These symbolic links
      may, for example, point to a FUSE file system that loads objects from the CAS on demand.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadSymlinkTemplate = Flag.Str("remoteDownloadSymlinkTemplate")

  @Option(
    name = "remote_download_toplevel",
    oldName = "experimental_remote_download_toplevel",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    expandsTo = ["--remote_download_outputs=toplevel"],
    help = """
      Only downloads remote outputs of top level targets to the local machine. This flag is an alias for
      --remote_download_outputs=toplevel.
    """,
    valueHelp = "",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadToplevel = Flag.Unknown("remoteDownloadToplevel")

  @Option(
    name = "remote_downloader_header",
    allowMultiple = true,
    help = """
      Specify a header that will be included in remote downloader requests: --remote_downloader_header=Name=Value.
      Multiple headers can be passed by specifying the flag multiple times. Multiple values for the same name will
      be converted to a comma-separated list.
    """,
    valueHelp = "a 'name=value' assignment",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloaderHeader = Flag.Unknown("remoteDownloaderHeader")

  @Option(
    name = "remote_exec_header",
    allowMultiple = true,
    help = """
      Specify a header that will be included in execution requests: --remote_exec_header=Name=Value. Multiple
      headers can be passed by specifying the flag multiple times. Multiple values for the same name will be
      converted to a comma-separated list.
    """,
    valueHelp = "a 'name=value' assignment",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteExecHeader = Flag.Unknown("remoteExecHeader")

  @Option(
    name = "remote_execution_priority",
    defaultValue = "0",
    help = """
      The relative priority of actions to be executed remotely. The semantics of the particular priority values are
      server-dependent.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteExecutionPriority = Flag.Integer("remoteExecutionPriority")

  @Option(
    name = "remote_executor",
    help = """
      HOST or HOST:PORT of a remote execution endpoint. The supported schemas are grpc, grpcs (grpc with TLS
      enabled) and unix (local UNIX sockets). If no schema is provided Bazel will default to grpcs. Specify grpc://
      or unix: schema to disable TLS.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteExecutor = Flag.Str("remoteExecutor")

  @Option(
    name = "remote_grpc_log",
    oldName = "experimental_remote_grpc_log",
    help = """
      If specified, a path to a file to log gRPC call related details. This log consists of a sequence of
      serialized com.google.devtools.build.lib.remote.logging.RemoteExecutionLog.LogEntry protobufs with each
      message prefixed by a varint denoting the size of the following serialized protobuf message, as performed by
      the method LogEntry.writeDelimitedTo(OutputStream).
    """,
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteGrpcLog = Flag.Path("remoteGrpcLog")

  @Option(
    name = "remote_header",
    allowMultiple = true,
    help = """
      Specify a header that will be included in requests: --remote_header=Name=Value. Multiple headers can be
      passed by specifying the flag multiple times. Multiple values for the same name will be converted to a
      comma-separated list.
    """,
    valueHelp = "a 'name=value' assignment",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteHeader = Flag.Unknown("remoteHeader")

  @Option(
    name = "remote_instance_name",
    defaultValue = "",
    help = "Value to pass as instance_name in the remote execution API.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteInstanceName = Flag.Str("remoteInstanceName")

  @Option(
    name = "remote_local_fallback",
    defaultValue = "false",
    help = "Whether to fall back to standalone local execution strategy if remote execution fails.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteLocalFallback = Flag.Boolean("remoteLocalFallback")

  @Option(
    name = "remote_local_fallback_strategy",
    defaultValue = "local",
    help = "Deprecated. See https://github.com/bazelbuild/bazel/issues/7480 for details.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteLocalFallbackStrategy = Flag.Str("remoteLocalFallbackStrategy")

  @Option(
    name = "remote_max_connections",
    defaultValue = "100",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Limit the max number of concurrent connections to remote cache/executor. By default the value is 100. Setting
      this to 0 means no limitation.
      For HTTP remote cache, one TCP connection could handle one request at one time, so Bazel could make up to
      --remote_max_connections concurrent requests.
      For gRPC remote cache/executor, one gRPC channel could usually handle 100+ concurrent requests, so Bazel
      could make around `--remote_max_connections * 100` concurrent requests.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteMaxConnections = Flag.Integer("remoteMaxConnections")

  @Option(
    name = "remote_print_execution_messages",
    defaultValue = "failure",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Choose when to print remote execution messages. Valid values are `failure`, to print only on failures,
      `success` to print only on successes and `all` to print always.
    """,
    valueHelp = "failure, success or all",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remotePrintExecutionMessages = Flag.OneOf("remotePrintExecutionMessages")

  @Option(
    name = "remote_proxy",
    oldName = "remote_cache_proxy",
    help = """
      Connect to the remote cache through a proxy. Currently this flag can only be used to configure a Unix domain
      socket (unix:/path/to/socket).
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteProxy = Flag.Str("remoteProxy")

  @Option(
    name = "remote_result_cache_priority",
    defaultValue = "0",
    help = """
      The relative priority of remote actions to be stored in remote cache. The semantics of the particular
      priority values are server-dependent.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteResultCachePriority = Flag.Integer("remoteResultCachePriority")

  @Option(
    name = "remote_retries",
    oldName = "experimental_remote_retry_max_attempts",
    defaultValue = "5",
    help = "The maximum number of attempts to retry a transient error. If set to 0, retries are disabled.",
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteRetries = Flag.Integer("remoteRetries")

  @Option(
    name = "remote_retry_max_delay",
    defaultValue = "5s",
    help = """
      The maximum backoff delay between remote retry attempts. Following units can be used: Days (d), hours (h),
      minutes (m), seconds (s), and milliseconds (ms). If the unit is omitted, the value is interpreted as seconds.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteRetryMaxDelay = Flag.Duration("remoteRetryMaxDelay")

  @Option(
    name = "remote_timeout",
    defaultValue = "60s",
    help = """
      The maximum amount of time to wait for remote execution and cache calls. For the REST cache, this is both the
      connect and the read timeout. Following units can be used: Days (d), hours (h), minutes (m), seconds (s), and
      milliseconds (ms). If the unit is omitted, the value is interpreted as seconds.
    """,
    valueHelp = "An immutable length of time.",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteTimeout = Flag.Duration("remoteTimeout")

  @Option(
    name = "remote_upload_local_results",
    defaultValue = "true",
    help = """
      Whether to upload locally executed action results to the remote cache if the remote cache supports it and the
      user is authorized to do so.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteUploadLocalResults = Flag.Boolean("remoteUploadLocalResults")

  @Option(
    name = "remote_verify_downloads",
    defaultValue = "true",
    help = """
      If set to true, Bazel will compute the hash sum of all remote downloads and  discard the remotely cached
      values if they don't match the expected value.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val remoteVerifyDownloads = Flag.Boolean("remoteVerifyDownloads")

  @Option(
    name = "repo",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      Only fetches the specified repository, which can be either {@apparent_repo_name} or {@@canonical_repo_name}.
      Only works when --enable_bzlmod is on.
    """,
    valueHelp = "a string",
    commands = ["fetch", "vendor"],
  )
  @JvmField
  @Suppress("unused")
  val repo = Flag.Str("repo")

  @Option(
    name = "repo_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """
      Specifies additional environment variables to be available only for repository rules. Note that repository
      rules see the full environment anyway, but in this way configuration information can be passed to
      repositories through options without invalidating the action graph.
    """,
    valueHelp = "a 'name=value' assignment with an optional value part",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val repoEnv = Flag.Unknown("repoEnv")

  @Option(
    name = "repositories_without_autoloads",
    defaultValue = "",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      A list of additional repositories (beyond the hardcoded ones Bazel knows about) where autoloads are not to be
      added. This should typically contain repositories that are transitively depended on by a repository that may
      be loaded automatically (and which can therefore potentially create a cycle).
    """,
    valueHelp = "comma-separated set of options",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val repositoriesWithoutAutoloads = Flag.Unknown("repositoriesWithoutAutoloads")

  @Option(
    name = "repository_cache",
    oldName = "experimental_repository_cache",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      Specifies the cache location of the downloaded values obtained during the fetching of external repositories.
      An empty string as argument requests the cache to be disabled, otherwise the default of
      '<output_user_root>/cache/repos/v1' is used
    """,
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val repositoryCache = Flag.Path("repositoryCache")

  @Option(
    name = "repository_disable_download",
    oldName = "experimental_repository_disable_download",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      If set, downloading using ctx.download{,_and_extract} is not allowed during repository fetching. Note that
      network access is not completely disabled; ctx.execute could still run an arbitrary executable that accesses
      the Internet.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val repositoryDisableDownload = Flag.Boolean("repositoryDisableDownload")

  @Option(
    name = "restart_reason",
    defaultValue = "no_restart",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "The reason for the server restart.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val restartReason = Flag.Str("restartReason")

  @Option(
    name = "reuse_sandbox_directories",
    oldName = "experimental_reuse_sandbox_directories",
    defaultValue = "true",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """
      If set to true, directories used by sandboxed non-worker execution may be reused to avoid unnecessary setup
      costs.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val reuseSandboxDirectories = Flag.Boolean("reuseSandboxDirectories")

  @Option(
    name = "rewind_lost_inputs",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = "Whether to use action rewinding to recover from lost inputs.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val rewindLostInputs = Flag.Boolean("rewindLostInputs")

  @Option(
    name = "rule_classes",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Dump rule classes.",
    valueHelp = "a boolean",
    commands = ["dump"],
  )
  @JvmField
  @Suppress("unused")
  val ruleClasses = Flag.Boolean("ruleClasses")

  @Option(
    name = "rules",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Dump rules, including counts and memory usage (if memory is tracked).",
    valueHelp = "a boolean",
    commands = ["dump"],
  )
  @JvmField
  @Suppress("unused")
  val rules = Flag.Boolean("rules")

  @Option(
    name = "run",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If false, skip running the command line constructed for the built target. Note that this flag is ignored for
      all --script_path builds.
    """,
    valueHelp = "a boolean",
    commands = ["run"],
  )
  @JvmField
  @Suppress("unused")
  val run = Flag.Boolean("run")

  @Option(
    name = "run_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specifies the set of environment variables available to actions with target configuration. Variables can be
      either specified by name, in which case the value will be taken from the invocation environment, or by the
      name=value pair which sets the value independent of the invocation environment. This option can be used
      multiple times; for options given for the same variable, the latest wins, options for different variables
      accumulate.
    """,
    valueHelp = "a 'name=value' assignment with an optional value part",
    commands = ["run"],
  )
  @JvmField
  @Suppress("unused")
  val runEnv = Flag.Unknown("runEnv")

  @Option(
    name = "run_in_client",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      If true, the mobile-install deployer command will be sent to the bazel client for execution. Useful for
      configurations where the bazel client is on a different machine than the bazel server.
    """,
    valueHelp = "a boolean",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val runInClient = Flag.Boolean("runInClient")

  @Option(
    name = "run_under",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """
      Prefix to insert before the executables for the 'test' and 'run' commands. If the value is 'foo -bar', and
      the execution command line is 'test_binary -baz', then the final command line is 'foo -bar test_binary
      -baz'.This can also be a label to an executable target. Some examples are: 'valgrind', 'strace', 'strace -c',
      'valgrind --quiet --num-callers=20', '//package:target',  '//package:target --options'.
    """,
    valueHelp = "a prefix in front of command",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val runUnder = Flag.Unknown("runUnder")

  @Option(
    name = "run_validations",
    oldName = "experimental_run_validations",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Whether to run validation actions as part of the build. See
      https://bazel.build/extending/rules#validation_actions
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val runValidations = Flag.Boolean("runValidations")

  @Option(
    name = "runs_per_test",
    defaultValue = "1",
    allowMultiple = true,
    help = """
      Specifies number of times to run each test. If any of those attempts fail for any reason, the whole test is
      considered failed. Normally the value specified is just an integer. Example: --runs_per_test=3 will run all
      tests 3 times. Alternate syntax: regex_filter@runs_per_test. Where runs_per_test stands for an integer value
      and regex_filter stands for a list of include and exclude regular expression patterns (Also see
      --instrumentation_filter). Example: --runs_per_test=//foo/.*,-//foo/bar/.*@3 runs all tests in //foo/ except
      those under foo/bar three times. This option can be passed multiple times. The most recently passed argument
      that matches takes precedence. If nothing matches, the test is only run once.
    """,
    valueHelp = "a positive integer or test_regex@runs. This flag may be passed more than once",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val runsPerTest = Flag.Unknown("runsPerTest")

  @Option(
    name = "runs_per_test_detects_flakes",
    defaultValue = "false",
    help = """
      If true, any shard in which at least one run/attempt passes and at least one run/attempt fails gets a FLAKY
      status.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val runsPerTestDetectsFlakes = Flag.Boolean("runsPerTestDetectsFlakes")

  @Option(
    name = "sandbox_add_mount_pair",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = "Add additional path pair to mount in sandbox.",
    valueHelp = "a single path or a 'source:target' pair",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxAddMountPair = Flag.Unknown("sandboxAddMountPair")

  @Option(
    name = "sandbox_base",
    oldName = "experimental_sandbox_base",
    defaultValue = "",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """
      Lets the sandbox create its sandbox directories underneath this path. Specify a path on tmpfs (like /run/shm)
      to possibly improve performance a lot when your build / tests have many input files. Note: You need enough
      RAM and free space on the tmpfs to hold output and intermediate files generated by running actions.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxBase = Flag.Str("sandboxBase")

  @Option(
    name = "sandbox_block_path",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = "For sandboxed actions, disallow access to this path.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxBlockPath = Flag.Str("sandboxBlockPath")

  @Option(
    name = "sandbox_debug",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Enables debugging features for the sandboxing feature. This includes two things: first, the sandbox root
      contents are left untouched after a build; and second, prints extra debugging information on execution. This
      can help developers of Bazel or Starlark rules with debugging failures due to missing input files, etc.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxDebug = Flag.Boolean("sandboxDebug")

  @Option(
    name = "sandbox_default_allow_network",
    oldName = "experimental_sandbox_default_allow_network",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Allow network access by default for actions; this may not work with all sandboxing implementations.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxDefaultAllowNetwork = Flag.Boolean("sandboxDefaultAllowNetwork")

  @Option(
    name = "sandbox_explicit_pseudoterminal",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Explicitly enable the creation of pseudoterminals for sandboxed actions. Some linux distributions require
      setting the group id of the process to 'tty' inside the sandbox in order for pseudoterminals to function. If
      this is causing issues, this flag can be disabled to enable other groups to be used.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxExplicitPseudoterminal = Flag.Boolean("sandboxExplicitPseudoterminal")

  @Option(
    name = "sandbox_fake_hostname",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = "Change the current hostname to 'localhost' for sandboxed actions.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxFakeHostname = Flag.Boolean("sandboxFakeHostname")

  @Option(
    name = "sandbox_fake_username",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = "Change the current username to 'nobody' for sandboxed actions.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxFakeUsername = Flag.Boolean("sandboxFakeUsername")

  @Option(
    name = "sandbox_tmpfs_path",
    allowMultiple = true,
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """
      For sandboxed actions, mount an empty, writable directory at this absolute path (if supported by the
      sandboxing implementation, ignored otherwise).
    """,
    valueHelp = "an absolute path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxTmpfsPath = Flag.Unknown("sandboxTmpfsPath")

  @Option(
    name = "sandbox_writable_path",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      For sandboxed actions, make an existing directory writable in the sandbox (if supported by the sandboxing
      implementation, ignored otherwise).
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sandboxWritablePath = Flag.Str("sandboxWritablePath")

  @Option(
    name = "save_temps",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If set, temporary outputs from gcc will be saved.  These include .s files (assembler code), .i files
      (preprocessed C) and .ii files (preprocessed C++).
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val saveTemps = Flag.Boolean("saveTemps")

  @Option(
    name = "scl_config",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """
      Name of the scl config defined in PROJECT.scl. Note that this feature is still under development b/324119879.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val sclConfig = Flag.Str("sclConfig")

  @Option(
    name = "script_path",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION],
    help = """
      If set, write a shell script to the given file which invokes the target. If this option is set, the target is
      not run from %{product}. Use '%{product} run --script_path=foo //foo && ./foo' to invoke target '//foo' This
      differs from '%{product} run //foo' in that the %{product} lock is released and the executable is connected
      to the terminal's stdin.
    """,
    valueHelp = "a path",
    commands = ["run"],
  )
  @JvmField
  @Suppress("unused")
  val scriptPath = Flag.Path("scriptPath")

  @Option(
    name = "separate_aspect_deps",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "No-op",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val separateAspectDeps = Flag.Boolean("separateAspectDeps")

  @Option(
    name = "serialized_frontier_profile",
    defaultValue = "",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Dump a profile of serialized frontier bytes. Specifies the output path.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val serializedFrontierProfile = Flag.Str("serializedFrontierProfile")

  @Option(
    name = "server_javabase",
    defaultValue = "",
    help = "Path to the JVM used to execute Bazel itself.",
    valueHelp = "a string",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val serverJavabase = Flag.Str("serverJavabase")

  @Option(
    name = "server_jvm_out",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      The location to write the server's JVM's output. If unset then defaults to a location in output_base.
    """,
    valueHelp = "a path",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val serverJvmOut = Flag.Path("serverJvmOut")

  @Option(
    name = "share_native_deps",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If true, native libraries that contain identical functionality will be shared among different targets
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val shareNativeDeps = Flag.Boolean("shareNativeDeps")

  @Option(
    name = "shell_executable",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Absolute path to the shell executable for Bazel to use. If this is unset, but the BAZEL_SH environment
      variable is set on the first Bazel invocation (that starts up a Bazel server), Bazel uses that. If neither is
      set, Bazel uses a hard-coded default path depending on the operating system it runs on (Windows:
      c:/msys64/usr/bin/bash.exe, FreeBSD: /usr/local/bin/bash, all others: /bin/bash). Note that using a shell
      that is not compatible with bash may lead to build failures or runtime failures of the generated binaries.
    """,
    valueHelp = "a path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val shellExecutable = Flag.Path("shellExecutable")

  @Option(
    name = "short",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    expandsTo = ["--help_verbosity=short"],
    help = "Show only the names of the options, not their types or meanings.",
    valueHelp = "",
    commands = ["help"],
  )
  @JvmField
  @Suppress("unused")
  val short = Flag.Unknown("short")

  @Option(
    name = "show_config_fragments",
    defaultValue = "off",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Shows the configuration fragments required by a rule and its transitive dependencies. This can be useful for
      evaluating how much a configured target graph can be trimmed.
    """,
    valueHelp = "off, direct or transitive",
    commands = ["cquery"],
  )
  @JvmField
  @Suppress("unused")
  val showConfigFragments = Flag.OneOf("showConfigFragments")

  @Option(
    name = "show_loading_progress",
    defaultValue = "true",
    help = """If enabled, causes Bazel to print "Loading package:" messages.""",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val showLoadingProgress = Flag.Boolean("showLoadingProgress")

  @Option(
    name = "show_make_env",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.TERMINAL_OUTPUT],
    help = """Include the "Make" environment in the output.""",
    valueHelp = "a boolean",
    commands = ["info"],
  )
  @JvmField
  @Suppress("unused")
  val showMakeEnv = Flag.Boolean("showMakeEnv")

  @Option(
    name = "show_progress",
    defaultValue = "true",
    help = "Display progress messages during a build.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val showProgress = Flag.Boolean("showProgress")

  @Option(
    name = "show_progress_rate_limit",
    defaultValue = "0.2",
    help = "Minimum number of seconds between progress messages in the output.",
    valueHelp = "a double",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val showProgressRateLimit = Flag.Double("showProgressRateLimit")

  @Option(
    name = "show_result",
    defaultValue = "1",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Show the results of the build.  For each target, state whether or not it was brought up-to-date, and if so, a
      list of output files that were built.  The printed files are convenient strings for copy+pasting to the
      shell, to execute them.
      This option requires an integer argument, which is the threshold number of targets above which result
      information is not printed. Thus zero causes suppression of the message and MAX_INT causes printing of the
      result to occur always. The default is one.
      If nothing was built for a target its results may be omitted to keep the output under the threshold.
    """,
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val showResult = Flag.Integer("showResult")

  @Option(
    name = "show_timestamps",
    defaultValue = "false",
    help = "Include timestamps in messages",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val showTimestamps = Flag.Boolean("showTimestamps")

  @Option(
    name = "shutdown_on_low_sys_mem",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      If max_idle_secs is set and the build server has been idle for a while, shut down the server when the system
      is low on free RAM. Linux only.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val shutdownOnLowSysMem = Flag.Boolean("shutdownOnLowSysMem")

  @Option(
    name = "skip_incompatible_explicit_targets",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Skip incompatible targets that are explicitly listed on the command line. By default, building such targets
      results in an error but they are silently skipped when this option is enabled. See:
      https://bazel.build/extending/platforms#skipping-incompatible-targets
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val skipIncompatibleExplicitTargets = Flag.Boolean("skipIncompatibleExplicitTargets")

  @Option(
    name = "skyframe",
    defaultValue = "off",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Dump the Skyframe graph.",
    valueHelp = "off, summary, count, value, deps, rdeps, function_graph, working_set or working_set_frontier_deps",
    commands = ["dump"],
  )
  @JvmField
  @Suppress("unused")
  val skyframe = Flag.OneOf("skyframe")

  @Option(
    name = "skyframe_high_water_mark_full_gc_drops_per_invocation",
    defaultValue = "10",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Flag for advanced configuration of Bazel's internal Skyframe engine. If Bazel detects its retained heap
      percentage usage exceeds the threshold set by --skyframe_high_water_mark_threshold, when a full GC event
      occurs, it will drop unnecessary temporary Skyframe state, up to this many times per invocation. Defaults to
      10. Zero means that full GC events will never trigger drops. If the limit is reached, Skyframe state will no
      longer be dropped when a full GC event occurs and that retained heap percentage threshold is exceeded.
    """,
    valueHelp = "an integer, >= 0",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val skyframeHighWaterMarkFullGcDropsPerInvocation = Flag.Unknown("skyframeHighWaterMarkFullGcDropsPerInvocation")

  @Option(
    name = "skyframe_high_water_mark_minor_gc_drops_per_invocation",
    defaultValue = "10",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Flag for advanced configuration of Bazel's internal Skyframe engine. If Bazel detects its retained heap
      percentage usage exceeds the threshold set by --skyframe_high_water_mark_threshold, when a minor GC event
      occurs, it will drop unnecessary temporary Skyframe state, up to this many times per invocation. Defaults to
      10. Zero means that minor GC events will never trigger drops. If the limit is reached, Skyframe state will no
      longer be dropped when a minor GC event occurs and that retained heap percentage threshold is exceeded.
    """,
    valueHelp = "an integer, >= 0",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val skyframeHighWaterMarkMinorGcDropsPerInvocation = Flag.Unknown("skyframeHighWaterMarkMinorGcDropsPerInvocation")

  @Option(
    name = "skyframe_high_water_mark_threshold",
    defaultValue = "85",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Flag for advanced configuration of Bazel's internal Skyframe engine. If Bazel detects its retained heap
      percentage usage is at least this threshold, it will drop unnecessary temporary Skyframe state. Tweaking this
      may let you mitigate wall time impact of GC thrashing, when the GC thrashing is (i) caused by the memory
      usage of this temporary state and (ii) more costly than reconstituting the state when it is needed.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val skyframeHighWaterMarkThreshold = Flag.Integer("skyframeHighWaterMarkThreshold")

  @Option(
    name = "skyframe_state",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Without performing extra analysis, dump the current Action Graph from Skyframe. Note: Specifying a target
      with --skyframe_state is currently not supported. This flag is only available with --output=proto or
      --output=textproto.
    """,
    valueHelp = "a boolean",
    commands = ["aquery"],
  )
  @JvmField
  @Suppress("unused")
  val skyframeState = Flag.Boolean("skyframeState")

  @Option(
    name = "skykey_filter",
    defaultValue = ".*",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Regex filter of SkyKey names to output. Only used with --skyframe=deps, rdeps, function_graph.",
    valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
    commands = ["dump"],
  )
  @JvmField
  @Suppress("unused")
  val skykeyFilter = Flag.Unknown("skykeyFilter")

  @Option(
    name = "skylark_memory",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """
      Dumps a pprof-compatible memory profile to the specified path. To learn more please see
      https://github.com/google/pprof.
    """,
    valueHelp = "a string",
    commands = ["dump"],
  )
  @JvmField
  @Suppress("unused")
  val skylarkMemory = Flag.Str("skylarkMemory")

  @Option(
    name = "slim_profile",
    oldName = "experimental_slim_json_profile",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Slims down the size of the JSON profile by merging events if the profile gets  too large.",
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val slimProfile = Flag.Boolean("slimProfile")

  @Option(
    name = "spawn_strategy",
    defaultValue = "",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Specify how spawn actions are executed by default. Accepts a comma-separated list of strategies from highest
      to lowest priority. For each action Bazel picks the strategy with the highest priority that can execute the
      action. The default value is "remote,worker,sandboxed,local". See
      https://blog.bazel.build/2019/06/19/list-strategy.html for details.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val spawnStrategy = Flag.Unknown("spawnStrategy")

  @Option(
    name = "split_apks",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Whether to use split apks to install and update the application on the device. Works only with devices with
      Marshmallow or later
    """,
    valueHelp = "a boolean",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val splitApks = Flag.Boolean("splitApks")

  @Option(
    name = "split_bytecode_optimization_pass",
    defaultValue = "false",
    help = "Do not use.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val splitBytecodeOptimizationPass = Flag.Boolean("splitBytecodeOptimizationPass")

  @Option(
    name = "stamp",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Stamp binaries with the date, username, hostname, workspace information, etc.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val stamp = Flag.Boolean("stamp")

  @Option(
    name = "starlark:expr",
    defaultValue = "",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      A Starlark expression to format each configured target in cquery's --output=starlark mode. The configured
      target is bound to 'target'. If neither --starlark:expr nor --starlark:file is specified, this option will
      default to 'str(target.label)'. It is an error to specify both --starlark:expr and --starlark:file.
    """,
    valueHelp = "a string",
    commands = ["cquery"],
  )
  @JvmField
  @Suppress("unused")
  val starlark_expr = Flag.Str("starlark_expr")

  @Option(
    name = "starlark:file",
    defaultValue = "",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      The name of a file that defines a Starlark function called 'format', of one argument, that is applied to each
      configured target to format it as a string. It is an error to specify both --starlark:expr and
      --starlark:file. See help for --output=starlark for additional detail.
    """,
    valueHelp = "a string",
    commands = ["cquery"],
  )
  @JvmField
  @Suppress("unused")
  val starlark_file = Flag.Str("starlark_file")

  @Option(
    name = "starlark_cpu_profile",
    defaultValue = "",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = "Writes into the specified file a pprof profile of CPU usage by all Starlark threads.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val starlarkCpuProfile = Flag.Str("starlarkCpuProfile")

  @Option(
    name = "start",
    defaultValue = "NO",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      How the app should be started after installing it. Set to WARM to preserve and restore application state on
      incremental installs.
    """,
    valueHelp = "no, cold, warm or debug",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val start = Flag.OneOf("start")

  @Option(
    name = "start_app",
    effectTags = [OptionEffectTag.EXECUTION],
    expandsTo = ["--start=COLD"],
    help = "Whether to start the app after installing it.",
    valueHelp = "",
    commands = ["mobile-install"],
  )
  @JvmField
  @Suppress("unused")
  val startApp = Flag.Unknown("startApp")

  @Option(
    name = "start_end_lib",
    defaultValue = "true",
    effectTags = [
      OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.CHANGES_INPUTS,
      OptionEffectTag.AFFECTS_OUTPUTS,
    ],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "Use the --start-lib/--end-lib ld options if supported by the toolchain.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val startEndLib = Flag.Boolean("startEndLib")

  @Option(
    name = "startup_time",
    defaultValue = "0",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "The time in ms the launcher spends before sending the request to the bazel server.",
    valueHelp = "a long integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val startupTime = Flag.Unknown("startupTime")

  @Option(
    name = "strategy",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Specify how to distribute compilation of other spawn actions. Accepts a comma-separated list of strategies
      from highest to lowest priority. For each action Bazel picks the strategy with the highest priority that can
      execute the action. The default value is "remote,worker,sandboxed,local". This flag overrides the values set
      by --spawn_strategy (and --genrule_strategy if used with mnemonic Genrule). See
      https://blog.bazel.build/2019/06/19/list-strategy.html for details.
    """,
    valueHelp = "a '[name=]value1[,..,valueN]' assignment",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val strategy = Flag.Unknown("strategy")

  @Option(
    name = "strategy_regexp",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      Override which spawn strategy should be used to execute spawn actions that have descriptions matching a
      certain regex_filter. See --per_file_copt for details onregex_filter matching. The last regex_filter that
      matches the description is used. This option overrides other flags for specifying strategy. Example:
      --strategy_regexp=//foo.*\.cc,-//foo/bar=local means to run actions using local strategy if their
      descriptions match //foo.*.cc but not //foo/bar. Example: --strategy_regexp='Compiling.*/bar=local
      --strategy_regexp=Compiling=sandboxed will run 'Compiling //foo/bar/baz' with the 'local' strategy, but
      reversing the order would run it with 'sandboxed'.
    """,
    valueHelp = "a '<RegexFilter>=value[,value]' assignment",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val strategyRegexp = Flag.Unknown("strategyRegexp")

  @Option(
    name = "strict_deps_java_protos",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = "No-op, kept only for backwards compatibility",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val strictDepsJavaProtos = Flag.Boolean("strictDepsJavaProtos")

  @Option(
    name = "strict_filesets",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = "If this option is enabled, filesets crossing package boundaries are reported as errors.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val strictFilesets = Flag.Boolean("strictFilesets")

  @Option(
    name = "strict_proto_deps",
    defaultValue = "error",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      Unless OFF, checks that a proto_library target explicitly declares all directly used targets as dependencies.
    """,
    valueHelp = "off, warn, error, strict or default",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val strictProtoDeps = Flag.OneOf("strictProtoDeps")

  @Option(
    name = "strict_public_imports",
    defaultValue = "off",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """
      Unless OFF, checks that a proto_library target explicitly declares all targets used in 'import public' as
      exported.
    """,
    valueHelp = "off, warn, error, strict or default",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val strictPublicImports = Flag.OneOf("strictPublicImports")

  @Option(
    name = "strict_system_includes",
    defaultValue = "false",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      If true, headers found through system include paths (-isystem) are also required to be declared.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val strictSystemIncludes = Flag.Boolean("strictSystemIncludes")

  @Option(
    name = "strict_test_suite",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """
      If true, the tests() expression gives an error if it encounters a test_suite containing non-test targets.
    """,
    valueHelp = "a boolean",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val strictTestSuite = Flag.Boolean("strictTestSuite")

  @Option(
    name = "strip",
    defaultValue = "sometimes",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Specifies whether to strip binaries and shared libraries  (using "-Wl,--strip-debug").  The default value of
      'sometimes' means strip iff --compilation_mode=fastbuild.
    """,
    valueHelp = "always, sometimes or never",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val strip = Flag.OneOf("strip")

  @Option(
    name = "stripopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = "Additional options to pass to strip when generating a '<name>.stripped' binary.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val stripopt = Flag.Str("stripopt")

  @Option(
    name = "subcommands",
    abbrev = 's',
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Display the subcommands executed during a build. Related flags: --execution_log_json_file,
      --execution_log_binary_file (for logging subcommands to a file in a tool-friendly format).
    """,
    valueHelp = "true, pretty_print or false",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val subcommands = Flag.OneOf("subcommands")

  @Option(
    name = "symlink_prefix",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      The prefix that is prepended to any of the convenience symlinks that are created after a build. If omitted,
      the default value is the name of the build tool followed by a hyphen. If '/' is passed, then no symlinks are
      created and no warning is emitted. Warning: the special functionality for '/' will be deprecated soon; use
      --experimental_convenience_symlinks=ignore instead.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val symlinkPrefix = Flag.Str("symlinkPrefix")

  @Option(
    name = "system_rc",
    defaultValue = "true",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = "Whether or not to look for the system-wide bazelrc.",
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val systemRc = Flag.Boolean("systemRc")

  @Option(
    name = "target_environment",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      Declares this build's target environment. Must be a label reference to an "environment" rule. If specified,
      all top-level targets must be compatible with this environment.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val targetEnvironment = Flag.Label("targetEnvironment")

  @Option(
    name = "target_pattern_file",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """
      If set, build will read patterns from the file named here, rather than on the command line. It is an error to
      specify a file here as well as command-line patterns.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val targetPatternFile = Flag.Str("targetPatternFile")

  @Option(
    name = "target_platform_fallback",
    defaultValue = "",
    effectTags = [OptionEffectTag.NO_OP],
    help = "This option is deprecated and has no effect.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val targetPlatformFallback = Flag.Str("targetPlatformFallback")

  @Option(
    name = "terminal_columns",
    defaultValue = "80",
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = "A system-generated parameter which specifies the terminal width in columns.",
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val terminalColumns = Flag.Integer("terminalColumns")

  @Option(
    name = "test_arg",
    allowMultiple = true,
    help = """
      Specifies additional options and arguments that should be passed to the test executable. Can be used multiple
      times to specify several arguments. If multiple tests are executed, each of them will receive identical
      arguments. Used only by the 'bazel test' command.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testArg = Flag.Str("testArg")

  @Option(
    name = "test_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.TEST_RUNNER],
    help = """
      Specifies additional environment variables to be injected into the test runner environment. Variables can be
      either specified by name, in which case its value will be read from the Bazel client environment, or by the
      name=value pair. This option can be used multiple times to specify several variables. Used only by the 'bazel
      test' command.
    """,
    valueHelp = "a 'name=value' assignment with an optional value part",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testEnv = Flag.Unknown("testEnv")

  @Option(
    name = "test_filter",
    help = """
      Specifies a filter to forward to the test framework.  Used to limit the tests run. Note that this does not
      affect which targets are built.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testFilter = Flag.Str("testFilter")

  @Option(
    name = "test_keep_going",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      When disabled, any non-passing test will cause the entire build to stop. By default all tests are run, even
      if some do not pass.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testKeepGoing = Flag.Boolean("testKeepGoing")

  @Option(
    name = "test_lang_filters",
    defaultValue = "",
    help = """
      Specifies a comma-separated list of test languages. Each language can be optionally preceded with '-' to
      specify excluded languages. Only those test targets will be found that are written in the specified
      languages. The name used for each language should be the same as the language prefix in the *_test rule, e.g.
      one of 'cc', 'java', 'py', etc. This option affects --build_tests_only behavior and the test command.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testLangFilters = Flag.Unknown("testLangFilters")

  @Option(
    name = "test_output",
    defaultValue = "summary",
    effectTags = [OptionEffectTag.TEST_RUNNER, OptionEffectTag.TERMINAL_OUTPUT, OptionEffectTag.EXECUTION],
    help = """
      Specifies desired output mode. Valid values are 'summary' to output only test status summary, 'errors' to
      also print test logs for failed tests, 'all' to print logs for all tests and 'streamed' to output logs for
      all tests in real time (this will force tests to be executed locally one at a time regardless of
      --test_strategy value).
    """,
    valueHelp = "summary, errors, all or streamed",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testOutput = Flag.OneOf("testOutput")

  @Option(
    name = "test_result_expiration",
    defaultValue = "-1",
    help = "This option is deprecated and has no effect.",
    valueHelp = "an integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testResultExpiration = Flag.Integer("testResultExpiration")

  @Option(
    name = "test_runner_fail_fast",
    defaultValue = "false",
    help = """
      Forwards fail fast option to the test runner. The test runner should stop execution upon first failure.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testRunnerFailFast = Flag.Boolean("testRunnerFailFast")

  @Option(
    name = "test_sharding_strategy",
    defaultValue = "explicit",
    help = """
      Specify strategy for test sharding: 'explicit' to only use sharding if the 'shard_count' BUILD attribute is
      present. 'disabled' to never use test sharding. 'forced=k' to enforce 'k' shards for testing regardless of
      the 'shard_count' BUILD attribute.
    """,
    valueHelp = "explicit, disabled or forced=k where k is the number of shards to enforce",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testShardingStrategy = Flag.Unknown("testShardingStrategy")

  @Option(
    name = "test_size_filters",
    defaultValue = "",
    help = """
      Specifies a comma-separated list of test sizes. Each size can be optionally preceded with '-' to specify
      excluded sizes. Only those test targets will be found that contain at least one included size and do not
      contain any excluded sizes. This option affects --build_tests_only behavior and the test command.
    """,
    valueHelp = "comma-separated list of values: small, medium, large, or enormous",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testSizeFilters = Flag.Unknown("testSizeFilters")

  @Option(
    name = "test_strategy",
    defaultValue = "",
    effectTags = [OptionEffectTag.EXECUTION],
    help = "Specifies which strategy to use when running tests.",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testStrategy = Flag.Str("testStrategy")

  @Option(
    name = "test_summary",
    defaultValue = "short",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Specifies the desired format of the test summary. Valid values are 'short' to print information only about
      tests executed, 'terse', to print information only about unsuccessful tests that were run, 'detailed' to
      print detailed information about failed test cases, 'testcase' to print summary in test case resolution, do
      not print detailed information about failed test cases and 'none' to omit the summary.
    """,
    valueHelp = "short, terse, detailed, none or testcase",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testSummary = Flag.OneOf("testSummary")

  @Option(
    name = "test_tag_filters",
    defaultValue = "",
    help = """
      Specifies a comma-separated list of test tags. Each tag can be optionally preceded with '-' to specify
      excluded tags. Only those test targets will be found that contain at least one included tag and do not
      contain any excluded tags. This option affects --build_tests_only behavior and the test command.
    """,
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testTagFilters = Flag.Unknown("testTagFilters")

  @Option(
    name = "test_timeout",
    defaultValue = "-1",
    help = """
      Override the default test timeout values for test timeouts (in secs). If a single positive integer value is
      specified it will override all categories.  If 4 comma-separated integers are specified, they will override
      the timeouts for short, moderate, long and eternal (in that order). In either form, a value of -1 tells blaze
      to use its default timeouts for that category.
    """,
    valueHelp = "a single integer or comma-separated list of 4 integers",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testTimeout = Flag.Unknown("testTimeout")

  @Option(
    name = "test_timeout_filters",
    defaultValue = "",
    help = """
      Specifies a comma-separated list of test timeouts. Each timeout can be optionally preceded with '-' to
      specify excluded timeouts. Only those test targets will be found that contain at least one included timeout
      and do not contain any excluded timeouts. This option affects --build_tests_only behavior and the test
      command.
    """,
    valueHelp = "comma-separated list of values: short, moderate, long, or eternal",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testTimeoutFilters = Flag.Unknown("testTimeoutFilters")

  @Option(
    name = "test_tmpdir",
    help = "Specifies the base temporary directory for 'bazel test' to use.",
    valueHelp = "a path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val testTmpdir = Flag.Path("testTmpdir")

  @Option(
    name = "test_verbose_timeout_warnings",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      If true, print additional warnings when the actual test execution time does not match the timeout defined by
      the test (whether implied or explicit).
    """,
    valueHelp = "a boolean",
    commands = ["coverage", "cquery", "fetch", "test", "vendor"],
  )
  @JvmField
  @Suppress("unused")
  val testVerboseTimeoutWarnings = Flag.Boolean("testVerboseTimeoutWarnings")

  @Option(
    name = "tls_authority_override",
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = """
      TESTING ONLY! Can be used with a self-signed certificate to consider the specified value a valid TLS
      authority.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val tlsAuthorityOverride = Flag.Str("tlsAuthorityOverride")

  @Option(
    name = "tls_certificate",
    help = "Specify a path to a TLS certificate that is trusted to sign server certificates.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val tlsCertificate = Flag.Str("tlsCertificate")

  @Option(
    name = "tls_client_certificate",
    help = """
      Specify the TLS client certificate to use; you also need to provide a client key to enable client
      authentication.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val tlsClientCertificate = Flag.Str("tlsClientCertificate")

  @Option(
    name = "tls_client_key",
    help = """
      Specify the TLS client key to use; you also need to provide a client certificate to enable client
      authentication.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val tlsClientKey = Flag.Str("tlsClientKey")

  @Option(
    name = "tool_deps",
    oldName = "host_deps",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """
      Query: If disabled, dependencies on 'exec configuration' will not be included in the dependency graph over
      which the query operates. An 'exec configuration' dependency edge, such as the one from any 'proto_library'
      rule to the Protocol Compiler, usually points to a tool executed during the build rather than a part of the
      same 'target' program.
      Cquery: If disabled, filters out all configured targets which cross an execution transition from the
      top-level target that discovered this configured target. That means if the top-level target is in the target
      configuration, only configured targets also in the target configuration will be returned. If the top-level
      target is in the exec configuration, only exec configured targets will be returned. This option will NOT
      exclude resolved toolchains.
    """,
    valueHelp = "a boolean",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val toolDeps = Flag.Boolean("toolDeps")

  @Option(
    name = "tool_java_language_version",
    defaultValue = "",
    help = "The Java language version used to execute the tools that are needed during a build",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val toolJavaLanguageVersion = Flag.Str("toolJavaLanguageVersion")

  @Option(
    name = "tool_java_runtime_version",
    defaultValue = "remotejdk_11",
    help = "The Java runtime version used to execute tools during the build",
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val toolJavaRuntimeVersion = Flag.Str("toolJavaRuntimeVersion")

  @Option(
    name = "tool_tag",
    defaultValue = "",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    help = "A tool name to attribute this Bazel invocation to.",
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val toolTag = Flag.Str("toolTag")

  @Option(
    name = "toolchain_resolution_debug",
    defaultValue = "-.*",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Print debug information during toolchain resolution. The flag takes a regex, which is checked against
      toolchain types and specific targets to see which to debug. Multiple regexes may be  separated by commas, and
      then each regex is checked separately. Note: The output of this flag is very complex and will likely only be
      useful to experts in toolchain resolution.
    """,
    valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val toolchainResolutionDebug = Flag.Unknown("toolchainResolutionDebug")

  @Option(
    name = "track_incremental_state",
    oldName = "keep_incrementality_data",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      If false, Blaze will not persist data that allows for invalidation and re-evaluation on incremental builds in
      order to save memory on this build. Subsequent builds will not have any incrementality with respect to this
      one. Usually you will want to specify --batch when setting this to false.
    """,
    valueHelp = "a boolean",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val trackIncrementalState = Flag.Boolean("trackIncrementalState")

  @Option(
    name = "transitions",
    defaultValue = "none",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "The format in which cquery will print transition information.",
    valueHelp = "full, lite or none",
    commands = ["cquery"],
  )
  @JvmField
  @Suppress("unused")
  val transitions = Flag.OneOf("transitions")

  @Option(
    name = "trim_test_configuration",
    defaultValue = "true",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      When enabled, test-related options will be cleared below the top level of the build. When this flag is
      active, tests cannot be built as dependencies of non-test rules, but changes to test-related options will not
      cause non-test rules to be re-analyzed.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val trimTestConfiguration = Flag.Boolean("trimTestConfiguration")

  @Option(
    name = "tvos_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Comma-separated list of architectures for which to build Apple tvOS binaries.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val tvosCpus = Flag.Unknown("tvosCpus")

  @Option(
    name = "tvos_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      Minimum compatible tvOS version for target simulators and devices. If unspecified, uses 'tvos_sdk_version'.
    """,
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val tvosMinimumOs = Flag.Unknown("tvosMinimumOs")

  @Option(
    name = "tvos_sdk_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      Specifies the version of the tvOS SDK to use to build tvOS applications. If unspecified, uses the default
      tvOS SDK version from 'xcode_version'.
    """,
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val tvosSdkVersion = Flag.Unknown("tvosSdkVersion")

  @Option(
    name = "ui_actions_shown",
    oldName = "experimental_ui_actions_shown",
    defaultValue = "8",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Number of concurrent actions shown in the detailed progress bar; each action is shown on a separate line. The
      progress bar always shows at least one one, all numbers less than 1 are mapped to 1.
    """,
    valueHelp = "an integer",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val uiActionsShown = Flag.Integer("uiActionsShown")

  @Option(
    name = "ui_event_filters",
    allowMultiple = true,
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      Specifies which events to show in the UI. It is possible to add or remove events to the default ones using
      leading +/-, or override the default set completely with direct assignment. The set of supported event kinds
      include INFO, DEBUG, ERROR and more.
    """,
    valueHelp = "Convert list of comma separated event kind to list of filters",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val uiEventFilters = Flag.Unknown("uiEventFilters")

  @Option(
    name = "unconditional_warning",
    allowMultiple = true,
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      A warning that will unconditionally get printed with build warnings and errors. This is useful to deprecate
      bazelrc files or --config definitions. If the intent is to effectively deprecate some flag or combination of
      flags, this is NOT sufficient. The flag or flags should use the deprecationWarning field in the option
      definition, or the bad combination should be checked for programmatically.
    """,
    valueHelp = "a string",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val unconditionalWarning = Flag.Str("unconditionalWarning")

  @Option(
    name = "universe_scope",
    defaultValue = "",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      A comma-separated set of target patterns (additive and subtractive). The query may be performed in the
      universe defined by the transitive closure of the specified targets. This option is used for the query and
      cquery commands.
      For cquery, the input to this option is the targets all answers are built under and so this option may affect
      configurations and transitions. If this option is not specified, the top-level targets are assumed to be the
      targets parsed from the query expression. Note: For cquery, not specifying this option may cause the build to
      break if targets parsed from the query expression are not buildable with top-level options.
    """,
    valueHelp = "comma-separated list of options",
    commands = ["aquery", "cquery", "query"],
  )
  @JvmField
  @Suppress("unused")
  val universeScope = Flag.Unknown("universeScope")

  @Option(
    name = "unix_digest_hash_attribute_name",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      The name of an extended attribute that can be placed on files to store a precomputed copy of the file's hash,
      corresponding with --digest_function. This option can be used to reduce disk I/O and CPU load caused by hash
      computation. This extended attribute is checked on all source files and output files, meaning that it causes
      a significant number of invocations of the getxattr() system call.
    """,
    valueHelp = "a string",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val unixDigestHashAttributeName = Flag.Str("unixDigestHashAttributeName")

  @Option(
    name = "unlimit_coredumps",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      Raises the soft coredump limit to the hard limit to make coredumps of the server (including the JVM) and the
      client possible under common conditions. Stick this flag in your bazelrc once and forget about it so that you
      get coredumps when you actually encounter a condition that triggers them.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val unlimitCoredumps = Flag.Boolean("unlimitCoredumps")

  @Option(
    name = "use_action_cache",
    defaultValue = "true",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = "Whether to use the action cache",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val useActionCache = Flag.Boolean("useActionCache")

  @Option(
    name = "use_ijars",
    defaultValue = "true",
    help = """
      If enabled, this option causes Java compilation to use interface jars. This will result in faster incremental
      compilation, but error messages can be different.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val useIjars = Flag.Boolean("useIjars")

  @Option(
    name = "use_target_platform_for_tests",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If true, then Bazel will use the target platform for running tests rather than the test exec group.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val useTargetPlatformForTests = Flag.Boolean("useTargetPlatformForTests")

  @Option(
    name = "use_top_level_targets_for_symlinks",
    defaultValue = "true",
    effectTags = [OptionEffectTag.NO_OP],
    help = "Deprecated. No-op.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val useTopLevelTargetsForSymlinks = Flag.Boolean("useTopLevelTargetsForSymlinks")

  @Option(
    name = "vendor_dir",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      Specifies the directory that should hold the external repositories in vendor mode, whether for the purpose of
      fetching them into it or using them while building. The path can be specified as either an absolute path or a
      path relative to the workspace directory.
    """,
    valueHelp = "a path",
    commands = [
      "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
      "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
      "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val vendorDir = Flag.Path("vendorDir")

  @Option(
    name = "verbose",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      The queries will also display the reason why modules were resolved to their current version (if changed).
      Defaults to true only for the explain query.
    """,
    valueHelp = "a boolean",
    commands = ["mod"],
  )
  @JvmField
  @Suppress("unused")
  val verbose = Flag.Boolean("verbose")

  @Option(
    name = "verbose_explanations",
    defaultValue = "false",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Increases the verbosity of the explanations issued if --explain is enabled. Has no effect if --explain is not
      enabled.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val verboseExplanations = Flag.Boolean("verboseExplanations")

  @Option(
    name = "verbose_failures",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = "If a command fails, print out the full command line.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val verboseFailures = Flag.Boolean("verboseFailures")

  @Option(
    name = "verbose_test_summary",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = "If true, print additional information (timing, number of failed runs, etc) in the test summary.",
    valueHelp = "a boolean",
    commands = ["coverage", "cquery", "fetch", "test", "vendor"],
  )
  @JvmField
  @Suppress("unused")
  val verboseTestSummary = Flag.Boolean("verboseTestSummary")

  @Option(
    name = "version_window_for_dirty_node_gc",
    defaultValue = "0",
    help = """
      Nodes that have been dirty for more than this many versions will be deleted from the graph upon the next
      update. Values must be non-negative long integers, or -1 indicating the maximum possible window.
    """,
    valueHelp = "a long integer",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val versionWindowForDirtyNodeGc = Flag.Unknown("versionWindowForDirtyNodeGc")

  @Option(
    name = "visionos_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Comma-separated list of architectures for which to build Apple visionOS binaries.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val visionosCpus = Flag.Unknown("visionosCpus")

  @Option(
    name = "watchfs",
    defaultValue = "false",
    metadataTags = [OptionMetadataTag.DEPRECATED],
    help = """
      If true, %{product} tries to use the operating system's file watch service for local changes instead of
      scanning every file for a change.
    """,
    valueHelp = "a boolean",
    commands = [
      "startup", "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config",
      "coverage", "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod",
      "print_action", "query", "run", "shutdown", "sync", "test", "vendor", "version",
    ],
  )
  @JvmField
  @Suppress("unused")
  val watchfs = Flag.Boolean("watchfs")

  @Option(
    name = "watchos_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = "Comma-separated list of architectures for which to build Apple watchOS binaries.",
    valueHelp = "comma-separated list of options",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val watchosCpus = Flag.Unknown("watchosCpus")

  @Option(
    name = "watchos_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      Minimum compatible watchOS version for target simulators and devices. If unspecified, uses
      'watchos_sdk_version'.
    """,
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val watchosMinimumOs = Flag.Unknown("watchosMinimumOs")

  @Option(
    name = "watchos_sdk_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      Specifies the version of the watchOS SDK to use to build watchOS applications. If unspecified, uses the
      default watchOS SDK version from 'xcode_version'.
    """,
    valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val watchosSdkVersion = Flag.Unknown("watchosSdkVersion")

  @Option(
    name = "windows_enable_symlinks",
    defaultValue = "false",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """
      If true, real symbolic links will be created on Windows instead of file copying. Requires Windows developer
      mode to be enabled and Windows 10 version 1703 or greater.
    """,
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val windowsEnableSymlinks = Flag.Boolean("windowsEnableSymlinks")

  @Option(
    name = "worker_extra_flag",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      Extra command-flags that will be passed to worker processes in addition to --persistent_worker, keyed by
      mnemonic (e.g. --worker_extra_flag=Javac=--debug.
    """,
    valueHelp = "a 'name=value' assignment",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val workerExtraFlag = Flag.Unknown("workerExtraFlag")

  @Option(
    name = "worker_max_instances",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      How many instances of each kind of persistent worker may be launched if you use the 'worker' strategy. May be
      specified as [name=value] to give a different value per mnemonic. The limit is based on worker keys, which
      are differentiated based on mnemonic, but also on startup flags and environment, so there can in some cases
      be more workers per mnemonic than this flag specifies. Takes an integer, or a keyword ("auto", "HOST_CPUS",
      "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". 'auto' calculates
      a reasonable default based on machine capacity. "=value" sets a default for unspecified mnemonics.
    """,
    valueHelp = """
      [name=]value, where value is an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally
      followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val workerMaxInstances = Flag.Unknown("workerMaxInstances")

  @Option(
    name = "worker_max_multiplex_instances",
    oldName = "experimental_worker_max_multiplex_instances",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """
      How many WorkRequests a multiplex worker process may receive in parallel if you use the 'worker' strategy
      with --worker_multiplex. May be specified as [name=value] to give a different value per mnemonic. The limit
      is based on worker keys, which are differentiated based on mnemonic, but also on startup flags and
      environment, so there can in some cases be more workers per mnemonic than this flag specifies. Takes an
      integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>)
      eg. "auto", "HOST_CPUS*.5". 'auto' calculates a reasonable default based on machine capacity. "=value" sets a
      default for unspecified mnemonics.
    """,
    valueHelp = """
      [name=]value, where value is an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally
      followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
    """,
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val workerMaxMultiplexInstances = Flag.Unknown("workerMaxMultiplexInstances")

  @Option(
    name = "worker_multiplex",
    oldName = "experimental_worker_multiplex",
    defaultValue = "true",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = "If enabled, workers will use multiplexing if they support it. ",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val workerMultiplex = Flag.Boolean("workerMultiplex")

  @Option(
    name = "worker_quit_after_build",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = "If enabled, all workers quit after a build is done.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val workerQuitAfterBuild = Flag.Boolean("workerQuitAfterBuild")

  @Option(
    name = "worker_sandboxing",
    defaultValue = "false",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """
      If enabled, singleplex workers will run in a sandboxed environment. Singleplex workers are always sandboxed
      when running under the dynamic execution strategy, irrespective of this flag.
    """,
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val workerSandboxing = Flag.Boolean("workerSandboxing")

  @Option(
    name = "worker_verbose",
    defaultValue = "false",
    help = "If enabled, prints verbose messages when workers are started, shutdown, ...",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val workerVerbose = Flag.Boolean("workerVerbose")

  @Option(
    name = "workspace_directory",
    defaultValue = "",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.HIDDEN],
    help = """
      The root of the workspace, that is, the directory that Bazel uses as the root of the build. This flag is only
      to be set by the bazel client.
    """,
    valueHelp = "a path",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val workspaceDirectory = Flag.Path("workspaceDirectory")

  @Option(
    name = "workspace_rc",
    defaultValue = "true",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = "Whether or not to look for the workspace bazelrc file at ${'$'}workspace/.bazelrc",
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val workspaceRc = Flag.Boolean("workspaceRc")

  @Option(
    name = "workspace_status_command",
    defaultValue = "",
    help = """
      A command invoked at the beginning of the build to provide status information about the workspace in the form
      of key/value pairs.  See the User's Manual for the full specification. Also see
      tools/buildstamp/get_workspace_status for an example.
    """,
    valueHelp = "a path",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val workspaceStatusCommand = Flag.Path("workspaceStatusCommand")

  @Option(
    name = "write_command_log",
    defaultValue = "true",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = "Whether or not to write the command.log file",
    valueHelp = "a boolean",
    commands = ["startup"],
  )
  @JvmField
  @Suppress("unused")
  val writeCommandLog = Flag.Boolean("writeCommandLog")

  @Option(
    name = "xbinary_fdo",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """
      Use XbinaryFDO profile information to optimize compilation. Specify the name of default cross binary profile.
      When the option is used together with --fdo_instrument/--fdo_optimize/--fdo_profile, those options will
      always prevail as if xbinary_fdo is never specified.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val xbinaryFdo = Flag.Label("xbinaryFdo")

  @Option(
    name = "xcode_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """
      If specified, uses Xcode of the given version for relevant build actions. If unspecified, uses the executor
      default version of Xcode.
    """,
    valueHelp = "a string",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val xcodeVersion = Flag.Str("xcodeVersion")

  @Option(
    name = "xcode_version_config",
    defaultValue = "@bazel_tools//tools/cpp:host_xcodes",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """
      The label of the xcode_config rule to be used for selecting the Xcode version in the build configuration.
    """,
    valueHelp = "a build target label",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val xcodeVersionConfig = Flag.Label("xcodeVersionConfig")

  @Option(
    name = "xml:default_values",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      If true, rule attributes whose value is not explicitly specified in the BUILD file are printed; otherwise
      they are omitted.
    """,
    valueHelp = "a boolean",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val xml_defaultValues = Flag.Boolean("xml_defaultValues")

  @Option(
    name = "xml:line_numbers",
    defaultValue = "true",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """
      If true, XML output contains line numbers. Disabling this option may make diffs easier to read.  This option
      is only applicable to --output=xml.
    """,
    valueHelp = "a boolean",
    commands = ["query"],
  )
  @JvmField
  @Suppress("unused")
  val xml_lineNumbers = Flag.Boolean("xml_lineNumbers")

  @Option(
    name = "zip_undeclared_test_outputs",
    defaultValue = "false",
    effectTags = [OptionEffectTag.TEST_RUNNER],
    help = "If true, undeclared test outputs will be archived in a zip file.",
    valueHelp = "a boolean",
    commands = [
      "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
      "mobile-install", "print_action", "run", "test", "vendor",
    ],
  )
  @JvmField
  @Suppress("unused")
  val zipUndeclaredTestOutputs = Flag.Boolean("zipUndeclaredTestOutputs")
}
