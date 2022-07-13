package com.duncpro.autochess

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
}