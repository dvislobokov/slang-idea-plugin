package org.slang.idea.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.psi.tree.IElementType;

import static org.slang.idea.psi.SlangTypes.*;

/**
 * Parser utilities for the generated {@code SlangParser} ({@code parserUtilClass}
 * in the grammar header, a plugin-specific line added by tools/sync-slang.sh).
 *
 * <p>The parser lexer ({@link org.slang.idea.lexer.SlangLexer}) splits {@code >>}
 * into two adjacent {@code GT} tokens and {@code >>=} into {@code GT GE}, so that
 * {@code generic_args} can close nested arguments ({@code List<List<int>>}), as
 * the compiler's {@code expect_gt} does. Where the grammar asks for {@code SHR}
 * or {@code SHREQ}, the static methods below (which hide the ones of
 * {@link GeneratedParserUtilBase} for the generated code, since it imports
 * {@code SlangParserUtil.*}) accept such an adjacent pair and collapse it into a
 * single {@code SHR}/{@code SHREQ} leaf.
 *
 * <p>Written in Java on purpose: the generated parser relies on static-method
 * hiding, which Kotlin cannot express.
 */
public class SlangParserUtil extends GeneratedParserUtilBase {

  /** Whether the current tokens are an adjacent {@code > >} / {@code > >=} pair forming {@code token}. */
  static boolean atComposite(PsiBuilder b, IElementType token) {
    IElementType second;
    if (token == SHR) second = GT;
    else if (token == SHREQ) second = GE;
    else return false;
    // getTokenType() skips whitespace, so rawLookup(1) is the very next token:
    // equal to `second` only when there was no whitespace in between, i.e. the
    // source had `>>` / `>>=`.
    return b.getTokenType() == GT && b.rawLookup(1) == second;
  }

  private static boolean atAnyComposite(PsiBuilder b, IElementType... tokens) {
    for (IElementType t : tokens) {
      if (atComposite(b, t)) return true;
    }
    return false;
  }

  private static void glue(PsiBuilder b, IElementType token) {
    PsiBuilder.Marker m = b.mark();
    b.advanceLexer();
    b.advanceLexer();
    m.collapse(token);
  }

  // ----- hidden GeneratedParserUtilBase methods ------------------------------

  public static boolean consumeToken(PsiBuilder b, IElementType token) {
    if (atComposite(b, token)) {
      glue(b, token);
      return true;
    }
    return GeneratedParserUtilBase.consumeToken(b, token);
  }

  public static boolean consumeTokenFast(PsiBuilder b, IElementType token) {
    if (atComposite(b, token)) {
      glue(b, token);
      return true;
    }
    return GeneratedParserUtilBase.consumeTokenFast(b, token);
  }

  public static boolean consumeTokenSmart(PsiBuilder b, IElementType token) {
    if (atComposite(b, token)) {
      glue(b, token);
      return true;
    }
    return GeneratedParserUtilBase.consumeTokenSmart(b, token);
  }

  public static boolean nextTokenIs(PsiBuilder b, IElementType token) {
    return atComposite(b, token) || GeneratedParserUtilBase.nextTokenIs(b, token);
  }

  public static boolean nextTokenIs(PsiBuilder b, String frameName, IElementType... tokens) {
    return atAnyComposite(b, tokens) || GeneratedParserUtilBase.nextTokenIs(b, frameName, tokens);
  }

  public static boolean nextTokenIsFast(PsiBuilder b, IElementType token) {
    return atComposite(b, token) || GeneratedParserUtilBase.nextTokenIsFast(b, token);
  }

  public static boolean nextTokenIsFast(PsiBuilder b, IElementType... tokens) {
    return atAnyComposite(b, tokens) || GeneratedParserUtilBase.nextTokenIsFast(b, tokens);
  }

  public static boolean nextTokenIsSmart(PsiBuilder b, IElementType token) {
    return atComposite(b, token) || GeneratedParserUtilBase.nextTokenIsSmart(b, token);
  }

  public static boolean nextTokenIsSmart(PsiBuilder b, IElementType... tokens) {
    return atAnyComposite(b, tokens) || GeneratedParserUtilBase.nextTokenIsSmart(b, tokens);
  }
}
