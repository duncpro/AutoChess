package com.duncpro.autochess.behavior

import com.duncpro.autochess.*

object QueenBehavior: PieceBehavior {
    override fun invoke(origin: Cell, ownColor: Color, board: Position): Set<SynchronousEffect> {
        return RookBehavior(origin, ownColor, board) + BishopBehavior(origin, ownColor, board)
    }
}