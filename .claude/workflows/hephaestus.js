export const meta = {
  name: 'hephaestus',
  description: 'Cyclic Agentic Agile V-Model with an MVP maturity ladder: ONE run advances the product by one maturity level (loop) across the backlog — re-run to deepen until INPUT.md is fully resolved. Per increment it runs a full V-pass (requirements->software-system->architecture->design->implementation, mirrored by acceptance/system/module/component/unit tests) with TDD, adversarial verification, a clean-code refactor pass, and quality gates, committing each phase onto the working branch. Language/toolchain are fully config-driven (default: C++).',
  whenToUse: 'Building or extending software increment-by-increment with V-Model traceability, TDD, and enforced clean-code gates. Re-runnable: each invocation is one MVP/deepening loop. Pass the parsed config/hephaestus.config.yaml as args (or rely on the built-in C++ defaults).',
  phases: [
    { title: 'Setup' },
    { title: 'Intake', model: 'sonnet' },
    { title: 'Requirements', model: 'sonnet' },          // 1  (validated by 10)
    { title: 'Software System', model: 'opus' },         // 2  topology + deployables/executables (validated by 9)
    { title: 'Architecture', model: 'opus' },            // 3  modules define each deployable (validated by 8)
    { title: 'Design', model: 'opus' },                  // 4  components define modules (validated by 7)
    { title: 'Implementation (TDD)', model: 'sonnet' },  // 5  units define components (validated by 6)
    { title: 'Integrate', model: 'sonnet' },             // on demand: assemble components→modules→deployables→system
    { title: 'Unit Tests', model: 'haiku' },             // 6  → falls back to 5
    { title: 'Component Tests', model: 'haiku' },        // 7  → falls back to 4 (needs mocking)
    { title: 'Module Tests', model: 'haiku' },           // 8  → falls back to 3 (needs mocking)
    { title: 'System Tests', model: 'haiku' },           // 9  → falls back to 2 (deployables run together)
    { title: 'Acceptance', model: 'haiku' },             // 10 → falls back to 1 (run the system, e.g. screenshots / E2E) — runs on models.verification
    { title: 'Iteration Gate', model: 'opus' },
    { title: 'Report', model: 'sonnet' },
  ],
}

// ===========================================================================
//  CONFIG — built-in C++ defaults. The `args` you pass (parsed from
//  config/hephaestus.config.yaml) is merged ON TOP of these, so you only need
//  to supply what differs. This keeps the script the source of behavior and
//  the YAML the source of project parameters.
// ===========================================================================
const DEFAULTS = {
  project: { name: 'project', description: '', backlog: [] },
  language: { name: 'C++', standard: 'c++23', compilers: ['clang++ >= 17', 'g++ >= 13'], style_guide: 'C++ Core Guidelines' },
  toolchain: {
    build_system: { tool: 'cmake', min_version: '3.28' },
    package_manager: { tool: 'vcpkg' },
    formatter: { tool: 'clang-format', config_file: '.clang-format', fix_command: 'clang-format -i $FILES', check_command: 'clang-format --dry-run --Werror $FILES' },
    linters: [
      { tool: 'clang-tidy', config_file: '.clang-tidy', command: 'clang-tidy -p build $FILES', fail_on_warning: true },
      { tool: 'cppcheck', command: 'cppcheck --enable=all --error-exitcode=1 src/', fail_on_warning: true },
    ],
    static_analysis: [{ tool: 'clang static analyzer', command: 'scan-build cmake --build build' }],
    sanitizers: ['asan', 'ubsan', 'tsan'],
    test_frameworks: {
      unit: { tool: 'GoogleTest', mock: 'GoogleMock' },
      component: { tool: 'GoogleTest' },
      integration: { tool: 'ctest' },
      acceptance: { tool: 'ctest + scenario harness' },
    },
    coverage: { tool: 'llvm-cov', report_command: 'llvm-cov report' },
    docs: { tool: 'doxygen', config_file: 'Doxyfile' },
  },
  v_model: {
    composition: {
      unit_to_component: 'one or more units : one component',
      component_to_module: 'one or more components : one module (a component may be reused across modules, built once)',
      module_to_software: 'one or more modules : one software (executable/deployable)',
      software_to_system: 'one or more softwares (executables) : one software system (topology, e.g. client-server)',
    },
    requirements: { verifies_with: 'acceptance test (whole running system, end-to-end)' },
    software_system: { verifies_with: 'system test (deployables run together)' },
    architecture: { verifies_with: 'module / integration test (mocked)' },
    design: { verifies_with: 'component test (mocked)' },
    implementation: { verifies_with: 'unit test' },
    test_execution_order: ['unit', 'component', 'module', 'system', 'acceptance'],
  },
  agile: {
    iteration_name: 'sprint', tdd: true, max_refactor_rounds: 2, max_gate_retries: 2, max_fix_rounds: 2,
    definition_of_done: [
      'All five test levels green (unit, component, module, system, acceptance) for the requirements IN SCOPE at this maturity level.',
      "The EFFECTIVE quality_gates for this maturity level satisfied (strict gates with the level's relaxations merged on top).",
      'Requirements <-> tests traceability matrix complete for the in-scope requirements.',
      'No new linter/formatter/sanitizer findings.',
      'Public API documented (minimal & effective; coverage scaled by the maturity level).',
      'Each phase committed on the working branch with its per-phase trace file written; increment report written and living artifacts updated.',
    ],
  },
  quality_gates: {
    unit_line_coverage_min: 80, unit_branch_coverage_min: 70, cyclomatic_complexity_max: 10,
    function_length_max_lines: 60, clang_tidy_warnings_max: 0, cppcheck_warnings_max: 0,
    format_check: 'enforced', sanitizers_clean: true, public_api_doc_coverage_min: 100,
    traceability: 'every requirement maps to >=1 acceptance test',
  },
  clean_code: {
    principles: ['SOLID', 'DRY', 'KISS', 'YAGNI', 'Law of Demeter', 'Composition over inheritance'],
    architecture: 'Ports & Adapters (Hexagonal) — domain core has no I/O dependencies',
    dependency_rule: 'source dependencies point inward, toward the domain',
    naming: 'intention-revealing; no abbreviations',
    error_handling: 'exceptions for exceptional flow; std::expected/Result for expected failures',
    resource_management: 'RAII everywhere; smart pointers at ownership boundaries',
    forbidden_smells: ['god class / long parameter list', 'primitive obsession', 'feature envy', 'shotgun surgery', 'duplicated logic', 'deep nesting (> 3 levels)'],
  },
  references: {
    software_architecture_patterns: {
      source: 'https://tecnovy.com/en/top-10-software-architecture-patterns',
      catalog: ['Layered (N-Tier)', 'Client-Server', 'Microservices', 'Event-Driven', 'MVC', 'Service-Oriented (SOA)', 'Repository', 'CQRS', 'Domain-Driven Design (DDD)', 'Peer-to-Peer'],
    },
    design_patterns: {
      source: 'https://refactoring.guru/design-patterns',
      creational: ['Factory Method', 'Abstract Factory', 'Builder', 'Prototype', 'Singleton'],
      structural: ['Adapter', 'Bridge', 'Composite', 'Decorator', 'Facade', 'Flyweight', 'Proxy'],
      behavioral: ['Chain of Responsibility', 'Command', 'Iterator', 'Mediator', 'Memento', 'Observer', 'State', 'Strategy', 'Template Method', 'Visitor'],
    },
    refactoring: {
      source: 'https://refactoring.guru/refactoring',
      smells: ['Long Method', 'Large Class', 'Primitive Obsession', 'Long Parameter List', 'Data Clumps', 'Switch Statements', 'Refused Bequest', 'Divergent Change', 'Shotgun Surgery', 'Duplicate Code', 'Dead Code', 'Speculative Generality', 'Feature Envy', 'Inappropriate Intimacy', 'Message Chains', 'Middle Man'],
      techniques: ['Extract Method', 'Inline Method', 'Extract Variable', 'Replace Temp with Query', 'Move Method', 'Extract Class', 'Hide Delegate', 'Decompose Conditional', 'Replace Nested Conditional with Guard Clauses', 'Replace Conditional with Polymorphism', 'Introduce Null Object', 'Extract Superclass', 'Extract Interface', 'Form Template Method', 'Replace Inheritance with Delegation'],
    },
    documentation: {
      source: 'https://docs.arc42.org/home/',
      template: 'arc42',
      sections: ['Introduction and Goals', 'Constraints', 'Context and Scope', 'Solution Strategy', 'Building Block View', 'Runtime View', 'Deployment View', 'Crosscutting Concepts', 'Architecture Decisions', 'Quality Requirements', 'Risks and Technical Debt', 'Glossary'],
    },
  },
  // MVP maturity ladder: one run advances the product by ONE level across the
  // backlog; re-run to climb. Each level may relax quality_gates (merged on top).
  strategy: {
    approach: 'mvp',  // mvp | full
    maturity_levels: [
      { name: 'mvp', intent: 'Thinnest end-to-end vertical slice that delivers user-visible value. Happy path only; defer edge cases and non-functional hardening as logged debt. Architecture extensible (not throwaway), but resist gold-plating (YAGNI).', gates: { unit_line_coverage_min: 50, unit_branch_coverage_min: 40, public_api_doc_coverage_min: 0 } },
      { name: 'harden', intent: 'Add the edge cases, error handling, and non-functional requirements deferred at MVP. Pull items from the debt log and from INPUT.md ideas not yet resolved. Tighten robustness without changing scope.', gates: { unit_line_coverage_min: 70, unit_branch_coverage_min: 60, public_api_doc_coverage_min: 80 } },
      { name: 'complete', intent: 'Fully resolve INPUT.md: every requirement implemented, full robustness, documentation complete, strict gates enforced. Nothing deferred.', gates: {} },
    ],
  },
  // Commit-per-phase: each phase lands its content on the working branch as it
  // finishes; parallel impl worktrees are merged back in the Integrate phase.
  git: { commit_per_phase: true, branch: 'current', worktree_merge: true, commit_prefix: 'hephaestus' },
  toggles: { documentation: 'minimal' },  // full | minimal | off — see docInstruction below
  // Per-phase model routing (opus | sonnet | haiku). A phase falls back to
  // `default`; if that is unset too, the agent inherits the session model.
  models: {
    default: 'sonnet',
    requirements: 'sonnet',
    system: 'opus',
    architecture: 'opus',
    design: 'opus',
    implementation: 'sonnet',
    verification: 'haiku',
    refactor: 'sonnet',
    gate: 'opus',
    intake: 'sonnet',
    report: 'sonnet',
  },
  interface: { input: 'INPUT.md', output: 'OUTPUT.md' },
  layout: { source_dir: 'src/', include_dir: 'include/', test_dir: 'tests/', docs_dir: 'docs/', build_dir: 'build/' },
}

// shallow-by-section merge: args overrides DEFAULTS one level deep per section
function mergeConfig(base, over) {
  const out = {}
  for (const k of Object.keys(base)) {
    const b = base[k], o = over ? over[k] : undefined
    out[k] = (b && typeof b === 'object' && !Array.isArray(b) && o && typeof o === 'object' && !Array.isArray(o)) ? { ...b, ...o } : (o === undefined ? b : o)
  }
  if (over) for (const k of Object.keys(over)) if (!(k in out)) out[k] = over[k]
  return out
}

const cfg = mergeConfig(DEFAULTS, args || {})
const lang = `${cfg.language.name} ${cfg.language.standard}`
const linters = cfg.toolchain.linters.map(l => l.tool).join(', ')
const fmt = cfg.toolchain.formatter.tool
const sans = cfg.toolchain.sanitizers.join(', ')
const cov = cfg.toolchain.coverage.tool
const tf = cfg.toolchain.test_frameworks
const cc = JSON.stringify(cfg.clean_code)
const refs = cfg.references || {}
// Per-phase model routing. Returns undefined when nothing is configured so the
// agent inherits the session model (fully backward compatible).
const modelFor = (key) => (cfg.models && (cfg.models[key] || cfg.models.default)) || undefined
// Human <-> process interface files (read/written by agents, which have file tools).
const inputFile = (cfg.interface && cfg.interface.input) || 'INPUT.md'
const outputFile = (cfg.interface && cfg.interface.output) || 'OUTPUT.md'
// Documentation toggle: full (arc42 + API docs + UML) | minimal (ADRs + sketch) | off (code/tests only)
const docMode = (cfg.toggles && cfg.toggles.documentation) || 'full'
const docInstruction =
  docMode === 'off'
    ? 'DOCUMENTATION IS OFF: produce NO separate documentation. The code and tests are the specification; capture at most a one-line rationale for any irreversible decision.'
    : docMode === 'minimal'
      ? 'DOCUMENTATION IS MINIMAL: record only ADRs (context/decision/consequences) and a brief building-block sketch; skip full arc42 prose, API-doc generation, and diagrams unless they clarify a decision.'
      : `DOCUMENTATION IS FULL but MINIMAL & EFFECTIVE: use the arc42 sections ${JSON.stringify((refs.documentation || {}).sections || [])}, plus API docs and UML; document decisions and interfaces, not the obvious; mark irrelevant sections n/a.`

// ---- MVP maturity ladder ---------------------------------------------------
// The product climbs the ladder one rung per run. Intake (which can read state
// in OUTPUT.md) chooses this run's rung; we resolve its spec + effective gates.
const strategy = cfg.strategy || {}
const ladder = (strategy.maturity_levels && strategy.maturity_levels.length)
  ? strategy.maturity_levels : [{ name: 'complete', intent: 'Resolve everything; strict gates.', gates: {} }]
const ladderNames = ladder.map(l => l.name)
const levelByName = (name) => ladder.find(l => l.name === name) || ladder[0]
// Effective gates for a level = strict quality_gates with the level's relaxations merged on top.
const gatesForLevel = (lvl) => JSON.stringify({ ...cfg.quality_gates, ...((lvl && lvl.gates) || {}) })

// ---- commit-per-phase + per-phase trace ------------------------------------
// Appended to every phase prompt so the phase's content lands on the working
// branch as it completes. Implementation agents run in worktrees, so "current
// branch" there means their worktree branch (merged back during Integrate).
//
// EVERY phase ALSO writes a small trace file to disk — independent of the
// documentation toggle — so there is always a file-level trail (and every
// commit is non-empty). The trace is process telemetry, NOT product docs:
// `minimal`/`off` scale the arc42/API docs, never this trail.
const git = cfg.git || {}
const commitsOn = git.commit_per_phase !== false
const commitPrefix = git.commit_prefix || 'hephaestus'
const docsDir = (cfg.layout && cfg.layout.docs_dir) || 'docs/'
const tracePath = (tag, level, n, phaseName) =>
  `${docsDir}hephaestus/trace/${tag}/${level}/${String(n).padStart(2, '0')}-${phaseName.toLowerCase().replace(/[^a-z0-9]+/g, '-')}.md`
// n = phase ordinal within the V-pass (for stable, sortable trace filenames).
const commitDirective = (tag, level, n, phaseName) => {
  const trace = `\nTRACE (always, regardless of the documentation toggle): write a short markdown file to "${tracePath(tag, level, n, phaseName)}" capturing this phase — heading "${phaseName} — ${tag} @ ${level}", then a few bullets: key outputs/decisions, anything DEFERRED to a later loop, files touched, and a one-line status. Keep it minimal & effective; it is a file-level trail, not product documentation.`
  const commit = commitsOn
    ? ` Then persist this phase's other living artifact(s) too (code and tests always count; arc42/API docs honor the documentation toggle), \`git add -A\` and \`git commit -m "${commitPrefix}(${level}/${tag}): ${phaseName}"\` on the CURRENT branch using your shell tools. Do NOT create or switch branches.`
    : ''
  return trace + commit
}

// Refactoring is ON-DEMAND inside every (code-producing) phase, not a separate
// numbered phase: when an agent spots a smell while working, it applies the
// matching technique immediately and keeps tests green (red → green → refactor).
const maxRefactorRounds = (cfg.agile && cfg.agile.max_refactor_rounds != null) ? cfg.agile.max_refactor_rounds : 2
const refactorOnDemand = `REFACTORING IS ON-DEMAND (part of this phase, not a separate phase): whenever you spot a smell from the catalog while working, apply the matching technique right away and keep all tests green. Keep it bounded — at most ${maxRefactorRounds} focused refactor pass(es) within this phase (YAGNI; don't gold-plate). Smells ${JSON.stringify((refs.refactoring || {}).smells || [])}; techniques ${JSON.stringify((refs.refactoring || {}).techniques || [])}; honor clean-code rules ${cc}.`

// ---- schemas -------------------------------------------------------------
const BACKLOG_ITEM = { type: 'object', additionalProperties: false, required: ['id', 'title'], properties: { id: { type: 'string' }, title: { type: 'string' }, acceptance: { type: 'string' } } }
const RESOLUTION_ITEM = { type: 'object', additionalProperties: false, required: ['input', 'status'], properties: { input: { type: 'string' }, status: { type: 'string', enum: ['resolved', 'partial', 'queued'] }, note: { type: 'string' } } }
// Intake returns the merged backlog AND the maturity rung to run this loop,
// plus how much of INPUT.md is resolved so far (read from OUTPUT.md state).
const INTAKE_SCHEMA = {
  type: 'object', additionalProperties: false, required: ['backlog', 'level', 'loop'],
  properties: {
    backlog: { type: 'array', items: BACKLOG_ITEM },
    level: { type: 'string' },                 // one of the maturity-ladder names
    loop: { type: 'number' },                  // 1-based loop counter (this run)
    resolution: { type: 'array', items: RESOLUTION_ITEM },
    fully_resolved: { type: 'boolean' },        // true => INPUT.md done at the top rung
    notes: { type: 'string' },
  },
}
const SYNC_SCHEMA = {
  type: 'object', additionalProperties: false, required: ['new_items'],
  properties: {
    new_items: { type: 'array', items: BACKLOG_ITEM },
    resolution: { type: 'array', items: RESOLUTION_ITEM },
    next_level: { type: 'string' },             // rung to run on the NEXT invocation
    fully_resolved: { type: 'boolean' },
    output_written: { type: 'boolean' },
  },
}
const REQ_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['requirements', 'acceptance_tests'],
  properties: {
    requirements: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['id', 'text', 'type'], properties: { id: { type: 'string' }, text: { type: 'string' }, type: { type: 'string', enum: ['functional', 'non-functional'] } } } },
    acceptance_tests: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['req_ids', 'given', 'when', 'then'], properties: { req_ids: { type: 'array', items: { type: 'string' } }, given: { type: 'string' }, when: { type: 'string' }, then: { type: 'string' } } } },
  },
}
// Software System owns the TOP tier: the system topology and the deployable
// executables it decomposes into (e.g. client-server → a client + a server exe).
const SYSTEM_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['topology', 'deployables'],
  properties: {
    topology: { type: 'string' },                                          // e.g. "standalone", "client-server", "service + CLI"
    deployables: { type: 'array', minItems: 1, items: { type: 'object', additionalProperties: false, required: ['name', 'kind', 'responsibility'], properties: { name: { type: 'string' }, kind: { type: 'string' }, responsibility: { type: 'string' } } } }, // e.g. { client, executable, ... }, { server, executable, ... }
    context: { type: 'string' },
    external_interfaces: { type: 'array', items: { type: 'string' } },
    quality_scenarios: { type: 'array', items: { type: 'string' } },
  },
}
// Architecture owns the MODULE layer — PER DEPLOYABLE: each executable gets its own
// architecture pattern and modules (the client may be layered with 3 modules; the
// server its own). Modules compose into their deployable; deployables into the system.
const ARCH_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['deployable_patterns', 'modules', 'adrs', 'module_test_plan', 'system_test_plan', 'req_to_module', 'component_plan'],
  properties: {
    // Per deployable: the architecture pattern chosen for that executable (e.g. client → Layered).
    deployable_patterns: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['deployable', 'pattern'], properties: { deployable: { type: 'string' }, pattern: { type: 'string' }, justification: { type: 'string' } } } },
    // Modules belong to a deployable (the executable they compose into).
    modules: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['name', 'deployable', 'responsibility', 'interfaces'], properties: { name: { type: 'string' }, deployable: { type: 'string' }, responsibility: { type: 'string' }, interfaces: { type: 'array', items: { type: 'string' } } } } },
    adrs: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['title', 'decision', 'consequences'], properties: { title: { type: 'string' }, decision: { type: 'string' }, consequences: { type: 'string' } } } },
    module_test_plan: { type: 'array', items: { type: 'string' } },              // components -> module (per module)
    system_test_plan: { type: 'array', items: { type: 'string' } },              // deployables -> system (topology runs together)
    req_to_module: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['req_id', 'module'], properties: { req_id: { type: 'string' }, module: { type: 'string' } } } },
    // The component WORK-LIST (coarse) that seeds the per-component design→implement
    // pipeline. Design details each; a component may serve one or more modules.
    component_plan: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['name', 'modules', 'responsibility'], properties: { name: { type: 'string' }, modules: { type: 'array', minItems: 1, items: { type: 'string' } }, responsibility: { type: 'string' } } } },
  },
}
// Design owns the COMPONENT and UNIT layers, designed ONE COMPONENT AT A TIME so
// the per-component design→implement pipeline can overlap. HIERARCHY (explicit):
//   unit(s) -> component  : a unit belongs to EXACTLY ONE component (units nested under it)
//   component(s) -> module: a component may be assigned to ONE OR MORE modules (modules[] >= 1)
const COMPONENT_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['name', 'modules', 'interface', 'patterns', 'units', 'component_test_spec'],
  properties: {
    name: { type: 'string' },
    modules: { type: 'array', minItems: 1, items: { type: 'string' } },        // M:N — a component can serve several modules
    interface: { type: 'string' },                                             // the CONTRACT that decouples parallel work
    patterns: { type: 'array', items: { type: 'string' } },
    units: { type: 'array', minItems: 1, items: { type: 'object', additionalProperties: false, required: ['name', 'unit_test_spec'], properties: { name: { type: 'string' }, unit_test_spec: { type: 'string' } } } },  // 1 unit : 1 component
    component_test_spec: { type: 'string' },
  },
}
const IMPL_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['files_changed', 'unit_tests_added', 'summary'],
  properties: {
    component: { type: 'string' }, units_implemented: { type: 'array', items: { type: 'string' } },
    files_changed: { type: 'array', items: { type: 'string' } }, unit_tests_added: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' },
  },
}
const VERIFY_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['level', 'passed', 'details'],
  properties: { level: { type: 'string' }, scope: { type: 'string' }, passed: { type: 'boolean' }, details: { type: 'string' }, coverage_pct: { type: 'number' } },
}
const REVIEW_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['lens', 'findings'],
  properties: { lens: { type: 'string' }, findings: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['severity', 'location', 'issue', 'fix'], properties: { severity: { type: 'string', enum: ['blocker', 'major', 'minor'] }, location: { type: 'string' }, issue: { type: 'string' }, fix: { type: 'string' }, smell: { type: 'string' }, technique: { type: 'string' } } } } },
}
const GATE_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['passed', 'checklist', 'traceability_complete', 'verdict'],
  properties: {
    passed: { type: 'boolean' },
    traceability_complete: { type: 'boolean' },
    checklist: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['gate', 'ok'], properties: { gate: { type: 'string' }, ok: { type: 'boolean' }, note: { type: 'string' } } } },
    verdict: { type: 'string' },
    key_decisions: { type: 'array', items: { type: 'string' } },  // carried forward as constraints
    debt: { type: 'array', items: { type: 'string' } },           // carried forward as future work
  },
}

// ===========================================================================
//  One full V-pass for a single backlog increment.
// ===========================================================================
async function runIncrement(item, idx, priorContext, levelSpec) {
  const tag = item.id || `INC-${idx + 1}`
  const carry = JSON.stringify(priorContext || [])
  const level = (levelSpec && levelSpec.name) || 'complete'
  const levelIntent = (levelSpec && levelSpec.intent) || 'Resolve everything; strict gates.'
  const effGates = gatesForLevel(levelSpec)
  // The MVP banner is prepended to every left-arm prompt so each stage scopes
  // its work to THIS loop's maturity rung instead of building everything at once.
  const mvpBanner = `MATURITY LEVEL for this loop: "${level}". Intent: ${levelIntent}
Scope ALL work to this level only. Anything beyond it MUST be explicitly DEFERRED (recorded as
debt with a short reason) so a later loop picks it up — do not build it now. Effective quality
gates for this level (relaxations already merged over the strict gates): ${effGates}.`
  log(`▼ Increment ${tag} @ ${level}: ${item.title}`)

  // ---- LEFT ARM — straight, forward decomposition (NO backward jumps) -------
  // The decomposition flows forward only: requirements → system → architecture →
  // (per-component) design → implement. We do NOT bounce a stage back to its
  // predecessor; any real gap surfaces in the TEST PHASES (right arm), where the
  // fix is a TARGETED repeat of the failing element's build loop (see below).
  const dp = refs.design_patterns || {}

  const runRequirements = () => agent(
    `You are the Requirements Analyst in a V-Model. Backlog item: ${JSON.stringify(item)}.
${mvpBanner}
Carry-forward context from prior increments (stay consistent; treat logged decisions and
debt as constraints): ${carry}.
Produce functional + non-functional requirements with stable IDs (REQ-${tag}-n) and
acceptance tests (Given/When/Then) for ${tf.acceptance.tool}. Capture ONLY the requirements
needed at the "${level}" level; mark the rest as deferred. Do NOT design a solution.${commitDirective(tag, level, 1, 'Requirements')}`,
    { label: `req:${tag}`, phase: 'Requirements', schema: REQ_SCHEMA, model: modelFor('requirements') })

  // Software System (2): topology + deployable executables — the TOP tier.
  const runSystem = (reqsArg) => agent(
    `You are the Systems Architect. Requirements: ${JSON.stringify(reqsArg)}. Prior decisions to honor: ${carry}.
${mvpBanner}
Define the SOFTWARE SYSTEM as a whole — the TOP of the composition hierarchy:
  • topology: choose the system style (e.g. standalone, client-server, service + CLI) and justify it briefly.
  • deployables: the concrete DELIVERABLES the system decomposes into — e.g. a client-server topology
    yields TWO executables (a client and a server). Give each {name, kind (executable/service/library), responsibility}.
  • context, external_interfaces, and key quality_scenarios.
At the "${level}" level keep this minimal but EXTENSIBLE. Each deployable will be architected into modules next.${commitDirective(tag, level, 2, 'Software-System')}`,
    { label: `system:${tag}`, phase: 'Software System', schema: SYSTEM_SCHEMA, model: modelFor('system') })

  // Architecture (3): PER DEPLOYABLE — pattern + modules; modules compose the deployable.
  const runArchitecture = (reqsArg, sysArg) => agent(
    `You are the Architect. Requirements: ${JSON.stringify(reqsArg)}.
Software system (topology + deployables): ${JSON.stringify(sysArg)}. Prior ADRs to honor: ${carry}.
${mvpBanner}
Architect EACH DEPLOYABLE on its own: for every deployable, choose its architecture pattern from this
catalog and justify it (record in deployable_patterns): ${JSON.stringify((refs.software_architecture_patterns || {}).catalog || [])}
— e.g. a client might be Layered with three modules; the server its own pattern.
Decompose each deployable into MODULES (set each module's "deployable") honoring "${cfg.clean_code.architecture}"
and the rule "${cfg.clean_code.dependency_rule}". At the "${level}" level keep it small but EXTENSIBLE.
COMPOSITION HIERARCHY: modules compose their deployable; deployables compose the system. Define each
module's interface contract and ADRs. ${docInstruction}
Produce TWO test plans: (1) module_test_plan — how each module's components compose into the module
(${tf.integration.tool}); (2) system_test_plan — how the deployables run together as the topology.
Map every REQ id to a module. Also produce the component_plan: the coarse WORK-LIST of components per
module — {name, modules (one or more it serves), one-line responsibility}; a component shared by several
modules is listed ONCE. Design details each component.${commitDirective(tag, level, 3, 'Architecture')}`,
    { label: `arch:${tag}`, phase: 'Architecture', schema: ARCH_SCHEMA, model: modelFor('architecture') })

  // Design ONE component (interface-first) — the contract it returns is what lets
  // implementation + collaborators proceed in parallel (others mock this interface).
  const designComponent = (plan) => agent(
    `You are the Designer detailing ONE component from the architecture's work-list: ${JSON.stringify(plan)}.
Architecture context (modules, boundaries, ADRs): ${JSON.stringify({ modules: arch.modules, adrs: arch.adrs })}.
${mvpBanner}
INTERFACE-FIRST: define this component's public INTERFACE/contract precisely — that contract is what
lets its implementation and its collaborators proceed IN PARALLEL (collaborators mock this interface).
Then make the lower hierarchy explicit:
  • units: list the UNITS that compose this component, each with a unit_test_spec. A unit belongs to
    EXACTLY ONE component.
  • modules: the module(s) this component serves (one or more), from ${JSON.stringify(plan.modules || [])}.
Choose design patterns ONLY from this catalog (justify each by the problem it solves; YAGNI at the
"${level}" level): creational ${JSON.stringify(dp.creational || [])}, structural ${JSON.stringify(dp.structural || [])},
behavioral ${JSON.stringify(dp.behavioral || [])}. Specify error handling
("${cfg.clean_code.error_handling}") and ownership ("${cfg.clean_code.resource_management}").
Give the component_test_spec (collaborators mocked with ${tf.unit.mock || 'a mock framework'}).${commitDirective(tag, level, 4, `Design-${plan.name}`)}`,
    { label: `design:${plan.name}`, phase: 'Design', schema: COMPONENT_SCHEMA, model: modelFor('design') })

  // Implement ONE component (interface-first, in an isolated worktree).
  const implementComponent = (c) => agent(
    `You are the Implementer using TDD in ${lang}. Implement component "${c.name}", which belongs to
module(s) [${(c.modules || []).join(', ')}] and is composed of these UNITS: ${JSON.stringify((c.units || []).map(u => u.name))}.
${mvpBanner}
Interface/contract: ${c.interface}. Patterns: ${(c.patterns || []).join(', ')}.
HIGHLY PARALLEL + PARTIAL STATE: components are designed/implemented CONCURRENTLY, and across loops the
codebase is partial — some units/components already exist while others do not. INSPECT the current code
first; REUSE and EXTEND what is there, implement only the units that are missing or need deepening at
this level, and keep existing tests green. Do not rebuild or duplicate. Code ONLY against the interfaces
(mock collaborators) and touch only THIS component's files so concurrent work does not collide.
Implement UNIT BY UNIT (each unit belongs to this component alone): write the FAILING ${tf.unit.tool}
test first (from its unit_test_spec), then minimal code to pass, then tidy. Also run this component's
unit + component self-tests (collaborators mocked) so it is green in isolation. Unit specs: ${JSON.stringify(c.units || [])}.
At the "${level}" level implement only what the slice needs; defer the rest as TODO+debt. Put code under
${cfg.layout.source_dir}/${cfg.layout.include_dir}, tests under ${cfg.layout.test_dir}.
${refactorOnDemand}
Return your "component" name and "units_implemented".
Run ${fmt} and ${linters} on touched files. Build with ${cfg.toolchain.build_system.tool}.
You are in an ISOLATED git worktree: commit on the current (worktree) branch — \`git add -A\` and
\`git commit -m "${commitPrefix}(${level}/${tag}): impl ${c.name}"\` — so Integrate can merge it back.
Do NOT create extra branches or touch other worktrees.`,
    { label: `impl:${c.name}`, phase: 'Implementation (TDD)', schema: IMPL_SCHEMA, isolation: 'worktree', model: modelFor('implementation') })

  // Forward pass through the upper-left stages.
  phase('Requirements');     const reqs = await runRequirements();       if (!reqs) return { tag, status: 'aborted', stage: 'requirements' }
  phase('Software System');  const sys = await runSystem(reqs);          if (!sys) return { tag, status: 'aborted', stage: 'system' }
  phase('Architecture');     const arch = await runArchitecture(reqs, sys); if (!arch) return { tag, status: 'aborted', stage: 'architecture' }

  // ---- HIERARCHY (explicit): unit -> component -> module -> deployable -> system
  let components, moduleNames, totalUnits
  const componentsInModule = (m) => components.filter(c => (c.modules || []).includes(m))
  const deriveHierarchy = (designed) => {
    components = designed
    moduleNames = ((arch.modules || []).map(m => m.name).length)
      ? (arch.modules || []).map(m => m.name)
      : [...new Set(components.flatMap(c => c.modules || []))]
    totalUnits = components.reduce((n, c) => n + ((c.units || []).length), 0)
    log(`  hierarchy: ${moduleNames.length} module(s) ◄ ${components.length} component(s) ◄ ${totalUnits} unit(s)`)
  }

  // ---- MIDDLE: per-component DESIGN → IMPLEMENT pipeline (interface-first) ----
  // Architecture emitted the modules AND the component work-list. Each component
  // flows design → implement INDEPENDENTLY (pipeline, NO barrier between the two):
  // once a component's interface is designed its implementation can start while
  // OTHER components are still being designed. Forward only — failures are caught
  // in the test phases and fixed there (targeted), not by jumping back to design.
  phase('Design')
  const plan = (arch.component_plan || [])
  if (!plan.length) { log(`No component_plan from architecture for ${tag}.`); return { tag, status: 'aborted', stage: 'architecture' } }
  const built = (await pipeline(plan,
    (p) => designComponent(p),                                            // stage 4: design this component
    (comp) => comp ? implementComponent(comp).then(impl => ({ comp, impl })) : null,  // stage 5: implement it
  )).filter(Boolean)
  deriveHierarchy(built.map(b => b.comp).filter(Boolean))
  const impls = built.map(b => b.impl).filter(Boolean)

  // ---- INTEGRATE (on demand): merge whatever needs merging, assemble hierarchy
  // Runs only when there is something to merge (parallel worktrees were used).
  const needsIntegration = commitsOn && git.worktree_merge !== false && components.length > 0
  if (needsIntegration) {
    phase('Integrate')
    const moduleMap = (arch.modules || []).map(m => ({ module: m.name, deployable: m.deployable, components: componentsInModule(m.name).map(c => c.name) }))
    const deployables = (sys.deployables || []).map(d => d.name)
    await agent(
      `You are the Integrator for increment ${tag} @ ${level}. Integration happens ON DEMAND and assembles
the composition hierarchy BOTTOM-UP — units→components→modules→deployables(executables)→system — merging
exactly what the parallel implementation produced. The implementers each built ONE component in an
ISOLATED git worktree and committed on its own branch.
Topology: ${sys.topology}. Deployables (executables): ${JSON.stringify(deployables)}.
Composition map (module ◄ components, with its deployable): ${JSON.stringify(moduleMap)}.
1. Discover the worktrees with \`git worktree list --porcelain\`.
2. For each implementation worktree branch (everything except the main working branch), merge it into
   the current branch with \`git merge --no-ff\`. Resolve conflicts so ALL components are preserved and
   shared files (build config, shared headers, test registration) reconciled — never drop a component;
   a component shared by several modules is integrated ONCE and referenced by each module.
3. Assemble the hierarchy in the build: group each module's components into its module target, each
   module into its DEPLOYABLE (executable) target, and wire the deployables into the SYSTEM (topology)
   — so unit→component→module→deployable→system is reflected in the ${cfg.toolchain.build_system.tool} structure.
4. Run ${fmt} and a ${cfg.toolchain.build_system.tool} build to confirm every executable compiles and links;
   fix trivial integration breakage (includes, target wiring) so it builds.
5. Prune the merged worktrees (\`git worktree remove\`).
${commitDirective(tag, level, 5, 'Integrate')}
If there were no extra worktrees (components implemented inline), just ensure everything is staged
and committed on the current branch, and still write the trace file.`,
      { label: `integrate:${tag}`, phase: 'Integrate', model: modelFor('implementation') })
  }

  // ---- RIGHT ARM: climb the FIVE test levels, adversarial (fresh agents) -----
  // 6 Unit ◄5 · 7 Component ◄4 (mocked) · 8 Module ◄3 (mocked) · 9 System ◄2 ·
  // 10 Acceptance ◄1. Each level mirrors the hierarchy and FANS OUT in parallel
  // (per component / per module / per deployable / whole system); each level is a
  // BARRIER (a clear GATE). A RED level is fixed by a TARGETED repeat of the
  // failing element's own build loop (+ refactor), then re-verified — bounded by
  // max_fix_rounds. Already-passing siblings are NOT touched or re-implemented.
  const order = cfg.v_model.test_execution_order
  const adversarial = `Be adversarial: try to find a failing or missing case (within the "${level}" maturity scope — intentionally-deferred behavior is out of scope, not a failure).`
  const deployableNames = (sys.deployables || []).map(d => d.name)
  const LEVELS = {
    unit:      { phase: 'Unit Tests',      ord: 6,  fixes: 'the failing unit(s) — re-run their red→green→refactor loop' },
    component: { phase: 'Component Tests', ord: 7,  fixes: 'the failing component(s) — the unit(s) involved in the contract failure' },
    module:    { phase: 'Module Tests',    ord: 8,  fixes: 'the failing module(s) — the component wiring/contracts at that boundary' },
    system:    { phase: 'System Tests',    ord: 9,  fixes: 'the failing deployable(s) — how its modules assemble and talk in the topology' },
    acceptance:{ phase: 'Acceptance',      ord: 10, fixes: 'the specific behavior the scenario exercises, across the running system' },
  }
  // Build the per-level verification tasks (each returns a labelled thunk).
  const verifyTasks = (lvl) => {
    const L = LEVELS[lvl]; if (!L) return []
    if (lvl === 'unit') return components.map(c => () =>
      agent(`You are the Verifier (independent of the implementer). Increment ${tag} @ "${level}".
Test level: UNIT (6), scope = component "${c.name}". Run its unit tests (${tf.unit.tool}) with sanitizers
(${sans}) enabled for units ${JSON.stringify((c.units || []).map(u => u.name))}; report ${cov} coverage %.
Set "scope" to "${c.name}". ${adversarial}${commitDirective(tag, level, L.ord, `UnitTests-${c.name}`)}`,
        { label: `unit:${c.name}`, phase: L.phase, schema: VERIFY_SCHEMA, model: modelFor('verification') }))
    if (lvl === 'component') return components.map(c => () =>
      agent(`You are the Verifier (independent of the implementer). Increment ${tag} @ "${level}".
Test level: COMPONENT (7), scope = "${c.name}". Run the component test (${tf.component.tool}) against its
contract "${c.interface}". THIS REQUIRES MOCKING: mock the component's collaborators with
${tf.unit.mock || 'a mock framework'} so only this component is exercised. Set "scope" to "${c.name}". ${adversarial}${commitDirective(tag, level, L.ord, `ComponentTests-${c.name}`)}`,
        { label: `component:${c.name}`, phase: L.phase, schema: VERIFY_SCHEMA, model: modelFor('verification') }))
    if (lvl === 'module') return moduleNames.map(m => () =>
      agent(`You are the Verifier (independent of the implementer). Increment ${tag} @ "${level}".
Test level: MODULE (8), scope = "${m}". Prove the components [${componentsInModule(m).map(c => c.name).join(', ')}]
compose into module "${m}" (per the module_test_plan, ${tf.integration.tool}). THIS REQUIRES MOCKING: mock
the OTHER modules at "${m}"'s boundary so only this module is exercised. Set "scope" to "${m}". ${adversarial}${commitDirective(tag, level, L.ord, `ModuleTests-${m}`)}`,
        { label: `module:${m}`, phase: L.phase, schema: VERIFY_SCHEMA, model: modelFor('verification') }))
    if (lvl === 'system') return (deployableNames.length ? deployableNames : ['system']).map(d => () =>
      agent(`You are the Verifier (independent of the implementer). Increment ${tag} @ "${level}".
Test level: SYSTEM (9), scope = deployable "${d}". Build deployable "${d}" and verify its modules compose
into the running executable, and that it participates correctly in the "${sys.topology}" topology with the
other deployables [${deployableNames.filter(x => x !== d).join(', ') || 'none'}] (per the system_test_plan).
External systems may be mocked; the deployables themselves are real. Set "scope" to "${d}". ${adversarial}${commitDirective(tag, level, L.ord, `SystemTests-${d}`)}`,
        { label: `system:${d}`, phase: L.phase, schema: VERIFY_SCHEMA, model: modelFor('verification') }))
    if (lvl === 'acceptance') return [() =>
      agent(`You are the Validator (independent of the implementer). Increment ${tag} @ "${level}".
Test level: ACCEPTANCE (10), scope = the whole running SYSTEM — NO mocking. Build and RUN the system
(topology "${sys.topology}", deployables [${deployableNames.join(', ')}]) and validate it against the
requirements end-to-end: ${JSON.stringify(reqs.acceptance_tests)}. Capture concrete EVIDENCE (e.g.
screenshots / recorded output / exit codes) proving each acceptance scenario; reference it in your
details. Set "scope" to "acceptance". ${adversarial}${commitDirective(tag, level, L.ord, 'Acceptance')}`,
        { label: `acceptance:${tag}`, phase: L.phase, schema: VERIFY_SCHEMA, model: modelFor('verification') })]
    return []
  }
  // TARGETED REPAIR: re-run ONLY the failing element's build loop (+ refactor), then
  // re-verify this level. Passing siblings (e.g. units that already went green) are
  // left untouched — they are NOT re-implemented.
  const maxFix = (cfg.agile && cfg.agile.max_fix_rounds != null) ? cfg.agile.max_fix_rounds : 2
  const repair = (lvl, failed, round) => {
    const L = LEVELS[lvl]
    const scopes = failed.map(v => v.scope).filter(Boolean)
    return agent(
      `You are the Fixer for increment ${tag} @ "${level}". The ${lvl.toUpperCase()} test (level ${L.ord}) went RED.
Failures: ${JSON.stringify(failed.map(v => ({ scope: v.scope, details: v.details })))}.
Repeat the build loop for ${L.fixes}, scoped to ONLY: [${scopes.join(', ') || 'the failing element'}].
Do this as a focused red→green→refactor loop on the failing element(s): reproduce with the failing
${tf.unit.tool} test, make the minimal change to go green, then refactor on demand. Do NOT re-implement
or modify elements that already pass — leave green units/components/modules exactly as they are. Do NOT
redesign or re-architect the whole increment; only adjust the failing element's local design/code if the
failure genuinely requires it. ${refactorOnDemand}
Re-run ${fmt}/${linters} and the affected tests; keep every previously-passing test green.
Stay within the "${level}" maturity scope.${commitDirective(tag, level, L.ord, `Fix-${lvl}-r${round}`)}`,
      { label: `fix:${lvl}:${tag}:r${round}`, phase: L.phase, model: modelFor('implementation') })
  }

  const verifications = []
  let climbBroken = false
  for (const lvl of order) {
    const L = LEVELS[lvl]
    if (!L || !verifyTasks(lvl).length) continue
    phase(L.phase)
    let round = 0, levelResults = []
    while (true) {
      levelResults = (await parallel(verifyTasks(lvl))).filter(Boolean)  // fan out within the level
      const failed = levelResults.filter(v => v && v.passed === false)
      if (!failed.length) break                                   // level green → climb to the next level
      if (round >= maxFix) {                                      // out of fix budget → climb stops, gate fails
        log(`✗ ${L.phase} still red for ${tag} after ${round} fix round(s): ${failed.map(v => `${v.scope || ''}: ${v.details}`).join(' | ')}`)
        climbBroken = true; break
      }
      round++
      log(`↩ ${L.phase} red for ${tag} → targeted fix of [${failed.map(v => v.scope || '').filter(Boolean).join(', ')}] (round ${round}/${maxFix})`)
      await repair(lvl, failed, round)                            // re-run only the failing element's loop, then re-verify
    }
    verifications.push(...levelResults)
    if (climbBroken) break
  }

  // ---- ITERATION GATE (Definition of Done, scaled to the maturity level) ----
  phase('Iteration Gate')
  const gate = await agent(
    `You are the Gatekeeper for increment ${tag} @ maturity "${level}" (${levelIntent}).
Evaluate the Definition of Done and the EFFECTIVE quality gates for THIS level: ${effGates}.
(These are the strict gates relaxed for "${level}"; judge against them, not the strict ones.)
DoD: ${JSON.stringify(cfg.agile.definition_of_done)}.
Verifications: ${JSON.stringify(verifications)}. Confirm the requirements<->tests traceability
matrix is complete for the requirements IN SCOPE at this level (${cfg.quality_gates.traceability}).
Intentionally-deferred behavior is NOT a failure at this level — record it as debt, do not block on it.
Pass ONLY if every effective gate for this level is met.
Documentation mode is "${docMode}": if "off", DO NOT require the documentation DoD item; otherwise enforce it.
Also write the increment report (minimal & effective): list key_decisions (carried forward as
constraints) and debt / deferred-to-next-loop items (carried forward as future work).${commitDirective(tag, level, 11, 'Iteration-Gate')}`,
    { label: `gate:${tag}`, phase: 'Iteration Gate', schema: GATE_SCHEMA, model: modelFor('gate') })

  const moduleDeployable = {}
  for (const m of (arch.modules || [])) moduleDeployable[m.name] = m.deployable
  return {
    tag, title: item.title, level,
    status: gate && gate.passed ? 'passed' : 'failed',
    requirements: reqs.requirements,
    topology: sys.topology,
    deployables: (sys.deployables || []).map(d => d.name),
    modules: moduleNames,
    components: components.map(c => c.name),
    // Explicit system → software(executable) → module → component → unit hierarchy (OUTPUT.md visibility).
    hierarchy: {
      system: cfg.project.name, topology: sys.topology,
      deployables: (sys.deployables || []).map(d => ({ deployable: d.name, kind: d.kind, modules: moduleNames.filter(m => moduleDeployable[m] === d.name) })),
      modules: moduleNames.map(m => ({ module: m, deployable: moduleDeployable[m], components: componentsInModule(m).map(c => c.name) })),
      components: components.map(c => ({ component: c.name, modules: c.modules || [], units: (c.units || []).map(u => u.name) })),
      totals: { deployables: (sys.deployables || []).length, modules: moduleNames.length, components: components.length, units: totalUnits },
    },
    architecture_pattern: (arch.deployable_patterns || []).map(p => `${p.deployable}: ${p.pattern}`),
    key_decisions: (gate && gate.key_decisions) || [],
    debt: (gate && gate.debt) || [],
    verifications, gate,
  }
}

// ===========================================================================
//  DRIVER — Intake (INPUT.md) -> cyclic loop -> Report (OUTPUT.md).
// ===========================================================================
phase('Setup')
let backlog = (cfg.project.backlog && cfg.project.backlog.length) ? cfg.project.backlog.slice() : (Array.isArray(args) ? args.slice() : [])

// ---- INTAKE: ingest INPUT.md, pick this loop's maturity rung, build backlog ----
phase('Intake')
const mvpMode = (strategy.approach || 'mvp') === 'mvp'
const intake = await agent(
  `You are the Intake agent — the bridge between the human's free-form ${inputFile} and the project.
OWNERSHIP RULE (strict): ${inputFile} is HUMAN-ONLY — READ it, NEVER write to or modify it.
${outputFile} is PROCESS-ONLY — the human never edits it; everything you create or derive is recorded there.
Use your file tools to READ ${inputFile} at the project root (loose ideas/requirements/changes — possibly
just one sentence), AND READ ${outputFile} if it exists (it carries the state from prior loops). Then:
1. Turn feature/requirement ideas into vertical-slice backlog increments (id INC-NNN, title, acceptance),
   merged with the existing backlog ${JSON.stringify(backlog)} WITHOUT duplicating.
2. If the input implies CONFIGURATION/process changes, REWRITE config/hephaestus.config.yaml accordingly
   and minimally — language/standard, toolchain tools, quality_gates, toggles.documentation, models.*,
   project.backlog. Leave everything else at its default; never invent changes the input does not ask for.
3. MVP MATURITY LADDER (strategy.approach="${strategy.approach}"). One run advances the product by exactly
   ONE rung across the whole backlog; the human RE-RUNS to climb. The ordered rungs are: ${JSON.stringify(ladderNames)}.
   ${mvpMode
      ? `Decide which rung to run THIS loop by reading ${outputFile}'s recorded state: if no prior loop ran, choose "${ladderNames[0]}" (the MVP). A NEW loop only ADVANCES to the next rung when the previous cycle COMPLETELY FINISHED — i.e. every increment in the backlog passed its gate at that rung. If the previous loop did NOT fully finish (any increment still queued / in-progress / failed), STAY on that same rung this loop to complete it; do not advance. So: choose the LOWEST rung not yet completed for the WHOLE backlog. Set "loop" to the 1-based loop counter (prior loop + 1). If every increment has passed the TOP rung ("${ladderNames[ladderNames.length - 1]}") and INPUT.md has nothing unresolved, set fully_resolved=true.`
      : `strategy.approach is "full", so set level to the top rung "${ladderNames[ladderNames.length - 1]}" every run (no MVP laddering).`}
4. Track RESOLUTION of ${inputFile}: for each distinct idea/requirement in ${inputFile}, return its status —
   resolved (done at the top rung), partial (some loops done, more to deepen), or queued (not started).
5. WRITE ${outputFile} with this loop's state: an "Intake" section recording what you captured from
   ${inputFile} (config changes + backlog), the chosen maturity level + loop number, the resolution table,
   and the increment checklist. Commit ${outputFile} and any config change with your shell tools on the
   current branch (message "${commitPrefix}(intake): loop <n> @ <level>"). Do NOT touch ${inputFile}.
Return the merged backlog, the chosen level, the loop number, the resolution table, and fully_resolved.`,
  { label: 'intake', phase: 'Intake', schema: INTAKE_SCHEMA, model: modelFor('intake') })
if (intake && intake.backlog && intake.backlog.length) backlog = intake.backlog

if (!backlog.length) {
  log(`No backlog items. Add ideas to ${inputFile} (or project.backlog in the config).`)
  return { error: 'empty_backlog', config_used: cfg.project.name }
}

// Resolve this loop's maturity rung (validated against the ladder; fallback = first rung).
const loopNo = (intake && intake.loop) || 1
const levelName = (intake && ladderNames.includes(intake.level)) ? intake.level : ladderNames[0]
const levelSpec = levelByName(levelName)
if (mvpMode && intake && intake.fully_resolved) {
  log(`INPUT.md is fully resolved at the top rung "${ladderNames[ladderNames.length - 1]}". Nothing to deepen — see ${outputFile}.`)
  return { project: cfg.project.name, language: lang, loop: loopNo, level: levelName, fully_resolved: true, increments: [], interface: { input: inputFile, output: outputFile } }
}
log(`Hephaestus loop ${loopNo} @ "${levelName}" for "${cfg.project.name}" — ${lang} — ${backlog.length} increment(s) from ${inputFile}/config.`)

const results = []
const ledger = []  // carry-forward memory: prior decisions + debt feed later increments
let i = 0
while (i < backlog.length) {
  let attempt = 0, res
  do {
    if (attempt > 0) log(`↻ Re-looping increment ${backlog[i].id || i + 1} @ ${levelName} (attempt ${attempt + 1})`)
    res = await runIncrement(backlog[i], i, ledger, levelSpec)
    attempt++
  } while (res.status === 'failed' && attempt <= (cfg.agile.max_gate_retries || 0))
  results.push({ ...res, attempts: attempt })
  ledger.push({ tag: res.tag, title: res.title, level: levelName, decisions: res.key_decisions, debt: res.debt })

  // ---- REPORT: rewrite OUTPUT.md and re-check INPUT.md for new/changed items ----
  phase('Report')
  const sync = await agent(
    `You are the Reporting agent. Use your file tools. This is loop ${loopNo} at maturity level "${levelName}".
OWNERSHIP RULE (strict): ${inputFile} is HUMAN-ONLY — READ it, NEVER modify it. ${outputFile} is PROCESS-ONLY.
1. OVERWRITE ${outputFile} at the project root with a SHORT, structured checklist of the CURRENT state:
   the current loop number + maturity level; one line per backlog increment with a checkbox + status
   (queued / in-progress / passed / failed) AND the rung it has reached so far, the V-Model stage reached,
   the gate result, the five test levels for the latest increment, open debt / items deferred to a later
   loop, and a single "next action". ALSO write a "Resolution of ${inputFile}" table (one row per input
   idea: resolved / partial / queued) so the human can see how much of ${inputFile} is resolved. Keep it
   minimal & effective.
2. Decide the NEXT loop's maturity level (next_level): the lowest rung from ${JSON.stringify(ladderNames)} not
   yet completed for the whole backlog; if every increment passed the top rung and ${inputFile} has nothing
   unresolved, set fully_resolved=true and next_level to the top rung. Record next_level + "what the next
   loop will do" in ${outputFile}.
3. RE-READ ${inputFile} (read only) for any NEW or changed ideas not already represented in the backlog
   ${JSON.stringify(backlog.map(b => b.id))}; return them as new_items (empty array if none) and note them
   in ${outputFile}. Do NOT write to ${inputFile}.
4. Commit ${outputFile} with your shell tools on the current branch (message "${commitPrefix}(report): loop ${loopNo} @ ${levelName}").
Current results so far: ${JSON.stringify(results)}.`,
    { label: `report:${res.tag}`, phase: 'Report', schema: SYNC_SCHEMA, model: modelFor('report') })
  if (sync && sync.new_items && sync.new_items.length) {
    const known = new Set(backlog.map(b => b.id))
    for (const it of sync.new_items) if (it && it.id && !known.has(it.id)) { backlog.push(it); known.add(it.id); log(`+ new item from ${inputFile}: ${it.id} — ${it.title}`) }
  }
  i++
}

const passed = results.filter(r => r.status === 'passed').length
log(`Hephaestus loop ${loopNo} @ "${levelName}" complete: ${passed}/${results.length} increment(s) passed. Re-run to deepen — see ${outputFile}.`)
return {
  project: cfg.project.name, language: lang, loop: loopNo, level: levelName,
  increments: results, summary: { passed, total: results.length, level: levelName, loop: loopNo },
  interface: { input: inputFile, output: outputFile },
  next: `Re-run the workflow to advance to the next maturity rung until ${inputFile} is fully resolved.`,
}
