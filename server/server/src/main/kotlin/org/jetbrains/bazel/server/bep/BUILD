load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "bep",
    srcs = glob([
        "*.java",
        "*.kt",
    ]),
    resources = ["//server/log4j_config"],
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/bazelrunner",
        "//server/commons",
        "//server/logger",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/diagnostics",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
        "//third_party/bazel/src/main/java/com/google/devtools/build/lib/buildeventstream/proto:build_event_stream_java_proto",
        "@googleapis//google/devtools/build/v1:build_java_grpc",
        "@googleapis//google/devtools/build/v1:build_java_proto",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_xml",
        "@maven//:com_fasterxml_jackson_module_jackson_module_kotlin",
        "@maven//:com_google_guava_guava",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
