package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.api.ApiPosts
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException


class PostRepositoryImpl(private val postDao: PostDao) : PostRepository {
    override val posts: LiveData<List<Post>> = postDao.getAll().map(List<PostEntity>::toDto)

//    private fun getById(id: Long, callback: PostRepository.ResponseCallback<Post>) {
//        ApiPosts.retrofitService.getById(id).enqueue(object : Callback<Post> {
//            override fun onResponse(call: Call<Post>, response: Response<Post>) {
//                if (!response.isSuccessful) {
//                    callback.onError(response.code(), response.message())
//                } else {
//                    callback.onSuccess(requireNotNull(response.body()) { "body is null" })
//                }
//            }
//
//            override fun onFailure(call: Call<Post>, t: Throwable) {
//                callback.onFailure(Exception(t))
//            }
//        })
//    }

    override suspend fun getAll() {
        try {
            val response = ApiPosts.retrofitService.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun save(post: Post) {
        try {
            val response = ApiPosts.retrofitService.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        val postRemoved = posts.value?.firstOrNull { it.id == id }
            ?: throw RuntimeException("Failed to find a post by ID")
        postDao.removeById(id)
        try {
            val response = ApiPosts.retrofitService.removeById(id)
            if (!response.isSuccessful) {
                postDao.insert(PostEntity.fromDto(postRemoved)) // recover previous state
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            postDao.insert(PostEntity.fromDto(postRemoved)) // recover previous state
            throw NetworkError
        } catch (e: Exception) {
            postDao.insert(PostEntity.fromDto(postRemoved)) // recover previous state
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        val post = posts.value?.firstOrNull { it.id == id }
            ?: throw RuntimeException("Failed to find a post by ID")
        postDao.likeById(id)
        try {
            val response = if (!post.likedByMe)
                ApiPosts.retrofitService.likeById(id)
            else
                ApiPosts.retrofitService.dislikeById(id)
            if (!response.isSuccessful) {
                postDao.likeById(id)    // recover previous state
                throw ApiError(response.code(), response.message())
            }
            if (response.body() == null) {
                postDao.likeById(id)    // recover previous state
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            postDao.likeById(id)    // recover previous state
            throw NetworkError
        } catch (e: Exception) {
            postDao.likeById(id)    // recover previous state
            throw UnknownError
        }
    }
}
