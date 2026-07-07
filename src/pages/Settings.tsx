import { PageHeader } from "@/components/kit/page-header";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AccountTab } from "@/components/settings/account-tab";
import { SecurityTab } from "@/components/settings/security-tab";
import { PluginsTab } from "@/components/settings/plugins-tab";
import { PromptTemplatesTab } from "@/components/settings/prompt-templates-tab";
import { TeamTab } from "@/components/settings/team-tab";

export default function Settings() {
  return (
    <div className="space-y-6">
      <PageHeader title="Settings" subtitle="Credentials, plugins, team workspaces, and prompt templates." />

      <Tabs defaultValue="account" className="space-y-4">
        <TabsList className="h-auto flex-wrap gap-1 bg-surface/60 p-1">
          <TabsTrigger value="account" className="rounded-md text-xs">Account</TabsTrigger>
          <TabsTrigger value="security" className="rounded-md text-xs">Security</TabsTrigger>
          <TabsTrigger value="plugins" className="rounded-md text-xs">Plugins</TabsTrigger>
          <TabsTrigger value="prompts" className="rounded-md text-xs">Prompt templates</TabsTrigger>
          <TabsTrigger value="team" className="rounded-md text-xs">Team</TabsTrigger>
        </TabsList>

        <TabsContent value="account"><AccountTab /></TabsContent>
        <TabsContent value="security"><SecurityTab /></TabsContent>
        <TabsContent value="plugins"><PluginsTab /></TabsContent>
        <TabsContent value="prompts"><PromptTemplatesTab /></TabsContent>
        <TabsContent value="team"><TeamTab /></TabsContent>
      </Tabs>
    </div>
  );
}
