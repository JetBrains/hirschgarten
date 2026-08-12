package testb

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestB(t *testing.T) {
	OtherFunction()

	t.Run("basic assertion", func(t *testing.T) {
		assert.True(t, true)
	})

	t.Run("this should fail for testing sub test run", func(t *testing.T) {
		FromSourceFile()
		assert.True(t, false)
	})

	t.Run("generated file test", func(t *testing.T) {
		FromGeneratedFile()
	})
}
