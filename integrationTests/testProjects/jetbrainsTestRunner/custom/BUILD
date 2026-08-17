load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

kt_jvm_test(
    name = "custom_test",
    srcs = glob(
        [
            "runner/*.java",
            "*.kt",
            "*.java",
        ],
    ),
    main_class = "runner.JUnit5BazelRunner",
    tags = ["jetbrains_test_runner"],
    deps = [
        "@maven//:junit_junit",
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@maven//:org_junit_platform_junit_platform_engine",
        "@maven//:org_junit_platform_junit_platform_launcher",
    ],
)
