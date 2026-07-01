load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.kotlin.coverage` start

jvm_library(
    name = "bazel-kotlin-coverage",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.kotlin.coverage",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@lib//:kotlinc-kotlin-compiler-common-provided",
        "@lib//:kotlinc-kotlin-jps-common-provided",
        "@lib//:kotlinc-analysis-api-platform-interface-provided",
        "//plugins/bazel/misc/intellij.bazel.coverage:bazel-coverage",
        "@community//platform/core-api:core",
        "@community//plugins/kotlin/base/plugin",
        "//plugins/bazel/java/intellij.bazel.java.coverage:bazel-java-coverage",
        "@community//plugins/kotlin/base/psi",
        "@community//platform/util",
        "@community//platform/core-impl",
    ],
)

jvm_library(
    name = "bazel-kotlin-coverage_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.kotlin.coverage",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-kotlin-coverage",
        "@lib//:kotlinc-kotlin-compiler-common-provided",
        "@lib//:kotlinc-kotlin-jps-common-provided",
        "@lib//:kotlinc-analysis-api-platform-interface-provided",
        "//plugins/bazel/misc/intellij.bazel.coverage:bazel-coverage_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//plugins/kotlin/base/plugin:plugin_test_lib",
        "//plugins/bazel/java/intellij.bazel.java.coverage:bazel-java-coverage_test_lib",
        "@community//plugins/kotlin/base/psi:psi_test_lib",
        "@community//platform/util:util_test_lib",
        "@community//platform/core-impl:core-impl_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.kotlin.coverage` end

### auto-generated section `iml intellij.bazel.kotlin.coverage` start
exports_files(
    [
        "intellij.bazel.kotlin.coverage.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.kotlin.coverage` end
