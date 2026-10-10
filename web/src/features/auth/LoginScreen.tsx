import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Lock, Mail, ArrowRight, Sparkles } from 'lucide-react';
import { useAuthStore } from './useAuthStore';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/Card';
import { fadeIn, slideInUp } from '@/lib/animations';

export const LoginScreen: React.FC = () => {
  const [email, setEmail] = useState('demo@synapse.social');
  const [password, setPassword] = useState('password123');
  const { login, isLoading, error } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await login(email, password);
  };

  const handleDemoLogin = async () => {
    await login('demo@synapse.social', 'password123');
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-background via-card to-background relative overflow-hidden">
      {/* Background Decorative Blur Orbs */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />

      <motion.div variants={fadeIn} initial="initial" animate="animate" className="w-full max-w-md relative z-10">
        <Card className="border-border/80 backdrop-blur shadow-xl">
          <CardHeader className="text-center space-y-2 pb-4">
            <div className="mx-auto w-14 h-14 rounded-2xl bg-primary-500 flex items-center justify-center text-white font-bold text-2xl shadow-lg mb-2">
              S
            </div>
            <CardTitle className="text-2xl font-bold tracking-tight">Welcome to Synapse</CardTitle>
            <CardDescription className="text-sm">
              Connect, share, and experience the modern social web.
            </CardDescription>
          </CardHeader>

          <CardContent>
            {error && (
              <motion.div variants={slideInUp} initial="initial" animate="animate" className="mb-4 p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs font-medium text-center">
                {error}
              </motion.div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-muted-foreground">Email address</label>
                <Input
                  type="email"
                  placeholder="name@domain.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  icon={<Mail className="w-4 h-4" />}
                  required
                />
              </div>

              <div className="space-y-1.5">
                <div className="flex justify-between items-center">
                  <label className="text-xs font-semibold text-muted-foreground">Password</label>
                  <a href="#forgot" className="text-xs text-primary-500 hover:underline">
                    Forgot?
                  </a>
                </div>
                <Input
                  type="password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  icon={<Lock className="w-4 h-4" />}
                  required
                />
              </div>

              <Button type="submit" disabled={isLoading} className="w-full py-5 rounded-xl font-semibold shadow-md">
                {isLoading ? (
                  'Signing in...'
                ) : (
                  <span className="flex items-center justify-center space-x-2">
                    <span>Sign In</span>
                    <ArrowRight className="w-4 h-4" />
                  </span>
                )}
              </Button>
            </form>
          </CardContent>

          <CardFooter className="flex flex-col space-y-3 pt-2">
            <div className="relative w-full flex items-center justify-center my-2">
              <div className="border-t w-full" />
              <span className="bg-card px-2 text-[10px] text-muted-foreground uppercase font-semibold absolute">
                Or
              </span>
            </div>

            <Button
              variant="outline"
              onClick={handleDemoLogin}
              disabled={isLoading}
              className="w-full rounded-xl py-5 font-medium flex items-center justify-center space-x-2 border-primary-500/30 text-primary-500 hover:bg-primary-500/5"
            >
              <Sparkles className="w-4 h-4 text-amber-500" />
              <span>Explore as Demo User</span>
            </Button>
          </CardFooter>
        </Card>
      </motion.div>
    </div>
  );
};
