export const meta = {
  name: 'caav-model',
  description: 'Cyclic Agentic Agile V-Model with an MVP maturity ladder: ONE run advances the product by one maturity level (loop) across the backlog — re-run to deepen until INPUT.md is fully resolved. Per increment it runs a full V-pass (requirements->architecture->design->implementation, mirrored by acceptance/integration/component/unit tests) with TDD, adversarial verification, a clean-code refactor pass, and quality gates, committing each phase onto the working branch. Language/toolchain are fully config-driven (default: C++).',
  whenToUse: 'Building or extending software increment-by-increment with V-Model traceability, TDD, and enforced clean-code gates. Re-runnable: each invocation is one MVP/deepening loop. Pass the parsed config/caav-model.config.yaml as args (or rely on the built-in C++ defaults).',
  phases: [
    { title: 'Setup' },
    { title: 'Intake', model: 'sonnet' },
    { title: 'Requirements', model: 'sonnet' },
    { title: 'Architecture', model: 'opus' },
    { title: 'Design', model: 'opus' },
    { title: 'Implementation (TDD)', model: 'sonnet' },
    { title: 'Integrate', model: 'sonnet' },
    { title: 'Verification', model: 'haiku' },
    { title: 'Refactor', model: 'sonnet' },
    { title: 'Iteration Gate', model: 'opus' },
    { title: 'Report', model: 'sonnet' },
  ],
}

// ===========================================================================
//  CONFIG — built-in C++ defaults. The `args` you pass (parsed from
//  config/caav-model.config.yaml) is merged ON TOP of these, so you only need
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
    requirements: { verifies_with: 'acceptance / system / final software-integration test' },
    architecture: { verifies_with: 'module integration test' },
    design: { verifies_with: 'component test' },
    implementation: { verifies_with: 'unit test' },
    test_execution_order: ['unit', 'component', 'module', 'software-integration', 'acceptance'],
  },
  agile: {
    iteration_name: 'sprint', tdd: true, max_refactor_rounds: 2, max_gate_retries: 2,
    definition_of_done: [
      'All five test levels green (unit, component, module, integration, acceptance).',
      'All quality_gates satisfied.',
      'Requirements <-> tests traceability matrix complete.',
      'No new linter/formatter/sanitizer findings.',
      'Public API documented.',
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
  git: { commit_per_phase: true, branch: 'current', worktree_merge: true, commit_prefix: 'caav' },
  toggles: { documentation: 'minimal' },  // full | minimal | off — see docInstruction below
  // Per-phase model routing (opus | sonnet | haiku). A phase falls back to
  // `default`; if that is unset too, the agent inherits the session model.
  models: {
    default: 'sonnet',
    requirements: 'sonnet',
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
const commitPrefix = git.commit_prefix || 'caav'
const docsDir = (cfg.layout && cfg.layout.docs_dir) || 'docs/'
const tracePath = (tag, level, n, phaseName) =>
  `${docsDir}caav-model/trace/${tag}/${level}/${String(n).padStart(2, '0')}-${phaseName.toLowerCase().replace(/[^a-z0-9]+/g, '-')}.md`
// n = phase ordinal within the V-pass (for stable, sortable trace filenames).
const commitDirective = (tag, level, n, phaseName) => {
  const trace = `\nTRACE (always, regardless of the documentation toggle): write a short markdown file to "${tracePath(tag, level, n, phaseName)}" capturing this phase — heading "${phaseName} — ${tag} @ ${level}", then a few bullets: key outputs/decisions, anything DEFERRED to a later loop, files touched, and a one-line status. Keep it minimal & effective; it is a file-level trail, not product documentation.`
  const commit = commitsOn
    ? ` Then persist this phase's other living artifact(s) too (code and tests always count; arc42/API docs honor the documentation toggle), \`git add -A\` and \`git commit -m "${commitPrefix}(${level}/${tag}): ${phaseName}"\` on the CURRENT branch using your shell tools. Do NOT create or switch branches.`
    : ''
  return trace + commit
}

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
const ARCH_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['modules', 'adrs', 'integration_test_plan', 'req_to_module'],
  properties: {
    modules: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['name', 'responsibility', 'interfaces'], properties: { name: { type: 'string' }, responsibility: { type: 'string' }, interfaces: { type: 'array', items: { type: 'string' } } } } },
    adrs: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['title', 'decision', 'consequences'], properties: { title: { type: 'string' }, decision: { type: 'string' }, consequences: { type: 'string' } } } },
    integration_test_plan: { type: 'array', items: { type: 'string' } },
    req_to_module: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['req_id', 'module'], properties: { req_id: { type: 'string' }, module: { type: 'string' } } } },
  },
}
const DESIGN_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['components'],
  properties: {
    components: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['name', 'module', 'interface', 'patterns', 'component_test_spec'], properties: { name: { type: 'string' }, module: { type: 'string' }, interface: { type: 'string' }, patterns: { type: 'array', items: { type: 'string' } }, component_test_spec: { type: 'string' } } } },
  },
}
const IMPL_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['files_changed', 'unit_tests_added', 'summary'],
  properties: { files_changed: { type: 'array', items: { type: 'string' } }, unit_tests_added: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' } },
}
const VERIFY_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['level', 'passed', 'details'],
  properties: { level: { type: 'string' }, passed: { type: 'boolean' }, details: { type: 'string' }, coverage_pct: { type: 'number' } },
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

  // ---- LEFT ARM (sequential: each stage refines the previous) ----
  phase('Requirements')
  const reqs = await agent(
    `You are the Requirements Analyst in a V-Model. Backlog item: ${JSON.stringify(item)}.
${mvpBanner}
Carry-forward context from prior increments (stay consistent; treat logged decisions and
debt as constraints): ${carry}.
Produce functional + non-functional requirements with stable IDs (REQ-${tag}-n) and
acceptance tests (Given/When/Then) for ${tf.acceptance.tool}. Capture ONLY the requirements
needed at the "${level}" level; mark the rest as deferred. Do NOT design a solution.${commitDirective(tag, level, 1, 'Requirements')}`,
    { label: `req:${tag}`, phase: 'Requirements', schema: REQ_SCHEMA, model: modelFor('requirements') })
  if (!reqs) return { tag, status: 'aborted', stage: 'requirements' }

  phase('Architecture')
  const arch = await agent(
    `You are the Architect. Requirements: ${JSON.stringify(reqs)}. Prior ADRs/decisions to honor: ${carry}.
${mvpBanner}
Choose an architecture pattern from this catalog and justify it: ${JSON.stringify((refs.software_architecture_patterns || {}).catalog || [])}.
Decompose into modules with boundaries honoring "${cfg.clean_code.architecture}" and the rule
"${cfg.clean_code.dependency_rule}". At the "${level}" level keep the decomposition as small as
delivers the slice, but EXTENSIBLE so later loops deepen it without rework. Define interface
contracts and ADRs. ${docInstruction}
Produce an integration/module test plan for ${tf.integration.tool}, and map every REQ id to a module.${commitDirective(tag, level, 2, 'Architecture')}`,
    { label: `arch:${tag}`, phase: 'Architecture', schema: ARCH_SCHEMA, model: modelFor('architecture') })
  if (!arch) return { tag, status: 'aborted', stage: 'architecture' }

  phase('Design')
  const dp = refs.design_patterns || {}
  const design = await agent(
    `You are the Designer. Architecture: ${JSON.stringify(arch)}.
${mvpBanner}
For each module, specify components: public interface, data structures, and design patterns
chosen ONLY from this catalog (justify each by the problem it solves; no gratuitous patterns —
honor YAGNI at the "${level}" level):
creational ${JSON.stringify(dp.creational || [])}, structural ${JSON.stringify(dp.structural || [])},
behavioral ${JSON.stringify(dp.behavioral || [])}. Specify error handling
("${cfg.clean_code.error_handling}") and ownership ("${cfg.clean_code.resource_management}").
Give a component-test spec per component (deps mocked with ${tf.unit.mock || 'a mock framework'}).${commitDirective(tag, level, 3, 'Design')}`,
    { label: `design:${tag}`, phase: 'Design', schema: DESIGN_SCHEMA, model: modelFor('design') })
  if (!design) return { tag, status: 'aborted', stage: 'design' }

  // ---- RIGHT ARM: implement each component via TDD (parallel, isolated) ----
  phase('Implementation (TDD)')
  const components = (design.components || [])
  const impls = (await parallel(components.map(c => () =>
    agent(
      `You are the Implementer using TDD in ${lang}. Implement component "${c.name}" (module ${c.module}).
${mvpBanner}
Interface/contract: ${c.interface}. Patterns: ${(c.patterns || []).join(', ')}.
For each unit: write the FAILING ${tf.unit.tool} test first, then minimal code to pass, then tidy.
At the "${level}" level implement only what the slice needs; leave clearly-marked TODO+debt notes
for deferred behavior rather than building it. Honor clean-code rules ${cc}. Put code under
${cfg.layout.source_dir}/${cfg.layout.include_dir}, tests under ${cfg.layout.test_dir}.
Run ${fmt} and ${linters} on touched files. Build with ${cfg.toolchain.build_system.tool}.
You are working in an ISOLATED git worktree: commit your component on the current (worktree)
branch — \`git add -A\` and \`git commit -m "${commitPrefix}(${level}/${tag}): impl ${c.name}"\` — so
the Integrate phase can merge it back. Do NOT create extra branches or touch other worktrees.`,
      { label: `impl:${c.name}`, phase: 'Implementation (TDD)', schema: IMPL_SCHEMA, isolation: 'worktree', model: modelFor('implementation') })
  ))).filter(Boolean)

  // ---- INTEGRATE: merge the parallel worktree branches back onto main --------
  phase('Integrate')
  if (commitsOn && git.worktree_merge !== false) {
    await agent(
      `You are the Integrator for increment ${tag} @ ${level}. The implementers above each worked in
an ISOLATED git worktree and committed on its own branch. Bring it all onto the working branch:
1. Discover the worktrees/branches with \`git worktree list --porcelain\` (and \`git branch\`).
2. For each implementation worktree branch (everything except the main working branch), merge it
   into the current branch with \`git merge --no-ff\`. Resolve any conflicts so that ALL components
   are preserved and shared files (build files like ${cfg.toolchain.build_system.tool} config,
   shared headers, test registration) are reconciled — never drop a component.
3. After merging, run ${fmt} and a ${cfg.toolchain.build_system.tool} build to confirm the assembled
   code compiles and links; fix trivial integration breakage (includes, target wiring) so it builds.
4. Prune the merged worktrees (\`git worktree remove\`).
${commitDirective(tag, level, 5, 'Integrate')}
If there were no extra worktrees (components implemented inline), just ensure everything is staged
and committed on the current branch, and still write the trace file.`,
      { label: `integrate:${tag}`, phase: 'Integrate', model: modelFor('implementation') })
  }

  // ---- VERIFICATION: climb the V bottom-up, adversarial (fresh agents) ----
  phase('Verification')
  const order = cfg.v_model.test_execution_order
  const levelInstructions = {
    unit: `Run all unit tests (${tf.unit.tool}) with sanitizers (${sans}) enabled; report ${cov} coverage %.`,
    component: `Run component tests (${tf.component.tool}) — each component against its contract, collaborators mocked.`,
    module: `Run module/integration tests (${tf.integration.tool}) across real module boundaries.`,
    'software-integration': `Run the final software-integration test on the assembled build (${cfg.toolchain.build_system.tool}).`,
    acceptance: `Run the Stage-1 acceptance scenarios end-to-end (${tf.acceptance.tool}): ${JSON.stringify(reqs.acceptance_tests)}.`,
  }
  const verifications = []
  for (const testLevel of order) {
    const v = await agent(
      `You are the Verifier (independent of the implementer). Increment ${tag} @ maturity "${level}".
Test level: ${testLevel}. ${levelInstructions[testLevel] || ''}
Be adversarial: try to find a failing or missing case (within the "${level}" scope — deferred
behavior is out of scope, not a failure). Implementation summary: ${JSON.stringify(impls.map(i => i.summary))}.${commitDirective(tag, level, 6, `Verification-${testLevel}`)}`,
      { label: `verify:${testLevel}:${tag}`, phase: 'Verification', schema: VERIFY_SCHEMA, model: modelFor('verification') })
    verifications.push(v)
    if (v && v.passed === false) { log(`✗ ${testLevel} test failed for ${tag}: ${v.details}`); break } // stop the climb on red
  }

  // ---- REFACTOR PASS (bounded), multi-lens, tests must stay green ----
  phase('Refactor')
  const lenses = ['SOLID & principles', 'code smells', 'architecture & dependency rule', 'naming & function size/complexity']
  let refactorRounds = 0
  while (refactorRounds < (cfg.agile.max_refactor_rounds || 1)) {
    const reviews = (await parallel(lenses.map(lens => () =>
      agent(
        `You are a clean-code reviewer for the "${lens}" lens on increment ${tag} @ "${level}" (${lang}).
Rules: ${cc}. Effective quality gates for this level: ${effGates}.
Cross-check against the refactoring catalog — smells ${JSON.stringify((refs.refactoring || {}).smells || [])}
and techniques ${JSON.stringify((refs.refactoring || {}).techniques || [])}. For each finding, name
the smell and the technique that removes it. Files: ${JSON.stringify(impls.flatMap(i => i.files_changed))}.
Report concrete findings with fixes (do NOT flag intentionally-deferred behavior as a smell).
Empty findings = clean.${commitDirective(tag, level, 7, `Refactor-review-${lens}`)}`,
        { label: `review:${lens}:${tag}`, phase: 'Refactor', schema: REVIEW_SCHEMA, model: modelFor('refactor') })
    ))).filter(Boolean)
    const actionable = reviews.flatMap(r => (r.findings || []).filter(f => f.severity !== 'minor'))
    if (actionable.length === 0) { log(`✓ Refactor clean for ${tag} (round ${refactorRounds + 1})`); break }
    await agent(
      `You are the Refactorer for increment ${tag} @ "${level}". Apply these fixes while keeping ALL tests green,
re-running ${tf.unit.tool} and ${fmt}/${linters} after each change: ${JSON.stringify(actionable)}.${commitDirective(tag, level, 7, `Refactor-apply-r${refactorRounds + 1}`)}`,
      { label: `refactor:${tag}:r${refactorRounds + 1}`, phase: 'Refactor', model: modelFor('refactor') })
    refactorRounds++
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
constraints) and debt / deferred-to-next-loop items (carried forward as future work).${commitDirective(tag, level, 8, 'Iteration-Gate')}`,
    { label: `gate:${tag}`, phase: 'Iteration Gate', schema: GATE_SCHEMA, model: modelFor('gate') })

  return {
    tag, title: item.title, level,
    status: gate && gate.passed ? 'passed' : 'failed',
    requirements: reqs.requirements,
    modules: (arch.modules || []).map(m => m.name),
    components: components.map(c => c.name),
    architecture_pattern: (arch.adrs || []).map(a => a.title),
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
2. If the input implies CONFIGURATION/process changes, REWRITE config/caav-model.config.yaml accordingly
   and minimally — language/standard, toolchain tools, quality_gates, toggles.documentation, models.*,
   project.backlog. Leave everything else at its default; never invent changes the input does not ask for.
3. MVP MATURITY LADDER (strategy.approach="${strategy.approach}"). One run advances the product by exactly
   ONE rung across the whole backlog; the human RE-RUNS to climb. The ordered rungs are: ${JSON.stringify(ladderNames)}.
   ${mvpMode
      ? `Decide which rung to run THIS loop by reading ${outputFile}'s recorded state: if no prior loop ran, choose "${ladderNames[0]}" (the MVP). Otherwise choose the LOWEST rung not yet completed for the whole backlog (e.g. once every increment passed "${ladderNames[0]}", choose the next rung). Set "loop" to the 1-based loop counter (prior loop + 1). If every increment has passed the TOP rung ("${ladderNames[ladderNames.length - 1]}") and INPUT.md has nothing unresolved, set fully_resolved=true.`
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
log(`CAAVM loop ${loopNo} @ "${levelName}" for "${cfg.project.name}" — ${lang} — ${backlog.length} increment(s) from ${inputFile}/config.`)

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
log(`CAAVM loop ${loopNo} @ "${levelName}" complete: ${passed}/${results.length} increment(s) passed. Re-run to deepen — see ${outputFile}.`)
return {
  project: cfg.project.name, language: lang, loop: loopNo, level: levelName,
  increments: results, summary: { passed, total: results.length, level: levelName, loop: loopNo },
  interface: { input: inputFile, output: outputFile },
  next: `Re-run the workflow to advance to the next maturity rung until ${inputFile} is fully resolved.`,
}
