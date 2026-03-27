package com.synapse.social.studioasinc.shared.domain.model.business

data class AnalyticsData(
    val profileViews: Int,
    val engagementRate: Float,
    val followerGrowth: List<DataPoint>,
    val topPosts: List<PostAnalytics>
)

data class DataPoint(
    val date: String,
    val value: Float
)

data class PostAnalytics(
    val postId: String,
    val title: String,
    val views: Int
)
