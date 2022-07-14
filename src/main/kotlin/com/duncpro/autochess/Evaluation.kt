package com.duncpro.autochess

import com.duncpro.autochess.behavior.SynchronousAction
import java.time.Instant
import kotlin.math.max

val PieceType.value: Int get() = when (this) {
    PieceType.ROOK -> 10
    PieceType.KNIGHT -> 6
    PieceType.BISHOP -> 6
    PieceType.QUEEN -> 18
    PieceType.KING -> 0
    PieceType.PAWN -> 2
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

    val md = materialDifference(position)

    // A draw is considered barely advantageous if we are loosing in terms of material difference.
    // Any move which results in a material difference in our favor is better than a draw.
    if (position.isDraw && md < 0) return 1

    // From our perspective a draw is not advantageous if the material difference is even, or we are up by at least 1.
    // Still a draw is better than being down a piece.
    if (position.isDraw && md >= 0) return -1
    return md
}

class IterativeDeepeningSearchResult(
    val deepestResult: DfsSearchResult,
    val depth: Int
)

/**
 * Performs an iteratively deepening search from the given position until the given [deadline] has been reached.
 * A search of depth [minDepth] will be carried out regardless of any time-constraints. The deadline will
 * only be honored after the min-depth search is complete.
 */
fun search(fromPosition: Position, minDepth: Int, deadline: Instant, positionCache: PositionCache,
           sortingAlgorithm: MoveSortingAlgorithm): IterativeDeepeningSearchResult {
    // Before embarking on any time-constrained searches, at least perform a search of minDepth
    var dfsResult = searchDeep(fromPosition, minDepth, positionCache, null, sortingAlgorithm)!!

    var depth = minDepth
    while (deadline.isAfter(Instant.now())) {
        // We have achieved a search depth large enough to compute every possible outcome of the game.
        if (dfsResult.isTreeComplete) break

        // If the returned tree is null then the deadline has been reached, therefore break and use the result
        // computed by the last iteration.
        dfsResult = searchDeep(fromPosition, depth + 1, positionCache, deadline, sortingAlgorithm) ?: break
        depth++

        // If children is null then depth is equal to zero therefore no children were scored. See searchDeep
        // function declaration for details. In such a case continue looping, this time search 1 move deeper.
        if (dfsResult.principleMove == null && !dfsResult.isTreeComplete) continue

        // The given position represents the terminus of the game.
        @Suppress("KotlinConstantConditions")
        if (dfsResult.principleMove == null && dfsResult.isTreeComplete) break
    }

    return IterativeDeepeningSearchResult(dfsResult, depth)
}

data class DfsSearchResult(
    val score: Int,
    val isTreeComplete: Boolean,
    val principleMove: Pair<SynchronousAction, DfsSearchResult>?
)

typealias MoveSortingAlgorithm = (legalMoves: Set<SynchronousAction>, branchIndex: Int, currentDepth: Int) -> List<SynchronousAction>

class PrincipleVariationMoveSortingAlgorithm(private val variation: List<SynchronousAction>): MoveSortingAlgorithm {
    override fun invoke(
        legalMoves: Set<SynchronousAction>,
        branchIndex: Int,
        currentDepth: Int
    ): List<SynchronousAction> {
        if (branchIndex != 0 || currentDepth >= variation.size) return legalMoves.toList()
        val sortedMoves = ArrayDeque<SynchronousAction>(legalMoves.size)
        sortedMoves.addAll(legalMoves)

        val nextMove = variation[currentDepth]
        if (sortedMoves.remove(nextMove)) {
            sortedMoves.addFirst(nextMove)
        }

        return sortedMoves
    }
}
fun searchDeep(fromPosition: Position, targetDepth: Int, positionCache: PositionCache,
               deadline: Instant? = null, sortMoves: MoveSortingAlgorithm) =
    searchDeepRecursive(fromPosition, targetDepth, positionCache, deadline, sortMoves, targetDepth)

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
fun searchDeepRecursive(fromPosition: Position,
                        depthLeft: Int,
                        positionCache: PositionCache,
                        deadline: Instant?,
                        sortMoves: MoveSortingAlgorithm,
                        targetDepth: Int,
                        parentScore: Int = Int.MIN_VALUE + 1,
                        beta: Int = Int.MAX_VALUE,
                        thisBranchIndex: Int = 0,
): DfsSearchResult? {
    if (deadline?.isBefore(Instant.now()) == true) return null
    if (fromPosition.legalMoves.isEmpty()) return DfsSearchResult(heuristicScore(fromPosition), true, null)

    // If the maximum depth has been reached and the game is not over, simply perform a heuristic score evaluation.
    if (depthLeft <= 0) return DfsSearchResult(heuristicScore(fromPosition), false, null)

    var thisScore: Int = parentScore
    var isTreeComplete = true
    val children = mutableMapOf<SynchronousAction, DfsSearchResult>()

    val sortedMoves = sortMoves(fromPosition.legalMoves, thisBranchIndex, targetDepth - depthLeft)
    // Compute all possible moves we can make
    for ((childBranchIndex, move) in sortedMoves.withIndex()) {
        // Now create a new position where we've made that move
        val childPosition = fromPosition.branch(move)

        // Now compute all possible moves our opponent can make.
        // While searchDown may not actually reach the real terminus of the game, it is assumed to.
        // Therefore, the scores returned by this function should be treated as exact values, not approximations.

        val child: DfsSearchResult = when (val cachedResult = positionCache[childPosition, depthLeft - 1]) {
            is PositionCache.Hit -> DfsSearchResult(
                score = cachedResult.cacheEntry.score * -1,
                isTreeComplete = cachedResult.cacheEntry.isTreeComplete,
                principleMove = cachedResult.cacheEntry.principleMove
            )
            is PositionCache.Miss -> {
                val childResult = searchDeepRecursive(
                    fromPosition = childPosition,
                    depthLeft = depthLeft - 1,
                    positionCache,
                    deadline,
                    sortMoves,
                    targetDepth,
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
                    childBranchIndex
                ) ?: return null

                DfsSearchResult(
                    score = childResult.score * -1,
                    childResult.isTreeComplete,
                    childResult.principleMove
                )
            }
        }

        isTreeComplete = isTreeComplete && child.isTreeComplete
        thisScore = max(thisScore, child.score)
        children[move] = child

        // alpha > beta
        // minimum score which the maximized player is assured of > maximum score with the minimized player is assured of
        // alpha being the minimum score for the maximized player
        // beta being the maximum score for the minimized player
        if (thisScore >= beta) break
    }

    val principleMove = children.entries.stream()
        .max { (_, node) -> node.score }!!
        .let { (move, node) -> Pair(move, node) }

    positionCache.set(fromPosition, depthLeft, PositionCache.CacheEntry(thisScore, isTreeComplete, principleMove))
    return DfsSearchResult(thisScore, isTreeComplete, principleMove)
}