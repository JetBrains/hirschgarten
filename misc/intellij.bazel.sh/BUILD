load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.sh` start

jvm_library(
    name = "bazel-sh",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.sh",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "//plugins/bazel/intellij.bazel.core:core",
        "@community//platform/util",
        "@community//platform/core-impl",
        "@community//platform/core-api:core",
        "@community//plugins/sh/core",
    ],
)

jvm_library(
    name = "bazel-sh_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.sh",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-sh",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "@community//platform/util:util_test_lib",
        "@community//platform/core-impl:core-impl_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//plugins/sh/core:core_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.sh` end

### auto-generated section `iml intellij.bazel.sh` start
exports_files(
    [
        "intellij.bazel.sh.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.sh` end
