import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Card, CardHeader, CardTitle, CardContent } from '../Card';

describe('Card component', () => {
  it('renders card with header title and content', () => {
    render(
      <Card>
        <CardHeader>
          <CardTitle>Card Title Header</CardTitle>
        </CardHeader>
        <CardContent>
          <p>Card Content Text</p>
        </CardContent>
      </Card>
    );

    expect(screen.getByText('Card Title Header')).toBeInTheDocument();
    expect(screen.getByText('Card Content Text')).toBeInTheDocument();
  });
});
