# Android Code Review - .agent/memory/CODE_REVIEW.md

## A. Review Overview

- **Scope:** Android-only source code (:app module) - production (main), debug, and release source sets. Excludes shared KMP domain/data (except Android-specific app-layer classes), iOS (:iosApp), web (:web), and desktop (:desktop).
- **Strictness:** MAXIMUM - production-grade, security-conscious, zero-tolerance for overlooked issues.
- **Start date:** 2026-11-05
- **Last updated date:** 2026-11-05
- **Total discovered classes:** 837
- **Completed classes:** 51
- **Pending classes:** 705
- **Classes with findings:** 102
- **Classes with no findings:** 57
- **Overall completion:** 15.8%

## B. Module-by-Module Progress

All paths are repository-relative under app/src/. A completed checkbox means the class has actually undergone review (not that it is bug-free).

- [ ] app/src/debug/kotlin/com/synapse/social/studioasinc/core/di/DebugCallModule.kt — Pending
- [ ] app/src/debug/kotlin/com/synapse/social/studioasinc/data/call/MockCallRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/auth/AuthHelper.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/auth/GoogleAuthHelper.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/config/AppConstants.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/config/Constants.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/config/NotificationConfig.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/crash/GlobalCrashHandler.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/ai/AiModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/AppModule.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/AuthModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/BackendModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/BlockingModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/CallBackendModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/CallDeviceModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/CallMediaEntryPoint.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/CallUseCaseModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/ChatModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/CoreModule.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/DatabaseEntryPoint.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/DatabaseModule.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/EchoRoomModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/EchoUseCaseModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/NotificationModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/PostModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/PresenceModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/SearchModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/SettingsModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/SoundModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/StorageModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/UserModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/UtilsEntryPoint.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/di/WebRtcModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/domain/parser/MentionParser.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/media/AndroidSoundPlayer.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/media/VideoPlayerManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/media/VoicePlayerManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/monitoring/SentryInitializer.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/navigation/NotificationNavigationCoordinator.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/network/SupabaseErrorHandler.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/storage/providers/S3Signer.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/SynapseApplication.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/ui/animation/ActivityTransitions.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/ui/animation/SkeletonMorphing.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/ui/base/BaseComposeActivity.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/ui/util/AmbientColorExtractor.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/AutoDownloadManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/BiometricChecker.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/CallNotificationHelper.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/ChatLockManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/CoilImagesPlugin.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/DownloadManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/EchoNotificationHelper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/ErrorHandler.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/FileUtils.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/ImageLoader.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/ImageUtils.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/IntentUtils.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/JsonUtils.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/MediaCache.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/MediaCacheCleanupWorker.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/NetworkUtil.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/NotificationHelper.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/NumberFormatter.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/PaginationManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/RetryHandler.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/ScrollPositionState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/StorageCalculator.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/SupabaseStorageImageInterceptor.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/SupabaseTypes.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/TimeUtils.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/UiText.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/UploadProgressManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/UriUtils.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/UserProfileManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/core/util/VideoUtils.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/ActionQueue.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/AndroidCallPermissionRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/audio/AndroidCallAudioRouter.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/audio/CallAudioRouter.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/CallMediaHold.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/CallPermissionNames.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/CallRealtimeDataSource.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/CallSessionCoordinator.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/CallSessionDataSource.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/CallSimulationController.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/CallStateMachine.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/dto/CallDtos.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/mapper/CallMapper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/NoOpCallSimulationController.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/SupabaseCallSessionRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/TurnCredentialRepositoryImpl.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/webrtc/CallEngine.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/webrtc/CallMediaHost.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/webrtc/CallMediaRegistry.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/webrtc/DefaultWebRtcCallEngine.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/webrtc/VideoCapturerFactory.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/webrtc/WebRtcRuntime.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/call/WebRtcCallRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/crypto/SignalSafetyNumberGenerator.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/echo/audio/LiveKitEchoAudioEngine.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/echo/EchoMediaHold.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/AndroidDeviceInfoProvider.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/AndroidPlatformInfoProvider.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/AppSettingsManager.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/AiStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/AppearanceStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/ChatStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/GeneralStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/MediaStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/NotificationStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/PrivacyStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/SettingsConstants.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/SoundStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/settings/StoreUtils.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/SettingsDataStore.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/StorageMigration.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/SyncWorker.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/local/ReleaseCacheStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/model/GitHubReleaseDto.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/model/LinkedAccount.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/model/MessageDto.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/model/OpenSourceLicenseDto.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/model/RelationshipStatus.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/model/StoryCreateRequest.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/model/UserProfile.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/paging/CommentPagingSource.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/paging/FeedPagingSource.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/paging/PostPagingSource.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/permission/AndroidPermissionStatusProvider.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/permission/AppPermission.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/permission/PermissionOnboardingStore.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/permission/PermissionStatus.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/preferences/SettingsPreferences.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/GitHubReleaseApi.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/AuthConfig.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/AuthConfigManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/AuthDevelopmentUtils.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/AuthErrorHandler.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/AuthModels.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/ChatStorageService.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/fcm/SynapseFirebaseMessagingService.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/interfaces/IAuthenticationService.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/interfaces/IDatabaseService.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/StorageException.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/StorageRetryHelper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/SupabaseAuthenticationService.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/SupabaseDatabaseService.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/SupabaseFollowService.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/SupabaseStorageService.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ai/AiRepositoryImpl.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ai/AnthropicRepositoryImpl.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ai/MultiProviderAiRepository.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ai/OpenAiRepositoryImpl.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ai/OpenRouterRepositoryImpl.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ai/SentryAgentTracing.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/AppReleaseMapper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/AssetOpenSourceLicenseRepository.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/BookmarkRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ChatMessagingRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ChatRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ChatVerificationRepositoryImpl.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/CommentMapper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/CommentRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/DomainProfileRepositoryAdapter.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/DomainSettingsRepositoryAdapter.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/DraftRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/EditProfileRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/FeedbackRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/GitHubAppUpdateRepository.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/GitHubReleaseRepository.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/PostCrudHelper.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/PostFeedHelper.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/PostReactionHelper.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/PostRepositoryUtils.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/PostSyncHelper.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/SecureMediaUrls.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/LocationRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/OpenSourceLicenseMapper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/PollRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/PostDetailRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/PostDtos.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/PostInteractionRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/PostMapper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/PostRepositoryImpl.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileActionRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileJsonExtensions.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfilePostsRepositoryImpl.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ReactionRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ReportRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ScheduledPostDtos.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ScheduledPostRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/SettingsRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/SettingsRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/SoundSettingsRepositoryImpl.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/StoryDtos.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/StoryRepository.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/UserMapper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/UsernameRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/UserRepositoryImpl.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/source/CommentLocalDataSource.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/data/source/CommentRemoteDataSource.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/di/AboutSupportRepositoryModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/di/AuthUseCaseModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/di/DraftRepositoryModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/di/GroupCreationModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/di/SettingsUseCaseModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/AppReleaseInfo.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Attachment.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/ChatVerification.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Comment.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/CommentAction.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/CommentReaction.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/CommentsState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/CommentWithUser.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/CreatePostRequest.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/FeedbackSubmission.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/FeedItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Follow.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Gender.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Like.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/LinkPreview.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/LocationData.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/MediaItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/models/UploadProgress.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Notification.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/OpenSourceLicense.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/PollOption.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/PollOptionResult.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Post.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/PostDetail.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/PostDetailItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/PostDetailState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/ProfileFollowModels.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Reaction.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/ReactionType.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/release/AppRelease.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/release/ReleaseFeed.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/release/ReleaseNotesParser.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/release/ReleaseVersion.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Reply.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/ScheduledPost.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/SearchResult.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/Story.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/StoryReaction.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/StoryView.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/StoryWithUser.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/User.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/UserProfile.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/model/UserReaction.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/ai/AiRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/AppReleaseRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/AppUpdateRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/ChatVerificationRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/DeviceInfoProvider.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/DraftRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/FeedbackRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/MediaUploadHandler.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/OpenSourceLicenseRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/PollRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/PostInteractionRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/PostRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/ProfilePrivacyRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/ProfileRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/ReactionRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/ReelSubmissionHandler.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/ReportRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/repository/ScheduledPostRepository.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/about/CheckForUpdatesUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/about/GetOpenSourceLicensesUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/about/SubmitFeedbackUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/about/ValidateFeedbackUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/ai/GenerateSmartRepliesUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/ai/SummarizeChatUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/ai/SummarizeMessageUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/ai/SummarizePostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/ai/SummarizeThreadUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/chat/ChatVerificationUseCases.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/BookmarkPostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/ClearDraftUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/GetDraftUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/GetFeedPagedUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/GetPostByIdUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/GetPostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/PopulatePostPollsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/QuotePostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/ReactToCommentUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/ReactToPostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/ReportPostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/RepostPostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/RevokeVoteUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/SaveDraftUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/SubmitPostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/UnrepostPostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/post/VotePollUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/ArchiveProfileUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/BlockUserUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/FollowUserUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/GetFollowingUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/GetInterestsSkillsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/GetPendingFollowRequestsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/GetProfileFollowStatusUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/GetProfilePhotosUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/GetProfilePostsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/GetProfileReelsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/GetProfileRepliesUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/GetProfileUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/LockProfileUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/MuteUserUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/ReportUserUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/RespondToFollowRequestUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/UnfollowUserUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/profile/UpdateInterestsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/reaction/PopulatePostReactionsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/release/GetReleaseFeedUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/release/MarkReleaseSeenUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/search/SearchFeelingsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/search/SearchLocationsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/search/SearchUsersForPostUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/story/HasActiveStoryUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/update/GetUpdateStateUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/domain/usecase/user/GetUserByIdUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/AuthActivity.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/AuthInputHelper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/AuthOAuthHelper.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/AuthViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/AuthViewModelExtensions.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/EmailVerificationViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/ForgotPasswordViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/ResetPasswordViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/AuthScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/AuthButton.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/AuthErrorDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/AuthScreenLayout.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/AuthTextField.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/ErrorCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/LoadingOverlay.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/OAuthButton.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/OAuthSection.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/PasswordStrengthIndicator.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/ProfileCompletionDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/ProfileCompletionDialogFragment.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/components/UserCreatedDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/EmailVerificationScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/ForgotPasswordScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/models/AuthNavigationEvent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/models/AuthUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/ResetPasswordScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/SignInSignUpScreenContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/util/AnimationUtil.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/util/WindowSizeClass.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/ui/WelcomeScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/blocking/BlockingUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/blocking/BlockingViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/blocking/ui/BlockedContactsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/blocking/ui/BlockedUserItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/blocking/ui/EmptyBlockedListContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/blocking/ui/UnblockConfirmationDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/CallActivity.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/CallLaunchRequest.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/CallPermissionUi.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/CallScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/CallUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/CallViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallAnswerSlider.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallBackdrop.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallChrome.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallControls.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallDimens.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallEndedContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallIcons.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallPermissionContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallPreviews.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallSheets.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallSurfaceVideo.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallVideoContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/components/CallVoiceContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/IncomingCallViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/crash/CrashActivity.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/components/CreatePostTopBar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/components/CreationActionButtons.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/components/MediaAttachmentPreview.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/components/PollEditor.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/components/UserHeader.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/CreatePostContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/CreatePostScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/CreatePostSearchComponents.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/CreatePostSheets.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/CreatePostViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/handlers/MediaUploadHandler.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/handlers/ReelSubmissionHandler.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/handlers/SearchHandler.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/CreatePostActivity.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/quote/QuotePostScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/quote/QuotePostViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/scheduling/ScheduledPostPublisherWorker.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/EchoErrors.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/lobby/components/CreateEchoRoomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/lobby/components/EchoRoomCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/lobby/EchoLobbyContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/lobby/EchoLobbyScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/lobby/EchoLobbyUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/lobby/EchoLobbyViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/components/EchoIcons.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/components/EchoParticipantSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/components/EchoReactionOverlay.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/components/EchoRoomBanners.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/components/EchoRoomControls.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/components/EchoRoomPanels.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/components/EchoStage.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/EchoPermissionGate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/EchoRoomScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/EchoRoomUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/EchoRoomViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/hashtag/HashtagFeedScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/hashtag/HashtagFeedViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/home/home/components/QuickPostArea.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/home/home/FeedLoadingStates.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/home/home/FeedScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/home/home/FeedViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/home/home/HomeScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/home/HomeActivity.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/animations/InboxAnimations.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/CallsUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/CallsViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatAiDelegate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatDependencies.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatE2EDelegate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatEncryptionViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatInfoViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatInitializationDelegate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatInputDelegate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatItemsMapper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatMediaDelegate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatMessagingDelegate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatReactionDelegate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatSettingsDelegate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatSubscriptionDelegate.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/AttachmentMenu.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/BatchMediaGalleryDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/BatchMediaViewerDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/CallRecordItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/ChatAttachmentSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/ChatDialogs.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/ChatFolderSelectionBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/ChatInputBar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/ChatIntroHeader.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/ChatReportDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/ChatShimmer.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/ChatTopAppBar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/ContactItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/DeleteMessageDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/DeleteSelectedMessagesDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/FolderTabRow.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/InboxEmptyState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/InboxShimmer.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/MediaPlayers.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/MessageBubble.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/MessageCardComponents.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/MessageContextMenu.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/QuotedMessagePreview.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/QuotedMessagePreviewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/SharedContentComponents.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/StackedMediaCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/SystemHistoryChip.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/TypingIndicator.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/components/UndecryptableMessageNotice.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ContactsViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/CreateGroupScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/CreateGroupViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupCreationCommon.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupCreationIdentityAndPermissions.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupCreationStages.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupHubStage.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupInfoScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupInfoViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupInviteActivity.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupInviteScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupInviteViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/GroupParticipantsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/InboxScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/InboxViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/models/ChatModels.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/models/ChatUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/models/InboxUiModels.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/CallsTabScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/ChatBackground.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/ChatEncryptionSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/ChatInfoScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/ChatMessageList.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/ChatPrivacyScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/ChatScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/ChatSearchResults.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/ChatsTabScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/ContactsTabScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/DisappearingMessagesScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/EchoTabScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/InboxExperienceScreens.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/screens/SharedContentScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/SharedContentViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/theme/InboxExpressiveDimens.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/theme/InboxTheme.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/utils/ShortcutManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/components/LiveWaveformView.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/components/OdometerTimerText.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/TimeProvider.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceAction.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceDownloadCache.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceEncoder.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceGestureConfig.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceInputHost.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceMediaCrypto.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceMessagePlayerViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceRecorder.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceRecordingController.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceRecordingGlow.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceUploadService.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/voice/VoiceWaveformExtractor.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/components/PermissionCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/components/PermissionRationaleDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/components/PermissionRequestEffect.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/components/PermissionStatusChip.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/PermissionManagerScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/PermissionManagerViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/PermissionOnboardingScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/PermissionOnboardingViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/PermissionPresentation.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/PermissionSummaryScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/permission/PermissionUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/components/CommentInput.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/components/CommentOptionsBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/components/CommentShimmer.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/components/CommentsList.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/components/ReplyIndicator.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/components/ReplyingToBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/PostDetailScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/PostDetailUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/PostDetailViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/PostDetailActivity.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/appearance/ProfileAppearanceEvent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/appearance/ProfileAppearanceNavigation.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/appearance/ProfileAppearanceScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/appearance/ProfileAppearanceUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/appearance/ProfileAppearanceViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/components/BasicInformationSection.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/components/EditProfileCategorySections.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/components/EditProfileContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/components/EditProfileTopBar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/components/GenderSelector.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/components/ProfileCompletionCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/components/ProfileFormFields.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/components/ProfileIdentityPreview.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/components/ProfileImageSection.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/EditProfileScreen.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/EditProfileUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/EditProfileViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/featured/FeaturedContentEvent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/featured/FeaturedContentNavigation.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/featured/FeaturedContentScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/featured/FeaturedContentUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/featured/FeaturedContentViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/interestsskills/InterestsSkillsEvent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/interestsskills/InterestsSkillsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/interestsskills/InterestsSkillsUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/interestsskills/InterestsSkillsViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/location/LocationEvent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/location/LocationNavigation.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/location/LocationScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/location/LocationUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/location/LocationViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/media/ProfileMediaScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/media/ProfileMediaUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/media/ProfileMediaViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/photohistory/HistoryItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/photohistory/PhotoHistoryScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/photohistory/PhotoHistoryViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/professional/ProfessionalProfileScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/professional/ProfessionalProfileUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/professional/ProfessionalProfileViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/RegionSelectionScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/social/SocialLinksScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/social/SocialLinksUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/social/SocialLinksViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/workeducation/WorkEducationScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/workeducation/WorkEducationUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/workeducation/WorkEducationViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/lockprofile/di/ProfilePrivacyModule.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/lockprofile/FollowRequestsSection.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/lockprofile/LockProfileScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/lockprofile/LockProfileViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/personalinfo/PersonalInformationContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/personalinfo/PersonalInformationDialogs.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/personalinfo/PersonalInformationScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/personalinfo/PersonalInformationUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/personalinfo/PersonalInformationViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/animations/ProfileAnimations.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ContentFilterBar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/CoverPhoto.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/DetailItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/FollowingSection.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/FollowingUserItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/PhotoGrid.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ProfileHeader.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ProfileMoreMenuBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ProfileSkeleton.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ProfileTopAppBar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/QRCodeDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ReelsGrid.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ReportUserDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/SevenSidedCookieShape.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ShareProfileBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/StatItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/UserDetailsSection.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/UserSearchDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ViewAsBanner.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/components/ViewAsBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/ProfileAccessPolicy.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/ProfileContentFilter.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/ProfileContentSection.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/ProfileDialogs.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/ProfileScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/ProfileUiState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/profile/ProfileViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/ProfileEditActivity.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/search/search/components/AccountCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/search/search/components/HashtagCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/search/search/components/NewsCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/search/search/components/SearchUtils.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/search/search/SearchScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/search/search/SearchViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/search/SearchActivity.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/backup/SettingsBackupCodec.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/backup/SettingsBackupManager.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/backup/SettingsCloudSyncScheduler.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/backup/SettingsCloudSyncWorker.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/legacy/ApiKeySettingsService.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/search/SettingsSearchScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/SettingsActivity.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/ActiveStatusIndicator.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/ActivityLogActivity.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/AnimatedCounter.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/Avatar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/Badges.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/CommentSection.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/compose/components/FollowButtonCompose.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/compose/components/UserListItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/compose/FollowListScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/EmptyState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/ErrorState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/ExpressiveLoadingIndicator.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/FollowListActivity.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/LinkPreviewCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/LinkPreviewViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/MediaViewer.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/picker/PickedFile.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/MediaContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/PollContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/PostActionsFactory.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/PostCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/PostCommon.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/PostContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/PostHeader.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/PostInteractionBar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/PostOptionsBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/PostSummarySheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/QuotedPostCard.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/ReactionPicker.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/post/SharedPostItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/PostActionBar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/PostContent.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/PostHeader.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/PostOptionsMenu.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/PressScale.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/RegionItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/ReportPostDialog.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/SectionHeader.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/SharePostBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/ShimmerEffect.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/States.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/UserAvatar.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/UserAvatarWithStatus.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/components/UserListItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/main/MainActivity.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/main/MainViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/navigation/AppDestination.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/navigation/AppNavigation.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/navigation/DeepLink.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/navigation/HomeNavGraph.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/reels/components/HeartAnimation.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/reels/components/MoreActionsBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/reels/components/ShareBottomSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/reels/components/UploadProgressOverlay.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/reels/ReelItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/reels/ReelsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/reels/ReelsViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/reels/ReelUploadManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/reels/VideoPlayerViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/sharepost/SharedPostMapper.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/sharepost/SharePostSheetViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/sharepost/ShareToChatSheet.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/AppTokens.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/AuthTheme.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/ChatEncryptionTheme.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/ChatInfoMetrics.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/Color.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/GroupedList.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/ProfileTheme.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/Spacing.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/styling/MarkdownRenderer.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/SynapseTheme.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/ThemeManager.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/Typography.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/theme/UiAtmosphereState.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/util/ChatPresenceTracker.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/utils/PostShareUtils.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/utils/UrlUtils.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/viewmodel/BaseViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/viewmodel/FollowButtonViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/shared/viewmodel/UserPresenceViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/creator/StoryCreatorScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/creator/StoryCreatorViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/creator/StoryEditor.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/creator/StoryEditorComponents.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/management/StorySheets.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/tray/StoryTray.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/tray/StoryTrayViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/viewer/StoryPagerScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/viewer/StoryViewerActivity.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/viewer/StoryViewerScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/viewer/StoryViewerViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/service/call/CallActionReceiver.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/service/call/CallForegroundService.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/service/echo/EchoRoomActionReceiver.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/service/echo/EchoRoomForegroundService.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/shared/domain/usecase/chat/GetChatInfoUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/shared/domain/usecase/chat/GetChatSettingsUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/shared/domain/usecase/chat/GetCurrentUserIdUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/shared/domain/usecase/chat/GetGroupMembersUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/shared/domain/usecase/chat/GetOrCreateChatUseCase.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/animations/MagneticPeeking.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/createpost/CreatePostSearchViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/home/HomeViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/notifications/NotificationItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/notifications/NotificationsScreen.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/notifications/NotificationsViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AboutAppScreens.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AboutAppViewModels.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AboutSupportScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AboutSupportViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AccessibilityScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AccessibilityViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AccountDialogs.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AccountSettingsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AccountSettingsViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AddApiKeyDialog.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ApiKeyItem.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ApiKeySettingsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ApiKeySettingsViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AppearanceScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AppearanceViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AppLockScreen.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AvatarScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AvatarViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/BusinessPlatformScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/BusinessPlatformViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ChangeNumberScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ChangeNumberViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ChatFoldersScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ChatFoldersViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ChatSettingsComponents.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ChatSettingsPickers.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ChatSettingsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ChatSettingsViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/FavouritesScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/FeedbackViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/FlagsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/FlagsViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/FontScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/FontViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/LanguageRegionScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/LanguageRegionViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ManageStorageScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ManageStorageViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/NetworkUsageScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/NetworkUsageViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/NotificationSettingsOptions.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/NotificationSettingsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/NotificationSettingsViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PasskeysScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PasskeysViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PrivacyCheckupCategoryScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PrivacyCheckupScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PrivacyExceptionSelectorScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PrivacyExceptionSelectorViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PrivacySecurityScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PrivacySecuritySections.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PrivacySecurityViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PrivacySelectionScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/RegionData.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/RequestAccountInfoScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/RequestAccountInfoViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ScheduledPostsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ScheduledPostsViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SelectRegionScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsAnimations.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsBackupScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsBackupViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsDataProvider.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsDestination.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsHubScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsHubViewModel.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsItemComponents.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsItemPosition.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsLayoutComponents.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsModels.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsNavHost.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsProfileComponents.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SignOutViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageDataDialogs.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageDataScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageDataViewModel.kt — Reviewed; findings documented
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageDataViewModelFactory.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageProviderConfigForms.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageProviderScreen.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageUsageSection.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageUtils.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SynapsePlusScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/WhatsNewScreen.kt — Pending
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/WhatsNewViewModel.kt — Reviewed; no findings
- [ ] app/src/main/kotlin/com/synapse/social/studioasinc/viewmodel/FollowListViewModel.kt — Pending
- [ ] app/src/release/kotlin/com/synapse/social/studioasinc/core/di/ReleaseCallModule.kt — Pending

## C. Findings Summary

| # | Class | File | Severity | Finding | Location | Impact | Remediation | Status |
|---|-------|------|----------|---------|----------|--------|-------------|--------|
| 1 | Manifest (BackgroundDeletionService) | app/src/main/AndroidManifest.xml:304 | Critical | Service .data.repository.deletion.BackgroundDeletionService is declared in AndroidManifest.xml with foregroundServiceType=dataSync but no such class exists anywhere in the codebase. | AndroidManifest.xml:304 | Crash (ClassNotFoundException) if the service is ever started; broken manifest config. | Remove the manifest declaration or implement the missing service. | Open |
| 2 | AuthHelper | app/src/main/kotlin/com/synapse/social/studioasinc/core/auth/AuthHelper.kt:8 | High | Directly instantiates SupabaseAuthRepository() (a concrete Supabase-backed backend repository) and wraps it in UseCases, bypassing Hilt DI. Violates backend-independence/migration-isolation: app layer reaches directly into a provider-specific data impl. | AuthHelper.kt:7-15 | Hard-couples the app to Supabase; prevents backend migration; breaks DI lifecycle/testing. | Inject AuthRepository interface via Hilt; construct use cases in a DI module. | Open |
| 3 | SupabaseAuthenticationService | app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/SupabaseAuthenticationService.kt | High | Uses static SupabaseClient.client and a companion INSTANCE singleton instead of Hilt injection; bypasses DI entirely. | SupabaseAuthenticationService.kt:23,50-56 | Hard-couples to Supabase SDK; untestable; violates strict DI rule. | Inject Supabase client via Hilt module; remove companion singleton. | Open |
| 4 | SupabaseAuthenticationService | app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/SupabaseAuthenticationService.kt:197-199,217,284-286,303,304 | High | Logs authenticated userId and email to Logcat via android.util.Log.d/e (OneSignal login success/failure). Logcat is readable on many devices and persists. | Multiple lines in signUp/signIn | Sensitive user identifiers exposed in system logs. | Remove userId/email from Log.d/e; use Napier debug-only or redact identifiers. | Open |
| 5 | SupabaseAuthenticationService | app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/SupabaseAuthenticationService.kt:100-108,228-233,307 | Medium | enableDevelopmentMode()/createForDevelopment() bypass email verification (shouldBypassEmailVerification()); capability reachable and could leak into production builds. | enableDevelopmentMode, shouldBypassEmailVerification call sites | Unverified accounts could authenticate in production if dev mode is accidentally enabled. | Gate dev-mode behind build-type flag; remove or disable in release. | Open |
| 6 | AuthOAuthHelper | app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/AuthOAuthHelper.kt:11 | High | Logs the full OAuth deep-link URI ($uri) via Napier.d, which may contain authorization codes, access tokens, and refresh tokens. | parseDeepLink:11 | OAuth tokens/codes exposed in Logcat -> account takeover risk. | Remove URI from debug log; log only a redacted marker. | Open |
| 7 | AuthViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/AuthViewModel.kt:286-288 | Medium | onOAuthClick non-Google branch has an empty onFailure = { e -> } that silently swallows OAuth URL errors. | onOAuthClick:286-288 | OAuth failures for non-Google providers surface as silent no-ops; poor UX, hidden errors. | Surface error to user via UiEvent. | Open |
| 8 | EmailVerificationViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/EmailVerificationViewModel.kt:91-107 | Medium | Polling job launched in checkEmailVerification() (3s interval) is never tracked or cancelled in onCleared(); continues after ViewModel cleared. | checkEmailVerification, onCleared | Background polling drains battery and may post navigation events after detachment. | Track the job and cancel it in onCleared(). | Open |
| 9 | SynapseFirebaseMessagingService | app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/fcm/SynapseFirebaseMessagingService.kt:51,55 | Medium | Logs full FCM message data payload and notification body to Logcat via Log.d (unconditional, not debug-gated). | onMessageReceived:51,55 | FCM payloads (call hints, potentially PII) exposed in system logs. | Remove or redact payload logging; gate behind debug flag. | Open |
| 10 | SignalSafetyNumberGenerator | app/src/main/kotlin/com/synapse/social/studioasinc/data/crypto/SignalSafetyNumberGenerator.kt:22,27 | Low | Uses plain SHA-512 with fixed 5200 iterations and no salt as a key-derived fingerprint; not a proper KDF (no salt, fast hash). | fingerprint:22-34 | Weaker than a salted KDF; acceptable for a fingerprint but not for key derivation. | Document as fingerprint-only; use a proper KDF if output is ever used as key material. | Accepted |
| 11 | AuthViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/presentation/viewmodel/AuthViewModel.kt | Low | Plaintext passwords are held in AuthUiState (ViewModel memory) across screen transitions. | AuthUiState sign-in/sign-up fields | Passwords linger in ViewModel memory; low risk (in-process only) but avoidable. | Clear sensitive fields after use; do not persist passwords in UI state. | Accepted |

| 12 | SynapseApplication | app/src/main/kotlin/com/synapse/social/studioasinc/core/SynapseApplication.kt | High | Authenticated `userId` logged to Logcat via `android.util.Log.d/e` and `Napier.d` (OneSignal login, auth state, realtime connect) in `setupAuthSessionObserver` and `initializeOneSignal`. Logcat persists and is readable. | `setupAuthSessionObserver:323,336,344`; `initializeOneSignal:285,265,292` | Sensitive user identifiers exposed in system logs. | Remove userId from Log.d/e and Napier; redact identifiers. | Fixed |
| 13 | SynapseApplication | app/src/main/kotlin/com/synapse/social/studioasinc/core/SynapseApplication.kt | High | Direct static `SupabaseClient.client` access (`newImageLoader:72`, `setupAuthSessionObserver:316`) plus `SupabaseAuthenticationService` companion singleton (`initialize:145`, `getInstance`) bypass Hilt DI; hard-couples to Supabase. | Multiple lines | Hard-couples app to Supabase; untestable; violates strict DI rule. | Inject Supabase client and auth service via Hilt; remove static/singleton access. | Fixed |
| 14 | ApiKeySettingsService | app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/legacy/ApiKeySettingsService.kt:138 | Low | `getRawApiKey()` performs a synchronous SharedPreferences read on the calling thread; `apply()` disk writes are async so in-memory/disk state can diverge after crash. | `getRawApiKey:138`; `storeApiKey:110`; `deleteApiKey:128` | Minor main-thread blocking risk; eventual-consistency window for key storage. | Make key retrieval suspend or use `commit()` for critical writes. | Open |
| 15 | SentryAgentTracing | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ai/SentryAgentTracing.kt:62 | Low | Sanitized exceptions preserve the full `error.stackTrace` (file paths, line numbers) when captured to Sentry. | `sanitizedException:62` | Minor source-code structure disclosure via Sentry stack traces. | Strip stack frames or limit to exception type/message. | Open |
| 16 | AuthActivity | app/src/main/kotlin/com/synapse/social/studioasinc/feature/auth/AuthActivity.kt:70 | Medium | Logs the first 10 characters of the Google Web Client ID via `Napier.d` during sign-in initiation. | `handleGoogleSignIn:70` | Client ID partially exposed in logs (low secrecy value but identifying). | Redact or remove Client ID from debug log. | Open |
| 17 | CoreModule | app/src/main/kotlin/com/synapse/social/studioasinc/core/di/CoreModule.kt:102 | Low | `provideApplicationScope` uses `Dispatchers.Default` while `SynapseApplication` uses its own `Dispatchers.Main` scope — two inconsistent "application" scopes. | `provideApplicationScope:102` | Inconsistent coroutine scoping; potential context confusion. | Unify application scopes under one provider. | Open |
| 18 | GlobalCrashHandler | app/src/main/kotlin/com/synapse/social/studioasinc/core/crash/GlobalCrashHandler.kt:13 | Low | Full throwable stack trace (internal code paths) passed via Intent extra to CrashActivity and displayed. | `uncaughtException:13` | Minor internal information exposure via crash screen. | Strip sensitive frames or avoid displaying raw stack to user. | Open |
| 19 | AuthConfig | app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/AuthConfig.kt:168 | Medium | `Log.d(TAG, "AuthConfig created: $config")` logs the full AuthConfig (incl. developmentMode) and `isDebugLoggingEnabled()` is always true in debug builds (`BuildConfig.DEBUG`), so all sensitive auth logs are always written to Logcat in debug. | `create:167-171`, `isDebugLoggingEnabled:235` | Dev-mode/security state exposed in Logcat; debug builds always log sensitive auth data. | Gate config logging behind a non-DEBUG build check; review debug logging scope. | Open |
| 20 | AuthErrorHandler | app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/AuthErrorHandler.kt:124 | High | Email addresses logged to Logcat in plaintext via `logVerificationAttempt(email, ...)` (line 124) and `logResendVerificationAttempt(email, ...)` (line 132); error messages logged at line 15. | `logVerificationAttempt:124`, `logResendVerificationAttempt:132`, `handleAuthError:15` | User email addresses (PII) exposed in system logs. | Remove email from Log.i/w/d; log only a hashed or redacted marker. | Fixed |
| 21 | DatabaseModule | app/src/main/kotlin/com/synapse/social/studioasinc/core/di/DatabaseModule.kt:82 | Medium | `provideStorageDatabase` lacks `@Singleton`; each injection creates a new `StorageDatabase`/`AndroidSqliteDriver` for the same DB file, risking SQLite lock contention and inconsistent state. | `provideStorageDatabase:82-120` | Multiple DB driver instances; potential concurrency/locking bugs. | Add `@Singleton` to `provideStorageDatabase`. | Open |
| 22 | SettingsDataStore | app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/SettingsDataStore.kt:180-184 | Medium | Data layer (`data/local/database`) imports UI classes `com.synapse.social.studioasinc.ui.settings.MediaUploadQuality` and `MediaType` in `restoreDefaults()`, violating layering (data depends on UI). | `restoreDefaults:180-184` | Improper layer dependency; data module pulls in UI module. | Move defaults to a domain/data constant or invert the dependency. | Open |
| 23 | SyncWorker | app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/SyncWorker.kt:18 | High | `SyncWorker` is a `CoroutineWorker` with a 2-arg constructor but is NOT annotated with `@HiltWorker`/`@AssistedInject`; WorkManager cannot instantiate it via reflection (no no-arg constructor) unless a custom WorkerFactory is registered — likely a runtime crash when the worker runs. | `SyncWorker:18-106` | Scheduled sync/worker may crash at runtime; offline actions never processed. | Annotate `@HiltWorker` and use `@AssistedInject`, or register a Hilt WorkerFactory. | Open |
| 24 | SyncWorker | app/src/main/kotlin/com/synapse/social/studioasinc/data/local/database/SyncWorker.kt:106 | Medium | `runAttemptCount` is referenced but is not a property of `CoroutineWorker`/`Worker`; may be a compile error or rely on an undefined extension. | `doWork:106` | Possible compilation failure. | Verify `runAttemptCount` source or use `getRunAttemptCount()`-equivalent. | Open |
| 25 | MediaCacheCleanupWorker | app/src/main/kotlin/com/synapse/social/studioasinc/core/util/MediaCacheCleanupWorker.kt:106 | Low | References `runAttemptCount` (same concern as SyncWorker); uses `SettingsRepositoryImpl.getInstance(...)` direct singleton instead of DI. | `doWork:106` | Possible compile/runtime issue; DI inconsistency. | Verify `runAttemptCount`; inject repository via Hilt WorkerFactory. | Open |
| 26 | RetryHandler | app/src/main/kotlin/com/synapse/social/studioasinc/core/util/RetryHandler.kt:57 | Low | Logs exception messages (`Error: ${e.message}`) which may contain sensitive data. | `executeWithRetry:57` | Minor info in logs. | Redact exception messages in logs. | Open |
| 27 | ErrorHandler | app/src/main/kotlin/com/synapse/social/studioasinc/core/util/ErrorHandler.kt:159 | Medium | `logError` writes `userId` and `messageId` plus the full exception stack trace to Logcat via `Log.e`. | `logError:159` | User ID exposed in system logs. | Remove userId/messageId from log message; redact stack trace. | Open |
| 28 | NetworkUtil | app/src/main/kotlin/com/synapse/social/studioasinc/core/util/NetworkUtil.kt:30 | Low | `registerNetworkCallback` registers a `ConnectivityManager.NetworkCallback` with no lifecycle binding; the caller must remember to unregister or the callback leaks. | `registerNetworkCallback:30-56` | Potential callback leak if caller forgets unregister. | Return a `Closeable`/AutoCloseable wrapper or integrate with Lifecycle. | Open |
| 29 | ImageUtils | app/src/main/kotlin/com/synapse/social/studioasinc/core/util/ImageUtils.kt:70 | Low | `getScaledBitmap` decodes the full image via `BitmapFactory.decodeFile(path)` before scaling, risking OutOfMemoryError on large images (no inSampleSize downsampling). | `getScaledBitmap:70-78` | OOM crashes when processing large photos/videos. | Use `decodeSampleBitmapFromPath`/inSampleSize before scaling. | Open |
| 30 | TimeUtils | app/src/main/kotlin/com/synapse/social/studioasinc/core/util/TimeUtils.kt:25 | Low | `getTimeAgo` uses fragile multi-strategy timestamp parsing with manual string manipulation; edge-case formats may misparse. | `getTimeAgo:25-75` | Incorrect relative timestamps for some ISO formats. | Simplify parsing or use a dedicated library. | Open |
| 31 | UserRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/UserRepositoryImpl.kt:25 | Medium | Constructor default `client = SharedSupabaseClient.client` references the static Supabase client (DI bypass); logs `$userId` and `$sanitizedQuery` (search queries) to Logcat. | `:25`, `:63`, `:155` | DI bypass; user IDs and search queries exposed in logs. | Inject client via Hilt (remove default); redact logged identifiers/queries. | Open |
| 32 | UserRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/UserRepositoryImpl.kt:101 | Medium | `updateUser` writes sensitive fields (`banned`, `verify`, `account_type`, `email`) directly to Supabase with no client-side field protection; relies entirely on server RLS. | `updateUser:101-121` | If server RLS is misconfigured, a client can escalate privileges / unban itself. | Strip sensitive fields from client updates; enforce server-side RLS. | Open |
| 33 | ProfileRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileRepositoryImpl.kt:30 | Medium | Class not annotated with `@Singleton`/`@InstallIn`; each Hilt injection creates a new repository instance (and its inner `ProfilePostsRepositoryImpl`). | `ProfileRepositoryImpl:30` | Inconsistent repository state; wasted object creation. | Add `@Singleton` and Hilt constructor injection. | Open |
| 34 | ProfileRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileRepositoryImpl.kt:79 | High | `resolveUserId` calls `runBlocking { ... }` to perform a Supabase query, blocking the caller thread. If invoked from a bounded dispatcher or Main, this causes thread starvation / potential deadlock. | `resolveUserId:75-88` | Thread blocking; UI jank or coroutine deadlock risk. | Convert `resolveUserId` to a suspend function; remove `runBlocking`. | Fixed |
| 35 | ProfileRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileRepositoryImpl.kt:311 | Medium | `getFollowers` decodes `decodeList<Map<String, UserProfile>>()` then reads `it["following_id"]` — the `follows` table column `following_id` is a string, not a nested UserProfile; the decoding type is almost certainly wrong. | `getFollowers:305-315` | Followers list likely returns empty/garbage data. | Decode `List<FollowingIdResponse>` (string IDs) then fetch profiles. | Open |
| 36 | UserRepositoryImpl / ProfileRepositoryImpl | various | Low | User IDs, search queries, and profile data logged via `android.util.Log.d/e` throughout repository methods. | Multiple Log lines | PII (userIds, search terms) in Logcat. | Redact identifiers from repository logs. | Open |
| 37 | ChatRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ChatRepositoryImpl.kt:14 | Medium | Class not annotated `@Singleton`; each injection creates a new instance. | `ChatRepositoryImpl:14` | Inconsistent repository scoping. | Add `@Singleton` / Hilt scoping. | Open |
| 38 | ChatRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ChatRepositoryImpl.kt:47 | High | `deviceId = 1` hardcoded in `sendMessage` for E2E Signal Protocol ("Assuming default device ID 1"). All messages encrypted as device 1, breaking per-installation device identity and multi-device E2E. | `sendMessage:47` | E2E encryption device identity broken; messages not bound to actual device. | Use a stable per-installation device ID from secure storage. | Fixed |
| 39 | CommentRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/CommentRepositoryImpl.kt:176 | Medium | `getCommentsForPost` calls `userRepository.getUserById(entity.authorUid)` once per comment — N+1 queries when loading a comment thread with users. | `getCommentsForPost:170-218` | Poor performance on threads with many comments; many round trips. | Batch user profile fetches by ID. | Fixed |
| 40 | SettingsRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/SettingsRepositoryImpl.kt:29 | Medium | Private constructor + manual singleton (`getInstance`) instead of Hilt `@Singleton`; Hilt cannot inject it directly. Also uses static `SupabaseClient.client` in `checkForUpdates` (line 487) and imports UI types (`StorageUsageBreakdown`, etc.) — data layer depends on UI layer. | `:29`, `:487`, imports | DI inconsistency; DI bypass; layering violation. | Make it a Hilt `@Singleton`; inject Supabase client; remove UI imports. | Open |
| 41 | ReportRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ReportRepositoryImpl.kt:13 | Low | No `@Singleton` scope; confusing `runCatching { return ... }` usage in `createReport`. | `:13`, `:32-45` | Inconsistent scoping; unclear control flow. | Add `@Singleton`; simplify `runCatching`. | Open |
| 42 | ReactionRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ReactionRepositoryImpl.kt:28 | Low | No `@Singleton` scope; logs `$userId` in `toggleReaction` (line 52). | `:28`, `:52` | Inconsistent scoping; user ID in logs. | Add `@Singleton`; redact userId from logs. | Open |
| 43 | PostCrudHelper | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/PostCrudHelper.kt:307 | High | `updatePost(postId, updates: Map<String, Any?>)` applies an arbitrary field map to the `posts` table with no field validation/allowlist — mass-assignment risk (could write sensitive columns). | `updatePost:307-319` | Unsafe mass assignment to posts table. | Restrict to an explicit allowlist of updatable fields. | Fixed |
| 44 | PostCrudHelper | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/PostCrudHelper.kt:337 | Medium | `deletePost` and `toggleComments` perform no client-side authorization check (rely entirely on server RLS). | `deletePost:337`, `toggleComments:381` | If RLS is misconfigured, any user can delete/toggle any post. | Add caller ownership checks or rely on documented server RLS. | Open |
| 45 | PostFeedHelper | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/PostFeedHelper.kt:88 | Medium | `refreshPosts` issues a heavy joined posts query plus `populatePostReactions` (RPC + per-chunk user-reactions query) and `populatePostPolls` (2 queries) — many round trips per feed refresh. `profileCache` in PostRepositoryUtils is unbounded. | `refreshPosts:88-136` | Slow feed loads; unbounded cache memory growth. | Reduce round trips; bound/evict profileCache. | Open |
| 46 | CommentRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/CommentRepositoryImpl.kt:267 | Low | `externalScope.launch` fire-and-forget tasks (updateRepliesCount, processMentions, updatePostCommentsCount) are not tracked; failures are silent. | `addComment:267-277` | Silent background task failures. | Track jobs or propagate errors. | Open |
| 47 | PostRepositoryUtils | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/helpers/PostRepositoryUtils.kt:21 | Low | `profileCache` (`ConcurrentHashMap`) is unbounded — entries accumulate for every distinct userId and are never evicted (only ignored after expiry). | `profileCache:21` | Memory growth proportional to users viewed. | Add bounded cache (e.g., LruCache) with eviction. | Fixed |
| 48 | StoryRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/StoryRepository.kt:93 | Medium | `private val client = SupabaseClient.client` uses the static Supabase client (DI bypass) instead of constructor injection. | `StoryRepositoryImpl:93` | Hard-coupled to Supabase; untestable. | Inject Supabase client via Hilt. | Open |
| 49 | StoryRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/StoryRepository.kt:334 | Medium | `deleteStory`, `markAsSeen`, `reactToStory`, `removeReaction` perform no client-side authorization (rely entirely on server RLS). | `deleteStory:334`, `markAsSeen:349`, `reactToStory:405`, `removeReaction:423` | If RLS misconfigured, any user can delete/react to any story. | Add ownership checks or document server RLS. | Open |
| 50 | ChatMessagingRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ChatMessagingRepositoryImpl.kt:110 | High | `getConversations` launches one unbounded `async` per chat ID to fetch last message — with many chats this fires dozens of concurrent Supabase queries. | `getConversations:110-129` | Server overload / rate-limit risk; poor performance on large inboxes. | Bound concurrency with a Semaphore or batch the last-message query. | Open |
| 51 | ChatMessagingRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ChatMessagingRepositoryImpl.kt:160 | Medium | `getMessages`, `sendMessage`, `getOrCreateChat` perform no client-side authorization (rely entirely on server RLS). | `getMessages:160`, `sendMessage:182`, `getOrCreateChat:211` | If RLS misconfigured, unauthorized message access/send. | Add participation checks or document server RLS. | Open |
| 52 | DraftRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/DraftRepositoryImpl.kt:8 | Low | Draft text stored in plaintext SharedPreferences ("create_post_draft"); no `@Singleton` scope. | `DraftRepositoryImpl:8-29` | Post draft content in plaintext prefs. | Encrypt draft text; add `@Singleton`. | Open |
| 53 | BookmarkRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/BookmarkRepositoryImpl.kt:15 | Medium | Constructor default `client = SupabaseClient.client` (DI bypass); `delete { existing.id?.let {...} }` may be a type mismatch (passing `Unit?` where a lambda is expected) — verify compilation. | `:15`, `:62` | DI bypass; possible compile error. | Inject client via Hilt; verify delete lambda compiles. | Open |
| 54 | PollRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/PollRepositoryImpl.kt:17 | Low | No `@Singleton` scope; poll end-time enforcement is client-side only (server should be source of truth). | `submitVote:43-90` | Inconsistent scoping; client-side poll deadline bypassable. | Add `@Singleton`; enforce poll deadline server-side. | Open |
| 55 | LocationRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/LocationRepositoryImpl.kt:28 | Medium | Location search queries are sent to a third-party service (Nominatim / OpenStreetMap) via direct HttpURLConnection — the user's search intent and approximate location interest are exposed to an external party. | `searchLocations:28` | User location-search data exposed to third party. | Route through app backend or disclose in privacy policy; consider rate-limiting. | Open |
| 56 | FeedbackRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/FeedbackRepositoryImpl.kt:15 | Low | No `@Singleton`; `private val client = SupabaseClient.client` static DI bypass. | `:15` | DI inconsistency/bypass. | Add `@Singleton`; inject client. | Open |
| 57 | EditProfileRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/EditProfileRepositoryImpl.kt:33 | Medium | `private val client by lazy { SupabaseClient.client }` static DI bypass. | `:33` | Hard-coupled to Supabase. | Inject client via Hilt. | Open |
| 58 | EditProfileRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/EditProfileRepositoryImpl.kt:127 | High | `updateProfile(userId, updateData: Map<String, Any?>)` applies an arbitrary field map to the `users` table with no field allowlist — mass-assignment risk (could write email, role, banned, etc.). | `updateProfile:127-156` | Unsafe mass assignment to users table; privilege/data tampering if RLS permissive. | Restrict to explicit allowlist of updatable profile fields. | Fixed |
| 59 | UsernameRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/UsernameRepositoryImpl.kt:8 | Informational | Repository is effectively a stub — declares `client` and `usernameCache` but exposes no methods (only `clearCache`). Static `SupabaseClient.client` DI bypass. | `UsernameRepositoryImpl:8-14` | Dead/incomplete code. | Implement methods or remove. | Open |
| 60 | ProfileActionRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileActionRepositoryImpl.kt:20 | Medium | `private val supabase = SupabaseClient.client` static DI bypass. | `:20` | Hard-coupled to Supabase. | Inject client via Hilt. | Open |
| 61 | ProfileActionRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileActionRepositoryImpl.kt:95 | High | `archiveProfile(userId, isArchived)` updates `users.is_archived` for any `userId` with no authorization check that the caller owns the profile. | `archiveProfile:95-105` | Any caller can archive/disable any user's profile (if RLS permissive). | Add caller-ownership check. | Open |
| 62 | ProfileActionRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ProfileActionRepositoryImpl.kt:107 | Medium | `blockUser`, `reportUser`, `muteUser` accept a `userId`/`reporter_id` parameter without verifying the caller matches it — a caller could act as another user (relies on RLS). | `blockUser:107`, `reportUser:120`, `muteUser:134` | Cross-user action spoofing if RLS is permissive. | Verify caller == userId before mutating. | Open |
| 63 | PostDetailRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/PostDetailRepositoryImpl.kt:23 | Low | No `@Singleton` scope; `incrementViewCount` swallows failures and returns success. | `:23`, `:141-152` | Inconsistent scoping; view-count errors hidden. | Add `@Singleton`; propagate or log view-count failures. | Open |
| 64 | PostInteractionRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/PostInteractionRepositoryImpl.kt:22 | Medium | `private val client = SupabaseClient.client` static DI bypass; `likePost`/`savePost`/`reportPost` accept a `userId` without verifying the caller matches it (relies on RLS). | `:22`, `:24-128` | DI bypass; cross-user like/save/report spoofing if RLS permissive. | Inject client; verify caller == userId. | Open |
| 65 | ChatVerificationRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ChatVerificationRepositoryImpl.kt:22 | None | E2E safety-number verification uses constant-time comparison (`MessageDigest.isEqual`), synchronous commit for storage, and proper identity-key validation. No findings. | — | — | — | — |
| 66 | ScheduledPostRepositoryImpl | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ScheduledPostRepositoryImpl.kt:51 | Medium | `updateScheduledPost`, `markPublished`, `markFailed`, `cancelScheduledPost`, `retryScheduledPost` filter by `id` only with no owner check — rely entirely on server RLS. | `updateScheduledPost:73`, `markPublished:98`, `cancelScheduledPost:130` | If RLS misconfigured, any user can modify/cancel any scheduled post. | Add owner_uid filter or document server RLS. | Open |
| 67 | DomainProfileRepositoryAdapter | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/DomainProfileRepositoryAdapter.kt:98 | Low | `education = EducationSanitizer.sanitizeEducationList(education).firstOrNull()` truncates multiple education entries to a single item — data loss. | `toData:98-124` | Education history beyond first item lost when persisting. | Preserve all education items. | Open |
| 68 | DomainSettingsRepositoryAdapter | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/DomainSettingsRepositoryAdapter.kt:49 | Low | No `@Singleton`; enum mapping via `valueOf(name)` throws if UI and domain enum names diverge. | `DomainSettingsRepositoryAdapter:49` | Inconsistent scoping; potential IllegalArgumentException on enum mismatch. | Add `@Singleton`; use safe mapping. | Open |
| 69 | GitHubAppUpdateRepository | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/GitHubAppUpdateRepository.kt:15 | None | No `@Singleton`, but otherwise sound: validates update URL is github.com https with correct host/path before use. | — | — | — | — |
| 70 | GitHubReleaseRepository | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/GitHubReleaseRepository.kt:26 | None | Sound read-through cache with TTL and cache-fallback on failure. No findings. | — | — | — | — |
| 71 | AssetOpenSourceLicenseRepository | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/AssetOpenSourceLicenseRepository.kt:21 | None | Validates repositoryUrl scheme (http/https) and rejects userinfo. No findings. | — | — | — | — |
| 72 | CommentRemoteDataSource | app/src/main/kotlin/com/synapse/social/studioasinc/data/source/CommentRemoteDataSource.kt:236 | Medium | `updateRepliesCount`/`updatePostCommentsCount` do a read-modify-write on `comments_count` — concurrent increments lose updates (no atomic increment). | `updateRepliesCount:236`, `updatePostCommentsCount:251` | Lost comment-count updates under concurrency. | Use atomic server-side increment. | Open |
| 73 | SupabaseStorageService | app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/SupabaseStorageService.kt:23 | Medium | `private val client = SupabaseClient.client` static DI bypass. | `:23` | Hard-coupled to Supabase. | Inject client via Hilt. | Open |
| 74 | CommentRemoteDataSource | app/src/main/kotlin/com/synapse/social/studioasinc/data/source/CommentRemoteDataSource.kt:25 | Low | `POST_COLUMNS` selects the author's `email` (unused, parseUserFromJson sets email=""); `reportComment` doesn't verify caller==reporterId. | `:25`, `:225` | Unnecessary email data fetched; cross-user report spoofing if RLS permissive. | Drop email from projection; verify caller. | Open |
| 75 | SettingsViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SettingsViewModel.kt:38 | Medium | `resetE2EE()` launches `resetE2EUseCase()` without try/finally — if the use case throws, `_isResettingE2EE` stays `true` forever (UI stuck in resetting state). | `resetE2EE:38-44` | UI state inconsistency; reset stuck. | Wrap in try/finally to reset flag. | Fixed |
| 76 | AvatarViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AvatarViewModel.kt:14 | Medium | Injects concrete `EditProfileRepositoryImpl` instead of the `EditProfileRepository` interface (DI to concrete class). `uploadPhoto`/`uploadBitmap` only "simulate" upload (`delay(1000)` then fake success) — no real upload, misleading UX. `loadCurrentAvatar` silently swallows failures (`onFailure = { }`). | `:14`, `:61-77`, `:36-55` | DI to concrete class; fake upload stub; silent error swallowing. | Inject interface; implement real upload; surface load errors. | Open |
| 77 | PrivacySecurityViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/PrivacySecurityViewModel.kt:15 | Medium | Not annotated `@HiltViewModel`; uses `SettingsRepositoryImpl.getInstance(application)` manual singleton and `BiometricCheckerImpl()` direct instantiation — DI bypass. `TwoFactorSetupState.secret` holds the TOTP secret in ViewModel memory. | `:15-21`, `:319-323` | DI bypass; 2FA secret linger in ViewModel memory. | Annotate `@HiltViewModel`, inject deps; clear secret after use. | Open |
| 78 | AppLockScreen | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AppLockScreen.kt:41 | Low | `context as? FragmentActivity` cast — if context isn't a FragmentActivity, `toggleAppLock` is silently skipped with no user feedback. | `AppLockScreen:41-45` | Silent feature failure. | Surface error if cast fails. | Open |
| 79 | StorageDataViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageDataViewModel.kt:13 | Medium | Not `@HiltViewModel`; created via manual `StorageDataViewModelFactory` (ViewModelProvider.Factory) instead of Hilt injection — DI inconsistency. | `StorageDataViewModel:13` | Bypasses Hilt DI lifecycle/scoping. | Convert to `@HiltViewModel`. | Open |
| 80 | AppearanceViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/AppearanceViewModel.kt:13 | Medium | Not `@HiltViewModel`; uses `SettingsRepositoryImpl.getInstance(application)` manual singleton — DI bypass. | `AppearanceViewModel:13-16` | Bypasses Hilt DI. | Convert to `@HiltViewModel`, inject repository. | Open |
| 81 | SignOutViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/SignOutViewModel.kt:17 | Low | `signOut()` calls `authRepository.signOut()` without try/finally — if it throws, `isSigningOut` stays `true` and `onSignedOut()` never fires. | `signOut:17-24` | Sign-out flag stuck; callback never invoked. | Wrap in try/finally to reset flag. | Fixed |
| 82 | NotificationSettingsViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/NotificationSettingsViewModel.kt:209 | Informational | `updateOneSignalTags` syncs the user's notification preferences (likes, comments, global, etc.) as tags to OneSignal (third party). | `updateOneSignalTags:209-226` | User notification-preference state exposed to OneSignal. | Disclose in privacy policy; consider not tagging sensitive prefs. | Open |
| 83 | ScheduledPostsViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/ScheduledPostsViewModel.kt:30 | Medium | `private val authService = SupabaseAuthenticationService.getInstance(application)` uses the static singleton (DI bypass) instead of injecting an auth repository. | `:30` | Hard-coupled to Supabase auth singleton. | Inject auth repository via Hilt. | Open |
| 84 | LanguageRegionViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/LanguageRegionViewModel.kt:18 | Medium | Not `@HiltViewModel`; uses `SettingsRepositoryImpl.getInstance(application)` manual singleton — DI bypass. | `:18-23` | Bypasses Hilt DI. | Convert to `@HiltViewModel`, inject repository. | Open |
| 85 | StorageProviderScreen | app/src/main/kotlin/com/synapse/social/studioasinc/ui/settings/StorageProviderScreen.kt:76 | None | Storage provider config sheets accept API keys/secrets and pass them to the ViewModel for encrypted storage (ApiKeySettingsService). No UI-level findings; keys are transient in Compose state. | — | — | — | — |
| 86 | CreatePostViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/CreatePostViewModel.kt:124 | High | `private val authService = SupabaseAuthenticationService()` directly instantiates a backend auth service inside a ViewModel — a backend SDK call in the UI layer, violating the strict dependency-injection/frontend-separation rule (ViewModels must consume UseCases/domain interfaces only). | `:124`, `:138`, `:498`, `:405` | Hard-couples UI to Supabase auth; untestable; violates architecture. | Remove direct auth service; use an AuthRepository UseCase. | Open |
| 87 | CreatePostViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/CreatePostViewModel.kt:317-325 | Medium | `addMedia`/`updateMediaItem` log the full content URI and converted file path (`Processing URI: $uri`, `Converted URI to path: $path`) via `Log.d` — file paths can reveal user data (DCIM paths, filenames). | `addMedia:312-328`, `updateMediaItem:338-356` | File paths/URIs exposed in Logcat (PII). | Remove URI/path from logs; log only a result marker. | Open |
| 88 | SearchHandler | app/src/main/kotlin/com/synapse/social/studioasinc/feature/createpost/createpost/handlers/SearchHandler.kt:14 | Medium | Injects concrete `UserRepositoryImpl` and `LocationRepositoryImpl` (not interfaces); `searchUsers` propagates `userProfile.email` into `User` results (PII in search results). | `:14-16`, `:29-39` | DI to concrete classes; email exposed in user search results. | Inject interfaces; don't propagate email in search results. | Open |
| 89 | ScheduledPostPublisherWorker | app/src/main/kotlin/com/synapse/social/studioasinc/data/repository/ScheduledPostPublisherWorker.kt:46 | Medium | `runAttemptCount` referenced (not a `CoroutineWorker` property — verify compilation); uses static `SupabaseAuthenticationService.getInstance` (DI bypass). | `:46`, `:102` | Possible compile error; DI bypass. | Verify `runAttemptCount`; inject auth via EntryPoint. | Open |
| 90 | HomeActivity | app/src/main/kotlin/com/synapse/social/studioasinc/feature/home/HomeActivity.kt:40 | High | `getCurrentUserIdUseCase` lazy-initializes `GetCurrentUserIdUseCase(SupabaseAuthRepository())` — directly instantiates a concrete Supabase repository in the Activity (DI bypass, backend SDK in UI layer). | `:40-42` | Hard-couples Activity to Supabase; violates frontend/backend separation. | Inject GetCurrentUserIdUseCase via Hilt. | Open |
| 91 | HomeActivity | app/src/main/kotlin/com/synapse/social/studioasinc/feature/home/HomeActivity.kt:51 | Medium | `SettingsRepositoryImpl.getInstance(this@HomeActivity)` manual singleton (not injected); `onStart()` calls `getCurrentUserIdUseCase()` but discards the result (dead code). | `:51`, `:159-164` | DI inconsistency; dead code. | Inject repository; remove dead onStart logic. | Open |
| 92 | FeedViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/home/home/FeedViewModel.kt:276 | Low | `deletePost` and `toggleComments` call `e.printStackTrace()` — writes full stack traces to Logcat/System.err; error messages surfaced raw to user. | `deletePost:276`, `toggleComments:291` | Stack traces in logs; raw errors to UI. | Use Log.e sparingly; surface generic messages. | Open |
| 93 | EditProfileViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/EditProfileViewModel.kt:261-307 | High | `compressAndUploadAvatar` and `uploadCover` log the user's `userId`, content URI, and absolute file path to Logcat (`Processing avatar URI: $uri`, `Starting avatar upload for user: $userId, file: ...`). | `compressAndUploadAvatar:261-307`, `uploadCover:432-479` | User ID and file paths exposed in system logs. | Remove identifiers/paths from logs. | Open |
| 94 | EditProfileViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/EditProfileViewModel.kt:34 | Medium | Injects concrete `EditProfileRepositoryImpl` instead of the `EditProfileRepository` interface (DI to concrete class). | `:34` | Hard-coupled to concrete repository. | Inject interface via Hilt. | Open |
| 95 | EditProfileScreen | app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/EditProfileScreen.kt:195 | Low | `LaunchedEffect(uiState.error)` shows raw error strings (e.g. "Failed to save: ${error.message}") in a Snackbar — may leak exception details to the user. | `EditProfileScreen:195-199` | Raw error details to user. | Map errors to user-friendly messages. | Open |
| 96 | ProfileMediaViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/media/ProfileMediaViewModel.kt:29 | Medium | Injects concrete `EditProfileRepositoryImpl` instead of the interface (DI to concrete class). | `:29` | Hard-coupled to concrete repository. | Inject interface via Hilt. | Open |
| 97 | ProfessionalProfileViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/professional/ProfessionalProfileViewModel.kt:20 | Medium | Injects concrete `EditProfileRepositoryImpl`; `save()` calls `updateProfile` with a mass-assignment map (occupation/workplace) — relies on server RLS, no field allowlist. | `:20`, `:108-113` | DI to concrete class; mass-assignment via vulnerable repository. | Inject interface; allowlist fields. | Open |
| 98 | LocationViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/location/LocationViewModel.kt:19 | Medium | Injects concrete `EditProfileRepositoryImpl`; `saveProfile()` mass-assignment map (region/city/hometown) via `updateProfile`. | `:19`, `:136` | DI to concrete class; mass-assignment. | Inject interface; allowlist fields. | Open |
| 99 | FeaturedContentViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/featured/FeaturedContentViewModel.kt:22 | Medium | Injects concrete `EditProfileRepositoryImpl` and `PostRepositoryImpl` (both concrete classes). | `:22` | DI to concrete classes. | Inject interfaces via Hilt. | Open |
| 100 | PhotoHistoryViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/profile/editprofile/photohistory/PhotoHistoryViewModel.kt:32 | Medium | Injects concrete `EditProfileRepositoryImpl`; `setAsCurrent`/`deleteItem` mass-assignment maps (avatar/profile_cover_image) via `updateProfile`. | `:32`, `:107` | DI to concrete class; mass-assignment. | Inject interface; allowlist fields. | Open |
| 108 | StoryCreatorViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/creator/StoryCreatorViewModel.kt:83 | Medium | `currentUserId` getter calls `AuthHelper.getCurrentUserId()` which transitively uses `SupabaseAuthRepository()` direct instantiation (DI bypass, finding #2); `loadCurrentUser` logs full exception stack trace via `Log.e(..., error)`. | `:83`, `:97` | DI bypass via AuthHelper; stack trace in logs. | Inject AuthRepository via Hilt; redact exception. | Open |
| 109 | StoryViewerViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/stories/viewer/StoryViewerViewModel.kt:54 | Medium | Injects concrete `UserRepositoryImpl` (DI to concrete class); `loadStories` logs `$userId` and `${user.displayName ?: user.username}` via `android.util.Log.d/e` — user IDs and usernames in Logcat. | `:54`, `:69,86,94` | DI to concrete class; PII (userIds, usernames) in logs. | Inject interface; remove identifiers from logs. | Open |
| 110 | PostDetailViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/postdetail/PostDetailViewModel.kt:39 | Medium | Injects 7 concrete repository impls (`PostDetailRepositoryImpl`, `CommentRepositoryImpl`, `ReactionRepositoryImpl`, `PollRepositoryImpl`, `BookmarkRepositoryImpl`, `ReportRepositoryImpl`, `UserRepositoryImpl`) — DI to concrete classes; raw `e.message` surfaced to UI on load failure; `summarizePost` sends post content to AI provider. | `:39-54`, `:125`, `:472` | DI to concrete classes; raw errors to UI; post content to AI. | Inject interfaces; map errors; disclose AI content use. | Open |
| 111 | PostDetailActivity | app/src/main/kotlin/com/synapse/social/studioasinc/feature/post/PostDetailActivity.kt:13 | None | Sound `@AndroidEntryPoint` activity with proper deep-link/intent extra handling and navigation. No findings. | — | — | — | — |
| 112 | CallViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/call/CallViewModel.kt:287 | Low | Error messages logged via `Napier.w("Call could not be placed: ${error.message}")` and `Napier.w("Call control refused: ${error.message}")` — raw error strings in debug logs. | `:287`, `:402` | Minor info in logs. | Redact error messages in logs. | Open |
| 113 | BlockingViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/blocking/BlockingViewModel.kt:55 | Low | Raw `error.message` surfaced to UI in `BlockingUiState.Error`. | `:55`, `:81`, `:103` | Raw exception details to user. | Map errors to user-friendly messages. | Open |
| 114 | SearchViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/search/search/SearchViewModel.kt:113 | Medium | `addToHistory` stores raw search queries in `SettingsDataStore` (plaintext DataStore) — sensitive query history at rest; raw `err.message` surfaced to UI on failures; `summarizePost` sends post content to an AI provider. | `addToHistory:113`, `refreshCurrentTab:188`, `summarizePost:470` | Search history exposed at rest; raw errors to UI; post content to AI. | Encrypt search history; map errors; disclose AI content use. | Open |
| 115 | SearchActivity | app/src/main/kotlin/com/synapse/social/studioasinc/feature/search/SearchActivity.kt:36 | Informational | Chat mode shows a "Chat feature not implemented" Toast placeholder — dead/incomplete code path. | `:36` | Misleading UX for chat-originated search. | Implement or remove chat mode. | Open |
| 116 | EchoRoomViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/room/EchoRoomViewModel.kt:156 | Low | Error messages logged via `Napier.w("Echo join failed: ${error.message}", ...)` and similar throughout. | `:156`, `:216`, `:317`, `:433`, `:455`, `:494`, `:529`, `:548`, `:639` | Minor info in logs. | Redact error messages in logs. | Open |
| 117 | EchoLobbyViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/feature/echo/lobby/EchoLobbyViewModel.kt:61 | Low | Error messages logged via `Napier.w("Echo lobby load failed: ${error.message}", ...)`. | `:61`, `:131` | Minor info in logs. | Redact error messages in logs. | Open |
| 118 | SettingsBackupManager | app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/backup/SettingsBackupManager.kt:92 | High | `currentUserId()`/`requireCurrentUserId()` use `SupabaseClient.client.auth.currentUserOrNull()` — static Supabase client DI bypass; settings backup written to plaintext AtomicFile in `filesDir/settings_backups/` (all settings at rest unencrypted). | `:92-96`, `:186-190`, `:485-487` | Hard-coupled to Supabase; unencrypted settings backup at rest. | Inject Supabase client; encrypt backup at rest. | Open |
| 119 | SettingsCloudSyncWorker | app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/backup/SettingsCloudSyncWorker.kt:55 | Medium | `runAttemptCount < MAX_ATTEMPTS` — `runAttemptCount` is not a `CoroutineWorker` property (same as SyncWorker #24) — verify compilation. | `:55` | Possible compile error. | Verify `runAttemptCount` source. | Open |
| 120 | NotificationsViewModel | app/src/main/kotlin/com/synapse/social/studioasinc/ui/notifications/NotificationsViewModel.kt:72 | Low | `Napier.e("Failed to load notifications: $error")` logs the full error object (toString may include message/stack trace). | `:72`, `:152` | Minor info in logs. | Redact error in logs. | Open |
| 121 | NotificationsScreen | app/src/main/kotlin/com/synapse/social/studioasinc/ui/notifications/NotificationsScreen.kt:122 | Low | `item(key = "header_${date.hashCode()}")` uses String.hashCode() as a LazyList key — hash collisions could cause incorrect item identity/state. | `:122` | Potential wrong header key/state on hash collision. | Use the date string directly as key. | Open |
| 122 | BaseComposeActivity | app/src/main/kotlin/com/synapse/social/studioasinc/core/ui/base/BaseComposeActivity.kt:30 | Medium | `SettingsRepositoryImpl.getInstance(this)` manual singleton (not Hilt-injected) used in `setSynapseContent` and `applyThemeFromSettings` — DI bypass. | `:30`, `:59` | Bypasses Hilt DI for theming. | Inject repository via Hilt. | Open |
## D. Review Methodology

Phase 1 - Discovery: enumerated all 837 Kotlin source files across app/src/main, app/src/debug, and app/src/release source sets. Inspected AndroidManifest.xml and app/build.gradle.

Phase 2 - Individual Review: inspect each class end-to-end, following relevant dependencies (UseCases, Repositories, DataSources, DTOs, mappers). Evaluate architecture, correctness, security, Android-specific quality, performance, and maintainability per the task standard.

Phase 3 - Verification: revisit suspicious findings; verify fixes; run static analysis where practical.

Phase 4 - Final Audit: re-enumerate and reconcile inventory against this document.

Limitations: Review is based on static source inspection only. Runtime behavior (e.g., Supabase SDK internals, WebRTC native crashes) is assessed from code, not execution.

## E. Completion Criteria

The review is complete only when all 837 discovered classes have a documented review status (Reviewed or Blocked) and all findings have a resolution status. PAUSED at 19.0% (159/837). Continuation point: next unreviewed class in `app/src/main/kotlin/com/synapse/social/studioasinc/` — resume Phase 2 Individual Review from the Pending checklist entries.

