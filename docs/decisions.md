# Decisions

Dated decisions for this repo, newest last.

- 2026-11-14: created from the ops starter kit, area `minecraft-plugins`.

## 2026-11-14

- **Fieldnotes (owner, Helm chat, relayed verbatim):** "Yes — definitely. And if the game crashes at something he does, it attempts to gracefully log that before getting shut off". Name: "Fieldnotes (Recommended)". Built as `/bug` notes plus a rolling record of recent actions, saved on leaving (with the reason), on disable, from a JVM shutdown hook, and every few seconds. A hard kill can't be caught; the README says so.
