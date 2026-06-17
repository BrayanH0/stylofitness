export const AUTH_KEYS = ['token', 'dni', 'nombre', 'apellido', 'email', 'rol', 'usuario'];

export const SESSION_KEYS = ['datosCliente', 'membresiaSeleccionada', 'ultimoPago', 'tempPassword', 'tempToken'];

export const TOKEN_STORAGE_KEYS = [...AUTH_KEYS, ...SESSION_KEYS];

export function isTokenValid(token: string | null): boolean {
  if (!token) return false;
  const parts = token.split('.');
  if (parts.length !== 3 || !parts[0] || !parts[1] || !parts[2]) {
    return false;
  }
  try {
    const header = JSON.parse(atob(parts[0]));
    if (!header || typeof header !== 'object') {
      return false;
    }
    const payload = JSON.parse(atob(parts[1]));
    const expirationDate = new Date(payload.exp * 1000);
    return expirationDate > new Date();
  } catch {
    return false;
  }
}

export function clearAuthStorage(): void {
  TOKEN_STORAGE_KEYS.forEach(key => localStorage.removeItem(key));
}

export function clearSessionData(): void {
  SESSION_KEYS.forEach(key => localStorage.removeItem(key));
}
