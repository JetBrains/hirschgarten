load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.python.backend` start

jvm_library(
    name = "backend",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.python.backend",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@community//platform/kernel/rpc.backend",
        "@community//platform/kernel/backend",
        "@community//platform/platform-impl/rpc",
        "@community//platform/backend",
        "@community//platform/core-api:core",
        "@community//platform/projectModel-api:projectModel",
        "@community//platform/projectModel-impl",
        "@community//platform/workspace/jps",
        "@community//platform/workspace/storage",
        "@community//python:python-community-impl",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "//plugins/bazel/intellij.bazel.connector:connector",
        "//plugins/bazel/intellij.bazel.importer:importer",
        "@community//platform/backend/workspace",
        "@community//platform/platform-api:ide",
        "@community//platform/util/progress",
        "@community//platform/lang-impl",
        "@community//platform/lang-core",
        "@community//platform/ide-core",
        "@community//platform/util",
        "@community//python/services/system-python",
        "@community//python/python-sdk:sdk",
        "//plugins/bazel/python/intellij.bazel.python.common:bazel-python-common",
    ],
)

jvm_library(
    name = "backend_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.python.backend",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":backend",
        "@community//platform/kernel/rpc.backend:rpc.backend_test_lib",
        "@community//platform/kernel/backend:backend_test_lib",
        "@community//platform/platform-impl/rpc:rpc_test_lib",
        "@community//platform/backend:backend_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "@community//platform/projectModel-impl:projectModel-impl_test_lib",
        "@community//platform/workspace/jps:jps_test_lib",
        "@community//platform/workspace/storage:storage_test_lib",
        "@community//python:python-community-impl_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "//plugins/bazel/intellij.bazel.connector:connector_test_lib",
        "//plugins/bazel/intellij.bazel.importer:importer_test_lib",
        "@community//platform/backend/workspace:workspace_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/util/progress:progress_test_lib",
        "@community//platform/lang-impl:lang-impl_test_lib",
        "@community//platform/lang-core:lang-core_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/util:util_test_lib",
        "@community//python/services/system-python:system-python_test_lib",
        "@community//python/python-sdk:sdk_test_lib",
        "//plugins/bazel/python/intellij.bazel.python.common:bazel-python-common_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.python.backend` end

### auto-generated section `iml intellij.bazel.python.backend` start
exports_files(
    [
        "intellij.bazel.python.backend.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.python.backend` end
