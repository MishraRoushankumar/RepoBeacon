"use client";

import { Spinner } from "@/components/ui/spinner";
import { useCurrentUser } from "@/hooks/use-auth";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function AuthCallbackPage() {
  const router = useRouter();
  const { data: user, isLoading, isError, isFetched } = useCurrentUser();

  useEffect(() => {
    if (!isFetched || isLoading) return;

    if (user) {
      router.replace("/dashboard");
      return;
    }

    router.replace("/login?error=session");
  }, [user, isLoading, isFetched, isError, router]);

  return (
    <div className="flex min-h-svh flex-col items-center justify-center">
      <Spinner className="size-6" />
      <p className="text-sm text-muted-foreground">
        Finishing Github sign-in...
      </p>
    </div>
  );
}
