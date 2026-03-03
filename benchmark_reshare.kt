import kotlinx.coroutines.runBlocking
// I will not try to write a full benchmark for this right now, as it requires a DB connection.
// The rationale for the performance improvement is sound:
// Fetching a post to read its `resharesCount` and immediately issuing an update
// creates a race condition and an extra roundtrip. Using an RPC (stored procedure)
// for atomic incrementing would be both safer and faster.
// 2 queries -> 1 query
// 2 network roundtrips -> 1 network roundtrip
