# Magpie — Android Frontend Specification

> Complete implementation guide for a development agent.
> Read in full before writing any code. The backend OpenAPI contract is the source of truth
> for all network calls — never assume field names or response shapes.

---

## 1. Overview

Native Android application (Kotlin) for Magpie, a music review platform.
Three developers work in parallel, each owning a vertical slice end-to-end
(UI → ViewModel → Repository → Room/Network).

**What the app does:**
- Browse and search a music catalog (artists, albums, tracks)
- Read and write reviews (0–5 stars, optional text body)
- Like other users' reviews
- Follow other users, see a personalized feed
- View profiles (own and others')

**Academic requirements this spec satisfies:**
- R1: Material Design 3 identity with a defined color palette
- R2: 10 screens (≥ 3 per developer)
- R3: Real network access via Retrofit to the FastAPI backend
- R4: Android Room for local persistence (session + catalog cache + review draft queue)
- R5: Full i18n — Portuguese (default) and English, zero hardcoded strings
- R6: MVVM architecture, Repository pattern, automated tests per module

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Repository |
| Navigation | Navigation Compose (single Activity) |
| Networking | Retrofit 2 + OkHttp + Moshi (or kotlinx.serialization) |
| Local DB | Room |
| DI | Hilt |
| Image loading | Coil |
| Auth token storage | EncryptedSharedPreferences |
| State management | StateFlow + collectAsStateWithLifecycle |
| Testing | JUnit 4, MockWebServer (OkHttp), Room in-memory |
| i18n | `strings.xml` in `values/` (pt-BR) and `values-en/` (en) |

### `build.gradle` dependencies (key ones)

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.x.x"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui")
implementation("androidx.navigation:navigation-compose:2.x.x")
implementation("androidx.hilt:hilt-navigation-compose:1.x.x")
implementation("com.google.dagger:hilt-android:2.x.x")
implementation("androidx.room:room-runtime:2.x.x")
implementation("androidx.room:room-ktx:2.x.x")
implementation("com.squareup.retrofit2:retrofit:2.x.x")
implementation("com.squareup.retrofit2:converter-moshi:2.x.x")
implementation("com.squareup.okhttp3:logging-interceptor:4.x.x")
implementation("io.coil-kt:coil-compose:2.x.x")
implementation("androidx.security:security-crypto:1.1.x")
```

---

## 3. Design System

### 3.1 Color Palette (Material Design 3)

Define a `Color.kt` and wire into a `MaterialTheme` with `darkColorScheme` as default.

```kotlin
// Primary brand color: deep slate/charcoal with amber accent
// Rationale: music criticism associations — vinyl, editorial print
val MagpiePrimary       = Color(0xFF1A1A2E)  // deep navy-black
val MagpieOnPrimary     = Color(0xFFFFFFFF)
val MagpieSecondary     = Color(0xFFE8B86D)  // warm amber (accent)
val MagpieOnSecondary   = Color(0xFF1A1A2E)
val MagpieSurface       = Color(0xFF12121F)  // slightly lighter than primary
val MagpieOnSurface     = Color(0xFFF0EFE9)  // warm off-white
val MagpieBackground    = Color(0xFF0D0D1A)
val MagpieSurfaceVariant= Color(0xFF2A2A3E)
val MagpieError         = Color(0xFFCF6679)
```

The amber accent (`MagpieSecondary`) is used exclusively for:
- Star rating filled state
- Active tab indicator
- Primary CTA buttons
- Follow button (active state)

Everything else uses neutral surface/on-surface tokens.

### 3.2 Typography

```kotlin
val MagpieTypography = Typography(
    displayLarge  = TextStyle(fontFamily = /* Playfair Display */, fontSize = 57.sp),
    headlineLarge = TextStyle(fontFamily = /* Playfair Display */, fontSize = 32.sp),
    headlineMedium= TextStyle(fontFamily = /* Playfair Display */, fontSize = 28.sp),
    titleLarge    = TextStyle(fontFamily = /* DM Sans */, fontWeight = Bold, fontSize = 22.sp),
    bodyLarge     = TextStyle(fontFamily = /* DM Sans */, fontSize = 16.sp),
    bodyMedium    = TextStyle(fontFamily = /* DM Sans */, fontSize = 14.sp),
    labelSmall    = TextStyle(fontFamily = /* DM Sans */, fontSize = 11.sp, letterSpacing = 0.5.sp),
)
```

Use Google Fonts Compose for Playfair Display (headings) and DM Sans (body).

### 3.3 Reusable Components (shared across all developers)

Each component lives in `ui/components/`. No developer ships a screen without using these.

| Component | Purpose |
|---|---|
| `StarRatingBar` | Displays 0–5 stars in 0.5 increments, read-only and interactive variants |
| `ReviewCard` | Card showing author avatar, target title, rating, body excerpt, like count |
| `ArtistChip` | Compact artist name + thumbnail |
| `AlbumCover` | Coil-loaded image with rounded corners and shimmer placeholder |
| `UserAvatar` | Circular avatar with initials fallback |
| `MagpieTopBar` | Consistent top app bar with back navigation |
| `EmptyState` | Illustration + message for empty lists |
| `ErrorState` | Error message + retry button |
| `LoadingIndicator` | Centered CircularProgressIndicator with surface background |
| `PagedLazyColumn` | LazyColumn with automatic pagination trigger at bottom |

---

## 4. Project Structure

```
app/
├── src/main/
│   ├── java/com/magpie/
│   │   ├── MagpieApp.kt              # Application class, Hilt entry point
│   │   ├── MainActivity.kt           # Single activity, NavHost host
│   │   ├── navigation/
│   │   │   ├── NavGraph.kt           # All composable destinations
│   │   │   └── Screen.kt             # Sealed class of routes
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── MagpieDatabase.kt
│   │   │   │   ├── dao/
│   │   │   │   │   ├── UserDao.kt
│   │   │   │   │   ├── ArtistDao.kt
│   │   │   │   │   ├── AlbumDao.kt
│   │   │   │   │   ├── TrackDao.kt
│   │   │   │   │   └── PendingReviewDao.kt
│   │   │   │   └── entity/
│   │   │   │       ├── UserEntity.kt
│   │   │   │       ├── ArtistEntity.kt
│   │   │   │       ├── AlbumEntity.kt
│   │   │   │       ├── TrackEntity.kt
│   │   │   │       └── PendingReviewEntity.kt
│   │   │   ├── remote/
│   │   │   │   ├── api/
│   │   │   │   │   ├── AuthApi.kt
│   │   │   │   │   ├── UserApi.kt
│   │   │   │   │   ├── CatalogApi.kt
│   │   │   │   │   ├── ReviewApi.kt
│   │   │   │   │   └── FeedApi.kt
│   │   │   │   ├── dto/              # Data classes matching OpenAPI schemas exactly
│   │   │   │   │   ├── auth/
│   │   │   │   │   ├── user/
│   │   │   │   │   ├── catalog/
│   │   │   │   │   ├── review/
│   │   │   │   │   └── common/       # PageDto, ErrorDto
│   │   │   │   └── interceptor/
│   │   │   │       └── AuthInterceptor.kt
│   │   │   └── repository/
│   │   │       ├── AuthRepository.kt
│   │   │       ├── UserRepository.kt
│   │   │       ├── CatalogRepository.kt
│   │   │       ├── ReviewRepository.kt
│   │   │       └── FeedRepository.kt
│   │   ├── di/
│   │   │   ├── NetworkModule.kt
│   │   │   ├── DatabaseModule.kt
│   │   │   └── RepositoryModule.kt
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Type.kt
│   │   │   │   └── Theme.kt
│   │   │   ├── components/           # Shared composables (listed in 3.3)
│   │   │   └── screens/
│   │   │       ├── auth/             # Developer A
│   │   │       ├── profile/          # Developer A
│   │   │       ├── catalog/          # Developer B
│   │   │       ├── review/           # Developer C
│   │   │       └── feed/             # Developer C
│   │   └── util/
│   │       ├── TokenManager.kt       # EncryptedSharedPreferences wrapper
│   │       ├── Resource.kt           # sealed class Success/Error/Loading
│   │       └── Extensions.kt
│   └── res/
│       ├── values/strings.xml        # Portuguese (default)
│       └── values-en/strings.xml     # English
└── src/test/
    ├── data/remote/                  # MockWebServer tests per API
    └── data/local/                   # Room in-memory tests per DAO
```

---

## 5. Network Layer

### 5.1 DTOs

Create one Kotlin data class per OpenAPI schema. Field names must match the JSON keys exactly (or use `@Json(name = "...")` annotations). Never use the DTO directly in the UI — always map to a domain model or UI state.

**Key DTOs to implement:**

```kotlin
// common/PageDto.kt
data class PageDto<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val pages: Int
)

// auth/AuthResponseDto.kt
data class AuthResponseDto(
    val access_token: String,
    val token_type: String,
    val refresh_token: String?,
    val user: UserReadDto
)

// user/UserReadDto.kt
data class UserReadDto(
    val id: Int,
    val username: String,
    val display_name: String,
    val avatar_url: String?,
    val bio: String?,
    val created_at: String,
    val follower_count: Int,
    val following_count: Int,
    val is_following: Boolean
)

// review/ReviewReadDto.kt
data class ReviewReadDto(
    val id: Int,
    val author: UserReadDto,
    val target_type: String,       // "track" or "album"
    val target_id: Int,
    val target_title: String,
    val target_image_url: String?,
    val artist_name: String,
    val rating: Double,
    val body: String?,
    val like_count: Int,
    val liked_by_me: Boolean,
    val created_at: String,
    val updated_at: String
)

// catalog/ArtistReadDto.kt
data class ArtistReadDto(
    val id: Int,
    val spotify_id: String,
    val name: String,
    val image_url: String?,
    val genres: List<String>,
    val avg_rating: Double
)

// catalog/AlbumReadDto.kt
data class AlbumReadDto(
    val id: Int,
    val spotify_id: String,
    val title: String,
    val artist_name: String,
    val artist_id: Int,
    val image_url: String?,
    val release_date: String?,
    val album_type: String?,
    val avg_rating: Double,
    val review_count: Int
)

// catalog/TrackReadDto.kt
data class TrackReadDto(
    val id: Int,
    val spotify_id: String,
    val title: String,
    val artist_name: String,
    val album_title: String,
    val album_id: Int,
    val album_image_url: String?,
    val duration_ms: Int?,
    val preview_url: String?,
    val avg_rating: Double,
    val review_count: Int
)
```

### 5.2 API Interfaces

```kotlin
// AuthApi.kt
interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: UserCreateDto): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(@Body body: UserLoginDto): Response<AuthResponseDto>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: TokenRefreshRequestDto): Response<TokenResponseDto>
}

// CatalogApi.kt
interface CatalogApi {
    @GET("catalog/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("type") type: String = "all",
        @Query("limit") limit: Int = 10
    ): Response<CatalogSearchResponseDto>

    @GET("catalog/artists/{id}")
    suspend fun getArtist(@Path("id") id: Int): Response<ArtistReadDto>

    @GET("catalog/artists/{id}/albums")
    suspend fun getArtistAlbums(@Path("id") id: Int): Response<List<AlbumReadDto>>

    @GET("catalog/albums/{id}")
    suspend fun getAlbum(@Path("id") id: Int): Response<AlbumReadDto>

    @GET("catalog/albums/{id}/tracks")
    suspend fun getAlbumTracks(@Path("id") id: Int): Response<List<TrackReadDto>>

    @GET("catalog/tracks/{id}")
    suspend fun getTrack(@Path("id") id: Int): Response<TrackReadDto>
}

// ReviewApi.kt
interface ReviewApi {
    @GET("reviews")
    suspend fun listReviews(
        @Query("target_type") targetType: String? = null,
        @Query("target_id") targetId: Int? = null,
        @Query("author_id") authorId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("order_by") orderBy: String = "recent"
    ): Response<PageDto<ReviewReadDto>>

    @POST("reviews")
    suspend fun createReview(@Body body: ReviewCreateDto): Response<ReviewReadDto>

    @PUT("reviews/{id}")
    suspend fun updateReview(
        @Path("id") id: Int,
        @Body body: ReviewUpdateDto
    ): Response<ReviewReadDto>

    @DELETE("reviews/{id}")
    suspend fun deleteReview(@Path("id") id: Int): Response<Unit>

    @POST("reviews/{id}/like")
    suspend fun likeReview(@Path("id") id: Int): Response<Map<String, String>>

    @DELETE("reviews/{id}/like")
    suspend fun unlikeReview(@Path("id") id: Int): Response<Map<String, String>>
}

// FeedApi.kt
interface FeedApi {
    @GET("feed")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PageDto<ReviewReadDto>>

    @GET("feed/popular")
    suspend fun getPopularFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PageDto<ReviewReadDto>>
}

// UserApi.kt
interface UserApi {
    @GET("users/me")
    suspend fun getMe(): Response<UserReadDto>

    @PUT("users/me")
    suspend fun updateMe(@Body body: UserUpdateDto): Response<UserReadDto>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<UserReadDto>

    @GET("users/{id}/reviews")
    suspend fun getUserReviews(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PageDto<ReviewReadDto>>

    @POST("users/{id}/follow")
    suspend fun followUser(@Path("id") id: Int): Response<Map<String, String>>

    @DELETE("users/{id}/follow")
    suspend fun unfollowUser(@Path("id") id: Int): Response<Map<String, String>>

    @GET("users/{id}/followers")
    suspend fun getFollowers(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PageDto<UserReadDto>>

    @GET("users/{id}/following")
    suspend fun getFollowing(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PageDto<UserReadDto>>
}
```

### 5.3 Auth Interceptor

```kotlin
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = tokenManager.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

### 5.4 Resource Wrapper

All repository methods return `Resource<T>`:

```kotlin
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
```

Map HTTP error codes to user-facing strings:
- 401 → `R.string.error_unauthorized`
- 403 → `R.string.error_forbidden`
- 404 → `R.string.error_not_found`
- 409 → `R.string.error_already_exists`
- 503 → `R.string.error_service_unavailable`
- else → `R.string.error_generic`

---

## 6. Local Storage (Room)

### 6.1 Entities

```kotlin
// UserEntity — stores the logged-in user's data locally
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val followerCount: Int,
    val followingCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

// ArtistEntity — catalog cache
@Entity(tableName = "artist")
data class ArtistEntity(
    @PrimaryKey val id: Int,
    val spotifyId: String,
    val name: String,
    val imageUrl: String?,
    val genres: String,          // JSON array serialized as String
    val avgRating: Double,
    val cachedAt: Long = System.currentTimeMillis()
)

// AlbumEntity
@Entity(tableName = "album")
data class AlbumEntity(
    @PrimaryKey val id: Int,
    val spotifyId: String,
    val title: String,
    val artistId: Int,
    val artistName: String,
    val imageUrl: String?,
    val releaseDate: String?,
    val albumType: String?,
    val avgRating: Double,
    val reviewCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

// TrackEntity
@Entity(tableName = "track")
data class TrackEntity(
    @PrimaryKey val id: Int,
    val spotifyId: String,
    val title: String,
    val albumId: Int,
    val albumTitle: String,
    val artistName: String,
    val albumImageUrl: String?,
    val durationMs: Int?,
    val previewUrl: String?,
    val avgRating: Double,
    val reviewCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

// PendingReviewEntity — offline queue
@Entity(tableName = "pending_review")
data class PendingReviewEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val targetType: String,
    val targetId: Int,
    val rating: Double,
    val body: String?,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 6.2 DAOs

```kotlin
// UserDao.kt
@Dao
interface UserDao {
    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("DELETE FROM user")
    suspend fun clearAll()
}

// ArtistDao.kt
@Dao
interface ArtistDao {
    @Query("SELECT * FROM artist WHERE id = :id")
    suspend fun getById(id: Int): ArtistEntity?

    @Query("SELECT * FROM artist WHERE name LIKE '%' || :query || '%'")
    suspend fun searchByName(query: String): List<ArtistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(artists: List<ArtistEntity>)
}

// AlbumDao.kt
@Dao
interface AlbumDao {
    @Query("SELECT * FROM album WHERE id = :id")
    suspend fun getById(id: Int): AlbumEntity?

    @Query("SELECT * FROM album WHERE artistId = :artistId")
    suspend fun getByArtist(artistId: Int): List<AlbumEntity>

    @Query("SELECT * FROM album WHERE title LIKE '%' || :query || '%'")
    suspend fun searchByTitle(query: String): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(albums: List<AlbumEntity>)
}

// TrackDao.kt
@Dao
interface TrackDao {
    @Query("SELECT * FROM track WHERE id = :id")
    suspend fun getById(id: Int): TrackEntity?

    @Query("SELECT * FROM track WHERE albumId = :albumId ORDER BY title ASC")
    suspend fun getByAlbum(albumId: Int): List<TrackEntity>

    @Query("SELECT * FROM track WHERE title LIKE '%' || :query || '%'")
    suspend fun searchByTitle(query: String): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<TrackEntity>)
}

// PendingReviewDao.kt
@Dao
interface PendingReviewDao {
    @Query("SELECT * FROM pending_review ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingReviewEntity>

    @Insert
    suspend fun insert(review: PendingReviewEntity)

    @Delete
    suspend fun delete(review: PendingReviewEntity)
}
```

---

## 7. Screens and Developer Ownership

### Navigation Routes

```kotlin
sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Feed           : Screen("feed")                          // bottom nav
    object Search         : Screen("search")                        // bottom nav
    object MyProfile      : Screen("profile/me")                    // bottom nav
    object UserProfile    : Screen("profile/{userId}")
    object Artist         : Screen("catalog/artist/{artistId}")
    object Album          : Screen("catalog/album/{albumId}")
    object Track          : Screen("catalog/track/{trackId}")
    object WriteReview    : Screen("review/write?type={type}&id={id}")
    object ReviewDetail   : Screen("review/{reviewId}")
}
```

Bottom navigation tabs: **Feed · Search · Profile**

---

### 👤 Developer A — Auth & Profile

**Screens:** Login · Register · My Profile · User Profile (other users)

**Owns:** `AuthRepository`, `UserRepository`, `UserDao`, `TokenManager`, all auth/user ViewModels

#### Screen: Login

Route: `login`

UI elements:
- Magpie logo (SVG asset) centered, `displayLarge` typography
- Email/username field (`OutlinedTextField`)
- Password field with toggle visibility icon
- "Sign in" primary button (full width, amber)
- "Create account" text link → navigates to Register
- "Forgot password?" text link (calls `POST /auth/password-recovery`, shows snackbar on success)

ViewModel state:
```kotlin
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToFeed: Boolean = false
)
```

On successful login:
1. Store `access_token` and `refresh_token` in `TokenManager` (EncryptedSharedPreferences)
2. Store `UserReadDto` → `UserEntity` in Room via `UserDao`
3. Navigate to `Feed`, clearing back stack

#### Screen: Register

Route: `register`

UI elements:
- Back button → Login
- Fields: display name, username, email, password, confirm password
- Client-side validations before network call:
    - Username: 3–30 chars, alphanumeric + underscore only
    - Email: basic format check
    - Password: minimum 8 chars
    - Confirm password matches
- "Create account" primary button

On success: same as Login (token + user stored, navigate to Feed).
On 409 from API: show inline error on the conflicting field.

#### Screen: My Profile

Route: `profile/me`

UI elements:
- Avatar (Coil, circular, 80dp) — fallback to initials in amber circle
- Display name (`headlineMedium`)
- Username (`@username`, `bodyMedium`, muted)
- Bio (if present)
- Follower count / Following count — tappable → UserProfile's followers/following lists
- "Edit profile" icon button → inline editing mode (display name, bio, avatar URL)
- `LazyColumn` of the user's own reviews using `ReviewCard`

Data sources:
- Primary: `GET /users/me` on load
- Fallback: `UserEntity` from Room if offline
- Reviews: `GET /users/{id}/reviews` (paginated)

#### Screen: User Profile (other users)

Route: `profile/{userId}`

Same layout as My Profile but:
- No "Edit profile" button
- Follow / Unfollow button (uses `is_following` from `UserReadDto`)
    - Follow: `POST /users/{id}/follow`
    - Unfollow: `DELETE /users/{id}/follow`
- Button state is optimistic: update locally immediately, revert on error

**Repository responsibilities (Developer A):**

```kotlin
interface AuthRepository {
    suspend fun register(username: String, email: String, password: String, displayName: String): Resource<AuthResponseDto>
    suspend fun login(username: String, password: String): Resource<AuthResponseDto>
    suspend fun logout()                    // clears token + Room user table
    fun isLoggedIn(): Boolean
}

interface UserRepository {
    suspend fun getMe(): Resource<UserReadDto>
    suspend fun updateMe(displayName: String?, avatarUrl: String?, bio: String?): Resource<UserReadDto>
    suspend fun getUser(id: Int): Resource<UserReadDto>
    suspend fun getUserReviews(userId: Int, page: Int): Resource<PageDto<ReviewReadDto>>
    suspend fun followUser(id: Int): Resource<Unit>
    suspend fun unfollowUser(id: Int): Resource<Unit>
    suspend fun getFollowers(userId: Int, page: Int): Resource<PageDto<UserReadDto>>
    suspend fun getFollowing(userId: Int, page: Int): Resource<PageDto<UserReadDto>>
}
```

---

### 👤 Developer B — Catalog & Search

**Screens:** Search · Artist Detail · Album Detail · Track Detail

**Owns:** `CatalogRepository`, all catalog DAOs, all catalog ViewModels

#### Screen: Search

Route: `search`

UI elements:
- Search bar (full width, auto-focused on tab selection)
- Segmented control: All / Artists / Albums / Tracks
- Results in `LazyColumn`, grouped by type when "All" is selected
- Each result row shows thumbnail + title + subtitle (artist name or album title)
- Tapping an artist → Artist Detail; album → Album Detail; track → Track Detail
- Debounce input by 300ms before firing `GET /catalog/search`
- While query is empty: show recent searches (from Room cache, last 10 local entities accessed)

Search logic:
1. Query Room first for instant results (ilike on cached entities)
2. Fire API call in parallel
3. Merge results, deduplicating by `id`
4. Persist new API results to Room

ViewModel state:
```kotlin
data class SearchUiState(
    val query: String = "",
    val selectedType: CatalogType = CatalogType.ALL,
    val artists: List<ArtistReadDto> = emptyList(),
    val albums: List<AlbumReadDto> = emptyList(),
    val tracks: List<TrackReadDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

#### Screen: Artist Detail

Route: `catalog/artist/{artistId}`

UI elements:
- Hero image (`AlbumCover`, 200dp height, full width with scrim overlay)
- Artist name in `headlineLarge` overlaid on hero
- Genre chips (`FilterChip`, non-interactive, horizontal scroll)
- Average rating: `StarRatingBar` (read-only) + "(X reviews)"
- Section header: "Discography"
- `LazyColumn` of albums using compact album cards (cover + title + year + avg_rating)
- Tapping an album → Album Detail

Data loading:
1. `GET /catalog/artists/{id}` for artist data
2. `GET /catalog/artists/{id}/albums` for discography
3. Cache both to Room

#### Screen: Album Detail

Route: `catalog/album/{albumId}`

UI elements:
- Album cover (square, 180dp)
- Title (`headlineMedium`) + artist name (`titleLarge`, tappable → Artist Detail)
- Release year + album type badge
- Average star rating + review count
- "Write a Review" button (amber, navigates to WriteReview with `type=album&id={albumId}`)
    - Button label changes to "Edit your review" if user already has a review for this album
- Section: "Tracks" — numbered list with title + duration
    - Tapping a track → Track Detail
- Section: "Reviews" — top 3 reviews using `ReviewCard`, "See all" → filtered review list

#### Screen: Track Detail

Route: `catalog/track/{trackId}`

UI elements:
- Album cover (120dp, square)
- Track title (`headlineMedium`)
- Artist name (tappable) + Album title (tappable)
- Duration formatted as `m:ss`
- Preview player (if `preview_url` is not null):
    - Play/Pause button using `MediaPlayer`
    - Simple waveform progress bar (animated `LinearProgressIndicator`)
- Average star rating
- "Write a Review" button → WriteReview with `type=track&id={trackId}`
- Reviews section (same as Album Detail)

**Repository responsibilities (Developer B):**

```kotlin
interface CatalogRepository {
    suspend fun search(query: String, type: String, limit: Int): Resource<CatalogSearchResponseDto>
    suspend fun getArtist(id: Int): Resource<ArtistReadDto>
    suspend fun getArtistAlbums(artistId: Int): Resource<List<AlbumReadDto>>
    suspend fun getAlbum(id: Int): Resource<AlbumReadDto>
    suspend fun getAlbumTracks(albumId: Int): Resource<List<TrackReadDto>>
    suspend fun getTrack(id: Int): Resource<TrackReadDto>
}
```

Cache policy: serve Room data immediately if present and `cachedAt` < 1 hour ago. Otherwise fetch from network and update Room.

---

### 👤 Developer C — Reviews & Feed

**Screens:** Feed · Write/Edit Review · Review Detail

**Owns:** `ReviewRepository`, `FeedRepository`, `PendingReviewDao`, all review/feed ViewModels

#### Screen: Feed

Route: `feed` (default bottom nav destination)

UI elements:
- Top bar: "Magpie" wordmark + notification bell icon (non-functional, present for visual completeness)
- Two tabs: "For You" (authenticated feed) and "Popular" (unauthenticated feed)
- `PagedLazyColumn` of `ReviewCard` items
- Pull-to-refresh (`PullToRefreshBox`)
- If not logged in, "For You" tab prompts sign in; "Popular" works freely

ReviewCard contents:
- Author avatar + display name + "@username" + timestamp (relative: "2h ago")
- Target: album cover thumbnail (40dp) + target title + artist name
- Star rating (read-only `StarRatingBar`)
- Body text (max 3 lines, expandable on tap)
- Like button with count (heart icon, filled amber when `liked_by_me`)
- Tapping the card body → Review Detail
- Tapping the target title → Album Detail or Track Detail

Like interaction (optimistic):
1. Toggle `liked_by_me` and update `like_count` locally
2. Call `POST /reviews/{id}/like` or `DELETE /reviews/{id}/like`
3. On error: revert local state and show snackbar

ViewModel state:
```kotlin
data class FeedUiState(
    val selectedTab: FeedTab = FeedTab.FOR_YOU,
    val reviews: List<ReviewReadDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)
```

#### Screen: Write / Edit Review

Route: `review/write?type={type}&id={id}`

Displayed as a bottom sheet or full screen — implement as full screen for simplicity.

UI elements:
- Top bar: "Review" title + "Cancel" action
- Target info header: cover image (60dp) + title + artist name (loaded from Room cache using `type` + `id`)
- Interactive `StarRatingBar` (tap and drag, 0–5 in 0.5 increments)
- `OutlinedTextField` for body (optional, hint: localized placeholder, max 500 chars with counter)
- "Publish" primary button (disabled if rating is 0)
- If editing an existing review: pre-populate fields, button says "Save changes"

Offline behavior:
- If network call fails, store in `PendingReviewEntity` (Room)
- Show snackbar: "Review saved. Will publish when you're back online."
- On next app launch with connectivity: attempt to flush `PendingReviewDao.getAll()`

Validation:
- Rating must be > 0 before submission
- Body trimmed before sending (null if empty)

On success:
- Navigate back with a result that triggers refresh on the calling screen
- If opened from Album/Track Detail: update that screen's reviews list

#### Screen: Review Detail

Route: `review/{reviewId}`

UI elements:
- Author info row (avatar + name + timestamp)
- Target info row (cover + title + artist) — tappable → Album/Track Detail
- Full star rating (large, `StarRatingBar` read-only, 32dp stars)
- Full body text (no truncation)
- Like button row (same optimistic behavior as in Feed)
- If `author.id == currentUser.id`:
    - Edit icon → WriteReview in edit mode
    - Delete icon → confirmation dialog → `DELETE /reviews/{id}` → navigate back

**Repository responsibilities (Developer C):**

```kotlin
interface ReviewRepository {
    suspend fun getReviews(
        targetType: String? = null,
        targetId: Int? = null,
        authorId: Int? = null,
        page: Int = 1,
        orderBy: String = "recent"
    ): Resource<PageDto<ReviewReadDto>>

    suspend fun getReview(id: Int): Resource<ReviewReadDto>
    suspend fun createReview(targetType: String, targetId: Int, rating: Double, body: String?): Resource<ReviewReadDto>
    suspend fun updateReview(id: Int, rating: Double?, body: String?): Resource<ReviewReadDto>
    suspend fun deleteReview(id: Int): Resource<Unit>
    suspend fun likeReview(id: Int): Resource<Unit>
    suspend fun unlikeReview(id: Int): Resource<Unit>
    suspend fun flushPendingReviews()       // syncs PendingReviewDao on connectivity restore
}

interface FeedRepository {
    suspend fun getFeed(page: Int): Resource<PageDto<ReviewReadDto>>
    suspend fun getPopularFeed(page: Int): Resource<PageDto<ReviewReadDto>>
}
```

---

## 8. TokenManager

Wraps `EncryptedSharedPreferences` — only Developer A implements this, everyone else injects it.

```kotlin
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "magpie_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, refreshToken: String?) { ... }
    fun getAccessToken(): String? { ... }
    fun getRefreshToken(): String? { ... }
    fun clearTokens() { ... }
}
```

---

## 9. i18n

### Rules (non-negotiable)
- Zero hardcoded strings in Kotlin or Composable code
- All user-visible text goes through `stringResource(R.string.key)`
- All format strings use `stringResource(R.string.key, arg1, arg2)`
- Enforce with lint: `HardcodedText` check enabled in `lint.xml`

### Required string keys (partial — expand as screens are built)

```xml
<!-- values/strings.xml (Portuguese default) -->
<resources>
    <string name="app_name">Magpie</string>

    <!-- Auth -->
    <string name="label_username">Nome de usuário</string>
    <string name="label_email">E-mail</string>
    <string name="label_password">Senha</string>
    <string name="label_confirm_password">Confirmar senha</string>
    <string name="label_display_name">Nome de exibição</string>
    <string name="action_sign_in">Entrar</string>
    <string name="action_create_account">Criar conta</string>
    <string name="action_forgot_password">Esqueci minha senha</string>

    <!-- Profile -->
    <string name="label_followers">Seguidores</string>
    <string name="label_following">Seguindo</string>
    <string name="action_follow">Seguir</string>
    <string name="action_unfollow">Deixar de seguir</string>
    <string name="action_edit_profile">Editar perfil</string>

    <!-- Catalog -->
    <string name="label_discography">Discografia</string>
    <string name="label_tracks">Faixas</string>
    <string name="label_reviews">Avaliações</string>
    <string name="label_avg_rating">Nota média</string>
    <string name="action_write_review">Escrever avaliação</string>
    <string name="action_edit_review">Editar sua avaliação</string>
    <string name="label_see_all">Ver todas</string>

    <!-- Review -->
    <string name="label_review_body_hint">O que você achou? (opcional)</string>
    <string name="action_publish">Publicar</string>
    <string name="action_save_changes">Salvar alterações</string>
    <string name="action_delete_review">Excluir avaliação</string>
    <string name="confirm_delete_review">Tem certeza que deseja excluir esta avaliação?</string>
    <string name="label_chars_remaining">%d/500</string>

    <!-- Feed -->
    <string name="tab_for_you">Para você</string>
    <string name="tab_popular">Populares</string>

    <!-- Errors -->
    <string name="error_generic">Algo deu errado. Tente novamente.</string>
    <string name="error_unauthorized">Sessão expirada. Faça login novamente.</string>
    <string name="error_forbidden">Você não tem permissão para isso.</string>
    <string name="error_not_found">Não encontrado.</string>
    <string name="error_already_exists">Já existe um registro com esses dados.</string>
    <string name="error_service_unavailable">Serviço temporariamente indisponível.</string>
    <string name="error_offline_review_saved">Avaliação salva. Será publicada quando você estiver online.</string>

    <!-- Empty states -->
    <string name="empty_feed">Nenhuma avaliação ainda. Que tal explorar alguns álbuns?</string>
    <string name="empty_reviews">Sem avaliações ainda.</string>
    <string name="empty_search">Nenhum resultado para "%s".</string>
</resources>
```

```xml
<!-- values-en/strings.xml (English) -->
<resources>
    <string name="app_name">Magpie</string>
    <string name="label_username">Username</string>
    <string name="label_email">Email</string>
    <string name="label_password">Password</string>
    <string name="label_confirm_password">Confirm password</string>
    <string name="label_display_name">Display name</string>
    <string name="action_sign_in">Sign in</string>
    <string name="action_create_account">Create account</string>
    <string name="action_forgot_password">Forgot password?</string>
    <string name="label_followers">Followers</string>
    <string name="label_following">Following</string>
    <string name="action_follow">Follow</string>
    <string name="action_unfollow">Unfollow</string>
    <string name="action_edit_profile">Edit profile</string>
    <string name="label_discography">Discography</string>
    <string name="label_tracks">Tracks</string>
    <string name="label_reviews">Reviews</string>
    <string name="label_avg_rating">Average rating</string>
    <string name="action_write_review">Write a review</string>
    <string name="action_edit_review">Edit your review</string>
    <string name="label_see_all">See all</string>
    <string name="label_review_body_hint">What did you think? (optional)</string>
    <string name="action_publish">Publish</string>
    <string name="action_save_changes">Save changes</string>
    <string name="action_delete_review">Delete review</string>
    <string name="confirm_delete_review">Are you sure you want to delete this review?</string>
    <string name="label_chars_remaining">%d/500</string>
    <string name="tab_for_you">For You</string>
    <string name="tab_popular">Popular</string>
    <string name="error_generic">Something went wrong. Please try again.</string>
    <string name="error_unauthorized">Session expired. Please sign in again.</string>
    <string name="error_forbidden">You don\'t have permission to do this.</string>
    <string name="error_not_found">Not found.</string>
    <string name="error_already_exists">This record already exists.</string>
    <string name="error_service_unavailable">Service temporarily unavailable.</string>
    <string name="error_offline_review_saved">Review saved. It will be published when you\'re back online.</string>
    <string name="empty_feed">No reviews yet. How about exploring some albums?</string>
    <string name="empty_reviews">No reviews yet.</string>
    <string name="empty_search">No results for "%s".</string>
</resources>
```

---

## 10. Automated Tests

Each developer writes tests for their own Repository and DAO layers.
Use `MockWebServer` for network tests, in-memory Room for DAO tests.

### Test structure

```
src/test/java/com/magpie/
├── data/
│   ├── remote/
│   │   ├── AuthApiTest.kt          // Developer A
│   │   ├── UserApiTest.kt          // Developer A
│   │   ├── CatalogApiTest.kt       // Developer B
│   │   ├── ReviewApiTest.kt        // Developer C
│   │   └── FeedApiTest.kt          // Developer C
│   └── local/
│       ├── UserDaoTest.kt          // Developer A
│       ├── CatalogDaoTest.kt       // Developer B
│       └── PendingReviewDaoTest.kt // Developer C
```

### MockWebServer pattern (shared)

```kotlin
class AuthApiTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var authApi: AuthApi

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        authApi = retrofit.create(AuthApi::class.java)
    }

    @After fun teardown() { mockWebServer.shutdown() }

    @Test fun `login with valid credentials returns auth response`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"access_token":"tok","token_type":"bearer","refresh_token":null,"user":{...}}"""))
        val response = authApi.login(UserLoginDto("user", "pass"))
        assertTrue(response.isSuccessful)
        assertEquals("tok", response.body()?.access_token)
    }

    @Test fun `login with wrong credentials returns 401`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        val response = authApi.login(UserLoginDto("user", "wrong"))
        assertEquals(401, response.code())
    }
}
```

### Minimum test coverage per developer

**Developer A:**
- Register → 201, 409 (duplicate username), 409 (duplicate email)
- Login → 200, 401
- Follow → 200, follow self → 4xx
- `UserDao`: upsert, getLoggedInUser, clearAll

**Developer B:**
- Search → results from MockWebServer
- GetArtist → 200, 404
- GetArtistAlbums → 200, empty list
- `ArtistDao`, `AlbumDao`, `TrackDao`: upsert and query by id/name

**Developer C:**
- CreateReview → 201, 409 (duplicate)
- DeleteReview as non-author → 403
- LikeReview → 200, unlike → 200
- `PendingReviewDao`: insert, getAll, delete

---

## 11. Dependency Injection (Hilt)

```kotlin
// NetworkModule.kt
@Module @InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)   // set in build.gradle per flavor
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

    @Provides fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class)
    @Provides fun provideCatalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class)
    @Provides fun provideReviewApi(retrofit: Retrofit): ReviewApi = retrofit.create(ReviewApi::class)
    @Provides fun provideFeedApi(retrofit: Retrofit): FeedApi = retrofit.create(FeedApi::class)
    @Provides fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class)
}

// DatabaseModule.kt
@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): MagpieDatabase =
        Room.databaseBuilder(ctx, MagpieDatabase::class.java, "magpie.db").build()

    @Provides fun provideUserDao(db: MagpieDatabase): UserDao = db.userDao()
    @Provides fun provideArtistDao(db: MagpieDatabase): ArtistDao = db.artistDao()
    @Provides fun provideAlbumDao(db: MagpieDatabase): AlbumDao = db.albumDao()
    @Provides fun provideTrackDao(db: MagpieDatabase): TrackDao = db.trackDao()
    @Provides fun providePendingReviewDao(db: MagpieDatabase): PendingReviewDao = db.pendingReviewDao()
}
```

`BASE_URL` set per build flavor in `build.gradle`:

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/\"")
    }
    release {
        buildConfigField("String", "BASE_URL", "\"https://magpie-api.railway.app/\"")
    }
}
```

---

## 12. Implementation Order

Each developer follows this sequence for their own screens.
All three run in parallel after week 1 setup.

**Week 1 (all together):**
1. Create project, configure Hilt, Compose, Navigation
2. Define `Theme.kt`, `Color.kt`, `Type.kt` — agree on design system before any screen
3. Create all shared components (`ReviewCard`, `StarRatingBar`, etc.) as empty stubs
4. Set up Room database and all DAOs
5. Set up Retrofit with `AuthInterceptor` and `TokenManager`
6. Define all DTOs matching the OpenAPI spec

**Week 2–3:**
- Developer A: Login + Register screens + AuthRepository + UserRepository (unblocks everyone — token management must work first)
- Developer B: Search + ArtistDetail screens + CatalogRepository (can use hardcoded mock data while A finishes auth)
- Developer C: Feed (Popular tab only, no auth needed) + ReviewCard component

**Week 4–5:**
- Developer A: MyProfile + UserProfile + follow logic
- Developer B: AlbumDetail + TrackDetail + Room caching
- Developer C: WriteReview + ReviewDetail + like logic

**Week 6:**
- All: pending reviews offline sync, pull-to-refresh, empty states, error states
- All: i18n sweep — ensure all strings are in `strings.xml`

**Week 7:**
- All: automated tests for own Repository and DAO
- Integration testing (end-to-end against real backend)
- UI polish, animation, edge cases

**Week 8:**
- Release build, ProGuard rules, demo prep

---

## 13. Delivery Contract Between Developers

- `TokenManager` is owned by Developer A and must be merged to `main` by end of **Week 2, Day 2**. Developers B and C depend on it for any authenticated call.
- All shared components in `ui/components/` are agreed upon by the team in Week 1. Any new shared component proposal goes through a team discussion before implementation.
- The `Screen.kt` sealed class is owned by no one — any developer can add a route via PR. No screen may navigate to a route that doesn't exist in `Screen.kt`.
- DTOs in `data/remote/dto/` follow the OpenAPI spec exactly. No unilateral changes to DTO field names.
- The backend Swagger at `/docs` is the authority for any API contract question — not memory, not assumption.