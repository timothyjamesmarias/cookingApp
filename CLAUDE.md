# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **personal culinary multitool** built with Kotlin — a native-first cooking assistant combining Compose Multiplatform (frontend) and Ktor + Exposed (backend). The architecture is designed for offline-first functionality with optional backend sync, and integrates with LLM services for contextual cooking assistance.

**Core Philosophy**: Local-first, ergonomic, modular, and educational. Must remain useful offline.

## Project Structure

```
cooking-app/
├── shared/domain/        # KMP shared domain models + API types (used by both frontend and backend)
├── backend/              # Ktor + Exposed REST API (Kotlin)
│   └── src/main/kotlin/com/timothymarias/cookingapp/
│       ├── db/           # DatabaseFactory, Exposed table definitions
│       ├── repository/   # Data access (Exposed DSL queries)
│       ├── routes/       # Ktor route handlers
│       ├── di/           # Koin DI module
│       └── Application.kt
├── frontend/
│   ├── shared/           # Kotlin Multiplatform shared code
│   │   └── src/commonMain/kotlin/com/timothymarias/cookingapp/shared/
│   │       ├── data/           # Repositories, data sources, SQLDelight
│   │       ├── di/             # Koin DI module
│   │       ├── presentation/   # UI state management (Stores)
│   │       └── App.kt          # Main Compose app entry
│   ├── android/          # Android app module
│   ├── ios/              # iOS app module
│   ├── desktop/          # Desktop (JVM) app module
│   └── web/              # Web app module (Compose for Web, disabled)
└── docs/                 # Design docs (see overview.md)
```

## Technology Stack

- **Language**: Kotlin (end-to-end)
- **Frontend**: Compose Multiplatform (Material 3)
- **Backend**: Ktor 3.1.3, Exposed ORM, Flyway
- **Database**: PostgreSQL (backend via Exposed), SQLDelight (frontend offline cache)
- **DI**: Koin (shared across frontend and backend)
- **Serialization**: kotlinx.serialization (shared domain types with `@Serializable`)
- **Networking**: Ktor Client (frontend), Ktor Server + Netty (backend)
- **Build**: Gradle with Kotlin DSL
- **JVM**: Java 21

## Common Commands

**Important**: This project requires Java 21. If your default Java is newer, prefix commands with:
`JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew ...`

### Backend (Ktor)

```bash
# Run backend server (requires PostgreSQL via Docker Compose)
./gradlew :backend:run

# Run backend tests (uses Testcontainers — requires Docker)
./gradlew :backend:test

# Run specific test class
./gradlew :backend:test --tests "com.timothymarias.cookingapp.routes.RecipeRoutesTest"

# Build backend
./gradlew :backend:build
```

### Frontend (Compose Multiplatform)

```bash
# Run desktop app
./gradlew :frontend:desktop:run

# Run Android app (requires emulator/device)
./gradlew :frontend:android:installDebug

# Run all shared module tests
./gradlew :frontend:shared:allTests

# Run desktop tests only
./gradlew :frontend:shared:desktopTest

# Run iOS simulator tests
./gradlew :frontend:shared:iosSimulatorArm64Test

# Build all frontend targets
./gradlew :frontend:shared:build
```

### Shared Domain Module

```bash
# Build shared domain types
./gradlew :shared:domain:build
```

### Database

```bash
# Start PostgreSQL (via Docker Compose)
docker compose up -d

# Stop PostgreSQL
docker compose down

# View Flyway migrations
ls backend/src/main/resources/db/migration/
```

### Full Project

```bash
# Build everything
./gradlew build

# Clean all build artifacts
./gradlew clean

# Run all tests
./gradlew check
```

## Architecture Patterns

### Shared Domain Module (`:shared:domain`)

Domain models and API types live in a standalone KMP module with `@Serializable` annotations. Both frontend and backend depend on this module — no duplicate type definitions.

- Models: `Recipe`, `Ingredient`, `Unit`, `Quantity`, `MeasurementType`
- API types: `CreateRecipeRequest`, `RecipeResponse`, `ApiRoutes`, etc.

### Frontend: Unidirectional Data Flow with Stores

The frontend uses a **Store pattern** for state management:

- **Store**: Holds `StateFlow<State>`, receives actions, updates state via coroutines
- **State**: Immutable data class representing UI state
- **Action**: Sealed class/interface representing user intents
- **Repository**: Abstracts data sources (local SQLDelight + remote API via Ktor)
- **DI**: Koin modules defined in `frontend/shared/.../di/SharedModule.kt`

### Backend: Ktor + Exposed (Flat Architecture)

- **Routes** → **Repository** → **Exposed Tables**
- No separate service layer (repositories return shared response types directly)
- No mappers or DTOs (shared `@Serializable` types serve as both)
- Validation via `require()`, caught by Ktor `StatusPages` as 400 errors
- DI via Koin (`di/BackendModule.kt`)

### Offline-First Design

- **SQLDelight** in `frontend/shared` provides local database (`.sq` files in `src/commonMain/sqldelight/`)
- Backend is optional; UI should degrade gracefully when offline
- Sync will use a library (e.g. PowerSync, ElectricSQL) below the repository layer

## Development Guidelines

### Adding a New Feature (Full Stack)

1. **Define shared domain model** in `shared/domain/src/commonMain/kotlin/.../domain/model/`
2. **Add API types** (request/response) in `shared/domain/src/commonMain/kotlin/.../domain/api/`
3. **Create SQLDelight schema** in `frontend/shared/src/commonMain/sqldelight/.../db/` if offline support is needed
4. **Add Flyway migration** in `backend/src/main/resources/db/migration/`
5. **Add Exposed table** in `backend/.../db/Tables.kt`
6. **Build backend**: Repository (Exposed DSL) → Routes (Ktor)
7. **Build frontend**: Repository (SQLDelight) → Store → UI
8. **Wire DI**: Add to `BackendModule.kt` and/or `SharedModule.kt`
9. **Write tests**: Backend route + repository tests, frontend store + repository tests

### Working with SQLDelight

- Schema files: `frontend/shared/src/commonMain/sqldelight/com/timothymarias/cookingapp/shared/db/*.sq`
- Database name: `CookingDatabase` (configured in `frontend/shared/build.gradle.kts`)
- After modifying `.sq` files, run `./gradlew :frontend:shared:generateCommonMainCookingDatabaseInterface` to regenerate code
- Access via platform-specific drivers (Android, iOS Native, JVM SQLite)

### Environment Setup

Create `.env` in project root with:

```bash
POSTGRES_DB=cooking_db
POSTGRES_USER=cooking_user
POSTGRES_PASSWORD=your_password
DB_PORT=5432
DB_URL=jdbc:postgresql://localhost:5432/cooking_db
```

(See `compose.yaml` for Docker Compose configuration)

### Testing Philosophy

- **Backend routes**: Ktor `testApplication` with MockK for mocking repositories
- **Backend repositories**: Testcontainers with real PostgreSQL (matches production)
- **Frontend**: Turbine for testing Flows, `desktopTest` for JVM-based tests with SQLite driver
- Always test repositories with real database interactions

## Module Dependencies

```
:shared:domain          ← Domain models + API types (no framework deps)
  ↑               ↑
:backend         :frontend:shared  ← SQLDelight, Compose, Ktor Client, Koin
                   ↑
          :frontend:{android,ios,desktop}
```

## Key Files

- `docs/overview.md` — Comprehensive feature roadmap and module descriptions
- `settings.gradle.kts` — Multi-project build structure
- `shared/domain/build.gradle.kts` — Shared KMP domain module
- `frontend/shared/build.gradle.kts` — KMP + Compose + SQLDelight configuration
- `backend/build.gradle.kts` — Ktor + Exposed configuration
- `backend/src/main/kotlin/.../Application.kt` — Backend entry point
- `frontend/shared/src/commonMain/kotlin/.../di/SharedModule.kt` — Frontend Koin DI

## Planned Features (See docs/overview.md)

**MVP Scope**: Recipe Library (CRUD), Pantry Tracker, Substitution Engine, basic theming

**Future**: Shopping List auto-generation, Cooking Assistant (step-by-step), Meal Planner, LLM integration for recipe parsing and suggestions, semantic search with vector embeddings

## Notes

- Compose Multiplatform version: 1.9.3 (Kotlin 2.3.0)
- Backend tests require Docker (Testcontainers pulls PostgreSQL image)
- Backend API speaks in `localId` (String UUID), not database `BIGINT` IDs
