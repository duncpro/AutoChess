package com.duncpro.autochess

import com.duncpro.autochess.Color.*
import com.duncpro.autochess.PieceType.*
import com.duncpro.autochess.TranspositionTable.Hit
import com.duncpro.autochess.TranspositionTable.Miss
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

sealed interface TransposablePiece {
    val color: Color
}

data class TransposablePawn(override val color: Color, val isEnpassantVulnerable: Boolean): TransposablePiece
@JvmInline value class TransposableKnight(override val color: Color): TransposablePiece
@JvmInline value class TransposableRook(override val color: Color): TransposablePiece
@JvmInline value class TransposableBishop(override val color: Color): TransposablePiece
@JvmInline value class TransposableQueen(override val color: Color): TransposablePiece
@JvmInline value class TransposableKing(override val color: Color): TransposablePiece

data class TransposablePosition(val pieceArrangement: Map<Cell, TransposablePiece>, val whosTurn: Color)

fun TransposablePosition(position: Position): TransposablePosition {
    val pieceArrangement = HashMap<Cell, TransposablePiece>(position.filledCells.size)

    for (piece in position.pieces) {
        val transposablePiece =
            when (piece.aesthetic.type) {
                KING -> TransposableKing(piece.aesthetic.color)
                QUEEN -> TransposableQueen(piece.aesthetic.color)
                ROOK -> TransposableRook(piece.aesthetic.color)
                KNIGHT -> TransposableKnight(piece.aesthetic.color)
                BISHOP -> TransposableBishop(piece.aesthetic.color)
                PAWN -> {
                    val enpassantVulnerableRank = when (piece.aesthetic.color) {
                        WHITE -> 3
                        BLACK -> 4
                    }
                    val isEnpassantVulnerable = piece.placed.atMove == position.nextMove - 1 &&
                            piece.location.rank == enpassantVulnerableRank
                    TransposablePawn(piece.aesthetic.color, isEnpassantVulnerable)
                }
            }

        pieceArrangement[piece.location] = transposablePiece
    }

    return TransposablePosition(pieceArrangement, position.whoseTurn)
}

class HashMapTranspositionTable: TranspositionTable {
    private val hashMap: ConcurrentMap<Key, TranspositionTable.CachedScore> = ConcurrentHashMap()

    override operator fun get(position: Position, depth: Int): TranspositionTable.Result {
        val key = Key(TransposablePosition(position), depth)
        return hashMap[key]?.let(::Hit) ?: Miss
    }

    override fun set(position: Position, depth: Int, score: Int, isTreeComplete: Boolean) {
        val key = Key(TransposablePosition(position), depth)
        hashMap[key] = TranspositionTable.CachedScore(score, isTreeComplete)
    }

    data class Key(val position: TransposablePosition, val depth: Int)
}

class VoidTranspositionTable: TranspositionTable {
    override fun get(position: Position, depth: Int): TranspositionTable.Result = Miss

    override fun set(position: Position, depth: Int, score: Int, isTreeComplete: Boolean) {}
}

interface TranspositionTable {
    operator fun get(position: Position, depth: Int): Result

    fun set(position: Position, depth: Int, score: Int, isTreeComplete: Boolean)

    data class CachedScore(val score: Int, val isTreeComplete: Boolean)

    sealed interface Result
    @JvmInline
    value class Hit(val cachedScore: CachedScore): Result
    object Miss: Result
}