/**
 * Traps Tab/Shift+Tab focus within a container element.
 * Call this from a (keydown) handler on the modal container.
 */
export function trapFocus(event: KeyboardEvent, container: HTMLElement): void {
  if (event.key === 'Escape') {
    return;
  }

  if (event.key !== 'Tab') return;

  const focusable = container.querySelectorAll<HTMLElement>(
    'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
  );

  if (focusable.length === 0) return;

  const first = focusable[0];
  const last = focusable[focusable.length - 1];

  if (event.shiftKey) {
    if (document.activeElement === first) {
      event.preventDefault();
      last.focus();
    }
  } else {
    if (document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }
}