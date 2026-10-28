/**
 * British Speech Synthesis Engine (Web Speech API)
 * Specifically configured for British English (en-GB / RP) self-study.
 */

// Common IPA vowel/consonant sound descriptors for text-to-speech fallback
const IPA_DESCRIPTORS: Record<string, string> = {
  "/iː/": "long ee sound, like see, tree, sheep",
  "/ɪ/": "short ih sound, like sit, pin, fish",
  "/e/": "short eh sound, like bed, set, ten",
  "/æ/": "short a sound, like cat, black, hand",
  "/ɑː/": "long ah sound, like car, star, heart",
  "/ɒ/": "short o sound, like hot, clock, lock",
  "/ɔː/": "long or sound, like four, door, walk",
  "/ʊ/": "short oo sound, like book, look, foot",
  "/uː/": "long oo sound, like moon, blue, shoe",
  "/ʌ/": "short uh sound, like cup, bus, love",
  "/ɜː/": "long er sound, like bird, girl, learn",
  "/ə/": "schwa sound, like teacher, about, computer",
  "/eɪ/": "diphthong sound, like face, day, rain",
  "/aɪ/": "diphthong sound, like price, high, sky",
  "/ɔɪ/": "diphthong sound, like boy, choice, coin",
  "/aʊ/": "diphthong sound, like mouth, now, cow",
  "/əʊ/": "diphthong sound, like goat, home, show",
  "/ɪə/": "diphthong sound, like near, here, beer",
  "/eə/": "diphthong sound, like square, fair, chair",
  "/ʊə/": "diphthong sound, like cure, tourist, pure",
  "/p/": "sound p, like pen, pet",
  "/b/": "sound b, like bat, ball",
  "/t/": "sound t, like tea, time",
  "/d/": "sound d, like day, dog",
  "/k/": "sound k, like key, car",
  "/g/": "sound g, like get, go",
  "/tʃ/": "sound ch, like check, church",
  "/dʒ/": "sound j, like judge, joy",
  "/f/": "sound f, like fan, fish",
  "/v/": "sound v, like van, voice",
  "/θ/": "unvoiced th sound, like think, thought, bath",
  "/ð/": "voiced th sound, like this, that, mother",
  "/s/": "sound s, like sun, see",
  "/z/": "sound z, like zoo, lazy",
  "/ʃ/": "sound sh, like shoe, fish",
  "/ʒ/": "sound zh, like measure, vision",
  "/h/": "sound h, like hat, home",
  "/m/": "sound m, like man, moon",
  "/n/": "sound n, like no, sun",
  "/ŋ/": "sound ng, like sing, song",
  "/l/": "sound l, like light, live",
  "/r/": "sound r, like red, road",
  "/w/": "sound w, like win, water",
  "/j/": "sound y, like yes, yellow",
};

let cachedVoices: SpeechSynthesisVoice[] = [];

function loadVoices(): Promise<SpeechSynthesisVoice[]> {
  return new Promise((resolve) => {
    if (typeof window === "undefined" || !("speechSynthesis" in window)) {
      resolve([]);
      return;
    }
    const voices = window.speechSynthesis.getVoices();
    if (voices.length > 0) {
      cachedVoices = voices;
      resolve(voices);
      return;
    }
    window.speechSynthesis.onvoiceschanged = () => {
      const refreshed = window.speechSynthesis.getVoices();
      cachedVoices = refreshed;
      resolve(refreshed);
    };
    // Fallback if event doesn't fire promptly
    setTimeout(() => {
      cachedVoices = window.speechSynthesis.getVoices();
      resolve(cachedVoices);
    }, 500);
  });
}

/** Checks if text is likely English (does not contain Vietnamese diacritics) */
export function isLikelyEnglish(text: string): boolean {
  if (!text) return false;
  // Regex detecting common Vietnamese accented characters
  const vietnamesePattern =
    /[àáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵĐ]/i;
  return !vietnamesePattern.test(text);
}

export interface VoiceInfo {
  voiceName: string;
  lang: string;
  isBritish: boolean;
}

/** Finds best British English voice available in user's browser */
export async function getBritishVoice(): Promise<SpeechSynthesisVoice | null> {
  const voices = cachedVoices.length > 0 ? cachedVoices : await loadVoices();
  // 1. Direct en-GB voice
  const ukVoice = voices.find(
    (v) =>
      v.lang.toLowerCase() === "en-gb" ||
      v.lang.toLowerCase() === "en_gb" ||
      v.name.includes("United Kingdom") ||
      v.name.includes("British") ||
      v.name.includes("English (UK)")
  );
  if (ukVoice) return ukVoice;

  // 2. Any English voice as fallback
  const anyEn = voices.find((v) => v.lang.toLowerCase().startsWith("en"));
  return anyEn ?? null;
}

/** Returns info on current voice used for honest user presentation */
export async function getVoiceInfo(): Promise<VoiceInfo> {
  const voice = await getBritishVoice();
  if (!voice) {
    return {
      voiceName: "Mặc định trình duyệt",
      lang: "en",
      isBritish: false,
    };
  }
  const isBritish =
    voice.lang.toLowerCase().includes("gb") ||
    voice.name.toLowerCase().includes("british") ||
    voice.name.toLowerCase().includes("united kingdom");
  return {
    voiceName: voice.name,
    lang: voice.lang,
    isBritish,
  };
}

/**
 * Speaks text using British English TTS.
 * Only accepts English text (disallows Vietnamese titles).
 * Automatically cleans IPA slashes or provides example word if needed.
 */
export async function speakBritish(
  text: string,
  options?: { rate?: number; pitch?: number; onEnd?: () => void }
): Promise<void> {
  if (typeof window === "undefined" || !("speechSynthesis" in window)) {
    options?.onEnd?.();
    return;
  }

  // Reject non-English text to prevent bizarre pronunciation of Vietnamese
  if (!isLikelyEnglish(text)) {
    options?.onEnd?.();
    return;
  }

  // Cancel any ongoing utterance
  window.speechSynthesis.cancel();

  const trimmed = text.trim();
  // Check if text is an IPA symbol
  const spokenText = IPA_DESCRIPTORS[trimmed] ?? trimmed.replace(/[/]/g, "");

  const utterance = new SpeechSynthesisUtterance(spokenText);
  utterance.lang = "en-GB";
  utterance.rate = options?.rate ?? 0.9; // Slightly slower than 1.0 for clearer articulation
  utterance.pitch = options?.pitch ?? 1.0;

  const voice = await getBritishVoice();
  if (voice) {
    utterance.voice = voice;
  }

  utterance.onend = () => {
    options?.onEnd?.();
  };
  utterance.onerror = () => {
    options?.onEnd?.();
  };

  window.speechSynthesis.speak(utterance);
}
