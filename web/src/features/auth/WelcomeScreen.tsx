import React from 'react';
import { Loader2, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/Button';

interface WelcomeScreenProps {
  onGoogleSignIn: () => void;
  onContinue: () => void;
  googleLoading: boolean;
}

const GoogleMark: React.FC = () => (
  <svg aria-hidden="true" viewBox="0 0 48 48" className="h-6 w-6">
    <path fill="#4285F4" d="M43.6 24.5c0-1.4-.1-2.7-.4-4H24v7.6h11c-.5 2.4-1.9 4.4-4.1 5.8l6.6 5.1c3.9-3.6 6.1-8.8 6.1-14.5z" />
    <path fill="#34A853" d="M24 44c5.5 0 10.1-1.8 13.5-4.9l-6.6-5.1c-1.8 1.2-4.1 2-6.9 2-5.3 0-9.8-3.6-11.4-8.4l-6.8 5.2C9.2 39.4 16 44 24 44z" />
    <path fill="#FBBC05" d="M12.6 27.6c-.4-1.1-.7-2.3-.7-3.6s.2-2.5.7-3.6l-6.8-5.2C4.7 18 4 20.9 4 24s.7 6 1.8 8.8l6.8-5.2z" />
    <path fill="#EA4335" d="M24 9.6c3 0 5.7 1 7.8 3.1l5.8-5.8C34.1 3.8 29.5 2 24 2 16 2 9.2 6.6 5.8 15.2l6.8 5.2C14.2 13.2 18.7 9.6 24 9.6z" />
  </svg>
);

export const WelcomeScreen: React.FC<WelcomeScreenProps> = ({ onGoogleSignIn, onContinue, googleLoading }) => (
  <main className="flex min-h-[100svh] w-full flex-col items-center bg-white px-6 text-zinc-950">
    <section className="mt-[22svh] flex w-full max-w-2xl flex-col items-center text-center">
      <span className="grid h-16 w-16 place-items-center text-zinc-950" aria-hidden="true">
        <Sparkles className="h-14 w-14" strokeWidth={1.5} />
      </span>
      <h1 className="mt-8 max-w-[18rem] text-4xl font-semibold leading-[1.12] tracking-[-0.045em] sm:max-w-none sm:text-5xl">Welcome to Synapse</h1>
      <p className="mt-5 max-w-xl text-lg leading-7 text-zinc-800 sm:text-xl sm:leading-8">
        Connect with your community, share what matters, and discover new voices. Log in for the full experience.
      </p>
    </section>

    <footer className="mt-auto w-full max-w-xl pb-[max(1.5rem,env(safe-area-inset-bottom))] pt-12">
      <p className="mb-8 px-2 text-center text-sm leading-6 text-zinc-500">
        By continuing, you agree to Synapse’s <span className="text-zinc-800 underline underline-offset-2">Terms</span> and have read our <span className="text-zinc-800 underline underline-offset-2">Privacy Policy</span>.
      </p>
      <div className="space-y-3">
        <Button type="button" onClick={onGoogleSignIn} disabled={googleLoading} className="h-14 w-full rounded-full bg-[#0d0d0d] text-base font-semibold text-white shadow-none hover:bg-black">
          {googleLoading ? <span className="flex items-center justify-center gap-3"><Loader2 className="h-5 w-5 animate-spin" />Connecting with Google…</span> : <span className="flex items-center justify-center gap-3"><GoogleMark />Continue with Google</span>}
        </Button>
        <Button type="button" variant="outline" onClick={onContinue} className="h-14 w-full rounded-full border border-zinc-300 bg-white text-base font-semibold text-zinc-950 shadow-none hover:bg-zinc-50 hover:text-black">
          Log in or sign up
        </Button>
      </div>
    </footer>
  </main>
);
