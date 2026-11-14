# Decisions

Dated decisions for this repo, newest last.

- 2026-11-14: created from the ops starter kit, area `minecraft-plugins`.

## 2026-11-14

- **Fieldnotes (owner, Helm chat, relayed verbatim):** "Yes — definitely. And if the game crashes at something he does, it attempts to gracefully log that before getting shut off". Name: "Fieldnotes (Recommended)". Built as `/bug` notes plus a rolling record of recent actions, saved on leaving (with the reason), on disable, from a JVM shutdown hook, and every few seconds. A hard kill can't be caught; the README says so.

## 2026-11-14

- **First release and going public.** The owner approved release tags for the staged jars (Helm chat, 2026-11-14, "Go (Recommended)"), under the standing yes of 2026-11-07 for a private Minecraft plugin repo to go public at its first release. Before going public: a history audit (no secrets, private terms, server addresses or player data; only noreply commit emails; no build output), the leak check in CI, an MIT LICENSE and a README status line.

- **Spelling (2026-11-14, owner, D194):** the name is spelled FieldNotes. The owner: "I think FieldNotes is the only one I'd change the case." Released as 0.1.1. On a case-sensitive disk the old plugins/Fieldnotes folder is copied across on first start.
