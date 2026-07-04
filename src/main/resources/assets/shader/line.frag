#version 330 core
#extension GL_ARB_separate_shader_objects : enable

in vec4 v_color;
out vec4 FragColor;

void main() {
    FragColor = v_color;
}