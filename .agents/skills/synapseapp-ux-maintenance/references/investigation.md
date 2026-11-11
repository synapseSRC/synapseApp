# Repository Investigation & Bug Qualification

## 1. Inspect the real repository first

Start from the actual repository state. Record the inspected branch, commit SHA, and relevant project documentation. Explore the implementation rather than inferring behavior from filenames, screenshots, issue titles, or assumptions.

Inspect relevant areas, including:

- UI components, screens, layouts, and shared design components.
- Navigation, routing, screen transitions, and back-button/gesture behavior.
- User interaction flows, callbacks, and event handlers.
- State management, state synchronization, and lifecycle behavior.
- Persistence, local storage, and business logic.
- API integrations, network states, and error handling.
- Accessibility and interaction support.
- Application architecture, dependencies, and relevant abstractions.
- Dark/light theme behavior.
- Responsive layouts and device-specific rendering.
- Build configuration and application infrastructure.

Follow the actual paths a user takes through the application. Trace the relevant code path far enough to understand the behavior and its cause.

## 2. Candidate defect patterns

Investigate user-visible failures such as:

- A button appears interactive but does nothing or triggers the wrong action.
- Navigation opens the wrong destination or loses required context.
- A dialog or sheet cannot be dismissed through expected interactions.
- A setting changes visually but is not persisted.
- Content is clipped or inaccessible at a supported screen size.
- A workflow leaves the user stuck or unable to recover.
- An interaction produces an incorrect or duplicate operation.
- An error state provides no usable recovery path.
- An animation disrupts layout or makes controls unusable.
- Stale content appears after navigation or state changes.
- A notification opens an incorrect screen.
- A loading state never resolves under a supported outcome.

These are examples, not permission to report a bug without evidence.

## 3. Qualification priority

Prefer defects in this order:

1. Completely or partially broken functionality.
2. Incorrect results from user interactions.
3. Navigation and workflow failures.
4. Persistence and state synchronization defects.
5. Misleading or incorrect UI behavior.
6. Accessibility barriers that affect real use.
7. Rendering defects on actual supported screen sizes/devices.
8. Performance problems that noticeably impair usability.

Every accepted finding must have a meaningful user consequence and a technically supportable cause.

Do not report speculative concerns, personal design preferences, hypothetical edge cases without evidence, unnecessary refactoring, or code-style disagreements.

## 4. Evidence standard

Before creating an issue or changing code:

1. Follow the relevant implementation across source files and dependencies.
2. Identify the component, function, event, state, or API path responsible.
3. Inspect related implementations to determine whether the behavior is handled elsewhere.
4. Review relevant project documentation and established conventions.
5. Distinguish observed behavior from behavior inferred from code.
6. Identify the exact user impact and why the implementation causes it.
7. Record source paths, symbol names, and accurate line references where available.

Do not claim a physical-device, emulator, or manual reproduction unless it actually happened. Clearly label inferred reproduction steps. Do not treat a code-level possibility as a confirmed runtime observation.

Discard findings whose evidence is insufficient or whose user impact cannot be established. Never alter application behavior based on an assumption.

## 5. Prioritization under execution limits

At most 2 new GitHub issues and 2 bug fixes may be handled in one execution. If there are more candidates, rank them by real user impact and evidence strength. Handle the highest-priority eligible findings and defer the rest with a concise reason. An existing duplicate does not count as a new issue.
