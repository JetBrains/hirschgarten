load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspacecontext",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/workspacecontext:__pkg__",
        "//server/executioncontext/workspacecontext/src/test/kotlin/org/jetbrains/bsp/bazel/workspacecontext:__pkg__",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/sharding:__pkg__",
    ],
    exports = [
        "//server/executioncontext/api",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/utils",
        "//server/bazelrunner:params",
        "//server/executioncontext/api",
        "//server/executioncontext/projectview:generator",
        "//server/executioncontext/projectview:parser",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "@maven//:commons_io_commons_io",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
)
