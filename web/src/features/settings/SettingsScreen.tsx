import React, { useEffect, useMemo, useState } from 'react';
import {
  Accessibility, ArrowLeft, BadgeCheck, Bell, Camera, Check, ChevronRight, CircleHelp,
  Cloud, Download, FlaskConical, Folder, HardDrive, Heart, Languages, Loader2,
  LockKeyhole, MessageCircle, Monitor, Moon, Palette, Search, Settings2, Shield,
  Sparkles, Sun, UserRound, type LucideIcon,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Switch } from '@/components/ui/Switch';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { supabase } from '@/lib/supabase';
import type { ThemeMode, UserProfile } from '@/types';
import { useThemeStore } from '@/app/useThemeStore';

type CategoryId =
  | 'synapse_plus' | 'account' | 'avatar' | 'privacy' | 'favourites' | 'appearance'
  | 'chat_settings' | 'chat_folders' | 'backup_restore' | 'notifications' | 'storage'
  | 'accessibility' | 'language' | 'about' | 'flags' | 'storage_provider' | 'ai_keys';
type Support = 'web' | 'partial' | 'android';

interface Feature { title: string; description: string; support?: Support; }
interface Category { id: CategoryId; title: string; subtitle: string; icon: LucideIcon; support: Support; keywords: string; }
interface Group { title: string; items: Category[]; }

type FontScale = 'small' | 'medium' | 'large' | 'extra_large';
interface AccessibilityPreferences {
  fontSize: FontScale;
  increaseContrast: boolean;
  highContrastText: boolean;
  reduceAnimations: boolean;
  autoplayAnimations: boolean;
}
interface NotificationPreferences {
  globalEnabled: boolean;
  likes: boolean;
  comments: boolean;
  replies: boolean;
  follows: boolean;
  messages: boolean;
  mentions: boolean;
  newPosts: boolean;
  shares: boolean;
  security: boolean;
  updates: boolean;
  quietHours: boolean;
  quietStart: string;
  quietEnd: string;
  doNotDisturb: boolean;
  inApp: boolean;
  reminders: boolean;
  highPriority: boolean;
  reactions: boolean;
}

const GROUPS: Group[] = [
  { title: 'Account', items: [
    { id: 'synapse_plus', title: 'Synapse Plus', subtitle: 'Membership, verification and subscriber benefits', icon: BadgeCheck, support: 'android', keywords: 'premium badge subscription plus' },
    { id: 'account', title: 'Account settings', subtitle: 'Profile, sign-in, account data and security', icon: UserRound, support: 'partial', keywords: 'security password email phone number profile delete passkeys' },
    { id: 'avatar', title: 'Avatar', subtitle: 'Profile photo and avatar controls', icon: Camera, support: 'partial', keywords: 'photo picture image profile avatar' },
  ]},
  { title: 'Preferences', items: [
    { id: 'privacy', title: 'Privacy & security', subtitle: 'Visibility, blocking, locks and privacy checkup', icon: Shield, support: 'android', keywords: 'block hide status last seen read receipts disappearing groups app lock' },
    { id: 'favourites', title: 'Favourites', subtitle: 'Your saved and prioritised people', icon: Heart, support: 'android', keywords: 'star bookmark save favourite favorites top' },
    { id: 'appearance', title: 'Appearance', subtitle: 'Theme, text size, typeface and feed layout', icon: Palette, support: 'web', keywords: 'theme dark mode light mode system font size colors layout' },
    { id: 'chat_settings', title: 'Chat settings', subtitle: 'Chat themes, wallpapers, media and gestures', icon: MessageCircle, support: 'android', keywords: 'chat message wallpaper bubble swipe media download quality' },
    { id: 'chat_folders', title: 'Chat folders', subtitle: 'Organise conversations into custom views', icon: Folder, support: 'android', keywords: 'chat folder organize tabs' },
  ]},
  { title: 'Notifications & storage', items: [
    { id: 'backup_restore', title: 'Backup & restore', subtitle: 'Back up and restore app data', icon: Download, support: 'android', keywords: 'backup restore sync export import' },
    { id: 'notifications', title: 'Notifications', subtitle: 'Choose which alerts you receive', icon: Bell, support: 'partial', keywords: 'sound tone alert vibrate badge mute likes comments follows messages mentions quiet hours' },
    { id: 'storage', title: 'Storage & data', subtitle: 'Storage use, downloads and network usage', icon: HardDrive, support: 'android', keywords: 'data usage download cache space memory network' },
  ]},
  { title: 'General', items: [
    { id: 'accessibility', title: 'Accessibility', subtitle: 'Text scale, contrast and motion preferences', icon: Accessibility, support: 'web', keywords: 'contrast animation text vision hearing reduce motion font size' },
    { id: 'language', title: 'Language & region', subtitle: 'App language and regional preferences', icon: Languages, support: 'partial', keywords: 'language locale translate region' },
    { id: 'about', title: 'About & support', subtitle: 'Help, policies, licences and app information', icon: CircleHelp, support: 'partial', keywords: 'help contact support privacy policy terms version licenses' },
  ]},
  { title: 'Experiments & services', items: [
    { id: 'flags', title: 'Feature flags', subtitle: 'Experimental and beta capabilities', icon: FlaskConical, support: 'android', keywords: 'flags experimental beta alpha features' },
    { id: 'storage_provider', title: 'Storage providers', subtitle: 'Configure upload and cloud providers', icon: Cloud, support: 'android', keywords: 'cloud provider cloudflare cloudinary supabase storage upload' },
    { id: 'ai_keys', title: 'AI & API keys', subtitle: 'AI provider credentials and integrations', icon: Sparkles, support: 'android', keywords: 'ai artificial intelligence api key openai gemini claude' },
  ]},
];

const FEATURE_DETAILS: Record<CategoryId, { intro: string; features: Feature[] }> = {
  synapse_plus: { intro: 'The Android app groups premium membership and verification tools here.', features: [
    { title: 'Membership status', description: 'View your Synapse Plus plan and renewal details.', support: 'android' },
    { title: 'Verification badge', description: 'Manage verification-related benefits.', support: 'android' },
    { title: 'Subscription management', description: 'Plan and billing actions remain in the Android app.', support: 'android' },
  ]},
  account: { intro: 'Manage the identity and sign-in options associated with your account.', features: [
    { title: 'View profile', description: 'Open your Synapse profile from this web app.', support: 'web' },
    { title: 'Password recovery', description: 'Password recovery is available from the sign-in screen.', support: 'partial' },
    { title: 'Passkeys & account security', description: 'Passkey management and additional sign-in controls are Android-only for now.', support: 'android' },
    { title: 'Change phone number', description: 'This account action is available in Android settings.', support: 'android' },
    { title: 'Passkeys & biometric sign-in', description: 'Passkey and biometric credentials are managed by the Android app.', support: 'android' },
    { title: 'Request account information', description: 'Export and account-data requests are not connected on web yet.', support: 'android' },
    { title: 'Scheduled posts', description: 'Review and manage scheduled posts in the Android app.', support: 'android' },
    { title: 'Business platform & brand partnerships', description: 'Business and partnership tools are Android-only.', support: 'android' },
  ]},
  avatar: { intro: 'Profile photo and avatar controls from the Android settings hub.', features: [
    { title: 'Current profile photo', description: 'Your current avatar is shown on the Profile page.', support: 'partial' },
    { title: 'Change or remove avatar', description: 'Avatar upload and history are not connected to web storage yet.', support: 'android' },
  ]},
  privacy: { intro: 'The Android app exposes detailed privacy and security controls. They are listed here for parity, but web changes are not yet wired to account enforcement.', features: [
    { title: 'Profile and content visibility', description: 'Public, followers-only and private visibility controls.', support: 'android' },
    { title: 'Last seen, profile photo, About and status', description: 'Set who can see each profile detail, including exception lists.', support: 'android' },
    { title: 'Group invitations', description: 'Choose who may add you to group conversations.', support: 'android' },
    { title: 'Read receipts & disappearing messages', description: 'Conversation privacy controls and retention periods.', support: 'android' },
    { title: 'Blocked contacts & privacy checkup', description: 'Review blocked accounts and run the privacy checklist.', support: 'android' },
    { title: 'Two-factor authentication & biometric lock', description: 'Additional account verification and biometric access are Android-only.', support: 'android' },
    { title: 'App lock & chat lock', description: 'Device-level locking controls are not available in this browser.', support: 'android' },
  ]},
  favourites: { intro: 'Manage the people and content you have marked as favourites.', features: [
    { title: 'Favourite people', description: 'View and organise priority contacts.', support: 'android' },
    { title: 'Saved shortcuts', description: 'Favourite lists and shortcut ordering are not connected on web yet.', support: 'android' },
  ]},
  appearance: { intro: 'Choose a calm, high-contrast look and comfortable reading size for Synapse on this device.', features: [
    { title: 'Theme mode', description: 'Choose light, dark or follow your device setting.', support: 'web' },
    { title: 'Text size', description: 'Scale the interface for comfortable reading.', support: 'web' },
    { title: 'Dynamic system colours', description: 'Android Material You colour extraction is not applicable to web.', support: 'android' },
    { title: 'Typeface & feed layout', description: 'Custom fonts and swipe/grid post layouts are Android-only today.', support: 'android' },
  ]},
  chat_settings: { intro: 'The Android app includes per-chat presentation and media options. Web chat is not enabled yet, so these controls are shown as unavailable rather than pretending to save.', features: [
    { title: 'Chat theme, wallpaper & bubbles', description: 'Conversation appearance controls.', support: 'android' },
    { title: 'Swipe actions & chat layout', description: 'Conversation gestures and layout options.', support: 'android' },
    { title: 'Media auto-download & upload quality', description: 'Choose download rules for mobile data, Wi-Fi and roaming.', support: 'android' },
    { title: 'Disappearing messages & chat lock', description: 'Conversation privacy controls are not available on web.', support: 'android' },
  ]},
  chat_folders: { intro: 'Create and organise conversation folders in the Android app.', features: [
    { title: 'Custom folders', description: 'Group chats by people, work or your own labels.', support: 'android' },
    { title: 'Folder filters', description: 'Unread, muted and pinned conversation filters.', support: 'android' },
  ]},
  backup_restore: { intro: 'Backups can contain private account and conversation data, so backup and restore are not offered from this web client.', features: [
    { title: 'Create or restore a backup', description: 'Use the Android app to create, export or restore a backup.', support: 'android' },
    { title: 'Backup encryption & migration', description: 'Encrypted backup and device migration are Android-only.', support: 'android' },
  ]},
  notifications: { intro: 'Notification preferences are saved to your Synapse account. Web push delivery itself is not configured in this client.', features: [
    { title: 'Global and category preferences', description: 'Control likes, comments, follows, messages and other alert categories.', support: 'partial' },
    { title: 'Quiet hours & Do Not Disturb', description: 'Set a quiet window and pause notifications.', support: 'partial' },
    { title: 'In-app, reminders and priority alerts', description: 'These preferences are stored for parity; web delivery is not connected yet.', support: 'partial' },
  ]},
  storage: { intro: 'Storage cleanup and media download controls depend on device storage and are available in the Android app.', features: [
    { title: 'Storage usage, cleanup & manage storage', description: 'Review media, documents, large files and chat storage.', support: 'android' },
    { title: 'Network usage', description: 'Review data usage and download behaviour.', support: 'android' },
    { title: 'Manage downloads', description: 'Auto-download and media-quality settings.', support: 'android' },
  ]},
  accessibility: { intro: 'These accessibility preferences are saved to your account and applied in this web app.', features: [
    { title: 'Text scale', description: 'Adjust the interface size.', support: 'web' },
    { title: 'Contrast & high-contrast text', description: 'Increase contrast for clearer visual separation.', support: 'web' },
    { title: 'Reduce motion & autoplay', description: 'Limit animation and autoplay behaviour.', support: 'web' },
  ]},
  language: { intro: 'The Android app provides language and region controls. The current web interface is English-only.', features: [
    { title: 'App language', description: 'English is the only language currently available in the web client.', support: 'partial' },
    { title: 'Region and locale', description: 'Regional formatting follows your browser settings.', support: 'partial' },
  ]},
  about: { intro: 'App information and support destinations from the Android settings hub.', features: [
    { title: 'Help & support', description: 'No web support destination is configured in this build.', support: 'partial' },
    { title: 'Privacy policy, terms & open-source licences', description: 'Policy and licence links are not configured in the web client.', support: 'android' },
    { title: 'App version & release information', description: 'Detailed version and release-note tools are not exposed in this web preview.', support: 'partial' },
    { title: 'Build information', description: 'This is the Synapse Social web client.', support: 'web' },
  ]},
  flags: { intro: 'Experimental flags are controlled by the Android app and are not exposed to web accounts.', features: [
    { title: 'Beta and experimental features', description: 'Feature flags remain Android-only to avoid enabling unsupported web behaviour.', support: 'android' },
  ]},
  storage_provider: { intro: 'Upload provider credentials and provider routing are intentionally not stored in browser code.', features: [
    { title: 'Image, video and file providers', description: 'Provider selection and upload credentials are Android-only.', support: 'android' },
    { title: 'Cloudinary, Supabase and Cloudflare R2', description: 'Provider configuration is not editable from the web client.', support: 'android' },
    { title: 'Upload compression', description: 'Compression preferences are not yet available on web.', support: 'android' },
  ]},
  ai_keys: { intro: 'The Android app can store AI provider keys locally. The web client does not expose API-key entry because browser storage is not a safe place for provider secrets.', features: [
    { title: 'AI provider keys', description: 'Manage provider credentials from the Android app only.', support: 'android' },
    { title: 'AI integrations', description: 'AI settings are not connected to this web client.', support: 'android' },
  ]},
};

const DEFAULT_NOTIFICATIONS: NotificationPreferences = {
  globalEnabled: true, likes: true, comments: true, replies: true, follows: true, messages: true,
  mentions: true, newPosts: true, shares: true, security: true, updates: true, quietHours: false,
  quietStart: '22:00', quietEnd: '07:00', doNotDisturb: false, inApp: true, reminders: false,
  highPriority: true, reactions: true,
};
const DEFAULT_ACCESSIBILITY: AccessibilityPreferences = {
  fontSize: 'medium', increaseContrast: false, highContrastText: false, reduceAnimations: false, autoplayAnimations: true,
};

const toRecord = (value: unknown): Record<string, unknown> => value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {};
const boolOr = (value: unknown, fallback: boolean) => typeof value === 'boolean' ? value : fallback;
const validTheme = (value: unknown): value is ThemeMode => value === 'light' || value === 'dark' || value === 'system';
const validFontScale = (value: unknown): value is FontScale => value === 'small' || value === 'medium' || value === 'large' || value === 'extra_large';

const decodeNotifications = (row: Record<string, unknown> | null): NotificationPreferences => {
  if (!row) return DEFAULT_NOTIFICATIONS;
  const settings = toRecord(row.settings);
  const social = toRecord(settings.social);
  const content = toRecord(settings.content);
  const system = toRecord(settings.system);
  const web = toRecord(settings.web);
  const quiet = toRecord(row.quiet_hours);
  return {
    globalEnabled: boolOr(row.enabled, DEFAULT_NOTIFICATIONS.globalEnabled),
    likes: boolOr(social.likes, DEFAULT_NOTIFICATIONS.likes),
    comments: boolOr(social.comments, DEFAULT_NOTIFICATIONS.comments),
    replies: boolOr(social.replies, DEFAULT_NOTIFICATIONS.replies),
    follows: boolOr(social.follows, DEFAULT_NOTIFICATIONS.follows),
    messages: boolOr(social.messages, DEFAULT_NOTIFICATIONS.messages),
    mentions: boolOr(social.mentions, DEFAULT_NOTIFICATIONS.mentions),
    newPosts: boolOr(content.new_posts, DEFAULT_NOTIFICATIONS.newPosts),
    shares: boolOr(content.shares, DEFAULT_NOTIFICATIONS.shares),
    security: boolOr(system.security, DEFAULT_NOTIFICATIONS.security),
    updates: boolOr(system.updates, DEFAULT_NOTIFICATIONS.updates),
    quietHours: boolOr(quiet.enabled, DEFAULT_NOTIFICATIONS.quietHours),
    quietStart: typeof quiet.start === 'string' ? quiet.start : DEFAULT_NOTIFICATIONS.quietStart,
    quietEnd: typeof quiet.end === 'string' ? quiet.end : DEFAULT_NOTIFICATIONS.quietEnd,
    doNotDisturb: boolOr(row.do_not_disturb, DEFAULT_NOTIFICATIONS.doNotDisturb),
    inApp: boolOr(web.in_app, DEFAULT_NOTIFICATIONS.inApp),
    reminders: boolOr(web.reminders, DEFAULT_NOTIFICATIONS.reminders),
    highPriority: boolOr(web.high_priority, DEFAULT_NOTIFICATIONS.highPriority),
    reactions: boolOr(web.reactions, DEFAULT_NOTIFICATIONS.reactions),
  };
};

const supportLabel = (support: Support) => support === 'web' ? 'Web ready' : support === 'partial' ? 'Partially available' : 'Android feature';

const FeatureRow: React.FC<{ feature: Feature }> = ({ feature }) => {
  const support = feature.support || 'android';
  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-border bg-card px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0"><h4 className="text-sm font-semibold text-foreground">{feature.title}</h4><p className="mt-1 text-sm leading-5 text-muted-foreground">{feature.description}</p></div>
      <span className={`inline-flex w-fit shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold ${support === 'web' ? 'bg-foreground text-background' : support === 'partial' ? 'bg-muted text-muted-foreground' : 'border border-border text-muted-foreground'}`}>{supportLabel(support)}</span>
    </div>
  );
};

const SettingToggle: React.FC<{ title: string; description: string; checked: boolean; onChange: (value: boolean) => void; disabled?: boolean }> = ({ title, description, checked, onChange, disabled }) => (
  <div className={`flex items-center justify-between gap-4 rounded-2xl border border-border bg-card px-4 py-4 ${disabled ? 'opacity-50' : ''}`}>
    <div className="min-w-0"><p className="text-sm font-medium text-foreground">{title}</p><p className="mt-1 text-xs leading-5 text-muted-foreground">{description}</p></div>
    <Switch checked={checked} onCheckedChange={onChange} disabled={disabled} aria-label={title} />
  </div>
);

const SaveButton: React.FC<{ saving: boolean; onClick: () => void; label?: string }> = ({ saving, onClick, label = 'Save preferences' }) => (
  <Button type="button" disabled={saving} onClick={onClick} className="rounded-full bg-foreground px-5 text-sm font-semibold text-background shadow-none hover:opacity-85 disabled:opacity-50">
    {saving ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Saving…</> : <><Check className="mr-2 h-4 w-4" />{label}</>}
  </Button>
);

interface SettingsScreenProps {
  user: UserProfile;
  onClose: () => void;
  onLogout?: () => void;
  onOpenProfile?: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ user, onClose, onLogout, onOpenProfile }) => {
  const theme = useThemeStore(state => state.theme);
  const setTheme = useThemeStore(state => state.setTheme);
  const [activeCategory, setActiveCategory] = useState<CategoryId | null>(null);
  const [search, setSearch] = useState('');
  const [appearance, setAppearance] = useState<AccessibilityPreferences>(DEFAULT_ACCESSIBILITY);
  const [notifications, setNotifications] = useState<NotificationPreferences>(DEFAULT_NOTIFICATIONS);
  const [userSettingsRow, setUserSettingsRow] = useState<Record<string, unknown>>({});
  const [notificationSettingsRow, setNotificationSettingsRow] = useState<Record<string, unknown>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [saving, setSaving] = useState<'appearance' | 'notifications' | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setIsLoading(true);
      setLoadError(null);
      try {
        const [settingsResult, notificationResult] = await Promise.all([
          supabase.from('user_settings').select('theme,language,font_size,notifications_enabled,increase_contrast,high_contrast_text,reduce_animations,autoplay_animations,data').eq('user_id', user.id).maybeSingle(),
          supabase.from('notification_preferences').select('enabled,settings,quiet_hours,do_not_disturb,dnd_until').eq('user_id', user.id).maybeSingle(),
        ]);
        if (cancelled) return;
        const errors: string[] = [];
        if (settingsResult.error) errors.push('Appearance preferences could not be loaded.');
        else {
          const row = (settingsResult.data || {}) as Record<string, unknown>;
          setUserSettingsRow(row);
          const loadedTheme = validTheme(row.theme) ? row.theme : useThemeStore.getState().theme;
          setTheme(loadedTheme);
          const fontSize = String(row.font_size || '').toLowerCase().replace(/-/g, '_');
          setAppearance({
            fontSize: validFontScale(fontSize) ? fontSize : DEFAULT_ACCESSIBILITY.fontSize,
            increaseContrast: boolOr(row.increase_contrast, DEFAULT_ACCESSIBILITY.increaseContrast),
            highContrastText: boolOr(row.high_contrast_text, DEFAULT_ACCESSIBILITY.highContrastText),
            reduceAnimations: boolOr(row.reduce_animations, DEFAULT_ACCESSIBILITY.reduceAnimations),
            autoplayAnimations: boolOr(row.autoplay_animations, DEFAULT_ACCESSIBILITY.autoplayAnimations),
          });
        }
        if (notificationResult.error) errors.push('Notification preferences could not be loaded.');
        else {
          const row = (notificationResult.data || {}) as Record<string, unknown>;
          setNotificationSettingsRow(row);
          setNotifications(decodeNotifications(notificationResult.data as Record<string, unknown> | null));
        }
        if (errors.length) setLoadError(errors.join(' '));
      } catch (error) {
        if (!cancelled) setLoadError(error instanceof Error ? error.message : 'Could not load your settings.');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };
    void load();
    return () => { cancelled = true; };
  }, [user.id, setTheme]);

  useEffect(() => {
    const root = document.documentElement;
    const fontSize = appearance.fontSize === 'small' ? '14px' : appearance.fontSize === 'large' ? '18px' : appearance.fontSize === 'extra_large' ? '20px' : '16px';
    root.style.fontSize = fontSize;
    root.dataset.synapseContrast = appearance.increaseContrast ? 'true' : 'false';
    root.dataset.synapseHighContrastText = appearance.highContrastText ? 'true' : 'false';
    root.dataset.synapseReduceMotion = appearance.reduceAnimations ? 'true' : 'false';
  }, [appearance]);

  const filteredGroups = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return GROUPS;
    return GROUPS.map(group => ({
      ...group,
      items: group.items.filter(item => `${item.title} ${item.subtitle} ${item.keywords} ${FEATURE_DETAILS[item.id].features.map(feature => `${feature.title} ${feature.description}`).join(' ')}`.toLowerCase().includes(query)),
    })).filter(group => group.items.length > 0);
  }, [search]);

  const category = activeCategory ? GROUPS.flatMap(group => group.items).find(item => item.id === activeCategory) || null : null;
  const selectedGroup = activeCategory ? GROUPS.find(group => group.items.some(item => item.id === activeCategory)) : undefined;
  const detail = category ? FEATURE_DETAILS[category.id] : null;

  const saveAppearance = async () => {
    setSaving('appearance'); setSaveError(null); setSaveMessage(null);
    try {
      const oldData = toRecord(userSettingsRow.data);
      const oldWebData = toRecord(oldData.web);
      const payload = {
        ...userSettingsRow,
        user_id: user.id,
        theme,
        font_size: appearance.fontSize,
        increase_contrast: appearance.increaseContrast,
        high_contrast_text: appearance.highContrastText,
        reduce_animations: appearance.reduceAnimations,
        autoplay_animations: appearance.autoplayAnimations,
        data: { ...oldData, web: { ...oldWebData, settingsVersion: 1, fontSize: appearance.fontSize, reduceAnimations: appearance.reduceAnimations } },
        updated_at: new Date().toISOString(),
      };
      const { error } = await supabase.from('user_settings').upsert(payload, { onConflict: 'user_id' });
      if (error) throw error;
      setUserSettingsRow(payload);
      setSaveMessage('Appearance and accessibility preferences saved to your account.');
    } catch (error) {
      setSaveError(error instanceof Error ? error.message : 'Could not save these preferences.');
    } finally { setSaving(null); }
  };

  const saveNotifications = async () => {
    setSaving('notifications'); setSaveError(null); setSaveMessage(null);
    try {
      const oldSettings = toRecord(notificationSettingsRow.settings);
      const oldSocial = toRecord(oldSettings.social);
      const oldContent = toRecord(oldSettings.content);
      const oldSystem = toRecord(oldSettings.system);
      const oldWeb = toRecord(oldSettings.web);
      const oldQuiet = toRecord(notificationSettingsRow.quiet_hours);
      const settings = {
        ...oldSettings,
        social: { ...oldSocial, likes: notifications.likes, comments: notifications.comments, replies: notifications.replies, follows: notifications.follows, messages: notifications.messages, mentions: notifications.mentions },
        content: { ...oldContent, new_posts: notifications.newPosts, shares: notifications.shares },
        system: { ...oldSystem, security: notifications.security, updates: notifications.updates },
        web: { ...oldWeb, in_app: notifications.inApp, reminders: notifications.reminders, high_priority: notifications.highPriority, reactions: notifications.reactions },
      };
      const quietHours = { ...oldQuiet, enabled: notifications.quietHours, start: notifications.quietStart, end: notifications.quietEnd };
      const payload = {
        ...notificationSettingsRow,
        user_id: user.id,
        enabled: notifications.globalEnabled,
        settings,
        quiet_hours: quietHours,
        do_not_disturb: notifications.doNotDisturb,
        dnd_until: notifications.doNotDisturb ? notificationSettingsRow.dnd_until ?? null : null,
        updated_at: new Date().toISOString(),
      };
      const { error } = await supabase.from('notification_preferences').upsert(payload, { onConflict: 'user_id' });
      if (error) throw error;
      setNotificationSettingsRow(payload);
      setSaveMessage('Notification preferences saved. Web push delivery is not configured in this client.');
    } catch (error) {
      setSaveError(error instanceof Error ? error.message : 'Could not save notification preferences.');
    } finally { setSaving(null); }
  };

  const updateNotification = (key: keyof NotificationPreferences, value: boolean | string) => {
    setNotifications(current => ({ ...current, [key]: value }));
    setSaveMessage(null); setSaveError(null);
  };

  const renderSupportedControls = () => {
    if (!category || !detail) return null;
    if (category.id === 'appearance') return (
      <div className="space-y-6">
        <div><h3 className="text-sm font-semibold text-foreground">Theme</h3><p className="mt-1 text-sm text-muted-foreground">Use a clean light or dark surface, or follow your device.</p>
          <div className="mt-3 grid grid-cols-3 gap-2">{([{ value: 'system', label: 'System', Icon: Monitor }, { value: 'light', label: 'Light', Icon: Sun }, { value: 'dark', label: 'Dark', Icon: Moon }] as const).map(option => <button key={option.value} type="button" onClick={() => { setTheme(option.value); setSaveMessage(null); }} aria-pressed={theme === option.value} className={`flex items-center justify-center gap-2 rounded-2xl border px-3 py-3 text-sm font-medium transition-colors ${theme === option.value ? 'border-foreground bg-foreground text-background' : 'border-border bg-card text-muted-foreground hover:bg-muted'}`}><option.Icon className="h-4 w-4" />{option.label}</button>)}</div>
        </div>
        <div><h3 className="text-sm font-semibold text-foreground">Interface text size</h3><p className="mt-1 text-sm text-muted-foreground">Applied immediately in this browser; save to sync with your account.</p>
          <div className="mt-3 flex flex-wrap gap-2">{([{ value: 'small', label: 'Small' }, { value: 'medium', label: 'Default' }, { value: 'large', label: 'Large' }, { value: 'extra_large', label: 'Extra large' }] as const).map(option => <button key={option.value} type="button" onClick={() => setAppearance(current => ({ ...current, fontSize: option.value }))} aria-pressed={appearance.fontSize === option.value} className={`rounded-full border px-4 py-2 text-sm font-medium transition-colors ${appearance.fontSize === option.value ? 'border-foreground bg-foreground text-background' : 'border-border text-muted-foreground hover:bg-muted'}`}>{option.label}</button>)}</div>
        </div>
        <div className="space-y-3"><FeatureRow feature={detail.features[2]} /><FeatureRow feature={detail.features[3]} /></div>
        <SaveButton saving={saving === 'appearance'} onClick={() => void saveAppearance()} />
      </div>
    );

    if (category.id === 'accessibility') return (
      <div className="space-y-3">
        <SettingToggle title="Increase contrast" description="Strengthen the separation between surfaces and controls." checked={appearance.increaseContrast} onChange={value => setAppearance(current => ({ ...current, increaseContrast: value }))} />
        <SettingToggle title="High contrast text" description="Increase text contrast throughout the web app." checked={appearance.highContrastText} onChange={value => setAppearance(current => ({ ...current, highContrastText: value }))} />
        <SettingToggle title="Reduce animations" description="Reduce motion and transition effects in this browser." checked={appearance.reduceAnimations} onChange={value => setAppearance(current => ({ ...current, reduceAnimations: value }))} />
        <SettingToggle title="Autoplay animations" description="Allow motion previews to play automatically." checked={appearance.autoplayAnimations} onChange={value => setAppearance(current => ({ ...current, autoplayAnimations: value }))} />
        <div className="pt-2"><SaveButton saving={saving === 'appearance'} onClick={() => void saveAppearance()} /></div>
      </div>
    );

    if (category.id === 'notifications') {
      const rows: Array<[keyof NotificationPreferences, string, string]> = [
        ['likes', 'Likes', 'When someone likes your post.'], ['comments', 'Comments', 'New comments on your posts.'], ['replies', 'Replies', 'Replies to conversations you follow.'], ['follows', 'New followers', 'When someone follows your profile.'], ['messages', 'Messages', 'New direct-message alerts.'], ['mentions', 'Mentions', 'When another person mentions you.'], ['newPosts', 'New posts', 'Posts from people you follow.'], ['shares', 'Shares', 'When your content is shared.'], ['security', 'Security alerts', 'Important account and security events.'], ['updates', 'Product updates', 'Synapse service and release updates.'], ['inApp', 'In-app alerts', 'Show notification indicators inside Synapse.'], ['reminders', 'Reminders', 'Occasional reminders and follow-ups.'], ['highPriority', 'Priority alerts', 'Keep important alerts prominent.'], ['reactions', 'Reactions', 'Notifications for post reactions.'],
      ];
      return <div className="space-y-3">
        <SettingToggle title="All notifications" description="Master switch for notification preferences." checked={notifications.globalEnabled} onChange={value => updateNotification('globalEnabled', value)} />
        <div className={`space-y-3 ${notifications.globalEnabled ? '' : 'opacity-50'}`}>{rows.map(([key, title, description]) => <SettingToggle key={key} title={title} description={description} checked={notifications[key] as boolean} onChange={value => updateNotification(key, value)} disabled={!notifications.globalEnabled} />)}</div>
        <div className="rounded-2xl border border-border bg-muted/50 p-4"><h4 className="text-sm font-semibold text-foreground">Quiet time</h4><div className="mt-3"><SettingToggle title="Quiet hours" description="Pause routine alerts during a daily window." checked={notifications.quietHours} onChange={value => updateNotification('quietHours', value)} /></div>{notifications.quietHours && <div className="mt-3 grid grid-cols-2 gap-3"><label className="text-xs font-medium text-muted-foreground">Starts<input type="time" value={notifications.quietStart} onChange={event => updateNotification('quietStart', event.target.value)} className="mt-1 h-10 w-full rounded-xl border border-border bg-background px-3 text-sm text-foreground" /></label><label className="text-xs font-medium text-muted-foreground">Ends<input type="time" value={notifications.quietEnd} onChange={event => updateNotification('quietEnd', event.target.value)} className="mt-1 h-10 w-full rounded-xl border border-border bg-background px-3 text-sm text-foreground" /></label></div>}<div className="mt-3"><SettingToggle title="Do Not Disturb" description="Pause routine notifications until turned off." checked={notifications.doNotDisturb} onChange={value => updateNotification('doNotDisturb', value)} /></div></div>
        <div className="pt-2"><SaveButton saving={saving === 'notifications'} onClick={() => void saveNotifications()} label="Save notification preferences" /></div>
      </div>;
    }

    if (category.id === 'account') return (
      <div className="space-y-4">
        <div className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 sm:flex-row sm:items-center"><div className="flex min-w-0 items-center gap-3"><Avatar className="h-12 w-12"><AvatarImage src={user.avatarUrl} alt="" /><AvatarFallback>{user.fullName.slice(0, 1).toUpperCase()}</AvatarFallback></Avatar><div className="min-w-0"><p className="truncate font-semibold text-foreground">{user.fullName}</p><p className="truncate text-sm text-muted-foreground">@{user.username}</p></div></div>{onOpenProfile && <Button type="button" variant="outline" onClick={onOpenProfile} className="rounded-full border-border sm:ml-auto">View profile<ChevronRight className="ml-2 h-4 w-4" /></Button>}</div>
        <FeatureRow feature={detail.features[1]} />
        {detail.features.slice(2).map(feature => <FeatureRow key={feature.title} feature={feature} />)}
        {onLogout && <Button type="button" variant="outline" onClick={onLogout} className="w-full justify-start rounded-2xl border-border px-4 py-3 text-sm text-foreground"><LockKeyhole className="mr-3 h-4 w-4" />Sign out of Synapse</Button>}
      </div>
    );

    if (category.id === 'avatar' && onOpenProfile) return <div className="space-y-4"><div className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5"><Avatar className="h-16 w-16"><AvatarImage src={user.avatarUrl} alt="" /><AvatarFallback>{user.fullName.slice(0, 1).toUpperCase()}</AvatarFallback></Avatar><div><p className="font-semibold text-foreground">{user.fullName}</p><p className="text-sm text-muted-foreground">@{user.username}</p></div></div>{detail.features.map(feature => <FeatureRow key={feature.title} feature={feature} />)}<Button type="button" variant="outline" onClick={onOpenProfile} className="rounded-full border-border">Open profile<ChevronRight className="ml-2 h-4 w-4" /></Button></div>;

    if (category.id === 'language') return <div className="space-y-4"><div className="rounded-2xl border border-border bg-card p-5"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">Current web language</p><p className="mt-2 text-lg font-semibold text-foreground">English</p><p className="mt-1 text-sm leading-5 text-muted-foreground">This web build is English-only. Region and number formatting follow your browser.</p></div>{detail.features.map(feature => <FeatureRow key={feature.title} feature={feature} />)}</div>;

    return <div className="space-y-3">{detail.features.map(feature => <FeatureRow key={feature.title} feature={feature} />)}{category.id === 'about' && <div className="rounded-2xl border border-border bg-muted/40 px-4 py-3 text-xs text-muted-foreground">Settings catalog based on the current Synapse Android settings hub. Unsupported web actions are labelled rather than simulated.</div>}</div>;
  };

  const accountInitial = user.fullName?.trim()?.slice(0, 1)?.toUpperCase() || 'S';
  const navigateBack = () => {
    if (activeCategory) {
      setActiveCategory(null);
      setSaveError(null);
      setSaveMessage(null);
    } else {
      onClose();
    }
  };

  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="sticky top-0 z-30 border-b border-border bg-card">
        <div className="mx-auto flex h-16 max-w-5xl items-center gap-3 px-4 sm:px-6">
          <Button type="button" variant="ghost" onClick={navigateBack} aria-label={activeCategory ? 'Back to settings' : 'Back to Home'} className="h-10 w-10 shrink-0 rounded-full p-0 text-foreground hover:bg-muted"><ArrowLeft className="h-5 w-5" /></Button>
          <div className="min-w-0 flex-1">
            <p className="truncate text-[10px] font-bold uppercase tracking-[0.16em] text-muted-foreground">{activeCategory ? selectedGroup?.title : 'Synapse account'}</p>
            <h1 className="truncate text-base font-semibold text-card-foreground">{category ? category.title : 'Settings'}</h1>
          </div>
          {category && <span className={category.support === 'web' ? 'rounded-full bg-foreground px-3 py-1 text-[10px] font-semibold text-background' : category.support === 'partial' ? 'rounded-full bg-muted px-3 py-1 text-[10px] font-semibold text-muted-foreground' : 'rounded-full border border-border px-3 py-1 text-[10px] font-semibold text-muted-foreground'}>{supportLabel(category.support)}</span>}
        </div>
      </header>

      <main className="mx-auto w-full max-w-4xl space-y-5 px-4 py-6 sm:px-6 sm:py-8">
        {(loadError || saveError || saveMessage) && <div role={saveError || loadError ? 'alert' : 'status'} className={saveError || loadError ? 'rounded-2xl border border-border bg-card px-4 py-3 text-sm text-foreground' : 'rounded-2xl border border-border bg-muted/50 px-4 py-3 text-sm text-foreground'}>{saveError || loadError || saveMessage}</div>}

        {category && detail ? (
          <section className="space-y-5">
            <div className="rounded-3xl border border-border bg-card p-5 shadow-sm sm:p-7">
              <div className="flex items-start gap-4">
                <span className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-muted text-foreground"><category.icon className="h-5 w-5" /></span>
                <div className="min-w-0"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">{selectedGroup?.title}</p><h2 className="mt-1 text-2xl font-semibold tracking-tight text-card-foreground">{category.title}</h2><p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">{detail.intro}</p></div>
              </div>
            </div>
            <div className="rounded-3xl border border-border bg-card p-5 shadow-sm sm:p-7">
              {isLoading ? <div className="space-y-3" aria-label="Loading preferences"><div className="h-20 animate-pulse rounded-2xl bg-muted" /><div className="h-20 animate-pulse rounded-2xl bg-muted" /><div className="h-20 animate-pulse rounded-2xl bg-muted" /></div> : renderSupportedControls()}
            </div>
            <div className="flex items-center gap-2 px-1 text-xs text-muted-foreground"><Settings2 className="h-4 w-4 shrink-0" /><span>Settings unavailable on web are identified as Android features.</span></div>
          </section>
        ) : (
          <div className="space-y-6">
            <section className="space-y-2"><p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">Your Synapse</p><h2 className="text-3xl font-semibold tracking-tight text-foreground">Settings</h2><p className="max-w-2xl text-sm leading-6 text-muted-foreground">Manage your account and preferences. Choose a section to open its own screen.</p></section>

            <div className="flex items-center gap-4 rounded-3xl border border-border bg-card p-5 shadow-sm">
              <Avatar className="h-14 w-14"><AvatarImage src={user.avatarUrl} alt="" /><AvatarFallback>{accountInitial}</AvatarFallback></Avatar>
              <div className="min-w-0 flex-1"><p className="truncate text-base font-semibold text-card-foreground">{user.fullName}</p><p className="mt-0.5 truncate text-sm text-muted-foreground">@{user.username}</p></div>
              <span className="hidden rounded-full bg-muted px-3 py-1.5 text-[11px] font-medium text-muted-foreground sm:inline-flex">Personal account</span>
            </div>

            <label className="relative block"><Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" /><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Search settings" aria-label="Search settings" className="h-12 w-full rounded-2xl border border-border bg-card pl-11 pr-4 text-sm text-foreground shadow-sm outline-none placeholder:text-muted-foreground focus:ring-2 focus:ring-ring/30" /></label>

            {filteredGroups.length ? filteredGroups.map(group => <section key={group.title} className="space-y-2">
              <h3 className="px-1 text-xs font-bold uppercase tracking-[0.16em] text-muted-foreground">{group.title}</h3>
              <div className="overflow-hidden rounded-3xl border border-border bg-card shadow-sm divide-y divide-border">
                {group.items.map(item => { const Icon = item.icon; return <button type="button" key={item.id} onClick={() => { setActiveCategory(item.id); setSaveError(null); setSaveMessage(null); window.scrollTo({ top: 0, behavior: appearance.reduceAnimations ? 'auto' : 'smooth' }); }} className="flex w-full items-center gap-4 px-4 py-4 text-left transition-colors hover:bg-muted/60 sm:px-5">
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-muted text-foreground"><Icon className="h-4 w-4" /></span>
                  <span className="min-w-0 flex-1"><span className="block text-sm font-semibold text-card-foreground">{item.title}</span><span className="mt-1 block text-xs leading-5 text-muted-foreground">{item.subtitle}</span></span>
                  <span className="hidden shrink-0 rounded-full border border-border px-2.5 py-1 text-[10px] font-semibold text-muted-foreground sm:inline-flex">{supportLabel(item.support)}</span>
                  <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
                </button>; })}
              </div>
            </section>) : <div className="rounded-3xl border border-border bg-card px-6 py-12 text-center"><Search className="mx-auto h-5 w-5 text-muted-foreground" /><p className="mt-3 font-medium text-card-foreground">No settings found</p><p className="mt-1 text-sm text-muted-foreground">Try a different search term.</p></div>}
          </div>
        )}
      </main>
    </div>
  );
};
