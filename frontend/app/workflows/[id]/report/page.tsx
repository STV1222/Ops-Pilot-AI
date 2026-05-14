"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import Link from "next/link";
import ReactMarkdown from "react-markdown";
import { authFetch } from "../../../../lib/api";

export default function ReportPage() {
  const { id } = useParams<{ id: string }>();
  const [report, setReport] = useState<any>(null);

  useEffect(() => {
    authFetch(`/api/v1/workflows/${id}/report`).then((r) => r.json()).then(setReport).catch(() => setReport(null));
  }, [id]);

  return (
    <main className="p-8 max-w-4xl mx-auto space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-semibold">Generated Report</h1>
        <Link href={`/workflows/${id}`} className="px-3 py-2 bg-zinc-800 rounded text-sm">← Back</Link>
      </div>
      {report?.summary && (
        <div className="flex gap-4 p-4 bg-zinc-900 border border-zinc-800 rounded">
          <div className="text-center">
            <div className="text-xs text-zinc-400 uppercase">Best Supplier</div>
            <div className="text-emerald-400 font-semibold">{report.summary.bestSupplier}</div>
          </div>
          <div className="text-center">
            <div className="text-xs text-zinc-400 uppercase">Confidence</div>
            <div className="font-semibold">{Math.round((report.summary.confidence || 0) * 100)}%</div>
          </div>
          <div className="text-center">
            <div className="text-xs text-zinc-400 uppercase">Anomalies</div>
            <div className="font-semibold">{report.summary.anomalyCount ?? 0}</div>
          </div>
        </div>
      )}
      <div className="prose prose-invert prose-table:w-full max-w-none bg-zinc-900 border border-zinc-800 rounded p-6">
        <ReactMarkdown>{report?.markdown || "No report available."}</ReactMarkdown>
      </div>
    </main>
  );
}
