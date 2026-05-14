"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { authFetch } from "../../../lib/api";

export default function UploadWorkflowPage() {
  const [files, setFiles] = useState<FileList | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  async function submit() {
    if (!files?.length) return;
    setLoading(true);
    const form = new FormData();
    Array.from(files).forEach((f) => form.append("files", f));
    const res = await authFetch("/api/v1/workflows/quotation-comparison", { method: "POST", body: form });
    const data = await res.json();
    router.push(`/workflows/${data.id}`);
  }

  return (
    <main className="p-8 max-w-3xl mx-auto space-y-4">
      <h1 className="text-2xl font-semibold">Upload Quotation Documents</h1>
      <p className="text-zinc-400 text-sm">Select two or more supplier quotation files (PDF or Excel).</p>
      <input type="file" multiple onChange={(e) => setFiles(e.target.files)} className="w-full border border-zinc-700 rounded p-3 bg-zinc-900" />
      <button onClick={submit} disabled={loading || !files?.length} className="bg-emerald-600 px-4 py-2 rounded disabled:opacity-50">
        {loading ? "Processing..." : "Run Workflow"}
      </button>
    </main>
  );
}
