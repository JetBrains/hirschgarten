load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.python.common` start

jvm_library(
    name = "bazel-python-common",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.python.common",
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
        "//plugins/bazel/intellij.bazel.connector:connector",
        "//plugins/bazel/intellij.bazel.importer:importer",
        "@community//platform/core-api:core",
        "@community//platform/platform-api:ide",
        "@community//platform/ide-core",
        "@community//platform/ide-core-impl",
        "@community//platform/lang-api:lang",
        "@community//platform/workspace/storage",
        "@community//platform/lang-core",
        "@community//platform/lang-impl",
        "@community//platform/backend/workspace",
        "@community//platform/analysis-api:analysis",
        "@community//platform/xdebugger-api:debugger",
        "@community//platform/xdebugger-impl:debugger-impl",
        "@community//platform/execution",
        "@community//platform/execution-impl",
        "@community//platform/workspace/jps",
        "@community//python:python-community-impl",
        "@community//platform/projectModel-api:projectModel",
        "@community//platform/util/concurrency",
        "@community//platform/util/progress",
        "@community//python/python-sdk:sdk",
        "@community//python:python-pydev",
        "@community//platform/util/jdom",
        "@community//platform/smRunner",
        "@community//platform/testRunner",
        "@community//python/services/system-python",
    ],
)

jvm_library(
    name = "bazel-python-common_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.python.common",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-python-common",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "//plugins/bazel/intellij.bazel.connector:connector_test_lib",
        "//plugins/bazel/intellij.bazel.importer:importer_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/ide-core-impl:ide-core-impl_test_lib",
        "@community//platform/lang-api:lang_test_lib",
        "@community//platform/workspace/storage:storage_test_lib",
        "@community//platform/lang-core:lang-core_test_lib",
        "@community//platform/lang-impl:lang-impl_test_lib",
        "@community//platform/backend/workspace:workspace_test_lib",
        "@community//platform/analysis-api:analysis_test_lib",
        "@community//platform/xdebugger-api:debugger_test_lib",
        "@community//platform/xdebugger-impl:debugger-impl_test_lib",
        "@community//platform/execution:execution_test_lib",
        "@community//platform/execution-impl:execution-impl_test_lib",
        "@community//platform/workspace/jps:jps_test_lib",
        "@community//python:python-community-impl_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "@community//platform/util/concurrency:concurrency_test_lib",
        "@community//platform/util/progress:progress_test_lib",
        "@community//python/python-sdk:sdk_test_lib",
        "@community//python:python-pydev_test_lib",
        "@community//platform/util/jdom:jdom_test_lib",
        "@community//platform/smRunner:smRunner_test_lib",
        "@community//platform/testRunner:testRunner_test_lib",
        "@community//python/services/system-python:system-python_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.python.common` end

### auto-generated section `iml intellij.bazel.python.common` start
exports_files(
    [
        "intellij.bazel.python.common.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.python.common` end
