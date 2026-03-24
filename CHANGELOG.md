# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

#### Audio System Implementation - Complete Integration
- **Complete OpenAL-based audio system** for SE (sound effects) and BGM (background music)
- **AudioContext** - OpenAL device and context management with proper initialization/cleanup
- **AudioManager** - Central coordination singleton with source pooling (16 SFX sources + 1 BGM source)
- **AudioBuffer** - OpenAL buffer wrapper class with automatic resource management
- **AudioSource** - OpenAL source wrapper class with position, volume, and playback controls
- **AudioLoader** - OGG Vorbis file loader using STB decoder from LWJGL
- **Audio** - Resource storage singleton following SMF's pattern (similar to Textures, EntityModels)

#### Game Engine Integration
- **SMF.kt integration** - Audio system initialization in main engine startup sequence
- **Game.kt integration** - Audio updates in game loop and proper cleanup on shutdown
- **TestLevel audio controls** - Comprehensive keyboard testing interface:
  - `B` - Play BGM test file
  - `N/M` - Play SFX test files
  - `+/-` - Master volume control
  - `[/]` - BGM volume control
  - `I` - Audio system status display

#### Audio Features
- **Source pooling** - Efficient management of multiple simultaneous sound effects
- **3D audio positioning** - OpenAL listener integration with game camera
- **Volume controls** - Separate master, BGM, and SFX volume levels
- **Error handling** - Comprehensive logging and error management
- **Resource management** - Automatic cleanup and memory management
- **OGG format support** - Using STB Vorbis decoder for compressed audio

#### Documentation
- **Audio assets directory** - Created `/assets/audio/bgm/` and `/assets/audio/se/` structure
- **Usage documentation** - README.md with testing instructions and code examples
- **Git configuration** - Added line ending prevention settings to project instructions

### Changed
- **SMF.kt** - Added audio system initialization alongside other engine resources
- **Game.kt** - Integrated audio updates in game loop and cleanup sequence
- **TestLevel.kt** - Enhanced with comprehensive audio testing capabilities

### Technical Details
- **Dependencies** - Uses existing LWJGL 3.3.3 OpenAL binding (no new dependencies)
- **Audio format** - OGG Vorbis only, decoded using LWJGL's STB library
- **Memory management** - Proper BufferUtils usage for OpenAL listener calls
- **Architecture** - Follows SMF's singleton resource pattern for consistency
- **Testing** - Integrated testing interface in TestLevel with keyboard controls

### Files Added
- `src/main/kotlin/qorrnsmj/smf/audio/AudioContext.kt`
- `src/main/kotlin/qorrnsmj/smf/audio/AudioManager.kt`
- `src/main/kotlin/qorrnsmj/smf/audio/AudioBuffer.kt`
- `src/main/kotlin/qorrnsmj/smf/audio/AudioSource.kt`
- `src/main/kotlin/qorrnsmj/smf/audio/AudioLoader.kt`
- `src/main/kotlin/qorrnsmj/smf/audio/Audio.kt`
- `src/main/resources/assets/audio/README.md`
- `src/main/resources/assets/audio/bgm/` (directory)
- `src/main/resources/assets/audio/se/` (directory)

### Files Modified
- `src/main/kotlin/qorrnsmj/smf/SMF.kt` - Audio system initialization
- `src/main/kotlin/qorrnsmj/smf/core/Game.kt` - Audio updates and cleanup
- `src/main/kotlin/qorrnsmj/smf/game/level/test/TestLevel.kt` - Audio testing controls
- `.github/copilot-instructions.md` - Git configuration section