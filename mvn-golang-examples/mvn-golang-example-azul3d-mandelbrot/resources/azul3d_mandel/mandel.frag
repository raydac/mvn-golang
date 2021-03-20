#version 120

varying vec2 tc0;

uniform sampler2D Texture0;

void main()
{
	gl_FragColor = texture2D(Texture0, tc0);
}
