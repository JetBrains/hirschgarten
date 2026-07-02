load("@rules_jvm//:jvm.bzl", "jvm_library", "jvm_provided_library")

### auto-generated section `build intellij.bazel.commons` start

jvm_provided_library(
    name = "community_libraries_kotlinx_coroutines_core_provided",
    lib = "@community//libraries/kotlinx/coroutines/core",
)

jvm_provided_library(
    name = "community_libraries_opentelemetry-extension-kotlin_provided",
    lib = "@community//libraries/opentelemetry-extension-kotlin",
)

jvm_provided_library(
    name = "community_platform_analysis_provided",
    lib = "@community//platform/analysis-api:analysis",
)

jvm_library(
    name = "commons",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.commons",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@community//libraries/opentelemetry",
        "@community//platform/lang-api:lang",
        "@community//platform/lang-impl",
        "@community//libraries/guava",
        "@community//libraries/fastutil",
        "//plugins/bazel/protobuf",
        "@community//libraries/protobuf",
        "@community//libraries/gson",
        "@community//libraries/commons/cli",
        "@community//libraries/commons/io",
        "@community//libraries/hash4j",
        "@lib//:jetbrains-annotations",
        "@community//libraries/kotlinx/datetime",
        "@community//platform/util",
        "@community//platform/core-api:core",
        "@community//platform/platform-impl:ide-impl",
        "@community//platform/projectModel-api:projectModel",
        "@community//platform/projectModel-impl",
        "@community//platform/platform-util-io:ide-util-io",
        "@community//libraries/jediterm-core",
        "@community//platform/diagnostic/telemetry",
        "@community//platform/util/progress",
        "@community//platform/eel-provider",
        "//plugins/bazel/aspect-sdk:libraries-bazel-aspect-sdk",
        ":community_libraries_kotlinx_coroutines_core_provided",
        ":community_libraries_opentelemetry-extension-kotlin_provided",
        ":community_platform_analysis_provided",
    ],
)

jvm_library(
    name = "commons_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.commons",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":commons",
        "@community//libraries/opentelemetry:opentelemetry_test_lib",
        "@community//platform/lang-api:lang_test_lib",
        "@community//platform/lang-impl:lang-impl_test_lib",
        "@community//libraries/guava:guava_test_lib",
        "@community//libraries/fastutil:fastutil_test_lib",
        "//plugins/bazel/protobuf:protobuf_test_lib",
        "@community//libraries/protobuf:protobuf_test_lib",
        "@community//libraries/gson:gson_test_lib",
        "@community//libraries/commons/cli:cli_test_lib",
        "@community//libraries/commons/io:io_test_lib",
        "@community//libraries/kotlinx/coroutines/core",
        "@community//libraries/kotlinx/coroutines/core:core_test_lib",
        "@community//libraries/opentelemetry-extension-kotlin",
        "@community//libraries/opentelemetry-extension-kotlin:opentelemetry-extension-kotlin_test_lib",
        "@community//libraries/hash4j:hash4j_test_lib",
        "@community//libraries/kotlinx/datetime:datetime_test_lib",
        "@community//platform/util:util_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/analysis-api:analysis",
        "@community//platform/analysis-api:analysis_test_lib",
        "@community//platform/platform-impl:ide-impl_test_lib",
        "@community//platform/projectModel-api:projectModel_test_lib",
        "@community//platform/projectModel-impl:projectModel-impl_test_lib",
        "@community//platform/platform-util-io:ide-util-io_test_lib",
        "@community//libraries/jediterm-core:jediterm-core_test_lib",
        "@community//platform/diagnostic/telemetry:telemetry_test_lib",
        "@community//platform/util/progress:progress_test_lib",
        "@community//platform/eel-provider:eel-provider_test_lib",
        "//plugins/bazel/aspect-sdk:libraries-bazel-aspect-sdk_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.commons` end

### auto-generated section `iml intellij.bazel.commons` start
exports_files(
    [
        "intellij.bazel.commons.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.commons` end
