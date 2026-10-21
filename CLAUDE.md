# CLAUDE.md — Slang plugin for IntelliJ IDEA

A native IntelliJ Platform plugin for the Slang language, built the way
JetBrains builds the Go plugin: Grammar-Kit PSI, stubs and indexes,
references, type inference and completion in Kotlin. No LSP in the core.
The Slang compiler is used only as an external tool (diagnostics,
formatting, run, test, debug).

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
| `docs/TOOLING.md` | tooling plan (IDEA section, doc comments, `slang doc`) |
| `docs/DEBUGGER.md` | DAP protocol of the in-process debug agent (`SLANG_DEBUG=host:port`) |

## Decisions

- **Native, not LSP.** PSI, resolve, completion, navigation, rename, find
  usages, structure view, folding, formatter, inspections and intentions
  are implemented in Kotlin on the PSI. `slang-lsp` is not a dependency.
- **The compiler is the oracle for semantics it owns.** Compiler
  diagnostics come from `slang check --json <project>` run by an external
  annotator (debounced, per project); the plugin does not re-implement the
  type checker. Plugin-side type inference covers what completion and
  navigation need: locals, params, fields, methods, static methods, enum
  variants, namespaces/imports, generics by declared signatures, `this`.
- **Stdlib from disk.** `SLANG_HOME/std` (from `slang env SLANG_STD`) is
  attached as a library root and indexed with the same stubs as user code;
  `*_extern.sl` files are declarations only.
- **Target**: IntelliJ Community 2024.2+ (and all IDEs on the platform),
  JDK 17 for the build, Kotlin, `org.jetbrains.intellij.platform` Gradle
  plugin 2.x. Keep `pluginSinceBuild` honest.
- **Toolchain detection**: `slang` on PATH, `SLANG_HOME`, or a path in
  settings; show the version; offer to download via the install script
  from the slang repo (`scripts/install.sh` / `install.ps1`).

## Layout

```
build.gradle.kts, settings.gradle.kts, gradle.properties
src/main/kotlin/org/slang/idea/
  lang/        SlangLanguage, SlangFileType, SlangFile, icons
  lexer/       Slang.flex (JFlex, generated from the BNF tokens) + semicolon insertion layer
  parser/      SlangParserDefinition, generated parser (gen/), error recovery tweaks
  psi/         PSI interfaces + impl mixins (named elements, references), stubs, element types
  stubs/       stub indexes: namespaces, structs/enums/interfaces, functions, methods, consts
  resolve/     scopes, import rules (import std / std.io / none → see DESIGN.md table), references
  types/       lightweight type model and inference for completion/navigation
  completion/  contributors (members after `.`, names in scope, keywords, imports, attributes)
  highlighting/ syntax + semantic annotators, color settings page
  structure/   structure view, breadcrumbs, folding, brace matcher, commenter, spellchecker
  formatter/   formatting model (gofmt-like rules from docs/TOOLING.md) + `slang fmt` external formatter
  inspections/ unused variable/import, unhandled `T!`, `?` outside fallible fn (mirror compiler codes E0001..)
  annotator/   external annotator running `slang check --json`
  run/         run configurations: slang run / build / test (test tree via `--json`), console filters for error traces
  debug/       XDebugProcess over DAP to the in-process agent (breakpoints, stepping per task, variables)
  toolchain/   locating slang, SLANG_HOME, std root, version, settings UI
  docs/        quick documentation from `//` doc comments (Markdown, first sentence = summary, `Deprecated:`)
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
- **Resolve follows DESIGN.md imports.** `import std.io` → bare `println`;
  `import std` → `io.println`; no import → `std.io.println`; `as` aliases;
  ambiguity is an error only when used; uppercase exported, lowercase
  private to namespace (stdlib lowercase builtins are the exception).
- **Don't duplicate the type checker.** If a check needs full inference
  (operator overloading rules, generic constraint satisfaction, ownership),
  leave it to the external annotator. Plugin inspections cover only
  cheap, local, high-confidence checks and must carry the compiler's code
  (E0001 unused variable, E0003 unused import, ...) so quick fixes match.
- **Performance.** Stubs for all top-level declarations and members; no
  full-file re-parse on keystroke beyond what the platform does;
  completion must answer in < 100 ms on `std` + a 50k-line project; the
  external annotator is debounced and runs on the whole project, not per
  file.
- **Tests for everything**: each feature gets a `BasePlatformTestCase`
  test with fixtures; completion/resolve tests run against a copy of
  `std/`; the corpus test is mandatory and must stay green.
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
2. **PSI and resolve**: named elements, stubs/indexes, scopes and import
   rules, references for namespaces/types/functions/fields/methods/locals,
   go-to-declaration, find usages, rename, std root attached.
3. **Completion and types**: member completion after `.`, in-scope names,
   keywords, attribute names and arguments, struct literal fields, import
   paths; lightweight type inference for `var x = ...`, calls, generics.
4. **Compiler integration**: toolchain settings, external annotator with
   `slang check --json`, quick fixes for E0001/E0002/E0003/E0004,
   formatter + `slang fmt`, run/build/test configurations with test tree,
   quick documentation from doc comments, error-trace console links.
5. **Debugger**: DAP client to the in-process agent (`slang debug run`),
   breakpoints with conditions/logpoints, task-aware stepping, variables.
6. **Polish**: inspections (unused, unhandled `T!`, missing import with
   fix), intentions (add `?`, wrap in `try`, add import), live templates,
   color settings, new-project wizard (`slang init`), Marketplace listing.
