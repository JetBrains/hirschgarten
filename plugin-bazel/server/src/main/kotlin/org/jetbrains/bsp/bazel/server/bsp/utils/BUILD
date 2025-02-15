load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "utils",
    srcs = [
        "DelimitedMessageReader.kt",
        "InternalAspectsResolver.kt",
    ],
    visibility = ["//plugin-bazel/server:__subpackages__"],
    deps = [
        "//plugin-bazel/bazelrunner",
        "//plugin-bazel/commons",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/info",
        "@maven//:io_grpc_grpc_protobuf",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)

kt_jvm_library(
    name = "fileutils",
    srcs = ["FileUtils.kt"],
    visibility = [
        "//plugin-bazel/install/src/main/kotlin/org/jetbrains/bsp/bazel/install:__subpackages__",
        "//plugin-bazel/server:__subpackages__",
    ],
)
