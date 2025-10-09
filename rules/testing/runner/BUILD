load("@rules_java//java:defs.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "test_runner",
    srcs = ["TestRunner.java"],
    visibility = ["//visibility:public"],
    deps = [
        artifact("org.ow2.asm:asm"),
        artifact("info.picocli:picocli"),
        "//rules/testing/stamper:test_data_proto_java",
        #         "@contrib_rules_jvm//java/src/com/github/bazel_contrib/contrib_rules_jvm/junit5",
        "@maven//:org_junit_platform_junit_platform_console",
        "@protobuf//:protobuf_java",
    ],
)
