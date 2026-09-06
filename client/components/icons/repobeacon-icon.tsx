import type { SVGProps } from "react";

import { cn } from "@/lib/utils";

type RepoBeaconIconProps = SVGProps<SVGSVGElement> & {
  variant?: "color" | "mono";
};

export function RepoBeaconIcon({
  className,
  variant = "color",
  ...props
}: RepoBeaconIconProps) {
  // We use an img tag pointing to the public SVG to maintain the complex gradients
  // and shapes of the new Minimal Beacon logo.
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src="/logo.svg"
      alt="RepoBeacon"
      aria-hidden="true"
      className={cn("shrink-0 object-contain", className)}
      {...(props as any)}
    />
  );
}

export function RepoBeaconLogo({
  className,
  ...props
}: SVGProps<SVGSVGElement>) {
  return <RepoBeaconIcon className={className} {...props} />;
}
