package com.synapse.social.studioasinc.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.synapse.social.studioasinc.shared.data.database.Post
import com.synapse.social.studioasinc.shared.data.database.Comment
import com.synapse.social.studioasinc.shared.data.database.User
import com.synapse.social.studioasinc.shared.data.database.*
import com.synapse.social.studioasinc.shared.data.local.database.CommentDao
import com.synapse.social.studioasinc.shared.data.local.database.SqlDelightCommentDao
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.data.local.database.SqlDelightPostDao
import com.synapse.social.studioasinc.shared.data.local.database.PendingActionDao
import com.synapse.social.studioasinc.shared.data.local.database.SqlDelightPendingActionDao
import com.synapse.social.studioasinc.shared.data.local.database.DraftDao
import com.synapse.social.studioasinc.shared.data.local.database.SqlDelightDraftDao
import com.synapse.social.studioasinc.shared.data.local.database.UserDao
import com.synapse.social.studioasinc.shared.data.local.database.UserDaoImpl
import com.synapse.social.studioasinc.shared.security.SecurityCipher
import com.synapse.social.studioasinc.shared.security.AndroidSecurityCipher
import com.synapse.social.studioasinc.shared.data.adapter.EncryptedStringAdapter

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideCommentDao(db: StorageDatabase): CommentDao {
        return SqlDelightCommentDao(db)
    }

    @Provides
    fun providePendingActionDao(db: StorageDatabase): PendingActionDao {
        return SqlDelightPendingActionDao(db)
    }

    @Provides
    fun provideDraftDao(db: StorageDatabase): DraftDao {
        return SqlDelightDraftDao(db)
    }

    @Provides
    fun providePostDao(db: StorageDatabase): PostDao {
        return SqlDelightPostDao(db)
    }

    @Provides
    fun provideUserDao(db: StorageDatabase): UserDao {
        return UserDaoImpl(db)
    }

    @Provides
    @Singleton
    fun provideSecurityCipher(): SecurityCipher {
        return AndroidSecurityCipher()
    }

    @Provides
    @Singleton
    fun provideStorageDatabase(@ApplicationContext context: Context, securityCipher: SecurityCipher): StorageDatabase {
        val driver = AndroidSqliteDriver(StorageDatabase.Schema, context, "storage.db")
        return StorageDatabase(
            driver = driver,
            PostAdapter = Post.Adapter(
                mediaItemsAdapter = mediaItemListAdapter,
                pollOptionsAdapter = pollOptionListAdapter,
                reactionsAdapter = reactionMapAdapter,
                userReactionAdapter = reactionTypeAdapter,
                metadataAdapter = postMetadataAdapter,
                likesCountAdapter = intAdapter,
                commentsCountAdapter = intAdapter,
                viewsCountAdapter = intAdapter,
                resharesCountAdapter = intAdapter,
                userPollVoteAdapter = intAdapter,
                encryptedContentAdapter = stringMapAdapter
            ),
            CommentAdapter = Comment.Adapter(
                likesCountAdapter = intAdapter,
                repliesCountAdapter = intAdapter,
            ),
            UserAdapter = User.Adapter(
                emailAdapter = EncryptedStringAdapter(securityCipher),
                followersCountAdapter = intAdapter,
                followingCountAdapter = intAdapter,
                postsCountAdapter = intAdapter
            ),
            PendingActionAdapter = com.synapse.social.studioasinc.shared.data.database.PendingAction.Adapter(
                retryCountAdapter = intAdapter
            )
        )
    }
}
