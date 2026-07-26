# Hermes — Android voice client

A phone client for a self-hosted **Hermes** agent. Talk to it, hear it talk back, and
control it from the lock screen, the Quick Settings tile, or the assistant gesture.

Native Android — **Kotlin + Jetpack Compose**, no web view, no backend of its own. It talks
straight to whatever HTTP or WebSocket endpoint your agent already exposes, and does speech
entirely on the device.

```
minSdk 26 (Android 8.0)   ·   targetSdk 35   ·   ~12 MB release APK
```

---

## Build

```bash
cd hermes-android
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # app/build/outputs/apk/release/app-release.apk
./gradlew test                   # 67 unit tests
```

Install with `adb install -r app/build/outputs/apk/release/app-release.apk`, or copy the APK
to the phone and open it (enable "install unknown apps" for your file manager first).

Release builds are signed with the standard debug key so the APK installs directly. Swap in
your own keystore in `app/build.gradle.kts` before distributing it anywhere.

---

## Connecting it to Hermes

Everything is configured in **Settings**; nothing is hardcoded. Pick the transport that
matches what your agent already serves — you should not have to change the agent.

| Transport | Request | Response it understands |
|---|---|---|
| **OpenAI-compatible** | `POST {base}/v1/chat/completions`, `stream: true` | SSE frames with `choices[0].delta.content`, ending at `[DONE]` or a `finish_reason` |
| **Generic JSON REST** | `POST {base}{path}` | SSE, NDJSON, or one JSON document |
| **WebSocket** | JSON frame on a persistent `ws://`/`wss://` socket | JSON frames until a terminal one |

**OpenAI-compatible** is the one to try first — it is what Ollama, llama.cpp, vLLM, LM Studio,
LiteLLM, and most agent frameworks put in front of themselves.

### If your agent has its own shape (Generic JSON REST)

The request sends the prompt under every field name that shows up in practice, so your server
only has to recognise one of them:

```json
{
  "message": "…", "prompt": "…", "input": "…", "query": "…", "text": "…",
  "conversation_id": "…", "session_id": "…", "stream": true,
  "messages": [{"role": "user", "content": "…"}],
  "history":  [{"role": "user", "content": "…"}],
  "model": "…", "temperature": 0.7, "system": "…"
}
```

The reply is read just as loosely. Assistant text is found under `delta`, `text`, `content`,
`token`, `chunk`, `response`, `answer`, `reply`, `completion`, `output`, `result`, or
`message`, including OpenAI's `choices[].delta.content` and Anthropic-style
`content: [{type: "text", …}]` blocks. Alongside that it picks up:

- **Tool activity** — `tool_call`, `tool_calls`, `function_call`, `{"type": "tool_use"}` →
  shown on the message and in the status line while it runs.
- **Progress** — `status`, `stage`, `state`, `thinking` → shown under the orb.
- **Errors** — `error`, or `{"error": {"message": …}}` → surfaced instead of a silent stall.
- **End of turn** — `{"done": true}`, `finish_reason`, `stop_reason`, `[DONE]`, or a
  `type` of `done` / `end` / `complete` / `message_stop`.

Servers that re-send the whole answer each frame (cumulative rather than delta) are detected
and collapsed to the new tail, so the transcript doesn't stutter.

### WebSocket notes

The socket stays open across turns, so your agent can also push status and tool frames
between prompts. Outbound frames are `{"type": "user_message", "message": …, "text": …,
"prompt": …, "conversation_id": …, "messages": [...]}`; **Stop** sends `{"type": "cancel"}`.

Send a terminal frame (`{"type": "done"}`) when a turn ends. Without one there is an 8-second
idle fallback after the last token, which is a safety net, not a design.

### Auth and networking

- **API key** → sent as both `Authorization: Bearer <key>` and `x-api-key`. Paste a value
  that already has a scheme (`Token abc…`) and it is used verbatim.
- **Extra headers** → one `Name: value` per line, for anything else your setup needs.
- **Cleartext `http://` is allowed** so a LAN address works out of the box. If you only ever
  reach Hermes over TLS, delete `res/xml/network_security_config.xml` and its manifest
  attribute.
- **Test connection** in Settings reports reachability and latency, and populates the model
  dropdown when the server exposes `/v1/models`.

---

## Voice

Speech recognition and synthesis both run through the device's own engines — no API keys,
no per-minute cost, and no audio leaving the phone except as the text you chose to send.

**Four microphone modes**, switchable from the chips under the orb:

- **Off** — typing only.
- **Tap** — push-to-talk. Tap the orb, speak, it sends.
- **Hands-free** — the mic reopens by itself after each reply. A back-and-forth conversation
  with the phone on the desk.
- **Wake** — always listening for a wake phrase (default *"hey hermes"*, editable). Say
  "hey hermes, what's the build status" and the tail becomes the prompt; say just the phrase
  and it waits for your command.

Wake-phrase matching is fuzzy, because on-device recognition really does return "hey her mess"
and "hey hermez" — those all match, while ordinary speech does not.

**Replies are spoken as they stream.** Sentences are handed to the synthesizer as they
complete, so Hermes starts talking while it is still writing. Markdown is stripped first and
fenced code is replaced with "code block" rather than read out character by character.

**Barge-in**: tapping the orb while Hermes is speaking cuts it off immediately.

**Echo avoidance**: the mic never opens while the speaker is active, so it can't transcribe
Hermes talking to itself.

### Spoken commands handled on the phone

These never reach the agent. Matching is strict — only an utterance that is *nothing but* the
command counts, so "stop the nightly deploy" is still sent to Hermes while a bare "stop" halts it.

| Say | Effect |
|---|---|
| "stop", "cancel", "never mind" | Cancels the turn in flight and stops speech |
| "new conversation", "start over" | Starts a fresh conversation |
| "repeat that", "say that again" | Re-reads the last reply |
| "stop listening", "go to sleep" | Ends the voice session |

### Voice shortcuts

A trigger phrase that expands into a longer prompt, so a routine instruction is three words
instead of thirty. Edit them under the ⚡ icon; three are set up to start.

---

## Controlling the agent

- **Stop** — cancels the streaming turn; on WebSocket it also sends a cancel frame. Partial
  output is kept and marked `(stopped)` rather than thrown away.
- **Conversations** — multiple threads, each persisted on the phone, switchable and
  deletable. The conversation id travels with every request so a stateful agent can thread
  its own side.
- **History control** — send the last *N* turns, or turn history off entirely if your agent
  keeps its own session state.
- **Model, system prompt, temperature, max tokens** — per-request, editable in Settings.
- **Live status** — connection dot with latency in the title bar, tool calls and progress
  lines under the orb as they happen.

### Getting to it fast

- **Quick Settings tile** — pull down, tap, talk.
- **Assistant gesture** — Settings → Default digital assistant → Hermes. Then the usual
  long-press opens it already listening.
- **Notification** — a voice session shows an ongoing notification with a Stop action, so it
  is controllable with the screen off.

---

## How it is put together

```
app/src/main/java/com/hermes/voice/
  MainActivity.kt          entry point, permissions, assistant/tile intents, navigation
  ServiceLocator.kt        one engine instance, shared by the UI and the service

  data/                    settings (DataStore), conversations (JSON file), domain models
  net/
    HermesTransport.kt     transport interface + shared HTTP setup
    OpenAiCompatTransport  SSE chat-completions client
    RestJsonTransport      generic JSON client, SSE/NDJSON/single-document
    WebSocketTransport     persistent socket client
    JsonShapes.kt          shape-tolerant readers — where unknown payloads get decoded
    StreamReader.kt        SSE + NDJSON framing
  voice/
    SttEngine.kt           SpeechRecognizer as a Flow
    TtsEngine.kt           streaming TTS with sentence chunking and barge-in
    SpeechText.kt          markdown → speakable text, sentence splitting
    WakePhrase.kt          fuzzy wake-word matching
    VoiceSessionService.kt foreground service for background/screen-off sessions
  engine/
    HermesEngine.kt        the state machine: turns, voice loops, conversations
    LocalCommands.kt       utterances handled on-device
  ui/                      Compose screens: chat, settings, conversations, shortcuts
```

The engine is a singleton that both the Activity and the foreground service talk to, which is
what lets a hands-free session keep running while the app is backgrounded and still show the
right state when you come back.

## Tests

`./gradlew test` — 67 unit tests, no device needed. The transport tests run against a real
socket (MockWebServer) and cover SSE streaming, NDJSON, cumulative-vs-delta collapsing,
single-document replies, tool/status/error frames, HTTP failures, and the WebSocket exchange.
`JsonShapesTest` is the record of which server payload shapes are supported — add a case
there first when adding another.

## Known limits

- Wake-word mode drives the platform recognizer in a loop. It works, but it uses noticeably
  more battery than push-to-talk and its accuracy is whatever the device's speech service
  gives you. Treat it as a convenience, not an always-on assistant.
- Speech recognition needs a recognizer installed (Google app or equivalent). Without one the
  app says so instead of failing silently.
- The API key sits in app-private DataStore — protected by the app sandbox, not by hardware
  keystore encryption.
