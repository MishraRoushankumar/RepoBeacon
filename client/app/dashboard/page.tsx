"use client";

import { RepoDashboard } from "@/components/dashboard/repo-dashboard";
import { AppShell } from "@/components/layout/app-shell";
import { RequireAuth } from "@/providers/require-auth";

const DashboardPage = () => {
  return (
    <RequireAuth>
      <AppShell hideHeader>
        <RepoDashboard />
      </AppShell>
    </RequireAuth>
  );
};

export default DashboardPage;
