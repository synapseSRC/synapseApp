import { test, expect } from '@playwright/test';

const LINKS = [
  '/guides/setup/',
  '/guides/architecture/',
  '/guides/modules/',
  '/guides/state-management/',
  '/guides/contributing/',
  '/reference/codestyle/',
  '/reference/testing/',
  '/reference/roadmap/',
  '/reference/changelog/',
  '/reference/release/',
  '/reference/funding/',
  '/reference/security/',
  '/reference/privacy/',
  '/reference/terms/',
];

test.describe('Navigation Tests', () => {
  test('should navigate to homepage successfully', async ({ page }) => {
    const response = await page.goto('/');
    expect(response?.status()).toBe(200);
    await expect(page).toHaveTitle(/Synapse Docs/);
  });

  for (const link of LINKS) {
    test(`should navigate to ${link} successfully`, async ({ page }) => {
      const response = await page.goto(link);
      expect(response?.status()).toBe(200);

      // Simple check to ensure page isn't empty or showing a generic 404
      const heading = page.locator('h1');
      await expect(heading).toBeVisible();
    });
  }
});
