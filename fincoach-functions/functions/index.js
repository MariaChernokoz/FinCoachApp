const { setGlobalOptions } = require("firebase-functions/v2");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const axios = require("axios");
const https = require("https");
const { randomUUID } = require("crypto");

admin.initializeApp();
const db = admin.firestore();

//обход проверки SSL-сертификата
const sberAgent = new https.Agent({ rejectUnauthorized: false });

setGlobalOptions({ region: "us-central1", maxInstances: 10 });

const AUTH_KEY = process.env.GIGACHAT_AUTH_KEY;
const RATE_LIMIT_MS = 3000; //защита от спама
const MAX_MESSAGE_LENGTH = 500;
const GIGACHAT_FALLBACK = "Упс, я временно вне зоны доступа. Попробуй чуть позже — я никуда не ухожу!";

// Категории которые нельзя советовать сокращать
const FIXED_CATEGORY_KEYWORDS = [
    "жильё",           // → "Жильё"
    "жилье",           // → вариант без ударения, на всякий случай
    "аренда",          // → "Аренда", и входит в "Жильё и аренда"
    "коммунальн",      // → "Коммунальные" (обрезаем — includes() найдёт подстроку)
    "связь",
    "страховка",
    "налог",
    "ипотека",
    "кредит",
];

function isFixed(title) {
    const lower = (title ?? "").toLowerCase();
    return FIXED_CATEGORY_KEYWORDS.some(k => lower.includes(k));
}

function fmt(n) {
    return Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, " "); // Форматирование чисел
}

// ─── Token Cache ───────────── in-memory кэш токена OAuth ───────────────────────
let _tokenCache = { value: null, expiresAt: 0 };

async function getGigaToken() {
    if (_tokenCache.value && _tokenCache.expiresAt - Date.now() > 60_000) {
        return _tokenCache.value;
    }
    if (!AUTH_KEY) throw new Error("GIGACHAT_AUTH_KEY is not configured");

    const response = await axios.request({
        method: "post",
        maxBodyLength: Infinity,
        url: "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "Accept": "application/json",
            "RqUID": randomUUID(),
            "Authorization": `Basic ${AUTH_KEY}`
        },
        data: new URLSearchParams({ scope: "GIGACHAT_API_PERS" }).toString(),
        httpsAgent: sberAgent
    });

    const expiresIn = response.data.expires_in ?? 1800; //30 мин
    _tokenCache = {
        value: response.data.access_token,
        expiresAt: Date.now() + expiresIn * 1000
    };
    return _tokenCache.value;
}

// ─── Intent Detection ──────────────────────────────────────────────────────────
const INTENT_PATTERNS = {
    budget: [
        /бюджет/i,
        /лимит/i,
        /превыси/i,
        /уложи[лсь]/i,
        /план.*(трат|расход)/i
    ],
    savings_goals: [
        /цел[ьи]/i,
        /накопи/i,
        /копилк/i,
        /откладыва/i,
        /мечт/i,
        /на море/i,
        /на отпуск/i,
        /прогресс/i,
        /сколько осталось/i,
        /успе[юя] накопить/i
    ],
    expense_analytics: [
        /трат/i,
        /расход/i,
        /статистик/i,
        /категор/i,
        /куда.*(уход|ушл)/i,
        /за (месяц|неделю|период)/i,
        /анали[зт]/i,
        /граф[иу]/i
    ],
    income: [
        /доход/i,
        /зарплат/i,
        /получ[аи]/i,
        /заработ/i,
        /сколько (я |мы )?зараб/i
    ],
    advice: [
        /сове[тч]/i,
        /помог/i,
        /как (мне |нам )?(сэконом|накопит|откладыват|улучшить|оптимизирова)/i,
        /что (мне |нам )?делать/i,
        /с чего начать/i
    ]
};

function detectIntent(message) {
    for (const [intent, patterns] of Object.entries(INTENT_PATTERNS)) {
        if (patterns.some(re => re.test(message))) return intent;
    }
    return "general";
}

// ─── Financial Profile ─────────────────────────────────────────────────────────
async function getFinancialProfile(userId) {
    const monthAgoMs = Date.now() - 30 * 24 * 60 * 60 * 1000; //30 дней

    const [txSnap, goalsSnap, budgetsSnap] = await Promise.all([ //параллельно
        db.collection("transactions")
            .where("userId", "==", userId)
            .where("timestamp", ">=", monthAgoMs)
            .orderBy("timestamp", "desc")
            .limit(100)
            .get(),
        db.collection("goals")
            .where("userId", "==", userId)
            .where("isCompleted", "==", false)
            .limit(10)
            .get(),
        db.collection("budgets")
            .where("userId", "==", userId)
            .get()
    ]);

    // Aggregate transactions
    let incomeTotal = 0;
    let expenseTotal = 0;
    const categoryMap = {}; // categoryId → { title, amount }
    const allExpenses = [];

    txSnap.forEach(doc => {
        const { amount = 0, isIncome, categoryId, category, categoryTitle, title } = doc.data();
        const catKey = categoryId || category || "other";
        const catTitle = categoryTitle || category || catKey;

        if (isIncome) {
            incomeTotal += amount;
        } else {
            expenseTotal += amount;
            if (!categoryMap[catKey]) categoryMap[catKey] = { title: catTitle, amount: 0 };
            categoryMap[catKey].amount += amount;
            allExpenses.push({ label: title ?? catTitle, amount });
        }
    });

    // Top 10 single transactions
    allExpenses.sort((a, b) => b.amount - a.amount);
    const topTransactions = allExpenses.slice(0, 10);

    // Build budgets map
    const budgetMap = {};
    budgetsSnap.forEach(doc => {
        const { categoryId, limitAmount, currentSpent } = doc.data();
        if (categoryId) budgetMap[categoryId] = { limit: limitAmount ?? 0, spent: currentSpent ?? 0 };
    });

    // Build sorted categories with budget info
    const categories = Object.entries(categoryMap)
        .sort(([, a], [, b]) => b.amount - a.amount)
        .map(([catId, { title, amount }]) => {
            const pct = expenseTotal > 0 ? Math.round((amount / expenseTotal) * 100) : 0;
            const fixed = isFixed(title);
            const b = budgetMap[catId];
            const entry = { title, amount, pct, fixed };
            if (b) {
                entry.budgetLimit = b.limit;
                entry.budgetOverrun = Math.max(0, amount - b.limit);
                entry.budgetLeft = Math.max(0, b.limit - amount);
            }
            return entry;
        });

    // Goals
    const goals = goalsSnap.docs.map(doc => {
        const { title, targetAmount = 0, savedAmount = 0, deadline } = doc.data();
        const progress = targetAmount > 0 ? Math.round((savedAmount / targetAmount) * 100) : 0;
        const remaining = Math.max(0, targetAmount - savedAmount);
        const deadlineDate = deadline ? new Date(deadline).toISOString().split("T")[0] : null;

        // Months to deadline
        let monthsLeft = null;
        if (deadline) {
            const msLeft = deadline - Date.now();
            monthsLeft = Math.max(0, Math.round(msLeft / (30 * 24 * 60 * 60 * 1000)));
        }

        return { title, targetAmount, savedAmount, remaining, progress, deadline: deadlineDate, monthsLeft };
    });

    const savingsRate = incomeTotal > 0
        ? Math.round(((incomeTotal - expenseTotal) / incomeTotal) * 100)
        : null;

    return {
        incomeTotal,
        expenseTotal,
        savingsRate,
        categories,
        goals,
        topTransactions,
        isEmpty: txSnap.empty && goalsSnap.empty
    };
}

// ─── Profile Formatter ─────────────────────────────────────────────────────────
function formatProfile(p) {
    const lines = ["=== ФИНАНСОВЫЙ ПРОФИЛЬ (30 дней) ==="];

    if (p.incomeTotal > 0) {
        const saved = p.incomeTotal - p.expenseTotal;
        lines.push(`Доход: ${fmt(p.incomeTotal)} ₽ | Расходы: ${fmt(p.expenseTotal)} ₽ | Свободно: ${fmt(saved)} ₽ (норма сбережения: ${p.savingsRate}%)`);
    } else {
        lines.push(`Расходы: ${fmt(p.expenseTotal)} ₽ (доход не записан)`);
    }

    if (p.categories.length > 0) {
        lines.push("\nРасходы по категориям:");
        for (const c of p.categories) {
            let line = `• ${c.title}: ${fmt(c.amount)} ₽ (${c.pct}%)`;
            if (c.fixed) line += " [обязательная]";
            if (c.budgetLimit !== undefined) {
                if (c.budgetOverrun > 0) {
                    line += ` ⚠️ превышен лимит ${fmt(c.budgetLimit)} ₽ на ${fmt(c.budgetOverrun)} ₽`;
                } else {
                    line += ` ✓ лимит ${fmt(c.budgetLimit)} ₽, остаток ${fmt(c.budgetLeft)} ₽`;
                }
            }
            lines.push(line);
        }
    }

    if (p.goals.length > 0) {
        const monthlySavings = p.incomeTotal > 0 ? p.incomeTotal - p.expenseTotal : 0;
        lines.push("\nЦели накопления:");
        for (const g of p.goals) {
            let line = `• "${g.title}": накоплено ${fmt(g.savedAmount)} из ${fmt(g.targetAmount)} ₽ (${g.progress}%), осталось: ${fmt(g.remaining)} ₽`;
            if (g.deadline && g.monthsLeft > 0) {
                const monthlyNeeded = Math.round(g.remaining / g.monthsLeft);
                line += `, дедлайн: ${g.deadline} (осталось ${g.monthsLeft} мес.), нужно откладывать: ${fmt(monthlyNeeded)} ₽/мес.`;
            } else if (!g.deadline && monthlySavings > 0) {
                const monthsAtCurrentRate = Math.round(g.remaining / monthlySavings);
                line += `, без дедлайна; при текущем темпе (${fmt(monthlySavings)} ₽/мес. свободно) — цель через ~${monthsAtCurrentRate} мес.`;
            } else if (!g.deadline) {
                line += `, дедлайн не установлен`;
            }
            lines.push(line);
        }
    }

    if (p.topTransactions.length > 0) {
        lines.push("\nКрупнейшие траты:");
        p.topTransactions.forEach(t => lines.push(`• ${t.label}: ${fmt(t.amount)} ₽`));
    }

    return lines.join("\n");
}

// ─── Prompt Builder ────────────────────────────────────────────────────────────
const SYSTEM_PROMPT = `Ты профессиональный финансовый коуч. Твои правила:

1. Используй ТОЛЬКО данные из профиля пользователя. Не придумывай цифры.
2. НЕ задавай уточняющих вопросов — данные уже есть, отвечай сразу.
3. Категории помеченные [обязательная] (жильё, аренда, ипотека, коммуналка) — НЕ советуй их сокращать, это базовые нужды.
4. Фокусируй советы на дискреционных тратах (красота, развлечения, рестораны, одежда, хобби).
5. Превышение бюджета — не повод ругать. Скажи: "Бывает, вот как скорректировать оставшиеся дни месяца..."
6. Давай конкретные числа: не "сократи красоту", а "сократив [категория] с [сумма] до [исходная сумма - 15-20%] ты освободишь [сумма]/мес — это [сэкономленная сумма] в год на накопления".
7. Если есть цель и свободные средства — посчитай реалистичность: "при текущей норме сбережения X ₽/мес цель достижима за Y месяцев".
8. Отвечай тепло, дружелюбно, кратко (5-7 предложений), на русском. Без воды и общих фраз.

Структурируй ответ. Добавь в ответ несколько подходящих эмодзи`;

const INTENT_FOCUS = {
    budget:            "Сфокусируйся на бюджетах. Отметь превышения (без осуждения) и предложи как скорректировать оставшиеся дни.",
    savings_goals:     "Сфокусируйся на целях накопления. Оцени реалистичность, посчитай сколько нужно откладывать в месяц, подбодри.",
    expense_analytics: "Сделай анализ расходов. Выдели слабые места среди дискреционных категорий, дай конкретный совет с цифрами.",
    income:            "Сфокусируйся на соотношении доходов и расходов, норме сбережения. Дай совет как улучшить баланс.",
    advice:            "Дай персональный финансовый совет. Опирайся на реальные цифры профиля — что улучшить в первую очередь.",
    general:           "Ответь на вопрос пользователя, используя данные профиля для персонализации ответа."
};

function buildPrompt(profileText, intent, userMessage) {
    const focus = INTENT_FOCUS[intent] ?? INTENT_FOCUS.general;
    return [
        { role: "system", content: SYSTEM_PROMPT },
        {
            role: "user",
            content: `${profileText}\n\nЗадача коуча: ${focus}\n\nВопрос пользователя: "${userMessage}"`
        }
    ];
}

function buildNewUserPrompt(userMessage) {
    return [
        {
            role: "system",
            content: "Ты дружелюбный финансовый коуч в приложении FinCoach. У пользователя пока нет данных. Тепло поприветствуй, объясни как начать: добавить первую транзакцию или поставить цель накопления. Кратко, на русском, без воды."
        },
        { role: "user", content: userMessage }
    ];
}

// ─── GigaChat Call ─────────────────────────────────────────────────────────────
async function callGigaChat(messages) {
    const token = await getGigaToken();
    const response = await axios.request({
        method: "post",
        url: "https://gigachat.devices.sberbank.ru/api/v1/chat/completions",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Authorization": `Bearer ${token}`
        },
        data: {
            model: "GigaChat",
            messages,
            temperature: 0.75,
            max_tokens: 1000,
            top_p: 0.9
        },
        httpsAgent: sberAgent,
        timeout: 15000
    });
    return response.data.choices[0].message.content;
}

// ─── Main Cloud Function ───────────────────────────────────────────────────────
exports.analyzeFinances = onCall({ maxInstances: 10 }, async (request) => {

    // Проверка авторизации
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Необходима авторизация.");
    }
    const userId = request.auth.uid;

    // Валидация сообщения
    const userMessage = (request.data.message ?? "").trim();
    if (!userMessage) throw new HttpsError("invalid-argument", "Сообщение не может быть пустым.");
    if (userMessage.length > MAX_MESSAGE_LENGTH) {
        throw new HttpsError("invalid-argument", `Максимум ${MAX_MESSAGE_LENGTH} символов.`);
    }

    // Rate limiting
    const userRef = db.doc(`users/${userId}`);
    const userSnap = await userRef.get();
    const lastRequestMs = userSnap.data()?.lastAiRequest?.toMillis?.() ?? 0;
    if (Date.now() - lastRequestMs < RATE_LIMIT_MS) {
        throw new HttpsError("resource-exhausted", "Слишком много запросов. Подождите несколько секунд.");
    }
    
    //Обновление времени последнего запроса
    userRef.set(
        { lastAiRequest: admin.firestore.FieldValue.serverTimestamp() },
        { merge: true }
    ).catch(err => console.error("Rate limit write:", err));

    // Определение намерения
    const intent = detectIntent(userMessage);
    console.log(`[1/3] intent="${intent}" uid=${userId}`);

    // Загрузка финансового профиля
    let profile;
    try {
        profile = await getFinancialProfile(userId);
        console.log(`[2/3] income=${profile.incomeTotal} expenses=${profile.expenseTotal} cats=${profile.categories.length} goals=${profile.goals.length} empty=${profile.isEmpty}`);
        console.log(`[2/3] goals_raw=${JSON.stringify(profile.goals)}`);
    } catch (err) {
        console.error("[2/3] fetch error:", err);
        profile = { isEmpty: true };
    }

    // Построение промта
    const messages = profile.isEmpty
        ? buildNewUserPrompt(userMessage)
        : buildPrompt(formatProfile(profile), intent, userMessage);

    // Вызов GigaChat
    try {
        const answer = await callGigaChat(messages);
        console.log(`[3/3] ok len=${answer.length}`);
        return { success: true, answer };
    } catch (error) {
        console.error("[3/3] GigaChat error:", error.response?.data ?? error.message);
        return { success: false, answer: GIGACHAT_FALLBACK };
    }
});

console.log("<------> Cloud Functions v2 loaded <------>");
