package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator

/**
 * Outlined auth input.
 *
 * Behaviour is unchanged from before - validation, error text, password masking, the visibility
 * toggle, the loading indicator, focus scale animation, keyboard options/actions and the semantics
 * (error state, valid state, content description) all behave exactly as they did.
 *
 * What the redesign adds is appearance control: [shape], [minHeight], [containerColor],
 * [focusedBorderColor], [unfocusedBorderColor] and [textStyle]. Every default reproduces the
 * previous look, so callers that do not pass anything are unaffected, while the sign-in screen can
 * align the field with the brand surface (56dp tall, medium corner, teal focus ring).
 *
 * The accessibility strings that used to be hardcoded here now come from resources.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    isValid: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isPassword: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = AppShapes.inputShape,
    minHeight: Dp = Sizes.ButtonHeightM3Medium,
    containerColor: Color = Color.Transparent,
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor: Color = MaterialTheme.colorScheme.outline,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        label = "Focus Scale"
    )

    val fieldDescription = stringResource(R.string.auth_field_content_description, label)
    val validDescription = stringResource(R.string.auth_field_valid_input)
    val errorDescription = stringResource(R.string.auth_field_error)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(vertical = Spacing.space8)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                if (isLoading) {
                    ExpressiveLoadingIndicator(
                        modifier = Modifier.size(Sizes.IconLarge)
                    )
                } else if (isPassword) {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(Sizes.IconGiant)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) R.string.auth_field_hide_password
                                else R.string.auth_field_show_password
                            ),
                            tint = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (isValid) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = validDescription,
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else if (error != null) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = errorDescription,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            isError = error != null,
            interactionSource = interactionSource,
            textStyle = textStyle,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = if (error != null) MaterialTheme.colorScheme.error else focusedBorderColor,
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
                focusedBorderColor = if (error != null) MaterialTheme.colorScheme.error else focusedBorderColor,
                unfocusedBorderColor = if (error != null) MaterialTheme.colorScheme.error else unfocusedBorderColor,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = if (error != null) MaterialTheme.colorScheme.error else focusedBorderColor,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,
                focusedLeadingIconColor = focusedBorderColor,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLeadingIconColor = MaterialTheme.colorScheme.error,
                errorSupportingTextColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .semantics {
                    contentDescription = fieldDescription
                    if (error != null) {
                        this.error(error)
                    }
                    if (isValid) {
                        stateDescription = validDescription
                    }
                }
        )

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = Spacing.space16, top = Spacing.space4)
                )
            }
        }
    }
}

