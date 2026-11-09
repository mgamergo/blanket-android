# AGENTS.md

## PROJECT

```text

NAME: Blanket for Android

DESC: Blanket for Android: a native Android port of the GNOME Blanket ambient sounds app

```

## RULES

### 0. NEW PROJECT RULES
Prefer NextJS, shadcn, convex, clerk, and Infisical when starting a new project. Prefer bun > pnpm in this order.

### 1. START
Read `logbook.md`.
Run `git status`. If the tree is dirty, stop and report. Otherwise, `git fetch` and rebase the feature branch on `origin/main`.
Don't start duplicate dev servers. Stop any processes that you started during the run.

### 2. RESEARCH
If something is unfamiliar or niche or requires substantial external knowledge, research before proceeding further.
Don't make uneducated assumptions. Keep research brief and to the point; don't overextend. Mention any big decisions made in the logbook.

### 3. LOGBOOK
`logbook.md` is project memory (decisions, blockers, next). Read before meaningful work; update after. Newest first. Keep short. No diary.

### 4. PLANNING
If there is any important decision made, explain why it's done that way and its tradeoffs compared to other routes

### 5. CODE
Always strive for simple solutions, quality code, type safety, and structure. Ask if a piece of code solving a problem can be written simpler and better. If so, do it. Don't use `any` and one-off clever code pieces unless absolutely necessary.
Before coding, search existing code and reuse any good patterns and suggest improvements for any bad patterns.
Write comments if it's necessary to explain a non-obvious behavior. Don't explain what the code does again in comments.

### 6. VERSION CONTROL
Always work on a feature branch. Commit locally unless I ask you to push.
Never add anything like "co-authored by: {agent_name}" or something equivalent.
Ignore local files, generated files, and secrets while committing.
PRs contain what changed, why, verification (images are also fine if needed for verification), and known limitations

### 7. SAFETY
Never run commands that modify data in a DB without the user's permission or modify system files.

### 8. DEPENDENCIES
Before adding one, check existing dependencies, check stdlib/platform.
Do not install packages for trivial tasks.

### 9. VERIFICATION
When starting a new ambiguous task, use the "grill-with-docs" skill to understand user intentions and turn them into tests for verification. Run lint commands and tests for affected areas after code changes.
Use the browser, or Playwright (if no browser is available) to verify your work and report proof of work with screenshots and recordings. Report any bugs/issues that need the user to solve them clearly with actionable items.
Run the "sol-loop" skill after lint/tests pass for any non-trivial change (features, refactors, bug fixes touching logic). Skip it for trivial edits (copy, styles, config tweaks, renames).
Run the "council" skill only when my prompt explicitly asks for it. Never run it on your own.

### 10. ADDITIONAL NOTES
Read `package.json` and understand commands avaliable. Setup commands with preferred package manager like lint, typecheck, test etc if not already present and use them.
