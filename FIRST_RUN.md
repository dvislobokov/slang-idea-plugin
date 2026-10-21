# First local run

This plugin was scaffolded in a container without access to the JetBrains
repositories, so the IntelliJ Platform build has never run. This is the
checklist for the first build on a machine with normal network access and
JDK 21. Each step says what to expect and the likely fix if it fails.

## What was verified without the platform

| Part | How | Result |
|---|---|---|
| Gradle scripts | `./gradlew help` / `tasks` with Gradle 8.10, IntelliJ Platform Gradle Plugin 2.9.0, Grammar-Kit plugin 2022.3.2.2, Kotlin 2.0.21 | configure without errors or deprecation warnings |
| JFlex lexer + semicolon insertion (`SlangLexer`) | generated with stock JFlex 1.9.1 and `idea-flex.skeleton`, run against minimal stand-ins for `FlexAdapter`/`IElementType` | `SlangLexerTest`: 30/30 pass |
| Lexer vs compiler | token types, UTF-16 spans and automatic semicolons dumped for all 1142 `.sl` files of `examples/`, `tests/spec/`, `std/` and compared with `compiler/src/lexer.rs` (built standalone) | identical (`>>` re-joined, see below) |
| Grammar | `Slang.bnf` interpreted as PEG (ordered choice, greedy repetition, no pins: what Grammar-Kit generates for this grammar) over that token stream | upstream grammar: 56/1142 files parse; with `tools/grammar-patches/0001-*.patch`: 1142/1142; test fixtures and the color-page demo parse too |
| PSI/stub Kotlin | `psi/`, `stubs/` (except the goto contributor), `lang/`, `lexer/`, `parser/SlangParserDefinition`, `SlangParserUtil` compiled against the IntelliJ core classes inside `kotlin-compiler-2.0.21.jar`, with hand-written stand-ins for the generated `SlangTypes`/`Slang*DeclImpl` (shaped like Grammar-Kit 2022.3.2 test data) | compiles |
| Everything else | read against the 2024.2 (`idea/242.20224.300`) sources of intellij-community | not compiled |

The LSP API (`com.intellij.platform.lsp`) is closed source: the LSP code
was written from the documentation only (see the `TODO(verify)` list below).

## Steps

1. **Gradle**: `./gradlew --version` → Gradle 8.10. Use JDK 21 (2024.2
   platform jars are Java 21 class files; CLAUDE.md said 17, corrected).

2. **Sync**: `tools/sync-slang.sh` → prints
   `applied tools/grammar-patches/0001-psi-grammar-corpus-fixes.patch` and
   the corpus size; `git diff src/main/grammar/Slang.bnf` must be empty.

3. **Generate**: `./gradlew generateSlangLexer generateSlangParser`.
   Expect `src/main/gen/org/slang/idea/lexer/_SlangLexer.java`,
   `src/main/gen/org/slang/idea/parser/SlangParser.java`,
   `src/main/gen/org/slang/idea/psi/SlangTypes.java` and
   `src/main/gen/org/slang/idea/psi/{,impl/}Slang*.java`. Check:
   - Grammar-Kit warnings about unused tokens (`LINE_COMMENT`,
     `BLOCK_COMMENT`, `NEWLINE`, `WHITE_SPACE`) are expected: the lexer
     uses `SlangTokenTypes` and `TokenType.WHITE_SPACE` instead.
   - `SlangParser.java` imports `static org.slang.idea.parser.SlangParserUtil.*`
     (not `GeneratedParserUtilBase.*`); `SlangParserUtil` hides the
     `consumeToken*`/`nextTokenIs*` methods to glue `> >` into `SHR`.
   - `SlangTypes.STRUCT_DECL` (and `INTERFACE_DECL`, `ENUM_DECL`,
     `FUNC_DECL`, `CONST_DECL`) is `SlangStubElementTypes.factory("STRUCT_DECL")`.
   - `SlangStructDeclImpl extends SlangDeclMixin` with the constructors
     `(SlangDeclStub, IStubElementType)`, `(ASTNode)` and
     `(SlangDeclStub, IElementType, ASTNode)`; the interfaces extend
     `SlangNamedElement, StubBasedPsiElement<SlangDeclStub>`.
   - If the header attributes are rejected, they are only in the block
     between the `plugin-specific` markers that `tools/sync-slang.sh` writes.

4. **Compile**: `./gradlew compileKotlin compileJava`. Likely fix-ups:
   - `Unresolved reference: lsp` → add `bundledModule("intellij.platform.lsp.impl")`
     in `build.gradle.kts` (comment there).
   - LSP descriptor properties (`lspGoToDefinitionSupport`,
     `lspHoverSupport`, `lspFormattingSupport`, `getLanguageId`): adjust to
     the 2024.2 API or delete; the platform defaults enable them anyway.
   - A generated PSI getter clashing with a `SlangDeclMixin` member: rename
     in the mixin.

5. **Tests**: `./gradlew test`.
   - `SlangLexerTest` (plain JUnit) — passed locally with stand-ins.
   - `SlangParserCorpusTest` — parses `build/slang/{examples,tests/spec,std}/**/*.sl`
     (sync first), fails listing `file:line: error | source line`. Only
     `tests/spec/basics/bad_escape.sl` is skipped (its first line names a
     lexer diagnostic). If it fails: the grammar or the lexer differs from
     the PEG model; fix the grammar in `tools/grammar-patches/` (and upstream).
   - `SlangHighlightingTest`, `SlangFoldingTest`, `SlangStructureViewTest`
     (also checks the stub index) — fixtures under `src/test/testData/`.
     The folding test compares exact `<fold>` regions; adjust the fixture
     if the platform adds regions of its own.

6. **Commit the generated sources** (CLAUDE.md: `src/main/gen` is
   committed; CI checks it is current): `git add src/main/gen`.

7. **Run**: `./gradlew runIde`, open `build/slang` as a project, then
   `examples/hello.sl`:
   - colours (also Settings › Editor › Color Scheme › Slang), brace
     matching, Ctrl+/ comments, folding of the import group and bodies;
   - Structure (Alt+7) shows `namespace examples.hello` › `main()`;
   - Navigate › Symbol finds top-level declarations;
   - External Libraries contains **Slang std** (needs `slang` on PATH or
     the settings page);
   - with `slang-lsp` installed: diagnostics, hover, Ctrl+B, completion
     after `.`, Reformat Code (slang fmt), Tools › Restart Slang Language
     Server; the server's stderr is in `idea.log`.

8. **Verify**: `./gradlew verifyPlugin` (IC 2024.2.4, IU 2024.2.4,
   IU 2025.2, GoLand 2024.2.3). In IC the LSP part must be reported as an
   optional dependency that is not available, not as an error.

## `TODO(verify)` in the code

| Where | What to check |
|---|---|
| `build.gradle.kts` (dependencies) | LSP API on the compile classpath without `bundledModule("intellij.platform.lsp.impl")` |
| `build.gradle.kts` (`pluginVerification`) | the IDE versions resolve (`./gradlew printProductsReleases`), else use `recommended()` |
| `stubs/SlangDeclStubElementType.kt` | generated `Slang*DeclImpl` names and `(stub, IStubElementType)` constructors |
| `lsp/SlangLspServerSupportProvider.kt` (header) | `LspServerSupportProvider.fileOpened(project, file, serverStarter)`, `LspServerStarter.ensureServerStarted`, `ProjectWideLspServerDescriptor(project, name)`, `isSupportedFile`, `createCommandLine` — https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html |
| `lsp/SlangLspServerSupportProvider.kt` (descriptor) | `lspGoToDefinitionSupport`/`lspHoverSupport` (Boolean) and `lspFormattingSupport` (`LspFormattingSupport.shouldFormatThisFileExclusivelyByServer`) exist as `open val`s in 242 |
| `lsp/SlangLspServerSupportProvider.kt` (`getLanguageId`) | overridable in 242 |
| `lsp/SlangLspRestartAction.kt` | `LspServerManager.getInstance(project).stopAndRestartIfNeeded(Class)` |

## Design notes the first run depends on

- **Two lexers.** `SlangHighlightingLexer` (the JFlex lexer, restartable,
  no semicolons) for highlighting; `SlangLexer` (parser only, always from
  offset 0 and state 0) adds the zero-width automatic `SEMI` exactly where
  `compiler/src/lexer.rs` puts it and splits `>>`/`>>=` into `>` `>` /
  `>` `>=` (the compiler parser's `expect_gt` does the same for nested
  generic arguments). Inserted semicolons sit right after the token that
  ends the statement, before trailing comments.
- **Grammar patches.** `tools/grammar-patches/0001-psi-grammar-corpus-fixes.patch`
  fixes rules of upstream `grammar/slang.bnf` that do not match the
  compiler (e.g. no `eos` after a top-level `}`); it should be sent
  upstream as is. The sync script skips it once upstream contains it.
- **Stubs.** One stub class (`SlangDeclStub`, name only) for `struct`,
  `interface`, `enum`, `func` (incl. methods) and top-level `const`; the
  index holds top-level names only. Bump `SlangStubElementTypes.FILE`'s
  stub version when the grammar or stub format changes.
