load(":commons.bzl", "kt_test")

JUNIT5_DEPS = [
    "@maven//:org_junit_jupiter_junit_jupiter",
    "@maven//:org_junit_platform_junit_platform_console",
]

def kt_junit5_test(deps = [], **kwargs):
    kt_test(
        main_class = "org.junit.platform.console.ConsoleLauncher",
        deps = JUNIT5_DEPS + deps,
        **kwargs
    )
