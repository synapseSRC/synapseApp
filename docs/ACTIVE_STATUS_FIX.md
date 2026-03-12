# Active Status Fix - March 12, 2026

## Problem
Active status indicators were not showing users as online even when they were recently active.

## Root Cause
The active status logic was checking TWO conditions:
1. `is_online` flag must be `true`
2. `last_seen` timestamp must be within 5 minutes

**The Issue:** When users navigate away from the app or it goes to background, `stopPresenceTracking()` is called, which immediately sets `is_online = false`. This meant users would NEVER show as active because the flag was always false, even if they were just active seconds ago.

## Solution
Changed the active status detection to rely ONLY on the `last_seen` timestamp:
- User is considered active if `last_seen` is within **2 minutes** (reduced from 5 for better accuracy)
- Removed dependency on the unreliable `is_online` flag
- The `is_online` flag is still updated for other purposes, but not used for active status display

## Changes Made

### 1. SupabasePresenceRepository.kt
- **`observeUserPresence()`**: Now checks only `last_seen` timestamp with 2-minute window
- **`isUserInChat()`**: Removed `is_online` check, uses only `last_seen` and `current_chat_id`
- **`isWithinActiveWindow()`**: Added `windowMinutes` parameter for flexibility

### 2. User.kt (Domain Model)
- Removed the incorrect `isActive` computed property that was checking wrong fields

### 3. Documentation Updates
- Updated `ACTIVE_STATUS.md` to reflect new logic
- Clarified that `is_online` flag is not used for active status detection

## Testing
To verify the fix works:

1. **Check database:**
   ```sql
   SELECT 
       user_id,
       last_seen,
       EXTRACT(EPOCH FROM (NOW() - last_seen))/60 as minutes_ago
   FROM user_presence 
   WHERE EXTRACT(EPOCH FROM (NOW() - last_seen))/60 < 2
   ORDER BY last_seen DESC;
   ```

2. **In the app:**
   - Open the app and navigate to a screen showing user avatars
   - Users active within the last 2 minutes should show green indicator
   - Close/background the app
   - Within 2 minutes, the user should still show as active to others

## Benefits
- ✅ More reliable active status detection
- ✅ Users show as active even after closing the app (for 2 minutes)
- ✅ Reduced active window from 5 to 2 minutes for better accuracy
- ✅ Simpler logic, easier to maintain

## Migration Notes
No database migration needed. This is a client-side logic change only.

## Future Improvements
- Consider implementing Supabase Realtime Presence API instead of polling
- Add user preference to hide active status
- Implement "Last seen X minutes ago" text for offline users
