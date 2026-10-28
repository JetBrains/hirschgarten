load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.javascript.common` start

jvm_library(
    name = "bazel-javascript-common",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.javascript.common",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@community//libraries/kotlinx/coroutines/core",
        "//plugins/bazel/intellij.bazel.backend:backend",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "@community//platform/core-api:core",
        "@community//platform/util",
        "@community//platform/projectModel-api:projectModel",
        "@community//platform/workspace/storage",
        "@community//platform/workspace/jps",
        "//plugins/JavaScriptLanguage/backend",
    ],
)

jvm_library(
    name = "bazel-javascript-common_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.javascript.common",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-javascript-common",
        "//plugins/bazel/intellij.bazel.backend:backend_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "@community//platform/workspace/storage:storage_test_lib",
        "@community//platform/workspace/jps:jps_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.javascript.common` end

### auto-generated section `iml intellij.bazel.javascript.common` start
exports_files(
    [
        "intellij.bazel.javascript.common.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.javascript.common` end
