# CoFHCore — NeoForge port (foundation library)

## Where things are

| File | Read it when |
|---|---|
| **This file** | Always. Context, decisions, and current state. |
| [docs/port-plan.md](docs/port-plan.md) | **Starting any porting work.** The approved plan (2026-09-21): 1.20.6-partial → 1.21.1 → 26.1.2 directly, with every version, coordinate, replacement API and reference location stated. Phase/category order lives there, not here. |
| `docs/reference/` (**local only, gitignored**) | Confirming an API shape. Vendored primers (1.20.5 → 26.1), NeoForge release notes, the 26.1 and 1.21.1 docs, NeoForge/JEI source file lists and key source files. `scripts/fetch_reference.sh` recreates it. `grep` it. |
| [docs/api-notes-26.1.2.md](docs/api-notes-26.1.2.md) | **Writing any code against a 26.1.2 API (Phase B, in progress).** Shapes confirmed against `minecraft-patched-26.1.2.109-sources.jar`, category by category: B.0 through B.9. Check it, then Pyronetics' notes, before deriving anything. |
| [docs/api-notes-1.21.1.md](docs/api-notes-1.21.1.md) | **Writing any code against a 1.21.1 API**, or when a 26.1 change needs its 1.21.1 starting point. Every shape confirmed during the completed 1.21.1 hop, in the port plan's category order — including the four sweeps that had to be partly reverted. |
| [docs/api-notes-1.20.6.md](docs/api-notes-1.20.6.md) | The 1.20.5/1.20.6 predecessor (all still valid on 1.21.1, but 1.21.1's notes supersede it where they overlap). |
| [docs/code-style.md](docs/code-style.md) | **Before writing any code, in any of the four repos.** Upstream CoFH's style, measured from the `1.20.4` code: one-line comments at most, a blank line opening every method body, `@Annotation (args)`, regions, imports over qualified names. |
| [docs/TODO.md](docs/TODO.md) | Picking up work. The live, current-priority list of what's still broken, by error count. **Anything noticed mid-session goes in its Inbox.** |
| [docs/progress-log.md](docs/progress-log.md) | The story behind a decision, or the chronology of the hop so far — what's already fixed, in what order, why a number is what it is. Append-only. |
| `docs/context/` (**local only, gitignored**) | The progress log doesn't have the detail you need. Full session transcript exports — this whole 4-repo porting effort runs in one shared session, exported under `ThermalExpansion/docs/context/` (that's the session's project directory) rather than duplicated into each repo. `grep` it, don't read it whole. |

## Context

CoFHCore is the foundation library for the whole "Thermal Series" — **ThermalCore**,
**ThermalDynamics**, and **ThermalExpansion** (sibling repos under
`/Volumes/Mac_External/Developer/Minecraft/`) all depend on it, so its port comes first
at every version hop and the others follow. All four repos are worked in **one shared
Claude Code session**, not independently — session transcripts live under
`ThermalExpansion/docs/context/`.

This is a port of the existing NeoForge 1.20.4 codebase (already in place before this
effort began) to **26.1.2**, in exactly two hops: **1.21.1** (a required stop — the
long-lived modding version, and where the SPLIGAN reference forks live) and then
**26.1.2** directly. It is not a from-scratch rewrite and, since 2026-09-21, no longer a
one-version-at-a-time primer climb — see progress-log.md's Phase 2 entry for the change
and docs/port-plan.md §1/§3 for why. No NeoForge port of CoFHCore exists anywhere to
build on instead, official or community.

**Reference implementations on this machine** (read before deriving a shape yourself):
`../Pyronetics` — Joel's own from-scratch mod, **built and running on NeoForge 26.1.2.109**
(MDG, screens, block-entity renderers, the transfer API, fluids, recipes, networking); its
`docs/api-notes-26.1.2.md` is the first stop for any 26.1 shape. `../ThermalCoreForNeoForge`,
`../ThermalExpansionForNeoForge`, `../ThermalDynamicsForNeoForge` — SPLIGAN's minimal-diff
**1.21.1** ports of the three Thermal repos (they compiled against a private CoFHCore build
we don't have, so CoFHCore itself is ported by hand).

Pyronetics is otherwise unrelated — a separate from-scratch mod in its own repo/session,
inspired by classic Thermal Expansion's designs, with zero code dependency on this codebase.
It is a *reference* here, never a dependency.

## Decisions already made

- **No official or community NeoForge port of CoFHCore exists** to build on. The
  community fork (SPLIGAN's `ThermalExpansionForNeoForge`/`ThermalCoreForNeoForge`,
  `ThermalDynamicsForNeoForge`, cloned as sibling repos) is a real 1.21.1 port of the
  three Thermal repos but depends on a `cofh_core:1.21.1-11.0.2.0` artifact that only
  existed in SPLIGAN's `mavenLocal()` — it can't compile as checked out, and CoFHCore is
  ported here by hand from its existing 1.20.4 codebase. The forks are the Phase A
  reference for the other three repos.
- **Target: NeoForge 26.1.2.109 via 1.21.1 (21.1.251)**, no other intermediates
  (Joel, 2026-09-21). The primers for every skipped version are still *read* (vendored in
  `docs/reference/`), they are just not *built against*.
- **Build plugin: ModDevGradle 2.0.147**, switched once in Phase 0 (replacing NeoGradle
  userdev) — it is what Pyronetics builds 26.1.2 with on this machine, and it produces the
  patched-sources jar used to confirm shapes. See docs/port-plan.md §1.4 / §4.3.
- **Patchouli stays** through 26.1.2 (GitHub-release jar `patchouli-neoforge-26.1-94.jar`
  as a `libs/` dependency in ThermalCore until it reaches maven).
- **License**: "CoFH - Don't Be a Jerk (Learn, Don't Steal)" — permits forking,
  modifying, and copying portions of CoFH's code. Art/sound assets are separately
  licensed CC BY-SA 4.0.
- **Branch per target version** (`1.20.4`, `1.20.6`, `1.21.1`, `26.1.2`), climbing in
  place rather than one long-lived branch — matches the sibling repos' branch strategy.
  `1.20.6` is a dead end kept for history; `1.21.1` was branched from it.
- **Code style matches upstream CoFH** (Joel, 2026-09-22). The port is meant to go upstream as
  pull requests, so every added line follows [docs/code-style.md](docs/code-style.md). Keep
  comments brief (one line, no version tags, porting narration or doc pointers) unless the change
  is major. API findings go in the api-notes docs, not in code comments.
- **Verify every API shape against the real jar**, never against summarized docs or
  recollection of older versions. With ModDevGradle that is the patched *sources* jar:
  `unzip -p build/moddev/artifacts/neoforge-<neo_version>-sources.jar net/minecraft/.../X.java` (1.21.1 naming; 26.1's is `minecraft-patched-<neo_version>-sources.jar`)
  (NeoForge's own: `~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/<ver>/*/neoforge-<ver>-sources.jar`),
  or `javap -p` on `minecraft-patched-<ver>.jar`. This has been the single most reliable
  practice of the port so far — see progress-log.md's "working method" section.

## Current state

**Phase A (1.21.1) and Phase B (26.1.2) are both code-complete: all four repos compile and boot headless on 26.1.2. Owed: Joel's client pass.** (2026-09-22)

| Repo | Branch | State |
|---|---|---|
| CoFHCore | `26.1.2` | **0 errors** (baseline 2445), B.0-B.9 done; `runData` clean, boots headless with mixins applied. B.10 added the recipe-template bridge (`RecipeJsonUtils`, `JsonMapCodec.of`, `FluidIngredient`) |
| ThermalCore | `26.1.2` | **0 errors** (baseline 2142), boots headless to `Done` with 1760 recipes, `runData` clean. Resources on the 26.1 layout |
| ThermalDynamics | `26.1.2` | **0 errors** (baseline 368), boots headless to `Done` with 1770 recipes, `runData` clean |
| ThermalExpansion | `26.1.2` | **0 errors** (baseline 243), boots headless to `Done` with 2287 recipes (loads all three mods), `runData` clean |
| all four | `1.21.1` | 0 errors, boot headless, `runData` clean (Phase A) |

All repos build with **ModDevGradle 2.0.147**. Shape oracles differ by branch:
`build/moddev/artifacts/minecraft-patched-26.1.2.109-sources.jar` on `26.1.2`,
`neoforge-21.1.251-sources.jar` on `1.21.1`. MDG keeps both in `build/moddev/artifacts/`, so
switching branches doesn't rebuild them. The Thermal repos `includeBuild('../CoFHCore')`, so
**whatever branch CoFHCore has checked out is what they compile against**. CoFHCore is on `26.1.2`
now; the Thermal repos' `1.21.1` branches need CoFHCore on `1.21.1` to build.

### Resuming Phase B: read this first

- **Where things stand**: [docs/TODO.md](docs/TODO.md) has the per-step table (commit and error
  count per step), what B.10 inherits from each step, and the Inbox of behaviour changes the new
  API forced. [docs/api-notes-26.1.2.md](docs/api-notes-26.1.2.md) has every confirmed shape, B.0–B.9.
- **Design principle, chosen by Joel for B.3 and applied again in B.4: bridge at the edge.** Keep
  CoFH's own layers (`CompoundTag` read/write chains, legacy `IItemHandler`/`IFluidHandler`/
  `IEnergyStorage` storages) and adapt only at the vanilla/NeoForge boundary. That keeps the
  upstream diff small and the Thermal repos mostly unchanged. Prefer it for new questions of the
  same kind, but ask when a choice shapes downstream code.
- **Working method**, repeated for each step:
  1. Compile, then group errors by file and by missing symbol.
  2. Read the target API in `build/moddev/artifacts/minecraft-patched-26.1.2.109-sources.jar` or the
     NeoForge sources jar before writing anything.
  3. For mechanical sweeps (tag getters, renames), use a paren-aware script restricted to known
     receivers. Afterwards, **restore any commented-out upstream code the sweep touched**: diff
     against the previous commit and put back changed comment-only lines.
  4. Fix imports: add what's needed, drop only what the change orphaned, and sort into IntelliJ
     order only in files whose previous version was already sorted.
  5. Recompile; the error count should only fall.
  6. Write the shapes into api-notes, update the TODO table and progress log, commit, push.
- **Broad, independent tails** (B.5's ~90 files, all of B.7) went well split across parallel agents by
  exclusive file ownership, with the style guide, the api-notes and "verify against the jar" in the
  prompt, and an explicit contract (kept names/signatures) wherever two areas touch. Review their
  behaviour decisions afterwards; some belong in the TODO Inbox. Condensed reports from the B.7 agents
  are in `../ThermalExpansion/docs/context/subagents-2026-09-22-c/*-report.md`.
- **B.10 is done for all three Thermal repos** (2026-09-22). What is left is verification, not porting:
  Joel's client pass (port plan §A.4 / §B.10 checklist — everything client-side is unverified on both hops),
  B.4 at runtime (a pipe mod or a GameTest, including an aborted simulation), and the Inbox in
  [docs/TODO.md](docs/TODO.md) (behaviour changes the new API forced, per repo). The boot-only lessons
  (id-before-construction, no stacks at recipe-parse time, `RegistryOps` for ingredients, missing tags are
  hard errors, `validateSpec` on config defaults, generated assets in `resources`) are in api-notes
  "B.10 ThermalCore" and "B.10 ThermalExpansion and ThermalDynamics". To boot any repo:
  `scripts/verify_runserver.sh <repo> <log> 300` (from a Thermal repo, `../CoFHCore/scripts/...`). It clears
  a stale server and `session.lock` for that repo itself, and exits non-zero unless the server reaches `Done`.
  **To verify everything**: `scripts/verify_all.sh [log-dir]` runs all four repos in dependency order (compile,
  `runData` must not change committed output, headless boot, GameTests) and stops at the first failure. GameTests
  live in each repo's `src/gametest` (never in the jar); shapes are in api-notes "GameTests".

**Phase A's one owed item is the client pass** (port plan §A.4), which is Joel's to run. The
`runData` pass already found and fixed one client crash (`LevelRendererMixin`), and
`MouseHandlerMixin` is flagged in [docs/TODO.md](docs/TODO.md) as a specific thing to check.
Phase B's order and contents are in [docs/port-plan.md](docs/port-plan.md) §6. Live status by
category, with error counts, is in the TODO.
