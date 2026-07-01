load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.kotlin.sync` start

jvm_library(
    name = "bazel-kotlin-sync",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.kotlin.sync",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@lib//:kotlinc-kotlin-compiler-common-provided",
        "@lib//:kotlinc-kotlin-jps-common-provided",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "//plugins/bazel/intellij.bazel.importer:importer",
        "//plugins/bazel/java/intellij.bazel.java.sync:bazel-java-sync",
        "@community//platform/core-api:core",
        "@community//platform/platform-api:ide",
        "@community//plugins/kotlin/base/facet",
        "@community//plugins/kotlin/base/plugin",
        "@community//platform/workspace/storage",
        "@community//platform/backend/workspace",
        "@community//platform/workspace/jps",
        "@community//java/openapi:java",
        "@community//plugins/kotlin/base/project-structure",
    ],
)

jvm_library(
    name = "bazel-kotlin-sync_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.kotlin.sync",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":bazel-kotlin-sync",
        "@lib//:kotlinc-kotlin-compiler-common-provided",
        "@lib//:kotlinc-kotlin-jps-common-provided",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "//plugins/bazel/intellij.bazel.importer:importer_test_lib",
        "//plugins/bazel/java/intellij.bazel.java.sync:bazel-java-sync_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//plugins/kotlin/base/facet:facet_test_lib",
        "@community//plugins/kotlin/base/plugin:plugin_test_lib",
        "@community//platform/workspace/storage:storage_test_lib",
        "@community//platform/backend/workspace:workspace_test_lib",
        "@community//platform/workspace/jps:jps_test_lib",
        "@community//java/openapi:java_test_lib",
        "@community//plugins/kotlin/base/project-structure:project-structure_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.kotlin.sync` end

### auto-generated section `iml intellij.bazel.kotlin.sync` start
exports_files(
    [
        "intellij.bazel.kotlin.sync.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.kotlin.sync` end
