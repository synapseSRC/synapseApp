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
* **Secrets never reach a log.** Every line printed, teed or archived passes through
  a redaction filter that removes the API key exactly, plus JWTs, GitHub tokens and
  bearer headers. The key is also removed from the Gradle environment.
* **Least privilege.** The workflow gets `contents: read` (+`actions: read`); only the
  build job widens to `contents: write`, which is exactly what the one push needs.

## Configuration

| Name | Kind | Purpose |
| --- | --- | --- |
| `KILO_API_KEY` | secret (required to enable repair) | Kilo Gateway credential. Without it the loop reports the build failure instead of pretending to repair. |
| `KILO_REPAIR_MODEL` | variable (optional) | overrides the primary model; default `kilo/kilo-auto/free` |
| `KILO_REPAIR_FALLBACK_MODELS` | variable (optional) | space-separated extra models tried when the primary is rate limited |
| `AI_REPAIR_MAX_BUILD_ATTEMPTS` | step env | total build attempts, default `5` |
| `AI_REPAIR_MAX_INFRA_RETRIES` | step env | infrastructure-only retries inside that cap, default `2` |

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
