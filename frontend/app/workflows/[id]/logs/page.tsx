"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import Link from "next/link";
import { authFetch } from "../../../../lib/api";

export default function LogsPage() {
  const { id } = useParams<{ id: string }>();
  const [logs, setLogs] = useState<any[]>([]);

  useEffect(() => {
    authFetch(`/api/v1/workflows/${id}/logs`).then((r) => r.json()).then(setLogs).catch(() => setLogs([]));
  }, [id]);

  const stageColor = (status: string) => status === "ok" ? "text-emerald-400" : "text-red-400";

  return (
    <main className="p-8 max-w-4xl mx-auto space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-semibold">Execution Logs</h1>
        <Link href={`/workflows/${id}`} className="px-3 py-2 bg-zinc-800 rounded text-sm">← Back</Link>
      </div>
      <div className="space-y-2">
        {logs.map((log, i) => (
          <div key={i} className="border border-zinc-800 rounded p-3 bg-zinc-900 flex justify-between">
            <div className="font-medium capitalize">{log.stage}</div>
            <div className={`text-sm font-semibold ${stageColor(log.status)}`}>{log.status}</div>
          </div>
        ))}
      </div>
    </main>
  );
}
