import React from 'react';
import { Sun, Moon, Shield, Bell, LogOut, UserCheck } from 'lucide-react';
import { UserProfile } from '@/types';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/Dialog';
import { Switch } from '@/components/ui/Switch';
import { Button } from '@/components/ui/Button';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { useThemeStore } from '@/app/useThemeStore';

interface SettingsScreenProps {
  user: UserProfile;
  isOpen: boolean;
  onClose: () => void;
  onLogout?: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  user,
  isOpen,
  onClose,
  onLogout,
}) => {
  const { isDarkMode, toggleTheme } = useThemeStore();

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md p-6">
        <DialogHeader>
          <DialogTitle className="text-lg font-bold">Settings Hub</DialogTitle>
        </DialogHeader>

        <div className="space-y-6 pt-2">
          {/* User Account Card */}
          <div className="flex items-center space-x-3 p-3.5 rounded-2xl bg-muted/50 border">
            <Avatar size="lg">
              <AvatarImage src={user.avatarUrl} alt={user.fullName} />
              <AvatarFallback>{user.fullName.slice(0, 2)}</AvatarFallback>
            </Avatar>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-bold truncate">{user.fullName}</p>
              <p className="text-xs text-muted-foreground truncate">@{user.username}</p>
            </div>
          </div>

          {/* Preferences Category List */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">
              Preferences
            </h4>

            {/* Dark Mode Toggle */}
            <div className="flex items-center justify-between p-3.5 rounded-2xl border bg-card">
              <div className="flex items-center space-x-3">
                {isDarkMode ? <Moon className="w-5 h-5 text-amber-400" /> : <Sun className="w-5 h-5 text-primary-500" />}
                <div>
                  <p className="text-sm font-semibold">Dark Mode</p>
                  <p className="text-xs text-muted-foreground">Adjust visual appearance</p>
                </div>
              </div>
              <Switch checked={isDarkMode} onCheckedChange={toggleTheme} />
            </div>

            {/* Notifications Toggle */}
            <div className="flex items-center justify-between p-3.5 rounded-2xl border bg-card">
              <div className="flex items-center space-x-3">
                <Bell className="w-5 h-5 text-blue-500" />
                <div>
                  <p className="text-sm font-semibold">Push Notifications</p>
                  <p className="text-xs text-muted-foreground">Alerts for likes & messages</p>
                </div>
              </div>
              <Switch defaultChecked />
            </div>

            {/* Privacy */}
            <div className="flex items-center justify-between p-3.5 rounded-2xl border bg-card">
              <div className="flex items-center space-x-3">
                <Shield className="w-5 h-5 text-emerald-500" />
                <div>
                  <p className="text-sm font-semibold">Private Account</p>
                  <p className="text-xs text-muted-foreground">Only approved followers see posts</p>
                </div>
              </div>
              <Switch />
            </div>
          </div>

          {/* Account Actions */}
          <div className="pt-2 border-t space-y-2">
            <Button variant="outline" className="w-full justify-start rounded-xl gap-2 text-xs">
              <UserCheck className="w-4 h-4 text-primary-500" />
              <span>Edit Account Information</span>
            </Button>

            {onLogout && (
              <Button
                variant="destructive"
                onClick={() => {
                  onClose();
                  onLogout();
                }}
                className="w-full justify-start rounded-xl gap-2 text-xs"
              >
                <LogOut className="w-4 h-4" />
                <span>Sign Out of Synapse</span>
              </Button>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};
