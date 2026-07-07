import { SourceBadge } from "@/components/kit/source-badge";
import type { ScheduledPost } from "@/lib/mock/schedule";
import { STATUS_DOT } from "./status";

export function PostChip({ post }: { post: ScheduledPost }) {
  return (
    <div className="flex min-w-0 items-center gap-1 rounded-md bg-surface/80 px-1 py-0.5 text-[10px]">
      <span className={`size-1.5 shrink-0 rounded-full ${STATUS_DOT[post.status]}`} />
      <SourceBadge source={post.platform} compact />
      <span className="min-w-0 truncate text-foreground/80">{post.projectTitle}</span>
    </div>
  );
}
