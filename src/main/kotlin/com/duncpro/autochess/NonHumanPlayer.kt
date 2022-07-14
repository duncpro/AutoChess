package com.duncpro.autochess

import com.duncpro.autochess.behavior.SynchronousAction
import java.time.Instant

class NonHumanPlayer(
    private val positionCache: PositionCache,
) {
    private var principleVariationPair: Pair<SynchronousAction, DfsSearchResult>? = null

    private fun makeList(): List<SynchronousAction> {
        if (principleVariationPair == null) return emptyList()
        val list = ArrayList<SynchronousAction>()
        var next: Pair<SynchronousAction, DfsSearchResult>? = principleVariationPair
        while (next != null) {
            list.add(next.first)
            next = next.second.principleMove
        }
        return list
    }

    fun takeTurn(position: Position): SynchronousAction {
        val result = search(position, 4, Instant.now().plusSeconds(15), positionCache,
            PrincipleVariationMoveSortingAlgorithm(makeList()))
        this.principleVariationPair = result.deepestResult.principleMove
        println("Depth: ${result.depth}")
        return this.principleVariationPair!!.first
    }
}