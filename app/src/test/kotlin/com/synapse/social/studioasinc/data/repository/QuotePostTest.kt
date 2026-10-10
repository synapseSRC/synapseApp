package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.data.repository.helpers.PostCrudHelper
import com.synapse.social.studioasinc.data.repository.helpers.PostRepositoryUtils
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.data.local.entity.PostEntity
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class QuotePostTest {

    private val testDispatcher = StandardTestDispatcher()
    private val postDao = mock(PostDao::class.java)
    private val supabaseClient = mock(SupabaseClient::class.java)
    private val offlineActionRepository = mock(OfflineActionRepository::class.java)
    private val postRepositoryUtils = mock(PostRepositoryUtils::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `PostMapper maps quotedPostId and isQuote correctly between Post model and PostEntity`() {
        val originalPost = Post(
            id = "post_100",
            authorUid = "user_1",
            postText = "Original post content",
            username = "original_author"
        )
        val quotePost = Post(
            id = "quote_1",
            authorUid = "user_2",
            postText = "Quoting this!",
            quotedPostId = "post_100",
            quotedPost = originalPost,
            isQuote = true
        )

        val entity = PostMapper.toEntity(quotePost)
        assertEquals("post_100", entity.quotedPostId)
        assertTrue(entity.isQuote)

        val mappedBackModel = PostMapper.toModel(entity)
        assertEquals("post_100", mappedBackModel.quotedPostId)
        assertTrue(mappedBackModel.isQuote)
    }

    @Test
    fun `PostSelectDto toDomain maps nested quoted_post properly`() {
        val quotedUserDto = UserSummaryDto(
            uid = "user_1",
            username = "original_user",
            displayName = "Original User",
            avatarUrl = "avatar.jpg",
            isVerified = true
        )
        val quotedSelectDto = PostSelectDto(
            id = "post_100",
            authorUid = "user_1",
            postText = "Quoted post body",
            timestamp = 1000L,
            user = quotedUserDto
        )
        val quotePostSelectDto = PostSelectDto(
            id = "post_200",
            authorUid = "user_2",
            postText = "My commentary",
            timestamp = 2000L,
            quotedPostId = "post_100",
            quotedPost = quotedSelectDto,
            isQuote = true
        )

        val domainPost = quotePostSelectDto.toDomain({ "http://media/$it" }, { "http://avatar/$it" })

        assertEquals("post_200", domainPost.id)
        assertEquals("post_100", domainPost.quotedPostId)
        assertTrue(domainPost.isQuote)
        assertNotNull(domainPost.quotedPost)
        assertEquals("post_100", domainPost.quotedPost?.id)
        assertEquals("original_user", domainPost.quotedPost?.username)
        assertEquals("Quoted post body", domainPost.quotedPost?.postText)
    }

    @Test
    fun `hydrateQuotedPosts resolves missing quoted post from PostDao`() = runTest {
        val crudHelper = PostCrudHelper(
            postDao = postDao,
            client = supabaseClient,
            offlineActionRepository = offlineActionRepository,
            utils = postRepositoryUtils
        )

        val quotedEntity = PostEntity(
            id = "post_100",
            key = null,
            authorUid = "user_1",
            postText = "Original content",
            postImage = null,
            postType = "TEXT",
            postHideViewsCount = null,
            postHideLikeCount = null,
            postHideCommentsCount = null,
            postDisableComments = null,
            postVisibility = null,
            publishDate = null,
            createdAt = null,
            timestamp = 1000L,
            likesCount = 0,
            commentsCount = 0,
            viewsCount = 0,
            resharesCount = 0,
            mediaItems = null,
            linkPreviews = null,
            isEncrypted = null,
            nonce = null,
            encryptionKeyId = null,
            encryptedContent = null,
            isDeleted = false,
            isEdited = false,
            editedAt = null,
            deletedAt = null,
            hasPoll = false,
            pollQuestion = null,
            pollOptions = null,
            pollEndTime = null,
            pollAllowMultiple = null,
            hasLocation = false,
            locationName = null,
            locationAddress = null,
            locationLatitude = null,
            locationLongitude = null,
            locationPlaceId = null,
            youtubeUrl = null,
            reactions = null,
            userReaction = null,
            username = "alice",
            displayName = "Alice",
            avatarUrl = "alice.jpg",
            isVerified = false,
            userPollVote = null,
            metadata = null,
            quotedPostId = null,
            isQuote = false,
            rootPostId = null
        )

        `when`(postDao.getPostById("post_100")).thenReturn(quotedEntity)

        val unhydratedQuotePost = Post(
            id = "quote_1",
            authorUid = "user_2",
            postText = "Check this out",
            quotedPostId = "post_100",
            quotedPost = null,
            isQuote = true
        )

        val hydratedList = crudHelper.hydrateQuotedPosts(listOf(unhydratedQuotePost))

        assertEquals(1, hydratedList.size)
        val hydratedPost = hydratedList.first()
        assertNotNull(hydratedPost.quotedPost)
        assertEquals("post_100", hydratedPost.quotedPost?.id)
        assertEquals("alice", hydratedPost.quotedPost?.username)
        assertEquals("Original content", hydratedPost.quotedPost?.postText)
    }

    @Test
    fun `PostDetailRepositoryImpl parsePostFromJson handles object, 1-element array, empty array, and null quoted_post defensively`() {
        val detailRepo = PostDetailRepositoryImpl(supabaseClient, mock(ReactionRepositoryImpl::class.java))

        val quotedObj = buildJsonObject {
            put("id", "q_100")
            put("author_uid", "user_q")
            put("post_text", "Quoted text")
        }

        // Case 1: JsonObject
        val dataWithObj = buildJsonObject {
            put("id", "p_1")
            put("author_uid", "user_1")
            put("quoted_post_id", "q_100")
            put("quoted_post", quotedObj)
        }
        val postFromObj = detailRepo.parsePostFromJson(dataWithObj)
        assertNotNull(postFromObj.quotedPost)
        assertEquals("q_100", postFromObj.quotedPost?.id)
        assertEquals("Quoted text", postFromObj.quotedPost?.postText)

        // Case 2: 1-element JsonArray
        val dataWithArray = buildJsonObject {
            put("id", "p_1")
            put("author_uid", "user_1")
            put("quoted_post_id", "q_100")
            put("quoted_post", buildJsonArray { add(quotedObj) })
        }
        val postFromArray = detailRepo.parsePostFromJson(dataWithArray)
        assertNotNull(postFromArray.quotedPost)
        assertEquals("q_100", postFromArray.quotedPost?.id)
        assertEquals("Quoted text", postFromArray.quotedPost?.postText)

        // Case 3: Empty JsonArray
        val dataWithEmptyArray = buildJsonObject {
            put("id", "p_1")
            put("author_uid", "user_1")
            put("quoted_post_id", "q_100")
            put("quoted_post", buildJsonArray { })
        }
        val postFromEmptyArray = detailRepo.parsePostFromJson(dataWithEmptyArray)
        assertNull(postFromEmptyArray.quotedPost)

        // Case 4: JsonNull
        val dataWithNull = buildJsonObject {
            put("id", "p_1")
            put("author_uid", "user_1")
            put("quoted_post_id", "q_100")
            put("quoted_post", JsonNull)
        }
        val postFromNull = detailRepo.parsePostFromJson(dataWithNull)
        assertNull(postFromNull.quotedPost)
    }

    @Test
    fun `ProfilePostsRepositoryImpl parsePost handles object, 1-element array, empty array, and null quoted_post defensively`() {
        val profileRepo = ProfilePostsRepositoryImpl(
            client = supabaseClient,
            commentRepository = mock(CommentRepositoryImpl::class.java),
            constructMediaUrl = { "http://media/$it" },
            constructAvatarUrl = { "http://avatar/$it" },
            resolveUserId = { it }
        )

        val quotedObj = buildJsonObject {
            put("id", "q_100")
            put("author_uid", "user_q")
            put("post_text", "Quoted text")
            put("timestamp", 1000L)
        }

        // Case 1: JsonObject
        val dataWithObj = buildJsonObject {
            put("id", "p_1")
            put("author_uid", "user_1")
            put("timestamp", 2000L)
            put("quoted_post_id", "q_100")
            put("quoted_post", quotedObj)
        }
        val postFromObj = profileRepo.parsePost(dataWithObj)
        assertNotNull(postFromObj?.quotedPost)
        assertEquals("q_100", postFromObj?.quotedPost?.id)
        assertEquals("Quoted text", postFromObj?.quotedPost?.postText)

        // Case 2: 1-element JsonArray
        val dataWithArray = buildJsonObject {
            put("id", "p_1")
            put("author_uid", "user_1")
            put("timestamp", 2000L)
            put("quoted_post_id", "q_100")
            put("quoted_post", buildJsonArray { add(quotedObj) })
        }
        val postFromArray = profileRepo.parsePost(dataWithArray)
        assertNotNull(postFromArray?.quotedPost)
        assertEquals("q_100", postFromArray?.quotedPost?.id)
        assertEquals("Quoted text", postFromArray?.quotedPost?.postText)

        // Case 3: Empty JsonArray
        val dataWithEmptyArray = buildJsonObject {
            put("id", "p_1")
            put("author_uid", "user_1")
            put("timestamp", 2000L)
            put("quoted_post_id", "q_100")
            put("quoted_post", buildJsonArray { })
        }
        val postFromEmptyArray = profileRepo.parsePost(dataWithEmptyArray)
        assertNull(postFromEmptyArray?.quotedPost)

        // Case 4: JsonNull
        val dataWithNull = buildJsonObject {
            put("id", "p_1")
            put("author_uid", "user_1")
            put("timestamp", 2000L)
            put("quoted_post_id", "q_100")
            put("quoted_post", JsonNull)
        }
        val postFromNull = profileRepo.parsePost(dataWithNull)
        assertNull(postFromNull?.quotedPost)
    }
}
