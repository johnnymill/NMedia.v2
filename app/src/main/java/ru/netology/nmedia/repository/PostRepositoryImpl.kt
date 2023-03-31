package ru.netology.nmedia.repository

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.ApiPosts
import ru.netology.nmedia.dto.Post


class PostRepositoryImpl : PostRepository {

    private fun getById(id: Long, callback: PostRepository.ResponseCallback<Post>) {
        ApiPosts.retrofitService.getById(id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful) {
                    callback.onError(response.code(), response.message())
                } else {
                    callback.onSuccess(requireNotNull(response.body()) { "body is null" })
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onFailure(Exception(t))
            }
        })
    }

    override fun getAll(callback: PostRepository.ResponseCallback<List<Post>>) {
        ApiPosts.retrofitService.getAll().enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (!response.isSuccessful) {
                    callback.onError(response.code(), response.message())
                } else {
                    callback.onSuccess(requireNotNull(response.body()) { "body is null" })
                }
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                callback.onFailure(Exception(t))
            }
        })
    }

    override fun save(post: Post, callback: PostRepository.ResponseCallback<Post>) {
        ApiPosts.retrofitService.save(post).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful) {
                    callback.onError(response.code(), response.message())
                } else {
                    callback.onSuccess(requireNotNull(response.body()) { "body is null" })
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onFailure(Exception(t))
            }
        })
    }

    override fun removeById(id: Long, callback: PostRepository.ResponseCallback<Unit>) {
        ApiPosts.retrofitService.removeById(id).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (!response.isSuccessful) {
                    callback.onError(response.code(), response.message())
                } else {
                    callback.onSuccess(requireNotNull(response.body()) { "body is null" })
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                callback.onFailure(Exception(t))
            }
        })
    }

    override fun likeById(id: Long, callback: PostRepository.ResponseCallback<Post>) {
        ApiPosts.retrofitService.likeById(id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful) {
                    callback.onError(response.code(), response.message())
                } else {
                    callback.onSuccess(requireNotNull(response.body()) { "body is null" })
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onFailure(Exception(t))
            }
        })
    }

    override fun dislikeById(id: Long, callback: PostRepository.ResponseCallback<Post>) {
        ApiPosts.retrofitService.dislikeById(id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful) {
                    callback.onError(response.code(), response.message())
                } else {
                    callback.onSuccess(requireNotNull(response.body()) { "body is null" })
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onFailure(Exception(t))
            }
        })
    }
}
