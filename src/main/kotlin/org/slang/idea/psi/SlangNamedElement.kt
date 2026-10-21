package org.slang.idea.psi

import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * A named declaration: `struct`, `interface`, `enum`, `func` (also methods)
 * and `const`. Implemented by the generated PSI through the grammar header
 * (`implements`/`mixin`, see tools/sync-slang.sh) and
 * [org.slang.idea.psi.impl.SlangDeclMixin]. Names here are syntactic only;
 * resolve and rename come from slang-lsp.
 */
interface SlangNamedElement : PsiNameIdentifierOwner, NavigationItem
