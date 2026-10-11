import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { SuccessExpandingButton } from './SuccessExpandingButton';

afterEach(() => vi.restoreAllMocks());

describe('SuccessExpandingButton', () => {
  it('preserves the idle button label and interaction', () => {
    render(
      <SuccessExpandingButton
        phase="idle"
        loadingLabel="Signing in…"
        successTitle="Welcome back!"
        successMessage="Signed in."
      >
        Continue
      </SuccessExpandingButton>,
    );

    expect(screen.getByRole('button', { name: 'Continue' })).toBeEnabled();
  });

  it('prevents duplicate submission and shows contextual loading inside the button', () => {
    render(
      <SuccessExpandingButton
        phase="loading"
        loadingLabel="Signing in…"
        successTitle="Welcome back!"
      >
        Continue
      </SuccessExpandingButton>,
    );

    expect(screen.getByRole('button', { name: 'Signing in…' })).toBeDisabled();
  });

  it('reveals success only in the success phase and completes the transition once', async () => {
    const onComplete = vi.fn();
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({
      x: 32,
      y: 40,
      left: 32,
      top: 40,
      right: 232,
      bottom: 88,
      width: 200,
      height: 48,
      toJSON: () => ({}),
    } as DOMRect);

    const { rerender } = render(
      <SuccessExpandingButton
        phase="idle"
        loadingLabel="Signing in…"
        successTitle="Welcome back!"
        successMessage="You’re signed in."
        onSuccessTransitionComplete={onComplete}
      >
        Continue
      </SuccessExpandingButton>,
    );

    expect(screen.queryByText('You’re signed in.')).not.toBeInTheDocument();
    rerender(
      <SuccessExpandingButton
        phase="success"
        loadingLabel="Signing in…"
        successTitle="Welcome back!"
        successMessage="You’re signed in."
        onSuccessTransitionComplete={onComplete}
      >
        Continue
      </SuccessExpandingButton>,
    );

    expect(await screen.findByRole('status')).toHaveTextContent('Welcome back!');
    expect(screen.getByRole('status')).toHaveTextContent('You’re signed in.');
    await waitFor(() => expect(onComplete).toHaveBeenCalledTimes(1), { timeout: 1800 });
  });

  it('can skip the takeover animation and still finish successfully', async () => {
    const onComplete = vi.fn();
    render(
      <SuccessExpandingButton
        phase="success"
        animationEnabled={false}
        loadingLabel="Signing in…"
        successTitle="Done"
        onSuccessTransitionComplete={onComplete}
      >
        Continue
      </SuccessExpandingButton>,
    );

    expect(screen.getByRole('button')).toHaveTextContent('Done');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    await waitFor(() => expect(onComplete).toHaveBeenCalledTimes(1));
  });
});
