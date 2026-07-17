# Dismissed Review Issues

Issues listed here are suppressed in future PR reviews.

Each entry should record a **Pattern** (what the reviewer keeps flagging), a **Reason**
(why it is acceptable in this codebase), the **Dismissed** date, and **Dismissed by**.
Add an entry only for a recurring false-positive that is genuinely an intentional
Soularium v2 convention — not to silence a real finding.

---

## Presenter `UiState` exposing `MutableState` for transient view state
**Pattern**: Flagging a Circuit `UiState` that holds a `MutableState<T>` (or similar stable-but-mutable holder) for transient view state — e.g. a menu open/close toggle or a text-field value — that the Layout reads and writes directly, instead of a plain value plus a `UiEvent` round-trip through the presenter.
**Reason**: `UiState` only needs to be *stable*, not immutable, and `MutableState` is a stable type, so this doesn't break Compose. Hoisting transient view state into `UiState` keeps it observable/controllable from the presenter (and tests) while avoiding a value-down/event-up round-trip through the presenter's recomposition loop — that round-trip can add thread context switches and introduce lag/jank (most visible with text fields routing every keystroke through the presenter).
**Dismissed**: 2026-07-17
**Dismissed by**: Daniel Frett

<!--
Template:

## Short title of the recurring finding
**Pattern**: What the review agents repeatedly flag
**Reason**: Why this is intentional / acceptable in Soularium v2
**Dismissed**: YYYY-MM-DD
**Dismissed by**: Name
-->
