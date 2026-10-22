package org.zell.idea.stubs

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import org.zell.idea.psi.ZellNamedElement

/**
 * Navigate | Symbol over top-level Zell declarations, from [ZellDeclNameIndex].
 * (zell-lsp workspace symbols complement this with members.)
 */
class ZellGotoSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        StubIndex.getInstance().processAllKeys(ZellDeclNameIndex.KEY, processor, scope, filter)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters,
    ) {
        val elements = StubIndex.getElements(
            ZellDeclNameIndex.KEY,
            name,
            parameters.project,
            parameters.searchScope,
            parameters.idFilter,
            ZellNamedElement::class.java,
        )
        for (element in elements) {
            if (!processor.process(element)) return
        }
    }
}
