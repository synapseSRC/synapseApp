# AI self-healing release build

The `🔧 Build` workflow (`build.yml`) runs `./gradlew assembleRelease`. When that
build fails on a **source-code** error, the same job now diagnoses, repairs and
rebuilds itself with the [Kilo Code CLI](https://kilo.ai/docs/code-with-ai/platforms/cli)
before giving up.

Nothing else about the pipeline changed: the same job, the same runner, the same
working directory, the same Gradle caches, the same verification step, the same
artifact upload and the same Telegram delivery.

## Control flow

```
checkout → JDK → google-services.json → Gradle setup
   └─ ai_build_repair.sh
        attempt 1: ./gradlew assembleRelease                      (streamed, like before)
          success ─────────────────────────────────────────────► verification → artifacts → done
          failure → classify
              infrastructure → rebuild (bounded, no AI attempt spent)
              source code    → capture log → Kilo `--auto` edit → guard rails → rebuild
        … up to AI_REPAIR_MAX_BUILD_ATTEMPTS (default 5) total build attempts
   └─ verify shipped auth and push config                        (unchanged, gates the commit)
   └─ ai_commit_repairs.sh   one commit, only if a repair happened and everything passed
   └─ rename + upload APK artifacts                              (unchanged)
```

Files:

| File | Role |
| --- | --- |
| `ai_build_repair.sh` | the repair loop: build, classify, capture, invoke the agent, rebuild, verify |
| `ai_commit_repairs.sh` | the single consolidated repair commit (skipped when no repair was needed) |
| `ai_repair_guard.sh` | shared redaction, path policy, classification and change detection |
| `kilo_repair_config.jsonc` | Kilo CLI config: model `kilo/kilo-auto/free` + permission policy |

## Guarantees this implementation is built around

* **A green build never calls the agent.** The CLI is not even installed unless a
  build has already failed with a code error.
* **One job, one workspace, no intermediate commits.** The agent edits the working
  tree in place; commits are made only by `ai_commit_repairs.sh`, after the release
  build **and** the existing release verification step have passed.
* **Bounded work.** At most 5 build attempts (configurable), at most 4 agent runs
  (attempts − 1), an infrastructure retry budget (default 2) inside that cap, a
  per-agent wall-clock timeout, and a 240-minute step timeout. The loop cannot spin.
* **Infrastructure failures are not code failures.** `Could not resolve …`,
  HTTP 429/5xx from a repository, DNS/TLS/timeout errors, `OutOfMemoryError`,
  daemon loss and similar are retried directly, and never consume an AI repair.
* **The agent cannot publish anything.** It runs under `env -i` with only `PATH`,
  `HOME`, `TMPDIR`, `LANG`, proxy variables and `KILO_API_KEY`: it never sees
  `GITHUB_TOKEN` or any repository secret. `git commit`, `git push`, `git reset`,
  package installs and network commands are denied in its permission policy, and a
  commit it manages to make anyway is undone (`git reset --mixed`) with the fix left
  in the tree.
* **Out-of-scope edits are reverted, not trusted.** `path_is_allowed()` re-derives
  the same policy the config expresses and reverts anything outside production
  source in `app/` and `shared/`: workflows, Gradle/build scripts, the wrapper,
  `gradle.properties`, signing material, `google-services.json`, `local.properties`,
  test source sets and build output. `ai_commit_repairs.sh` refuses to commit if
  anything out of scope is still present.
* **Tests cannot be weakened to get green.** Test source sets are outside the
  allow-list, test tasks are denied to the agent, and the pipeline adds no tests.
* **The agent CLI comes from a package registry, not a pipe to a shell.** It is installed
  from npm (`@kilocode/cli`, pin with `AI_REPAIR_KILO_VERSION`) and only lazily, after a
  code failure. The vendor's remote shell installer is never used unless the repository
  sets `AI_REPAIR_ALLOW_CURL_INSTALL=true`, because it executes downloaded code on a runner
  that holds this step's build secrets.
* **Secrets never reach a log.** Every line printed, teed or archived passes through
  a redaction filter that removes the API key exactly, plus JWTs, GitHub tokens and
  bearer headers. The key is also removed from the Gradle environment.
* **Least privilege.** The workflow gets `contents: read` (+`actions: read`); only the
  build job widens to `contents: write`, which is exactly what the one push needs. The
  checkout runs with `persist-credentials: false`, so no credential is written into
  `.git/config` and the build step - the one that runs AI-generated code on a failure -
  holds no usable credential at all. The job's token is materialised only in the
  `Commit AI repairs` step's environment and passed to git as a one-shot
  `http.extraheader` for the push and the push-retry rebase.
* **Failures are checked by hand.** The scripts deliberately do not use `set -e`: the loop
  inspects the status of commands that are allowed to fail (`grep`, `git diff --cached
  --quiet`, the push retry path) and an unexpected abort mid-sequence would be worse than
  the explicit checks. Every command whose failure would corrupt state or silently drop a
  repair - resolving `HEAD`, staging, committing, pushing - is checked and reported as an
  `::error`.

## Configuration

| Name | Kind | Purpose |
| --- | --- | --- |
| `KILO_API_KEY` | secret (required to enable repair) | Kilo Gateway credential. Without it the loop reports the build failure instead of pretending to repair. |
| `KILO_REPAIR_MODEL` | variable (optional) | overrides the primary model; default `kilo/kilo-auto/free` |
| `KILO_REPAIR_FALLBACK_MODELS` | variable (optional) | space-separated extra models tried when the primary is rate limited |
| `AI_REPAIR_MAX_BUILD_ATTEMPTS` | step env | total build attempts, default `5` |
| `AI_REPAIR_MAX_INFRA_RETRIES` | step env | infrastructure-only retries inside that cap, default `2` |
| `AI_REPAIR_KILO_VERSION` | step env | pins the npm package version of the CLI |
| `AI_REPAIR_ALLOW_CURL_INSTALL` | variable (optional) | set to `true` to allow the vendor's remote shell installer as a fallback; off by default |
| `AI_REPAIR_MAX_DIAGNOSTIC_ANNOTATIONS` | step env | annotation lines taken from the last failing attempt, default `6` |

Free tiers are rate limited upstream (Kilo documents ~200 requests/hour for
`kilo-auto/free`), so the model chain is not a formality: a rate-limited or
unavailable model advances the chain — including free models the CLI's own
`kilo models` catalog advertises — and only costs wall clock, not a build attempt.

Diagnostics for every attempt (Gradle log, the exact prompt, the agent's output,
the model catalog) are uploaded as the `ai-repair-diagnostics-*` artifact.

## Known behaviour worth knowing

* The APK is built from the tree that the repair produced, so its `GIT_COMMIT_HASH`
  identifies the commit that *triggered* the run, not the repair commit that the job
  pushes afterwards. Building after the commit is not possible without a second job,
  which the design forbids.
* The repair commit is pushed with `GITHUB_TOKEN`. GitHub does not start new workflow
  runs for events caused by that token, which is what keeps a repair from
  re-triggering the job that produced it.
* A repair is only ever committed when the build and the release verification both
  pass; a failed cycle leaves the branch untouched and the job red.

## Annotation budget

GitHub Actions surfaces at most **10 error annotations per step**. The pipeline therefore
streams the diagnosis of every attempt as plain log text and stores it per attempt
(`attempt-<n>-gradle-diagnostics.txt`, inside the uploaded artifact), and spends the step's
annotation budget on the loop's own verdict plus at most `AI_REPAIR_MAX_DIAGNOSTIC_ANNOTATIONS`
(default 6) lines from the **last still-failing** attempt. Annotating every attempt would push
the verdict — "KILO_API_KEY is not configured", "CLI unavailable", "attempts exhausted" — out
of the budget exactly when an operator needs it.

## Tests

`outputs/ai-repair-tests/harness.py` drives the real scripts, guard rails, classifiers and
workflow wiring against stub `gradlew`/`kilo` executables in throw-away git workspaces. It
covers 82 assertions across: green first attempt (agent not invoked, no commit), repair-then-green,
attempt exhaustion, infrastructure classification, out-of-scope reverts, undo of an agent-created
commit, redaction, rate-limit model fallback, auth abort, missing secret, the consolidated
commit + push, refusal in PR-head/tag contexts, the APK guard, test-source protection and the
annotation budget. It found and fixed four real defects before the first live run, and covers the five
security/robustness findings raised by the PR review bots (installer policy, redaction
delimiter with hostile keys, missing `-e`, credential scoping, explicit status checks).
