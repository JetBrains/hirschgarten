java_library(
    name = "myLib",
    srcs = glob<caret>(["**/*.nonexistent"]),
)