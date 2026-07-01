load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.protoedit` start

jvm_library(
    name = "bazel-protoedit",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.protoedit",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@community//libraries/mvstore",
        "//plugins/bazel/intellij.bazel.core:core",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "//plugins/bazel/intellij.bazel.importer:importer",
        "@community//platform/core-api:core",
        "@community//platform/platform-api:ide",
        "@community//platform/platform-util-io:ide-util-io",
        "@community//platform/ide-core",
        "@community//platform/util-ex",
        "@community//platform/workspace/storage",
        "@community//platform/backend/workspace",
        "@community//platform/projectModel-api:projectModel",
        "//contrib/protobuf:protoeditor",
        "//contrib/protobuf/protoeditor-core",
        "//plugins/bazel/aspect-sdk:libraries-bazel-aspect-sdk",
        "//plugins/bazel/intellij.bazel.connector:connector",
    ],
)

jvm_library(
    name = "bazel-protoedit_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.protoedit",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-protoedit",
        "@community//libraries/mvstore:mvstore_test_lib",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "//plugins/bazel/intellij.bazel.importer:importer_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/platform-util-io:ide-util-io_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/util-ex:util-ex_test_lib",
        "@community//platform/workspace/storage:storage_test_lib",
        "@community//platform/backend/workspace:workspace_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "//contrib/protobuf:protoeditor_test_lib",
        "//contrib/protobuf/protoeditor-core:protoeditor-core_test_lib",
        "//plugins/bazel/aspect-sdk:libraries-bazel-aspect-sdk_test_lib",
        "//plugins/bazel/intellij.bazel.connector:connector_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.protoedit` end

### auto-generated section `iml intellij.bazel.protoedit` start
exports_files(
    [
        "intellij.bazel.protoedit.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.protoedit` end
