# Test audit

Kept by this repo's owning session, per `~/Code/CLAUDE.md`, "Test self-audits". Newest first.

## 2026-11-14: first audit (starter kit)

- Promises: none yet.
- Covered: none yet.
- Gaps added: none.
- Gaps skipped (and why): none.

## 2026-11-14: first version

| Promise | Covered by |
| --- | --- |
| What's typed after a command never reaches a file | unit `commandsAreRecordedByNameOnlySoPrivateWordsNeverReachTheFile` |
| Each entry stays on one line | unit `fieldsStayOnOneLine` |
| Facing is right | unit `facingFollowsMinecraftsYaw` |
| Only the last N actions are kept; unchanged records aren't rewritten | unit `theRecordKeepsOnlyTheLastFewAndSaysWhenItChanged` |
| Notes append; records are replaced whole, never half-written | unit `notesAppendAndRecordsReplaceWhole` |
| `/bug`, the leave reason and the saves on stop | bots on the test clone (D171) |
