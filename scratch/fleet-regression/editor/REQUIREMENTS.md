# Requirements: Navi editor v1

## About this document
- **Kind:** `requirements` (what the software must do and why — V-model stage 1 of 6)
- **Read by:** the downstream V stages — system (W-002, SYSTEM.md), architecture, design, implementation, and test-requirements (acceptance tests derive from the ACs here); **written by:** the requirements stage worker (role `pm`)
- **Related:** formalizes the product-owner PINNED SPEC reproduced **verbatim** in Appendix A; owning ticket **W-001** in the task store. The binding table is fixed: this document adds precision, never bindings.

## Goal
Specify Navi editor v1: a modal (NORMAL/INSERT) text-editor **core** defined as a pure function `apply(state, key) -> state` over a JSON-serializable `CoreState`, with a fixed, closed binding table — deterministic, pure Java 21, zero dependencies, single-file-source runnable — so it can be implemented and golden-vector verified through the V pipeline.

## Users
- **Primary:** an end user typing and editing text (via the thin terminal frontend, which is out of scope for v1 — see C-001).
- **Secondary:** Navi developers and testers, who consume the pure core directly by feeding key sequences and comparing JSON states.

## User Stories
- US-001: As a user, I want modal editing (NORMAL for commands, INSERT for typing) so that every keystroke has one unambiguous meaning.
- US-002: As a user, I want cursor navigation (arrows, Home/End, Ctrl+Home/Ctrl+End, PageUp/PageDown) so that I can move around the buffer quickly.
- US-003: As a user, I want to edit text (insert characters, newlines, delete chars, join lines) so that I can author content.
- US-004: As a user, I want undo so that my last changes are recoverable.
- US-005: As a user, I want to save the buffer to the loaded file and quit so that my work persists and I can exit.

## Terms
- **Navi core** — the pure editor state machine: `apply(state, key) -> state`.
- **Navi editor** — the whole v1 program: the core plus the save/quit behaviors (FR-025, FR-026).
- **Buffer / lines** — the text content as an ordered list of lines without newline terminators (`lines: string[]`).
- **Cursor** — insertion point `{line, col}`; `col` counts characters from line start (`0 ≤ col ≤ length(lines[line])`).
- **Printable character** — any non-control character (letters, digits, punctuation, symbols, space, and other non-control Unicode).
- **Loaded path** — the file path the editor was started with (if any). It is held **outside** `CoreState` (the pinned shape has no path field — see I-6, Q-002).
- **Change** — a key application after which `lines` differ from before (see I-4).
- **Snapshot** — a full deep copy of a `CoreState` (including its `undoStack` at that moment).

## Functional Requirements
EARS syntax. Mode scope follows the pinned wording (formalized as I-2): bindings the spec states **without** a mode qualifier hold in **both** modes; "in INSERT …" holds only in INSERT; "in NORMAL …" holds only in NORMAL.

### Modes & startup
- **FR-001 (ubiquitous):** THE Navi core SHALL have exactly two modes, NORMAL and INSERT.
- **FR-002 (event-driven):** WHEN the editor starts, the Navi core SHALL begin in NORMAL mode with an empty buffer (`lines = [""]`) and the cursor at line 0, col 0 (empty-buffer form per I-1).

### Cursor validity
- **FR-003 (ubiquitous):** THE Navi core SHALL keep the cursor valid at all times: `0 ≤ line ≤ lines.length − 1` and `0 ≤ col ≤ length(lines[line])`; any movement that would leave this range SHALL be clamped into it ("col clamps to line length"; no wrapping between lines — I-3).

### Navigation (both modes)
- **FR-004 (event-driven):** WHEN Left is pressed, the Navi core SHALL move the cursor one character left, clamped at col 0.
- **FR-005 (event-driven):** WHEN Right is pressed, the Navi core SHALL move the cursor one character right, clamped at the end of the current line.
- **FR-006 (event-driven):** WHEN Up is pressed, the Navi core SHALL move the cursor one line up, clamped at line 0, with col clamped to the new line's length.
- **FR-007 (event-driven):** WHEN Down is pressed, the Navi core SHALL move the cursor one line down, clamped at the last line, with col clamped to the new line's length.
- **FR-008 (event-driven):** WHEN Home is pressed, the Navi core SHALL move the cursor to the start of the current line (col 0).
- **FR-009 (event-driven):** WHEN End is pressed, the Navi core SHALL move the cursor to the end of the current line (col = length of the current line).
- **FR-010 (event-driven):** WHEN Ctrl+Home is pressed, the Navi core SHALL move the cursor to the buffer start (line 0, col 0).
- **FR-011 (event-driven):** WHEN Ctrl+End is pressed, the Navi core SHALL move the cursor to the buffer end (last line, col = that line's length).
- **FR-012 (event-driven):** WHEN PageUp is pressed, the Navi core SHALL move the cursor 12 lines up with the clamping of FR-003.
- **FR-013 (event-driven):** WHEN PageDown is pressed, the Navi core SHALL move the cursor 12 lines down with the clamping of FR-003.

### Mode switching
- **FR-014 (complex):** WHILE in NORMAL mode, WHEN `i` or Insert is pressed, the Navi core SHALL switch to INSERT mode, leaving buffer and cursor unchanged ("enters INSERT at cursor").
- **FR-015 (complex):** WHILE in INSERT mode, WHEN Esc is pressed, the Navi core SHALL switch to NORMAL mode, leaving buffer and cursor unchanged.

### INSERT-mode editing
- **FR-016 (complex):** WHILE in INSERT mode, WHEN a printable character is pressed, the Navi core SHALL insert it at the cursor and advance col by 1.
- **FR-017 (complex):** WHILE in INSERT mode, WHEN Enter is pressed, the Navi core SHALL insert a newline after the cursor line by splitting the cursor line at the cursor: the text right of the cursor moves to a new line inserted after the cursor line, and the cursor moves to col 0 of that new line (split-at-cursor per I-7).
- **FR-018 (complex, unwanted-branch):** WHILE in INSERT mode, WHEN Backspace is pressed, the Navi core SHALL delete the character left of the cursor (col decreases by 1); IF col = 0 and line > 0, THEN it SHALL instead join the current line onto the end of the previous line, placing the cursor at the join; IF col = 0 and line = 0, THEN it SHALL change nothing.
- **FR-019 (complex, unwanted-branch):** WHILE in INSERT mode, WHEN Delete is pressed, the Navi core SHALL delete the character at the cursor (col unchanged); IF the cursor is at the end of the line and a next line exists, THEN it SHALL instead join the next line onto the end of the current line (cursor unchanged); IF the cursor is at the end of the last line, THEN it SHALL change nothing.

### NORMAL-mode editing
- **FR-020 (complex, unwanted-branch):** WHILE in NORMAL mode, WHEN `x` is pressed, the Navi core SHALL delete the character at the cursor (col unchanged); IF the cursor is at the end of the line, THEN it SHALL change nothing.
- **FR-021 (event-driven):** WHILE in NORMAL mode, WHEN `d` is pressed, the Navi core SHALL delete the current line; if it was the only line, the buffer SHALL become a single empty line (`[""]`); the cursor SHALL be clamped per FR-003.

### Undo
- **FR-022 (complex, unwanted-branch):** WHILE in NORMAL mode, WHEN `u` is pressed, the Navi core SHALL undo the last change by popping the most recent snapshot from `undoStack` and restoring it as the complete new state (lines, cursor, mode, and undoStack); IF the stack is empty, THEN it SHALL change nothing. Restoring SHALL NOT push a new snapshot (no redo in v1).
- **FR-023 (event-driven):** WHEN a key application changes the buffer, the Navi core SHALL have pushed a full deep-copy snapshot of the pre-change `CoreState` (including its `undoStack` at that moment) onto `undoStack`; key applications that do not change the buffer SHALL push nothing (full-state snapshots per I-4).
- **FR-024 (ubiquitous):** THE Navi core SHALL cap `undoStack` at 100 snapshots ("stack max 100"); pushing a 101st SHALL discard the oldest.

### Save & quit (both modes)
- **FR-025 (complex, unwanted-branch):** WHEN Ctrl+S is pressed, IF a loaded path exists, THEN the Navi editor SHALL write the buffer to the loaded path; ELSE it SHALL do nothing ("no-op when no path"). Saving SHALL NOT change the `CoreState`.
- **FR-026 (event-driven):** WHEN Ctrl+Q is pressed, the Navi editor SHALL quit (terminate the editor without modifying the buffer and without implicit saving).

### Closed binding table
- **FR-027 (event-driven):** WHEN a pressed key has no binding in the current mode, the Navi core SHALL return the state unchanged. (The pinned table is closed — see C-002.)

### Core state model
- **FR-028 (ubiquitous):** THE Navi core SHALL implement the editor as a pure function `apply(state, key) -> state`: the same `(state, key)` always yields the same resulting state, with no dependence on, or mutation of, anything outside the inputs (see Q-001 for the side-effect-bearing save/quit bindings).
- **FR-029 (ubiquitous):** THE Navi core SHALL hold its state as `CoreState = {lines: string[], cursor: {line, col}, mode: NORMAL|INSERT, undoStack: CoreState[]}` — exactly these fields, `undoStack` being recursive full states.
- **FR-030 (ubiquitous):** THE Navi core SHALL serialize `CoreState` as JSON and deserialize it back losslessly, using exactly the field names of FR-029 (mode as the strings `"NORMAL"` / `"INSERT"`).

## Non-Functional Requirements
- NFR-TECH-001 (technology): The Navi core is pure Java 21 — language level and runtime JDK 21.
- NFR-DEP-001 (dependencies): Zero third-party dependencies — JDK standard library only (JSON handling for FR-030 included: no external JSON library).
- NFR-RUN-001 (runnability): Single-file-source runnable — the v1 deliverable launches via plain `java <File>.java` with no build step and no classpath additions.

(Determinism and golden-vector testability are not separate NFRs: they follow from FR-028 purity plus FR-030 JSON serialization.)

## Constraints & Assumptions
- C-001 (scope): v1 delivers the editor core specified by the pinned table; the terminal frontend is a later, out-of-v1 concern (carried by the regression plan, W-002).
- C-002 (fixed spec): The binding table in Appendix A is **verbatim and closed**. Adding, removing, or reinterpreting bindings is a product-owner spec change, not an implementation freedom. FR completeness against that table is this document's acceptance gate.
- C-003 (platform): JDK 21 only, standard library only (mirrors NFR-TECH-001/NFR-DEP-001).
- C-004 (workspace): All Navi work and artifacts live under `scratch/fleet-regression/editor/` only.
- A-001 (assumption): Input is a stream of key events as named in the binding table (arrows, Home/End, PageUp/PageDown, Ctrl+Home/Ctrl+End, Ctrl+S/Ctrl+Q, Esc, Insert, Delete, Backspace, Enter, printable characters). How a terminal encodes these keys is the frontend's concern (out of v1 scope).

## Acceptance Criteria
Given/When/Then; the test-requirements stage (V tip) expands these into acceptance tests.

- AC-001 (FR-001, FR-002): Given a fresh start, When the editor starts, Then mode is NORMAL, `lines = [""]`, cursor is `{line: 0, col: 0}`.
- AC-002 (FR-004–FR-007): Given buffer `["ab", "", "cdef"]`, cursor `{2, 4}`, When Up is pressed twice, Then the cursor is `{0, 0}` (col clamps: 4 → 0 → 0); When Down is pressed from `{0, 0}`, Then the cursor is `{1, 0}`; When Right is pressed on `["ab"]` at `{0, 1}`, Then the cursor stays `{0, 1}`; When Left is pressed at `{0, 0}`, Then the cursor stays `{0, 0}`.
- AC-003 (FR-008, FR-009): Given `["hello"]`, cursor `{0, 3}`, When Home is pressed, Then col is 0; When End is pressed, Then col is 5.
- AC-004 (FR-010, FR-011): Given `["a", "b"]`, cursor `{0, 1}`, When Ctrl+End is pressed, Then the cursor is `{1, 1}`; When Ctrl+Home is pressed, Then the cursor is `{0, 0}`.
- AC-005 (FR-012, FR-013): Given 30 one-char lines and cursor `{15, 0}`, When PageDown is pressed, Then line is 27; When PageUp is pressed from `{15, 0}`, Then line is 3; When PageDown is pressed from `{25, 0}`, Then line is 29 (clamped, not 37).
- AC-006 (FR-014): Given NORMAL mode, `["hi"]`, cursor `{0, 1}`, When `i` (or Insert) is pressed, Then mode is INSERT with buffer and cursor unchanged; pressing `i` again while in INSERT inserts the character `i` (FR-016).
- AC-007 (FR-016): Given INSERT mode, `["ab"]`, cursor `{0, 1}`, When `X` is pressed, Then `lines = ["aXb"]` and the cursor is `{0, 2}`.
- AC-008 (FR-017): Given INSERT mode, `["abcd"]`, cursor `{0, 2}`, When Enter is pressed, Then `lines = ["ab", "cd"]` and the cursor is `{1, 0}`.
- AC-009 (FR-018): Given INSERT mode, `["ab"]`, cursor `{0, 1}`, When Backspace is pressed, Then `lines = ["a"]`, cursor `{0, 0}`; given `["ab", "cd"]`, cursor `{1, 0}`, When Backspace is pressed, Then `lines = ["abcd"]`, cursor `{0, 2}`; given cursor `{0, 0}` on line 0, When Backspace is pressed, Then nothing changes.
- AC-010 (FR-019): Given INSERT mode, `["ab"]`, cursor `{0, 0}`, When Delete is pressed, Then `lines = ["b"]`; given `["ab", "cd"]`, cursor `{0, 2}`, When Delete is pressed, Then `lines = ["abcd"]`, cursor `{0, 2}`; given `["ab"]`, cursor `{0, 2}` (last line, EOL), When Delete is pressed, Then nothing changes.
- AC-011 (FR-015, FR-027): Given INSERT mode, When Esc is pressed, Then mode is NORMAL with buffer and cursor unchanged; given NORMAL mode, When Esc or an unbound key such as `z` is pressed, Then nothing changes.
- AC-012 (FR-020, FR-021): Given NORMAL mode, `["abc"]`, cursor `{0, 1}`, When `x` is pressed, Then `lines = ["ac"]`, cursor `{0, 1}`; given `["ab", "cd"]`, cursor `{0, 1}`, When `d` is pressed, Then `lines = ["cd"]` (cursor clamped onto it); given `["ab"]` as the only line, When `d` is pressed, Then `lines = [""]`.
- AC-013 (FR-022, FR-023): Given NORMAL mode, `[""]`, then typing `i`, `a`, `b` (buffer `["ab"]`), then Esc, When `u` is pressed, Then the full state is the snapshot before `b`: `lines = ["a"]` with the cursor and mode of that moment (full-state snapshot); When `u` is pressed twice more, Then the state reaches `[""]` and a further `u` changes nothing (empty-stack no-op).
- AC-014 (FR-024): Given 100 snapshots already on `undoStack`, When a new buffer change pushes a 101st, Then `undoStack` length is 100 and the oldest snapshot is the one discarded.
- AC-015 (FR-025): Given a loaded path and a modified buffer, When Ctrl+S is pressed, Then the buffer is written to that path and the `CoreState` is unchanged; given no loaded path, When Ctrl+S is pressed, Then nothing happens.
- AC-016 (FR-026): Given the running editor, When Ctrl+Q is pressed, Then the editor terminates with the buffer unmodified.
- AC-017 (FR-028): Given the same `(state, key)` applied twice, the resulting states are equal and the input state object is not mutated.
- AC-018 (FR-029, FR-030): Given any reachable state (including non-empty `undoStack`), When it is serialized to JSON and deserialized, Then the result equals the original, with exactly the field names `lines`, `cursor{line, col}`, `mode` (`"NORMAL"`/`"INSERT"`), `undoStack`.

## Formal interpretations (clarifications, not new bindings)
Each item disambiguates the pinned wording for testability; none adds or removes a binding (gate: C-002).

- I-1 (empty buffer): An "empty buffer" is `lines = [""]` — one empty line — because a zero-line buffer has no valid cursor line; startup cursor is `{0, 0}`.
- I-2 (mode scope): Bindings stated without a mode qualifier in the pinned spec (arrows, Home/End, Ctrl+Home/Ctrl+End, PageUp/PageDown, Ctrl+S, Ctrl+Q) hold in both modes; "in INSERT …" binds INSERT only; "in NORMAL …" binds NORMAL only. Unlisted keys are no-ops (FR-027) — so, e.g., `x`/`d`/`u` insert as printable characters in INSERT, and Esc in NORMAL is a no-op.
- I-3 (movement bounds): Horizontal moves clamp within the line (no wrapping to the previous/next line); vertical moves clamp line to the buffer range and col to the new line's length. Column "stickiness" (remembering a desired column across shorter lines) is **not** specified and is not required — col simply clamps.
- I-4 (change & snapshots): A "change" is a key application after which `lines` differ. Snapshots are full-state deep copies pushed only by changes (insertions, Enter, Backspace, Delete, `x`, `d`). Cursor moves, mode switches, `u`, Ctrl+S, Ctrl+Q, and no-op keys push nothing. Undo restores the whole state (including mode and cursor), and there is no redo in v1.
- I-5 (editor invariants): The buffer never becomes zero lines (FR-021's single-line case yields `[""]`), and the cursor is always valid (FR-003).
- I-6 (loaded path): The loaded path exists outside `CoreState` (the pinned shape has no path field); only Ctrl+S reads it. How a path is provided in v1 is not a binding — see Q-002.
- I-7 (Enter split point): "inserts a newline after cursor line" means the split happens at the cursor: right-of-cursor text moves to the new line, and the cursor lands at col 0 of it (the standard reading, consistent with the spec's other "at cursor" INSERT semantics).

## Open Questions
- Q-001: How do the side-effect-bearing bindings Ctrl+S (file write) and Ctrl+Q (termination) surface around the pure `apply(state, key) -> state` core (e.g., an effect/command representation)? Requirements pin both purity (FR-028) and the bindings (FR-025, FR-026); the mechanism is the system stage's decision (W-002, "file I/O boundary").
- Q-002: How is the loaded path provided or changed in v1 (no open command is bound)? Pinned is only: save targets the loaded path and is a no-op without one. Mechanism deferred to the system stage.

## Appendix A: PINNED SPEC (verbatim, product-owner authored)
Navi editor v1: two modes NORMAL and INSERT, startup NORMAL with empty buffer. Bindings: arrows move cursor (Left/Right chars, Up/Down lines, col clamps to line length); Home/End jump to line start/end; Ctrl+Home/Ctrl+End to buffer start/end; PageUp/PageDown move 12 lines; i or Insert enters INSERT at cursor; in INSERT printable characters insert at cursor, Enter inserts a newline after cursor line, Backspace deletes left char or joins previous line when col=0, Delete deletes right char or joins next line at EOL; Esc returns to NORMAL; in NORMAL x deletes char at cursor, d deletes current line, u undoes the last change (full-state snapshots, stack max 100), Ctrl+S saves the buffer to the loaded path (no-op when no path), Ctrl+Q quits. The core is a pure function apply(state, key) -> state; CoreState = {lines: string[], cursor: {line, col}, mode: NORMAL|INSERT, undoStack: CoreState[]} serialized as JSON. Work happens under scratch/fleet-regression/editor/ only.

## Appendix B: Binding → requirement traceability
| # | Pinned binding | FRs |
|---|---|---|
| 1 | two modes NORMAL and INSERT | FR-001 |
| 2 | startup NORMAL with empty buffer | FR-002 |
| 3 | arrows move cursor (Left/Right chars) | FR-004, FR-005 |
| 4 | Up/Down lines | FR-006, FR-007 |
| 5 | col clamps to line length | FR-003 (+ FR-006/007/012/013 effects) |
| 6 | Home/End jump to line start/end | FR-008, FR-009 |
| 7 | Ctrl+Home/Ctrl+End to buffer start/end | FR-010, FR-011 |
| 8 | PageUp/PageDown move 12 lines | FR-012, FR-013 |
| 9 | i or Insert enters INSERT at cursor | FR-014 |
| 10 | INSERT: printable characters insert at cursor | FR-016 |
| 11 | INSERT: Enter inserts a newline after cursor line | FR-017 |
| 12 | INSERT: Backspace deletes left char / joins previous line when col=0 | FR-018 |
| 13 | INSERT: Delete deletes right char / joins next line at EOL | FR-019 |
| 14 | Esc returns to NORMAL | FR-015 |
| 15 | NORMAL: x deletes char at cursor | FR-020 |
| 16 | NORMAL: d deletes current line | FR-021 |
| 17 | NORMAL: u undoes the last change | FR-022, FR-023 |
| 18 | full-state snapshots | FR-023, FR-029 |
| 19 | undo stack max 100 | FR-024 |
| 20 | Ctrl+S saves the buffer to the loaded path (no-op when no path) | FR-025 |
| 21 | Ctrl+Q quits | FR-026 |
| 22 | core is a pure function apply(state, key) -> state | FR-028 |
| 23 | CoreState shape, serialized as JSON | FR-029, FR-030 |
| 24 | closed binding table (no further bindings) | FR-027, C-002 |
