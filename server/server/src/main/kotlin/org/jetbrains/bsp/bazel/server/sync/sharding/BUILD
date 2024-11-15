load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sync",
    srcs = glob([
        "*.kt",
        "*.java",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//server/executioncontext/workspacecontext",
        "//server/logger",
        "@maven//:org_apache_logging_log4j_log4j_api",
    ],
)
