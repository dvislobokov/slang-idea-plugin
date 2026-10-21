# Slang for IntelliJ

IntelliJ Platform plugin for the [Slang](https://github.com/dvislobokov/slang)
language. Syntax comes from a Grammar-Kit PSI generated from the language's
canonical grammar (`grammar/slang.bnf`); semantics come from the Slang
language server `slang-lsp`, hosted through the platform's own LSP API
(`com.intellij.platform.lsp`), the way the Go plugin hosts `gopls`.

Status: milestone 1 (skeleton) and the start of milestone 2 (LSP hosting).
The code has not been built against the IntelliJ Platform yet; see
[FIRST_RUN.md](FIRST_RUN.md) for the first local build and what to check.

## Install

Build the plugin (`./gradlew buildPlugin`) or download the `slang-idea-plugin`
artifact of a CI run, then in the IDE: **Settings | Plugins | ⚙ | Install
Plugin from Disk…** and pick `build/distributions/slang-idea-plugin-*.zip`.

Supported IDEs: 2024.2 – 2025.2 (builds 242 – 252.*). The LSP features need
a commercial IDE (IntelliJ IDEA Ultimate, GoLand, WebStorm, …); in IntelliJ
IDEA Community the plugin loads with the PSI features only.

Install the toolchain with `scripts/install.sh` (or `install.ps1`) from the
slang repository; it links `slang` and `slang-lsp` into `~/.local/bin`.

## Features

| Milestone | Feature | Source |
|---|---|---|
| 1 | `.sl` file type and icon | plugin |
| 1 | Highlighting: keywords, literals, comments, operators, `?` | lexer (JFlex) |
| 1 | Declaration names, attributes (`@json`), namespace paths | PSI annotator |
| 1 | Brace matching, `//` and `/* */` commenting | plugin |
| 1 | Folding: bodies, multi-line comments, import groups | PSI |
| 1 | Structure view: namespace › types › fields/methods, functions, constants | PSI |
| 1 | Navigate › Symbol over top-level declarations | stub index |
| 1 | Color scheme page (Settings › Editor › Color Scheme › Slang) | plugin |
| 2 | Standard library (`slang env SLANG_STD`) as an external library | toolchain |
| 2 | Diagnostics, go to definition, references, hover, completion, rename, code actions, semantic tokens, formatting (`slang fmt`) | slang-lsp |
| 2 | Tools › Restart Slang Language Server | plugin |
| 3–5 | Run/test configurations, debugger (DAP), templates, wizard | planned, see CLAUDE.md |

## Settings

**Settings | Languages & Frameworks | Slang**

- `slang` — the compiler. Empty: found on `PATH`, then in
  `$SLANG_HOME/toolchain/current/bin` and `~/.slang/toolchain/current/bin`.
- `slang-lsp` — the language server. Empty: found on `PATH` or next to
  `slang`; when only `slang` is found the IDE runs `slang lsp`.
- `SLANG_HOME` — used for `std/` when `slang env SLANG_STD` is unavailable.

Changing the settings restarts the language server and re-attaches the
standard library.

## Development

```sh
tools/sync-slang.sh                 # build/slang at the pinned commit + src/main/grammar/Slang.bnf
./gradlew test                      # lexer, parser corpus, highlighting, folding, structure tests
./gradlew runIde                    # sandbox IDE (IntelliJ IDEA Ultimate 2024.2)
./gradlew buildPlugin verifyPlugin  # zip + Plugin Verifier
```

The grammar is synced from the slang repository at `slangCommit`
(`gradle.properties`); pending grammar fixes that are meant for upstream
live in `tools/grammar-patches/`. See [CLAUDE.md](CLAUDE.md) for the
architecture and rules.
