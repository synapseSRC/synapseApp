package com.synapse.social.studioasinc.shared.data.repository
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.model.BusinessAccountDto
import com.synapse.social.studioasinc.shared.domain.model.business.AccountType
import com.synapse.social.studioasinc.shared.domain.model.business.AnalyticsData
import com.synapse.social.studioasinc.shared.domain.model.business.BusinessAccount
import com.synapse.social.studioasinc.shared.domain.model.business.DataPoint
import com.synapse.social.studioasinc.shared.domain.model.business.PostAnalytics
import com.synapse.social.studioasinc.shared.domain.model.business.RevenueData
import com.synapse.social.studioasinc.shared.domain.model.business.VerificationStatus
import com.synapse.social.studioasinc.shared.domain.repository.BusinessRepository
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.synapse.social.studioasinc.shared.util.TimeProvider

class BusinessRepositoryImpl(
    private val client: SupabaseClientType = SupabaseClient.client
) : BusinessRepository {

    override suspend fun getBusinessAccount(userId: String): Result<BusinessAccount?> = withContext(AppDispatchers.IO) {
        runCatching {
            val dto = client.postgrest["business_accounts"].select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeSingleOrNull<BusinessAccountDto>()

            dto?.let {
                BusinessAccount(
                    userId = it.user_id,
                    accountType = try {
                        AccountType.valueOf(it.account_type.uppercase())
                    } catch (e: Exception) {
                        AccountType.PERSONAL
                    },
                    monetizationEnabled = it.monetization_enabled,
                    verificationStatus = try {
                        VerificationStatus.valueOf(it.verification_status.uppercase())
                    } catch (e: Exception) {
                        VerificationStatus.NOT_APPLIED
                    }
                )
            }
        }
    }

    override suspend fun createBusinessAccount(userId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        runCatching {
            val newAccount = BusinessAccountDto(
                user_id = userId,
                account_type = "BUSINESS",
                monetization_enabled = false,
                verification_status = "NOT_APPLIED"
            )
            client.postgrest["business_accounts"].upsert(newAccount)
            Unit
        }
    }

    override suspend fun updateMonetization(userId: String, enabled: Boolean): Result<Unit> = withContext(AppDispatchers.IO) {
        runCatching {
            client.postgrest["business_accounts"].update(
                {
                    set("monetization_enabled", enabled)
                }
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            Unit
        }
    }

    override suspend fun applyForVerification(userId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        runCatching {
            client.postgrest["business_accounts"].update(
                {
                    set("verification_status", "PENDING")
                }
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            Unit
        }
    }

    override suspend fun getAnalytics(userId: String): Result<AnalyticsData> = withContext(AppDispatchers.IO) {
        runCatching {
            // Mock data as per original ViewModel
             val points = (0..6).map { dayOffset ->
                 DataPoint(
                     date = "2024-01-${dayOffset + 1}",
                     value = (100..500).random().toFloat()
                 )
            }

            AnalyticsData(
                profileViews = 1250,
                engagementRate = 4.5f,
                followerGrowth = points,
                topPosts = listOf(
                    PostAnalytics("1", "Summer Vibes", 5000),
                    PostAnalytics("2", "Tech Talk", 3200),
                    PostAnalytics("3", "Tutorial", 2800)
                )
            )
        }
    }

    override suspend fun getRevenue(userId: String): Result<RevenueData> = withContext(AppDispatchers.IO) {
        runCatching {
             // Mock data as per original ViewModel
            RevenueData(
                totalEarnings = 1500.00,
                pendingPayout = 250.00,
                lastPayoutDate = TimeProvider.nowMillis() - 86400000 * 5
            )
        }
    }
}
