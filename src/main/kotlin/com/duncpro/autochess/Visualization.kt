package com.duncpro.autochess

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset


fun AestheticPiece.unicodeSymbol(): String =  when (this.color) {
    Color.WHITE -> when (this.type) {
        PieceType.ROOK -> "♖"
        PieceType.KNIGHT -> "♘"
        PieceType.BISHOP -> "♗"
        PieceType.QUEEN -> "♕"
        PieceType.KING -> "♔"
        PieceType.PAWN -> "♙"
    }
    Color.BLACK -> when (this.type) {
        PieceType.ROOK -> "♜"
        PieceType.KNIGHT -> "♞"
        PieceType.BISHOP -> "♝"
        PieceType.QUEEN -> "♛"
        PieceType.KING -> "♚"
        PieceType.PAWN -> "♟"
    }
}

fun Position.toBoardString(): String {
    ByteArrayOutputStream().use { bos ->
        PrintStream(bos).use { stream-> printBoard(stream) }
        return bos.toString(Charset.defaultCharset())
    }
}

fun Position.printBoard(stream: PrintStream) {
    stream.println("WHITE           ")

    for (file in 0 until 8) {
        // Files descend from the top of the page to the bottom of the page, not across the page like
        // how a human would normally play chess.
        stream.print(fileLetter(file).toString().padEnd(3))

        for (rank in 0 until 8) {
            val piece = this[(Cell(file, rank))]?.aesthetic
            if (piece == null) {
                stream.print("".padEnd(3))
            } else {
                stream.print(piece.unicodeSymbol().padEnd(3))
            }
        }
        stream.println(fileLetter(file))
    }
    stream.print("".padEnd(3))
    for (rank in 0 until 8) {
       stream. print((rank + 1).toString().padEnd(3))
    }
    stream.print("\n")
    stream.println("BLACK".padStart(3 * 8))
    stream.flush()
}

