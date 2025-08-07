java_library(
    name = "myLib",
    my_srcs = glob([<error descr="Glob pattern does not match any files">"**/*.nonexistent"</error>, "*.java"]),
)