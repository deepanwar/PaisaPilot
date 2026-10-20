
# Android Development Instructions

## Technology
- Kotlin
- Jetpack Compose
- Material 3 Expressive
- Gradle Kotlin DSL
- MVVM with unidirectional data flow
- Coroutines and StateFlow

## Source of truth
Read the relevant files in docs/ before implementing.
If documentation conflicts, report the conflict.
Do not silently change product requirements.

## Compose rules
- Prefer Material 3 components over custom equivalents.
- Use expressive components where appropriate.
- Follow the project's MaterialTheme.
- Hoist state appropriately.
- Keep business logic outside Composables.
- Create previews for important UI components.
- Support dark theme and different screen sizes.
- Respect accessibility and reduced-motion needs.
- Verify experimental APIs before using them.

## Architecture
- ViewModels expose immutable UI state.
- Screens render state and emit user actions.
- Repositories manage data access.
- Do not pass repositories directly into UI components.
- Keep feature boundaries clear.
- Avoid unnecessary complexity.

## Development process
1. Inspect existing files before editing.
2. Explain the scope of important changes.
3. Implement one feature at a time.
4. Build and run relevant tests.
5. Fix failures before completing the task.
6. Report files changed and tests performed.
7. Never claim a test passed unless it ran.

## Safety
- Never commit API keys or credentials.
- Do not delete unrelated files.
- Do not upgrade dependencies without justification.
- Do not implement undocumented backend behavior.