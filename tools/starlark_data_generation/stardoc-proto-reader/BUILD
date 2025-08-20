load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(
    name = "stardoc-proto-reader",
    srcs = glob([
        "src/**/*.kt",
    ]),
    main_class = "ProtobufReader",
    deps = [
        ":stardoc_output_java_proto",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
        "//tools/starlark_data_generation/common:commons",
    ],
)

java_proto_library(
    name = "stardoc_output_java_proto",
    visibility = ["//visibility:public"],
    deps = [
        "@stardoc//stardoc/proto:stardoc_output_proto",
    ],
)
