# 🔱 Hephaestus

> *[Hephaestus](https://en.wikipedia.org/wiki/Hephaestus) — Greek god of the forge, and the one who
> built automatons (Talos, the golden mechanical attendants). A fitting patron for a disciplined,
> agent-driven build process.*

## Skills

Hephaestus provides ten agent skills arranged as a **V-model** software lifecycle. The
left side defines the software; the right side verifies it. Each definition skill is
paired with exactly one verification skill, and every skill is scoped so it does not
overlap its neighbours. The skills are deliberately kept at a lean **hobby-project**
level — minimal but useful; a heavier production variant could live as a separate set.

| Definition (left)                  | ↔ | Verification (right)                   |
|------------------------------------|---|----------------------------------------|
| `01-software-requirements`         | ↔ | `10-software-acceptance-test`          |
| `02-software-system`               | ↔ | `09-software-integration-test`         |
| `03-software-architecture`         | ↔ | `08-software-module-test`              |
| `04-software-design`               | ↔ | `07-software-component-test`           |
| `05-software-implementation`       | ↔ | `06-software-unit-test`                |

Each skill lives in `skills/<name>/SKILL.md` and follows the same lean template:
V-model position, hobby-level scope, core principles, a compact default output, and
hand-off guidance to neighbouring skills.

## License

[MIT](LICENSE) © 2026 Norbert Nopper.
