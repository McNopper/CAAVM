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

### On-demand graphics skills

Three extra skills use the **same lean template** but sit **outside** the V-model
process — they are graphics utilities you trigger on demand:

| Skill | Purpose |
|-------|---------|
| `graphics-window-screenshot`   | Capture only the rendered **client area** of a window (no title bar/borders). |
| `graphics-renderdoc-profiling` | GPU frame **capture & profiling** via the RenderDoc CLI (`renderdoccmd`). |
| `graphics-render-comparison`   | **Compare renderings** from different methods (diff images + PSNR/SSIM/FLIP). |

## Install & Use

The skills follow the GitHub Copilot CLI [agent skills](https://docs.github.com/copilot/how-tos/use-copilot-agents/use-copilot-cli)
format. To install them permanently, place each skill folder under your personal
skills directory so it loads in every session:

- macOS/Linux: `~/.copilot/skills/`
- Windows: `%USERPROFILE%\.copilot\skills\`

Copy (or symlink) the folders, e.g.:

```bash
# macOS/Linux — symlink so the skills stay in sync with this repo
ln -s "$(pwd)"/skills/* ~/.copilot/skills/
```

```powershell
# Windows PowerShell — copy the skill folders
Copy-Item -Recurse .\skills\* "$env:USERPROFILE\.copilot\skills\"
```

Then in the Copilot CLI:

- Run `/skills` to manage and confirm the installed skills.
- Run `/env` to verify they are loaded for the current session.

**Triggering:** skills are invoked automatically — Copilot matches your request
against each skill's `description`. Just describe the task (e.g. *"write the
requirements for ..."*, *"design the architecture for ..."*, *"add unit tests
for ..."*) and the matching skill activates. You can also name it explicitly,
e.g. *"use the software-architecture skill"*.

## License

[MIT](LICENSE) © 2026 Norbert Nopper.
