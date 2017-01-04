package testexample

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestGetString(t *testing.T) {
	assert.Equal(t, "Hello String", GetString())
}

func TestGetInt(t *testing.T) {
	assert.Equal(t, 768, GetInt())
}

func TestMakeSumm(t *testing.T) {
	assert.Equal(t, 7, MakeSumm(2, 5))
}
