package com.duncpro.autochess.behavior

import com.duncpro.autochess.*
import com.duncpro.autochess.PieceType.*

sealed interface Action

data class Spawn(val piece: AestheticPiece, val point: Cell): Action

data class Take(val target: Cell): Action

data class Translate(val origin: Cell, val destination: Cell): Action

data class ClaimDrawAction(val evidence: Set<Position>): Action {
    init {
        if (evidence.size < 3) throw IllegalArgumentException()
    }
}

data class SynchronousAction(val actions: List<Action>) {
    constructor(vararg actions: Action) : this(actions.toList())
}

typealias PieceBehavior = (origin: Cell, ownColor: Color, board: Position) -> Set<SynchronousAction>

/**
 * Some standard pieces behave similarly in terms of their effects.
 * Queens, Bishops, and Rooks move along contiguous lines or diagonals where the path
 * is halted short if another piece is encountered. A piece of the same color cannot be passed,
 * and a piece of a different color can be taken, but the path must end at that square.
 *
 * This function converts so-called "step sequences", that is, the rays produced by these pieces,
 * to a set of [SynchronousAction]s.
 *
 * The given [origin] described the [Cell] which the piece to which this step sequence belongs originally inhabited.
 * The [stepSequence] is a list of [Cell]s which fall along the piece's path.
 */
fun convertStepSequenceToMoves(origin: Cell, ownColor: Color, board: Position, stepSequence: List<Cell>): Set<SynchronousAction> {
    val moves = mutableSetOf<SynchronousAction>()

    for (step in stepSequence) {
        val currentOccupant = board[step]

        if (currentOccupant == null) {
            moves.add(SynchronousAction(Translate(origin, step)))
            continue
        }

        // A piece can not jump over a piece of its own color, nor can it take another
        // piece of its own color. Therefor, the step sequence will end short.
        if (currentOccupant.aesthetic.color == ownColor) break

        // Once a piece encounters a piece of an opposing color, the path must end.
        if (currentOccupant.aesthetic.color == ownColor.opposite) {
            moves.add(SynchronousAction(
                Take(step),
                Translate(origin, step)
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
