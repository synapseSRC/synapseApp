package com.synapse.social.studioasinc.shared.data.passkey

import com.synapse.social.studioasinc.shared.domain.repository.PasskeyPlatformManager
import kotlin.experimental.ExperimentalNativeApi
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationErrorDomain
import platform.AuthenticationServices.ASAuthorizationErrorUnknown
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialAssertion
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialAssertionRequest
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialProvider
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialRegistration
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialRegistrationRequest
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * iOS implementation of [PasskeyPlatformManager] backed by the AuthenticationServices
 * WebAuthn/passkey API (ASAuthorizationController). All controller and delegate work is
 * dispatched to the main queue; the platform flow is bridged to coroutines via
 * [suspendCancellableCoroutine].
 *
 * The credential JSON handed back mirrors the WebAuthn PublicKeyCredential shape so it
 * can be sent to the backend verification Edge Functions unchanged.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class, ExperimentalNativeApi::class)
class IosPasskeyPlatformManager() : PasskeyPlatformManager {

    private companion object {
        const val DEFAULT_RP_ID = "synapse-social.com"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * ASAuthorizationController only holds a weak reference to its delegate, so the
     * delegate must be strongly retained while a flow is in flight.
     */
    private var activeDelegate: PasskeyAuthorizationDelegate? = null

    override val isSupported: Boolean
        get() = NSProcessInfo.processInfo.operatingSystemVersion.majorVersion >= 15L

    override suspend fun createCredential(requestJson: String): String =
        suspendCancellableCoroutine { continuation ->
            val registrationRequest = registrationRequestFromJson(requestJson)
            val controller = ASAuthorizationController(authorizationRequests = listOf(registrationRequest))
            val delegate = PasskeyAuthorizationDelegate(
                onComplete = { authorization ->
                    val credential =
                        authorization.credential as? ASAuthorizationPlatformPublicKeyCredentialRegistration
                    if (credential == null) {
                        completeExceptionally(
                            continuation,
                            IllegalStateException("Unexpected authorization credential: ${authorization.credential}")
                        )
                    } else {
                        complete(continuation) { registrationResponseJson(credential) }
                    }
                },
                onError = { error ->
                    completeExceptionally(continuation, IllegalStateException(passkeyErrorMessage(error)))
                },
                presentationAnchor = { presentationAnchor() }
            )
            activeDelegate = delegate
            controller.delegate = delegate
            controller.presentationContextProvider = delegate

            continuation.invokeOnCancellation {
                dispatch_async(dispatch_get_main_queue()) {
                    cancelSafely(controller)
                }
            }
            dispatch_async(dispatch_get_main_queue()) {
                controller.performRequests()
            }
        }

    override suspend fun assertCredential(optionsJson: String): String =
        suspendCancellableCoroutine { continuation ->
            val assertionRequest = assertionRequestFromJson(optionsJson)
            val controller = ASAuthorizationController(authorizationRequests = listOf(assertionRequest))
            val delegate = PasskeyAuthorizationDelegate(
                onComplete = { authorization ->
                    val credential =
                        authorization.credential as? ASAuthorizationPlatformPublicKeyCredentialAssertion
                    if (credential == null) {
                        completeExceptionally(
                            continuation,
                            IllegalStateException("Unexpected authorization credential: ${authorization.credential}")
                        )
                    } else {
                        complete(continuation) { authenticationResponseJson(credential) }
                    }
                },
                onError = { error ->
                    completeExceptionally(continuation, IllegalStateException(passkeyErrorMessage(error)))
                },
                presentationAnchor = { presentationAnchor() }
            )
            activeDelegate = delegate
            controller.delegate = delegate
            controller.presentationContextProvider = delegate

            continuation.invokeOnCancellation {
                dispatch_async(dispatch_get_main_queue()) {
                    cancelSafely(controller)
                }
            }
            dispatch_async(dispatch_get_main_queue()) {
                controller.performRequests()
            }
        }

    private fun registrationRequestFromJson(
        requestJson: String
    ): ASAuthorizationPlatformPublicKeyCredentialRegistrationRequest {
        val root = json.parseToJsonElement(requestJson).jsonObject
        val challenge = root.getValue("challenge").jsonPrimitive.content
        val user = root.getValue("user").jsonObject
        val userId = user.getValue("id").jsonPrimitive.content
        val userName = user.getValue("name").jsonPrimitive.content
        val rpId = root.getValue("rp").jsonObject.getValue("id").jsonPrimitive.content

        val provider = ASAuthorizationPlatformPublicKeyCredentialProvider(rpId)
        return provider.createCredentialRegistrationRequest(
            challenge = challenge.toNSData(),
            name = userName,
            userID = userId.toNSData()
        )
    }

    private fun assertionRequestFromJson(
        optionsJson: String
    ): ASAuthorizationPlatformPublicKeyCredentialAssertionRequest {
        val root = json.parseToJsonElement(optionsJson).jsonObject
        val challenge = root.getValue("challenge").jsonPrimitive.content
        val rpId = root["rpId"]?.jsonPrimitive?.contentOrNull ?: DEFAULT_RP_ID

        val provider = ASAuthorizationPlatformPublicKeyCredentialProvider(rpId)
        return provider.createCredentialAssertionRequest(challenge = challenge.toNSData())
    }

    private fun registrationResponseJson(
        credential: ASAuthorizationPlatformPublicKeyCredentialRegistration
    ): String {
        val credentialId = credential.credentialID.toBase64Url()
        val attestationObject = credential.rawAttestationObject?.toBase64Url()
            ?: throw IllegalStateException("Registration response is missing attestationObject")

        return buildJsonObject {
            put("id", credentialId)
            put("rawId", credentialId)
            put("type", "public-key")
            putJsonObject("response") {
                put("clientDataJSON", credential.rawClientDataJSON.toBase64Url())
                put("attestationObject", attestationObject)
            }
        }.toString()
    }

    private fun authenticationResponseJson(
        credential: ASAuthorizationPlatformPublicKeyCredentialAssertion
    ): String {
        val credentialId = credential.credentialID.toBase64Url()

        return buildJsonObject {
            put("id", credentialId)
            put("rawId", credentialId)
            put("type", "public-key")
            putJsonObject("response") {
                put("clientDataJSON", credential.rawClientDataJSON.toBase64Url())
                put("authenticatorData", credential.rawAuthenticatorData.toBase64Url())
                put("signature", credential.signature.toBase64Url())
                put("userHandle", credential.userID.toBase64Url())
            }
        }.toString()
    }

    private fun passkeyErrorMessage(error: NSError): String {
        if (error.domain == ASAuthorizationErrorDomain) {
            return when {
                error.code == ASAuthorizationErrorCanceled ->
                    "Passkey operation was cancelled by the user"
                error.code == ASAuthorizationErrorUnknown ->
                    "Passkey operation failed with an unknown error"
                else -> "Passkey operation failed: ${error.localizedDescription}"
            }
        }
        return "Passkey operation failed: ${error.localizedDescription}"
    }

    private fun presentationAnchor(): UIWindow {
        val application = UIApplication.sharedApplication
        return application.keyWindow
            ?: application.windows.firstOrNull { window ->
                (window as? UIWindow)?.isKeyWindow() == true
            } as? UIWindow
            ?: application.windows.firstOrNull() as? UIWindow
            ?: throw IllegalStateException("No window available to present the passkey flow")
    }

    private fun complete(continuation: CancellableContinuation<String>, buildResponse: () -> String) {
        val response = try {
            buildResponse()
        } catch (e: Exception) {
            completeExceptionally(continuation, e)
            return
        }
        activeDelegate = null
        if (continuation.isActive) {
            continuation.resume(response)
        }
    }

    private fun completeExceptionally(continuation: CancellableContinuation<*>, error: Throwable) {
        activeDelegate = null
        if (continuation.isActive) {
            continuation.resumeWithException(error)
        }
    }

    private fun String.toNSData(): NSData =
        (this as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: NSData()

    private fun NSData.toBase64Url(): String {
        val size = length.toInt()
        if (size <= 0) return ""
        val bytes = ByteArray(size)
        memcpy(bytes.refTo(0), this.bytes, length)
        return Base64.UrlSafe.encode(bytes).trim('=')
    }
}

/**
 * Bridges ASAuthorizationController callbacks to the suspending caller. All callback
 * methods are invoked by the system on the main thread.
 */
@OptIn(ExperimentalForeignApi::class)
private class PasskeyAuthorizationDelegate(
    private val onComplete: (ASAuthorization) -> Unit,
    private val onError: (NSError) -> Unit,
    private val presentationAnchor: () -> UIWindow
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization
    ) {
        onComplete(didCompleteWithAuthorization)
    }

    override fun authorizationController(controller: ASAuthorizationController, didCompleteWithError: NSError) {
        onError(didCompleteWithError)
    }

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): UIWindow {
        return presentationAnchor()
    }
}
