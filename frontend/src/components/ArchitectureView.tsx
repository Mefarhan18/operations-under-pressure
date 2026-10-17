import React from 'react';
import { Layers, Database, Lock, Shield, CheckCircle2, Cpu, Code2 } from 'lucide-react';

export const ArchitectureView: React.FC = () => {
  return (
    <div className="space-y-6">
      {/* Hero Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-900 to-emerald-950/40 border border-slate-800 p-8 rounded-2xl">
        <div className="max-w-3xl">
          <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            Operations Under Pressure Challenge
          </span>
          <h1 className="text-3xl font-extrabold text-white mt-3 tracking-tight">
            Newtonite Operations Management System
          </h1>
          <p className="text-slate-300 mt-2 text-base leading-relaxed">
            Phase 1 Foundation complete. Built for high-stakes operational workflows where employees create, assign,
            track, approve, and resolve urgent incidents and work items across cross-functional teams with zero data loss.
          </p>
        </div>
      </div>

      {/* 4 Pillars of Phase 1 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-2">
          <div className="p-2.5 bg-sky-500/10 text-sky-400 rounded-lg w-fit">
            <Cpu className="w-5 h-5" />
          </div>
          <h3 className="font-semibold text-white">Java 21 & Spring Boot 3</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Clean 3-tier architecture (Controllers, Services, Repositories) organized across domain packages.
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-2">
          <div className="p-2.5 bg-emerald-500/10 text-emerald-400 rounded-lg w-fit">
            <Database className="w-5 h-5" />
          </div>
          <h3 className="font-semibold text-white">MySQL & Flyway V1</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Reproducible migrations with explicit foreign keys, indexes, and ON DELETE RESTRICT historical protection.
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-2">
          <div className="p-2.5 bg-purple-500/10 text-purple-400 rounded-lg w-fit">
            <Lock className="w-5 h-5" />
          </div>
          <h3 className="font-semibold text-white">Optimistic Locking</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            JPA <code className="text-purple-400">@Version</code> field prevents silent overwrites when operations personnel edit tickets concurrently.
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-2">
          <div className="p-2.5 bg-amber-500/10 text-amber-400 rounded-lg w-fit">
            <Shield className="w-5 h-5" />
          </div>
          <h3 className="font-semibold text-white">BCrypt & Spring Security</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Passwords stored as salted BCrypt hashes. Stateless Spring Security filter chain configured for Phase 2 auth.
          </p>
        </div>
      </div>

      {/* Architecture Spec Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-slate-900 border border-slate-800 p-6 rounded-xl space-y-4">
          <h3 className="text-lg font-bold text-white flex items-center gap-2">
            <Code2 className="w-5 h-5 text-emerald-400" />
            Backend Package Organization
          </h3>
          <ul className="space-y-2 text-sm text-slate-300">
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span><code className="text-emerald-400 font-mono text-xs">com.newtonite.ops.config</code> — CORS and MVC configuration</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span><code className="text-emerald-400 font-mono text-xs">com.newtonite.ops.security</code> — Spring Security chain & BCrypt bean</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span><code className="text-emerald-400 font-mono text-xs">com.newtonite.ops.user</code> — User entity, repo, service, controller</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span><code className="text-emerald-400 font-mono text-xs">com.newtonite.ops.team</code> — Team & Membership entities and roles</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span><code className="text-emerald-400 font-mono text-xs">com.newtonite.ops.workitem</code> — Work items with optimistic lock versioning</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span><code className="text-emerald-400 font-mono text-xs">com.newtonite.ops.activity</code> — Immutable activity audit log entity</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span><code className="text-emerald-400 font-mono text-xs">com.newtonite.ops.comment</code> — Collaborative discussion thread entity</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span><code className="text-emerald-400 font-mono text-xs">com.newtonite.ops.common</code> — HealthController, GlobalExceptionHandler, DataSeeder</span>
            </li>
          </ul>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-6 rounded-xl space-y-4">
          <h3 className="text-lg font-bold text-white flex items-center gap-2">
            <Layers className="w-5 h-5 text-sky-400" />
            Frontend Technology Stack
          </h3>
          <ul className="space-y-2 text-sm text-slate-300">
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-sky-400 shrink-0" />
              <span><strong>React 18 + Vite + TypeScript</strong> for fast, strictly typed frontend development</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-sky-400 shrink-0" />
              <span><strong>TanStack Query v5</strong> for asynchronous server-state caching and auto-polling</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-sky-400 shrink-0" />
              <span><strong>Axios</strong> configured with timeout and baseURL abstraction</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-sky-400 shrink-0" />
              <span><strong>React Router v6</strong> for declarative SPA navigation</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-sky-400 shrink-0" />
              <span><strong>Tailwind CSS</strong> for responsive, utility-first layout styling</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
};
