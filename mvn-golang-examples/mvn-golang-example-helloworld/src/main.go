package main

import (
	"fmt"
	"sync"
	"sync/atomic"
)

var Buildstamp string
var svnRevision string

func FunctionForVetReport() {
	var a int32 = 0

	var wg sync.WaitGroup
	for i := 0; i < 500; i++ {
		wg.Add(1)
		go func() {
			a = atomic.AddInt32(&a, 1)
			wg.Done()
		}()
	}
	wg.Wait()
}

func main() {
	fmt.Printf(Buildstamp + " " + svnRevision + "\n")
}
