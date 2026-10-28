import { useState } from "react";
import { Volume2, VolumeX } from "lucide-react";
import { speakBritish, isLikelyEnglish } from "@/lib/speech/british-speech";
import { cn } from "@/lib/utils";

interface PronounceButtonProps {
  text?: string | null;
  label?: string;
  rate?: number;
  size?: "sm" | "default" | "lg" | "icon";
  className?: string;
}

export function PronounceButton({
  text,
  label,
  rate = 0.9,
  size = "default",
  className,
}: PronounceButtonProps) {
  const [isPlaying, setIsPlaying] = useState(false);

  // Strictly disallow non-English text (e.g. Vietnamese titles) from having a TTS button
  if (!text || !isLikelyEnglish(text)) return null;

  const handleSpeak = (e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();

    if (isPlaying) {
      window.speechSynthesis?.cancel();
      setIsPlaying(false);
      return;
    }

    setIsPlaying(true);
    void speakBritish(text, {
      rate,
      onEnd: () => setIsPlaying(false),
    });
  };

  const isIconOnly = size === "icon" || !label;

  return (
    <button
      type="button"
      onClick={handleSpeak}
      title={`Nghe phát âm mẫu tiếng Anh (giọng tổng hợp TTS tham khảo): "${text}"`}
      aria-label={`Nghe phát âm mẫu: ${text}`}
      className={cn(
        "inline-flex items-center justify-center gap-1.5 rounded-lg border font-medium transition-all active:scale-95",
        isPlaying
          ? "border-primary bg-primary/15 text-primary animate-pulse"
          : "border-border/80 bg-card hover:bg-primary/10 hover:border-primary/40 hover:text-primary text-muted-foreground",
        isIconOnly
          ? "h-7 w-7 p-0 rounded-md"
          : "h-7 px-2.5 text-xs",
        className
      )}
    >
      {isPlaying ? (
        <VolumeX className="h-3.5 w-3.5 text-primary shrink-0" />
      ) : (
        <Volume2 className="h-3.5 w-3.5 shrink-0" />
      )}
      {!isIconOnly && <span>{label || "Nghe"}</span>}
    </button>
  );
}
