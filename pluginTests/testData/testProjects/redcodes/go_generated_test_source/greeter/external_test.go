package greeter_test

import (
	"testing"

	"example.com/greeter"
)

type echoGreeter struct{}

func (echoGreeter) Greet(name string) string {
	return name
}

func TestWelcome(t *testing.T) {
	if got := greeter.Welcome(echoGreeter{}, "world"); got != "Hello, world!" {
		t.Fatalf("Welcome() = %q", got)
	}
}
