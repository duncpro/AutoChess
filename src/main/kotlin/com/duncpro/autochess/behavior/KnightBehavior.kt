package com.duncpro.autochess.behavior

import com.duncpro.autochess.*
import com.duncpro.autochess.BoardDimension.*

object KnightBehavior: PieceBehavior {
    override fun invoke(origin: Cell, ownColor: Color, board: Position): Set<SynchronousAction> {
        val moves = LinkedHashSet<SynchronousAction>(8)

        castL(origin, FILE, Direction.HIGHER, Direction.HIGHER)
            ?.let { convertStepToMove(origin, ownColor, board, it) }
            ?.let(moves::add)

        castL(origin, FILE, Direction.HIGHER, Direction.LOWER)
            ?.let { convertStepToMove(origin, ownColor, board, it) }
            ?.let(moves::add)

        castL(origin, FILE, Direction.LOWER, Direction.LOWER)
            ?.let { convertStepToMove(origin, ownColor, board, it) }
            ?.let(moves::add)

        castL(origin, FILE, Direction.LOWER, Direction.HIGHER)
            ?.let { convertStepToMove(origin, ownColor, board, it) }
            ?.let(moves::add)

        castL(origin, RANK, Direction.HIGHER, Direction.HIGHER)
            ?.let { convertStepToMove(origin, ownColor, board, it) }
            ?.let(moves::add)

        castL(origin, RANK, Direction.HIGHER, Direction.LOWER)
            ?.let { convertStepToMove(origin, ownColor, board, it) }
            ?.let(moves::add)

        castL(origin, RANK, Direction.LOWER, Direction.LOWER)
            ?.let { convertStepToMove(origin, ownColor, board, it) }
            ?.let(moves::add)

        castL(origin, RANK, Direction.LOWER, Direction.HIGHER)
            ?.let { convertStepToMove(origin, ownColor, board, it) }
            ?.let(moves::add)

        return moves
    }

    private fun castL(origin: Cell, lengthDimension: BoardDimension, lengthDirection: Direction,
                      widthDirection: Direction): Cell? {

        val y = origin[lengthDimension] + (2 * lengthDirection.signum)
        val x = origin[lengthDimension.opposite] + widthDirection.signum

        return when (lengthDimension) {
            FILE -> cellOrNull(y, x)
            RANK -> cellOrNull(x, y)
        }
    }

}