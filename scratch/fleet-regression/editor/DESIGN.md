# Design: Navi core (navi-core)

## About this document
- **Kind:** `design` (inside of the navi-core library: units, interfaces/contracts, data structures, key workflows, edge behavior — V-model stage 4 of 6)
- **Read by:** implementation (W-005 — codes `EditorCore.java` from these contracts) and the test stages (test-design verifies this transition table; W-006 builds its golden vectors from the key spellings, JSON form, and undoStack order pinned here); **written by:** the design stage worker (role `developer`)
- **Related:** derives from `ARCHITECTURE.md` (stage 3, W-003 — components, boundaries, dependency rules; this document changes none of them), `SYSTEM.md` (stage 2, W-002 — SD-001..SD-007), and `REQUIREMENTS.md` (stage 1, W-001 — FR-001..FR-030, interpretations I-1..I-7, Appendix A). Owning ticket **W-004**. The pinned binding table stays verbatim and closed (C-002): this document re-indexes it into canonical commands — no more, no fewer (FR-027, ARCHITECTURE.md Keymap row).

## Design inputs and freedoms
Architecture (W-003) handed four exact-shape decisions to this stage (its "Homes" table); SYSTEM.md handed one more:

| Item (upstream wording) | Pinned in section | Basis |
|---|---|---|
| Key representation & key-name spelling | Keymap — wire spelling | A-001, FR-027 |
| Command enumeration + façade shape ("exact façade and command-enum shapes are design's") | Keymap — canonical command set; Interfaces | AD-003, AD-005 |
| JSON codec structure (field names fixed by FR-029) | The JSON codec | FR-029, FR-030 |
| Snapshot deep-copy strategy | Undo design | FR-022, FR-023, AD-006 |
| File naming ("finalized at design stage", SYSTEM.md Technology Choices) | Single-file layout | SD-001, NFR-RUN-001 |

The **harness protocol** (vector file format, CLI shape) is deliberately not designed here — its architectural home is the driver/harness *with W-006* (SYSTEM.md part 3); this document only pins the observable seams it consumes (key names, canonical JSON, undoStack order, startup state).

## Design Patterns Used
- **Command (GoF, behavioral)** — every key event reifies as a canonical `Command` value; Core interprets commands, never key spellings (AD-003).
- **Memento (GoF, behavioral)** — undo stores full-state deep-copy snapshots inside `CoreState.undoStack` (AD-006, FR-023).
- **Facade (GoF, structural)** — `apply` / `effectOf` compose Keymap + Core behind the pinned signatures (AD-005, FR-028, SD-002).
- **State — considered, simplified.** Two modes do not justify polymorphic state objects; the mode is an enum guard column on the transition table (simpler, golden-vector friendly).

## Data structures
Java 21 shapes — records and enums, immutable values (NFR-TECH-001); file placement in Single-file layout.

```java
enum Mode { NORMAL, INSERT }
record Cursor(int line, int col) {}
record CoreState(String[] lines, Cursor cursor, Mode mode, CoreState[] undoStack) {}
record Key(String name) {}
sealed interface Command { /* 22 members — Keymap section */ }
enum Effect { NONE, SAVE, QUIT }
```

`CoreState` is the pinned shape, exactly four fields, `undoStack` recursive (FR-029). No path field — the loaded path stays outside, held by the driver (I-6, SD-003).

### Invariants (every reachable state; `apply` preserves them all)
| # | Invariant | Basis |
|---|---|---|
| INV-1 | `lines.length >= 1` — the buffer is never zero lines (a deleted only line becomes `[""]`) | I-5, FR-021 |
| INV-2 | every line is terminator-free and contains no control characters (only printable INSERT input ever enters lines) | A-001, FR-016 |
| INV-3 | `0 <= line <= lines.length − 1` and `0 <= col <= lines[line].length()` | FR-003 |
| INV-4 | `mode ∈ {NORMAL, INSERT}` | FR-001 |
| INV-5 | `0 <= undoStack.length <= 100`, ordered **oldest → newest** (index 0 oldest, last index newest); each element is itself a valid CoreState (recursively, with its own length bound) | FR-022..FR-024 |
| INV-6 | **write-once**: no command mutates its input state or any array reachable from it; every change yields new objects | FR-028, AC-017 |

### Startup state
`CoreState initial()` = `{lines: [""], cursor: {line: 0, col: 0}, mode: NORMAL, undoStack: []}` (FR-002, I-1, AC-001). The driver seeds the loop with it; every golden-vector session starts from it.

## The JSON codec (CoreState serialization — field by field)
In-file codec (NFR-DEP-001 — no external JSON library), grammar exactly the pinned field names (FR-030, AC-018).

### Field by field
| Field | Java type | JSON form | Serialization rules |
|---|---|---|---|
| `lines` | `String[]` | array of JSON strings | one element per line, in order; content verbatim, WITHOUT line terminator (INV-2 guarantees no control characters in reachable lines); JSON string escaping per below |
| `cursor` | `Cursor` | object `{"line": N, "col": N}` | exactly these two members, this key order in canonical output |
| `cursor.line` | `int` | JSON number | non-negative decimal integer; no plus sign, no fraction, no leading zeros (except `0` itself) |
| `cursor.col` | `int` | JSON number | same numeric form |
| `mode` | `Mode` | `"NORMAL"` or `"INSERT"` | exactly these two strings (FR-030) |
| `undoStack` | `CoreState[]` | array of CoreState objects | each element serialized by this very grammar (recursive); **index 0 = oldest snapshot, last index = newest** (undo's restore target); serialized length <= 100 |

### Canonical output form (what `toJson` emits)
1. Object member order is exactly `lines`, `cursor`, `mode`, `undoStack`; `cursor` prints `line` then `col` — byte-deterministic output, so golden vectors can compare strings (SD-005, FR-028, QA-002).
2. Compact: no insignificant whitespace; UTF-8.
3. String escaping: `\` as `\\`, `"` as `\"`, control characters below U+0020 as `\uXXXX` (unreachable per INV-2 — emitted defensively); every other character verbatim.

### Input parsing (what `fromJson` accepts)
1. Standard JSON whitespace tolerated between tokens; the four members may arrive in any order.
2. Strict: all four members present, each exactly once; unknown member names rejected; malformed JSON rejected; values violating INV-1..INV-5 rejected. Every failure is one `IllegalArgumentException` carrying the byte offset — no partial state is ever returned.
3. Decodes the standard escape set (`\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t`, `\uXXXX`).

### Round-trip law
For every state satisfying INV-1..INV-5: `fromJson(toJson(s))` equals `s` structurally, and equal states serialize to identical bytes (AC-018). Nesting depth is bounded by the undo cap (<= 100), so recursive (de)serialization cannot overflow the stack.

### Worked example (the AC-013 session — startup, then keys `i`, `a`, `b`, `Esc`)
Final state; note newest-last undo order and that each snapshot embeds the undoStack of its moment:
```json
{"lines":["ab"],"cursor":{"line":0,"col":2},"mode":"NORMAL","undoStack":[{"lines":[""],"cursor":{"line":0,"col":0},"mode":"INSERT","undoStack":[]},{"lines":["a"],"cursor":{"line":0,"col":1},"mode":"INSERT","undoStack":[{"lines":[""],"cursor":{"line":0,"col":0},"mode":"INSERT","undoStack":[]}]}]}
```

## Keymap: the closed key → command table
### Key wire spelling (A-001 — exact, case-sensitive, no whitespace)
Named keys: `Left` `Right` `Up` `Down` `Home` `End` `Ctrl+Home` `Ctrl+End` `PageUp` `PageDown` `Insert` `Delete` `Backspace` `Enter` `Esc` `Ctrl+S` `Ctrl+Q`.
Character keys: any single non-control character is its own key name (`i`, `x`, `d`, `u`, `A`, `7`, a space, any printable Unicode). Anything else is an unknown key. `Keymap` never throws — unknown keys map to NOOP (FR-027).

### Mode-scoped mapping (I-2: unqualified bindings hold in both modes)
| Key | in NORMAL | in INSERT | FRs |
|---|---|---|---|
| `Left` / `Right` / `Up` / `Down` | LEFT / RIGHT / UP / DOWN | same | FR-004..FR-007 |
| `Home` / `End` | HOME / END | same | FR-008, FR-009 |
| `Ctrl+Home` / `Ctrl+End` | TOP / BOTTOM | same | FR-010, FR-011 |
| `PageUp` / `PageDown` | PAGE_UP / PAGE_DOWN | same | FR-012, FR-013 |
| `Ctrl+S` / `Ctrl+Q` | SAVE / QUIT | same | FR-025, FR-026, I-2 |
| `i` | ENTER_INSERT | INSERT_CHAR('i') | FR-014, FR-016, AC-006 |
| `Insert` | ENTER_INSERT | NOOP | FR-014 (NORMAL-scoped), I-2 |
| `Esc` | NOOP | EXIT_INSERT | FR-015, I-2 |
| `x` / `d` / `u` | DELETE_HERE / DELETE_LINE / UNDO | INSERT_CHAR of the letter | FR-020..FR-022, FR-016 |
| any other printable character | NOOP | INSERT_CHAR(c) | FR-016, FR-027 |
| `Enter` / `Backspace` / `Delete` | NOOP | SPLIT_LINE / DELETE_BWD / DELETE_FWD | FR-017..FR-019, I-2 |
| anything else | NOOP | NOOP | FR-027 |

### Canonical command set (closed — the pinned table re-indexed, no more, no fewer)
Motions (both modes): `LEFT`, `RIGHT`, `UP`, `DOWN`, `HOME`, `END`, `TOP`, `BOTTOM`, `PAGE_UP`, `PAGE_DOWN`. Mode commands: `ENTER_INSERT`, `EXIT_INSERT`. INSERT edits: `INSERT_CHAR(c)`, `SPLIT_LINE`, `DELETE_BWD`, `DELETE_FWD`. NORMAL edits: `DELETE_HERE`, `DELETE_LINE`, `UNDO`. Effect commands: `SAVE`, `QUIT`. Totality sentinel: `NOOP`. **22 members**; every pinned binding maps to exactly one (completeness map in Traceability below).

## Core: the apply() transition table
`apply(state, key) = Core.dispatch(state, Keymap.map(state.mode, key))` — the façade composes Keymap + Core inside navi-core (AD-005, FR-028). Notation: `L` = lines, `len(i)` = `L[i]` length, `cur` = cursor, `last` = `L.length − 1`; **clamp col** on a line change = `col' = min(col, len(target line))` (no stickiness — I-3).

### One row per command — guard/mode, state effect, cursor rule
| # | Command | Guard | Lines effect | Cursor rule | Snapshot | FRs |
|---|---|---|---|---|---|---|
| 1 | `LEFT` | both | — | `col − 1`, floored at 0; no line wrap | no | FR-004 |
| 2 | `RIGHT` | both | — | `col + 1`, capped at `len(line)`; no wrap | no | FR-005 |
| 3 | `UP` | both | — | `line − 1` floored at 0, then clamp col | no | FR-006 |
| 4 | `DOWN` | both | — | `line + 1` capped at `last`, then clamp col | no | FR-007 |
| 5 | `HOME` | both | — | `col = 0` | no | FR-008 |
| 6 | `END` | both | — | `col = len(line)` | no | FR-009 |
| 7 | `TOP` | both | — | `{0, 0}` | no | FR-010 |
| 8 | `BOTTOM` | both | — | `{last, len(last)}` | no | FR-011 |
| 9 | `PAGE_UP` | both | — | `line − 12` floored at 0, then clamp col | no | FR-012 |
| 10 | `PAGE_DOWN` | both | — | `line + 12` capped at `last`, then clamp col | no | FR-013 |
| 11 | `ENTER_INSERT` | NORMAL | — | unchanged | no | FR-014 |
| 12 | `EXIT_INSERT` | INSERT | — | unchanged | no | FR-015 |
| 13 | `INSERT_CHAR(c)` | INSERT | insert `c` at cursor: `L[line] = L[line][0:col] + c + L[line][col:]` | `col + 1` | yes | FR-016 |
| 14 | `SPLIT_LINE` | INSERT | split at cursor: `L = [.., L[line][0:col], L[line][col:], ..]` — the right part becomes the new line at `line + 1` (I-7) | `{line + 1, 0}` | yes | FR-017 |
| 15 | `DELETE_BWD` | INSERT | `col > 0`: delete the left char; `col = 0 ∧ line > 0`: join — `L[line−1] += L[line]`, remove `L[line]`; `col = 0 ∧ line = 0`: nothing | `col > 0`: `col − 1`; join: `{line − 1, len(L[line−1]) before the join}`; else unchanged | iff lines changed | FR-018 |
| 16 | `DELETE_FWD` | INSERT | `col < len(line)`: delete the char at `col`; `col = len ∧ line < last`: join next — `L[line] += L[line+1]`, remove `L[line+1]`; `col = len ∧ line = last`: nothing | unchanged, all branches | iff lines changed | FR-019 |
| 17 | `DELETE_HERE` | NORMAL | `col < len(line)`: delete the char at `col` — **never joins**; `col = len(line)`: nothing | unchanged | iff lines changed | FR-020 |
| 18 | `DELETE_LINE` | NORMAL | remove `L[line]`; if it was the only line: `L = [""]` (INV-1) | with `L'` = lines after removal: `line' = min(line, len(L') − 1)`, then `col' = min(col, len(L'[line']))` | iff lines changed (`d` on `[""]` changes nothing — no push) | FR-021, I-5 |
| 19 | `UNDO` | NORMAL | stack non-empty: adopt the **last** snapshot as the complete new state (lines, cursor, mode, undoStack — time travel); empty: nothing | the restored snapshot's cursor | **never** (no redo) | FR-022 |
| 20 | `SAVE` | both | — (identity; the driver executes the SAVE effect) | unchanged | no | FR-025, SD-002 |
| 21 | `QUIT` | both | — (identity; the driver exits) | unchanged | no | FR-026, SD-002 |
| 22 | `NOOP` | — | — | unchanged | no | FR-027 |

- **"iff lines changed"** (I-4): the row pushes a deep snapshot of the pre-state only in branches that actually alter `lines`; no-op branches push nothing.
- **Identity rows** (11, 12, 20, 21, 22, and every no-op branch) return the input state reference — safe under INV-6, invisible in JSON.
- A command arriving under the wrong mode (unreachable via Keymap, e.g. `ENTER_INSERT` in INSERT) is treated as NOOP — the table stays total.

### effectOf(key) → Effect (SD-002 — pure, total, mode-independent)
`Ctrl+S` → `SAVE`; `Ctrl+Q` → `QUIT`; every other key → `NONE` (both modes, I-2).

### Dispatch workflow (one key application)
1. Driver reads one wire name from stdin → `Key` (any junk is just an unknown key).
2. `apply`: `Keymap.map(mode, key)` → `Command`.
3. `Core.dispatch`: guard column first (failed guard → identity); else compute lines′ and cursor′ per the row, and the snapshot per its push column (Undo design).
4. Driver prints `toJson(state′)` (every application prints — SD-005), then executes `effectOf(key)`: SAVE → FileStore write iff a loaded path exists; QUIT → exit without implicit save.

### Purity rules (FR-028, AC-017, QA-001)
**P-1 total** — defined for every (valid state, key); unbound → identity. **P-2 deterministic** — same inputs, same output. **P-3 non-mutating** — INV-6; the input object is never changed. **P-4 effect-free** — SAVE/QUIT transitions are identity; side-effect intent rides `effectOf` only (SD-002). No clock, random, threads, or ambient state (SD-007).

## Undo design (full-state snapshots, cap 100)
### What a snapshot is
`snapshot(S)` is a **recursive structural deep copy**: `new CoreState(S.lines.clone(), S.cursor, S.mode, deep copy of S.undoStack)` — strings, `Cursor`, and `Mode` are immutable values; every `undoStack` element is copied the same way. It captures **all four fields, including the undoStack as it was at that moment** (FR-023, I-4, AD-006). Under INV-6 (write-once arrays) a shared reference could never diverge from a copy — the copy is made anyway because the spec pins "full deep-copy snapshot", and it keeps INV-6 a local, testable discipline rather than a load-bearing global assumption. Cost: nesting is bounded by the cap (depth <= 100; worst-case ~5050 stored states, sum 1..100) — accepted at architecture (AD-006).

### Push (only when a change happens)
`undoStack′ = [...old, snapshot(S_pre)]` — appended at the **end** (newest last, INV-5). Editing commands only (rows 13–18); motions, mode switches, `UNDO`, `SAVE`, `QUIT`, NOOP never push (I-4).

### Cap (FR-024, AC-014)
After appending, if length > 100: drop from the **front** (the oldest snapshot is discarded). Length never exceeds 100.

### Restore (`u`, NORMAL only)
Stack non-empty: the new state **is the last snapshot adopted wholesale** — its lines, cursor, mode *and* undoStack become current. The restored mode may be INSERT — undo is time travel, not a buffer patch. Restore never pushes; there is no redo in v1 (FR-022). Empty stack: identity.

### Worked example (AC-013)
| after key | lines | cursor | mode | undoStack (oldest→newest) | event |
|---|---|---|---|---|---|
| startup | `[""]` | {0,0} | NORMAL | `[]` | — |
| `i` | `[""]` | {0,0} | INSERT | `[]` | mode switch, no push |
| `a` | `["a"]` | {0,1} | INSERT | `[snap([""],{0,0},INSERT,[])]` | push pre-state, then insert |
| `b` | `["ab"]` | {0,2} | INSERT | `[snap₁, snap(["a"],{0,1},INSERT,[snap₁])]` | push pre-state (with its stack), insert |
| `Esc` | `["ab"]` | {0,2} | NORMAL | unchanged | no push |
| `u` | `["a"]` | {0,1} | **INSERT** | `[snap₁]` | adopts snap(["a"],…) wholesale — mode restored too |
| `u` | `[""]` | {0,0} | INSERT | `[]` | adopts snap₁ |
| `u` | `[""]` | {0,0} | INSERT | `[]` | empty stack: identity |

## Edge-case matrix (the six ticket boundary cases; EOL/EOF split for review clarity)
| # | Case | Setup | Command(s) | Expected behavior | Refs |
|---|---|---|---|---|---|
| 1 | empty buffer | `[""]`, {0,0} (startup) | all motions; `x`, `d`, Backspace, Delete, `u`; typing | every motion stays {0,0}; DELETE_HERE, DELETE_BWD, DELETE_FWD are no-ops; DELETE_LINE yields `[""]` again and **pushes nothing** (lines unchanged); UNDO on empty stack is a no-op; INSERT_CHAR('a') → `["a"]`, {0,1} | FR-002, I-1, I-5, AC-001 |
| 2 | col clamping after edits | `["abcdef"]`, {0,5}; multi-line variants | DELETE_LINE, DELETE_HERE, UNDO | any edit that shortens or replaces the cursor line re-clamps on exit: e.g. DELETE_LINE from {0,5} lands `line' = min(line, len(L') − 1)` and `col' = min(col, len(new line))`; DELETE_HERE deletion keeps col <= len; UNDO restores the snapshot's cursor wholesale | FR-003, I-3 |
| 3 | cursor at EOL (`col = len(line)`) | `["ab"]`, {0,2} | RIGHT, `x`, typing, DELETE_FWD, END | RIGHT stays (clamped); `x` is a no-op — **never joins**; INSERT_CHAR appends (`["abX"]`, {0,3}); DELETE_FWD joins the next line if one exists, else no-op; END idempotent | FR-005, FR-016, FR-019, FR-020, FR-009 |
| 4 | cursor at EOF (last line) | `["a","b"]`, {1,1} | DOWN, PAGE_DOWN, Ctrl+End, `d` | DOWN stays; PAGE_DOWN clamps line to 1; BOTTOM idempotent {1,1}; DELETE_LINE removes line 1 → cursor {0, min(1,1)} = {0,1} on `"a"` | FR-007, FR-013, FR-011, FR-021 |
| 5 | Backspace at col 0 | `["ab","cd"]`, {1,0}; and {0,0} on line 0 | DELETE_BWD | line > 0: join onto the previous line → `["abcd"]`, cursor {0,2} (at the join); line 0 col 0: identity, no snapshot | FR-018, AC-009 |
| 6 | Delete at EOL | `["ab","cd"]`, {0,2}; `["ab"]`, {0,2} | DELETE_FWD vs `x` | INSERT Delete: next line exists → join → `["abcd"]`, cursor {0,2} unchanged; last line → no-op. Contrast: NORMAL `x` at EOL is always a no-op — Delete joins, `x` never does | FR-019, FR-020, AC-010 |
| 7 | PageUp/PageDown clamping | 30 one-char lines; {15,0} and {25,0} | PAGE_UP / PAGE_DOWN | PAGE_UP: `line − 12` floored at 0 ({15,0} → 3); PAGE_DOWN: `line + 12` capped at 29 ({25,0} → 29, not 37); col clamps to the target line's length in both | FR-012, FR-013, FR-003, AC-005 |

## Single-file layout (SD-001, NFR-RUN-001)
File name — finalized here (SYSTEM.md Technology Choices): **`EditorCore.java`**, under `scratch/fleet-regression/editor/` (C-004), launched `java EditorCore.java [path]` (SD-003). One top-level class `EditorCore` (declared first, holds `main` — JEP 330 launches the first top-level class); all other types are nested static members, grouped in two comment-marked zones:

```java
public class EditorCore {
    // === navi-core (pure — ARCHITECTURE dependency rules 1–2) ===
    //   enum Mode; record Cursor; record CoreState; record Key;
    //   sealed interface Command; enum Effect;
    //   Keymap.map(Mode, Key) -> Command
    //   Core.initial() / Core.apply(CoreState, Key) / Core.effectOf(Key)
    //   Json.toJson(CoreState) / Json.fromJson(String)
    // === application (impure — driver is the composition root, SD-004) ===
    //   main(String[] argv): seed initial(), run the dispatch loop,
    //   FileStore.save(lines, path) on SAVE; exit on QUIT;
    //   harness mode (vector replay) — protocol owned by W-006
}
```

The comment boundary is the logical library boundary of AD-002 inside the one file; the dependency rules of ARCHITECTURE.md (Keymap → Core types only; nothing → Renderer; driver sees all) still hold member-to-member.

## Interfaces / contracts (what W-005 implements and W-006 drives)
| Signature | Contract |
|---|---|
| `static CoreState initial()` | the startup state (FR-002) |
| `static CoreState apply(CoreState s, Key k)` | pure and total; precondition: `s` satisfies INV-1..5 (violation → `IllegalArgumentException`, defensive — unreachable from driver/harness); never mutates `s`; behavior exactly the transition table |
| `static Effect effectOf(Key k)` | SAVE / QUIT / NONE per SD-002, mode-independent |
| `static String toJson(CoreState s)` | canonical output form (byte-deterministic) |
| `static CoreState fromJson(String json)` | strict parser, round-trip law, fail-fast exceptions |
| `static void save(String[] lines, java.nio.file.Path pathOrNull)` (FileStore, application side) | UTF-8, LF, terminator after the last line too (SD-006); `null` path → no-op (FR-025); write failure reported on stderr, loop continues, core state unaffected |

Driver loop contract: read one key name → `apply` → print `toJson(state′)` → execute `effectOf` (SAVE via FileStore iff a loaded path exists; QUIT exits without implicit save — FR-026). Every key application prints its state (SD-005) — this stdout stream is the subprocess seam W-006 drives.

## Error handling (contract level)
| Condition | Behavior |
|---|---|
| unknown or malformed key name at Keymap | NOOP — Keymap never throws (FR-027) |
| `apply` receives a state violating INV-1..5 | `IllegalArgumentException` (defensive; the driver and harness only ever build valid states) |
| `fromJson`: malformed JSON, missing/duplicate member, unknown member, invariant violation | `IllegalArgumentException` with byte offset; no partial result |
| FileStore write failure (driver-side) | stderr report; editor continues; `CoreState` unaffected (FR-025 untouched) |

## Hand-off notes
- **W-005 (implementation)** owns everything below these contracts (method decomposition, locals, tests' naming) — observable behavior must match this document table-for-table; nothing here may be re-decided.
- **W-006 (golden vectors)** owns the vector file format and harness CLI (architectural home: driver/harness, with W-006). The seams it drives are pinned here: key wire spelling, canonical JSON (incl. undoStack newest-last), startup state, and the transition table as the expected-behavior oracle.

## Open Questions
None blocking. All remaining choices are enumerated implementation freedoms (Hand-off notes) and the W-006-owned harness protocol.

## Traceability: commands → REQUIREMENTS.md
| Command(s) | FRs |
|---|---|
| LEFT, RIGHT, UP, DOWN | FR-004–FR-007 |
| HOME, END | FR-008, FR-009 |
| TOP, BOTTOM | FR-010, FR-011 |
| PAGE_UP, PAGE_DOWN | FR-012, FR-013 |
| ENTER_INSERT, EXIT_INSERT | FR-014, FR-015 |
| INSERT_CHAR, SPLIT_LINE, DELETE_BWD, DELETE_FWD | FR-016–FR-019 |
| DELETE_HERE, DELETE_LINE | FR-020, FR-021 |
| UNDO (+ snapshot/cap mechanics) | FR-022–FR-024 |
| SAVE, QUIT | FR-025, FR-026 |
| NOOP | FR-027 |
| CoreState + JSON codec | FR-029, FR-030 |
| apply façade + purity rules | FR-028 |

**Completeness against the pinned table** (REQUIREMENTS.md Appendix B rows 1–24): rows 1–2 (modes, startup) → Data structures + startup; 3–8 (navigation) → rows 1–10; 9 (i/Insert) → ENTER_INSERT; 10–13 (INSERT edits) → rows 13–16; 14 (Esc) → EXIT_INSERT; 15–16 (x, d) → rows 17–18; 17–19 (u, snapshots, cap) → UNDO + Undo design; 20 (Ctrl+S) → SAVE + FileStore contract; 21 (Ctrl+Q) → QUIT; 22 (pure apply) → dispatch + purity rules; 23 (CoreState JSON) → JSON codec; 24 (closed table) → NOOP + keymap totality. **No gaps; no additions.**

## Acceptance-criteria map (ticket W-004)
| Ticket AC | Where met |
|---|---|
| 1. DESIGN.md exists under scratch/fleet-regression/editor/ with an About-this-document section | top of this file |
| 2. The transition table covers EVERY command from the pinned spec | transition table rows 1–22 + keymap mapping + the Appendix-B completeness map (Traceability) |
| 3. The edge-case matrix lists all six boundary cases with expected behavior | Edge-case matrix rows 1–7 (EOL/EOF split into rows 3/4) |
| 4. CoreState JSON serialization is specified field by field | JSON codec — field-by-field table + canonical output + parsing + round-trip law |
| 5. Only files under scratch/fleet-regression/editor/ are touched | this file is the only work artifact (verified via git status) |
