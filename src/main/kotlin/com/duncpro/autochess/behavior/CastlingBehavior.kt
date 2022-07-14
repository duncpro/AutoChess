package com.duncpro.autochess.behavior

import com.duncpro.autochess.*
import com.duncpro.autochess.Color.*

class CastlingScheme(
    private val necessarilyEmptyFiles: Set<Int>,
    private val rookFile: Int,
    val createMove: (color: Color) -> SynchronousAction
) {
    fun areNecessaryPiecesIntact(castler: Color, position: Position): Boolean {
        val king = position[Cell(4, baseRank(castler))]
        val kingIntact = (king == preGamePlacedPiece(PieceType.KING, castler))

        val rook = position[Cell(rookFile, baseRank(castler))]
        val rookIntact = (rook == preGamePlacedPiece(PieceType.ROOK,castler))
        return kingIntact && rookIntact
    }

    fun canCastleNextMove(position: Position): Boolean {
        val emptinessSatisfied = necessarilyEmptyFiles.all { file -> position[Cell(file, baseRank(position.whoseTurn))] == null }
        val hasCastlingRights = areNecessaryPiecesIntact(position.whoseTurn, position)
        val notInCheck = !position.underAttack.contains(position.locateKing(position.whoseTurn))
        return emptinessSatisfied && hasCastlingRights && notInCheck
    }
}

val QueensideCastlingScheme = CastlingScheme(
    necessarilyEmptyFiles = setOf(1, 2, 3),
    rookFile = 0,
    createMove = { color ->
        SynchronousAction(
            Translate(Cell(4, baseRank(color)), Cell(2, baseRank(color))),
            Translate(Cell(0, baseRank(color)), Cell(3, baseRank(color)))
        )
    }
)

val KingsideCastlingScheme = CastlingScheme(
    necessarilyEmptyFiles = setOf(5, 6),
    rookFile = 7,
    createMove = { color ->
        SynchronousAction(
            Translate(Cell(4, baseRank(color)), Cell(6, baseRank(color))),
            Translate(Cell(7, baseRank(color)), Cell(5, baseRank(color)))
        )
    }
)

fun baseRank(color: Color) = when (color) {
    WHITE -> 0
    BLACK -> 7
}