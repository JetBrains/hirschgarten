load("@rules_java//java:defs.bzl", "java_library", "java_binary")

java_library(
    name = "internal",
    srcs = ["Internal.java"],
)

java_library(
    name = "dontlook",
    tags = ["no-ide"],
    srcs = ["Heavy.java"],
    deps =  [":internal"],
)

java_binary(
    name = "main",
    srcs = ["Main.java"],
    main_class = "com.example.project.Main",
    deps = [":dontlook"],
)