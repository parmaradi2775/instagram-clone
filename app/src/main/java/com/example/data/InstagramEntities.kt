package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorName: String,
    val authorAvatarResId: Int,
    val imageResId: Int, // positive drawable ID or 0 for custom gradient
    val gradientStartHex: String? = null,
    val gradientEndHex: String? = null,
    val location: String = "",
    val caption: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val authorName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorName: String,
    val authorAvatarResId: Int,
    val storyImageResId: Int,
    val isViewed: Boolean = false,
    val caption: String = ""
)
