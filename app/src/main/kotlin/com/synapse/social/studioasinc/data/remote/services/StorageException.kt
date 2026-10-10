package com.synapse.social.studioasinc.data.remote.services

sealed class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class FileNotFound(message: String) : StorageException(message)
    class InvalidFile(message: String) : StorageException(message)
    class InvalidUrl(message: String) : StorageException(message)
    class InvalidPath(message: String) : StorageException(message)
    class NetworkError(message: String) : StorageException(message)
    class AuthenticationError(message: String) : StorageException(message)
    class StorageQuotaError(message: String) : StorageException(message)
    class ServerError(message: String) : StorageException(message)
    class EmptyFile(message: String) : StorageException(message)
    class UnknownError(message: String) : StorageException(message)
}
