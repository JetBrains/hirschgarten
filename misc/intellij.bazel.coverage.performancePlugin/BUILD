load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.coverage.performancePlugin` start

jvm_library(
    name = "bazel-coverage-performancePlugin",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.coverage.performancePlugin",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@community//libraries/kotlinx/coroutines/core",
        "//plugins/bazel/misc/intellij.bazel.coverage:bazel-coverage",
        "//plugins/bazel/intellij.bazel.core:core",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "@community//plugins/performanceTesting/core:performanceTesting",
        "@community//platform/core-api:core",
        "@community//platform/core-impl",
        "@community//platform/platform-api:ide",
        "@community//platform/platform-impl:ide-impl",
        "@community//platform/lang-api:lang",
        "@community//platform/lang-impl",
        "@community//platform/execution",
        "@community//platform/projectModel-api:projectModel",
        "@community//platform/analysis-api:analysis",
        "@community//platform/smRunner",
        "@community//plugins/coverage-common:coverage",
        "@community//plugins/coverage-common/intellij.platform.coverage.agent:coverage-agent",
        "@community//platform/editor-ui-api:editor-ui",
        "@community//plugins/coverage",
        "@community//java/java-psi-api:psi",
    ],
)

jvm_library(
    name = "bazel-coverage-performancePlugin_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.coverage.performancePlugin",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-coverage-performancePlugin",
        "@community//libraries/kotlin/test:libraries-kotlinTest",
        "@community//libraries/kotlin/test:libraries-kotlinTest_test_lib",
        "@community//libraries/kotlinx/coroutines/core:core_test_lib",
        "//plugins/bazel/misc/intellij.bazel.coverage:bazel-coverage_test_lib",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//plugins/performanceTesting/core:performanceTesting_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/core-impl:core-impl_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/platform-impl:ide-impl_test_lib",
        "@community//platform/lang-api:lang_test_lib",
        "@community//platform/lang-impl:lang-impl_test_lib",
        "@community//platform/execution:execution_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "@community//platform/analysis-api:analysis_test_lib",
        "@community//platform/smRunner:smRunner_test_lib",
        "@community//plugins/coverage-common:coverage_test_lib",
        "@community//plugins/coverage-common/intellij.platform.coverage.agent:coverage-agent_test_lib",
        "@community//platform/editor-ui-api:editor-ui_test_lib",
        "@community//plugins/coverage:coverage_test_lib",
        "@community//java/java-psi-api:psi_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.coverage.performancePlugin` end

### auto-generated section `iml intellij.bazel.coverage.performancePlugin` start
exports_files(
    [
        "intellij.bazel.coverage.performancePlugin.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.coverage.performancePlugin` end
