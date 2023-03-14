package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(callback: ResponseCallback<List<Post>>)
    fun likeById(id: Long, callback: ResponseCallback<Post>)
    fun save(post: Post, callback: ResponseCallback<Post>)
    fun removeById(id: Long, callback: ResponseCallback<Any>)

    interface ResponseCallback<T> {
        fun onSuccess(result: T) {}
        fun onError(e: Exception) {}
    }
}
