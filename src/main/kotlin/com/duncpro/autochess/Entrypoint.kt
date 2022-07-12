package com.duncpro.autochess

import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.runAsync

fun main() = selfPlay()

fun selfPlay() {
    var position = DEFAULT_POSITION
    val transpositionTable = HashMapTranspositionTable()

    position.printBoard(System.out)
    while (!position.isGameOver) {
        val result = search(position, 4, Instant.now().plusSeconds(15), transpositionTable)

        val bestMove = result.finalResult.children!!
            .entries
            .stream()
            .max { (_, score) -> score }
            ?.let { (move, _) -> move }
            ?: throw IllegalStateException()

        println("${position.whoseTurn} moved: $bestMove")
        println("Depth: ${result.finalDepth}")
        position = position.branch(bestMove)
        position.printBoard(System.out)
    }

    if (position.isStalemate) println("Draw")
    if (position.isCheckmate) println("${position.whoseTurn.opposite} wins")
}