import { definePreset } from '@primeng/themes';
import Aura from '@primeng/themes/aura';

// Dark surface ramp carried over from the validated React theme (WCAG-checked
// against the brand-bg/surface/surface2/border hierarchy: index.css in frontend-react-old).
export const AppPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{sky.50}',
      100: '{sky.100}',
      200: '{sky.200}',
      300: '{sky.300}',
      400: '{sky.400}',
      500: '{sky.500}',
      600: '{sky.600}',
      700: '{sky.700}',
      800: '{sky.800}',
      900: '{sky.900}',
      950: '{sky.950}',
    },
    colorScheme: {
      dark: {
        surface: {
          0: '#ffffff',
          50: '#e8ecf1',
          100: '#c7d0da',
          200: '#a3b1c2',
          300: '#7a8ca3',
          400: '#5c7089',
          500: '#33455a',
          600: '#1c2634',
          700: '#131b26',
          800: '#0d1520',
          900: '#080d14',
          950: '#05090f',
        },
      },
    },
  },
});
