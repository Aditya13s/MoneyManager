package com.moneymanager.app.data.network

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface NotionApiService {
    @POST("v1/pages")
    suspend fun createPage(
        @Header("Authorization") token: String,
        @Header("Notion-Version") version: String = "2022-06-28",
        @Body body: JsonObject
    ): Response<JsonObject>

    @PATCH("v1/pages/{page_id}")
    suspend fun updatePage(
        @Header("Authorization") token: String,
        @Header("Notion-Version") version: String = "2022-06-28",
        @Path("page_id") pageId: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @POST("v1/databases/{database_id}/query")
    suspend fun queryDatabase(
        @Header("Authorization") token: String,
        @Header("Notion-Version") version: String = "2022-06-28",
        @Path("database_id") databaseId: String,
        @Body body: JsonObject
    ): Response<JsonObject>
}
