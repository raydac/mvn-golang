// Copyright 2014 The Azul3D Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package main

import (
	"image"
	"image/color"
	"math"
)

// Calculates and returns a mandelbrot image.
func Mandelbrot(w, h, maxIterations int, zoom, posX, posY float64) *image.RGBA {
	var (
		img                        = image.NewRGBA(image.Rect(0, 0, w, h))
		pr, pi                     float64
		newRe, newIm, oldRe, oldIm float64
		fw, fh                     = float64(w), float64(h)
	)

	for y := 0; y < h; y++ {
		for x := 0; x < w; x++ {
			fx := float64(x)
			fy := float64(y)
			pr = 1.5*(fx-fw/2)/(0.5*zoom*fw) + posX
			pi = (fy-fh/2)/(0.5*zoom*fh) + posY
			newRe = 0
			newIm = 0
			oldRe = 0
			oldIm = 0
			var i int
			for i = 0; i < maxIterations; i++ {
				oldRe = newRe
				oldIm = newIm
				newRe = oldRe*oldRe - oldIm*oldIm + pr
				newIm = 2*oldRe*oldIm + pi
				if (newRe*newRe + newIm*newIm) > 4 {
					break
				}
			}

			if i == maxIterations {
				img.Set(x, y, color.Black)
			} else {
				z := math.Sqrt(newRe*newRe + newIm*newIm)
				brightness := uint8(256.0 * math.Log2(1.75+float64(i)-math.Log2(math.Log2(z))) / math.Log2(float64(maxIterations)))
				img.Set(x, y, color.RGBA{brightness, brightness, 255, 255})
			}
		}
	}
	return img
}
