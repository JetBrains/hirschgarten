java_library(
    name = "myLib",
    srcs = glob<caret>(["*.java"], exclude = ["example*.java"], allow_empty = False),
)