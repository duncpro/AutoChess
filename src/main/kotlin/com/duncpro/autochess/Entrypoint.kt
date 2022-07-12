package com.duncpro.autochess

import java.time.Instant

fun main() = selfPlay()

fun selfPlay() {
    var position = DEFAULT_POSITION

    position.printBoard(System.out)
    while (!position.isGameOver) {
        val result = search(position, Instant.now().plusSeconds(15), 3)

        val bestMove = result.finalResult.children!!
            .entries
            .stream()
            .max(Comparator.comparing { (_, score) -> score })
            .map { (move, _) -> move }
            .orElseThrow()

        println("${position.whoseTurn} moved: $bestMove")
        println("Depth: ${result.finalDepth}")
        position = position.branch(bestMove)
        position.printBoard(System.out)
    }

    if (position.isStalemate) println("Draw")
    if (position.isCheckmate) println("${position.whoseTurn.opposite} wins")
}