# Culinary Multitool — Feature & Module Overview

A personal, native-first cooking assistant built in Kotlin (Compose Multiplatform + Spring).  
Focus: offline-first ergonomics, AI-assisted creativity, and modular expansion.

---

## 1. Core Goals

- Serve as a **personal culinary intelligence tool** (not a social app).
- Function **offline-first**, with optional backend sync.
- Use **Kotlin end-to-end** for shared logic and learning.
- Integrate with **LLM services** for contextual cooking assistance.
- Provide a **consistent, themed UI** via a custom design system.
- Enable gradual expansion into a full-fledged product if desired.

---

## 2. High-Level Architecture

| Layer | Technology | Notes                                               |
|-------|-------------|-----------------------------------------------------|
| UI | Compose Multiplatform (Desktop + Mobile) | Native-first, shared code                           |
| Shared Logic | Kotlin Multiplatform shared module | Domain models, repositories, conversions — used by frontend and backend |
| Backend | Ktor + Exposed | GraalVM native image, low memory footprint           |
| DI | Koin | Lightweight, KMP-compatible across frontend and backend |
| Persistence | SQLDelight (frontend) / Exposed (backend) | Offline cache (SQLDelight), server DB (Exposed + PostgreSQL) |
| AI | Cloud LLM (primary), local Ollama (optional/deferred) | Tiered: cloud for reasoning, local for extraction, deterministic where possible |

---

## 3. Core Modules

### 3.1 Recipe Library
**Purpose:** Central repository for all personal recipes.

- CRUD for recipes (title, ingredients, steps, tags)
- Recipe parsing (text → structured format via LLM)
- Import from websites or plaintext (URL paste → LLM extraction → review → save)
- Scaling (serving size adjustment)
- Image attachments
- Favorite and tag system
- Unit conversion engine (shared module): grams ↔ oz, ml ↔ cups, etc. — reused across recipes, pantry, and shopping lists

---

### 3.2 Pantry Tracker
**Purpose:** Maintain an up-to-date list of owned ingredients.

- Track items, quantities, units, expiration dates
- Manual or barcode-based entry
- Smart categorization (fridge, freezer, pantry)
- Integration with Recipe Library: “What can I make?”
- Expiration reminders (local notifications)

---

### 3.3 Shopping List
**Purpose:** Generate grocery lists from recipes and meal plans.

- Auto-generate from selected recipes
- Merge and normalize duplicate items
- Unit conversions (oz → g → ml)
- Group by category (produce, dairy, etc.)
- Mark purchased items locally
- Export to text / share sheet

---

### 3.4 Substitution Engine
**Purpose:** Suggest alternatives when ingredients are missing.

- Static substitution database (common pairs)
- Contextual suggestions via LLM (based on recipe)
- Explain reasoning: “Why this works”
- Flavor similarity model (embedding-based)

---

### 3.5 Cooking Assistant
**Purpose:** Interactive step-by-step guidance during cooking.

- Step viewer with progress indicators
- Timers (multi-step parallel)
- Voice input/output (“Next step,” “How long left?”)
- Offline speech support (optional)
- Adaptive layout for hands-free kitchen use

---

### 3.6 Meal Planner
**Purpose:** Plan meals and automate grocery/recipe organization.

- Calendar integration (per day/meal slot)
- Weekly overview
- Auto-generate shopping list
- Nutrition summary (optional)
- “Regenerate next week’s plan” via LLM

---

### 3.7 Knowledge & Insight Layer
**Purpose:** Centralized semantic memory for recipes and history.

- Vector store of recipe embeddings
- Search: “Find recipes with cardamom + orange”
- Historical insight: “What did I cook last winter?”
- Optional backend sync for persistent state

---

## 4. Auxiliary Modules

### 4.1 Design System
- Custom `CulinaryTheme` extending MaterialTheme
- Figma Tokens → JSON → Kotlin theme codegen
- Three variants: Light, Dark, Seasonal
- Token categories: color, typography, spacing, shape
- AI-assisted palette generation (via MCP or CLI)

### 4.2 AI / LLM Integration

Tiered approach — use the right tool for each job:

**Tier 1: Cloud LLM** (conversational, knowledge-heavy tasks)
- Cooking assistant Q&A, recipe adaptation (“make this vegan”), substitution reasoning, pairing suggestions
- Ktor backend proxies calls to cloud provider (Claude, OpenAI, etc.), validates structured JSON against shared Kotlin types
- Provider-agnostic interface: backend abstracts the LLM behind a service, frontend doesn't know or care which model is used

**Tier 2: Local LLM** (optional, deferred until RAM budget allows)
- Mechanical extraction tasks: recipe parsing from pasted text/URLs → structured JSON, ingredient categorization
- Could run via Ollama on NUC if/when RAM is upgraded (currently 32GB, shared with other services)
- Not needed at MVP — cloud model handles these tasks fine

**Tier 3: No LLM needed** (deterministic)
- Static substitution database, unit conversion, “what can I make” (DB query: pantry ∩ recipes), search/filtering

Cross-cutting:
- Caching for repeated requests
- Semantic search via pgvector on PostgreSQL + embeddings (“recipes with warm spices”, “something light for summer”)
- Schema-based structured prompts with output conforming to shared Kotlin domain models

### 4.3 Sync & Backup (optional)
- Use a sync library (e.g. PowerSync, ElectricSQL) rather than hand-rolling
- Local-first data model; sync sits below the repository layer (stores/screens don't know about it)
- JSON export/import for user data

---

## 5. MVP Scope

Minimum viable product should include:

- Recipe Library (CRUD + import)
- Pantry Tracker (manual entry + “What can I make?”)
- Substitution Engine (static + basic LLM)
- Basic `CulinaryTheme` (one color scheme)
- Offline persistence
- Compose Desktop + Android support

Stretch goals:
- Shopping List auto-generation
- Step-by-step Assistant
- Light/Dark themes

---

## 6. Future Extensions

- Voice command interface
- Nutritional database integration
- Smart kitchen device support (IoT timers, scales)
- Family sharing / multi-user sync
- “Culinary Journal” mode (meal logging)
- Seasonal theme switching (AI-assisted)
- Recipe graph visualization (ingredients ↔ flavor network)

---

## 7. Development Priorities

1. Migrate backend: Spring Boot → Ktor + Exposed (GraalVM native image target)
2. Shared KMP domain models used by both frontend (SQLDelight) and backend (Exposed)
3. Introduce Koin for DI across frontend and backend
4. Recipe & pantry core logic
5. LLM integration (local Ollama + structured output pipeline)
6. Sync via library (below repository layer)
7. MVP release for personal daily use
8. Polish + design iterations

---

## 8. Guiding Principles

- **Local-first**: must remain useful offline.
- **Ergonomic**: frictionless UX for in-kitchen use.
- **Aesthetic**: cohesive, calm, timeless visual tone.
- **Extensible**: codebase designed for modular feature growth.
- **Educational**: serve as a learning lab for Kotlin, Compose, and AI integration.

---

*Document version: 2026-05-01*