package main

import (
	"example.com/lib_a/lib"
	lib_b "example.com/lib_b/lib"
)

func Main() {
	lib.A()
	lib_b.B()

	lib.<error>Missing</error>()
	lib_b.<error>Missing</error>()
}
