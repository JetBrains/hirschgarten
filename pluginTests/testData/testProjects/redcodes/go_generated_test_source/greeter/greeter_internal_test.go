package greeter

import "testing"

func TestWelcomeWithMock(t *testing.T) {
	if got := Welcome(NewMockGreeter(), "world"); got != "Hello, world!" {
		t.Fatalf("Welcome() = %q", got)
	}
}
