load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "utils",
    srcs = [
        "DelimitedMessageReader.kt",
        "InternalAspectsResolver.kt",
    ],
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//server/bazelrunner",
        "//server/commons",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/info",
        "@maven//:io_grpc_grpc_protobuf",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)

kt_jvm_library(
    name = "fileutils",
    srcs = ["FileUtils.kt"],
    visibility = [
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install:__subpackages__",
        "//server/server:__subpackages__",
    ],
)

kt_jvm_library(
    name = "structured_files_utils",
    srcs = ["StructuredFilesUtils.kt"],
    visibility = ["//server:__subpackages__"],
    deps = [
        "@maven//:com_google_code_gson_gson",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
