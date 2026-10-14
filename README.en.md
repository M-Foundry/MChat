<div align="center" style="display: flex; justify-content: center; align-items: center; gap: 20px;">

![LOGO](mimo-title.jpg)

</div>

> [!TIP]
> Translated by LLM, may contain inaccuracies.

MChat - An AI chat client optimized for the MiMo large model, built for a native Android experience

**Languages:** [简体中文](README.md) | [繁體中文](README.zh-TW.md) | English | [日本語](README.ja.md)

![GitHub stars](https://img.shields.io/github/stars/MRoldL001/MChat?style=for-the-badge&label=Stars)
![GitHub Release Downloads](https://img.shields.io/github/downloads/MRoldL001/MChat/total?style=for-the-badge&label=Downloads)
![MiMo](https://img.shields.io/badge/MiMo-console-FF7E00?style=for-the-badge\&logo=xiaomi\&logoColor=white)
![README](https://img.shields.io/badge/MiMo-README-FF7E00?style=for-the-badge\&logo=xiaomi\&logoColor=white)

---

> [!NOTE]
>
> A native Android AI chat client built with Kotlin and Jetpack Compose, featuring multi-model conversations, multimodal input, web search, thinking mode, and Markdown, LaTeX, and code syntax highlighting

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square\&logo=kotlin\&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.13.0-02303A?style=flat-square\&logo=gradle\&logoColor=white)
![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2024.12.01-4285F4?style=flat-square\&logo=jetpackcompose\&logoColor=white)
![Target API](https://img.shields.io/badge/Target%20API-36%20%28Android%2016%29-3DDC84?style=flat-square\&logo=android\&logoColor=white)

---

<p align="center">
    <img src="MChat.gif" alt="MChat" width="100">
</p>

## ✨ Features

- Multi-model switching
- Stylized rendering of tables, code blocks, dividers, and block quotes, with fairly complete LaTeX formula rendering (*including extra extensions such as chemical equations*)
- Thinking mode with a visible reasoning process, web search with inspectable sources, and attachments in conversations
- Comprehensive conversation history management
- Multiple theme options (including Material You dynamic color and custom colors), system dark mode following, and customizable chat background images
- Custom system prompts and model parameter tuning
- Adaptive layouts for phones and tablets
- Smooth animations with a personal take on Material You
- There's a squishable icon on the About page (

## 🚀 Quick Start

### Installation

#### Option 1: Download from GitHub Releases (Recommended)

1. Visit the project's [Releases](https://github.com/MRoldL001/MChat/releases) page
2. Download the latest `.apk` file
3. Install it on your phone (you may need to allow unknown sources)

#### Option 2: Build from Source

> [!WARNING]
>
> Building from source is not recommended for regular users — it's more trouble than it's worth, and when building the `.apk`:
>
> A `debug` build produces a larger package and, because of the debug tag, cannot be installed through the system installer
>
> A `releases` build requires the developer's signing key, which is kept private for security reasons; if you sign it yourself, every update must be rebuilt with the same key, so you won't be able to update directly over the developer's released `.apk`

1. Clone the repo locally with `git clone`
2. Open the project in `Android Studio` or any other `JetBrains IDE` with the Android development plugin installed
3. Sync `Gradle`
4. Run or build the `.apk` — a self-signed `releases` build is recommended

### Configuration

1. On first launch, tap the settings icon in the top-right corner
2. Set your Xiaomi MiMo API Key
3. (Optional) Configure the API Base URL
4. (Optional) Set a custom system prompt and adjust model parameters
5. Start chatting!

## 📱 Device Requirements

- Android 8 (API 26) or higher
- Dynamic color themes require Android 12 (API 31) or higher
- Predictive back requires Android 14 (API 34) or higher
- Live notifications require Android 16 (API 36) or higher

## 📚 Language Support

| Language                       | Status        |
| ------------------------------ | ------------- |
| 简体中文 (Simplified Chinese)  | Supported     |
| 繁體中文 (Traditional Chinese) | AI translated |
| English                        | AI translated |
| 日本語 (Japanese)              | AI translated |

## 🎨 Themes

The app supports several theme colors:

| Theme                 | Light Mode       | Dark Mode        |
| --------------------- | ---------------- | ---------------- |
| Ink & Paper (Default) | `#000000`        | `#FFFFFF`        |
| Dynamic Color         | System extracted | System extracted |
| Custom Color          | Custom           | Custom           |
| Hatsune Resonance     | `#39C5BB`        | `#39C5BB`        |
| Yoshiwara Lament      | `#D93A49`        | `#D93A49`        |
| Miyou Glimmer         | `#FF7E00`        | `#E86F00`        |
| Silent Jade Whisper   | `#5EA0AA`        | `#8FCED0`        |
| Verdant Shade         | `#006E2A`        | `#5CDB78`        |
| Eternal Reverie       | `#6650A4`        | `#D0BCFF`        |
| Abyssal Blue          | `#0B57D0`        | `#A8C7FA`        |

## 📖 Usage

### Sending Messages

Type your message in the input box at the bottom and tap the send button

### Switching Models

Tap the current model name at the top, then pick another model from the model selector

### Viewing the Thinking Process

When thinking mode is enabled, tap to expand the "Thinking Process" card to see the AI's reasoning

### Viewing Search Results

When web search is enabled, tap to expand the "Search Results" card to see the AI's sources

### Adding Attachments

Tap the button on the left of the input box to attach files to the conversation (some models may not support this)

### Code Blocks

Code blocks show a language icon and syntax highlighting

- Fullscreen: tap the fullscreen button at the top-right of a code block to expand it to fullscreen

- Copy code: tap the copy button at the top-right of a code block to copy its contents

### Managing Conversations

- New conversation: tap the new button in the sidebar
- Select conversation: tap a past conversation in the sidebar
- Delete conversation: tap the delete icon in the conversation list, then long-press to confirm
- Regenerate: tap the circular button below the latest message to regenerate it
- Edit and regenerate: tap the pen button below the latest message, edit the content in the input box, then tap send again to regenerate
- Undo edit: after tapping the pen button below the latest message, tap the arrow button on the left of the input box to revert your edit

## 🔧 Advanced Features

### Web Search

When enabled, the AI searches the web for real-time information and cites its sources

### Thinking Mode

When enabled, the AI shows its reasoning process

### Skill Modes

- **Poet Mode**: turns your input into classical poetry
- **Tutor Mode**: teaches new knowledge step by step

### Attachment Upload

Tap the plus button on the left of the input box to take a photo or upload files — images, audio, video, and **mixed uploads** are supported

### Custom Prompts

Add custom system prompts in Settings to shape the AI's behavior and style

### Parameter Settings

Tweak model parameters in Settings to personalize your experience

## 🗒️ Changelog

See [CHANGELOG.md](CHANGELOG.md)

## Disclaimer

### Non-official Statement

This app (MChat) is a third-party open-source client, **and has no affiliation, authorization, or partnership with Xiaomi Corporation and its affiliates (hereinafter referred to as Xiaomi)**.

### Intellectual Property

Trademarks and logos such as "Xiaomi" and "MiMo" are owned by Xiaomi Corporation. This app uses these marks only to describe functional compatibility (**i.e., nominative fair use**).

### Privacy Protection

All data of this app is stored locally on the device. Conversations are sent directly to Xiaomi's servers via the API. The developer does not collect or store any user data.

### Risk Assumption

**This app is provided on an "as-is" basis without any express or implied warranty**. Users assume all risks arising from using this app, including but not limited to data loss, privacy leakage, or device damage. The developer and Xiaomi are not responsible for the legality, security, or functionality of this app.

### Feedback & Support

If you encounter issues, please report them at [Issues](https://github.com/MRoldL001/MChat/issues). If you can, feel free to fork the source code, make changes, and submit a PR.
