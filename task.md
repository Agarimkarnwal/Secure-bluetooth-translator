# Setu Build Tasks

## Gradle / Project Setup
- [x] settings.gradle.kts
- [x] build.gradle.kts (root)
- [x] app/build.gradle.kts
- [x] gradle/libs.versions.toml
- [x] gradle-wrapper.properties

## Manifest + Core
- [x] AndroidManifest.xml
- [x] colors.xml / strings.xml / themes.xml
- [x] nav_graph.xml

## Step 1 — Bluetooth Transport & Security
- [x] ConnectionListener.kt
- [x] PermissionUtils.kt (Android 12+ permission handling)
- [x] CryptoManager.kt (AES-256-GCM E2EE payload encryption)
- [x] DebugLogManager.kt (Real-time log stream & ring buffer)
- [x] BluetoothConnectionManager.kt (Heartbeat ping/pong, RTT ms, buffer guards)

## Step 2 — Translation & On-Device NPU
- [x] TranslationManager.kt (NPU execution latency tracking in ms)
- [x] ModelDownloadManager.kt

## Data / Adapter
- [x] ChatMessage.kt + WireProtocol (E2EE tags & quick-reply key mappings)
- [x] ChatAdapter.kt (E2EE lock status & Hexagon NPU timing badges)

## UI Fragments
- [x] MainActivity.kt
- [x] StartFragment.kt
- [x] DeviceScanFragment.kt
- [x] ModelSetupFragment.kt
- [x] ChatFragment.kt (Live Diagnostics HUD, Debug Console, Voice, Quick Replies, Haptics)
- [x] TranslationTestFragment.kt

## Layouts
- [x] activity_main.xml
- [x] fragment_start.xml
- [x] fragment_device_scan.xml
- [x] fragment_model_setup.xml
- [x] fragment_chat.xml (HUD banner, quick reply chips, log modal button)
- [x] fragment_translation_test.xml
- [x] item_chat_sent.xml (tvMetaInfo E2EE + NPU badge)
- [x] item_chat_received.xml (tvMetaInfo E2EE + NPU badge)
- [x] item_device.xml
- [x] bottom_sheet_monster_mode.xml

## Drawables / Animations
- [x] bg_bubble_sent.xml
- [x] bg_bubble_received.xml
- [x] bg_input_bar.xml
- [x] bg_role_button.xml
- [x] bg_bottom_sheet.xml
- [x] slide_in_right / slide_out_left / slide_in_left / slide_out_right

## ✅ SECURITY, DEBUGGING & HACKATHON UPGRADE COMPLETE
