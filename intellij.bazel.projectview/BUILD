load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.projectview` start

jvm_library(
    name = "projectview",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.projectview",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@community//libraries/kotlinx/collections-immutable:libraries-kotlinx-collections-immutable",
        "@lib//:jetbrains-annotations",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "@community//platform/core-api:core",
        "@community//platform/platform-api:ide",
        "@community//platform/ide-core-impl",
        "@community//platform/ide-core",
        "@community//platform/platform-impl:ide-impl",
        "@community//platform/lang-api:lang",
        "@community//platform/analysis-api:analysis",
        "@community//platform/lang-impl",
        "@community//platform/markdown-utils",
        "@community//platform/code-style-api:codeStyle",
        "@community//platform/core-ui",
        "@community//platform/core-impl",
        "@community//platform/editor-ui-api:editor-ui",
        "@community//platform/util:util-ui",
    ],
)

jvm_library(
    name = "projectview_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.projectview",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":projectview",
        "@community//libraries/kotlinx/collections-immutable:libraries-kotlinx-collections-immutable_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/ide-core-impl:ide-core-impl_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/platform-impl:ide-impl_test_lib",
        "@community//platform/lang-api:lang_test_lib",
        "@community//platform/analysis-api:analysis_test_lib",
        "@community//platform/lang-impl:lang-impl_test_lib",
        "@community//platform/markdown-utils:markdown-utils_test_lib",
        "@community//platform/code-style-api:codeStyle_test_lib",
        "@community//platform/core-ui:core-ui_test_lib",
        "@community//platform/core-impl:core-impl_test_lib",
        "@community//platform/editor-ui-api:editor-ui_test_lib",
        "@community//platform/util:util-ui_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.projectview` end

### auto-generated section `iml intellij.bazel.projectview` start
exports_files(
    [
        "intellij.bazel.projectview.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.projectview` end
