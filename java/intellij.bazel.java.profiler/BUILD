load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.java.profiler` start

jvm_library(
    name = "bazel-java-profiler",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.java.profiler",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "//plugins/profiler/ultimate",
        "//plugins/profiler/common",
        "@community//platform/util",
        "@community//platform/execution",
        "@community//java/execution/impl",
        "@community//platform/ide-core",
        "//plugins/bazel/intellij.bazel.core:core",
        "//plugins/bazel/java/intellij.bazel.java.core:bazel-java-core",
        "@community//platform/smRunner",
        "@community//platform/testRunner",
        "@community//platform/platform-util-io:ide-util-io",
        "@community//platform/core-api:core",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "@community//platform/execution-impl",
    ],
)

jvm_library(
    name = "bazel-java-profiler_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.java.profiler",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-java-profiler",
        "//plugins/profiler/ultimate:ultimate_test_lib",
        "//plugins/profiler/common:common_test_lib",
        "@community//platform/util:util_test_lib",
        "@community//platform/execution:execution_test_lib",
        "@community//java/execution/impl:impl_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/java/intellij.bazel.java.core:bazel-java-core_test_lib",
        "@community//platform/smRunner:smRunner_test_lib",
        "@community//platform/testRunner:testRunner_test_lib",
        "@community//platform/platform-util-io:ide-util-io_test_lib",
        "@community//platform/core-api:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//platform/execution-impl:execution-impl_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.java.profiler` end

### auto-generated section `iml intellij.bazel.java.profiler` start
exports_files(
    [
        "intellij.bazel.java.profiler.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.java.profiler` end
