/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        base: '#0a0f1e',
        surface: '#111827',
        card: '#1a2234',
        subtle: '#1e2a3a',
        'text-primary': '#e2e8f0',
        'text-muted': '#64748b',
        accent: '#3b82f6',
        'risk-low': '#22c55e',
        'risk-medium': '#f59e0b',
        'risk-high': '#ef4444',
      },
    },
  },
  plugins: [],
};
