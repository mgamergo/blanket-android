# Sol-loop review

Scope is the native Android port and its test/build support. Remote publishing, Linux integration and real-device validation are out of scope.

Base is `e8de0dd`, the pre-implementation HEAD in this repository without a remote. User instructions prohibit fetching, adding a remote or pushing. The live T3 catalog exposes provider `codex`, model `gpt-6.1-sol`, reasoning effort `high`. The installed sol-loop skill is present and read. Default budget is three rounds with one reviewer at a time.

## Ledger

| ID | Severity | Round 1 finding | Resolution | State |
| --- | --- | --- | --- | --- |
| S1 | should | Unmuting reposted a bootstrap notification without transport controls. | Foreground promotion reuses the active media notification. Regression checks the action list and actually sends Pause after unmuting. | Resolved and approved in round 2. |
| S2 | should | DataStore inherited the main dispatcher and serialized/fsynced settings on the UI thread. | DataStore receives an IO-dispatched scope with the same application job. Repository commands and collection retain their main scope and ordering. A recording serializer exercises the production factory and asserts writes happen off the main looper. | Resolved and approved in round 2. |

Both regression checks were first run against the old behavior and failed. The focused checks and all 80 debug/release tests pass after the fixes; formatting, lint and APK assembly also pass. No findings were declined. The main-activity/picker integration test was also added and passes with the real activity and playback service.

Round 1 verdict was CHANGES. Its reproduction XML is retained at `app/build/review-evidence/round1-reproductions.xml`; it showed two failing assertions before fixes. Round 2 verdict was APPROVE. Both IDs were confirmed resolved, with no new findings. The reviewer checked current code and retained test evidence and did not rerun the full suite. Usage was not exposed and is not estimated. No push was performed.

The full change before review was 98 files, 9,030 insertions and 3 deletions, including original upstream SVG/license text and copied binary assets. Final size: 99 files changed, 9243 insertions(+), 3 deletions(-).

The loop converged in two rounds with GPT-6.1 Sol on high reasoning effort. No blocker or should finding remains open.
