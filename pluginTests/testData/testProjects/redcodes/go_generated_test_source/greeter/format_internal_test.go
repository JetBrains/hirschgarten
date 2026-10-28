package greeter

import "testing"

type fixedGreeter string

func (g fixedGreeter) Greet(string) string {
	return string(g)
}

func TestWelcomeFormat(t *testing.T) {
	if got := Welcome(fixedGreeter("x"), "y"); got != "Hello, x!" {
		t.Fatalf("Welcome() = %q", got)
	}
}
