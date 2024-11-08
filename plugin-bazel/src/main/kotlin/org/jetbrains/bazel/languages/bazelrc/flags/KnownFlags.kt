package org.jetbrains.bazel.languages.bazelrc.flags

internal object KnownFlags {
  //   --distdir (a path; may be used multiple times)
  @Option(
    name = "distdir",
    allowMultiple = true,
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      Additional places to search for archives before accessing the network to 
      download them.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val distdir = Flag.Path("distdir")

  //   --[no]experimental_repository_cache_hardlinks (a boolean; default: "false")
  @Option(
    name = "experimental_repository_cache_hardlinks",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      If set, the repository cache will hardlink the file in case of a cache hit, 
      rather than copying. This is intended to save disk space.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRepositoryCacheHardlinks = Flag.Boolean("experimentalRepositoryCacheHardlinks")

  //   --experimental_repository_downloader_retries (an integer; default: "0")
  @Option(
    name = "experimental_repository_downloader_retries",
    defaultValue = """"0"""",
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      The maximum number of attempts to retry a download error. If set to 0, retries 
      are disabled.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRepositoryDownloaderRetries = Flag.Integer("experimentalRepositoryDownloaderRetries")

  //   --experimental_scale_timeouts (a double; default: "1.0")
  @Option(
    name = "experimental_scale_timeouts",
    defaultValue = """"1.0"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      Scale all timeouts in Starlark repository rules by this factor. In this way, 
      external repositories can be made working on machines that are slower than the 
      rule author expected, without changing the source code
      """,
    valueHelp = """a double""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalScaleTimeouts = Flag.Double("experimentalScaleTimeouts")

  //   --http_connector_attempts (an integer; default: "8")
  @Option(
    name = "http_connector_attempts",
    defaultValue = """"8"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """The maximum number of attempts for http downloads.""",
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val httpConnectorAttempts = Flag.Integer("httpConnectorAttempts")

  //   --http_connector_retry_max_timeout (An immutable length of time.; default: "0s")
  @Option(
    name = "http_connector_retry_max_timeout",
    defaultValue = """"0s"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      The maximum timeout for http download retries. With a value of 0, no timeout 
      maximum is defined.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val httpConnectorRetryMaxTimeout = Flag.Duration("httpConnectorRetryMaxTimeout")

  //   --http_timeout_scaling (a double; default: "1.0")
  @Option(
    name = "http_timeout_scaling",
    defaultValue = """"1.0"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """Scale all timeouts related to http downloads by the given factor""",
    valueHelp = """a double""",
  )
  @JvmField
  @Suppress("unused")
  val httpTimeoutScaling = Flag.Double("httpTimeoutScaling")

  //   --[no]incompatible_disable_native_repo_rules (a boolean; default: "false")
  @Option(
    name = "incompatible_disable_native_repo_rules",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      If false, native repo rules can be used in WORKSPACE; otherwise, Starlark repo 
      rules must be used instead. Native repo rules include local_repository, 
      new_local_repository, local_config_platform, android_sdk_repository, and 
      android_ndk_repository.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableNativeRepoRules = Flag.Boolean("incompatibleDisableNativeRepoRules")

  //   --repository_cache (a path; default: see description)
  @Option(
    name = "repository_cache",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      Specifies the cache location of the downloaded values obtained during the 
      fetching of external repositories. An empty string as argument requests the 
      cache to be disabled, otherwise the default of 
      '<output_user_root>/cache/repos/v1' is used
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val repositoryCache = Flag.Path("repositoryCache")

  //   --[no]repository_disable_download (a boolean; default: "false")
  @Option(
    name = "repository_disable_download",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      If set, downloading using ctx.download{,_and_extract} is not allowed during 
      repository fetching. Note that network access is not completely disabled; 
      ctx.execute could still run an arbitrary executable that accesses the Internet.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val repositoryDisableDownload = Flag.Boolean("repositoryDisableDownload")

  //   --[no]check_up_to_date (a boolean; default: "false")
  @Option(
    name = "check_up_to_date",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Don't perform the build, just check if it is up-to-date.  If all targets are 
      up-to-date, the build completes successfully.  If any step needs to be executed 
      an error is reported and the build fails.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val checkUpToDate = Flag.Boolean("checkUpToDate")

  //   --dynamic_local_execution_delay (an integer; default: "1000")
  @Option(
    name = "dynamic_local_execution_delay",
    defaultValue = """"1000"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      How many milliseconds should local execution be delayed, if remote execution 
      was faster during a build at least once?
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val dynamicLocalExecutionDelay = Flag.Integer("dynamicLocalExecutionDelay")

  //   --dynamic_local_strategy (a '[name=]value1[,..,valueN]' assignment; may be used multiple times)
  @Option(
    name = "dynamic_local_strategy",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      The local strategies, in order, to use for the given mnemonic - the first 
      applicable strategy is used. For example, `worker,sandboxed` runs actions that 
      support persistent workers using the worker strategy, and all others using the 
      sandboxed strategy. If no mnemonic is given, the list of strategies is used as 
      the fallback for all mnemonics. The default fallback list is 
      `worker,sandboxed`, or`worker,sandboxed,standalone` if 
      `experimental_local_lockfree_output` is set. Takes 
      [mnemonic=]local_strategy[,local_strategy,...]
      """,
    valueHelp = """a '[name=]value1[,..,valueN]' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val dynamicLocalStrategy = Flag.Unknown("dynamicLocalStrategy")

  //   --dynamic_remote_strategy (a '[name=]value1[,..,valueN]' assignment; may be used multiple times)
  @Option(
    name = "dynamic_remote_strategy",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      The remote strategies, in order, to use for the given mnemonic - the first 
      applicable strategy is used. If no mnemonic is given, the list of strategies is 
      used as the fallback for all mnemonics. The default fallback list is `remote`, 
      so this flag usually does not need to be set explicitly. Takes 
      [mnemonic=]remote_strategy[,remote_strategy,...]
      """,
    valueHelp = """a '[name=]value1[,..,valueN]' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val dynamicRemoteStrategy = Flag.Unknown("dynamicRemoteStrategy")

  //   --experimental_docker_image (a string; default: "")
  @Option(
    name = "experimental_docker_image",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Specify a Docker image name (e.g. "ubuntu:latest") that should be used to 
      execute a sandboxed action when using the docker strategy and the action itself 
      doesn't already have a container-image attribute in its 
      remote_execution_properties in the platform description. The value of this flag 
      is passed verbatim to 'docker run', so it supports the same syntax and 
      mechanisms as Docker itself.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDockerImage = Flag.Str("experimentalDockerImage")

  //   --[no]experimental_docker_use_customized_images (a boolean; default: "true")
  @Option(
    name = "experimental_docker_use_customized_images",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If enabled, injects the uid and gid of the current user into the Docker image 
      before using it. This is required if your build / tests depend on the user 
      having a name and home directory inside the container. This is on by default, 
      but you can disable it in case the automatic image customization feature 
      doesn't work in your case or you know that you don't need it.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDockerUseCustomizedImages = Flag.Boolean("experimentalDockerUseCustomizedImages")

  //   --[no]experimental_dynamic_exclude_tools (a boolean; default: "true")
  @Option(
    name = "experimental_dynamic_exclude_tools",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      When set, targets that are build "for tool" are not subject to dynamic 
      execution. Such targets are extremely unlikely to be built incrementally and 
      thus not worth spending local cycles on.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDynamicExcludeTools = Flag.Boolean("experimentalDynamicExcludeTools")

  //   --experimental_dynamic_local_load_factor (a double; default: "0")
  @Option(
    name = "experimental_dynamic_local_load_factor",
    defaultValue = """"0"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Controls how much load from dynamic execution to put on the local machine. This 
      flag adjusts how many actions in dynamic execution we will schedule 
      concurrently. It is based on the number of CPUs Blaze thinks is available, 
      which can be controlled with the --local_cpu_resources flag.If this flag is 0, 
      all actions are scheduled locally immediately. If > 0, the amount of actions 
      scheduled locally is limited by the number of CPUs available. If < 1, the load 
      factor is used to reduce the number of locally scheduled actions when the 
      number of actions waiting to schedule is high. This lessens the load on the 
      local machine in the clean build case, where the local machine does not 
      contribute much.
      """,
    valueHelp = """a double""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDynamicLocalLoadFactor = Flag.Double("experimentalDynamicLocalLoadFactor")

  //   --experimental_dynamic_slow_remote_time (An immutable length of time.; default: "0")
  @Option(
    name = "experimental_dynamic_slow_remote_time",
    defaultValue = """"0"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      If >0, the time a dynamically run action must run remote-only before we 
      prioritize its local execution to avoid remote timeouts. This may hide some 
      problems on the remote execution system. Do not turn this on without monitoring 
      of remote execution issues.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDynamicSlowRemoteTime = Flag.Duration("experimentalDynamicSlowRemoteTime")

  //   --[no]experimental_enable_docker_sandbox (a boolean; default: "false")
  @Option(
    name = "experimental_enable_docker_sandbox",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Enable Docker-based sandboxing. This option has no effect if Docker is not 
      installed.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableDockerSandbox = Flag.Boolean("experimentalEnableDockerSandbox")

  //   --[no]experimental_inmemory_sandbox_stashes (a boolean; default: "false")
  @Option(
    name = "experimental_inmemory_sandbox_stashes",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """      
      If set to true, the contents of stashed sandboxes for reuse_sandbox_directories 
      will be tracked in memory. This reduces the amount of I/O needed during reuse. 
      Depending on the build this flag may improve wall time. Depending on the build 
      as well this flag may use a significant amount of additional memory.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalInmemorySandboxStashes = Flag.Boolean("experimentalInmemorySandboxStashes")

  //   --[no]experimental_inprocess_symlink_creation (a boolean; default: "false")
  @Option(
    name = "experimental_inprocess_symlink_creation",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Whether to make direct file system calls to create symlink trees""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalInprocessSymlinkCreation = Flag.Boolean("experimentalInprocessSymlinkCreation")

  //   --[no]experimental_persistent_aar_extractor (a boolean; default: "false")
  @Option(
    name = "experimental_persistent_aar_extractor",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """Enable persistent aar extractor by using workers.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalPersistentAarExtractor = Flag.Boolean("experimentalPersistentAarExtractor")

  //   --[no]experimental_remotable_source_manifests (a boolean; default: "false")
  @Option(
    name = "experimental_remotable_source_manifests",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Whether to make source manifest actions remotable""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemotableSourceManifests = Flag.Boolean("experimentalRemotableSourceManifests")

  //
  //   --experimental_sandbox_async_tree_delete_idle_threads (an integer, or a keyword ("auto",
  // "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto",
  // "HOST_CPUS*.5"; default: "4")
  //
  @Option(
    name = "experimental_sandbox_async_tree_delete_idle_threads",
    defaultValue = """"4"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """      
      If 0, delete sandbox trees as soon as an action completes (causing completion 
      of the action to be delayed). If greater than zero, execute the deletion of 
      such threes on an asynchronous thread pool that has size 1 when the build is 
      running and grows to the size specified by this flag when the server is idle.
      """,
    valueHelp = """      
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed 
      by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
      """,
  )
  @JvmField
  @Suppress("unused")
  val experimentalSandboxAsyncTreeDeleteIdleThreads = Flag.Unknown("experimentalSandboxAsyncTreeDeleteIdleThreads")

  //
  //   --experimental_sandbox_memory_limit_mb (an integer number of MBs, or "HOST_RAM", optionally
  // followed by [-|*]<float>.; default: "0")
  //
  @Option(
    name = "experimental_sandbox_memory_limit_mb",
    defaultValue = """"0"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If > 0, each Linux sandbox will be limited to the given amount of memory (in 
      MB). Requires cgroups v1 or v2 and permissions for the users to the cgroups dir.
      """,
    valueHelp = """an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalSandboxMemoryLimitMb = Flag.Unknown("experimentalSandboxMemoryLimitMb")

  //   --[no]experimental_shrink_worker_pool (a boolean; default: "false")
  @Option(
    name = "experimental_shrink_worker_pool",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      If enabled, could shrink worker pool if worker memory pressure is high. This 
      flag works only when flag experimental_total_worker_memory_limit_mb is enabled.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalShrinkWorkerPool = Flag.Boolean("experimentalShrinkWorkerPool")

  //   --[no]experimental_split_coverage_postprocessing (a boolean; default: "false")
  @Option(
    name = "experimental_split_coverage_postprocessing",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """If true, then Bazel will run coverage postprocessing for test in a new spawn.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalSplitCoveragePostprocessing = Flag.Boolean("experimentalSplitCoveragePostprocessing")

  //   --[no]experimental_split_xml_generation (a boolean; default: "true")
  @Option(
    name = "experimental_split_xml_generation",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If this flag is set, and a test action does not generate a test.xml file, then 
      Bazel uses a separate action to generate a dummy test.xml file containing the 
      test log. Otherwise, Bazel generates a test.xml as part of the test action.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalSplitXmlGeneration = Flag.Boolean("experimentalSplitXmlGeneration")

  //   --[no]experimental_strict_fileset_output (a boolean; default: "false")
  @Option(
    name = "experimental_strict_fileset_output",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If this option is enabled, filesets will treat all output artifacts as regular 
      files. They will not traverse directories or be sensitive to symlinks.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalStrictFilesetOutput = Flag.Boolean("experimentalStrictFilesetOutput")

  //
  //   --experimental_total_worker_memory_limit_mb (an integer number of MBs, or "HOST_RAM", optionally
  // followed by [-|*]<float>.; default: "0")
  //
  @Option(
    name = "experimental_total_worker_memory_limit_mb",
    defaultValue = """"0"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      If this limit is greater than zero idle workers might be killed if the total 
      memory usage of all  workers exceed the limit.
      """,
    valueHelp = """an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalTotalWorkerMemoryLimitMb = Flag.Unknown("experimentalTotalWorkerMemoryLimitMb")

  //   --experimental_ui_max_stdouterr_bytes (an integer in (-1)-1073741819 range; default: "1048576")
  @Option(
    name = "experimental_ui_max_stdouterr_bytes",
    defaultValue = """"1048576"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      The maximum size of the stdout / stderr files that will be printed to the 
      console. -1 implies no limit.
      """,
    valueHelp = """an integer in (-1)-1073741819 range""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalUiMaxStdouterrBytes = Flag.Unknown("experimentalUiMaxStdouterrBytes")

  //   --[no]experimental_use_hermetic_linux_sandbox (a boolean; default: "false")
  @Option(
    name = "experimental_use_hermetic_linux_sandbox",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If set to true, do not mount root, only mount whats provided with 
      sandbox_add_mount_pair. Input files will be hardlinked to the sandbox instead 
      of symlinked to from the sandbox. If action input files are located on a 
      filesystem different from the sandbox, then the input files will be copied 
      instead.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseHermeticLinuxSandbox = Flag.Boolean("experimentalUseHermeticLinuxSandbox")

  //   --[no]experimental_use_semaphore_for_jobs (a boolean; default: "true")
  @Option(
    name = "experimental_use_semaphore_for_jobs",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """If set to true, additionally use semaphore to limit number of concurrent jobs.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseSemaphoreForJobs = Flag.Boolean("experimentalUseSemaphoreForJobs")

  //   --[no]experimental_use_windows_sandbox (a tri-state (auto, yes, no); default: "false")
  @Option(
    name = "experimental_use_windows_sandbox",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Use Windows sandbox to run actions. If "yes", the binary provided by 
      --experimental_windows_sandbox_path must be valid and correspond to a supported 
      version of sandboxfs. If "auto", the binary may be missing or not compatible.
      """,
    valueHelp = """a tri-state (auto, yes, no)""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseWindowsSandbox = Flag.TriState("experimentalUseWindowsSandbox")

  //   --experimental_windows_sandbox_path (a string; default: "BazelSandbox.exe")
  @Option(
    name = "experimental_windows_sandbox_path",
    defaultValue = """"BazelSandbox.exe"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Path to the Windows sandbox binary to use when 
      --experimental_use_windows_sandbox is true. If a bare name, use the first 
      binary of that name found in the PATH.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWindowsSandboxPath = Flag.Str("experimentalWindowsSandboxPath")

  //   --experimental_worker_allowlist (comma-separated set of options; default: see description)
  @Option(
    name = "experimental_worker_allowlist",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      If non-empty, only allow using persistent workers with the given worker key 
      mnemonic.
      """,
    valueHelp = """comma-separated set of options""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerAllowlist = Flag.Unknown("experimentalWorkerAllowlist")

  //   --[no]experimental_worker_as_resource (a boolean; default: "true")
  @Option(
    name = "experimental_worker_as_resource",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.NO_OP],
    help = """No-op, will be removed soon.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerAsResource = Flag.Boolean("experimentalWorkerAsResource")

  //   --[no]experimental_worker_cancellation (a boolean; default: "false")
  @Option(
    name = "experimental_worker_cancellation",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """If enabled, Bazel may send cancellation requests to workers that support them.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerCancellation = Flag.Boolean("experimentalWorkerCancellation")

  //
  //   --experimental_worker_memory_limit_mb (an integer number of MBs, or "HOST_RAM", optionally
  // followed by [-|*]<float>.; default: "0")
  //
  @Option(
    name = "experimental_worker_memory_limit_mb",
    defaultValue = """"0"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      If this limit is greater than zero, workers might be killed if the memory usage 
      of the worker exceeds the limit. If not used together with dynamic execution 
      and `--experimental_dynamic_ignore_local_signals=9`, this may crash your build.
      """,
    valueHelp = """an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerMemoryLimitMb = Flag.Unknown("experimentalWorkerMemoryLimitMb")

  //   --experimental_worker_metrics_poll_interval (An immutable length of time.; default: "5s")
  @Option(
    name = "experimental_worker_metrics_poll_interval",
    defaultValue = """"5s"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      The interval between collecting worker metrics and possibly attempting 
      evictions. Cannot effectively be less than 1s for performance reasons.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerMetricsPollInterval = Flag.Duration("experimentalWorkerMetricsPollInterval")

  //   --[no]experimental_worker_multiplex_sandboxing (a boolean; default: "false")
  @Option(
    name = "experimental_worker_multiplex_sandboxing",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If enabled, multiplex workers will be sandboxed, using a separate sandbox 
      directory per work request. Only workers that have the 
      'supports-multiplex-sandboxing' execution requirement will be sandboxed.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerMultiplexSandboxing = Flag.Boolean("experimentalWorkerMultiplexSandboxing")

  //   --[no]experimental_worker_sandbox_hardening (a boolean; default: "false")
  @Option(
    name = "experimental_worker_sandbox_hardening",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If enabled, workers are run in a hardened sandbox, if the implementation allows 
      it.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerSandboxHardening = Flag.Boolean("experimentalWorkerSandboxHardening")

  //   --[no]experimental_worker_strict_flagfiles (a boolean; default: "false")
  @Option(
    name = "experimental_worker_strict_flagfiles",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If enabled, actions arguments for workers that do not follow the worker 
      specification will cause an error. Worker arguments must have exactly one 
      @flagfile argument as the last of its list of arguments.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerStrictFlagfiles = Flag.Boolean("experimentalWorkerStrictFlagfiles")

  //   --gc_thrashing_threshold (an integer in 0-100 range; default: "100")
  @Option(
    name = "gc_thrashing_threshold",
    defaultValue = """"100"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      The percent of tenured space occupied (0-100) above which GcThrashingDetector 
      considers memory pressure events against its limits (--gc_thrashing_limits). If 
      set to 100, GcThrashingDetector is disabled.
      """,
    valueHelp = """an integer in 0-100 range""",
  )
  @JvmField
  @Suppress("unused")
  val gcThrashingThreshold = Flag.Unknown("gcThrashingThreshold")

  //   --genrule_strategy (comma-separated list of options; default: "")
  @Option(
    name = "genrule_strategy",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Specify how to execute genrules. This flag will be phased out. Instead, use 
      --spawn_strategy=<value> to control all actions or --strategy=Genrule=<value> 
      to control genrules only.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val genruleStrategy = Flag.Unknown("genruleStrategy")

  //   --high_priority_workers (a string; may be used multiple times)
  @Option(
    name = "high_priority_workers",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """No-op, will be removed soon.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val highPriorityWorkers = Flag.Str("highPriorityWorkers")

  //   --[no]incompatible_disallow_unsound_directory_outputs (a boolean; default: "true")
  @Option(
    name = "incompatible_disallow_unsound_directory_outputs",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set, it is an error for an action to materialize an output file as a 
      directory. Does not affect source directories. See 
      https://github.com/bazelbuild/bazel/issues/18646.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowUnsoundDirectoryOutputs = Flag.Boolean("incompatibleDisallowUnsoundDirectoryOutputs")

  //   --[no]incompatible_modify_execution_info_additive (a boolean; default: "false")
  @Option(
    name = "incompatible_modify_execution_info_additive",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      When enabled, passing multiple --modify_execution_info flags is additive. When 
      disabled, only the last flag is taken into account.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleModifyExecutionInfoAdditive = Flag.Boolean("incompatibleModifyExecutionInfoAdditive")

  //   --[no]incompatible_remote_dangling_symlinks (a boolean; default: "true")
  @Option(
    name = "incompatible_remote_dangling_symlinks",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, symlinks uploaded to a remote or disk cache are allowed to 
      dangle.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoteDanglingSymlinks = Flag.Boolean("incompatibleRemoteDanglingSymlinks")

  //   --[no]incompatible_remote_symlinks (a boolean; default: "true")
  @Option(
    name = "incompatible_remote_symlinks",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, Bazel will always upload symlinks as such to a remote or disk 
      cache. Otherwise, non-dangling relative symlinks (and only those) will be 
      uploaded as the file or directory they point to.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoteSymlinks = Flag.Boolean("incompatibleRemoteSymlinks")

  //   --[no]incompatible_sandbox_hermetic_tmp (a boolean; default: "true")
  @Option(
    name = "incompatible_sandbox_hermetic_tmp",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If set to true, each Linux sandbox will have its own dedicated empty directory 
      mounted as /tmp rather than sharing /tmp with the host filesystem. Use 
      --sandbox_add_mount_pair=/tmp to keep seeing the host's /tmp in all sandboxes.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleSandboxHermeticTmp = Flag.Boolean("incompatibleSandboxHermeticTmp")

  //   --[no]internal_spawn_scheduler (a boolean; default: "false")
  @Option(
    name = "internal_spawn_scheduler",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Placeholder option so that we can tell in Blaze whether the spawn scheduler was 
      enabled.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val internalSpawnScheduler = Flag.Boolean("internalSpawnScheduler")

  //
  //   --jobs [-j] (an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by
  // an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"; default: "auto")
  //
  @Option(
    name = "jobs",
    abbrev = 'j',
    defaultValue = """"auto"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """      
      The number of concurrent jobs to run. Takes an integer, or a keyword ("auto", 
      "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) 
      eg. "auto", "HOST_CPUS*.5". Values must be between 1 and 5000. Values above 
      2500 may cause memory issues. "auto" calculates a reasonable default based on 
      host resources.
      """,
    valueHelp = """      
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed 
      by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
      """,
  )
  @JvmField
  @Suppress("unused")
  val jobs = Flag.Unknown("jobs")

  //   --[no]keep_going [-k] (a boolean; default: "false")
  @Option(
    name = "keep_going",
    abbrev = 'k',
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      Continue as much as possible after an error.  While the target that failed and 
      those that depend on it cannot be analyzed, other prerequisites of these 
      targets can be.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val keepGoing = Flag.Boolean("keepGoing")

  //
  //   --loading_phase_threads (an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally
  // followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"; default: "auto")
  //
  @Option(
    name = "loading_phase_threads",
    defaultValue = """"auto"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      Number of parallel threads to use for the loading/analysis phase.Takes an 
      integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by 
      an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". "auto" sets a 
      reasonable default based on host resources. Must be at least 1.
      """,
    valueHelp = """      
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed 
      by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
      """,
  )
  @JvmField
  @Suppress("unused")
  val loadingPhaseThreads = Flag.Unknown("loadingPhaseThreads")

  //   --modify_execution_info (regex=[+-]key,regex=[+-]key,...; may be used multiple times)
  @Option(
    name = "modify_execution_info",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Add or remove keys from an action's execution info based on action mnemonic.  
      Applies only to actions which support execution info. Many common actions 
      support execution info, e.g. Genrule, CppCompile, Javac, StarlarkAction, 
      TestRunner. When specifying multiple values, order matters because many regexes 
      may apply to the same mnemonic.
      
      Syntax: `regex=[+-]key,regex=[+-]key,...`.
      
      Examples:  
        * `.*=+x,.*=-y,.*=+z` adds `x` and `z` to, and removes `y` from, the execution info for all actions.
        * `Genrule=+requires-x` adds 'requires-x' to the execution info for all Genrule actions.
        * `(?!Genrule).*=-requires-x` removes `requires-x` from the execution info for all non-Genrule actions.
      """,
    valueHelp = """regex=[+-]key,regex=[+-]key,...""",
  )
  @JvmField
  @Suppress("unused")
  val modifyExecutionInfo = Flag.Unknown("modifyExecutionInfo")

  //   --persistent_android_dex_desugar
  @Option(
    name = "persistent_android_dex_desugar",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    expandsTo = [
      "--internal_persistent_android_dex_desugar", "--strategy=Desugar=worker",
      "--strategy=DexBuilder=worker",
    ],
    help = """Enable persistent Android dex and desugar actions by using workers.""",
  )
  @JvmField
  @Suppress("unused")
  val persistentAndroidDexDesugar = Flag.Unknown("persistentAndroidDexDesugar")

  //   --persistent_android_resource_processor
  @Option(
    name = "persistent_android_resource_processor",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    expandsTo = [
      "--internal_persistent_busybox_tools", "--strategy=AaptPackage=worker",
      "--strategy=AndroidResourceParser=worker",
      "--strategy=AndroidResourceValidator=worker",
      "--strategy=AndroidResourceCompiler=worker",
      "--strategy=RClassGenerator=worker", "--strategy=AndroidResourceLink=worker",
      "--strategy=AndroidAapt2=worker", "--strategy=AndroidAssetMerger=worker",
      "--strategy=AndroidResourceMerger=worker",
      "--strategy=AndroidCompiledResourceMerger=worker",
      "--strategy=ManifestMerger=worker", "--strategy=AndroidManifestMerger=worker",
      "--strategy=Aapt2Optimize=worker", "--strategy=AARGenerator=worker",
      "--strategy=ProcessDatabinding=worker",
      "--strategy=GenerateDataBindingBaseClasses=worker",
    ],
    help = """Enable persistent Android resource processor by using workers.""",
  )
  @JvmField
  @Suppress("unused")
  val persistentAndroidResourceProcessor = Flag.Unknown("persistentAndroidResourceProcessor")

  //   --persistent_multiplex_android_dex_desugar
  @Option(
    name = "persistent_multiplex_android_dex_desugar",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    expandsTo = [
      "--persistent_android_dex_desugar",
      "--internal_persistent_multiplex_android_dex_desugar",
    ],
    help = """Enable persistent multiplexed Android dex and desugar actions by using workers.""",
  )
  @JvmField
  @Suppress("unused")
  val persistentMultiplexAndroidDexDesugar = Flag.Unknown("persistentMultiplexAndroidDexDesugar")

  //   --persistent_multiplex_android_resource_processor
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
    help = """Enable persistent multiplexed Android resource processor by using workers.""",
  )
  @JvmField
  @Suppress("unused")
  val persistentMultiplexAndroidResourceProcessor = Flag.Unknown("persistentMultiplexAndroidResourceProcessor")

  //   --persistent_multiplex_android_tools
  @Option(
    name = "persistent_multiplex_android_tools",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    expandsTo = [
      "--internal_persistent_multiplex_busybox_tools",
      "--persistent_multiplex_android_resource_processor",
      "--persistent_multiplex_android_dex_desugar",
    ],
    help = """      
      Enable persistent and multiplexed Android tools (dexing, desugaring, resource 
      processing).
      """,
  )
  @JvmField
  @Suppress("unused")
  val persistentMultiplexAndroidTools = Flag.Unknown("persistentMultiplexAndroidTools")

  //   --[no]reuse_sandbox_directories (a boolean; default: "true")
  @Option(
    name = "reuse_sandbox_directories",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """      
      If set to true, directories used by sandboxed non-worker execution may be 
      reused to avoid unnecessary setup costs.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val reuseSandboxDirectories = Flag.Boolean("reuseSandboxDirectories")

  //   --sandbox_base (a string; default: "")
  @Option(
    name = "sandbox_base",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """      
      Lets the sandbox create its sandbox directories underneath this path. Specify a 
      path on tmpfs (like /run/shm) to possibly improve performance a lot when your 
      build / tests have many input files. Note: You need enough RAM and free space 
      on the tmpfs to hold output and intermediate files generated by running actions.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxBase = Flag.Str("sandboxBase")

  //   --[no]sandbox_explicit_pseudoterminal (a boolean; default: "false")
  @Option(
    name = "sandbox_explicit_pseudoterminal",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Explicitly enable the creation of pseudoterminals for sandboxed actions. Some 
      linux distributions require setting the group id of the process to 'tty' inside 
      the sandbox in order for pseudoterminals to function. If this is causing 
      issues, this flag can be disabled to enable other groups to be used.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxExplicitPseudoterminal = Flag.Boolean("sandboxExplicitPseudoterminal")

  //   --sandbox_tmpfs_path (an absolute path; may be used multiple times)
  @Option(
    name = "sandbox_tmpfs_path",
    allowMultiple = true,
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """      
      For sandboxed actions, mount an empty, writable directory at this absolute path 
      (if supported by the sandboxing implementation, ignored otherwise).
      """,
    valueHelp = """an absolute path""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxTmpfsPath = Flag.Unknown("sandboxTmpfsPath")

  //   --[no]skip_incompatible_explicit_targets (a boolean; default: "false")
  @Option(
    name = "skip_incompatible_explicit_targets",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Skip incompatible targets that are explicitly listed on the command line. By 
      default, building such targets results in an error but they are silently 
      skipped when this option is enabled. See: 
      https://bazel.build/extending/platforms#skipping-incompatible-targets
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val skipIncompatibleExplicitTargets = Flag.Boolean("skipIncompatibleExplicitTargets")

  //   --spawn_strategy (comma-separated list of options; default: "")
  @Option(
    name = "spawn_strategy",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Specify how spawn actions are executed by default. Accepts a comma-separated 
      list of strategies from highest to lowest priority. For each action Bazel picks 
      the strategy with the highest priority that can execute the action. The default 
      value is "remote,worker,sandboxed,local". See 
      https://blog.bazel.build/2019/06/19/list-strategy.html for details.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val spawnStrategy = Flag.Unknown("spawnStrategy")

  //   --strategy (a '[name=]value1[,..,valueN]' assignment; may be used multiple times)
  @Option(
    name = "strategy",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Specify how to distribute compilation of other spawn actions. Accepts a 
      comma-separated list of strategies from highest to lowest priority. For each 
      action Bazel picks the strategy with the highest priority that can execute the 
      action. The default value is "remote,worker,sandboxed,local". This flag 
      overrides the values set by --spawn_strategy (and --genrule_strategy if used 
      with mnemonic Genrule). See 
      https://blog.bazel.build/2019/06/19/list-strategy.html for details.
      """,
    valueHelp = """a '[name=]value1[,..,valueN]' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val strategy = Flag.Unknown("strategy")

  //   --strategy_regexp (a '<RegexFilter>=value[,value]' assignment; may be used multiple times)
  @Option(
    name = "strategy_regexp",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Override which spawn strategy should be used to execute spawn actions that have 
      descriptions matching a certain regex_filter. See --per_file_copt for details 
      onregex_filter matching. The last regex_filter that matches the description is 
      used. This option overrides other flags for specifying strategy. Example: 
      --strategy_regexp=//foo.*\.cc,-//foo/bar=local means to run actions using local 
      strategy if their descriptions match //foo.*.cc but not //foo/bar. Example: 
      --strategy_regexp='Compiling.*/bar=local  --strategy_regexp=Compiling=sandboxed 
      will run 'Compiling //foo/bar/baz' with the 'local' strategy, but reversing the 
      order would run it with 'sandboxed'. 
      """,
    valueHelp = """a '<RegexFilter>=value[,value]' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val strategyRegexp = Flag.Unknown("strategyRegexp")

  //   --[no]use_target_platform_for_tests (a boolean; default: "false")
  @Option(
    name = "use_target_platform_for_tests",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If true, then Bazel will use the target platform for running tests rather than 
      the test exec group.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val useTargetPlatformForTests = Flag.Boolean("useTargetPlatformForTests")

  //   --worker_extra_flag (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "worker_extra_flag",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Extra command-flags that will be passed to worker processes in addition to 
      --persistent_worker, keyed by mnemonic (e.g. --worker_extra_flag=Javac=--debug.
      """,
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val workerExtraFlag = Flag.Unknown("workerExtraFlag")

  //
  //   --worker_max_instances ([name=]value, where value is an integer, or a keyword ("auto",
  // "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto",
  // "HOST_CPUS*.5"; may be used multiple times)
  //
  @Option(
    name = "worker_max_instances",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      How many instances of each kind of persistent worker may be launched if you use 
      the 'worker' strategy. May be specified as [name=value] to give a different 
      value per mnemonic. The limit is based on worker keys, which are differentiated 
      based on mnemonic, but also on startup flags and environment, so there can in 
      some cases be more workers per mnemonic than this flag specifies. Takes an 
      integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by 
      an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". 'auto' calculates a 
      reasonable default based on machine capacity. "=value" sets a default for 
      unspecified mnemonics.
      """,
    valueHelp = """      
      [name=]value, where value is an integer, or a keyword ("auto", "HOST_CPUS", 
      "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", 
      "HOST_CPUS*.5"
      """,
  )
  @JvmField
  @Suppress("unused")
  val workerMaxInstances = Flag.Unknown("workerMaxInstances")

  //
  //   --worker_max_multiplex_instances ([name=]value, where value is an integer, or a keyword ("auto",
  // "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto",
  // "HOST_CPUS*.5"; may be used multiple times)
  //
  @Option(
    name = "worker_max_multiplex_instances",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      How many WorkRequests a multiplex worker process may receive in parallel if you 
      use the 'worker' strategy with --worker_multiplex. May be specified as 
      [name=value] to give a different value per mnemonic. The limit is based on 
      worker keys, which are differentiated based on mnemonic, but also on startup 
      flags and environment, so there can in some cases be more workers per mnemonic 
      than this flag specifies. Takes an integer, or a keyword ("auto", "HOST_CPUS", 
      "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", 
      "HOST_CPUS*.5". 'auto' calculates a reasonable default based on machine 
      capacity. "=value" sets a default for unspecified mnemonics.
      """,
    valueHelp = """      
      [name=]value, where value is an integer, or a keyword ("auto", "HOST_CPUS", 
      "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", 
      "HOST_CPUS*.5"
      """,
  )
  @JvmField
  @Suppress("unused")
  val workerMaxMultiplexInstances = Flag.Unknown("workerMaxMultiplexInstances")

  //   --[no]worker_multiplex (a boolean; default: "true")
  @Option(
    name = "worker_multiplex",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """If enabled, workers will use multiplexing if they support it. """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val workerMultiplex = Flag.Boolean("workerMultiplex")

  //   --[no]worker_quit_after_build (a boolean; default: "false")
  @Option(
    name = "worker_quit_after_build",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """If enabled, all workers quit after a build is done.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val workerQuitAfterBuild = Flag.Boolean("workerQuitAfterBuild")

  //   --[no]worker_sandboxing (a boolean; default: "false")
  @Option(
    name = "worker_sandboxing",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """If enabled, workers will be executed in a sandboxed environment.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val workerSandboxing = Flag.Boolean("workerSandboxing")

  //   --[no]worker_verbose (a boolean; default: "false")
  @Option(
    name = "worker_verbose",
    defaultValue = """"false"""",
    help = """If enabled, prints verbose messages when workers are started, shutdown, ...""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val workerVerbose = Flag.Boolean("workerVerbose")

  //   --android_compiler (a string; default: see description)
  @Option(
    name = "android_compiler",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """The Android target compiler.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val androidCompiler = Flag.Str("androidCompiler")

  //   --android_crosstool_top (a build target label; default: "//external:android/crosstool")
  @Option(
    name = "android_crosstool_top",
    defaultValue = """"//external:android/crosstool"""",
    effectTags =
      [
        OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
        OptionEffectTag.LOSES_INCREMENTAL_STATE,
      ],
    help = """The location of the C++ compiler used for Android builds.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val androidCrosstoolTop = Flag.Label("androidCrosstoolTop")

  //   --android_grte_top (a label; default: see description)
  @Option(
    name = "android_grte_top",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """The Android target grte_top.""",
    valueHelp = """a label""",
  )
  @JvmField
  @Suppress("unused")
  val androidGrteTop = Flag.Unknown("androidGrteTop")

  //   --android_manifest_merger (legacy, android or force_android; default: "android")
  @Option(
    name = "android_manifest_merger",
    defaultValue = """"android"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Selects the manifest merger to use for android_binary rules. Flag to help 
      thetransition to the Android manifest merger from the legacy merger.
      """,
    valueHelp = """legacy, android or force_android""",
  )
  @JvmField
  @Suppress("unused")
  val androidManifestMerger = Flag.OneOf("androidManifestMerger")

  //   --android_platforms (a build target label; default: "")
  @Option(
    name = "android_platforms",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Sets the platforms that android_binary targets use. If multiple platforms are 
      specified, then the binary is a fat APKs, which contains native binaries for 
      each specified target platform.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val androidPlatforms = Flag.Label("androidPlatforms")

  //   --android_sdk (a build target label; default: "@bazel_tools//tools/android:sdk")
  @Option(
    name = "android_sdk",
    defaultValue = """"@bazel_tools//tools/android:sdk"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """Specifies Android SDK/platform that is used to build Android applications.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val androidSdk = Flag.Label("androidSdk")

  //   --apple_crosstool_top (a build target label; default: "@bazel_tools//tools/cpp:toolchain")
  @Option(
    name = "apple_crosstool_top",
    defaultValue = """"@bazel_tools//tools/cpp:toolchain"""",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.CHANGES_INPUTS],
    help = """      
      The label of the crosstool package to be used in Apple and Objc rules and their 
      dependencies.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val appleCrosstoolTop = Flag.Label("appleCrosstoolTop")

  //   --cc_output_directory_tag (a string; default: "")
  @Option(
    name = "cc_output_directory_tag",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Specifies a suffix to be added to the configuration directory.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val ccOutputDirectoryTag = Flag.Str("ccOutputDirectoryTag")

  //   --compiler (a string; default: see description)
  @Option(
    name = "compiler",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    help = """The C++ compiler to use for compiling the target.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val compiler = Flag.Str("compiler")

  //
  //   --coverage_output_generator (a build target label; default:
  // "@bazel_tools//tools/test:lcov_merger")
  //
  @Option(
    name = "coverage_output_generator",
    defaultValue = """"@bazel_tools//tools/test:lcov_merger"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Location of the binary that is used to postprocess raw coverage reports. This 
      must currently be a filegroup that contains a single file, the binary. Defaults 
      to '//tools/test:lcov_merger'.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val coverageOutputGenerator = Flag.Label("coverageOutputGenerator")

  //
  //   --coverage_report_generator (a build target label; default:
  // "@bazel_tools//tools/test:coverage_report_generator")
  //
  @Option(
    name = "coverage_report_generator",
    defaultValue = """"@bazel_tools//tools/test:coverage_report_generator"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Location of the binary that is used to generate coverage reports. This must 
      currently be a filegroup that contains a single file, the binary. Defaults to 
      '//tools/test:coverage_report_generator'.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val coverageReportGenerator = Flag.Label("coverageReportGenerator")

  //   --coverage_support (a build target label; default: "@bazel_tools//tools/test:coverage_support")
  @Option(
    name = "coverage_support",
    defaultValue = """"@bazel_tools//tools/test:coverage_support"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Location of support files that are required on the inputs of every test action 
      that collects code coverage. Defaults to '//tools/test:coverage_support'.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val coverageSupport = Flag.Label("coverageSupport")

  //   --crosstool_top (a build target label; default: "@bazel_tools//tools/cpp:toolchain")
  @Option(
    name = "crosstool_top",
    defaultValue = """"@bazel_tools//tools/cpp:toolchain"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """The label of the crosstool package to be used for compiling C++ code.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val crosstoolTop = Flag.Label("crosstoolTop")

  //   --custom_malloc (a build target label; default: see description)
  @Option(
    name = "custom_malloc",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies a custom malloc implementation. This setting overrides malloc 
      attributes in build rules.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val customMalloc = Flag.Label("customMalloc")

  //
  //   --experimental_add_exec_constraints_to_targets (a '<RegexFilter>=<label1>[,<label2>,...]'
  // assignment; may be used multiple times)
  //
  @Option(
    name = "experimental_add_exec_constraints_to_targets",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      List of comma-separated regular expressions, each optionally prefixed by - 
      (negative expression), assigned (=) to a list of comma-separated constraint 
      value targets. If a target matches no negative expression and at least one 
      positive expression its toolchain resolution will be performed as if it had 
      declared the constraint values as execution constraints. Example: 
      //demo,-test=@platforms//cpus:x86_64 will add 'x86_64' to any target under 
      //demo except for those whose name contains 'test'.
      """,
    valueHelp = """a '<RegexFilter>=<label1>[,<label2>,...]' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalAddExecConstraintsToTargets = Flag.Unknown("experimentalAddExecConstraintsToTargets")

  //   --[no]experimental_include_xcode_execution_requirements (a boolean; default: "false")
  @Option(
    name = "experimental_include_xcode_execution_requirements",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    help = """      
      If set, add a "requires-xcode:{version}" execution requirement to every Xcode 
      action.  If the xcode version has a hyphenated label,  also add a 
      "requires-xcode-label:{version_label}" execution requirement.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalIncludeXcodeExecutionRequirements = Flag.Boolean("experimentalIncludeXcodeExecutionRequirements")

  //   --[no]experimental_prefer_mutual_xcode (a boolean; default: "true")
  @Option(
    name = "experimental_prefer_mutual_xcode",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      If true, use the most recent Xcode that is available both locally and remotely. 
      If false, or if there are no mutual available versions, use the local Xcode 
      version selected via xcode-select.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalPreferMutualXcode = Flag.Boolean("experimentalPreferMutualXcode")

  //   --extra_execution_platforms (comma-separated list of options; default: "")
  @Option(
    name = "extra_execution_platforms",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      The platforms that are available as execution platforms to run actions. 
      Platforms can be specified by exact target, or as a target pattern. These 
      platforms will be considered before those declared in the WORKSPACE file by 
      register_execution_platforms(). This option may only be set once; later 
      instances will override earlier flag settings.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val extraExecutionPlatforms = Flag.Unknown("extraExecutionPlatforms")

  //   --extra_toolchains (comma-separated list of options; may be used multiple times)
  @Option(
    name = "extra_toolchains",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      The toolchain rules to be considered during toolchain resolution. Toolchains 
      can be specified by exact target, or as a target pattern. These toolchains will 
      be considered before those declared in the WORKSPACE file by 
      register_toolchains().
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val extraToolchains = Flag.Unknown("extraToolchains")

  //   --grte_top (a label; default: see description)
  @Option(
    name = "grte_top",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      A label to a checked-in libc library. The default value is selected by the 
      crosstool toolchain, and you almost never need to override it.
      """,
    valueHelp = """a label""",
  )
  @JvmField
  @Suppress("unused")
  val grteTop = Flag.Unknown("grteTop")

  //   --host_compiler (a string; default: see description)
  @Option(
    name = "host_compiler",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION],
    help = """      
      The C++ compiler to use for host compilation. It is ignored if 
      --host_crosstool_top is not set.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostCompiler = Flag.Str("hostCompiler")

  //   --host_crosstool_top (a build target label; default: see description)
  @Option(
    name = "host_crosstool_top",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      By default, the --crosstool_top and --compiler options are also used for the 
      exec configuration. If this flag is provided, Bazel uses the default libc and 
      compiler for the given crosstool_top.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val hostCrosstoolTop = Flag.Label("hostCrosstoolTop")

  //   --host_grte_top (a label; default: see description)
  @Option(
    name = "host_grte_top",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If specified, this setting overrides the libc top-level directory (--grte_top) 
      for the exec configuration.
      """,
    valueHelp = """a label""",
  )
  @JvmField
  @Suppress("unused")
  val hostGrteTop = Flag.Unknown("hostGrteTop")

  //   --host_platform (a build target label; default: "@bazel_tools//tools:host_platform")
  @Option(
    name = "host_platform",
    defaultValue = """"@bazel_tools//tools:host_platform"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """The label of a platform rule that describes the host system.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val hostPlatform = Flag.Label("hostPlatform")

  //   --[no]incompatible_dont_enable_host_nonhost_crosstool_features (a boolean; default: "true")
  @Option(
    name = "incompatible_dont_enable_host_nonhost_crosstool_features",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, Bazel will not enable 'host' and 'nonhost' features in the c++ 
      toolchain (see https://github.com/bazelbuild/bazel/issues/7407 for more 
      information).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDontEnableHostNonhostCrosstoolFeatures = Flag.Boolean("incompatibleDontEnableHostNonhostCrosstoolFeatures")

  //   --[no]incompatible_enable_android_toolchain_resolution (a boolean; default: "true")
  @Option(
    name = "incompatible_enable_android_toolchain_resolution",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      Use toolchain resolution to select the Android SDK for android rules (Starlark 
      and native)
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableAndroidToolchainResolution = Flag.Boolean("incompatibleEnableAndroidToolchainResolution")

  //   --[no]incompatible_enable_apple_toolchain_resolution (a boolean; default: "false")
  @Option(
    name = "incompatible_enable_apple_toolchain_resolution",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      Use toolchain resolution to select the Apple SDK for apple rules (Starlark and 
      native)
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableAppleToolchainResolution = Flag.Boolean("incompatibleEnableAppleToolchainResolution")

  //   --[no]incompatible_enable_proto_toolchain_resolution (a boolean; default: "false")
  @Option(
    name = "incompatible_enable_proto_toolchain_resolution",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, proto lang rules define toolchains from rules_proto, rules_java, 
      rules_cc repositories.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableProtoToolchainResolution = Flag.Boolean("incompatibleEnableProtoToolchainResolution")

  //   --[no]incompatible_make_thinlto_command_lines_standalone (a boolean; default: "true")
  @Option(
    name = "incompatible_make_thinlto_command_lines_standalone",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, Bazel will not reuse C++ link action command lines for lto indexing 
      command lines (see https://github.com/bazelbuild/bazel/issues/6791 for more 
      information).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleMakeThinltoCommandLinesStandalone = Flag.Boolean("incompatibleMakeThinltoCommandLinesStandalone")

  //   --[no]incompatible_remove_legacy_whole_archive (a boolean; default: "true")
  @Option(
    name = "incompatible_remove_legacy_whole_archive",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, Bazel will not link library dependencies as whole archive by default 
      (see https://github.com/bazelbuild/bazel/issues/7362 for migration 
      instructions).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoveLegacyWholeArchive = Flag.Boolean("incompatibleRemoveLegacyWholeArchive")

  //   --[no]incompatible_require_ctx_in_configure_features (a boolean; default: "true")
  @Option(
    name = "incompatible_require_ctx_in_configure_features",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, Bazel will require 'ctx' parameter in to cc_common.configure_features 
      (see https://github.com/bazelbuild/bazel/issues/7793 for more information).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRequireCtxInConfigureFeatures = Flag.Boolean("incompatibleRequireCtxInConfigureFeatures")

  //   --[no]interface_shared_objects (a boolean; default: "true")
  @Option(
    name = "interface_shared_objects",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Use interface shared objects if supported by the toolchain. All ELF toolchains 
      currently support this setting.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val interfaceSharedObjects = Flag.Boolean("interfaceSharedObjects")

  //
  //   --ios_sdk_version (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see
  // description)
  //
  @Option(
    name = "ios_sdk_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Specifies the version of the iOS SDK to use to build iOS applications. If 
      unspecified, uses default iOS SDK version from 'xcode_version'.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val iosSdkVersion = Flag.Unknown("iosSdkVersion")

  //
  //   --macos_sdk_version (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see
  // description)
  //
  @Option(
    name = "macos_sdk_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Specifies the version of the macOS SDK to use to build macOS applications. If 
      unspecified, uses default macOS SDK version from 'xcode_version'.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val macosSdkVersion = Flag.Unknown("macosSdkVersion")

  //   --minimum_os_version (a string; default: see description)
  @Option(
    name = "minimum_os_version",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """The minimum OS version which your compilation targets.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val minimumOsVersion = Flag.Str("minimumOsVersion")

  //   --platform_mappings (a relative path; default: "")
  @Option(
    name = "platform_mappings",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      The location of a mapping file that describes which platform to use if none is 
      set or which flags to set when a platform already exists. Must be relative to 
      the main workspace root. Defaults to 'platform_mappings' (a file directly under 
      the workspace root).
      """,
    valueHelp = """a relative path""",
  )
  @JvmField
  @Suppress("unused")
  val platformMappings = Flag.Unknown("platformMappings")

  //   --platforms (a build target label; default: "")
  @Option(
    name = "platforms",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      The labels of the platform rules describing the target platforms for the 
      current command.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val platforms = Flag.Label("platforms")

  //   --python2_path (a string; default: see description)
  @Option(
    name = "python2_path",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.DEPRECATED],
    help = """Deprecated, no-op. Disabled by `--incompatible_use_python_toolchains`.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val python2Path = Flag.Str("python2Path")

  //   --python3_path (a string; default: see description)
  @Option(
    name = "python3_path",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.DEPRECATED],
    help = """Deprecated, no-op. Disabled by `--incompatible_use_python_toolchains`.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val python3Path = Flag.Str("python3Path")

  //   --python_path (a string; default: see description)
  @Option(
    name = "python_path",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      The absolute path of the Python interpreter invoked to run Python targets on 
      the target platform. Deprecated; disabled by 
      --incompatible_use_python_toolchains.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val pythonPath = Flag.Str("pythonPath")

  //   --python_top (a build target label; default: see description)
  @Option(
    name = "python_top",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      The label of a py_runtime representing the Python interpreter invoked to run 
      Python targets on the target platform. Deprecated; disabled by 
      --incompatible_use_python_toolchains.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val pythonTop = Flag.Label("pythonTop")

  //   --target_platform_fallback (a string; default: "")
  @Option(
    name = "target_platform_fallback",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """This option is deprecated and has no effect.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val targetPlatformFallback = Flag.Str("targetPlatformFallback")

  //
  //   --tvos_sdk_version (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see
  // description)
  //
  @Option(
    name = "tvos_sdk_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Specifies the version of the tvOS SDK to use to build tvOS applications. If 
      unspecified, uses default tvOS SDK version from 'xcode_version'.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val tvosSdkVersion = Flag.Unknown("tvosSdkVersion")

  //
  //   --watchos_sdk_version (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see
  // description)
  //
  @Option(
    name = "watchos_sdk_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Specifies the version of the watchOS SDK to use to build watchOS applications. 
      If unspecified, uses default watchOS SDK version from 'xcode_version'.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val watchosSdkVersion = Flag.Unknown("watchosSdkVersion")

  //   --xcode_version (a string; default: see description)
  @Option(
    name = "xcode_version",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      If specified, uses Xcode of the given version for relevant build actions. If 
      unspecified, uses the executor default version of Xcode.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val xcodeVersion = Flag.Str("xcodeVersion")

  //   --xcode_version_config (a build target label; default: "@bazel_tools//tools/cpp:host_xcodes")
  @Option(
    name = "xcode_version_config",
    defaultValue = """"@bazel_tools//tools/cpp:host_xcodes"""",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      The label of the xcode_config rule to be used for selecting the Xcode version 
      in the build configuration.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val xcodeVersionConfig = Flag.Label("xcodeVersionConfig")

  //   --[no]apple_generate_dsym (a boolean; default: "false")
  @Option(
    name = "apple_generate_dsym",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.ACTION_COMMAND_LINES],
    help = """Whether to generate debug symbol(.dSYM) file(s).""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val appleGenerateDsym = Flag.Boolean("appleGenerateDsym")

  //   --[no]build (a boolean; default: "true")
  @Option(
    name = "build",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Execute the build; this is the usual behaviour. Specifying --nobuild causes the 
      build to stop before executing the build actions, returning zero iff the 
      package loading and analysis phases completed successfully; this mode is useful 
      for testing those phases.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val build = Flag.Boolean("build")

  //   --[no]build_runfile_links (a boolean; default: "true")
  @Option(
    name = "build_runfile_links",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If true, build runfiles symlink forests for all targets.  If false, write them 
      only when required by a local action, test or run command.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val buildRunfileLinks = Flag.Boolean("buildRunfileLinks")

  //   --[no]build_runfile_manifests (a boolean; default: "true")
  @Option(
    name = "build_runfile_manifests",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If true, write runfiles manifests for all targets. If false, omit them. Local 
      tests will fail to run when false.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val buildRunfileManifests = Flag.Boolean("buildRunfileManifests")

  //   --[no]build_test_dwp (a boolean; default: "false")
  @Option(
    name = "build_test_dwp",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If enabled, when building C++ tests statically and with fission the .dwp file  
      for the test binary will be automatically built as well.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val buildTestDwp = Flag.Boolean("buildTestDwp")

  //   --cc_proto_library_header_suffixes (comma-separated set of options; default: ".pb.h")
  @Option(
    name = "cc_proto_library_header_suffixes",
    defaultValue = """".pb.h"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Sets the suffixes of header files that a cc_proto_library creates.""",
    valueHelp = """comma-separated set of options""",
  )
  @JvmField
  @Suppress("unused")
  val ccProtoLibraryHeaderSuffixes = Flag.Unknown("ccProtoLibraryHeaderSuffixes")

  //   --cc_proto_library_source_suffixes (comma-separated set of options; default: ".pb.cc")
  @Option(
    name = "cc_proto_library_source_suffixes",
    defaultValue = """".pb.cc"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Sets the suffixes of source files that a cc_proto_library creates.""",
    valueHelp = """comma-separated set of options""",
  )
  @JvmField
  @Suppress("unused")
  val ccProtoLibrarySourceSuffixes = Flag.Unknown("ccProtoLibrarySourceSuffixes")

  //   --[no]experimental_proto_descriptor_sets_include_source_info (a boolean; default: "false")
  @Option(
    name = "experimental_proto_descriptor_sets_include_source_info",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Run extra actions for alternative Java api versions in a proto_library.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalProtoDescriptorSetsIncludeSourceInfo = Flag.Boolean("experimentalProtoDescriptorSetsIncludeSourceInfo")

  //   --[no]experimental_proto_extra_actions (a boolean; default: "false")
  @Option(
    name = "experimental_proto_extra_actions",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Run extra actions for alternative Java api versions in a proto_library.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalProtoExtraActions = Flag.Boolean("experimentalProtoExtraActions")

  //   --[no]experimental_save_feature_state (a boolean; default: "false")
  @Option(
    name = "experimental_save_feature_state",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Save the state of enabled and requested feautres as an output of compilation.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalSaveFeatureState = Flag.Boolean("experimentalSaveFeatureState")

  //   --[no]experimental_use_validation_aspect (a boolean; default: "false")
  @Option(
    name = "experimental_use_validation_aspect",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Whether to run validation actions using aspect (for parallelism with tests).""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseValidationAspect = Flag.Boolean("experimentalUseValidationAspect")

  //   --fission (a set of compilation modes; default: "no")
  @Option(
    name = "fission",
    defaultValue = """"no"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies which compilation modes use fission for C++ compilations and links.  
      May be any combination of {'fastbuild', 'dbg', 'opt'} or the special values 
      'yes'  to enable all modes and 'no' to disable all modes.
      """,
    valueHelp = """a set of compilation modes""",
  )
  @JvmField
  @Suppress("unused")
  val fission = Flag.Unknown("fission")

  //   --[no]incompatible_always_include_files_in_data (a boolean; default: "true")
  @Option(
    name = "incompatible_always_include_files_in_data",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, native rules add <code>DefaultInfo.files</code> of data dependencies 
      to their runfiles, which matches the recommended behavior for Starlark rules 
      (https://bazel.build/extending/rules#runfiles_features_to_avoid).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAlwaysIncludeFilesInData = Flag.Boolean("incompatibleAlwaysIncludeFilesInData")

  //   --[no]legacy_external_runfiles (a boolean; default: "true")
  @Option(
    name = "legacy_external_runfiles",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If true, build runfiles symlink forests for external repositories under 
      .runfiles/wsname/external/repo (in addition to .runfiles/repo).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val legacyExternalRunfiles = Flag.Boolean("legacyExternalRunfiles")

  //   --[no]objc_generate_linkmap (a boolean; default: "false")
  @Option(
    name = "objc_generate_linkmap",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Specifies whether to generate a linkmap file.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val objcGenerateLinkmap = Flag.Boolean("objcGenerateLinkmap")

  //   --output_groups (comma-separated list of options; may be used multiple times)
  @Option(
    name = "output_groups",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      A list of comma-separated output group names, each of which optionally prefixed 
      by a + or a -. A group prefixed by + is added to the default set of output 
      groups, while a group prefixed by - is removed from the default set. If at 
      least one group is not prefixed, the default set of output groups is omitted. 
      For example, --output_groups=+foo,+bar builds the union of the default set, 
      foo, and bar, while --output_groups=foo,bar overrides the default set such that 
      only foo and bar are built.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val outputGroups = Flag.Unknown("outputGroups")

  //   --[no]run_validations (a boolean; default: "true")
  @Option(
    name = "run_validations",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Whether to run validation actions as part of the build. See 
      https://bazel.build/extending/rules#validation_actions
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val runValidations = Flag.Boolean("runValidations")

  //   --[no]save_temps (a boolean; default: "false")
  @Option(
    name = "save_temps",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If set, temporary outputs from gcc will be saved.  These include .s files 
      (assembler code), .i files (preprocessed C) and .ii files (preprocessed C++).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val saveTemps = Flag.Boolean("saveTemps")

  //   --action_env (a 'name=value' assignment with an optional value part; may be used multiple times)
  @Option(
    name = "action_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """      
      Specifies the set of environment variables available to actions with target 
      configuration. Variables can be either specified by name, in which case the 
      value will be taken from the invocation environment, or by the name=value pair 
      which sets the value independent of the invocation environment. This option can 
      be used multiple times; for options given for the same variable, the latest 
      wins, options for different variables accumulate.
      """,
    valueHelp = """a 'name=value' assignment with an optional value part""",
  )
  @JvmField
  @Suppress("unused")
  val actionEnv = Flag.Unknown("actionEnv")

  //   --android_cpu (a string; default: "armeabi-v7a")
  @Option(
    name = "android_cpu",
    defaultValue = """"armeabi-v7a"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """The Android target CPU.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val androidCpu = Flag.Str("androidCpu")

  //   --[no]android_databinding_use_androidx (a boolean; default: "true")
  @Option(
    name = "android_databinding_use_androidx",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      Generate AndroidX-compatible data-binding files. This is only used with 
      databinding v2. This flag is a no-op.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val androidDatabindingUseAndroidx = Flag.Boolean("androidDatabindingUseAndroidx")

  //   --[no]android_databinding_use_v3_4_args (a boolean; default: "true")
  @Option(
    name = "android_databinding_use_v3_4_args",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Use android databinding v2 with 3.4.0 argument. This flag is a no-op.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val androidDatabindingUseV3_4Args = Flag.Boolean("androidDatabindingUseV3_4Args")

  //   --android_dynamic_mode (off, default or fully; default: "off")
  @Option(
    name = "android_dynamic_mode",
    defaultValue = """"off"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Determines whether C++ deps of Android rules will be linked dynamically when a 
      cc_binary does not explicitly create a shared library. 'default' means bazel 
      will choose whether to link dynamically.  'fully' means all libraries will be 
      linked dynamically. 'off' means that all libraries will be linked in mostly 
      static mode.
      """,
    valueHelp = """off, default or fully""",
  )
  @JvmField
  @Suppress("unused")
  val androidDynamicMode = Flag.OneOf("androidDynamicMode")

  //
  //   --android_manifest_merger_order (alphabetical, alphabetical_by_configuration or dependency;
  // default: "alphabetical")
  //
  @Option(
    name = "android_manifest_merger_order",
    defaultValue = """"alphabetical"""",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.EXECUTION],
    help = """      
      Sets the order of manifests passed to the manifest merger for Android binaries. 
      ALPHABETICAL means manifests are sorted by path relative to the execroot. 
      ALPHABETICAL_BY_CONFIGURATION means manifests are sorted by paths relative to 
      the configuration directory within the output directory. DEPENDENCY means 
      manifests are ordered with each library's manifest coming before the manifests 
      of its dependencies.
      """,
    valueHelp = """alphabetical, alphabetical_by_configuration or dependency""",
  )
  @JvmField
  @Suppress("unused")
  val androidManifestMergerOrder = Flag.OneOf("androidManifestMergerOrder")

  //   --[no]android_resource_shrinking (a boolean; default: "false")
  @Option(
    name = "android_resource_shrinking",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Enables resource shrinking for android_binary APKs that use ProGuard.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val androidResourceShrinking = Flag.Boolean("androidResourceShrinking")

  //   --aspects (comma-separated list of options; may be used multiple times)
  @Option(
    name = "aspects",
    allowMultiple = true,
    help = """      
      Comma-separated list of aspects to be applied to top-level targets. In the 
      list, if aspect some_aspect specifies required aspect providers via 
      required_aspect_providers, some_aspect will run after every aspect that was 
      mentioned before it in the aspects list whose advertised providers satisfy 
      some_aspect required aspect providers. Moreover, some_aspect will run after all 
      its required aspects specified by requires attribute. some_aspect will then 
      have access to the values of those aspects' providers. 
      <bzl-file-label>%<aspect_name>, for example '//tools:my_def.bzl%my_aspect', 
      where 'my_aspect' is a top-level value from a file tools/my_def.bzl
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val aspects = Flag.Unknown("aspects")

  //   --bep_maximum_open_remote_upload_files (an integer; default: "-1")
  @Option(
    name = "bep_maximum_open_remote_upload_files",
    defaultValue = """"-1"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Maximum number of open files allowed during BEP artifact upload.""",
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val bepMaximumOpenRemoteUploadFiles = Flag.Integer("bepMaximumOpenRemoteUploadFiles")

  //   --[no]build_python_zip (a tri-state (auto, yes, no); default: "auto")
  @Option(
    name = "build_python_zip",
    defaultValue = """"auto"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Build python executable zip; on on Windows, off on other platforms""",
    valueHelp = """a tri-state (auto, yes, no)""",
  )
  @JvmField
  @Suppress("unused")
  val buildPythonZip = Flag.TriState("buildPythonZip")

  //   --catalyst_cpus (comma-separated list of options; may be used multiple times)
  @Option(
    name = "catalyst_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Comma-separated list of architectures for which to build Apple Catalyst 
      binaries.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val catalystCpus = Flag.Unknown("catalystCpus")

  //   --[no]collect_code_coverage (a boolean; default: "false")
  @Option(
    name = "collect_code_coverage",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If specified, Bazel will instrument code (using offline instrumentation where 
      possible) and will collect coverage information during tests. Only targets 
      that  match --instrumentation_filter will be affected. Usually this option 
      should  not be specified directly - 'bazel coverage' command should be used 
      instead.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val collectCodeCoverage = Flag.Boolean("collectCodeCoverage")

  //   --compilation_mode [-c] (fastbuild, dbg or opt; default: "fastbuild")
  @Option(
    name = "compilation_mode",
    abbrev = 'c',
    defaultValue = """"fastbuild"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.ACTION_COMMAND_LINES],
    help = """Specify the mode the binary will be built in. Values: 'fastbuild', 'dbg', 'opt'.""",
    valueHelp = """fastbuild, dbg or opt""",
  )
  @JvmField
  @Suppress("unused")
  val compilationMode = Flag.OneOf("compilationMode")

  //   --conlyopt (a string; may be used multiple times)
  @Option(
    name = "conlyopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Additional option to pass to gcc when compiling C source files.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val conlyopt = Flag.Str("conlyopt")

  //   --copt (a string; may be used multiple times)
  @Option(
    name = "copt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Additional options to pass to gcc.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val copt = Flag.Str("copt")

  //   --cpu (a string; default: "")
  @Option(
    name = "cpu",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """The target CPU.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val cpu = Flag.Str("cpu")

  //   --cs_fdo_absolute_path (a string; default: see description)
  @Option(
    name = "cs_fdo_absolute_path",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Use CSFDO profile information to optimize compilation. Specify the absolute 
      path name of the zip file containing the profile file, a raw or an indexed LLVM 
      profile file.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val csFdoAbsolutePath = Flag.Str("csFdoAbsolutePath")

  //   --cs_fdo_instrument (a string; default: see description)
  @Option(
    name = "cs_fdo_instrument",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Generate binaries with context sensitive FDO instrumentation. With Clang/LLVM 
      compiler, it also accepts the directory name under which the raw profile 
      file(s) will be dumped at runtime.  Using this option will also add: 
      --copt=-Wno-error 
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val csFdoInstrument = Flag.Str("csFdoInstrument")

  //   --cs_fdo_profile (a build target label; default: see description)
  @Option(
    name = "cs_fdo_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      The cs_fdo_profile representing the context sensitive profile to be used for 
      optimization.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val csFdoProfile = Flag.Label("csFdoProfile")

  //   --cxxopt (a string; may be used multiple times)
  @Option(
    name = "cxxopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Additional option to pass to gcc when compiling C++ source files.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val cxxopt = Flag.Str("cxxopt")

  //   --define (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "define",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Each --define option specifies an assignment for a build variable.""",
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val define = Flag.Unknown("define")

  //   --dynamic_mode (off, default or fully; default: "default")
  @Option(
    name = "dynamic_mode",
    defaultValue = """"default"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Determines whether C++ binaries will be linked dynamically.  'default' means 
      Bazel will choose whether to link dynamically.  'fully' means all libraries 
      will be linked dynamically. 'off' means that all libraries will be linked in 
      mostly static mode.
      """,
    valueHelp = """off, default or fully""",
  )
  @JvmField
  @Suppress("unused")
  val dynamicMode = Flag.OneOf("dynamicMode")

  //   --[no]enable_fdo_profile_absolute_path (a boolean; default: "true")
  @Option(
    name = "enable_fdo_profile_absolute_path",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """If set, use of fdo_absolute_profile_path will raise an error.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val enableFdoProfileAbsolutePath = Flag.Boolean("enableFdoProfileAbsolutePath")

  //   --[no]enable_runfiles (a tri-state (auto, yes, no); default: "auto")
  @Option(
    name = "enable_runfiles",
    defaultValue = """"auto"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Enable runfiles symlink tree; By default, it's off on Windows, on on other 
      platforms.
      """,
    valueHelp = """a tri-state (auto, yes, no)""",
  )
  @JvmField
  @Suppress("unused")
  val enableRunfiles = Flag.TriState("enableRunfiles")

  //   --experimental_action_listener (a build target label; may be used multiple times)
  @Option(
    name = "experimental_action_listener",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      Deprecated in favor of aspects. Use action_listener to attach an extra_action 
      to existing build actions.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalActionListener = Flag.Label("experimentalActionListener")

  //   --[no]experimental_android_compress_java_resources (a boolean; default: "false")
  @Option(
    name = "experimental_android_compress_java_resources",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Compress Java resources in APKs""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidCompressJavaResources = Flag.Boolean("experimentalAndroidCompressJavaResources")

  //   --[no]experimental_android_databinding_v2 (a boolean; default: "true")
  @Option(
    name = "experimental_android_databinding_v2",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Use android databinding v2. This flag is a no-op.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidDatabindingV2 = Flag.Boolean("experimentalAndroidDatabindingV2")

  //   --[no]experimental_android_resource_shrinking (a boolean; default: "false")
  @Option(
    name = "experimental_android_resource_shrinking",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Enables resource shrinking for android_binary APKs that use ProGuard.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidResourceShrinking = Flag.Boolean("experimentalAndroidResourceShrinking")

  //   --[no]experimental_android_rewrite_dexes_with_rex (a boolean; default: "false")
  @Option(
    name = "experimental_android_rewrite_dexes_with_rex",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """use rex tool to rewrite dex files""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidRewriteDexesWithRex = Flag.Boolean("experimentalAndroidRewriteDexesWithRex")

  //   --[no]experimental_collect_code_coverage_for_generated_files (a boolean; default: "false")
  @Option(
    name = "experimental_collect_code_coverage_for_generated_files",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If specified, Bazel will also generate collect coverage information for 
      generated files.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectCodeCoverageForGeneratedFiles = Flag.Boolean("experimentalCollectCodeCoverageForGeneratedFiles")

  //   --[no]experimental_convenience_symlinks (normal, clean, ignore or log_only; default: "normal")
  @Option(
    name = "experimental_convenience_symlinks",
    defaultValue = """"normal"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      This flag controls how the convenience symlinks (the symlinks that appear in 
      the workspace after the build) will be managed. Possible values:  normal 
      (default): Each kind of convenience symlink will be created or deleted, as 
      determined by the build.  clean: All symlinks will be unconditionally deleted.  
      ignore: Symlinks will be left alone.  log_only: Generate log messages as if 
      'normal' were passed, but don't actually perform any filesystem operations 
      (useful for tools).Note that only symlinks whose names are generated by the 
      current value of --symlink_prefix can be affected; if the prefix changes, any 
      pre-existing symlinks will be left alone.
      """,
    valueHelp = """normal, clean, ignore or log_only""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalConvenienceSymlinks = Flag.OneOf("experimentalConvenienceSymlinks")

  //   --[no]experimental_convenience_symlinks_bep_event (a boolean; default: "false")
  @Option(
    name = "experimental_convenience_symlinks_bep_event",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      This flag controls whether or not we will post the build 
      eventConvenienceSymlinksIdentified to the BuildEventProtocol. If the value is 
      true, the BuildEventProtocol will have an entry for 
      convenienceSymlinksIdentified, listing all of the convenience symlinks created 
      in your workspace. If false, then the convenienceSymlinksIdentified entry in 
      the BuildEventProtocol will be empty.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalConvenienceSymlinksBepEvent = Flag.Boolean("experimentalConvenienceSymlinksBepEvent")

  //   --experimental_objc_fastbuild_options (comma-separated list of options; default: "-O0,-DDEBUG=1")
  @Option(
    name = "experimental_objc_fastbuild_options",
    defaultValue = """"-O0,-DDEBUG=1"""",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """Uses these strings as objc fastbuild compiler options.""",
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalObjcFastbuildOptions = Flag.Unknown("experimentalObjcFastbuildOptions")

  //   --[no]experimental_omitfp (a boolean; default: "false")
  @Option(
    name = "experimental_omitfp",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If true, use libunwind for stack unwinding, and compile with 
      -fomit-frame-pointer and -fasynchronous-unwind-tables.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalOmitfp = Flag.Boolean("experimentalOmitfp")

  //   --experimental_output_paths (off, content or strip; default: "off")
  @Option(
    name = "experimental_output_paths",
    defaultValue = """"off"""",
    effectTags =
      [
        OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
        OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION,
      ],
    help = """      
      Which model to use for where in the output tree rules write their outputs, 
      particularly for multi-platform / multi-configuration builds. This is highly 
      experimental. See https://github.com/bazelbuild/bazel/issues/6526 for details. 
      Starlark actions canopt into path mapping by adding the key 
      'supports-path-mapping' to the 'execution_requirements' dict.
      """,
    valueHelp = """off, content or strip""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalOutputPaths = Flag.OneOf("experimentalOutputPaths")

  //
  //   --experimental_override_name_platform_in_output_dir (a 'label=value' assignment; may be used
  // multiple times)
  //
  @Option(
    name = "experimental_override_name_platform_in_output_dir",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      Each entry should be of the form label=value where label refers to a platform 
      and values is the desired shortname to use in the output path. Only used when 
      --experimental_platform_in_output_dir is true. Has highest naming priority.
      """,
    valueHelp = """a 'label=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalOverrideNamePlatformInOutputDir = Flag.Unknown("experimentalOverrideNamePlatformInOutputDir")

  //   --[no]experimental_platform_in_output_dir (a boolean; default: "false")
  @Option(
    name = "experimental_platform_in_output_dir",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If true, a shortname for the target platform is used in the output directory 
      name instead of the CPU. The exact scheme is experimental and subject to 
      change: First, in the rare case the --platforms option does not have exactly 
      one value, a hash of the platforms option is used. Next, if any shortname for 
      the current platform was registered by 
      --experimental_override_name_platform_in_output_dir, then that shortname is 
      used. Then, if --experimental_use_platforms_in_output_dir_legacy_heuristic is 
      set, use a shortname based off the current platform Label. Finally, a hash of 
      the platform option is used as a last resort.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalPlatformInOutputDir = Flag.Boolean("experimentalPlatformInOutputDir")

  //   --[no]experimental_use_llvm_covmap (a boolean; default: "false")
  @Option(
    name = "experimental_use_llvm_covmap",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If specified, Bazel will generate llvm-cov coverage map information rather than 
      gcov when collect_code_coverage is enabled.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalUseLlvmCovmap = Flag.Boolean("experimentalUseLlvmCovmap")

  //   --[no]experimental_use_platforms_in_output_dir_legacy_heuristic (a boolean; default: "true")
  @Option(
    name = "experimental_use_platforms_in_output_dir_legacy_heuristic",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      Please only use this flag as part of a suggested migration or testing strategy. 
      Note that the heuristic has known deficiencies and it is suggested to migrate 
      to relying on just --experimental_override_name_platform_in_output_dir.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalUsePlatformsInOutputDirLegacyHeuristic = Flag.Boolean("experimentalUsePlatformsInOutputDirLegacyHeuristic")

  //   --fat_apk_cpu (comma-separated set of options; default: "armeabi-v7a")
  @Option(
    name = "fat_apk_cpu",
    defaultValue = """"armeabi-v7a"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Setting this option enables fat APKs, which contain native binaries for all 
      specified target architectures, e.g., --fat_apk_cpu=x86,armeabi-v7a. If this 
      flag is specified, then --android_cpu is ignored for dependencies of 
      android_binary rules.
      """,
    valueHelp = """comma-separated set of options""",
  )
  @JvmField
  @Suppress("unused")
  val fatApkCpu = Flag.Unknown("fatApkCpu")

  //   --[no]fat_apk_hwasan (a boolean; default: "false")
  @Option(
    name = "fat_apk_hwasan",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """Whether to create HWASAN splits.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val fatApkHwasan = Flag.Boolean("fatApkHwasan")

  //   --fdo_instrument (a string; default: see description)
  @Option(
    name = "fdo_instrument",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Generate binaries with FDO instrumentation. With Clang/LLVM compiler, it also 
      accepts the directory name under which the raw profile file(s) will be dumped 
      at runtime.  Using this option will also add: --copt=-Wno-error 
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val fdoInstrument = Flag.Str("fdoInstrument")

  //   --fdo_optimize (a string; default: see description)
  @Option(
    name = "fdo_optimize",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Use FDO profile information to optimize compilation. Specify the name of a zip 
      file containing a .gcda file tree, an afdo file containing an auto profile, or 
      an LLVM profile file. This flag also accepts files specified as labels (e.g. 
      `//foo/bar:file.afdo` - you may need to add an `exports_files` directive to the 
      corresponding package) and labels pointing to `fdo_profile` targets. This flag 
      will be superseded by the `fdo_profile` rule.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val fdoOptimize = Flag.Str("fdoOptimize")

  //   --fdo_prefetch_hints (a build target label; default: see description)
  @Option(
    name = "fdo_prefetch_hints",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Use cache prefetch hints.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val fdoPrefetchHints = Flag.Label("fdoPrefetchHints")

  //   --fdo_profile (a build target label; default: see description)
  @Option(
    name = "fdo_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """The fdo_profile representing the profile to be used for optimization.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val fdoProfile = Flag.Label("fdoProfile")

  //   --features (a string; may be used multiple times)
  @Option(
    name = "features",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      The given features will be enabled or disabled by default for targets built in 
      the target configuration. Specifying -<feature> will disable the feature. 
      Negative features always override positive ones. See also --host_features
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val features = Flag.Str("features")

  //   --[no]force_pic (a boolean; default: "false")
  @Option(
    name = "force_pic",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If enabled, all C++ compilations produce position-independent code ("-fPIC"), 
      links prefer PIC pre-built libraries over non-PIC libraries, and links produce 
      position-independent executables ("-pie").
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val forcePic = Flag.Boolean("forcePic")

  //
  //   --host_action_env (a 'name=value' assignment with an optional value part; may be used multiple
  // times)
  //
  @Option(
    name = "host_action_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """      
      Specifies the set of environment variables available to actions with execution 
      configurations. Variables can be either specified by name, in which case the 
      value will be taken from the invocation environment, or by the name=value pair 
      which sets the value independent of the invocation environment. This option can 
      be used multiple times; for options given for the same variable, the latest 
      wins, options for different variables accumulate.
      """,
    valueHelp = """a 'name=value' assignment with an optional value part""",
  )
  @JvmField
  @Suppress("unused")
  val hostActionEnv = Flag.Unknown("hostActionEnv")

  //   --host_compilation_mode (fastbuild, dbg or opt; default: "opt")
  @Option(
    name = "host_compilation_mode",
    defaultValue = """"opt"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.ACTION_COMMAND_LINES],
    help = """      
      Specify the mode the tools used during the build will be built in. Values: 
      'fastbuild', 'dbg', 'opt'.
      """,
    valueHelp = """fastbuild, dbg or opt""",
  )
  @JvmField
  @Suppress("unused")
  val hostCompilationMode = Flag.OneOf("hostCompilationMode")

  //   --host_conlyopt (a string; may be used multiple times)
  @Option(
    name = "host_conlyopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Additional option to pass to the C compiler when compiling C (but not C++) 
      source files in the exec configurations.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostConlyopt = Flag.Str("hostConlyopt")

  //   --host_copt (a string; may be used multiple times)
  @Option(
    name = "host_copt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Additional options to pass to the C compiler for tools built in the exec 
      configurations.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostCopt = Flag.Str("hostCopt")

  //   --host_cpu (a string; default: "")
  @Option(
    name = "host_cpu",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """The host CPU.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostCpu = Flag.Str("hostCpu")

  //   --host_cxxopt (a string; may be used multiple times)
  @Option(
    name = "host_cxxopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Additional options to pass to C++ compiler for tools built in the exec 
      configurations.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostCxxopt = Flag.Str("hostCxxopt")

  //   --host_features (a string; may be used multiple times)
  @Option(
    name = "host_features",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      The given features will be enabled or disabled by default for targets built in 
      the exec configuration. Specifying -<feature> will disable the feature. 
      Negative features always override positive ones.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostFeatures = Flag.Str("hostFeatures")

  //   --host_force_python (PY2 or PY3; default: see description)
  @Option(
    name = "host_force_python",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Overrides the Python version for the exec configuration. Can be "PY2" or "PY3".""",
    valueHelp = """PY2 or PY3""",
  )
  @JvmField
  @Suppress("unused")
  val hostForcePython = Flag.OneOf("hostForcePython")

  //   --host_linkopt (a string; may be used multiple times)
  @Option(
    name = "host_linkopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Additional option to pass to linker when linking tools in the exec 
      configurations.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostLinkopt = Flag.Str("hostLinkopt")

  //
  //   --host_macos_minimum_os (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see
  // description)
  //
  @Option(
    name = "host_macos_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Minimum compatible macOS version for host targets. If unspecified, uses 
      'macos_sdk_version'.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val hostMacosMinimumOs = Flag.Unknown("hostMacosMinimumOs")

  //
  //   --host_per_file_copt (a comma-separated list of regex expressions with prefix '-' specifying
  // excluded paths followed by an @ and a comma separated list of options; may be used multiple times)
  //
  @Option(
    name = "host_per_file_copt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Additional options to selectively pass to the C/C++ compiler when compiling 
      certain files in the exec configurations. This option can be passed multiple 
      times. Syntax: regex_filter@option_1,option_2,...,option_n. Where regex_filter 
      stands for a list of include and exclude regular expression patterns (Also see 
      --instrumentation_filter). option_1 to option_n stand for arbitrary command 
      line options. If an option contains a comma it has to be quoted with a 
      backslash. Options can contain @. Only the first @ is used to split the string. 
      Example: --host_per_file_copt=//foo/.*\.cc,-//foo/bar\.cc@-O0 adds the -O0 
      command line option to the gcc command line of all cc files in //foo/ except 
      bar.cc.
      """,
    valueHelp = """      
      a comma-separated list of regex expressions with prefix '-' specifying excluded 
      paths followed by an @ and a comma separated list of options
      """,
  )
  @JvmField
  @Suppress("unused")
  val hostPerFileCopt = Flag.Unknown("hostPerFileCopt")

  //   --host_swiftcopt (a string; may be used multiple times)
  @Option(
    name = "host_swiftcopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Additional options to pass to swiftc for exec tools.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostSwiftcopt = Flag.Str("hostSwiftcopt")

  //   --[no]incompatible_auto_exec_groups (a boolean; default: "false")
  @Option(
    name = "incompatible_auto_exec_groups",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      When enabled, an exec groups is automatically created for each toolchain used 
      by a rule. For this to work rule needs to specify `toolchain` parameter on its 
      actions. For more information, see 
      https://github.com/bazelbuild/bazel/issues/17134.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAutoExecGroups = Flag.Boolean("incompatibleAutoExecGroups")

  //   --[no]incompatible_merge_genfiles_directory (a boolean; default: "true")
  @Option(
    name = "incompatible_merge_genfiles_directory",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """If true, the genfiles directory is folded into the bin directory.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleMergeGenfilesDirectory = Flag.Boolean("incompatibleMergeGenfilesDirectory")

  //   --[no]incompatible_use_host_features (a boolean; default: "true")
  @Option(
    name = "incompatible_use_host_features",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, use --features only for the target configuration and --host_features 
      for the exec configuration.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUseHostFeatures = Flag.Boolean("incompatibleUseHostFeatures")

  //   --[no]instrument_test_targets (a boolean; default: "false")
  @Option(
    name = "instrument_test_targets",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      When coverage is enabled, specifies whether to consider instrumenting test 
      rules. When set, test rules included by --instrumentation_filter are 
      instrumented. Otherwise, test rules are always excluded from coverage 
      instrumentation.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val instrumentTestTargets = Flag.Boolean("instrumentTestTargets")

  //
  //   --instrumentation_filter (a comma-separated list of regex expressions with prefix '-' specifying
  // excluded paths; default: "-/javatests[/:],-/test/java[/:]")
  //
  @Option(
    name = "instrumentation_filter",
    defaultValue = """"-/javatests[/:],-/test/java[/:]"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      When coverage is enabled, only rules with names included by the specified 
      regex-based filter will be instrumented. Rules prefixed with '-' are excluded 
      instead. Note that only non-test rules are instrumented unless 
      --instrument_test_targets is enabled.
      """,
    valueHelp = """      
      a comma-separated list of regex expressions with prefix '-' specifying excluded 
      paths
      """,
  )
  @JvmField
  @Suppress("unused")
  val instrumentationFilter = Flag.Unknown("instrumentationFilter")

  //   --ios_minimum_os (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see description)
  @Option(
    name = "ios_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Minimum compatible iOS version for target simulators and devices. If 
      unspecified, uses 'ios_sdk_version'.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val iosMinimumOs = Flag.Unknown("iosMinimumOs")

  //   --ios_multi_cpus (comma-separated list of options; may be used multiple times)
  @Option(
    name = "ios_multi_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Comma-separated list of architectures to build an ios_application with. The 
      result is a universal binary containing all specified architectures.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val iosMultiCpus = Flag.Unknown("iosMultiCpus")

  //   --[no]legacy_whole_archive (a boolean; default: "true")
  @Option(
    name = "legacy_whole_archive",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.DEPRECATED],
    help = """      
      Deprecated, superseded by --incompatible_remove_legacy_whole_archive (see 
      https://github.com/bazelbuild/bazel/issues/7362 for details). When on, use 
      --whole-archive for cc_binary rules that have linkshared=True and either 
      linkstatic=True or '-static' in linkopts. This is for backwards compatibility 
      only. A better alternative is to use alwayslink=1 where required.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val legacyWholeArchive = Flag.Boolean("legacyWholeArchive")

  //   --linkopt (a string; may be used multiple times)
  @Option(
    name = "linkopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Additional option to pass to gcc when linking.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val linkopt = Flag.Str("linkopt")

  //   --ltobackendopt (a string; may be used multiple times)
  @Option(
    name = "ltobackendopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Additional option to pass to the LTO backend step (under --features=thin_lto).""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val ltobackendopt = Flag.Str("ltobackendopt")

  //   --ltoindexopt (a string; may be used multiple times)
  @Option(
    name = "ltoindexopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Additional option to pass to the LTO indexing step (under --features=thin_lto).""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val ltoindexopt = Flag.Str("ltoindexopt")

  //   --macos_cpus (comma-separated list of options; may be used multiple times)
  @Option(
    name = "macos_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Comma-separated list of architectures for which to build Apple macOS binaries.""",
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val macosCpus = Flag.Unknown("macosCpus")

  //
  //   --macos_minimum_os (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see
  // description)
  //
  @Option(
    name = "macos_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Minimum compatible macOS version for targets. If unspecified, uses 
      'macos_sdk_version'.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val macosMinimumOs = Flag.Unknown("macosMinimumOs")

  //   --memprof_profile (a build target label; default: see description)
  @Option(
    name = "memprof_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Use memprof profile.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val memprofProfile = Flag.Label("memprofProfile")

  //   --[no]objc_debug_with_GLIBCXX (a boolean; default: "false")
  @Option(
    name = "objc_debug_with_GLIBCXX",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """      
      If set, and compilation mode is set to 'dbg', define GLIBCXX_DEBUG,  
      GLIBCXX_DEBUG_PEDANTIC and GLIBCPP_CONCEPT_CHECKS.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val objcDebugWith_GLIBCXX = Flag.Boolean("objcDebugWith_GLIBCXX")

  //   --[no]objc_enable_binary_stripping (a boolean; default: "false")
  @Option(
    name = "objc_enable_binary_stripping",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """      
      Whether to perform symbol and dead-code strippings on linked binaries. Binary 
      strippings will be performed if both this flag and --compilation_mode=opt are 
      specified.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val objcEnableBinaryStripping = Flag.Boolean("objcEnableBinaryStripping")

  //   --objccopt (a string; may be used multiple times)
  @Option(
    name = "objccopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """Additional options to pass to gcc when compiling Objective-C/C++ source files.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val objccopt = Flag.Str("objccopt")

  //
  //   --per_file_copt (a comma-separated list of regex expressions with prefix '-' specifying excluded
  // paths followed by an @ and a comma separated list of options; may be used multiple times)
  //
  @Option(
    name = "per_file_copt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Additional options to selectively pass to gcc when compiling certain files. 
      This option can be passed multiple times. Syntax: 
      regex_filter@option_1,option_2,...,option_n. Where regex_filter stands for a 
      list of include and exclude regular expression patterns (Also see 
      --instrumentation_filter). option_1 to option_n stand for arbitrary command 
      line options. If an option contains a comma it has to be quoted with a 
      backslash. Options can contain @. Only the first @ is used to split the string. 
      Example: --per_file_copt=//foo/.*\.cc,-//foo/bar\.cc@-O0 adds the -O0 command 
      line option to the gcc command line of all cc files in //foo/ except bar.cc.
      """,
    valueHelp = """      
      a comma-separated list of regex expressions with prefix '-' specifying excluded 
      paths followed by an @ and a comma separated list of options
      """,
  )
  @JvmField
  @Suppress("unused")
  val perFileCopt = Flag.Unknown("perFileCopt")

  //
  //   --per_file_ltobackendopt (a comma-separated list of regex expressions with prefix '-' specifying
  // excluded paths followed by an @ and a comma separated list of options; may be used multiple times)
  //
  @Option(
    name = "per_file_ltobackendopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Additional options to selectively pass to LTO backend (under 
      --features=thin_lto) when compiling certain backend objects. This option can be 
      passed multiple times. Syntax: regex_filter@option_1,option_2,...,option_n. 
      Where regex_filter stands for a list of include and exclude regular expression 
      patterns. option_1 to option_n stand for arbitrary command line options. If an 
      option contains a comma it has to be quoted with a backslash. Options can 
      contain @. Only the first @ is used to split the string. Example: 
      --per_file_ltobackendopt=//foo/.*\.o,-//foo/bar\.o@-O0 adds the -O0 command 
      line option to the LTO backend command line of all o files in //foo/ except 
      bar.o.
      """,
    valueHelp = """      
      a comma-separated list of regex expressions with prefix '-' specifying excluded 
      paths followed by an @ and a comma separated list of options
      """,
  )
  @JvmField
  @Suppress("unused")
  val perFileLtobackendopt = Flag.Unknown("perFileLtobackendopt")

  //   --platform_suffix (a string; default: see description)
  @Option(
    name = "platform_suffix",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Specifies a suffix to be added to the configuration directory.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val platformSuffix = Flag.Str("platformSuffix")

  //   --propeller_optimize (a build target label; default: see description)
  @Option(
    name = "propeller_optimize",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Use Propeller profile information to optimize the build target.A propeller 
      profile must consist of at least one of two files, a cc profile and a ld 
      profile.  This flag accepts a build label which must refer to the propeller 
      profile input files. For example, the BUILD file that defines the label, in 
      a/b/BUILD:propeller_optimize(    name = "propeller_profile",    cc_profile = 
      "propeller_cc_profile.txt",    ld_profile = "propeller_ld_profile.txt",)An 
      exports_files directive may have to be added to the corresponding package to 
      make these files visible to Bazel. The option must be used as: 
      --propeller_optimize=//a/b:propeller_profile
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val propellerOptimize = Flag.Label("propellerOptimize")

  //   --propeller_optimize_absolute_cc_profile (a string; default: see description)
  @Option(
    name = "propeller_optimize_absolute_cc_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Absolute path name of cc_profile file for Propeller Optimized builds.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val propellerOptimizeAbsoluteCcProfile = Flag.Str("propellerOptimizeAbsoluteCcProfile")

  //   --propeller_optimize_absolute_ld_profile (a string; default: see description)
  @Option(
    name = "propeller_optimize_absolute_ld_profile",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Absolute path name of ld_profile file for Propeller Optimized builds.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val propellerOptimizeAbsoluteLdProfile = Flag.Str("propellerOptimizeAbsoluteLdProfile")

  //   --remote_download_all
  @Option(
    name = "remote_download_all",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    expandsTo = [ "--remote_download_outputs=all"],
    help = """      
      Downloads all remote outputs to the local machine. This flag is an alias for 
      --remote_download_outputs=all.
      """,
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadAll = Flag.Unknown("remoteDownloadAll")

  //   --remote_download_minimal
  @Option(
    name = "remote_download_minimal",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    expandsTo = [ "--remote_download_outputs=minimal"],
    help = """      
      Does not download any remote build outputs to the local machine. This flag is 
      an alias for --remote_download_outputs=minimal.
      """,
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadMinimal = Flag.Unknown("remoteDownloadMinimal")

  //   --remote_download_outputs (all, minimal or toplevel; default: "toplevel")
  @Option(
    name = "remote_download_outputs",
    defaultValue = """"toplevel"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If set to 'minimal' doesn't download any remote build outputs to the local 
      machine, except the ones required by local actions. If set to 'toplevel' 
      behaves like'minimal' except that it also downloads outputs of top level 
      targets to the local machine. Both options can significantly reduce build times 
      if network bandwidth is a bottleneck.
      """,
    valueHelp = """all, minimal or toplevel""",
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadOutputs = Flag.OneOf("remoteDownloadOutputs")

  //   --remote_download_symlink_template (a string; default: "")
  @Option(
    name = "remote_download_symlink_template",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Instead of downloading remote build outputs to the local machine, create 
      symbolic links. The target of the symbolic links can be specified in the form 
      of a template string. This template string may contain {hash} and {size_bytes} 
      that expand to the hash of the object and the size in bytes, respectively. 
      These symbolic links may, for example, point to a FUSE file system that loads 
      objects from the CAS on demand.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadSymlinkTemplate = Flag.Str("remoteDownloadSymlinkTemplate")

  //   --remote_download_toplevel
  @Option(
    name = "remote_download_toplevel",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    expandsTo = [ "--remote_download_outputs=toplevel"],
    help = """      
      Only downloads remote outputs of top level targets to the local machine. This 
      flag is an alias for --remote_download_outputs=toplevel.
      """,
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadToplevel = Flag.Unknown("remoteDownloadToplevel")

  //   --repo_env (a 'name=value' assignment with an optional value part; may be used multiple times)
  @Option(
    name = "repo_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """      
      Specifies additional environment variables to be available only for repository 
      rules. Note that repository rules see the full environment anyway, but in this 
      way configuration information can be passed to repositories through options 
      without invalidating the action graph.
      """,
    valueHelp = """a 'name=value' assignment with an optional value part""",
  )
  @JvmField
  @Suppress("unused")
  val repoEnv = Flag.Unknown("repoEnv")

  //   --run_under (a prefix in front of command; default: see description)
  @Option(
    name = "run_under",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """      
      Prefix to insert before the executables for the 'test' and 'run' commands. If 
      the value is 'foo -bar', and the execution command line is 'test_binary -baz', 
      then the final command line is 'foo -bar test_binary -baz'.This can also be a 
      label to an executable target. Some examples are: 'valgrind', 'strace', 'strace 
      -c', 'valgrind --quiet --num-callers=20', '//package:target',  
      '//package:target --options'.
      """,
    valueHelp = """a prefix in front of command""",
  )
  @JvmField
  @Suppress("unused")
  val runUnder = Flag.Unknown("runUnder")

  //   --[no]share_native_deps (a boolean; default: "true")
  @Option(
    name = "share_native_deps",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If true, native libraries that contain identical functionality will be shared 
      among different targets
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val shareNativeDeps = Flag.Boolean("shareNativeDeps")

  //   --[no]stamp (a boolean; default: "false")
  @Option(
    name = "stamp",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Stamp binaries with the date, username, hostname, workspace information, etc.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val stamp = Flag.Boolean("stamp")

  //   --strip (always, sometimes or never; default: "sometimes")
  @Option(
    name = "strip",
    defaultValue = """"sometimes"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies whether to strip binaries and shared libraries  (using 
      "-Wl,--strip-debug").  The default value of 'sometimes' means strip iff 
      --compilation_mode=fastbuild.
      """,
    valueHelp = """always, sometimes or never""",
  )
  @JvmField
  @Suppress("unused")
  val strip = Flag.OneOf("strip")

  //   --stripopt (a string; may be used multiple times)
  @Option(
    name = "stripopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Additional options to pass to strip when generating a '<name>.stripped' binary.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val stripopt = Flag.Str("stripopt")

  //   --swiftcopt (a string; may be used multiple times)
  @Option(
    name = "swiftcopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """Additional options to pass to Swift compilation.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val swiftcopt = Flag.Str("swiftcopt")

  //   --symlink_prefix (a string; default: see description)
  @Option(
    name = "symlink_prefix",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      The prefix that is prepended to any of the convenience symlinks that are 
      created after a build. If omitted, the default value is the name of the build 
      tool followed by a hyphen. If '/' is passed, then no symlinks are created and 
      no warning is emitted. Warning: the special functionality for '/' will be 
      deprecated soon; use --experimental_convenience_symlinks=ignore instead.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val symlinkPrefix = Flag.Str("symlinkPrefix")

  //   --tvos_cpus (comma-separated list of options; may be used multiple times)
  @Option(
    name = "tvos_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Comma-separated list of architectures for which to build Apple tvOS binaries.""",
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val tvosCpus = Flag.Unknown("tvosCpus")

  //
  //   --tvos_minimum_os (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see
  // description)
  //
  @Option(
    name = "tvos_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Minimum compatible tvOS version for target simulators and devices. If 
      unspecified, uses 'tvos_sdk_version'.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val tvosMinimumOs = Flag.Unknown("tvosMinimumOs")

  //   --visionos_cpus (comma-separated list of options; may be used multiple times)
  @Option(
    name = "visionos_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Comma-separated list of architectures for which to build Apple visionOS 
      binaries.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val visionosCpus = Flag.Unknown("visionosCpus")

  //   --watchos_cpus (comma-separated list of options; may be used multiple times)
  @Option(
    name = "watchos_cpus",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Comma-separated list of architectures for which to build Apple watchOS binaries.""",
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val watchosCpus = Flag.Unknown("watchosCpus")

  //
  //   --watchos_minimum_os (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see
  // description)
  //
  @Option(
    name = "watchos_minimum_os",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      Minimum compatible watchOS version for target simulators and devices. If 
      unspecified, uses 'watchos_sdk_version'.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val watchosMinimumOs = Flag.Unknown("watchosMinimumOs")

  //   --xbinary_fdo (a build target label; default: see description)
  @Option(
    name = "xbinary_fdo",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Use XbinaryFDO profile information to optimize compilation. Specify the name of 
      default cross binary profile. When the option is used together with 
      --fdo_instrument/--fdo_optimize/--fdo_profile, those options will always 
      prevail as if xbinary_fdo is never specified. 
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val xbinaryFdo = Flag.Label("xbinaryFdo")

  //   --auto_cpu_environment_group (a build target label; default: "")
  @Option(
    name = "auto_cpu_environment_group",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      Declare the environment_group to use for automatically mapping cpu values to 
      target_environment values.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val autoCpuEnvironmentGroup = Flag.Label("autoCpuEnvironmentGroup")

  //   --[no]check_bzl_visibility (a boolean; default: "true")
  @Option(
    name = "check_bzl_visibility",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """If disabled, .bzl load visibility errors are demoted to warnings.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val checkBzlVisibility = Flag.Boolean("checkBzlVisibility")

  //   --[no]check_licenses (a boolean; default: "false")
  @Option(
    name = "check_licenses",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """      
      Check that licensing constraints imposed by dependent packages do not conflict 
      with distribution modes of the targets being built. By default, licenses are 
      not checked.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val checkLicenses = Flag.Boolean("checkLicenses")

  //   --[no]check_visibility (a boolean; default: "true")
  @Option(
    name = "check_visibility",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """If disabled, visibility errors in target dependencies are demoted to warnings.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val checkVisibility = Flag.Boolean("checkVisibility")

  //   --[no]desugar_for_android (a boolean; default: "true")
  @Option(
    name = "desugar_for_android",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """Whether to desugar Java 8 bytecode before dexing.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val desugarForAndroid = Flag.Boolean("desugarForAndroid")

  //   --[no]desugar_java8_libs (a boolean; default: "false")
  @Option(
    name = "desugar_java8_libs",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Whether to include supported Java 8 libraries in apps for legacy devices.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val desugarJava8Libs = Flag.Boolean("desugarJava8Libs")

  //   --[no]enforce_constraints (a boolean; default: "true")
  @Option(
    name = "enforce_constraints",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """      
      Checks the environments each target is compatible with and reports errors if 
      any target has dependencies that don't support the same environments
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val enforceConstraints = Flag.Boolean("enforceConstraints")

  //   --[no]experimental_check_desugar_deps (a boolean; default: "true")
  @Option(
    name = "experimental_check_desugar_deps",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Whether to double-check correct desugaring at Android binary level.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCheckDesugarDeps = Flag.Boolean("experimentalCheckDesugarDeps")

  //   --[no]experimental_docker_privileged (a boolean; default: "false")
  @Option(
    name = "experimental_docker_privileged",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If enabled, Bazel will pass the --privileged flag to 'docker run' when running 
      actions. This might be required by your build, but it might also result in 
      reduced hermeticity.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDockerPrivileged = Flag.Boolean("experimentalDockerPrivileged")

  //   --experimental_import_deps_checking (off, warning or error; default: "OFF")
  @Option(
    name = "experimental_import_deps_checking",
    defaultValue = """"OFF"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      When enabled, check whether the dependencies of an aar_import are complete. 
      This enforcement can break the build, or can just result in warnings.
      """,
    valueHelp = """off, warning or error""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalImportDepsChecking = Flag.OneOf("experimentalImportDepsChecking")

  //   --experimental_one_version_enforcement (off, warning or error; default: "OFF")
  @Option(
    name = "experimental_one_version_enforcement",
    defaultValue = """"OFF"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      When enabled, enforce that a java_binary rule can't contain more than one 
      version of the same class file on the classpath. This enforcement can break the 
      build, or can just result in warnings.
      """,
    valueHelp = """off, warning or error""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalOneVersionEnforcement = Flag.OneOf("experimentalOneVersionEnforcement")

  //   --[no]experimental_sandboxfs_map_symlink_targets (a boolean; default: "false")
  @Option(
    name = "experimental_sandboxfs_map_symlink_targets",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION],
    help = """No-op""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalSandboxfsMapSymlinkTargets = Flag.Boolean("experimentalSandboxfsMapSymlinkTargets")

  //   --experimental_strict_java_deps (off, warn, error, strict or default; default: "default")
  @Option(
    name = "experimental_strict_java_deps",
    defaultValue = """"default"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      If true, checks that a Java target explicitly declares all directly used 
      targets as dependencies.
      """,
    valueHelp = """off, warn, error, strict or default""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalStrictJavaDeps = Flag.OneOf("experimentalStrictJavaDeps")

  //   --[no]incompatible_check_testonly_for_output_files (a boolean; default: "false")
  @Option(
    name = "incompatible_check_testonly_for_output_files",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If enabled, check testonly for prerequisite targets that are output files by 
      looking up the testonly of the generating rule. This matches visibility 
      checking.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleCheckTestonlyForOutputFiles = Flag.Boolean("incompatibleCheckTestonlyForOutputFiles")

  //   --[no]incompatible_check_visibility_for_toolchains (a boolean; default: "false")
  @Option(
    name = "incompatible_check_visibility_for_toolchains",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """If enabled, visibility checking also applies to toolchain implementations.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleCheckVisibilityForToolchains = Flag.Boolean("incompatibleCheckVisibilityForToolchains")

  //   --[no]incompatible_disable_native_android_rules (a boolean; default: "false")
  @Option(
    name = "incompatible_disable_native_android_rules",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If enabled, direct usage of the native Android rules is disabled. Please use 
      the Starlark Android rules from https://github.com/bazelbuild/rules_android
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableNativeAndroidRules = Flag.Boolean("incompatibleDisableNativeAndroidRules")

  //   --[no]incompatible_disable_native_apple_binary_rule (a boolean; default: "false")
  @Option(
    name = "incompatible_disable_native_apple_binary_rule",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """No-op. Kept here for backwards compatibility.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableNativeAppleBinaryRule = Flag.Boolean("incompatibleDisableNativeAppleBinaryRule")

  //   --[no]incompatible_legacy_local_fallback (a boolean; default: "false")
  @Option(
    name = "incompatible_legacy_local_fallback",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, enables the legacy implicit fallback from sandboxed to local 
      strategy. This flag will eventually default to false and then become a no-op. 
      Use --strategy, --spawn_strategy, or --dynamic_local_strategy to configure 
      fallbacks instead.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleLegacyLocalFallback = Flag.Boolean("incompatibleLegacyLocalFallback")

  //   --[no]incompatible_python_disable_py2 (a boolean; default: "true")
  @Option(
    name = "incompatible_python_disable_py2",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, using Python 2 settings will cause an error. This includes 
      python_version=PY2, srcs_version=PY2, and srcs_version=PY2ONLY. See 
      https://github.com/bazelbuild/bazel/issues/15684 for more information.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePythonDisablePy2 = Flag.Boolean("incompatiblePythonDisablePy2")

  //   --[no]incompatible_validate_top_level_header_inclusions (a boolean; default: "true")
  @Option(
    name = "incompatible_validate_top_level_header_inclusions",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, Bazel will also validate top level directory header inclusions (see 
      https://github.com/bazelbuild/bazel/issues/10047 for more information).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleValidateTopLevelHeaderInclusions = Flag.Boolean("incompatibleValidateTopLevelHeaderInclusions")

  //   --[no]one_version_enforcement_on_java_tests (a boolean; default: "true")
  @Option(
    name = "one_version_enforcement_on_java_tests",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      When enabled, and with experimental_one_version_enforcement set to a non-NONE 
      value, enforce one version on java_test targets. This flag can be disabled to 
      improve incremental test performance at the expense of missing potential one 
      version violations.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val oneVersionEnforcementOnJavaTests = Flag.Boolean("oneVersionEnforcementOnJavaTests")

  //   --python_native_rules_allowlist (a build target label; default: see description)
  @Option(
    name = "python_native_rules_allowlist",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      An allowlist (package_group target) to use when enforcing 
      --incompatible_python_disallow_native_rules.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val pythonNativeRulesAllowlist = Flag.Label("pythonNativeRulesAllowlist")

  //   --sandbox_add_mount_pair (a single path or a 'source:target' pair; may be used multiple times)
  @Option(
    name = "sandbox_add_mount_pair",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """Add additional path pair to mount in sandbox.""",
    valueHelp = """a single path or a 'source:target' pair""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxAddMountPair = Flag.Unknown("sandboxAddMountPair")

  //   --sandbox_block_path (a string; may be used multiple times)
  @Option(
    name = "sandbox_block_path",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """For sandboxed actions, disallow access to this path.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxBlockPath = Flag.Str("sandboxBlockPath")

  //   --[no]sandbox_default_allow_network (a boolean; default: "true")
  @Option(
    name = "sandbox_default_allow_network",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Allow network access by default for actions; this may not work with all 
      sandboxing implementations.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxDefaultAllowNetwork = Flag.Boolean("sandboxDefaultAllowNetwork")

  //   --[no]sandbox_fake_hostname (a boolean; default: "false")
  @Option(
    name = "sandbox_fake_hostname",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """Change the current hostname to 'localhost' for sandboxed actions.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxFakeHostname = Flag.Boolean("sandboxFakeHostname")

  //   --[no]sandbox_fake_username (a boolean; default: "false")
  @Option(
    name = "sandbox_fake_username",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """Change the current username to 'nobody' for sandboxed actions.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxFakeUsername = Flag.Boolean("sandboxFakeUsername")

  //   --sandbox_writable_path (a string; may be used multiple times)
  @Option(
    name = "sandbox_writable_path",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      For sandboxed actions, make an existing directory writable in the sandbox (if 
      supported by the sandboxing implementation, ignored otherwise).
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxWritablePath = Flag.Str("sandboxWritablePath")

  //   --[no]strict_filesets (a boolean; default: "false")
  @Option(
    name = "strict_filesets",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      If this option is enabled, filesets crossing package boundaries are reported as 
      errors.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val strictFilesets = Flag.Boolean("strictFilesets")

  //   --strict_proto_deps (off, warn, error, strict or default; default: "error")
  @Option(
    name = "strict_proto_deps",
    defaultValue = """"error"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      Unless OFF, checks that a proto_library target explicitly declares all directly 
      used targets as dependencies.
      """,
    valueHelp = """off, warn, error, strict or default""",
  )
  @JvmField
  @Suppress("unused")
  val strictProtoDeps = Flag.OneOf("strictProtoDeps")

  //   --strict_public_imports (off, warn, error, strict or default; default: "off")
  @Option(
    name = "strict_public_imports",
    defaultValue = """"off"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      Unless OFF, checks that a proto_library target explicitly declares all targets 
      used in 'import public' as exported.
      """,
    valueHelp = """off, warn, error, strict or default""",
  )
  @JvmField
  @Suppress("unused")
  val strictPublicImports = Flag.OneOf("strictPublicImports")

  //   --[no]strict_system_includes (a boolean; default: "false")
  @Option(
    name = "strict_system_includes",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      If true, headers found through system include paths (-isystem) are also 
      required to be declared.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val strictSystemIncludes = Flag.Boolean("strictSystemIncludes")

  //   --target_environment (a build target label; may be used multiple times)
  @Option(
    name = "target_environment",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      Declares this build's target environment. Must be a label reference to an 
      "environment" rule. If specified, all top-level targets must be compatible with 
      this environment.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val targetEnvironment = Flag.Label("targetEnvironment")

  //   --apk_signing_method (v1, v2, v1_v2 or v4; default: "v1_v2")
  @Option(
    name = "apk_signing_method",
    defaultValue = """"v1_v2"""",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Implementation to use to sign APKs""",
    valueHelp = """v1, v2, v1_v2 or v4""",
  )
  @JvmField
  @Suppress("unused")
  val apkSigningMethod = Flag.OneOf("apkSigningMethod")

  //   --[no]device_debug_entitlements (a boolean; default: "true")
  @Option(
    name = "device_debug_entitlements",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      If set, and compilation mode is not 'opt', objc apps will include debug 
      entitlements when signing.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val deviceDebugEntitlements = Flag.Boolean("deviceDebugEntitlements")

  //   --ios_signing_cert_name (a string; default: see description)
  @Option(
    name = "ios_signing_cert_name",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """      
      Certificate name to use for iOS signing. If not set will fall back to 
      provisioning profile. May be the certificate's keychain identity preference or 
      (substring) of the certificate's common name, as per codesign's man page 
      (SIGNING IDENTITIES).
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val iosSigningCertName = Flag.Str("iosSigningCertName")

  //   --[no]enable_bzlmod (a boolean; default: "true")
  @Option(
    name = "enable_bzlmod",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If true, enables the Bzlmod dependency management system, taking precedence 
      over WORKSPACE. See https://bazel.build/docs/bzlmod for more information.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val enableBzlmod = Flag.Boolean("enableBzlmod")

  //   --[no]enable_workspace (a boolean; default: "true")
  @Option(
    name = "enable_workspace",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If true, enables the legacy WORKSPACE system for external dependencies. See 
      https://bazel.build/external/overview for more information.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val enableWorkspace = Flag.Boolean("enableWorkspace")

  //   --[no]experimental_action_resource_set (a boolean; default: "true")
  @Option(
    name = "experimental_action_resource_set",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION, OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If set to true, ctx.actions.run() and ctx.actions.run_shell() accept a 
      resource_set parameter for local execution. Otherwise it will default to 250 MB 
      for memory and 1 cpu.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalActionResourceSet = Flag.Boolean("experimentalActionResourceSet")

  //   --[no]experimental_bzl_visibility (a boolean; default: "true")
  @Option(
    name = "experimental_bzl_visibility",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If enabled, adds a `visibility()` function that .bzl files may call during 
      top-level evaluation to set their visibility for the purpose of load() 
      statements.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalBzlVisibility = Flag.Boolean("experimentalBzlVisibility")

  //   --[no]experimental_cc_shared_library (a boolean; default: "false")
  @Option(
    name = "experimental_cc_shared_library",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If set to true, rule attributes and Starlark API methods needed for the rule 
      cc_shared_library will be available
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCcSharedLibrary = Flag.Boolean("experimentalCcSharedLibrary")

  //   --[no]experimental_disable_external_package (a boolean; default: "false")
  @Option(
    name = "experimental_disable_external_package",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If set to true, the auto-generated //external package will not be available 
      anymore. Bazel will still be unable to parse the file 'external/BUILD', but 
      globs reaching into external/ from the unnamed package will work.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDisableExternalPackage = Flag.Boolean("experimentalDisableExternalPackage")

  //   --[no]experimental_enable_android_migration_apis (a boolean; default: "false")
  @Option(
    name = "experimental_enable_android_migration_apis",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """      
      If set to true, enables the APIs required to support the Android Starlark 
      migration.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableAndroidMigrationApis = Flag.Boolean("experimentalEnableAndroidMigrationApis")

  //   --[no]experimental_enable_scl_dialect (a boolean; default: "false")
  @Option(
    name = "experimental_enable_scl_dialect",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """If set to true, .scl files may be used in load() statements.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalEnableSclDialect = Flag.Boolean("experimentalEnableSclDialect")

  //   --[no]experimental_google_legacy_api (a boolean; default: "false")
  @Option(
    name = "experimental_google_legacy_api",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If set to true, exposes a number of experimental pieces of Starlark build API 
      pertaining to Google legacy code.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalGoogleLegacyApi = Flag.Boolean("experimentalGoogleLegacyApi")

  //   --[no]experimental_isolated_extension_usages (a boolean; default: "false")
  @Option(
    name = "experimental_isolated_extension_usages",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If true, enables the <code>isolate</code> parameter in the <a 
      href="https://bazel.build/rules/lib/globals/module#use_extension"><code>use_extension</code></a> 
      function.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalIsolatedExtensionUsages = Flag.Boolean("experimentalIsolatedExtensionUsages")

  //   --[no]experimental_java_library_export (a boolean; default: "false")
  @Option(
    name = "experimental_java_library_export",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """If enabled, experimental_java_library_export_do_not_use module is available.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalJavaLibraryExport = Flag.Boolean("experimentalJavaLibraryExport")

  //   --[no]experimental_platforms_api (a boolean; default: "false")
  @Option(
    name = "experimental_platforms_api",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If set to true, enables a number of platform-related Starlark APIs useful for 
      debugging.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalPlatformsApi = Flag.Boolean("experimentalPlatformsApi")

  //   --[no]experimental_repo_remote_exec (a boolean; default: "false")
  @Option(
    name = "experimental_repo_remote_exec",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """If set to true, repository_rule gains some remote execution capabilities.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRepoRemoteExec = Flag.Boolean("experimentalRepoRemoteExec")

  //   --[no]experimental_sibling_repository_layout (a boolean; default: "false")
  @Option(
    name = "experimental_sibling_repository_layout",
    defaultValue = """"false"""",
    effectTags =
      [
        OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
        OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE,
      ],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If set to true, non-main repositories are planted as symlinks to the main 
      repository in the execution root. That is, all repositories are direct children 
      of the ${'$'}output_base/execution_root directory. This has the side effect of 
      freeing up ${'$'}output_base/execution_root/__main__/external for the real 
      top-level 'external' directory.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalSiblingRepositoryLayout = Flag.Boolean("experimentalSiblingRepositoryLayout")

  //   --[no]incompatible_allow_tags_propagation (a boolean; default: "true")
  @Option(
    name = "incompatible_allow_tags_propagation",
    oldName = "experimental_allow_tags_propagation",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If set to true, tags will be propagated from a target to the actions' execution 
      requirements; otherwise tags are not propagated. See 
      https://github.com/bazelbuild/bazel/issues/8830 for details.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAllowTagsPropagation = Flag.Boolean("incompatibleAllowTagsPropagation")

  //   --[no]incompatible_always_check_depset_elements (a boolean; default: "true")
  @Option(
    name = "incompatible_always_check_depset_elements",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      Check the validity of elements added to depsets, in all constructors. Elements 
      must be immutable, but historically the depset(direct=...) constructor forgot 
      to check. Use tuples instead of lists in depset elements. See 
      https://github.com/bazelbuild/bazel/issues/10313 for details.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleAlwaysCheckDepsetElements = Flag.Boolean("incompatibleAlwaysCheckDepsetElements")

  //   --[no]incompatible_config_setting_private_default_visibility (a boolean; default: "false")
  @Option(
    name = "incompatible_config_setting_private_default_visibility",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If incompatible_enforce_config_setting_visibility=false, this is a noop. Else, 
      if this flag is false, any config_setting without an explicit visibility 
      attribute is //visibility:public. If this flag is true, config_setting follows 
      the same visibility logic as all other rules. See 
      https://github.com/bazelbuild/bazel/issues/12933.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleConfigSettingPrivateDefaultVisibility = Flag.Boolean("incompatibleConfigSettingPrivateDefaultVisibility")

  //   --[no]incompatible_depset_for_java_output_source_jars (a boolean; default: "true")
  @Option(
    name = "incompatible_depset_for_java_output_source_jars",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      When true, Bazel no longer returns a list from 
      java_info.java_output[0].source_jars but returns a depset instead.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDepsetForJavaOutputSourceJars = Flag.Boolean("incompatibleDepsetForJavaOutputSourceJars")

  //   --[no]incompatible_depset_for_libraries_to_link_getter (a boolean; default: "true")
  @Option(
    name = "incompatible_depset_for_libraries_to_link_getter",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      When true, Bazel no longer returns a list from 
      linking_context.libraries_to_link but returns a depset instead.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDepsetForLibrariesToLinkGetter = Flag.Boolean("incompatibleDepsetForLibrariesToLinkGetter")

  //   --[no]incompatible_disable_objc_library_transition (a boolean; default: "true")
  @Option(
    name = "incompatible_disable_objc_library_transition",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      Disable objc_library's custom transition and inherit from the top level target 
      instead
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableObjcLibraryTransition = Flag.Boolean("incompatibleDisableObjcLibraryTransition")

  //   --[no]incompatible_disable_starlark_host_transitions (a boolean; default: "false")
  @Option(
    name = "incompatible_disable_starlark_host_transitions",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, rule attributes cannot set 'cfg = "host"'. Rules should set 
      'cfg = "exec"' instead.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableStarlarkHostTransitions = Flag.Boolean("incompatibleDisableStarlarkHostTransitions")

  //   --[no]incompatible_disable_target_provider_fields (a boolean; default: "false")
  @Option(
    name = "incompatible_disable_target_provider_fields",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, disable the ability to access providers on 'target' objects via 
      field syntax. Use provider-key syntax instead. For example, instead of using 
      `ctx.attr.dep.my_info` to access `my_info` from inside a rule implementation 
      function, use `ctx.attr.dep[MyInfo]`. See 
      https://github.com/bazelbuild/bazel/issues/9014 for details.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableTargetProviderFields = Flag.Boolean("incompatibleDisableTargetProviderFields")

  //   --[no]incompatible_disallow_empty_glob (a boolean; default: "false")
  @Option(
    name = "incompatible_disallow_empty_glob",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, the default value of the `allow_empty` argument of glob() is 
      False.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowEmptyGlob = Flag.Boolean("incompatibleDisallowEmptyGlob")

  //   --[no]incompatible_disallow_legacy_py_provider (a boolean; default: "true")
  @Option(
    name = "incompatible_disallow_legacy_py_provider",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """No-op, will be removed soon.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowLegacyPyProvider = Flag.Boolean("incompatibleDisallowLegacyPyProvider")

  //   --[no]incompatible_disallow_sdk_frameworks_attributes (a boolean; default: "false")
  @Option(
    name = "incompatible_disallow_sdk_frameworks_attributes",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, disallow sdk_frameworks and weak_sdk_frameworks attributes in 
      objc_library andobjc_import.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowSdkFrameworksAttributes = Flag.Boolean("incompatibleDisallowSdkFrameworksAttributes")

  //   --[no]incompatible_disallow_struct_provider_syntax (a boolean; default: "false")
  @Option(
    name = "incompatible_disallow_struct_provider_syntax",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, rule implementation functions may not return a struct. They 
      must instead return a list of provider instances.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowStructProviderSyntax = Flag.Boolean("incompatibleDisallowStructProviderSyntax")

  //   --[no]incompatible_enable_deprecated_label_apis (a boolean; default: "true")
  @Option(
    name = "incompatible_enable_deprecated_label_apis",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If enabled, certain deprecated APIs (native.repository_name, 
      Label.workspace_name, Label.relative) can be used.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnableDeprecatedLabelApis = Flag.Boolean("incompatibleEnableDeprecatedLabelApis")

  //   --[no]incompatible_enforce_config_setting_visibility (a boolean; default: "true")
  @Option(
    name = "incompatible_enforce_config_setting_visibility",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, enforce config_setting visibility restrictions. If false, every 
      config_setting is visible to every target. See 
      https://github.com/bazelbuild/bazel/issues/12932.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleEnforceConfigSettingVisibility = Flag.Boolean("incompatibleEnforceConfigSettingVisibility")

  //   --[no]incompatible_existing_rules_immutable_view (a boolean; default: "true")
  @Option(
    name = "incompatible_existing_rules_immutable_view",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, native.existing_rule and native.existing_rules return 
      lightweight immutable view objects instead of mutable dicts.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleExistingRulesImmutableView = Flag.Boolean("incompatibleExistingRulesImmutableView")

  //   --[no]incompatible_fail_on_unknown_attributes (a boolean; default: "true")
  @Option(
    name = "incompatible_fail_on_unknown_attributes",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """If enabled, targets that have unknown attributes set to None fail.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleFailOnUnknownAttributes = Flag.Boolean("incompatibleFailOnUnknownAttributes")

  //   --[no]incompatible_fix_package_group_reporoot_syntax (a boolean; default: "true")
  @Option(
    name = "incompatible_fix_package_group_reporoot_syntax",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      In package_group's `packages` attribute, changes the meaning of the value 
      "//..." to refer to all packages in the current repository instead of all 
      packages in any repository. You can use the special value "public" in place of 
      "//..." to obtain the old behavior. This flag requires that 
      --incompatible_package_group_has_public_syntax also be enabled.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleFixPackageGroupReporootSyntax = Flag.Boolean("incompatibleFixPackageGroupReporootSyntax")

  //   --[no]incompatible_java_common_parameters (a boolean; default: "true")
  @Option(
    name = "incompatible_java_common_parameters",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, the output_jar, and host_javabase parameters in pack_sources 
      and host_javabase in compile will all be removed.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleJavaCommonParameters = Flag.Boolean("incompatibleJavaCommonParameters")

  //   --[no]incompatible_merge_fixed_and_default_shell_env (a boolean; default: "true")
  @Option(
    name = "incompatible_merge_fixed_and_default_shell_env",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If enabled, actions registered with ctx.actions.run and ctx.actions.run_shell 
      with both 'env' and 'use_default_shell_env = True' specified will use an 
      environment obtained from the default shell environment by overriding with the 
      values passed in to 'env'. If disabled, the value of 'env' is completely 
      ignored in this case.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleMergeFixedAndDefaultShellEnv = Flag.Boolean("incompatibleMergeFixedAndDefaultShellEnv")

  //   --[no]incompatible_new_actions_api (a boolean; default: "true")
  @Option(
    name = "incompatible_new_actions_api",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, the API to create actions is only available on `ctx.actions`, 
      not on `ctx`.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNewActionsApi = Flag.Boolean("incompatibleNewActionsApi")

  //   --[no]incompatible_no_attr_license (a boolean; default: "true")
  @Option(
    name = "incompatible_no_attr_license",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """If set to true, disables the function `attr.license`.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNoAttrLicense = Flag.Boolean("incompatibleNoAttrLicense")

  //   --[no]incompatible_no_implicit_file_export (a boolean; default: "false")
  @Option(
    name = "incompatible_no_implicit_file_export",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set, (used) source files are are package private unless exported explicitly. 
      See 
      https://github.com/bazelbuild/proposals/blob/master/designs/2019-10-24-file-visibility.md
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNoImplicitFileExport = Flag.Boolean("incompatibleNoImplicitFileExport")

  //   --[no]incompatible_no_rule_outputs_param (a boolean; default: "false")
  @Option(
    name = "incompatible_no_rule_outputs_param",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, disables the `outputs` parameter of the `rule()` Starlark 
      function.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleNoRuleOutputsParam = Flag.Boolean("incompatibleNoRuleOutputsParam")

  //   --[no]incompatible_objc_alwayslink_by_default (a boolean; default: "false")
  @Option(
    name = "incompatible_objc_alwayslink_by_default",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, make the default value true for alwayslink attributes in objc_library 
      and objc_import.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleObjcAlwayslinkByDefault = Flag.Boolean("incompatibleObjcAlwayslinkByDefault")

  //   --[no]incompatible_objc_provider_remove_linking_info (a boolean; default: "false")
  @Option(
    name = "incompatible_objc_provider_remove_linking_info",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """If set to true, the ObjcProvider's APIs for linking info will be removed.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleObjcProviderRemoveLinkingInfo = Flag.Boolean("incompatibleObjcProviderRemoveLinkingInfo")

  //   --[no]incompatible_package_group_has_public_syntax (a boolean; default: "true")
  @Option(
    name = "incompatible_package_group_has_public_syntax",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      In package_group's `packages` attribute, allows writing "public" or "private" 
      to refer to all packages or no packages respectively.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePackageGroupHasPublicSyntax = Flag.Boolean("incompatiblePackageGroupHasPublicSyntax")

  //   --[no]incompatible_python_disallow_native_rules (a boolean; default: "false")
  @Option(
    name = "incompatible_python_disallow_native_rules",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      When true, an error occurs when using the builtin py_* rules; instead the 
      rule_python rules should be used. See 
      https://github.com/bazelbuild/bazel/issues/17773 for more information and 
      migration instructions.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePythonDisallowNativeRules = Flag.Boolean("incompatiblePythonDisallowNativeRules")

  //   --[no]incompatible_require_linker_input_cc_api (a boolean; default: "true")
  @Option(
    name = "incompatible_require_linker_input_cc_api",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, rule create_linking_context will require linker_inputs instead 
      of libraries_to_link. The old getters of linking_context will also be disabled 
      and just linker_inputs will be available.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRequireLinkerInputCcApi = Flag.Boolean("incompatibleRequireLinkerInputCcApi")

  //   --[no]incompatible_run_shell_command_string (a boolean; default: "true")
  @Option(
    name = "incompatible_run_shell_command_string",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, the command parameter of actions.run_shell will only accept 
      string
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRunShellCommandString = Flag.Boolean("incompatibleRunShellCommandString")

  //   --[no]incompatible_stop_exporting_language_modules (a boolean; default: "false")
  @Option(
    name = "incompatible_stop_exporting_language_modules",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If enabled, certain language-specific modules (such as `cc_common`) are 
      unavailable in user .bzl files and may only be called from their respective 
      rules repositories.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleStopExportingLanguageModules = Flag.Boolean("incompatibleStopExportingLanguageModules")

  //   --[no]incompatible_struct_has_no_methods (a boolean; default: "false")
  @Option(
    name = "incompatible_struct_has_no_methods",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      Disables the to_json and to_proto methods of struct, which pollute the struct 
      field namespace. Instead, use json.encode or json.encode_indent for JSON, or 
      proto.encode_text for textproto.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleStructHasNoMethods = Flag.Boolean("incompatibleStructHasNoMethods")

  //   --[no]incompatible_top_level_aspects_require_providers (a boolean; default: "false")
  @Option(
    name = "incompatible_top_level_aspects_require_providers",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, the top level aspect will honor its required providers and only 
      run on top level targets whose rules' advertised providers satisfy the required 
      providers of the aspect.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleTopLevelAspectsRequireProviders = Flag.Boolean("incompatibleTopLevelAspectsRequireProviders")

  //   --[no]incompatible_unambiguous_label_stringification (a boolean; default: "true")
  @Option(
    name = "incompatible_unambiguous_label_stringification",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      When true, Bazel will stringify the label @//foo:bar to @//foo:bar, instead of 
      //foo:bar. This only affects the behavior of str(), the % operator, and so on; 
      the behavior of repr() is unchanged. See 
      https://github.com/bazelbuild/bazel/issues/15916 for more information.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUnambiguousLabelStringification = Flag.Boolean("incompatibleUnambiguousLabelStringification")

  //   --[no]incompatible_use_cc_configure_from_rules_cc (a boolean; default: "false")
  @Option(
    name = "incompatible_use_cc_configure_from_rules_cc",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      When true, Bazel will no longer allow using cc_configure from @bazel_tools. 
      Please see https://github.com/bazelbuild/bazel/issues/10134 for details and 
      migration instructions.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUseCcConfigureFromRulesCc = Flag.Boolean("incompatibleUseCcConfigureFromRulesCc")

  //   --[no]incompatible_use_plus_in_repo_names (a boolean; default: "false")
  @Option(
    name = "incompatible_use_plus_in_repo_names",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If true, uses the plus sign (+) as the separator in canonical repo names, 
      instead of the tilde (~). This is to address severe performance issues on 
      Windows; see https://github.com/bazelbuild/bazel/issues/22865 for more 
      information.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUsePlusInRepoNames = Flag.Boolean("incompatibleUsePlusInRepoNames")

  //   --[no]incompatible_visibility_private_attributes_at_definition (a boolean; default: "true")
  @Option(
    name = "incompatible_visibility_private_attributes_at_definition",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, the visibility of private rule attributes is checked with 
      respect to the rule definition, falling back to rule usage if not visible.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleVisibilityPrivateAttributesAtDefinition = Flag.Boolean("incompatibleVisibilityPrivateAttributesAtDefinition")

  //   --max_computation_steps (a long integer; default: "0")
  @Option(
    name = "max_computation_steps",
    defaultValue = """"0"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """      
      The maximum number of Starlark computation steps that may be executed by a 
      BUILD file (zero means no limit).
      """,
    valueHelp = """a long integer""",
  )
  @JvmField
  @Suppress("unused")
  val maxComputationSteps = Flag.Unknown("maxComputationSteps")

  //   --nested_set_depth_limit (an integer; default: "3500")
  @Option(
    name = "nested_set_depth_limit",
    defaultValue = """"3500"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      The maximum depth of the graph internal to a depset (also known as NestedSet), 
      above which the depset() constructor will fail.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val nestedSetDepthLimit = Flag.Integer("nestedSetDepthLimit")

  //   --[no]allow_analysis_failures (a boolean; default: "false")
  @Option(
    name = "allow_analysis_failures",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If true, an analysis failure of a rule target results in the target's 
      propagation of an instance of AnalysisFailureInfo containing the error 
      description, instead of resulting in a build failure.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val allowAnalysisFailures = Flag.Boolean("allowAnalysisFailures")

  //   --analysis_testing_deps_limit (an integer; default: "2000")
  @Option(
    name = "analysis_testing_deps_limit",
    defaultValue = """"2000"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Sets the maximum number of transitive dependencies through a rule attribute 
      with a for_analysis_testing configuration transition. Exceeding this limit will 
      result in a rule error.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val analysisTestingDepsLimit = Flag.Integer("analysisTestingDepsLimit")

  //   --[no]break_build_on_parallel_dex2oat_failure (a boolean; default: "false")
  @Option(
    name = "break_build_on_parallel_dex2oat_failure",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If true dex2oat action failures will cause the build to break instead of 
      executing dex2oat during test runtime.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val breakBuildOnParallelDex2oatFailure = Flag.Boolean("breakBuildOnParallelDex2oatFailure")

  //   --[no]check_tests_up_to_date (a boolean; default: "false")
  @Option(
    name = "check_tests_up_to_date",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Don't run tests, just check if they are up-to-date.  If all tests results are 
      up-to-date, the testing completes successfully.  If any test needs to be built 
      or executed, an error is reported and the testing fails.  This option implies 
      --check_up_to_date behavior.  Using this option will also add: 
      --check_up_to_date 
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val checkTestsUpToDate = Flag.Boolean("checkTestsUpToDate")

  //
  //   --default_test_resources (a resource name followed by equal and 1 float or 4 float, e.g
  // memory=10,30,60,100; may be used multiple times)
  //
  @Option(
    name = "default_test_resources",
    allowMultiple = true,
    help = """      
      Override the default resources amount for tests. The expected format is 
      <resource>=<value>. If a single positive number is specified as <value> it will 
      override the default resources for all test sizes. If 4 comma-separated numbers 
      are specified, they will override the resource amount for respectively the 
      small, medium, large, enormous test sizes. Values can also be 
      HOST_RAM/HOST_CPU, optionally followed by [-|*]<float> (eg. 
      memory=HOST_RAM*.1,HOST_RAM*.2,HOST_RAM*.3,HOST_RAM*.4). The default test 
      resources specified by this flag are overridden by explicit resources specified 
      in tags.
      """,
    valueHelp = """      
      a resource name followed by equal and 1 float or 4 float, e.g 
      memory=10,30,60,100
      """,
  )
  @JvmField
  @Suppress("unused")
  val defaultTestResources = Flag.Unknown("defaultTestResources")

  //   --[no]experimental_android_use_parallel_dex2oat (a boolean; default: "false")
  @Option(
    name = "experimental_android_use_parallel_dex2oat",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Use dex2oat in parallel to possibly speed up android_test.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalAndroidUseParallelDex2oat = Flag.Boolean("experimentalAndroidUseParallelDex2oat")

  //
  //   --flaky_test_attempts (a positive integer, the string "default", or test_regex@attempts. This
  // flag may be passed more than once; may be used multiple times)
  //
  @Option(
    name = "flaky_test_attempts",
    allowMultiple = true,
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Each test will be retried up to the specified number of times in case of any 
      test failure. Tests that required more than one attempt to pass are marked as 
      'FLAKY' in the test summary. Normally the value specified is just an integer or 
      the string 'default'. If an integer, then all tests will be run up to N times. 
      If 'default', then only a single test attempt will be made for regular tests 
      and three for tests marked explicitly as flaky by their rule (flaky=1 
      attribute). Alternate syntax: regex_filter@flaky_test_attempts. Where 
      flaky_test_attempts is as above and regex_filter stands for a list of include 
      and exclude regular expression patterns (Also see --runs_per_test). Example: 
      --flaky_test_attempts=//foo/.*,-//foo/bar/.*@3 deflakes all tests in //foo/ 
      except those under foo/bar three times. This option can be passed multiple 
      times. The most recently passed argument that matches takes precedence. If 
      nothing matches, behavior is as if 'default' above.
      """,
    valueHelp = """      
      a positive integer, the string "default", or test_regex@attempts. This flag may 
      be passed more than once
      """,
  )
  @JvmField
  @Suppress("unused")
  val flakyTestAttempts = Flag.Unknown("flakyTestAttempts")

  //   --[no]ios_memleaks (a boolean; default: "false")
  @Option(
    name = "ios_memleaks",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """Enable checking for memory leaks in ios_test targets.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val iosMemleaks = Flag.Boolean("iosMemleaks")

  //   --ios_simulator_device (a string; default: see description)
  @Option(
    name = "ios_simulator_device",
    effectTags = [OptionEffectTag.TEST_RUNNER],
    help = """      
      The device to simulate when running an iOS application in the simulator, e.g. 
      'iPhone 6'. You can get a list of devices by running 'xcrun simctl list 
      devicetypes' on the machine the simulator will be run on.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val iosSimulatorDevice = Flag.Str("iosSimulatorDevice")

  //
  //   --ios_simulator_version (a dotted version (for example '2.3' or '3.3alpha2.4'); default: see
  // description)
  //
  @Option(
    name = "ios_simulator_version",
    effectTags = [OptionEffectTag.TEST_RUNNER],
    help = """      
      The version of iOS to run on the simulator when running or testing. This is 
      ignored for ios_test rules if a target device is specified in the rule.
      """,
    valueHelp = """a dotted version (for example '2.3' or '3.3alpha2.4')""",
  )
  @JvmField
  @Suppress("unused")
  val iosSimulatorVersion = Flag.Unknown("iosSimulatorVersion")

  //
  //   --local_test_jobs (an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally
  // followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"; default: "auto")
  //
  @Option(
    name = "local_test_jobs",
    defaultValue = """"auto"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      The max number of local test jobs to run concurrently. Takes an integer, or a 
      keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation 
      ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". 0 means local resources will limit 
      the number of local test jobs to run concurrently instead. Setting this greater 
      than the value for --jobs is ineffectual.
      """,
    valueHelp = """      
      an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed 
      by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
      """,
  )
  @JvmField
  @Suppress("unused")
  val localTestJobs = Flag.Unknown("localTestJobs")

  //
  //   --runs_per_test (a positive integer or test_regex@runs. This flag may be passed more than once;
  // may be used multiple times)
  //
  @Option(
    name = "runs_per_test",
    allowMultiple = true,
    help = """      
      Specifies number of times to run each test. If any of those attempts fail for 
      any reason, the whole test is considered failed. Normally the value specified 
      is just an integer. Example: --runs_per_test=3 will run all tests 3 times. 
      Alternate syntax: regex_filter@runs_per_test. Where runs_per_test stands for an 
      integer value and regex_filter stands for a list of include and exclude regular 
      expression patterns (Also see --instrumentation_filter). Example: 
      --runs_per_test=//foo/.*,-//foo/bar/.*@3 runs all tests in //foo/ except those 
      under foo/bar three times. This option can be passed multiple times. The most 
      recently passed argument that matches takes precedence. If nothing matches, the 
      test is only run once.
      """,
    valueHelp = """a positive integer or test_regex@runs. This flag may be passed more than once""",
  )
  @JvmField
  @Suppress("unused")
  val runsPerTest = Flag.Unknown("runsPerTest")

  //   --test_env (a 'name=value' assignment with an optional value part; may be used multiple times)
  @Option(
    name = "test_env",
    allowMultiple = true,
    effectTags = [OptionEffectTag.TEST_RUNNER],
    help = """      
      Specifies additional environment variables to be injected into the test runner 
      environment. Variables can be either specified by name, in which case its value 
      will be read from the Bazel client environment, or by the name=value pair. This 
      option can be used multiple times to specify several variables. Used only by 
      the 'bazel test' command.
      """,
    valueHelp = """a 'name=value' assignment with an optional value part""",
  )
  @JvmField
  @Suppress("unused")
  val testEnv = Flag.Unknown("testEnv")

  //   --[no]test_keep_going (a boolean; default: "true")
  @Option(
    name = "test_keep_going",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      When disabled, any non-passing test will cause the entire build to stop. By 
      default all tests are run, even if some do not pass.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val testKeepGoing = Flag.Boolean("testKeepGoing")

  //   --test_strategy (a string; default: "")
  @Option(
    name = "test_strategy",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """Specifies which strategy to use when running tests.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val testStrategy = Flag.Str("testStrategy")

  //   --test_timeout (a single integer or comma-separated list of 4 integers; default: "-1")
  @Option(
    name = "test_timeout",
    defaultValue = """"-1"""",
    help = """      
      Override the default test timeout values for test timeouts (in secs). If a 
      single positive integer value is specified it will override all categories.  If 
      4 comma-separated integers are specified, they will override the timeouts for 
      short, moderate, long and eternal (in that order). In either form, a value of 
      -1 tells blaze to use its default timeouts for that category.
      """,
    valueHelp = """a single integer or comma-separated list of 4 integers""",
  )
  @JvmField
  @Suppress("unused")
  val testTimeout = Flag.Unknown("testTimeout")

  //   --test_tmpdir (a path; default: see description)
  @Option(
    name = "test_tmpdir",
    help = """Specifies the base temporary directory for 'bazel test' to use.""",
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val testTmpdir = Flag.Path("testTmpdir")

  //   --[no]zip_undeclared_test_outputs (a boolean; default: "true")
  @Option(
    name = "zip_undeclared_test_outputs",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.TEST_RUNNER],
    help = """If true, undeclared test outputs will be archived in a zip file.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val zipUndeclaredTestOutputs = Flag.Boolean("zipUndeclaredTestOutputs")

  //   --[no]experimental_parallel_aquery_output (a boolean; default: "true")
  @Option(
    name = "experimental_parallel_aquery_output",
    defaultValue = """"true"""",
    help = """No-op.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalParallelAqueryOutput = Flag.Boolean("experimentalParallelAqueryOutput")

  //   --allow_yanked_versions (a string; may be used multiple times)
  @Option(
    name = "allow_yanked_versions",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Specified the module versions in the form of 
      `<module1>@<version1>,<module2>@<version2>` that will be allowed in the 
      resolved dependency graph even if they are declared yanked in the registry 
      where they come from (if they are not coming from a NonRegistryOverride). 
      Otherwise, yanked versions will cause the resolution to fail. You can also 
      define allowed yanked version with the `BZLMOD_ALLOW_YANKED_VERSIONS` 
      environment variable. You can disable this check by using the keyword 'all' 
      (not recommended).
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val allowYankedVersions = Flag.Str("allowYankedVersions")

  //   --check_bazel_compatibility (error, warning or off; default: "error")
  @Option(
    name = "check_bazel_compatibility",
    defaultValue = """"error"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Check bazel version compatibility of Bazel modules. Valid values are `error` to 
      escalate it to a resolution failure, `off` to disable the check, or `warning` 
      to print a warning when mismatch detected.
      """,
    valueHelp = """error, warning or off""",
  )
  @JvmField
  @Suppress("unused")
  val checkBazelCompatibility = Flag.OneOf("checkBazelCompatibility")

  //   --check_direct_dependencies (off, warning or error; default: "warning")
  @Option(
    name = "check_direct_dependencies",
    defaultValue = """"warning"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Check if the direct `bazel_dep` dependencies declared in the root module are 
      the same versions you get in the resolved dependency graph. Valid values are 
      `off` to disable the check, `warning` to print a warning when mismatch detected 
      or `error` to escalate it to a resolution failure.
      """,
    valueHelp = """off, warning or error""",
  )
  @JvmField
  @Suppress("unused")
  val checkDirectDependencies = Flag.OneOf("checkDirectDependencies")

  //   --[no]ignore_dev_dependency (a boolean; default: "false")
  @Option(
    name = "ignore_dev_dependency",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If true, Bazel ignores `bazel_dep` and `use_extension` declared as 
      `dev_dependency` in the MODULE.bazel of the root module. Note that, those dev 
      dependencies are always ignored in the MODULE.bazel if it's not the root module 
      regardless of the value of this flag.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val ignoreDevDependency = Flag.Boolean("ignoreDevDependency")

  //   --lockfile_mode (off, update, refresh or error; default: "update")
  @Option(
    name = "lockfile_mode",
    defaultValue = """"update"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Specifies how and whether or not to use the lockfile. Valid values are `update` 
      to use the lockfile and update it if there are changes, `refresh` to 
      additionally refresh mutable information (yanked versions and previously 
      missing modules) from remote registries from time to time, `error` to use the 
      lockfile but throw an error if it's not up-to-date, or `off` to neither read 
      from or write to the lockfile.
      """,
    valueHelp = """off, update, refresh or error""",
  )
  @JvmField
  @Suppress("unused")
  val lockfileMode = Flag.OneOf("lockfileMode")

  //   --override_module (an equals-separated mapping of module name to path; may be used multiple times)
  @Option(
    name = "override_module",
    allowMultiple = true,
    help = """      
      Override a module with a local path in the form of <module name>=<path>. If the 
      given path is an absolute path, it will be used as it is. If the given path is 
      a relative path, it is relative to the current working directory. If the given 
      path starts with '%workspace%, it is relative to the workspace root, which is 
      the output of `bazel info workspace`. If the given path is empty, then remove 
      any previous overrides.
      """,
    valueHelp = """an equals-separated mapping of module name to path""",
  )
  @JvmField
  @Suppress("unused")
  val overrideModule = Flag.Unknown("overrideModule")

  //   --registry (a string; may be used multiple times)
  @Option(
    name = "registry",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      Specifies the registries to use to locate Bazel module dependencies. The order 
      is important: modules will be looked up in earlier registries first, and only 
      fall back to later registries when they're missing from the earlier ones.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val registry = Flag.Str("registry")

  //   --vendor_dir (a path; default: see description)
  @Option(
    name = "vendor_dir",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Specifies the directory that should hold the external repositories in vendor 
      mode, whether for the purpose of fetching them into it or using them while 
      building. The path can be specified as either an absolute path or a path 
      relative to the workspace directory.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val vendorDir = Flag.Path("vendorDir")

  //   --cache_computed_file_digests (a long integer; default: "50000")
  @Option(
    name = "cache_computed_file_digests",
    defaultValue = """"50000"""",
    help = """      
      If greater than 0, configures Bazel to cache file digests in memory based on 
      their metadata instead of recomputing the digests from disk every time they are 
      needed. Setting this to 0 ensures correctness because not all file changes can 
      be noted from file metadata. When not 0, the number indicates the size of the 
      cache as the number of file digests to be cached.
      """,
    valueHelp = """a long integer""",
  )
  @JvmField
  @Suppress("unused")
  val cacheComputedFileDigests = Flag.Unknown("cacheComputedFileDigests")

  //
  //   --experimental_dynamic_ignore_local_signals (a comma-separated list of signal numbers; default:
  // see description)
  //
  @Option(
    name = "experimental_dynamic_ignore_local_signals",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Takes a list of OS signal numbers. If a local branch of dynamic execution gets 
      killed with any of these signals, the remote branch will be allowed to finish 
      instead. For persistent workers, this only affects signals that kill the worker 
      process.
      """,
    valueHelp = """a comma-separated list of signal numbers""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDynamicIgnoreLocalSignals = Flag.Unknown("experimentalDynamicIgnoreLocalSignals")

  //   --[no]experimental_filter_library_jar_with_program_jar (a boolean; default: "false")
  @Option(
    name = "experimental_filter_library_jar_with_program_jar",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.ACTION_COMMAND_LINES],
    help = """      
      Filter the ProGuard ProgramJar to remove any classes also present in the 
      LibraryJar.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalFilterLibraryJarWithProgramJar = Flag.Boolean("experimentalFilterLibraryJarWithProgramJar")

  //   --[no]experimental_inmemory_dotd_files (a boolean; default: "true")
  @Option(
    name = "experimental_inmemory_dotd_files",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If enabled, C++ .d files will be passed through in memory directly from the 
      remote build nodes instead of being written to disk.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalInmemoryDotdFiles = Flag.Boolean("experimentalInmemoryDotdFiles")

  //   --[no]experimental_inmemory_jdeps_files (a boolean; default: "true")
  @Option(
    name = "experimental_inmemory_jdeps_files",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """      
      If enabled, the dependency (.jdeps) files generated from Java compilations will 
      be passed through in memory directly from the remote build nodes instead of 
      being written to disk.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalInmemoryJdepsFiles = Flag.Boolean("experimentalInmemoryJdepsFiles")

  //   --[no]experimental_objc_include_scanning (a boolean; default: "false")
  @Option(
    name = "experimental_objc_include_scanning",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.CHANGES_INPUTS],
    help = """Whether to perform include scanning for objective C/C++.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalObjcIncludeScanning = Flag.Boolean("experimentalObjcIncludeScanning")

  //   --[no]experimental_retain_test_configuration_across_testonly (a boolean; default: "false")
  @Option(
    name = "experimental_retain_test_configuration_across_testonly",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      When enabled, --trim_test_configuration will not trim the test configuration 
      for rules marked testonly=1. This is meant to reduce action conflict issues 
      when non-test rules depend on cc_test rules. No effect if 
      --trim_test_configuration is false.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRetainTestConfigurationAcrossTestonly = Flag.Boolean("experimentalRetainTestConfigurationAcrossTestonly")

  //   --[no]experimental_starlark_cc_import (a boolean; default: "false")
  @Option(
    name = "experimental_starlark_cc_import",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """If enabled, the Starlark version of cc_import can be used.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalStarlarkCcImport = Flag.Boolean("experimentalStarlarkCcImport")

  //   --[no]experimental_unsupported_and_brittle_include_scanning (a boolean; default: "false")
  @Option(
    name = "experimental_unsupported_and_brittle_include_scanning",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.CHANGES_INPUTS],
    help = """      
      Whether to narrow inputs to C/C++ compilation by parsing #include lines from 
      input files. This can improve performance and incrementality by decreasing the 
      size of compilation input trees. However, it can also break builds because the 
      include scanner does not fully implement C preprocessor semantics. In 
      particular, it does not understand dynamic #include directives and ignores 
      preprocessor conditional logic. Use at your own risk. Any issues relating to 
      this flag that are filed will be closed.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalUnsupportedAndBrittleIncludeScanning = Flag.Boolean("experimentalUnsupportedAndBrittleIncludeScanning")

  //   --gc_thrashing_limits (comma separated pairs of <period>:<count>; default: "1s:2,20s:3,1m:5")
  @Option(
    name = "gc_thrashing_limits",
    defaultValue = """"1s:2,20s:3,1m:5"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Limits which, if reached, cause GcThrashingDetector to crash Bazel with an OOM. 
      Each limit is specified as <period>:<count> where period is a duration and 
      count is a positive integer. If more than --gc_thrashing_threshold percent of 
      tenured space (old gen heap) remains occupied after <count> consecutive full 
      GCs within <period>, an OOM is triggered. Multiple limits can be specified 
      separated by commas.
      """,
    valueHelp = """comma separated pairs of <period>:<count>""",
  )
  @JvmField
  @Suppress("unused")
  val gcThrashingLimits = Flag.Unknown("gcThrashingLimits")

  //   --[no]heuristically_drop_nodes (a boolean; default: "false")
  @Option(
    name = "heuristically_drop_nodes",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      If true, Blaze will remove FileState and DirectoryListingState nodes after 
      related File and DirectoryListing node is done to save memory. We expect that 
      it is less likely that these nodes will be needed again. If so, the program 
      will re-evaluate them.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val heuristicallyDropNodes = Flag.Boolean("heuristicallyDropNodes")

  //   --[no]incompatible_do_not_split_linking_cmdline (a boolean; default: "true")
  @Option(
    name = "incompatible_do_not_split_linking_cmdline",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      When true, Bazel no longer modifies command line flags used for linking, and 
      also doesn't selectively decide which flags go to the param file and which 
      don't.  See https://github.com/bazelbuild/bazel/issues/7670 for details.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDoNotSplitLinkingCmdline = Flag.Boolean("incompatibleDoNotSplitLinkingCmdline")

  //   --[no]incremental_dexing (a boolean; default: "true")
  @Option(
    name = "incremental_dexing",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """Does most of the work for dexing separately for each Jar file.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incrementalDexing = Flag.Boolean("incrementalDexing")

  //   --[no]keep_state_after_build (a boolean; default: "true")
  @Option(
    name = "keep_state_after_build",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      If false, Blaze will discard the inmemory state from this build when the build 
      finishes. Subsequent builds will not have any incrementality with respect to 
      this one.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val keepStateAfterBuild = Flag.Boolean("keepStateAfterBuild")

  //
  //   --local_cpu_resources (an integer, or "HOST_CPUS", optionally followed by [-|*]<float>.; default:
  // "HOST_CPUS")
  //
  @Option(
    name = "local_cpu_resources",
    defaultValue = """"HOST_CPUS"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Explicitly set the total number of local CPU cores available to Bazel to spend 
      on build actions executed locally. Takes an integer, or "HOST_CPUS", optionally 
      followed by [-|*]<float> (eg. HOST_CPUS*.5 to use half the available CPU 
      cores). By default, ("HOST_CPUS"), Bazel will query system configuration to 
      estimate the number of CPU cores available.
      """,
    valueHelp = """an integer, or "HOST_CPUS", optionally followed by [-|*]<float>.""",
  )
  @JvmField
  @Suppress("unused")
  val localCpuResources = Flag.Unknown("localCpuResources")

  //   --local_extra_resources (a named float, 'name=value'; may be used multiple times)
  @Option(
    name = "local_extra_resources",
    allowMultiple = true,
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Set the number of extra resources available to Bazel. Takes in a string-float 
      pair. Can be used multiple times to specify multiple types of extra resources. 
      Bazel will limit concurrently running actions based on the available extra 
      resources and the extra resources required. Tests can declare the amount of 
      extra resources they need by using a tag of the 
      "resources:<resoucename>:<amount>" format. Available CPU, RAM and resources 
      cannot be set with this flag.
      """,
    valueHelp = """a named float, 'name=value'""",
  )
  @JvmField
  @Suppress("unused")
  val localExtraResources = Flag.Unknown("localExtraResources")

  //
  //   --local_ram_resources (an integer number of MBs, or "HOST_RAM", optionally followed by
  // [-|*]<float>.; default: "HOST_RAM*.67")
  //
  @Option(
    name = "local_ram_resources",
    defaultValue = """"HOST_RAM*.67"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Explicitly set the total amount of local host RAM (in MB) available to Bazel to 
      spend on build actions executed locally. Takes an integer, or "HOST_RAM", 
      optionally followed by [-|*]<float> (eg. HOST_RAM*.5 to use half the available 
      RAM). By default, ("HOST_RAM*.67"), Bazel will query system configuration to 
      estimate the amount of RAM available and will use 67% of it.
      """,
    valueHelp = """an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.""",
  )
  @JvmField
  @Suppress("unused")
  val localRamResources = Flag.Unknown("localRamResources")

  //
  //   --local_resources (a named double, 'name=value', where value is an integer, or a keyword ("auto",
  // "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto",
  // "HOST_CPUS*.5"; may be used multiple times)
  //
  @Option(
    name = "local_resources",
    allowMultiple = true,
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Set the number of resources available to Bazel. Takes in an assignment to a 
      float or HOST_RAM/HOST_CPUS, optionally followed by [-|*]<float> (eg. 
      memory=HOST_RAM*.5 to use half the available RAM). Can be used multiple times 
      to specify multiple types of resources. Bazel will limit concurrently running 
      actions based on the available resources and the resources required. Tests can 
      declare the amount of resources they need by using a tag of the 
      "resources:<resource name>:<amount>" format. Overrides resources specified by 
      --local_{cpu|ram|extra}_resources.
      """,
    valueHelp = """      
      a named double, 'name=value', where value is an integer, or a keyword ("auto", 
      "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) 
      eg. "auto", "HOST_CPUS*.5"
      """,
  )
  @JvmField
  @Suppress("unused")
  val localResources = Flag.Unknown("localResources")

  //   --[no]objc_use_dotd_pruning (a boolean; default: "true")
  @Option(
    name = "objc_use_dotd_pruning",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If set, .d files emitted by clang will be used to prune the set of inputs 
      passed into objc compiles.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val objcUseDotdPruning = Flag.Boolean("objcUseDotdPruning")

  //   --[no]process_headers_in_dependencies (a boolean; default: "false")
  @Option(
    name = "process_headers_in_dependencies",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      When building a target //a:a, process headers in all targets that //a:a depends 
      on (if header processing is enabled for the toolchain).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val processHeadersInDependencies = Flag.Boolean("processHeadersInDependencies")

  //   --skyframe_high_water_mark_full_gc_drops_per_invocation (an integer, >= 0; default: "2147483647")
  @Option(
    name = "skyframe_high_water_mark_full_gc_drops_per_invocation",
    defaultValue = """"2147483647"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Flag for advanced configuration of Bazel's internal Skyframe engine. If Bazel 
      detects its retained heap percentage usage exceeds the threshold set by 
      --skyframe_high_water_mark_threshold, when a full GC event occurs, it will drop 
      unnecessary temporary Skyframe state, up to this many times per invocation. 
      Defaults to Integer.MAX_VALUE; effectively unlimited. Zero means that full GC 
      events will never trigger drops. If the limit is reached, Skyframe state will 
      no longer be dropped when a full GC event occurs and that retained heap 
      percentage threshold is exceeded.
      """,
    valueHelp = """an integer, >= 0""",
  )
  @JvmField
  @Suppress("unused")
  val skyframeHighWaterMarkFullGcDropsPerInvocation = Flag.Unknown("skyframeHighWaterMarkFullGcDropsPerInvocation")

  //   --skyframe_high_water_mark_minor_gc_drops_per_invocation (an integer, >= 0; default: "2147483647")
  @Option(
    name = "skyframe_high_water_mark_minor_gc_drops_per_invocation",
    defaultValue = """"2147483647"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Flag for advanced configuration of Bazel's internal Skyframe engine. If Bazel 
      detects its retained heap percentage usage exceeds the threshold set by 
      --skyframe_high_water_mark_threshold, when a minor GC event occurs, it will 
      drop unnecessary temporary Skyframe state, up to this many times per 
      invocation. Defaults to Integer.MAX_VALUE; effectively unlimited. Zero means 
      that minor GC events will never trigger drops. If the limit is reached, 
      Skyframe state will no longer be dropped when a minor GC event occurs and that 
      retained heap percentage threshold is exceeded.
      """,
    valueHelp = """an integer, >= 0""",
  )
  @JvmField
  @Suppress("unused")
  val skyframeHighWaterMarkMinorGcDropsPerInvocation = Flag.Unknown("skyframeHighWaterMarkMinorGcDropsPerInvocation")

  //   --skyframe_high_water_mark_threshold (an integer; default: "85")
  @Option(
    name = "skyframe_high_water_mark_threshold",
    defaultValue = """"85"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Flag for advanced configuration of Bazel's internal Skyframe engine. If Bazel 
      detects its retained heap percentage usage is at least this threshold, it will 
      drop unnecessary temporary Skyframe state. Tweaking this may let you mitigate 
      wall time impact of GC thrashing, when the GC thrashing is (i) caused by the 
      memory usage of this temporary state and (ii) more costly than reconstituting 
      the state when it is needed.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val skyframeHighWaterMarkThreshold = Flag.Integer("skyframeHighWaterMarkThreshold")

  //   --[no]track_incremental_state (a boolean; default: "true")
  @Option(
    name = "track_incremental_state",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      If false, Blaze will not persist data that allows for invalidation and 
      re-evaluation on incremental builds in order to save memory on this build. 
      Subsequent builds will not have any incrementality with respect to this one. 
      Usually you will want to specify --batch when setting this to false.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val trackIncrementalState = Flag.Boolean("trackIncrementalState")

  //   --[no]trim_test_configuration (a boolean; default: "true")
  @Option(
    name = "trim_test_configuration",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      When enabled, test-related options will be cleared below the top level of the 
      build. When this flag is active, tests cannot be built as dependencies of 
      non-test rules, but changes to test-related options will not cause non-test 
      rules to be re-analyzed.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val trimTestConfiguration = Flag.Boolean("trimTestConfiguration")

  //   --[no]announce_rc (a boolean; default: "false")
  @Option(
    name = "announce_rc",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Whether to announce rc options.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val announceRc = Flag.Boolean("announceRc")

  //   --[no]attempt_to_print_relative_paths (a boolean; default: "false")
  @Option(
    name = "attempt_to_print_relative_paths",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      When printing the location part of messages, attempt to use a path relative to 
      the workspace directory or one of the directories specified by --package_path.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val attemptToPrintRelativePaths = Flag.Boolean("attemptToPrintRelativePaths")

  //   --bes_backend (a string; default: "")
  @Option(
    name = "bes_backend",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies the build event service (BES) backend endpoint in the form 
      [SCHEME://]HOST[:PORT]. The default is to disable BES uploads. Supported 
      schemes are grpc and grpcs (grpc with TLS enabled). If no scheme is provided, 
      Bazel assumes grpcs.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val besBackend = Flag.Str("besBackend")

  //   --[no]bes_check_preceding_lifecycle_events (a boolean; default: "false")
  @Option(
    name = "bes_check_preceding_lifecycle_events",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Sets the field check_preceding_lifecycle_events_present on 
      PublishBuildToolEventStreamRequest which tells BES to check whether it 
      previously received InvocationAttemptStarted and BuildEnqueued events matching 
      the current tool event.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val besCheckPrecedingLifecycleEvents = Flag.Boolean("besCheckPrecedingLifecycleEvents")

  //   --bes_header (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "bes_header",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specify a header in NAME=VALUE form that will be included in BES requests. 
      Multiple headers can be passed by specifying the flag multiple times. Multiple 
      values for the same name will be converted to a comma-separated list.
      """,
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val besHeader = Flag.Unknown("besHeader")

  //   --bes_instance_name (a string; default: see description)
  @Option(
    name = "bes_instance_name",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies the instance name under which the BES will persist uploaded BEP. 
      Defaults to null.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val besInstanceName = Flag.Str("besInstanceName")

  //   --bes_keywords (comma-separated list of options; may be used multiple times)
  @Option(
    name = "bes_keywords",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies a list of notification keywords to be added the default set of 
      keywords published to BES ("command_name=<command_name> ", 
      "protocol_name=BEP"). Defaults to none.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val besKeywords = Flag.Unknown("besKeywords")

  //   --[no]bes_lifecycle_events (a boolean; default: "true")
  @Option(
    name = "bes_lifecycle_events",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Specifies whether to publish BES lifecycle events. (defaults to 'true').""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val besLifecycleEvents = Flag.Boolean("besLifecycleEvents")

  //   --bes_oom_finish_upload_timeout (An immutable length of time.; default: "10m")
  @Option(
    name = "bes_oom_finish_upload_timeout",
    defaultValue = """"10m"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      Specifies how long bazel should wait for the BES/BEP upload to complete while 
      OOMing. This flag ensures termination when the JVM is severely GC thrashing and 
      cannot make progress on any user thread.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val besOomFinishUploadTimeout = Flag.Duration("besOomFinishUploadTimeout")

  //   --bes_outerr_buffer_size (an integer; default: "10240")
  @Option(
    name = "bes_outerr_buffer_size",
    defaultValue = """"10240"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies the maximal size of stdout or stderr to be buffered in BEP, before it 
      is reported as a progress event. Individual writes are still reported in a 
      single event, even if larger than the specified value up to 
      --bes_outerr_chunk_size.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val besOuterrBufferSize = Flag.Integer("besOuterrBufferSize")

  //   --bes_outerr_chunk_size (an integer; default: "1048576")
  @Option(
    name = "bes_outerr_chunk_size",
    defaultValue = """"1048576"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies the maximal size of stdout or stderr to be sent to BEP in a single 
      message.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val besOuterrChunkSize = Flag.Integer("besOuterrChunkSize")

  //   --bes_proxy (a string; default: see description)
  @Option(
    name = "bes_proxy",
    help = """      
      Connect to the Build Event Service through a proxy. Currently this flag can 
      only be used to configure a Unix domain socket (unix:/path/to/socket).
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val besProxy = Flag.Str("besProxy")

  //   --bes_results_url (a string; default: "")
  @Option(
    name = "bes_results_url",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Specifies the base URL where a user can view the information streamed to the 
      BES backend. Bazel will output the URL appended by the invocation id to the 
      terminal.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val besResultsUrl = Flag.Str("besResultsUrl")

  //   --bes_system_keywords (comma-separated list of options; may be used multiple times)
  @Option(
    name = "bes_system_keywords",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies a list of notification keywords to be included directly, without the 
      "user_keyword=" prefix included for keywords supplied via --bes_keywords. 
      Intended for Build service operators that set --bes_lifecycle_events=false and 
      include keywords when calling PublishLifecycleEvent. Build service operators 
      using this flag should prevent users from overriding the flag value.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val besSystemKeywords = Flag.Unknown("besSystemKeywords")

  //   --bes_timeout (An immutable length of time.; default: "0s")
  @Option(
    name = "bes_timeout",
    defaultValue = """"0s"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Specifies how long bazel should wait for the BES/BEP upload to complete after 
      the build and tests have finished. A valid timeout is a natural number followed 
      by a unit: Days (d), hours (h), minutes (m), seconds (s), and milliseconds 
      (ms). The default value is '0' which means that there is no timeout.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val besTimeout = Flag.Duration("besTimeout")

  //
  //   --bes_upload_mode (wait_for_upload_complete, nowait_for_upload_complete or fully_async; default:
  // "wait_for_upload_complete")
  //
  @Option(
    name = "bes_upload_mode",
    defaultValue = """"wait_for_upload_complete"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      Specifies whether the Build Event Service upload should block the build 
      completion or should end the invocation immediately and finish the upload in 
      the background. Either 'wait_for_upload_complete' (default), 
      'nowait_for_upload_complete', or 'fully_async'.
      """,
    valueHelp = """wait_for_upload_complete, nowait_for_upload_complete or fully_async""",
  )
  @JvmField
  @Suppress("unused")
  val besUploadMode = Flag.OneOf("besUploadMode")

  //   --build_event_binary_file (a string; default: "")
  @Option(
    name = "build_event_binary_file",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If non-empty, write a varint delimited binary representation of representation 
      of the build event protocol to that file. This option implies 
      --bes_upload_mode=wait_for_upload_complete.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventBinaryFile = Flag.Str("buildEventBinaryFile")

  //   --[no]build_event_binary_file_path_conversion (a boolean; default: "true")
  @Option(
    name = "build_event_binary_file_path_conversion",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Convert paths in the binary file representation of the build event protocol to 
      more globally valid URIs whenever possible; if disabled, the file:// uri scheme 
      will always be used
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventBinaryFilePathConversion = Flag.Boolean("buildEventBinaryFilePathConversion")

  //
  //   --build_event_binary_file_upload_mode (wait_for_upload_complete, nowait_for_upload_complete or
  // fully_async; default: "wait_for_upload_complete")
  //
  @Option(
    name = "build_event_binary_file_upload_mode",
    defaultValue = """"wait_for_upload_complete"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      Specifies whether the Build Event Service upload for --build_event_binary_file 
      should block the build completion or should end the invocation immediately and 
      finish the upload in the background. Either 'wait_for_upload_complete' 
      (default), 'nowait_for_upload_complete', or 'fully_async'.
      """,
    valueHelp = """wait_for_upload_complete, nowait_for_upload_complete or fully_async""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventBinaryFileUploadMode = Flag.OneOf("buildEventBinaryFileUploadMode")

  //   --build_event_json_file (a string; default: "")
  @Option(
    name = "build_event_json_file",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If non-empty, write a JSON serialisation of the build event protocol to that 
      file. This option implies --bes_upload_mode=wait_for_upload_complete.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventJsonFile = Flag.Str("buildEventJsonFile")

  //   --[no]build_event_json_file_path_conversion (a boolean; default: "true")
  @Option(
    name = "build_event_json_file_path_conversion",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Convert paths in the json file representation of the build event protocol to 
      more globally valid URIs whenever possible; if disabled, the file:// uri scheme 
      will always be used
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventJsonFilePathConversion = Flag.Boolean("buildEventJsonFilePathConversion")

  //
  //   --build_event_json_file_upload_mode (wait_for_upload_complete, nowait_for_upload_complete or
  // fully_async; default: "wait_for_upload_complete")
  //
  @Option(
    name = "build_event_json_file_upload_mode",
    defaultValue = """"wait_for_upload_complete"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      Specifies whether the Build Event Service upload for --build_event_json_file 
      should block the build completion or should end the invocation immediately and 
      finish the upload in the background. Either 'wait_for_upload_complete' 
      (default), 'nowait_for_upload_complete', or 'fully_async'.
      """,
    valueHelp = """wait_for_upload_complete, nowait_for_upload_complete or fully_async""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventJsonFileUploadMode = Flag.OneOf("buildEventJsonFileUploadMode")

  //   --build_event_max_named_set_of_file_entries (an integer; default: "-1")
  @Option(
    name = "build_event_max_named_set_of_file_entries",
    defaultValue = """"-1"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      The maximum number of entries for a single named_set_of_files event; values 
      smaller than 2 are ignored and no event splitting is performed. This is 
      intended for limiting the maximum event size in the build event protocol, 
      although it does not directly control event size. The total event size is a 
      function of the structure of the set as well as the file and uri lengths, which 
      may in turn depend on the hash function.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventMaxNamedSetOfFileEntries = Flag.Integer("buildEventMaxNamedSetOfFileEntries")

  //   --[no]build_event_publish_all_actions (a boolean; default: "false")
  @Option(
    name = "build_event_publish_all_actions",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Whether all actions should be published.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventPublishAllActions = Flag.Boolean("buildEventPublishAllActions")

  //   --build_event_text_file (a string; default: "")
  @Option(
    name = "build_event_text_file",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If non-empty, write a textual representation of the build event protocol to 
      that file
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventTextFile = Flag.Str("buildEventTextFile")

  //   --[no]build_event_text_file_path_conversion (a boolean; default: "true")
  @Option(
    name = "build_event_text_file_path_conversion",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Convert paths in the text file representation of the build event protocol to 
      more globally valid URIs whenever possible; if disabled, the file:// uri scheme 
      will always be used
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventTextFilePathConversion = Flag.Boolean("buildEventTextFilePathConversion")

  //
  //   --build_event_text_file_upload_mode (wait_for_upload_complete, nowait_for_upload_complete or
  // fully_async; default: "wait_for_upload_complete")
  //
  @Option(
    name = "build_event_text_file_upload_mode",
    defaultValue = """"wait_for_upload_complete"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      Specifies whether the Build Event Service upload for --build_event_text_file 
      should block the build completion or should end the invocation immediately and 
      finish the upload in the background. Either 'wait_for_upload_complete' 
      (default), 'nowait_for_upload_complete', or 'fully_async'.
      """,
    valueHelp = """wait_for_upload_complete, nowait_for_upload_complete or fully_async""",
  )
  @JvmField
  @Suppress("unused")
  val buildEventTextFileUploadMode = Flag.OneOf("buildEventTextFileUploadMode")

  //   --[no]debug_spawn_scheduler (a boolean; default: "false")
  @Option(
    name = "debug_spawn_scheduler",
    defaultValue = """"false"""",
    help = """""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val debugSpawnScheduler = Flag.Boolean("debugSpawnScheduler")

  //   --[no]experimental_announce_profile_path (a boolean; default: "false")
  @Option(
    name = "experimental_announce_profile_path",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """If enabled, adds the JSON profile path to the log.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalAnnounceProfilePath = Flag.Boolean("experimentalAnnounceProfilePath")

  //   --[no]experimental_bep_target_summary (a boolean; default: "false")
  @Option(
    name = "experimental_bep_target_summary",
    defaultValue = """"false"""",
    help = """Whether to publish TargetSummary events.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalBepTargetSummary = Flag.Boolean("experimentalBepTargetSummary")

  //   --[no]experimental_build_event_expand_filesets (a boolean; default: "false")
  @Option(
    name = "experimental_build_event_expand_filesets",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """If true, expand Filesets in the BEP when presenting output files.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventExpandFilesets = Flag.Boolean("experimentalBuildEventExpandFilesets")

  //   --[no]experimental_build_event_fully_resolve_fileset_symlinks (a boolean; default: "false")
  @Option(
    name = "experimental_build_event_fully_resolve_fileset_symlinks",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If true, fully resolve relative Fileset symlinks in the BEP when presenting 
      output files. Requires --experimental_build_event_expand_filesets.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventFullyResolveFilesetSymlinks = Flag.Boolean("experimentalBuildEventFullyResolveFilesetSymlinks")

  //   --experimental_build_event_upload_max_retries (an integer; default: "4")
  @Option(
    name = "experimental_build_event_upload_max_retries",
    defaultValue = """"4"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """The maximum number of times Bazel should retry uploading a build event.""",
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventUploadMaxRetries = Flag.Integer("experimentalBuildEventUploadMaxRetries")

  //
  //   --experimental_build_event_upload_retry_minimum_delay (An immutable length of time.; default:
  // "1s")
  //
  @Option(
    name = "experimental_build_event_upload_retry_minimum_delay",
    defaultValue = """"1s"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      Initial, minimum delay for exponential backoff retries when BEP upload fails. 
      (exponent: 1.6)
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventUploadRetryMinimumDelay = Flag.Duration("experimentalBuildEventUploadRetryMinimumDelay")

  //   --experimental_build_event_upload_strategy (a string; default: see description)
  @Option(
    name = "experimental_build_event_upload_strategy",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Selects how to upload artifacts referenced in the build event protocol.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalBuildEventUploadStrategy = Flag.Str("experimentalBuildEventUploadStrategy")

  //   --[no]experimental_collect_load_average_in_profiler (a boolean; default: "true")
  @Option(
    name = "experimental_collect_load_average_in_profiler",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """If enabled, the profiler collects the system's overall load average.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectLoadAverageInProfiler = Flag.Boolean("experimentalCollectLoadAverageInProfiler")

  //   --[no]experimental_collect_local_sandbox_action_metrics (a boolean; default: "true")
  @Option(
    name = "experimental_collect_local_sandbox_action_metrics",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """Deprecated no-op.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectLocalSandboxActionMetrics = Flag.Boolean("experimentalCollectLocalSandboxActionMetrics")

  //   --[no]experimental_collect_pressure_stall_indicators (a boolean; default: "false")
  @Option(
    name = "experimental_collect_pressure_stall_indicators",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """If enabled, the profiler collects the Linux PSI data.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectPressureStallIndicators = Flag.Boolean("experimentalCollectPressureStallIndicators")

  //   --[no]experimental_collect_resource_estimation (a boolean; default: "false")
  @Option(
    name = "experimental_collect_resource_estimation",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      If enabled, the profiler collects CPU and memory usage estimation for local 
      actions.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectResourceEstimation = Flag.Boolean("experimentalCollectResourceEstimation")

  //   --[no]experimental_collect_system_network_usage (a boolean; default: "false")
  @Option(
    name = "experimental_collect_system_network_usage",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """If enabled, the profiler collects the system's network usage.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectSystemNetworkUsage = Flag.Boolean("experimentalCollectSystemNetworkUsage")

  //   --[no]experimental_collect_worker_data_in_profiler (a boolean; default: "false")
  @Option(
    name = "experimental_collect_worker_data_in_profiler",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """If enabled, the profiler collects worker's aggregated resource data.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCollectWorkerDataInProfiler = Flag.Boolean("experimentalCollectWorkerDataInProfiler")

  //   --experimental_command_profile (cpu, wall, alloc or lock; default: see description)
  @Option(
    name = "experimental_command_profile",
    help = """      
      Records a Java Flight Recorder profile for the duration of the command. One of 
      the supported profiling event types (cpu, wall, alloc or lock) must be given as 
      an argument. The profile is written to a file named after the event type under 
      the output base directory. The syntax and semantics of this flag might change 
      in the future to support additional profile types or output formats; use at 
      your own risk.
      """,
    valueHelp = """cpu, wall, alloc or lock""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCommandProfile = Flag.OneOf("experimentalCommandProfile")

  //   --[no]experimental_docker_verbose (a boolean; default: "false")
  @Option(
    name = "experimental_docker_verbose",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      If enabled, Bazel will print more verbose messages about the Docker sandbox 
      strategy.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDockerVerbose = Flag.Boolean("experimentalDockerVerbose")

  //   --[no]experimental_materialize_param_files_directly (a boolean; default: "false")
  @Option(
    name = "experimental_materialize_param_files_directly",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """If materializing param files, do so with direct writes to disk.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalMaterializeParamFilesDirectly = Flag.Boolean("experimentalMaterializeParamFilesDirectly")

  //
  //   --experimental_profile_additional_tasks (phase, action, action_check, action_lock,
  // action_release, action_update, action_complete, bzlmod, info, create_package, remote_execution,
  // local_execution, scanner, local_parse, upload_time, remote_process_time, remote_queue,
  // remote_setup, fetch, local_process_time, vfs_stat, vfs_dir, vfs_readlink, vfs_md5, vfs_xattr,
  // vfs_delete, vfs_open, vfs_read, vfs_write, vfs_glob, vfs_vmfs_stat, vfs_vmfs_dir, vfs_vmfs_read,
  // wait, thread_name, thread_sort_index, skyframe_eval, skyfunction, critical_path,
  // critical_path_component, handle_gc_notification, action_counts, action_cache_counts,
  // local_cpu_usage, system_cpu_usage, cpu_usage_estimation, local_memory_usage, system_memory_usage,
  // memory_usage_estimation, system_network_up_usage, system_network_down_usage, workers_memory_usage,
  // system_load_average, starlark_parser, starlark_user_fn, starlark_builtin_fn,
  // starlark_user_compiled_fn, starlark_repository_fn, action_fs_staging, remote_cache_check,
  // remote_download, remote_network, filesystem_traversal, worker_execution, worker_setup,
  // worker_borrow, worker_working, worker_copying_outputs, credential_helper, pressure_stall_io,
  // pressure_stall_memory, conflict_check, dynamic_lock, repository_fetch, repository_vendor or
  // unknown; may be used multiple times)
  //
  @Option(
    name = "experimental_profile_additional_tasks",
    allowMultiple = true,
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """Specifies additional profile tasks to be included in the profile.""",
    valueHelp = """      
      phase, action, action_check, action_lock, action_release, action_update, 
      action_complete, bzlmod, info, create_package, remote_execution, 
      local_execution, scanner, local_parse, upload_time, remote_process_time, 
      remote_queue, remote_setup, fetch, local_process_time, vfs_stat, vfs_dir, 
      vfs_readlink, vfs_md5, vfs_xattr, vfs_delete, vfs_open, vfs_read, vfs_write, 
      vfs_glob, vfs_vmfs_stat, vfs_vmfs_dir, vfs_vmfs_read, wait, thread_name, 
      thread_sort_index, skyframe_eval, skyfunction, critical_path, 
      critical_path_component, handle_gc_notification, action_counts, 
      action_cache_counts, local_cpu_usage, system_cpu_usage, cpu_usage_estimation, 
      local_memory_usage, system_memory_usage, memory_usage_estimation, 
      system_network_up_usage, system_network_down_usage, workers_memory_usage, 
      system_load_average, starlark_parser, starlark_user_fn, starlark_builtin_fn, 
      starlark_user_compiled_fn, starlark_repository_fn, action_fs_staging, 
      remote_cache_check, remote_download, remote_network, filesystem_traversal, 
      worker_execution, worker_setup, worker_borrow, worker_working, 
      worker_copying_outputs, credential_helper, pressure_stall_io, 
      pressure_stall_memory, conflict_check, dynamic_lock, repository_fetch, 
      repository_vendor or unknown
      """,
  )
  @JvmField
  @Suppress("unused")
  val experimentalProfileAdditionalTasks = Flag.OneOf("experimentalProfileAdditionalTasks")

  //   --[no]experimental_profile_include_primary_output (a boolean; default: "false")
  @Option(
    name = "experimental_profile_include_primary_output",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      Includes the extra "out" attribute in action events that contains the exec path 
      to the action's primary output.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalProfileIncludePrimaryOutput = Flag.Boolean("experimentalProfileIncludePrimaryOutput")

  //   --[no]experimental_profile_include_target_label (a boolean; default: "false")
  @Option(
    name = "experimental_profile_include_target_label",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """Includes target label in action events' JSON profile data.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalProfileIncludeTargetLabel = Flag.Boolean("experimentalProfileIncludeTargetLabel")

  //   --[no]experimental_record_metrics_for_all_mnemonics (a boolean; default: "false")
  @Option(
    name = "experimental_record_metrics_for_all_mnemonics",
    defaultValue = """"false"""",
    help = """      
      By default the number of action types is limited to the 20 mnemonics with the 
      largest number of executed actions. Setting this option will write statistics 
      for all mnemonics.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRecordMetricsForAllMnemonics = Flag.Boolean("experimentalRecordMetricsForAllMnemonics")

  //   --experimental_repository_resolved_file (a string; default: "")
  @Option(
    name = "experimental_repository_resolved_file",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If non-empty, write a Starlark value with the resolved information of all 
      Starlark repository rules that were executed.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRepositoryResolvedFile = Flag.Str("experimentalRepositoryResolvedFile")

  //   --[no]experimental_run_bep_event_include_residue (a boolean; default: "false")
  @Option(
    name = "experimental_run_bep_event_include_residue",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Whether to include the command-line residue in run build events which could 
      contain the residue. By default, the residue is not included in run command 
      build events that could contain the residue.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRunBepEventIncludeResidue = Flag.Boolean("experimentalRunBepEventIncludeResidue")

  //   --[no]experimental_stream_log_file_uploads (a boolean; default: "false")
  @Option(
    name = "experimental_stream_log_file_uploads",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Stream log file uploads directly to the remote storage rather than writing them 
      to disk.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalStreamLogFileUploads = Flag.Boolean("experimentalStreamLogFileUploads")

  //   --experimental_workspace_rules_log_file (a path; default: see description)
  @Option(
    name = "experimental_workspace_rules_log_file",
    help = """      
      Log certain Workspace Rules events into this file as delimited WorkspaceEvent 
      protos.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkspaceRulesLogFile = Flag.Path("experimentalWorkspaceRulesLogFile")

  //   --explain (a path; default: see description)
  @Option(
    name = "explain",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Causes the build system to explain each executed step of the build. The 
      explanation is written to the specified log file.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val explain = Flag.Path("explain")

  //   --[no]generate_json_trace_profile (a tri-state (auto, yes, no); default: "auto")
  @Option(
    name = "generate_json_trace_profile",
    defaultValue = """"auto"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      If enabled, Bazel profiles the build and writes a JSON-format profile into a 
      file in the output base. View profile by loading into chrome://tracing. By 
      default Bazel writes the profile for all build-like commands and query.
      """,
    valueHelp = """a tri-state (auto, yes, no)""",
  )
  @JvmField
  @Suppress("unused")
  val generateJsonTraceProfile = Flag.TriState("generateJsonTraceProfile")

  //   --[no]heap_dump_on_oom (a boolean; default: "false")
  @Option(
    name = "heap_dump_on_oom",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      Whether to manually output a heap dump if an OOM is thrown (including manual 
      OOMs due to reaching --gc_thrashing_limits). The dump will be written to 
      <output_base>/<invocation_id>.heapdump.hprof. This option effectively replaces 
      -XX:+HeapDumpOnOutOfMemoryError, which has no effect for manual OOMs.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val heapDumpOnOom = Flag.Boolean("heapDumpOnOom")

  //   --[no]ignore_unsupported_sandboxing (a boolean; default: "false")
  @Option(
    name = "ignore_unsupported_sandboxing",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """Do not print a warning when sandboxed execution is not supported on this system.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val ignoreUnsupportedSandboxing = Flag.Boolean("ignoreUnsupportedSandboxing")

  //   --[no]legacy_important_outputs (a boolean; default: "true")
  @Option(
    name = "legacy_important_outputs",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Use this to suppress generation of the legacy important_outputs field in the 
      TargetComplete event. important_outputs are required for Bazel to ResultStore 
      integration.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val legacyImportantOutputs = Flag.Boolean("legacyImportantOutputs")

  //   --logging (0 <= an integer <= 6; default: "3")
  @Option(
    name = "logging",
    defaultValue = """"3"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """The logging level.""",
    valueHelp = """0 <= an integer <= 6""",
  )
  @JvmField
  @Suppress("unused")
  val logging = Flag.Unknown("logging")

  //   --[no]materialize_param_files (a boolean; default: "false")
  @Option(
    name = "materialize_param_files",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Writes intermediate parameter files to output tree even when using remote 
      action execution. Useful when debugging actions. This is implied by 
      --subcommands and --verbose_failures.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val materializeParamFiles = Flag.Boolean("materializeParamFiles")

  //   --max_config_changes_to_show (an integer; default: "3")
  @Option(
    name = "max_config_changes_to_show",
    defaultValue = """"3"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      When discarding the analysis cache due to a change in the build options, 
      displays up to the given number of changed option names. If the number given is 
      -1, all changed options will be displayed.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val maxConfigChangesToShow = Flag.Integer("maxConfigChangesToShow")

  //   --max_test_output_bytes (an integer; default: "-1")
  @Option(
    name = "max_test_output_bytes",
    defaultValue = """"-1"""",
    effectTags = [OptionEffectTag.TEST_RUNNER, OptionEffectTag.TERMINAL_OUTPUT, OptionEffectTag.EXECUTION],
    help = """      
      Specifies maximum per-test-log size that can be emitted when --test_output is 
      'errors' or 'all'. Useful for avoiding overwhelming the output with excessively 
      noisy test output. The test header is included in the log size. Negative values 
      imply no limit. Output is all or nothing.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val maxTestOutputBytes = Flag.Integer("maxTestOutputBytes")

  //   --memory_profile (a path; default: see description)
  @Option(
    name = "memory_profile",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      If set, write memory usage data to the specified file at phase ends and stable 
      heap to master log at end of build.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val memoryProfile = Flag.Path("memoryProfile")

  //
  //   --memory_profile_stable_heap_parameters (integers, separated by a comma expected in pairs;
  // default: "1,0")
  //
  @Option(
    name = "memory_profile_stable_heap_parameters",
    defaultValue = """"1,0"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      Tune memory profile's computation of stable heap at end of build. Should be and 
      even number of  integers separated by commas. In each pair the first integer is 
      the number of GCs to perform. The second integer in each pair is the number of 
      seconds to wait between GCs. Ex: 2,4,4,0 would 2 GCs with a 4sec pause, 
      followed by 4 GCs with zero second pause
      """,
    valueHelp = """integers, separated by a comma expected in pairs""",
  )
  @JvmField
  @Suppress("unused")
  val memoryProfileStableHeapParameters = Flag.Unknown("memoryProfileStableHeapParameters")

  //   --output_filter (a valid Java regular expression; default: see description)
  @Option(
    name = "output_filter",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Only shows warnings and action outputs for rules with a name matching the 
      provided regular expression.
      """,
    valueHelp = """a valid Java regular expression""",
  )
  @JvmField
  @Suppress("unused")
  val outputFilter = Flag.Unknown("outputFilter")

  //   --profile (a path; default: see description)
  @Option(
    name = "profile",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      If set, profile Bazel and write data to the specified file. Use bazel 
      analyze-profile to analyze the profile.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val profile = Flag.Path("profile")

  //   --progress_report_interval (an integer in 0-3600 range; default: "0")
  @Option(
    name = "progress_report_interval",
    defaultValue = """"0"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      The number of seconds to wait between reports on still running jobs. The 
      default value 0 means the first report will be printed after 10 seconds, then 
      30 seconds and after that progress is reported once every minute. When --curses 
      is enabled, progress is reported every second.
      """,
    valueHelp = """an integer in 0-3600 range""",
  )
  @JvmField
  @Suppress("unused")
  val progressReportInterval = Flag.Unknown("progressReportInterval")

  //   --[no]record_full_profiler_data (a boolean; default: "false")
  @Option(
    name = "record_full_profiler_data",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      By default, Bazel profiler will record only aggregated data for fast but 
      numerous events (such as statting the file). If this option is enabled, 
      profiler will record each event - resulting in more precise profiling data but 
      LARGE performance hit. Option only has effect if --profile used as well.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val recordFullProfilerData = Flag.Boolean("recordFullProfilerData")

  //   --remote_print_execution_messages (failure, success or all; default: "failure")
  @Option(
    name = "remote_print_execution_messages",
    defaultValue = """"failure"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Choose when to print remote execution messages. Valid values are `failure`, to 
      print only on failures, `success` to print only on successes and `all` to print 
      always.
      """,
    valueHelp = """failure, success or all""",
  )
  @JvmField
  @Suppress("unused")
  val remotePrintExecutionMessages = Flag.OneOf("remotePrintExecutionMessages")

  //   --[no]sandbox_debug (a boolean; default: "false")
  @Option(
    name = "sandbox_debug",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Enables debugging features for the sandboxing feature. This includes two 
      things: first, the sandbox root contents are left untouched after a build; and 
      second, prints extra debugging information on execution. This can help 
      developers of Bazel or Starlark rules with debugging failures due to missing 
      input files, etc.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val sandboxDebug = Flag.Boolean("sandboxDebug")

  //   --show_result (an integer; default: "1")
  @Option(
    name = "show_result",
    defaultValue = """"1"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Show the results of the build.  For each target, state whether or not it was 
      brought up-to-date, and if so, a list of output files that were built.  The 
      printed files are convenient strings for copy+pasting to the shell, to execute 
      them.This option requires an integer argument, which is the threshold number of 
      targets above which result information is not printed. Thus zero causes 
      suppression of the message and MAX_INT causes printing of the result to occur 
      always. The default is one.If nothing was built for a target its results may be 
      omitted to keep the output under the threshold.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val showResult = Flag.Integer("showResult")

  //   --[no]slim_profile (a boolean; default: "true")
  @Option(
    name = "slim_profile",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      Slims down the size of the JSON profile by merging events if the profile gets  
      too large.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val slimProfile = Flag.Boolean("slimProfile")

  //   --starlark_cpu_profile (a string; default: "")
  @Option(
    name = "starlark_cpu_profile",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.BAZEL_MONITORING],
    help = """      
      Writes into the specified file a pprof profile of CPU usage by all Starlark 
      threads.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val starlarkCpuProfile = Flag.Str("starlarkCpuProfile")

  //   --[no]subcommands [-s] (true, pretty_print or false; default: "false")
  @Option(
    name = "subcommands",
    abbrev = 's',
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Display the subcommands executed during a build. Related flags: 
      --execution_log_json_file, --execution_log_binary_file (for logging subcommands 
      to a file in a tool-friendly format).
      """,
    valueHelp = """true, pretty_print or false""",
  )
  @JvmField
  @Suppress("unused")
  val subcommands = Flag.OneOf("subcommands")

  //   --test_output (summary, errors, all or streamed; default: "summary")
  @Option(
    name = "test_output",
    defaultValue = """"summary"""",
    effectTags = [OptionEffectTag.TEST_RUNNER, OptionEffectTag.TERMINAL_OUTPUT, OptionEffectTag.EXECUTION],
    help = """      
      Specifies desired output mode. Valid values are 'summary' to output only test 
      status summary, 'errors' to also print test logs for failed tests, 'all' to 
      print logs for all tests and 'streamed' to output logs for all tests in real 
      time (this will force tests to be executed locally one at a time regardless of 
      --test_strategy value).
      """,
    valueHelp = """summary, errors, all or streamed""",
  )
  @JvmField
  @Suppress("unused")
  val testOutput = Flag.OneOf("testOutput")

  //   --test_summary (short, terse, detailed, none or testcase; default: "short")
  @Option(
    name = "test_summary",
    defaultValue = """"short"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Specifies the desired format of the test summary. Valid values are 'short' to 
      print information only about tests executed, 'terse', to print information only 
      about unsuccessful tests that were run, 'detailed' to print detailed 
      information about failed test cases, 'testcase' to print summary in test case 
      resolution, do not print detailed information about failed test cases and 
      'none' to omit the summary.
      """,
    valueHelp = """short, terse, detailed, none or testcase""",
  )
  @JvmField
  @Suppress("unused")
  val testSummary = Flag.OneOf("testSummary")

  //   --tool_tag (a string; default: "")
  @Option(
    name = "tool_tag",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    help = """A tool name to attribute this Bazel invocation to.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val toolTag = Flag.Str("toolTag")

  //
  //   --toolchain_resolution_debug (a comma-separated list of regex expressions with prefix '-'
  // specifying excluded paths; default: "-.*")
  //
  @Option(
    name = "toolchain_resolution_debug",
    defaultValue = """"-.*"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Print debug information during toolchain resolution. The flag takes a regex, 
      which is checked against toolchain types and specific targets to see which to 
      debug. Multiple regexes may be  separated by commas, and then each regex is 
      checked separately. Note: The output of this flag is very complex and will 
      likely only be useful to experts in toolchain resolution.
      """,
    valueHelp = """      
      a comma-separated list of regex expressions with prefix '-' specifying excluded 
      paths
      """,
  )
  @JvmField
  @Suppress("unused")
  val toolchainResolutionDebug = Flag.Unknown("toolchainResolutionDebug")

  //
  //   --ui_event_filters (Convert list of comma separated event kind to list of filters; may be used
  // multiple times)
  //
  @Option(
    name = "ui_event_filters",
    allowMultiple = true,
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Specifies which events to show in the UI. It is possible to add or remove 
      events to the default ones using leading +/-, or override the default set 
      completely with direct assignment. The set of supported event kinds include 
      INFO, DEBUG, ERROR and more.
      """,
    valueHelp = """Convert list of comma separated event kind to list of filters""",
  )
  @JvmField
  @Suppress("unused")
  val uiEventFilters = Flag.Unknown("uiEventFilters")

  //   --[no]verbose_explanations (a boolean; default: "false")
  @Option(
    name = "verbose_explanations",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Increases the verbosity of the explanations issued if --explain is enabled. Has 
      no effect if --explain is not enabled.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val verboseExplanations = Flag.Boolean("verboseExplanations")

  //   --[no]verbose_failures (a boolean; default: "false")
  @Option(
    name = "verbose_failures",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """If a command fails, print out the full command line.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val verboseFailures = Flag.Boolean("verboseFailures")

  //   --aspects_parameters (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "aspects_parameters",
    allowMultiple = true,
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Specifies the values of the command-line aspects parameters. Each parameter 
      value is specified via <param_name>=<param_value>, for example 
      'my_param=my_val' where 'my_param' is a parameter of some aspect in --aspects 
      list or required by an aspect in the list. This option can be used multiple 
      times. However, it is not allowed to assign values to the same parameter more 
      than once.
      """,
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val aspectsParameters = Flag.Unknown("aspectsParameters")

  //   --experimental_resolved_file_instead_of_workspace (a string; default: "")
  @Option(
    name = "experimental_resolved_file_instead_of_workspace",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """If non-empty read the specified resolved file instead of the WORKSPACE file""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalResolvedFileInsteadOfWorkspace = Flag.Str("experimentalResolvedFileInsteadOfWorkspace")

  //   --flag_alias (a 'name=value' flag alias; may be used multiple times)
  @Option(
    name = "flag_alias",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      Sets a shorthand name for a Starlark flag. It takes a single key-value pair in 
      the form "<key>=<value>" as an argument.
      """,
    valueHelp = """a 'name=value' flag alias""",
  )
  @JvmField
  @Suppress("unused")
  val flagAlias = Flag.Unknown("flagAlias")

  //   --[no]incompatible_default_to_explicit_init_py (a boolean; default: "false")
  @Option(
    name = "incompatible_default_to_explicit_init_py",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      This flag changes the default behavior so that __init__.py files are no longer 
      automatically created in the runfiles of Python targets. Precisely, when a 
      py_binary or py_test target has legacy_create_init set to "auto" (the default), 
      it is treated as false if and only if this flag is set. See 
      https://github.com/bazelbuild/bazel/issues/10076.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDefaultToExplicitInitPy = Flag.Boolean("incompatibleDefaultToExplicitInitPy")

  //   --[no]incompatible_py2_outputs_are_suffixed (a boolean; default: "true")
  @Option(
    name = "incompatible_py2_outputs_are_suffixed",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, targets built in the Python 2 configuration will appear under an 
      output root that includes the suffix '-py2', while targets built for Python 3 
      will appear in a root with no Python-related suffix. This means that the 
      `bazel-bin` convenience symlink will point to Python 3 targets rather than 
      Python 2. If you enable this option it is also recommended to enable 
      `--incompatible_py3_is_default`.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePy2OutputsAreSuffixed = Flag.Boolean("incompatiblePy2OutputsAreSuffixed")

  //   --[no]incompatible_py3_is_default (a boolean; default: "true")
  @Option(
    name = "incompatible_py3_is_default",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, `py_binary` and `py_test` targets that do not set their 
      `python_version` (or `default_python_version`) attribute will default to PY3 
      rather than to PY2. If you set this flag it is also recommended to set 
      `--incompatible_py2_outputs_are_suffixed`.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePy3IsDefault = Flag.Boolean("incompatiblePy3IsDefault")

  //   --[no]incompatible_use_python_toolchains (a boolean; default: "true")
  @Option(
    name = "incompatible_use_python_toolchains",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, executable native Python rules will use the Python runtime 
      specified by the Python toolchain, rather than the runtime given by legacy 
      flags like --python_top.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleUsePythonToolchains = Flag.Boolean("incompatibleUsePythonToolchains")

  //   --python_version (PY2 or PY3; default: see description)
  @Option(
    name = "python_version",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      The Python major version mode, either `PY2` or `PY3`. Note that this is 
      overridden by `py_binary` and `py_test` targets (even if they don't explicitly 
      specify a version) so there is usually not much reason to supply this flag.
      """,
    valueHelp = """PY2 or PY3""",
  )
  @JvmField
  @Suppress("unused")
  val pythonVersion = Flag.OneOf("pythonVersion")

  //   --target_pattern_file (a string; default: "")
  @Option(
    name = "target_pattern_file",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      If set, build will read patterns from the file named here, rather than on the 
      command line. It is an error to specify a file here as well as command-line 
      patterns.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val targetPatternFile = Flag.Str("targetPatternFile")

  //   --experimental_circuit_breaker_strategy (failure; default: see description)
  @Option(
    name = "experimental_circuit_breaker_strategy",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Specifies the strategy for the circuit breaker to use. Available strategies are 
      "failure". On invalid value for the option the behavior same as the option is 
      not set.
      """,
    valueHelp = """failure""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCircuitBreakerStrategy = Flag.Unknown("experimentalCircuitBreakerStrategy")

  //   --experimental_downloader_config (a string; default: see description)
  @Option(
    name = "experimental_downloader_config",
    help = """      
      Specify a file to configure the remote downloader with. This file consists of 
      lines, each of which starts with a directive (`allow`, `block` or `rewrite`) 
      followed by either a host name (for `allow` and `block`) or two patterns, one 
      to match against, and one to use as a substitute URL, with back-references 
      starting from `${'$'}1`. It is possible for multiple `rewrite` directives for 
      the same URL to be give, and in this case multiple URLs will be returned.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalDownloaderConfig = Flag.Str("experimentalDownloaderConfig")

  //   --[no]experimental_guard_against_concurrent_changes (a boolean; default: "false")
  @Option(
    name = "experimental_guard_against_concurrent_changes",
    defaultValue = """"false"""",
    help = """      
      Turn this off to disable checking the ctime of input files of an action before 
      uploading it to a remote cache. There may be cases where the Linux kernel 
      delays writing of files, which could cause false positives.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalGuardAgainstConcurrentChanges = Flag.Boolean("experimentalGuardAgainstConcurrentChanges")

  //   --[no]experimental_remote_cache_async (a boolean; default: "false")
  @Option(
    name = "experimental_remote_cache_async",
    defaultValue = """"false"""",
    help = """      
      If true, remote cache I/O will happen in the background instead of taking place 
      as the part of a spawn.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCacheAsync = Flag.Boolean("experimentalRemoteCacheAsync")

  //   --experimental_remote_cache_compression_threshold (an integer; default: "0")
  @Option(
    name = "experimental_remote_cache_compression_threshold",
    defaultValue = """"0"""",
    help = """      
      The minimum blob size required to compress/decompress with zstd. Ineffectual 
      unless --remote_cache_compression is set.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCacheCompressionThreshold = Flag.Integer("experimentalRemoteCacheCompressionThreshold")

  //   --experimental_remote_cache_eviction_retries (an integer; default: "0")
  @Option(
    name = "experimental_remote_cache_eviction_retries",
    defaultValue = """"0"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      The maximum number of attempts to retry if the build encountered remote cache 
      eviction error. A non-zero value will implicitly set 
      --incompatible_remote_use_new_exit_code_for_lost_inputs to true. A new 
      invocation id will be generated for each attempt. If you generate invocation id 
      and provide it to Bazel with --invocation_id, you should not use this flag. 
      Instead, set flag --incompatible_remote_use_new_exit_code_for_lost_inputs and 
      check for the exit code 39.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCacheEvictionRetries = Flag.Integer("experimentalRemoteCacheEvictionRetries")

  //   --[no]experimental_remote_cache_lease_extension (a boolean; default: "false")
  @Option(
    name = "experimental_remote_cache_lease_extension",
    defaultValue = """"false"""",
    help = """      
      If set to true, Bazel will extend the lease for outputs of remote actions 
      during the build by sending `FindMissingBlobs` calls periodically to remote 
      cache. The frequency is based on the value of `--experimental_remote_cache_ttl`.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCacheLeaseExtension = Flag.Boolean("experimentalRemoteCacheLeaseExtension")

  //   --experimental_remote_cache_ttl (An immutable length of time.; default: "3h")
  @Option(
    name = "experimental_remote_cache_ttl",
    defaultValue = """"3h"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      The guaranteed minimal TTL of blobs in the remote cache after their digests are 
      recently referenced e.g. by an ActionResult or FindMissingBlobs. Bazel does 
      several optimizations based on the blobs' TTL e.g. doesn't repeatedly call 
      GetActionResult in an incremental build. The value should be set slightly less 
      than the real TTL since there is a gap between when the server returns the 
      digests and when Bazel receives them.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCacheTtl = Flag.Duration("experimentalRemoteCacheTtl")

  //   --experimental_remote_capture_corrupted_outputs (a path; default: see description)
  @Option(
    name = "experimental_remote_capture_corrupted_outputs",
    help = """A path to a directory where the corrupted outputs will be captured to.""",
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteCaptureCorruptedOutputs = Flag.Path("experimentalRemoteCaptureCorruptedOutputs")

  //   --[no]experimental_remote_discard_merkle_trees (a boolean; default: "false")
  @Option(
    name = "experimental_remote_discard_merkle_trees",
    defaultValue = """"false"""",
    help = """      
      If set to true, discard in-memory copies of the input root's Merkle tree and 
      associated input mappings during calls to GetActionResult() and Execute(). This 
      reduces memory usage significantly, but does require Bazel to recompute them 
      upon remote cache misses and retries.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteDiscardMerkleTrees = Flag.Boolean("experimentalRemoteDiscardMerkleTrees")

  //   --experimental_remote_downloader (a string; default: see description)
  @Option(
    name = "experimental_remote_downloader",
    help = """      
      A Remote Asset API endpoint URI, to be used as a remote download proxy. The 
      supported schemas are grpc, grpcs (grpc with TLS enabled) and unix (local UNIX 
      sockets). If no schema is provided Bazel will default to grpcs. See: 
      https://github.com/bazelbuild/remote-apis/blob/master/build/bazel/remote/asset/v1/remote_asset.proto
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteDownloader = Flag.Str("experimentalRemoteDownloader")

  //   --[no]experimental_remote_downloader_local_fallback (a boolean; default: "false")
  @Option(
    name = "experimental_remote_downloader_local_fallback",
    defaultValue = """"false"""",
    help = """Whether to fall back to the local downloader if remote downloader fails.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteDownloaderLocalFallback = Flag.Boolean("experimentalRemoteDownloaderLocalFallback")

  //   --[no]experimental_remote_execution_keepalive (a boolean; default: "false")
  @Option(
    name = "experimental_remote_execution_keepalive",
    defaultValue = """"false"""",
    help = """Whether to use keepalive for remote execution calls.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteExecutionKeepalive = Flag.Boolean("experimentalRemoteExecutionKeepalive")

  //   --experimental_remote_failure_rate_threshold (an integer in 0-100 range; default: "10")
  @Option(
    name = "experimental_remote_failure_rate_threshold",
    defaultValue = """"10"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      Sets the allowed number of failure rate in percentage for a specific time 
      window after which it stops calling to the remote cache/executor. By default 
      the value is 10. Setting this to 0 means no limitation.
      """,
    valueHelp = """an integer in 0-100 range""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteFailureRateThreshold = Flag.Unknown("experimentalRemoteFailureRateThreshold")

  //   --experimental_remote_failure_window_interval (An immutable length of time.; default: "60s")
  @Option(
    name = "experimental_remote_failure_window_interval",
    defaultValue = """"60s"""",
    effectTags = [OptionEffectTag.EXECUTION],
    help = """      
      The interval in which the failure rate of the remote requests are computed. On 
      zero or negative value the failure duration is computed the whole duration of 
      the execution.Following units can be used: Days (d), hours (h), minutes (m), 
      seconds (s), and milliseconds (ms). If the unit is omitted, the value is 
      interpreted as seconds.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteFailureWindowInterval = Flag.Duration("experimentalRemoteFailureWindowInterval")

  //   --[no]experimental_remote_mark_tool_inputs (a boolean; default: "false")
  @Option(
    name = "experimental_remote_mark_tool_inputs",
    defaultValue = """"false"""",
    help = """      
      If set to true, Bazel will mark inputs as tool inputs for the remote executor. 
      This can be used to implement remote persistent workers.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteMarkToolInputs = Flag.Boolean("experimentalRemoteMarkToolInputs")

  //   --[no]experimental_remote_merkle_tree_cache (a boolean; default: "false")
  @Option(
    name = "experimental_remote_merkle_tree_cache",
    defaultValue = """"false"""",
    help = """      
      If set to true, Merkle tree calculations will be memoized to improve the remote 
      cache hit checking speed. The memory foot print of the cache is controlled by 
      --experimental_remote_merkle_tree_cache_size.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteMerkleTreeCache = Flag.Boolean("experimentalRemoteMerkleTreeCache")

  //   --experimental_remote_merkle_tree_cache_size (a long integer; default: "1000")
  @Option(
    name = "experimental_remote_merkle_tree_cache_size",
    defaultValue = """"1000"""",
    help = """      
      The number of Merkle trees to memoize to improve the remote cache hit checking 
      speed. Even though the cache is automatically pruned according to Java's 
      handling of soft references, out-of-memory errors can occur if set too high. If 
      set to 0  the cache size is unlimited. Optimal value varies depending on 
      project's size. Default to 1000.
      """,
    valueHelp = """a long integer""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteMerkleTreeCacheSize = Flag.Unknown("experimentalRemoteMerkleTreeCacheSize")

  //   --experimental_remote_output_service (a string; default: see description)
  @Option(
    name = "experimental_remote_output_service",
    help = """      
      HOST or HOST:PORT of a remote output service endpoint. The supported schemas 
      are grpc, grpcs (grpc with TLS enabled) and unix (local UNIX sockets). If no 
      schema is provided Bazel will default to grpcs. Specify grpc:// or unix: schema 
      to disable TLS.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteOutputService = Flag.Str("experimentalRemoteOutputService")

  //   --experimental_remote_output_service_output_path_prefix (a string; default: "")
  @Option(
    name = "experimental_remote_output_service_output_path_prefix",
    defaultValue = """""""",
    help = """      
      The path under which the contents of output directories managed by the 
      --experimental_remote_output_service are placed. The actual output directory 
      used by a build will be a descendant of this path and determined by the output 
      service.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteOutputServiceOutputPathPrefix = Flag.Str("experimentalRemoteOutputServiceOutputPathPrefix")

  //   --[no]experimental_remote_require_cached (a boolean; default: "false")
  @Option(
    name = "experimental_remote_require_cached",
    defaultValue = """"false"""",
    help = """      
      If set to true, enforce that all actions that can run remotely are cached, or 
      else fail the build. This is useful to troubleshoot non-determinism issues as 
      it allows checking whether actions that should be cached are actually cached 
      without spuriously injecting new results into the cache.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteRequireCached = Flag.Boolean("experimentalRemoteRequireCached")

  //   --experimental_remote_scrubbing_config (Converts to a Scrubber; default: see description)
  @Option(
    name = "experimental_remote_scrubbing_config",
    help = """      
      Enables remote cache key scrubbing with the supplied configuration file, which 
      must be a protocol buffer in text format (see 
      src/main/protobuf/remote_scrubbing.proto).
      
      This feature is intended to facilitate sharing a remote/disk cache between 
      actions executing on different platforms but targeting the same platform. It 
      should be used with extreme care, as improper settings may cause accidental 
      sharing of cache entries and result in incorrect builds.
      
      Scrubbing does not affect how an action is executed, only how its remote/disk 
      cache key is computed for the purpose of retrieving or storing an action 
      result. Scrubbed actions are incompatible with remote execution, and will 
      always be executed locally instead.
      
      Modifying the scrubbing configuration does not invalidate outputs present in 
      the local filesystem or internal caches; a clean build is required to reexecute 
      affected actions.
      
      In order to successfully use this feature, you likely want to set a custom 
      --host_platform together with --experimental_platform_in_output_dir (to 
      normalize output prefixes) and --incompatible_strict_action_env (to normalize 
      environment variables).
      """,
    valueHelp = """Converts to a Scrubber""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRemoteScrubbingConfig = Flag.Unknown("experimentalRemoteScrubbingConfig")

  //   --experimental_worker_for_repo_fetching (off, platform, virtual or auto; default: "auto")
  @Option(
    name = "experimental_worker_for_repo_fetching",
    defaultValue = """"auto"""",
    help = """      
      The threading mode to use for repo fetching. If set to 'off', no worker thread 
      is used, and the repo fetching is subject to restarts. Otherwise, uses a 
      virtual worker thread.
      """,
    valueHelp = """off, platform, virtual or auto""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWorkerForRepoFetching = Flag.OneOf("experimentalWorkerForRepoFetching")

  //   --[no]incompatible_remote_build_event_upload_respect_no_cache (a boolean; default: "false")
  @Option(
    name = "incompatible_remote_build_event_upload_respect_no_cache",
    defaultValue = """"false"""",
    help = """Deprecated. No-op. Use --remote_build_event_upload=minimal instead.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoteBuildEventUploadRespectNoCache = Flag.Boolean("incompatibleRemoteBuildEventUploadRespectNoCache")

  //   --[no]incompatible_remote_downloader_send_all_headers (a boolean; default: "true")
  @Option(
    name = "incompatible_remote_downloader_send_all_headers",
    defaultValue = """"true"""",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      Whether to send all values of a multi-valued header to the remote downloader 
      instead of just the first.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoteDownloaderSendAllHeaders = Flag.Boolean("incompatibleRemoteDownloaderSendAllHeaders")

  //   --[no]incompatible_remote_output_paths_relative_to_input_root (a boolean; default: "false")
  @Option(
    name = "incompatible_remote_output_paths_relative_to_input_root",
    defaultValue = """"false"""",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, output paths are relative to input root instead of working 
      directory.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoteOutputPathsRelativeToInputRoot = Flag.Boolean("incompatibleRemoteOutputPathsRelativeToInputRoot")

  //   --[no]incompatible_remote_results_ignore_disk (a boolean; default: "true")
  @Option(
    name = "incompatible_remote_results_ignore_disk",
    defaultValue = """"true"""",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """No-op""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoteResultsIgnoreDisk = Flag.Boolean("incompatibleRemoteResultsIgnoreDisk")

  //   --[no]incompatible_remote_use_new_exit_code_for_lost_inputs (a boolean; default: "true")
  @Option(
    name = "incompatible_remote_use_new_exit_code_for_lost_inputs",
    defaultValue = """"true"""",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If set to true, Bazel will use new exit code 39 instead of 34 if remote cache 
      evicts blobs during the build.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleRemoteUseNewExitCodeForLostInputs = Flag.Boolean("incompatibleRemoteUseNewExitCodeForLostInputs")

  //   --[no]remote_accept_cached (a boolean; default: "true")
  @Option(
    name = "remote_accept_cached",
    defaultValue = """"true"""",
    help = """Whether to accept remotely cached action results.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val remoteAcceptCached = Flag.Boolean("remoteAcceptCached")

  //   --remote_build_event_upload (all or minimal; default: "minimal")
  @Option(
    name = "remote_build_event_upload",
    defaultValue = """"minimal"""",
    help = """      
      If set to 'all', all local outputs referenced by BEP are uploaded to remote 
      cache.If set to 'minimal', local outputs referenced by BEP are not uploaded to 
      the remote cache, except for files that are important to the consumers of BEP 
      (e.g. test logs and timing profile). bytestream:// scheme is always used for 
      the uri of files even if they are missing from remote cache.Default to 
      'minimal'.
      """,
    valueHelp = """all or minimal""",
  )
  @JvmField
  @Suppress("unused")
  val remoteBuildEventUpload = Flag.OneOf("remoteBuildEventUpload")

  //   --remote_bytestream_uri_prefix (a string; default: see description)
  @Option(
    name = "remote_bytestream_uri_prefix",
    help = """      
      The hostname and instance name to be used in bytestream:// URIs that are 
      written into build event streams. This option can be set when builds are 
      performed using a proxy, which causes the values of --remote_executor and 
      --remote_instance_name to no longer correspond to the canonical name of the 
      remote execution service. When not set, it will default to 
      "${'$'}{hostname}/${'$'}{instance_name}".
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val remoteBytestreamUriPrefix = Flag.Str("remoteBytestreamUriPrefix")

  //   --remote_cache (a string; default: see description)
  @Option(
    name = "remote_cache",
    help = """      
      A URI of a caching endpoint. The supported schemas are http, https, grpc, grpcs 
      (grpc with TLS enabled) and unix (local UNIX sockets). If no schema is provided 
      Bazel will default to grpcs. Specify grpc://, http:// or unix: schema to 
      disable TLS. See https://bazel.build/remote/caching
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val remoteCache = Flag.Str("remoteCache")

  //   --[no]remote_cache_compression (a boolean; default: "false")
  @Option(
    name = "remote_cache_compression",
    defaultValue = """"false"""",
    help = """      
      If enabled, compress/decompress cache blobs with zstd when their size is at 
      least --experimental_remote_cache_compression_threshold.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val remoteCacheCompression = Flag.Boolean("remoteCacheCompression")

  //   --remote_cache_header (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "remote_cache_header",
    allowMultiple = true,
    help = """      
      Specify a header that will be included in cache requests: 
      --remote_cache_header=Name=Value. Multiple headers can be passed by specifying 
      the flag multiple times. Multiple values for the same name will be converted to 
      a comma-separated list.
      """,
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val remoteCacheHeader = Flag.Unknown("remoteCacheHeader")

  //   --remote_default_exec_properties (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "remote_default_exec_properties",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Set the default exec properties to be used as the remote execution platform if 
      an execution platform does not already set exec_properties.
      """,
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val remoteDefaultExecProperties = Flag.Unknown("remoteDefaultExecProperties")

  //   --remote_default_platform_properties (a string; default: "")
  @Option(
    name = "remote_default_platform_properties",
    defaultValue = """""""",
    help = """      
      Set the default platform properties to be set for the remote execution API, if 
      the execution platform does not already set remote_execution_properties. This 
      value will also be used if the host platform is selected as the execution 
      platform for remote execution.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val remoteDefaultPlatformProperties = Flag.Str("remoteDefaultPlatformProperties")

  //   --remote_download_regex (a valid Java regular expression; may be used multiple times)
  @Option(
    name = "remote_download_regex",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Force remote build outputs whose path matches this pattern to be downloaded, 
      irrespective of --remote_download_outputs. Multiple patterns may be specified 
      by repeating this flag.
      """,
    valueHelp = """a valid Java regular expression""",
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloadRegex = Flag.Unknown("remoteDownloadRegex")

  //   --remote_downloader_header (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "remote_downloader_header",
    allowMultiple = true,
    help = """      
      Specify a header that will be included in remote downloader requests: 
      --remote_downloader_header=Name=Value. Multiple headers can be passed by 
      specifying the flag multiple times. Multiple values for the same name will be 
      converted to a comma-separated list.
      """,
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val remoteDownloaderHeader = Flag.Unknown("remoteDownloaderHeader")

  //   --remote_exec_header (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "remote_exec_header",
    allowMultiple = true,
    help = """      
      Specify a header that will be included in execution requests: 
      --remote_exec_header=Name=Value. Multiple headers can be passed by specifying 
      the flag multiple times. Multiple values for the same name will be converted to 
      a comma-separated list.
      """,
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val remoteExecHeader = Flag.Unknown("remoteExecHeader")

  //   --remote_execution_priority (an integer; default: "0")
  @Option(
    name = "remote_execution_priority",
    defaultValue = """"0"""",
    help = """      
      The relative priority of actions to be executed remotely. The semantics of the 
      particular priority values are server-dependent.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val remoteExecutionPriority = Flag.Integer("remoteExecutionPriority")

  //   --remote_executor (a string; default: see description)
  @Option(
    name = "remote_executor",
    help = """      
      HOST or HOST:PORT of a remote execution endpoint. The supported schemas are 
      grpc, grpcs (grpc with TLS enabled) and unix (local UNIX sockets). If no schema 
      is provided Bazel will default to grpcs. Specify grpc:// or unix: schema to 
      disable TLS.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val remoteExecutor = Flag.Str("remoteExecutor")

  //   --remote_grpc_log (a path; default: see description)
  @Option(
    name = "remote_grpc_log",
    help = """      
      If specified, a path to a file to log gRPC call related details. This log 
      consists of a sequence of serialized 
      com.google.devtools.build.lib.remote.logging.RemoteExecutionLog.LogEntry 
      protobufs with each message prefixed by a varint denoting the size of the 
      following serialized protobuf message, as performed by the method 
      LogEntry.writeDelimitedTo(OutputStream).
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val remoteGrpcLog = Flag.Path("remoteGrpcLog")

  //   --remote_header (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "remote_header",
    allowMultiple = true,
    help = """      
      Specify a header that will be included in requests: --remote_header=Name=Value. 
      Multiple headers can be passed by specifying the flag multiple times. Multiple 
      values for the same name will be converted to a comma-separated list.
      """,
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val remoteHeader = Flag.Unknown("remoteHeader")

  //   --remote_instance_name (a string; default: "")
  @Option(
    name = "remote_instance_name",
    defaultValue = """""""",
    help = """Value to pass as instance_name in the remote execution API.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val remoteInstanceName = Flag.Str("remoteInstanceName")

  //   --[no]remote_local_fallback (a boolean; default: "false")
  @Option(
    name = "remote_local_fallback",
    defaultValue = """"false"""",
    help = """      
      Whether to fall back to standalone local execution strategy if remote execution 
      fails.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val remoteLocalFallback = Flag.Boolean("remoteLocalFallback")

  //   --remote_local_fallback_strategy (a string; default: "local")
  @Option(
    name = "remote_local_fallback_strategy",
    defaultValue = """"local"""",
    help = """      
      No-op, deprecated. See https://github.com/bazelbuild/bazel/issues/7480 for 
      details.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val remoteLocalFallbackStrategy = Flag.Str("remoteLocalFallbackStrategy")

  //   --remote_max_connections (an integer; default: "100")
  @Option(
    name = "remote_max_connections",
    defaultValue = """"100"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Limit the max number of concurrent connections to remote cache/executor. By 
      default the value is 100. Setting this to 0 means no limitation.For HTTP remote 
      cache, one TCP connection could handle one request at one time, so Bazel could 
      make up to --remote_max_connections concurrent requests.For gRPC remote 
      cache/executor, one gRPC channel could usually handle 100+ concurrent requests, 
      so Bazel could make around `--remote_max_connections * 100` concurrent requests.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val remoteMaxConnections = Flag.Integer("remoteMaxConnections")

  //   --remote_proxy (a string; default: see description)
  @Option(
    name = "remote_proxy",
    help = """      
      Connect to the remote cache through a proxy. Currently this flag can only be 
      used to configure a Unix domain socket (unix:/path/to/socket).
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val remoteProxy = Flag.Str("remoteProxy")

  //   --remote_result_cache_priority (an integer; default: "0")
  @Option(
    name = "remote_result_cache_priority",
    defaultValue = """"0"""",
    help = """      
      The relative priority of remote actions to be stored in remote cache. The 
      semantics of the particular priority values are server-dependent.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val remoteResultCachePriority = Flag.Integer("remoteResultCachePriority")

  //   --remote_retries (an integer; default: "5")
  @Option(
    name = "remote_retries",
    defaultValue = """"5"""",
    help = """      
      The maximum number of attempts to retry a transient error. If set to 0, retries 
      are disabled.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val remoteRetries = Flag.Integer("remoteRetries")

  //   --remote_retry_max_delay (An immutable length of time.; default: "5s")
  @Option(
    name = "remote_retry_max_delay",
    defaultValue = """"5s"""",
    help = """      
      The maximum backoff delay between remote retry attempts. Following units can be 
      used: Days (d), hours (h), minutes (m), seconds (s), and milliseconds (ms). If 
      the unit is omitted, the value is interpreted as seconds.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val remoteRetryMaxDelay = Flag.Duration("remoteRetryMaxDelay")

  //   --remote_timeout (An immutable length of time.; default: "60s")
  @Option(
    name = "remote_timeout",
    defaultValue = """"60s"""",
    help = """      
      The maximum amount of time to wait for remote execution and cache calls. For 
      the REST cache, this is both the connect and the read timeout. Following units 
      can be used: Days (d), hours (h), minutes (m), seconds (s), and milliseconds 
      (ms). If the unit is omitted, the value is interpreted as seconds.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val remoteTimeout = Flag.Duration("remoteTimeout")

  //   --[no]remote_upload_local_results (a boolean; default: "true")
  @Option(
    name = "remote_upload_local_results",
    defaultValue = """"true"""",
    help = """      
      Whether to upload locally executed action results to the remote cache if the 
      remote cache supports it and the user is authorized to do so.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val remoteUploadLocalResults = Flag.Boolean("remoteUploadLocalResults")

  //   --[no]remote_verify_downloads (a boolean; default: "true")
  @Option(
    name = "remote_verify_downloads",
    defaultValue = """"true"""",
    help = """      
      If set to true, Bazel will compute the hash sum of all remote downloads and  
      discard the remotely cached values if they don't match the expected value.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val remoteVerifyDownloads = Flag.Boolean("remoteVerifyDownloads")

  //   --[no]allow_analysis_cache_discard (a boolean; default: "true")
  @Option(
    name = "allow_analysis_cache_discard",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      If discarding the analysis cache due to a change in the build system, setting 
      this option to false will cause bazel to exit, rather than continuing with the 
      build. This option has no effect when 'discard_analysis_cache' is also set.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val allowAnalysisCacheDiscard = Flag.Boolean("allowAnalysisCacheDiscard")

  //   --auto_output_filter (none, all, packages or subpackages; default: "none")
  @Option(
    name = "auto_output_filter",
    defaultValue = """"none"""",
    help = """      
      If --output_filter is not specified, then the value for this option is used 
      create a filter automatically. Allowed values are 'none' (filter nothing / show 
      everything), 'all' (filter everything / show nothing), 'packages' (include 
      output from rules in packages mentioned on the Blaze command line), and 
      'subpackages' (like 'packages', but also include subpackages). For the 
      'packages' and 'subpackages' values //java/foo and //javatests/foo are treated 
      as one package)'.
      """,
    valueHelp = """none, all, packages or subpackages""",
  )
  @JvmField
  @Suppress("unused")
  val autoOutputFilter = Flag.OneOf("autoOutputFilter")

  //   --[no]build_manual_tests (a boolean; default: "false")
  @Option(
    name = "build_manual_tests",
    defaultValue = """"false"""",
    help = """      
      Forces test targets tagged 'manual' to be built. 'manual' tests are excluded 
      from processing. This option forces them to be built (but not executed).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val buildManualTests = Flag.Boolean("buildManualTests")

  //   --build_metadata (a 'name=value' assignment; may be used multiple times)
  @Option(
    name = "build_metadata",
    allowMultiple = true,
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """Custom key-value string pairs to supply in a build event.""",
    valueHelp = """a 'name=value' assignment""",
  )
  @JvmField
  @Suppress("unused")
  val buildMetadata = Flag.Unknown("buildMetadata")

  //   --build_tag_filters (comma-separated list of options; default: "")
  @Option(
    name = "build_tag_filters",
    defaultValue = """""""",
    help = """      
      Specifies a comma-separated list of tags. Each tag can be optionally preceded 
      with '-' to specify excluded tags. Only those targets will be built that 
      contain at least one included tag and do not contain any excluded tags. This 
      option does not affect the set of tests executed with the 'test' command; those 
      are be governed by the test filtering options, for example '--test_tag_filters'
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val buildTagFilters = Flag.Unknown("buildTagFilters")

  //   --[no]build_tests_only (a boolean; default: "false")
  @Option(
    name = "build_tests_only",
    defaultValue = """"false"""",
    help = """      
      If specified, only *_test and test_suite rules will be built and other targets 
      specified on the command line will be ignored. By default everything that was 
      requested will be built.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val buildTestsOnly = Flag.Boolean("buildTestsOnly")

  //   --[no]cache_test_results [-t] (a tri-state (auto, yes, no); default: "auto")
  @Option(
    name = "cache_test_results",
    abbrev = 't',
    defaultValue = """"auto"""",
    help = """      
      If set to 'auto', Bazel reruns a test if and only if: (1) Bazel detects changes 
      in the test or its dependencies, (2) the test is marked as external, (3) 
      multiple test runs were requested with --runs_per_test, or(4) the test 
      previously failed. If set to 'yes', Bazel caches all test results except for 
      tests marked as external. If set to 'no', Bazel does not cache any test results.
      """,
    valueHelp = """a tri-state (auto, yes, no)""",
  )
  @JvmField
  @Suppress("unused")
  val cacheTestResults = Flag.TriState("cacheTestResults")

  //   --color (yes, no or auto; default: "auto")
  @Option(
    name = "color",
    defaultValue = """"auto"""",
    help = """Use terminal controls to colorize output.""",
    valueHelp = """yes, no or auto""",
  )
  @JvmField
  @Suppress("unused")
  val color = Flag.OneOf("color")

  //   --combined_report (none or lcov; default: "none")
  @Option(
    name = "combined_report",
    defaultValue = """"none"""",
    help = """      
      Specifies desired cumulative coverage report type. At this point only LCOV is 
      supported.
      """,
    valueHelp = """none or lcov""",
  )
  @JvmField
  @Suppress("unused")
  val combinedReport = Flag.OneOf("combinedReport")

  //   --[no]compile_one_dependency (a boolean; default: "false")
  @Option(
    name = "compile_one_dependency",
    defaultValue = """"false"""",
    help = """      
      Compile a single dependency of the argument files. This is useful for syntax 
      checking source files in IDEs, for example, by rebuilding a single target that 
      depends on the source file to detect errors as early as possible in the 
      edit/build/test cycle. This argument affects the way all non-flag arguments are 
      interpreted; instead of being targets to build they are source filenames.  For 
      each source filename an arbitrary target that depends on it will be built.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val compileOneDependency = Flag.Boolean("compileOneDependency")

  //   --config (a string; may be used multiple times)
  @Option(
    name = "config",
    allowMultiple = true,
    help = """      
      Selects additional config sections from the rc files; for every <command>, it 
      also pulls in the options from <command>:<config> if such a section exists; if 
      this section doesn't exist in any .rc file, Blaze fails with an error. The 
      config sections and flag combinations they are equivalent to are located in the 
      tools/*.blazerc config files.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val config = Flag.Str("config")

  //
  //   --credential_helper (Path to a credential helper. It may be absolute, relative to the PATH
  // environment variable, or %workspace%-relative. The path be optionally prefixed by a scope  followed
  // by an '='. The scope is a domain name, optionally with a single leading '*' wildcard component. A
  // helper applies to URIs matching its scope, with more specific scopes preferred. If a helper has no
  // scope, it applies to every URI.; may be used multiple times)
  //
  @Option(
    name = "credential_helper",
    allowMultiple = true,
    help = """      
      Configures a credential helper conforming to the <a 
      href="https://github.com/EngFlow/credential-helper-spec">Credential Helper 
      Specification</a> to use for retrieving authorization credentials for  
      repository fetching, remote caching and execution, and the build event service.
      
      Credentials supplied by a helper take precedence over credentials supplied by 
      `--google_default_credentials`, `--google_credentials`, a `.netrc` file, or the 
      auth parameter to `repository_ctx.download()` and 
      `repository_ctx.download_and_extract()`.
      
      May be specified multiple times to set up multiple helpers.
      
      See https://blog.engflow.com/2023/10/09/configuring-bazels-credential-helper/ 
      for instructions.
      """,
    valueHelp = """      
      Path to a credential helper. It may be absolute, relative to the PATH 
      environment variable, or %workspace%-relative. The path be optionally prefixed 
      by a scope  followed by an '='. The scope is a domain name, optionally with a 
      single leading '*' wildcard component. A helper applies to URIs matching its 
      scope, with more specific scopes preferred. If a helper has no scope, it 
      applies to every URI.
      """,
  )
  @JvmField
  @Suppress("unused")
  val credentialHelper = Flag.Unknown("credentialHelper")

  //   --credential_helper_cache_duration (An immutable length of time.; default: "30m")
  @Option(
    name = "credential_helper_cache_duration",
    defaultValue = """"30m"""",
    help = """      
      The default duration for which credentials supplied by a credential helper are 
      cached if the helper does not provide when the credentials expire.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val credentialHelperCacheDuration = Flag.Duration("credentialHelperCacheDuration")

  //   --credential_helper_timeout (An immutable length of time.; default: "10s")
  @Option(
    name = "credential_helper_timeout",
    defaultValue = """"10s"""",
    help = """      
      Configures the timeout for a credential helper.
      
      Credential helpers failing to respond within this timeout will fail the 
      invocation.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val credentialHelperTimeout = Flag.Duration("credentialHelperTimeout")

  //   --curses (yes, no or auto; default: "auto")
  @Option(
    name = "curses",
    defaultValue = """"auto"""",
    help = """Use terminal cursor controls to minimize scrolling output.""",
    valueHelp = """yes, no or auto""",
  )
  @JvmField
  @Suppress("unused")
  val curses = Flag.OneOf("curses")

  //   --deleted_packages (comma-separated list of package names; may be used multiple times)
  @Option(
    name = "deleted_packages",
    allowMultiple = true,
    help = """      
      A comma-separated list of names of packages which the build system will 
      consider non-existent, even if they are visible somewhere on the package 
      path.Use this option when deleting a subpackage 'x/y' of an existing package 
      'x'.  For example, after deleting x/y/BUILD in your client, the build system 
      may complain if it encounters a label '//x:y/z' if that is still provided by 
      another package_path entry.  Specifying --deleted_packages x/y avoids this 
      problem.
      """,
    valueHelp = """comma-separated list of package names""",
  )
  @JvmField
  @Suppress("unused")
  val deletedPackages = Flag.Unknown("deletedPackages")

  //   --[no]discard_analysis_cache (a boolean; default: "false")
  @Option(
    name = "discard_analysis_cache",
    defaultValue = """"false"""",
    help = """      
      Discard the analysis cache immediately after the analysis phase completes. 
      Reduces memory usage by ~10%, but makes further incremental builds slower.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val discardAnalysisCache = Flag.Boolean("discardAnalysisCache")

  //   --disk_cache (a path; default: see description)
  @Option(
    name = "disk_cache",
    help = """      
      A path to a directory where Bazel can read and write actions and action 
      outputs. If the directory does not exist, it will be created.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val diskCache = Flag.Path("diskCache")

  //   --embed_label (a one-line string; default: "")
  @Option(
    name = "embed_label",
    defaultValue = """""""",
    help = """Embed source control revision or release label in binary""",
    valueHelp = """a one-line string""",
  )
  @JvmField
  @Suppress("unused")
  val embedLabel = Flag.Unknown("embedLabel")

  //   --[no]enable_platform_specific_config (a boolean; default: "false")
  @Option(
    name = "enable_platform_specific_config",
    defaultValue = """"false"""",
    help = """      
      If true, Bazel picks up host-OS-specific config lines from bazelrc files. For 
      example, if the host OS is Linux and you run bazel build, Bazel picks up lines 
      starting with build:linux. Supported OS identifiers are linux, macos, windows, 
      freebsd, and openbsd. Enabling this flag is equivalent to using --config=linux 
      on Linux, --config=windows on Windows, etc.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val enablePlatformSpecificConfig = Flag.Boolean("enablePlatformSpecificConfig")

  //   --execution_log_binary_file (a path; default: see description)
  @Option(
    name = "execution_log_binary_file",
    help = """      
      Log the executed spawns into this file as length-delimited SpawnExec protos, 
      according to src/main/protobuf/spawn.proto. Related flags: 
      --execution_log_json_file (text JSON format; mutually exclusive), 
      --execution_log_sort (whether to sort the execution log), --subcommands (for 
      displaying subcommands in terminal output).
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val executionLogBinaryFile = Flag.Path("executionLogBinaryFile")

  //   --execution_log_json_file (a path; default: see description)
  @Option(
    name = "execution_log_json_file",
    help = """      
      Log the executed spawns into this file as newline-delimited JSON 
      representations of SpawnExec protos, according to 
      src/main/protobuf/spawn.proto. Related flags: --execution_log_binary_file 
      (binary protobuf format; mutually exclusive), --execution_log_sort (whether to 
      sort the execution log), --subcommands (for displaying subcommands in terminal 
      output).
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val executionLogJsonFile = Flag.Path("executionLogJsonFile")

  //   --[no]execution_log_sort (a boolean; default: "true")
  @Option(
    name = "execution_log_sort",
    defaultValue = """"true"""",
    help = """      
      Whether to sort the execution log, making it easier to compare logs across 
      invocations. Set to false to avoid potentially significant CPU and memory usage 
      at the end of the invocation, at the cost of producing the log in 
      nondeterministic execution order. Only applies to the binary and JSON formats; 
      the compact format is never sorted.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val executionLogSort = Flag.Boolean("executionLogSort")

  //   --[no]expand_test_suites (a boolean; default: "true")
  @Option(
    name = "expand_test_suites",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Expand test_suite targets into their constituent tests before analysis. When 
      this flag is turned on (the default), negative target patterns will apply to 
      the tests belonging to the test suite, otherwise they will not. Turning off 
      this flag is useful when top-level aspects are applied at command line: then 
      they can analyze test_suite targets.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val expandTestSuites = Flag.Boolean("expandTestSuites")

  //   --[no]experimental_cancel_concurrent_tests (a boolean; default: "false")
  @Option(
    name = "experimental_cancel_concurrent_tests",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If true, then Blaze will cancel concurrently running tests on the first 
      successful run. This is only useful in combination with 
      --runs_per_test_detects_flakes.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCancelConcurrentTests = Flag.Boolean("experimentalCancelConcurrentTests")

  //   --experimental_execution_log_compact_file (a path; default: see description)
  @Option(
    name = "experimental_execution_log_compact_file",
    help = """      
      Log the executed spawns into this file as length-delimited ExecLogEntry protos, 
      according to src/main/protobuf/spawn.proto. The entire file is zstd compressed. 
      This is an experimental format under active development, and may change at any 
      time. Related flags: --execution_log_binary_file (binary protobuf format; 
      mutually exclusive), --execution_log_json_file (text JSON format; mutually 
      exclusive), --subcommands (for displaying subcommands in terminal output).
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalExecutionLogCompactFile = Flag.Path("experimentalExecutionLogCompactFile")

  //
  //   --experimental_extra_action_filter (a comma-separated list of regex expressions with prefix '-'
  // specifying excluded paths; default: "")
  //
  @Option(
    name = "experimental_extra_action_filter",
    defaultValue = """""""",
    help = """      
      Deprecated in favor of aspects. Filters set of targets to schedule 
      extra_actions for.
      """,
    valueHelp = """      
      a comma-separated list of regex expressions with prefix '-' specifying excluded 
      paths
      """,
  )
  @JvmField
  @Suppress("unused")
  val experimentalExtraActionFilter = Flag.Unknown("experimentalExtraActionFilter")

  //   --[no]experimental_extra_action_top_level_only (a boolean; default: "false")
  @Option(
    name = "experimental_extra_action_top_level_only",
    defaultValue = """"false"""",
    help = """      
      Deprecated in favor of aspects. Only schedules extra_actions for top level 
      targets.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalExtraActionTopLevelOnly = Flag.Boolean("experimentalExtraActionTopLevelOnly")

  //   --[no]experimental_fetch_all_coverage_outputs (a boolean; default: "false")
  @Option(
    name = "experimental_fetch_all_coverage_outputs",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If true, then Bazel fetches the entire coverage data directory for each test 
      during a coverage run.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalFetchAllCoverageOutputs = Flag.Boolean("experimentalFetchAllCoverageOutputs")

  //   --[no]experimental_generate_llvm_lcov (a boolean; default: "false")
  @Option(
    name = "experimental_generate_llvm_lcov",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """If true, coverage for clang will generate an LCOV report.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalGenerateLlvmLcov = Flag.Boolean("experimentalGenerateLlvmLcov")

  //   --[no]experimental_j2objc_header_map (a boolean; default: "true")
  @Option(
    name = "experimental_j2objc_header_map",
    defaultValue = """"true"""",
    help = """Whether to generate J2ObjC header map in parallel of J2ObjC transpilation.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalJ2objcHeaderMap = Flag.Boolean("experimentalJ2objcHeaderMap")

  //   --[no]experimental_j2objc_shorter_header_path (a boolean; default: "false")
  @Option(
    name = "experimental_j2objc_shorter_header_path",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Whether to generate with shorter header path (uses "_ios" instead of "_j2objc").""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalJ2objcShorterHeaderPath = Flag.Boolean("experimentalJ2objcShorterHeaderPath")

  //   --experimental_java_classpath (off, javabuilder or bazel; default: "javabuilder")
  @Option(
    name = "experimental_java_classpath",
    defaultValue = """"javabuilder"""",
    help = """Enables reduced classpaths for Java compilations.""",
    valueHelp = """off, javabuilder or bazel""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalJavaClasspath = Flag.OneOf("experimentalJavaClasspath")

  //   --[no]experimental_limit_android_lint_to_android_constrained_java (a boolean; default: "false")
  @Option(
    name = "experimental_limit_android_lint_to_android_constrained_java",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      Limit --experimental_run_android_lint_on_java_rules to Android-compatible 
      libraries.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalLimitAndroidLintToAndroidConstrainedJava = Flag.Boolean("experimentalLimitAndroidLintToAndroidConstrainedJava")

  //   --[no]experimental_rule_extension_api (a boolean; default: "false")
  @Option(
    name = "experimental_rule_extension_api",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.EXPERIMENTAL],
    help = """Enable experimental rule extension API and subrule APIs""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRuleExtensionApi = Flag.Boolean("experimentalRuleExtensionApi")

  //   --[no]experimental_run_android_lint_on_java_rules (a boolean; default: "false")
  @Option(
    name = "experimental_run_android_lint_on_java_rules",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Whether to validate java_* sources.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalRunAndroidLintOnJavaRules = Flag.Boolean("experimentalRunAndroidLintOnJavaRules")

  //   --experimental_spawn_scheduler
  @Option(
    name = "experimental_spawn_scheduler",
    expandsTo = [ "--internal_spawn_scheduler", "--spawn_strategy=dynamic"],
    help = """      
      Enable dynamic execution by running actions locally and remotely in parallel. 
      Bazel spawns each action locally and remotely and picks the one that completes 
      first. If an action supports workers, the local action will be run in the 
      persistent worker mode. To enable dynamic execution for an individual action 
      mnemonic, use the `--internal_spawn_scheduler` and 
      `--strategy=<mnemonic>=dynamic` flags instead.
      """,
  )
  @JvmField
  @Suppress("unused")
  val experimentalSpawnScheduler = Flag.Unknown("experimentalSpawnScheduler")

  //   --[no]experimental_windows_watchfs (a boolean; default: "false")
  @Option(
    name = "experimental_windows_watchfs",
    defaultValue = """"false"""",
    help = """      
      If true, experimental Windows support for --watchfs is enabled. Otherwise 
      --watchfsis a non-op on Windows. Make sure to also enable --watchfs.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalWindowsWatchfs = Flag.Boolean("experimentalWindowsWatchfs")

  //   --[no]explicit_java_test_deps (a boolean; default: "false")
  @Option(
    name = "explicit_java_test_deps",
    defaultValue = """"false"""",
    help = """      
      Explicitly specify a dependency to JUnit or Hamcrest in a java_test instead of  
      accidentally obtaining from the TestRunner's deps. Only works for bazel right 
      now.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val explicitJavaTestDeps = Flag.Boolean("explicitJavaTestDeps")

  //   --[no]fetch (a boolean; default: "true")
  @Option(
    name = "fetch",
    defaultValue = """"true"""",
    help = """      
      Allows the command to fetch external dependencies. If set to false, the command 
      will utilize any cached version of the dependency, and if none exists, the 
      command will result in failure.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val fetch = Flag.Boolean("fetch")

  //
  //   --google_auth_scopes (comma-separated list of options; default:
  // "https://www.googleapis.com/auth/cloud-platform")
  //
  @Option(
    name = "google_auth_scopes",
    defaultValue = """"https://www.googleapis.com/auth/cloud-platform"""",
    help = """A comma-separated list of Google Cloud authentication scopes.""",
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val googleAuthScopes = Flag.Unknown("googleAuthScopes")

  //   --google_credentials (a string; default: see description)
  @Option(
    name = "google_credentials",
    help = """      
      Specifies the file to get authentication credentials from. See 
      https://cloud.google.com/docs/authentication for details.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val googleCredentials = Flag.Str("googleCredentials")

  //   --[no]google_default_credentials (a boolean; default: "false")
  @Option(
    name = "google_default_credentials",
    defaultValue = """"false"""",
    help = """      
      Whether to use 'Google Application Default Credentials' for authentication. See 
      https://cloud.google.com/docs/authentication for details. Disabled by default.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val googleDefaultCredentials = Flag.Boolean("googleDefaultCredentials")

  //   --grpc_keepalive_time (An immutable length of time.; default: see description)
  @Option(
    name = "grpc_keepalive_time",
    help = """      
      Configures keep-alive pings for outgoing gRPC connections. If this is set, then 
      Bazel sends pings after this much time of no read operations on the connection, 
      but only if there is at least one pending gRPC call. Times are treated as 
      second granularity; it is an error to set a value less than one second. By 
      default, keep-alive pings are disabled. You should coordinate with the service 
      owner before enabling this setting. For example to set a value of 30 seconds to 
      this flag, it should be done as this --grpc_keepalive_time=30s
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val grpcKeepaliveTime = Flag.Duration("grpcKeepaliveTime")

  //   --grpc_keepalive_timeout (An immutable length of time.; default: "20s")
  @Option(
    name = "grpc_keepalive_timeout",
    defaultValue = """"20s"""",
    help = """      
      Configures a keep-alive timeout for outgoing gRPC connections. If keep-alive 
      pings are enabled with --grpc_keepalive_time, then Bazel times out a connection 
      if it does not receive a ping reply after this much time. Times are treated as 
      second granularity; it is an error to set a value less than one second. If 
      keep-alive pings are disabled, then this setting is ignored.
      """,
    valueHelp = """An immutable length of time.""",
  )
  @JvmField
  @Suppress("unused")
  val grpcKeepaliveTimeout = Flag.Duration("grpcKeepaliveTimeout")

  //   --host_java_launcher (a build target label; default: see description)
  @Option(
    name = "host_java_launcher",
    help = """The Java launcher used by tools that are executed during a build.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val hostJavaLauncher = Flag.Label("hostJavaLauncher")

  //   --host_javacopt (a string; may be used multiple times)
  @Option(
    name = "host_javacopt",
    allowMultiple = true,
    help = """      
      Additional options to pass to javac when building tools that are executed 
      during a build.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostJavacopt = Flag.Str("hostJavacopt")

  //   --host_jvmopt (a string; may be used multiple times)
  @Option(
    name = "host_jvmopt",
    allowMultiple = true,
    help = """      
      Additional options to pass to the Java VM when building tools that are executed 
      during  the build. These options will get added to the VM startup options of 
      each  java_binary target.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostJvmopt = Flag.Str("hostJvmopt")

  //   --[no]incompatible_check_sharding_support (a boolean; default: "true")
  @Option(
    name = "incompatible_check_sharding_support",
    defaultValue = """"true"""",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, Bazel will fail a sharded test if the test runner does not indicate 
      that it supports sharding by touching the file at the path in 
      TEST_SHARD_STATUS_FILE. If false, a test runner that does not support sharding 
      will lead to all tests running in each shard.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleCheckShardingSupport = Flag.Boolean("incompatibleCheckShardingSupport")

  //   --[no]incompatible_disable_non_executable_java_binary (a boolean; default: "false")
  @Option(
    name = "incompatible_disable_non_executable_java_binary",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, java_binary is always executable. create_executable attribute is 
      removed.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisableNonExecutableJavaBinary = Flag.Boolean("incompatibleDisableNonExecutableJavaBinary")

  //   --[no]incompatible_disallow_symlink_file_to_dir (a boolean; default: "true")
  @Option(
    name = "incompatible_disallow_symlink_file_to_dir",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """No-op.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDisallowSymlinkFileToDir = Flag.Boolean("incompatibleDisallowSymlinkFileToDir")

  //   --[no]incompatible_dont_use_javasourceinfoprovider (a boolean; default: "false")
  @Option(
    name = "incompatible_dont_use_javasourceinfoprovider",
    defaultValue = """"false"""",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """No-op""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleDontUseJavasourceinfoprovider = Flag.Boolean("incompatibleDontUseJavasourceinfoprovider")

  //   --[no]incompatible_exclusive_test_sandboxed (a boolean; default: "true")
  @Option(
    name = "incompatible_exclusive_test_sandboxed",
    defaultValue = """"true"""",
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, exclusive tests will run with sandboxed strategy. Add 'local' tag to 
      force an exclusive test run locally
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleExclusiveTestSandboxed = Flag.Boolean("incompatibleExclusiveTestSandboxed")

  //   --[no]incompatible_strict_action_env (a boolean; default: "false")
  @Option(
    name = "incompatible_strict_action_env",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If true, Bazel uses an environment with a static value for PATH and does not 
      inherit LD_LIBRARY_PATH. Use --action_env=ENV_VARIABLE if you want to inherit 
      specific environment variables from the client, but note that doing so can 
      prevent cross-user caching if a shared cache is used.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatibleStrictActionEnv = Flag.Boolean("incompatibleStrictActionEnv")

  //   --j2objc_translation_flags (comma-separated list of options; may be used multiple times)
  @Option(
    name = "j2objc_translation_flags",
    allowMultiple = true,
    help = """Additional options to pass to the J2ObjC tool.""",
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val j2objcTranslationFlags = Flag.Unknown("j2objcTranslationFlags")

  //   --java_debug
  @Option(
    name = "java_debug",
    expandsTo = [
      "--test_arg=--wrapper_script_flag=--debug", "--test_output=streamed",
      "--test_strategy=exclusive", "--test_timeout=9999", "--nocache_test_results",
    ],
    help = """      
      Causes the Java virtual machine of a java test to wait for a connection from a 
      JDWP-compliant debugger (such as jdb) before starting the test. Implies 
      -test_output=streamed.
      """,
  )
  @JvmField
  @Suppress("unused")
  val javaDebug = Flag.Unknown("javaDebug")

  //   --[no]java_deps (a boolean; default: "true")
  @Option(
    name = "java_deps",
    defaultValue = """"true"""",
    help = """      
      Generate dependency information (for now, compile-time classpath) per Java 
      target.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val javaDeps = Flag.Boolean("javaDeps")

  //   --[no]java_header_compilation (a boolean; default: "true")
  @Option(
    name = "java_header_compilation",
    defaultValue = """"true"""",
    help = """Compile ijars directly from source.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val javaHeaderCompilation = Flag.Boolean("javaHeaderCompilation")

  //   --java_language_version (a string; default: "")
  @Option(
    name = "java_language_version",
    defaultValue = """""""",
    help = """The Java language version""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val javaLanguageVersion = Flag.Str("javaLanguageVersion")

  //   --java_launcher (a build target label; default: see description)
  @Option(
    name = "java_launcher",
    help = """      
      The Java launcher to use when building Java binaries.  If this flag is set to 
      the empty string, the JDK launcher is used. The "launcher" attribute overrides 
      this flag. 
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val javaLauncher = Flag.Label("javaLauncher")

  //   --java_runtime_version (a string; default: "local_jdk")
  @Option(
    name = "java_runtime_version",
    defaultValue = """"local_jdk"""",
    help = """The Java runtime version""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val javaRuntimeVersion = Flag.Str("javaRuntimeVersion")

  //   --javacopt (a string; may be used multiple times)
  @Option(
    name = "javacopt",
    allowMultiple = true,
    help = """Additional options to pass to javac.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val javacopt = Flag.Str("javacopt")

  //   --jvmopt (a string; may be used multiple times)
  @Option(
    name = "jvmopt",
    allowMultiple = true,
    help = """      
      Additional options to pass to the Java VM. These options will get added to the 
      VM startup options of each java_binary target.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val jvmopt = Flag.Str("jvmopt")

  //   --legacy_main_dex_list_generator (a build target label; default: see description)
  @Option(
    name = "legacy_main_dex_list_generator",
    help = """      
      Specifies a binary to use to generate the list of classes that must be in the 
      main dex when compiling legacy multidex.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val legacyMainDexListGenerator = Flag.Label("legacyMainDexListGenerator")

  //   --local_termination_grace_seconds (an integer; default: "15")
  @Option(
    name = "local_termination_grace_seconds",
    defaultValue = """"15"""",
    help = """      
      Time to wait between terminating a local process due to timeout and forcefully 
      shutting it down.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val localTerminationGraceSeconds = Flag.Integer("localTerminationGraceSeconds")

  //   --optimizing_dexer (a build target label; default: see description)
  @Option(
    name = "optimizing_dexer",
    help = """Specifies a binary to use to do dexing without sharding.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val optimizingDexer = Flag.Label("optimizingDexer")

  //
  //   --override_repository (an equals-separated mapping of repository name to path; may be used
  // multiple times)
  //
  @Option(
    name = "override_repository",
    allowMultiple = true,
    help = """      
      Override a repository with a local path in the form of <repository 
      name>=<path>. If the given path is an absolute path, it will be used as it is. 
      If the given path is a relative path, it is relative to the current working 
      directory. If the given path starts with '%workspace%, it is relative to the 
      workspace root, which is the output of `bazel info workspace`. If the given 
      path is empty, then remove any previous overrides.
      """,
    valueHelp = """an equals-separated mapping of repository name to path""",
  )
  @JvmField
  @Suppress("unused")
  val overrideRepository = Flag.Unknown("overrideRepository")

  //   --package_path (colon-separated list of options; default: "%workspace%")
  @Option(
    name = "package_path",
    defaultValue = """"%workspace%"""",
    help = """      
      A colon-separated list of where to look for packages. Elements beginning with 
      '%workspace%' are relative to the enclosing workspace. If omitted or empty, the 
      default is the output of 'bazel info default-package-path'.
      """,
    valueHelp = """colon-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val packagePath = Flag.Unknown("packagePath")

  //   --plugin (a build target label; may be used multiple times)
  @Option(
    name = "plugin",
    allowMultiple = true,
    help = """Plugins to use in the build. Currently works with java_plugin.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val plugin = Flag.Label("plugin")

  //   --[no]progress_in_terminal_title (a boolean; default: "false")
  @Option(
    name = "progress_in_terminal_title",
    defaultValue = """"false"""",
    help = """      
      Show the command progress in the terminal title. Useful to see what bazel is 
      doing when having multiple terminal tabs.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val progressInTerminalTitle = Flag.Boolean("progressInTerminalTitle")

  //   --proguard_top (a build target label; default: see description)
  @Option(
    name = "proguard_top",
    help = """      
      Specifies which version of ProGuard to use for code removal when building a 
      Java binary.
      """,
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val proguardTop = Flag.Label("proguardTop")

  //   --proto_compiler (a build target label; default: "@bazel_tools//tools/proto:protoc")
  @Option(
    name = "proto_compiler",
    defaultValue = """"@bazel_tools//tools/proto:protoc"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """The label of the proto-compiler.""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val protoCompiler = Flag.Label("protoCompiler")

  //   --proto_toolchain_for_cc (a build target label; default: "@bazel_tools//tools/proto:cc_toolchain")
  @Option(
    name = "proto_toolchain_for_cc",
    defaultValue = """"@bazel_tools//tools/proto:cc_toolchain"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Label of proto_lang_toolchain() which describes how to compile C++ protos""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val protoToolchainForCc = Flag.Label("protoToolchainForCc")

  //
  //   --proto_toolchain_for_j2objc (a build target label; default:
  // "@bazel_tools//tools/j2objc:j2objc_proto_toolchain")
  //
  @Option(
    name = "proto_toolchain_for_j2objc",
    defaultValue = """"@bazel_tools//tools/j2objc:j2objc_proto_toolchain"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Label of proto_lang_toolchain() which describes how to compile j2objc protos""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val protoToolchainForJ2objc = Flag.Label("protoToolchainForJ2objc")

  //
  //   --proto_toolchain_for_java (a build target label; default:
  // "@bazel_tools//tools/proto:java_toolchain")
  //
  @Option(
    name = "proto_toolchain_for_java",
    defaultValue = """"@bazel_tools//tools/proto:java_toolchain"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Label of proto_lang_toolchain() which describes how to compile Java protos""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val protoToolchainForJava = Flag.Label("protoToolchainForJava")

  //
  //   --proto_toolchain_for_javalite (a build target label; default:
  // "@bazel_tools//tools/proto:javalite_toolchain")
  //
  @Option(
    name = "proto_toolchain_for_javalite",
    defaultValue = """"@bazel_tools//tools/proto:javalite_toolchain"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """Label of proto_lang_toolchain() which describes how to compile JavaLite protos""",
    valueHelp = """a build target label""",
  )
  @JvmField
  @Suppress("unused")
  val protoToolchainForJavalite = Flag.Label("protoToolchainForJavalite")

  //   --protocopt (a string; may be used multiple times)
  @Option(
    name = "protocopt",
    allowMultiple = true,
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """Additional options to pass to the protobuf compiler.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val protocopt = Flag.Str("protocopt")

  //   --[no]runs_per_test_detects_flakes (a boolean; default: "false")
  @Option(
    name = "runs_per_test_detects_flakes",
    defaultValue = """"false"""",
    help = """      
      If true, any shard in which at least one run/attempt passes and at least one 
      run/attempt fails gets a FLAKY status.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val runsPerTestDetectsFlakes = Flag.Boolean("runsPerTestDetectsFlakes")

  //   --shell_executable (a path; default: see description)
  @Option(
    name = "shell_executable",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      Absolute path to the shell executable for Bazel to use. If this is unset, but 
      the BAZEL_SH environment variable is set on the first Bazel invocation (that 
      starts up a Bazel server), Bazel uses that. If neither is set, Bazel uses a 
      hard-coded default path depending on the operating system it runs on (Windows: 
      c:/tools/msys64/usr/bin/bash.exe, FreeBSD: /usr/local/bin/bash, all others: 
      /bin/bash). Note that using a shell that is not compatible with bash may lead 
      to build failures or runtime failures of the generated binaries.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val shellExecutable = Flag.Path("shellExecutable")

  //   --[no]show_loading_progress (a boolean; default: "true")
  @Option(
    name = "show_loading_progress",
    defaultValue = """"true"""",
    help = """If enabled, causes Bazel to print "Loading package:" messages.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val showLoadingProgress = Flag.Boolean("showLoadingProgress")

  //   --[no]show_progress (a boolean; default: "true")
  @Option(
    name = "show_progress",
    defaultValue = """"true"""",
    help = """Display progress messages during a build.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val showProgress = Flag.Boolean("showProgress")

  //   --show_progress_rate_limit (a double; default: "0.2")
  @Option(
    name = "show_progress_rate_limit",
    defaultValue = """"0.2"""",
    help = """Minimum number of seconds between progress messages in the output.""",
    valueHelp = """a double""",
  )
  @JvmField
  @Suppress("unused")
  val showProgressRateLimit = Flag.Double("showProgressRateLimit")

  //   --[no]show_timestamps (a boolean; default: "false")
  @Option(
    name = "show_timestamps",
    defaultValue = """"false"""",
    help = """Include timestamps in messages""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val showTimestamps = Flag.Boolean("showTimestamps")

  //   --test_arg (a string; may be used multiple times)
  @Option(
    name = "test_arg",
    allowMultiple = true,
    help = """      
      Specifies additional options and arguments that should be passed to the test 
      executable. Can be used multiple times to specify several arguments. If 
      multiple tests are executed, each of them will receive identical arguments. 
      Used only by the 'bazel test' command.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val testArg = Flag.Str("testArg")

  //   --test_filter (a string; default: see description)
  @Option(
    name = "test_filter",
    help = """      
      Specifies a filter to forward to the test framework.  Used to limit the tests 
      run. Note that this does not affect which targets are built.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val testFilter = Flag.Str("testFilter")

  //   --test_lang_filters (comma-separated list of options; default: "")
  @Option(
    name = "test_lang_filters",
    defaultValue = """""""",
    help = """      
      Specifies a comma-separated list of test languages. Each language can be 
      optionally preceded with '-' to specify excluded languages. Only those test 
      targets will be found that are written in the specified languages. The name 
      used for each language should be the same as the language prefix in the *_test 
      rule, e.g. one of 'cc', 'java', 'py', etc. This option affects 
      --build_tests_only behavior and the test command.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val testLangFilters = Flag.Unknown("testLangFilters")

  //   --test_result_expiration (an integer; default: "-1")
  @Option(
    name = "test_result_expiration",
    defaultValue = """"-1"""",
    help = """This option is deprecated and has no effect.""",
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val testResultExpiration = Flag.Integer("testResultExpiration")

  //   --[no]test_runner_fail_fast (a boolean; default: "false")
  @Option(
    name = "test_runner_fail_fast",
    defaultValue = """"false"""",
    help = """      
      Forwards fail fast option to the test runner. The test runner should stop 
      execution upon first failure.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val testRunnerFailFast = Flag.Boolean("testRunnerFailFast")

  //
  //   --test_sharding_strategy (explicit, disabled or forced=k where k is the number of shards to
  // enforce; default: "explicit")
  //
  @Option(
    name = "test_sharding_strategy",
    defaultValue = """"explicit"""",
    help = """      
      Specify strategy for test sharding: 'explicit' to only use sharding if the 
      'shard_count' BUILD attribute is present. 'disabled' to never use test 
      sharding. 'forced=k' to enforce 'k' shards for testing regardless of the 
      'shard_count' BUILD attribute.
      """,
    valueHelp = """explicit, disabled or forced=k where k is the number of shards to enforce""",
  )
  @JvmField
  @Suppress("unused")
  val testShardingStrategy = Flag.Unknown("testShardingStrategy")

  //
  //   --test_size_filters (comma-separated list of values: small, medium, large or enormous; default:
  // "")
  //
  @Option(
    name = "test_size_filters",
    defaultValue = """""""",
    help = """      
      Specifies a comma-separated list of test sizes. Each size can be optionally 
      preceded with '-' to specify excluded sizes. Only those test targets will be 
      found that contain at least one included size and do not contain any excluded 
      sizes. This option affects --build_tests_only behavior and the test command.
      """,
    valueHelp = """comma-separated list of values: small, medium, large or enormous""",
  )
  @JvmField
  @Suppress("unused")
  val testSizeFilters = Flag.Unknown("testSizeFilters")

  //   --test_tag_filters (comma-separated list of options; default: "")
  @Option(
    name = "test_tag_filters",
    defaultValue = """""""",
    help = """      
      Specifies a comma-separated list of test tags. Each tag can be optionally 
      preceded with '-' to specify excluded tags. Only those test targets will be 
      found that contain at least one included tag and do not contain any excluded 
      tags. This option affects --build_tests_only behavior and the test command.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val testTagFilters = Flag.Unknown("testTagFilters")

  //
  //   --test_timeout_filters (comma-separated list of values: short, moderate, long or eternal;
  // default: "")
  //
  @Option(
    name = "test_timeout_filters",
    defaultValue = """""""",
    help = """      
      Specifies a comma-separated list of test timeouts. Each timeout can be 
      optionally preceded with '-' to specify excluded timeouts. Only those test 
      targets will be found that contain at least one included timeout and do not 
      contain any excluded timeouts. This option affects --build_tests_only behavior 
      and the test command.
      """,
    valueHelp = """comma-separated list of values: short, moderate, long or eternal""",
  )
  @JvmField
  @Suppress("unused")
  val testTimeoutFilters = Flag.Unknown("testTimeoutFilters")

  //   --tls_certificate (a string; default: see description)
  @Option(
    name = "tls_certificate",
    help = """Specify a path to a TLS certificate that is trusted to sign server certificates.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val tlsCertificate = Flag.Str("tlsCertificate")

  //   --tls_client_certificate (a string; default: see description)
  @Option(
    name = "tls_client_certificate",
    help = """      
      Specify the TLS client certificate to use; you also need to provide a client 
      key to enable client authentication.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val tlsClientCertificate = Flag.Str("tlsClientCertificate")

  //   --tls_client_key (a string; default: see description)
  @Option(
    name = "tls_client_key",
    help = """      
      Specify the TLS client key to use; you also need to provide a client 
      certificate to enable client authentication.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val tlsClientKey = Flag.Str("tlsClientKey")

  //   --tool_java_language_version (a string; default: "")
  @Option(
    name = "tool_java_language_version",
    defaultValue = """""""",
    help = """      
      The Java language version used to execute the tools that are needed during a 
      build
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val toolJavaLanguageVersion = Flag.Str("toolJavaLanguageVersion")

  //   --tool_java_runtime_version (a string; default: "remotejdk_11")
  @Option(
    name = "tool_java_runtime_version",
    defaultValue = """"remotejdk_11"""",
    help = """The Java runtime version used to execute tools during the build""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val toolJavaRuntimeVersion = Flag.Str("toolJavaRuntimeVersion")

  //   --ui_actions_shown (an integer; default: "8")
  @Option(
    name = "ui_actions_shown",
    defaultValue = """"8"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Number of concurrent actions shown in the detailed progress bar; each action is 
      shown on a separate line. The progress bar always shows at least one one, all 
      numbers less than 1 are mapped to 1.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val uiActionsShown = Flag.Integer("uiActionsShown")

  //   --[no]use_ijars (a boolean; default: "true")
  @Option(
    name = "use_ijars",
    defaultValue = """"true"""",
    help = """      
      If enabled, this option causes Java compilation to use interface jars. This 
      will result in faster incremental compilation, but error messages can be 
      different.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val useIjars = Flag.Boolean("useIjars")

  //   --[no]watchfs (a boolean; default: "false")
  @Option(
    name = "watchfs",
    defaultValue = """"false"""",
    help = """      
      On Linux/macOS: If true, bazel tries to use the operating system's file watch 
      service for local changes instead of scanning every file for a change. On 
      Windows: this flag currently is a non-op but can be enabled in conjunction with 
      --experimental_windows_watchfs. On any OS: The behavior is undefined if your 
      workspace is on a network file system, and files are edited on a remote machine.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val watchfs = Flag.Boolean("watchfs")

  //   --workspace_status_command (a path; default: "")
  @Option(
    name = "workspace_status_command",
    defaultValue = """""""",
    help = """      
      A command invoked at the beginning of the build to provide status information 
      about the workspace in the form of key/value pairs.  See the User's Manual for 
      the full specification. Also see tools/buildstamp/get_workspace_status for an 
      example.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val workspaceStatusCommand = Flag.Path("workspaceStatusCommand")

  //   --[no]run (a boolean; default: "true")
  @Option(
    name = "run",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """If false, skip running the command line constructed for the built target.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val run = Flag.Boolean("run")

  //   --script_path (a path; default: see description)
  @Option(
    name = "script_path",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION],
    help = """      
      If set, write a shell script to the given file which invokes the target. If 
      this option is set, the target is not run from bazel. Use 'bazel run 
      --script_path=foo //foo && ./foo' to invoke target '//foo' This differs from 
      'bazel run //foo' in that the bazel lock is released and the executable is 
      connected to the terminal's stdin.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val scriptPath = Flag.Path("scriptPath")

  //   --[no]configure (a boolean; default: "False")
  @Option(
    name = "configure",
    defaultValue = """"False"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """Only sync repositories marked as 'configure' for system-configuration purpose.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val configure = Flag.Boolean("configure")

  //   --only (a string; may be used multiple times)
  @Option(
    name = "only",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      If this option is given, only sync the repositories specified with this option. 
      Still consider all (or all configure-like, of --configure is given) outdated.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val only = Flag.Str("only")

  //   --[no]async (a boolean; default: "false")
  @Option(
    name = "async",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      If true, output cleaning is asynchronous. When this command completes, it will 
      be safe to execute new commands in the same client, even though the deletion 
      may continue in the background.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val async = Flag.Boolean("async")

  //   --[no]expunge (a boolean; default: "false")
  @Option(
    name = "expunge",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      If true, clean removes the entire working tree for this bazel instance, which 
      includes all bazel-created temporary and build output files, and stops the 
      bazel server if it is running.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val expunge = Flag.Boolean("expunge")

  //   --expunge_async
  @Option(
    name = "expunge_async",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    expandsTo = [ "--expunge", "--async"],
    help = """      
      If specified, clean asynchronously removes the entire working tree for this 
      bazel instance, which includes all bazel-created temporary and build output 
      files, and stops the bazel server if it is running. When this command 
      completes, it will be safe to execute new commands in the same client, even 
      though the deletion may continue in the background.
      """,
  )
  @JvmField
  @Suppress("unused")
  val expungeAsync = Flag.Unknown("expungeAsync")

  //   --[no]all (a boolean; default: "false")
  @Option(
    name = "all",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      Fetches all external repositories necessary for building any target or 
      repository. Only works when --enable_bzlmod is on.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val all = Flag.Boolean("all")

  //   --[no]force (a boolean; default: "false")
  @Option(
    name = "force",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      Ignore existing repository if any and force fetch the repository again. Only 
      works when --enable_bzlmod is on.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val force = Flag.Boolean("force")

  //   --repo (a string; may be used multiple times)
  @Option(
    name = "repo",
    allowMultiple = true,
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      Only fetches the specified repository, which can be either 
      {@apparent_repo_name} or {@@canonical_repo_name}. Only works when 
      --enable_bzlmod is on.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val repo = Flag.Str("repo")

  //   --[no]print_relative_test_log_paths (a boolean; default: "false")
  @Option(
    name = "print_relative_test_log_paths",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If true, when printing the path to a test log, use relative path that makes use 
      of the 'testlogs' convenience symlink. N.B. - A subsequent 'build'/'test'/etc 
      invocation with a different configuration can cause the target of this symlink 
      to change, making the path printed previously no longer useful.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val printRelativeTestLogPaths = Flag.Boolean("printRelativeTestLogPaths")

  //   --[no]test_verbose_timeout_warnings (a boolean; default: "false")
  @Option(
    name = "test_verbose_timeout_warnings",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If true, print additional warnings when the actual test execution time does not 
      match the timeout defined by the test (whether implied or explicit).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val testVerboseTimeoutWarnings = Flag.Boolean("testVerboseTimeoutWarnings")

  //   --[no]verbose_test_summary (a boolean; default: "true")
  @Option(
    name = "verbose_test_summary",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS],
    help = """      
      If true, print additional information (timing, number of failed runs, etc) in 
      the test summary.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val verboseTestSummary = Flag.Boolean("verboseTestSummary")

  //   --aspect_deps (off, conservative or precise; default: "conservative")
  @Option(
    name = "aspect_deps",
    defaultValue = """"conservative"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """      
      How to resolve aspect dependencies when the output format is one of 
      {xml,proto,record}. 'off' means no aspect dependencies are resolved, 
      'conservative' (the default) means all declared aspect dependencies are added 
      regardless of whether they are given the rule class of direct dependencies, 
      'precise' means that only those aspects are added that are possibly active 
      given the rule class of the direct dependencies. Note that precise mode 
      requires loading other packages to evaluate a single target thus making it 
      slower than the other modes. Also note that even precise mode is not completely 
      precise: the decision whether to compute an aspect is decided in the analysis 
      phase, which is not run during 'bazel query'.
      """,
    valueHelp = """off, conservative or precise""",
  )
  @JvmField
  @Suppress("unused")
  val aspectDeps = Flag.OneOf("aspectDeps")

  //   --[no]consistent_labels (a boolean; default: "false")
  @Option(
    name = "consistent_labels",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      If enabled, every query command emits labels as if by the Starlark 
      <code>str</code> function applied to a <code>Label</code> instance. This is 
      useful for tools that need to match the output of different query commands 
      and/or labels emitted by rules. If not enabled, output formatters are free to 
      emit apparent repository names (relative to the main repository) instead to 
      make the output more readable.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val consistentLabels = Flag.Boolean("consistentLabels")

  //   --[no]experimental_explicit_aspects (a boolean; default: "false")
  @Option(
    name = "experimental_explicit_aspects",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      aquery, cquery: whether to include aspect-generated actions in the output. 
      query: no-op (aspects are always followed).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalExplicitAspects = Flag.Boolean("experimentalExplicitAspects")

  //   --[no]graph:factored (a boolean; default: "true")
  @Option(
    name = "graph:factored",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      If true, then the graph will be emitted 'factored', i.e. 
      topologically-equivalent nodes will be merged together and their labels 
      concatenated. This option is only applicable to --output=graph.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val graph_factored = Flag.Boolean("graph_factored")

  //   --graph:node_limit (an integer; default: "512")
  @Option(
    name = "graph:node_limit",
    defaultValue = """"512"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      The maximum length of the label string for a graph node in the output.  Longer 
      labels will be truncated; -1 means no truncation.  This option is only 
      applicable to --output=graph.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val graph_nodeLimit = Flag.Integer("graph_nodeLimit")

  //   --[no]implicit_deps (a boolean; default: "true")
  @Option(
    name = "implicit_deps",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """      
      If enabled, implicit dependencies will be included in the dependency graph over 
      which the query operates. An implicit dependency is one that is not explicitly 
      specified in the BUILD file but added by bazel. For cquery, this option 
      controls filtering resolved toolchains.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val implicitDeps = Flag.Boolean("implicitDeps")

  //   --[no]include_artifacts (a boolean; default: "true")
  @Option(
    name = "include_artifacts",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Includes names of the action inputs and outputs in the output (potentially 
      large).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val includeArtifacts = Flag.Boolean("includeArtifacts")

  //   --[no]include_aspects (a boolean; default: "true")
  @Option(
    name = "include_aspects",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      aquery, cquery: whether to include aspect-generated actions in the output. 
      query: no-op (aspects are always followed).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val includeAspects = Flag.Boolean("includeAspects")

  //   --[no]include_commandline (a boolean; default: "true")
  @Option(
    name = "include_commandline",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Includes the content of the action command lines in the output (potentially 
      large).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val includeCommandline = Flag.Boolean("includeCommandline")

  //   --[no]include_file_write_contents (a boolean; default: "false")
  @Option(
    name = "include_file_write_contents",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Include the file contents for the FileWrite, SourceSymlinkManifest, and 
      RepoMappingManifest actions (potentially large). 
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val includeFileWriteContents = Flag.Boolean("includeFileWriteContents")

  //   --[no]include_param_files (a boolean; default: "false")
  @Option(
    name = "include_param_files",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Include the content of the param files used in the command (potentially large). 
      Note: Enabling this flag will automatically enable the --include_commandline 
      flag.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val includeParamFiles = Flag.Boolean("includeParamFiles")

  //   --[no]incompatible_package_group_includes_double_slash (a boolean; default: "true")
  @Option(
    name = "incompatible_package_group_includes_double_slash",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    metadataTags = [OptionMetadataTag.INCOMPATIBLE_CHANGE],
    help = """      
      If enabled, when outputting package_group's `packages` attribute, the leading 
      `//` will not be omitted.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val incompatiblePackageGroupIncludesDoubleSlash = Flag.Boolean("incompatiblePackageGroupIncludesDoubleSlash")

  //   --[no]infer_universe_scope (a boolean; default: "false")
  @Option(
    name = "infer_universe_scope",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      If set and --universe_scope is unset, then a value of --universe_scope will be 
      inferred as the list of unique target patterns in the query expression. Note 
      that the --universe_scope value inferred for a query expression that uses 
      universe-scoped functions (e.g.`allrdeps`) may not be what you want, so you 
      should use this option only if you know what you are doing. See 
      https://bazel.build/reference/query#sky-query for details and examples. If 
      --universe_scope is set, then this option's value is ignored. Note: this option 
      applies only to `query` (i.e. not `cquery`).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val inferUniverseScope = Flag.Boolean("inferUniverseScope")

  //   --[no]line_terminator_null (a boolean; default: "false")
  @Option(
    name = "line_terminator_null",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """Whether each format is terminated with \0 instead of newline.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val lineTerminatorNull = Flag.Boolean("lineTerminatorNull")

  //   --[no]nodep_deps (a boolean; default: "true")
  @Option(
    name = "nodep_deps",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """      
      If enabled, deps from "nodep" attributes will be included in the dependency 
      graph over which the query operates. A common example of a "nodep" attribute is 
      "visibility". Run and parse the output of `info build-language` to learn about 
      all the "nodep" attributes in the build language.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val nodepDeps = Flag.Boolean("nodepDeps")

  //   --output (a string; default: "text")
  @Option(
    name = "output",
    defaultValue = """"text"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      The format in which the aquery results should be printed. Allowed values for 
      aquery are: text, textproto, proto, streamed_proto, jsonproto.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val output = Flag.Str("output")

  //   --[no]proto:default_values (a boolean; default: "true")
  @Option(
    name = "proto:default_values",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      If true, attributes whose value is not explicitly specified in the BUILD file 
      are included; otherwise they are omitted. This option is applicable to 
      --output=proto
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val proto_defaultValues = Flag.Boolean("proto_defaultValues")

  //   --[no]proto:definition_stack (a boolean; default: "false")
  @Option(
    name = "proto:definition_stack",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Populate the definition_stack proto field, which records for each rule instance 
      the Starlark call stack at the moment the rule's class was defined.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val proto_definitionStack = Flag.Boolean("proto_definitionStack")

  //   --[no]proto:flatten_selects (a boolean; default: "true")
  @Option(
    name = "proto:flatten_selects",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """      
      If enabled, configurable attributes created by select() are flattened. For list 
      types the flattened representation is a list containing each value of the 
      select map exactly once. Scalar types are flattened to null.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val proto_flattenSelects = Flag.Boolean("proto_flattenSelects")

  //   --[no]proto:include_attribute_source_aspects (a boolean; default: "false")
  @Option(
    name = "proto:include_attribute_source_aspects",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Populate the source_aspect_name proto field of each Attribute with the source 
      aspect that the attribute came from (empty string if it did not).
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val proto_includeAttributeSourceAspects = Flag.Boolean("proto_includeAttributeSourceAspects")

  //   --[no]proto:include_synthetic_attribute_hash (a boolean; default: "false")
  @Option(
    name = "proto:include_synthetic_attribute_hash",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """Whether or not to calculate and populate the ${'$'}internal_attr_hash attribute.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val proto_includeSyntheticAttributeHash = Flag.Boolean("proto_includeSyntheticAttributeHash")

  //   --[no]proto:instantiation_stack (a boolean; default: "false")
  @Option(
    name = "proto:instantiation_stack",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Populate the instantiation call stack of each rule. Note that this requires the 
      stack to be present
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val proto_instantiationStack = Flag.Boolean("proto_instantiationStack")

  //   --[no]proto:locations (a boolean; default: "true")
  @Option(
    name = "proto:locations",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """Whether to output location information in proto output at all.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val proto_locations = Flag.Boolean("proto_locations")

  //   --proto:output_rule_attrs (comma-separated list of options; default: "all")
  @Option(
    name = "proto:output_rule_attrs",
    defaultValue = """"all"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Comma separated list of attributes to include in output. Defaults to all 
      attributes. Set to empty string to not output any attribute. This option is 
      applicable to --output=proto.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val proto_outputRuleAttrs = Flag.Unknown("proto_outputRuleAttrs")

  //   --[no]proto:rule_inputs_and_outputs (a boolean; default: "true")
  @Option(
    name = "proto:rule_inputs_and_outputs",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """Whether or not to populate the rule_input and rule_output fields.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val proto_ruleInputsAndOutputs = Flag.Boolean("proto_ruleInputsAndOutputs")

  //   --query_file (a string; default: "")
  @Option(
    name = "query_file",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      If set, query will read the query from the file named here, rather than on the 
      command line. It is an error to specify a file here as well as a command-line 
      query.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val queryFile = Flag.Str("queryFile")

  //   --[no]relative_locations (a boolean; default: "false")
  @Option(
    name = "relative_locations",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      If true, the location of BUILD files in xml and proto outputs will be relative. 
      By default, the location output is an absolute path and will not be consistent 
      across machines. You can set this option to true to have a consistent result 
      across machines.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val relativeLocations = Flag.Boolean("relativeLocations")

  //   --[no]skyframe_state (a boolean; default: "false")
  @Option(
    name = "skyframe_state",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.TERMINAL_OUTPUT],
    help = """      
      Without performing extra analysis, dump the current Action Graph from Skyframe. 
      Note: Specifying a target with --skyframe_state is currently not supported. 
      This flag is only available with --output=proto or --output=textproto.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val skyframeState = Flag.Boolean("skyframeState")

  //   --[no]tool_deps (a boolean; default: "true")
  @Option(
    name = "tool_deps",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.BUILD_FILE_SEMANTICS],
    help = """      
      Query: If disabled, dependencies on 'exec configuration' will not be included 
      in the dependency graph over which the query operates. An 'exec configuration' 
      dependency edge, such as the one from any 'proto_library' rule to the Protocol 
      Compiler, usually points to a tool executed during the build rather than a part 
      of the same 'target' program.Cquery: If disabled, filters out all configured 
      targets which cross an execution transition from the top-level target that 
      discovered this configured target. That means if the top-level target is in the 
      target configuration, only configured targets also in the target configuration 
      will be returned. If the top-level target is in the exec configuration, only 
      exec configured targets will be returned. This option will NOT exclude resolved 
      toolchains.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val toolDeps = Flag.Boolean("toolDeps")

  //   --universe_scope (comma-separated list of options; default: "")
  @Option(
    name = "universe_scope",
    defaultValue = """""""",
    effectTags = [OptionEffectTag.LOADING_AND_ANALYSIS],
    help = """      
      A comma-separated set of target patterns (additive and subtractive). The query 
      may be performed in the universe defined by the transitive closure of the 
      specified targets. This option is used for the query and cquery commands.For 
      cquery, the input to this option is the targets all answers are built under and 
      so this option may affect configurations and transitions. If this option is not 
      specified, the top-level targets are assumed to be the targets parsed from the 
      query expression. Note: For cquery, not specifying this option may cause the 
      build to break if targets parsed from the query expression are not buildable 
      with top-level options.
      """,
    valueHelp = """comma-separated list of options""",
  )
  @JvmField
  @Suppress("unused")
  val universeScope = Flag.Unknown("universeScope")

  //   --[no]autodetect_server_javabase (a boolean; default: "true")
  @Option(
    name = "autodetect_server_javabase",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      When --noautodetect_server_javabase is passed, Bazel does not fall back to the 
      local JDK for running the bazel server and instead exits.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val autodetectServerJavabase = Flag.Boolean("autodetectServerJavabase")

  //   --[no]batch (a boolean; default: "false")
  @Option(
    name = "batch",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    metadataTags = [OptionMetadataTag.DEPRECATED],
    help = """      
      If set, Bazel will be run as just a client process without a server, instead of 
      in the standard client/server mode. This is deprecated and will be removed, 
      please prefer shutting down the server explicitly if you wish to avoid 
      lingering servers.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val batch = Flag.Boolean("batch")

  //   --[no]batch_cpu_scheduling (a boolean; default: "false")
  @Option(
    name = "batch_cpu_scheduling",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Only on Linux; use 'batch' CPU scheduling for Blaze. This policy is useful for 
      workloads that are non-interactive, but do not want to lower their nice value. 
      See 'man 2 sched_setscheduler'. If false, then Bazel does not perform a system 
      call.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val batchCpuScheduling = Flag.Boolean("batchCpuScheduling")

  //   --bazelrc (a string; default: see description)
  @Option(
    name = "bazelrc",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      The location of the user .bazelrc file containing default values of Bazel 
      options. /dev/null indicates that all further `--bazelrc`s will be ignored, 
      which is useful to disable the search for a user rc file, e.g. in release 
      builds.This option can also be specified multiple times.E.g. with 
      `--bazelrc=x.rc --bazelrc=y.rc --bazelrc=/dev/null --bazelrc=z.rc`,  1) x.rc 
      and y.rc are read.  2) z.rc is ignored due to the prior /dev/null.If 
      unspecified, Bazel uses the first .bazelrc file it finds in the following two 
      locations: the workspace directory, then the user's home directory.Note: 
      command line options will always supersede any option in bazelrc.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val bazelrc = Flag.Str("bazelrc")

  //   --[no]block_for_lock (a boolean; default: "true")
  @Option(
    name = "block_for_lock",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """      
      When --noblock_for_lock is passed, Bazel does not wait for a running command to 
      complete, but instead exits immediately.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val blockForLock = Flag.Boolean("blockForLock")

  //   --[no]client_debug (a boolean; default: "false")
  @Option(
    name = "client_debug",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING],
    help = """      
      If true, log debug information from the client to stderr. Changing this option 
      will not cause the server to restart.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val clientDebug = Flag.Boolean("clientDebug")

  //   --connect_timeout_secs (an integer; default: "30")
  @Option(
    name = "connect_timeout_secs",
    defaultValue = """"30"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """The amount of time the client waits for each attempt to connect to the server""",
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val connectTimeoutSecs = Flag.Integer("connectTimeoutSecs")

  //   --digest_function (hash function; default: see description)
  @Option(
    name = "digest_function",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """The hash function to use when computing file digests.""",
    valueHelp = """hash function""",
  )
  @JvmField
  @Suppress("unused")
  val digestFunction = Flag.Unknown("digestFunction")

  //   --[no]expand_configs_in_place (a boolean; default: "true")
  @Option(
    name = "expand_configs_in_place",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.NO_OP],
    metadataTags = [OptionMetadataTag.DEPRECATED],
    help = """      
      Changed the expansion of --config flags to be done in-place, as opposed to in a 
      fixed point expansion between normal rc options and command-line specified 
      options.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val expandConfigsInPlace = Flag.Boolean("expandConfigsInPlace")

  //   --failure_detail_out (a path; default: see description)
  @Option(
    name = "failure_detail_out",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      If set, specifies a location to write a failure_detail protobuf message if the 
      server experiences a failure and cannot report it via gRPC, as normal. 
      Otherwise, the location will be ${'$'}{OUTPUT_BASE}/failure_detail.rawproto.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val failureDetailOut = Flag.Path("failureDetailOut")

  //   --[no]home_rc (a boolean; default: "true")
  @Option(
    name = "home_rc",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """Whether or not to look for the home bazelrc file at ${'$'}HOME/.bazelrc""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val homeRc = Flag.Boolean("homeRc")

  //   --[no]idle_server_tasks (a boolean; default: "true")
  @Option(
    name = "idle_server_tasks",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """Run System.gc() when the server is idle""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val idleServerTasks = Flag.Boolean("idleServerTasks")

  //   --[no]ignore_all_rc_files (a boolean; default: "false")
  @Option(
    name = "ignore_all_rc_files",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      Disables all rc files, regardless of the values of other rc-modifying flags, 
      even if these flags come later in the list of startup options.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val ignoreAllRcFiles = Flag.Boolean("ignoreAllRcFiles")

  //   --io_nice_level (an integer; default: "-1")
  @Option(
    name = "io_nice_level",
    defaultValue = """"-1"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Only on Linux; set a level from 0-7 for best-effort IO scheduling using the 
      sys_ioprio_set system call. 0 is highest priority, 7 is lowest. The 
      anticipatory scheduler may only honor up to priority 4. If set to a negative 
      value, then Bazel does not perform a system call.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val ioNiceLevel = Flag.Integer("ioNiceLevel")

  //   --local_startup_timeout_secs (an integer; default: "120")
  @Option(
    name = "local_startup_timeout_secs",
    defaultValue = """"120"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """The maximum amount of time the client waits to connect to the server""",
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val localStartupTimeoutSecs = Flag.Integer("localStartupTimeoutSecs")

  //   --macos_qos_class (a string; default: "default")
  @Option(
    name = "macos_qos_class",
    defaultValue = """"default"""",
    effectTags = [OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS],
    help = """      
      Sets the QoS service class of the bazel server when running on macOS. This flag 
      has no effect on all other platforms but is supported to ensure rc files can be 
      shared among them without changes. Possible values are: user-interactive, 
      user-initiated, default, utility, and background.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val macosQosClass = Flag.Str("macosQosClass")

  //   --max_idle_secs (an integer; default: "10800")
  @Option(
    name = "max_idle_secs",
    defaultValue = """"10800"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      The number of seconds the build server will wait idling before shutting down. 
      Zero means that the server will never shutdown. This is only read on 
      server-startup, changing this option will not cause the server to restart.
      """,
    valueHelp = """an integer""",
  )
  @JvmField
  @Suppress("unused")
  val maxIdleSecs = Flag.Integer("maxIdleSecs")

  //   --output_base (a path; default: see description)
  @Option(
    name = "output_base",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      If set, specifies the output location to which all build output will be 
      written. Otherwise, the location will be 
      ${'$'}{OUTPUT_ROOT}/_blaze_${'$'}{USER}/${'$'}{MD5_OF_WORKSPACE_ROOT}. Note: If 
      you specify a different option from one to the next Bazel invocation for this 
      value, you'll likely start up a new, additional Bazel server. Bazel starts 
      exactly one server per specified output base. Typically there is one output 
      base per workspace - however, with this option you may have multiple output 
      bases per workspace and thereby run multiple builds for the same client on the 
      same machine concurrently. See 'bazel help shutdown' on how to shutdown a Bazel 
      server.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val outputBase = Flag.Path("outputBase")

  //   --output_user_root (a path; default: see description)
  @Option(
    name = "output_user_root",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      The user-specific directory beneath which all build outputs are written; by 
      default, this is a function of ${'$'}USER, but by specifying a constant, build 
      outputs can be shared between collaborating users.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val outputUserRoot = Flag.Path("outputUserRoot")

  //   --[no]preemptible (a boolean; default: "false")
  @Option(
    name = "preemptible",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT],
    help = """If true, the command can be preempted if another command is started.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val preemptible = Flag.Boolean("preemptible")

  //   --server_jvm_out (a path; default: see description)
  @Option(
    name = "server_jvm_out",
    effectTags = [OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      The location to write the server's JVM's output. If unset then defaults to a 
      location in output_base.
      """,
    valueHelp = """a path""",
  )
  @JvmField
  @Suppress("unused")
  val serverJvmOut = Flag.Path("serverJvmOut")

  //   --[no]shutdown_on_low_sys_mem (a boolean; default: "false")
  @Option(
    name = "shutdown_on_low_sys_mem",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOSES_INCREMENTAL_STATE],
    help = """      
      If max_idle_secs is set and the build server has been idle for a while, shut 
      down the server when the system is low on free RAM. Linux only.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val shutdownOnLowSysMem = Flag.Boolean("shutdownOnLowSysMem")

  //   --[no]system_rc (a boolean; default: "true")
  @Option(
    name = "system_rc",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """Whether or not to look for the system-wide bazelrc.""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val systemRc = Flag.Boolean("systemRc")

  //   --[no]unlimit_coredumps (a boolean; default: "false")
  @Option(
    name = "unlimit_coredumps",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      Raises the soft coredump limit to the hard limit to make coredumps of the 
      server (including the JVM) and the client possible under common conditions. 
      Stick this flag in your bazelrc once and forget about it so that you get 
      coredumps when you actually encounter a condition that triggers them.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val unlimitCoredumps = Flag.Boolean("unlimitCoredumps")

  //   --[no]windows_enable_symlinks (a boolean; default: "false")
  @Option(
    name = "windows_enable_symlinks",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION],
    help = """      
      If true, real symbolic links will be created on Windows instead of file 
      copying. Requires Windows developer mode to be enabled and Windows 10 version 
      1703 or greater.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val windowsEnableSymlinks = Flag.Boolean("windowsEnableSymlinks")

  //   --[no]workspace_rc (a boolean; default: "true")
  @Option(
    name = "workspace_rc",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.CHANGES_INPUTS],
    help = """      
      Whether or not to look for the workspace bazelrc file at 
      ${'$'}workspace/.bazelrc
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val workspaceRc = Flag.Boolean("workspaceRc")

  //   --host_jvm_args (a string; may be used multiple times)
  @Option(
    name = "host_jvm_args",
    allowMultiple = true,
    help = """Flags to pass to the JVM executing Blaze.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostJvmArgs = Flag.Str("hostJvmArgs")

  //   --host_jvm_debug
  @Option(
    name = "host_jvm_debug",
    expandsTo = [
      "--host_jvm_args=-Xdebug",
      "--host_jvm_args=-Xrunjdwp:transport=dt_socket,server=y,address=5005",
    ],
    help = """      
      Convenience option to add some additional JVM startup flags, which cause the 
      JVM to wait during startup until you connect from a JDWP-compliant debugger 
      (like Eclipse) to port 5005.
      """,
  )
  @JvmField
  @Suppress("unused")
  val hostJvmDebug = Flag.Unknown("hostJvmDebug")

  //   --host_jvm_profile (a string; default: "")
  @Option(
    name = "host_jvm_profile",
    defaultValue = """""""",
    help = """      
      Convenience option to add some profiler/debugger-specific JVM startup flags. 
      Bazel has a list of known values that it maps to hard-coded JVM startup flags, 
      possibly searching some hardcoded paths for certain files.
      """,
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val hostJvmProfile = Flag.Str("hostJvmProfile")

  //   --server_javabase (a string; default: "")
  @Option(
    name = "server_javabase",
    defaultValue = """""""",
    help = """Path to the JVM used to execute Bazel itself.""",
    valueHelp = """a string""",
  )
  @JvmField
  @Suppress("unused")
  val serverJavabase = Flag.Str("serverJavabase")

  // an internal undocumented flag
  @Option(
    name = "experimental_check_output_files",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.UNKNOWN],
    help = """      
      Check for modifications made to the output files of a build. Consider setting 
      this flag to false if you don't expect these files to change outside of bazel 
      since it will speed up subsequent runs as they won't have to check a previous 
      run's cache      
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCheckOutputFiles = Flag.Boolean("experimentalCheckOutputFiles")

  // an internal undocumented flag
  @Option(
    name = "action_cache_store_output_metadata",
    oldName = "experimental_action_cache_store_output_metadata",
    defaultValue = """"false"""",
    effectTags = [OptionEffectTag.NO_OP],
    help = """no-op""",
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val actionCacheStoreOutputMetadata = Flag.Boolean("actionCacheStoreOutputMetadata")

  // an internal undocumented flag
  @Option(
    name = "experimental_check_external_repository_files",
    defaultValue = """"true"""",
    effectTags = [OptionEffectTag.UNKNOWN],
    help = """      
      Check for modifications to files in external repositories. Consider setting 
      this flag to false if you don't expect these files to change outside of bazel 
      since it will speed up subsequent runs as they won't have to check a previous 
      run's cache.
      """,
    valueHelp = """a boolean""",
  )
  @JvmField
  @Suppress("unused")
  val experimentalCheckExternalRepositoryFiles = Flag.Boolean("experimentalCheckExternalRepositoryFiles")
}
