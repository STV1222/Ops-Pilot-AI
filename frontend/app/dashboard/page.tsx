"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { authFetch } from "../../lib/api";

type Workflow = {
  id: string;
  workflowType: string;
  status: string;
  startedAt: string;
};

export default function DashboardPage() {
  const [rows, setRows] = useState<Workflow[]>([]);

  useEffect(() => {
    authFetch("/api/v1/workflows").then((r) => r.json()).then(setRows).catch(() => setRows([]));
  }, []);

  return (
    <main className="p-8 max-w-5xl mx-auto space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-semibold">Dashboard</h1>
        <Link href="/workflows/upload" className="bg-emerald-600 px-3 py-2 rounded">New Workflow</Link>
      </div>
      <div className="border border-zinc-800 rounded">
        {rows.length === 0 && (
          <div className="p-4 text-zinc-400">No workflows yet. Upload quotation documents to get started.</div>
        )}
        {rows.map((w) => (
          <Link key={w.id} href={`/workflows/${w.id}`} className="p-4 border-b border-zinc-800 block hover:bg-zinc-900">
            <div className="font-medium">{w.workflowType}</div>
            <div className="text-sm text-zinc-400">{w.status} — {new Date(w.startedAt).toLocaleString()}</div>
          </Link>
        ))}
      </div>
    </main>
  );
}
