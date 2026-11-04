import { ReactNode } from "react";
import { AthleteNotifications } from "@/components/AthleteNotifications";
import { getAthleteId } from "@/lib/auth";
import { ATHLETE_NAV } from "@/lib/athlete-nav";
import { AppShell } from "./AppShell";

export function AthleteShell({ children }: { children: ReactNode }) {
  return (
    <AppShell
      navItems={ATHLETE_NAV}
      headerActions={<AthleteNotifications athleteId={getAthleteId()} />}
    >
      {children}
    </AppShell>
  );
}
