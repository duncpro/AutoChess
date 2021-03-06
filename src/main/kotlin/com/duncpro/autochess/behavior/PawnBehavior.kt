package com.duncpro.autochess.behavior

import com.duncpro.autochess.*
import com.duncpro.autochess.Color.*
import java.util.stream.Collectors
import java.util.stream.Stream

object PawnBehavior: PieceBehavior {
    override fun invoke(origin: Cell, ownColor: Color, board: Position): Set<SynchronousAction> {
        val moves = LinkedHashSet<SynchronousAction>(4)
        doubleStart(origin, ownColor, board)?.let(moves::add)
        advance(origin, ownColor, board)?.let(moves::add)
        take(origin, ownColor, board)?.let(moves::addAll)
        enpassant(origin, ownColor, board)?.let(moves::addAll)
        return moves
    }

    private fun doubleStart(origin: Cell, ownColor: Color, board: Position): SynchronousAction? {
        val initialRank = when (ownColor) {
            BLACK -> 6
            WHITE -> 1
        }

        if (origin.rank != initialRank) return null

        val destination = when (ownColor) {
            BLACK -> Cell(origin.file, origin.rank - 2)
            WHITE -> Cell(origin.file, origin.rank + 2)
        }

        if (board[destination] != null) return null

        val hoppedOver = when (ownColor) {
            WHITE -> Cell(origin.file, 2)
            BLACK -> Cell(origin.file, 5)
        }

        if (board[hoppedOver] != null) return null

        return SynchronousAction(Translate(origin, destination))
    }

    private fun wrapWithQueenSpawnIfNecessary(destination: Cell, ownColor: Color, vararg prefixActions: Action): SynchronousAction {
        val queenSpawnRank = when (ownColor) {
            BLACK -> 0
            WHITE -> 7
        }

        val spawnsQueen = queenSpawnRank == destination.rank

        return if (spawnsQueen) {
            SynchronousAction(*prefixActions, Spawn(AestheticPiece(PieceType.QUEEN, ownColor), destination))
        } else {
            SynchronousAction(*prefixActions)
        }
    }

    private fun advance(origin: Cell, ownColor: Color, board: Position): SynchronousAction? {
        val destination = origin.towardsColor(ownColor.opposite)!!

        if (board[destination] != null) return null

        return wrapWithQueenSpawnIfNecessary(destination, ownColor, Translate(origin, destination))
    }

    private fun take(origin: Cell, ownColor: Color, board: Position): Set<SynchronousAction>? {
        val kingside = origin.towardsColor(ownColor.opposite)!!.towardsKingside()
        val queenside =  origin.towardsColor(ownColor.opposite)!!.towardsQueenside()
        return Stream.of(kingside, queenside)
            .filterNotNull()
            .filter { target -> board[target]?.aesthetic?.color == ownColor.opposite }
            .map { target -> wrapWithQueenSpawnIfNecessary(target, ownColor, Take(target), Translate(origin, target)) }
            .collect(Collectors.toSet())
    }

    private fun enpassant(origin: Cell, ownColor: Color, board: Position): List<SynchronousAction>? {
        val enpassantOriginRank = when (ownColor) {
            WHITE -> 4
            BLACK -> 3
        }

        if (origin.rank != enpassantOriginRank) return null

        return sequenceOf(-1, 1)
            .mapNotNull { fileOffset -> /* target = */ cellOrNull(origin.file + fileOffset, origin.towardsColor(ownColor.opposite)!!.rank) }
            .associateWith { target -> /* destination = */ target.towardsColor(ownColor)!! }
            .filter { (target, _) -> board[target]?.aesthetic == AestheticPiece(PieceType.PAWN, ownColor.opposite) }
            .filter { (target, _) -> board[target]!!.atMove == board.nextMove - 1 }
            .map { (target, destination) -> SynchronousAction(Take(target), Translate(origin, destination)) }
            .toList()
    }
}