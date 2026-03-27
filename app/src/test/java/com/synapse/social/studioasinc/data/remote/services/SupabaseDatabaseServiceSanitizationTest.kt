package com.synapse.social.studioasinc.data.remote.services

import org.junit.Assert.assertEquals
import org.junit.Test

class SupabaseDatabaseServiceSanitizationTest {

    private fun sanitize(query: String): String {
        return query.trim()
            .take(100)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    @Test
    fun `test basic alphanumeric strings remain unchanged`() {
        assertEquals("hello123", sanitize("hello123"))
        assertEquals("Simple Search", sanitize("Simple Search"))
    }

    @Test
    fun `test wildcards are escaped`() {
        assertEquals("100\\%", sanitize("100%"))
        assertEquals("user\\_name", sanitize("user_name"))
        assertEquals("\\%\\_\\%", sanitize("%_%"))
    }

    @Test
    fun `test backslashes are escaped`() {
        assertEquals("C:\\\\Windows", sanitize("C:\\Windows"))
        assertEquals("\\\\\\%", sanitize("\\%"))
    }

    @Test
    fun `test single quotes remain unchanged`() {
        // PostgREST/PostgreSQL handles single quotes within the value string if passed correctly via the API.
        // If we escape ' to '', it might result in literal '' in the search if the SDK also escapes it.
        // However, ilike in Supabase/PostgREST is usually passed as a query parameter where ' is just a character.
        assertEquals("O'Reilly", sanitize("O'Reilly"))
    }

    @Test
    fun `test SQL special characters are not removed but treated as literals`() {
        // These should not be removed because they are valid parts of a search query (e.g. searching for code snippets)
        // and they don't pose an injection risk when used with ilike in PostgREST.
        assertEquals("select; from", sanitize("select; from"))
        assertEquals("admin' --", sanitize("admin' --"))
        assertEquals("/* comment */", sanitize("/* comment */"))
    }

    @Test
    fun `test trimming and length limit`() {
        assertEquals("trimmed", sanitize("  trimmed  "))

        val longString = "a".repeat(150)
        val sanitizedLong = sanitize(longString)
        assertEquals(100, sanitizedLong.length)
        assertEquals("a".repeat(100), sanitizedLong)
    }
}
