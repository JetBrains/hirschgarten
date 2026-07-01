load("@rules_jvm//:jvm.bzl", "jvm_library")

### auto-generated section `build intellij.bazel.protobuf` start

jvm_library(
    name = "protobuf",
    srcs = glob(
        [
            "src/main/gen/**/*.kt",
            "src/main/gen/**/*.java",
        ],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.protobuf",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        # do not sort,
        "@community//libraries/javax/annotation",
        "@community//libraries/grpc",
        "@community//libraries/guava",
        "@community//libraries/protobuf",
    ],
)

jvm_library(
    name = "protobuf_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.bazel.protobuf",
    visibility = ["//visibility:public"],
    runtime_deps = [
        # do not sort,
        ":protobuf",
        "@community//libraries/javax/annotation:annotation_test_lib",
        "@community//libraries/grpc:grpc_test_lib",
        "@community//libraries/guava:guava_test_lib",
        "@community//libraries/protobuf:protobuf_test_lib",
    ],
)
### auto-generated section `build intellij.bazel.protobuf` end

### auto-generated section `iml intellij.bazel.protobuf` start
exports_files(
    [
        "intellij.bazel.protobuf.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.bazel.protobuf` end
