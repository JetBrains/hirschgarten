load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspacecontext",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/workspacecontext:__pkg__",
        "//server/executioncontext/workspacecontext/src/test/kotlin/org/jetbrains/bsp/bazel/workspacecontext:__pkg__",
    ],
    exports = [
        "//server/executioncontext/api",
    ],
    deps = [
        "//server/bazelrunner:params",
        "//server/executioncontext/api",
        "//server/executioncontext/projectview:generator",
        "//server/executioncontext/projectview:parser",
        "@maven//:commons_io_commons_io",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
)
