java_library(
    name = "myLib",
    my_srcs = [
      <error descr="File does not exist">"Unresolved1.kt"</error>,
      "Resolved.kt",
      <error descr="File does not exist">"Unresolved2.kt"</error>,
    ],
)
