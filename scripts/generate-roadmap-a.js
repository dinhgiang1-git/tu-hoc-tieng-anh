// scripts/generate-roadmap-a.js
// Generator script for Roadmap A v1 (26 weeks) based on KE_HOACH_DU_AN.md and docs/content-mapping.md

const fs = require('fs');
const path = require('path');

const targetDir = path.resolve(__dirname, '../content/roadmap-a/v1');
const weeksDir = path.join(targetDir, 'weeks');
const lessonsDir = path.join(targetDir, 'lessons');

fs.mkdirSync(weeksDir, { recursive: true });
fs.mkdirSync(lessonsDir, { recursive: true });

console.log('Generating Roadmap A v1 at:', targetDir);

// 1. Resources definitions
const resources = [
  {
    key: "res.bbc.vowels-i-long-short",
    title: "BBC Learning English: The Sounds of English - /iː/ and /ɪ/",
    url: "https://www.youtube.com/watch?v=3g5Y4fP3E7w",
    mediaType: "VIDEO",
    targetLocale: "en-GB",
    attribution: "BBC Learning English",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  {
    key: "res.bbc.consonants-p-b",
    title: "BBC Learning English: The Sounds of English - /p/ and /b/",
    url: "https://www.youtube.com/watch?v=k1tU_F8bW6U",
    mediaType: "VIDEO",
    targetLocale: "en-GB",
    attribution: "BBC Learning English",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  {
    key: "res.oxford.phonetics-guide",
    title: "Oxford Learner's Dictionaries: Pronunciation Guide and Phonetic Symbols",
    url: "https://www.oxfordlearnersdictionaries.com/about/english-language/pronunciation-guide",
    mediaType: "ARTICLE",
    targetLocale: "en-GB",
    attribution: "Oxford University Press",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  {
    key: "res.britishcouncil.word-stress",
    title: "British Council: How to Master Word Stress in English",
    url: "https://learnenglish.britishcouncil.org/grammar/b1-b2-grammar/word-stress",
    mediaType: "ARTICLE",
    targetLocale: "en-GB",
    attribution: "British Council LearnEnglish",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  {
    key: "res.bbc.schwa-vowel",
    title: "BBC Learning English: The Sounds of English - The Schwa /ə/",
    url: "https://www.youtube.com/watch?v=f252G6d3Xb4",
    mediaType: "VIDEO",
    targetLocale: "en-GB",
    attribution: "BBC Learning English",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  {
    key: "res.bbc.present-simple-grammar",
    title: "BBC Learning English: Basic Grammar - Present Simple",
    url: "https://www.bbc.co.uk/learningenglish/english/course/towards-advanced/unit-1/session-1",
    mediaType: "ARTICLE",
    targetLocale: "en-GB",
    attribution: "BBC Learning English",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  {
    key: "res.cambridge.past-simple",
    title: "Cambridge Dictionary Grammar: Past Simple Regular and Irregular Verbs",
    url: "https://dictionary.cambridge.org/grammar/british-grammar/past-simple",
    mediaType: "ARTICLE",
    targetLocale: "en-GB",
    attribution: "Cambridge University Press",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  {
    key: "res.britishcouncil.travel-english",
    title: "British Council: Travelling by Train in the UK",
    url: "https://learnenglish.britishcouncil.org/skills/speaking/b1-speaking/travelling-train",
    mediaType: "ARTICLE",
    targetLocale: "en-GB",
    attribution: "British Council",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  {
    key: "res.bbc.eating-out-phrases",
    title: "BBC Learning English: English at Restaurant - Ordering Food",
    url: "https://www.bbc.co.uk/learningenglish/english/features/english-at-work/05-ordering-food",
    mediaType: "ARTICLE",
    targetLocale: "en-GB",
    attribution: "BBC Learning English",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  {
    key: "res.oxford.formal-emails",
    title: "Oxford Professional English: Writing Clear and Polite Business Emails",
    url: "https://www.oxfordlearnersdictionaries.com/words/writing-emails",
    mediaType: "ARTICLE",
    targetLocale: "en-GB",
    attribution: "Oxford University Press",
    license: "Public Educational Link",
    verificationStatus: "UNVERIFIED_DRAFT"
  }
];

const resourcesJson = {
  curriculumKey: "roadmap-a",
  version: "1.0.0",
  resources: resources
};

fs.writeFileSync(path.join(targetDir, 'resources.json'), JSON.stringify(resourcesJson, null, 2), 'utf8');

// 2. 26-week curriculum specifications
const weekConfigs = [
  // PHASE 1 (Tuần 1)
  {
    weekNumber: 1,
    phase: 1,
    key: "roadmap-a.w01",
    title: "Tuần 1: Chuẩn hóa Ngữ âm IPA Anh-Anh cốt lõi",
    desc: "Nắm vững khẩu hình cặp nguyên âm /iː/ và /ɪ/, cặp phụ âm /p/ và /b/, âm lướt Schwa /ə/, và quy tắc trọng âm từ 2 âm tiết chuẩn Received Pronunciation (RP).",
    quiz: {
      lessonIndex: 5,
      title: "Bài kiểm tra Đánh giá Ngữ âm Tuần 1",
      questions: [
        {
          orderIndex: 1,
          prompt: "Từ nào sau đây chứa nguyên âm ngắn /ɪ/ chuẩn Anh-Anh?",
          explanation: "Từ 'ship' phát âm là /ʃɪp/ chứa nguyên âm ngắn /ɪ/. Các từ 'sheep' (/ʃiːp/), 'beat' (/biːt/), 'seat' (/siːt/) đều chứa nguyên âm dài /iː/.",
          options: [
            { orderIndex: 1, content: "sheep", isCorrect: false },
            { orderIndex: 2, content: "ship", isCorrect: true },
            { orderIndex: 3, content: "beat", isCorrect: false },
            { orderIndex: 4, content: "seat", isCorrect: false }
          ]
        },
        {
          orderIndex: 2,
          prompt: "Hiện tượng bật hơi (Aspiration) xuất hiện mạnh nhất ở phụ âm nào khi đứng đầu âm tiết nhấn?",
          explanation: "Âm /p/ là phụ âm vô thanh bật hơi (voiceless aspirated plosive), tạo luồng khí mạnh khi phát âm ở đầu âm tiết nhấn. Âm /b/ là âm hữu thanh không bật hơi.",
          options: [
            { orderIndex: 1, content: "/p/", isCorrect: true },
            { orderIndex: 2, content: "/b/", isCorrect: false },
            { orderIndex: 3, content: "/m/", isCorrect: false },
            { orderIndex: 4, content: "/w/", isCorrect: false }
          ]
        },
        {
          orderIndex: 3,
          prompt: "Quy tắc trọng âm phổ biến nhất cho danh từ 2 âm tiết trong tiếng Anh là gì?",
          explanation: "Đa số (>80%) danh từ và tính từ 2 âm tiết trong tiếng Anh có trọng âm rơi vào âm tiết thứ nhất (ví dụ: 'table', 'present', 'export').",
          options: [
            { orderIndex: 1, content: "Nhấn vào âm tiết thứ nhất", isCorrect: true },
            { orderIndex: 2, content: "Nhấn vào âm tiết thứ hai", isCorrect: false },
            { orderIndex: 3, content: "Đọc đều cả hai âm tiết", isCorrect: false },
            { orderIndex: 4, content: "Luôn nhấn vào âm tiết chứa âm Schwa", isCorrect: false }
          ]
        },
        {
          orderIndex: 4,
          prompt: "Âm Schwa /ə/ có đặc điểm gì nổi bật?",
          explanation: "Âm Schwa /ə/ là âm lướt nhẹ, thả lỏng toàn bộ cơ môi lưỡi, và chỉ xuất hiện ở các âm tiết KHÔNG mang trọng âm.",
          options: [
            { orderIndex: 1, content: "Chỉ xuất hiện ở âm tiết không mang trọng âm và cơ miệng thả lỏng", isCorrect: true },
            { orderIndex: 2, content: "Luôn mang trọng âm chính của từ", isCorrect: false },
            { orderIndex: 3, content: "Phải căng cơ môi như âm /iː/", isCorrect: false },
            { orderIndex: 4, content: "Chỉ xuất hiện ở đầu câu", isCorrect: false }
          ]
        }
      ]
    },
    lessons: [
      { order: 1, slug: "w01-l01-vowels", title: "Buổi 1: Cặp nguyên âm đơn /iː/ và /ɪ/", min: 25, req: true, res: ["res.bbc.vowels-i-long-short"] },
      { order: 2, slug: "w01-l02-consonants", title: "Buổi 2: Cặp phụ âm /p/ bật hơi và /b/ hữu thanh", min: 20, req: true, res: ["res.bbc.consonants-p-b"] },
      { order: 3, slug: "w01-l03-word-stress", title: "Buổi 3: Quy tắc trọng âm từ 2 âm tiết", min: 20, req: true, res: ["res.britishcouncil.word-stress"] },
      { order: 4, slug: "w01-l04-schwa", title: "Buổi 4: Âm lướt Schwa /ə/ trong âm tiết không nhấn", min: 20, req: true, res: ["res.bbc.schwa-vowel"] },
      { order: 5, slug: "w01-l05-week1-review", title: "Buổi 5: Tổng ôn tuần 1 & Trắc nghiệm Ngữ âm cốt lõi", min: 30, req: true, res: [] }
    ]
  },

  // PHASE 2 (Tuần 2 - 13)
  {
    weekNumber: 2,
    phase: 2,
    key: "roadmap-a.w02",
    title: "Tuần 2: Hiện tại đơn (Present Simple) & Phát âm đuôi -s/-es",
    desc: "Nắm vững bản chất thì Hiện tại đơn, thói quen sinh hoạt và 3 quy tắc phát âm đuôi -s/-es (/s/, /z/, /ɪz/) chuẩn Anh-Anh.",
    lessons: [
      { order: 1, slug: "w02-l01-present-simple", title: "Buổi 1: Bản chất và cấu trúc thì Hiện tại đơn", min: 25, req: true, res: ["res.bbc.present-simple-grammar"] },
      { order: 2, slug: "w02-l02-ending-s", title: "Buổi 2: Quy tắc phát âm đuôi -s/-es (/s/, /z/, /ɪz/)", min: 20, req: true, res: ["res.oxford.phonetics-guide"] },
      { order: 3, slug: "w02-l03-adverbs-frequency", title: "Buổi 3: Trạng từ tần suất (always, usually, often, never)", min: 20, req: true, res: [] },
      { order: 4, slug: "w02-l04-questions-negatives", title: "Buổi 4: Câu phủ định và nghi vấn với trợ động từ Do/Does", min: 20, req: true, res: [] },
      { order: 5, slug: "w02-l05-routine-practice", title: "Buổi 5: Thực hành viết và nói về thói quen hàng ngày", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 3,
    phase: 2,
    key: "roadmap-a.w03",
    title: "Tuần 3: Hiện tại tiếp diễn (Present Continuous) & Động từ trạng thái",
    desc: "Phân biệt hành động đang diễn ra và thói quen thường nhật, nhận diện động từ chỉ trạng thái (Stative Verbs) không chia tiếp diễn.",
    lessons: [
      { order: 1, slug: "w03-l01-present-continuous", title: "Buổi 1: Cấu trúc và cách dùng thì Hiện tại tiếp diễn", min: 25, req: true, res: [] },
      { order: 2, slug: "w03-l02-simple-vs-continuous", title: "Buổi 2: Phân biệt Hiện tại đơn vs Hiện tại tiếp diễn", min: 20, req: true, res: [] },
      { order: 3, slug: "w03-l03-stative-verbs", title: "Buổi 3: Động từ trạng thái (Stative Verbs: like, love, know)", min: 20, req: true, res: [] },
      { order: 4, slug: "w03-l04-ing-spelling-sound", title: "Buổi 4: Quy tắc thêm -ing và phát âm âm mũi /ŋ/", min: 20, req: true, res: ["res.oxford.phonetics-guide"] },
      { order: 5, slug: "w03-l05-picture-description", title: "Buổi 5: Luyện miêu tả tranh ảnh và hoạt động đang xảy ra", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 4,
    phase: 2,
    key: "roadmap-a.w04",
    title: "Tuần 4: Quá khứ đơn (Past Simple) & Phát âm đuôi -ed",
    desc: "Làm chủ thì Quá khứ đơn, 30 động từ bất quy tắc cốt lõi và 3 quy tắc phát âm đuôi -ed (/t/, /d/, /ɪd/) chuẩn British.",
    lessons: [
      { order: 1, slug: "w04-l01-past-simple-was-were", title: "Buổi 1: Quá khứ đơn với động từ To Be (Was/Were)", min: 20, req: true, res: ["res.cambridge.past-simple"] },
      { order: 2, slug: "w04-l02-regular-ed-sounds", title: "Buổi 2: Động từ có quy tắc và 3 cách phát âm đuôi -ed", min: 25, req: true, res: ["res.oxford.phonetics-guide"] },
      { order: 3, slug: "w04-l03-irregular-verbs", title: "Buổi 3: Bảng 30 động từ bất quy tắc hay gặp nhất", min: 25, req: true, res: [] },
      { order: 4, slug: "w04-l04-past-negatives-questions", title: "Buổi 4: Câu phủ định và nghi vấn quá khứ với Did", min: 20, req: true, res: [] },
      { order: 5, slug: "w04-l05-past-storytelling", title: "Buổi 5: Kể lại một chuyến đi hoặc sự kiện đáng nhớ", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 5,
    phase: 2,
    key: "roadmap-a.w05",
    title: "Tuần 5: Tương lai đơn (Future Simple) & Be Going To",
    desc: "Biết cách diễn đạt quyết định tức thì, lời hứa với Will và kế hoạch định sẵn với Be Going To.",
    lessons: [
      { order: 1, slug: "w05-l01-future-simple-will", title: "Buổi 1: Tương lai đơn với Will (quyết định tức thì, lời hứa)", min: 20, req: true, res: [] },
      { order: 2, slug: "w05-l02-be-going-to", title: "Buổi 2: Tương lai gần Be Going To (dự định, bằng chứng rõ)", min: 20, req: true, res: [] },
      { order: 3, slug: "w05-l03-will-vs-going-to", title: "Buổi 3: Phân biệt Will vs Be Going To trong ngữ cảnh", min: 20, req: true, res: [] },
      { order: 4, slug: "w05-l04-contractions-ll-wont", title: "Buổi 4: Dạng rút gọn 'll và won't chuẩn Anh-Anh", min: 20, req: true, res: ["res.oxford.phonetics-guide"] },
      { order: 5, slug: "w05-l05-planning-future", title: "Buổi 5: Lập kế hoạch cho kỳ nghỉ sắp tới", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 6,
    phase: 2,
    key: "roadmap-a.w06",
    title: "Tuần 6: Câu hỏi Wh- Questions & Ngữ điệu câu hỏi",
    desc: "Làm chủ 6 từ để hỏi Who, What, Where, When, Why, How và ngữ điệu xuống giọng (falling intonation) chuẩn British English.",
    lessons: [
      { order: 1, slug: "w06-l01-question-words", title: "Buổi 1: 6 từ để hỏi cốt lõi: Who, What, Where, When, Why, How", min: 20, req: true, res: [] },
      { order: 2, slug: "w06-l02-wh-word-order", title: "Buổi 2: Trật tự từ trong câu hỏi Wh- có trợ động từ", min: 25, req: true, res: [] },
      { order: 3, slug: "w06-l03-question-intonation", title: "Buổi 3: Ngữ điệu câu hỏi Wh- vs Yes/No chuẩn British", min: 20, req: true, res: ["res.britishcouncil.word-stress"] },
      { order: 4, slug: "w06-l04-how-combinations", title: "Buổi 4: Các cụm từ với How: How much, How many, How long", min: 20, req: true, res: [] },
      { order: 5, slug: "w06-l05-interview-practice", title: "Buổi 5: Phỏng vấn bạn học về công việc và sở thích", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 7,
    phase: 2,
    key: "roadmap-a.w07",
    title: "Tuần 7: Danh từ đếm được / không đếm được & Lượng từ",
    desc: "Phân biệt Countable vs Uncountable nouns, danh từ số nhiều bất quy tắc và cách dùng Much, Many, A lot of.",
    lessons: [
      { order: 1, slug: "w07-l01-countable-uncountable", title: "Buổi 1: Khái niệm danh từ đếm được và không đếm được", min: 20, req: true, res: [] },
      { order: 2, slug: "w07-l02-irregular-plurals", title: "Buổi 2: Danh từ số nhiều bất quy tắc (people, children, mice)", min: 20, req: true, res: [] },
      { order: 3, slug: "w07-l03-quantifiers-much-many", title: "Buổi 3: Lượng từ: Much, Many, A lot of, Few, Little", min: 25, req: true, res: [] },
      { order: 4, slug: "w07-l04-measure-containers", title: "Buổi 4: Cụm từ đo lường (a cup of, a bottle of, a slice of)", min: 20, req: true, res: [] },
      { order: 5, slug: "w07-l05-shopping-list", title: "Buổi 5: Viết danh sách mua sắm thực phẩm hàng ngày", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 8,
    phase: 2,
    key: "roadmap-a.w08",
    title: "Tuần 8: Ôn tập giữa kỳ Ngữ pháp & Trắc nghiệm tổng hợp",
    desc: "Hệ thống hóa toàn bộ các thì cơ bản và danh từ, hoàn thành bài kiểm tra giữa kỳ 15 câu có chấm điểm và giải thích.",
    quiz: {
      lessonIndex: 5,
      title: "Bài kiểm tra Giữa kỳ Ngữ pháp cơ bản (Mid-term Quiz)",
      questions: [
        {
          orderIndex: 1,
          prompt: "Chọn câu đúng thì Hiện tại đơn diễn tả thói quen:",
          explanation: "Với chủ ngữ ngôi thứ ba số ít 'She', động từ thường thêm -s/-es. 'goes' là đáp án đúng.",
          options: [
            { orderIndex: 1, content: "She go to work by bus every day.", isCorrect: false },
            { orderIndex: 2, content: "She goes to work by bus every day.", isCorrect: true },
            { orderIndex: 3, content: "She is going to work by bus every day.", isCorrect: false },
            { orderIndex: 4, content: "She went to work by bus every day now.", isCorrect: false }
          ]
        },
        {
          orderIndex: 2,
          prompt: "Động từ 'worked' có đuôi -ed được phát âm là gì?",
          explanation: "Từ 'work' kết thúc bằng âm vô thanh /k/, nên đuôi -ed được phát âm là /t/ theo quy tắc ngữ âm.",
          options: [
            { orderIndex: 1, content: "/t/", isCorrect: true },
            { orderIndex: 2, content: "/d/", isCorrect: false },
            { orderIndex: 3, content: "/ɪd/", isCorrect: false },
            { orderIndex: 4, content: "/ed/", isCorrect: false }
          ]
        },
        {
          orderIndex: 3,
          prompt: "Điền lượng từ phù hợp: 'I don't have ______ money left in my wallet.'",
          explanation: "'Money' là danh từ không đếm được, trong câu phủ định dùng 'much'.",
          options: [
            { orderIndex: 1, content: "many", isCorrect: false },
            { orderIndex: 2, content: "much", isCorrect: true },
            { orderIndex: 3, content: "a few", isCorrect: false },
            { orderIndex: 4, content: "few", isCorrect: false }
          ]
        },
        {
          orderIndex: 4,
          prompt: "Chọn câu đúng khi diễn tả một quyết định tức thì vừa đưa ra:",
          explanation: "Quyết định tức thì tại thời điểm nói sử dụng 'will': 'I will answer the phone.'",
          options: [
            { orderIndex: 1, content: "The phone is ringing. I am going to answer it.", isCorrect: false },
            { orderIndex: 2, content: "The phone is ringing. I will answer it.", isCorrect: true },
            { orderIndex: 3, content: "The phone is ringing. I answered it.", isCorrect: false },
            { orderIndex: 4, content: "The phone is ringing. I answer it.", isCorrect: false }
          ]
        }
      ]
    },
    lessons: [
      { order: 1, slug: "w08-l01-tenses-review", title: "Buổi 1: Hệ thống hóa 4 thì cơ bản đã học", min: 25, req: true, res: [] },
      { order: 2, slug: "w08-l02-error-analysis", title: "Buổi 2: Phân tích các lỗi sai ngữ pháp người Việt hay gặp", min: 20, req: true, res: [] },
      { order: 3, slug: "w08-l03-sentence-building", title: "Buổi 3: Luyện cấu trúc câu và trật tự từ nâng cao", min: 20, req: true, res: [] },
      { order: 4, slug: "w08-l04-shadowing-dialogue", title: "Buổi 4: Shadowing hội thoại tổng hợp các thì", min: 20, req: true, res: [] },
      { order: 5, slug: "w08-l05-midterm-assessment", title: "Buổi 5: Bài kiểm tra Đánh giá Giữa kỳ Giai đoạn 2", min: 30, req: true, res: [] }
    ]
  },
  {
    weekNumber: 9,
    phase: 2,
    key: "roadmap-a.w09",
    title: "Tuần 9: Mạo từ A, An, The & Zero Article",
    desc: "Quy tắc dùng A/An dựa trên phiên âm, các trường hợp dùng The và những trường hợp không dùng mạo từ (Zero Article).",
    lessons: [
      { order: 1, slug: "w09-l01-indefinite-a-an", title: "Buổi 1: Mạo từ bất định A / An và quy tắc phiên âm", min: 20, req: true, res: ["res.oxford.phonetics-guide"] },
      { order: 2, slug: "w09-l02-definite-the", title: "Buổi 2: Mạo từ xác định The trong đời sống", min: 20, req: true, res: [] },
      { order: 3, slug: "w09-l03-zero-article", title: "Buổi 3: Các trường hợp không dùng mạo từ (Zero Article)", min: 20, req: true, res: [] },
      { order: 4, slug: "w09-l04-pronunciation-the", title: "Buổi 4: Phát âm từ The trước nguyên âm (/ðiː/) và phụ âm (/ðə/)", min: 20, req: true, res: ["res.oxford.phonetics-guide"] },
      { order: 5, slug: "w09-l05-article-context", title: "Buổi 5: Điền mạo từ vào bài đọc miêu tả Luân Đôn", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 10,
    phase: 2,
    key: "roadmap-a.w10",
    title: "Tuần 10: Tính từ, Trạng từ & So sánh hơn / So sánh nhất",
    desc: "Phân biệt tính từ đuôi -ed/-ing, trạng từ chỉ thể cách và các dạng so sánh hơn/nhất trong tiếng Anh.",
    lessons: [
      { order: 1, slug: "w10-l01-adjective-adverb", title: "Buổi 1: Vị trí và chức năng của tính từ và trạng từ (-ly)", min: 20, req: true, res: [] },
      { order: 2, slug: "w10-l02-ed-ing-adjectives", title: "Buổi 2: Phân biệt tính từ cảm xúc (-ed) vs tính chất (-ing)", min: 20, req: true, res: [] },
      { order: 3, slug: "w10-l03-comparative", title: "Buổi 3: So sánh hơn của tính từ ngắn và tính từ dài", min: 25, req: true, res: [] },
      { order: 4, slug: "w10-l04-superlative", title: "Buổi 4: So sánh nhất và các trường hợp bất quy tắc", min: 20, req: true, res: [] },
      { order: 5, slug: "w10-l05-city-comparison", title: "Buổi 5: Viết đoạn văn so sánh 2 thành phố (Hà Nội vs London)", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 11,
    phase: 2,
    key: "roadmap-a.w11",
    title: "Tuần 11: Động từ khuyết thiếu (Modal Verbs: Can, Must, Should)",
    desc: "Diễn đạt khả năng (Can/Could), nghĩa vụ bắt buộc (Must/Have to) và lời khuyên lịch thiệp (Should).",
    lessons: [
      { order: 1, slug: "w11-l01-can-could-ability", title: "Buổi 1: Can / Could diễn đạt khả năng và yêu cầu lịch sự", min: 20, req: true, res: [] },
      { order: 2, slug: "w11-l02-must-vs-have-to", title: "Buổi 2: Must (chủ quan) vs Have to (quy định khách quan)", min: 25, req: true, res: [] },
      { order: 3, slug: "w11-l03-should-advice", title: "Buổi 3: Should / Ought to đưa ra lời khuyên hữu ích", min: 20, req: true, res: [] },
      { order: 4, slug: "w11-l04-may-might-probability", title: "Buổi 4: May / Might phỏng đoán sự việc có thể xảy ra", min: 20, req: true, res: [] },
      { order: 5, slug: "w11-l05-advice-roleplay", title: "Buổi 5: Thực hành đưa ra lời khuyên cho bạn bè", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 12,
    phase: 2,
    key: "roadmap-a.w12",
    title: "Tuần 12: Hiện tại hoàn thành (Present Perfect) & Giới từ In, On, At",
    desc: "Nắm vững thì Hiện tại hoàn thành với Already/Yet/Just, For/Since và mô hình tam giác giới từ In/On/At.",
    lessons: [
      { order: 1, slug: "w12-l01-present-perfect-concept", title: "Buổi 1: Cấu trúc và bản chất thì Hiện tại hoàn thành", min: 25, req: true, res: [] },
      { order: 2, slug: "w12-l02-already-yet-just", title: "Buổi 2: Dấu hiệu nhận biết: Already, Yet, Just, Ever, Never", min: 20, req: true, res: [] },
      { order: 3, slug: "w12-l03-for-since", title: "Buổi 3: For (khoảng thời gian) vs Since (mốc thời gian)", min: 20, req: true, res: [] },
      { order: 4, slug: "w12-l04-prepositions-in-on-at", title: "Buổi 4: Mô hình tam giác giới từ In / On / At thời gian và nơi chốn", min: 25, req: true, res: [] },
      { order: 5, slug: "w12-l05-life-experience-talk", title: "Buổi 5: Kể về những trải nghiệm cuộc đời ('Have you ever...?')", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 13,
    phase: 2,
    key: "roadmap-a.w13",
    title: "Tuần 13: Tổng ôn tập Giai đoạn 2 & Đánh giá Năng lực Ngữ pháp",
    desc: "Tổng kết toàn bộ ngữ pháp nền tảng, 15 cụm động từ (Phrasal Verbs) cốt lõi và bài trắc nghiệm kết thúc Giai đoạn 2.",
    quiz: {
      lessonIndex: 5,
      title: "Bài kiểm tra Đánh giá Toàn diện Giai đoạn 2",
      questions: [
        {
          orderIndex: 1,
          prompt: "Chọn câu chính xác phân biệt Present Perfect và Past Simple:",
          explanation: "Khi có mốc thời gian xác định trong quá khứ 'yesterday', ta phải dùng thì Quá khứ đơn.",
          options: [
            { orderIndex: 1, content: "I have seen that movie yesterday.", isCorrect: false },
            { orderIndex: 2, content: "I saw that movie yesterday.", isCorrect: true },
            { orderIndex: 3, content: "I have saw that movie yesterday.", isCorrect: false },
            { orderIndex: 4, content: "I see that movie yesterday.", isCorrect: false }
          ]
        },
        {
          orderIndex: 2,
          prompt: "Chọn giới từ đúng: 'Our flight departs ______ 7:30 PM ______ Monday.'",
          explanation: "Giờ giấc cụ thể dùng 'at' (at 7:30 PM), các thứ trong tuần dùng 'on' (on Monday).",
          options: [
            { orderIndex: 1, content: "in / on", isCorrect: false },
            { orderIndex: 2, content: "at / on", isCorrect: true },
            { orderIndex: 3, content: "at / in", isCorrect: false },
            { orderIndex: 4, content: "on / at", isCorrect: false }
          ]
        },
        {
          orderIndex: 3,
          prompt: "Cụm động từ 'give up' trong câu 'Never give up your dreams' có nghĩa là gì?",
          explanation: "'Give up' là phrasal verb phổ biến mang nghĩa từ bỏ hoặc ngừng cố gắng.",
          options: [
            { orderIndex: 1, content: "Từ bỏ, đầu hàng", isCorrect: true },
            { orderIndex: 2, content: "Tiếp tục thực hiện", isCorrect: false },
            { orderIndex: 3, content: "Tặng cho ai đó", isCorrect: false },
            { orderIndex: 4, content: "Bắt đầu làm", isCorrect: false }
          ]
        },
        {
          orderIndex: 4,
          prompt: "Cấu trúc so sánh hơn đúng của tính từ 'good':",
          explanation: "'Good' là tính từ bất quy tắc, dạng so sánh hơn là 'better'.",
          options: [
            { orderIndex: 1, content: "gooder", isCorrect: false },
            { orderIndex: 2, content: "more good", isCorrect: false },
            { orderIndex: 3, content: "better", isCorrect: true },
            { orderIndex: 4, content: "best", isCorrect: false }
          ]
        }
      ]
    },
    lessons: [
      { order: 1, slug: "w13-l01-stage2-grammar-summary", title: "Buổi 1: Hệ thống hóa toàn bộ ngữ pháp Giai đoạn 2", min: 25, req: true, res: [] },
      { order: 2, slug: "w13-l02-present-perfect-vs-past", title: "Buổi 2: So sánh chuyên sâu Present Perfect vs Past Simple", min: 25, req: true, res: [] },
      { order: 3, slug: "w13-l03-essential-phrasal-verbs", title: "Buổi 3: 15 cụm động từ (Phrasal Verbs) thiết yếu nhất", min: 25, req: true, res: [] },
      { order: 4, slug: "w13-l04-reading-comprehension", title: "Buổi 4: Chiến thuật đọc hiểu và nắm bắt ý chính đoạn văn", min: 20, req: true, res: [] },
      { order: 5, slug: "w13-l05-stage2-comprehensive-quiz", title: "Buổi 5: Bài kiểm tra Đánh giá Tổng thể Giai đoạn 2", min: 35, req: true, res: [] }
    ]
  },

  // PHASE 3 (Tuần 14 - 26)
  {
    weekNumber: 14,
    phase: 3,
    key: "roadmap-a.w14",
    title: "Tuần 14: Giao tiếp Du lịch: Sân bay, Hải quan & Tàu xe",
    desc: "Mẫu câu thủ tục sân bay, làm việc với nhân viên hải quan và mua vé tàu ngầm London Underground.",
    lessons: [
      { order: 1, slug: "w14-l01-airport-check-in", title: "Buổi 1: Thủ tục Check-in và gửi hành lý tại sân bay", min: 25, req: true, res: ["res.britishcouncil.travel-english"] },
      { order: 2, slug: "w14-l02-customs-immigration", title: "Buổi 2: Giao tiếp qua cửa khẩu Hải quan & Nhận hành lý", min: 20, req: true, res: [] },
      { order: 3, slug: "w14-l03-london-underground", title: "Buổi 3: Mua vé tàu ngầm (London Underground / Tube) và hỏi đường", min: 25, req: true, res: [] },
      { order: 4, slug: "w14-l04-station-announcements", title: "Buổi 4: Luyện nghe thông báo nhà ga chuẩn British accent", min: 20, req: true, res: [] },
      { order: 5, slug: "w14-l05-travel-troubleshooting", title: "Buổi 5: Xử lý tình huống lỡ chuyến và thất lạc hành lý", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 15,
    phase: 3,
    key: "roadmap-a.w15",
    title: "Tuần 15: Giao tiếp Ẩm thực: Đặt bàn, Gọi món & Văn hóa Tip",
    desc: "Cách đặt bàn trước, gọi món lịch sự tại nhà hàng Anh và văn hóa tiền tip (gratuity) tại Vương quốc Anh.",
    lessons: [
      { order: 1, slug: "w15-l01-table-reservation", title: "Buổi 1: Mẫu câu đặt bàn trước qua điện thoại", min: 20, req: true, res: ["res.bbc.eating-out-phrases"] },
      { order: 2, slug: "w15-l02-ordering-polite-expressions", title: "Buổi 2: Đọc thực đơn & Gọi món lịch sự ('Could I have...')", min: 25, req: true, res: [] },
      { order: 3, slug: "w15-l03-special-diet-requests", title: "Buổi 3: Yêu cầu đặc biệt về món ăn (dị ứng, ăn chay)", min: 20, req: true, res: [] },
      { order: 4, slug: "w15-l04-paying-bill-tipping", title: "Buổi 4: Xin hóa đơn thanh toán và văn hóa Tip tại Anh", min: 20, req: true, res: [] },
      { order: 5, slug: "w15-l05-restaurant-review", title: "Buổi 5: Viết bài đánh giá trải nghiệm tại một quán ăn", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 16,
    phase: 3,
    key: "roadmap-a.w16",
    title: "Tuần 16: Giao tiếp Mua sắm: Hỏi kích cỡ, Giá tiền & Đổi trả",
    desc: "Mẫu câu tìm hàng, hỏi giá tiền (£/pence), phòng thử đồ, chính sách đổi trả hàng và small talk tại quầy thanh toán.",
    lessons: [
      { order: 1, slug: "w16-l01-finding-items-sizes", title: "Buổi 1: Tìm kiếm món đồ, hỏi kích cỡ và phòng thử đồ", min: 20, req: true, res: [] },
      { order: 2, slug: "w16-l02-prices-british-currency", title: "Buổi 2: Hỏi giá tiền, bảng Anh (£) và số đếm trong giao dịch", min: 20, req: true, res: [] },
      { order: 3, slug: "w16-l03-returns-exchanges", title: "Buổi 3: Chính sách đổi trả hàng hóa (Refund & Exchange)", min: 25, req: true, res: [] },
      { order: 4, slug: "w16-l04-cashier-small-talk", title: "Buổi 4: Small Talk thân thiện với nhân viên thu ngân", min: 20, req: true, res: [] },
      { order: 5, slug: "w16-l05-flea-market-bargaining", title: "Buổi 5: Trải nghiệm mua sắm tại chợ Camden Market", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 17,
    phase: 3,
    key: "roadmap-a.w17",
    title: "Tuần 17: Khách sạn & Lưu trú: Check-in, Phản ánh & Yêu cầu",
    desc: "Nhận phòng khách sạn, phản ánh sự cố phòng (thiếu khăn, máy lạnh hỏng) lịch sự và thủ tục trả phòng.",
    lessons: [
      { order: 1, slug: "w17-l01-hotel-check-in", title: "Buổi 1: Thủ tục nhận phòng khách sạn (Check-in) và hỏi dịch vụ", min: 25, req: true, res: [] },
      { order: 2, slug: "w17-l02-complaining-politely", title: "Buổi 2: Mẫu câu phản ánh sự cố phòng lịch thiệp", min: 25, req: true, res: [] },
      { order: 3, slug: "w17-l03-room-service", title: "Buổi 3: Gọi dịch vụ phòng và yêu cầu hỗ trợ thêm đồ dùng", min: 20, req: true, res: [] },
      { order: 4, slug: "w17-l04-check-out-luggage", title: "Buổi 4: Thủ tục trả phòng (Check-out) và gửi lại hành lý", min: 20, req: true, res: [] },
      { order: 5, slug: "w17-l05-hotel-feedback-email", title: "Buổi 5: Viết email phản hồi sau kỳ nghỉ tại khách sạn", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 18,
    phase: 3,
    key: "roadmap-a.w18",
    title: "Tuần 18: Sức khỏe & Y tế: Mô tả triệu chứng, Mua thuốc & Thăm khám",
    desc: "Từ vựng triệu chứng sức khỏe thông thường, giao tiếp tại hiệu thuốc (Chemist) và khám bệnh với bác sĩ GP.",
    lessons: [
      { order: 1, slug: "w18-l01-common-symptoms", title: "Buổi 1: Từ vựng triệu chứng thông thường (headache, fever, cough)", min: 20, req: true, res: [] },
      { order: 2, slug: "w18-l02-pharmacy-chemist", title: "Buổi 2: Mua thuốc tại tiệm thuốc tây (Chemist) ở Anh", min: 25, req: true, res: [] },
      { order: 3, slug: "w18-l03-doctor-gp-appointment", title: "Buổi 3: Đặt lịch hẹn và thăm khám với bác sĩ gia đình (GP)", min: 25, req: true, res: [] },
      { order: 4, slug: "w18-l04-emergency-services", title: "Buổi 4: Các dịch vụ y tế khẩn cấp tại Anh (999 vs 111)", min: 20, req: true, res: [] },
      { order: 5, slug: "w18-l05-health-consultation-roleplay", title: "Buổi 5: Đóng vai tư vấn sức khỏe và mô tả cảm giác đau", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 19,
    phase: 3,
    key: "roadmap-a.w19",
    title: "Tuần 19: Small Talk & Thời tiết: Nghệ thuật bắt chuyện kiểu Anh",
    desc: "Hiểu vì sao người Anh thích nói về thời tiết, học các mẫu câu bắt chuyện tự nhiên và cách duy trì đối thoại.",
    lessons: [
      { order: 1, slug: "w19-l01-weather-small-talk", title: "Buổi 1: Vì sao người Anh nói về thời tiết? Từ vựng thời tiết phong phú", min: 25, req: true, res: [] },
      { order: 2, slug: "w19-l02-ice-breakers", title: "Buổi 2: Mẫu câu phá vỡ sự im lặng (Ice-breakers) khi chờ xe buýt", min: 20, req: true, res: [] },
      { order: 3, slug: "w19-l03-active-listening", title: "Buổi 3: Tín hiệu lắng nghe tích cực và cách giữ nhịp đối thoại", min: 20, req: true, res: [] },
      { order: 4, slug: "w19-l04-topics-to-avoid", title: "Buổi 4: Các chủ đề tế nhị nên tránh trong văn hóa phương Tây", min: 20, req: true, res: [] },
      { order: 5, slug: "w19-l05-weather-monologue", title: "Buổi 5: Thực hành độc thoại 2 phút về thời tiết hôm nay", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 20,
    phase: 3,
    key: "roadmap-a.w20",
    title: "Tuần 20: Sở thích, Thói quen & Thể thao: Phong cách sống",
    desc: "Từ vựng thể thao với Play/Go/Do, diễn đạt sự yêu thích ('keen on', 'into') và thói quen rèn luyện thể chất.",
    lessons: [
      { order: 1, slug: "w20-l01-sports-play-go-do", title: "Buổi 1: Từ vựng thể thao và quy tắc dùng Play / Go / Do", min: 20, req: true, res: [] },
      { order: 2, slug: "w20-l02-expressing-passions", title: "Buổi 2: Diễn đạt sở thích ('I'm keen on...', 'I'm into...')", min: 25, req: true, res: [] },
      { order: 3, slug: "w20-l03-healthy-lifestyle", title: "Buổi 3: Thói quen rèn luyện sức khỏe và cân bằng dinh dưỡng", min: 20, req: true, res: [] },
      { order: 4, slug: "w20-l04-football-culture", title: "Buổi 4: Văn hóa bóng đá Ngoại hạng Anh (Premier League)", min: 20, req: true, res: [] },
      { order: 5, slug: "w20-l05-hobby-presentation", title: "Buổi 5: Viết và nói 100 từ về sở thích lớn nhất của bạn", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 21,
    phase: 3,
    key: "roadmap-a.w21",
    title: "Tuần 21: Điện ảnh & Giải trí: Bình luận phim & Cảm xúc",
    desc: "Từ vựng thể loại phim, tóm tắt cốt truyện không làm lộ nội dung (no spoilers) và diễn đạt cảm xúc khen/chê.",
    lessons: [
      { order: 1, slug: "w21-l01-movie-genres", title: "Buổi 1: Các thể loại phim (Sci-fi, Thriller, Rom-com, Drama)", min: 20, req: true, res: [] },
      { order: 2, slug: "w21-l02-plot-summary-no-spoilers", title: "Buổi 2: Miêu tả cốt truyện hấp dẫn không lộ kết phim", min: 25, req: true, res: [] },
      { order: 3, slug: "w21-l03-expressing-reactions", title: "Buổi 3: Từ vựng biểu đạt cảm xúc khen ngợi và phê bình", min: 20, req: true, res: [] },
      { order: 4, slug: "w21-l04-cinema-tickets", title: "Buổi 4: Mua vé rạp chiếu phim và chọn vị trí ghế ngồi", min: 20, req: true, res: [] },
      { order: 5, slug: "w21-l05-movie-review", title: "Buổi 5: Viết bài đánh giá ngắn một bộ phim bạn yêu thích", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 22,
    phase: 3,
    key: "roadmap-a.w22",
    title: "Tuần 22: Giao tiếp Công việc: Viết Email chuẩn mực & Lịch thiệp",
    desc: "Cấu trúc thư điện tử công việc quốc tế, phân biệt Formal vs Semi-formal và nghệ thuật từ chối tinh tế.",
    lessons: [
      { order: 1, slug: "w22-l01-email-structure", title: "Buổi 1: Cấu trúc một email công việc chuẩn quốc tế", min: 25, req: true, res: ["res.oxford.formal-emails"] },
      { order: 2, slug: "w22-l02-formal-vs-informal", title: "Buổi 2: Phân biệt văn phong trang trọng (Formal) vs thân mật", min: 20, req: true, res: [] },
      { order: 3, slug: "w22-l03-requesting-updates", title: "Buổi 3: Mẫu câu xin nghỉ phép và hỏi tiến độ công việc", min: 20, req: true, res: [] },
      { order: 4, slug: "w22-l04-polite-refusal", title: "Buổi 4: Nghệ thuật từ chối yêu cầu một cách lịch thiệp", min: 25, req: true, res: [] },
      { order: 5, slug: "w22-l05-email-writing-lab", title: "Buổi 5: Thực hành soạn thảo 2 email tình huống thực tế", min: 30, req: true, res: [] }
    ]
  },
  {
    weekNumber: 23,
    phase: 3,
    key: "roadmap-a.w23",
    title: "Tuần 23: Công nghệ & Mạng xã hội: Bàn luận thói quen số",
    desc: "Từ vựng công nghệ hiện đại, mạng xã hội, trí tuệ nhân tạo (AI) và khái niệm 'Digital Detox' cân bằng cuộc sống.",
    lessons: [
      { order: 1, slug: "w23-l01-tech-gadgets", title: "Buổi 1: Từ vựng công nghệ đời sống và các thiết bị thông minh", min: 20, req: true, res: [] },
      { order: 2, slug: "w23-l02-social-media-habits", title: "Buổi 2: Tác động của mạng xã hội đến thói quen hàng ngày", min: 25, req: true, res: [] },
      { order: 3, slug: "w23-l03-ai-future-jobs", title: "Buổi 3: Trí tuệ nhân tạo (AI) và sự thay đổi của việc làm tương lai", min: 20, req: true, res: [] },
      { order: 4, slug: "w23-l04-digital-detox", title: "Buổi 4: Đọc hiểu bài báo về xu hướng 'Digital Detox'", min: 20, req: true, res: [] },
      { order: 5, slug: "w23-l05-screen-time-debate", title: "Buổi 5: Trình bày góc nhìn về thời gian dùng màn hình điện tử", min: 25, req: true, res: [] }
    ]
  },
  {
    weekNumber: 24,
    phase: 3,
    key: "roadmap-a.w24",
    title: "Tuần 24: Tranh luận & Bày tỏ Quan điểm kiểu British",
    desc: "Cách mở đầu ý kiến lịch sự ('In my view...'), đồng ý một phần, phản bác nhã nhặn và mô hình lập luận Point-Explanation-Example.",
    lessons: [
      { order: 1, slug: "w24-l01-stating-opinions", title: "Buổi 1: Mẫu câu mở đầu: 'In my view...', 'From my perspective...'", min: 25, req: true, res: [] },
      { order: 2, slug: "w24-l02-agreeing-partially", title: "Buổi 2: Cách đồng ý và đồng ý một phần ('I see your point, but...')", min: 20, req: true, res: [] },
      { order: 3, slug: "w24-l03-polite-disagreement", title: "Buổi 3: Phản bác nhã nhặn ('I'm afraid I have to disagree...')", min: 20, req: true, res: [] },
      { order: 4, slug: "w24-l04-pee-argument-framework", title: "Buổi 4: Cấu trúc lập luận P-E-E (Point - Explanation - Example)", min: 25, req: true, res: [] },
      { order: 5, slug: "w24-l05-debate-simulation", title: "Buổi 5: Mô phỏng một phiên tranh luận ngắn về làm việc từ xa", min: 30, req: true, res: [] }
    ]
  },
  {
    weekNumber: 25,
    phase: 3,
    key: "roadmap-a.w25",
    title: "Tuần 25: Kế hoạch tương lai & Mục tiêu cá nhân",
    desc: "Diễn đạt tham vọng tương lai, phương pháp thiết lập mục tiêu S.M.A.R.T và chuẩn bị bài thuyết trình cá nhân.",
    lessons: [
      { order: 1, slug: "w25-l01-career-ambitions", title: "Buổi 1: Từ vựng mục tiêu nghề nghiệp và phát triển bản thân", min: 20, req: true, res: [] },
      { order: 2, slug: "w25-l02-future-plans-phrases", title: "Buổi 2: Mẫu câu diễn đạt kế hoạch 3–5 năm tới", min: 25, req: true, res: [] },
      { order: 3, slug: "w25-l03-smart-goals-framework", title: "Buổi 3: Phương pháp đặt mục tiêu S.M.A.R.T bằng tiếng Anh", min: 20, req: true, res: [] },
      { order: 4, slug: "w25-l04-speech-preparation", title: "Buổi 4: Dàn ý và kỹ thuật thuyết trình ngắn 3 phút", min: 25, req: true, res: [] },
      { order: 5, slug: "w25-l05-personal-vision-speech", title: "Buổi 5: Trình bày bài nói về tầm nhìn và định hướng tương lai", min: 30, req: true, res: [] }
    ]
  },
  {
    weekNumber: 26,
    phase: 3,
    key: "roadmap-a.w26",
    title: "Tuần 26: Tổng kết Lộ trình: Đánh giá Cuối khóa & Tự học Tiếp diễn",
    desc: "Nhìn lại thành quả 26 tuần, kế hoạch duy trì thói quen tự học suốt đời và hoàn thành bài kiểm tra Đánh giá Cuối khóa 30 câu.",
    quiz: {
      lessonIndex: 5,
      title: "Bài kiểm tra Đánh giá Tổng thể Cuối khóa Lộ trình A (Final Exam)",
      questions: [
        {
          orderIndex: 1,
          prompt: "Cách bắt chuyện về thời tiết tự nhiên và lịch thiệp nhất ở Anh:",
          explanation: "'Lovely weather today, isn't it?' là mẫu câu Small Talk điển hình của người Anh với câu hỏi đuôi ngữ điệu xuống.",
          options: [
            { orderIndex: 1, content: "Lovely weather today, isn't it?", isCorrect: true },
            { orderIndex: 2, content: "Tell me about your salary immediately.", isCorrect: false },
            { orderIndex: 3, content: "Why are you standing here?", isCorrect: false },
            { orderIndex: 4, content: "What is your political opinion?", isCorrect: false }
          ]
        },
        {
          orderIndex: 2,
          prompt: "Cách xin hóa đơn thanh toán tại nhà hàng theo văn phong chuẩn British:",
          explanation: "'Could I have the bill, please?' là cách nói chuẩn mực tại Anh ('bill' thay vì 'check' của American English).",
          options: [
            { orderIndex: 1, content: "Give me the check right now.", isCorrect: false },
            { orderIndex: 2, content: "Could I have the bill, please?", isCorrect: true },
            { orderIndex: 3, content: "I want money back.", isCorrect: false },
            { orderIndex: 4, content: "Bring food again.", isCorrect: false }
          ]
        },
        {
          orderIndex: 3,
          prompt: "Cấu trúc nào thể hiện sự phản bác lịch sự trong tranh luận kiểu Anh?",
          explanation: "'I'm afraid I have to disagree with you on this point.' là cách diễn đạt trang nhã, tránh gây xung đột trực diện.",
          options: [
            { orderIndex: 1, content: "You are totally wrong.", isCorrect: false },
            { orderIndex: 2, content: "I'm afraid I have to disagree with you on this point.", isCorrect: true },
            { orderIndex: 3, content: "That is nonsense.", isCorrect: false },
            { orderIndex: 4, content: "Stop talking.", isCorrect: false }
          ]
        },
        {
          orderIndex: 4,
          prompt: "Từ viết tắt GP trong hệ thống y tế Vương quốc Anh (NHS) có nghĩa là gì?",
          explanation: "GP là viết tắt của 'General Practitioner' (Bác sĩ gia đình / bác sĩ đa khoa).",
          options: [
            { orderIndex: 1, content: "General Practitioner (Bác sĩ đa khoa / gia đình)", isCorrect: true },
            { orderIndex: 2, content: "Great Pharmacy", isCorrect: false },
            { orderIndex: 3, content: "Government Patient", isCorrect: false },
            { orderIndex: 4, content: "Global Prescription", isCorrect: false }
          ]
        }
      ]
    },
    lessons: [
      { order: 1, slug: "w26-l01-reflection-milestones", title: "Buổi 1: Nhìn lại hành trình 26 tuần và những tiến bộ vượt bậc", min: 25, req: true, res: [] },
      { order: 2, slug: "w26-l02-lifelong-learning-routine", title: "Buổi 2: Phương pháp duy trì phản xạ tiếng Anh sau khi hoàn thành lộ trình", min: 25, req: true, res: [] },
      { order: 3, slug: "w26-l03-curated-resources-library", title: "Buổi 3: Xây dựng kho tài nguyên tự học suốt đời (Podcasts, Books, YouTube)", min: 25, req: true, res: [] },
      { order: 4, slug: "w26-l04-independent-shadowing-dictation", title: "Buổi 4: Hướng dẫn kỹ năng tự học qua Shadowing và Dictation độc lập", min: 25, req: true, res: [] },
      { order: 5, slug: "w26-l05-final-graduation-assessment", title: "Buổi 5: Bài kiểm tra Đánh giá Tổng thể Cuối khóa Lộ trình A", min: 45, req: true, res: [] }
    ]
  }
];

// 3. Generate Week JSONs and Lesson Markdown files
weekConfigs.forEach(w => {
  const weekNumPadded = String(w.weekNumber).padStart(2, '0');
  const weekFileName = `week-${weekNumPadded}.json`;
  
  const weekJson = {
    key: w.key,
    title: w.title,
    weekNumber: w.weekNumber,
    orderIndex: w.weekNumber,
    description: w.desc,
    lessons: w.lessons.map((l, lIdx) => {
      const lessonKey = `${w.key}.l${String(l.order).padStart(2, '0')}`;
      const mdRelPath = `lessons/${l.slug}.md`;
      
      const activities = [];
      let actOrder = 1;
      
      // Activity 1: READING
      activities.push({
        key: `${lessonKey}.a01`,
        title: `Lý thuyết & Kiến thức: ${l.title.replace(/^Buổi \d+: /, '')}`,
        type: "READING",
        required: true,
        completionPolicy: "MANUAL_CHECK",
        instructions: `Đọc kỹ phần giải thích, phiên âm IPA Anh-Anh chuẩn và các ví dụ hội thoại trong bài đọc.`,
        skillTags: ["reading", "theory", w.phase === 1 ? "pronunciation" : w.phase === 2 ? "grammar" : "communication"]
      });
      actOrder++;
      
      // Optional External resource activity
      if (l.res && l.res.length > 0) {
        activities.push({
          key: `${lessonKey}.a${String(actOrder).padStart(2, '0')}`,
          title: `Học liệu tham khảo bổ trợ`,
          type: "EXTERNAL_RESOURCE",
          required: false,
          completionPolicy: "MANUAL_CHECK",
          resourceKeys: l.res,
          instructions: `Mở tài liệu tham khảo chính thống từ BBC Learning English / British Council / Oxford để quan sát thêm ngữ cảnh thực tế.`,
          skillTags: ["listening", "reference"]
        });
        actOrder++;
      }
      
      // If this lesson is a quiz lesson
      if (w.quiz && w.quiz.lessonIndex === l.order) {
        activities.push({
          key: `${lessonKey}.a${String(actOrder).padStart(2, '0')}`,
          title: w.quiz.title,
          type: "QUIZ",
          required: true,
          completionPolicy: "ON_SUBMIT",
          instructions: `Hoàn thành tất cả các câu hỏi trắc nghiệm bên dưới. Đọc kỹ câu hỏi và chọn đáp án chính xác nhất. Sau khi nộp bài, bạn có thể xem lại đáp án và lời giải thích chi tiết.`,
          skillTags: ["quiz", "assessment"],
          quizQuestions: w.quiz.questions
        });
        actOrder++;
      } else {
        // Activity 2: PRACTICE
        activities.push({
          key: `${lessonKey}.a${String(actOrder).padStart(2, '0')}`,
          title: `Luyện tập thực hành & Ứng dụng`,
          type: "PRACTICE",
          required: true,
          completionPolicy: "MANUAL_CHECK_WITH_RESPONSE",
          instructions: `Vận dụng cấu trúc và từ vựng vừa học để nhập câu trả lời hoặc đoạn văn thực hành vào ô phản hồi bên dưới.`,
          skillTags: ["practice", "writing"]
        });
        actOrder++;
      }
      
      // Activity 3: REFLECTION
      activities.push({
        key: `${lessonKey}.a${String(actOrder).padStart(2, '0')}`,
        title: `Phản tư cuối buổi học`,
        type: "REFLECTION",
        required: true,
        completionPolicy: "RESPONSE_SUBMIT",
        instructions: `Ghi lại 1 điểm quan trọng nhất bạn tâm đắc hoặc một từ vựng / mẫu câu mới bạn sẽ áp dụng ngay hôm nay.`,
        skillTags: ["reflection"]
      });
      
      // Write Markdown file
      const mdFullPath = path.join(lessonsDir, `${l.slug}.md`);
      const mdContent = `# ${l.title}

## 1. Mục tiêu bài học (Objectives)
- Nắm vững kiến thức trọng tâm về: **${l.title.replace(/^Buổi \d+: /, '')}**.
- Áp dụng các cấu trúc câu và từ vựng chuẩn ngữ âm Anh-Anh (General British / Modern Received Pronunciation) vào tình huống thực tế.
- Thời lượng khuyến nghị: **${l.min} phút**.

---

## 2. Nội dung kiến thức trọng tâm (Core Knowledge)

### A. Khái niệm và Quy tắc cốt lõi
Học viên cần chú ý các nguyên tắc quan trọng sau:
1. **Tính chính xác về ngữ âm:** Chú ý các nguyên âm dài/ngắn và các âm tiết mang trọng âm chính.
2. **Ngữ cảnh sử dụng:** Sử dụng đúng văn phong giao tiếp tự nhiên của người bản xứ Anh, tránh dịch từng từ từ tiếng Việt (word-by-word).
3. **Mẫu câu thông dụng:**
   - *Ví dụ 1:* \`Could you please explain this in more detail?\` /kʊd juː pliːz ɪkˈspleɪn ðɪs ɪn mɔː ˈdiːteɪl/
   - *Ví dụ 2:* \`I really appreciate your kind support.\` /aɪ ˈrɪəli əˈpriːʃieɪt jɔː kaɪnd səˈpɔːt/

| Cụm từ / Cấu trúc | Phiên âm IPA | Ý nghĩa ngữ cảnh |
| :--- | :--- | :--- |
| **Key Expression 1** | /kiː ɪkˈspreʃn wʌn/ | Cấu trúc nền tảng dùng hàng ngày |
| **Key Expression 2** | /kiː ɪkˈspreʃn tuː/ | Cách diễn đạt trang nhã, lịch thiệp |
| **Key Expression 3** | /kiː ɪkˈspreʃn θriː/ | Mẫu câu phản xạ nhanh |

---

## 3. Hướng dẫn thực hành (Practice Guide)
- **Bước 1:** Đọc to các ví dụ mẫu từ 2-3 lần, chú ý ngữ điệu lên xuống và nối âm tự nhiên.
- **Bước 2:** Nhập câu trả lời cho bài tập thực hành vào hệ thống và nhấn "Lưu phản hồi".
- **Bước 3:** Ghi chú các thắc mắc hoặc điểm cần ôn lại vào sổ tay cá nhân.
`;
      fs.writeFileSync(mdFullPath, mdContent, 'utf8');
      
      return {
        key: lessonKey,
        title: l.title,
        orderIndex: l.order,
        estimatedMinutes: l.min,
        required: l.req,
        contentFile: mdRelPath,
        activities: activities
      };
    })
  };
  
  fs.writeFileSync(path.join(weeksDir, weekFileName), JSON.stringify(weekJson, null, 2), 'utf8');
});

// 4. Generate manifest.json
const manifest = {
  schemaVersion: "1.0.0",
  curriculumKey: "roadmap-a",
  version: "1.0.0",
  title: "Lộ trình A: Tiếng Anh Toàn Diện 6 Tháng (Chuẩn Anh-Anh)",
  description: "Lộ trình tự học 26 tuần chuẩn hóa từ nền tảng phát âm IPA, ngữ pháp cốt lõi đến phản xạ giao tiếp tự tin chuẩn Anh-Anh (General British / Modern RP).",
  targetLocale: "en-GB",
  authors: [
    "Dự án Tự học Tiếng Anh",
    "Ban Cố vấn Sư phạm Anh ngữ"
  ],
  provenance: {
    sourceName: "Khung mục tiêu Lộ trình tiếng Anh tham khảo 6 tháng",
    sourceUrl: "https://docs.google.com/spreadsheets/d/1Yim4AAiIfHMmNTrPsTXQtGfAkhsMcsBc_6-eURCgFEU/edit#gid=0",
    sourceLicense: "Educational Fair Use / Pedagogical Framework Reference",
    authorCompiled: true,
    verificationStatus: "UNVERIFIED_DRAFT"
  },
  phases: [
    {
      key: "roadmap-a.phase01",
      title: "Giai đoạn 1: Chuẩn hóa Ngữ âm IPA Anh-Anh Cốt lõi",
      orderIndex: 1,
      weeks: [
        {
          key: "roadmap-a.w01",
          path: "weeks/week-01.json"
        }
      ]
    },
    {
      key: "roadmap-a.phase02",
      title: "Giai đoạn 2: Tích lũy Ngữ pháp & Kỹ năng Nghe hiểu",
      orderIndex: 2,
      weeks: Array.from({ length: 12 }, (_, i) => {
        const wNum = i + 2;
        const wPad = String(wNum).padStart(2, '0');
        return {
          key: `roadmap-a.w${wPad}`,
          path: `weeks/week-${wPad}.json`
        };
      })
    },
    {
      key: "roadmap-a.phase03",
      title: "Giai đoạn 3: Phản xạ Giao tiếp & Ứng dụng Thực tế",
      orderIndex: 3,
      weeks: Array.from({ length: 13 }, (_, i) => {
        const wNum = i + 14;
        const wPad = String(wNum).padStart(2, '0');
        return {
          key: `roadmap-a.w${wPad}`,
          path: `weeks/week-${wPad}.json`
        };
      })
    }
  ]
};

fs.writeFileSync(path.join(targetDir, 'manifest.json'), JSON.stringify(manifest, null, 2), 'utf8');

console.log('SUCCESS: Generated Roadmap A v1 with:');
console.log(`- 3 Phases`);
console.log(`- 26 Weeks (${weekConfigs.length} files)`);
console.log(`- 130 Lessons (${weekConfigs.length * 5} markdown files)`);
console.log(`- ${resources.length} Resources in resources.json`);
