import React from 'react';
import { MainTab } from '@/types';
import { MobileShell } from './MobileShell';
import { TabletShell } from './TabletShell';
import { DesktopShell } from './DesktopShell';

interface ShellProps {
  activeTab: MainTab;
  onTabChange: (tab: MainTab) => void;
  onOpenCreatePost?: () => void;
  onOpenSettings?: () => void;
  onLogout?: () => void;
  userAvatar?: string;
  userName?: string;
  userHandle?: string;
  isDarkMode: boolean;
  onToggleTheme: () => void;
  children: React.ReactNode;
}

export const Shell: React.FC<ShellProps> = (props) => {
  return (
    <>
      <MobileShell {...props} />
      <TabletShell {...props} />
      <DesktopShell {...props} />
    </>
  );
};
