package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.database.User as DbUser
import com.synapse.social.studioasinc.domain.model.User as DomainUser
import com.synapse.social.studioasinc.shared.data.local.database.UserEntity
import com.synapse.social.studioasinc.shared.data.model.EncryptedString

object UserMapper {

    fun toEntity(user: DomainUser): DbUser {
        return DbUser(
            id = user.uid,
            username = user.username ?: "",
            email = user.email?.let { EncryptedString(it) },
            fullName = user.displayName,
            avatarUrl = user.avatar,
            bio = user.bio,
            website = null,
            location = null,
            isVerified = user.verify,
            followersCount = user.followersCount,
            followingCount = user.followingCount,
            postsCount = user.postsCount
        )
    }

    fun toUserEntity(user: DomainUser): UserEntity {
        return UserEntity(
            uid = user.uid,
            username = user.username,
            email = user.email,
            fullName = user.displayName,
            avatarUrl = user.avatar,
            bio = user.bio,
            website = null,
            location = null,
            isVerified = user.verify,
            followersCount = user.followersCount,
            followingCount = user.followingCount,
            postsCount = user.postsCount
        )
    }

    fun toModel(entity: DbUser): DomainUser {
        return DomainUser(
            uid = entity.id,
            username = entity.username,
            displayName = entity.fullName,
            email = entity.email?.value,
            avatar = entity.avatarUrl?.let { url ->
                if (url.startsWith("http")) url else SupabaseClient.constructAvatarUrl(url)
            },
            verify = entity.isVerified,
            bio = entity.bio,
            followersCount = entity.followersCount,
            followingCount = entity.followingCount,
            postsCount = entity.postsCount
        )
    }

    fun toModel(entity: UserEntity): DomainUser {
        return DomainUser(
            uid = entity.uid,
            username = entity.username,
            displayName = entity.fullName,
            email = entity.email,
            avatar = entity.avatarUrl?.let { url ->
                if (url.startsWith("http")) url else SupabaseClient.constructAvatarUrl(url)
            },
            verify = entity.isVerified,
            bio = entity.bio,
            followersCount = entity.followersCount,
            followingCount = entity.followingCount,
            postsCount = entity.postsCount
        )
    }
}
