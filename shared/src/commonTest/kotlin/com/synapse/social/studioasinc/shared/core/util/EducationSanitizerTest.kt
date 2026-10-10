package com.synapse.social.studioasinc.shared.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EducationSanitizerTest {

    @Test
    fun testSanitizeNormalInput() {
        val result = EducationSanitizer.sanitizeEducationString("Stanford University")
        assertEquals("Stanford University", result)
    }

    @Test
    fun testUnwrapSingleStringifiedJsonArray() {
        val result = EducationSanitizer.sanitizeEducationString("""["Stanford University"]""")
        assertEquals("Stanford University", result)
    }

    @Test
    fun testUnwrapNestedStringifiedJsonArrays() {
        val result = EducationSanitizer.sanitizeEducationString("""["[\"Stanford University\"]"]""")
        assertEquals("Stanford University", result)
    }

    @Test
    fun testCorruptedEmptyBrackets() {
        val rawCorrupted = """["[\"[\"\\\\\\\\\"[]\\\\\\\\\"\\\"]\"]"]"""
        val result = EducationSanitizer.sanitizeEducationString(rawCorrupted)
        assertNull(result)
    }

    @Test
    fun testSanitizeList() {
        val rawList = listOf("""["[\"Harvard\"]"]""", """["Stanford"]""")
        val result = EducationSanitizer.sanitizeEducationList(rawList)
        assertEquals(listOf("Harvard", "Stanford"), result)
    }
}
