package ru.netology.nmedia.model

import ru.netology.nmedia.dto.Post

data class FeedModel(
    val posts: List<Post> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val empty: Boolean = false,
    val refreshing: Boolean = false,
    val response: FeedResponse = FeedResponse()
)

data class FeedResponse(
    val code: Int = 0,
    val message: String? = null
)
