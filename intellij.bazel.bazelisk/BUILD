load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.bazelisk` start

jvm_library(
    name = "bazelisk",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.bazelisk",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "@community//libraries/jediterm-core",
        "@community//platform/extensions",
        "@community//platform/core-api:core",
        "@community//platform/platform-api:ide",
        "@community//platform/ide-core",
        "@community//platform/util/base",
        "@community//platform/util",
        "@community//platform/lang-api:lang",
        "@community//platform/lang-impl",
        "@community//platform/util-ex",
        "@community//platform/util/progress",
    ],
)

jvm_library(
    name = "bazelisk_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.bazelisk",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazelisk",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//libraries/jediterm-core:jediterm-core_test_lib",
        "@community//platform/extensions:extensions_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/util/base:base_test_lib",
        "@community//platform/util:util_test_lib",
        "@community//platform/lang-api:lang_test_lib",
        "@community//platform/lang-impl:lang-impl_test_lib",
        "@community//platform/util-ex:util-ex_test_lib",
        "@community//platform/util/progress:progress_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.bazelisk` end

### auto-generated section `iml intellij.bazel.bazelisk` start
exports_files(
    [
        "intellij.bazel.bazelisk.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.bazelisk` end
