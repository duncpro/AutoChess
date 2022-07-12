package com.duncpro.autochess.behavior

import com.duncpro.autochess.*
import com.duncpro.autochess.PieceType.*

/**
 * An [EffectfulMove] is composed of sequence of [Effect]s which modify the
 * board. For example, en passant results in two effects, a [Take] and a [Translation].
 * And a pawn making it to the opposite side of the board results in potentially a [Take], [Translation], and [Spawn].
 * No [Effect] implies any other. Therefore, it is necessary to be verbose when creating the effect list for a move.
 */
sealed interface Effect

/**
 * Spawns the given [piece] at the given [point] on the board, replacing the piece which inhabits the cell currently (if any).
 */
data class Spawn(val piece: AestheticPiece, val point: Cell): Effect

/**
 * This effect causes the piece at the given [target] to be removed from the board.
 */
data class Take(val target: Cell): Effect

/**
 * This effect causes the piece at the given [origin] to be moved to the given [destination].
 */
data class Translation(val origin: Cell, val destination: Cell): Effect

/**
 * An [EffectfulMove] is composed of sequence of [Effect]s which modify the
 * board. For example, en passant results in two effects, a [Take] and a [Translation].
 * And a pawn making it to the opposite side of the board results in a [Spawn].
 * The effects are applied to the board in the order in which the appear in the [effects] list.
 */
data class EffectfulMove(val effects: List<Effect>) {
    constructor(vararg effects: Effect) : this(effects.toList())
}

typealias PieceBehavior = (origin: Cell, ownColor: Color, board: Position) -> Set<EffectfulMove>

/**
 * Some standard pieces behave similarly in terms of their effects.
 * Queens, Bishops, and Rooks move along contiguous lines or diagonals where the path
 * is halted short if another piece is encountered. A piece of the same color cannot be passed,
 * and a piece of a different color can be taken, but the path must end at that square.
 *
 * This function converts so-called "step sequences", that is, the rays produced by these pieces,
 * to a set of [EffectfulMove]s.
 *
 * The given [origin] described the [Cell] which the piece to which this step sequence belongs originally inhabited.
 * The [stepSequence] is a list of [Cell]s which fall along the piece's path.
 */
fun convertStepSequenceToMoves(origin: Cell, ownColor: Color, board: Position, stepSequence: List<Cell>): Set<EffectfulMove> {
    val moves = mutableSetOf<EffectfulMove>()

    for (step in stepSequence) {
        val currentOccupant = board[step]

        if (currentOccupant == null) {
            moves.add(EffectfulMove(Translation(origin, step)))
            continue
        }

        // A piece can not jump over a piece of its own color, nor can it take another
        // piece of its own color. Therefor, the step sequence will end short.
        if (currentOccupant.aesthetic.color == ownColor) break

        // Once a piece encounters a piece of an opposing color, the path must end.
        if (currentOccupant.aesthetic.color == ownColor.opposite) {
            moves.add(EffectfulMove(
                Take(step),
                Translation(origin, step)
            ))
            break
        }
    }

    return moves
}

fun convertStepToMove(origin: Cell, ownColor: Color, board: Position, step: Cell) =
    convertStepSequenceToMoves(origin, ownColor, board, listOf(step))
        .firstOrNull()

val PieceType.behavior: PieceBehavior get() = when (this) {
    KING -> KingBehavior
    QUEEN -> QueenBehavior
    ROOK -> RookBehavior
    KNIGHT -> KnightBehavior
    BISHOP -> BishopBehavior
    PAWN -> PawnBehavior
}
