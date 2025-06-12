load(":commons.bzl", "kt_test")
load(":intellij.bzl", "ADD_OPENS_FLAGS", "INTELLIJ_DEPS", "INTELLIJ_RUNTIME_DEPS", "JVM_FLAGS")

JUNIT5_DEPS = [
    "@maven//:org_junit_jupiter_junit_jupiter_api",
    "@maven//:org_junit_jupiter_junit_jupiter",
    "@maven//:org_junit_jupiter_junit_jupiter_params",
    "@maven//:org_junit_platform_junit_platform_console",
    "@maven//:org_mockito_mockito_core",
]

def kt_junit5_test(deps = [], runtime_deps = [], jvm_flags = [], **kwargs):
    kt_test(
        main_class = "org.junit.platform.console.ConsoleLauncher",
        deps = JUNIT5_DEPS + INTELLIJ_DEPS + deps,
        jvm_flags = JVM_FLAGS + ADD_OPENS_FLAGS + jvm_flags,
        runtime_deps = INTELLIJ_RUNTIME_DEPS + runtime_deps,
        **kwargs
    )
