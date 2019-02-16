package mvngotest

import (
	meta "com/igormaznitsa/mvngotestmeta"
	"fmt"
)

var Buildstamp string
var svnRevision string

func SomeTestMethod() {
	fmt.Printf(meta.SomeMetaMethod("mvn-go-test-lib module") + "!\n")
}
