package org.zell.idea.psi

import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * A named declaration: `struct`, `interface`, `enum`, `func` (also methods)
 * and `const`. Implemented by the generated PSI through the grammar header
 * (`implements`/`mixin`, see tools/sync-zell.sh) and
 * [org.zell.idea.psi.impl.ZellDeclMixin]. Names here are syntactic only;
 * resolve and rename come from zell-lsp.
 */
interface ZellNamedElement : PsiNameIdentifierOwner, NavigationItem
