#version 330 core

//uniform sampler2D texImage;

layout(location = 0) in vec4 vertexColor;
//layout(location = 1) in vec2 textureCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    //fragColor = vertexColor * texture(texImage, textureCoord);
    fragColor = vertexColor;
}
