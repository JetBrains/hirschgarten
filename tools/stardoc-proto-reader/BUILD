load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")
load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")

kt_jvm_binary(
    name = "stardoc-proto-reader",
    main_class = "ProtobufReader",
    srcs = glob([
        "src/**/*.kt",
    ]),
    deps = [
        ":stardoc_output_java_proto",
    ]
)

java_proto_library(
    name = "stardoc_output_java_proto",
    visibility = ["//visibility:public"],
    deps = ["@stardoc//stardoc/proto:stardoc_output_proto"],
)
