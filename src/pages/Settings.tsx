import { PageHeader } from "@/components/kit/page-header";
import { EmptyState } from "@/components/kit/empty-state";
import { Settings as SettingsIcon } from "lucide-react";

export default function Settings() {
  return (
    <div className="space-y-6">
      <PageHeader title="Settings" subtitle="Credentials, plugins, team workspaces, and prompt templates." />
      <EmptyState icon={<SettingsIcon className="size-6" />} description="Account, security, and plugin settings are coming together next." />
    </div>
  );
}
