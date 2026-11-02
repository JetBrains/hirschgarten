load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "tests",
    testonly = True,
    srcs = glob(["*Test.kt"]),
    deps = [
        "@maven//:org_junit_jupiter_junit_jupiter",
    ],
)

kt_jvm_test(
    name = "MultiplyTest",
    args = [
        "--select-class=org.example.sharedTestLib.MultiplyTest",
        "--fail-if-no-tests",
    ],
    main_class = "org.junit.platform.console.ConsoleLauncher",
    test_class = "org.example.sharedTestLib.MultiplyTest",
    runtime_deps = [
        ":tests",
        "@maven//:org_junit_platform_junit_platform_console",
    ],
)

kt_jvm_test(
    name = "DivideTest",
    args = [
        "--select-class=org.example.sharedTestLib.DivideTest",
        "--fail-if-no-tests",
    ],
    main_class = "org.junit.platform.console.ConsoleLauncher",
    test_class = "org.example.sharedTestLib.DivideTest",
    runtime_deps = [
        ":tests",
        "@maven//:org_junit_platform_junit_platform_console",
    ],
)

kt_jvm_library(
    name = "apps",
    srcs = ["FooApp.kt"],
)

# a top-level `main` function belongs to the file facade class `FooAppKt`
kt_jvm_binary(
    name = "FooApp",
    main_class = "org.example.sharedTestLib.FooAppKt",
    runtime_deps = [":apps"],
)
