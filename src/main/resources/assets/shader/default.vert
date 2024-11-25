#version 330 core

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec3 tangent;

layout(location = 0) out vec4 outVertexColor;
layout(location = 1) out vec2 outTexCoord;
layout(location = 2) out vec3 outTangent;
layout(location = 3) out vec3 worldPosition;
layout(location = 4) out vec3 worldNormal;
layout(location = 5) out vec3 viewPosition;

void main() {
    gl_Position = projection * view * model * vec4(position, 1.0);

    outVertexColor = vec4(1.0, 1.0, 1.0, 1.0);
    outTexCoord = texCoord;
    outTangent = tangent;
    worldPosition = vec3(model * vec4(position, 1.0));
    worldNormal = mat3(transpose(inverse(model))) * normal;
    viewPosition = vec3(inverse(view)[3]);
}
