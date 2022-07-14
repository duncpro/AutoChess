package com.duncpro.autochess

import com.duncpro.autochess.behavior.Translate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FoundationTest {
    @Test
    fun testConstructionAndAccessForCell() {
        for (file in 0 until 8) {
            for (rank in 0 until 8) {
                val cell = Cell(file, rank)
                assertEquals(cell.file, file)
                assertEquals(cell.rank, rank)
            }
        }
    }

    @Test
    fun testCellEquality() {
        assertEquals(Cell(3, 5), Cell(3, 5))
    }

    @Test
    fun testTranspositionEquality() {
        assertEquals(DEFAULT_POSITION, DEFAULT_POSITION)

        val changedPos = DEFAULT_POSITION.branch(Translate(Cell(3, 1), Cell(3, 2)))
        assertNotEquals(changedPos, DEFAULT_POSITION)
    }
}