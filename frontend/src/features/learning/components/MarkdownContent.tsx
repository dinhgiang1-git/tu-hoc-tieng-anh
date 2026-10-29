import { useMemo } from "react";
import { marked } from "marked";
import DOMPurify from "dompurify";

interface MarkdownContentProps {
  content: string;
  className?: string;
}

// Configure marked with Github Flavored Markdown & breaks
marked.setOptions({
  gfm: true,
  breaks: true,
});

export function MarkdownContent({ content, className = "" }: MarkdownContentProps) {
  const sanitizedHtml = useMemo(() => {
    if (!content) return "";

    // 1. Parse markdown to raw HTML
    const rawHtml = marked.parse(content) as string;

    // 2. Sanitize using DOMPurify with strict XSS protections and target=_blank for external links
    const cleanHtml = DOMPurify.sanitize(rawHtml, {
      ALLOWED_TAGS: [
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "p",
        "a",
        "ul",
        "ol",
        "li",
        "blockquote",
        "code",
        "pre",
        "strong",
        "em",
        "table",
        "thead",
        "tbody",
        "tr",
        "th",
        "td",
        "hr",
        "br",
        "span",
      ],
      ALLOWED_ATTR: ["href", "title", "target", "rel", "class"],
      ADD_ATTR: ["target", "rel"],
    });

    // Ensure external links have safe attributes
    return cleanHtml.replace(/<a\s+(?:[^>]*?\s+)?href="([^"]*)"/gi, (match, href) => {
      if (href.startsWith("http://") || href.startsWith("https://")) {
        return `<a href="${href}" target="_blank" rel="noopener noreferrer"`;
      }
      return match;
    });
  }, [content]);

  return (
    <div
      className={`prose prose-neutral dark:prose-invert max-w-none text-lg leading-[1.8] xl:text-xl
        [&_h1]:text-3xl [&_h1]:font-bold [&_h1]:text-foreground [&_h1]:mb-4 [&_h1]:mt-6 [&_h1]:tracking-tight
        [&_h2]:text-2xl [&_h2]:font-bold [&_h2]:text-foreground [&_h2]:mb-3 [&_h2]:mt-5
        [&_h3]:text-xl [&_h3]:font-semibold [&_h3]:text-foreground [&_h3]:mb-2 [&_h3]:mt-4
        [&_p]:leading-[1.8] [&_p]:text-foreground/90 [&_p]:mb-4
        [&_ul]:list-disc [&_ul]:pl-5 [&_ul]:mb-3 [&_ul]:space-y-1
        [&_ol]:list-decimal [&_ol]:pl-5 [&_ol]:mb-3 [&_ol]:space-y-1
        [&_li]:text-foreground/90
        [&_blockquote]:border-l-4 [&_blockquote]:border-primary/40 [&_blockquote]:pl-4 [&_blockquote]:italic [&_blockquote]:text-muted-foreground [&_blockquote]:my-3
        [&_code]:font-mono [&_code]:text-[0.9em] [&_code]:bg-muted [&_code]:px-1.5 [&_code]:py-0.5 [&_code]:rounded [&_code]:text-primary [&_code]:break-words
        [&_pre]:bg-muted/80 [&_pre]:p-4 [&_pre]:rounded-lg [&_pre]:overflow-x-auto [&_pre]:my-3
        [&_table]:w-full [&_table]:border-collapse [&_table]:my-4 [&_table]:text-base xl:[&_table]:text-lg
        [&_th]:border [&_th]:border-border [&_th]:bg-muted/50 [&_th]:p-2.5 [&_th]:text-left [&_th]:font-semibold [&_th]:text-foreground
        [&_td]:border [&_td]:border-border [&_td]:p-2.5 [&_td]:text-foreground/90
        [&_tr:nth-child(even)]:bg-muted/20
        [&_a]:text-primary [&_a]:underline [&_a]:underline-offset-2 hover:[&_a]:text-primary/80
        [&_table]:overflow-x-auto
        ${className}`}
      data-testid="markdown-content"
      dangerouslySetInnerHTML={{ __html: sanitizedHtml }}
    />
  );
}
