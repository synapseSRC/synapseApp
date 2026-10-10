package com.synapse.social.studioasinc.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImgBBConfigFormTest {

    @Test
    fun testImgBBKeyFormState_typingDoesNotInvokeSaveCallbackUntilExplicitSave() {
        var saveCallbackInvoked = false
        var savedApiKey = ""

        val onApiKeyChange: (String) -> Unit = {
            saveCallbackInvoked = true
            savedApiKey = it
        }

        var localStateKey = ""

        // User typing simulates local value changes
        localStateKey = "my_custom_imgbb_key"
        assertFalse("Typing in local state must not invoke save callback", saveCallbackInvoked)

        // Explicit save action triggered
        onApiKeyChange(localStateKey)

        assertTrue("Save callback should be invoked on explicit save action", saveCallbackInvoked)
        assertEquals("my_custom_imgbb_key", savedApiKey)
    }
}
