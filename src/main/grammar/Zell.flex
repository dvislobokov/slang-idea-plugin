// JFlex lexer for Slang, mirroring compiler/src/lexer.rs + token.rs of the
// slang repository (token set, literals, nested block comments).
//
// This lexer does NOT insert semicolons: it is used as-is for highlighting
// (SlangHighlightingLexer). The parser lexer org.slang.idea.lexer.SlangLexer
// wraps it and inserts the Go-style automatic semicolons exactly like the
// compiler (TokenKind::ends_statement, no insertion inside ( ) / [ ]).
//
// Generated with `./gradlew generateSlangLexer` into
// src/main/gen/org/slang/idea/lexer/_SlangLexer.java.

package org.slang.idea.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.slang.idea.psi.SlangTokenTypes;

import static org.slang.idea.psi.SlangTypes.*;

%%

%{
  /** Nesting depth of the block comment being scanned. */
  private int commentDepth = 0;
  /** Start offset of the block comment being scanned. */
  private int commentStart = 0;

  public _SlangLexer() {
    this((java.io.Reader) null);
  }
%}

%public
%class _SlangLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%state IN_BLOCK_COMMENT

// Rust char::is_whitespace (Unicode White_Space) without '\n'.
WS_CHAR = [ \t\r\f\u000B\u0085\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]
WHITE_SPACE = ({WS_CHAR} | \n)+

// is_ident_start: '_' or char::is_alphabetic; is_ident_continue adds char::is_numeric.
IDENT_START = [\p{Alphabetic}_]
IDENT_PART = [\p{Alphabetic}\p{N}_]
IDENTIFIER = {IDENT_START} {IDENT_PART}*

DEC = [0-9][0-9_]*
EXP = [eE][+-]?[0-9][0-9_]*
// The compiler consumes every alphanumeric after 0x/0b/0o and reports bad digits.
RADIX_INT = 0[xXbBoO][0-9a-zA-Z_]*
FLOAT = {DEC} "." [0-9][0-9_]* {EXP}? | {DEC} {EXP}
INT = {DEC}

// Unterminated literals still form one token (the compiler reports them and
// keeps the token, so the semicolon rule sees a literal).
STRING = \" ([^\"\\\n] | \\[^\n])* \\? \"?
RAW_STRING = ` [^`]* `?
CHAR = ' (\\[^\n] | [^'\\\n]) [^'\n]* ' | '' | ' (\\[^\n] | [^'\\\n])?

LINE_COMMENT = "//" [^\n]*

%%

<YYINITIAL> {
  {WHITE_SPACE}      { return TokenType.WHITE_SPACE; }
  {LINE_COMMENT}     { return SlangTokenTypes.LINE_COMMENT; }
  "/*"               { commentDepth = 1; commentStart = getTokenStart(); yybegin(IN_BLOCK_COMMENT); }

  // Keywords (token.rs TokenKind::keyword)
  "namespace"        { return NAMESPACE; }
  "import"           { return IMPORT; }
  "as"               { return AS; }
  "struct"           { return STRUCT; }
  "interface"        { return INTERFACE; }
  "enum"             { return ENUM; }
  "func"             { return FUNC; }
  "static"           { return STATIC; }
  "async"            { return ASYNC; }
  "await"            { return AWAIT; }
  "var"              { return VAR; }
  "const"            { return CONST; }
  "if"               { return IF; }
  "else"             { return ELSE; }
  "for"              { return FOR; }
  "in"               { return IN; }
  "switch"           { return SWITCH; }
  "case"             { return CASE; }
  "default"          { return DEFAULT; }
  "return"           { return RETURN; }
  "break"            { return BREAK; }
  "continue"         { return CONTINUE; }
  "defer"            { return DEFER; }
  "try"              { return TRY; }
  "catch"            { return CATCH; }
  "new"              { return NEW; }
  "nil"              { return NIL; }
  "true"             { return TRUE; }
  "false"            { return FALSE; }
  "map"              { return MAP; }
  "this"             { return THIS; }
  "_"                { return UNDERSCORE; }

  {IDENTIFIER}       { return IDENTIFIER; }

  {RADIX_INT}        { return INT_LITERAL; }
  {FLOAT}            { return FLOAT_LITERAL; }
  {INT}              { return INT_LITERAL; }
  {STRING}           { return STRING_LITERAL; }
  {RAW_STRING}       { return RAW_STRING_LITERAL; }
  {CHAR}             { return CHAR_LITERAL; }

  // Punctuation (lexer.rs punct), longest match first by JFlex rules.
  "<<="              { return SHLEQ; }
  ">>="              { return SHREQ; }
  "<<"               { return SHL; }
  ">>"               { return SHR; }
  "<="               { return LE; }
  ">="               { return GE; }
  "=="               { return EQEQ; }
  "!="               { return NOTEQ; }
  "=>"               { return ARROW; }
  "&&"               { return ANDAND; }
  "||"               { return OROR; }
  "+="               { return PLUSEQ; }
  "-="               { return MINUSEQ; }
  "*="               { return STAREQ; }
  "/="               { return SLASHEQ; }
  "%="               { return PERCENTEQ; }
  "&="               { return AMPEQ; }
  "|="               { return PIPEEQ; }
  "^="               { return CARETEQ; }
  "@="               { return ATEQ; }
  ".."               { return DOTDOT; }
  "("                { return LPAREN; }
  ")"                { return RPAREN; }
  "{"                { return LBRACE; }
  "}"                { return RBRACE; }
  "["                { return LBRACKET; }
  "]"                { return RBRACKET; }
  ","                { return COMMA; }
  "."                { return DOT; }
  ":"                { return COLON; }
  ";"                { return SEMI; }
  "?"                { return QUESTION; }
  "!"                { return BANG; }
  "@"                { return AT; }
  "&"                { return AMP; }
  "*"                { return STAR; }
  "+"                { return PLUS; }
  "-"                { return MINUS; }
  "/"                { return SLASH; }
  "%"                { return PERCENT; }
  "|"                { return PIPE; }
  "^"                { return CARET; }
  "<"                { return LT; }
  ">"                { return GT; }
  "="                { return ASSIGN; }

  [^]                { return TokenType.BAD_CHARACTER; }
}

// `/* ... */`, nestable (lexer.rs block_comment). The whole comment is one
// token; an unterminated comment runs to the end of the input.
<IN_BLOCK_COMMENT> {
  "/*"               { commentDepth++; }
  "*/"               {
                       if (--commentDepth == 0) {
                         yybegin(YYINITIAL);
                         zzStartRead = commentStart;
                         return SlangTokenTypes.BLOCK_COMMENT;
                       }
                     }
  [^/*]+             { }
  [^]                { }
  <<EOF>>            {
                       yybegin(YYINITIAL);
                       zzStartRead = commentStart;
                       return SlangTokenTypes.BLOCK_COMMENT;
                     }
}
