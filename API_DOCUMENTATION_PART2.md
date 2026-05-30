# TrendSearchor API — Part 2

This document contains API specifications for Search (FR-02), Trends (FR-03), Dashboard & Social (FR-04), and additional Profile endpoints. 

---

## 8. Search Endpoints (FR-02)

### 8.1 GET `/api/papers/search`
Search papers with Semantic Scholar integration.
**Params:**
- `query` (required): Search keyword
- `page`, `size`
- `year`, `journal`, `author` (optional filters)
- `sortBy` (default: relevance)

### 8.2 GET `/api/papers/{id}`
Get paper details.

### 8.3 GET `/api/journals/search`
Search journals locally.
**Params:** `query`, `page`, `size`

### 8.4 GET `/api/authors/search`
Search authors locally.
**Params:** `query`, `page`, `size`

### 8.5 GET `/api/keywords/search`
Search keywords locally.

### 8.6 GET `/api/top-papers`
Get top cited papers globally.
**Params:** `limit`

---

## 9. Trend Endpoints (FR-03)

### 9.1 GET `/api/trends/analyze/{keyword}`
Full publication trend analysis for a keyword.
**Params:** `startYear`, `endYear` (optional)

### 9.2 GET `/api/trends/search`
Live search and analyze a keyword (fetches from OpenAlex directly).
**Params:** `query` (required)

### 9.3 GET `/api/trends/ranking`
Get ranked list of trending topics (Topic ranking).
**Params:** `limit`

### 9.4 GET `/api/trends/emerging`
Get emerging topics.

### 9.5 GET `/api/trends/compare-full`
Compare multiple topics side-by-side.
**Params:** `keywords` (comma separated, e.g. `llm,computer vision`), `startYear`, `endYear`

### 9.6 GET `/api/trends/related`
Get keyword co-occurrence data for visualization.
**Params:** `keyword`, `limit`

---

## 10. Dashboard & User Activity (FR-04)

### 10.1 GET `/api/dashboard/public`
Get public platform statistics (total papers, authors, etc).

### 10.2 GET `/api/dashboard/me`
Get personalized dashboard stats for the logged-in user.

### 10.3 GET `/api/dashboard/activity`
Get user's recent activity stream.

### 10.4 GET `/api/dashboard/admin/stats`
*(Admin Only)* Get platform administrative stats.

---

## 11. Social Endpoints (Bookmarks & Follows)

### 11.1 POST `/api/bookmarks`
Bookmark a paper or keyword.
**Request:**
```json
{
  "referenceId": 123,
  "type": "PAPER" // or "KEYWORD"
}
```

### 11.2 GET `/api/bookmarks`
**Params:** `type` (optional filter)

### 11.3 DELETE `/api/bookmarks/{id}`

### 11.4 POST `/api/follows`
Follow a journal or topic.
**Request:**
```json
{
  "referenceId": 456,
  "type": "TOPIC" // or "JOURNAL"
}
```

### 11.5 GET `/api/follows`
**Params:** `type`

### 11.6 DELETE `/api/follows/{id}`

---

## 12. Extended Profile Endpoints

### 12.1 GET `/api/profile/me`
Get detailed user profile.

### 12.2 PUT `/api/profile/me`
Update profile information (phone, dob, workplace, gender).
**Request:**
```json
{
  "phone": "0987654321",
  "dob": "1999-01-01",
  "workplace": "Company ABC",
  "gender": "MALE"
}
```
