import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { MarkdownContent } from "../components/MarkdownContent";

describe("MarkdownContent", () => {
  it("renders GFM table correctly with table structure", () => {
    const markdown = `
| Vowel | IPA | Example |
|---|---|---|
| Short e | /e/ | bed |
| Short a | /æ/ | cat |
`;
    render(<MarkdownContent content={markdown} />);

    const table = screen.getByRole("table");
    expect(table).toBeInTheDocument();
    expect(screen.getByText("Vowel")).toBeInTheDocument();
    expect(screen.getByText("Short e")).toBeInTheDocument();
    expect(screen.getByText("/æ/")).toBeInTheDocument();
    expect(screen.getByText("cat")).toBeInTheDocument();
  });

  it("renders unicode IPA characters accurately without corruption", () => {
    const ipaText = "Phonetics: /ə/ (schwa), /ɪ/ (kit), /iː/ (fleece), /θ/ (think), /ð/ (this)";
    render(<MarkdownContent content={ipaText} />);

    expect(screen.getByText(/Phonetics:/)).toBeInTheDocument();
    expect(screen.getByText(/\/ə\//)).toBeInTheDocument();
    expect(screen.getByText(/\/iː\//)).toBeInTheDocument();
    expect(screen.getByText(/\/θ\//)).toBeInTheDocument();
  });

  it("sanitizes dangerous XSS payloads while preserving safe markdown", () => {
    const maliciousMarkdown = `
# Safe Title

<script>alert("hacked")</script>

<img src="x" onerror="alert(1)" />

<a href="javascript:alert('xss')">Dangerous Link</a>

[Legit Link](https://bbc.co.uk)
`;
    const { container } = render(<MarkdownContent content={maliciousMarkdown} />);

    // Script tag must be removed
    expect(container.querySelector("script")).toBeNull();

    // Image onerror attribute must be sanitized
    const img = container.querySelector("img");
    if (img) {
      expect(img.getAttribute("onerror")).toBeNull();
    }

    // javascript: link must be sanitized
    const dangerousLink = container.querySelector("a[href^='javascript:']");
    expect(dangerousLink).toBeNull();

    // Safe external link preserved with target="_blank" and rel="noopener noreferrer"
    const safeLink = screen.getByRole("link", { name: "Legit Link" });
    expect(safeLink).toBeInTheDocument();
    expect(safeLink).toHaveAttribute("target", "_blank");
    expect(safeLink).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("renders blockquotes and code blocks properly", () => {
    const markdown = `
> This is an important rule.

\`\`\`javascript
const greeting = "Hello";
\`\`\`

Here is \`inline code\` snippet.
`;
    render(<MarkdownContent content={markdown} />);

    expect(screen.getByText("This is an important rule.")).toBeInTheDocument();
    expect(screen.getByText(/const greeting = "Hello";/)).toBeInTheDocument();
    expect(screen.getByText("inline code")).toBeInTheDocument();
  });
});
