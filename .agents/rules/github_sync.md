# Continuous Documentation & GitHub Synchronization Rule

## Core Directives
1. **Always Keep Documentation Synchronized**:
   - Whenever any feature, bug fix, UI change, or architectural refactor is made, ensure all relevant documentation (such as `README.md` and codebase docs) is updated to reflect the new state.
2. **Always Synchronize with GitHub**:
   - After completing code modifications and ensuring `./gradlew testDebugUnitTest` and/or `./gradlew assembleDebug` pass, automatically commit all changes with a clean conventional commit message and push to the remote GitHub repository (`origin/master` or active branch).
3. **Preserve Build Integrity**:
   - Never push broken builds. Always verify that Gradle builds with JDK 17 without compilation errors before pushing to GitHub.
