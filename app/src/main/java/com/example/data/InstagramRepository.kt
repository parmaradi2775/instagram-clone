package com.example.data

import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class InstagramRepository(private val dao: InstagramDao) {

    val allPosts: Flow<List<PostEntity>> = dao.getAllPosts()
    val allStories: Flow<List<StoryEntity>> = dao.getAllStories()

    fun getCommentsForPost(postId: Int): Flow<List<CommentEntity>> {
        return dao.getCommentsForPost(postId)
    }

    fun getCommentCountForPost(postId: Int): Flow<Int> {
        return dao.getCommentCountForPost(postId)
    }

    suspend fun createPost(
        authorName: String,
        authorAvatarResId: Int,
        imageResId: Int,
        gradientStartHex: String? = null,
        gradientEndHex: String? = null,
        location: String,
        caption: String
    ) = withContext(Dispatchers.IO) {
        val post = PostEntity(
            authorName = authorName,
            authorAvatarResId = authorAvatarResId,
            imageResId = imageResId,
            gradientStartHex = gradientStartHex,
            gradientEndHex = gradientEndHex,
            location = location,
            caption = caption,
            likesCount = 0,
            isLiked = false
        )
        dao.insertPost(post)
    }

    suspend fun addComment(postId: Int, authorName: String, content: String) = withContext(Dispatchers.IO) {
        val comment = CommentEntity(
            postId = postId,
            authorName = authorName,
            content = content
        )
        dao.insertComment(comment)
    }

    suspend fun toggleLike(postId: Int) = withContext(Dispatchers.IO) {
        val post = dao.getPostById(postId) ?: return@withContext
        val updatedPost = post.copy(
            isLiked = !post.isLiked,
            likesCount = if (post.isLiked) post.likesCount - 1 else post.likesCount + 1
        )
        dao.updatePost(updatedPost)
    }

    suspend fun markStoryAsViewed(storyId: Int) = withContext(Dispatchers.IO) {
        val list = dao.getAllStories().first()
        val story = list.find { it.id == storyId } ?: return@withContext
        dao.updateStory(story.copy(isViewed = true))
    }

    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        val existingPosts = dao.getAllPosts().first()
        if (existingPosts.isEmpty()) {
            // Seed Stories
            dao.insertStory(
                StoryEntity(
                    authorName = "you",
                    authorAvatarResId = R.drawable.img_avatar_admin,
                    storyImageResId = R.drawable.img_post_travel,
                    isViewed = false,
                    caption = "Beach days are the best days! 🏖️"
                )
            )
            dao.insertStory(
                StoryEntity(
                    authorName = "travel_explorer",
                    authorAvatarResId = R.drawable.img_post_travel,
                    storyImageResId = R.drawable.img_post_cafe,
                    isViewed = false,
                    caption = "Tokyo mornings... 🇯🇵"
                )
            )
            dao.insertStory(
                StoryEntity(
                    authorName = "cafe_lover",
                    authorAvatarResId = R.drawable.img_post_cafe,
                    storyImageResId = R.drawable.img_post_creative,
                    isViewed = false,
                    caption = "Aesthetic vibes only ✨"
                )
            )
            dao.insertStory(
                StoryEntity(
                    authorName = "neon_designer",
                    authorAvatarResId = R.drawable.img_post_creative,
                    storyImageResId = R.drawable.img_post_travel,
                    isViewed = false,
                    caption = "Digital futures exhibition 💫"
                )
            )

            // Seed Post 1
            val post1Id = dao.insertPost(
                PostEntity(
                    authorName = "travel_explorer",
                    authorAvatarResId = R.drawable.img_post_travel,
                    imageResId = R.drawable.img_post_travel,
                    location = "Bora Bora, French Polynesia",
                    caption = "Unbelievable sunsets here in Bora Bora. Waking up to the sound of turquoise waves crashing and the sun gently dipping below the horizon. Truly a slice of heaven. 🌅🌴 #oceanview #travel #summervibes",
                    likesCount = 1245,
                    isLiked = false
                )
            ).toInt()

            dao.insertComment(
                CommentEntity(
                    postId = post1Id,
                    authorName = "cafe_lover",
                    content = "Omg please take me with you next time!!"
                )
            )
            dao.insertComment(
                CommentEntity(
                    postId = post1Id,
                    authorName = "neon_designer",
                    content = "The color palette in this photo is insane. Composition is 10/10."
                )
            )

            // Seed Post 2
            val post2Id = dao.insertPost(
                PostEntity(
                    authorName = "cafe_lover",
                    authorAvatarResId = R.drawable.img_post_cafe,
                    imageResId = R.drawable.img_post_cafe,
                    location = "Matcha House, Tokyo",
                    caption = "Nothing beats a fresh butter croissant and a hot matcha latte on a rainy Wednesday morning. Finding peace in small, quiet moments. ☕️🥐🍵 #aesthetic #cafelife #tokyoeats",
                    likesCount = 832,
                    isLiked = false
                )
            ).toInt()

            dao.insertComment(
                CommentEntity(
                    postId = post2Id,
                    authorName = "you",
                    content = "That matcha espresso art looks too pretty to drink!"
                )
            )
            dao.insertComment(
                CommentEntity(
                    postId = post2Id,
                    authorName = "travel_explorer",
                    content = "Craving classic pastries so badly now! 🥐"
                )
            )

            // Seed Post 3
            val post3Id = dao.insertPost(
                PostEntity(
                    authorName = "neon_designer",
                    authorAvatarResId = R.drawable.img_post_creative,
                    imageResId = R.drawable.img_post_creative,
                    location = "Museum of Digital Arts",
                    caption = "Exploring the boundary of physical space and luminous spectrums at the cyber installation. These neon structures respond dynamically to acoustic resonance. Completely mesmerized! 👾✨ #neon #cyberpunk #modernart #exhibition",
                    likesCount = 421,
                    isLiked = false
                )
            ).toInt()

            dao.insertComment(
                CommentEntity(
                    postId = post3Id,
                    authorName = "cafe_lover",
                    content = "This looks incredibly futuristic!"
                )
            )
            dao.insertComment(
                CommentEntity(
                    postId = post3Id,
                    authorName = "travel_explorer",
                    content = "Wow, where is this located exactly?"
                )
            )
        }
    }
}
