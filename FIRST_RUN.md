# First local run

This plugin was scaffolded in a container without access to the JetBrains
repositories, so the IntelliJ Platform build has never run. This is the
checklist for the first build on a machine with normal network access and
JDK 21. Each step says what to expect and the likely fix if it fails.

## What was verified without the platform

| Part | How | Result |
|---|---|---|
| Gradle scripts | `./gradlew help` / `tasks` with Gradle 8.10, IntelliJ Platform Gradle Plugin 2.9.0, Grammar-Kit plugin 2022.3.2.2, Kotlin 2.0.21 | configure without errors or deprecation warnings |
| JFlex lexer + semicolon insertion (`ZellLexer`) | generated with stock JFlex 1.9.1 and `idea-flex.skeleton`, run against minimal stand-ins for `FlexAdapter`/`IElementType` | `ZellLexerTest`: 30/30 pass |
| Lexer vs compiler | token types, UTF-16 spans and automatic semicolons dumped for all 1142 `.zl` files of `examples/`, `tests/spec/`, `std/` and compared with `compiler/src/lexer.rs` (built standalone) | identical (`>>` re-joined, see below) |
| Grammar | `Zell.bnf` interpreted as PEG (ordered choice, greedy repetition, no pins: what Grammar-Kit generates for this grammar) over that token stream | upstream grammar: 56/1142 files parse; with `tools/grammar-patches/0001-*.patch`: 1142/1142; test fixtures and the color-page demo parse too |
| PSI/stub Kotlin | `psi/`, `stubs/` (except the goto contributor), `lang/`, `lexer/`, `parser/ZellParserDefinition`, `ZellParserUtil` compiled against the IntelliJ core classes inside `kotlin-compiler-2.0.21.jar`, with hand-written stand-ins for the generated `ZellTypes`/`Zell*DeclImpl` (shaped like Grammar-Kit 2022.3.2 test data) | compiles |
| Everything else | read against the 2024.2 (`idea/242.20224.300`) sources of intellij-community | not compiled |

The LSP API (`com.intellij.platform.lsp`) is closed source: the LSP code
was written from the documentation only (see the `TODO(verify)` list below).

## Steps

1. **Gradle**: `./gradlew --version` → Gradle 8.10. Use JDK 21 (2024.2
   platform jars are Java 21 class files; CLAUDE.md said 17, corrected).

2. **Sync**: `tools/sync-zell.sh` → prints
   no `applied tools/grammar-patches/...` line (the corpus fixes are upstream since zell 6e5a456) and
   the corpus size; `git diff src/main/grammar/Zell.bnf` must be empty.

3. **Generate**: `./gradlew generateZellLexer generateZellParser`.
   Expect `src/main/gen/org/zell/idea/lexer/_ZellLexer.java`,
   `src/main/gen/org/zell/idea/parser/ZellParser.java`,
   `src/main/gen/org/zell/idea/psi/ZellTypes.java` and
   `src/main/gen/org/zell/idea/psi/{,impl/}Zell*.java`. Check:
   - Grammar-Kit warnings about unused tokens (`LINE_COMMENT`,
     `BLOCK_COMMENT`, `NEWLINE`, `WHITE_SPACE`) are expected: the lexer
     uses `ZellTokenTypes` and `TokenType.WHITE_SPACE` instead.
   - `ZellParser.java` imports `static org.zell.idea.parser.ZellParserUtil.*`
     (not `GeneratedParserUtilBase.*`); `ZellParserUtil` hides the
     `consumeToken*`/`nextTokenIs*` methods to glue `> >` into `SHR`.
   - `ZellTypes.STRUCT_DECL` (and `INTERFACE_DECL`, `ENUM_DECL`,
     `FUNC_DECL`, `CONST_DECL`) is `ZellStubElementTypes.factory("STRUCT_DECL")`.
   - `ZellStructDeclImpl extends ZellDeclMixin` with the constructors
     `(ZellDeclStub, IStubElementType)`, `(ASTNode)` and
     `(ZellDeclStub, IElementType, ASTNode)`; the interfaces extend
     `ZellNamedElement, StubBasedPsiElement<ZellDeclStub>`.
   - If the header attributes are rejected, they are only in the block
     between the `plugin-specific` markers that `tools/sync-zell.sh` writes.

4. **Compile**: `./gradlew compileKotlin compileJava`. Likely fix-ups:
   - `Unresolved reference: lsp` → add `bundledModule("intellij.platform.lsp.impl")`
     in `build.gradle.kts` (comment there).
   - LSP descriptor properties (`lspGoToDefinitionSupport`,
     `lspHoverSupport`, `lspFormattingSupport`, `getLanguageId`): adjust to
     the 2024.2 API or delete; the platform defaults enable them anyway.
   - A generated PSI getter clashing with a `ZellDeclMixin` member: rename
     in the mixin.

5. **Tests**: `./gradlew test`.
   - `ZellLexerTest` (plain JUnit) — passed locally with stand-ins.
   - `ZellParserCorpusTest` — parses `build/zell/{examples,tests/spec,std}/**/*.zl`
     (sync first), fails listing `file:line: error | source line`. Only
     `tests/spec/basics/bad_escape.zl` is skipped (its first line names a
     lexer diagnostic). If it fails: the grammar or the lexer differs from
     the PEG model; fix the grammar in `tools/grammar-patches/` (and upstream).
   - `ZellHighlightingTest`, `ZellFoldingTest`, `ZellStructureViewTest`
     (also checks the stub index) — fixtures under `src/test/testData/`.
     The folding test compares exact `<fold>` regions; adjust the fixture
     if the platform adds regions of its own.

6. **Commit the generated sources** (CLAUDE.md: `src/main/gen` is
   committed; CI checks it is current): `git add src/main/gen`.

7. **Run**: `./gradlew runIde`, open `build/zell` as a project, then
   `examples/hello.zl`:
   - colours (also Settings › Editor › Color Scheme › Zell), brace
     matching, Ctrl+/ comments, folding of the import group and bodies;
   - Structure (Alt+7) shows `namespace examples.hello` › `main()`;
   - Navigate › Symbol finds top-level declarations;
   - External Libraries contains **Zell std** (needs `zell` on PATH or
     the settings page);
   - with `zell-lsp` installed: diagnostics, hover, Ctrl+B, completion
     after `.`, Reformat Code (zell fmt), Tools › Restart Zell Language
     Server; the server's stderr is in `idea.log`.

8. **Verify**: `./gradlew verifyPlugin` (IC 2024.2.4, IU 2024.2.4,
   IU 2025.2, GoLand 2024.2.3). In IC the LSP part must be reported as an
   optional dependency that is not available, not as an error.

## `TODO(verify)` in the code

| Where | What to check |
|---|---|
| `build.gradle.kts` (dependencies) | LSP API on the compile classpath without `bundledModule("intellij.platform.lsp.impl")` |
| `build.gradle.kts` (`pluginVerification`) | the IDE versions resolve (`./gradlew printProductsReleases`), else use `recommended()` |
| `stubs/ZellDeclStubElementType.kt` | generated `Zell*DeclImpl` names and `(stub, IStubElementType)` constructors |
| `lsp/ZellLspServerSupportProvider.kt` (header) | `LspServerSupportProvider.fileOpened(project, file, serverStarter)`, `LspServerStarter.ensureServerStarted`, `ProjectWideLspServerDescriptor(project, name)`, `isSupportedFile`, `createCommandLine` — https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html |
| `lsp/ZellLspServerSupportProvider.kt` (descriptor) | `lspGoToDefinitionSupport`/`lspHoverSupport` (Boolean) and `lspFormattingSupport` (`LspFormattingSupport.shouldFormatThisFileExclusivelyByServer`) exist as `open val`s in 242 |
| `lsp/ZellLspServerSupportProvider.kt` (`getLanguageId`) | overridable in 242 |
| `lsp/ZellLspRestartAction.kt` | `LspServerManager.getInstance(project).stopAndRestartIfNeeded(Class)` |

## Design notes the first run depends on

- **Two lexers.** `ZellHighlightingLexer` (the JFlex lexer, restartable,
  no semicolons) for highlighting; `ZellLexer` (parser only, always from
  offset 0 and state 0) adds the zero-width automatic `SEMI` exactly where
  `compiler/src/lexer.rs` puts it and splits `>>`/`>>=` into `>` `>` /
  `>` `>=` (the compiler parser's `expect_gt` does the same for nested
  generic arguments). Inserted semicolons sit right after the token that
  ends the statement, before trailing comments.
- **Grammar patches.** None pending: the corpus fixes were merged upstream
  (zell commit 6e5a456). The mechanism stays for future divergences.
- **Stubs.** One stub class (`ZellDeclStub`, name only) for `struct`,
  `interface`, `enum`, `func` (incl. methods) and top-level `const`; the
  index holds top-level names only. Bump `ZellStubElementTypes.FILE`'s
  stub version when the grammar or stub format changes.
