/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        base: '#f1f5f9',
        surface: '#ffffff',
        card: '#f8fafc',
        subtle: '#e2e8f0',
        'text-primary': '#0f172a',
        'text-muted': '#64748b',
        accent: '#2563eb',
        'risk-low': '#16a34a',
        'risk-medium': '#d97706',
        'risk-high': '#dc2626',
      },
    },
  },
  plugins: [],
};
