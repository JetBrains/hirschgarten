load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.terminal` start

jvm_library(
    name = "bazel-terminal",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.terminal",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "//plugins/bazel/intellij.bazel.core:core",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "//plugins/bazel/intellij.bazel.projectview:projectview",
        "@community//platform/platform-api:ide",
        "@community//platform/core-api:core",
        "@community//plugins/terminal",
        "@community//plugins/terminal/completion",
        "@community//platform/markdown-utils",
    ],
)

jvm_library(
    name = "bazel-terminal_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.terminal",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-terminal",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "//plugins/bazel/intellij.bazel.projectview:projectview_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//plugins/terminal:terminal_test_lib",
        "@community//plugins/terminal/completion:completion_test_lib",
        "@community//platform/markdown-utils:markdown-utils_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.terminal` end

### auto-generated section `iml intellij.bazel.terminal` start
exports_files(
    [
        "intellij.bazel.terminal.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.terminal` end
