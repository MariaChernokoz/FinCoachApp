const { setGlobalOptions } = require("firebase-functions/v2");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const axios = require("axios");
const https = require("https");
const { randomUUID } = require("crypto");

admin.initializeApp();
const db = admin.firestore();

const sberAgent = new https.Agent({ rejectUnauthorized: false });

setGlobalOptions({ region: "us-central1", maxInstances: 10 });

const AUTH_KEY = process.env.GIGACHAT_AUTH_KEY;
const RATE_LIMIT_MS = 3000;
const MAX_MESSAGE_LENGTH = 500;
const GIGACHAT_FALLBACK = "Упс, я временно вне зоны доступа. Но я помню, что ты молодец! Попробуй чуть позже.";

// ─── Token Cache ───────────────────────────────────────────────────────────────
// GigaChat токен живёт ~30 минут. Кешируем на уровне модуля, чтобы не делать
// лишний OAuth-запрос на каждый вызов функции (экономия времени и квоты Сбера).
let _tokenCache = { value: null, expiresAt: 0 };

async function getGigaToken() {
    // Обновляем токен только если до истечения осталось меньше 60 секунд
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
            "RqUID": randomUUID(), // Сбер требует уникальный UUID на каждый запрос
            "Authorization": `Basic ${AUTH_KEY}`
        },
        data: new URLSearchParams({ scope: "GIGACHAT_API_PERS" }).toString(),
        httpsAgent: sberAgent
    });

    const expiresIn = response.data.expires_in ?? 1800; // fallback 30 мин
    _tokenCache = {
        value: response.data.access_token,
        expiresAt: Date.now() + expiresIn * 1000
    };

    return _tokenCache.value;
}

// ─── Intent Router ─────────────────────────────────────────────────────────────
// Определяем намерение пользователя по регулярным выражениям.
// Первый совпавший интент побеждает — порядок в объекте = приоритет.
const INTENT_PATTERNS = {
    expense_analytics: [
        /сколько.*(потрат|трат|расход)/i,
        /трат[аыиу]/i,
        /расход[ыа]/i,
        /категор/i,
        /бюджет/i,
        /куда.*(уход|ушл)/i,
        /за (месяц|неделю|период)/i,
        /анали[зт]/i,
        /граф[иу]/i
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
        /сколько осталось/i
    ]
    // Всё что не попало в паттерны выше → "general"
};

function detectIntent(message) {
    for (const [intent, patterns] of Object.entries(INTENT_PATTERNS)) {
        if (patterns.some(re => re.test(message))) return intent;
    }
    return "general";
}

// ─── Firestore Fetchers ────────────────────────────────────────────────────────

// Расходы: сначала пробуем агрегированный документ за текущий месяц.
// Если его нет — fallback-запрос к транзакциям (последние 30 дней, лимит 50).
async function getExpenseContext(userId) {
    const now = new Date();
    const monthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;

    const statsSnap = await db.doc(`users/${userId}/monthly_stats/${monthKey}`).get();

    if (statsSnap.exists) {
        const data = statsSnap.data();
        return {
            isEmpty: false,
            source: "monthly_stats",
            stats: {
                totalExpenses: data.totalExpenses ?? 0,
                categories: data.categories ?? {}
            }
        };
    }

    // Fallback: транзакции хранятся в глобальной коллекции "transactions",
    // фильтруем по userId. Дата — timestamp в миллисекундах (Int64).
    const monthAgoMs = Date.now() - 30 * 24 * 60 * 60 * 1000;

    const txSnap = await db.collection("transactions")
        .where("userId", "==", userId)
        .where("isIncome", "==", false)
        .where("timestamp", ">=", monthAgoMs)
        .orderBy("timestamp", "desc")
        .limit(50)
        .get();

    if (txSnap.empty) {
        return { isEmpty: true, source: "fallback", stats: { totalExpenses: 0, categories: {} } };
    }

    // Агрегируем в памяти — в GigaChat уйдёт готовая статистика, не сырые документы
    const stats = { totalExpenses: 0, categories: {} };
    txSnap.forEach(doc => {
        const { amount = 0, category = "other" } = doc.data();
        stats.totalExpenses += amount;
        stats.categories[category] = (stats.categories[category] ?? 0) + amount;
    });

    return { isEmpty: false, source: "fallback", stats };
}

// Цели: читаем только активные, без архивных
async function getGoalsContext(userId) {
    const snap = await db.collection(`users/${userId}/goals`)
        .where("status", "==", "active")
        .limit(10)
        .get();

    if (snap.empty) return { isEmpty: true, goals: [] };

    const goals = snap.docs.map(doc => {
        const { name, targetAmount, currentAmount, deadline } = doc.data();
        const progress = targetAmount > 0
            ? Math.round((currentAmount / targetAmount) * 100)
            : 0;
        return { name, targetAmount, currentAmount, progress, deadline: deadline ?? null };
    });

    return { isEmpty: false, goals };
}

// ─── Prompt Builders ───────────────────────────────────────────────────────────
// Каждый билдер возвращает массив messages для GigaChat API:
// [{ role: "system", content: инструкция }, { role: "user", content: данные + вопрос }]

function buildExpensePrompt(stats, userMessage) {
    // Компактный JSON: только category + amount, отсортированные по убыванию трат
    const breakdown = Object.entries(stats.categories)
        .sort(([, a], [, b]) => b - a)
        .map(([category, amount]) => ({ category, amount }));

    return [
        {
            role: "system",
            content: "Ты финансовый коуч в мобильном приложении FinCoach. Отвечай кратко, по делу, на русском. Используй цифры из данных пользователя. Дай 1–2 практических совета. Не повторяй вопрос."
        },
        {
            role: "user",
            content: `Мои расходы за последний месяц:\n${JSON.stringify({ total: stats.totalExpenses, breakdown })}\n\nВопрос: "${userMessage}"`
        }
    ];
}

function buildGoalsPrompt(goals, userMessage) {
    return [
        {
            role: "system",
            content: "Ты финансовый коуч в мобильном приложении FinCoach. Отвечай кратко, мотивирующе, на русском. Опирайся на прогресс целей пользователя. Дай 1–2 конкретных совета."
        },
        {
            role: "user",
            content: `Мои цели накопления:\n${JSON.stringify(goals)}\n\nВопрос: "${userMessage}"`
        }
    ];
}

function buildGeneralPrompt(userMessage) {
    return [
        {
            role: "system",
            content: "Ты дружелюбный финансовый коуч в мобильном приложении FinCoach. Отвечай кратко, по делу, на русском. Помогай разобраться в личных финансах."
        },
        {
            role: "user",
            content: userMessage
        }
    ];
}

// Edge case: пользователь без данных — не говорим «нет данных», а мягко направляем
function buildNewUserPrompt(userMessage) {
    return [
        {
            role: "system",
            content: "Ты дружелюбный финансовый коуч в мобильном приложении FinCoach. У этого пользователя ещё нет данных о расходах или целях. Мягко поприветствуй его, объясни как начать: добавить первую транзакцию или поставить цель накопления. Отвечай тепло, коротко, на русском."
        },
        {
            role: "user",
            content: userMessage
        }
    ];
}

// ─── GigaChat API Call ─────────────────────────────────────────────────────────
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
            temperature: 0.7,
            max_tokens: 500
        },
        httpsAgent: sberAgent,
        timeout: 15000
    });

    return response.data.choices[0].message.content;
}

// ─── Main Cloud Function ───────────────────────────────────────────────────────
exports.analyzeFinances = onCall({ maxInstances: 10 }, async (request) => {

    // ── Safety: Auth check ──────────────────────────────────────────────────
    // invoker не задан → только аутентифицированные пользователи Firebase
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Необходима авторизация.");
    }
    const userId = request.auth.uid;

    // ── Safety: Input validation ────────────────────────────────────────────
    const userMessage = (request.data.message ?? "").trim();

    if (!userMessage) {
        throw new HttpsError("invalid-argument", "Сообщение не может быть пустым.");
    }
    if (userMessage.length > MAX_MESSAGE_LENGTH) {
        throw new HttpsError(
            "invalid-argument",
            `Сообщение слишком длинное. Максимум ${MAX_MESSAGE_LENGTH} символов.`
        );
    }

    // ── Safety: Rate limiting ───────────────────────────────────────────────
    // serverTimestamp() гарантирует синхронизацию с сервером Firestore,
    // а не с локальными часами инстанса Cloud Function.
    const userRef = db.doc(`users/${userId}`);
    const userSnap = await userRef.get();
    const lastRequestMs = userSnap.data()?.lastAiRequest?.toMillis?.() ?? 0;

    if (Date.now() - lastRequestMs < RATE_LIMIT_MS) {
        throw new HttpsError(
            "resource-exhausted",
            "Слишком много запросов. Подождите несколько секунд."
        );
    }

    // Записываем timestamp асинхронно — не блокируем основной поток ответа
    userRef.set(
        { lastAiRequest: admin.firestore.FieldValue.serverTimestamp() },
        { merge: true }
    ).catch(err => console.error("Rate limit write error:", err));

    // ── Intent Router ───────────────────────────────────────────────────────
    const intent = detectIntent(userMessage);
    console.log(`[analyzeFinances] uid=${userId} intent=${intent} msgLen=${userMessage.length}`);

    // ── Context Fetching + Prompt Building ─────────────────────────────────
    let messages;

    try {
        if (intent === "expense_analytics") {
            const context = await getExpenseContext(userId);
            messages = context.isEmpty
                ? buildNewUserPrompt(userMessage)
                : buildExpensePrompt(context.stats, userMessage);

        } else if (intent === "savings_goals") {
            const context = await getGoalsContext(userId);
            messages = context.isEmpty
                ? buildNewUserPrompt(userMessage)
                : buildGoalsPrompt(context.goals, userMessage);

        } else {
            // "general" — никаких запросов в Firestore, экономим чтения
            messages = buildGeneralPrompt(userMessage);
        }
    } catch (dbError) {
        // Firestore недоступен — деградируем к общему промпту, не роняем функцию
        console.error("Firestore fetch error:", dbError);
        messages = buildGeneralPrompt(userMessage);
    }

    // ── GigaChat Call ───────────────────────────────────────────────────────
    // Структура ответа одинакова при успехе и при ошибке — iOS-клиент всегда
    // получит { success: bool, answer: string } и не упадёт при парсинге.
    try {
        const answer = await callGigaChat(messages);
        return { success: true, answer };
    } catch (error) {
        console.error("GigaChat error:", error.response?.data ?? error.message);
        return { success: false, answer: GIGACHAT_FALLBACK };
    }
});

console.log("<------> Cloud Functions loaded <------>");
