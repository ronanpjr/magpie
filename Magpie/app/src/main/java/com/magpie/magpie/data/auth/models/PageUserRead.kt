package com.magpie.magpie.data.auth.models

import com.squareup.moshi.Json

data class PageUserRead(
    @Json(name = "items")
    val items: List<UserRead>,
    @Json(name = "total")
    val total: Int,
    @Json(name = "page")
    val page: Int,
    @Json(name = "limit")
    val limit: Int,
    @Json(name = "pages")
    val pages: Int
)
