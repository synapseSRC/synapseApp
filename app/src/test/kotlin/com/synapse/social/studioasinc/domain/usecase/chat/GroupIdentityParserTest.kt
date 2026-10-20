package com.synapse.social.studioasinc.domain.usecase.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GroupIdentityParserTest {
    @Test
    fun parsesTheThreeLabeledFields() {
        val result = GroupIdentityParser.parse(
            "Name: Systems Circle\nDescription: Read one paper each week and be kind.\nIcon concept: Open book"
        )
        assertEquals("Systems Circle", result.name)
        assertEquals("Read one paper each week and be kind.", result.description)
        assertEquals("Open book", result.iconConcept)
    }

    @Test
    fun rejectsMissingFieldsInsteadOfInventingFallbacks() {
        assertThrows(IllegalArgumentException::class.java) {
            GroupIdentityParser.parse("Name: Systems Circle")
        }
    }
}
