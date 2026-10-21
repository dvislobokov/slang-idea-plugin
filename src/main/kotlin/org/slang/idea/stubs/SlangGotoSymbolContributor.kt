package org.slang.idea.stubs

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import org.slang.idea.psi.SlangNamedElement

/**
 * Navigate | Symbol over top-level Slang declarations, from [SlangDeclNameIndex].
 * (slang-lsp workspace symbols complement this with members.)
 */
class SlangGotoSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        StubIndex.getInstance().processAllKeys(SlangDeclNameIndex.KEY, processor, scope, filter)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters,
    ) {
        val elements = StubIndex.getElements(
            SlangDeclNameIndex.KEY,
            name,
            parameters.project,
            parameters.searchScope,
            parameters.idFilter,
            SlangNamedElement::class.java,
        )
        for (element in elements) {
            if (!processor.process(element)) return
        }
    }
}
