import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ProfileEditorSectionContent } from './ProfileEditorSections';
import type { ProfileEditorModel } from './useProfileEditor';

const makeModel = (overrides: Record<string, unknown> = {}) => ({
  section: 'basic',
  user: { id: 'user-1', username: 'ashik', fullName: 'Ashik Ahmed' },
  profile: { profile_cover_image: '' },
  draft: {
    username: 'ashik', display_name: 'Ashik Ahmed', bio: '', avatar: '', profile_cover_image: '',
    gender: 'hidden', pronouns: '', birthday: '', relationship_status: '', public_email: '',
    region: '', current_city: '', hometown: '', occupation: '', workplace: '', education: '',
    discord_tag: '', github_profile: '', personal_website: '', website: '', interests: [],
    featured_post_ids: [], featured_media_urls: [], profile_layout: 'feed',
  },
  avatarPreview: null,
  coverPreview: null,
  avatarHistory: [],
  coverHistory: [],
  historyLoading: false,
  historyError: null,
  selectFile: vi.fn(),
  setAvatarFile: vi.fn(),
  setCoverFile: vi.fn(),
  updateDraft: vi.fn(),
  pendingInterest: '',
  setPendingInterest: vi.fn(),
  addInterest: vi.fn(),
  error: null,
  businessAccount: null,
  businessError: null,
  businessLoading: false,
  confirmBusiness: false,
  setConfirmBusiness: vi.fn(),
  switchingBusiness: false,
  changeBusinessAccount: vi.fn(),
  candidatePosts: [],
  candidateMedia: [],
  featuredError: null,
  featuredLoading: false,
  featuredTab: 'posts',
  setFeaturedTab: vi.fn(),
  imageFor: (url: string | null) => url,
  ...overrides,
}) as unknown as ProfileEditorModel;

const sectionCases = [
  ['basic', 'Identity preview'],
  ['media', 'Profile photo'],
  ['personal', 'Gender'],
  ['location', 'Region'],
  ['work', 'Occupation'],
  ['social', 'Discord tag'],
  ['interests', 'Your interests'],
  ['professional', 'Account type'],
  ['appearance', 'Cover appearance'],
  ['featured', 'Profile highlights'],
  ['layout', 'Choose a profile layout'],
] as const;

describe('ProfileEditorSectionContent', () => {
  it.each(sectionCases)('renders the dedicated %s section', (section, expectedContent) => {
    render(<ProfileEditorSectionContent model={makeModel({ section })} />);
    expect(screen.getByText(expectedContent)).toBeInTheDocument();
  });

  it('forwards basic-information edits to the profile model', () => {
    const model = makeModel({ section: 'basic' });
    render(<ProfileEditorSectionContent model={model} />);
    fireEvent.change(screen.getByLabelText(/Username/), { target: { value: 'new_handle' } });
    expect(model.updateDraft).toHaveBeenCalledWith('username', 'new_handle');
  });

  it('composes the avatar and cover managers in the media section', () => {
    render(<ProfileEditorSectionContent model={makeModel({ section: 'media' })} />);
    expect(screen.getByText('Cover photo')).toBeInTheDocument();
    expect(screen.getAllByText('No previous images found.')).toHaveLength(2);
  });
});
