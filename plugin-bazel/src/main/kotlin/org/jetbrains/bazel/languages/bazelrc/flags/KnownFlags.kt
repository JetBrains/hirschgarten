package org.jetbrains.bazel.languages.bazelrc.flags

// temporary generated from bazel help build.
// will be followed up with a more comprehensive variant including all the flag and extra data (tags / availability for what commands / etc)
internal val flags =
  object {
// unknown line:                                                            [bazel release 7.3.1]
// unknown line: Usage: bazel build <options> <targets>
// unknown line:
// unknown line: Builds the specified targets, using the options.
// unknown line:
// unknown line: See 'bazel help target-syntax' for details and examples on how to
// unknown line: specify targets to build.
// unknown line:
// unknown line: Options that appear before the command and are parsed by the client:
// a path
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val distdir =
      BazelFlag<String>(
        name = "distdir",
        description =
          """
          Additional places to search for archives before accessing the network to 
          download them.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRepositoryCacheHardlinks =
      BazelFlag.boolean(
        name = "experimental_repository_cache_hardlinks",
        description =
          """
          If set, the repository cache will hardlink the file in case of a cache hit, 
          rather than copying. This is intended to save disk space.
          """.trimIndent(),
      )

// an integer
// default: "0"
    @JvmField
    @Suppress("unused")
    val experimentalRepositoryDownloaderRetries =
      BazelFlag<String>(
        name = "experimental_repository_downloader_retries",
        description =
          """
          The maximum number of attempts to retry a download error. If set to 0, 
          retries are disabled.
          """.trimIndent(),
      )

// a double
// default: "1.0"
    @JvmField
    @Suppress("unused")
    val experimentalScaleTimeouts =
      BazelFlag<String>(
        name = "experimental_scale_timeouts",
        description =
          """
          Scale all timeouts in Starlark repository rules by this factor. In this 
          way, external repositories can be made working on machines that are slower 
          than the rule author expected, without changing the source code
          """.trimIndent(),
      )

// an integer
// default: "8"
    @JvmField
    @Suppress("unused")
    val httpConnectorAttempts =
      BazelFlag<String>(
        name = "http_connector_attempts",
        description =
          """
          The maximum number of attempts for http downloads.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "0s"
    @JvmField
    @Suppress("unused")
    val httpConnectorRetryMaxTimeout =
      BazelFlag<String>(
        name = "http_connector_retry_max_timeout",
        description =
          """
          The maximum timeout for http download retries. With a value of 0, no 
          timeout maximum is defined.
          """.trimIndent(),
      )

// a double
// default: "1.0"
    @JvmField
    @Suppress("unused")
    val httpTimeoutScaling =
      BazelFlag<String>(
        name = "http_timeout_scaling",
        description =
          """
          Scale all timeouts related to http downloads by the given factor
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDisableNativeRepoRules =
      BazelFlag.boolean(
        name = "incompatible_disable_native_repo_rules",
        description =
          """
          If false, native repo rules can be used in WORKSPACE; otherwise, Starlark 
          repo rules must be used instead. Native repo rules include 
          local_repository, new_local_repository, local_config_platform, 
          android_sdk_repository, and android_ndk_repository.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val repositoryCache =
      BazelFlag<String>(
        name = "repository_cache",
        description =
          """
          Specifies the cache location of the downloaded values obtained during the 
          fetching of external repositories. An empty string as argument requests the 
          cache to be disabled, otherwise the default of 
          '<output_user_root>/cache/repos/v1' is used
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val repositoryDisableDownload =
      BazelFlag.boolean(
        name = "repository_disable_download",
        description =
          """
          If set, downloading using ctx.download{,_and_extract} is not allowed during 
          repository fetching. Note that network access is not completely disabled; 
          ctx.execute could still run an arbitrary executable that accesses the 
          Internet.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options that control build execution:
// default: "false"
    @JvmField
    @Suppress("unused")
    val checkUpToDate =
      BazelFlag.boolean(
        name = "check_up_to_date",
        description =
          """
          Don't perform the build, just check if it is up-to-date.  If all targets 
          are up-to-date, the build completes successfully.  If any step needs to be 
          executed an error is reported and the build fails.
          """.trimIndent(),
      )

// an integer
// default: "1000"
    @JvmField
    @Suppress("unused")
    val dynamicLocalExecutionDelay =
      BazelFlag<String>(
        name = "dynamic_local_execution_delay",
        description =
          """
          How many milliseconds should local execution be delayed, if remote 
          execution was faster during a build at least once?
          """.trimIndent(),
      )

// a '[name=]value1[,..,valueN]' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val dynamicLocalStrategy =
      BazelFlag<String>(
        name = "dynamic_local_strategy",
        description =
          """
          The local strategies, in order, to use for the given mnemonic - the first 
          applicable strategy is used. For example, `worker,sandboxed` runs actions 
          that support persistent workers using the worker strategy, and all others 
          using the sandboxed strategy. If no mnemonic is given, the list of 
          strategies is used as the fallback for all mnemonics. The default fallback 
          list is `worker,sandboxed`, or`worker,sandboxed,standalone` if 
          `experimental_local_lockfree_output` is set. Takes [mnemonic=]local_strategy
          [,local_strategy,...]
          """.trimIndent(),
      )

// a '[name=]value1[,..,valueN]' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val dynamicRemoteStrategy =
      BazelFlag<String>(
        name = "dynamic_remote_strategy",
        description =
          """
          The remote strategies, in order, to use for the given mnemonic - the first 
          applicable strategy is used. If no mnemonic is given, the list of 
          strategies is used as the fallback for all mnemonics. The default fallback 
          list is `remote`, so this flag usually does not need to be set explicitly. 
          Takes [mnemonic=]remote_strategy[,remote_strategy,...]
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val experimentalDockerImage =
      BazelFlag<String>(
        name = "experimental_docker_image",
        description =
          """
          Specify a Docker image name (e.g. "ubuntu:latest") that should be used to 
          execute a sandboxed action when using the docker strategy and the action 
          itself doesn't already have a container-image attribute in its 
          remote_execution_properties in the platform description. The value of this 
          flag is passed verbatim to 'docker run', so it supports the same syntax and 
          mechanisms as Docker itself.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalDockerUseCustomizedImages =
      BazelFlag.boolean(
        name = "experimental_docker_use_customized_images",
        description =
          """
          If enabled, injects the uid and gid of the current user into the Docker 
          image before using it. This is required if your build / tests depend on the 
          user having a name and home directory inside the container. This is on by 
          default, but you can disable it in case the automatic image customization 
          feature doesn't work in your case or you know that you don't need it.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalDynamicExcludeTools =
      BazelFlag.boolean(
        name = "experimental_dynamic_exclude_tools",
        description =
          """
          When set, targets that are build "for tool" are not subject to dynamic 
          execution. Such targets are extremely unlikely to be built incrementally 
          and thus not worth spending local cycles on.
          """.trimIndent(),
      )

// a double
// default: "0"
    @JvmField
    @Suppress("unused")
    val experimentalDynamicLocalLoadFactor =
      BazelFlag<String>(
        name = "experimental_dynamic_local_load_factor",
        description =
          """
          Controls how much load from dynamic execution to put on the local machine. 
          This flag adjusts how many actions in dynamic execution we will schedule 
          concurrently. It is based on the number of CPUs Blaze thinks is available, 
          which can be controlled with the --local_cpu_resources flag.
          If this flag is 0, all actions are scheduled locally immediately. If > 0, 
          the amount of actions scheduled locally is limited by the number of CPUs 
          available. If < 1, the load factor is used to reduce the number of locally 
          scheduled actions when the number of actions waiting to schedule is high. 
          This lessens the load on the local machine in the clean build case, where 
          the local machine does not contribute much.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "0"
    @JvmField
    @Suppress("unused")
    val experimentalDynamicSlowRemoteTime =
      BazelFlag<String>(
        name = "experimental_dynamic_slow_remote_time",
        description =
          """
          If >0, the time a dynamically run action must run remote-only before we 
          prioritize its local execution to avoid remote timeouts. This may hide some 
          problems on the remote execution system. Do not turn this on without 
          monitoring of remote execution issues.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalEnableDockerSandbox =
      BazelFlag.boolean(
        name = "experimental_enable_docker_sandbox",
        description =
          """
          Enable Docker-based sandboxing. This option has no effect if Docker is not 
          installed.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalInmemorySandboxStashes =
      BazelFlag.boolean(
        name = "experimental_inmemory_sandbox_stashes",
        description =
          """
          If set to true, the contents of stashed sandboxes for 
          reuse_sandbox_directories will be tracked in memory. This reduces the 
          amount of I/O needed during reuse. Depending on the build this flag may 
          improve wall time. Depending on the build as well this flag may use a 
          significant amount of additional memory.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalInprocessSymlinkCreation =
      BazelFlag.boolean(
        name = "experimental_inprocess_symlink_creation",
        description =
          """
          Whether to make direct file system calls to create symlink trees
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalPersistentAarExtractor =
      BazelFlag.boolean(
        name = "experimental_persistent_aar_extractor",
        description =
          """
          Enable persistent aar extractor by using workers.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRemotableSourceManifests =
      BazelFlag.boolean(
        name = "experimental_remotable_source_manifests",
        description =
          """
          Whether to make source manifest actions remotable
          """.trimIndent(),
      )

// an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
// default: "4"
    @JvmField
    @Suppress("unused")
    val experimentalSandboxAsyncTreeDeleteIdleThreads =
      BazelFlag<String>(
        name = "experimental_sandbox_async_tree_delete_idle_threads",
        description =
          """
          If 0, delete sandbox trees as soon as an action completes (causing 
          completion of the action to be delayed). If greater than zero, execute the 
          deletion of such threes on an asynchronous thread pool that has size 1 when 
          the build is running and grows to the size specified by this flag when the 
          server is idle.
          """.trimIndent(),
      )

// an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.
// default: "0"
    @JvmField
    @Suppress("unused")
    val experimentalSandboxMemoryLimitMb =
      BazelFlag<String>(
        name = "experimental_sandbox_memory_limit_mb",
        description =
          """
          If > 0, each Linux sandbox will be limited to the given amount of memory 
          (in MB). Requires cgroups v1 or v2 and permissions for the users to the 
          cgroups dir.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalShrinkWorkerPool =
      BazelFlag.boolean(
        name = "experimental_shrink_worker_pool",
        description =
          """
          If enabled, could shrink worker pool if worker memory pressure is high. 
          This flag works only when flag experimental_total_worker_memory_limit_mb is 
          enabled.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalSplitCoveragePostprocessing =
      BazelFlag.boolean(
        name = "experimental_split_coverage_postprocessing",
        description =
          """
          If true, then Bazel will run coverage postprocessing for test in a new 
          spawn.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalSplitXmlGeneration =
      BazelFlag.boolean(
        name = "experimental_split_xml_generation",
        description =
          """
          If this flag is set, and a test action does not generate a test.xml file, 
          then Bazel uses a separate action to generate a dummy test.xml file 
          containing the test log. Otherwise, Bazel generates a test.xml as part of 
          the test action.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalStrictFilesetOutput =
      BazelFlag.boolean(
        name = "experimental_strict_fileset_output",
        description =
          """
          If this option is enabled, filesets will treat all output artifacts as 
          regular files. They will not traverse directories or be sensitive to 
          symlinks.
          """.trimIndent(),
      )

// an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.
// default: "0"
    @JvmField
    @Suppress("unused")
    val experimentalTotalWorkerMemoryLimitMb =
      BazelFlag<String>(
        name = "experimental_total_worker_memory_limit_mb",
        description =
          """
          If this limit is greater than zero idle workers might be killed if the 
          total memory usage of all  workers exceed the limit.
          """.trimIndent(),
      )

// an integer in (-1)-1073741819 range
// default: "1048576"
    @JvmField
    @Suppress("unused")
    val experimentalUiMaxStdouterrBytes =
      BazelFlag<String>(
        name = "experimental_ui_max_stdouterr_bytes",
        description =
          """
          The maximum size of the stdout / stderr files that will be printed to the 
          console. -1 implies no limit.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalUseHermeticLinuxSandbox =
      BazelFlag.boolean(
        name = "experimental_use_hermetic_linux_sandbox",
        description =
          """
          If set to true, do not mount root, only mount whats provided with 
          sandbox_add_mount_pair. Input files will be hardlinked to the sandbox 
          instead of symlinked to from the sandbox. If action input files are located 
          on a filesystem different from the sandbox, then the input files will be 
          copied instead.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalUseSemaphoreForJobs =
      BazelFlag.boolean(
        name = "experimental_use_semaphore_for_jobs",
        description =
          """
          If set to true, additionally use semaphore to limit number of concurrent 
          jobs.
          """.trimIndent(),
      )

// a tri-state (auto, yes, no)
// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalUseWindowsSandbox =
      BazelFlag<String>(
        name = "experimental_use_windows_sandbox",
        description =
          """
          Use Windows sandbox to run actions. If "yes", the binary provided by --
          experimental_windows_sandbox_path must be valid and correspond to a 
          supported version of sandboxfs. If "auto", the binary may be missing or not 
          compatible.
          """.trimIndent(),
      )

// a string
// default: "BazelSandbox.exe"
    @JvmField
    @Suppress("unused")
    val experimentalWindowsSandboxPath =
      BazelFlag<String>(
        name = "experimental_windows_sandbox_path",
        description =
          """
          Path to the Windows sandbox binary to use when --
          experimental_use_windows_sandbox is true. If a bare name, use the first 
          binary of that name found in the PATH.
          """.trimIndent(),
      )

// comma-separated set of options
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalWorkerAllowlist =
      BazelFlag<String>(
        name = "experimental_worker_allowlist",
        description =
          """
          If non-empty, only allow using persistent workers with the given worker key 
          mnemonic.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalWorkerAsResource =
      BazelFlag.boolean(
        name = "experimental_worker_as_resource",
        description =
          """
          No-op, will be removed soon.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalWorkerCancellation =
      BazelFlag.boolean(
        name = "experimental_worker_cancellation",
        description =
          """
          If enabled, Bazel may send cancellation requests to workers that support 
          them.
          """.trimIndent(),
      )

// an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.
// default: "0"
    @JvmField
    @Suppress("unused")
    val experimentalWorkerMemoryLimitMb =
      BazelFlag<String>(
        name = "experimental_worker_memory_limit_mb",
        description =
          """
          If this limit is greater than zero, workers might be killed if the memory 
          usage of the worker exceeds the limit. If not used together with dynamic 
          execution and `--experimental_dynamic_ignore_local_signals=9`, this may 
          crash your build.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "5s"
    @JvmField
    @Suppress("unused")
    val experimentalWorkerMetricsPollInterval =
      BazelFlag<String>(
        name = "experimental_worker_metrics_poll_interval",
        description =
          """
          The interval between collecting worker metrics and possibly attempting 
          evictions. Cannot effectively be less than 1s for performance reasons.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalWorkerMultiplexSandboxing =
      BazelFlag.boolean(
        name = "experimental_worker_multiplex_sandboxing",
        description =
          """
          If enabled, multiplex workers will be sandboxed, using a separate sandbox 
          directory per work request. Only workers that have the 'supports-multiplex-
          sandboxing' execution requirement will be sandboxed.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalWorkerSandboxHardening =
      BazelFlag.boolean(
        name = "experimental_worker_sandbox_hardening",
        description =
          """
          If enabled, workers are run in a hardened sandbox, if the implementation 
          allows it.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalWorkerStrictFlagfiles =
      BazelFlag.boolean(
        name = "experimental_worker_strict_flagfiles",
        description =
          """
          If enabled, actions arguments for workers that do not follow the worker 
          specification will cause an error. Worker arguments must have exactly one 
          @flagfile argument as the last of its list of arguments.
          """.trimIndent(),
      )

// an integer in 0-100 range
// default: "100"
    @JvmField
    @Suppress("unused")
    val gcThrashingThreshold =
      BazelFlag<String>(
        name = "gc_thrashing_threshold",
        description =
          """
          The percent of tenured space occupied (0-100) above which 
          GcThrashingDetector considers memory pressure events against its limits (--
          gc_thrashing_limits). If set to 100, GcThrashingDetector is disabled.
          """.trimIndent(),
      )

// comma-separated list of options
// default: ""
    @JvmField
    @Suppress("unused")
    val genruleStrategy =
      BazelFlag<String>(
        name = "genrule_strategy",
        description =
          """
          Specify how to execute genrules. This flag will be phased out. Instead, use 
          --spawn_strategy=<value> to control all actions or --
          strategy=Genrule=<value> to control genrules only.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val highPriorityWorkers =
      BazelFlag<String>(
        name = "high_priority_workers",
        description =
          """
          No-op, will be removed soon.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleDisallowUnsoundDirectoryOutputs =
      BazelFlag.boolean(
        name = "incompatible_disallow_unsound_directory_outputs",
        description =
          """
          If set, it is an error for an action to materialize an output file as a 
          directory. Does not affect source directories. See https://github.
          com/bazelbuild/bazel/issues/18646.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleModifyExecutionInfoAdditive =
      BazelFlag.boolean(
        name = "incompatible_modify_execution_info_additive",
        description =
          """
          When enabled, passing multiple --modify_execution_info flags is additive. 
          When disabled, only the last flag is taken into account.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleRemoteDanglingSymlinks =
      BazelFlag.boolean(
        name = "incompatible_remote_dangling_symlinks",
        description =
          """
          If set to true, symlinks uploaded to a remote or disk cache are allowed to 
          dangle.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleRemoteSymlinks =
      BazelFlag.boolean(
        name = "incompatible_remote_symlinks",
        description =
          """
          If set to true, Bazel will always upload symlinks as such to a remote or 
          disk cache. Otherwise, non-dangling relative symlinks (and only those) will 
          be uploaded as the file or directory they point to.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleSandboxHermeticTmp =
      BazelFlag.boolean(
        name = "incompatible_sandbox_hermetic_tmp",
        description =
          """
          If set to true, each Linux sandbox will have its own dedicated empty 
          directory mounted as /tmp rather than sharing /tmp with the host 
          filesystem. Use --sandbox_add_mount_pair=/tmp to keep seeing the host's 
          /tmp in all sandboxes.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val internalSpawnScheduler =
      BazelFlag.boolean(
        name = "internal_spawn_scheduler",
        description =
          """
          Placeholder option so that we can tell in Blaze whether the spawn scheduler 
          was enabled.
          """.trimIndent(),
      )

// an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
// default: "auto"
    @JvmField
    @Suppress("unused")
    val jobs =
      BazelFlag<String>(
        name = "jobs",
        description =
          """
        The number of concurrent jobs to run. Takes an integer, or a keyword 
        ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-
*]<float>) eg. "auto", "HOST_CPUS*.5". Values must be between 1 and 5000. 
        Values above 2500 may cause memory issues. "auto" calculates a reasonable 
        default based on host resources.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val keepGoing =
      BazelFlag.boolean(
        name = "keep_going",
        description =
          """
          Continue as much as possible after an error.  While the target that failed 
          and those that depend on it cannot be analyzed, other prerequisites of 
          these targets can be.
          """.trimIndent(),
      )

// an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
// default: "auto"
    @JvmField
    @Suppress("unused")
    val loadingPhaseThreads =
      BazelFlag<String>(
        name = "loading_phase_threads",
        description =
          """
          Number of parallel threads to use for the loading/analysis phase.Takes an 
          integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally 
          followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". "auto" 
          sets a reasonable default based on host resources. Must be at least 1.
          """.trimIndent(),
      )

// regex=[+-]key,regex=[+-]key,...
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val modifyExecutionInfo =
      BazelFlag<String>(
        name = "modify_execution_info",
        description =
          """
          Add or remove keys from an action's execution info based on action 
          mnemonic.  Applies only to actions which support execution info. Many 
          common actions support execution info, e.g. Genrule, CppCompile, Javac, 
          StarlarkAction, TestRunner. When specifying multiple values, order matters 
          because many regexes may apply to the same mnemonic.
          
          Syntax: "regex=[+-]key,regex=[+-]key,...".
          
          Examples:
            '.*=+x,.*=-y,.*=+z' adds 'x' and 'z' to, and removes 'y' from, the 
          execution info for all actions.
            'Genrule=+requires-x' adds 'requires-x' to the execution info for all 
          Genrule actions.
            '(?!Genrule).*=-requires-x' removes 'requires-x' from the execution info 
          for all non-Genrule actions.
          """.trimIndent(),
      )

// regex=[+-]key,regex=[+-]key,...
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val persistentAndroidDexDesugar =
      BazelFlag<String>(
        name = "persistent_android_dex_desugar",
        description =
          """
          Enable persistent Android dex and desugar actions by using workers.
            Expands to: --internal_persistent_android_dex_desugar --
            strategy=Desugar=worker --strategy=DexBuilder=worker
          """.trimIndent(),
      )

// regex=[+-]key,regex=[+-]key,...
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val persistentAndroidResourceProcessor =
      BazelFlag<String>(
        name = "persistent_android_resource_processor",
        description =
          """
          Enable persistent Android resource processor by using workers.
          
          
          
            Expands to: --internal_persistent_busybox_tools --
            strategy=AaptPackage=worker --strategy=AndroidResourceParser=worker --
            strategy=AndroidResourceValidator=worker --
            strategy=AndroidResourceCompiler=worker --strategy=RClassGenerator=worker 
            --strategy=AndroidResourceLink=worker --strategy=AndroidAapt2=worker --
            strategy=AndroidAssetMerger=worker --
            strategy=AndroidResourceMerger=worker --
            strategy=AndroidCompiledResourceMerger=worker --
            strategy=ManifestMerger=worker --strategy=AndroidManifestMerger=worker --
            strategy=Aapt2Optimize=worker --strategy=AARGenerator=worker --
            strategy=ProcessDatabinding=worker --
            strategy=GenerateDataBindingBaseClasses=worker
          """.trimIndent(),
      )

// regex=[+-]key,regex=[+-]key,...
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val persistentMultiplexAndroidDexDesugar =
      BazelFlag<String>(
        name = "persistent_multiplex_android_dex_desugar",
        description =
          """
          Enable persistent multiplexed Android dex and desugar actions by using 
          workers.
            Expands to: --persistent_android_dex_desugar --
            internal_persistent_multiplex_android_dex_desugar
          """.trimIndent(),
      )

// regex=[+-]key,regex=[+-]key,...
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val persistentMultiplexAndroidResourceProcessor =
      BazelFlag<String>(
        name = "persistent_multiplex_android_resource_processor",
        description =
          """
          Enable persistent multiplexed Android resource processor by using workers.
            Expands to: --persistent_android_resource_processor --
            modify_execution_info=AaptPackage=+supports-multiplex-workers --
            modify_execution_info=AndroidResourceParser=+supports-multiplex-workers --
            modify_execution_info=AndroidResourceValidator=+supports-multiplex-
            workers --modify_execution_info=AndroidResourceCompiler=+supports-
            multiplex-workers --modify_execution_info=RClassGenerator=+supports-
            multiplex-workers --modify_execution_info=AndroidResourceLink=+supports-
            multiplex-workers --modify_execution_info=AndroidAapt2=+supports-
            multiplex-workers --modify_execution_info=AndroidAssetMerger=+supports-
            multiplex-workers --modify_execution_info=AndroidResourceMerger=+supports-
            multiplex-workers --
            modify_execution_info=AndroidCompiledResourceMerger=+supports-multiplex-
            workers --modify_execution_info=ManifestMerger=+supports-multiplex-
            workers --modify_execution_info=AndroidManifestMerger=+supports-multiplex-
            workers --modify_execution_info=Aapt2Optimize=+supports-multiplex-workers 
            --modify_execution_info=AARGenerator=+supports-multiplex-workers
          """.trimIndent(),
      )

// regex=[+-]key,regex=[+-]key,...
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val persistentMultiplexAndroidTools =
      BazelFlag<String>(
        name = "persistent_multiplex_android_tools",
        description =
          """
          Enable persistent and multiplexed Android tools (dexing, desugaring, 
          resource processing).
            Expands to: --internal_persistent_multiplex_busybox_tools --
            persistent_multiplex_android_resource_processor --
            persistent_multiplex_android_dex_desugar
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val reuseSandboxDirectories =
      BazelFlag.boolean(
        name = "reuse_sandbox_directories",
        description =
          """
          If set to true, directories used by sandboxed non-worker execution may be 
          reused to avoid unnecessary setup costs.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val sandboxBase =
      BazelFlag<String>(
        name = "sandbox_base",
        description =
          """
          Lets the sandbox create its sandbox directories underneath this path. 
          Specify a path on tmpfs (like /run/shm) to possibly improve performance a 
          lot when your build / tests have many input files. Note: You need enough 
          RAM and free space on the tmpfs to hold output and intermediate files 
          generated by running actions.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val sandboxExplicitPseudoterminal =
      BazelFlag.boolean(
        name = "sandbox_explicit_pseudoterminal",
        description =
          """
          Explicitly enable the creation of pseudoterminals for sandboxed actions. 
          Some linux distributions require setting the group id of the process to 
          'tty' inside the sandbox in order for pseudoterminals to function. If this 
          is causing issues, this flag can be disabled to enable other groups to be 
          used.
          """.trimIndent(),
      )

// an absolute path
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val sandboxTmpfsPath =
      BazelFlag<String>(
        name = "sandbox_tmpfs_path",
        description =
          """
          For sandboxed actions, mount an empty, writable directory at this absolute 
          path (if supported by the sandboxing implementation, ignored otherwise).
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val skipIncompatibleExplicitTargets =
      BazelFlag.boolean(
        name = "skip_incompatible_explicit_targets",
        description =
          """
          Skip incompatible targets that are explicitly listed on the command line. 
          By default, building such targets results in an error but they are silently 
          skipped when this option is enabled. See: https://bazel.
          build/extending/platforms#skipping-incompatible-targets
          """.trimIndent(),
      )

// comma-separated list of options
// default: ""
    @JvmField
    @Suppress("unused")
    val spawnStrategy =
      BazelFlag<String>(
        name = "spawn_strategy",
        description =
          """
          Specify how spawn actions are executed by default. Accepts a comma-
          separated list of strategies from highest to lowest priority. For each 
          action Bazel picks the strategy with the highest priority that can execute 
          the action. The default value is "remote,worker,sandboxed,local". See https:
          //blog.bazel.build/2019/06/19/list-strategy.html for details.
          """.trimIndent(),
      )

// a '[name=]value1[,..,valueN]' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val strategy =
      BazelFlag<String>(
        name = "strategy",
        description =
          """
          Specify how to distribute compilation of other spawn actions. Accepts a 
          comma-separated list of strategies from highest to lowest priority. For 
          each action Bazel picks the strategy with the highest priority that can 
          execute the action. The default value is "remote,worker,sandboxed,local". 
          This flag overrides the values set by --spawn_strategy (and --
          genrule_strategy if used with mnemonic Genrule). See https://blog.bazel.
          build/2019/06/19/list-strategy.html for details.
          """.trimIndent(),
      )

// a '<RegexFilter>=value[,value]' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val strategyRegexp =
      BazelFlag<String>(
        name = "strategy_regexp",
        description =
          """
          Override which spawn strategy should be used to execute spawn actions that 
          have descriptions matching a certain regex_filter. See --per_file_copt for 
          details onregex_filter matching. The last regex_filter that matches the 
          description is used. This option overrides other flags for specifying 
          strategy. Example: --strategy_regexp=//foo.*\.cc,-//foo/bar=local means to 
          run actions using local strategy if their descriptions match //foo.*.cc but 
          not //foo/bar. Example: --strategy_regexp='Compiling.*/bar=local  --
          strategy_regexp=Compiling=sandboxed will run 'Compiling //foo/bar/baz' with 
          the 'local' strategy, but reversing the order would run it with 
          'sandboxed'.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val useTargetPlatformForTests =
      BazelFlag.boolean(
        name = "use_target_platform_for_tests",
        description =
          """
          If true, then Bazel will use the target platform for running tests rather 
          than the test exec group.
          """.trimIndent(),
      )

// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val workerExtraFlag =
      BazelFlag<String>(
        name = "worker_extra_flag",
        description =
          """
          Extra command-flags that will be passed to worker processes in addition to 
          --persistent_worker, keyed by mnemonic (e.g. --worker_extra_flag=Javac=--
          debug.
          """.trimIndent(),
      )

// [name=]value, where value is an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val workerMaxInstances =
      BazelFlag<String>(
        name = "worker_max_instances",
        description =
          """
          How many instances of each kind of persistent worker may be launched if you 
          use the 'worker' strategy. May be specified as [name=value] to give a 
          different value per mnemonic. The limit is based on worker keys, which are 
          differentiated based on mnemonic, but also on startup flags and 
          environment, so there can in some cases be more workers per mnemonic than 
          this flag specifies. Takes an integer, or a keyword ("auto", "HOST_CPUS", 
          "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", 
          "HOST_CPUS*.5". 'auto' calculates a reasonable default based on machine 
          capacity. "=value" sets a default for unspecified mnemonics.
          """.trimIndent(),
      )

// [name=]value, where value is an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val workerMaxMultiplexInstances =
      BazelFlag<String>(
        name = "worker_max_multiplex_instances",
        description =
          """
        How many WorkRequests a multiplex worker process may receive in parallel if 
        you use the 'worker' strategy with --worker_multiplex. May be specified as 
        [name=value] to give a different value per mnemonic. The limit is based on 
        worker keys, which are differentiated based on mnemonic, but also on 
        startup flags and environment, so there can in some cases be more workers 
        per mnemonic than this flag specifies. Takes an integer, or a keyword 
        ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-
*]<float>) eg. "auto", "HOST_CPUS*.5". 'auto' calculates a reasonable 
        default based on machine capacity. "=value" sets a default for unspecified 
        mnemonics.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val workerMultiplex =
      BazelFlag.boolean(
        name = "worker_multiplex",
        description =
          """
          If enabled, workers will use multiplexing if they support it.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val workerQuitAfterBuild =
      BazelFlag.boolean(
        name = "worker_quit_after_build",
        description =
          """
          If enabled, all workers quit after a build is done.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val workerSandboxing =
      BazelFlag.boolean(
        name = "worker_sandboxing",
        description =
          """
          If enabled, workers will be executed in a sandboxed environment.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val workerVerbose =
      BazelFlag.boolean(
        name = "worker_verbose",
        description =
          """
          If enabled, prints verbose messages when workers are started, shutdown, ...
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options that configure the toolchain used for action execution:
// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val androidCompiler =
      BazelFlag<String>(
        name = "android_compiler",
        description =
          """
          The Android target compiler.
          """.trimIndent(),
      )

// a build target label
// default: "//external:android/crosstool"
    @JvmField
    @Suppress("unused")
    val androidCrosstoolTop =
      BazelFlag<String>(
        name = "android_crosstool_top",
        description =
          """
          The location of the C++ compiler used for Android builds.
          """.trimIndent(),
      )

// a label
// default: see description
    @JvmField
    @Suppress("unused")
    val androidGrteTop =
      BazelFlag<String>(
        name = "android_grte_top",
        description =
          """
          The Android target grte_top.
          """.trimIndent(),
      )

// legacy, android or force_android
// default: "android"
    @JvmField
    @Suppress("unused")
    val androidManifestMerger =
      BazelFlag<String>(
        name = "android_manifest_merger",
        description =
          """
          Selects the manifest merger to use for android_binary rules. Flag to help 
          thetransition to the Android manifest merger from the legacy merger.
          """.trimIndent(),
      )

// a build target label
// default: ""
    @JvmField
    @Suppress("unused")
    val androidPlatforms =
      BazelFlag<String>(
        name = "android_platforms",
        description =
          """
          Sets the platforms that android_binary targets use. If multiple platforms 
          are specified, then the binary is a fat APKs, which contains native 
          binaries for each specified target platform.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/android:sdk"
    @JvmField
    @Suppress("unused")
    val androidSdk =
      BazelFlag<String>(
        name = "android_sdk",
        description =
          """
          Specifies Android SDK/platform that is used to build Android applications.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/cpp:toolchain"
    @JvmField
    @Suppress("unused")
    val appleCrosstoolTop =
      BazelFlag<String>(
        name = "apple_crosstool_top",
        description =
          """
          The label of the crosstool package to be used in Apple and Objc rules and 
          their dependencies.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val ccOutputDirectoryTag =
      BazelFlag<String>(
        name = "cc_output_directory_tag",
        description =
          """
          Specifies a suffix to be added to the configuration directory.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val compiler =
      BazelFlag<String>(
        name = "compiler",
        description =
          """
          The C++ compiler to use for compiling the target.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/test:lcov_merger"
    @JvmField
    @Suppress("unused")
    val coverageOutputGenerator =
      BazelFlag<String>(
        name = "coverage_output_generator",
        description =
          """
          Location of the binary that is used to postprocess raw coverage reports. 
          This must currently be a filegroup that contains a single file, the binary. 
          Defaults to '//tools/test:lcov_merger'.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/test:coverage_report_generator"
    @JvmField
    @Suppress("unused")
    val coverageReportGenerator =
      BazelFlag<String>(
        name = "coverage_report_generator",
        description =
          """
          Location of the binary that is used to generate coverage reports. This must 
          currently be a filegroup that contains a single file, the binary. Defaults 
          to '//tools/test:coverage_report_generator'.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/test:coverage_support"
    @JvmField
    @Suppress("unused")
    val coverageSupport =
      BazelFlag<String>(
        name = "coverage_support",
        description =
          """
          Location of support files that are required on the inputs of every test 
          action that collects code coverage. Defaults to '//tools/test:
          coverage_support'.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/cpp:toolchain"
    @JvmField
    @Suppress("unused")
    val crosstoolTop =
      BazelFlag<String>(
        name = "crosstool_top",
        description =
          """
          The label of the crosstool package to be used for compiling C++ code.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val customMalloc =
      BazelFlag<String>(
        name = "custom_malloc",
        description =
          """
          Specifies a custom malloc implementation. This setting overrides malloc 
          attributes in build rules.
          """.trimIndent(),
      )

// a '<RegexFilter>=<label1>[,<label2>,...]' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val experimentalAddExecConstraintsToTargets =
      BazelFlag<String>(
        name = "experimental_add_exec_constraints_to_targets",
        description =
          """
          List of comma-separated regular expressions, each optionally prefixed by - 
          (negative expression), assigned (=) to a list of comma-separated constraint 
          value targets. If a target matches no negative expression and at least one 
          positive expression its toolchain resolution will be performed as if it had 
          declared the constraint values as execution constraints. Example: //demo,-
          test=@platforms//cpus:x86_64 will add 'x86_64' to any target under //demo 
          except for those whose name contains 'test'.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalIncludeXcodeExecutionRequirements =
      BazelFlag.boolean(
        name = "experimental_include_xcode_execution_requirements",
        description =
          """
          If set, add a "requires-xcode:{version}" execution requirement to every 
          Xcode action.  If the xcode version has a hyphenated label,  also add a 
          "requires-xcode-label:{version_label}" execution requirement.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalPreferMutualXcode =
      BazelFlag.boolean(
        name = "experimental_prefer_mutual_xcode",
        description =
          """
          If true, use the most recent Xcode that is available both locally and 
          remotely. If false, or if there are no mutual available versions, use the 
          local Xcode version selected via xcode-select.
          """.trimIndent(),
      )

// comma-separated list of options
// default: ""
    @JvmField
    @Suppress("unused")
    val extraExecutionPlatforms =
      BazelFlag<String>(
        name = "extra_execution_platforms",
        description =
          """
          The platforms that are available as execution platforms to run actions. 
          Platforms can be specified by exact target, or as a target pattern. These 
          platforms will be considered before those declared in the WORKSPACE file by 
          register_execution_platforms(). This option may only be set once; later 
          instances will override earlier flag settings.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val extraToolchains =
      BazelFlag<String>(
        name = "extra_toolchains",
        description =
          """
          The toolchain rules to be considered during toolchain resolution. 
          Toolchains can be specified by exact target, or as a target pattern. These 
          toolchains will be considered before those declared in the WORKSPACE file 
          by register_toolchains().
          """.trimIndent(),
      )

// a label
// default: see description
    @JvmField
    @Suppress("unused")
    val grteTop =
      BazelFlag<String>(
        name = "grte_top",
        description =
          """
          A label to a checked-in libc library. The default value is selected by the 
          crosstool toolchain, and you almost never need to override it.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val hostCompiler =
      BazelFlag<String>(
        name = "host_compiler",
        description =
          """
          The C++ compiler to use for host compilation. It is ignored if --
          host_crosstool_top is not set.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val hostCrosstoolTop =
      BazelFlag<String>(
        name = "host_crosstool_top",
        description =
          """
          By default, the --crosstool_top and --compiler options are also used for 
          the exec configuration. If this flag is provided, Bazel uses the default 
          libc and compiler for the given crosstool_top.
          """.trimIndent(),
      )

// a label
// default: see description
    @JvmField
    @Suppress("unused")
    val hostGrteTop =
      BazelFlag<String>(
        name = "host_grte_top",
        description =
          """
          If specified, this setting overrides the libc top-level directory (--
          grte_top) for the exec configuration.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools:host_platform"
    @JvmField
    @Suppress("unused")
    val hostPlatform =
      BazelFlag<String>(
        name = "host_platform",
        description =
          """
          The label of a platform rule that describes the host system.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleDontEnableHostNonhostCrosstoolFeatures =
      BazelFlag.boolean(
        name = "incompatible_dont_enable_host_nonhost_crosstool_features",
        description =
          """
          If true, Bazel will not enable 'host' and 'nonhost' features in the c++ 
          toolchain (see https://github.com/bazelbuild/bazel/issues/7407 for more 
          information).
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleEnableAndroidToolchainResolution =
      BazelFlag.boolean(
        name = "incompatible_enable_android_toolchain_resolution",
        description =
          """
          Use toolchain resolution to select the Android SDK for android rules 
          (Starlark and native)
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleEnableAppleToolchainResolution =
      BazelFlag.boolean(
        name = "incompatible_enable_apple_toolchain_resolution",
        description =
          """
          Use toolchain resolution to select the Apple SDK for apple rules (Starlark 
          and native)
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleEnableProtoToolchainResolution =
      BazelFlag.boolean(
        name = "incompatible_enable_proto_toolchain_resolution",
        description =
          """
          If true, proto lang rules define toolchains from rules_proto, rules_java, 
          rules_cc repositories.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleMakeThinltoCommandLinesStandalone =
      BazelFlag.boolean(
        name = "incompatible_make_thinlto_command_lines_standalone",
        description =
          """
          If true, Bazel will not reuse C++ link action command lines for lto 
          indexing command lines (see https://github.com/bazelbuild/bazel/issues/6791 
          for more information).
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleRemoveLegacyWholeArchive =
      BazelFlag.boolean(
        name = "incompatible_remove_legacy_whole_archive",
        description =
          """
          If true, Bazel will not link library dependencies as whole archive by 
          default (see https://github.com/bazelbuild/bazel/issues/7362 for migration 
          instructions).
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleRequireCtxInConfigureFeatures =
      BazelFlag.boolean(
        name = "incompatible_require_ctx_in_configure_features",
        description =
          """
          If true, Bazel will require 'ctx' parameter in to cc_common.
          configure_features (see https://github.com/bazelbuild/bazel/issues/7793 for 
          more information).
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val interfaceSharedObjects =
      BazelFlag.boolean(
        name = "interface_shared_objects",
        description =
          """
          Use interface shared objects if supported by the toolchain. All ELF 
          toolchains currently support this setting.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val iosSdkVersion =
      BazelFlag<String>(
        name = "ios_sdk_version",
        description =
          """
          Specifies the version of the iOS SDK to use to build iOS applications. If 
          unspecified, uses default iOS SDK version from 'xcode_version'.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val macosSdkVersion =
      BazelFlag<String>(
        name = "macos_sdk_version",
        description =
          """
          Specifies the version of the macOS SDK to use to build macOS applications. 
          If unspecified, uses default macOS SDK version from 'xcode_version'.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val minimumOsVersion =
      BazelFlag<String>(
        name = "minimum_os_version",
        description =
          """
          The minimum OS version which your compilation targets.
          """.trimIndent(),
      )

// a relative path
// default: ""
    @JvmField
    @Suppress("unused")
    val platformMappings =
      BazelFlag<String>(
        name = "platform_mappings",
        description =
          """
          The location of a mapping file that describes which platform to use if none 
          is set or which flags to set when a platform already exists. Must be 
          relative to the main workspace root. Defaults to 'platform_mappings' (a 
          file directly under the workspace root).
          """.trimIndent(),
      )

// a build target label
// default: ""
    @JvmField
    @Suppress("unused")
    val platforms =
      BazelFlag<String>(
        name = "platforms",
        description =
          """
          The labels of the platform rules describing the target platforms for the 
          current command.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val python2Path =
      BazelFlag<String>(
        name = "python2_path",
        description =
          """
          Deprecated, no-op. Disabled by `--incompatible_use_python_toolchains`.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val python3Path =
      BazelFlag<String>(
        name = "python3_path",
        description =
          """
          Deprecated, no-op. Disabled by `--incompatible_use_python_toolchains`.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val pythonPath =
      BazelFlag<String>(
        name = "python_path",
        description =
          """
          The absolute path of the Python interpreter invoked to run Python targets 
          on the target platform. Deprecated; disabled by --
          incompatible_use_python_toolchains.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val pythonTop =
      BazelFlag<String>(
        name = "python_top",
        description =
          """
          The label of a py_runtime representing the Python interpreter invoked to 
          run Python targets on the target platform. Deprecated; disabled by --
          incompatible_use_python_toolchains.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val targetPlatformFallback =
      BazelFlag<String>(
        name = "target_platform_fallback",
        description =
          """
          This option is deprecated and has no effect.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val tvosSdkVersion =
      BazelFlag<String>(
        name = "tvos_sdk_version",
        description =
          """
          Specifies the version of the tvOS SDK to use to build tvOS applications. If 
          unspecified, uses default tvOS SDK version from 'xcode_version'.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val watchosSdkVersion =
      BazelFlag<String>(
        name = "watchos_sdk_version",
        description =
          """
          Specifies the version of the watchOS SDK to use to build watchOS 
          applications. If unspecified, uses default watchOS SDK version from 
          'xcode_version'.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val xcodeVersion =
      BazelFlag<String>(
        name = "xcode_version",
        description =
          """
          If specified, uses Xcode of the given version for relevant build actions. 
          If unspecified, uses the executor default version of Xcode.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/cpp:host_xcodes"
    @JvmField
    @Suppress("unused")
    val xcodeVersionConfig =
      BazelFlag<String>(
        name = "xcode_version_config",
        description =
          """
          The label of the xcode_config rule to be used for selecting the Xcode 
          version in the build configuration.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options that control the output of the command:
// default: "false"
    @JvmField
    @Suppress("unused")
    val appleGenerateDsym =
      BazelFlag.boolean(
        name = "apple_generate_dsym",
        description =
          """
          Whether to generate debug symbol(.dSYM) file(s).
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val build =
      BazelFlag.boolean(
        name = "build",
        description =
          """
          Execute the build; this is the usual behaviour. Specifying --nobuild causes 
          the build to stop before executing the build actions, returning zero iff 
          the package loading and analysis phases completed successfully; this mode 
          is useful for testing those phases.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val buildRunfileLinks =
      BazelFlag.boolean(
        name = "build_runfile_links",
        description =
          """
          If true, build runfiles symlink forests for all targets.  If false, write 
          them only when required by a local action, test or run command.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val buildRunfileManifests =
      BazelFlag.boolean(
        name = "build_runfile_manifests",
        description =
          """
          If true, write runfiles manifests for all targets. If false, omit them. 
          Local tests will fail to run when false.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val buildTestDwp =
      BazelFlag.boolean(
        name = "build_test_dwp",
        description =
          """
          If enabled, when building C++ tests statically and with fission the .dwp 
          file  for the test binary will be automatically built as well.
          """.trimIndent(),
      )

// comma-separated set of options
// default: ".pb.h"
    @JvmField
    @Suppress("unused")
    val ccProtoLibraryHeaderSuffixes =
      BazelFlag<String>(
        name = "cc_proto_library_header_suffixes",
        description =
          """
          Sets the suffixes of header files that a cc_proto_library creates.
          """.trimIndent(),
      )

// comma-separated set of options
// default: ".pb.cc"
    @JvmField
    @Suppress("unused")
    val ccProtoLibrarySourceSuffixes =
      BazelFlag<String>(
        name = "cc_proto_library_source_suffixes",
        description =
          """
          Sets the suffixes of source files that a cc_proto_library creates.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalProtoDescriptorSetsIncludeSourceInfo =
      BazelFlag.boolean(
        name = "experimental_proto_descriptor_sets_include_source_info",
        description =
          """
          Run extra actions for alternative Java api versions in a proto_library.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalProtoExtraActions =
      BazelFlag.boolean(
        name = "experimental_proto_extra_actions",
        description =
          """
          Run extra actions for alternative Java api versions in a proto_library.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalSaveFeatureState =
      BazelFlag.boolean(
        name = "experimental_save_feature_state",
        description =
          """
          Save the state of enabled and requested feautres as an output of 
          compilation.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalUseValidationAspect =
      BazelFlag.boolean(
        name = "experimental_use_validation_aspect",
        description =
          """
          Whether to run validation actions using aspect (for parallelism with tests).
          """.trimIndent(),
      )

// a set of compilation modes
// default: "no"
    @JvmField
    @Suppress("unused")
    val fission =
      BazelFlag<String>(
        name = "fission",
        description =
          """
          Specifies which compilation modes use fission for C++ compilations and 
          links.  May be any combination of {'fastbuild', 'dbg', 'opt'} or the 
          special values 'yes'  to enable all modes and 'no' to disable all modes.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleAlwaysIncludeFilesInData =
      BazelFlag.boolean(
        name = "incompatible_always_include_files_in_data",
        description =
          """
          If true, native rules add <code>DefaultInfo.files</code> of data 
          dependencies to their runfiles, which matches the recommended behavior for 
          Starlark rules (https://bazel.
          build/extending/rules#runfiles_features_to_avoid).
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val legacyExternalRunfiles =
      BazelFlag.boolean(
        name = "legacy_external_runfiles",
        description =
          """
          If true, build runfiles symlink forests for external repositories under .
          runfiles/wsname/external/repo (in addition to .runfiles/repo).
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val objcGenerateLinkmap =
      BazelFlag.boolean(
        name = "objc_generate_linkmap",
        description =
          """
          Specifies whether to generate a linkmap file.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val outputGroups =
      BazelFlag<String>(
        name = "output_groups",
        description =
          """
          A list of comma-separated output group names, each of which optionally 
          prefixed by a + or a -. A group prefixed by + is added to the default set 
          of output groups, while a group prefixed by - is removed from the default 
          set. If at least one group is not prefixed, the default set of output 
          groups is omitted. For example, --output_groups=+foo,+bar builds the union 
          of the default set, foo, and bar, while --output_groups=foo,bar overrides 
          the default set such that only foo and bar are built.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val runValidations =
      BazelFlag.boolean(
        name = "run_validations",
        description =
          """
          Whether to run validation actions as part of the build. See https://bazel.
          build/extending/rules#validation_actions
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val saveTemps =
      BazelFlag.boolean(
        name = "save_temps",
        description =
          """
          If set, temporary outputs from gcc will be saved.  These include .s files 
          (assembler code), .i files (preprocessed C) and .ii files (preprocessed 
          C++).
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options that let the user configure the intended output, affecting its value, as opposed to its existence:
// a 'name=value' assignment with an optional value part
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val actionEnv =
      BazelFlag<String>(
        name = "action_env",
        description =
          """
          Specifies the set of environment variables available to actions with target 
          configuration. Variables can be either specified by name, in which case the 
          value will be taken from the invocation environment, or by the name=value 
          pair which sets the value independent of the invocation environment. This 
          option can be used multiple times; for options given for the same variable, 
          the latest wins, options for different variables accumulate.
          """.trimIndent(),
      )

// a string
// default: "armeabi-v7a"
    @JvmField
    @Suppress("unused")
    val androidCpu =
      BazelFlag<String>(
        name = "android_cpu",
        description =
          """
          The Android target CPU.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val androidDatabindingUseAndroidx =
      BazelFlag.boolean(
        name = "android_databinding_use_androidx",
        description =
          """
          Generate AndroidX-compatible data-binding files. This is only used with 
          databinding v2. This flag is a no-op.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val androidDatabindingUseV3_4Args =
      BazelFlag.boolean(
        name = "android_databinding_use_v3_4_args",
        description =
          """
          Use android databinding v2 with 3.4.0 argument. This flag is a no-op.
          """.trimIndent(),
      )

// off, default or fully
// default: "off"
    @JvmField
    @Suppress("unused")
    val androidDynamicMode =
      BazelFlag<String>(
        name = "android_dynamic_mode",
        description =
          """
          Determines whether C++ deps of Android rules will be linked dynamically 
          when a cc_binary does not explicitly create a shared library. 'default' 
          means bazel will choose whether to link dynamically.  'fully' means all 
          libraries will be linked dynamically. 'off' means that all libraries will 
          be linked in mostly static mode.
          """.trimIndent(),
      )

// alphabetical, alphabetical_by_configuration or dependency
// default: "alphabetical"
    @JvmField
    @Suppress("unused")
    val androidManifestMergerOrder =
      BazelFlag<String>(
        name = "android_manifest_merger_order",
        description =
          """
          Sets the order of manifests passed to the manifest merger for Android 
          binaries. ALPHABETICAL means manifests are sorted by path relative to the 
          execroot. ALPHABETICAL_BY_CONFIGURATION means manifests are sorted by paths 
          relative to the configuration directory within the output directory. 
          DEPENDENCY means manifests are ordered with each library's manifest coming 
          before the manifests of its dependencies.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val androidResourceShrinking =
      BazelFlag.boolean(
        name = "android_resource_shrinking",
        description =
          """
          Enables resource shrinking for android_binary APKs that use ProGuard.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val aspects =
      BazelFlag<String>(
        name = "aspects",
        description =
          """
          Comma-separated list of aspects to be applied to top-level targets. In the 
          list, if aspect some_aspect specifies required aspect providers via 
          required_aspect_providers, some_aspect will run after every aspect that was 
          mentioned before it in the aspects list whose advertised providers satisfy 
          some_aspect required aspect providers. Moreover, some_aspect will run after 
          all its required aspects specified by requires attribute. some_aspect will 
          then have access to the values of those aspects' providers. <bzl-file-
          label>%<aspect_name>, for example '//tools:my_def.bzl%my_aspect', where 
          'my_aspect' is a top-level value from a file tools/my_def.bzl
          """.trimIndent(),
      )

// an integer
// default: "-1"
    @JvmField
    @Suppress("unused")
    val bepMaximumOpenRemoteUploadFiles =
      BazelFlag<String>(
        name = "bep_maximum_open_remote_upload_files",
        description =
          """
          Maximum number of open files allowed during BEP artifact upload.
          """.trimIndent(),
      )

// a tri-state (auto, yes, no)
// default: "auto"
    @JvmField
    @Suppress("unused")
    val buildPythonZip =
      BazelFlag<String>(
        name = "build_python_zip",
        description =
          """
          Build python executable zip; on on Windows, off on other platforms
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val catalystCpus =
      BazelFlag<String>(
        name = "catalyst_cpus",
        description =
          """
          Comma-separated list of architectures for which to build Apple Catalyst 
          binaries.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val collectCodeCoverage =
      BazelFlag.boolean(
        name = "collect_code_coverage",
        description =
          """
          If specified, Bazel will instrument code (using offline instrumentation 
          where possible) and will collect coverage information during tests. Only 
          targets that  match --instrumentation_filter will be affected. Usually this 
          option should  not be specified directly - 'bazel coverage' command should 
          be used instead.
          """.trimIndent(),
      )

// fastbuild, dbg or opt
// default: "fastbuild"
    @JvmField
    @Suppress("unused")
    val compilationMode =
      BazelFlag<String>(
        name = "compilation_mode",
        description =
          """
          Specify the mode the binary will be built in. Values: 'fastbuild', 'dbg', 
          'opt'.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val conlyopt =
      BazelFlag<String>(
        name = "conlyopt",
        description =
          """
          Additional option to pass to gcc when compiling C source files.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val copt =
      BazelFlag<String>(
        name = "copt",
        description =
          """
          Additional options to pass to gcc.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val cpu =
      BazelFlag<String>(
        name = "cpu",
        description =
          """
          The target CPU.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val csFdoAbsolutePath =
      BazelFlag<String>(
        name = "cs_fdo_absolute_path",
        description =
          """
          Use CSFDO profile information to optimize compilation. Specify the absolute 
          path name of the zip file containing the profile file, a raw or an indexed 
          LLVM profile file.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val csFdoInstrument =
      BazelFlag<String>(
        name = "cs_fdo_instrument",
        description =
          """
          Generate binaries with context sensitive FDO instrumentation. With 
          Clang/LLVM compiler, it also accepts the directory name under which the raw 
          profile file(s) will be dumped at runtime.
            Using this option will also add: --copt=-Wno-error
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val csFdoProfile =
      BazelFlag<String>(
        name = "cs_fdo_profile",
        description =
          """
          The cs_fdo_profile representing the context sensitive profile to be used 
          for optimization.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val cxxopt =
      BazelFlag<String>(
        name = "cxxopt",
        description =
          """
          Additional option to pass to gcc when compiling C++ source files.
          """.trimIndent(),
      )

// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val define =
      BazelFlag<String>(
        name = "define",
        description =
          """
          Each --define option specifies an assignment for a build variable.
          """.trimIndent(),
      )

// off, default or fully
// default: "default"
    @JvmField
    @Suppress("unused")
    val dynamicMode =
      BazelFlag<String>(
        name = "dynamic_mode",
        description =
          """
          Determines whether C++ binaries will be linked dynamically.  'default' 
          means Bazel will choose whether to link dynamically.  'fully' means all 
          libraries will be linked dynamically. 'off' means that all libraries will 
          be linked in mostly static mode.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val enableFdoProfileAbsolutePath =
      BazelFlag.boolean(
        name = "enable_fdo_profile_absolute_path",
        description =
          """
          If set, use of fdo_absolute_profile_path will raise an error.
          """.trimIndent(),
      )

// a tri-state (auto, yes, no)
// default: "auto"
    @JvmField
    @Suppress("unused")
    val enableRunfiles =
      BazelFlag<String>(
        name = "enable_runfiles",
        description =
          """
          Enable runfiles symlink tree; By default, it's off on Windows, on on other 
          platforms.
          """.trimIndent(),
      )

// a build target label
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val experimentalActionListener =
      BazelFlag<String>(
        name = "experimental_action_listener",
        description =
          """
          Deprecated in favor of aspects. Use action_listener to attach an 
          extra_action to existing build actions.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalAndroidCompressJavaResources =
      BazelFlag.boolean(
        name = "experimental_android_compress_java_resources",
        description =
          """
          Compress Java resources in APKs
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalAndroidDatabindingV2 =
      BazelFlag.boolean(
        name = "experimental_android_databinding_v2",
        description =
          """
          Use android databinding v2. This flag is a no-op.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalAndroidResourceShrinking =
      BazelFlag.boolean(
        name = "experimental_android_resource_shrinking",
        description =
          """
          Enables resource shrinking for android_binary APKs that use ProGuard.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalAndroidRewriteDexesWithRex =
      BazelFlag.boolean(
        name = "experimental_android_rewrite_dexes_with_rex",
        description =
          """
          use rex tool to rewrite dex files
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalCollectCodeCoverageForGeneratedFiles =
      BazelFlag.boolean(
        name = "experimental_collect_code_coverage_for_generated_files",
        description =
          """
          If specified, Bazel will also generate collect coverage information for 
          generated files.
          """.trimIndent(),
      )

// normal, clean, ignore or log_only
// default: "normal"
    @JvmField
    @Suppress("unused")
    val experimentalConvenienceSymlinks =
      BazelFlag<String>(
        name = "experimental_convenience_symlinks",
        description =
          """
          This flag controls how the convenience symlinks (the symlinks that appear 
          in the workspace after the build) will be managed. Possible values:
            normal (default): Each kind of convenience symlink will be created or 
          deleted, as determined by the build.
            clean: All symlinks will be unconditionally deleted.
            ignore: Symlinks will be left alone.
            log_only: Generate log messages as if 'normal' were passed, but don't 
          actually perform any filesystem operations (useful for tools).
          Note that only symlinks whose names are generated by the current value of --
          symlink_prefix can be affected; if the prefix changes, any pre-existing 
          symlinks will be left alone.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalConvenienceSymlinksBepEvent =
      BazelFlag.boolean(
        name = "experimental_convenience_symlinks_bep_event",
        description =
          """
          This flag controls whether or not we will post the build 
          eventConvenienceSymlinksIdentified to the BuildEventProtocol. If the value 
          is true, the BuildEventProtocol will have an entry for 
          convenienceSymlinksIdentified, listing all of the convenience symlinks 
          created in your workspace. If false, then the convenienceSymlinksIdentified 
          entry in the BuildEventProtocol will be empty.
          """.trimIndent(),
      )

// comma-separated list of options
// default: "-O0,-DDEBUG=1"
    @JvmField
    @Suppress("unused")
    val experimentalObjcFastbuildOptions =
      BazelFlag<String>(
        name = "experimental_objc_fastbuild_options",
        description =
          """
          Uses these strings as objc fastbuild compiler options.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalOmitfp =
      BazelFlag.boolean(
        name = "experimental_omitfp",
        description =
          """
          If true, use libunwind for stack unwinding, and compile with -fomit-frame-
          pointer and -fasynchronous-unwind-tables.
          """.trimIndent(),
      )

// off, content or strip
// default: "off"
    @JvmField
    @Suppress("unused")
    val experimentalOutputPaths =
      BazelFlag<String>(
        name = "experimental_output_paths",
        description =
          """
          Which model to use for where in the output tree rules write their outputs, 
          particularly for multi-platform / multi-configuration builds. This is 
          highly experimental. See https://github.com/bazelbuild/bazel/issues/6526 
          for details. Starlark actions canopt into path mapping by adding the key 
          'supports-path-mapping' to the 'execution_requirements' dict.
          """.trimIndent(),
      )

// a 'label=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val experimentalOverrideNamePlatformInOutputDir =
      BazelFlag<String>(
        name = "experimental_override_name_platform_in_output_dir",
        description =
          """
          Each entry should be of the form label=value where label refers to a 
          platform and values is the desired shortname to use in the output path. 
          Only used when --experimental_platform_in_output_dir is true. Has highest 
          naming priority.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalPlatformInOutputDir =
      BazelFlag.boolean(
        name = "experimental_platform_in_output_dir",
        description =
          """
          If true, a shortname for the target platform is used in the output 
          directory name instead of the CPU. The exact scheme is experimental and 
          subject to change: First, in the rare case the --platforms option does not 
          have exactly one value, a hash of the platforms option is used. Next, if 
          any shortname for the current platform was registered by --
          experimental_override_name_platform_in_output_dir, then that shortname is 
          used. Then, if --experimental_use_platforms_in_output_dir_legacy_heuristic 
          is set, use a shortname based off the current platform Label. Finally, a 
          hash of the platform option is used as a last resort.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalUseLlvmCovmap =
      BazelFlag.boolean(
        name = "experimental_use_llvm_covmap",
        description =
          """
          If specified, Bazel will generate llvm-cov coverage map information rather 
          than gcov when collect_code_coverage is enabled.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalUsePlatformsInOutputDirLegacyHeuristic =
      BazelFlag.boolean(
        name = "experimental_use_platforms_in_output_dir_legacy_heuristic",
        description =
          """
          Please only use this flag as part of a suggested migration or testing 
          strategy. Note that the heuristic has known deficiencies and it is 
          suggested to migrate to relying on just --
          experimental_override_name_platform_in_output_dir.
          """.trimIndent(),
      )

// comma-separated set of options
// default: "armeabi-v7a"
    @JvmField
    @Suppress("unused")
    val fatApkCpu =
      BazelFlag<String>(
        name = "fat_apk_cpu",
        description =
          """
          Setting this option enables fat APKs, which contain native binaries for all 
          specified target architectures, e.g., --fat_apk_cpu=x86,armeabi-v7a. If 
          this flag is specified, then --android_cpu is ignored for dependencies of 
          android_binary rules.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val fatApkHwasan =
      BazelFlag.boolean(
        name = "fat_apk_hwasan",
        description =
          """
          Whether to create HWASAN splits.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val fdoInstrument =
      BazelFlag<String>(
        name = "fdo_instrument",
        description =
          """
          Generate binaries with FDO instrumentation. With Clang/LLVM compiler, it 
          also accepts the directory name under which the raw profile file(s) will be 
          dumped at runtime.
            Using this option will also add: --copt=-Wno-error
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val fdoOptimize =
      BazelFlag<String>(
        name = "fdo_optimize",
        description =
          """
          Use FDO profile information to optimize compilation. Specify the name of a 
          zip file containing a .gcda file tree, an afdo file containing an auto 
          profile, or an LLVM profile file. This flag also accepts files specified as 
          labels (e.g. `//foo/bar:file.afdo` - you may need to add an `exports_files` 
          directive to the corresponding package) and labels pointing to 
          `fdo_profile` targets. This flag will be superseded by the `fdo_profile` 
          rule.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val fdoPrefetchHints =
      BazelFlag<String>(
        name = "fdo_prefetch_hints",
        description =
          """
          Use cache prefetch hints.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val fdoProfile =
      BazelFlag<String>(
        name = "fdo_profile",
        description =
          """
          The fdo_profile representing the profile to be used for optimization.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val features =
      BazelFlag<String>(
        name = "features",
        description =
          """
          The given features will be enabled or disabled by default for targets built 
          in the target configuration. Specifying -<feature> will disable the 
          feature. Negative features always override positive ones. See also --
          host_features
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val forcePic =
      BazelFlag.boolean(
        name = "force_pic",
        description =
          """
          If enabled, all C++ compilations produce position-independent code ("-
          fPIC"), links prefer PIC pre-built libraries over non-PIC libraries, and 
          links produce position-independent executables ("-pie").
          """.trimIndent(),
      )

// a 'name=value' assignment with an optional value part
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostActionEnv =
      BazelFlag<String>(
        name = "host_action_env",
        description =
          """
          Specifies the set of environment variables available to actions with 
          execution configurations. Variables can be either specified by name, in 
          which case the value will be taken from the invocation environment, or by 
          the name=value pair which sets the value independent of the invocation 
          environment. This option can be used multiple times; for options given for 
          the same variable, the latest wins, options for different variables 
          accumulate.
          """.trimIndent(),
      )

// fastbuild, dbg or opt
// default: "opt"
    @JvmField
    @Suppress("unused")
    val hostCompilationMode =
      BazelFlag<String>(
        name = "host_compilation_mode",
        description =
          """
          Specify the mode the tools used during the build will be built in. Values: 
          'fastbuild', 'dbg', 'opt'.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostConlyopt =
      BazelFlag<String>(
        name = "host_conlyopt",
        description =
          """
          Additional option to pass to the C compiler when compiling C (but not C++) 
          source files in the exec configurations.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostCopt =
      BazelFlag<String>(
        name = "host_copt",
        description =
          """
          Additional options to pass to the C compiler for tools built in the exec 
          configurations.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val hostCpu =
      BazelFlag<String>(
        name = "host_cpu",
        description =
          """
          The host CPU.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostCxxopt =
      BazelFlag<String>(
        name = "host_cxxopt",
        description =
          """
          Additional options to pass to C++ compiler for tools built in the exec 
          configurations.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostFeatures =
      BazelFlag<String>(
        name = "host_features",
        description =
          """
          The given features will be enabled or disabled by default for targets built 
          in the exec configuration. Specifying -<feature> will disable the feature. 
          Negative features always override positive ones.
          """.trimIndent(),
      )

// PY2 or PY3
// default: see description
    @JvmField
    @Suppress("unused")
    val hostForcePython =
      BazelFlag<String>(
        name = "host_force_python",
        description =
          """
          Overrides the Python version for the exec configuration. Can be "PY2" or 
          "PY3".
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostLinkopt =
      BazelFlag<String>(
        name = "host_linkopt",
        description =
          """
          Additional option to pass to linker when linking tools in the exec 
          configurations.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val hostMacosMinimumOs =
      BazelFlag<String>(
        name = "host_macos_minimum_os",
        description =
          """
          Minimum compatible macOS version for host targets. If unspecified, uses 
          'macos_sdk_version'.
          """.trimIndent(),
      )

// a comma-separated list of regex expressions with prefix '-' specifying excluded paths followed by an @ and a comma separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostPerFileCopt =
      BazelFlag<String>(
        name = "host_per_file_copt",
        description =
          """
          Additional options to selectively pass to the C/C++ compiler when compiling 
          certain files in the exec configurations. This option can be passed 
          multiple times. Syntax: regex_filter@option_1,option_2,...,option_n. Where 
          regex_filter stands for a list of include and exclude regular expression 
          patterns (Also see --instrumentation_filter). option_1 to option_n stand 
          for arbitrary command line options. If an option contains a comma it has to 
          be quoted with a backslash. Options can contain @. Only the first @ is used 
          to split the string. Example: --host_per_file_copt=//foo/.*\.cc,-//foo/bar\.
          cc@-O0 adds the -O0 command line option to the gcc command line of all cc 
          files in //foo/ except bar.cc.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostSwiftcopt =
      BazelFlag<String>(
        name = "host_swiftcopt",
        description =
          """
          Additional options to pass to swiftc for exec tools.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleAutoExecGroups =
      BazelFlag.boolean(
        name = "incompatible_auto_exec_groups",
        description =
          """
          When enabled, an exec groups is automatically created for each toolchain 
          used by a rule. For this to work rule needs to specify `toolchain` 
          parameter on its actions. For more information, see https://github.
          com/bazelbuild/bazel/issues/17134.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleMergeGenfilesDirectory =
      BazelFlag.boolean(
        name = "incompatible_merge_genfiles_directory",
        description =
          """
          If true, the genfiles directory is folded into the bin directory.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleUseHostFeatures =
      BazelFlag.boolean(
        name = "incompatible_use_host_features",
        description =
          """
          If true, use --features only for the target configuration and --
          host_features for the exec configuration.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val instrumentTestTargets =
      BazelFlag.boolean(
        name = "instrument_test_targets",
        description =
          """
          When coverage is enabled, specifies whether to consider instrumenting test 
          rules. When set, test rules included by --instrumentation_filter are 
          instrumented. Otherwise, test rules are always excluded from coverage 
          instrumentation.
          """.trimIndent(),
      )

// a comma-separated list of regex expressions with prefix '-' specifying excluded paths
// default: "-/javatests[/:],-/test/java[/:]"
    @JvmField
    @Suppress("unused")
    val instrumentationFilter =
      BazelFlag<String>(
        name = "instrumentation_filter",
        description =
          """
          When coverage is enabled, only rules with names included by the specified 
          regex-based filter will be instrumented. Rules prefixed with '-' are 
          excluded instead. Note that only non-test rules are instrumented unless --
          instrument_test_targets is enabled.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val iosMinimumOs =
      BazelFlag<String>(
        name = "ios_minimum_os",
        description =
          """
          Minimum compatible iOS version for target simulators and devices. If 
          unspecified, uses 'ios_sdk_version'.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val iosMultiCpus =
      BazelFlag<String>(
        name = "ios_multi_cpus",
        description =
          """
          Comma-separated list of architectures to build an ios_application with. The 
          result is a universal binary containing all specified architectures.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val legacyWholeArchive =
      BazelFlag.boolean(
        name = "legacy_whole_archive",
        description =
          """
          Deprecated, superseded by --incompatible_remove_legacy_whole_archive (see 
          https://github.com/bazelbuild/bazel/issues/7362 for details). When on, use 
          --whole-archive for cc_binary rules that have linkshared=True and either 
          linkstatic=True or '-static' in linkopts. This is for backwards 
          compatibility only. A better alternative is to use alwayslink=1 where 
          required.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val linkopt =
      BazelFlag<String>(
        name = "linkopt",
        description =
          """
          Additional option to pass to gcc when linking.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val ltobackendopt =
      BazelFlag<String>(
        name = "ltobackendopt",
        description =
          """
          Additional option to pass to the LTO backend step (under --
          features=thin_lto).
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val ltoindexopt =
      BazelFlag<String>(
        name = "ltoindexopt",
        description =
          """
          Additional option to pass to the LTO indexing step (under --
          features=thin_lto).
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val macosCpus =
      BazelFlag<String>(
        name = "macos_cpus",
        description =
          """
          Comma-separated list of architectures for which to build Apple macOS 
          binaries.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val macosMinimumOs =
      BazelFlag<String>(
        name = "macos_minimum_os",
        description =
          """
          Minimum compatible macOS version for targets. If unspecified, uses 
          'macos_sdk_version'.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val memprofProfile =
      BazelFlag<String>(
        name = "memprof_profile",
        description =
          """
          Use memprof profile.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val objcDebugWith_GLIBCXX =
      BazelFlag.boolean(
        name = "objc_debug_with_GLIBCXX",
        description =
          """
          If set, and compilation mode is set to 'dbg', define GLIBCXX_DEBUG,  
          GLIBCXX_DEBUG_PEDANTIC and GLIBCPP_CONCEPT_CHECKS.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val objcEnableBinaryStripping =
      BazelFlag.boolean(
        name = "objc_enable_binary_stripping",
        description =
          """
          Whether to perform symbol and dead-code strippings on linked binaries. 
          Binary strippings will be performed if both this flag and --
          compilation_mode=opt are specified.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val objccopt =
      BazelFlag<String>(
        name = "objccopt",
        description =
          """
          Additional options to pass to gcc when compiling Objective-C/C++ source 
          files.
          """.trimIndent(),
      )

// a comma-separated list of regex expressions with prefix '-' specifying excluded paths followed by an @ and a comma separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val perFileCopt =
      BazelFlag<String>(
        name = "per_file_copt",
        description =
          """
          Additional options to selectively pass to gcc when compiling certain files. 
          This option can be passed multiple times. Syntax: regex_filter@option_1,
          option_2,...,option_n. Where regex_filter stands for a list of include and 
          exclude regular expression patterns (Also see --instrumentation_filter). 
          option_1 to option_n stand for arbitrary command line options. If an option 
          contains a comma it has to be quoted with a backslash. Options can contain 
          @. Only the first @ is used to split the string. Example: --
          per_file_copt=//foo/.*\.cc,-//foo/bar\.cc@-O0 adds the -O0 command line 
          option to the gcc command line of all cc files in //foo/ except bar.cc.
          """.trimIndent(),
      )

// a comma-separated list of regex expressions with prefix '-' specifying excluded paths followed by an @ and a comma separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val perFileLtobackendopt =
      BazelFlag<String>(
        name = "per_file_ltobackendopt",
        description =
          """
          Additional options to selectively pass to LTO backend (under --
          features=thin_lto) when compiling certain backend objects. This option can 
          be passed multiple times. Syntax: regex_filter@option_1,option_2,...,
          option_n. Where regex_filter stands for a list of include and exclude 
          regular expression patterns. option_1 to option_n stand for arbitrary 
          command line options. If an option contains a comma it has to be quoted 
          with a backslash. Options can contain @. Only the first @ is used to split 
          the string. Example: --per_file_ltobackendopt=//foo/.*\.o,-//foo/bar\.o@-O0 
          adds the -O0 command line option to the LTO backend command line of all o 
          files in //foo/ except bar.o.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val platformSuffix =
      BazelFlag<String>(
        name = "platform_suffix",
        description =
          """
          Specifies a suffix to be added to the configuration directory.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val propellerOptimize =
      BazelFlag<String>(
        name = "propeller_optimize",
        description =
          """
          Use Propeller profile information to optimize the build target.A propeller 
          profile must consist of at least one of two files, a cc profile and a ld 
          profile.  This flag accepts a build label which must refer to the propeller 
          profile input files. For example, the BUILD file that defines the label, in 
          a/b/BUILD:propeller_optimize(    name = "propeller_profile",    cc_profile 
          = "propeller_cc_profile.txt",    ld_profile = "propeller_ld_profile.txt",)
          An exports_files directive may have to be added to the corresponding 
          package to make these files visible to Bazel. The option must be used as: --
          propeller_optimize=//a/b:propeller_profile
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val propellerOptimizeAbsoluteCcProfile =
      BazelFlag<String>(
        name = "propeller_optimize_absolute_cc_profile",
        description =
          """
          Absolute path name of cc_profile file for Propeller Optimized builds.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val propellerOptimizeAbsoluteLdProfile =
      BazelFlag<String>(
        name = "propeller_optimize_absolute_ld_profile",
        description =
          """
          Absolute path name of ld_profile file for Propeller Optimized builds.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val remoteDownloadAll =
      BazelFlag<String>(
        name = "remote_download_all",
        description =
          """
          Downloads all remote outputs to the local machine. This flag is an alias 
          for --remote_download_outputs=all.
            Expands to: --remote_download_outputs=all
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val remoteDownloadMinimal =
      BazelFlag<String>(
        name = "remote_download_minimal",
        description =
          """
          Does not download any remote build outputs to the local machine. This flag 
          is an alias for --remote_download_outputs=minimal.
            Expands to: --remote_download_outputs=minimal
          """.trimIndent(),
      )

// all, minimal or toplevel
// default: "toplevel"
    @JvmField
    @Suppress("unused")
    val remoteDownloadOutputs =
      BazelFlag<String>(
        name = "remote_download_outputs",
        description =
          """
          If set to 'minimal' doesn't download any remote build outputs to the local 
          machine, except the ones required by local actions. If set to 'toplevel' 
          behaves like'minimal' except that it also downloads outputs of top level 
          targets to the local machine. Both options can significantly reduce build 
          times if network bandwidth is a bottleneck.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val remoteDownloadSymlinkTemplate =
      BazelFlag<String>(
        name = "remote_download_symlink_template",
        description =
          """
          Instead of downloading remote build outputs to the local machine, create 
          symbolic links. The target of the symbolic links can be specified in the 
          form of a template string. This template string may contain {hash} and 
          {size_bytes} that expand to the hash of the object and the size in bytes, 
          respectively. These symbolic links may, for example, point to a FUSE file 
          system that loads objects from the CAS on demand.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val remoteDownloadToplevel =
      BazelFlag<String>(
        name = "remote_download_toplevel",
        description =
          """
          Only downloads remote outputs of top level targets to the local machine. 
          This flag is an alias for --remote_download_outputs=toplevel.
            Expands to: --remote_download_outputs=toplevel
          """.trimIndent(),
      )

// a 'name=value' assignment with an optional value part
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val repoEnv =
      BazelFlag<String>(
        name = "repo_env",
        description =
          """
          Specifies additional environment variables to be available only for 
          repository rules. Note that repository rules see the full environment 
          anyway, but in this way configuration information can be passed to 
          repositories through options without invalidating the action graph.
          """.trimIndent(),
      )

// a prefix in front of command
// default: see description
    @JvmField
    @Suppress("unused")
    val runUnder =
      BazelFlag<String>(
        name = "run_under",
        description =
          """
          Prefix to insert before the executables for the 'test' and 'run' commands. 
          If the value is 'foo -bar', and the execution command line is 'test_binary -
          baz', then the final command line is 'foo -bar test_binary -baz'.This can 
          also be a label to an executable target. Some examples are: 'valgrind', 
          'strace', 'strace -c', 'valgrind --quiet --num-callers=20', '//package:
          target',  '//package:target --options'.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val shareNativeDeps =
      BazelFlag.boolean(
        name = "share_native_deps",
        description =
          """
          If true, native libraries that contain identical functionality will be 
          shared among different targets
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val stamp =
      BazelFlag.boolean(
        name = "stamp",
        description =
          """
          Stamp binaries with the date, username, hostname, workspace information, 
          etc.
          """.trimIndent(),
      )

// always, sometimes or never
// default: "sometimes"
    @JvmField
    @Suppress("unused")
    val strip =
      BazelFlag<String>(
        name = "strip",
        description =
          """
          Specifies whether to strip binaries and shared libraries  (using "-Wl,--
          strip-debug").  The default value of 'sometimes' means strip iff --
          compilation_mode=fastbuild.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val stripopt =
      BazelFlag<String>(
        name = "stripopt",
        description =
          """
          Additional options to pass to strip when generating a '<name>.stripped' 
          binary.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val swiftcopt =
      BazelFlag<String>(
        name = "swiftcopt",
        description =
          """
          Additional options to pass to Swift compilation.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val symlinkPrefix =
      BazelFlag<String>(
        name = "symlink_prefix",
        description =
          """
          The prefix that is prepended to any of the convenience symlinks that are 
          created after a build. If omitted, the default value is the name of the 
          build tool followed by a hyphen. If '/' is passed, then no symlinks are 
          created and no warning is emitted. Warning: the special functionality for 
          '/' will be deprecated soon; use --experimental_convenience_symlinks=ignore 
          instead.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val tvosCpus =
      BazelFlag<String>(
        name = "tvos_cpus",
        description =
          """
          Comma-separated list of architectures for which to build Apple tvOS 
          binaries.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val tvosMinimumOs =
      BazelFlag<String>(
        name = "tvos_minimum_os",
        description =
          """
          Minimum compatible tvOS version for target simulators and devices. If 
          unspecified, uses 'tvos_sdk_version'.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val visionosCpus =
      BazelFlag<String>(
        name = "visionos_cpus",
        description =
          """
          Comma-separated list of architectures for which to build Apple visionOS 
          binaries.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val watchosCpus =
      BazelFlag<String>(
        name = "watchos_cpus",
        description =
          """
          Comma-separated list of architectures for which to build Apple watchOS 
          binaries.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val watchosMinimumOs =
      BazelFlag<String>(
        name = "watchos_minimum_os",
        description =
          """
          Minimum compatible watchOS version for target simulators and devices. If 
          unspecified, uses 'watchos_sdk_version'.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val xbinaryFdo =
      BazelFlag<String>(
        name = "xbinary_fdo",
        description =
          """
          Use XbinaryFDO profile information to optimize compilation. Specify the 
          name of default cross binary profile. When the option is used together with 
          --fdo_instrument/--fdo_optimize/--fdo_profile, those options will always 
          prevail as if xbinary_fdo is never specified.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options that affect how strictly Bazel enforces valid build inputs (rule definitions,  flag combinations, etc.):
// a build target label
// default: ""
    @JvmField
    @Suppress("unused")
    val autoCpuEnvironmentGroup =
      BazelFlag<String>(
        name = "auto_cpu_environment_group",
        description =
          """
          Declare the environment_group to use for automatically mapping cpu values 
          to target_environment values.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val checkBzlVisibility =
      BazelFlag.boolean(
        name = "check_bzl_visibility",
        description =
          """
          If disabled, .bzl load visibility errors are demoted to warnings.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val checkLicenses =
      BazelFlag.boolean(
        name = "check_licenses",
        description =
          """
          Check that licensing constraints imposed by dependent packages do not 
          conflict with distribution modes of the targets being built. By default, 
          licenses are not checked.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val checkVisibility =
      BazelFlag.boolean(
        name = "check_visibility",
        description =
          """
          If disabled, visibility errors in target dependencies are demoted to 
          warnings.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val desugarForAndroid =
      BazelFlag.boolean(
        name = "desugar_for_android",
        description =
          """
          Whether to desugar Java 8 bytecode before dexing.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val desugarJava8Libs =
      BazelFlag.boolean(
        name = "desugar_java8_libs",
        description =
          """
          Whether to include supported Java 8 libraries in apps for legacy devices.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val enforceConstraints =
      BazelFlag.boolean(
        name = "enforce_constraints",
        description =
          """
          Checks the environments each target is compatible with and reports errors 
          if any target has dependencies that don't support the same environments
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalCheckDesugarDeps =
      BazelFlag.boolean(
        name = "experimental_check_desugar_deps",
        description =
          """
          Whether to double-check correct desugaring at Android binary level.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalDockerPrivileged =
      BazelFlag.boolean(
        name = "experimental_docker_privileged",
        description =
          """
          If enabled, Bazel will pass the --privileged flag to 'docker run' when 
          running actions. This might be required by your build, but it might also 
          result in reduced hermeticity.
          """.trimIndent(),
      )

// off, warning or error
// default: "OFF"
    @JvmField
    @Suppress("unused")
    val experimentalImportDepsChecking =
      BazelFlag<String>(
        name = "experimental_import_deps_checking",
        description =
          """
          When enabled, check whether the dependencies of an aar_import are complete. 
          This enforcement can break the build, or can just result in warnings.
          """.trimIndent(),
      )

// off, warning or error
// default: "OFF"
    @JvmField
    @Suppress("unused")
    val experimentalOneVersionEnforcement =
      BazelFlag<String>(
        name = "experimental_one_version_enforcement",
        description =
          """
          When enabled, enforce that a java_binary rule can't contain more than one 
          version of the same class file on the classpath. This enforcement can break 
          the build, or can just result in warnings.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalSandboxfsMapSymlinkTargets =
      BazelFlag.boolean(
        name = "experimental_sandboxfs_map_symlink_targets",
        description =
          """
          No-op
          """.trimIndent(),
      )

// off, warn, error, strict or default
// default: "default"
    @JvmField
    @Suppress("unused")
    val experimentalStrictJavaDeps =
      BazelFlag<String>(
        name = "experimental_strict_java_deps",
        description =
          """
          If true, checks that a Java target explicitly declares all directly used 
          targets as dependencies.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleCheckTestonlyForOutputFiles =
      BazelFlag.boolean(
        name = "incompatible_check_testonly_for_output_files",
        description =
          """
          If enabled, check testonly for prerequisite targets that are output files 
          by looking up the testonly of the generating rule. This matches visibility 
          checking.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleCheckVisibilityForToolchains =
      BazelFlag.boolean(
        name = "incompatible_check_visibility_for_toolchains",
        description =
          """
          If enabled, visibility checking also applies to toolchain implementations.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDisableNativeAndroidRules =
      BazelFlag.boolean(
        name = "incompatible_disable_native_android_rules",
        description =
          """
          If enabled, direct usage of the native Android rules is disabled. Please 
          use the Starlark Android rules from https://github.
          com/bazelbuild/rules_android
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDisableNativeAppleBinaryRule =
      BazelFlag.boolean(
        name = "incompatible_disable_native_apple_binary_rule",
        description =
          """
          No-op. Kept here for backwards compatibility.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleLegacyLocalFallback =
      BazelFlag.boolean(
        name = "incompatible_legacy_local_fallback",
        description =
          """
          If set to true, enables the legacy implicit fallback from sandboxed to 
          local strategy. This flag will eventually default to false and then become 
          a no-op. Use --strategy, --spawn_strategy, or --dynamic_local_strategy to 
          configure fallbacks instead.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatiblePythonDisablePy2 =
      BazelFlag.boolean(
        name = "incompatible_python_disable_py2",
        description =
          """
          If true, using Python 2 settings will cause an error. This includes 
          python_version=PY2, srcs_version=PY2, and srcs_version=PY2ONLY. See https:
          //github.com/bazelbuild/bazel/issues/15684 for more information.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleValidateTopLevelHeaderInclusions =
      BazelFlag.boolean(
        name = "incompatible_validate_top_level_header_inclusions",
        description =
          """
          If true, Bazel will also validate top level directory header inclusions 
          (see https://github.com/bazelbuild/bazel/issues/10047 for more information).
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val oneVersionEnforcementOnJavaTests =
      BazelFlag.boolean(
        name = "one_version_enforcement_on_java_tests",
        description =
          """
          When enabled, and with experimental_one_version_enforcement set to a non-
          NONE value, enforce one version on java_test targets. This flag can be 
          disabled to improve incremental test performance at the expense of missing 
          potential one version violations.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val pythonNativeRulesAllowlist =
      BazelFlag<String>(
        name = "python_native_rules_allowlist",
        description =
          """
          An allowlist (package_group target) to use when enforcing --
          incompatible_python_disallow_native_rules.
          """.trimIndent(),
      )

// a single path or a 'source:target' pair
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val sandboxAddMountPair =
      BazelFlag<String>(
        name = "sandbox_add_mount_pair",
        description =
          """
          Add additional path pair to mount in sandbox.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val sandboxBlockPath =
      BazelFlag<String>(
        name = "sandbox_block_path",
        description =
          """
          For sandboxed actions, disallow access to this path.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val sandboxDefaultAllowNetwork =
      BazelFlag.boolean(
        name = "sandbox_default_allow_network",
        description =
          """
          Allow network access by default for actions; this may not work with all 
          sandboxing implementations.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val sandboxFakeHostname =
      BazelFlag.boolean(
        name = "sandbox_fake_hostname",
        description =
          """
          Change the current hostname to 'localhost' for sandboxed actions.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val sandboxFakeUsername =
      BazelFlag.boolean(
        name = "sandbox_fake_username",
        description =
          """
          Change the current username to 'nobody' for sandboxed actions.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val sandboxWritablePath =
      BazelFlag<String>(
        name = "sandbox_writable_path",
        description =
          """
          For sandboxed actions, make an existing directory writable in the sandbox 
          (if supported by the sandboxing implementation, ignored otherwise).
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val strictFilesets =
      BazelFlag.boolean(
        name = "strict_filesets",
        description =
          """
          If this option is enabled, filesets crossing package boundaries are 
          reported as errors.
          """.trimIndent(),
      )

// off, warn, error, strict or default
// default: "error"
    @JvmField
    @Suppress("unused")
    val strictProtoDeps =
      BazelFlag<String>(
        name = "strict_proto_deps",
        description =
          """
          Unless OFF, checks that a proto_library target explicitly declares all 
          directly used targets as dependencies.
          """.trimIndent(),
      )

// off, warn, error, strict or default
// default: "off"
    @JvmField
    @Suppress("unused")
    val strictPublicImports =
      BazelFlag<String>(
        name = "strict_public_imports",
        description =
          """
          Unless OFF, checks that a proto_library target explicitly declares all 
          targets used in 'import public' as exported.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val strictSystemIncludes =
      BazelFlag.boolean(
        name = "strict_system_includes",
        description =
          """
          If true, headers found through system include paths (-isystem) are also 
          required to be declared.
          """.trimIndent(),
      )

// a build target label
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val targetEnvironment =
      BazelFlag<String>(
        name = "target_environment",
        description =
          """
          Declares this build's target environment. Must be a label reference to an 
          "environment" rule. If specified, all top-level targets must be compatible 
          with this environment.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options that affect the signing outputs of a build:
// v1, v2, v1_v2 or v4
// default: "v1_v2"
    @JvmField
    @Suppress("unused")
    val apkSigningMethod =
      BazelFlag<String>(
        name = "apk_signing_method",
        description =
          """
          Implementation to use to sign APKs
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val deviceDebugEntitlements =
      BazelFlag.boolean(
        name = "device_debug_entitlements",
        description =
          """
          If set, and compilation mode is not 'opt', objc apps will include debug 
          entitlements when signing.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val iosSigningCertName =
      BazelFlag<String>(
        name = "ios_signing_cert_name",
        description =
          """
          Certificate name to use for iOS signing. If not set will fall back to 
          provisioning profile. May be the certificate's keychain identity preference 
          or (substring) of the certificate's common name, as per codesign's man page 
          (SIGNING IDENTITIES).
          """.trimIndent(),
      )

// unknown line:
// unknown line: This option affects semantics of the Starlark language or the build API accessible to BUILD files, .bzl files, or WORKSPACE files.:
// default: "true"
    @JvmField
    @Suppress("unused")
    val enableBzlmod =
      BazelFlag.boolean(
        name = "enable_bzlmod",
        description =
          """
          If true, enables the Bzlmod dependency management system, taking precedence 
          over WORKSPACE. See https://bazel.build/docs/bzlmod for more information.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val enableWorkspace =
      BazelFlag.boolean(
        name = "enable_workspace",
        description =
          """
          If true, enables the legacy WORKSPACE system for external dependencies. See 
          https://bazel.build/external/overview for more information.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalActionResourceSet =
      BazelFlag.boolean(
        name = "experimental_action_resource_set",
        description =
          """
          If set to true, ctx.actions.run() and ctx.actions.run_shell() accept a 
          resource_set parameter for local execution. Otherwise it will default to 
          250 MB for memory and 1 cpu.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalBzlVisibility =
      BazelFlag.boolean(
        name = "experimental_bzl_visibility",
        description =
          """
          If enabled, adds a `visibility()` function that .bzl files may call during 
          top-level evaluation to set their visibility for the purpose of load() 
          statements.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalCcSharedLibrary =
      BazelFlag.boolean(
        name = "experimental_cc_shared_library",
        description =
          """
          If set to true, rule attributes and Starlark API methods needed for the 
          rule cc_shared_library will be available
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalDisableExternalPackage =
      BazelFlag.boolean(
        name = "experimental_disable_external_package",
        description =
          """
          If set to true, the auto-generated //external package will not be available 
          anymore. Bazel will still be unable to parse the file 'external/BUILD', but 
          globs reaching into external/ from the unnamed package will work.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalEnableAndroidMigrationApis =
      BazelFlag.boolean(
        name = "experimental_enable_android_migration_apis",
        description =
          """
          If set to true, enables the APIs required to support the Android Starlark 
          migration.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalEnableSclDialect =
      BazelFlag.boolean(
        name = "experimental_enable_scl_dialect",
        description =
          """
          If set to true, .scl files may be used in load() statements.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalGoogleLegacyApi =
      BazelFlag.boolean(
        name = "experimental_google_legacy_api",
        description =
          """
          If set to true, exposes a number of experimental pieces of Starlark build 
          API pertaining to Google legacy code.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalIsolatedExtensionUsages =
      BazelFlag.boolean(
        name = "experimental_isolated_extension_usages",
        description =
          """
          If true, enables the <code>isolate</code> parameter in the <a href="https:
          //bazel.build/rules/lib/globals/module#use_extension"
          ><code>use_extension</code></a> function.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalJavaLibraryExport =
      BazelFlag.boolean(
        name = "experimental_java_library_export",
        description =
          """
          If enabled, experimental_java_library_export_do_not_use module is available.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalPlatformsApi =
      BazelFlag.boolean(
        name = "experimental_platforms_api",
        description =
          """
          If set to true, enables a number of platform-related Starlark APIs useful 
          for debugging.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRepoRemoteExec =
      BazelFlag.boolean(
        name = "experimental_repo_remote_exec",
        description =
          """
          If set to true, repository_rule gains some remote execution capabilities.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalSiblingRepositoryLayout =
      BazelFlag.boolean(
        name = "experimental_sibling_repository_layout",
        description =
          """
          If set to true, non-main repositories are planted as symlinks to the main 
          repository in the execution root. That is, all repositories are direct 
          children of the ${"$"}output_base/execution_root directory. This has the side 
          effect of freeing up ${"$"}output_base/execution_root/__main__/external for the 
          real top-level 'external' directory.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleAllowTagsPropagation =
      BazelFlag.boolean(
        name = "incompatible_allow_tags_propagation",
        description =
          """
          If set to true, tags will be propagated from a target to the actions' 
          execution requirements; otherwise tags are not propagated. See https:
          //github.com/bazelbuild/bazel/issues/8830 for details.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleAlwaysCheckDepsetElements =
      BazelFlag.boolean(
        name = "incompatible_always_check_depset_elements",
        description =
          """
          Check the validity of elements added to depsets, in all constructors. 
          Elements must be immutable, but historically the depset(direct=...) 
          constructor forgot to check. Use tuples instead of lists in depset 
          elements. See https://github.com/bazelbuild/bazel/issues/10313 for details.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleConfigSettingPrivateDefaultVisibility =
      BazelFlag.boolean(
        name = "incompatible_config_setting_private_default_visibility",
        description =
          """
          If incompatible_enforce_config_setting_visibility=false, this is a noop. 
          Else, if this flag is false, any config_setting without an explicit 
          visibility attribute is //visibility:public. If this flag is true, 
          config_setting follows the same visibility logic as all other rules. See 
          https://github.com/bazelbuild/bazel/issues/12933.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleDepsetForJavaOutputSourceJars =
      BazelFlag.boolean(
        name = "incompatible_depset_for_java_output_source_jars",
        description =
          """
          When true, Bazel no longer returns a list from java_info.java_output[0].
          source_jars but returns a depset instead.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleDepsetForLibrariesToLinkGetter =
      BazelFlag.boolean(
        name = "incompatible_depset_for_libraries_to_link_getter",
        description =
          """
          When true, Bazel no longer returns a list from linking_context.
          libraries_to_link but returns a depset instead.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleDisableObjcLibraryTransition =
      BazelFlag.boolean(
        name = "incompatible_disable_objc_library_transition",
        description =
          """
          Disable objc_library's custom transition and inherit from the top level 
          target instead
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDisableStarlarkHostTransitions =
      BazelFlag.boolean(
        name = "incompatible_disable_starlark_host_transitions",
        description =
          """
          If set to true, rule attributes cannot set 'cfg = "host"'. Rules should set 
          'cfg = "exec"' instead.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDisableTargetProviderFields =
      BazelFlag.boolean(
        name = "incompatible_disable_target_provider_fields",
        description =
          """
          If set to true, disable the ability to access providers on 'target' objects 
          via field syntax. Use provider-key syntax instead. For example, instead of 
          using `ctx.attr.dep.my_info` to access `my_info` from inside a rule 
          implementation function, use `ctx.attr.dep[MyInfo]`. See https://github.
          com/bazelbuild/bazel/issues/9014 for details.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDisallowEmptyGlob =
      BazelFlag.boolean(
        name = "incompatible_disallow_empty_glob",
        description =
          """
          If set to true, the default value of the `allow_empty` argument of glob() 
          is False.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleDisallowLegacyPyProvider =
      BazelFlag.boolean(
        name = "incompatible_disallow_legacy_py_provider",
        description =
          """
          No-op, will be removed soon.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDisallowSdkFrameworksAttributes =
      BazelFlag.boolean(
        name = "incompatible_disallow_sdk_frameworks_attributes",
        description =
          """
          If true, disallow sdk_frameworks and weak_sdk_frameworks attributes in 
          objc_library andobjc_import.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDisallowStructProviderSyntax =
      BazelFlag.boolean(
        name = "incompatible_disallow_struct_provider_syntax",
        description =
          """
          If set to true, rule implementation functions may not return a struct. They 
          must instead return a list of provider instances.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleEnableDeprecatedLabelApis =
      BazelFlag.boolean(
        name = "incompatible_enable_deprecated_label_apis",
        description =
          """
          If enabled, certain deprecated APIs (native.repository_name, Label.
          workspace_name, Label.relative) can be used.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleEnforceConfigSettingVisibility =
      BazelFlag.boolean(
        name = "incompatible_enforce_config_setting_visibility",
        description =
          """
          If true, enforce config_setting visibility restrictions. If false, every 
          config_setting is visible to every target. See https://github.
          com/bazelbuild/bazel/issues/12932.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleExistingRulesImmutableView =
      BazelFlag.boolean(
        name = "incompatible_existing_rules_immutable_view",
        description =
          """
          If set to true, native.existing_rule and native.existing_rules return 
          lightweight immutable view objects instead of mutable dicts.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleFailOnUnknownAttributes =
      BazelFlag.boolean(
        name = "incompatible_fail_on_unknown_attributes",
        description =
          """
          If enabled, targets that have unknown attributes set to None fail.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleFixPackageGroupReporootSyntax =
      BazelFlag.boolean(
        name = "incompatible_fix_package_group_reporoot_syntax",
        description =
          """
          In package_group's `packages` attribute, changes the meaning of the value 
          "//..." to refer to all packages in the current repository instead of all 
          packages in any repository. You can use the special value "public" in place 
          of "//..." to obtain the old behavior. This flag requires that --
          incompatible_package_group_has_public_syntax also be enabled.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleJavaCommonParameters =
      BazelFlag.boolean(
        name = "incompatible_java_common_parameters",
        description =
          """
          If set to true, the output_jar, and host_javabase parameters in 
          pack_sources and host_javabase in compile will all be removed.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleMergeFixedAndDefaultShellEnv =
      BazelFlag.boolean(
        name = "incompatible_merge_fixed_and_default_shell_env",
        description =
          """
          If enabled, actions registered with ctx.actions.run and ctx.actions.
          run_shell with both 'env' and 'use_default_shell_env = True' specified will 
          use an environment obtained from the default shell environment by 
          overriding with the values passed in to 'env'. If disabled, the value of 
          'env' is completely ignored in this case.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleNewActionsApi =
      BazelFlag.boolean(
        name = "incompatible_new_actions_api",
        description =
          """
          If set to true, the API to create actions is only available on `ctx.
          actions`, not on `ctx`.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleNoAttrLicense =
      BazelFlag.boolean(
        name = "incompatible_no_attr_license",
        description =
          """
          If set to true, disables the function `attr.license`.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleNoImplicitFileExport =
      BazelFlag.boolean(
        name = "incompatible_no_implicit_file_export",
        description =
          """
          If set, (used) source files are are package private unless exported 
          explicitly. See https://github.
          com/bazelbuild/proposals/blob/master/designs/2019-10-24-file-visibility.md
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleNoRuleOutputsParam =
      BazelFlag.boolean(
        name = "incompatible_no_rule_outputs_param",
        description =
          """
          If set to true, disables the `outputs` parameter of the `rule()` Starlark 
          function.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleObjcAlwayslinkByDefault =
      BazelFlag.boolean(
        name = "incompatible_objc_alwayslink_by_default",
        description =
          """
          If true, make the default value true for alwayslink attributes in 
          objc_library and objc_import.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleObjcProviderRemoveLinkingInfo =
      BazelFlag.boolean(
        name = "incompatible_objc_provider_remove_linking_info",
        description =
          """
          If set to true, the ObjcProvider's APIs for linking info will be removed.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatiblePackageGroupHasPublicSyntax =
      BazelFlag.boolean(
        name = "incompatible_package_group_has_public_syntax",
        description =
          """
          In package_group's `packages` attribute, allows writing "public" or 
          "private" to refer to all packages or no packages respectively.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatiblePythonDisallowNativeRules =
      BazelFlag.boolean(
        name = "incompatible_python_disallow_native_rules",
        description =
          """
          When true, an error occurs when using the builtin py_* rules; instead the 
          rule_python rules should be used. See https://github.
          com/bazelbuild/bazel/issues/17773 for more information and migration 
          instructions.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleRequireLinkerInputCcApi =
      BazelFlag.boolean(
        name = "incompatible_require_linker_input_cc_api",
        description =
          """
          If set to true, rule create_linking_context will require linker_inputs 
          instead of libraries_to_link. The old getters of linking_context will also 
          be disabled and just linker_inputs will be available.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleRunShellCommandString =
      BazelFlag.boolean(
        name = "incompatible_run_shell_command_string",
        description =
          """
          If set to true, the command parameter of actions.run_shell will only accept 
          string
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleStopExportingLanguageModules =
      BazelFlag.boolean(
        name = "incompatible_stop_exporting_language_modules",
        description =
          """
          If enabled, certain language-specific modules (such as `cc_common`) are 
          unavailable in user .bzl files and may only be called from their respective 
          rules repositories.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleStructHasNoMethods =
      BazelFlag.boolean(
        name = "incompatible_struct_has_no_methods",
        description =
          """
          Disables the to_json and to_proto methods of struct, which pollute the 
          struct field namespace. Instead, use json.encode or json.encode_indent for 
          JSON, or proto.encode_text for textproto.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleTopLevelAspectsRequireProviders =
      BazelFlag.boolean(
        name = "incompatible_top_level_aspects_require_providers",
        description =
          """
          If set to true, the top level aspect will honor its required providers and 
          only run on top level targets whose rules' advertised providers satisfy the 
          required providers of the aspect.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleUnambiguousLabelStringification =
      BazelFlag.boolean(
        name = "incompatible_unambiguous_label_stringification",
        description =
          """
          When true, Bazel will stringify the label @//foo:bar to @//foo:bar, instead 
          of //foo:bar. This only affects the behavior of str(), the % operator, and 
          so on; the behavior of repr() is unchanged. See https://github.
          com/bazelbuild/bazel/issues/15916 for more information.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleUseCcConfigureFromRulesCc =
      BazelFlag.boolean(
        name = "incompatible_use_cc_configure_from_rules_cc",
        description =
          """
          When true, Bazel will no longer allow using cc_configure from @bazel_tools. 
          Please see https://github.com/bazelbuild/bazel/issues/10134 for details and 
          migration instructions.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleUsePlusInRepoNames =
      BazelFlag.boolean(
        name = "incompatible_use_plus_in_repo_names",
        description =
          """
          If true, uses the plus sign (+) as the separator in canonical repo names, 
          instead of the tilde (~). This is to address severe performance issues on 
          Windows; see https://github.com/bazelbuild/bazel/issues/22865 for more 
          information.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleVisibilityPrivateAttributesAtDefinition =
      BazelFlag.boolean(
        name = "incompatible_visibility_private_attributes_at_definition",
        description =
          """
          If set to true, the visibility of private rule attributes is checked with 
          respect to the rule definition, falling back to rule usage if not visible.
          """.trimIndent(),
      )

// a long integer
// default: "0"
    @JvmField
    @Suppress("unused")
    val maxComputationSteps =
      BazelFlag<String>(
        name = "max_computation_steps",
        description =
          """
          The maximum number of Starlark computation steps that may be executed by a 
          BUILD file (zero means no limit).
          """.trimIndent(),
      )

// an integer
// default: "3500"
    @JvmField
    @Suppress("unused")
    val nestedSetDepthLimit =
      BazelFlag<String>(
        name = "nested_set_depth_limit",
        description =
          """
          The maximum depth of the graph internal to a depset (also known as 
          NestedSet), above which the depset() constructor will fail.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options that govern the behavior of the test environment or test runner:
// default: "false"
    @JvmField
    @Suppress("unused")
    val allowAnalysisFailures =
      BazelFlag.boolean(
        name = "allow_analysis_failures",
        description =
          """
          If true, an analysis failure of a rule target results in the target's 
          propagation of an instance of AnalysisFailureInfo containing the error 
          description, instead of resulting in a build failure.
          """.trimIndent(),
      )

// an integer
// default: "2000"
    @JvmField
    @Suppress("unused")
    val analysisTestingDepsLimit =
      BazelFlag<String>(
        name = "analysis_testing_deps_limit",
        description =
          """
          Sets the maximum number of transitive dependencies through a rule attribute 
          with a for_analysis_testing configuration transition. Exceeding this limit 
          will result in a rule error.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val breakBuildOnParallelDex2oatFailure =
      BazelFlag.boolean(
        name = "break_build_on_parallel_dex2oat_failure",
        description =
          """
          If true dex2oat action failures will cause the build to break instead of 
          executing dex2oat during test runtime.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val checkTestsUpToDate =
      BazelFlag.boolean(
        name = "check_tests_up_to_date",
        description =
          """
          Don't run tests, just check if they are up-to-date.  If all tests results 
          are up-to-date, the testing completes successfully.  If any test needs to 
          be built or executed, an error is reported and the testing fails.  This 
          option implies --check_up_to_date behavior.
            Using this option will also add: --check_up_to_date
          """.trimIndent(),
      )

// a resource name followed by equal and 1 float or 4 float, e.g memory=10,30,60,100
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val defaultTestResources =
      BazelFlag<String>(
        name = "default_test_resources",
        description =
          """
          Override the default resources amount for tests. The expected format is 
          <resource>=<value>. If a single positive number is specified as <value> it 
          will override the default resources for all test sizes. If 4 comma-
          separated numbers are specified, they will override the resource amount for 
          respectively the small, medium, large, enormous test sizes. Values can also 
          be HOST_RAM/HOST_CPU, optionally followed by [-|*]<float> (eg. 
          memory=HOST_RAM*.1,HOST_RAM*.2,HOST_RAM*.3,HOST_RAM*.4). The default test 
          resources specified by this flag are overridden by explicit resources 
          specified in tags.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalAndroidUseParallelDex2oat =
      BazelFlag.boolean(
        name = "experimental_android_use_parallel_dex2oat",
        description =
          """
          Use dex2oat in parallel to possibly speed up android_test.
          """.trimIndent(),
      )

// a positive integer, the string "default", or test_regex@attempts. This flag may be passed more than once
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val flakyTestAttempts =
      BazelFlag<String>(
        name = "flaky_test_attempts",
        description =
          """
          Each test will be retried up to the specified number of times in case of 
          any test failure. Tests that required more than one attempt to pass are 
          marked as 'FLAKY' in the test summary. Normally the value specified is just 
          an integer or the string 'default'. If an integer, then all tests will be 
          run up to N times. If 'default', then only a single test attempt will be 
          made for regular tests and three for tests marked explicitly as flaky by 
          their rule (flaky=1 attribute). Alternate syntax: 
          regex_filter@flaky_test_attempts. Where flaky_test_attempts is as above and 
          regex_filter stands for a list of include and exclude regular expression 
          patterns (Also see --runs_per_test). Example: --flaky_test_attempts=//foo/.
          *,-//foo/bar/.*@3 deflakes all tests in //foo/ except those under foo/bar 
          three times. This option can be passed multiple times. The most recently 
          passed argument that matches takes precedence. If nothing matches, behavior 
          is as if 'default' above.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val iosMemleaks =
      BazelFlag.boolean(
        name = "ios_memleaks",
        description =
          """
          Enable checking for memory leaks in ios_test targets.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val iosSimulatorDevice =
      BazelFlag<String>(
        name = "ios_simulator_device",
        description =
          """
          The device to simulate when running an iOS application in the simulator, e.
          g. 'iPhone 6'. You can get a list of devices by running 'xcrun simctl list 
          devicetypes' on the machine the simulator will be run on.
          """.trimIndent(),
      )

// a dotted version (for example '2.3' or '3.3alpha2.4')
// default: see description
    @JvmField
    @Suppress("unused")
    val iosSimulatorVersion =
      BazelFlag<String>(
        name = "ios_simulator_version",
        description =
          """
          The version of iOS to run on the simulator when running or testing. This is 
          ignored for ios_test rules if a target device is specified in the rule.
          """.trimIndent(),
      )

// an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
// default: "auto"
    @JvmField
    @Suppress("unused")
    val localTestJobs =
      BazelFlag<String>(
        name = "local_test_jobs",
        description =
          """
          The max number of local test jobs to run concurrently. Takes an integer, or 
          a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an 
          operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5". 0 means local 
          resources will limit the number of local test jobs to run concurrently 
          instead. Setting this greater than the value for --jobs is ineffectual.
          """.trimIndent(),
      )

// a positive integer or test_regex@runs. This flag may be passed more than once
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val runsPerTest =
      BazelFlag<String>(
        name = "runs_per_test",
        description =
          """
          Specifies number of times to run each test. If any of those attempts fail 
          for any reason, the whole test is considered failed. Normally the value 
          specified is just an integer. Example: --runs_per_test=3 will run all tests 
          3 times. Alternate syntax: regex_filter@runs_per_test. Where runs_per_test 
          stands for an integer value and regex_filter stands for a list of include 
          and exclude regular expression patterns (Also see --
          instrumentation_filter). Example: --runs_per_test=//foo/.*,-//foo/bar/.*@3 
          runs all tests in //foo/ except those under foo/bar three times. This 
          option can be passed multiple times. The most recently passed argument that 
          matches takes precedence. If nothing matches, the test is only run once.
          """.trimIndent(),
      )

// a 'name=value' assignment with an optional value part
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val testEnv =
      BazelFlag<String>(
        name = "test_env",
        description =
          """
          Specifies additional environment variables to be injected into the test 
          runner environment. Variables can be either specified by name, in which 
          case its value will be read from the Bazel client environment, or by the 
          name=value pair. This option can be used multiple times to specify several 
          variables. Used only by the 'bazel test' command.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val testKeepGoing =
      BazelFlag.boolean(
        name = "test_keep_going",
        description =
          """
          When disabled, any non-passing test will cause the entire build to stop. By 
          default all tests are run, even if some do not pass.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val testStrategy =
      BazelFlag<String>(
        name = "test_strategy",
        description =
          """
          Specifies which strategy to use when running tests.
          """.trimIndent(),
      )

// a single integer or comma-separated list of 4 integers
// default: "-1"
    @JvmField
    @Suppress("unused")
    val testTimeout =
      BazelFlag<String>(
        name = "test_timeout",
        description =
          """
          Override the default test timeout values for test timeouts (in secs). If a 
          single positive integer value is specified it will override all 
          categories.  If 4 comma-separated integers are specified, they will 
          override the timeouts for short, moderate, long and eternal (in that 
          order). In either form, a value of -1 tells blaze to use its default 
          timeouts for that category.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val testTmpdir =
      BazelFlag<String>(
        name = "test_tmpdir",
        description =
          """
          Specifies the base temporary directory for 'bazel test' to use.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val zipUndeclaredTestOutputs =
      BazelFlag.boolean(
        name = "zip_undeclared_test_outputs",
        description =
          """
          If true, undeclared test outputs will be archived in a zip file.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options relating to query output and semantics:
// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalParallelAqueryOutput =
      BazelFlag.boolean(
        name = "experimental_parallel_aquery_output",
        description =
          """
          No-op.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options relating to Bzlmod output and semantics:
// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val allowYankedVersions =
      BazelFlag<String>(
        name = "allow_yanked_versions",
        description =
          """
          Specified the module versions in the form of `<module1>@<version1>,
          <module2>@<version2>` that will be allowed in the resolved dependency graph 
          even if they are declared yanked in the registry where they come from (if 
          they are not coming from a NonRegistryOverride). Otherwise, yanked versions 
          will cause the resolution to fail. You can also define allowed yanked 
          version with the `BZLMOD_ALLOW_YANKED_VERSIONS` environment variable. You 
          can disable this check by using the keyword 'all' (not recommended).
          """.trimIndent(),
      )

// error, warning or off
// default: "error"
    @JvmField
    @Suppress("unused")
    val checkBazelCompatibility =
      BazelFlag<String>(
        name = "check_bazel_compatibility",
        description =
          """
          Check bazel version compatibility of Bazel modules. Valid values are 
          `error` to escalate it to a resolution failure, `off` to disable the check, 
          or `warning` to print a warning when mismatch detected.
          """.trimIndent(),
      )

// off, warning or error
// default: "warning"
    @JvmField
    @Suppress("unused")
    val checkDirectDependencies =
      BazelFlag<String>(
        name = "check_direct_dependencies",
        description =
          """
          Check if the direct `bazel_dep` dependencies declared in the root module 
          are the same versions you get in the resolved dependency graph. Valid 
          values are `off` to disable the check, `warning` to print a warning when 
          mismatch detected or `error` to escalate it to a resolution failure.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val ignoreDevDependency =
      BazelFlag.boolean(
        name = "ignore_dev_dependency",
        description =
          """
          If true, Bazel ignores `bazel_dep` and `use_extension` declared as 
          `dev_dependency` in the MODULE.bazel of the root module. Note that, those 
          dev dependencies are always ignored in the MODULE.bazel if it's not the 
          root module regardless of the value of this flag.
          """.trimIndent(),
      )

// off, update, refresh or error
// default: "update"
    @JvmField
    @Suppress("unused")
    val lockfileMode =
      BazelFlag<String>(
        name = "lockfile_mode",
        description =
          """
          Specifies how and whether or not to use the lockfile. Valid values are 
          `update` to use the lockfile and update it if there are changes, `refresh` 
          to additionally refresh mutable information (yanked versions and previously 
          missing modules) from remote registries from time to time, `error` to use 
          the lockfile but throw an error if it's not up-to-date, or `off` to neither 
          read from or write to the lockfile.
          """.trimIndent(),
      )

// an equals-separated mapping of module name to path
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val overrideModule =
      BazelFlag<String>(
        name = "override_module",
        description =
          """
          Override a module with a local path in the form of <module name>=<path>. If 
          the given path is an absolute path, it will be used as it is. If the given 
          path is a relative path, it is relative to the current working directory. 
          If the given path starts with '%workspace%, it is relative to the workspace 
          root, which is the output of `bazel info workspace`. If the given path is 
          empty, then remove any previous overrides.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val registry =
      BazelFlag<String>(
        name = "registry",
        description =
          """
          Specifies the registries to use to locate Bazel module dependencies. The 
          order is important: modules will be looked up in earlier registries first, 
          and only fall back to later registries when they're missing from the 
          earlier ones.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val vendorDir =
      BazelFlag<String>(
        name = "vendor_dir",
        description =
          """
          Specifies the directory that should hold the external repositories in 
          vendor mode, whether for the purpose of fetching them into it or using them 
          while building. The path can be specified as either an absolute path or a 
          path relative to the workspace directory.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options that trigger optimizations of the build time:
// a long integer
// default: "50000"
    @JvmField
    @Suppress("unused")
    val cacheComputedFileDigests =
      BazelFlag<String>(
        name = "cache_computed_file_digests",
        description =
          """
          If greater than 0, configures Bazel to cache file digests in memory based 
          on their metadata instead of recomputing the digests from disk every time 
          they are needed. Setting this to 0 ensures correctness because not all file 
          changes can be noted from file metadata. When not 0, the number indicates 
          the size of the cache as the number of file digests to be cached.
          """.trimIndent(),
      )

// a comma-separated list of signal numbers
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalDynamicIgnoreLocalSignals =
      BazelFlag<String>(
        name = "experimental_dynamic_ignore_local_signals",
        description =
          """
          Takes a list of OS signal numbers. If a local branch of dynamic execution 
          gets killed with any of these signals, the remote branch will be allowed to 
          finish instead. For persistent workers, this only affects signals that kill 
          the worker process.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalFilterLibraryJarWithProgramJar =
      BazelFlag.boolean(
        name = "experimental_filter_library_jar_with_program_jar",
        description =
          """
          Filter the ProGuard ProgramJar to remove any classes also present in the 
          LibraryJar.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalInmemoryDotdFiles =
      BazelFlag.boolean(
        name = "experimental_inmemory_dotd_files",
        description =
          """
          If enabled, C++ .d files will be passed through in memory directly from the 
          remote build nodes instead of being written to disk.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalInmemoryJdepsFiles =
      BazelFlag.boolean(
        name = "experimental_inmemory_jdeps_files",
        description =
          """
          If enabled, the dependency (.jdeps) files generated from Java compilations 
          will be passed through in memory directly from the remote build nodes 
          instead of being written to disk.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalObjcIncludeScanning =
      BazelFlag.boolean(
        name = "experimental_objc_include_scanning",
        description =
          """
          Whether to perform include scanning for objective C/C++.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRetainTestConfigurationAcrossTestonly =
      BazelFlag.boolean(
        name = "experimental_retain_test_configuration_across_testonly",
        description =
          """
          When enabled, --trim_test_configuration will not trim the test 
          configuration for rules marked testonly=1. This is meant to reduce action 
          conflict issues when non-test rules depend on cc_test rules. No effect if --
          trim_test_configuration is false.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalStarlarkCcImport =
      BazelFlag.boolean(
        name = "experimental_starlark_cc_import",
        description =
          """
          If enabled, the Starlark version of cc_import can be used.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalUnsupportedAndBrittleIncludeScanning =
      BazelFlag.boolean(
        name = "experimental_unsupported_and_brittle_include_scanning",
        description =
          """
          Whether to narrow inputs to C/C++ compilation by parsing #include lines 
          from input files. This can improve performance and incrementality by 
          decreasing the size of compilation input trees. However, it can also break 
          builds because the include scanner does not fully implement C preprocessor 
          semantics. In particular, it does not understand dynamic #include 
          directives and ignores preprocessor conditional logic. Use at your own 
          risk. Any issues relating to this flag that are filed will be closed.
          """.trimIndent(),
      )

// comma separated pairs of <period>:<count>
// default: "1s:2,20s:3,1m:5"
    @JvmField
    @Suppress("unused")
    val gcThrashingLimits =
      BazelFlag<String>(
        name = "gc_thrashing_limits",
        description =
          """
          Limits which, if reached, cause GcThrashingDetector to crash Bazel with an 
          OOM. Each limit is specified as <period>:<count> where period is a duration 
          and count is a positive integer. If more than --gc_thrashing_threshold 
          percent of tenured space (old gen heap) remains occupied after <count> 
          consecutive full GCs within <period>, an OOM is triggered. Multiple limits 
          can be specified separated by commas.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val heuristicallyDropNodes =
      BazelFlag.boolean(
        name = "heuristically_drop_nodes",
        description =
          """
          If true, Blaze will remove FileState and DirectoryListingState nodes after 
          related File and DirectoryListing node is done to save memory. We expect 
          that it is less likely that these nodes will be needed again. If so, the 
          program will re-evaluate them.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleDoNotSplitLinkingCmdline =
      BazelFlag.boolean(
        name = "incompatible_do_not_split_linking_cmdline",
        description =
          """
          When true, Bazel no longer modifies command line flags used for linking, 
          and also doesn't selectively decide which flags go to the param file and 
          which don't.  See https://github.com/bazelbuild/bazel/issues/7670 for 
          details.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incrementalDexing =
      BazelFlag.boolean(
        name = "incremental_dexing",
        description =
          """
          Does most of the work for dexing separately for each Jar file.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val keepStateAfterBuild =
      BazelFlag.boolean(
        name = "keep_state_after_build",
        description =
          """
          If false, Blaze will discard the inmemory state from this build when the 
          build finishes. Subsequent builds will not have any incrementality with 
          respect to this one.
          """.trimIndent(),
      )

// an integer, or "HOST_CPUS", optionally followed by [-|*]<float>.
// default: "HOST_CPUS"
    @JvmField
    @Suppress("unused")
    val localCpuResources =
      BazelFlag<String>(
        name = "local_cpu_resources",
        description =
          """
          Explicitly set the total number of local CPU cores available to Bazel to 
          spend on build actions executed locally. Takes an integer, or "HOST_CPUS", 
          optionally followed by [-|*]<float> (eg. HOST_CPUS*.5 to use half the 
          available CPU cores). By default, ("HOST_CPUS"), Bazel will query system 
          configuration to estimate the number of CPU cores available.
          """.trimIndent(),
      )

// a named float, 'name=value'
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val localExtraResources =
      BazelFlag<String>(
        name = "local_extra_resources",
        description =
          """
          Set the number of extra resources available to Bazel. Takes in a string-
          float pair. Can be used multiple times to specify multiple types of extra 
          resources. Bazel will limit concurrently running actions based on the 
          available extra resources and the extra resources required. Tests can 
          declare the amount of extra resources they need by using a tag of the 
          "resources:<resoucename>:<amount>" format. Available CPU, RAM and resources 
          cannot be set with this flag.
          """.trimIndent(),
      )

// an integer number of MBs, or "HOST_RAM", optionally followed by [-|*]<float>.
// default: "HOST_RAM*.67"
    @JvmField
    @Suppress("unused")
    val localRamResources =
      BazelFlag<String>(
        name = "local_ram_resources",
        description =
          """
          Explicitly set the total amount of local host RAM (in MB) available to 
          Bazel to spend on build actions executed locally. Takes an integer, or 
          "HOST_RAM", optionally followed by [-|*]<float> (eg. HOST_RAM*.5 to use 
          half the available RAM). By default, ("HOST_RAM*.67"), Bazel will query 
          system configuration to estimate the amount of RAM available and will use 
          67% of it.
          """.trimIndent(),
      )

// a named double, 'name=value', where value is an integer, or a keyword ("auto", "HOST_CPUS", "HOST_RAM"), optionally followed by an operation ([-|*]<float>) eg. "auto", "HOST_CPUS*.5"
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val localResources =
      BazelFlag<String>(
        name = "local_resources",
        description =
          """
          Set the number of resources available to Bazel. Takes in an assignment to a 
          float or HOST_RAM/HOST_CPUS, optionally followed by [-|*]<float> (eg. 
          memory=HOST_RAM*.5 to use half the available RAM). Can be used multiple 
          times to specify multiple types of resources. Bazel will limit concurrently 
          running actions based on the available resources and the resources 
          required. Tests can declare the amount of resources they need by using a 
          tag of the "resources:<resource name>:<amount>" format. Overrides resources 
          specified by --local_{cpu|ram|extra}_resources.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val objcUseDotdPruning =
      BazelFlag.boolean(
        name = "objc_use_dotd_pruning",
        description =
          """
          If set, .d files emitted by clang will be used to prune the set of inputs 
          passed into objc compiles.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val processHeadersInDependencies =
      BazelFlag.boolean(
        name = "process_headers_in_dependencies",
        description =
          """
          When building a target //a:a, process headers in all targets that //a:a 
          depends on (if header processing is enabled for the toolchain).
          """.trimIndent(),
      )

// an integer, >= 0
// default: "2147483647"
    @JvmField
    @Suppress("unused")
    val skyframeHighWaterMarkFullGcDropsPerInvocation =
      BazelFlag<String>(
        name = "skyframe_high_water_mark_full_gc_drops_per_invocation",
        description =
          """
          Flag for advanced configuration of Bazel's internal Skyframe engine. If 
          Bazel detects its retained heap percentage usage exceeds the threshold set 
          by --skyframe_high_water_mark_threshold, when a full GC event occurs, it 
          will drop unnecessary temporary Skyframe state, up to this many times per 
          invocation. Defaults to Integer.MAX_VALUE; effectively unlimited. Zero 
          means that full GC events will never trigger drops. If the limit is 
          reached, Skyframe state will no longer be dropped when a full GC event 
          occurs and that retained heap percentage threshold is exceeded.
          """.trimIndent(),
      )

// an integer, >= 0
// default: "2147483647"
    @JvmField
    @Suppress("unused")
    val skyframeHighWaterMarkMinorGcDropsPerInvocation =
      BazelFlag<String>(
        name = "skyframe_high_water_mark_minor_gc_drops_per_invocation",
        description =
          """
          Flag for advanced configuration of Bazel's internal Skyframe engine. If 
          Bazel detects its retained heap percentage usage exceeds the threshold set 
          by --skyframe_high_water_mark_threshold, when a minor GC event occurs, it 
          will drop unnecessary temporary Skyframe state, up to this many times per 
          invocation. Defaults to Integer.MAX_VALUE; effectively unlimited. Zero 
          means that minor GC events will never trigger drops. If the limit is 
          reached, Skyframe state will no longer be dropped when a minor GC event 
          occurs and that retained heap percentage threshold is exceeded.
          """.trimIndent(),
      )

// an integer
// default: "85"
    @JvmField
    @Suppress("unused")
    val skyframeHighWaterMarkThreshold =
      BazelFlag<String>(
        name = "skyframe_high_water_mark_threshold",
        description =
          """
          Flag for advanced configuration of Bazel's internal Skyframe engine. If 
          Bazel detects its retained heap percentage usage is at least this 
          threshold, it will drop unnecessary temporary Skyframe state. Tweaking this 
          may let you mitigate wall time impact of GC thrashing, when the GC 
          thrashing is (i) caused by the memory usage of this temporary state and 
          (ii) more costly than reconstituting the state when it is needed.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val trackIncrementalState =
      BazelFlag.boolean(
        name = "track_incremental_state",
        description =
          """
          If false, Blaze will not persist data that allows for invalidation and re-
          evaluation on incremental builds in order to save memory on this build. 
          Subsequent builds will not have any incrementality with respect to this 
          one. Usually you will want to specify --batch when setting this to false.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val trimTestConfiguration =
      BazelFlag.boolean(
        name = "trim_test_configuration",
        description =
          """
          When enabled, test-related options will be cleared below the top level of 
          the build. When this flag is active, tests cannot be built as dependencies 
          of non-test rules, but changes to test-related options will not cause non-
          test rules to be re-analyzed.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options that affect the verbosity, format or location of logging:
// default: "false"
    @JvmField
    @Suppress("unused")
    val announceRc =
      BazelFlag.boolean(
        name = "announce_rc",
        description =
          """
          Whether to announce rc options.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val attemptToPrintRelativePaths =
      BazelFlag.boolean(
        name = "attempt_to_print_relative_paths",
        description =
          """
          When printing the location part of messages, attempt to use a path relative 
          to the workspace directory or one of the directories specified by --
          package_path.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val besBackend =
      BazelFlag<String>(
        name = "bes_backend",
        description =
          """
          Specifies the build event service (BES) backend endpoint in the form 
          [SCHEME://]HOST[:PORT]. The default is to disable BES uploads. Supported 
          schemes are grpc and grpcs (grpc with TLS enabled). If no scheme is 
          provided, Bazel assumes grpcs.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val besCheckPrecedingLifecycleEvents =
      BazelFlag.boolean(
        name = "bes_check_preceding_lifecycle_events",
        description =
          """
          Sets the field check_preceding_lifecycle_events_present on 
          PublishBuildToolEventStreamRequest which tells BES to check whether it 
          previously received InvocationAttemptStarted and BuildEnqueued events 
          matching the current tool event.
          """.trimIndent(),
      )

// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val besHeader =
      BazelFlag<String>(
        name = "bes_header",
        description =
          """
          Specify a header in NAME=VALUE form that will be included in BES requests. 
          Multiple headers can be passed by specifying the flag multiple times. 
          Multiple values for the same name will be converted to a comma-separated 
          list.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val besInstanceName =
      BazelFlag<String>(
        name = "bes_instance_name",
        description =
          """
          Specifies the instance name under which the BES will persist uploaded BEP. 
          Defaults to null.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val besKeywords =
      BazelFlag<String>(
        name = "bes_keywords",
        description =
          """
          Specifies a list of notification keywords to be added the default set of 
          keywords published to BES ("command_name=<command_name> ", 
          "protocol_name=BEP"). Defaults to none.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val besLifecycleEvents =
      BazelFlag.boolean(
        name = "bes_lifecycle_events",
        description =
          """
          Specifies whether to publish BES lifecycle events. (defaults to 'true').
          """.trimIndent(),
      )

// An immutable length of time.
// default: "10m"
    @JvmField
    @Suppress("unused")
    val besOomFinishUploadTimeout =
      BazelFlag<String>(
        name = "bes_oom_finish_upload_timeout",
        description =
          """
          Specifies how long bazel should wait for the BES/BEP upload to complete 
          while OOMing. This flag ensures termination when the JVM is severely GC 
          thrashing and cannot make progress on any user thread.
          """.trimIndent(),
      )

// an integer
// default: "10240"
    @JvmField
    @Suppress("unused")
    val besOuterrBufferSize =
      BazelFlag<String>(
        name = "bes_outerr_buffer_size",
        description =
          """
          Specifies the maximal size of stdout or stderr to be buffered in BEP, 
          before it is reported as a progress event. Individual writes are still 
          reported in a single event, even if larger than the specified value up to --
          bes_outerr_chunk_size.
          """.trimIndent(),
      )

// an integer
// default: "1048576"
    @JvmField
    @Suppress("unused")
    val besOuterrChunkSize =
      BazelFlag<String>(
        name = "bes_outerr_chunk_size",
        description =
          """
          Specifies the maximal size of stdout or stderr to be sent to BEP in a 
          single message.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val besProxy =
      BazelFlag<String>(
        name = "bes_proxy",
        description =
          """
          Connect to the Build Event Service through a proxy. Currently this flag can 
          only be used to configure a Unix domain socket (unix:/path/to/socket).
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val besResultsUrl =
      BazelFlag<String>(
        name = "bes_results_url",
        description =
          """
          Specifies the base URL where a user can view the information streamed to 
          the BES backend. Bazel will output the URL appended by the invocation id to 
          the terminal.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val besSystemKeywords =
      BazelFlag<String>(
        name = "bes_system_keywords",
        description =
          """
          Specifies a list of notification keywords to be included directly, without 
          the "user_keyword=" prefix included for keywords supplied via --
          bes_keywords. Intended for Build service operators that set --
          bes_lifecycle_events=false and include keywords when calling 
          PublishLifecycleEvent. Build service operators using this flag should 
          prevent users from overriding the flag value.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "0s"
    @JvmField
    @Suppress("unused")
    val besTimeout =
      BazelFlag<String>(
        name = "bes_timeout",
        description =
          """
          Specifies how long bazel should wait for the BES/BEP upload to complete 
          after the build and tests have finished. A valid timeout is a natural 
          number followed by a unit: Days (d), hours (h), minutes (m), seconds (s), 
          and milliseconds (ms). The default value is '0' which means that there is 
          no timeout.
          """.trimIndent(),
      )

// wait_for_upload_complete, nowait_for_upload_complete or fully_async
// default: "wait_for_upload_complete"
    @JvmField
    @Suppress("unused")
    val besUploadMode =
      BazelFlag<String>(
        name = "bes_upload_mode",
        description =
          """
          Specifies whether the Build Event Service upload should block the build 
          completion or should end the invocation immediately and finish the upload 
          in the background. Either 'wait_for_upload_complete' (default), 
          'nowait_for_upload_complete', or 'fully_async'.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val buildEventBinaryFile =
      BazelFlag<String>(
        name = "build_event_binary_file",
        description =
          """
          If non-empty, write a varint delimited binary representation of 
          representation of the build event protocol to that file. This option 
          implies --bes_upload_mode=wait_for_upload_complete.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val buildEventBinaryFilePathConversion =
      BazelFlag.boolean(
        name = "build_event_binary_file_path_conversion",
        description =
          """
          Convert paths in the binary file representation of the build event protocol 
          to more globally valid URIs whenever possible; if disabled, the file:// uri 
          scheme will always be used
          """.trimIndent(),
      )

// wait_for_upload_complete, nowait_for_upload_complete or fully_async
// default: "wait_for_upload_complete"
    @JvmField
    @Suppress("unused")
    val buildEventBinaryFileUploadMode =
      BazelFlag<String>(
        name = "build_event_binary_file_upload_mode",
        description =
          """
          Specifies whether the Build Event Service upload for --
          build_event_binary_file should block the build completion or should end the 
          invocation immediately and finish the upload in the background. Either 
          'wait_for_upload_complete' (default), 'nowait_for_upload_complete', or 
          'fully_async'.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val buildEventJsonFile =
      BazelFlag<String>(
        name = "build_event_json_file",
        description =
          """
          If non-empty, write a JSON serialisation of the build event protocol to 
          that file. This option implies --bes_upload_mode=wait_for_upload_complete.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val buildEventJsonFilePathConversion =
      BazelFlag.boolean(
        name = "build_event_json_file_path_conversion",
        description =
          """
          Convert paths in the json file representation of the build event protocol 
          to more globally valid URIs whenever possible; if disabled, the file:// uri 
          scheme will always be used
          """.trimIndent(),
      )

// wait_for_upload_complete, nowait_for_upload_complete or fully_async
// default: "wait_for_upload_complete"
    @JvmField
    @Suppress("unused")
    val buildEventJsonFileUploadMode =
      BazelFlag<String>(
        name = "build_event_json_file_upload_mode",
        description =
          """
          Specifies whether the Build Event Service upload for --
          build_event_json_file should block the build completion or should end the 
          invocation immediately and finish the upload in the background. Either 
          'wait_for_upload_complete' (default), 'nowait_for_upload_complete', or 
          'fully_async'.
          """.trimIndent(),
      )

// an integer
// default: "-1"
    @JvmField
    @Suppress("unused")
    val buildEventMaxNamedSetOfFileEntries =
      BazelFlag<String>(
        name = "build_event_max_named_set_of_file_entries",
        description =
          """
          The maximum number of entries for a single named_set_of_files event; values 
          smaller than 2 are ignored and no event splitting is performed. This is 
          intended for limiting the maximum event size in the build event protocol, 
          although it does not directly control event size. The total event size is a 
          function of the structure of the set as well as the file and uri lengths, 
          which may in turn depend on the hash function.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val buildEventPublishAllActions =
      BazelFlag.boolean(
        name = "build_event_publish_all_actions",
        description =
          """
          Whether all actions should be published.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val buildEventTextFile =
      BazelFlag<String>(
        name = "build_event_text_file",
        description =
          """
          If non-empty, write a textual representation of the build event protocol to 
          that file
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val buildEventTextFilePathConversion =
      BazelFlag.boolean(
        name = "build_event_text_file_path_conversion",
        description =
          """
          Convert paths in the text file representation of the build event protocol 
          to more globally valid URIs whenever possible; if disabled, the file:// uri 
          scheme will always be used
          """.trimIndent(),
      )

// wait_for_upload_complete, nowait_for_upload_complete or fully_async
// default: "wait_for_upload_complete"
    @JvmField
    @Suppress("unused")
    val buildEventTextFileUploadMode =
      BazelFlag<String>(
        name = "build_event_text_file_upload_mode",
        description =
          """
          Specifies whether the Build Event Service upload for --
          build_event_text_file should block the build completion or should end the 
          invocation immediately and finish the upload in the background. Either 
          'wait_for_upload_complete' (default), 'nowait_for_upload_complete', or 
          'fully_async'.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val debugSpawnScheduler =
      BazelFlag.boolean(
        name = "debug_spawn_scheduler",
        description =
          """
    
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalAnnounceProfilePath =
      BazelFlag.boolean(
        name = "experimental_announce_profile_path",
        description =
          """
          If enabled, adds the JSON profile path to the log.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalBepTargetSummary =
      BazelFlag.boolean(
        name = "experimental_bep_target_summary",
        description =
          """
          Whether to publish TargetSummary events.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalBuildEventExpandFilesets =
      BazelFlag.boolean(
        name = "experimental_build_event_expand_filesets",
        description =
          """
          If true, expand Filesets in the BEP when presenting output files.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalBuildEventFullyResolveFilesetSymlinks =
      BazelFlag.boolean(
        name = "experimental_build_event_fully_resolve_fileset_symlinks",
        description =
          """
          If true, fully resolve relative Fileset symlinks in the BEP when presenting 
          output files. Requires --experimental_build_event_expand_filesets.
          """.trimIndent(),
      )

// an integer
// default: "4"
    @JvmField
    @Suppress("unused")
    val experimentalBuildEventUploadMaxRetries =
      BazelFlag<String>(
        name = "experimental_build_event_upload_max_retries",
        description =
          """
          The maximum number of times Bazel should retry uploading a build event.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "1s"
    @JvmField
    @Suppress("unused")
    val experimentalBuildEventUploadRetryMinimumDelay =
      BazelFlag<String>(
        name = "experimental_build_event_upload_retry_minimum_delay",
        description =
          """
          Initial, minimum delay for exponential backoff retries when BEP upload 
          fails. (exponent: 1.6)
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalBuildEventUploadStrategy =
      BazelFlag<String>(
        name = "experimental_build_event_upload_strategy",
        description =
          """
          Selects how to upload artifacts referenced in the build event protocol.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalCollectLoadAverageInProfiler =
      BazelFlag.boolean(
        name = "experimental_collect_load_average_in_profiler",
        description =
          """
          If enabled, the profiler collects the system's overall load average.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalCollectLocalSandboxActionMetrics =
      BazelFlag.boolean(
        name = "experimental_collect_local_sandbox_action_metrics",
        description =
          """
          Deprecated no-op.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalCollectPressureStallIndicators =
      BazelFlag.boolean(
        name = "experimental_collect_pressure_stall_indicators",
        description =
          """
          If enabled, the profiler collects the Linux PSI data.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalCollectResourceEstimation =
      BazelFlag.boolean(
        name = "experimental_collect_resource_estimation",
        description =
          """
          If enabled, the profiler collects CPU and memory usage estimation for local 
          actions.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalCollectSystemNetworkUsage =
      BazelFlag.boolean(
        name = "experimental_collect_system_network_usage",
        description =
          """
          If enabled, the profiler collects the system's network usage.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalCollectWorkerDataInProfiler =
      BazelFlag.boolean(
        name = "experimental_collect_worker_data_in_profiler",
        description =
          """
          If enabled, the profiler collects worker's aggregated resource data.
          """.trimIndent(),
      )

// cpu, wall, alloc or lock
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalCommandProfile =
      BazelFlag<String>(
        name = "experimental_command_profile",
        description =
          """
          Records a Java Flight Recorder profile for the duration of the command. One 
          of the supported profiling event types (cpu, wall, alloc or lock) must be 
          given as an argument. The profile is written to a file named after the 
          event type under the output base directory. The syntax and semantics of 
          this flag might change in the future to support additional profile types or 
          output formats; use at your own risk.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalDockerVerbose =
      BazelFlag.boolean(
        name = "experimental_docker_verbose",
        description =
          """
          If enabled, Bazel will print more verbose messages about the Docker sandbox 
          strategy.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalMaterializeParamFilesDirectly =
      BazelFlag.boolean(
        name = "experimental_materialize_param_files_directly",
        description =
          """
          If materializing param files, do so with direct writes to disk.
          """.trimIndent(),
      )

// phase, action, action_check, action_lock, action_release, action_update, action_complete, bzlmod, info, create_package, remote_execution, local_execution, scanner, local_parse, upload_time, remote_process_time, remote_queue, remote_setup, fetch, local_process_time, vfs_stat, vfs_dir, vfs_readlink, vfs_md5, vfs_xattr, vfs_delete, vfs_open, vfs_read, vfs_write, vfs_glob, vfs_vmfs_stat, vfs_vmfs_dir, vfs_vmfs_read, wait, thread_name, thread_sort_index, skyframe_eval, skyfunction, critical_path, critical_path_component, handle_gc_notification, action_counts, action_cache_counts, local_cpu_usage, system_cpu_usage, cpu_usage_estimation, local_memory_usage, system_memory_usage, memory_usage_estimation, system_network_up_usage, system_network_down_usage, workers_memory_usage, system_load_average, starlark_parser, starlark_user_fn, starlark_builtin_fn, starlark_user_compiled_fn, starlark_repository_fn, action_fs_staging, remote_cache_check, remote_download, remote_network, filesystem_traversal, worker_execution, worker_setup, worker_borrow, worker_working, worker_copying_outputs, credential_helper, pressure_stall_io, pressure_stall_memory, conflict_check, dynamic_lock, repository_fetch, repository_vendor or unknown
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val experimentalProfileAdditionalTasks =
      BazelFlag<String>(
        name = "experimental_profile_additional_tasks",
        description =
          """
          Specifies additional profile tasks to be included in the profile.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalProfileIncludePrimaryOutput =
      BazelFlag.boolean(
        name = "experimental_profile_include_primary_output",
        description =
          """
          Includes the extra "out" attribute in action events that contains the exec 
          path to the action's primary output.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalProfileIncludeTargetLabel =
      BazelFlag.boolean(
        name = "experimental_profile_include_target_label",
        description =
          """
          Includes target label in action events' JSON profile data.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRecordMetricsForAllMnemonics =
      BazelFlag.boolean(
        name = "experimental_record_metrics_for_all_mnemonics",
        description =
          """
          By default the number of action types is limited to the 20 mnemonics with 
          the largest number of executed actions. Setting this option will write 
          statistics for all mnemonics.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val experimentalRepositoryResolvedFile =
      BazelFlag<String>(
        name = "experimental_repository_resolved_file",
        description =
          """
          If non-empty, write a Starlark value with the resolved information of all 
          Starlark repository rules that were executed.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRunBepEventIncludeResidue =
      BazelFlag.boolean(
        name = "experimental_run_bep_event_include_residue",
        description =
          """
          Whether to include the command-line residue in run build events which could 
          contain the residue. By default, the residue is not included in run command 
          build events that could contain the residue.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalStreamLogFileUploads =
      BazelFlag.boolean(
        name = "experimental_stream_log_file_uploads",
        description =
          """
          Stream log file uploads directly to the remote storage rather than writing 
          them to disk.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalWorkspaceRulesLogFile =
      BazelFlag<String>(
        name = "experimental_workspace_rules_log_file",
        description =
          """
          Log certain Workspace Rules events into this file as delimited 
          WorkspaceEvent protos.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val explain =
      BazelFlag<String>(
        name = "explain",
        description =
          """
          Causes the build system to explain each executed step of the build. The 
          explanation is written to the specified log file.
          """.trimIndent(),
      )

// a tri-state (auto, yes, no)
// default: "auto"
    @JvmField
    @Suppress("unused")
    val generateJsonTraceProfile =
      BazelFlag<String>(
        name = "generate_json_trace_profile",
        description =
          """
          If enabled, Bazel profiles the build and writes a JSON-format profile into 
          a file in the output base. View profile by loading into chrome://tracing. 
          By default Bazel writes the profile for all build-like commands and query.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val heapDumpOnOom =
      BazelFlag.boolean(
        name = "heap_dump_on_oom",
        description =
          """
          Whether to manually output a heap dump if an OOM is thrown (including 
          manual OOMs due to reaching --gc_thrashing_limits). The dump will be 
          written to <output_base>/<invocation_id>.heapdump.hprof. This option 
          effectively replaces -XX:+HeapDumpOnOutOfMemoryError, which has no effect 
          for manual OOMs.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val ignoreUnsupportedSandboxing =
      BazelFlag.boolean(
        name = "ignore_unsupported_sandboxing",
        description =
          """
          Do not print a warning when sandboxed execution is not supported on this 
          system.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val legacyImportantOutputs =
      BazelFlag.boolean(
        name = "legacy_important_outputs",
        description =
          """
          Use this to suppress generation of the legacy important_outputs field in 
          the TargetComplete event. important_outputs are required for Bazel to 
          ResultStore integration.
          """.trimIndent(),
      )

// 0 <= an integer <= 6
// default: "3"
    @JvmField
    @Suppress("unused")
    val logging =
      BazelFlag<String>(
        name = "logging",
        description =
          """
          The logging level.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val materializeParamFiles =
      BazelFlag.boolean(
        name = "materialize_param_files",
        description =
          """
          Writes intermediate parameter files to output tree even when using remote 
          action execution. Useful when debugging actions. This is implied by --
          subcommands and --verbose_failures.
          """.trimIndent(),
      )

// an integer
// default: "3"
    @JvmField
    @Suppress("unused")
    val maxConfigChangesToShow =
      BazelFlag<String>(
        name = "max_config_changes_to_show",
        description =
          """
          When discarding the analysis cache due to a change in the build options, 
          displays up to the given number of changed option names. If the number 
          given is -1, all changed options will be displayed.
          """.trimIndent(),
      )

// an integer
// default: "-1"
    @JvmField
    @Suppress("unused")
    val maxTestOutputBytes =
      BazelFlag<String>(
        name = "max_test_output_bytes",
        description =
          """
          Specifies maximum per-test-log size that can be emitted when --test_output 
          is 'errors' or 'all'. Useful for avoiding overwhelming the output with 
          excessively noisy test output. The test header is included in the log size. 
          Negative values imply no limit. Output is all or nothing.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val memoryProfile =
      BazelFlag<String>(
        name = "memory_profile",
        description =
          """
          If set, write memory usage data to the specified file at phase ends and 
          stable heap to master log at end of build.
          """.trimIndent(),
      )

// integers, separated by a comma expected in pairs
// default: "1,0"
    @JvmField
    @Suppress("unused")
    val memoryProfileStableHeapParameters =
      BazelFlag<String>(
        name = "memory_profile_stable_heap_parameters",
        description =
          """
          Tune memory profile's computation of stable heap at end of build. Should be 
          and even number of  integers separated by commas. In each pair the first 
          integer is the number of GCs to perform. The second integer in each pair is 
          the number of seconds to wait between GCs. Ex: 2,4,4,0 would 2 GCs with a 
          4sec pause, followed by 4 GCs with zero second pause
          """.trimIndent(),
      )

// a valid Java regular expression
// default: see description
    @JvmField
    @Suppress("unused")
    val outputFilter =
      BazelFlag<String>(
        name = "output_filter",
        description =
          """
          Only shows warnings and action outputs for rules with a name matching the 
          provided regular expression.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val profile =
      BazelFlag<String>(
        name = "profile",
        description =
          """
          If set, profile Bazel and write data to the specified file. Use bazel 
          analyze-profile to analyze the profile.
          """.trimIndent(),
      )

// an integer in 0-3600 range
// default: "0"
    @JvmField
    @Suppress("unused")
    val progressReportInterval =
      BazelFlag<String>(
        name = "progress_report_interval",
        description =
          """
          The number of seconds to wait between reports on still running jobs. The 
          default value 0 means the first report will be printed after 10 seconds, 
          then 30 seconds and after that progress is reported once every minute. When 
          --curses is enabled, progress is reported every second.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val recordFullProfilerData =
      BazelFlag.boolean(
        name = "record_full_profiler_data",
        description =
          """
          By default, Bazel profiler will record only aggregated data for fast but 
          numerous events (such as statting the file). If this option is enabled, 
          profiler will record each event - resulting in more precise profiling data 
          but LARGE performance hit. Option only has effect if --profile used as well.
          """.trimIndent(),
      )

// failure, success or all
// default: "failure"
    @JvmField
    @Suppress("unused")
    val remotePrintExecutionMessages =
      BazelFlag<String>(
        name = "remote_print_execution_messages",
        description =
          """
          Choose when to print remote execution messages. Valid values are `failure`, 
          to print only on failures, `success` to print only on successes and `all` 
          to print always.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val sandboxDebug =
      BazelFlag.boolean(
        name = "sandbox_debug",
        description =
          """
          Enables debugging features for the sandboxing feature. This includes two 
          things: first, the sandbox root contents are left untouched after a build; 
          and second, prints extra debugging information on execution. This can help 
          developers of Bazel or Starlark rules with debugging failures due to 
          missing input files, etc.
          """.trimIndent(),
      )

// an integer
// default: "1"
    @JvmField
    @Suppress("unused")
    val showResult =
      BazelFlag<String>(
        name = "show_result",
        description =
          """
          Show the results of the build.  For each target, state whether or not it 
          was brought up-to-date, and if so, a list of output files that were built.  
          The printed files are convenient strings for copy+pasting to the shell, to 
          execute them.
          This option requires an integer argument, which is the threshold number of 
          targets above which result information is not printed. Thus zero causes 
          suppression of the message and MAX_INT causes printing of the result to 
          occur always. The default is one.
          If nothing was built for a target its results may be omitted to keep the 
          output under the threshold.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val slimProfile =
      BazelFlag.boolean(
        name = "slim_profile",
        description =
          """
          Slims down the size of the JSON profile by merging events if the profile 
          gets  too large.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val starlarkCpuProfile =
      BazelFlag<String>(
        name = "starlark_cpu_profile",
        description =
          """
          Writes into the specified file a pprof profile of CPU usage by all Starlark 
          threads.
          """.trimIndent(),
      )

// true, pretty_print or false
// default: "false"
    @JvmField
    @Suppress("unused")
    val subcommands =
      BazelFlag<String>(
        name = "subcommands",
        description =
          """
          Display the subcommands executed during a build. Related flags: --
          execution_log_json_file, --execution_log_binary_file (for logging 
          subcommands to a file in a tool-friendly format).
          """.trimIndent(),
      )

// summary, errors, all or streamed
// default: "summary"
    @JvmField
    @Suppress("unused")
    val testOutput =
      BazelFlag<String>(
        name = "test_output",
        description =
          """
          Specifies desired output mode. Valid values are 'summary' to output only 
          test status summary, 'errors' to also print test logs for failed tests, 
          'all' to print logs for all tests and 'streamed' to output logs for all 
          tests in real time (this will force tests to be executed locally one at a 
          time regardless of --test_strategy value).
          """.trimIndent(),
      )

// short, terse, detailed, none or testcase
// default: "short"
    @JvmField
    @Suppress("unused")
    val testSummary =
      BazelFlag<String>(
        name = "test_summary",
        description =
          """
          Specifies the desired format of the test summary. Valid values are 'short' 
          to print information only about tests executed, 'terse', to print 
          information only about unsuccessful tests that were run, 'detailed' to 
          print detailed information about failed test cases, 'testcase' to print 
          summary in test case resolution, do not print detailed information about 
          failed test cases and 'none' to omit the summary.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val toolTag =
      BazelFlag<String>(
        name = "tool_tag",
        description =
          """
          A tool name to attribute this Bazel invocation to.
          """.trimIndent(),
      )

// a comma-separated list of regex expressions with prefix '-' specifying excluded paths
// default: "-.*"
    @JvmField
    @Suppress("unused")
    val toolchainResolutionDebug =
      BazelFlag<String>(
        name = "toolchain_resolution_debug",
        description =
          """
          Print debug information during toolchain resolution. The flag takes a 
          regex, which is checked against toolchain types and specific targets to see 
          which to debug. Multiple regexes may be  separated by commas, and then each 
          regex is checked separately. Note: The output of this flag is very complex 
          and will likely only be useful to experts in toolchain resolution.
          """.trimIndent(),
      )

// Convert list of comma separated event kind to list of filters
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val uiEventFilters =
      BazelFlag<String>(
        name = "ui_event_filters",
        description =
          """
          Specifies which events to show in the UI. It is possible to add or remove 
          events to the default ones using leading +/-, or override the default set 
          completely with direct assignment. The set of supported event kinds include 
          INFO, DEBUG, ERROR and more.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val verboseExplanations =
      BazelFlag.boolean(
        name = "verbose_explanations",
        description =
          """
          Increases the verbosity of the explanations issued if --explain is enabled. 
          Has no effect if --explain is not enabled.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val verboseFailures =
      BazelFlag.boolean(
        name = "verbose_failures",
        description =
          """
          If a command fails, print out the full command line.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Options specifying or altering a generic input to a Bazel command that does not fall into other categories.:
// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val aspectsParameters =
      BazelFlag<String>(
        name = "aspects_parameters",
        description =
          """
          Specifies the values of the command-line aspects parameters. Each parameter 
          value is specified via <param_name>=<param_value>, for example 
          'my_param=my_val' where 'my_param' is a parameter of some aspect in --
          aspects list or required by an aspect in the list. This option can be used 
          multiple times. However, it is not allowed to assign values to the same 
          parameter more than once.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val experimentalResolvedFileInsteadOfWorkspace =
      BazelFlag<String>(
        name = "experimental_resolved_file_instead_of_workspace",
        description =
          """
          If non-empty read the specified resolved file instead of the WORKSPACE file
          """.trimIndent(),
      )

// a 'name=value' flag alias
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val flagAlias =
      BazelFlag<String>(
        name = "flag_alias",
        description =
          """
          Sets a shorthand name for a Starlark flag. It takes a single key-value pair 
          in the form "<key>=<value>" as an argument.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDefaultToExplicitInitPy =
      BazelFlag.boolean(
        name = "incompatible_default_to_explicit_init_py",
        description =
          """
          This flag changes the default behavior so that __init__.py files are no 
          longer automatically created in the runfiles of Python targets. Precisely, 
          when a py_binary or py_test target has legacy_create_init set to "auto" 
          (the default), it is treated as false if and only if this flag is set. See 
          https://github.com/bazelbuild/bazel/issues/10076.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatiblePy2OutputsAreSuffixed =
      BazelFlag.boolean(
        name = "incompatible_py2_outputs_are_suffixed",
        description =
          """
          If true, targets built in the Python 2 configuration will appear under an 
          output root that includes the suffix '-py2', while targets built for Python 
          3 will appear in a root with no Python-related suffix. This means that the 
          `bazel-bin` convenience symlink will point to Python 3 targets rather than 
          Python 2. If you enable this option it is also recommended to enable `--
          incompatible_py3_is_default`.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatiblePy3IsDefault =
      BazelFlag.boolean(
        name = "incompatible_py3_is_default",
        description =
          """
          If true, `py_binary` and `py_test` targets that do not set their 
          `python_version` (or `default_python_version`) attribute will default to 
          PY3 rather than to PY2. If you set this flag it is also recommended to set 
          `--incompatible_py2_outputs_are_suffixed`.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleUsePythonToolchains =
      BazelFlag.boolean(
        name = "incompatible_use_python_toolchains",
        description =
          """
          If set to true, executable native Python rules will use the Python runtime 
          specified by the Python toolchain, rather than the runtime given by legacy 
          flags like --python_top.
          """.trimIndent(),
      )

// PY2 or PY3
// default: see description
    @JvmField
    @Suppress("unused")
    val pythonVersion =
      BazelFlag<String>(
        name = "python_version",
        description =
          """
          The Python major version mode, either `PY2` or `PY3`. Note that this is 
          overridden by `py_binary` and `py_test` targets (even if they don't 
          explicitly specify a version) so there is usually not much reason to supply 
          this flag.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val targetPatternFile =
      BazelFlag<String>(
        name = "target_pattern_file",
        description =
          """
          If set, build will read patterns from the file named here, rather than on 
          the command line. It is an error to specify a file here as well as command-
          line patterns.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Remote caching and execution options:
// failure
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalCircuitBreakerStrategy =
      BazelFlag<String>(
        name = "experimental_circuit_breaker_strategy",
        description =
          """
          Specifies the strategy for the circuit breaker to use. Available strategies 
          are "failure". On invalid value for the option the behavior same as the 
          option is not set.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalDownloaderConfig =
      BazelFlag<String>(
        name = "experimental_downloader_config",
        description =
          """
          Specify a file to configure the remote downloader with. This file consists 
          of lines, each of which starts with a directive (`allow`, `block` or 
          `rewrite`) followed by either a host name (for `allow` and `block`) or two 
          patterns, one to match against, and one to use as a substitute URL, with 
          back-references starting from `${"$"}1`. It is possible for multiple `rewrite` 
          directives for the same URL to be give, and in this case multiple URLs will 
          be returned.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalGuardAgainstConcurrentChanges =
      BazelFlag.boolean(
        name = "experimental_guard_against_concurrent_changes",
        description =
          """
          Turn this off to disable checking the ctime of input files of an action 
          before uploading it to a remote cache. There may be cases where the Linux 
          kernel delays writing of files, which could cause false positives.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteCacheAsync =
      BazelFlag.boolean(
        name = "experimental_remote_cache_async",
        description =
          """
          If true, remote cache I/O will happen in the background instead of taking 
          place as the part of a spawn.
          """.trimIndent(),
      )

// an integer
// default: "0"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteCacheCompressionThreshold =
      BazelFlag<String>(
        name = "experimental_remote_cache_compression_threshold",
        description =
          """
          The minimum blob size required to compress/decompress with zstd. 
          Ineffectual unless --remote_cache_compression is set.
          """.trimIndent(),
      )

// an integer
// default: "0"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteCacheEvictionRetries =
      BazelFlag<String>(
        name = "experimental_remote_cache_eviction_retries",
        description =
          """
          The maximum number of attempts to retry if the build encountered remote 
          cache eviction error. A non-zero value will implicitly set --
          incompatible_remote_use_new_exit_code_for_lost_inputs to true. A new 
          invocation id will be generated for each attempt. If you generate 
          invocation id and provide it to Bazel with --invocation_id, you should not 
          use this flag. Instead, set flag --
          incompatible_remote_use_new_exit_code_for_lost_inputs and check for the 
          exit code 39.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteCacheLeaseExtension =
      BazelFlag.boolean(
        name = "experimental_remote_cache_lease_extension",
        description =
          """
          If set to true, Bazel will extend the lease for outputs of remote actions 
          during the build by sending `FindMissingBlobs` calls periodically to remote 
          cache. The frequency is based on the value of `--
          experimental_remote_cache_ttl`.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "3h"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteCacheTtl =
      BazelFlag<String>(
        name = "experimental_remote_cache_ttl",
        description =
          """
          The guaranteed minimal TTL of blobs in the remote cache after their digests 
          are recently referenced e.g. by an ActionResult or FindMissingBlobs. Bazel 
          does several optimizations based on the blobs' TTL e.g. doesn't repeatedly 
          call GetActionResult in an incremental build. The value should be set 
          slightly less than the real TTL since there is a gap between when the 
          server returns the digests and when Bazel receives them.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalRemoteCaptureCorruptedOutputs =
      BazelFlag<String>(
        name = "experimental_remote_capture_corrupted_outputs",
        description =
          """
          A path to a directory where the corrupted outputs will be captured to.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteDiscardMerkleTrees =
      BazelFlag.boolean(
        name = "experimental_remote_discard_merkle_trees",
        description =
          """
          If set to true, discard in-memory copies of the input root's Merkle tree 
          and associated input mappings during calls to GetActionResult() and 
          Execute(). This reduces memory usage significantly, but does require Bazel 
          to recompute them upon remote cache misses and retries.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalRemoteDownloader =
      BazelFlag<String>(
        name = "experimental_remote_downloader",
        description =
          """
          A Remote Asset API endpoint URI, to be used as a remote download proxy. The 
          supported schemas are grpc, grpcs (grpc with TLS enabled) and unix (local 
          UNIX sockets). If no schema is provided Bazel will default to grpcs. See: 
          https://github.com/bazelbuild/remote-
          apis/blob/master/build/bazel/remote/asset/v1/remote_asset.proto
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteDownloaderLocalFallback =
      BazelFlag.boolean(
        name = "experimental_remote_downloader_local_fallback",
        description =
          """
          Whether to fall back to the local downloader if remote downloader fails.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteExecutionKeepalive =
      BazelFlag.boolean(
        name = "experimental_remote_execution_keepalive",
        description =
          """
          Whether to use keepalive for remote execution calls.
          """.trimIndent(),
      )

// an integer in 0-100 range
// default: "10"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteFailureRateThreshold =
      BazelFlag<String>(
        name = "experimental_remote_failure_rate_threshold",
        description =
          """
          Sets the allowed number of failure rate in percentage for a specific time 
          window after which it stops calling to the remote cache/executor. By 
          default the value is 10. Setting this to 0 means no limitation.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "60s"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteFailureWindowInterval =
      BazelFlag<String>(
        name = "experimental_remote_failure_window_interval",
        description =
          """
          The interval in which the failure rate of the remote requests are computed. 
          On zero or negative value the failure duration is computed the whole 
          duration of the execution.Following units can be used: Days (d), hours (h), 
          minutes (m), seconds (s), and milliseconds (ms). If the unit is omitted, 
          the value is interpreted as seconds.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteMarkToolInputs =
      BazelFlag.boolean(
        name = "experimental_remote_mark_tool_inputs",
        description =
          """
          If set to true, Bazel will mark inputs as tool inputs for the remote 
          executor. This can be used to implement remote persistent workers.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteMerkleTreeCache =
      BazelFlag.boolean(
        name = "experimental_remote_merkle_tree_cache",
        description =
          """
          If set to true, Merkle tree calculations will be memoized to improve the 
          remote cache hit checking speed. The memory foot print of the cache is 
          controlled by --experimental_remote_merkle_tree_cache_size.
          """.trimIndent(),
      )

// a long integer
// default: "1000"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteMerkleTreeCacheSize =
      BazelFlag<String>(
        name = "experimental_remote_merkle_tree_cache_size",
        description =
          """
          The number of Merkle trees to memoize to improve the remote cache hit 
          checking speed. Even though the cache is automatically pruned according to 
          Java's handling of soft references, out-of-memory errors can occur if set 
          too high. If set to 0  the cache size is unlimited. Optimal value varies 
          depending on project's size. Default to 1000.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalRemoteOutputService =
      BazelFlag<String>(
        name = "experimental_remote_output_service",
        description =
          """
          HOST or HOST:PORT of a remote output service endpoint. The supported 
          schemas are grpc, grpcs (grpc with TLS enabled) and unix (local UNIX 
          sockets). If no schema is provided Bazel will default to grpcs. Specify 
          grpc:// or unix: schema to disable TLS.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val experimentalRemoteOutputServiceOutputPathPrefix =
      BazelFlag<String>(
        name = "experimental_remote_output_service_output_path_prefix",
        description =
          """
          The path under which the contents of output directories managed by the [](--
          experimental_remote_output_service) are placed. The actual output directory 
          used by a build will be a descendant of this path and determined by the 
          output service.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRemoteRequireCached =
      BazelFlag.boolean(
        name = "experimental_remote_require_cached",
        description =
          """
          If set to true, enforce that all actions that can run remotely are cached, 
          or else fail the build. This is useful to troubleshoot non-determinism 
          issues as it allows checking whether actions that should be cached are 
          actually cached without spuriously injecting new results into the cache.
          """.trimIndent(),
      )

// Converts to a Scrubber
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalRemoteScrubbingConfig =
      BazelFlag<String>(
        name = "experimental_remote_scrubbing_config",
        description =
          """
          Enables remote cache key scrubbing with the supplied configuration file, 
          which must be a protocol buffer in text format (see 
          src/main/protobuf/remote_scrubbing.proto).
          
          This feature is intended to facilitate sharing a remote/disk cache between 
          actions executing on different platforms but targeting the same platform. 
          It should be used with extreme care, as improper settings may cause 
          accidental sharing of cache entries and result in incorrect builds.
          
          Scrubbing does not affect how an action is executed, only how its 
          remote/disk cache key is computed for the purpose of retrieving or storing 
          an action result. Scrubbed actions are incompatible with remote execution, 
          and will always be executed locally instead.
          
          Modifying the scrubbing configuration does not invalidate outputs present 
          in the local filesystem or internal caches; a clean build is required to 
          reexecute affected actions.
          
          In order to successfully use this feature, you likely want to set a custom 
          --host_platform together with --experimental_platform_in_output_dir (to 
          normalize output prefixes) and --incompatible_strict_action_env (to 
          normalize environment variables).
          """.trimIndent(),
      )

// off, platform, virtual or auto
// default: "auto"
    @JvmField
    @Suppress("unused")
    val experimentalWorkerForRepoFetching =
      BazelFlag<String>(
        name = "experimental_worker_for_repo_fetching",
        description =
          """
          The threading mode to use for repo fetching. If set to 'off', no worker 
          thread is used, and the repo fetching is subject to restarts. Otherwise, 
          uses a virtual worker thread.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleRemoteBuildEventUploadRespectNoCache =
      BazelFlag.boolean(
        name = "incompatible_remote_build_event_upload_respect_no_cache",
        description =
          """
          Deprecated. No-op. Use --remote_build_event_upload=minimal instead.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleRemoteDownloaderSendAllHeaders =
      BazelFlag.boolean(
        name = "incompatible_remote_downloader_send_all_headers",
        description =
          """
          Whether to send all values of a multi-valued header to the remote 
          downloader instead of just the first.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleRemoteOutputPathsRelativeToInputRoot =
      BazelFlag.boolean(
        name = "incompatible_remote_output_paths_relative_to_input_root",
        description =
          """
          If set to true, output paths are relative to input root instead of working 
          directory.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleRemoteResultsIgnoreDisk =
      BazelFlag.boolean(
        name = "incompatible_remote_results_ignore_disk",
        description =
          """
          No-op
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleRemoteUseNewExitCodeForLostInputs =
      BazelFlag.boolean(
        name = "incompatible_remote_use_new_exit_code_for_lost_inputs",
        description =
          """
          If set to true, Bazel will use new exit code 39 instead of 34 if remote 
          cache evicts blobs during the build.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val remoteAcceptCached =
      BazelFlag.boolean(
        name = "remote_accept_cached",
        description =
          """
          Whether to accept remotely cached action results.
          """.trimIndent(),
      )

// all or minimal
// default: "minimal"
    @JvmField
    @Suppress("unused")
    val remoteBuildEventUpload =
      BazelFlag<String>(
        name = "remote_build_event_upload",
        description =
          """
          If set to 'all', all local outputs referenced by BEP are uploaded to remote 
          cache.
          If set to 'minimal', local outputs referenced by BEP are not uploaded to 
          the remote cache, except for files that are important to the consumers of 
          BEP (e.g. test logs and timing profile). bytestream:// scheme is always 
          used for the uri of files even if they are missing from remote cache.
          Default to 'minimal'.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val remoteBytestreamUriPrefix =
      BazelFlag<String>(
        name = "remote_bytestream_uri_prefix",
        description =
          """
          The hostname and instance name to be used in bytestream:// URIs that are 
          written into build event streams. This option can be set when builds are 
          performed using a proxy, which causes the values of --remote_executor and --
          remote_instance_name to no longer correspond to the canonical name of the 
          remote execution service. When not set, it will default to "${"$"}{hostname}
          /${"$"}{instance_name}".
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val remoteCache =
      BazelFlag<String>(
        name = "remote_cache",
        description =
          """
          A URI of a caching endpoint. The supported schemas are http, https, grpc, 
          grpcs (grpc with TLS enabled) and unix (local UNIX sockets). If no schema 
          is provided Bazel will default to grpcs. Specify grpc://, http:// or unix: 
          schema to disable TLS. See https://bazel.build/remote/caching
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val remoteCacheCompression =
      BazelFlag.boolean(
        name = "remote_cache_compression",
        description =
          """
          If enabled, compress/decompress cache blobs with zstd when their size is at 
          least --experimental_remote_cache_compression_threshold.
          """.trimIndent(),
      )

// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val remoteCacheHeader =
      BazelFlag<String>(
        name = "remote_cache_header",
        description =
          """
          Specify a header that will be included in cache requests: --
          remote_cache_header=Name=Value. Multiple headers can be passed by 
          specifying the flag multiple times. Multiple values for the same name will 
          be converted to a comma-separated list.
          """.trimIndent(),
      )

// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val remoteDefaultExecProperties =
      BazelFlag<String>(
        name = "remote_default_exec_properties",
        description =
          """
          Set the default exec properties to be used as the remote execution platform 
          if an execution platform does not already set exec_properties.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val remoteDefaultPlatformProperties =
      BazelFlag<String>(
        name = "remote_default_platform_properties",
        description =
          """
          Set the default platform properties to be set for the remote execution API, 
          if the execution platform does not already set remote_execution_properties. 
          This value will also be used if the host platform is selected as the 
          execution platform for remote execution.
          """.trimIndent(),
      )

// a valid Java regular expression
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val remoteDownloadRegex =
      BazelFlag<String>(
        name = "remote_download_regex",
        description =
          """
          Force remote build outputs whose path matches this pattern to be 
          downloaded, irrespective of --remote_download_outputs. Multiple patterns 
          may be specified by repeating this flag.
          """.trimIndent(),
      )

// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val remoteDownloaderHeader =
      BazelFlag<String>(
        name = "remote_downloader_header",
        description =
          """
          Specify a header that will be included in remote downloader requests: --
          remote_downloader_header=Name=Value. Multiple headers can be passed by 
          specifying the flag multiple times. Multiple values for the same name will 
          be converted to a comma-separated list.
          """.trimIndent(),
      )

// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val remoteExecHeader =
      BazelFlag<String>(
        name = "remote_exec_header",
        description =
          """
          Specify a header that will be included in execution requests: --
          remote_exec_header=Name=Value. Multiple headers can be passed by specifying 
          the flag multiple times. Multiple values for the same name will be 
          converted to a comma-separated list.
          """.trimIndent(),
      )

// an integer
// default: "0"
    @JvmField
    @Suppress("unused")
    val remoteExecutionPriority =
      BazelFlag<String>(
        name = "remote_execution_priority",
        description =
          """
          The relative priority of actions to be executed remotely. The semantics of 
          the particular priority values are server-dependent.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val remoteExecutor =
      BazelFlag<String>(
        name = "remote_executor",
        description =
          """
          HOST or HOST:PORT of a remote execution endpoint. The supported schemas are 
          grpc, grpcs (grpc with TLS enabled) and unix (local UNIX sockets). If no 
          schema is provided Bazel will default to grpcs. Specify grpc:// or unix: 
          schema to disable TLS.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val remoteGrpcLog =
      BazelFlag<String>(
        name = "remote_grpc_log",
        description =
          """
          If specified, a path to a file to log gRPC call related details. This log 
          consists of a sequence of serialized com.google.devtools.build.lib.remote.
          logging.RemoteExecutionLog.LogEntry protobufs with each message prefixed by 
          a varint denoting the size of the following serialized protobuf message, as 
          performed by the method LogEntry.writeDelimitedTo(OutputStream).
          """.trimIndent(),
      )

// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val remoteHeader =
      BazelFlag<String>(
        name = "remote_header",
        description =
          """
          Specify a header that will be included in requests: --
          remote_header=Name=Value. Multiple headers can be passed by specifying the 
          flag multiple times. Multiple values for the same name will be converted to 
          a comma-separated list.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val remoteInstanceName =
      BazelFlag<String>(
        name = "remote_instance_name",
        description =
          """
          Value to pass as instance_name in the remote execution API.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val remoteLocalFallback =
      BazelFlag.boolean(
        name = "remote_local_fallback",
        description =
          """
          Whether to fall back to standalone local execution strategy if remote 
          execution fails.
          """.trimIndent(),
      )

// a string
// default: "local"
    @JvmField
    @Suppress("unused")
    val remoteLocalFallbackStrategy =
      BazelFlag<String>(
        name = "remote_local_fallback_strategy",
        description =
          """
          No-op, deprecated. See https://github.com/bazelbuild/bazel/issues/7480 for 
          details.
          """.trimIndent(),
      )

// an integer
// default: "100"
    @JvmField
    @Suppress("unused")
    val remoteMaxConnections =
      BazelFlag<String>(
        name = "remote_max_connections",
        description =
          """
          Limit the max number of concurrent connections to remote cache/executor. By 
          default the value is 100. Setting this to 0 means no limitation.
          For HTTP remote cache, one TCP connection could handle one request at one 
          time, so Bazel could make up to --remote_max_connections concurrent 
          requests.
          For gRPC remote cache/executor, one gRPC channel could usually handle 100+ 
          concurrent requests, so Bazel could make around `--remote_max_connections * 
          100` concurrent requests.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val remoteProxy =
      BazelFlag<String>(
        name = "remote_proxy",
        description =
          """
          Connect to the remote cache through a proxy. Currently this flag can only 
          be used to configure a Unix domain socket (unix:/path/to/socket).
          """.trimIndent(),
      )

// an integer
// default: "0"
    @JvmField
    @Suppress("unused")
    val remoteResultCachePriority =
      BazelFlag<String>(
        name = "remote_result_cache_priority",
        description =
          """
          The relative priority of remote actions to be stored in remote cache. The 
          semantics of the particular priority values are server-dependent.
          """.trimIndent(),
      )

// an integer
// default: "5"
    @JvmField
    @Suppress("unused")
    val remoteRetries =
      BazelFlag<String>(
        name = "remote_retries",
        description =
          """
          The maximum number of attempts to retry a transient error. If set to 0, 
          retries are disabled.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "5s"
    @JvmField
    @Suppress("unused")
    val remoteRetryMaxDelay =
      BazelFlag<String>(
        name = "remote_retry_max_delay",
        description =
          """
          The maximum backoff delay between remote retry attempts. Following units 
          can be used: Days (d), hours (h), minutes (m), seconds (s), and 
          milliseconds (ms). If the unit is omitted, the value is interpreted as 
          seconds.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "60s"
    @JvmField
    @Suppress("unused")
    val remoteTimeout =
      BazelFlag<String>(
        name = "remote_timeout",
        description =
          """
          The maximum amount of time to wait for remote execution and cache calls. 
          For the REST cache, this is both the connect and the read timeout. 
          Following units can be used: Days (d), hours (h), minutes (m), seconds (s), 
          and milliseconds (ms). If the unit is omitted, the value is interpreted as 
          seconds.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val remoteUploadLocalResults =
      BazelFlag.boolean(
        name = "remote_upload_local_results",
        description =
          """
          Whether to upload locally executed action results to the remote cache if 
          the remote cache supports it and the user is authorized to do so.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val remoteVerifyDownloads =
      BazelFlag.boolean(
        name = "remote_verify_downloads",
        description =
          """
          If set to true, Bazel will compute the hash sum of all remote downloads 
          and  discard the remotely cached values if they don't match the expected 
          value.
          """.trimIndent(),
      )

// unknown line:
// unknown line: Miscellaneous options, not otherwise categorized.:
// default: "true"
    @JvmField
    @Suppress("unused")
    val allowAnalysisCacheDiscard =
      BazelFlag.boolean(
        name = "allow_analysis_cache_discard",
        description =
          """
          If discarding the analysis cache due to a change in the build system, 
          setting this option to false will cause bazel to exit, rather than 
          continuing with the build. This option has no effect when 
          'discard_analysis_cache' is also set.
          """.trimIndent(),
      )

// none, all, packages or subpackages
// default: "none"
    @JvmField
    @Suppress("unused")
    val autoOutputFilter =
      BazelFlag<String>(
        name = "auto_output_filter",
        description =
          """
          If --output_filter is not specified, then the value for this option is used 
          create a filter automatically. Allowed values are 'none' (filter nothing / 
          show everything), 'all' (filter everything / show nothing), 'packages' 
          (include output from rules in packages mentioned on the Blaze command 
          line), and 'subpackages' (like 'packages', but also include subpackages). 
          For the 'packages' and 'subpackages' values //java/foo and //javatests/foo 
          are treated as one package)'.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val buildManualTests =
      BazelFlag.boolean(
        name = "build_manual_tests",
        description =
          """
          Forces test targets tagged 'manual' to be built. 'manual' tests are 
          excluded from processing. This option forces them to be built (but not 
          executed).
          """.trimIndent(),
      )

// a 'name=value' assignment
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val buildMetadata =
      BazelFlag<String>(
        name = "build_metadata",
        description =
          """
          Custom key-value string pairs to supply in a build event.
          """.trimIndent(),
      )

// comma-separated list of options
// default: ""
    @JvmField
    @Suppress("unused")
    val buildTagFilters =
      BazelFlag<String>(
        name = "build_tag_filters",
        description =
          """
          Specifies a comma-separated list of tags. Each tag can be optionally 
          preceded with '-' to specify excluded tags. Only those targets will be 
          built that contain at least one included tag and do not contain any 
          excluded tags. This option does not affect the set of tests executed with 
          the 'test' command; those are be governed by the test filtering options, 
          for example '--test_tag_filters'
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val buildTestsOnly =
      BazelFlag.boolean(
        name = "build_tests_only",
        description =
          """
          If specified, only *_test and test_suite rules will be built and other 
          targets specified on the command line will be ignored. By default 
          everything that was requested will be built.
          """.trimIndent(),
      )

// a tri-state (auto, yes, no)
// default: "auto"
    @JvmField
    @Suppress("unused")
    val cacheTestResults =
      BazelFlag<String>(
        name = "cache_test_results",
        description =
          """
          If set to 'auto', Bazel reruns a test if and only if: (1) Bazel detects 
          changes in the test or its dependencies, (2) the test is marked as 
          external, (3) multiple test runs were requested with --runs_per_test, or(4) 
          the test previously failed. If set to 'yes', Bazel caches all test results 
          except for tests marked as external. If set to 'no', Bazel does not cache 
          any test results.
          """.trimIndent(),
      )

// yes, no or auto
// default: "auto"
    @JvmField
    @Suppress("unused")
    val color =
      BazelFlag<String>(
        name = "color",
        description =
          """
          Use terminal controls to colorize output.
          """.trimIndent(),
      )

// none or lcov
// default: "none"
    @JvmField
    @Suppress("unused")
    val combinedReport =
      BazelFlag<String>(
        name = "combined_report",
        description =
          """
          Specifies desired cumulative coverage report type. At this point only LCOV 
          is supported.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val compileOneDependency =
      BazelFlag.boolean(
        name = "compile_one_dependency",
        description =
          """
          Compile a single dependency of the argument files. This is useful for 
          syntax checking source files in IDEs, for example, by rebuilding a single 
          target that depends on the source file to detect errors as early as 
          possible in the edit/build/test cycle. This argument affects the way all 
          non-flag arguments are interpreted; instead of being targets to build they 
          are source filenames.  For each source filename an arbitrary target that 
          depends on it will be built.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val config =
      BazelFlag<String>(
        name = "config",
        description =
          """
          Selects additional config sections from the rc files; for every <command>, 
          it also pulls in the options from <command>:<config> if such a section 
          exists; if this section doesn't exist in any .rc file, Blaze fails with an 
          error. The config sections and flag combinations they are equivalent to are 
          located in the tools/*.blazerc config files.
          """.trimIndent(),
      )

// Path to a credential helper. It may be absolute, relative to the PATH environment variable, or %workspace%-relative. The path be optionally prefixed by a scope  followed by an '='. The scope is a domain name, optionally with a single leading '*' wildcard component. A helper applies to URIs matching its scope, with more specific scopes preferred. If a helper has no scope, it applies to every URI.
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val credentialHelper =
      BazelFlag<String>(
        name = "credential_helper",
        description =
          """
          Configures a credential helper conforming to the <a href="https://github.
          com/EngFlow/credential-helper-spec">Credential Helper Specification</a> to 
          use for retrieving authorization credentials for  repository fetching, 
          remote caching and execution, and the build event service.
          
          Credentials supplied by a helper take precedence over credentials supplied 
          by `--google_default_credentials`, `--google_credentials`, a `.netrc` file, 
          or the auth parameter to `repository_ctx.download()` and `repository_ctx.
          download_and_extract()`.
          
          May be specified multiple times to set up multiple helpers.
          
          See https://blog.engflow.com/2023/10/09/configuring-bazels-credential-
          helper/ for instructions.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "30m"
    @JvmField
    @Suppress("unused")
    val credentialHelperCacheDuration =
      BazelFlag<String>(
        name = "credential_helper_cache_duration",
        description =
          """
          The default duration for which credentials supplied by a credential helper 
          are cached if the helper does not provide when the credentials expire.
          """.trimIndent(),
      )

// An immutable length of time.
// default: "10s"
    @JvmField
    @Suppress("unused")
    val credentialHelperTimeout =
      BazelFlag<String>(
        name = "credential_helper_timeout",
        description =
          """
          Configures the timeout for a credential helper.
          
          Credential helpers failing to respond within this timeout will fail the 
          invocation.
          """.trimIndent(),
      )

// yes, no or auto
// default: "auto"
    @JvmField
    @Suppress("unused")
    val curses =
      BazelFlag<String>(
        name = "curses",
        description =
          """
          Use terminal cursor controls to minimize scrolling output.
          """.trimIndent(),
      )

// comma-separated list of package names
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val deletedPackages =
      BazelFlag<String>(
        name = "deleted_packages",
        description =
          """
          A comma-separated list of names of packages which the build system will 
          consider non-existent, even if they are visible somewhere on the package 
          path.
          Use this option when deleting a subpackage 'x/y' of an existing package 
          'x'.  For example, after deleting x/y/BUILD in your client, the build 
          system may complain if it encounters a label '//x:y/z' if that is still 
          provided by another package_path entry.  Specifying --deleted_packages x/y 
          avoids this problem.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val discardAnalysisCache =
      BazelFlag.boolean(
        name = "discard_analysis_cache",
        description =
          """
          Discard the analysis cache immediately after the analysis phase completes. 
          Reduces memory usage by ~10%, but makes further incremental builds slower.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val diskCache =
      BazelFlag<String>(
        name = "disk_cache",
        description =
          """
          A path to a directory where Bazel can read and write actions and action 
          outputs. If the directory does not exist, it will be created.
          """.trimIndent(),
      )

// a one-line string
// default: ""
    @JvmField
    @Suppress("unused")
    val embedLabel =
      BazelFlag<String>(
        name = "embed_label",
        description =
          """
          Embed source control revision or release label in binary
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val enablePlatformSpecificConfig =
      BazelFlag.boolean(
        name = "enable_platform_specific_config",
        description =
          """
          If true, Bazel picks up host-OS-specific config lines from bazelrc files. 
          For example, if the host OS is Linux and you run bazel build, Bazel picks 
          up lines starting with build:linux. Supported OS identifiers are linux, 
          macos, windows, freebsd, and openbsd. Enabling this flag is equivalent to 
          using --config=linux on Linux, --config=windows on Windows, etc.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val executionLogBinaryFile =
      BazelFlag<String>(
        name = "execution_log_binary_file",
        description =
          """
          Log the executed spawns into this file as length-delimited SpawnExec 
          protos, according to src/main/protobuf/spawn.proto. Related flags: --
          execution_log_json_file (text JSON format; mutually exclusive), --
          execution_log_sort (whether to sort the execution log), --subcommands (for 
          displaying subcommands in terminal output).
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val executionLogJsonFile =
      BazelFlag<String>(
        name = "execution_log_json_file",
        description =
          """
          Log the executed spawns into this file as newline-delimited JSON 
          representations of SpawnExec protos, according to src/main/protobuf/spawn.
          proto. Related flags: --execution_log_binary_file (binary protobuf format; 
          mutually exclusive), --execution_log_sort (whether to sort the execution 
          log), --subcommands (for displaying subcommands in terminal output).
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val executionLogSort =
      BazelFlag.boolean(
        name = "execution_log_sort",
        description =
          """
          Whether to sort the execution log, making it easier to compare logs across 
          invocations. Set to false to avoid potentially significant CPU and memory 
          usage at the end of the invocation, at the cost of producing the log in 
          nondeterministic execution order. Only applies to the binary and JSON 
          formats; the compact format is never sorted.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val expandTestSuites =
      BazelFlag.boolean(
        name = "expand_test_suites",
        description =
          """
          Expand test_suite targets into their constituent tests before analysis. 
          When this flag is turned on (the default), negative target patterns will 
          apply to the tests belonging to the test suite, otherwise they will not. 
          Turning off this flag is useful when top-level aspects are applied at 
          command line: then they can analyze test_suite targets.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalCancelConcurrentTests =
      BazelFlag.boolean(
        name = "experimental_cancel_concurrent_tests",
        description =
          """
          If true, then Blaze will cancel concurrently running tests on the first 
          successful run. This is only useful in combination with --
          runs_per_test_detects_flakes.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val experimentalExecutionLogCompactFile =
      BazelFlag<String>(
        name = "experimental_execution_log_compact_file",
        description =
          """
          Log the executed spawns into this file as length-delimited ExecLogEntry 
          protos, according to src/main/protobuf/spawn.proto. The entire file is zstd 
          compressed. This is an experimental format under active development, and 
          may change at any time. Related flags: --execution_log_binary_file (binary 
          protobuf format; mutually exclusive), --execution_log_json_file (text JSON 
          format; mutually exclusive), --subcommands (for displaying subcommands in 
          terminal output).
          """.trimIndent(),
      )

// a comma-separated list of regex expressions with prefix '-' specifying excluded paths
// default: ""
    @JvmField
    @Suppress("unused")
    val experimentalExtraActionFilter =
      BazelFlag<String>(
        name = "experimental_extra_action_filter",
        description =
          """
          Deprecated in favor of aspects. Filters set of targets to schedule 
          extra_actions for.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalExtraActionTopLevelOnly =
      BazelFlag.boolean(
        name = "experimental_extra_action_top_level_only",
        description =
          """
          Deprecated in favor of aspects. Only schedules extra_actions for top level 
          targets.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalFetchAllCoverageOutputs =
      BazelFlag.boolean(
        name = "experimental_fetch_all_coverage_outputs",
        description =
          """
          If true, then Bazel fetches the entire coverage data directory for each 
          test during a coverage run.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalGenerateLlvmLcov =
      BazelFlag.boolean(
        name = "experimental_generate_llvm_lcov",
        description =
          """
          If true, coverage for clang will generate an LCOV report.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val experimentalJ2objcHeaderMap =
      BazelFlag.boolean(
        name = "experimental_j2objc_header_map",
        description =
          """
          Whether to generate J2ObjC header map in parallel of J2ObjC transpilation.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalJ2objcShorterHeaderPath =
      BazelFlag.boolean(
        name = "experimental_j2objc_shorter_header_path",
        description =
          """
          Whether to generate with shorter header path (uses "_ios" instead of 
          "_j2objc").
          """.trimIndent(),
      )

// off, javabuilder or bazel
// default: "javabuilder"
    @JvmField
    @Suppress("unused")
    val experimentalJavaClasspath =
      BazelFlag<String>(
        name = "experimental_java_classpath",
        description =
          """
          Enables reduced classpaths for Java compilations.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalLimitAndroidLintToAndroidConstrainedJava =
      BazelFlag.boolean(
        name = "experimental_limit_android_lint_to_android_constrained_java",
        description =
          """
          Limit --experimental_run_android_lint_on_java_rules to Android-compatible 
          libraries.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRuleExtensionApi =
      BazelFlag.boolean(
        name = "experimental_rule_extension_api",
        description =
          """
          Enable experimental rule extension API and subrule APIs
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalRunAndroidLintOnJavaRules =
      BazelFlag.boolean(
        name = "experimental_run_android_lint_on_java_rules",
        description =
          """
          Whether to validate java_* sources.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalSpawnScheduler =
      BazelFlag.boolean(
        name = "experimental_spawn_scheduler",
        description =
          """
          Enable dynamic execution by running actions locally and remotely in 
          parallel. Bazel spawns each action locally and remotely and picks the one 
          that completes first. If an action supports workers, the local action will 
          be run in the persistent worker mode. To enable dynamic execution for an 
          individual action mnemonic, use the `--internal_spawn_scheduler` and `--
          strategy=<mnemonic>=dynamic` flags instead.
            Expands to: --internal_spawn_scheduler --spawn_strategy=dynamic
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val experimentalWindowsWatchfs =
      BazelFlag.boolean(
        name = "experimental_windows_watchfs",
        description =
          """
          If true, experimental Windows support for --watchfs is enabled. Otherwise --
          watchfsis a non-op on Windows. Make sure to also enable --watchfs.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val explicitJavaTestDeps =
      BazelFlag.boolean(
        name = "explicit_java_test_deps",
        description =
          """
          Explicitly specify a dependency to JUnit or Hamcrest in a java_test instead 
          of  accidentally obtaining from the TestRunner's deps. Only works for bazel 
          right now.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val fetch =
      BazelFlag.boolean(
        name = "fetch",
        description =
          """
          Allows the command to fetch external dependencies. If set to false, the 
          command will utilize any cached version of the dependency, and if none 
          exists, the command will result in failure.
          """.trimIndent(),
      )

// comma-separated list of options
// default: "https://www.googleapis.com/auth/cloud-platform"
    @JvmField
    @Suppress("unused")
    val googleAuthScopes =
      BazelFlag<String>(
        name = "google_auth_scopes",
        description =
          """
          A comma-separated list of Google Cloud authentication scopes.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val googleCredentials =
      BazelFlag<String>(
        name = "google_credentials",
        description =
          """
          Specifies the file to get authentication credentials from. See https:
          //cloud.google.com/docs/authentication for details.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val googleDefaultCredentials =
      BazelFlag.boolean(
        name = "google_default_credentials",
        description =
          """
          Whether to use 'Google Application Default Credentials' for authentication. 
          See https://cloud.google.com/docs/authentication for details. Disabled by 
          default.
          """.trimIndent(),
      )

// An immutable length of time.
// default: see description
    @JvmField
    @Suppress("unused")
    val grpcKeepaliveTime =
      BazelFlag<String>(
        name = "grpc_keepalive_time",
        description =
          """
          Configures keep-alive pings for outgoing gRPC connections. If this is set, 
          then Bazel sends pings after this much time of no read operations on the 
          connection, but only if there is at least one pending gRPC call. Times are 
          treated as second granularity; it is an error to set a value less than one 
          second. By default, keep-alive pings are disabled. You should coordinate 
          with the service owner before enabling this setting. For example to set a 
          value of 30 seconds to this flag, it should be done as this --
          grpc_keepalive_time=30s
          """.trimIndent(),
      )

// An immutable length of time.
// default: "20s"
    @JvmField
    @Suppress("unused")
    val grpcKeepaliveTimeout =
      BazelFlag<String>(
        name = "grpc_keepalive_timeout",
        description =
          """
          Configures a keep-alive timeout for outgoing gRPC connections. If keep-
          alive pings are enabled with --grpc_keepalive_time, then Bazel times out a 
          connection if it does not receive a ping reply after this much time. Times 
          are treated as second granularity; it is an error to set a value less than 
          one second. If keep-alive pings are disabled, then this setting is ignored.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val hostJavaLauncher =
      BazelFlag<String>(
        name = "host_java_launcher",
        description =
          """
          The Java launcher used by tools that are executed during a build.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostJavacopt =
      BazelFlag<String>(
        name = "host_javacopt",
        description =
          """
          Additional options to pass to javac when building tools that are executed 
          during a build.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val hostJvmopt =
      BazelFlag<String>(
        name = "host_jvmopt",
        description =
          """
          Additional options to pass to the Java VM when building tools that are 
          executed during  the build. These options will get added to the VM startup 
          options of each  java_binary target.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleCheckShardingSupport =
      BazelFlag.boolean(
        name = "incompatible_check_sharding_support",
        description =
          """
          If true, Bazel will fail a sharded test if the test runner does not 
          indicate that it supports sharding by touching the file at the path in 
          TEST_SHARD_STATUS_FILE. If false, a test runner that does not support 
          sharding will lead to all tests running in each shard.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDisableNonExecutableJavaBinary =
      BazelFlag.boolean(
        name = "incompatible_disable_non_executable_java_binary",
        description =
          """
          If true, java_binary is always executable. create_executable attribute is 
          removed.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleDisallowSymlinkFileToDir =
      BazelFlag.boolean(
        name = "incompatible_disallow_symlink_file_to_dir",
        description =
          """
          No-op.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleDontUseJavasourceinfoprovider =
      BazelFlag.boolean(
        name = "incompatible_dont_use_javasourceinfoprovider",
        description =
          """
          No-op
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val incompatibleExclusiveTestSandboxed =
      BazelFlag.boolean(
        name = "incompatible_exclusive_test_sandboxed",
        description =
          """
          If true, exclusive tests will run with sandboxed strategy. Add 'local' tag 
          to force an exclusive test run locally
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val incompatibleStrictActionEnv =
      BazelFlag.boolean(
        name = "incompatible_strict_action_env",
        description =
          """
          If true, Bazel uses an environment with a static value for PATH and does 
          not inherit LD_LIBRARY_PATH. Use --action_env=ENV_VARIABLE if you want to 
          inherit specific environment variables from the client, but note that doing 
          so can prevent cross-user caching if a shared cache is used.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val j2objcTranslationFlags =
      BazelFlag<String>(
        name = "j2objc_translation_flags",
        description =
          """
          Additional options to pass to the J2ObjC tool.
          """.trimIndent(),
      )

// comma-separated list of options
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val javaDebug =
      BazelFlag<String>(
        name = "java_debug",
        description =
          """
          Causes the Java virtual machine of a java test to wait for a connection 
          from a JDWP-compliant debugger (such as jdb) before starting the test. 
          Implies -test_output=streamed.
            Expands to: --test_arg=--wrapper_script_flag=--debug 
            --test_output=streamed --test_strategy=exclusive --test_timeout=9999 
            --nocache_test_results
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val javaDeps =
      BazelFlag.boolean(
        name = "java_deps",
        description =
          """
          Generate dependency information (for now, compile-time classpath) per Java 
          target.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val javaHeaderCompilation =
      BazelFlag.boolean(
        name = "java_header_compilation",
        description =
          """
          Compile ijars directly from source.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val javaLanguageVersion =
      BazelFlag<String>(
        name = "java_language_version",
        description =
          """
          The Java language version
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val javaLauncher =
      BazelFlag<String>(
        name = "java_launcher",
        description =
          """
          The Java launcher to use when building Java binaries.  If this flag is set 
          to the empty string, the JDK launcher is used. The "launcher" attribute 
          overrides this flag.
          """.trimIndent(),
      )

// a string
// default: "local_jdk"
    @JvmField
    @Suppress("unused")
    val javaRuntimeVersion =
      BazelFlag<String>(
        name = "java_runtime_version",
        description =
          """
          The Java runtime version
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val javacopt =
      BazelFlag<String>(
        name = "javacopt",
        description =
          """
          Additional options to pass to javac.
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val jvmopt =
      BazelFlag<String>(
        name = "jvmopt",
        description =
          """
          Additional options to pass to the Java VM. These options will get added to 
          the VM startup options of each java_binary target.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val legacyMainDexListGenerator =
      BazelFlag<String>(
        name = "legacy_main_dex_list_generator",
        description =
          """
          Specifies a binary to use to generate the list of classes that must be in 
          the main dex when compiling legacy multidex.
          """.trimIndent(),
      )

// an integer
// default: "15"
    @JvmField
    @Suppress("unused")
    val localTerminationGraceSeconds =
      BazelFlag<String>(
        name = "local_termination_grace_seconds",
        description =
          """
          Time to wait between terminating a local process due to timeout and 
          forcefully shutting it down.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val optimizingDexer =
      BazelFlag<String>(
        name = "optimizing_dexer",
        description =
          """
          Specifies a binary to use to do dexing without sharding.
          """.trimIndent(),
      )

// an equals-separated mapping of repository name to path
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val overrideRepository =
      BazelFlag<String>(
        name = "override_repository",
        description =
          """
          Override a repository with a local path in the form of <repository 
          name>=<path>. If the given path is an absolute path, it will be used as it 
          is. If the given path is a relative path, it is relative to the current 
          working directory. If the given path starts with '%workspace%, it is 
          relative to the workspace root, which is the output of `bazel info 
          workspace`. If the given path is empty, then remove any previous overrides.
          """.trimIndent(),
      )

// colon-separated list of options
// default: "%workspace%"
    @JvmField
    @Suppress("unused")
    val packagePath =
      BazelFlag<String>(
        name = "package_path",
        description =
          """
          A colon-separated list of where to look for packages. Elements beginning 
          with '%workspace%' are relative to the enclosing workspace. If omitted or 
          empty, the default is the output of 'bazel info default-package-path'.
          """.trimIndent(),
      )

// a build target label
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val plugin =
      BazelFlag<String>(
        name = "plugin",
        description =
          """
          Plugins to use in the build. Currently works with java_plugin.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val progressInTerminalTitle =
      BazelFlag.boolean(
        name = "progress_in_terminal_title",
        description =
          """
          Show the command progress in the terminal title. Useful to see what bazel 
          is doing when having multiple terminal tabs.
          """.trimIndent(),
      )

// a build target label
// default: see description
    @JvmField
    @Suppress("unused")
    val proguardTop =
      BazelFlag<String>(
        name = "proguard_top",
        description =
          """
          Specifies which version of ProGuard to use for code removal when building a 
          Java binary.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/proto:protoc"
    @JvmField
    @Suppress("unused")
    val protoCompiler =
      BazelFlag<String>(
        name = "proto_compiler",
        description =
          """
          The label of the proto-compiler.
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/proto:cc_toolchain"
    @JvmField
    @Suppress("unused")
    val protoToolchainForCc =
      BazelFlag<String>(
        name = "proto_toolchain_for_cc",
        description =
          """
          Label of proto_lang_toolchain() which describes how to compile C++ protos
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/j2objc:j2objc_proto_toolchain"
    @JvmField
    @Suppress("unused")
    val protoToolchainForJ2objc =
      BazelFlag<String>(
        name = "proto_toolchain_for_j2objc",
        description =
          """
          Label of proto_lang_toolchain() which describes how to compile j2objc protos
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/proto:java_toolchain"
    @JvmField
    @Suppress("unused")
    val protoToolchainForJava =
      BazelFlag<String>(
        name = "proto_toolchain_for_java",
        description =
          """
          Label of proto_lang_toolchain() which describes how to compile Java protos
          """.trimIndent(),
      )

// a build target label
// default: "@bazel_tools//tools/proto:javalite_toolchain"
    @JvmField
    @Suppress("unused")
    val protoToolchainForJavalite =
      BazelFlag<String>(
        name = "proto_toolchain_for_javalite",
        description =
          """
          Label of proto_lang_toolchain() which describes how to compile JavaLite 
          protos
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val protocopt =
      BazelFlag<String>(
        name = "protocopt",
        description =
          """
          Additional options to pass to the protobuf compiler.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val runsPerTestDetectsFlakes =
      BazelFlag.boolean(
        name = "runs_per_test_detects_flakes",
        description =
          """
          If true, any shard in which at least one run/attempt passes and at least 
          one run/attempt fails gets a FLAKY status.
          """.trimIndent(),
      )

// a path
// default: see description
    @JvmField
    @Suppress("unused")
    val shellExecutable =
      BazelFlag<String>(
        name = "shell_executable",
        description =
          """
          Absolute path to the shell executable for Bazel to use. If this is unset, 
          but the BAZEL_SH environment variable is set on the first Bazel invocation 
          (that starts up a Bazel server), Bazel uses that. If neither is set, Bazel 
          uses a hard-coded default path depending on the operating system it runs on 
          (Windows: c:/tools/msys64/usr/bin/bash.exe, FreeBSD: /usr/local/bin/bash, 
          all others: /bin/bash). Note that using a shell that is not compatible with 
          bash may lead to build failures or runtime failures of the generated 
          binaries.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val showLoadingProgress =
      BazelFlag.boolean(
        name = "show_loading_progress",
        description =
          """
          If enabled, causes Bazel to print "Loading package:" messages.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val showProgress =
      BazelFlag.boolean(
        name = "show_progress",
        description =
          """
          Display progress messages during a build.
          """.trimIndent(),
      )

// a double
// default: "0.2"
    @JvmField
    @Suppress("unused")
    val showProgressRateLimit =
      BazelFlag<String>(
        name = "show_progress_rate_limit",
        description =
          """
          Minimum number of seconds between progress messages in the output.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val showTimestamps =
      BazelFlag.boolean(
        name = "show_timestamps",
        description =
          """
          Include timestamps in messages
          """.trimIndent(),
      )

// a string
// may be used multiple times
    @JvmField
    @Suppress("unused")
    val testArg =
      BazelFlag<String>(
        name = "test_arg",
        description =
          """
          Specifies additional options and arguments that should be passed to the 
          test executable. Can be used multiple times to specify several arguments. 
          If multiple tests are executed, each of them will receive identical 
          arguments. Used only by the 'bazel test' command.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val testFilter =
      BazelFlag<String>(
        name = "test_filter",
        description =
          """
          Specifies a filter to forward to the test framework.  Used to limit the 
          tests run. Note that this does not affect which targets are built.
          """.trimIndent(),
      )

// comma-separated list of options
// default: ""
    @JvmField
    @Suppress("unused")
    val testLangFilters =
      BazelFlag<String>(
        name = "test_lang_filters",
        description =
          """
          Specifies a comma-separated list of test languages. Each language can be 
          optionally preceded with '-' to specify excluded languages. Only those test 
          targets will be found that are written in the specified languages. The name 
          used for each language should be the same as the language prefix in the 
          *_test rule, e.g. one of 'cc', 'java', 'py', etc. This option affects --
          build_tests_only behavior and the test command.
          """.trimIndent(),
      )

// an integer
// default: "-1"
    @JvmField
    @Suppress("unused")
    val testResultExpiration =
      BazelFlag<String>(
        name = "test_result_expiration",
        description =
          """
          This option is deprecated and has no effect.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val testRunnerFailFast =
      BazelFlag.boolean(
        name = "test_runner_fail_fast",
        description =
          """
          Forwards fail fast option to the test runner. The test runner should stop 
          execution upon first failure.
          """.trimIndent(),
      )

// explicit, disabled or forced=k where k is the number of shards to enforce
// default: "explicit"
    @JvmField
    @Suppress("unused")
    val testShardingStrategy =
      BazelFlag<String>(
        name = "test_sharding_strategy",
        description =
          """
          Specify strategy for test sharding: 'explicit' to only use sharding if the 
          'shard_count' BUILD attribute is present. 'disabled' to never use test 
          sharding. 'forced=k' to enforce 'k' shards for testing regardless of the 
          'shard_count' BUILD attribute.
          """.trimIndent(),
      )

// comma-separated list of values: small, medium, large or enormous
// default: ""
    @JvmField
    @Suppress("unused")
    val testSizeFilters =
      BazelFlag<String>(
        name = "test_size_filters",
        description =
          """
          Specifies a comma-separated list of test sizes. Each size can be optionally 
          preceded with '-' to specify excluded sizes. Only those test targets will 
          be found that contain at least one included size and do not contain any 
          excluded sizes. This option affects --build_tests_only behavior and the 
          test command.
          """.trimIndent(),
      )

// comma-separated list of options
// default: ""
    @JvmField
    @Suppress("unused")
    val testTagFilters =
      BazelFlag<String>(
        name = "test_tag_filters",
        description =
          """
          Specifies a comma-separated list of test tags. Each tag can be optionally 
          preceded with '-' to specify excluded tags. Only those test targets will be 
          found that contain at least one included tag and do not contain any 
          excluded tags. This option affects --build_tests_only behavior and the test 
          command.
          """.trimIndent(),
      )

// comma-separated list of values: short, moderate, long or eternal
// default: ""
    @JvmField
    @Suppress("unused")
    val testTimeoutFilters =
      BazelFlag<String>(
        name = "test_timeout_filters",
        description =
          """
          Specifies a comma-separated list of test timeouts. Each timeout can be 
          optionally preceded with '-' to specify excluded timeouts. Only those test 
          targets will be found that contain at least one included timeout and do not 
          contain any excluded timeouts. This option affects --build_tests_only 
          behavior and the test command.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val tlsCertificate =
      BazelFlag<String>(
        name = "tls_certificate",
        description =
          """
          Specify a path to a TLS certificate that is trusted to sign server 
          certificates.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val tlsClientCertificate =
      BazelFlag<String>(
        name = "tls_client_certificate",
        description =
          """
          Specify the TLS client certificate to use; you also need to provide a 
          client key to enable client authentication.
          """.trimIndent(),
      )

// a string
// default: see description
    @JvmField
    @Suppress("unused")
    val tlsClientKey =
      BazelFlag<String>(
        name = "tls_client_key",
        description =
          """
          Specify the TLS client key to use; you also need to provide a client 
          certificate to enable client authentication.
          """.trimIndent(),
      )

// a string
// default: ""
    @JvmField
    @Suppress("unused")
    val toolJavaLanguageVersion =
      BazelFlag<String>(
        name = "tool_java_language_version",
        description =
          """
          The Java language version used to execute the tools that are needed during 
          a build
          """.trimIndent(),
      )

// a string
// default: "remotejdk_11"
    @JvmField
    @Suppress("unused")
    val toolJavaRuntimeVersion =
      BazelFlag<String>(
        name = "tool_java_runtime_version",
        description =
          """
          The Java runtime version used to execute tools during the build
          """.trimIndent(),
      )

// an integer
// default: "8"
    @JvmField
    @Suppress("unused")
    val uiActionsShown =
      BazelFlag<String>(
        name = "ui_actions_shown",
        description =
          """
          Number of concurrent actions shown in the detailed progress bar; each 
          action is shown on a separate line. The progress bar always shows at least 
          one one, all numbers less than 1 are mapped to 1.
          """.trimIndent(),
      )

// default: "true"
    @JvmField
    @Suppress("unused")
    val useIjars =
      BazelFlag.boolean(
        name = "use_ijars",
        description =
          """
          If enabled, this option causes Java compilation to use interface jars. This 
          will result in faster incremental compilation, but error messages can be 
          different.
          """.trimIndent(),
      )

// default: "false"
    @JvmField
    @Suppress("unused")
    val watchfs =
      BazelFlag.boolean(
        name = "watchfs",
        description =
          """
          On Linux/macOS: If true, bazel tries to use the operating system's 
          file watch service for local changes instead of scanning every file for a 
          change. On Windows: this flag currently is a non-op but can be enabled in 
          conjunction with --experimental_windows_watchfs. On any OS: The behavior is 
          undefined if your workspace is on a network file system, and files are 
          edited on a remote machine.
          """.trimIndent(),
      )

// a path
// default: ""
    @JvmField
    @Suppress("unused")
    val workspaceStatusCommand =
      BazelFlag<String>(
        name = "workspace_status_command",
        description =
          """
          A command invoked at the beginning of the build to provide status 
          information about the workspace in the form of key/value pairs.  See the 
          User's Manual for the full specification. Also see 
          tools/buildstamp/get_workspace_status for an example.
          """.trimIndent(),
      )
// unknown line:
  }
