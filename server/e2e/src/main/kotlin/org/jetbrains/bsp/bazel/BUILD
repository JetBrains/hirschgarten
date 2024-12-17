load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(
    name = "BazelBspSampleRepoTest",
    srcs = ["BazelBspSampleRepoTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspSampleRepoTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspLocalJdkTest",
    srcs = ["BazelBspLocalJdkTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspLocalJdkTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspRemoteJdkTest",
    srcs = ["BazelBspRemoteJdkTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspRemoteJdkTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspCppProjectTest",
    srcs = ["BazelBspCppProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspCppProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspPythonProjectTest",
    srcs = ["BazelBspPythonProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspPythonProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspScalaProjectTest",
    srcs = ["BazelBspScalaProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspScalaProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspKotlinProjectTest",
    srcs = ["BazelBspKotlinProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspKotlinProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "ServerDownloadsBazeliskTest",
    srcs = ["ServerDownloadsBazeliskTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.ServerDownloadsBazeliskTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspRustProjectTest",
    srcs = ["BazelBspRustProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspRustProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspAndroidProjectTest",
    srcs = ["BazelBspAndroidProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspAndroidProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/android",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspAndroidKotlinProjectTest",
    srcs = ["BazelBspAndroidKotlinProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspAndroidKotlinProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/android",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspGoProjectTest",
    srcs = ["BazelBspGoProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspGoProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "JavaDiagnosticsTest",
    srcs = ["JavaDiagnosticsTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.JavaDiagnosticsTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspAllowManualTargetsSyncTest",
    srcs = ["BazelBspAllowManualTargetsSyncTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspAllowManualTargetsSyncTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspPartialSyncTest",
    srcs = ["BazelBspPartialSyncTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspPartialSyncTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspBuildAndSyncTest",
    srcs = ["BazelBspBuildAndSyncTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspBuildAndSyncTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspFirstPhaseSyncTest",
    srcs = ["BazelBspFirstPhaseSyncTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspFirstPhaseSyncTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
