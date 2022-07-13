package com.duncpro.autochess

import com.duncpro.autochess.behavior.SynchronousEffect
import java.time.Instant
import kotlin.math.max

val PieceType.value: Int get() = when (this) {
    PieceType.ROOK -> 5
    PieceType.KNIGHT -> 3
    PieceType.BISHOP -> 3
    PieceType.QUEEN -> 9
    PieceType.KING -> 0
    PieceType.PAWN -> 1
}

fun materialDifference(position: Position): Int {
    var score = 0
    for (cell in position.filledCells) {
        val occupant = position[cell] ?: continue
        val value = occupant.aesthetic.type.value
        if (occupant.aesthetic.color == position.whoseTurn) score += value
        if (occupant.aesthetic.color == position.whoseTurn.opposite) score -= value
    }

    return score
}

fun heuristicScore(position: Position): Int {
    if (position.isCheckmate) return (Int.MIN_VALUE + 1)
    return materialDifference(position)
}

class IterativeDeepeningSearchResult(val deepestResult: DfsSearchResult, val finalDepth: Int)

/**
 * Performs an iteratively deepening search from the given position until the given [deadline] has been reached.
 * A search of depth [minDepth] will be carried out regardless of any time-constraints. The deadline will
 * only be honored after the min-depth search is complete.
 */
fun search(fromPosition: Position,  minDepth: Int, deadline: Instant, transpositionTable: TranspositionTable): IterativeDeepeningSearchResult {
    // Before embarking on any time-constrained searches, at least perform a search of minDepth,
    var dfsResult = searchDeep(fromPosition, minDepth, transpositionTable)!!

    var depth = minDepth
    while (deadline.isAfter(Instant.now())) {
        // We have achieved a search depth large enough to compute every possible outcome of the game.
        if (dfsResult.isTreeComplete) {
            break
        }

        // If the returned tree is null then the deadline has been reached, therefore break and use the result
        // computed by the last iteration.
        dfsResult = searchDeep(fromPosition, depth + 1, transpositionTable, deadline) ?: break
        depth++

        // If children is null then depth is equal to zero therefore no children were scored. See searchDeep
        // function declaration for details. In such a case continue looping, this time search 1 move deeper.
        if (dfsResult.children == null) continue

        // The given position represents the terminus of the game.
        if (dfsResult.children!!.isEmpty()) {
            assert(depth == 1)
            break
        }
    }

    return IterativeDeepeningSearchResult(dfsResult, depth)
}

class DfsSearchResult(
    val children: Map<SynchronousEffect, Int>?,
    val score: Int,
    val isTreeComplete: Boolean
)

/**
 * Searches the position tree with depth at most [depthLeft] whose root node is [fromPosition].
 * The position tree is a colored tree where each level is the opposite color of the previous level.
 * For example if it is white's move at [fromPosition], then the root node is white, and its immediate children
 * are black, and their immediate children are white, etc. Each vertex in a position tree has a score which
 * describes how advantageous that move is for the player whose color matches the color of the level which
 * the vertex inhabits.
 *
 * The search will continue until the desired depth has been reached, the entire game has been searched, or
 * the deadline has passed. If the deadline was reached before the search completed the desired depth then null
 * is returned. If the search is not time constrained then the deadline parameter can be set to null.
 * Note that for a depth of zero this function will simply return a [DfsSearchResult] containing the score
 * of the position but without any child moves ([DfsSearchResult.children] = null). Such a result is typically only
 * useful in the context of a deeper search.
 *
 * @param parentScore The minimum score the maximized player is assured of. If no alpha is known, assume that the minimum score
 *  the maximized player is assured of is negative infinity. In more literal terms, the maximized player is not assured
 *  of achieving any good score. Intended for recursive use internally.
 *
 * @param beta The maximum score the minimized player is assured of. If no beta is known, assume that the maximum score the
 *  minimized player is assured of is infinity. In more literal terms, the minimized player is assured of winning.
 *  Intended for recursive use internally.
 */
fun searchDeep(fromPosition: Position,
               depthLeft: Int,
               transpositionTable: TranspositionTable,
               deadline: Instant? = null,
               parentScore: Int = Int.MIN_VALUE + 1,
               beta: Int = Int.MAX_VALUE
): DfsSearchResult? {
    if (deadline?.isBefore(Instant.now()) == true) return null
    if (fromPosition.legalMoves.isEmpty()) return DfsSearchResult(emptyMap(), heuristicScore(fromPosition), true)

    // If the maximum depth has been reached and the game is not over, simply perform a heuristic score evaluation.
    if (depthLeft <= 0) return DfsSearchResult(null, heuristicScore(fromPosition), false)

    var thisScore: Int = parentScore
    var isTreeComplete = true
    val children = mutableMapOf<SynchronousEffect, Int>()

    // Compute all possible moves we can make
    for (move in fromPosition.legalMoves) {
        // Now create a new position where we've made that move
        val childPosition = fromPosition.branch(move)

        // Now compute all possible moves our opponent can make.
        // While searchDown may not actually reach the real terminus of the game, it is assumed to.
        // Therefore, the scores returned by this function should be treated as exact values, not approximations.

        val childScore: Int

        val cachedResult = transpositionTable[childPosition, depthLeft - 1]
        if (cachedResult is TranspositionTable.Hit) {
            childScore = cachedResult.cachedScore.score
            isTreeComplete = isTreeComplete && cachedResult.cachedScore.isTreeComplete
        } else {
            val dfsResult = searchDeep(
                fromPosition = childPosition,
                depthLeft = depthLeft - 1,
                transpositionTable,
                deadline,
                // This search is from the perspective of our opponent. Therefore, the opponent is now the maximized
                // player, while we are the minimized player.
                // The pre-assigned value is in terms of the move's advantage to us, and a move that is advantageous to us is
                // disadvantageous to our opponent. Therefore, the sign is changed so that the quantity properly
                // describes the advantageous-ness/disadvantageous-ness from the perspective of our opponent.
                parentScore = beta * -1,

                // thisScore is in terms of the move's advantage to us, and a move that is advantageous to us is
                // disadvantageous to our opponent. Therefore, the sign of thisScore is changed, so that it properly
                // describes the advantageous-ness/disadvantageous-ness from the perspective of our opponent.
                beta = thisScore * -1,
            ) ?: return null // if null then the deadline has been reached, the search terminated prematurely.
            childScore = dfsResult.score
            isTreeComplete = isTreeComplete && dfsResult.isTreeComplete
        }

        children[move] = childScore * -1

        // The score of this move is equal to the opposite of the best move our opponent can make after this move.
        thisScore = max(thisScore, childScore * -1)

        // alpha > beta
        // minimum score which the maximized player is assured of > maximum score with the minimized player is assured of
        // alpha being the minimum score for the maximized player
        // beta being the maximum score for the minimized player
        if (thisScore >= beta) break
    }

    transpositionTable.set(fromPosition, depthLeft, thisScore, isTreeComplete)
    return DfsSearchResult(children, thisScore, isTreeComplete)
}