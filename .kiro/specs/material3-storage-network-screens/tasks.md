# Implementation Plan: Material 3 Expressive Storage and Network Screens

## Overview

This implementation plan upgrades the Manage Storage and Network Usage screens to Material 3 Expressive design standards. The approach follows a phased migration strategy: preparation, ManageStorageScreen upgrade, NetworkUsageScreen upgrade, testing, and final integration. All tasks focus on replacing hardcoded values with theme system tokens while preserving existing functionality.

## Tasks

- [ ] 1. Preparation and string resources
  - Add all required string resources to `app/src/main/res/values/strings.xml`
  - Verify theme system tokens (ColorScheme, Spacing, Typography, Shapes) are available
  - Create feature branch for Material 3 upgrade
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 10.3, 10.4, 12.3, 12.4_

- [ ] 2. Upgrade ManageStorageScreen TopAppBar
  - [ ] 2.1 Replace TopAppBar with Material 3 implementation
    - Use `TopAppBarDefaults.pinnedScrollBehavior()`
    - Apply `MaterialTheme.colorScheme.surface` for container color
    - Use `stringResource(R.string.storage_manage_title)` for title
    - Apply `MaterialTheme.typography.titleLarge` for title style
    - Add back button with `stringResource(R.string.cd_back_button)` content description
    - _Requirements: 1.1, 2.1, 2.3, 2.5, 3.1, 3.3, 4.1, 4.3, 4.5, 10.1, 10.3, 10.5_

  - [ ]* 2.2 Write property test for ManageStorageScreen TopAppBar
    - **Property 9: Storage Screen TopAppBar Presence**
    - **Validates: Requirements 10.1**

- [ ] 3. Upgrade StorageUsageSection component
  - [ ] 3.1 Replace hardcoded colors with MaterialTheme.colorScheme tokens
    - Use `primary` for used storage text and Synapse bar segment
    - Use `onSurfaceVariant` for free storage text
    - Use `tertiary` for Apps & Other bar segment
    - Use `surfaceVariant` with alpha 0.3 for free space bar segment
    - _Requirements: 1.1, 2.1, 2.3, 2.5, 4.1, 4.3, 4.7, 5.4_

  - [ ] 3.2 Replace hardcoded dimensions with Spacing tokens
    - Use `Spacing.Medium` for content padding
    - Use `Spacing.Small` for storage bar height
    - Use `Spacing.Small` for badge size
    - Use `Spacing.Small` for badge to label spacing
    - Use `Spacing.Medium` for spacing between badges
    - _Requirements: 2.3, 2.7, 5.7, 11.1, 11.3_

  - [ ] 3.3 Apply Material 3 typography scale
    - Use `MaterialTheme.typography.titleLarge` for storage amounts
    - Use `MaterialTheme.typography.bodyMedium` for category labels
    - _Requirements: 1.1, 4.1, 4.3, 4.5, 5.1, 5.2, 5.3_

  - [ ] 3.4 Apply Material 3 shapes to storage bar
    - Use `MaterialTheme.shapes.small` for storage bar clip shape
    - _Requirements: 1.3, 1.5_

  - [ ] 3.5 Add content descriptions to storage badges
    - Use `stringResource(R.string.cd_storage_badge)` for badge icons
    - _Requirements: 12.1, 12.3_

  - [ ]* 3.6 Write property test for storage data display
    - **Property 1: Storage Data Display Completeness**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

- [ ] 4. Upgrade ReviewDeleteSection component
  - [ ] 4.1 Replace hardcoded colors with MaterialTheme.colorScheme tokens
    - Use `primary` for file type icons
    - Use `onSurface` for list item text
    - Use `surfaceContainerLowest` for section divider
    - _Requirements: 1.1, 2.1, 2.5, 4.7, 6.4_

  - [ ] 4.2 Replace hardcoded dimensions with Spacing tokens
    - Use `Spacing.Large` for section spacing
    - Use `Spacing.ExtraSmall` for list item vertical padding
    - Use `Spacing.Small` for divider thickness
    - Use `Spacing.Medium` for bottom content padding
    - _Requirements: 2.3, 2.7, 6.5, 11.1, 11.3_

  - [ ] 4.3 Apply Material 3 typography and list components
    - Use `MaterialTheme.typography.labelLarge` for section title
    - Use `MaterialTheme.typography.bodyLarge` for list item headline
    - Use `MaterialTheme.typography.bodyMedium` for list item supporting text
    - Use Material 3 ListItem component
    - _Requirements: 1.1, 4.1, 4.3, 4.5, 6.1, 6.2, 6.3_

  - [ ] 4.4 Add content descriptions to file icons
    - Use `stringResource(R.string.cd_file_icon)` for file type icons
    - Use `stringResource(R.string.cd_chevron_right)` for navigation chevron
    - _Requirements: 12.1, 12.3_

  - [ ] 4.5 Ensure minimum touch target sizes
    - Verify all list items meet 48dp minimum height
    - _Requirements: 12.5_

  - [ ]* 4.6 Write property test for large files display
    - **Property 2: Large Files Display**
    - **Validates: Requirements 6.1, 6.2**

- [ ] 5. Integrate ManageStorageScreen components
  - [ ] 5.1 Update main ManageStorageScreen composable
    - Wire TopAppBar, StorageUsageSection, and ReviewDeleteSection
    - Apply `Spacing.Medium` for content padding
    - Implement loading state with Material 3 CircularProgressIndicator
    - Use `MaterialTheme.colorScheme.primary` for progress indicator color
    - _Requirements: 1.1, 1.5, 2.1, 2.3, 5.6, 11.1, 13.1, 13.7_

  - [ ] 5.2 Verify StateFlow observation for storage data
    - Ensure `storageUsage`, `largeFiles`, and `isLoading` StateFlows are observed
    - _Requirements: 13.1, 13.5, 13.7, 14.5, 14.7_

  - [ ]* 5.3 Write property test for storage screen state reactivity
    - **Property 15: Storage Screen State Reactivity**
    - **Validates: Requirements 13.1, 13.7**

  - [ ]* 5.4 Write property test for storage screen back navigation
    - **Property 5: Storage Screen Back Navigation**
    - **Validates: Requirements 9.1, 9.5**

  - [ ]* 5.5 Write property test for storage screen interactive list items
    - **Property 8: Storage Screen Interactive List Items**
    - **Validates: Requirements 9.4**

- [ ] 6. Checkpoint - ManageStorageScreen complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Upgrade NetworkUsageScreen TopAppBar
  - [ ] 7.1 Replace TopAppBar with Material 3 implementation
    - Use `TopAppBarDefaults.pinnedScrollBehavior()`
    - Apply `MaterialTheme.colorScheme.surface` for container color
    - Use `stringResource(R.string.network_usage_title)` for title
    - Apply `MaterialTheme.typography.titleLarge` for title style
    - Add back button with `stringResource(R.string.cd_back_button)` content description
    - _Requirements: 1.2, 2.2, 2.4, 2.6, 3.2, 3.4, 4.2, 4.4, 4.6, 10.2, 10.4, 10.6_

  - [ ]* 7.2 Write property test for NetworkUsageScreen TopAppBar
    - **Property 10: Network Screen TopAppBar Presence**
    - **Validates: Requirements 10.2**

- [ ] 8. Upgrade network usage statistics container
  - [ ] 8.1 Replace hardcoded colors with MaterialTheme.colorScheme tokens
    - Use `surfaceContainer` for statistics container background
    - Use `onSurface` for statistics title and values
    - Use `onSurfaceVariant` for statistics labels
    - _Requirements: 1.2, 2.2, 2.4, 2.6, 4.2, 4.4, 7.3, 7.4_

  - [ ] 8.2 Replace hardcoded dimensions with Spacing tokens
    - Use `Spacing.Medium` for container padding
    - Use `Spacing.Medium` for internal spacing between sent/received columns
    - _Requirements: 2.4, 2.8, 7.5, 11.2, 11.4_

  - [ ] 8.3 Apply Material 3 typography and shapes
    - Use `MaterialTheme.typography.titleMedium` for usage section title
    - Use `MaterialTheme.typography.bodyMedium` for sent/received labels
    - Use `MaterialTheme.typography.headlineMedium` for sent/received values
    - Use `MaterialTheme.shapes.large` for container shape
    - _Requirements: 1.2, 1.4, 1.6, 4.2, 4.4, 7.1, 7.2, 11.6_

  - [ ]* 8.4 Write property test for network statistics display
    - **Property 3: Network Statistics Display Completeness**
    - **Validates: Requirements 7.1, 7.2, 7.3**

- [ ] 9. Upgrade network usage items list
  - [ ] 9.1 Replace hardcoded colors with MaterialTheme.colorScheme tokens
    - Use `primary` for category icons
    - Use `onSurfaceVariant` for upload/download icons
    - Use `onSurface` for list item text
    - _Requirements: 1.2, 2.2, 2.6, 4.2, 8.3, 8.4_

  - [ ] 9.2 Replace hardcoded dimensions with Spacing tokens
    - Use `Spacing.Large` for category icon size
    - Use `Spacing.SmallMedium` for upload/download icon size
    - Use `Spacing.ExtraSmall` for icon to text spacing
    - Use `Spacing.Medium` for spacing between upload/download indicators
    - Use `Spacing.Small` for spacing between list items
    - _Requirements: 2.4, 2.8, 8.5, 8.6, 11.2, 11.4_

  - [ ] 9.3 Apply Material 3 list components and typography
    - Use Material 3 ListItem component
    - Use `MaterialTheme.typography.bodyLarge` for list item headlines
    - Use `MaterialTheme.typography.bodyMedium` for list item supporting text
    - _Requirements: 1.2, 4.2, 4.4, 4.6, 8.1, 8.2_

  - [ ] 9.4 Add content descriptions to icons
    - Use `stringResource(R.string.cd_category_icon)` for category icons
    - Use `stringResource(R.string.cd_upload_icon)` for upload icons
    - Use `stringResource(R.string.cd_download_icon)` for download icons
    - _Requirements: 12.2, 12.4_

  - [ ] 9.5 Ensure minimum touch target sizes
    - Verify all list items meet 48dp minimum height
    - _Requirements: 12.6_

  - [ ]* 9.6 Write property test for network usage items display
    - **Property 4: Network Usage Items Display**
    - **Validates: Requirements 8.1, 8.2**

- [ ] 10. Upgrade network screen reset button and divider
  - [ ] 10.1 Replace reset button with Material 3 text button
    - Use Material 3 TextButton component
    - Apply `MaterialTheme.colorScheme.primary` for button text color
    - Use `MaterialTheme.typography.labelLarge` for button text style
    - Use `stringResource(R.string.network_reset_statistics)` for button text
    - Use `Spacing.Medium` for button padding
    - _Requirements: 1.2, 2.2, 2.4, 3.2, 4.2, 9.3_

  - [ ] 10.2 Add HorizontalDivider with Material 3 styling
    - Use `Spacing.Small` for divider vertical padding
    - _Requirements: 2.4, 11.2, 11.4_

  - [ ]* 10.3 Write property test for network screen reset button
    - **Property 7: Network Screen Reset Button**
    - **Validates: Requirements 9.3**

- [ ] 11. Integrate NetworkUsageScreen components
  - [ ] 11.1 Update main NetworkUsageScreen composable
    - Wire TopAppBar, statistics container, usage items list, and reset button
    - Apply `Spacing.Medium` for content padding
    - Implement loading state with Material 3 CircularProgressIndicator
    - Use `MaterialTheme.colorScheme.primary` for progress indicator color
    - _Requirements: 1.2, 1.6, 2.2, 2.4, 7.6, 11.2, 13.2, 13.8_

  - [ ] 11.2 Verify StateFlow observation for network data
    - Ensure `usageItems`, `totalSent`, `totalReceived`, and `isLoading` StateFlows are observed
    - _Requirements: 13.2, 13.4, 13.6, 13.8, 14.6, 14.8_

  - [ ]* 11.3 Write property test for network screen state reactivity
    - **Property 16: Network Screen State Reactivity**
    - **Validates: Requirements 13.2, 13.8**

  - [ ]* 11.4 Write property test for network screen back navigation
    - **Property 6: Network Screen Back Navigation**
    - **Validates: Requirements 9.2, 9.6**

- [ ] 12. Checkpoint - NetworkUsageScreen complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 13. Write accessibility property tests
  - [ ]* 13.1 Write property test for storage screen icon accessibility
    - **Property 11: Storage Screen Icon Accessibility**
    - **Validates: Requirements 12.1**

  - [ ]* 13.2 Write property test for network screen icon accessibility
    - **Property 12: Network Screen Icon Accessibility**
    - **Validates: Requirements 12.2**

  - [ ]* 13.3 Write property test for storage screen touch target sizes
    - **Property 13: Storage Screen Touch Target Sizes**
    - **Validates: Requirements 12.5**

  - [ ]* 13.4 Write property test for network screen touch target sizes
    - **Property 14: Network Screen Touch Target Sizes**
    - **Validates: Requirements 12.6**

- [ ]* 14. Write unit tests for edge cases
  - [ ]* 14.1 Write unit test for storage screen loading state
    - Test that CircularProgressIndicator is shown when `isLoading = true`
    - Test that CircularProgressIndicator is shown when `storageUsage = null`

  - [ ]* 14.2 Write unit test for storage screen empty large files
    - Test that ReviewDeleteSection shows "0 B" when `largeFiles` is empty

  - [ ]* 14.3 Write unit test for network screen loading state
    - Test that CircularProgressIndicator is shown when `isLoading = true`

  - [ ]* 14.4 Write unit test for network screen empty usage items
    - Test that empty list is displayed when `usageItems` is empty

  - [ ]* 14.5 Write unit test for storage screen zero storage
    - Test display when `usedSize = 0` (empty storage bar)

  - [ ]* 14.6 Write unit test for storage screen full storage
    - Test display when `freeSize = 0` (100% filled storage bar)

  - [ ]* 14.7 Write unit test for network screen zero usage
    - Test display when `totalSent = 0` and `totalReceived = 0`

- [ ]* 15. Integration and accessibility testing
  - [ ]* 15.1 Run Compose UI tests for both screens
    - Test complete screen rendering with mock ViewModels
    - Verify theme system integration
    - Test user interaction flows

  - [ ]* 15.2 Manual accessibility testing with TalkBack
    - Verify all icons have meaningful content descriptions
    - Test screen reader navigation flow
    - Verify loading state announcements

  - [ ]* 15.3 Capture screenshot tests
    - Capture screenshots in light theme
    - Capture screenshots in dark theme
    - Compare before/after upgrade visuals

- [ ] 16. Final integration and code review
  - [ ] 16.1 Run all tests and verify passing
    - Run unit tests
    - Run property-based tests (100 iterations each)
    - Run integration tests
    - Fix any failing tests

  - [ ] 16.2 Self-review against code review checklist
    - Verify no hardcoded colors (all use MaterialTheme.colorScheme)
    - Verify no hardcoded dimensions (all use Spacing tokens)
    - Verify no hardcoded text (all use stringResource())
    - Verify all typography uses MaterialTheme.typography
    - Verify all shapes use MaterialTheme.shapes
    - Verify no business logic in composables
    - Verify StateFlow used for all state observation
    - Verify all icons have content descriptions
    - Verify minimum 48dp touch targets for interactive elements
    - _Requirements: 2.5, 2.6, 2.7, 2.8, 3.3, 3.4, 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7, 14.8_

  - [ ] 16.3 Verify Clean Architecture compliance
    - Confirm no business logic in UI layer
    - Confirm all data operations delegated to ViewModels
    - Confirm no direct repository/data source access
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.7, 14.8_

  - [ ] 16.4 Submit PR with proper template
    - Include build status
    - Describe changes and motivation
    - Reference requirements and design documents

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key milestones
- Property tests validate universal correctness properties across all valid inputs
- Unit tests validate specific examples and edge cases
- All 16 correctness properties from the design document are covered by property tests
- The implementation preserves 100% backward compatibility with existing functionality
- No changes to ViewModels, data models, or navigation structure are required
