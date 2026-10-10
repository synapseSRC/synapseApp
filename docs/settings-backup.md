# Settings backup and restore

## Data and security

Android settings are read from the primary SettingsDataStore (DataStore Preferences, file name synapse_user_settings), not the legacy synapse_settings SharedPreferences store. SettingsBackupCodec is the allowlist for exported keys and their types. It intentionally excludes search history and device/account security state (biometric/app/chat locks and two-factor flags); no credentials, API keys, auth tokens, account identity, message history, posts, or media are serialized. Current backups use version 2. Unknown preference keys within that version are ignored, malformed known values are rejected, and unsupported older or future versions are rejected before the DataStore transaction starts. The prior version-1 format read SharedPreferences and may be incomplete, so it is rejected instead of being applied to DataStore. Restore clears/replaces only allowlisted keys in one DataStore edit, so invalid input cannot partially modify settings.

Local snapshots live in the app's private files directory. Import/export uses Android's Storage Access Framework, so no broad storage permission is requested by this feature. Imported files are size-limited and validated before being staged for restore.

Cloud snapshots use one JSONB row per authenticated user in public.settings_backups (user_id UUID primary key referencing auth.users, backup_json JSONB, and server-updated updated_at). Row-level security allows select/insert/update/delete only when auth.uid() = user_id; the Android client also filters by its current auth user and uses no service-role credential. This schema and its policies have been applied to the selected Synapse Supabase project. At the user's request, the migration and RLS test SQL files were removed from the PR branch; this repository therefore no longer contains executable SQL to provision the table or policies in another environment.

## Sync and conflict policy

- Cloud writes happen only after the user selects Back up now, or after a prior shared baseline exists and a subsequent settings change is detected while the cloud destination is active.
- Entering/resuming the cloud destination performs a sync check. While that destination is active, DataStore changes are debounced before sync. Local-only changes update the matching cloud revision; cloud-only changes download only if local settings have not changed during the operation. Equal snapshots advance the per-user baseline.
- A baseline is kept in app-private storage under a hash of that user's ID. Sign-out/account changes invalidate open cloud restore/conflict actions; the app never applies another account's cached snapshot.
- If both local and cloud have diverged from the last successful baseline, neither side is overwritten automatically. The screen asks the user to keep this device's copy or the cloud copy. Before applying a choice it checks the cloud row's server updated_at; a newer cloud revision reopens the conflict with current data.
- First-time sync with no cloud snapshot does not silently upload local settings. Back up now is the explicit initial upload action.

The app does not create or repair the Supabase schema at runtime. Each additional environment must be provisioned with an equivalent owner-scoped table, policies, and updated_at trigger before cloud backup can work. If the schema is missing, the UI reports a cloud error and keeps local preferences intact.
