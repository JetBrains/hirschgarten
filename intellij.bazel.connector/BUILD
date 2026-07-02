load("@rules_jvm//:jvm.bzl", "jvm_library", "jvm_provided_library")
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")
load("@rules_java//java:java_import.bzl", "java_import")

### auto-generated section `build intellij.bazel.connector` start

jvm_provided_library(
    name = "community_libraries_opentelemetry_provided",
    lib = "@community//libraries/opentelemetry",
)

jvm_library(
    name = "connector",
    srcs = glob(
        [
            "src/**/*.kt",
            "src/**/*.java",
            "src/**/*.form",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.connector",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@lib//:kotlin-stdlib",
        "@lib//:jetbrains-annotations",
        "@community//libraries/jackson/dataformat-xml:libraries-jackson-dataformat-xml",
        "@community//platform/extensions",
        "@community//libraries/guava",
        "@community//libraries/fastutil",
        "@community//libraries/gson",
        "@community//libraries/protobuf",
        "@community//libraries/velocity",
        "//plugins/bazel/intellij.bazel.commons:commons",
        "//plugins/bazel/protobuf",
        "@community//platform/core-api:core",
        "@community//platform/platform-api:ide",
        "@community//platform/lang-api:lang",
        "@community//platform/util/base",
        "@community//platform/platform-util-io:ide-util-io",
        "@community//platform/util",
        "@community//platform/util-ex",
        "@community//platform/util/progress",
        "@community//platform/eel-provider",
        "@community//platform/backend",
        "@community//platform/util/coroutines",
        "@community//libraries/kotlinx/coroutines/core",
        "@community//libraries/jediterm-core",
        "@community//libraries/grpc",
        "@community//platform/diagnostic/telemetry",
        "@community//platform/diagnostic/telemetry.exporters",
        "@community//libraries/jackson/jackson",
        "@community//libraries/jackson/databind",
        "@community//libraries/jackson/module-kotlin:libraries-jackson-module-kotlin",
        "//plugins/bazel/aspect-sdk:libraries-bazel-aspect-sdk",
        ":community_libraries_opentelemetry_provided",
    ],
    deps_to_bundle = [
        ":jackson_dataformat_without_deps",
    ],
)

jvm_library(
    name = "connector_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.connector",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":connector",
        "@community//libraries/opentelemetry",
        "@community//libraries/opentelemetry:opentelemetry_test_lib",
        "@community//libraries/jackson/dataformat-xml:libraries-jackson-dataformat-xml_test_lib",
        "@community//platform/extensions:extensions_test_lib",
        "@community//libraries/guava:guava_test_lib",
        "@community//libraries/fastutil:fastutil_test_lib",
        "@community//libraries/gson:gson_test_lib",
        "@community//libraries/protobuf:protobuf_test_lib",
        "@community//libraries/velocity:velocity_test_lib",
        "//plugins/bazel/intellij.bazel.commons:commons_test_lib",
        "//plugins/bazel/protobuf:protobuf_test_lib",
        "@community//platform/core-api:core_test_lib",
        "@community//platform/platform-api:ide_test_lib",
        "@community//platform/lang-api:lang_test_lib",
        "@community//platform/util/base:base_test_lib",
        "@community//platform/platform-util-io:ide-util-io_test_lib",
        "@community//platform/util:util_test_lib",
        "@community//platform/util-ex:util-ex_test_lib",
        "@community//platform/util/progress:progress_test_lib",
        "@community//platform/eel-provider:eel-provider_test_lib",
        "@community//platform/backend:backend_test_lib",
        "@community//platform/util/coroutines:coroutines_test_lib",
        "@community//libraries/kotlinx/coroutines/core:core_test_lib",
        "@community//libraries/jediterm-core:jediterm-core_test_lib",
        "@community//libraries/grpc:grpc_test_lib",
        "@community//platform/diagnostic/telemetry:telemetry_test_lib",
        "@community//platform/diagnostic/telemetry.exporters:telemetry.exporters_test_lib",
        "@community//libraries/jackson/jackson:jackson_test_lib",
        "@community//libraries/jackson/databind:databind_test_lib",
        "@community//libraries/jackson/module-kotlin:libraries-jackson-module-kotlin_test_lib",
        "//plugins/bazel/aspect-sdk:libraries-bazel-aspect-sdk_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.connector` end

### auto-generated section `iml intellij.bazel.connector` start
exports_files(
    [
        "intellij.bazel.connector.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.connector` end

# Using @maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_xml directly means dependant libraries are added to the transitive closure.
# But all other Jackson libraries are already packed inside IJ platform, so import just one .jar file to avoid java.lang.LinkageError in runtime.
# See: https://youtrack.jetbrains.com/issue/BAZEL-3028/hirschgarten-253-and-261-branch-is-broken#focus=Comments-27-13760427.0-0
java_import(
    name = "jackson_dataformat_without_deps",
    jars = select_for_plugin_api({
        # The version must match IDEA's Jackson version exactly to the prevent exceptions in runtime
        "intellij-2026.2": ["@maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_xml_2_19_0_extension"],
    }),
)
