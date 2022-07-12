package com.duncpro.autochess

import com.duncpro.autochess.BoardDimension.*
import com.duncpro.autochess.Color.*
import com.duncpro.autochess.behavior.*
import java.util.stream.Collectors

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

/**
 * Represents a chess board at a distinct instant in time. More specifically at the move with index [nextMove].
 * Implementations of this class may or may not be mutable, therefore it is possible for the [nextMove], [whoseTurn],
 * and board state of a position to change. [MaskedPosition] provides an immutable implementation of this interface.
 */
abstract class Position {
    abstract val nextMove: Int

    val whoseTurn get() = Color.values()[nextMove % 2]

    abstract operator fun get(cell: Cell): PlacedPiece?

    /**
     * The set of all pieces owned by [whoseTurn] which are under immediate attack by the opponent.
     * This field is cached and therefore repeated accesses will not cause a significant performance hit.
     */
    val underAttack: Set<Cell> by CachedProperty(
        getCurrentVersionId = { this.nextMove },
        compute = {
            pieces.stream()
                .filter { capablePiece -> capablePiece.aesthetic.color == this.whoseTurn.opposite }
                .flatMap { capablePiece -> capablePiece.moves.stream() }
                .flatMap { move -> move.effects.stream() }
                .filterIsInstance<Take>()
                .map(Take::target)
                .collect(Collectors.toUnmodifiableSet())
        }
    )

    /**
     * Determines if the game is over by checking if the player with color [whoseTurn] has no legal moves to make.
     * This could be indicative of a stalemate, or a checkmate.
     */
    val isGameOver get() = possibleMoves.isEmpty()

    val isStalemate get() = possibleMoves.isEmpty() && !underAttack.contains(locateKing(whoseTurn))

    val isCheckmate get() = possibleMoves.isEmpty() && underAttack.contains(locateKing(whoseTurn))

    fun locateKing(color: Color): Cell = pieces.stream()
        .filter { piece -> piece.aesthetic == AestheticPiece(PieceType.KING, color) }
        .map { piece -> piece.location }
        .findFirst()
        .orElseThrow()

    /**
     * Creates [Position] which is identical to this one, except the given move has been applied.
     * This function is implemented using [MaskedPosition] and therefore does not incur the performance
     * hit of copying the entire board.
     */
    fun branch(move: EffectfulMove): Position {
        val mask = HashMap<Cell, PlacedPiece?>()

        for (effect in move.effects) {
            when (effect) {
                is Take -> mask[effect.target] = null
                is Translation -> {
                    val piece = this[effect.origin]?.aesthetic ?: throw IllegalStateException()
                    mask[effect.origin] = null
                    mask[effect.destination] = PlacedPiece(piece, this.nextMove)
                }
                is Spawn -> mask[effect.point] = PlacedPiece(effect.piece, this.nextMove)
            }
        }

        return MaskedPosition(this, mask)
    }

    /**
     * The set of [EffectfulMove]s which can legally be made by [whoseTurn] without inflicting self-check.
     * This field is cached and therefore repeated accesses will not cause a significant loss in performance.
     */
    val possibleMoves: Set<EffectfulMove> by CachedProperty(
        getCurrentVersionId = { this.nextMove },
        compute = {
            pieces.stream()
                .filter { ownPiece -> ownPiece.aesthetic.color == this.whoseTurn }
                .flatMap { ownPiece -> ownPiece.moves.stream() }
                .filter { move ->
                    val inNextPosition = this.branch(move)
                    val king = inNextPosition.locateKing(this.whoseTurn)

                    // Make sure that this move doesn't put the king in check.
                    !inNextPosition.pieces.stream()
                        .filter { opponentPiece -> opponentPiece.aesthetic.color == this.whoseTurn.opposite }
                        .flatMap { opponentPiece -> opponentPiece.moves.stream() }
                        .flatMap { opponentMove-> opponentMove.effects.stream()  }
                        .filterIsInstance<Take>()
                        .map(Take::target)
                        .anyMatch { target -> target == king }
                }
                .collect(Collectors.toUnmodifiableSet())
        }
    )

    /**
     * This class encapsulates a [AestheticPiece], it's position on the board represented as a [Cell], and all [EffectfulMove]s
     * which the piece is capable of making given its current position and the board's current state. Instances
     * of this class are exclusively produced by the private property [pieces].
     */
    data class CapablePiece(val location: Cell, val placed: PlacedPiece, val moves: Set<EffectfulMove>) {
        val aesthetic: AestheticPiece get() = placed.aesthetic
    }

    /**
     * The set of all possible moves which can be made from this position assuming that it is any player's turn.
     * In other words, this contains all the moves which black can make, and all the moves which white can make.
     */
    val pieces: Set<CapablePiece> by CachedProperty(
        getCurrentVersionId = { this.nextMove },
        compute = {
            filledCells.stream()
                .map { cell -> cell to this[cell]!! }
                .map { (cell, occupant) -> CapablePiece(cell, occupant, occupant.aesthetic.type.behavior(cell,
                    occupant.aesthetic.color, this)) }
                .collect(Collectors.toUnmodifiableSet())
        }
    )

    abstract val filledCells: Set<Cell>

    override fun toString(): String = this.toBoardString()
}

/**
 * A [Position] which masks another. This implementation should be used when performing search over the
 * position/move tree, since it avoids making a slow copy of the entire backing data structure.
 * The backing [Position], [under], must not change while this [MaskedPosition] is in use.
 * Such a change may not be accurately reflected. This class is intended for layered composition.
 */
class MaskedPosition(private val under: Position,
                     private val mask: Map<Cell, PlacedPiece?>): Position() {

    override val nextMove = under.nextMove + 1

    override operator fun get(cell: Cell): PlacedPiece? {
        if (mask.containsKey(cell)) return mask[cell]
        return under[cell]
    }

    override val filledCells: Set<Cell> by lazy {
        val list = LinkedHashSet<Cell>()
        val nulledCells = HashSet<Cell>()

        mask.forEach { (cell, contextPiece) ->
            if (contextPiece == null) {
                nulledCells.add(cell)
            } else {
                list.add(cell)
            }
        }

        for (cell in under.filledCells) {
            if (nulledCells.contains(cell)) continue
            list.add(cell)
        }

        return@lazy list
    }

    override fun equals(other: Any?): Boolean = implementInterfaceEquality<Position>(other) { otherPosition ->
        if (otherPosition.filledCells.size != this.filledCells.size)
            for (cell in filledCells) {
                if (this[cell] != otherPosition[cell]) return false
            }
        return@implementInterfaceEquality true
    }

    override fun hashCode(): Int {
        var result = 1
        for (cell in filledCells) {
            result = 31 * result + (this[cell]?.hashCode() ?: 0)
        }
        return result
    }
}

val EMPTY_POSITION: Position = object : Position() {
    override val nextMove: Int = -1

    override fun get(cell: Cell): PlacedPiece? = null

    override val filledCells: Set<Cell> = emptySet()
}

val DEFAULT_POSITION: Position = MaskedPosition(
    under = EMPTY_POSITION,
    mask = HashMap<Cell, PlacedPiece?>().apply {
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
    })
