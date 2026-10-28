import { useEffect, useRef, useState } from "react";
import { Headphones, Volume2, X } from "lucide-react";
import { speakBritish } from "@/lib/speech/british-speech";

interface IpaSound {
  ipa: string;
  example: string;
  exampleWord: string;
  type: "short-vowel" | "long-vowel" | "diphthong" | "consonant";
}

const IPA_SOUNDS: IpaSound[] = [
  // Short Vowels
  { ipa: "/ɪ/", exampleWord: "sit", example: "sɪt", type: "short-vowel" },
  { ipa: "/e/", exampleWord: "bed", example: "bed", type: "short-vowel" },
  { ipa: "/æ/", exampleWord: "cat", example: "kæt", type: "short-vowel" },
  { ipa: "/ɒ/", exampleWord: "hot", example: "hɒt", type: "short-vowel" },
  { ipa: "/ʊ/", exampleWord: "book", example: "bʊk", type: "short-vowel" },
  { ipa: "/ʌ/", exampleWord: "cup", example: "kʌp", type: "short-vowel" },
  { ipa: "/ə/", exampleWord: "about", example: "əˈbaʊt", type: "short-vowel" },

  // Long Vowels
  { ipa: "/iː/", exampleWord: "see", example: "siː", type: "long-vowel" },
  { ipa: "/ɑː/", exampleWord: "car", example: "kɑː", type: "long-vowel" },
  { ipa: "/ɔː/", exampleWord: "door", example: "dɔː", type: "long-vowel" },
  { ipa: "/uː/", exampleWord: "moon", example: "muːn", type: "long-vowel" },
  { ipa: "/ɜː/", exampleWord: "bird", example: "bɜːd", type: "long-vowel" },

  // Diphthongs
  { ipa: "/eɪ/", exampleWord: "day", example: "deɪ", type: "diphthong" },
  { ipa: "/aɪ/", exampleWord: "sky", example: "skaɪ", type: "diphthong" },
  { ipa: "/ɔɪ/", exampleWord: "boy", example: "bɔɪ", type: "diphthong" },
  { ipa: "/aʊ/", exampleWord: "now", example: "naʊ", type: "diphthong" },
  { ipa: "/əʊ/", exampleWord: "go", example: "ɡəʊ", type: "diphthong" },
  { ipa: "/ɪə/", exampleWord: "here", example: "hɪə", type: "diphthong" },
  { ipa: "/eə/", exampleWord: "hair", example: "heə", type: "diphthong" },
  { ipa: "/ʊə/", exampleWord: "pure", example: "pjʊə", type: "diphthong" },

  // Key Consonants
  { ipa: "/p/", exampleWord: "pen", example: "pen", type: "consonant" },
  { ipa: "/b/", exampleWord: "bad", example: "bæd", type: "consonant" },
  { ipa: "/t/", exampleWord: "tea", example: "tiː", type: "consonant" },
  { ipa: "/d/", exampleWord: "dog", example: "dɒɡ", type: "consonant" },
  { ipa: "/tʃ/", exampleWord: "church", example: "tʃɜːtʃ", type: "consonant" },
  { ipa: "/dʒ/", exampleWord: "judge", example: "dʒʌdʒ", type: "consonant" },
  { ipa: "/k/", exampleWord: "car", example: "kɑː", type: "consonant" },
  { ipa: "/ɡ/", exampleWord: "get", example: "ɡet", type: "consonant" },
  { ipa: "/f/", exampleWord: "four", example: "fɔː", type: "consonant" },
  { ipa: "/v/", exampleWord: "voice", example: "vɔɪs", type: "consonant" },
  { ipa: "/θ/", exampleWord: "think", example: "θɪŋk", type: "consonant" },
  { ipa: "/ð/", exampleWord: "this", example: "ðɪs", type: "consonant" },
  { ipa: "/s/", exampleWord: "see", example: "siː", type: "consonant" },
  { ipa: "/z/", exampleWord: "zoo", example: "zuː", type: "consonant" },
  { ipa: "/ʃ/", exampleWord: "she", example: "ʃiː", type: "consonant" },
  { ipa: "/ʒ/", exampleWord: "vision", example: "ˈvɪʒn", type: "consonant" },
  { ipa: "/m/", exampleWord: "man", example: "mæn", type: "consonant" },
  { ipa: "/n/", exampleWord: "no", example: "nəʊ", type: "consonant" },
  { ipa: "/ŋ/", exampleWord: "sing", example: "sɪŋ", type: "consonant" },
  { ipa: "/h/", exampleWord: "hat", example: "hæt", type: "consonant" },
  { ipa: "/l/", exampleWord: "leg", example: "leɡ", type: "consonant" },
  { ipa: "/r/", exampleWord: "red", example: "red", type: "consonant" },
  { ipa: "/w/", exampleWord: "wet", example: "wet", type: "consonant" },
  { ipa: "/j/", exampleWord: "yes", example: "jes", type: "consonant" },
];

interface IpaSoundboardModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function IpaSoundboardModal({ isOpen, onClose }: IpaSoundboardModalProps) {
  const [activeTab, setActiveTab] = useState<"all" | "short-vowel" | "long-vowel" | "diphthong" | "consonant">("all");
  const [playingIpa, setPlayingIpa] = useState<string | null>(null);

  const dialogRef = useRef<HTMLDivElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const previousActiveElementRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!isOpen) return;

    previousActiveElementRef.current = document.activeElement as HTMLElement | null;

    const timer = setTimeout(() => {
      closeButtonRef.current?.focus();
    }, 50);

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onClose();
        return;
      }

      if (e.key === "Tab" && dialogRef.current) {
        const focusable = dialogRef.current.querySelectorAll<HTMLElement>(
          'button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'
        );
        if (focusable.length === 0) return;

        const first = focusable[0];
        const last = focusable[focusable.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === first || !dialogRef.current.contains(document.activeElement)) {
            e.preventDefault();
            last.focus();
          }
        } else {
          if (document.activeElement === last || !dialogRef.current.contains(document.activeElement)) {
            e.preventDefault();
            first.focus();
          }
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      clearTimeout(timer);
      window.removeEventListener("keydown", handleKeyDown);
      previousActiveElementRef.current?.focus();
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handlePlay = (sound: IpaSound) => {
    setPlayingIpa(sound.ipa);
    void speakBritish(`${sound.exampleWord}`, {
      rate: 0.85,
      onEnd: () => setPlayingIpa(null),
    });
  };

  const filtered = activeTab === "all" ? IPA_SOUNDS : IPA_SOUNDS.filter((s) => s.type === activeTab);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="ipa-soundboard-title"
      ref={dialogRef}
      className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm p-4 overflow-y-auto"
      onClick={onClose}
    >
      <div
        className="w-full max-w-3xl rounded-2xl border border-border bg-card p-6 shadow-2xl space-y-5 my-8"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border pb-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Headphones className="h-5 w-5" />
            </div>
            <div>
              <h2 id="ipa-soundboard-title" className="text-lg font-bold text-foreground">
                Bảng Ngữ Âm Quốc Tế IPA (44 âm tiếng Anh)
              </h2>
              <p className="text-xs text-muted-foreground">
                Bấm vào từng âm để nghe đọc từ ví dụ bằng giọng tổng hợp (TTS) tham khảo
              </p>
            </div>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground transition"
            aria-label="Đóng bảng ngữ âm"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Filter Pills */}
        <div className="flex flex-wrap gap-2 text-xs">
          {[
            { id: "all", label: "Tất cả (44 âm)" },
            { id: "short-vowel", label: "Nguyên âm ngắn (7)" },
            { id: "long-vowel", label: "Nguyên âm dài (5)" },
            { id: "diphthong", label: "Nguyên âm đôi (8)" },
            { id: "consonant", label: "Phụ âm (24)" },
          ].map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id as typeof activeTab)}
              className={`px-3 py-1.5 rounded-lg font-medium transition ${
                activeTab === tab.id
                  ? "bg-primary text-primary-foreground shadow-xs"
                  : "bg-muted text-muted-foreground hover:text-foreground"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Grid of Sound Buttons */}
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2.5 max-h-[55vh] overflow-y-auto pr-1">
          {filtered.map((sound) => {
            const isPlaying = playingIpa === sound.ipa;
            return (
              <button
                key={sound.ipa}
                type="button"
                onClick={() => handlePlay(sound)}
                title={`Nghe từ ví dụ: "${sound.exampleWord}" (phiên âm /${sound.example}/)`}
                aria-label={`Nghe từ ví dụ ${sound.exampleWord}`}
                className={`flex items-center justify-between p-3 rounded-xl border text-left transition-all active:scale-95 ${
                  isPlaying
                    ? "border-primary bg-primary/15 ring-2 ring-primary/30"
                    : "border-border/80 bg-background/70 hover:border-primary/50 hover:bg-primary/5"
                }`}
              >
                <div>
                  <div className="text-base font-bold font-mono text-primary">
                    {sound.ipa}
                  </div>
                  <div className="text-xs text-foreground font-medium mt-0.5">
                    {sound.exampleWord}
                  </div>
                  <div className="text-[11px] text-muted-foreground font-mono">
                    /{sound.example}/
                  </div>
                </div>
                <div className={`p-1.5 rounded-lg ${isPlaying ? "bg-primary text-primary-foreground animate-bounce" : "text-muted-foreground"}`}>
                  <Volume2 className="h-4 w-4" />
                </div>
              </button>
            );
          })}
        </div>

        {/* Footer tip */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs text-muted-foreground pt-3 border-t border-border">
          <span className="leading-relaxed">
            * Lưu ý: Nút loa phát âm từ ví dụ bằng công nghệ SpeechSynthesis của trình duyệt để tham khảo, không phải âm vị độc lập và chưa qua thẩm định của giáo viên bản ngữ.
          </span>
          <button
            type="button"
            onClick={onClose}
            className="shrink-0 px-4 py-1.5 text-xs font-semibold rounded-lg bg-primary text-primary-foreground hover:bg-primary/90 transition"
          >
            Đã hiểu, đóng bảng
          </button>
        </div>
      </div>
    </div>
  );
}
