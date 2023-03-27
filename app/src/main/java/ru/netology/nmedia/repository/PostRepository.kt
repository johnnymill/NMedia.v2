package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(callback: ResponseCallback<List<Post>>)
    fun save(post: Post, callback: ResponseCallback<Post>)
    fun removeById(id: Long, callback: ResponseCallback<Unit>)
    fun likeById(id: Long, callback: ResponseCallback<Post>)
    fun dislikeById(id: Long, callback: ResponseCallback<Post>)

    interface ResponseCallback<T> {
        fun onSuccess(result: T) {}
        fun onError(e: Exception) {}
    }
}
