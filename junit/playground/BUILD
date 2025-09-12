package(default_visibility = ["//visibility:public"]) 

java_test(
    name = "playground_tests",
    srcs = [
        "Testsy.java",
        "ConsoleMain.java",
    ],
    test_class = "org.jetbrains.bazel.junit.playground.Testsy",
    deps = [
        "@lib//:junit5",                      # JUnit 5 API
        "@lib//:junit5Jupiter",               # JUnit 5 Jupiter engine
        "@lib//:junit5Launcher",              # JUnit Platform Launcher API (used by ConsoleMain)
        "//plugins/bazel/junit:junit",       # our TeamCity TestExecutionListener
    ],
    # Force auto-discovery of TestExecutionListener (some runners may disable it by default)
    jvm_flags = [
        "-Djunit.platform.listeners.autodetection.enabled=true",
    ],
    # Keep it out of bulk test runs unless explicitly requested
    tags = ["exclusive"],
    timeout = "moderate",
    use_testrunner = False,
    main_class = "org.jetbrains.bazel.junit.playground.ConsoleMain",
)
