"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { authFetch } from "../../../lib/api";

export default function WorkflowDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [item, setItem] = useState<any>(null);
  const [acting, setActing] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function poll() {
      try {
        const data = await authFetch(`/api/v1/workflows/${id}`).then((r) => r.json());
        if (!cancelled) {
          setItem(data);
          if (data.status === "RUNNING") {
            setTimeout(poll, 3000);
          }
        }
      } catch {
        if (!cancelled) setItem(null);
      }
    }

    poll();
    return () => { cancelled = true; };
  }, [id]);

  async function approve() {
    setActing(true);
    await authFetch(`/api/v1/workflows/${id}/approve`, { method: "POST" });
    router.push(`/workflows/${id}/report`);
  }

  async function reject() {
    setActing(true);
    await authFetch(`/api/v1/workflows/${id}/reject`, { method: "POST" });
    setItem((prev: any) => ({ ...prev, status: "FAILED" }));
    setActing(false);
  }

  if (!item) return <main className="p-8 text-zinc-400">Loading...</main>;

  const statusColor: Record<string, string> = {
    COMPLETED: "text-emerald-400",
    RUNNING: "text-yellow-400",
    PENDING_APPROVAL: "text-blue-400",
    FAILED: "text-red-400",
  };

  return (
    <main className="p-8 max-w-4xl mx-auto space-y-6">
      <h1 className="text-2xl font-semibold">Workflow Detail</h1>

      <div className="border border-zinc-800 rounded p-4 bg-zinc-900 space-y-1">
        <div><span className="text-zinc-400">ID:</span> {item.id}</div>
        <div><span className="text-zinc-400">Type:</span> {item.workflowType}</div>
        <div>
          <span className="text-zinc-400">Status:</span>{" "}
          <span className={statusColor[item.status] || "text-zinc-100"}>
            {item.status === "RUNNING" ? "⏳ Processing..." : item.status.replace("_", " ")}
          </span>
        </div>
        <div><span className="text-zinc-400">Started:</span> {new Date(item.startedAt).toLocaleString()}</div>
        {item.completedAt && (
          <div><span className="text-zinc-400">Completed:</span> {new Date(item.completedAt).toLocaleString()}</div>
        )}
      </div>

      {item.status === "PENDING_APPROVAL" && (
        <div className="border border-blue-800 rounded p-4 bg-blue-950 space-y-3">
          <p className="text-blue-200 font-medium">AI report is ready for manager review.</p>
          <div className="flex gap-3">
            <Link href={`/workflows/${id}/report`} className="px-4 py-2 bg-zinc-700 rounded text-sm">
              View Report
            </Link>
            <button
              onClick={approve}
              disabled={acting}
              className="px-4 py-2 bg-emerald-600 rounded text-sm disabled:opacity-50"
            >
              Approve & Complete
            </button>
            <button
              onClick={reject}
              disabled={acting}
              className="px-4 py-2 bg-red-700 rounded text-sm disabled:opacity-50"
            >
              Reject
            </button>
          </div>
        </div>
      )}

      {(item.status === "COMPLETED" || item.status === "FAILED") && (
        <div className="flex gap-3">
          <Link href={`/workflows/${id}/report`} className="px-3 py-2 bg-emerald-700 rounded">View Report</Link>
          <Link href={`/workflows/${id}/logs`} className="px-3 py-2 bg-zinc-800 rounded">Execution Logs</Link>
          <Link href="/dashboard" className="px-3 py-2 bg-zinc-800 rounded">← Dashboard</Link>
        </div>
      )}

      {item.status === "RUNNING" && (
        <p className="text-zinc-400 text-sm">Auto-refreshing every 3 seconds...</p>
      )}
    </main>
  );
}
