package a

import (
  "github.com/example/b"
  "github.com/example/c"
  <caret>"github.com/nonexistent"
)

func A() {
  b.B()
  c.C()
}
