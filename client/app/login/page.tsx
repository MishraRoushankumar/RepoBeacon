"use client";

import { GitHubIcon } from "@/components/icons/github-icon";
import { BrandMark } from "@/components/layout/app-shell";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { buttonVariants } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { ThemeToggle } from "@/components/ui/theme-toggle";
import { useCurrentUser } from "@/hooks/use-auth";
import { getGithubLoginUrl } from "@/lib/api";
import { cn } from "@/lib/utils";
import { AlertCircle } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect } from "react";

function LoginLoading() {
  return (
    <div className="flex min-h-svh items-center justify-center">
      <Spinner className="size-8" />
    </div>
  );
}

const LoginContent = () => {
  const router = useRouter();
  const params = useSearchParams();
  const error = params.get("error");
  const next = params.get("next") || "/dashboard";
  const { data: user, isLoading } = useCurrentUser();

  useEffect(() => {
    if (!isLoading && user) {
      router.replace(next.startsWith("/") ? next : "/dashboard");
    }
  }, [user, isLoading, next, router]);

  if (isLoading || user) {
    return <LoginLoading />;
  }

  return (
    <div className="relative flex min-h-svh flex-col overflow-hidden bg-background">
      <div className="pointer-events-auto absolute inset-0 bg-[radial-gradient(ellipse_at_top,oklch(from_var(--primary)_1_c_h/0.1),transparent_55%)]">
        <header className="relative z-10 flex h-14 items-center justify-between px-4">
          <Link href="/">
            <BrandMark />
          </Link>
          <ThemeToggle />
        </header>

        <main className="relative z-10 flex flex-1 items-center justify-center px-4 py-10">
          <Card className="w-full max-w-sm border-border/70 bg-card/90 shadow-lg shadow-foreground/5 backdrop-blur-xl">
            <CardHeader className="space-y-4 text-center">
              <div className="mx-auto flex size-12 items-center text-background justify-center rounded-2xl bg-foreground">
                <GitHubIcon className="size-10" />
              </div>
              <div className="space-y-1">
                <CardTitle className="text-xl">Sign in</CardTitle>
                <CardDescription>
                  Connect Github to Chat with your repositories
                </CardDescription>
              </div>
            </CardHeader>

            <CardContent className="space-y-4">
              {error && (
                <Alert variant="destructive">
                  <AlertCircle />
                  <AlertTitle>Sign-in failed</AlertTitle>
                  <AlertDescription>Please try again</AlertDescription>
                </Alert>
              )}

              <a
                href={getGithubLoginUrl(next)}
                className={cn(
                  buttonVariants({ size: "lg" }),
                  "inline-flex w-full items-center justify-center gap-2 bg-foreground text-background hover:bg-background/90 hover:text-foreground",
                )}
              >
                <GitHubIcon className="size-5" />
                Continue with Github
              </a>
            </CardContent>
          </Card>
        </main>
      </div>
    </div>
  );
};

export default function LoginPage() {
  return (
    <Suspense fallback={<LoginLoading />}>
      <LoginContent />
    </Suspense>
  );
}
