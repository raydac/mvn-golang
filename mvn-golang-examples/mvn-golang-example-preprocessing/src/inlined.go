//#excludeif true
//#-
package main

import (
	"bytes"
	"fmt"
	"image/png"
	"log"
)

func main() {
	//#+
	var imageArray = []uint8{/*$binfile("./image.png","uint8[]")$*/}
	var imageConfig, errDecode = png.DecodeConfig(bytes.NewBuffer(imageArray))
	if errDecode != nil {
		log.Fatal(errDecode)
	}

	fmt.Printf("Embedded image has size %dx%d\n", imageConfig.Width, imageConfig.Height)

	//#local text=str2java(evalfile("./text.txt"),false)
	fmt.Println( /*$"\""+text+"\""$*/ )
	//#-
}

//#+
