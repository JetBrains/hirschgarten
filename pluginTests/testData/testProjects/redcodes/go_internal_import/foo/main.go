package main

import (
	"internal/gover" // Use an internal import from Go SDK, rules_go allow that

	"github.com/example/internal/lib" // Internal import from generated code
)

func main() {
	lib.Bar()
	gover.CmpInt("a", "c")
}
