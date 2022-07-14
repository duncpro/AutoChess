package com.duncpro.autochess

import com.duncpro.autochess.Color.*
import com.duncpro.autochess.behavior.Translate
import org.fusesource.jansi.AnsiConsole
import java.time.Instant

fun main() {
    AnsiConsole.systemInstall()
    selfPlay()
    AnsiConsole.systemUninstall()
}

fun selfPlay() {
    var position = DEFAULT_POSITION
    val positionTable = HashMapPositionCache()

    val white = NonHumanPlayer(positionTable)
    val black = NonHumanPlayer(positionTable)

    position.printBoard(System.out)
    while (!position.isGameOver) {
        val move = when (position.whoseTurn) {
            WHITE -> white.takeTurn(position)
            BLACK -> black.takeTurn(position)
        }
        println("${position.whoseTurn} moved: $move")
        position = position.branch(move)
        position.printBoard(System.out)
    }

    if (position.isStalemate) println("Draw")
    if (position.isCheckmate) println("${position.whoseTurn.opposite} wins")
}

//fun versusHuman(humanColor: Color) = System.`in`.bufferedReader().use { reader ->
//    var position = DEFAULT_POSITION
//    val transpositionTable = HashMapPositionCache()
//
//    position.printBoard(System.out)
//    while (!position.isGameOver) {
//
//        if (position.whoseTurn == humanColor) {
//            println("It is your turn. Enter the move which you would like to perform, for instance A2A4")
//            val humanMove = parseTranslation(reader.readLine())
//            if (humanMove == null) {
//                println("Error: Not a valid translation label.")
//                continue
//            }
//            val newPosition = position.branch(humanMove)
//            if (newPosition == null) {
//                println("Error: Not a legal move.")
//                continue
//            }
//            position = newPosition
//        } else {
//            println("Performing move search with minimum depth 4 and deadline 20 seconds from now.")
//            val searchResult = search(position, 4, Instant.now().plusSeconds(20), transpositionTable)
//            val bestMove = searchResult.deepestResult.children!!
//                .entries
//                .stream()
//                .max { (_, score) -> score }
//                ?.let { (move, _) -> move }
//                ?: throw IllegalStateException()
//            println("${position.whoseTurn} moved: $bestMove")
//            println("Depth: ${searchResult.depth}")
//            position = position.branch(bestMove)
//        }
//
//        position.printBoard(System.out)
//    }
//
//    if (position.isStalemate) println("Draw")
//    if (position.isCheckmate) println("${position.whoseTurn.opposite} wins")
//}

fun parseTranslation(moveLabel: String): Translate? {
    val sanitizedMoveLabel = moveLabel.uppercase().replace("\n", "")
    if (sanitizedMoveLabel.length != 4) return null
    val from = parseCell(sanitizedMoveLabel.substring(0, 2)) ?: return null
    val to = parseCell(sanitizedMoveLabel.substring(2, 4)) ?: return null
    return Translate(from, to)
}