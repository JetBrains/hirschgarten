load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "consumer",
    srcs = glob(["src/java/org/jetbrains/bsp/example/animals/consumer/LibraryConsumer.java"]),
    deps = [":animals_lib"],
)

# This is a jvm library that doesn't have file sources. We must ensure we import it as a library, not as sources
# even if it is directly referred in projectview's targets section.
java_library(
    name = "animals_lib",
    srcs = ["//server/example/animals2:animals"],
)
