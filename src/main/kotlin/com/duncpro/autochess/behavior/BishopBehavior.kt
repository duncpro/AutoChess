package com.duncpro.autochess.behavior

import com.duncpro.autochess.*

object BishopBehavior: PieceBehavior {
    override fun invoke(origin: Cell, ownColor: Color, board: Position): Set<EffectfulMove> {
        val moves = LinkedHashSet<EffectfulMove>()

        val blackKingside = castDiagonal(origin, Direction.HIGHER, Direction.HIGHER)

        convertStepSequenceToMoves(origin, ownColor, board, blackKingside)
            .let(moves::addAll)

        val blackQueenside = castDiagonal(origin, Direction.LOWER, Direction.HIGHER)

        convertStepSequenceToMoves(origin, ownColor, board, blackQueenside)
            .let(moves::addAll)

        val whiteKingside = castDiagonal(origin, Direction.HIGHER, Direction.LOWER)

        convertStepSequenceToMoves(origin, ownColor, board, whiteKingside)
            .let(moves::addAll)

        val whiteQueenside = castDiagonal(origin, Direction.LOWER, Direction.LOWER)

        convertStepSequenceToMoves(origin, ownColor, board, whiteQueenside)
            .let(moves::addAll)

        return moves
    }

    private fun castDiagonal(origin: Cell, fileDirection: Direction, rankDirection: Direction): List<Cell> {
        var position: Cell? = origin
        val steps = mutableListOf<Cell>()

        while (position != null) {
            val nextFile = position.file + fileDirection.signum
            val nextRank = position.rank + rankDirection.signum
            position = cellOrNull(nextFile, nextRank)
            position?.let(steps::add)
        }

        return steps
    }
}