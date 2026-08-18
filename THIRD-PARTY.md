# Third-Party Licenses

This file aggregates the licenses and copyright notices of the third-party
libraries **redistributed with** the Hephaestus template (i.e. declared as
runtime dependencies of its bundled MCP servers). Hephaestus itself is
distributed under the **MIT License** — see [`LICENSE`](LICENSE).

All listed dependencies are permissive and fully compatible with the MIT
project license.

> The license texts and copyright holders below are the canonical SPDX texts with
> the best-known copyright holders. The authoritative source for each is the
> upstream package; regenerate this file at install time with
> `pip-licenses --from=mixed --with-license-file --format=markdown` against the `mcp/*/requirements.txt` files if you want a verified, environment-specific copy.

---

## Pillow (Python) — version ≥10.0

- **License:** HPND (Historical Permission Notice and Disclaimer)
- **Upstream:** https://python-pillow.org / https://github.com/python-pillow/Pillow
- **Used by:** `mcp/graphics`
- **Copyright:**
  - Copyright © 2010 by Jeffrey A. Crystal and contributors
  - Copyright © 1997-2011 by Secret Labs AB
  - Copyright © 1995-2011 by Fredrik Lundh

Permission to use, copy, modify, and distribute this software and its
associated documentation for any purpose and without fee is hereby granted,
provided that the above copyright notice appears in all copies, and that both
that copyright notice and this permission notice appear in supporting
documentation, and that the name of Secret Labs AB or the author not be used in
advertising or publicity pertaining to distribution of the software without
specific, written prior permission.

SECRET LABS AB AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS
SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO
EVENT SHALL SECRET LABS AB OR THE AUTHOR BE LIABLE FOR ANY SPECIAL, INDIRECT OR
CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
SOFTWARE.

---

## NumPy (Python) — version ≥1.24

- **License:** BSD-3-Clause
- **Upstream:** https://numpy.org / https://github.com/numpy/numpy
- **Used by:** `mcp/graphics`
- **Copyright:** Copyright (c) 2005-2024, NumPy Developers. All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
* Neither the name of the NumPy Developers nor the names of its contributors may
  be used to endorse or promote products derived from this software without
  specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

---

## Not redistributed (no attribution obligation for this template)

The following third-party components are **not** redistributed with Hephaestus,
so their full license texts are not aggregated here:

- **@opencode-ai/plugin** (Node, MIT) — the local opencode plugin. `.opencode/package.json`
  and `node_modules/` are **git-ignored** (see `.opencode/.gitignore`); it is a per-clone
  convenience, not part of the template.
- **GoogleTest** (C++, BSD-3-Clause, v1.17.0) — fetched on demand by the `cpp/` template via
  CMake `FetchContent` when `ENABLE_TESTING=ON`. Consumers fetch it themselves; it is never
  checked into this repository.
- **Doxygen** (build tool) — invoked via `find_package` when `ENABLE_DOXYGEN=ON`; a system
  documentation tool, not linked or redistributed.
