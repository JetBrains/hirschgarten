load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.devkit.monorepo` start

jvm_library(
    name = "bazel-devkit-monorepo",
    # Monorepo DevKit isn't published, we can't build against it
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.devkit.monorepo",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "//plugins/bazel/java/intellij.bazel.java.common:bazel-java-common",
        "@community//java/debugger/impl",
        "@community//platform/core-api:core",
        "@community//platform/xdebugger-api:debugger",
        "//plugins/monorepo-devkit",
        "//plugins/bazel/intellij.bazel.core:core",
        "@community//platform/execution-impl",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "@community//platform/util",
        "//plugins/monorepo-devkit/bazel",
        "@community//platform/projectModel-api:projectModel",
        "//plugins/bazel/intellij.bazel.importer:importer",
        "@community//platform/analysis-api:analysis",
        "//plugins/bazel/intellij.bazel.projectview:projectview",
        "//plugins/bazel/kotlin/intellij.bazel.kotlin.common:bazel-kotlin-common",
    ],
)

jvm_library(
    name = "bazel-devkit-monorepo_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.devkit.monorepo",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-devkit-monorepo",
        "//plugins/bazel/java/intellij.bazel.java.common:bazel-java-common_test_lib",
        "@community//java/debugger/impl:impl_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/xdebugger-api:debugger_test_lib",
        "//plugins/monorepo-devkit:monorepo-devkit_test_lib",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "@community//platform/execution-impl:execution-impl_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//platform/util:util_test_lib",
        "//plugins/monorepo-devkit/bazel:bazel_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "//plugins/bazel/intellij.bazel.importer:importer_test_lib",
        "@community//platform/analysis-api:analysis_test_lib",
        "//plugins/bazel/intellij.bazel.projectview:projectview_test_lib",
        "//plugins/bazel/kotlin/intellij.bazel.kotlin.common:bazel-kotlin-common_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.devkit.monorepo` end

### auto-generated section `iml intellij.bazel.devkit.monorepo` start
exports_files(
    [
        "intellij.bazel.devkit.monorepo.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.devkit.monorepo` end
