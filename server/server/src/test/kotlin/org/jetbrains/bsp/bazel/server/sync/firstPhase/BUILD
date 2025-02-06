load("@//rules/testing:junit5.bzl", "kt_junit5_test")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "mocks",
    testonly = True,
    srcs = ["TargetMocks.kt"],
    visibility = ["//visibility:private"],
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:build_java_proto",
    ],
)

kt_junit5_test(
    name = "FirstPhaseTargetToBspMapperTest",
    size = "small",
    src = "FirstPhaseTargetToBspMapperTest.kt",
    deps = [
        ":mocks",
        "//protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/firstPhase",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:build_java_proto",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_protobuf_protobuf_java",
    ],
)

kt_junit5_test(
    name = "FirstPhaseTargetUtilsTest",
    size = "small",
    src = "FirstPhaseTargetUtilsTest.kt",
    deps = [
        ":mocks",
        "//protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/firstPhase",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:build_java_proto",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_protobuf_protobuf_java",
    ],
)
