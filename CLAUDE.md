# CLAUDE.md — Slang plugin for IntelliJ IDEA

An IntelliJ Platform plugin for the Slang language, built the same way as
the maintainer's Go plugin (`idea-golang-support`, which hosts `gopls`
through the platform's own LSP API): Grammar-Kit PSI for everything
syntactic, and the Slang language server `slang-lsp` hosted through
`com.intellij.platform.lsp.api` for semantics. No LSP4IJ. The compiler
CLI is used for formatting, run, test and debug.

The language lives in https://github.com/dvislobokov/slang. Read these
there before writing code:

| In the `slang` repo | Why |
|---|---|
| `DESIGN.md` | the language spec: syntax, semantics, imports, generics, errors, async |
| `grammar/slang.bnf` | canonical Grammar-Kit grammar; the PSI is generated from it |
| `compiler/src/lexer.rs`, `token.rs` | the reference lexer, incl. Go-style automatic `;` insertion (`ends_statement`) and raw strings in backticks |
| `editors/vscode/syntaxes/slang.tmLanguage.json` | reference highlighting categories |
| `std/` | stdlib sources (`.sl` files and generated `*_extern.sl` declarations with doc comments); index them like GOROOT |
| `examples/**/*.sl`, `tests/spec/**/*.sl` | parser acceptance corpus: the plugin's parser must accept every file the compiler accepts |
| `lsp/` | `slang-lsp`: diagnostics, definition, references, hover, rename, semantic tokens, folding, completion, signature help, inlay hints, code actions, formatting, workspace symbols (see `docs/TOOLING.md` for the method list) |
| `docs/TOOLING.md` | tooling plan (IDEA section, doc comments, `slang doc`) |
| `docs/DEBUGGER.md` | DAP protocol of the in-process debug agent (`SLANG_DEBUG=host:port`) |

## Decisions

- **PSI for syntax, LSP for semantics.** Grammar-Kit PSI gives file type,
  highlighting, brace matching, commenter, folding, structure view,
  breadcrumbs, spellchecker scopes, live templates, and fast local
  features. `slang-lsp` (hosted via `com.intellij.platform.lsp.api`
  `LspServerSupportProvider` / `ProjectWideLspServerDescriptor`, exactly as
  the Go plugin hosts `gopls`) provides diagnostics, go-to-definition,
  references, hover/quick documentation, rename, completion, signature
  help, inlay hints, code actions, semantic tokens and formatting. Do not
  re-implement resolve or type inference in Kotlin; if the LSP lacks
  something, add it to `slang-lsp` upstream (Rust) and sync.
- **Platform LSP API availability.** `com.intellij.platform.lsp` is part of
  the platform in the commercial IDEs (IntelliJ IDEA Ultimate, GoLand,
  WebStorm, ...) since 2023.2; it is not available in IntelliJ Community.
  Target the same IDEs the Go plugin targets. If Community support is
  wanted later, add an optional LSP4IJ-based module; it is not a goal now.
- **Semantic tokens vs PSI highlighting.** The PSI highlighter colours
  keywords, literals, comments and declarations; the LSP semantic tokens
  refine types/functions/namespaces/parameters, as the platform merges them.
- **Stdlib from disk.** `SLANG_HOME/std` (from `slang env SLANG_STD`) is
  attached as a library root so PSI-based features (structure, search by
  name, go-to-file) see std sources; navigation into std comes from the LSP,
  which already indexes `std/` and `.slang/deps`.
- **Target**: IntelliJ Platform 2024.2+, JDK 17 for the build, Kotlin,
  `org.jetbrains.intellij.platform` Gradle plugin 2.x. Keep
  `pluginSinceBuild` honest.
- **Toolchain detection**: `slang` and `slang-lsp` on PATH, `SLANG_HOME`,
  or paths in settings; show versions; offer to download via the install
  script from the slang repo (`scripts/install.sh` / `install.ps1`); restart
  the server when the toolchain changes.

## Layout

```
build.gradle.kts, settings.gradle.kts, gradle.properties
src/main/kotlin/org/slang/idea/
  lang/        SlangLanguage, SlangFileType, SlangFile, icons
  lexer/       Slang.flex (JFlex, generated from the BNF tokens) + semicolon insertion layer
  parser/      SlangParserDefinition, generated parser (gen/), error recovery tweaks
  psi/         PSI interfaces + impl mixins (named elements for structure view, go-to-symbol), element types
  stubs/       stub indexes for declarations (structure/search by name only; semantics come from the LSP)
  lsp/         LspServerSupportProvider + ProjectWideLspServerDescriptor for slang-lsp, settings (inlay hints), restart action, custom LSP requests if needed
  highlighting/ PSI syntax highlighter, color settings page (semantic tokens come from the LSP)
  structure/   structure view, breadcrumbs, folding, brace matcher, commenter, spellchecker
  formatter/   `slang fmt` as the formatter (via LSP textDocument/formatting or the CLI), code style page (read-only, gofmt-like)
  run/         run configurations: slang run / build / test (test tree via `--json`), console filters for error traces
  debug/       XDebugProcess over DAP to the in-process agent (breakpoints, stepping per task, variables)
  toolchain/   locating slang/slang-lsp, SLANG_HOME, std root, versions, settings UI
  templates/   live templates, file templates, new-project wizard (`slang init`)
src/main/resources/META-INF/plugin.xml, icons, colorSchemes, fileTemplates, inspectionDescriptions
src/test/kotlin/   BasePlatformTestCase tests + the corpus test
src/test/testData/ fixtures
tools/sync-slang.sh  fetches the slang repo at the pinned commit into build/slang (grammar, std, corpus)
```

## Build and test

```sh
./gradlew buildPlugin          # produces build/distributions/*.zip
./gradlew test                 # headless platform tests, incl. the parser corpus test
./gradlew runIde               # launches a sandbox IDE (needs a desktop)
./gradlew verifyPlugin         # IntelliJ Plugin Verifier against the sinceBuild..untilBuild range
```

`tools/sync-slang.sh` clones `dvislobokov/slang` at the commit pinned in
`gradle.properties` (`slangCommit=...`) into `build/slang`; the parser
corpus test parses every `examples/**/*.sl` and `tests/spec/**/*.sl` (except
files whose first line marks a lexer error) and fails on any `PsiErrorElement`.
Bump `slangCommit` deliberately; when the grammar changes upstream, regenerate.

Grammar-Kit generation is a Gradle task (`generateSlangParser`,
`generateSlangLexer`) with outputs under `src/main/gen` (committed, so IDE
indexing works without a build).

## Rules

- **Grammar fidelity.** `grammar/slang.bnf` upstream is the source of
  truth; do not fork its rules silently. If the plugin needs a change
  (e.g. recovery sets), make it in the BNF upstream via a PR to the slang
  repo and sync. Semicolon insertion must match the compiler's
  `ends_statement` list exactly (idents, literals, `return/break/continue/
  nil/true/false/this/_`, `) ] } ? ! >`, `>>`); `x.` at line end joins the
  next line, as in the compiler.
- **Don't duplicate the language server.** Resolve, types, completion,
  diagnostics, rename and quick fixes come from `slang-lsp`; the plugin
  only wires them. Missing features are added upstream in `lsp/` (Rust)
  and the pinned slang commit is bumped.
- **Keep PSI and LSP consistent.** The PSI parser must accept everything
  the compiler accepts (corpus test); PSI-side features must not contradict
  LSP results (e.g. structure view names vs LSP document symbols).
- **Performance.** Stubs for top-level declarations only; no PSI-side
  analysis on keystroke; the LSP server is one per project, restarted on
  toolchain change, with its stderr in the IDE log.
- **Tests for everything**: `BasePlatformTestCase` for PSI features with
  fixtures; LSP integration tests start the real `slang-lsp` from the
  toolchain (skip with a message if not installed) and check diagnostics,
  completion and navigation on fixtures; the corpus test is mandatory and
  must stay green.
- **Style**: Kotlin official style, `org.slang.idea` package, no reflection
  hacks into platform internals, `@Suppress` only with a reason.
- **Git**: work on `main`, small commits in English imperative; never
  commit `build/`, `.gradle/`, `.intellijPlatform/`, or the synced
  `build/slang`.
- **Release**: `./gradlew buildPlugin verifyPlugin`; publishing to
  JetBrains Marketplace is done by the maintainer with their token
  (`PUBLISH_TOKEN` env in CI), never from an agent session.

## Milestones

1. **Skeleton**: Gradle project, file type, icons, JFlex lexer with
   semicolon insertion, Grammar-Kit parser, syntax highlighting, brace
   matcher, commenter, folding, structure view; corpus test green.
2. **LSP hosting**: toolchain settings, `LspServerSupportProvider` for
   `slang-lsp`, diagnostics, navigation, hover, rename, completion,
   signature help, inlay hints (with settings), code actions, semantic
   tokens, formatting; std root attached; restart action; integration tests.
3. **Run and test**: run/build/test configurations with the test tree
   (`slang test --json`), console filters for error traces and panics,
   `slang fmt` on save option, quick documentation rendering from LSP hover.
4. **Debugger**: DAP client to the in-process agent (`slang debug run`),
   breakpoints with conditions/logpoints, task-aware stepping, variables.
5. **Polish**: live templates, file templates, new-project wizard
   (`slang init`), color settings page, Marketplace listing, Plugin
   Verifier in CI.
