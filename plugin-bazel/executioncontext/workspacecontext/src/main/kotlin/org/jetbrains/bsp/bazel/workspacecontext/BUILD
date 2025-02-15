load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspacecontext",
    srcs = glob(["*.kt"]),
    visibility = [
        "//plugin-bazel/executioncontext/workspacecontext:__pkg__",
        "//plugin-bazel/executioncontext/workspacecontext/src/test/kotlin/org/jetbrains/bsp/bazel/workspacecontext:__pkg__",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/sharding:__pkg__",
    ],
    exports = [
        "//plugin-bazel/executioncontext/api",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/utils",
        "//plugin-bazel/bazelrunner:params",
        "//plugin-bazel/executioncontext/api",
        "//plugin-bazel/executioncontext/projectview:generator",
        "//plugin-bazel/executioncontext/projectview:parser",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "@maven//:commons_io_commons_io",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
)
