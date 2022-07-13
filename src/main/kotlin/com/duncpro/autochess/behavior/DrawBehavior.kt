package com.duncpro.autochess.behavior

import com.duncpro.autochess.Position
import com.duncpro.autochess.TransposablePosition

object DrawBehavior {
    fun canClaimDraw(position: Position): Boolean {
        val uniquePositions = HashSet<TransposablePosition>()
        var countedPositions = 0

        var nextPositionToCheck: Position? = position
        while (nextPositionToCheck != null) {
            uniquePositions.add(nextPositionToCheck.transposable)
            countedPositions++
            nextPositionToCheck = position.previous

            if (countedPositions - uniquePositions.size >= 3) {
                return true
            }
        }
        return false
    }


}