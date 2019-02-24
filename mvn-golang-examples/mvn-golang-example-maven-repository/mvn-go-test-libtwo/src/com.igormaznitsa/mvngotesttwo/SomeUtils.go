package mvngotesttwo

import (
	meta "com.igormaznitsa/mvngotestmeta"
	"fmt"
)

var Buildstamp string
var svnRevision string

func SomeTestMethodTwo() {
	fmt.Printf(meta.SomeMetaMethod("mvn-go-test-libtwo module") + "!\n")
}
