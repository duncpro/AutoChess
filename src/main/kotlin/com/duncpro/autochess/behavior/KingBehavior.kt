package com.duncpro.autochess.behavior

import com.duncpro.autochess.*

object KingBehavior: PieceBehavior {
    override fun invoke(origin: Cell, ownColor: Color, board: Position): Set<SynchronousAction> {
        // (-1,-1)
        // (-1, 0)
        // (-1, 1)
        // (0, -1)
        // (0,  1)
        // (1, -1)
        // (1,  0)
        // (1,  1)
        val moves = LinkedHashSet<SynchronousAction>(8)
        for (fileOffset in -1..1) {
            for (rankOffset in -1..1) {
                if (fileOffset == 0 && rankOffset == 0) continue
                val cell = cellOrNull(origin.file + fileOffset, origin.rank + rankOffset) ?: continue
                convertStepToMove(origin, ownColor, board, cell)?.let(moves::add)
            }
        }
        return moves
    }
}