java_library(
    name = "myLib",
    my_srcs = [
      <error descr="File does not exist">"Unresolved1.java"</error>,
      <error descr="File does not exist">"Unresolved2.java"</error>,
    ],
)
