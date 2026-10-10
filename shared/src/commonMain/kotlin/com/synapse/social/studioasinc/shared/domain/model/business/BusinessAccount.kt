package com.synapse.social.studioasinc.shared.domain.model.business

enum class AccountType {
    PERSONAL, CREATOR, BUSINESS
}

enum class VerificationStatus {
    NOT_APPLIED, PENDING, VERIFIED, REJECTED
}

data class BusinessAccount(
    val userId: String,
    val accountType: AccountType,
    val monetizationEnabled: Boolean,
    val verificationStatus: VerificationStatus
)
