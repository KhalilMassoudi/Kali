import { Component, input } from '@angular/core';

/** Chatbot glyph (PrimeIcons has no robot icon). Inherits the surrounding text colour. */
@Component({
  selector: 'app-bot-icon',
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M12 3v3" />
      <circle cx="12" cy="2.5" r="1" fill="currentColor" stroke="none" />
      <rect x="4" y="6" width="16" height="12" rx="3.5" />
      <circle cx="9" cy="12" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="15" cy="12" r="1.4" fill="currentColor" stroke="none" />
      <path d="M9.5 15.2h5" />
      <path d="M2 11v3M22 11v3" />
      <path d="M9 18l-1.5 3M15 18l1.5 3" />
    </svg>
  `,
  styles: [':host { display: inline-flex; line-height: 0; }'],
})
export class BotIcon {
  readonly size = input<number | string>(16);
}
