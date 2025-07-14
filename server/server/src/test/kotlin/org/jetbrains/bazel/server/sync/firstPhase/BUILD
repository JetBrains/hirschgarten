load("@//rules/testing:junit5.bzl", "kt_junit5_test")
load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "mocks",
    testonly = True,
    srcs = ["TargetMocks.kt"],
    visibility = ["//visibility:private"],
    deps = [
        "//third_party/bazel/src/main/protobuf:build_java_proto",
    ],
)

kt_junit5_test(
    name = "FirstPhaseTargetToBspMapperTest",
    size = "small",
    src = "FirstPhaseTargetToBspMapperTest.kt",
    tags = ["exclusive"],
    deps = [
        ":mocks",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/firstPhase",
        "//third_party/bazel/src/main/protobuf:build_java_proto",
        "@maven//:com_google_protobuf_protobuf_java",
    ],
)

kt_junit5_test(
    name = "FirstPhaseTargetUtilsTest",
    size = "small",
    src = "FirstPhaseTargetUtilsTest.kt",
    tags = ["exclusive"],
    deps = [
        ":mocks",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/firstPhase",
        "//third_party/bazel/src/main/protobuf:build_java_proto",
        "@maven//:com_google_protobuf_protobuf_java",
    ],
)
