load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.java.coverage` start

jvm_library(
    name = "bazel-java-coverage",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.java.coverage",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "//plugins/bazel/intellij.bazel.core:core",
        "//plugins/bazel/misc/intellij.bazel.coverage:bazel-coverage",
        "@community//platform/core-api:core",
        "@community//java/openapi:java",
        "@community//java/java-impl:impl",
        "@community//java/java-psi-api:psi",
        "@community//plugins/coverage",
        "@community//platform/execution",
        "//plugins/bazel/java/intellij.bazel.java.core:bazel-java-core",
        "@community//platform/ide-core",
        "@community//platform/smRunner",
        "@community//platform/testRunner",
        "@community//java/execution/impl",
        "@community//platform/analysis-api:analysis",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "@community//platform/analysis-impl",
        "@community//platform/projectModel-api:projectModel",
    ],
)

jvm_library(
    name = "bazel-java-coverage_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.java.coverage",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-java-coverage",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/misc/intellij.bazel.coverage:bazel-coverage_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//java/openapi:java_test_lib",
        "@community//java/java-impl:impl_test_lib",
        "@community//java/java-psi-api:psi_test_lib",
        "@community//plugins/coverage:coverage_test_lib",
        "@community//platform/execution:execution_test_lib",
        "//plugins/bazel/java/intellij.bazel.java.core:bazel-java-core_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/smRunner:smRunner_test_lib",
        "@community//platform/testRunner:testRunner_test_lib",
        "@community//java/execution/impl:impl_test_lib",
        "@community//platform/analysis-api:analysis_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//platform/analysis-impl:analysis-impl_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.java.coverage` end

### auto-generated section `iml intellij.bazel.java.coverage` start
exports_files(
    [
        "intellij.bazel.java.coverage.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.java.coverage` end
