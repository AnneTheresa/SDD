<!--
Sync Impact Report
- Version change: 1.0.0 -> 1.1.0
- Modified principles:
	- template principle slot 1 -> I. Contract Before Code
	- template principle slot 2 -> II. Single Source of Truth for Error Semantics
	- template principle slot 3 -> III. Security Is Default, Not Optional
	- template principle slot 4 -> IV. Environment Parity and Operability
	- template principle slot 5 -> V. Business Logic in Services, UX Logic in UI
	- (new) VI. Testability Is a Merge Gate
	- (new) VII. No Silent Degradation
	- (new) VIII. Style and Structure Discipline
	- (new) IX. Theme Parity Requirement
- Added sections:
	- Operational Constraints
	- Enforcement Rule Set (Practical Checklist)
- Removed sections:
	- None
- Templates requiring updates:
	- ✅ .specify/templates/plan-template.md
	- ✅ .specify/templates/spec-template.md
	- ✅ .specify/templates/tasks-template.md
	- ✅ .specify/extensions/git/commands/speckit.git.initialize.md (reviewed, no update required)
	- ✅ .specify/extensions/agent-context/commands/speckit.agent-context.update.md (reviewed, no update required)
	- ✅ .github/prompts/speckit.constitution.prompt.md (reviewed, no update required)
- Follow-up TODOs:
	- None
-->

# Voltio Engineering Constitution (Codebase-Specific)

## Core Principles

### I. Contract Before Code
API contracts are a product interface, not an implementation detail. Any DTO shape
change MUST be treated as a breaking interface change until compatibility is
demonstrated. A DTO change MUST include, in the same PR, controller test updates,
service mapping updates, and frontend consumer updates where fields are read.
Rationale: a prior `AccountResponse` constructor/field drift broke controller tests;
this class of failure must be prevented systematically.

### II. Single Source of Truth for Error Semantics
Backend errors MUST be emitted as stable `code + message + details` through
`GlobalExceptionHandler`. Frontend MUST map backend errors only in
`src/api/axiosClient.js`; ad-hoc parsing in pages/components/hooks is prohibited.
When a new backend error code is introduced, mapping coverage and user-visible
handling MUST be added before merge.

### III. Security Is Default, Not Optional
Stateless JWT enforcement and endpoint authorization in `SecurityConfig` are
authoritative and MUST be reviewed for every new endpoint. CORS policy in
`CorsConfig` MUST be explicit and environment-scoped. Wildcard production origins
are forbidden.

### IV. Environment Parity and Operability
Local development MUST NOT depend on cluster-internal DNS names or hardcoded
infrastructure hostnames. `vite.config.js` proxy targets MUST be environment-driven
with local-safe defaults and override support. Endpoint/proxy configuration MUST
work in local, CI, and deployed environments through configuration only.

### V. Business Logic in Services, UX Logic in UI
Backend service layer owns business invariants and is final authority. Frontend
pages may add UX pre-validation, but UI checks MUST NOT replace backend validation
or alter backend-derived error semantics.

### VI. Testability Is a Merge Gate
Controller behavior changes MUST include happy-path and failure-path tests.
Minimum coverage for changed endpoint behavior is: one success case, one
auth/permission or validation case, and one business-failure case
(not-found/conflict/domain failure). If behavior changes and tests do not, the PR
is incomplete.

### VII. No Silent Degradation
Auth and session failures in `src/api/axiosClient.js` MUST be deterministic,
race-safe, and actionable. Generic unknown-error messaging is allowed only for
truly unexpected states.

### VIII. Style and Structure Discipline
Layer boundaries are mandatory: config in config, transport in shared API client,
domain behavior in services/hooks, and presentation in pages/components.
Cross-layer coupling is prohibited, including page components constructing raw
backend URLs or bypassing shared clients.

### IX. Theme Parity Requirement
Voltio supports two official UI themes: Classic and Neon. Any new user-facing
feature, component, page, modal, form, notification, visual state, or UI bug fix
MUST be implemented, tested, and reviewed in both themes before merge.
Theme support is a core product requirement, not an enhancement.

## Operational Constraints

- No TODO placeholders are permitted in security, auth, or money movement logic.
- Error messages MUST originate from backend codes and be translated once in the
	frontend mapping layer; copy-pasted error text across layers is prohibited.
- All environment-specific URLs MUST come from environment variables.
- CORS origin additions MUST include explicit reason and environment scope.
- User-facing UI changes MUST preserve consistency, accessibility, and usability
	across Classic and Neon themes.

## Enforcement Rule Set (Practical Checklist)

PR Contract Rules:
- Any DTO/record change requires updated mappers/factories, controller tests,
	frontend API consumers (if fields are read), and sample payloads in tests.

Backend Rules:
- All new exceptions MUST map through the global handler with a deliberate status
	code.
- Controllers MUST NOT leak raw exceptions.
- No endpoint is complete without `SecurityConfig` matcher review.

Frontend Rules:
- All HTTP traffic MUST go through `src/api/axiosClient.js` or wrappers built on
	that client.
- Direct `fetch`/`axios` in pages is prohibited.
- User-facing errors MUST use mapped structured errors.
- New theme-dependent variables, colors, icons, animations, or assets MUST be
	implemented for both Classic and Neon themes.
- No user-facing UI change is complete if it functions or renders correctly in
	only one theme.

Config Rules:
- Hardcoded infrastructure hostnames in committed dev config are prohibited unless
	explicitly documented as a fallback.
- Proxy/runtime targets must be env-driven and parity-safe across local, CI, and
	deployed env.

Testing Rules:
- Changed endpoint behavior requires at minimum one success test, one auth or
	validation test, and one business-failure test.
- Frontend API behavior changes require at least one test update in existing
	frontend suites.
- UI behavior or styling changes require verification in both Classic and Neon
	themes, including bug fixes that affect user-visible behavior.
- Screenshots, demos, or QA validation for new UI features SHOULD include both
	theme variants when applicable.

UI Merge Gate Rules:
- Any PR introducing or modifying user-facing UI MUST explicitly confirm:
	Classic theme tested, Neon theme tested, and no visual regressions in either
	theme.

Release Rules:
- Before release: frontend tests pass; backend tests compile and pass; local smoke
	check for login, account list, and transfer path passes; and no 5xx/502 appears
	in browser network for core flows.
- Before release: run smoke checks for core user-facing flows in both Classic and
	Neon themes.

## Governance
This constitution supersedes conflicting local conventions for implementation and
review.

Amendment procedure:
- Any amendment MUST be proposed in a PR that includes a rationale, migration
	impact, and updates to affected templates and guidance docs.
- Approval requires maintainers for both backend and frontend areas when change
	affects cross-layer contracts.

Versioning policy:
- MAJOR: incompatible principle removals or redefinitions.
- MINOR: new principle or materially expanded mandatory guidance.
- PATCH: wording clarifications and non-semantic edits.

Compliance review expectations:
- Every plan and PR MUST include a Constitution Check against these principles.
- Violations MUST be explicitly justified in writing with an approved exception.
- Exceptions are time-bound and MUST include follow-up remediation tasks.

**Version**: 1.1.0 | **Ratified**: 2026-06-04 | **Last Amended**: 2026-06-04
