import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { CheckCircle2, Loader2 } from 'lucide-react';
import { Button, type ButtonProps } from './Button';

export type ExpandingActionPhase = 'idle' | 'loading' | 'success';

interface SuccessOrigin {
  clipPath: string;
  backgroundColor: string;
  color: string;
}

export interface SuccessExpandingButtonProps extends Omit<ButtonProps, 'children'> {
  phase: ExpandingActionPhase;
  busy?: boolean;
  loadingLabel: string;
  successTitle: string;
  successMessage?: string;
  animationEnabled?: boolean;
  onSuccessTransitionComplete?: () => void;
  children: React.ReactNode;
}

export const SuccessExpandingButton: React.FC<SuccessExpandingButtonProps> = ({
  phase,
  busy = false,
  loadingLabel,
  successTitle,
  successMessage,
  animationEnabled = true,
  onSuccessTransitionComplete,
  children,
  disabled,
  type = 'submit',
  ...buttonProps
}) => {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const completionFired = useRef(false);
  const completionCallback = useRef(onSuccessTransitionComplete);
  const [origin, setOrigin] = useState<SuccessOrigin | null>(null);
  const prefersReducedMotion = useReducedMotion();
  const shouldAnimate = animationEnabled && !prefersReducedMotion;

  completionCallback.current = onSuccessTransitionComplete;

  useLayoutEffect(() => {
    if (phase !== 'success' || !animationEnabled) {
      setOrigin(null);
      return;
    }

    const button = buttonRef.current;
    if (!button) return;

    const rect = button.getBoundingClientRect();
    const computed = window.getComputedStyle(button);
    const radius = Number.parseFloat(computed.borderTopLeftRadius) || 12;
    const right = Math.max(0, window.innerWidth - rect.right);
    const bottom = Math.max(0, window.innerHeight - rect.bottom);

    setOrigin({
      clipPath: `inset(${Math.max(0, rect.top)}px ${right}px ${bottom}px ${Math.max(0, rect.left)}px round ${radius}px)`,
      backgroundColor: computed.backgroundColor,
      color: computed.color,
    });
  }, [phase, animationEnabled]);

  useEffect(() => {
    if (phase !== 'success') {
      completionFired.current = false;
      return;
    }

    completionFired.current = false;
    const delay = !animationEnabled ? 0 : prefersReducedMotion ? 450 : 1050;
    const timeout = window.setTimeout(() => {
      if (completionFired.current) return;
      completionFired.current = true;
      completionCallback.current?.();
    }, delay);

    return () => window.clearTimeout(timeout);
  }, [phase, animationEnabled, prefersReducedMotion]);

  const isBusy = phase === 'loading' || busy;
  const isSuccessful = phase === 'success';

  return (
    <>
      <Button
        {...buttonProps}
        ref={buttonRef}
        type={type}
        disabled={disabled || isBusy || isSuccessful}
        aria-busy={isBusy || undefined}
      >
        {isBusy ? (
          <span className="flex items-center justify-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
            <span>{loadingLabel}</span>
          </span>
        ) : isSuccessful ? (
          <span className="flex items-center justify-center gap-2">
            <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
            <span>{successTitle}</span>
          </span>
        ) : children}
      </Button>

      <AnimatePresence>
        {isSuccessful && origin && animationEnabled && (
          <motion.div
            key="expanding-success-surface"
            role="status"
            aria-live="assertive"
            initial={shouldAnimate
              ? { clipPath: origin.clipPath }
              : { clipPath: 'inset(0px 0px 0px 0px round 0px)', opacity: 0 }}
            animate={{ clipPath: 'inset(0px 0px 0px 0px round 0px)', opacity: 1 }}
            transition={shouldAnimate
              ? { clipPath: { duration: 0.58, ease: 'easeInOut' }, opacity: { duration: 0.16 } }
              : { duration: 0.18 }}
            className="fixed inset-0 z-[200] flex items-center justify-center overflow-hidden"
            style={{ backgroundColor: origin.backgroundColor, color: origin.color }}
          >
            <motion.div
              initial={{ opacity: 0, y: 8, scale: 0.985 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              transition={{
                delay: shouldAnimate ? 0.38 : 0.02,
                duration: shouldAnimate ? 0.24 : 0.18,
                ease: 'easeOut',
              }}
              className="flex max-w-md flex-col items-center px-7 text-center"
            >
              <span className="mb-6 grid h-16 w-16 place-items-center rounded-full border border-current/20 bg-white/10">
                <CheckCircle2 className="h-8 w-8" strokeWidth={1.7} aria-hidden="true" />
              </span>
              <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">{successTitle}</h1>
              {successMessage && <p className="mt-3 text-base leading-7 opacity-85">{successMessage}</p>}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};
