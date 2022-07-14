package com.duncpro.autochess.behavior

import com.duncpro.autochess.Position
import com.duncpro.autochess.TransposablePosition

object DrawBehavior {
    fun getClaimDrawMoveIfExistsAtPosition(position: Position): SynchronousAction? {
        val positionHistory = HashMap<TransposablePosition, MutableSet<Position>>()

        var nextPositionToCheck: Position? = position
        while (nextPositionToCheck != null) {
            val duplicates = positionHistory.computeIfAbsent(nextPositionToCheck.transposable) { HashSet() }
            duplicates.add(nextPositionToCheck)
            nextPositionToCheck = nextPositionToCheck.previous

            if (duplicates.size >= 3) return SynchronousAction(ClaimDrawAction(duplicates))
        }
        return null
    }
}