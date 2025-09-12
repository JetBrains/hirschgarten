load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "phased",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    exports = ["//commons/src/main/kotlin/org/jetbrains/bazel/label"],
    deps = [
        "//third_party/bazel/src/main/protobuf:build_java_proto",
        "@maven//:com_google_protobuf_protobuf_java",
    ],
)
