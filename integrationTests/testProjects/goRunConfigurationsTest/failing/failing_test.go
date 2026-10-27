package failing

import "testing"

func TestPasses(t *testing.T) {}

func TestFails(t *testing.T) {
	t.Fail()
}

func TestAnotherFails(t *testing.T) {
	t.Fail()
}
