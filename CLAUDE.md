# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

**Build** (produces a self-contained jlink runtime image):
```
mvn clean verify
```
Output lands in `target/maven-jlink/classifiers/local/` and a zip at `target/charibot-*-local.zip`.

**Run the built image:**
```
target/maven-jlink/classifiers/local/bin/bot
```

**Run directly from source** (requires `chariot-0.2.9.jar` in the working directory):
```
java --enable-preview -p chariot-0.2.9.jar --add-modules chariot src/main/java/bot/Bot.java
```

There are no tests.

## Environment Variables

| Variable | Purpose |
|---|---|
| `BOT_TOKEN` | Lichess token with `bot:play` scope — skips interactive OAuth flow |
| `LICHESS_API` | Override Lichess base URL (default: `https://lichess.org`) |
| `BOT_CHALLENGE_USER` | Username to re-challenge after each game |
| `ARENA_ID` | Arena ID to join on startup |
| `EULER_STREAM_KEY` | API key for eulerstream TikTok websocket proxy |

JVM system property `prefs` controls the Java Preferences node used to persist the OAuth token (default: `charibot`).

## Project Goal

**Livestream vs Audience Chess** — a TikTok streamer goes live and plays chess against their own audience. On startup the bot challenges the streamer's Lichess account. The TikTok audience types chess moves in the stream chat; those comments are aggregated to pick the bot's move, which is then submitted via the Lichess API. The bot represents the audience; the streamer plays as the human opponent.

## Architecture

The project is a single Maven module (`charibot`) with four classes and uses the [chariot](https://github.com/tors42/chariot) library for all Lichess API communication and the [TikTok-Live-Java](https://github.com/jwdeveloper/TikTok-Live-Java) library for TikTok chat ingestion.

**`Bot`** — entry point and game loop. `static void main()` retries forever on failure. `run()` opens the Lichess event stream and fans out challenge and game-start events to virtual threads via `StructuredTaskScope`. Active games are tracked in a `ConcurrentHashMap<opponentId, gameId>`. `receiveMoves()` receives aggregated chat messages from `TiktokReader` and selects a move to play.

**`TiktokReader`** — connects to a TikTok live stream via the eulerstream websocket proxy. Registers event callbacks (comment, gift, etc.) that fire on background websocket threads. Comment events are forwarded to `Bot.receiveMoves()`. Requires `EULER_STREAM_KEY` env var for eulerstream authentication.

**`ClientAndAccount`** — authentication. Tries `BOT_TOKEN` env var first; falls back to a stored token in Java Preferences; falls back to an interactive OAuth PKCE flow. Also handles the one-time upgrade of a regular account to a BOT account.

**`Rules`** — a list of `BiPredicate<ChallengeCreatedEvent, Map<String,String>>` rules that decide whether to decline a challenge. Default rules decline rated games, unsupported variants, and challenges when more than 8 games are already running.

**Result types** — chariot uses `Opt<T>` (with subtypes `Some<T>` and `Fail<?>`) instead of exceptions or `Optional`. The code pattern-matches these with `instanceof Some(var x)` throughout.

**`module-info.java`** declares `uses chariot.chess.BoardProvider` so that variant-specific board implementations (e.g. Chess 960) are discovered via `ServiceLoader` at runtime.

## Java Version Notes

- Targets Java 25 (`maven.compiler.release=25`) with `--enable-preview`
- Uses Java 25 finalized features: module import declarations (`import module chariot;`), instance main methods (`static void main()`), and Structured Concurrency (`StructuredTaskScope.open()`)
- The CI workflow (`build.yml`) cross-compiles for Linux/macOS/Windows × x64/aarch64 using `source.jdk` and `os.arch.classifier` Maven properties to point jlink at the target platform's JDK