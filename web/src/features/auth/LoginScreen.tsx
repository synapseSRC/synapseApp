import React, { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { AlertCircle, ArrowLeft, ArrowRight, CheckCircle2, Eye, EyeOff, Loader2, LockKeyhole, Mail, ShieldCheck, Sparkles, UserRound, X } from 'lucide-react';
import { useAuthStore } from './useAuthStore';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { WelcomeScreen } from './WelcomeScreen';
import { HomeScreen } from '@/features/home/HomeScreen';
import { CreatePostScreen } from '@/features/create-post/CreatePostScreen';

type AuthMode = 'welcome' | 'login' | 'signup' | 'reset' | 'recovery';

export const LoginScreen: React.FC = () => {
  const [mode, setMode] = useState<AuthMode>(() => {
    const isRecoveryLink = new URLSearchParams(window.location.search).get('mode') === 'recovery' || window.location.hash.includes('type=recovery');
    return isRecoveryLink ? 'recovery' : 'welcome';
  });
  const [activeAppScreen, setActiveAppScreen] = useState<'home' | 'create-post'>('home');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [username, setUsername] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [actionPhase, setActionPhase] = useState<'idle' | 'loading'>('idle');
  const [authTransitionPending, setAuthTransitionPending] = useState(false);
  const actionLockRef = useRef(false);
  const { user, isAuthenticated, signInWithGoogle, login, signUp, requestPasswordReset, updatePassword, logout, clearMessages, isLoading, loadingAction, error, notice } = useAuthStore();
  const isSignup = mode === 'signup';
  const isReset = mode === 'reset';
  const isRecovery = mode === 'recovery';
  const loadingLabel = loadingAction === 'session' ? 'Checking session…'
    : loadingAction === 'login' ? 'Signing in…'
      : loadingAction === 'signup' ? 'Creating account…'
        : loadingAction === 'reset' ? 'Sending link…'
          : loadingAction === 'recovery' ? 'Updating password…'
            : loadingAction === 'logout' ? 'Signing out…' : 'Please wait…';
  const prefersReducedMotion = useReducedMotion();

  useEffect(() => {
    if (!error && !notice) return;
    const timeout = window.setTimeout(() => clearMessages(), error ? 7000 : 5000);
    return () => window.clearTimeout(timeout);
  }, [error, notice, clearMessages]);

  const changeMode = (next: AuthMode) => { clearMessages(); setMode(next); };
  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (actionLockRef.current || actionPhase !== 'idle') return;

    actionLockRef.current = true;
    const submittedMode = mode;
    setActionPhase('loading');
    if (submittedMode !== 'reset') setAuthTransitionPending(true);

    let succeeded = false;
    try {
      if (submittedMode === 'login') succeeded = await login(email, password);
      else if (submittedMode === 'signup') succeeded = await signUp(email, password, username);
      else if (submittedMode === 'recovery') succeeded = await updatePassword(password);
      else succeeded = await requestPasswordReset(email);
    } catch (error) {
      useAuthStore.setState({
        error: error instanceof Error ? error.message : 'The request failed. Please try again.',
        isLoading: false,
        loadingAction: null,
      });
    }

    const authState = useAuthStore.getState();
    if (succeeded) {
      if (submittedMode === 'signup' && !authState.isAuthenticated) setMode('login');
      if (submittedMode === 'recovery') setMode('login');
    }

    setAuthTransitionPending(false);
    setActionPhase('idle');
    actionLockRef.current = false;
  };
  const title = isSignup ? 'Create your account' : isRecovery ? 'Choose a new password' : isReset ? 'Reset your password' : 'Sign in to your account';
  const subtitle = isSignup ? 'A good place for your people starts here.' : isRecovery ? 'Set a new password to finish recovering your account.' : isReset ? 'We’ll email you a secure link to get back in.' : 'Continue to your Synapse community.';
  const handleLogout = () => { setActiveAppScreen('home'); void logout(); };

  if (isAuthenticated && user && !isRecovery && !authTransitionPending) {
    return activeAppScreen === 'create-post'
      ? <CreatePostScreen user={user} onClose={() => setActiveAppScreen('home')} onPostCreated={() => setActiveAppScreen('home')} />
      : <HomeScreen user={user} onLogout={handleLogout} onCreatePost={() => setActiveAppScreen('create-post')} />;
  }

  return (
    <div className={mode === 'welcome' && !isAuthenticated ? 'min-h-screen bg-white text-zinc-950' : 'min-h-screen bg-[#f3f6fb] p-0 text-slate-900 sm:p-4 lg:p-7'}>
      <AnimatePresence>
        {(error || notice) && (
          <motion.div
            key={error || notice || 'auth-toast'}
            initial={prefersReducedMotion ? false : { opacity: 0, y: -10, scale: 0.985 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={prefersReducedMotion ? { opacity: 0 } : { opacity: 0, y: -8, scale: 0.99 }}
            transition={prefersReducedMotion ? { duration: 0.12 } : { type: 'tween', duration: 0.32, ease: 'easeOut' }}
            className="pointer-events-none fixed inset-x-0 top-5 z-[100] flex justify-center px-5 sm:top-7"
          >
            <div role={error ? 'alert' : 'status'} aria-live={error ? 'assertive' : 'polite'} className="pointer-events-auto flex w-full max-w-xl items-start gap-4 rounded-2xl border border-zinc-200 bg-white px-5 py-4 shadow-[0_14px_44px_-20px_rgba(0,0,0,0.36)]">
              <span className="mt-0.5 grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-zinc-100 text-zinc-950">
                {error ? <AlertCircle className="h-5 w-5" /> : <CheckCircle2 className="h-5 w-5" />}
              </span>
              <span className="min-w-0 flex-1 pt-0.5">
                <span className="block text-sm font-semibold tracking-tight text-zinc-950">{error ? 'Unable to continue' : 'All set'}</span>
                <span className="mt-1 block text-sm leading-5 text-zinc-600">{error || notice}</span>
              </span>
              <button type="button" onClick={clearMessages} aria-label="Dismiss notification" className="rounded-lg p-2 text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-black focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400">
                <X className="h-4 w-4" />
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
      <AnimatePresence mode="wait" initial={false}>
        {mode === 'welcome' && !isAuthenticated ? (
          <motion.div
            key="welcome-screen"
            initial={prefersReducedMotion ? false : { opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={prefersReducedMotion ? { opacity: 0 } : { opacity: 0, y: -8 }}
            transition={prefersReducedMotion ? { duration: 0.1 } : { type: 'tween', duration: 0.24, ease: 'easeOut' }}
          >
            <WelcomeScreen
              onGoogleSignIn={() => { void signInWithGoogle(); }}
              onContinue={() => changeMode('login')}
              googleLoading={isLoading && loadingAction === 'google'}
            />
          </motion.div>
        ) : (
          <motion.div
            key="auth-screen"
            initial={prefersReducedMotion ? false : { opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={prefersReducedMotion ? { opacity: 0 } : { opacity: 0, y: -8 }}
            transition={prefersReducedMotion ? { duration: 0.1 } : { type: 'tween', duration: 0.24, ease: 'easeOut' }}
            className="mx-auto grid min-h-screen max-w-7xl overflow-hidden bg-white shadow-2xl shadow-slate-900/10 sm:min-h-[calc(100vh-2rem)] sm:rounded-[2rem] lg:min-h-[calc(100vh-3.5rem)] lg:grid-cols-[1.02fr_0.98fr]"
          >
        <aside className="relative hidden overflow-hidden bg-[#101a31] px-10 py-10 text-white lg:flex lg:flex-col lg:justify-between xl:px-14 xl:py-12">
          <div className="pointer-events-none absolute -right-40 -top-28 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-48 -left-36 h-[30rem] w-[30rem] rounded-full bg-violet-500/20 blur-3xl" />
          <div className="relative z-10 max-w-xl py-12">
            <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.06] px-3 py-1.5 text-xs font-medium text-sky-100"><Sparkles className="h-3.5 w-3.5 text-sky-300" /> A calmer kind of social</div>
            <h1 className="max-w-lg text-4xl font-semibold leading-[1.12] tracking-tight xl:text-5xl">Your people.<span className="mt-1 block bg-gradient-to-r from-sky-200 via-blue-300 to-violet-300 bg-clip-text text-transparent">Your corner of the web.</span></h1>
            <p className="mt-5 max-w-md text-base leading-7 text-slate-300">Share what matters, keep up with your community, and make room for better conversations.</p>
            <div className="relative mt-10 flex h-56 max-w-md items-center justify-center">
              <div className="absolute h-52 w-52 rounded-full border border-white/10" /><div className="absolute h-40 w-40 rounded-full border border-white/10" /><div className="absolute h-28 w-28 rounded-full bg-blue-400/10 blur-xl" />
              <div className="relative grid h-24 w-24 place-items-center rounded-[2rem] border border-white/15 bg-gradient-to-br from-blue-400/90 to-indigo-600 shadow-2xl shadow-blue-950/50"><Sparkles className="h-10 w-10 text-white" strokeWidth={1.5} /></div>
              <div className="absolute left-0 top-6 rounded-2xl border border-white/10 bg-white/[0.08] px-4 py-3 backdrop-blur"><p className="text-[10px] uppercase tracking-[0.18em] text-slate-400">Stay close</p><p className="mt-1 text-sm font-medium">Find your people</p></div>
              <div className="absolute bottom-4 right-0 rounded-2xl border border-white/10 bg-white/[0.08] px-4 py-3 backdrop-blur"><p className="text-[10px] uppercase tracking-[0.18em] text-slate-400">Make it yours</p><p className="mt-1 text-sm font-medium">Share at your pace</p></div>
            </div>
          </div>
          <div className="relative z-10 flex items-center gap-2 text-xs text-slate-400"><ShieldCheck className="h-4 w-4 text-sky-300" /> Your account is protected by Supabase Auth</div>
        </aside>

        <main className="flex min-h-screen flex-col justify-center px-6 py-9 sm:px-12 lg:min-h-0 lg:px-12 xl:px-20">
          <div className="mx-auto w-full max-w-md">
            {isAuthenticated && user && !isRecovery && !authTransitionPending ? (
              <HomeScreen user={user} onLogout={handleLogout} onCreatePost={() => setActiveAppScreen('create-post')} />
            ) : (
              <AnimatePresence mode="wait" initial={false}>
                <motion.div
                  key={mode}
                  initial={prefersReducedMotion ? false : { opacity: 0, x: 10 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={prefersReducedMotion ? { opacity: 0 } : { opacity: 0, x: -8 }}
                  transition={prefersReducedMotion ? { duration: 0.1 } : { type: 'tween', duration: 0.24, ease: 'easeOut' }}
                >
              <div className="mb-8"><h2 className="text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">{title}</h2><p className="mt-3 text-sm leading-6 text-slate-500">{subtitle}</p></div>
              <form onSubmit={handleSubmit} className="space-y-5">
                {isSignup && <div className="space-y-2"><label htmlFor="username" className="text-sm font-medium text-slate-700">Username</label><Input id="username" type="text" autoComplete="username" placeholder="Choose a username" value={username} onChange={(event) => setUsername(event.target.value)} icon={<UserRound className="h-4 w-4" />} className="h-12 rounded-xl border-slate-200 bg-slate-50 pl-10 focus:bg-white" minLength={3} maxLength={30} pattern="[A-Za-z0-9_.-]+" title="Use letters, numbers, dots, underscores, or hyphens." required /></div>}
                {!isRecovery && <div className="space-y-2"><label htmlFor="email" className="text-sm font-medium text-slate-700">Email address</label><Input id="email" type="email" autoComplete="email" placeholder="you@example.com" value={email} onChange={(event) => setEmail(event.target.value)} icon={<Mail className="h-4 w-4" />} className="h-12 rounded-xl border-slate-200 bg-slate-50 pl-10 focus:bg-white" required /></div>}
                {!isReset && <div className="space-y-2"><div className="flex items-center justify-between gap-3"><label htmlFor="password" className="text-sm font-medium text-slate-700">{isRecovery ? 'New password' : 'Password'}</label>{!isSignup && !isRecovery && <button type="button" disabled={actionPhase !== 'idle'} onClick={() => changeMode('reset')} className="text-xs font-semibold text-blue-700 hover:text-blue-900 hover:underline disabled:cursor-not-allowed disabled:opacity-50">Forgot password?</button>}</div><div className="relative"><Input id="password" type={showPassword ? 'text' : 'password'} autoComplete={isSignup || isRecovery ? 'new-password' : 'current-password'} placeholder={isSignup || isRecovery ? 'At least 6 characters' : 'Enter your password'} value={password} onChange={(event) => setPassword(event.target.value)} icon={<LockKeyhole className="h-4 w-4" />} className="h-12 rounded-xl border-slate-200 bg-slate-50 pl-10 pr-12 focus:bg-white" minLength={6} required /><button type="button" onClick={() => setShowPassword((visible) => !visible)} className="absolute right-3 top-1/2 -translate-y-1/2 rounded-md p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700" aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}</button></div>{isSignup && <p className="text-xs text-slate-500">Use at least 6 characters.</p>}</div>}
                <Button type="submit" disabled={isLoading || actionPhase === 'loading'} className="h-12 w-full rounded-xl bg-blue-700 text-sm font-semibold shadow-lg shadow-blue-700/15 hover:bg-blue-800">
                  {isLoading || actionPhase === 'loading' ? (
                    <span className="flex items-center justify-center gap-2"><Loader2 className="h-4 w-4 animate-spin" />{loadingLabel}</span>
                  ) : (
                    <span className="flex items-center justify-center gap-2"><span>{isReset ? 'Send reset link' : isRecovery ? 'Update password' : isSignup ? 'Create account' : 'Sign in'}</span><ArrowRight className="h-4 w-4" /></span>
                  )}
                </Button>
              </form>
              <div className="mt-7 text-center text-sm text-slate-500">{isReset || isRecovery ? <button type="button" disabled={actionPhase !== 'idle'} onClick={() => changeMode('login')} className="inline-flex items-center gap-2 font-semibold text-blue-700 hover:text-blue-900 disabled:cursor-not-allowed disabled:opacity-50"><ArrowLeft className="h-4 w-4" /> Back to sign in</button> : isSignup ? <span>Already have an account? <button type="button" disabled={actionPhase !== 'idle'} onClick={() => changeMode('login')} className="font-semibold text-blue-700 hover:text-blue-900 disabled:cursor-not-allowed disabled:opacity-50">Sign in</button></span> : <span>New to Synapse? <button type="button" disabled={actionPhase !== 'idle'} onClick={() => changeMode('signup')} className="font-semibold text-blue-700 hover:text-blue-900 disabled:cursor-not-allowed disabled:opacity-50">Create an account</button></span>}</div>
              <div className="mt-9 flex items-start gap-3 border-t border-slate-100 pt-5 text-xs leading-5 text-slate-500"><ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" /><p>Authentication is handled securely by Supabase. Your password is never stored by Synapse.</p></div>
                </motion.div>
              </AnimatePresence>
            )}
          </div>
        </main>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
