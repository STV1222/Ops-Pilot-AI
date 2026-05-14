import Link from "next/link";

export default function Home() {
  return (
    <main className="p-10 max-w-4xl mx-auto space-y-6">
      <h1 className="text-4xl font-semibold">OpsPilot AI</h1>
      <p className="text-zinc-300">Operational AI infrastructure for quotation workflows.</p>
      <div className="flex gap-3">
        <Link href="/login" className="px-4 py-2 bg-emerald-600 rounded">Login</Link>
        <Link href="/dashboard" className="px-4 py-2 bg-zinc-800 rounded">Dashboard</Link>
      </div>
    </main>
  );
}
