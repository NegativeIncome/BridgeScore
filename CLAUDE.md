# BridgeScore Android App — Claude Code Guide

## Project Purpose

Android app (Kotlin + Jetpack Compose) for tracking duplicate bridge contracts and computing
scores during club sessions. Supports Howell (3/4/5 tables) and Mitchell movements, Room
database persistence, portrait-only layout. Up to 36 boards per session.

## Key Paths

| Area | Path |
|------|------|
| Data models | `app/src/main/java/com/bridgescore/data/model/` |
| Room DB | `app/src/main/java/com/bridgescore/data/db/` |
| Scoring engine | `app/src/main/java/com/bridgescore/scoring/` |
| Movement tables | `app/src/main/java/com/bridgescore/scoring/movement/` |
| ViewModel | `app/src/main/java/com/bridgescore/ui/viewmodel/` |
| Screens | `app/src/main/java/com/bridgescore/ui/screens/` |
| Navigation | `app/src/main/java/com/bridgescore/navigation/` |
| Drawables | `app/src/main/res/drawable/` |

## Build

```
.\gradlew.bat assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

Environment variables needed:
- `JAVA_HOME=C:\Program Files\Android\openjdk\jdk-21.0.8`
- `ANDROID_HOME=C:\Android\Sdk`

---

## Duplicate Bridge Scoring Rules

All scoring is duplicate (not rubber). Score is always from NS perspective: positive = NS plus.

### Trick Values (bid tricks only)
| Suit | Per trick |
|------|-----------|
| Clubs, Diamonds | 20 |
| Hearts, Spades | 30 |
| No Trump | 40 first + 30 each subsequent |

Doubled: ×2. Redoubled: ×4.

### Game / Part-Score Bonus
| Result | Non-Vul | Vul |
|--------|---------|-----|
| Game (trick score ≥ 100) | +300 | +500 |
| Part score | +50 | +50 |

### Slam Bonuses (on top of game bonus)
| Slam | Non-Vul | Vul |
|------|---------|-----|
| Small slam (level 6) | +500 | +750 |
| Grand slam (level 7) | +1000 | +1500 |

### Making Doubled/Redoubled — Insult Bonus
- Doubled: +50
- Redoubled: +100

### Overtricks
| Doubled state | Non-Vul | Vul |
|---------------|---------|-----|
| Undoubled | trick value per overtrick | trick value per overtrick |
| Doubled | +100 each | +200 each |
| Redoubled | +200 each | +400 each |

### Undertrick Penalties
**Undoubled:** −50/trick NV, −100/trick V

**Doubled, Non-Vul:** −100 first, −200 second & third, −300 each subsequent

**Doubled, Vul:** −200 first, −300 each subsequent

**Redoubled, Non-Vul:** −200 first, −400 second & third, −600 each subsequent

**Redoubled, Vul:** −400 first, −600 each subsequent

---

## Vulnerability by Board Number

Repeating 16-board cycle. Index = (boardNumber − 1) % 16:

```
 0:None  1:NS   2:EW   3:Both
 4:NS    5:EW   6:Both 7:None
 8:EW    9:Both 10:None 11:NS
12:Both 13:None 14:NS  15:EW
```

---

## UI Requirements

- **Portrait only** (locked in manifest)
- **One board at a time** on the entry screen
- **Input controls per board:**
  - Level buttons: 1–7
  - Suit buttons: ♣ ♦ ♥ ♠ NT (suit symbols/images where possible)
  - Declarer: N, S, E, W
  - Double / Redouble toggle buttons
  - Result: tricks made (0–13) with +/− shortcut buttons relative to contract
  - Computed score displayed live
  - Passed-out checkbox
- **Navigation:** Previous board, Next board, Jump to any board number, Save
- **Summary screen** (on demand): all boards, contract string, tricks made, score; tap row to edit
- **Session header info:** board number, vulnerability, opponent pair number, next table

---

## Movement Requirements

### Howell
- All pairs move each round
- 3 tables: 6 pairs, 5 rounds, 2 boards/round (10 boards)
- 4 tables: 8 pairs, 7 rounds, 4 boards/round (28 boards)
- 5 tables: 9 pairs, 9 rounds, 2 boards/round (18 boards) — pair 1 sits out
- Movement tables hardcoded in `HowellMovement.kt`
- Per board: show opponent pair number and next table to move to

### Mitchell
- NS pairs stay fixed; EW pairs and boards move up/down each round
- Generative (not hardcoded) — works for any number of tables
- Implemented in `MitchellMovement.kt`

### Extensibility
- `getMovement(tables: Int)` is the single entry point; add more table counts there
- Movement type stored per session in the database

---

## Database Schema (Room)

### Session
| Field | Type | Notes |
|-------|------|-------|
| id | Long | auto PK |
| date | String | yyyy-MM-dd |
| partner | String | partner name |
| pairNumber | Int | our pair number |
| movementType | MovementType | HOWELL or MITCHELL |
| numberOfTables | Int | |
| boardCount | Int | actual boards played |

### BoardResult
| Field | Type | Notes |
|-------|------|-------|
| id | Long | auto PK |
| sessionId | Long | FK → Session |
| boardNumber | Int | |
| opponentPairNumber | Int | from movement table |
| declarer | String | N/S/E/W |
| level | Int | 1–7 |
| suit | Suit | CLUBS/DIAMONDS/HEARTS/SPADES/NOTRUMP |
| doubled | Doubled | NONE/DOUBLED/REDOUBLED |
| tricksMade | Int | actual tricks won by declarer |
| score | Int | NS-perspective duplicate score |
| passed | Boolean | passed-out board |

---

## Subagent Policy — IMPORTANT

**Delegate all non-trivial file edits to a subagent.** This keeps the main context window
small and prevents Claude crashes on large sessions.

### When to use a subagent
- Any task that touches more than one file
- Writing or rewriting a complete screen, ViewModel, or scoring class
- Bug fixes that require reading + editing multiple files
- Any task where you would otherwise read several files into the main context

### When NOT to use a subagent
- Single-line or single-block fixes where you already have the file content in context
- Build/Gradle commands (run those directly)
- Questions, explanations, or planning — keep those in the main session

### How to invoke
Use the Agent tool with `subagent_type: "general-purpose"`. Write a self-contained prompt
that includes:
1. Exactly which files to read/edit
2. What change to make and why
3. Any constraints (e.g. don't change the public API, keep Room schema version)

The subagent result comes back as a summary — verify the key changes before reporting done.

You can also invoke `/edit-files` as a slash command to trigger the pattern explicitly.
