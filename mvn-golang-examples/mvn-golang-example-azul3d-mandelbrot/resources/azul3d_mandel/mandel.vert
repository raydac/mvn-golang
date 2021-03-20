#version 120

attribute vec3 Vertex;
attribute vec2 TexCoord0;

varying vec2 tc0;

void main()
{
	tc0 = TexCoord0;
	gl_Position = vec4(Vertex, 1.0);
}
