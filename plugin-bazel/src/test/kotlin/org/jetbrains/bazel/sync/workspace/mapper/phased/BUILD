load("@//rules/testing:junit5.bzl", "kt_junit5_test")
load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "mocks",
    testonly = True,
    srcs = ["TargetMocks.kt"],
    visibility = ["//visibility:private"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/phased",
    ],
)

kt_junit5_test(
    name = "FirstPhaseTargetUtilsTest",
    size = "small",
    src = "FirstPhaseTargetUtilsTest.kt",
    tags = ["exclusive"],
    deps = [
        ":mocks",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/constants",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src:test_fixtures",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/firstPhase",
    ],
)

kt_junit5_test(
    name = "FirstPhaseTargetToBspMapperTest",
    size = "small",
    src = "FirstPhaseTargetToBspMapperTest.kt",
    tags = ["exclusive"],
    deps = [
        ":mocks",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/constants",
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
    ],
)
