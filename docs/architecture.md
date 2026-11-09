# State and playback ownership

`BlanketState` holds the catalog, named presets, global playback and preferences. It exposes immutable state transitions. Preset changes retain their own sound levels, while main volume belongs to the whole app.

Two designs were considered. One ExoPlayer per enabled recording supports the original independent loops with few dependencies. A custom PCM mixer feeding a single player would make one stream for the session, but needs decoding, resampling, buffering and a custom media source. The chosen design uses independent ExoPlayers and a Media3 SimpleBasePlayer adapter that presents presets as the session playlist. This keeps next/previous behavior and metadata meaningful without letting a silent proxy player drift from actual mixer state.

The repository is the only state writer. DataStore serializes edits, including commands from the UI and media session. The service owns all players, audio focus, receivers, the optional wake lock, and the session. The activity observes state and starts the service when playback needs it. Compose renders the state and issues transitions through the repository. No player belongs to a Composable.

Robolectric tests use the same session adapter with a recording backend. The production backend is separately tested for ExoPlayer repeat mode, gains, pause and release. Native audio output and timing remain hardware validation.

Storage work uses an IO-dispatched scope sharing the application job. Repository commands and state collection keep the main dispatcher, which preserves submission order without running serialization or fsync on the UI thread. Foreground promotion reuses the active media notification when reacquiring focus so a volume-only change cannot erase transport actions.
