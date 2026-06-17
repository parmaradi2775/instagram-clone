package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class InstaTab {
    HOME, SEARCH, CREATE, REELS, PROFILE
}

@OptIn(ExperimentalCoroutinesApi::class)
class InstagramViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InstagramDatabase.getDatabase(application)
    private val repository = InstagramRepository(db.dao)

    // Current selected tab
    private val _currentTab = MutableStateFlow(InstaTab.HOME)
    val currentTab: StateFlow<InstaTab> = _currentTab.asStateFlow()

    // Posts & Stories from Room
    val posts: StateFlow<List<PostEntity>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stories: StateFlow<List<StoryEntity>> = repository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered posts for search screen
    val filteredPosts: StateFlow<List<PostEntity>> = combine(posts, searchQuery) { postList, query ->
        if (query.isBlank()) {
            postList
        } else {
            postList.filter {
                it.caption.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true) ||
                it.authorName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active post for viewing comments
    private val _activePostForComments = MutableStateFlow<PostEntity?>(null)
    val activePostForComments: StateFlow<PostEntity?> = _activePostForComments.asStateFlow()

    // Commments for the active post
    val activeComments: StateFlow<List<CommentEntity>> = _activePostForComments
        .flatMapLatest { post ->
            if (post == null) flowOf(emptyList())
            else repository.getCommentsForPost(post.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active story slider index (null if not viewing stories)
    private val _activeStoryIndex = MutableStateFlow<Int?>(null)
    val activeStoryIndex: StateFlow<Int?> = _activeStoryIndex.asStateFlow()

    // Post template choices
    val postTemplates = listOf(
        R.drawable.img_post_travel,
        R.drawable.img_post_cafe,
        R.drawable.img_post_creative
    )

    // Create post state
    private val _selectedTemplateIndex = MutableStateFlow(0)
    val selectedTemplateIndex: StateFlow<Int> = _selectedTemplateIndex.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Normal")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    val availableFilters = listOf("Normal", "Clarendon", "Lark", "Juno", "Monochrome")

    // Reels simulation dataset (Interactive video posts)
    private val _reels = MutableStateFlow(getReelsMockData())
    val reels: StateFlow<List<ReelItem>> = _reels.asStateFlow()

    // Current User Profile details (simulated, can be updated)
    private val _userProfile = MutableStateFlow(
        UserProfile(
            username = "alex_traveler_26",
            name = "Alex Mercer",
            avatarResId = R.drawable.img_avatar_admin,
            bio = "Visual storyteller & local explorer. Capturing the aesthetic details of urban design, cozy cafe corners, and tropical ocean horizons.",
            followersCount = 1420,
            followingCount = 482
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    init {
        // Seed database
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }
    }

    // Tab control
    fun selectTab(tab: InstaTab) {
        _currentTab.value = tab
    }

    // Likes
    fun toggleLike(postId: Int) {
        viewModelScope.launch {
            repository.toggleLike(postId)
            // Update comments preview holder if it matches
            val currentActive = _activePostForComments.value
            if (currentActive != null && currentActive.id == postId) {
                val dbPost = db.dao.getPostById(postId)
                _activePostForComments.value = dbPost
            }
        }
    }

    // Comments bottom sheet
    fun showCommentsForPost(post: PostEntity) {
        _activePostForComments.value = post
    }

    fun dismissComments() {
        _activePostForComments.value = null
    }

    fun addCommentToPost(postId: Int, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.addComment(postId, "you", content)
        }
    }

    // Stories control
    fun startStoryViewing(index: Int) {
        _activeStoryIndex.value = index
        markActiveStoryViewed()
    }

    fun nextStory() {
        val currentIndex = _activeStoryIndex.value ?: return
        val totalStories = stories.value.size
        if (currentIndex < totalStories - 1) {
            _activeStoryIndex.value = currentIndex + 1
            markActiveStoryViewed()
        } else {
            _activeStoryIndex.value = null // Close stories
        }
    }

    fun previousStory() {
        val currentIndex = _activeStoryIndex.value ?: return
        if (currentIndex > 0) {
            _activeStoryIndex.value = currentIndex - 1
            markActiveStoryViewed()
        } else {
            _activeStoryIndex.value = null // Close stories
        }
    }

    fun closeStories() {
        _activeStoryIndex.value = null
    }

    private fun markActiveStoryViewed() {
        val index = _activeStoryIndex.value ?: return
        val storyList = stories.value
        if (index in storyList.indices) {
            val story = storyList[index]
            viewModelScope.launch {
                repository.markStoryAsViewed(story.id)
            }
        }
    }

    // Post Creation options
    fun selectTemplateIndex(index: Int) {
        _selectedTemplateIndex.value = index
    }

    fun selectFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun publishNewPost(caption: String, location: String) {
        val templateResId = postTemplates.getOrElse(selectedTemplateIndex.value) { R.drawable.img_post_travel }
        val finalCaption = if (caption.isNotBlank()) caption else "No caption."
        val finalLocation = if (location.isNotBlank()) location else "My World"

        // Let's pack the filter name into the caption if we want, or just apply it visual
        // We'll store filter info or just a custom identifier
        val captionedWithFilter = if (selectedFilter.value == "Normal") finalCaption else "$finalCaption [Filter: ${selectedFilter.value}]"

        viewModelScope.launch {
            repository.createPost(
                authorName = "you",
                authorAvatarResId = R.drawable.img_avatar_admin,
                imageResId = templateResId,
                gradientStartHex = getFilterGradientStart(selectedFilter.value),
                gradientEndHex = getFilterGradientEnd(selectedFilter.value),
                location = finalLocation,
                caption = captionedWithFilter
            )
            // Reset create state & redirect Home
            _selectedTemplateIndex.value = 0
            _selectedFilter.value = "Normal"
            _currentTab.value = InstaTab.HOME
        }
    }

    private fun getFilterGradientStart(filterName: String): String? {
        return when (filterName) {
            "Clarendon" -> "#00B4DB"
            "Lark" -> "#FF8C00"
            "Juno" -> "#f107a3"
            "Monochrome" -> "#606060"
            else -> null
        }
    }

    private fun getFilterGradientEnd(filterName: String): String? {
        return when (filterName) {
            "Clarendon" -> "#0083B0"
            "Lark" -> "#e52d27"
            "Juno" -> "#7b2ff7"
            "Monochrome" -> "#202020"
            else -> null
        }
    }

    // Update Profile details
    fun updateProfile(name: String, bio: String) {
        if (name.isBlank()) return
        _userProfile.value = _userProfile.value.copy(
            name = name,
            bio = bio
        )
    }

    // Search query update
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Active Reels feedback logic
    fun toggleReelLike(reelId: Int) {
        val list = _reels.value.map {
            if (it.id == reelId) {
                it.copy(
                    isLiked = !it.isLiked,
                    likesCount = if (it.isLiked) it.likesCount - 1 else it.likesCount + 1
                )
            } else it
        }
        _reels.value = list
    }

    fun addReelComment(reelId: Int, text: String) {
        if (text.isBlank()) return
        val list = _reels.value.map {
            if (it.id == reelId) {
                it.copy(
                    commentsList = it.commentsList + ReelComment("you", text)
                )
            } else it
        }
        _reels.value = list
    }

    // Static Mock reels datasets
    private fun getReelsMockData(): List<ReelItem> {
        return listOf(
            ReelItem(
                id = 11,
                authorName = "cozy_cafes",
                authorAvatarResId = R.drawable.img_post_cafe,
                musicName = "Chill Jazz Cafe Vibes • Original Audio",
                description = "Pouring the perfect cold brew on an afternoon in Tokyo. Who wants a cup? ☕️✨ #cafehop #tokyofoodie",
                likesCount = 2390,
                isLiked = false,
                imageResId = R.drawable.img_post_cafe,
                gradientHexStart = "#FFB347",
                gradientHexEnd = "#F07167",
                commentsList = listOf(
                    ReelComment("nature_guy", "Unbelievable aesthetic! Warm, cozy, peaceful."),
                    ReelComment("coffee_drinker", "What beans are you using? Looks so delicious.")
                )
            ),
            ReelItem(
                id = 12,
                authorName = "nature_hiker",
                authorAvatarResId = R.drawable.img_post_travel,
                musicName = "Suns out, funs out! • Summer Mix",
                description = "Paradise found! Swimming through the clearest lagoons in Bora Bora. Tag your travel bestie! 🌊🥥⛵️ #wanderlust #beachtrip",
                likesCount = 5902,
                isLiked = true,
                imageResId = R.drawable.img_post_travel,
                gradientHexStart = "#4CA1AF",
                gradientHexEnd = "#2C3E50",
                commentsList = listOf(
                    ReelComment("explorer_alex", "Adding to my bucket list immediately!"),
                    ReelComment("sunset_lover", "This water transparency is unreal!")
                )
            ),
            ReelItem(
                id = 13,
                authorName = "neon_architect",
                authorAvatarResId = R.drawable.img_post_creative,
                musicName = "Synthwave Odyssey • Retro 1984",
                description = "Walking through neon tunnels at midnight. The city beats like a heart of glass. 🌃🎨💥 #retrowave #neonlights",
                likesCount = 1432,
                isLiked = false,
                imageResId = R.drawable.img_post_creative,
                gradientHexStart = "#5433FF",
                gradientHexEnd = "#A5FECB",
                commentsList = listOf(
                    ReelComment("pixel_pioneer", "This is an absolute masterpieces!"),
                    ReelComment("lofi_beats", "Perfect background for a synth track.")
                )
            )
        )
    }
}

// Sub data models
data class UserProfile(
    val username: String,
    val name: String,
    val avatarResId: Int,
    val bio: String,
    val followersCount: Int,
    val followingCount: Int
)

data class ReelItem(
    val id: Int,
    val authorName: String,
    val authorAvatarResId: Int,
    val musicName: String,
    val description: String,
    val likesCount: Int,
    val isLiked: Boolean,
    val imageResId: Int,
    val gradientHexStart: String? = null,
    val gradientHexEnd: String? = null,
    val commentsList: List<ReelComment> = emptyList()
)

data class ReelComment(
    val authorName: String,
    val text: String
)
