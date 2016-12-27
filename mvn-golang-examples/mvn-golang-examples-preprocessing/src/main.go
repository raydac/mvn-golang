package main

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"image/png"
	"log"
)

func main() {
	fmt.Printf("Example of preprocessing GoLang sources with Java Comment Preprocessor\n")
	fmt.Printf("Project version is : /*$mvn.project.version$*/\n")
	fmt.Printf("Build timestamp is : /*$timeStamp$*/\n\n")
	//#local counter=1
	//#while counter<=10
	fmt.Println("/*$txtMsg$*/ /*$counter$*/")
	//#local counter=counter+1
	//#end
	//#include "./inlined.go"
}
