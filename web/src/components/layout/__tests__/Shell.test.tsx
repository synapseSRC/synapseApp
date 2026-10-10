import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Shell } from '../Shell';

describe('Shell layout component', () => {
  it('renders children and navigation tabs', () => {
    const handleTabChange = vi.fn();
    const handleToggleTheme = vi.fn();

    render(
      <Shell
        activeTab="feed"
        onTabChange={handleTabChange}
        isDarkMode={false}
        onToggleTheme={handleToggleTheme}
        userName="Test User"
      >
        <div>Test Shell Children Content</div>
      </Shell>
    );

    expect(screen.getAllByText('Test Shell Children Content').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Synapse').length).toBeGreaterThan(0);
  });
});
