load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.bytecodeViewer` start

jvm_library(
    name = "bazel-bytecodeViewer",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.bytecodeViewer",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@community//libraries/kotlinx/coroutines/core",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "//plugins/bazel/intellij.bazel.core:core",
        "@community//plugins/ByteCodeViewer:java-byteCodeViewer",
        "@community//java/java-psi-api:psi",
        "@community//platform/lang-api:lang",
        "@community//platform/lang-core",
        "@community//platform/core-api:core",
        "@community//platform/platform-api:ide",
        "@community//platform/ide-core",
        "@community//platform/analysis-api:analysis",
        "@community//platform/projectModel-api:projectModel",
        "@community//platform/workspace/storage",
        "@community//platform/backend/workspace",
    ],
)

jvm_library(
    name = "bazel-bytecodeViewer_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.bytecodeViewer",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-bytecodeViewer",
        "@community//libraries/kotlinx/coroutines/core:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "@community//plugins/ByteCodeViewer:java-byteCodeViewer_test_lib",
        "@community//java/java-psi-api:psi_test_lib",
        "@community//platform/lang-api:lang_test_lib",
        "@community//platform/lang-core:lang-core_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/analysis-api:analysis_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "@community//platform/workspace/storage:storage_test_lib",
        "@community//platform/backend/workspace:workspace_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.bytecodeViewer` end

### auto-generated section `iml intellij.bazel.bytecodeViewer` start
exports_files(
    [
        "intellij.bazel.bytecodeViewer.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.bytecodeViewer` end
