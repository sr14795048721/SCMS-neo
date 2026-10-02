"use client";

import { useEffect, useMemo, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { authorizedFetch } from "../../lib/auth/client";
import styles from "../../styles/newsMarkdown.module.css";

type MarkdownArticleProps = {
  content: string;
  mode?: "public" | "admin";
};

const PUBLIC_NEWS_ASSET_PATTERN = /\/api\/v1\/public\/news\/(\d+)\/assets\/(\d+)(?:[?#].*)?$/i;

function MarkdownImage({
  src,
  alt,
  adminPreview
}: {
  src?: string;
  alt?: string;
  adminPreview: boolean;
}) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);

  const previewPath = useMemo(() => {
    if (!adminPreview || !src) {
      return null;
    }

    const match = String(src).match(PUBLIC_NEWS_ASSET_PATTERN);
    if (!match) {
      return null;
    }
    return `/api/admin/news/${match[1]}/assets/${match[2]}/content`;
  }, [adminPreview, src]);

  useEffect(() => {
    let active = true;
    let localObjectUrl: string | null = null;

    async function loadPreview() {
      if (!previewPath) {
        setObjectUrl(null);
        return;
      }

      try {
        const response = await authorizedFetch(previewPath, {
          method: "GET",
          cache: "no-store"
        });
        if (!response?.ok) {
          if (active) {
            setObjectUrl(null);
          }
          return;
        }

        const blob = await response.blob();
        localObjectUrl = URL.createObjectURL(blob);
        if (active) {
          setObjectUrl(localObjectUrl);
        }
      } catch {
        if (active) {
          setObjectUrl(null);
        }
      }
    }

    void loadPreview();

    return () => {
      active = false;
      if (localObjectUrl) {
        URL.revokeObjectURL(localObjectUrl);
      }
    };
  }, [previewPath]);

  return <img src={objectUrl || (!adminPreview ? src : undefined)} alt={alt || ""} />;
}

export function MarkdownArticle({ content, mode = "public" }: MarkdownArticleProps) {
  return (
    <div className={styles.markdown}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          img: ({ src, alt }) => <MarkdownImage src={src} alt={alt} adminPreview={mode === "admin"} />
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
