package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val posts: Flow<List<Post>>

    suspend fun getAll()
    suspend fun save(post: Post)
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long)
}
