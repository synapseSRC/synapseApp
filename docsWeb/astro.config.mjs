// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

// https://astro.build/config
export default defineConfig({
	integrations: [
		starlight({
			title: 'Synapse Docs',
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/studioasinc/synapse' }],
			sidebar: [
				{
					label: 'Getting Started',
					items: [
						{ label: 'Setup Guide', slug: 'guides/setup' },
					],
				},
				{
					label: 'Architecture',
					items: [
						{ label: 'Overview', slug: 'guides/architecture' },
						{ label: 'Modules & Structure', slug: 'guides/modules' },
						{ label: 'State Management', slug: 'guides/state-management' },
					],
				},
				{
					label: 'Development',
					items: [
						{ label: 'Contributing', slug: 'guides/contributing' },
						{ label: 'Code Style', slug: 'reference/codestyle' },
						{ label: 'Testing Strategy', slug: 'reference/testing' },
					],
				},
				{
					label: 'Project',
					items: [
						{ label: 'Roadmap', slug: 'reference/roadmap' },
						{ label: 'Changelog', slug: 'reference/changelog' },
						{ label: 'Release Process', slug: 'reference/release' },
						{ label: 'Funding', slug: 'reference/funding' },
					],
				},
				{
					label: 'Legal',
					items: [
						{ label: 'Security Policy', slug: 'reference/security' },
						{ label: 'Privacy Policy', slug: 'reference/privacy' },
						{ label: 'Terms of Service', slug: 'reference/terms' },
					],
				},
			],
		}),
	],
});
