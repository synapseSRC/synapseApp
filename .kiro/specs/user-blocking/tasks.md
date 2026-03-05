# Implementation Plan: User Blocking Feature

## Overview

This plan implements a user blocking feature following Clean Architecture principles in a Kotlin Multiplatform (KMP) project. The implementation progresses from backend schema through domain, data, and UI layers, with incremental validation at each stage. All code follows the project's architectural guidelines with strict layer separation and no hardcoded values.

## Tasks

- [x] 1. Set up Supabase database schema and RLS policies
  - Create `blocks` table with proper columns (id, blocker_id, blocked_id, created_at)
  - Add unique constraint on (blocker_id, blocked_id) pair
  - Add check constraint to prevent self-blocking
  - Create indexes on blocker_id, blocked_id, and created_at columns
  - Implement Row Level Security (RLS) policies for SELECT, INSERT, and DELETE operations
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 5.4_

- [ ] 2. Implement domain layer interfaces and models
  - [x] 2.1 Create BlockedUser domain model
    - Define data class in `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/model/`
    - Include fields: id, blockedUserId, blockedUsername, blockedUserAvatar, blockedAt
    - Use kotlinx.datetime.Instant for timestamp
    - _Requirements: 3.2, 6.3_
  
  - [x] 2.2 Create BlockRepository interface
    - Define interface in `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/repository/`
    - Add suspend functions: blockUser, unblockUser, getBlockedUsers, isUserBlocked
    - All methods return Result types for error handling
    - _Requirements: 1.6, 2.7, 4.6, 5.4, 6.6, 9.5_

- [ ] 3. Implement domain layer use cases
  - [x] 3.1 Create BlockUserUseCase
    - Implement in `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/domain/usecase/blocking/`
    - Validate user is not blocking themselves
    - Inject BlockRepository and GetCurrentUserIdUseCase
    - _Requirements: 1.2, 1.6, 9.3_
  
  - [x] 3.2 Create UnblockUserUseCase
    - Implement in same package as BlockUserUseCase
    - Inject BlockRepository
    - _Requirements: 4.3, 4.6_
  
  - [x] 3.3 Create GetBlockedUsersUseCase
    - Implement in same package as BlockUserUseCase
    - Inject BlockRepository
    - _Requirements: 2.4, 2.7, 3.1_
  
  - [x] 3.4 Create IsUserBlockedUseCase
    - Implement in same package as BlockUserUseCase
    - Inject BlockRepository
    - _Requirements: 5.1, 5.4_

- [ ] 4. Implement data layer DTOs and mapper
  - [x] 4.1 Create BlockDTO and BlockWithUserDTO
    - Define in `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/dto/`
    - Use @Serializable annotation for Supabase serialization
    - Use @SerialName annotations for field mapping
    - Include UserProfileDTO for joined data
    - _Requirements: 6.1, 7.4_
  
  - [x] 4.2 Create BlockMapper
    - Implement in `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/mapper/`
    - Add toDomain and toDomainList functions
    - Convert ISO timestamp strings to Instant
    - _Requirements: 7.5_

- [ ] 5. Implement data layer data source
  - [x] 5.1 Create SupabaseBlockDataSource
    - Implement in `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/datasource/`
    - Inject SupabaseClient
    - Implement createBlock method with duplicate check
    - Implement deleteBlock method
    - Implement getBlockedUsers method with user profile join
    - Implement isUserBlocked method
    - All methods return Result types
    - _Requirements: 6.1, 6.2, 6.3, 6.5_
  
  - [ ]* 5.2 Write unit tests for SupabaseBlockDataSource
    - Test successful block creation
    - Test duplicate block prevention
    - Test unblock operation
    - Test fetching blocked users
    - Test error handling for network failures
    - _Requirements: 6.1, 6.2, 6.3, 6.5_

- [ ] 6. Implement data layer repository
  - [x] 6.1 Create BlockRepositoryImpl
    - Implement in `shared/src/commonMain/kotlin/com/synapse/social/studioasinc/shared/data/repository/`
    - Inject SupabaseBlockDataSource
    - Implement BlockRepository interface
    - Use withContext(Dispatchers.Default) for coroutine context
    - Check for existing blocks before creating new ones
    - Map DTOs to domain models using BlockMapper
    - _Requirements: 1.5, 2.4, 4.3, 5.1, 5.2, 5.3, 6.6, 7.2, 7.6_
  
  - [ ]* 6.2 Write unit tests for BlockRepositoryImpl
    - Test blockUser with duplicate prevention
    - Test unblockUser operation
    - Test getBlockedUsers with mapping
    - Test isUserBlocked check
    - Mock SupabaseBlockDataSource
    - _Requirements: 5.1, 5.2, 5.3, 7.6_

- [x] 7. Checkpoint - Ensure domain and data layers compile
  - Verify all domain and data layer files compile without errors
  - Ensure no backend types leak into domain layer
  - Confirm all interfaces properly implemented
  - Ask the user if questions arise

- [ ] 8. Implement UI layer ViewModel
  - [x] 8.1 Create BlockingUiState sealed class
    - Define in `app/src/main/java/com/synapse/social/studioasinc/feature/blocking/`
    - Include states: Idle, Loading, BlockSuccess, UnblockSuccess, Error
    - _Requirements: 1.3, 4.4, 9.1_
  
  - [x] 8.2 Create BlockingViewModel
    - Implement in same package as BlockingUiState
    - Inject all four use cases via constructor
    - Use StateFlow for uiState and blockedUsers
    - Implement blockUser, unblockUser, loadBlockedUsers, checkIfBlocked methods
    - Handle errors and update UI state appropriately
    - _Requirements: 1.2, 1.3, 2.4, 2.5, 4.3, 4.4, 5.1, 7.7, 9.1, 9.2_
  
  - [ ]* 8.3 Write unit tests for BlockingViewModel
    - Test blockUser success and error scenarios
    - Test unblockUser with list refresh
    - Test loadBlockedUsers with state updates
    - Mock all use cases
    - _Requirements: 1.2, 4.3, 2.4_

- [x] 9. Add string resources
  - Add all user-facing strings to `app/src/main/res/values/strings.xml`
  - Include: blocked_contacts, navigate_back, no_blocked_users, unblock_user_title, unblock_user_message, unblock, cancel, block_user, block_success, unblock_success, error_block_failed, error_unblock_failed, error_load_failed, error_already_blocked, error_cannot_block_self
  - _Requirements: 8.3, 9.6_

- [ ] 10. Implement blocked contacts list UI
  - [x] 10.1 Create BlockedUserItem composable
    - Implement in `app/src/main/java/com/synapse/social/studioasinc/feature/blocking/ui/`
    - Display user avatar, username, and unblock button
    - Use MaterialTheme.colorScheme for colors
    - Use Spacing tokens for dimensions
    - _Requirements: 3.2, 8.1, 8.2_
  
  - [x] 10.2 Create UnblockConfirmationDialog composable
    - Implement in same package as BlockedUserItem
    - Use AlertDialog with theme-compliant styling
    - Include confirm and dismiss actions
    - Use string resources for all text
    - _Requirements: 4.2, 8.4_
  
  - [x] 10.3 Create EmptyBlockedListContent composable
    - Implement in same package as BlockedUserItem
    - Display centered message when no blocked users
    - Use MaterialTheme for styling
    - _Requirements: 2.6, 8.1_
  
  - [x] 10.4 Create BlockedContactsScreen composable
    - Implement in same package as BlockedUserItem
    - Use Scaffold with TopAppBar
    - Display LazyColumn of blocked users
    - Show loading indicator during fetch
    - Handle empty state
    - Integrate UnblockConfirmationDialog
    - Observe ViewModel StateFlows
    - _Requirements: 2.3, 2.4, 2.5, 3.1, 3.3, 3.4, 4.1, 4.4, 8.1, 8.2, 8.5_

- [ ] 11. Integrate block action into post menu
  - [x] 11.1 Add "Block User" option to PostMenu composable
    - Locate existing PostMenu implementation
    - Add block option with appropriate icon
    - Show only for posts by other users (not current user)
    - Use string resource for label
    - _Requirements: 1.1_
  
  - [x] 11.2 Wire block action to ViewModel
    - Call BlockingViewModel.blockUser when option selected
    - Show confirmation message on success
    - Show error message on failure
    - _Requirements: 1.2, 1.3, 1.4_

- [ ] 12. Add blocked contacts navigation to settings
  - [x] 12.1 Add "Blocked Contacts" option to Privacy & Security section
    - Locate Settings screen implementation
    - Add navigation item in Privacy & Security section
    - Use string resource for label
    - _Requirements: 2.1, 2.2_
  
  - [x] 12.2 Wire navigation to BlockedContactsScreen
    - Add route for blocked contacts screen
    - Implement navigation action
    - _Requirements: 2.3_

- [ ] 13. Set up dependency injection
  - [x] 13.1 Register data layer components
    - Add SupabaseBlockDataSource to DI container
    - Add BlockRepositoryImpl as BlockRepository implementation
    - Inject SupabaseClient into data source
    - _Requirements: 7.7_
  
  - [x] 13.2 Register domain layer use cases
    - Add BlockUserUseCase to DI container
    - Add UnblockUserUseCase to DI container
    - Add GetBlockedUsersUseCase to DI container
    - Add IsUserBlockedUseCase to DI container
    - Inject BlockRepository into all use cases
    - _Requirements: 7.7_
  
  - [x] 13.3 Register UI layer ViewModel
    - Add BlockingViewModel to DI container
    - Inject all four use cases
    - _Requirements: 7.7_

- [x] 14. Final checkpoint - Integration testing
  - Verify block action appears in post menu
  - Test blocking a user from post menu
  - Navigate to blocked contacts from settings
  - Verify blocked user appears in list
  - Test unblocking a user
  - Verify duplicate block prevention
  - Test error scenarios (network failures)
  - Ensure all UI follows theme guidelines
  - Ensure all tests pass
  - Ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Implementation follows strict Clean Architecture with no layer violations
- All UI components use MaterialTheme and Spacing tokens (no hardcoded values)
- All user-facing text uses string resources
- Supabase RLS policies ensure data security at database level
- Result types provide comprehensive error handling throughout the stack
