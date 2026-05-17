# AGENTS.md

## Coding style

Write code for humans.

### Structure rules
- Prefer compact methods over extracting tiny helpers.
- Do NOT create helper methods used only once unless they hide real complexity.
- Do NOT split 3 lines into a separate method just to give them a name.
- Extract a method only if at least one of these is true:
  1. the logic is reused,
  2. the extracted block is genuinely complex,
  3. the extraction improves readability materially,
  4. the extraction isolates side effects or an important invariant.

### Comments
- Prefer short, direct comments when intent is not obvious.
- Do not replace useful comments with meaningless helper names.
- Do not add verbose docstrings for trivial code.

### Refactoring policy
- Avoid over-abstraction.
- Avoid premature generalization.
- Avoid creating wrappers, factories, adapters, mappers, or utility classes unless there is a concrete need.
- Keep related logic together when it is only used in one place.

### Editing policy
- First preserve the existing style of the file.
- Make the smallest sane change.
- Do not “clean up” unrelated code.
- Do not introduce architectural churn without being asked.

### Forbidden patterns
- Single-use private helpers with 1–5 trivial lines.
- Chains of tiny methods that force the reader to jump around the file.
- Renaming code into indirection instead of explaining intent.

### Preferred outcome
The result should usually be fewer moving parts, fewer files, fewer helpers, and clearer local reasoning.