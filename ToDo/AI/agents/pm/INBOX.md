<!--
  AI · PM INBOX · Internal bubble-up · Worker agents → PM
  Location:  AI/ — the workspace. Humans do NOT read this; the PM absorbs it.
  Flow:      A worker agent (any instance) appends an issue here when it hits the
             edge of its autonomy. The PM triages every item:
               - resolves it internally (reassign, resequence, unblock), OR
               - escalates it to the human in ../../../Human/DECISIONS.md.
  This is the noise filter that keeps the human's DECISIONS.md quiet.
  Fill every {{ placeholder }} and delete guidance comments before use.
-->

# INBOX — issues bubbling up to the PM

> Where worker agents raise anything they cannot decide alone. The PM works this
> queue and decides, for each, whether it stays in AI/ or gets escalated to the human.

## Legend

- **Kind:** `question` · `blocker` · `decision` · `risk` · `fyi`
- **State:** `open` (needs PM) · `pm-resolved` · `escalated` (→ Human/DECISIONS.md D-x)

---

## Open (newest first)

### I-{{ id }} · {{ short title }}
- **Raised by:** {{ agent instance, e.g. developer-02 }} at {{ time }}
- **Kind:** {{ question / blocker / decision / risk / fyi }}
- **Blocking that agent?** {{ yes / no }}
- **Detail:** {{ what the agent needs; what it already tried }}
- **Agent's ask:** {{ the specific unblock / answer }}
- **PM disposition:** _{{ pending }}_

---

## PM-resolved (newest first)

### I-{{ id }} · {{ short title }}
- **Resolved by PM:** {{ time }} — {{ what the PM did (reassigned / resequenced / answered) }}

## Escalated (newest first)

### I-{{ id }} → Human/DECISIONS.md D-{{ id }}
- **Escalated:** {{ time }} — reason: {{ crosses BRIEF autonomy boundary }}
