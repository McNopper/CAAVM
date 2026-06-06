export const meta = {
  name: 'caav-model',
  description: 'Cyclic Agentic Agile V-Model: per backlog increment, run a full V-pass (requirements->architecture->design->implementation, mirrored by acceptance/integration/component/unit tests) with TDD, adversarial verification, a clean-code refactor pass, and quality gates. Language/toolchain are fully config-driven (default: C++).',
  whenToUse: 'Building or extending software increment-by-increment with V-Model traceability, TDD, and enforced clean-code gates. Pass the parsed config/caav-model.config.yaml as args (or rely on the built-in C++ defaults).',
  phases: [
    { title: 'Setup' },
    { title: 'Requirements' },
    { title: 'Architecture' },
    { title: 'Design' },
    { title: 'Implementation (TDD)' },
    { title: 'Verification' },
    { title: 'Refactor' },
    { title: 'Iteration Gate' },
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
  language: { name: 'C++', standard: 'c++20', compilers: ['clang++ >= 17', 'g++ >= 13'], style_guide: 'C++ Core Guidelines' },
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
  toggles: { documentation: 'full' },  // full | minimal | off — see docInstruction below
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
const gates = JSON.stringify(cfg.quality_gates)
const refs = cfg.references || {}
// Documentation toggle: full (arc42 + API docs + UML) | minimal (ADRs + sketch) | off (code/tests only)
const docMode = (cfg.toggles && cfg.toggles.documentation) || 'full'
const docInstruction =
  docMode === 'off'
    ? 'DOCUMENTATION IS OFF: produce NO separate documentation. The code and tests are the specification; capture at most a one-line rationale for any irreversible decision.'
    : docMode === 'minimal'
      ? 'DOCUMENTATION IS MINIMAL: record only ADRs (context/decision/consequences) and a brief building-block sketch; skip full arc42 prose, API-doc generation, and diagrams unless they clarify a decision.'
      : `DOCUMENTATION IS FULL but MINIMAL & EFFECTIVE: use the arc42 sections ${JSON.stringify((refs.documentation || {}).sections || [])}, plus API docs and UML; document decisions and interfaces, not the obvious; mark irrelevant sections n/a.`

// ---- schemas -------------------------------------------------------------
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
async function runIncrement(item, idx, priorContext) {
  const tag = item.id || `INC-${idx + 1}`
  const carry = JSON.stringify(priorContext || [])
  log(`▼ Increment ${tag}: ${item.title}`)

  // ---- LEFT ARM (sequential: each stage refines the previous) ----
  phase('Requirements')
  const reqs = await agent(
    `You are the Requirements Analyst in a V-Model. Backlog item: ${JSON.stringify(item)}.
Carry-forward context from prior increments (stay consistent; treat logged decisions and
debt as constraints): ${carry}.
Produce functional + non-functional requirements with stable IDs (REQ-${tag}-n) and
acceptance tests (Given/When/Then) for ${tf.acceptance.tool}. Do NOT design a solution.`,
    { label: `req:${tag}`, phase: 'Requirements', schema: REQ_SCHEMA })
  if (!reqs) return { tag, status: 'aborted', stage: 'requirements' }

  phase('Architecture')
  const arch = await agent(
    `You are the Architect. Requirements: ${JSON.stringify(reqs)}. Prior ADRs/decisions to honor: ${carry}.
Choose an architecture pattern from this catalog and justify it: ${JSON.stringify((refs.software_architecture_patterns || {}).catalog || [])}.
Decompose into modules with boundaries honoring "${cfg.clean_code.architecture}" and the rule
"${cfg.clean_code.dependency_rule}". Define interface contracts and ADRs. ${docInstruction}
Produce an integration/module test plan for ${tf.integration.tool}, and map every REQ id to a module.`,
    { label: `arch:${tag}`, phase: 'Architecture', schema: ARCH_SCHEMA })
  if (!arch) return { tag, status: 'aborted', stage: 'architecture' }

  phase('Design')
  const dp = refs.design_patterns || {}
  const design = await agent(
    `You are the Designer. Architecture: ${JSON.stringify(arch)}.
For each module, specify components: public interface, data structures, and design patterns
chosen ONLY from this catalog (justify each by the problem it solves; no gratuitous patterns):
creational ${JSON.stringify(dp.creational || [])}, structural ${JSON.stringify(dp.structural || [])},
behavioral ${JSON.stringify(dp.behavioral || [])}. Specify error handling
("${cfg.clean_code.error_handling}") and ownership ("${cfg.clean_code.resource_management}").
Give a component-test spec per component (deps mocked with ${tf.unit.mock || 'a mock framework'}).`,
    { label: `design:${tag}`, phase: 'Design', schema: DESIGN_SCHEMA })
  if (!design) return { tag, status: 'aborted', stage: 'design' }

  // ---- RIGHT ARM: implement each component via TDD (parallel, isolated) ----
  phase('Implementation (TDD)')
  const components = (design.components || [])
  const impls = (await parallel(components.map(c => () =>
    agent(
      `You are the Implementer using TDD in ${lang}. Implement component "${c.name}" (module ${c.module}).
Interface/contract: ${c.interface}. Patterns: ${(c.patterns || []).join(', ')}.
For each unit: write the FAILING ${tf.unit.tool} test first, then minimal code to pass, then tidy.
Honor clean-code rules ${cc}. Put code under ${cfg.layout.source_dir}/${cfg.layout.include_dir},
tests under ${cfg.layout.test_dir}. Run ${fmt} and ${linters} on touched files. Build with ${cfg.toolchain.build_system.tool}.`,
      { label: `impl:${c.name}`, phase: 'Implementation (TDD)', schema: IMPL_SCHEMA, isolation: 'worktree' })
  ))).filter(Boolean)

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
  for (const level of order) {
    const v = await agent(
      `You are the Verifier (independent of the implementer). Increment ${tag}.
Test level: ${level}. ${levelInstructions[level] || ''}
Be adversarial: try to find a failing or missing case. Implementation summary: ${JSON.stringify(impls.map(i => i.summary))}.`,
      { label: `verify:${level}:${tag}`, phase: 'Verification', schema: VERIFY_SCHEMA })
    verifications.push(v)
    if (v && v.passed === false) { log(`✗ ${level} test failed for ${tag}: ${v.details}`); break } // stop the climb on red
  }

  // ---- REFACTOR PASS (bounded), multi-lens, tests must stay green ----
  phase('Refactor')
  const lenses = ['SOLID & principles', 'code smells', 'architecture & dependency rule', 'naming & function size/complexity']
  let refactorRounds = 0
  while (refactorRounds < (cfg.agile.max_refactor_rounds || 1)) {
    const reviews = (await parallel(lenses.map(lens => () =>
      agent(
        `You are a clean-code reviewer for the "${lens}" lens on increment ${tag} (${lang}).
Rules: ${cc}. Quality gates: ${gates}.
Cross-check against the refactoring catalog — smells ${JSON.stringify((refs.refactoring || {}).smells || [])}
and techniques ${JSON.stringify((refs.refactoring || {}).techniques || [])}. For each finding, name
the smell and the technique that removes it. Files: ${JSON.stringify(impls.flatMap(i => i.files_changed))}.
Report concrete findings with fixes. Empty findings = clean.`,
        { label: `review:${lens}:${tag}`, phase: 'Refactor', schema: REVIEW_SCHEMA })
    ))).filter(Boolean)
    const actionable = reviews.flatMap(r => (r.findings || []).filter(f => f.severity !== 'minor'))
    if (actionable.length === 0) { log(`✓ Refactor clean for ${tag} (round ${refactorRounds + 1})`); break }
    await agent(
      `You are the Refactorer for increment ${tag}. Apply these fixes while keeping ALL tests green,
re-running ${tf.unit.tool} and ${fmt}/${linters} after each change: ${JSON.stringify(actionable)}.`,
      { label: `refactor:${tag}:r${refactorRounds + 1}`, phase: 'Refactor' })
    refactorRounds++
  }

  // ---- ITERATION GATE (Definition of Done) ----
  phase('Iteration Gate')
  const gate = await agent(
    `You are the Gatekeeper for increment ${tag}. Evaluate the Definition of Done and quality gates.
Quality gates: ${gates}. DoD: ${JSON.stringify(cfg.agile.definition_of_done)}.
Verifications: ${JSON.stringify(verifications)}. Confirm the requirements<->tests traceability
matrix is complete (${cfg.quality_gates.traceability}). Pass ONLY if every gate is met.
Documentation mode is "${docMode}": if "off", DO NOT require the documentation DoD item; otherwise enforce it.
Also write the increment report (minimal & effective): list key_decisions (carried forward as
constraints) and debt (carried forward as future work) for the next cycle.`,
    { label: `gate:${tag}`, phase: 'Iteration Gate', schema: GATE_SCHEMA })

  return {
    tag, title: item.title,
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
//  DRIVER — cyclic loop over the backlog, with bounded gate retries.
// ===========================================================================
phase('Setup')
const backlog = (cfg.project.backlog && cfg.project.backlog.length) ? cfg.project.backlog : (Array.isArray(args) ? args : [])
if (!backlog.length) {
  log('No backlog items found. Populate project.backlog in config/caav-model.config.yaml (or pass items as args).')
  return { error: 'empty_backlog', config_used: cfg.project.name }
}
log(`CAAVM starting for "${cfg.project.name}" — ${lang} — ${backlog.length} increment(s).`)

const results = []
const ledger = []  // carry-forward memory: prior decisions + debt feed later increments
for (let i = 0; i < backlog.length; i++) {
  let attempt = 0, res
  do {
    if (attempt > 0) log(`↻ Re-looping increment ${backlog[i].id || i + 1} (attempt ${attempt + 1})`)
    res = await runIncrement(backlog[i], i, ledger)
    attempt++
  } while (res.status === 'failed' && attempt <= (cfg.agile.max_gate_retries || 0))
  results.push({ ...res, attempts: attempt })
  // append a compact summary so the next increment stays consistent with this one
  ledger.push({ tag: res.tag, title: res.title, decisions: res.key_decisions, debt: res.debt })
}

const passed = results.filter(r => r.status === 'passed').length
log(`CAAVM complete: ${passed}/${results.length} increment(s) passed the gate.`)
return { project: cfg.project.name, language: lang, increments: results, summary: { passed, total: results.length } }
