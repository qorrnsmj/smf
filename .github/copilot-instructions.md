# Copilot Instructions for SMF Project

## Project Overview

SMF is a low-level 3D game engine built with **LWJGL (OpenGL)** and **Kotlin**. The project is inspired by ThinMatrix's tutorial series and focuses on modern rendering techniques and game engine architecture.

### Technology Stack

- **Language**: Kotlin (JVM)
- **Graphics**: LWJGL 3.3.3 (OpenGL, GLFW, STB, Assimp)
- **Audio**: OpenAL
- **Build**: Gradle with Shadow plugin
- **Target Platform**: Windows (currently)

### Architecture

The engine follows a modular architecture:

- **Core**: Game loop with fixed timestep (`FixedTimestepGame`, `Timer`)
- **Graphics**: Rendering system with shaders and effects (in `graphic/`)
  - Model loading and rendering
  - Post-processing effects (blur, etc.)
  - Shader management
- **Entity System**: Component-based entities (`Entity`, `Player`, custom entities)
  - Entity loading and management (`EntityLoader`, `EntityModels`)
- **Level Management**: Scene system (`Level`, `LevelManager`)
- **Task System**: Event-driven tasks
  - Cutscenes and sequences
  - Triggers (area-based, etc.)
- **Camera**: 3D camera system with movement and rotation

### Coding Conventions

- Use Kotlin idioms (data classes, extension functions, etc.)
- Keep OpenGL calls organized in rendering classes
- Separate game logic from rendering logic
- Use meaningful names for shaders and resources

## Branch Naming Convention

Always create a new branch before starting work. Use the following naming conventions:

- **Features**: `feat/xxx` (e.g., `feat/add-user-authentication`)
- **Bug fixes**: `fix/xxx` (e.g., `fix/memory-leak`)
- **Refactoring**: `refactor/xxx` (e.g., `refactor/optimize-renderer`)
- **Performance improvements**: `perf/xxx` (e.g., `perf/cache-implementation`)

## Commit Messages

Follow conventional commit format:

- `feat: xyz` - for new features
- `fix: xyz` - for bug fixes
- `refactor: xyz` - for refactoring
- `perf: xyz` - for performance improvements

Example: `feat: add user authentication module`

## Workflow

1. **Create a feature branch FIRST** using the naming convention above (before adding any files)
2. Implement changes with appropriate tests
3. Commit changes to the branch
4. **Ask user to review and test** the implementation
5. **Do NOT create Pull Requests automatically** - user will create PR manually after testing

## Testing

- Create test cases whenever possible
- Tests should be ready for manual verification by the user
- Ensure tests cover the main functionality

## Code Style

- **Comments**: Write in English, keep them simple and concise
- Only comment when necessary for clarity

## Documentation

- Create a simple markdown document explaining the code changes
- Include brief descriptions of:
  - What was changed
  - Why it was changed
  - How to use the new functionality (if applicable)
