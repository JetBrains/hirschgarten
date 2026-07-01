load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.remoteDevelopment` start

jvm_library(
    name = "bazel-remoteDevelopment",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.remoteDevelopment",
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
        "@community//platform/core-api:core",
        "@community//platform/execution",
        "@community//platform/ide-core",
        "@community//platform/core-ui",
        "@community//platform/platform-util-io:ide-util-io",
        "@community//platform/util",
        "@community//libraries/rd/core",
        "//platform/backend/split",
    ],
)

jvm_library(
    name = "bazel-remoteDevelopment_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.remoteDevelopment",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-remoteDevelopment",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/execution:execution_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/core-ui:core-ui_test_lib",
        "@community//platform/platform-util-io:ide-util-io_test_lib",
        "@community//platform/util:util_test_lib",
        "@community//libraries/rd/core:core_test_lib",
        "//platform/backend/split:split_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.remoteDevelopment` end

### auto-generated section `iml intellij.bazel.remoteDevelopment` start
exports_files(
    [
        "intellij.bazel.remoteDevelopment.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.remoteDevelopment` end
