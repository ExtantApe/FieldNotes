# Fieldnotes

Fieldnotes for Paper: testers type `/bug` to leave a private note with where they were, and their last moments are saved if the game crashes.

**Status:** early, version 0.1.0. Built and unit-tested. Bots on a throwaway Paper 26.2 server test it before a play session; it hasn't been used in a real game client yet.

## What it does

- **`/bug <what happened>`** saves the note with the player's name, world, block position, which way they faced, their game mode and the time, to `plugins/Fieldnotes/notes.log`. It's written to disk before the player sees "Noted". Nobody else sees it in chat.
- **Recent actions.** For each player with `fieldnotes.watched`, it keeps their last 80 actions: blocks broken and placed, clicks, commands (by name only), teleports, world changes, damage, deaths and containers opened. They go to `plugins/Fieldnotes/recent/<name>.log` every 5 seconds if anything changed.
- **When they leave**, the record gets a last line saying how: left, kicked (with the reason), or timed out. A crashed or frozen game shows up as a time-out or a lost connection. The whole record is also added to `sessions.log`.
- **When the server stops**, every record is saved, from the plugin's own shutdown and again from a hook that runs as the Java process exits.

## Limits

- A hard kill of the server (kill -9, a power cut) can't be caught. At most the last 5 seconds of actions are lost then.
- The game client crashing can't be seen directly. The server only sees the connection drop or time out, usually within 30 seconds.
- Chat and what's typed after a command are never recorded, so private messages and passwords stay out of the files.

## Files

Everything is plain text in `plugins/Fieldnotes/`, one line per entry, fields separated by ` | `, times in UTC. The files hold player names and positions, so keep the folder private.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| `recent-actions` | 80 | How many recent actions to keep per player (10 to 1000). |
| `flush-seconds` | 5 | How often changed records are written (1 to 60). |

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `fieldnotes.note` | everyone | Use `/bug` (also `/fieldnote`). |
| `fieldnotes.watched` | everyone | Keep a record of this player's recent actions. |

## Building

`./gradlew build` with Java 25. The jar is in `build/libs/`.

## License

Not decided yet. Private repos carry no license.
