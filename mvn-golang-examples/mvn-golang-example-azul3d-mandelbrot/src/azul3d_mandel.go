// Copyright 2014 The Azul3D Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// Example - Generates a mandelbrot on the CPU and displays it with the GPU.
package main

import (
	"image"
	"image/color"
	"image/png"
	"log"
	"math"
	"os"

	"azul3d.org/engine/gfx"
	"azul3d.org/engine/gfx/gfxutil"
	"azul3d.org/engine/gfx/window"
	"azul3d.org/engine/keyboard"
	"azul3d.org/engine/mouse"

	"azul3d.org/examples/abs"
)

// mandelGen is a mandelbrot texture generator.
type mandelGen struct {
	// The channel of generated mandelbrot images. Only the last one received
	// over this channel is valid.
	Image chan image.Image

	x, y       float64         // Center, e.g. x=-0.5, y=0.
	zoom       float64         // Zoom, e.g. 1.0.
	resolution int             // Resolution divisor, e.g. 8.
	maxIter    int             // Maximum number of generator iterations, e.g. 1000.
	needUpdate bool            // Whether or not we should generate an updated image.
	bounds     image.Rectangle // The framebuffer's bounding rectangle.
}

// handle is called to handle a window event.
func (m *mandelGen) handle(e window.Event) {
	switch ev := e.(type) {
	case window.FramebufferResized:
		m.bounds = image.Rect(0, 0, ev.Width, ev.Height)

	case mouse.ButtonEvent:
		if ev.Button == mouse.Right && ev.State == mouse.Down {
			m.resolution += 2
			if m.resolution > 8 {
				m.resolution = 4
			}
			m.needUpdate = true
		}

	case mouse.Scrolled:
		m.zoom += ev.Y * 0.06 * math.Abs(m.zoom)
		m.needUpdate = true

	case window.CursorMoved:
		if ev.Delta {
			m.x += (ev.X / 900.0) / math.Abs(m.zoom)
			m.y += (ev.Y / 900.0) / math.Abs(m.zoom)
			m.needUpdate = true
		}
	}
}

// generate is a small helper function to call Mandelbrot and insert a small
// red square into the image. It then submits the image over the channel.
func (m *mandelGen) generate() {
	width := m.bounds.Dx() / m.resolution
	height := m.bounds.Dy() / m.resolution
	mbrot := Mandelbrot(width, height, m.maxIter, m.zoom, m.x, m.y)

	// Insert a small red square in the top-left of the image for ensuring
	// proper orientation exists in textures (this is just for testing).
	for x := 0; x < 5; x++ {
		for y := 0; y < 5; y++ {
			mbrot.Set(x, y, color.RGBA{255, 0, 0, 255})
		}
	}
	m.Image <- mbrot
}

// run is the mandelbrot generation goroutine. It listens for window events and
// responds by generating new mandelbrot images if needed.
func (m *mandelGen) run(w window.Window) {
	// generate the initial mandelbrot image right now.
	m.generate()

	// Have the window notify us of specific events.
	evMask := window.MouseEvents
	evMask |= window.MouseScrolledEvents
	evMask |= window.FramebufferResizedEvents
	evMask |= window.CursorMovedEvents
	event := make(chan window.Event, 256)
	w.Notify(event, evMask)

	// Wait for window events.
	for {
		// Wait for one window event now.
		m.handle(<-event)

		// Handle as many window events as possible now. We do this because
		// generating mandelbrot images on the CPU is expensive, so we only
		// want to do it for the very last event.
		window.Poll(event, m.handle)

		// If we need to update, generate the next mandelbrot image.
		if m.needUpdate {
			m.needUpdate = false
			m.generate()
		}
	}
}

// newMandelGen returns a new mandelbrot generator.
func newMandelGen(w window.Window, d gfx.Device) *mandelGen {
	m := &mandelGen{
		Image:      make(chan image.Image),
		x:          -.5,
		y:          0,
		zoom:       1,
		resolution: 8,
		maxIter:    1000,
		bounds:     d.Bounds(),
	}

	// Spawn the mandelbrot generation goroutine.
	go m.run(w)
	return m
}

// gfxLoop is responsible for drawing things to the window.
func gfxLoop(w window.Window, d gfx.Device) {
	// Create a new mandelbrot generator.
	gen := newMandelGen(w, d)

	// Read the GLSL shaders from disk.
	shader, err := gfxutil.OpenShader(abs.Path("azul3d_mandel/mandel"))
	if err != nil {
		log.Fatal(err)
	}

	// Create a card mesh.
	cardMesh := gfx.NewMesh()
	cardMesh.Vertices = []gfx.Vec3{
		// Left triangle.
		{-1, 1, 0},  // Left-Top
		{-1, -1, 0}, // Left-Bottom
		{1, -1, 0},  // Right-Bottom

		// Right triangle.
		{-1, 1, 0}, // Left-Top
		{1, -1, 0}, // Right-Bottom
		{1, 1, 0},  // Right-Top
	}
	cardMesh.TexCoords = []gfx.TexCoordSet{
		{
			Slice: []gfx.TexCoord{
				// Left triangle.
				{0, 0},
				{0, 1},
				{1, 1},

				// Right triangle.
				{0, 0},
				{1, 1},
				{1, 0},
			},
		},
	}

	// Create the texture that will display the image. We could use DXT texture
	// compression normally here, but because we want to download the texture
	// back to the CPU we must not.
	tex := gfx.NewTexture()
	tex.MinFilter = gfx.Nearest
	tex.MagFilter = gfx.Nearest
	tex.KeepDataOnLoad = true
	tex.Source = <-gen.Image // Wait for the first image to be generated.

	// Create a card object.
	card := gfx.NewObject()
	card.State = gfx.NewState()
	card.Shader = shader
	card.Textures = []*gfx.Texture{tex}
	card.Meshes = []*gfx.Mesh{cardMesh}

	// Get notified of mouse events and keyboard typing events.
	events := make(chan window.Event, 1)
	w.Notify(events, window.MouseEvents|window.KeyboardTypedEvents)

	for {
		// Handle each pending event.
		window.Poll(events, func(e window.Event) {
			switch ev := e.(type) {
			case mouse.ButtonEvent:
				// Toggle mouse grab when the user left clicks.
				if ev.Button == mouse.Left && ev.State == mouse.Down {
					props := w.Props()
					props.SetCursorGrabbed(!props.CursorGrabbed())
					w.Request(props)
				}

			case keyboard.Typed:
				if ev.S == "s" || ev.S == "S" {
					log.Println("Writing texture to file...")

					// Download the texture image from the graphics hardware
					// and save it to disk.
					complete := make(chan image.Image, 1)

					// Begin the texture download.
					card.Textures[0].Download(card.Textures[0].Bounds, complete)

					// The download can occur in any goroutine!
					go func() {
						img := <-complete // Wait for download to complete.
						if img == nil {
							log.Println("Failed to download texture.")
						} else {
							// Save to png.
							f, err := os.Create("mandel.png")
							if err != nil {
								log.Fatal(err)
							}
							err = png.Encode(f, img)
							if err != nil {
								log.Fatal(err)
							}
							log.Println("Wrote texture to mandel.png")
						}
					}()
				}
			}
		})

		// If the mandelbrot generator has a new image for us, update the
		// texture.
		select {
		case img := <-gen.Image:
			card.Textures[0].Loaded = false
			card.Textures[0].Source = img
			card.Textures[0].Bounds = img.Bounds()
		default:
			break
		}

		// Clear color and depth buffers.
		d.Clear(d.Bounds(), gfx.Color{1, 1, 1, 1})
		d.ClearDepth(d.Bounds(), 1.0)

		// Draw the card to the screen.
		d.Draw(d.Bounds(), card, nil)

		// Render the whole frame.
		d.Render()
	}
}

func main() {
	window.Run(gfxLoop, nil)
}
