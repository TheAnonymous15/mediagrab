# MediaGrab - Complete Architecture Documentation
## Professional-Grade Multimedia Platform for Android

**Version:** 1.0.0  
**Last Updated:** January 5, 2026  
**Platform:** Android (API 24+)  
**Architecture Pattern:** MVVM + Clean Architecture  

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Architecture](#system-architecture)
3. [Module Deep Dive](#module-deep-dive)
4. [Data Architecture](#data-architecture)
5. [Audio Processing Pipeline](#audio-processing-pipeline)
6. [Real-Time Communication](#real-time-communication)
7. [UI/UX Architecture](#uiux-architecture)
8. [Flow Diagrams](#flow-diagrams)
9. [State Management](#state-management)
10. [Integration Architecture](#integration-architecture)
11. [Security Architecture](#security-architecture)
12. [Performance Architecture](#performance-architecture)
13. [Testing Strategy](#testing-strategy)
14. [Deployment Architecture](#deployment-architecture)
15. [Future Roadmap](#future-roadmap)
16. [Appendices](#appendices)

---

## Executive Summary

### Product Vision

MediaGrab is an all-in-one multimedia platform that transforms how users consume, create, and share audio-visual content. Unlike fragmented solutions that require multiple apps, MediaGrab provides a unified ecosystem encompassing:

- **Content Acquisition** - Download media from 1000+ platforms
- **Professional Playback** - Studio-grade audio with 31-band EQ and DSP
- **Content Creation** - Podcast studio, DJ mixing, beat production
- **Social Audio** - Real-time collaborative listening rooms
- **Artist Platform** - Music discovery and promotion ecosystem

### Key Differentiators

| Feature | MediaGrab | Competitors |
|---------|-----------|-------------|
| Integrated Download + Playback | ✅ Seamless | ❌ Separate apps |
| Professional Audio DSP | ✅ 14-system EQ/FX | ❌ Basic EQ |
| Podcast Production | ✅ Full studio | ❌ Recording only |
| DJ Capabilities | ✅ Dual-deck mixing | ❌ Not available |
| Social Audio Rooms | ✅ Built-in | ❌ Separate platform |
| Artist Ecosystem | ✅ Integrated | ❌ Not available |

### Target Users

1. **Content Consumers** - Users who download and listen to media
2. **Audio Enthusiasts** - Users who want professional sound quality
3. **Content Creators** - Podcasters, musicians, DJs
4. **Social Listeners** - Users who enjoy shared listening experiences
5. **Independent Artists** - Musicians seeking promotion platform

---

## System Architecture

### High-Level System Overview

MediaGrab follows a layered architecture pattern with clear separation of concerns across four primary layers:

#### Layer 1: Presentation Layer
**Purpose:** User interface rendering and user interaction handling

**Components:**
- Jetpack Compose UI screens (20+ screens)
- Reusable UI components library
- Theme system with dark/light modes
- Animation and transition controllers
- Gesture handlers for DJ/Beat Maker interfaces

**Responsibilities:**
- Render reactive UI based on state changes
- Handle user input and gestures
- Navigate between screens
- Display real-time visualizations (waveforms, meters)
- Manage UI-specific state (dialogs, selections)

#### Layer 2: Domain Layer
**Purpose:** Business logic and feature orchestration

**Components:**
- Feature managers (DownloadManager, AudioPlayerManager, etc.)
- Use case implementations
- Business rule enforcement
- Cross-feature coordination

**Responsibilities:**
- Implement core business logic
- Coordinate between data and presentation layers
- Manage feature-specific state
- Handle background processing
- Enforce business rules and constraints

#### Layer 3: Data Layer
**Purpose:** Data persistence and external data access

**Components:**
- Room database with DAOs
- SharedPreferences for settings
- File system managers
- Network clients (yt-dlp, OkHttp)
- Content providers integration

**Responsibilities:**
- Persist application data
- Cache frequently accessed data
- Provide reactive data streams
- Handle data migrations
- Manage offline capabilities

#### Layer 4: Platform Layer
**Purpose:** Android platform services and native integrations

**Components:**
- MediaPlayer/ExoPlayer wrappers
- AudioFX system integration
- CameraX integration
- LiveKit SDK integration
- System services (AudioManager, MediaStore)

**Responsibilities:**
- Interface with Android APIs
- Manage hardware resources
- Handle platform-specific behaviors
- Provide native functionality access

### System Architecture Diagram

```
╔═══════════════════════════════════════════════════════════════════════════════════════╗
║                                   MEDIAGRAB SYSTEM                                     ║
╠═══════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                        ║
║  ┌─────────────────────────────────────────────────────────────────────────────────┐  ║
║  │                            PRESENTATION LAYER                                    │  ║
║  │  ╔═══════════════════════════════════════════════════════════════════════════╗  │  ║
║  │  ║                         JETPACK COMPOSE UI                                 ║  │  ║
║  │  ║  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ║  │  ║
║  │  ║  │MainHub  │ │Downloads│ │Playlists│ │Equalizer│ │ Podcast │ │   DJ    │ ║  │  ║
║  │  ║  │ Screen  │ │ Screen  │ │ Screen  │ │ Screen  │ │ Studio  │ │ Studio  │ ║  │  ║
║  │  ║  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ ║  │  ║
║  │  ║       │           │           │           │           │           │       ║  │  ║
║  │  ║  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ║  │  ║
║  │  ║  │BeatMaker│ │  Arena  │ │  Audio  │ │  Radio  │ │  Vault  │ │ Context │ ║  │  ║
║  │  ║  │ Screen  │ │ Screen  │ │ Social  │ │ Screen  │ │ Screen  │ │  Mode   │ ║  │  ║
║  │  ║  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ ║  │  ║
║  │  ╚═══════╪═══════════╪═══════════╪═══════════╪═══════════╪═══════════╪═══════╝  │  ║
║  └──────────┼───────────┼───────────┼───────────┼───────────┼───────────┼──────────┘  ║
║             │           │           │           │           │           │             ║
║             ▼           ▼           ▼           ▼           ▼           ▼             ║
║  ┌─────────────────────────────────────────────────────────────────────────────────┐  ║
║  │                              DOMAIN LAYER                                        │  ║
║  │  ╔════════════════════════════════════════════════════════════════════════════╗ │  ║
║  │  ║                           FEATURE MANAGERS                                  ║ │  ║
║  │  ║                                                                             ║ │  ║
║  │  ║  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐                   ║ │  ║
║  │  ║  │DownloadManager │ │AudioPlayerMgr  │ │ SuperEqualizer │                   ║ │  ║
║  │  ║  │                │ │                │ │                │                   ║ │  ║
║  │  ║  │• Queue Mgmt    │ │• Playback Ctrl │ │• 14 DSP Systems│                   ║ │  ║
║  │  ║  │• Concurrent DL │ │• Queue Mgmt    │ │• Preset Mgmt   │                   ║ │  ║
║  │  ║  │• Progress Track│ │• Audio Focus   │ │• Real-time FX  │                   ║ │  ║
║  │  ║  └────────────────┘ └────────────────┘ └────────────────┘                   ║ │  ║
║  │  ║                                                                             ║ │  ║
║  │  ║  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐                   ║ │  ║
║  │  ║  │ PodcastManager │ │  DJEnginePro   │ │BeatMakerEngine │                   ║ │  ║
║  │  ║  │                │ │                │ │                │                   ║ │  ║
║  │  ║  │• Recording     │ │• Dual Decks    │ │• Sequencer     │                   ║ │  ║
║  │  ║  │• AI Processing │ │• Mixing        │ │• Instruments   │                   ║ │  ║
║  │  ║  │• Export        │ │• Scratching    │ │• Pattern Chain │                   ║ │  ║
║  │  ║  └────────────────┘ └────────────────┘ └────────────────┘                   ║ │  ║
║  │  ║                                                                             ║ │  ║
║  │  ║  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐                   ║ │  ║
║  │  ║  │AudioSocialMgr  │ │  VaultManager  │ │  RadioManager  │                   ║ │  ║
║  │  ║  │                │ │                │ │                │                   ║ │  ║
║  │  ║  │• Room Mgmt     │ │• Indexing      │ │• Streaming     │                   ║ │  ║
║  │  ║  │• Co-Listening  │ │• Search        │ │• Favorites     │                   ║ │  ║
║  │  ║  │• Voice Chat    │ │• Collections   │ │• History       │                   ║ │  ║
║  │  ║  └────────────────┘ └────────────────┘ └────────────────┘                   ║ │  ║
║  │  ║                                                                             ║ │  ║
║  │  ║  ┌────────────────┐ ┌────────────────┐                                      ║ │  ║
║  │  ║  │ArenaRepository │ │ContextMediaMgr │                                      ║ │  ║
║  │  ║  │                │ │                │                                      ║ │  ║
║  │  ║  │• Artist Mgmt   │ │• Mode Detection│                                      ║ │  ║
║  │  ║  │• Track Mgmt    │ │• Auto Switching│                                      ║ │  ║
║  │  ║  │• Analytics     │ │• Device Aware  │                                      ║ │  ║
║  │  ║  └────────────────┘ └────────────────┘                                      ║ │  ║
║  │  ╚════════════════════════════════════════════════════════════════════════════╝ │  ║
║  └─────────────────────────────────────────────────────────────────────────────────┘  ║
║             │                                                         │               ║
║             ▼                                                         ▼               ║
║  ┌─────────────────────────────────────────────────────────────────────────────────┐  ║
║  │                               DATA LAYER                                         │  ║
║  │  ╔════════════════════════════════════════════════════════════════════════════╗ │  ║
║  │  ║  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       ║ │  ║
║  │  ║  │DownloadDB    │ │DownloadDao   │ │PlayStatsDao  │ │SettingsMgr   │       ║ │  ║
║  │  ║  │(Room)        │ │              │ │              │ │(SharedPrefs) │       ║ │  ║
║  │  ║  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘       ║ │  ║
║  │  ║  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                        ║ │  ║
║  │  ║  │DeviceMedia   │ │NetworkMonitor│ │ FileManager  │                        ║ │  ║
║  │  ║  │Scanner       │ │              │ │              │                        ║ │  ║
║  │  ║  └──────────────┘ └──────────────┘ └──────────────┘                        ║ │  ║
║  │  ╚════════════════════════════════════════════════════════════════════════════╝ │  ║
║  └─────────────────────────────────────────────────────────────────────────────────┘  ║
║             │                                                         │               ║
║             ▼                                                         ▼               ║
║  ┌─────────────────────────────────────────────────────────────────────────────────┐  ║
║  │                             PLATFORM LAYER                                       │  ║
║  │  ╔════════════════════════════════════════════════════════════════════════════╗ │  ║
║  │  ║  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       ║ │  ║
║  │  ║  │ MediaPlayer  │ │  AudioFX     │ │   CameraX    │ │   LiveKit    │       ║ │  ║
║  │  ║  │ / ExoPlayer  │ │  System      │ │              │ │   SDK        │       ║ │  ║
║  │  ║  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘       ║ │  ║
║  │  ║  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       ║ │  ║
║  │  ║  │ AudioManager │ │ MediaStore   │ │  yt-dlp      │ │ RootEncoder  │       ║ │  ║
║  │  ║  │              │ │              │ │  (Native)    │ │  (RTMP)      │       ║ │  ║
║  │  ║  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘       ║ │  ║
║  │  ╚════════════════════════════════════════════════════════════════════════════╝ │  ║
║  └─────────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                        ║
╚═══════════════════════════════════════════════════════════════════════════════════════╝
```

### Architectural Patterns in Use

#### 1. MVVM (Model-View-ViewModel)
- **Model:** Data classes and repository layer
- **View:** Composable functions
- **ViewModel:** StateFlow-based state holders

#### 2. Repository Pattern
- Single source of truth for data
- Abstraction over data sources
- Caching strategies implementation

#### 3. Observer Pattern
- Kotlin StateFlow for reactive state
- Flow for async data streams
- Compose recomposition triggers

#### 4. Singleton Pattern
- Manager classes (DownloadManager, AudioPlayerManager)
- Database instance
- Shared preferences manager

#### 5. Factory Pattern
- Media player creation
- Audio effect instantiation
- Preset generation

#### 6. Strategy Pattern
- Download strategies (WiFi-only, any network)
- Audio processing chains
- Export format handlers

### Module Dependency Graph

```
                            ┌─────────────────────────────────────────────┐
                            │              MainActivity                    │
                            │         (Navigation & Entry Point)          │
                            └──────────────────┬──────────────────────────┘
                                               │
                 ┌─────────────────────────────┼─────────────────────────────┐
                 │                             │                             │
                 ▼                             ▼                             ▼
    ┌────────────────────┐      ┌────────────────────┐      ┌────────────────────┐
    │    UI SCREENS      │      │   UI SCREENS       │      │    UI SCREENS      │
    │                    │      │                    │      │                    │
    │ • MainHubScreen    │      │ • PodcastScreen    │      │ • ArenaScreens     │
    │ • DownloadsScreen  │      │ • DJStudioScreen   │      │ • AudioSocialScreen│
    │ • PlaylistsScreen  │      │ • BeatMakerScreen  │      │ • RadioScreen      │
    │ • EqualizerScreen  │      │ • RemixStudioScreen│      │ • VaultScreen      │
    └─────────┬──────────┘      └─────────┬──────────┘      └─────────┬──────────┘
              │                           │                           │
              │                           │                           │
              ▼                           ▼                           ▼
    ┌────────────────────┐      ┌────────────────────┐      ┌────────────────────┐
    │  DOMAIN MANAGERS   │      │  DOMAIN MANAGERS   │      │  DOMAIN MANAGERS   │
    │                    │      │                    │      │                    │
    │ • DownloadManager  │◄────►│ • PodcastManager   │◄────►│ • ArenaRepository  │
    │ • AudioPlayerMgr   │      │ • DJEnginePro      │      │ • AudioSocialMgr   │
    │ • SuperEqualizer   │      │ • BeatMakerEngine  │      │ • RadioManager     │
    └─────────┬──────────┘      └─────────┬──────────┘      └─────────┬──────────┘
              │                           │                           │
              │                           │                           │
              └───────────────────────────┼───────────────────────────┘
                                          │
                                          ▼
                            ┌─────────────────────────────┐
                            │        DATA LAYER           │
                            │                             │
                            │ • DownloadDatabase (Room)   │
                            │ • DownloadDao               │
                            │ • MediaPlayStatsDao         │
                            │ • SettingsManager           │
                            │ • DeviceMediaScanner        │
                            │ • NetworkMonitor            │
                            └──────────────┬──────────────┘
                                           │
                                           ▼
                            ┌─────────────────────────────┐
                            │      PLATFORM LAYER         │
                            │                             │
                            │ • MediaPlayer / ExoPlayer   │
                            │ • Android AudioFX           │
                            │ • CameraX                   │
                            │ • LiveKit SDK               │
                            │ • yt-dlp Native Library     │
                            │ • RootEncoder               │
                            └─────────────────────────────┘
```

### Cross-Module Communication Matrix

```
                        │ Download │ Player │ Equalizer │ Podcast │ DJ │ Beat │ Social │ Arena │ Radio │ Vault │ Context │
────────────────────────┼──────────┼────────┼───────────┼─────────┼────┼──────┼────────┼───────┼───────┼───────┼─────────┤
Download Manager        │    -     │   ►    │     -     │    -    │ -  │  -   │   -    │   -   │   -   │   ►   │    -    │
Audio Player Manager    │    ◄     │   -    │     ►     │    ◄    │ -  │  -   │   ►    │   ◄   │   ◄   │   ►   │    ►    │
Super Equalizer         │    -     │   ◄    │     -     │    ◄    │ ◄  │  ◄   │   ◄    │   -   │   ◄   │   -   │    ◄    │
Podcast Manager         │    -     │   ►    │     ►     │    -    │ -  │  ◄   │   -    │   -   │   -   │   ►   │    -    │
DJ Engine              │    -     │   -    │     ►     │    -    │ -  │  -   │   -    │   -   │   -   │   ◄   │    -    │
Beat Maker Engine      │    -     │   -    │     ►     │    ►    │ -  │  -   │   -    │   ►   │   -   │   ◄   │    -    │
Audio Social Manager   │    -     │   ◄    │     ►     │    -    │ -  │  -   │   -    │   -   │   -   │   -   │    -    │
Arena Repository       │    -     │   ►    │     -     │    -    │ -  │  ◄   │   -    │   -   │   ►   │   -   │    -    │
Radio Manager          │    -     │   ►    │     ►     │    -    │ -  │  -   │   -    │   ◄   │   -   │   ►   │    ►    │
Vault Manager          │    ◄     │   ◄    │     -     │    ◄    │ ►  │  ►   │   -    │   -   │   ◄   │   -   │    -    │
Context Media Manager  │    -     │   ◄    │     ►     │    -    │ -  │  -   │   -    │   -   │   ◄   │   -   │    -    │

Legend: ► = Sends data/commands to    ◄ = Receives data/commands from    - = No direct communication
```

---

## Module Deep Dive

### Module 1: Download Engine

**Location:** `com.example.dwn.download`

**Purpose:** High-performance media downloading from 1000+ online platforms with queue management, concurrent downloads, and network-aware operation.

**Architecture:**

The download engine operates as a state machine with the following states:
- IDLE → QUEUED → CHECKING → DOWNLOADING → POST_PROCESSING → COMPLETED
- Error paths: Any state → FAILED → RETRY → QUEUED
- User actions: Any active state → PAUSED → RESUMED → Previous state

**Key Capabilities:**

1. **Multi-Platform Support**
   - YouTube (videos, shorts, playlists, channels)
   - Twitter/X (videos, spaces recordings)
   - Instagram (reels, stories, posts)
   - TikTok (videos, sounds)
   - Facebook (videos, reels)
   - SoundCloud (tracks, playlists)
   - 1000+ additional platforms via yt-dlp

2. **Queue Management System**
   - Maximum 3 concurrent downloads (configurable)
   - Priority queue support
   - Automatic retry on failure (3 attempts)
   - Resume capability for interrupted downloads
   - Playlist batch processing

3. **Network Intelligence**
   - WiFi-only mode with automatic pause on cellular
   - Network change detection and auto-resume
   - Bandwidth estimation for progress prediction
   - Connection quality monitoring

4. **Format Handling**
   - Audio extraction to MP3 (128kbps, 192kbps, 320kbps)
   - Video download to MP4 (360p, 480p, 720p, 1080p, 4K)
   - Automatic format selection based on user preference
   - FFmpeg-based post-processing for format conversion

**Data Flow:**

User Input → URL Validation → Metadata Extraction → Queue Addition → Network Check → Download Execution → Post-Processing → Database Update → UI Notification

**Error Handling Strategy:**
- Network errors: Automatic retry with exponential backoff
- Format errors: Fallback to alternative format
- Storage errors: User notification with cleanup option
- Platform errors: Detailed error message with troubleshooting

---

### Module 2: Audio Player Engine

**Location:** `com.example.dwn.player`

**Purpose:** Professional-grade audio playback with comprehensive DSP, queue management, and seamless system integration.

**Architecture:**

The player operates on three concurrent threads:
1. **UI Thread** - State updates, user interaction
2. **Playback Thread** - MediaPlayer control, position tracking
3. **DSP Thread** - Real-time audio effect processing

**Subsystems:**

#### 2.1 Core Playback Engine
- MediaPlayer-based playback for local files
- ExoPlayer integration for streaming content
- Gapless playback support
- Crossfade transitions (0-12 seconds)
- Playback speed control (0.5x - 3.0x)
- Pitch adjustment without tempo change

#### 2.2 Queue Management
- Unlimited queue size
- Shuffle with intelligent algorithm (avoids same artist consecutively)
- Repeat modes: Off, One, All
- Queue persistence across app restarts
- Drag-and-drop reordering
- Multi-select operations

#### 2.3 Audio Focus Management
- Proper audio focus request/release
- Duck audio on notifications
- Pause on phone calls
- Resume after transient interruptions
- Bluetooth disconnect handling (auto-pause)
- Volume zero detection (auto-pause/resume)

#### 2.4 Media Session Integration
- Lock screen controls
- Notification media controls
- Bluetooth AVRCP support
- Android Auto compatibility
- Google Assistant voice control

#### 2.5 Audio Output Management
- Real-time device detection
- Automatic routing on connection changes
- Device-specific audio profiles
- Bluetooth codec detection (SBC, AAC, aptX, LDAC)
- USB audio device support

**State Machine:**

IDLE → LOADING → PREPARED → PLAYING ⟷ PAUSED → STOPPED
                    ↓
              SEEKING → PLAYING
                    ↓
              BUFFERING → PLAYING (streaming only)

---

### Module 3: Super Equalizer & FX System

**Location:** `com.example.dwn.player.audio`

**Purpose:** Studio-grade audio processing with 14 integrated subsystems providing professional sound manipulation capabilities.

**System Architecture:**

The Super Equalizer implements a modular DSP chain architecture where each processing module can be independently enabled, configured, and reordered.

**Processing Chain Order:**
Input → Preamp → Graphic EQ → Parametric EQ → Dynamic EQ → Compressor → Gate → Spatial FX → Harmonic FX → Limiter → Output

#### Subsystem 1: Core Audio Engine
- 32-bit floating-point processing internally
- Sample rate support: 44.1kHz, 48kHz, 96kHz, 192kHz
- Latency: < 10ms in real-time mode
- CPU load monitoring with automatic quality adjustment
- Buffer size optimization based on device capability

#### Subsystem 2: Graphic Equalizer
**Modes Available:**
- 5-band (Simple mode for quick adjustments)
- 10-band (Standard mode for detailed control)
- 15-band (Professional mode for precision)
- 31-band (Mastering mode for surgical adjustments)

**Per-Band Features:**
- Frequency range: 20Hz - 20kHz
- Gain range: ±24dB (configurable)
- Solo and mute per band
- Visual frequency response curve
- Touch-draw curve editing

#### Subsystem 3: Parametric Equalizer
- Unlimited bands (CPU dependent, typically 8-16)
- Filter types: Bell, High Shelf, Low Shelf, Notch, High Pass, Low Pass
- Q factor: 0.1 - 30.0
- Variable slopes: 6dB, 12dB, 18dB, 24dB, 48dB, 96dB per octave
- Frequency: 20Hz - 20kHz (continuous)
- Gain: ±24dB

#### Subsystem 4: Advanced EQ
- **Dynamic EQ:** Level-reactive bands that adjust based on input signal
- **Linear Phase EQ:** Zero phase distortion with automatic latency compensation
- **Mid/Side Processing:** Independent EQ for center and side channels
- **Auto Gain Compensation:** Maintains perceived loudness during EQ changes

#### Subsystem 5: Dynamics Processing

**Compressor:**
- Threshold: -60dB to 0dB
- Ratio: 1:1 to ∞:1
- Attack: 0.1ms to 100ms
- Release: 10ms to 2000ms
- Knee: Hard to soft (0-24dB)
- Makeup gain: 0-24dB
- Sidechain filtering support

**Multiband Compressor:**
- 2 to 6 independent bands
- Adjustable crossover frequencies
- Per-band compression settings
- Band linking option
- Visual gain reduction meters

**Limiter:**
- True peak detection
- Brickwall limiting
- Ceiling: -12dB to 0dB
- Release: 1ms to 1000ms
- Automatic release option

**Noise Gate:**
- Threshold: -96dB to 0dB
- Attack: 0.1ms to 50ms
- Hold: 0ms to 500ms
- Release: 10ms to 2000ms
- Range: -∞ to 0dB

**Expander:**
- Downward expansion
- Ratio: 1:1 to 1:8
- Threshold-based activation

#### Subsystem 6: Spatial & Time Effects

**Reverb:**
- Algorithm types: Room, Hall, Plate, Chamber, Cathedral, Spring
- Convolution reverb with IR loading capability
- Parameters: Room size, Decay, Damping, Pre-delay, Wet/Dry mix
- Early reflections control
- Modulation depth and rate

**Delay:**
- Types: Digital, Analog (modeled), Tape (modeled), Ping-pong
- Sync to BPM option
- Delay time: 1ms to 5000ms
- Feedback: 0% to 100%
- Filter in feedback loop
- Stereo spread control

**Stereo Widener:**
- Width: 0% (mono) to 200% (wide)
- Mid/Side balance
- Bass mono (prevents phase issues)
- Haas effect integration

**Virtualizer:**
- Android Virtualizer effect integration
- Strength: 0 to 1000
- Headphone optimization
- Room simulation

**Bass Boost:**
- Android BassBoost integration
- Strength: 0 to 1000
- Center frequency adjustment
- Harmonic enhancement

#### Subsystem 7: Harmonic & Modulation Effects

**Distortion:**
- Types: Tube, Transistor, Digital, Fuzz
- Drive: 0% to 100%
- Tone control
- Mix blend

**Saturation:**
- Types: Tape, Analog, Tube
- Amount: Subtle to heavy
- Harmonic character selection

**Exciter:**
- Harmonic generation
- Frequency focus (low, mid, high)
- Blend control

**Chorus:**
- Voices: 2 to 8
- Rate: 0.1Hz to 10Hz
- Depth: 0% to 100%
- Stereo spread

**Flanger:**
- Rate: 0.01Hz to 20Hz
- Depth: 0% to 100%
- Feedback: -100% to 100%
- Manual offset

**Phaser:**
- Stages: 2 to 24
- Rate: 0.01Hz to 20Hz
- Depth: 0% to 100%
- Feedback control

**Tremolo:**
- Rate: 0.1Hz to 20Hz
- Depth: 0% to 100%
- Waveform: Sine, Triangle, Square

**Bitcrusher:**
- Bit depth: 1 to 16 bits
- Sample rate reduction
- Wet/Dry mix

#### Subsystem 8: AI/Smart Processing
- **Auto-EQ:** Analyzes audio and suggests optimal EQ curve
- **Genre Detection:** Identifies music genre for preset suggestions
- **Loudness Normalization:** LUFS-based (-14 to -23 LUFS targets)
- **Ear Profile Calibration:** Hearing test for personalized EQ
- **Intelligent Preset Suggestions:** Based on listening history

#### Subsystem 9: Analysis & Visualization
- **Real-time FFT Spectrum:** 512 to 8192 point FFT
- **Spectrogram:** Time-frequency waterfall display
- **Level Meters:** Peak, RMS, LUFS (momentary, short-term, integrated)
- **Phase Correlation:** Mono compatibility meter
- **Stereo Field Scope:** Lissajous pattern display
- **Clipping Detection:** Visual and audio indicators

#### Subsystem 10: FX Routing
- Drag-and-drop effect chain reordering
- Parallel processing buses
- Per-effect dry/wet mix
- A/B comparison switching
- Undo/Redo history (50 steps)
- Signal flow visualization

#### Subsystem 11: Presets System
- **Factory Presets:** 50+ presets across categories
  - Music genres (Rock, Pop, Jazz, Classical, Hip-Hop, Electronic, etc.)
  - Use cases (Podcast, Gaming, Movie, Voice Call)
  - Devices (Headphones, Car, Speakers)
- **User Presets:** Unlimited custom presets
- **Import/Export:** JSON and TXT formats
- **Sharing:** Generate shareable preset codes

#### Subsystem 12: Special Modes
- **Podcast Mode:** Voice clarity, compression, noise reduction
- **Gaming Mode:** Footstep enhancement, spatial audio boost
- **Call Clarity:** Voice enhancement for calls
- **Karaoke:** Vocal removal/isolation
- **Music Mastering:** Professional mastering chain
- **Night Mode:** Compressed dynamics for quiet listening
- **Workout Mode:** Heavy bass boost
- **Meditation:** Smooth, warm sound profile
- **Sleep Mode:** Ultra-soft dynamics

#### Subsystem 13: Developer Features
- DSP performance metrics
- CPU load per effect
- Latency monitoring
- Debug audio output
- Effect bypass testing

#### Subsystem 14: Safety & Reliability
- **Hard Limiter:** Prevents dangerous output levels
- **Hearing Protection:** WHO-standard exposure tracking
- **Auto Volume Rollback:** Reverts extreme settings
- **Crash-Safe Recovery:** Preserves settings on crash

---

### Module 4: Podcast Production Studio

**Location:** `com.example.dwn.podcast`

**Purpose:** End-to-end podcast production from recording to distribution with professional tools and AI assistance.

**Architecture:**

The podcast module is organized into 8 sub-modules:

#### 4.1 Audio Recording Engine
**Capabilities:**
- 32-bit float recording (clip-proof)
- Sample rates: 44.1kHz, 48kHz, 96kHz
- Multi-track recording (up to 8 simultaneous tracks)
- Per-track gain staging
- Real-time waveform display
- Peak, RMS, and LUFS metering
- Automatic backup recording (shadow recording)
- Latency-compensated monitoring

**Track Types:**
- Host track
- Guest tracks (1-7)
- System audio capture
- Music bed track
- Sound effects track

#### 4.2 Video Recording Engine
**Capabilities:**
- Multi-camera support (front, rear, external USB)
- Live camera switching
- Manual camera controls (ISO, shutter, focus, white balance)
- Up to 4K resolution recording
- Frame-accurate A/V sync
- Real-time preview
- Recording indicators overlay

#### 4.3 Live Streaming Engine
**Platforms Supported:**
- YouTube Live (RTMP)
- Twitch (RTMP)
- Facebook Live (RTMP)
- Custom RTMP endpoints
- Custom SRT endpoints

**Features:**
- Adaptive bitrate streaming
- Stream health monitoring
- Viewer count display
- Live chat integration readiness
- Multi-platform simultaneous streaming

#### 4.4 Remote Guest System (LiveKit Integration)
**Architecture:**
- WebRTC-based real-time communication
- LiveKit server connectivity
- SFU (Selective Forwarding Unit) topology

**Capabilities:**
- Up to 10 remote participants
- Individual audio tracks per guest
- Video support for guests
- Local recording per participant
- Automatic drift correction
- Network resilience with reconnection
- Echo cancellation
- Noise suppression

**Session Management:**
- Unique room codes (6-character)
- Host controls (mute, remove, spotlight)
- Waiting room support
- Recording permissions

#### 4.5 AI Processing Engine
**Real-time Features:**
- Noise suppression (ML-based)
- Voice isolation
- Room echo removal
- Auto-leveling

**Post-Processing Features:**
- **Transcription:** Full episode transcription with timestamps
- **Speaker Diarization:** Who spoke when
- **Chapter Detection:** Automatic chapter markers based on content
- **Filler Word Detection:** "Um," "uh," "like," etc.
- **Show Notes Generation:** AI-generated summaries
- **Title/Description Suggestions:** SEO-optimized

**Integration:**
- On-device processing for privacy-sensitive content
- Cloud processing option (Whisper API) for higher accuracy
- Hybrid mode for optimal balance

#### 4.6 Editing Suite
**Timeline Features:**
- Non-destructive editing
- Multi-track timeline
- Ripple edits
- Clip splitting and trimming
- Crossfades (linear, logarithmic, S-curve)
- Time stretching (pitch-preserving)

**Audio Processing:**
- Per-track EQ
- Per-track compression
- De-esser
- Noise reduction
- Loudness normalization

**Markers:**
- Chapter markers
- Ad slot markers
- Highlight markers
- Cut markers
- Notes

#### 4.7 Export Pipeline
**Audio Formats:**
- MP3 (128kbps, 192kbps, 320kbps)
- AAC (128kbps, 256kbps)
- WAV (16-bit, 24-bit)
- FLAC (lossless)
- Opus (64kbps - 256kbps)

**Video Formats:**
- MP4 (H.264)
- WebM (VP9)
- MOV (ProRes proxy)

**Podcast-Specific:**
- Chapter markers embedding
- Artwork embedding
- ID3 tags
- RSS feed generation
- Multiple versions (clean, with music)

#### 4.8 Distribution
- RSS feed hosting readiness
- Platform submission helpers
- Episode scheduling
- Analytics dashboard
- Social sharing

---

### Module 5: DJ Studio

**Location:** `com.example.dwn.dj`

**Purpose:** Professional DJ mixing environment with dual decks, effects, and real-time performance features.

**Architecture:**

The DJ Studio operates as a real-time audio mixing system with three main components:

#### 5.1 Dual Deck System

**Per-Deck Features:**
- Independent MediaPlayer instance
- Track loading from device library
- Waveform visualization (overview + detailed)
- BPM detection and display
- Key detection (Camelot wheel)
- Cue points (up to 8 hot cues)
- Looping (beat-synced, manual)
- Pitch/tempo adjustment (±50%)
- Vinyl mode with scratch simulation

**Deck State:**
- Track metadata (title, artist, album, artwork)
- Playback position
- BPM (detected and adjusted)
- Key
- Volume level
- EQ settings (high, mid, low)
- Effect chain active

#### 5.2 Mixer Section

**Crossfader:**
- Linear, logarithmic, and scratch curves
- Crossfader assign (A, B, Thru)
- Hamster mode (reverse)
- Cut-in adjustment

**Channel Strips:**
- Volume fader (0-100%)
- 3-band EQ with kill switches
- Filter (high-pass/low-pass)
- Gain trim
- VU meters
- Cue/PFL buttons

**Master Section:**
- Master volume
- Master limiter
- Booth output (if supported)
- Record output

#### 5.3 Effects Section

**Per-Deck Effects:**
- Filter (resonant low/high pass)
- Echo (tempo-synced)
- Reverb (multiple room types)
- Flanger
- Phaser
- Delay
- Bitcrusher
- Gate

**FX Controls:**
- Effect select
- Dry/wet mix
- Parameter knobs (effect-specific)
- On/off toggle
- Tempo sync

#### 5.4 Performance Features

**Scratching:**
- Touch-based vinyl control
- Scratch sound synthesis
- Haptic feedback
- Velocity-sensitive response

**Beat Sync:**
- Automatic BPM matching
- Phase alignment
- Quantized loop activation

**Sampler:**
- 8 sample pads
- One-shot and loop modes
- Volume per pad
- Trigger modes (gate, toggle)

**Recording:**
- Mix recording (stereo master)
- WAV output
- Automatic file naming

---

### Module 6: Beat Maker

**Location:** `com.example.dwn.beatmaker`

**Purpose:** Music production environment with step sequencing, instrument tracks, and professional mixing.

**Architecture:**

#### 6.1 Sequencer Engine

**Grid Configuration:**
- Pattern length: 1-64 steps
- Time signatures: 4/4, 3/4, 6/8, custom
- Tempo: 20-300 BPM
- Swing: 0-100%
- Step resolution: 1/4, 1/8, 1/16, 1/32

**Step Properties:**
- Velocity (0-127)
- Pan (-100 to +100)
- Probability (0-100%)
- Micro-timing offset

#### 6.2 Instrument Tracks

**Track Types:**

**Drums:**
- 16+ instrument slots
- Sample-based synthesis
- Built-in drum kits (808, 909, acoustic, etc.)
- Custom sample loading
- Per-pad tuning and envelope

**Bass:**
- Synthesizer-based
- Preset bass sounds
- Filter and envelope control
- Glide/portamento

**Synth/Keys:**
- Polyphonic playback
- Piano roll editor
- Chord mode
- Arpeggiator

**Samples:**
- Audio clip slots
- Time-stretching
- Slice mode
- Reverse playback

#### 6.3 Pattern System

**Pattern Features:**
- 16 pattern slots
- Pattern chaining
- Song arrangement mode
- Copy/paste patterns
- Pattern variations

#### 6.4 Mixer

**Per-Track:**
- Volume fader
- Pan knob
- Mute/Solo
- FX sends (2)

**FX Buses:**
- Reverb bus
- Delay bus
- Per-bus return level

**Master:**
- Master volume
- Master EQ
- Limiter
- Stereo width

#### 6.5 Export

**Formats:**
- WAV (stems or master)
- MP3 (master)
- MIDI export

**Options:**
- Normalize output
- Dither (for bit depth reduction)
- Tail inclusion (reverb/delay)

---

### Module 7: Audio Social Platform

**Location:** `com.example.dwn.audio.social`

**Purpose:** Real-time collaborative audio experiences including listening rooms and voice communication.

**Architecture:**

#### 7.1 Room System

**Room Types:**

**Open Rooms:**
- Anyone can join
- Raise-hand to speak
- Unlimited audience
- Public discovery

**Stage Rooms:**
- Host-controlled speakers
- Audience in listen-only mode
- Speaker invitations
- Moderation controls

**Private Rooms:**
- Invite-only access
- Optional E2E encryption
- No public listing
- Password protection option

#### 7.2 Audio Pipeline

**Capture:**
- Echo cancellation (AEC)
- Noise suppression (NS)
- Automatic gain control (AGC)
- Voice activity detection (VAD)

**Transmission:**
- Opus codec (speech-optimized)
- Adaptive bitrate (16-128 kbps)
- Jitter buffer
- Packet loss concealment

**Playback:**
- Per-speaker volume
- Spatial positioning
- Automatic level balancing

#### 7.3 Co-Listening Feature

**Synchronization:**
- Timestamp-based sync
- Host-controlled playback
- Late-join auto-sync
- Drift correction

**Features:**
- Shared play/pause/seek
- Queue management (host)
- Track suggestions (audience)
- Listening history

#### 7.4 Audio Clips

**Creation:**
- Time range selection
- Auto silence trimming
- Loudness normalization
- Caption attachment

**Sharing:**
- Internal sharing (rooms, profiles)
- External links
- Embedded players

---

### Module 8: Artists Arena

**Location:** `com.example.dwn.arena`

**Purpose:** Music discovery platform for independent artists to publish, promote, and monetize their work.

**Architecture:**

#### 8.1 Artist System

**Profile Components:**
- Display name and handle
- Bio and description
- Genre tags (up to 5)
- Mood tags (up to 5)
- Location (optional)
- Social links
- Verification status

**Artist Roles:**
- Independent Artist
- Producer
- DJ
- Podcaster

**Verification Levels:**
- Unverified (basic)
- Verified (identity confirmed)
- Official (label/management)

#### 8.2 Track Management

**Upload Pipeline:**
1. File upload (WAV, FLAC, MP3, AAC)
2. Format validation
3. Loudness analysis (LUFS target: -14)
4. Clipping detection
5. Metadata extraction
6. Waveform generation
7. Quality tier assignment
8. CDN distribution

**Track Metadata:**
- Title, artist, album
- Genre, mood, BPM, key
- Release date
- Credits
- Lyrics (optional)
- Remix permissions

**Publish States:**
- Draft
- Private
- Scheduled
- Public

#### 8.3 Discovery System

**Feed Types:**
- Fresh Releases (chronological)
- Trending (engagement-weighted)
- Genre feeds
- Mood-based feeds
- Local scene (geographic)
- Curated picks (editorial)

**Ranking Signals:**
- Listen-through rate
- Replay frequency
- Saves and likes
- Comments
- Radio spins
- Remix usage
- Share rate

#### 8.4 Engagement

**Listener Interactions:**
- Like/save
- Timestamped comments
- Voice reactions (clips)
- Share internally/externally
- Add to playlist
- Follow artist

**Community Features:**
- Artist spotlight
- Remix challenges
- Collaboration requests
- Fan clubs

#### 8.5 Analytics

**Track Analytics:**
- Play count (verified)
- Average listen duration
- Drop-off points
- Geographic distribution
- Platform breakdown
- Referral sources

**Artist Dashboard:**
- Total plays
- Follower growth
- Engagement rate
- Revenue (if enabled)
- Top tracks
- Audience demographics

#### 8.6 Monetization (Future)

**Revenue Streams:**
- Tips and donations
- Paid exclusive releases
- Subscription tiers
- Licensing fees
- Remix royalties

---

## System Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            PRESENTATION LAYER                                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │  MainHub    │ │  Downloads  │ │   Player    │ │  Podcast    │           │
│  │   Screen    │ │   Screen    │ │    UI       │ │   Studio    │           │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │  DJ Studio  │ │ Beat Maker  │ │   Artists   │ │Audio Social │           │
│  │   Screen    │ │   Screen    │ │    Arena    │ │   Rooms     │           │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘           │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             DOMAIN LAYER                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │ DownloadManager │  │AudioPlayerManager│  │  PodcastModule  │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │  DJEnginePro    │  │ BeatMakerEngine │  │AudioSocialManager│             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │  VaultManager   │  │  RadioManager   │  │ContextMediaMgr  │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│  ┌─────────────────┐  ┌─────────────────┐                                   │
│  │ SuperEqualizer  │  │ ArenaRepository │                                   │
│  └─────────────────┘  └─────────────────┘                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │DownloadDatabase │  │   DownloadDao   │  │MediaPlayStatsDao│              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │ SettingsManager │  │DeviceMediaScanner│ │  NetworkMonitor │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PLATFORM LAYER                                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ MediaPlayer │ │   Room DB   │ │   CameraX   │ │   LiveKit   │           │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │  AudioFX    │ │   yt-dlp    │ │ RootEncoder │ │   OkHttp    │           │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘           │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Module Structure

### 1. Download Module (`download/`)

Handles media downloading from YouTube and social platforms.

```
download/
├── DownloadManager.kt      # Core download orchestration
└── QueuedDownload.kt       # Download queue data model
```

**Key Classes:**

| Class | Responsibility |
|-------|----------------|
| `DownloadManager` | Queue management, concurrent downloads, progress tracking |
| `QueuedDownload` | Download item state (URL, progress, status) |

**Download States:**
```
QUEUED → CHECKING → DOWNLOADING → COMPLETED
                 ↘ FAILED
                 ↘ PAUSED → RESUMING → DOWNLOADING
```

### 2. Player Module (`player/`)

Audio and video playback with advanced DSP processing.

```
player/
├── AudioPlayerManager.kt       # Main audio player controller
├── AudioPlayerUI.kt            # Player UI components
├── EqualizerManager.kt         # Legacy equalizer
├── MediaActionReceiver.kt      # Notification action handler
├── MediaNotificationManager.kt # Playback notifications
├── VideoPlayerActivity.kt      # Video playback activity
└── audio/
    └── SuperEqualizer.kt       # Advanced DSP engine
```

**Audio Player State Machine:**
```
IDLE → LOADING → PLAYING ⟷ PAUSED → STOPPED
                    ↓
              SEEKING → PLAYING
```

### 3. Podcast Module (`podcast/`)

Complete podcast production suite.

```
podcast/
├── PodcastModule.kt        # Main orchestrator
├── ai/                     # AI-powered features
│   ├── PodcastAIProcessor.kt
│   └── PodcastSpeechRecognizer.kt
├── audio/
│   ├── PodcastAudioRecorder.kt
│   ├── PodcastAudioProcessor.kt
│   └── MediaCodecAudioEncoder.kt
├── export/
│   └── PodcastExporter.kt
├── livekit/
│   └── LiveKitRoomManager.kt
├── streaming/
│   ├── PodcastRTMPStreamer.kt
│   └── MultiPlatformStreamer.kt
├── ui/
│   └── PodcastScreen.kt
├── video/
│   └── PodcastVideoRecorder.kt
└── webrtc/
    └── PodcastWebRTCRoom.kt
```

### 4. DJ Studio Module (`dj/`)

Professional DJ mixing engine.

```
dj/
├── DJEnginePro.kt    # Dual-deck mixing engine
└── DJModels.kt       # State models
```

**DJ Engine Features:**
- Dual deck playback (Deck A & B)
- Real-time scratching with haptic feedback
- Crossfader mixing
- Per-deck EQ and effects
- BPM sync
- Waveform visualization

### 5. Beat Maker Module (`beatmaker/`)

Music production and sequencing.

```
beatmaker/
└── BeatMakerEngine.kt    # Sequencer and instrument engine
```

**Beat Maker Architecture:**
```
┌──────────────────────────────────────────────────────────┐
│                    SEQUENCER CORE                         │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐         │
│  │ Step Grid  │  │ Piano Roll │  │  Pattern   │         │
│  │ (16 steps) │  │ (Melodic)  │  │  Chainer   │         │
│  └────────────┘  └────────────┘  └────────────┘         │
└──────────────────────────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────┐
│                  INSTRUMENT TRACKS                        │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ │
│  │ Drums  │ │  Bass  │ │ Synth  │ │ Sample │ │  Pad   │ │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ │
└──────────────────────────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────┐
│                    MIXER & MASTER                         │
│  ┌────────────────┐  ┌────────────────┐                  │
│  │ Per-Track FX   │  │  Master Bus    │                  │
│  │ EQ/Comp/Reverb │  │  EQ/Limiter    │                  │
│  └────────────────┘  └────────────────┘                  │
└──────────────────────────────────────────────────────────┘
```

### 6. Audio Social Module (`audio/social/`)

Real-time collaborative audio rooms.

```
audio/social/
├── AudioSocialManager.kt    # Room management
└── AudioSocialModels.kt     # Data models
```

**Room Types:**
- **Open Rooms** - Drop-in listening, raise-hand to speak
- **Stage Rooms** - Host + speakers, audience silent
- **Private Rooms** - Invite-only with E2E encryption option

### 7. Artists Arena Module (`arena/`)

Music discovery and artist platform.

```
arena/
├── ArenaModels.kt              # Data models
├── ArenaOnboardingScreen.kt    # Artist signup
├── ArenaRepository.kt          # Data operations
├── ArenaSearchScreen.kt        # Discovery search
├── ArtistDashboardScreen.kt    # Analytics dashboard
├── ArtistProfileScreen.kt      # Artist profile
├── ArtistsArenaScreen.kt       # Main arena screen
├── RemixChallengeScreen.kt     # Remix competitions
├── TrackDetailScreen.kt        # Track view
└── TrackUploadScreen.kt        # Upload flow
```

### 8. Context Media Module (`context/`)

Intelligent context-aware audio adaptation.

```
context/
├── ContextMediaManager.kt    # Context detection
└── ContextMediaModels.kt     # Mode configurations
```

**Context Modes:**
| Mode | Trigger | Audio Adaptation |
|------|---------|------------------|
| Walk Mode | Motion detected | Enhanced speech, reduced bass |
| Drive Mode | Car Bluetooth | Loud voice, compressed dynamics |
| Focus Mode | Manual | Flat EQ, minimal FX |
| Night Mode | Time-based | Soft dynamics, reduced loudness |
| Cast Mode | Casting active | Disable local FX, source-accurate |

### 9. Vault Module (`vault/`)

Centralized media repository.

```
vault/
├── VaultManager.kt    # Media indexing & search
└── VaultModels.kt     # Data structures
```

### 10. Radio Module (`radio/`)

Online streaming radio.

```
radio/
├── RadioManager.kt      # Station playback
├── RadioModels.kt       # Station models
├── MyRadioManager.kt    # Personal stations
└── MyRadioModels.kt     # User station models
```

---

## Data Layer

### Database Schema

```sql
-- Download tracking
CREATE TABLE downloads (
    id TEXT PRIMARY KEY,
    url TEXT NOT NULL,
    title TEXT,
    fileName TEXT,
    mediaType TEXT,          -- 'MP3' or 'MP4'
    status TEXT,             -- PENDING, DOWNLOADING, COMPLETED, etc.
    progress REAL,
    filePath TEXT,
    fileSize INTEGER,
    downloadedBytes INTEGER,
    createdAt INTEGER,
    completedAt INTEGER,
    errorMessage TEXT,
    thumbnailUrl TEXT,
    playCount INTEGER DEFAULT 0,
    lastPlayedAt INTEGER
);

-- Unified play statistics (all media sources)
CREATE TABLE media_play_stats (
    id TEXT PRIMARY KEY,
    mediaUri TEXT UNIQUE NOT NULL,
    mediaType TEXT NOT NULL,     -- 'AUDIO' or 'VIDEO'
    mediaSource TEXT NOT NULL,   -- 'DOWNLOAD', 'DEVICE', 'STREAM'
    title TEXT NOT NULL,
    artist TEXT,
    album TEXT,
    duration INTEGER DEFAULT 0,
    playCount INTEGER DEFAULT 0,
    lastPlayedAt INTEGER,
    totalPlayDuration INTEGER DEFAULT 0,
    completedPlays INTEGER DEFAULT 0,
    createdAt INTEGER NOT NULL
);

CREATE UNIQUE INDEX idx_media_uri ON media_play_stats(mediaUri);
```

### Data Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   UI Layer  │ ──▶ │  DAO Layer  │ ──▶ │  Room DB    │
│  (Compose)  │ ◀── │  (Queries)  │ ◀── │  (SQLite)   │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │
       │   StateFlow       │   Flow<List<T>>
       ▼                   ▼
┌─────────────────────────────────────────────────────┐
│              Reactive Data Binding                   │
│  collectAsState() → Compose Recomposition           │
└─────────────────────────────────────────────────────┘
```

### DAO Interfaces

```kotlin
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>
    
    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED'")
    fun getCompletedDownloads(): Flow<List<DownloadItem>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItem)
    
    @Update
    suspend fun updateDownload(item: DownloadItem)
    
    @Query("UPDATE downloads SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: String, timestamp: Long)
}

@Dao
interface MediaPlayStatsDao {
    @Query("SELECT * FROM media_play_stats ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayed(limit: Int): Flow<List<MediaPlayStats>>
    
    @Upsert
    suspend fun upsertPlayStats(stats: MediaPlayStats)
    
    @Query("UPDATE media_play_stats SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE mediaUri = :uri")
    suspend fun incrementPlayCount(uri: String, timestamp: Long)
}
```

---

## Core Features

### 1. Media Download System

```
┌─────────────────────────────────────────────────────────────────┐
│                      DOWNLOAD PIPELINE                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. URL VALIDATION                                               │
│     ├── Detect platform (YouTube, Twitter, etc.)                │
│     ├── Check for playlist vs single video                      │
│     └── Validate URL format                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. METADATA EXTRACTION (yt-dlp)                                │
│     ├── Title, thumbnail, duration                              │
│     ├── Available formats/quality                               │
│     └── Playlist info (if applicable)                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. QUEUE MANAGEMENT                                            │
│     ├── Add to concurrent queue (max 3)                         │
│     ├── Check WiFi-only setting                                 │
│     └── Resume paused downloads on reconnect                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. DOWNLOAD EXECUTION                                          │
│     ├── yt-dlp process with progress callback                   │
│     ├── Format conversion (FFmpeg)                              │
│     └── File write to storage                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. POST-PROCESSING                                             │
│     ├── Update database record                                  │
│     ├── Add to MediaStore                                       │
│     ├── Generate thumbnail                                      │
│     └── Notify UI of completion                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2. Audio Playback System

```
┌─────────────────────────────────────────────────────────────────┐
│                    AUDIO PLAYBACK PIPELINE                       │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Media Source │───▶│ MediaPlayer  │───▶│ Audio Focus  │
│ (File/URI)   │    │  Decoder     │    │  Manager     │
└──────────────┘    └──────────────┘    └──────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SUPER EQUALIZER DSP CHAIN                     │
│                                                                  │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐         │
│  │ Graphic │──▶│ Dynamic │──▶│ Spatial │──▶│Harmonic │         │
│  │   EQ    │   │  Proc   │   │   FX    │   │   FX    │         │
│  │ 10/15/31│   │Comp/Lim │   │Reverb/  │   │Distort/ │         │
│  │  bands  │   │Gate/Exp │   │Virtual  │   │Saturate │         │
│  └─────────┘   └─────────┘   └─────────┘   └─────────┘         │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    MASTER OUTPUT                         │    │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │    │
│  │  │Bass     │  │Loudness │  │ Safety  │  │ Output  │    │    │
│  │  │Boost    │  │Enhancer │  │Limiter  │  │ Gain    │    │    │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘    │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Audio Track  │───▶│ Notification │───▶│ Audio Output │
│   Output     │    │   Controls   │    │   Device     │
└──────────────┘    └──────────────┘    └──────────────┘
```

### 3. Podcast Production Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    PODCAST PRODUCTION FLOW                       │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  RECORDING MODE                                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ Audio Input │  │ Video Input │  │Remote Guest │              │
│  │ (Mic)       │  │ (CameraX)   │  │ (LiveKit)   │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
│         │                │                │                      │
│         └────────────────┼────────────────┘                      │
│                          ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              AUDIO PROCESSOR                             │    │
│  │  Noise Suppression → Echo Cancel → Auto Gain → Limiter  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                          │                                       │
│                          ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              MULTI-TRACK RECORDER                        │    │
│  │  Track 1: Host    Track 2: Guest    Track 3: System     │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│  AI PROCESSING                                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │Transcription│  │  Chapter    │  │ Filler Word │              │
│  │  (Whisper)  │  │ Detection   │  │  Detection  │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│  EXPORT PIPELINE                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  Normalize  │  │   Encode    │  │  RSS Feed   │              │
│  │  (LUFS)     │  │ (MP3/AAC)   │  │ Generation  │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Audio Processing Pipeline

### Super Equalizer Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    SUPER EQUALIZER SYSTEM                        │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                 EQUALIZATION (Feature 2)                 │    │
│  │                                                          │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │    │
│  │  │   GRAPHIC    │  │ PARAMETRIC   │  │   ADVANCED   │   │    │
│  │  │  EQUALIZER   │  │  EQUALIZER   │  │      EQ      │   │    │
│  │  │              │  │              │  │              │   │    │
│  │  │ • 10-band    │  │ • Unlimited  │  │ • Dynamic EQ │   │    │
│  │  │ • 15-band    │  │   bands      │  │ • Linear     │   │    │
│  │  │ • 31-band    │  │ • Bell/Shelf │  │   Phase EQ   │   │    │
│  │  │ • ±24dB      │  │ • HPF/LPF    │  │ • Mid/Side   │   │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              DYNAMICS PROCESSING (Feature 3)             │    │
│  │                                                          │    │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐           │    │
│  │  │ Compressor │ │  Limiter   │ │   Gate     │           │    │
│  │  │ Single/    │ │ True Peak  │ │  Noise     │           │    │
│  │  │ Multiband  │ │ Brickwall  │ │  Expander  │           │    │
│  │  └────────────┘ └────────────┘ └────────────┘           │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              SPATIAL & TIME FX (Feature 4)               │    │
│  │                                                          │    │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐           │    │
│  │  │   Reverb   │ │   Delay    │ │  Stereo    │           │    │
│  │  │ Algorithmic│ │Digital/Tape│ │  Widener   │           │    │
│  │  │Convolution │ │ Ping-Pong  │ │ Virtualizer│           │    │
│  │  └────────────┘ └────────────┘ └────────────┘           │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │            HARMONIC & MODULATION FX (Feature 5)          │    │
│  │                                                          │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │    │
│  │  │Distortion│ │Saturation│ │ Exciter  │ │ Chorus   │    │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │    │
│  │  │ Flanger  │ │ Phaser   │ │ Tremolo  │ │Bitcrusher│    │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                ANALYSIS (Features 6 & 7)                 │    │
│  │                                                          │    │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐           │    │
│  │  │  Spectrum  │ │   Level    │ │  Stereo    │           │    │
│  │  │  Analyzer  │ │   Meter    │ │   Field    │           │    │
│  │  │    FFT     │ │ Peak/RMS   │ │   Scope    │           │    │
│  │  └────────────┘ └────────────┘ └────────────┘           │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Preset System

```kotlin
enum class AudioPreset {
    FLAT,
    BASS_BOOST,
    VOCAL_BOOST,
    TREBLE_BOOST,
    ROCK,
    POP,
    JAZZ,
    CLASSICAL,
    HIP_HOP,
    ELECTRONIC,
    PODCAST,
    GAMING,
    MOVIE,
    CUSTOM
}

// Special Modes (Feature 12)
enum class SpecialMode {
    PODCAST_MODE,      // Speech clarity enhancement
    GAMING_MODE,       // Footstep enhancement
    CALL_CLARITY,      // Voice call optimization
    KARAOKE,           // Vocal removal/isolation
    MASTERING          // Music mastering preset
}
```

---

## UI/UX Architecture

### Navigation Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                         MAIN HUB                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                   TOP APP BAR                            │    │
│  │  [MediaGrab Logo]              [Context Menu] [Profile]  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                   HERO BANNER                            │    │
│  │     "Experience the Power of Audio"                      │    │
│  │     [Artists Arena Introduction]                         │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              BOTTOM NAVIGATION TABS                      │    │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐        │    │
│  │  │  Audio  │ │  Video  │ │ Studio  │ │My Rooms │        │    │
│  │  │  Tools  │ │  Tools  │ │  Tools  │ │         │        │    │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘        │    │
│  │  ┌─────────┐ ┌─────────┐                                │    │
│  │  │Activity │ │Downloader│                               │    │
│  │  │         │ │         │                                │    │
│  │  └─────────┘ └─────────┘                                │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Tab Structure

| Tab | Features |
|-----|----------|
| **Audio Tools** | Player, Audio Space (Pro) |
| **Video Tools** | Video Editor, Screen Recorder |
| **Studio Tools** | Podcast Studio, DJ Studio, Beat Maker, Remix Studio |
| **My Rooms** | Audio Spaces, Podcasts (joined/available) |
| **Activity** | Unified notification center |
| **Downloader** | Media download interface |

### Theme System

```kotlin
// Color Palette
val PrimaryPink = Color(0xFFE91E63)
val PrimaryPurple = Color(0xFF9C27B0)
val PrimaryBlue = Color(0xFF2196F3)
val DarkBackground = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF1A1A1A)

// Gradient
val GradientStart = PrimaryPink
val GradientMid = PrimaryPurple
val GradientEnd = PrimaryBlue
```

### Component Library

| Component | Usage |
|-----------|-------|
| `GlassmorphicCard` | Elevated cards with blur effect |
| `AnimatedWaveform` | Audio visualization |
| `CircularKnob` | EQ/FX controls |
| `TurntableView` | DJ deck visualization |
| `StepSequencer` | Beat maker grid |
| `FloatingAudioPlayer` | Persistent mini player |

---

## Flow Diagrams

### 1. App Launch Flow

```
┌─────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────┐
│  Start  │────▶│   Splash    │────▶│  yt-dlp     │────▶│  Main   │
│   App   │     │   Screen    │     │   Init      │     │   Hub   │
└─────────┘     └─────────────┘     └─────────────┘     └─────────┘
                     │                     │
                     │ 2 seconds           │ Background
                     ▼                     ▼
              ┌─────────────┐       ┌─────────────┐
              │  Animation  │       │  Check      │
              │  Complete   │       │  Updates    │
              └─────────────┘       └─────────────┘
```

### 2. Download Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                            DOWNLOAD FLOW                                  │
└──────────────────────────────────────────────────────────────────────────┘

User Pastes URL
       │
       ▼
┌──────────────┐    No     ┌──────────────┐
│ Valid URL?   │──────────▶│ Show Error   │
└──────────────┘           └──────────────┘
       │ Yes
       ▼
┌──────────────┐    Yes    ┌──────────────┐    ┌──────────────┐
│ Is Playlist? │──────────▶│Show Playlist │───▶│ Select Items │
└──────────────┘           │   Dialog     │    │ to Download  │
       │ No                └──────────────┘    └──────────────┘
       │                                              │
       ▼                                              │
┌──────────────┐                                      │
│Select Format │◀─────────────────────────────────────┘
│  MP3 / MP4   │
└──────────────┘
       │
       ▼
┌──────────────┐    No     ┌──────────────┐
│WiFi Required?│──────────▶│ Add to Queue │
│(Setting On)  │           └──────────────┘
└──────────────┘                  │
       │ Yes                      │
       ▼                          │
┌──────────────┐    No     ┌──────────────┐
│ On WiFi?     │──────────▶│ Wait for     │
└──────────────┘           │    WiFi      │
       │ Yes               └──────────────┘
       ▼                          │
┌──────────────┐                  │
│Start Download│◀─────────────────┘
└──────────────┘
       │
       ▼
┌──────────────┐
│  Progress    │────────┐
│  Updates     │        │
└──────────────┘        │
       │                │ Error
       │ Complete       ▼
       ▼          ┌──────────────┐
┌──────────────┐  │ Retry or     │
│ Save to      │  │ Cancel       │
│ Storage      │  └──────────────┘
└──────────────┘
       │
       ▼
┌──────────────┐
│ Update DB    │
│ Notify User  │
└──────────────┘
```

### 3. Audio Playback Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          AUDIO PLAYBACK FLOW                              │
└──────────────────────────────────────────────────────────────────────────┘

User Taps Play
       │
       ▼
┌──────────────┐
│ Get Media    │
│ Source       │
└──────────────┘
       │
       ├──────────────┬──────────────┬──────────────┐
       ▼              ▼              ▼              ▼
┌──────────────┐┌──────────────┐┌──────────────┐┌──────────────┐
│  Downloaded  ││ Device Media ││   Stream     ││   Radio      │
│    File      ││   (URI)      ││   URL        ││   Station    │
└──────────────┘└──────────────┘└──────────────┘└──────────────┘
       │              │              │              │
       └──────────────┴──────────────┴──────────────┘
                      │
                      ▼
              ┌──────────────┐
              │Request Audio │
              │   Focus      │
              └──────────────┘
                      │
                      ▼
              ┌──────────────┐    No     ┌──────────────┐
              │Focus Granted?│──────────▶│ Wait/Retry   │
              └──────────────┘           └──────────────┘
                      │ Yes
                      ▼
              ┌──────────────┐
              │ Initialize   │
              │ MediaPlayer  │
              └──────────────┘
                      │
                      ▼
              ┌──────────────┐
              │ Initialize   │
              │ Equalizer    │
              └──────────────┘
                      │
                      ▼
              ┌──────────────┐
              │Record Play   │
              │ Statistics   │
              └──────────────┘
                      │
                      ▼
              ┌──────────────┐
              │ Start        │
              │ Playback     │
              └──────────────┘
                      │
                      ▼
              ┌──────────────┐
              │ Show         │
              │ Notification │
              └──────────────┘
                      │
                      ▼
              ┌──────────────┐
              │ Update UI    │
              │ (Position)   │◀────────────┐
              └──────────────┘             │
                      │                    │
                      ▼                    │
              ┌──────────────┐    Yes      │
              │ Still        │─────────────┘
              │ Playing?     │
              └──────────────┘
                      │ No
                      ▼
              ┌──────────────┐
              │ Handle       │
              │ Completion   │
              └──────────────┘
                      │
              ┌───────┴───────┐
              ▼               ▼
       ┌──────────────┐┌──────────────┐
       │ Repeat ONE   ││ Play Next    │
       │ (Restart)    ││ in Queue     │
       └──────────────┘└──────────────┘
```

### 4. Podcast Recording Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                       PODCAST RECORDING FLOW                              │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│   Create     │
│   Project    │
└──────────────┘
       │
       ▼
┌──────────────┐
│  Configure   │
│   Session    │
└──────────────┘
       │
       ├─────────────────────────────────────────┐
       ▼                                         ▼
┌──────────────┐                          ┌──────────────┐
│  Solo Mode   │                          │  Remote Mode │
│ (Local Rec)  │                          │  (LiveKit)   │
└──────────────┘                          └──────────────┘
       │                                         │
       │                                         ▼
       │                                  ┌──────────────┐
       │                                  │Create/Join   │
       │                                  │   Room       │
       │                                  └──────────────┘
       │                                         │
       │                                         ▼
       │                                  ┌──────────────┐
       │                                  │ Wait for     │
       │                                  │  Guests      │
       │                                  └──────────────┘
       │                                         │
       └─────────────────┬───────────────────────┘
                         ▼
                  ┌──────────────┐
                  │ Start        │
                  │ Recording    │
                  └──────────────┘
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
       ┌──────────┐┌──────────┐┌──────────┐
       │  Audio   ││  Video   ││  Remote  │
       │  Track   ││  Track   ││  Tracks  │
       └──────────┘└──────────┘└──────────┘
              │          │          │
              └──────────┴──────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ Real-time    │
                  │ Processing   │
                  │ • Noise Sup  │
                  │ • Auto Level │
                  │ • Limiter    │
                  └──────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │    Stop      │
                  │  Recording   │
                  └──────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │    AI        │
                  │ Processing   │
                  │ • Transcript │
                  │ • Chapters   │
                  │ • Show Notes │
                  └──────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │   Export     │
                  │  • MP3/AAC   │
                  │  • RSS Feed  │
                  └──────────────┘
```

### 5. DJ Studio Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           DJ STUDIO FLOW                                  │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────┐
│   DECK A     │         │   DECK B     │
└──────────────┘         └──────────────┘
       │                        │
       ▼                        ▼
┌──────────────┐         ┌──────────────┐
│ Load Track   │         │ Load Track   │
│ from Device  │         │ from Device  │
└──────────────┘         └──────────────┘
       │                        │
       ▼                        ▼
┌──────────────┐         ┌──────────────┐
│   Analyze    │         │   Analyze    │
│   BPM/Key    │         │   BPM/Key    │
└──────────────┘         └──────────────┘
       │                        │
       ▼                        ▼
┌──────────────┐         ┌──────────────┐
│ Per-Deck FX  │         │ Per-Deck FX  │
│ • EQ         │         │ • EQ         │
│ • Filter     │         │ • Filter     │
│ • Reverb     │         │ • Reverb     │
│ • Effects    │         │ • Effects    │
└──────────────┘         └──────────────┘
       │                        │
       └──────────┬─────────────┘
                  ▼
           ┌──────────────┐
           │  CROSSFADER  │
           │  ◀────●────▶ │
           │  A    MIX   B│
           └──────────────┘
                  │
                  ▼
           ┌──────────────┐
           │ MASTER MIX   │
           │ • Volume     │
           │ • Master EQ  │
           │ • Limiter    │
           └──────────────┘
                  │
                  ▼
           ┌──────────────┐
           │   OUTPUT     │
           │   (Speakers) │
           └──────────────┘
```

### 6. Artists Arena Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        ARTISTS ARENA FLOW                                 │
└──────────────────────────────────────────────────────────────────────────┘

                    ┌──────────────┐
                    │   Listener   │
                    │    Entry     │
                    └──────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌──────────────┐┌──────────────┐┌──────────────┐
    │   Discover   ││    Search    ││   Browse     │
    │    Feed      ││   Artists    ││   Genres     │
    └──────────────┘└──────────────┘└──────────────┘
           │               │               │
           └───────────────┴───────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │   Track      │
                    │   Detail     │
                    └──────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌──────────────┐┌──────────────┐┌──────────────┐
    │    Play      ││    Like /    ││   Remix      │
    │   Stream     ││   Comment    ││  (if allowed)│
    └──────────────┘└──────────────┘└──────────────┘


                    ┌──────────────┐
                    │   Artist     │
                    │    Entry     │
                    └──────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  Onboarding  │
                    │  • Profile   │
                    │  • Genres    │
                    │  • Bio       │
                    └──────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │   Upload     │
                    │   Track      │
                    └──────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  Validation  │
                    │ • Loudness   │
                    │ • Quality    │
                    │ • Metadata   │
                    └──────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │   Publish    │
                    │  • Schedule  │
                    │  • Immediate │
                    └──────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  Analytics   │
                    │  Dashboard   │
                    │ • Plays      │
                    │ • Likes      │
                    │ • Reach      │
                    └──────────────┘
```

---

## Integration Points

### External Services

| Service | Purpose | Integration Method |
|---------|---------|-------------------|
| **yt-dlp** | Media downloading | Native process execution |
| **FFmpeg** | Audio/video conversion | yt-dlp bundled |
| **LiveKit** | Real-time communication | SDK (livekit-android) |
| **Whisper** | AI transcription | REST API (optional) |
| **MediaStore** | Device media access | ContentResolver |

### Internal Integration

```
┌─────────────────────────────────────────────────────────────────┐
│                    INTEGRATION MAP                               │
└─────────────────────────────────────────────────────────────────┘

                    ┌──────────────┐
                    │   Media      │
                    │   Vault      │
                    └──────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  Downloader  │   │   Player     │   │   Podcast    │
│              │──▶│              │◀──│   Studio     │
└──────────────┘   └──────────────┘   └──────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│     DJ       │   │    Beat      │   │   Remix      │
│   Studio     │   │    Maker     │   │   Studio     │
└──────────────┘   └──────────────┘   └──────────────┘
        │                 │                 │
        └─────────────────┴─────────────────┘
                          │
                          ▼
                   ┌──────────────┐
                   │    Super     │
                   │  Equalizer   │
                   └──────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│   Context    │   │    Radio     │   │    Arena     │
│    Mode      │   │   Manager    │   │  Repository  │
└──────────────┘   └──────────────┘   └──────────────┘
```

---

## Security & Privacy

### Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| `INTERNET` | Media downloading, streaming | Yes |
| `READ_MEDIA_AUDIO` | Device music access | Yes (Android 13+) |
| `READ_MEDIA_VIDEO` | Device video access | Yes (Android 13+) |
| `RECORD_AUDIO` | Podcast recording | For Podcast |
| `POST_NOTIFICATIONS` | Playback notifications | Recommended |
| `FOREGROUND_SERVICE` | Background playback | Yes |

### Data Storage

| Data Type | Storage Location | Encryption |
|-----------|------------------|------------|
| Downloads | App-specific storage | No |
| User preferences | SharedPreferences | No |
| Play statistics | Room database | No |
| Audio recordings | App-specific storage | Optional |

### Network Security

- All external API calls use HTTPS
- LiveKit tokens are short-lived
- No user credentials stored locally

---

## Performance Considerations

### Memory Management

```kotlin
// Audio buffer sizes optimized for latency vs. stability
val bufferSize = AudioTrack.getMinBufferSize(
    sampleRate,          // 44100 Hz
    channelConfig,       // STEREO
    audioFormat          // PCM_16BIT
)

// Recommended: 2x minimum buffer for stability
val actualBuffer = bufferSize * 2
```

### Threading Model

| Operation | Thread | Reason |
|-----------|--------|--------|
| UI Updates | Main | Compose requirement |
| Audio Processing | Dedicated | Low latency |
| Database | IO Dispatcher | Non-blocking |
| Network | IO Dispatcher | Non-blocking |
| DSP Effects | Audio thread | Real-time |

### Battery Optimization

- Audio playback uses `PARTIAL_WAKE_LOCK`
- Download manager pauses on low battery
- Context Mode adjusts processing based on battery level

---

## Build Configuration

### Gradle Dependencies

```kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    
    // Database
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    ksp("androidx.room:room-compiler:2.6.0")
    
    // Media
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("com.github.AntennaPod:AntennaPod:2.7.0")
    
    // Download
    implementation("com.github.yausername.youtubedl-android:library:0.15.0")
    implementation("com.github.yausername.youtubedl-android:ffmpeg:0.15.0")
    
    // Real-time
    implementation("io.livekit:livekit-android:2.5.0")
    
    // Streaming
    implementation("com.github.pedroSG94.RootEncoder:library:2.4.3")
    
    // Camera
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-video:1.3.1")
}
```

### ProGuard Rules

```proguard
# Keep yt-dlp native libs
-keep class com.yausername.** { *; }

# Keep LiveKit
-keep class io.livekit.** { *; }

# Keep Room entities
-keep class com.example.dwn.data.** { *; }
```

---

## Future Roadmap

### Planned Features

1. **Cloud Sync** - Cross-device synchronization
2. **AI Co-host** - AI-generated podcast assistance
3. **Multi-platform Export** - Direct publishing to Spotify, Apple Podcasts
4. **Collaborative Beats** - Real-time beat making with friends
5. **Offline Mode** - Full functionality without internet
6. **MIDI Support** - External controller integration

### Technical Debt

- [ ] Migrate to Hilt for dependency injection
- [ ] Add comprehensive unit tests
- [ ] Implement proper error boundaries
- [ ] Add crashlytics integration
- [ ] Optimize memory usage in DJ Studio

---

## Appendix

### File Size Reference

| Module | Lines of Code | Complexity |
|--------|---------------|------------|
| MainActivity | ~1,300 | High |
| SuperEqualizer | ~625 | Very High |
| PodcastModule | ~1,400 | Very High |
| DJEnginePro | ~1,300 | High |
| BeatMakerEngine | ~750 | High |
| AudioSocialManager | ~630 | Medium |
| VaultManager | ~800 | Medium |
| DownloadManager | ~835 | High |
| ArenaRepository | ~700 | Medium |

### Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-05 | Initial release |

---

*Generated: January 5, 2026*
*MediaGrab Architecture Documentation v1.0*

