package com.duncpro.autochess

import com.duncpro.autochess.BoardDimension.*
import com.duncpro.autochess.Color.*
import com.duncpro.autochess.behavior.*
import java.util.stream.Collectors
import java.util.stream.Stream

enum class BoardDimension { FILE, RANK }

val BoardDimension.opposite get() = when (this) {
    FILE -> RANK
    RANK -> FILE
}

fun fileLetter(file: Int) = when (file) {
    0 -> 'A'
    1 -> 'B'
    2 -> 'C'
    3 -> 'D'
    4 -> 'E'
    5 -> 'F'
    6 -> 'G'
    7 -> 'H'
    else -> throw IllegalArgumentException()
}

fun fileIndex(file: Char): Int? = when(file) {
    'A' -> 0
    'B' -> 1
    'C' -> 2
    'D' -> 3
    'E' -> 4
    'F' -> 5
    'G' -> 6
    'H' -> 7
    else -> null
}

/**
 * Represents a cell on a chess board. There are 64 (8 files * 8 columns) cells on standard chess board.
 * Cells are zero-indexed, therefore instances of this class should only be constructed with an index value
 * in the range [0, 64). For efficiency's sake this constructor performs no validation on the index field.
 */
@JvmInline
value class Cell constructor(val index: Int) {
    constructor(file: Int, rank: Int) : this((file * 8) + rank)

    val rank: Int get() = index % 8
    val file: Int get() = index / 8

    operator fun get(dimension: BoardDimension): Int = when (dimension) {
        FILE -> this.file
        RANK -> this.rank
    }

    fun towardsQueenside(distance: Int = 1): Cell? = cellOrNull(file - distance, rank)
    fun towardsKingside(distance: Int = 1): Cell? = cellOrNull(file + distance, rank)
    fun towardsColor(color: Color, distance: Int = 1): Cell? = when (color) {
        WHITE -> cellOrNull(file, rank - distance)
        BLACK -> cellOrNull(file, rank + distance)
    }

    override fun toString(): String = "${fileLetter(file)}${rank + 1}"
}

fun cellOrNull(file: Int, rank: Int): Cell? {
    if (file < 0 || file > 7) return null
    if (rank < 0 || rank > 7) return null
    return Cell(file, rank)
}

fun parseCell(label: String): Cell? {
    return cellOrNull(
        file = fileIndex(label[0]) ?: return null,
        rank = label[1].digitToInt() - 1
    )
}

enum class Color {
    WHITE,
    BLACK;
}

val Color.opposite: Color get() = when (this) {
    WHITE -> BLACK
    BLACK -> WHITE
}


enum class PieceType {
    KING,
    QUEEN,
    ROOK,
    KNIGHT,
    BISHOP,
    PAWN
}

data class AestheticPiece(val type: PieceType, val color: Color)

data class PlacedPiece(val aesthetic: AestheticPiece, val atMove: Int) {
    val isOriginalPosition: Boolean = atMove == -1
}

class Position(
    val previous: Position? = null,
    private val mask: Map<Cell, PlacedPiece?>,
    val agreedToDraw: Boolean = false
) {
    val nextMove: Int = (previous?.nextMove ?: -1) + 1

    operator fun get(cell: Cell): PlacedPiece? {
        if (mask.containsKey(cell)) return mask[cell]
        if (previous == null) return null
        return previous[cell]
    }

    val filledCells: Set<Cell> by lazy {
        val list = LinkedHashSet<Cell>()
        val nulledCells = HashSet<Cell>()

        mask.forEach { (cell, contextPiece) ->
            if (contextPiece == null) {
                nulledCells.add(cell)
            } else {
                list.add(cell)
            }
        }

        if (previous != null) {
            for (cell in previous.filledCells) {
                if (nulledCells.contains(cell)) continue
                list.add(cell)
            }
        }

        return@lazy list
    }

    val whoseTurn get() = Color.values()[nextMove % 2]

    val transposable: TransposablePosition by lazy { TransposablePosition(this) }

    /**
     * The set of all pieces owned by [whoseTurn] which are under immediate attack by the opponent.
     * This field is cached and therefore repeated accesses will not cause a significant performance hit.
     */
    val underAttack: Set<Cell> by lazy {
        pieces.stream()
            .filter { capablePiece -> capablePiece.aesthetic.color == this.whoseTurn.opposite }
            .flatMap { capablePiece -> capablePiece.moves.stream() }
            .flatMap { move -> move.actions.stream() }
            .filterIsInstance<Take>()
            .map(Take::target)
            .collect(Collectors.toUnmodifiableSet())
    }

    /**
     * Determines if the game is over by checking if the player with color [whoseTurn] has no legal moves to make.
     * This could be indicative of a stalemate, or a checkmate.
     */
    val isGameOver get() = isDraw || isCheckmate

    val isDraw get() = isStalemate || agreedToDraw

    /**
     * Determines if the game has concluded in a stalemate by checking if the player with color [whoseTurn] has no
     * legal moves to make, and that player is not in check.
     */
    val isStalemate get() = legalMoves.isEmpty() && !underAttack.contains(locateKing(whoseTurn))

    /**
     * Determines if the game has concluded in a checkmate by checking if the player with color [whoseTurn] has
     * no legal moves to make, and that player's king is in check.
     */
    val isCheckmate get() = legalMoves.isEmpty() && underAttack.contains(locateKing(whoseTurn))

    fun locateKing(color: Color): Cell = pieces.stream()
        .filter { piece -> piece.aesthetic == AestheticPiece(PieceType.KING, color) }
        .map { piece -> piece.location }
        .findFirst()
        .orElseThrow()


    /**
     * Interprets the given [Translate] as a move in chess, inferring the legality and side effects of the translation
     * using the current state of the board. If this translation is not legal, then null is returned.
     */
    fun branch(translate: Translate): Position? {
        val move = this.legalMoves.singleOrNull { move -> move.actions.contains(translate) } ?: return null
        return branch(move)
    }

    /**
     * Creates [Position] which is identical to this one, except the given [SynchronousAction] has been applied.
     * The action will be applied regardless of its legality. For instance one might pass an action which
     * does not represent a legal move (like taking the opponent's king).
     */
    fun branch(move: SynchronousAction): Position {
        val mask = HashMap<Cell, PlacedPiece?>()

        for (effect in move.actions) {
            when (effect) {
                is Take -> mask[effect.target] = null
                is Translate -> {
                    val piece = this[effect.origin]?.aesthetic ?: throw IllegalStateException()
                    mask[effect.origin] = null
                    mask[effect.destination] = PlacedPiece(piece, this.nextMove)
                }
                is Spawn -> mask[effect.point] = PlacedPiece(effect.piece, this.nextMove)
                is ClaimDraw -> return Position(this, emptyMap(), true)
            }
        }

        return Position(this, mask)
    }

    /**
     * The set of [SynchronousAction]s which can legally be made by [whoseTurn] without inflicting self-check.
     * This field is cached and therefore repeated accesses do not imply a significant loss in performance.
     */
    val legalMoves: Set<SynchronousAction> by lazy {
        val pieceMoves = pieces.stream()
            .filter { ownPiece -> ownPiece.aesthetic.color == this.whoseTurn }
            .flatMap { ownPiece -> ownPiece.moves.stream() }
            // A move which actually takes the opponent's king is not legal.
            // Threatening the king with take is legal, but not actually doing it.
            // Therefore, such moves must be filtered out.
            .filter { move -> move.actions.stream()
                .filterIsInstance<Take>()
                .noneMatch { take -> take.target == this.locateKing(whoseTurn.opposite) }
            }
            .filter { move ->
                val inNextPosition = this.branch(move)
                val king = inNextPosition.locateKing(this.whoseTurn)

                // Make sure that this move doesn't put the king in check.
                !inNextPosition.pieces.stream()
                    .filter { opponentPiece -> opponentPiece.aesthetic.color == this.whoseTurn.opposite }
                    .flatMap { opponentPiece -> opponentPiece.moves.stream() }
                    .flatMap { opponentMove-> opponentMove.actions.stream()  }
                    .filterIsInstance<Take>()
                    .map(Take::target)
                    .anyMatch { target -> target == king }
            }
            .collect(Collectors.toUnmodifiableSet())

        val drawMove = if (DrawBehavior.canClaimDraw(this)) setOf(SynchronousAction(ClaimDraw)) else emptySet()

        return@lazy pieceMoves union drawMove
    }

    /**
     * This class encapsulates a [AestheticPiece], it's position on the board represented as a [Cell], and all
     * [SynchronousAction]s which the piece is capable of making (naive) given its current position and the board's
     * current state. Instances of this class should be produced exclusively by the property accessor [pieces].
     * Note, [moves] will contain illegal self-checking actions. Therefore, this class is not a suitable mechanism
     * for legal move discovery. For such a use case the [legalMoves] property accessor is provided.
     */
    data class CapablePiece(val location: Cell, val placed: PlacedPiece, val moves: Set<SynchronousAction>) {
        val aesthetic: AestheticPiece get() = placed.aesthetic
    }

    /**
     * The set of all pieces which are currently occupying the board.
     * Iteration over this set is preferable to iteration over the entire board (all 64 cells) if possible.
     */
    val pieces: Set<CapablePiece> by lazy {
        filledCells.stream()
            .map { cell -> cell to this[cell]!! }
            .map { (cell, occupant) -> CapablePiece(cell, occupant, occupant.aesthetic.type.behavior(cell,
                occupant.aesthetic.color, this)) }
            .collect(Collectors.toUnmodifiableSet())
    }
    override fun toString(): String = this.toBoardString()
}

private val DEFAULT_POSITION_MASK = HashMap<Cell, PlacedPiece?>().apply {
    fun defaultPiece(type: PieceType, color: Color) = PlacedPiece(AestheticPiece(type, color), Int.MIN_VALUE)

    this[Cell(0, 0)] = defaultPiece(PieceType.ROOK, WHITE)
    this[Cell(1, 0)] = defaultPiece(PieceType.KNIGHT, WHITE)
    this[Cell(2, 0)] = defaultPiece(PieceType.BISHOP, WHITE)
    this[Cell(3, 0)] = defaultPiece(PieceType.QUEEN, WHITE)
    this[Cell(4, 0)] = defaultPiece(PieceType.KING, WHITE)
    this[Cell(5, 0)] = defaultPiece(PieceType.BISHOP, WHITE)
    this[Cell(6, 0)] = defaultPiece(PieceType.KNIGHT, WHITE)
    this[Cell(7, 0)] = defaultPiece(PieceType.ROOK, WHITE)

    this[Cell(0, 7)] = defaultPiece(PieceType.ROOK, BLACK)
    this[Cell(1, 7)] = defaultPiece(PieceType.KNIGHT, BLACK)
    this[Cell(2, 7)] = defaultPiece(PieceType.BISHOP, BLACK)
    this[Cell(3, 7)] = defaultPiece(PieceType.QUEEN, BLACK)
    this[Cell(4, 7)] = defaultPiece(PieceType.KING, BLACK)
    this[Cell(5, 7)] = defaultPiece(PieceType.BISHOP, BLACK)
    this[Cell(6, 7)] = defaultPiece(PieceType.KNIGHT, BLACK)
    this[Cell(7, 7)] = defaultPiece(PieceType.ROOK, BLACK)

    for (file in 0 until 8) {
        this[Cell(file, 1)] = defaultPiece(PieceType.PAWN, WHITE)
        this[Cell(file, 6)] = defaultPiece(PieceType.PAWN, BLACK)
    }
}

val DEFAULT_POSITION = Position(null, DEFAULT_POSITION_MASK)