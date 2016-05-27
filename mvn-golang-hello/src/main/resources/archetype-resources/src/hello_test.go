package ${package}

import "testing"

func TestHello(t *testing.T) {
	str := Hello("xxx")
	if str != "xxx" {
		t.Fail()
	}
}
