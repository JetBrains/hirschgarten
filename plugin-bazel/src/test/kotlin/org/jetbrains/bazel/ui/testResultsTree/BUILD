load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/testing:ide_starter.bzl", "IDE_ID", "ide_starter_test")

ide_starter_test(
    name = "testResultsTree",
    src = "TestTargetActionResultsTreeTest.kt",
    ide_ids = [IDE_ID.IC],
    deps = [
        ":testTargetActionTreeUtil",
    ],
)

ide_starter_test(
    name = "runAllTestsActionTest",
    src = "RunAllTestsActionTest.kt",
    ide_ids = [IDE_ID.IC],
    deps = [
        ":testTargetActionTreeUtil",
    ],
)

kt_jvm_library(
    name = "testTargetActionTreeUtil",
    testonly = 1,
    srcs = ["TestTargetTreeUtil.kt"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/testFixtures/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    ],
)
