package com.moneymanager.app.data.network

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface NotionApiService {
    @Headers("Notion-Version: 2022-06-28")
    @POST("v1/pages")
    suspend fun createPage(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @Headers("Notion-Version: 2022-06-28")
    @PATCH("v1/pages/{page_id}")
    suspend fun updatePage(
        @Header("Authorization") token: String,
        @Path("page_id") pageId: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @Headers("Notion-Version: 2022-06-28")
    @POST("v1/databases/{database_id}/query")
    suspend fun queryDatabase(
        @Header("Authorization") token: String,
        @Path("database_id") databaseId: String,
        @Body body: JsonObject
    ): Response<JsonObject>
}
