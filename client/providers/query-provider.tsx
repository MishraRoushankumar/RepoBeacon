"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode, useState } from "react";

const QueryProvider = ({ children }: { children: ReactNode }) => {
  const [query, setQuery] = useState(() => new QueryClient());
  return <QueryClientProvider client={query}>{children}</QueryClientProvider>;
};

export default QueryProvider;
