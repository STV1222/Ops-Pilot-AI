const BASE = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

function getToken(): string {
  if (typeof window === "undefined") return "";
  return localStorage.getItem("token") || "";
}

export function authFetch(path: string, init?: RequestInit): Promise<Response> {
  return fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      ...(init?.headers || {}),
      Authorization: `Bearer ${getToken()}`,
    },
  });
}
