"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

export default function LoginPage() {
  const [email, setEmail] = useState("admin@opspilot.local");
  const router = useRouter();

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const base = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
    const res = await fetch(`${base}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password: "password" })
    });
    const data = await res.json();
    localStorage.setItem("token", data.token);
    router.push("/dashboard");
  }

  return (
    <main className="max-w-md mx-auto p-8">
      <h1 className="text-2xl font-semibold mb-6">Login</h1>
      <form onSubmit={onSubmit} className="space-y-3">
        <input className="w-full px-3 py-2 bg-zinc-900 border border-zinc-700 rounded" value={email} onChange={(e) => setEmail(e.target.value)} />
        <button className="w-full bg-emerald-600 px-3 py-2 rounded">Continue</button>
      </form>
    </main>
  );
}
