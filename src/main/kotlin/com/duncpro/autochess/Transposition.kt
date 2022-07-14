package com.duncpro.autochess

import com.duncpro.autochess.Color.*
import com.duncpro.autochess.PieceType.*
import com.duncpro.autochess.PositionCache.Hit
import com.duncpro.autochess.PositionCache.Miss
import com.duncpro.autochess.behavior.KingsideCastlingScheme
import com.duncpro.autochess.behavior.QueensideCastlingScheme
import com.duncpro.autochess.behavior.SynchronousAction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

open class TransposablePiece(val aesthetic: AestheticPiece) {
    override fun equals(other: Any?) = implementClassEquality<TransposablePiece>(other) { otherPiece ->
        if (otherPiece.aesthetic != this.aesthetic) return@implementClassEquality false
        return@implementClassEquality true
    }

    override fun hashCode(): Int {
        return aesthetic.hashCode()
    }
}

class TransposablePawn(color: Color, val isEnpassantVulnerable: Boolean):
    TransposablePiece(AestheticPiece(PAWN, color)) {
    override fun equals(other: Any?) = implementClassEquality<TransposablePawn>(other) { otherPawn ->
        if (otherPawn.aesthetic.color != this.aesthetic.color) return@implementClassEquality false
        if (otherPawn.isEnpassantVulnerable != this.isEnpassantVulnerable) return@implementClassEquality false
        return@implementClassEquality true
    }

    override fun hashCode(): Int {
        return 31 * aesthetic.hashCode() + isEnpassantVulnerable.hashCode()
    }
}

data class PlayerCastlingRights(val canCastleQueenside: Boolean, val canCastleKingside: Boolean)

data class TransposablePosition(
    val pieceArrangement: Map<Cell, TransposablePiece>,
    val whosTurn: Color,
    val castlingRights: Map<Color, PlayerCastlingRights>
)

fun TransposablePosition(position: Position): TransposablePosition {
    val pieceArrangement = HashMap<Cell, TransposablePiece>(position.filledCells.size)

    for (piece in position.pieces) {
        val transposablePiece =
            when (piece.aesthetic.type) {
                PAWN -> {
                    val enpassantVulnerableRank = when (piece.aesthetic.color) {
                        WHITE -> 3
                        BLACK -> 4
                    }
                    val isEnpassantVulnerable = piece.placed.atMove == position.nextMove - 1 &&
                            piece.location.rank == enpassantVulnerableRank
                    TransposablePawn(piece.aesthetic.color, isEnpassantVulnerable)
                }
                else -> TransposablePiece(piece.aesthetic)
            }

        pieceArrangement[piece.location] = transposablePiece
    }

    val castlingRights = HashMap<Color, PlayerCastlingRights>()
    for (color in Color.values()) {
        castlingRights[color] = PlayerCastlingRights(
            canCastleKingside = KingsideCastlingScheme.areNecessaryPiecesIntact(color, position),
            canCastleQueenside = QueensideCastlingScheme.areNecessaryPiecesIntact(color, position)
        )
    }

    return TransposablePosition(pieceArrangement, position.whoseTurn, castlingRights)
}

class HashMapPositionCache: PositionCache {
    private val hashMap: ConcurrentMap<Key, PositionCache.CacheEntry> = ConcurrentHashMap()

    override operator fun get(position: Position, depth: Int): PositionCache.Result {
        val key = Key(position.transposable, depth)
        return hashMap[key]?.let(::Hit) ?: Miss
    }

    override fun set(position: Position, depth: Int, cacheEntry: PositionCache.CacheEntry) {
        val key = Key(position.transposable, depth)
        hashMap[key] = cacheEntry
    }

    data class Key(val position: TransposablePosition, val depth: Int)
}

class VoidPositionCache: PositionCache {
    override fun get(position: Position, depth: Int): PositionCache.Result = Miss

    override fun set(position: Position, depth: Int, cacheEntry: PositionCache.CacheEntry) {}
}

interface PositionCache {
    operator fun get(position: Position, depth: Int): Result

    fun set(position: Position, depth: Int, cacheEntry: CacheEntry)

    data class CacheEntry(val score: Int, val isTreeComplete: Boolean, val principleMove: Pair<SynchronousAction, DfsSearchResult>)

    sealed interface Result
    @JvmInline value class Hit(val cacheEntry: CacheEntry): Result
    object Miss: Result
}