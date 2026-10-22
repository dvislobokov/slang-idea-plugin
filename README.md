# Zell for IntelliJ

IntelliJ Platform plugin for the [Zell](https://github.com/zell-lang/zell)
language. Syntax comes from a Grammar-Kit PSI generated from the language's
canonical grammar (`grammar/zell.bnf`); semantics come from the Zell
language server `zell-lsp`, hosted through the platform's own LSP API
(`com.intellij.platform.lsp`), the way the Go plugin hosts `gopls`.

The project was formerly named Slang (`.sl` files, `slang`/`slang-lsp`, plugin id `org.slang.idea`).

Status: milestone 1 (skeleton) and the start of milestone 2 (LSP hosting).
The code has not been built against the IntelliJ Platform yet; see
[FIRST_RUN.md](FIRST_RUN.md) for the first local build and what to check.

## Install

Build the plugin (`./gradlew buildPlugin`) or download the `zell-idea-plugin`
artifact of a CI run, then in the IDE: **Settings | Plugins | ⚙ | Install
Plugin from Disk…** and pick `build/distributions/zell-idea-plugin-*.zip`.

Supported IDEs: 2024.2 – 2025.2 (builds 242 – 252.*). The LSP features need
a commercial IDE (IntelliJ IDEA Ultimate, GoLand, WebStorm, …); in IntelliJ
IDEA Community the plugin loads with the PSI features only.

Install the toolchain with `scripts/install.sh` (or `install.ps1`) from the
zell repository; it links `zell` and `zell-lsp` into `~/.local/bin`.

## Features

| Milestone | Feature | Source |
|---|---|---|
| 1 | `.zl` file type and icon | plugin |
| 1 | Highlighting: keywords, literals, comments, operators, `?` | lexer (JFlex) |
| 1 | Declaration names, attributes (`@json`), namespace paths | PSI annotator |
| 1 | Brace matching, `//` and `/* */` commenting | plugin |
| 1 | Folding: bodies, multi-line comments, import groups | PSI |
| 1 | Structure view: namespace › types › fields/methods, functions, constants | PSI |
| 1 | Navigate › Symbol over top-level declarations | stub index |
| 1 | Color scheme page (Settings › Editor › Color Scheme › Zell) | plugin |
| 2 | Standard library (`zell env ZELL_STD`) as an external library | toolchain |
| 2 | Diagnostics, go to definition, references, hover, completion, rename, code actions, semantic tokens, formatting (`zell fmt`) | zell-lsp |
| 2 | Tools › Restart Zell Language Server | plugin |
| 2 | Code lens commands: Run test, Debug test (runs with `-v`), Run, N references (`zell.runTest`, `zell.debugTest`, `zell.run`, `zell.showReferences`) | zell-lsp + plugin |
| 3–5 | Run/test configurations, debugger (DAP), templates, wizard | planned, see CLAUDE.md |

## Settings

**Settings | Languages & Frameworks | Zell**

- `zell` — the compiler. Empty: found on `PATH`, then in
  `$ZELL_HOME/toolchain/current/bin` and `~/.zell/toolchain/current/bin`.
- `zell-lsp` — the language server. Empty: found on `PATH` or next to
  `zell`; when only `zell` is found the IDE runs `zell lsp`.
- `ZELL_HOME` — used for `std/` when `zell env ZELL_STD` is unavailable.

Changing the settings restarts the language server and re-attaches the
standard library.

## Development

```sh
tools/sync-zell.sh                 # build/zell at the pinned commit + src/main/grammar/Zell.bnf
./gradlew test                      # lexer, parser corpus, highlighting, folding, structure tests
./gradlew runIde                    # sandbox IDE (IntelliJ IDEA Ultimate 2024.2)
./gradlew buildPlugin verifyPlugin  # zip + Plugin Verifier
```

The grammar is synced from the zell repository at `zellCommit`
(`gradle.properties`); pending grammar fixes that are meant for upstream
live in `tools/grammar-patches/`. See [CLAUDE.md](CLAUDE.md) for the
architecture and rules.
