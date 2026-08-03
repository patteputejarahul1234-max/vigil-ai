/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: {
          DEFAULT: '#0D0F14',
          surface: '#161923',
          border: '#262B38',
        },
        text: {
          primary: '#ECEEF3',
          muted: '#8992A6',
        },
        signal: {
          teal: '#14B8A6',
          amber: '#FBBF24',
          danger: '#F87171',
        },
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'sans-serif'],
        body: ['"Inter"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      boxShadow: {
        ring: '0 0 0 1px rgba(20, 184, 166, 0.25), 0 0 24px rgba(20, 184, 166, 0.15)',
      },
    },
  },
  plugins: [],
}
