load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@rules_java//java:defs.bzl", "java_binary")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_binary(
    name = "test_stamper_tool",
    srcs = ["TestStamper.java"],
    main_class = "org.jetbrains.bazel.test.stamper.TestStamper",
    deps = [
        artifact("org.ow2.asm:asm"),
        artifact("info.picocli:picocli"),
        ":test_data_proto_java",
        "@protobuf//:protobuf_java",
    ],
)

proto_library(
    name = "test_data_proto",
    srcs = ["test_data.proto"],
)

java_proto_library(
    name = "test_data_proto_java",
    visibility = ["//visibility:public"],
    deps = [":test_data_proto"],
)
