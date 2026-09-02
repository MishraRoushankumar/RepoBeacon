"use client";

import { AppShell } from "@/components/layout/app-shell";
import { RequireAuth } from "@/providers/require-auth";

const DashboardPage = () => {
  return (
    <RequireAuth>
      <AppShell hideHeader>
        <div className="flex min-h-svh items-center justify-center">
          <h1 className="text-2xl font-bold">Welcome to RepoBeacon</h1>
        </div>
      </AppShell>
    </RequireAuth>
  );
};

export default DashboardPage;
