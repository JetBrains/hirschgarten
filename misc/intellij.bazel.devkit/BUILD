load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.devkit` start

jvm_library(
    name = "bazel-devkit",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.devkit",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@community//libraries/protobuf",
        "//plugins/bazel/intellij.bazel.core:core",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "//plugins/bazel/protobuf",
        "@community//platform/core-api:core",
        "@community//platform/platform-api:ide",
        "@community//platform/lang-api:lang",
        "@community//platform/util",
        "@community//platform/core-ui",
        "@community//platform/execution",
        "@community//platform/execution-impl",
        "@community//java/execution/openapi:java-execution",
        "@community//java/execution/impl",
        "@community//platform/platform-util-io:ide-util-io",
        "@community//platform/ide-core",
        "@community//platform/projectModel-api:projectModel",
        "@community//platform/xdebugger-api:debugger",
        "@community//java/debugger/impl",
        "@community//platform/external-system-impl:externalSystem-impl",
        "@community//platform/lang-core",
        "@community//java/java-impl:impl",
        "@community//platform/smRunner",
        "@community//platform/testRunner",
        "@community//plugins/devkit/devkit-core:core",
        "//plugins/bazel/intellij.bazel.importer:importer",
    ],
)

jvm_library(
    name = "bazel-devkit_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.devkit",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-devkit",
        "@community//libraries/protobuf:protobuf_test_lib",
        "//plugins/bazel/intellij.bazel.core:core_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "//plugins/bazel/protobuf:protobuf_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/lang-api:lang_test_lib",
        "@community//platform/util:util_test_lib",
        "@community//platform/core-ui:core-ui_test_lib",
        "@community//platform/execution:execution_test_lib",
        "@community//platform/execution-impl:execution-impl_test_lib",
        "@community//java/execution/openapi:java-execution_test_lib",
        "@community//java/execution/impl:impl_test_lib",
        "@community//platform/platform-util-io:ide-util-io_test_lib",
        "@community//platform/ide-core:ide-core_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "@community//platform/xdebugger-api:debugger_test_lib",
        "@community//java/debugger/impl:impl_test_lib",
        "@community//platform/external-system-impl:externalSystem-impl_test_lib",
        "@community//platform/lang-core:lang-core_test_lib",
        "@community//java/java-impl:impl_test_lib",
        "@community//platform/smRunner:smRunner_test_lib",
        "@community//platform/testRunner:testRunner_test_lib",
        "@community//plugins/devkit/devkit-core:core_test_lib",
        "//plugins/bazel/intellij.bazel.importer:importer_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.devkit` end

### auto-generated section `iml intellij.bazel.devkit` start
exports_files(
    [
        "intellij.bazel.devkit.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.devkit` end
