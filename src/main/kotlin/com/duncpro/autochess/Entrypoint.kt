package com.duncpro.autochess

import com.duncpro.autochess.behavior.Translate
import java.time.Instant

fun main() = selfPlay()

fun selfPlay() {
    var position = DEFAULT_POSITION
    val transpositionTable = HashMapTranspositionTable()

    position.printBoard(System.out)
    while (!position.isGameOver) {
        val result = search(position, 4, Instant.now().plusSeconds(15), transpositionTable)

        val bestMove = result.deepestResult.children!!
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

fun versusHuman(humanColor: Color) = System.`in`.bufferedReader().use { reader ->
    var position = DEFAULT_POSITION
    val transpositionTable = HashMapTranspositionTable()

    position.printBoard(System.out)
    while (!position.isGameOver) {

        if (position.whoseTurn == humanColor) {
            println("It is your turn. Enter the move which you would like to perform, for instance A2A4")
            val humanMove = parseTranslation(reader.readLine())
            if (humanMove == null) {
                println("Error: Not a valid translation label.")
                continue
            }
            val newPosition = position.branch(humanMove)
            if (newPosition == null) {
                println("Error: Not a legal move.")
                continue
            }
            position = newPosition
        } else {
            println("Performing move search with minimum depth 4 and deadline 20 seconds from now.")
            val searchResult = search(position, 4, Instant.now().plusSeconds(20), transpositionTable)
            val bestMove = searchResult.deepestResult.children!!
                .entries
                .stream()
                .max { (_, score) -> score }
                ?.let { (move, _) -> move }
                ?: throw IllegalStateException()
            println("${position.whoseTurn} moved: $bestMove")
            println("Depth: ${searchResult.finalDepth}")
            position = position.branch(bestMove)
        }

        position.printBoard(System.out)
    }

    if (position.isStalemate) println("Draw")
    if (position.isCheckmate) println("${position.whoseTurn.opposite} wins")
}

fun parseTranslation(moveLabel: String): Translate? {
    val sanitizedMoveLabel = moveLabel.uppercase().replace("\n", "")
    if (sanitizedMoveLabel.length != 4) return null
    val from = parseCell(sanitizedMoveLabel.substring(0, 2)) ?: return null
    val to = parseCell(sanitizedMoveLabel.substring(2, 4)) ?: return null
    return Translate(from, to)
}