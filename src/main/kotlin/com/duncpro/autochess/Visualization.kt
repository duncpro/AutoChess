package com.duncpro.autochess

import com.duncpro.autochess.Color.*
import com.duncpro.autochess.PieceType.*
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset

val Cell.ansiBackgroundColor: Ansi.Color get() =
    if (file % 2 == 0) {
        if (rank % 2 == 0) {
            Ansi.Color.GREEN
        } else {
            Ansi.Color.DEFAULT
        }
    } else {
        if (rank % 2 == 0) {
            Ansi.Color.DEFAULT
        } else {
            Ansi.Color.GREEN
        }
    }

val AestheticPiece.ansiForegroundColor: Ansi.Color get() =
    when (this.color) {
        WHITE -> Ansi.Color.DEFAULT
        BLACK -> Ansi.Color.BLACK
    }

fun AestheticPiece.unicodeSymbol(): String =  when (this.type) {
    PieceType.ROOK -> "♜"
    PieceType.KNIGHT -> "♞"
    PieceType.BISHOP -> "♝"
    PieceType.QUEEN -> "♛"
    PieceType.KING -> "♚"
    PieceType.PAWN -> "♟"
}

fun Position.toBoardString(): String {
    ByteArrayOutputStream().use { bos ->
        PrintStream(bos).use { stream-> printBoard(stream) }
        return bos.toString(Charset.defaultCharset())
    }
}

fun Position.printBoard(stream: PrintStream) {
    stream.println("WHITE")

    for (file in 0 until 8) {
        // Files descend from the top of the page to the bottom of the page, not across the page like
        // how a human would normally play chess.
        stream.print(fileLetter(file).toString().padEnd(3))

        for (rank in 0 until 8) {
            val cell = Cell(file, rank)
            val piece = this[cell]?.aesthetic
            stream.print(ansi().bg(cell.ansiBackgroundColor))
            if (piece == null) {
                stream.print("   ")
            } else {
                stream.print(ansi().fg(piece.ansiForegroundColor))
                stream.print(" ${piece.unicodeSymbol()} ")
            }
            stream.print(ansi().reset())
        }
        stream.println(fileLetter(file))
    }
    stream.print("".padEnd(3))
    for (rank in 0 until 8) {
       stream. print((rank + 1).toString().padEnd(3))
    }
    stream.print("\n")
    stream.println("BLACK".padStart(3 * 8))
    if (this.canClaimDraw) {
        println("${this.whoseTurn} can claim draw.")
    }
    stream.flush()
}

