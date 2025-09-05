load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")

kt_jvm_library(
    name = "BazelTestRunnerKt",
    srcs = ["BazelTestRunner.kt"],
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

java_binary(
    name = "BazelTestRunner",
    main_class = "org.jetbrains.bazel.BazelTestRunner",
    visibility = ["//server/e2e:__subpackages__"],
    runtime_deps = [
        ":BazelTestRunnerKt",
    ],
)

kt_jvm_library(
    name = "BazelTestRunnerWithDiagnosticsKt",
    srcs = ["BazelTestRunnerWithDiagnostics.kt"],
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

java_binary(
    name = "BazelTestRunnerWithDiagnostics",
    main_class = "org.jetbrains.bazel.BazelTestRunnerWithDiagnostics",
    visibility = ["//server/e2e:__subpackages__"],
    runtime_deps = [
        ":BazelTestRunnerWithDiagnosticsKt",
    ],
)
