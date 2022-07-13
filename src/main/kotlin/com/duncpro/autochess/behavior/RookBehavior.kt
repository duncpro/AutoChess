package com.duncpro.autochess.behavior

import com.duncpro.autochess.*

object RookBehavior: PieceBehavior {
    override fun invoke(origin: Cell, ownColor: Color, board: Position): Set<SynchronousAction> {
        val moves = LinkedHashSet<SynchronousAction>()

        val towardsKingside = castLine(origin.file, 7)
            .map { file -> Cell(file, origin.rank) }

        convertStepSequenceToMoves(origin, ownColor, board, towardsKingside)
            .let(moves::addAll)

        val towardsQueenside = castLine(origin.file, 0)
            .map { file -> Cell(file, origin.rank) }

        convertStepSequenceToMoves(origin, ownColor, board, towardsQueenside)
            .let(moves::addAll)

        val towardsBlack = castLine(origin.rank, 7)
            .map { rank -> Cell(origin.file, rank) }

        convertStepSequenceToMoves(origin, ownColor, board, towardsBlack)
            .let(moves::addAll)

        val towardsWhite = castLine(origin.rank, 0)
            .map { rank -> Cell(origin.file, rank) }

        convertStepSequenceToMoves(origin, ownColor, board, towardsWhite)
            .let(moves::addAll)

        return moves
    }

    private fun castLine(from: Int, to: Int): List<Int> {
        if (from == to) return emptyList()
        if (from < to) {
            val steps: MutableList<Int> = ArrayList(7)
            for (i in (from + 1)..to) {
                steps.add(i)
            }
            return steps
        }
        @Suppress("KotlinConstantConditions")
        if (from > to) {
            val steps: MutableList<Int> = ArrayList(7)
            for (i in (from - 1) downTo to) {
                steps.add(i)
            }
            return steps
        }
        throw AssertionError()
    }
}

