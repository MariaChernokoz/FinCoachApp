/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const { setGlobalOptions } = require("firebase-functions/v2");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const axios = require("axios");

setGlobalOptions({ region: "us-central1", maxInstances: 10 });

const AUTH_KEY = process.env.GIGACHAT_AUTH_KEY;
const MOCK_USER_DATA = { monthlyIncome: 80000, currency: 'RUB' };
const MOCK_GOALS = [
    { name: 'Отпуск', targetAmount: 200000, currentAmount: 50000 },
    { name: 'iPhone', targetAmount: 120000, currentAmount: 10000 }
];

// Receive a token (requires updating every 30 minutes)
async function getGigaToken() {
    try {
        if (!AUTH_KEY) {
            throw new Error('GIGACHAT_AUTH_KEY is not configured');
        }

        const data = new URLSearchParams({ scope: 'GIGACHAT_API_PERS' }).toString();
        const config = {
            method: 'post',
            maxBodyLength: Infinity,
            url: 'https://ngw.devices.sberbank.ru:9443/api/v2/oauth',
            headers: { 
                'Content-Type': 'application/x-www-form-urlencoded', 
                'Accept': 'application/json', 
                'RqUID': '6f0b1293-2713-4fd3-90d5-b04963595678',
                'Authorization': `Basic ${AUTH_KEY}`
            },
            data: data
        };

        const response = await axios.request(config);
        return response.data.access_token;
    } catch (error) {
        console.error('Token Error:', error.response ? error.response.data : error.message);
        throw error;
    }
}

exports.analyzeFinances = onCall({ maxInstances: 10 }, async (request) => {
    if (!request.auth) {
        throw new HttpsError('unauthenticated', 'Необходимо войти в аккаунт');
    }

    const userTransactions = request.data.transactions || [];
    const userQuestion = request.data.question;

    if (!userQuestion) throw new HttpsError('invalid-argument', 'Вопрос пуст');

    try {
        const token = await getGigaToken();

        const prompt = `Ты финансовый коуч. 
        Вот последние траты пользователя: ${JSON.stringify(userTransactions)}.
        Вопрос пользователя: "${userQuestion}"
        Проанализируй эти конкретные траты и ответь кратко.`;
        
        // const prompt = `Ты финансовый коуч. У пользователя доход ${MOCK_USER_DATA.monthlyIncome} руб. 
        // Цели: ${JSON.stringify(MOCK_GOALS)}.
        // Вопрос пользователя: "${userQuestion}"
        // Ответь на русском языке, кратко, дай 2 совета.`;

        const config = {
            method: 'post',
            url: 'https://gigachat.devices.sberbank.ru/api/v1/chat/completions',
            headers: { 
                'Content-Type': 'application/json', 
                'Accept': 'application/json', 
                'Authorization': `Bearer ${token}`
            },
            data: {
                model: "GigaChat",
                messages: [{ role: "user", content: prompt }],
                temperature: 0.7
            }
        };

        const response = await axios.request(config);
        
        return {
            answer: response.data.choices[0].message.content,
            success: true
        };

    } catch (error) {
        console.error('GigaChat Error:', error.response ? error.response.data : error.message);
        return {
            answer: "Произошла ошибка при связи. Попробуйте еще раз!",
            success: false
        };
    }
});

// Logic of analysis and context
function analyzeQuestionContext(question) {
    const lowerQuestion = question.toLowerCase();
    
    const timeKeywords = {
        day: ['сегодня', 'вчера', 'за день'],
        week: ['неделю', 'недели', 'за неделю', 'на этой неделе'],
        month: ['месяц', 'месяца', 'за месяц', 'в этом месяце'],
        quarter: ['квартал', 'за квартал', '3 месяца'],
        year: ['год', 'года', 'за год', 'годовой']
    };
    
    const categoryKeywords = {
        food: ['еда', 'еду', 'питание', 'ресторан', 'кафе', 'продукты', 'супермаркет'],
        transport: ['транспорт', 'такси', 'бензин', 'заправка', 'машина'],
        entertainment: ['развлечения', 'кино', 'концерт', 'отдых', 'бар'],
        shopping: ['покупки', 'одежда', 'шоппинг'],
        health: ['здоровье', 'аптека', 'врач', 'медицина']
    };
    
    let timeRange = 'month';
    for (const [range, keywords] of Object.entries(timeKeywords)) {
        if (keywords.some(keyword => lowerQuestion.includes(keyword))) {
            timeRange = range;
            break;
        }
    }
    
    let category = null;
    for (const [cat, keywords] of Object.entries(categoryKeywords)) {
        if (keywords.some(keyword => lowerQuestion.includes(keyword))) {
            category = cat;
            break;
        }
    }
    
    const transactionLimits = { day: 50, week: 100, month: 200, quarter: 500, year: 1000 };
    
    return { timeRange, category, limit: transactionLimits[timeRange] };
}

function getAdaptiveUserContext(questionContext) {
    const { timeRange, category, limit } = questionContext;
    const startDate = getStartDate(timeRange);
    
    let filtered = MOCK_TRANSACTIONS.filter(t => new Date(t.date) >= startDate);
    if (category) filtered = filtered.filter(t => t.category === category);
    
    return {
        user: MOCK_USER_DATA,
        transactions: filtered.slice(0, limit),
        goals: MOCK_GOALS,
        questionContext
    };
}

function getStartDate(timeRange) {
    const result = new Date();
    if (timeRange === 'day') result.setDate(result.getDate() - 1);
    else if (timeRange === 'week') result.setDate(result.getDate() - 7);
    else if (timeRange === 'month') result.setMonth(result.getMonth() - 1);
    else if (timeRange === 'quarter') result.setMonth(result.getMonth() - 3);
    else if (timeRange === 'year') result.setFullYear(result.getFullYear() - 1);
    return result;
}

// Forming a prompt
function buildSmartPrompt(contextData, userQuestion, questionContext) {
    const { user, transactions, goals } = contextData;
    const analysis = analyzeTransactions(transactions);
    
    return `Ты финансовый коуч. Отвечай кратко и профессионально на основе данных.
    
    ДОХОД: ${user.monthlyIncome} ${user.currency}
    ТРАТЫ ЗА ПЕРИОД: ${analysis.totalExpenses} ${user.currency}
    КАТЕГОРИИ: ${JSON.stringify(analysis.categories)}
    ЦЕЛИ: ${JSON.stringify(goals)}
    
    ВОПРОС: "${userQuestion}"
    
    Ответь на русском языке, используй цифры, дай 2 совета.`;
}

function analyzeTransactions(transactions) {
    let totalExpenses = 0;
    const categories = {};
    transactions.forEach(t => {
        if (t.type === 'expense') {
            totalExpenses += t.amount;
            categories[t.category] = (categories[t.category] || 0) + t.amount;
        }
    });
    return { totalExpenses, categories };
}

// Generating mock data
function generateMockTransactions() {
    const transactions = [];
    const now = new Date();
    for (let i = 0; i < 90; i++) {
        const date = new Date(now);
        date.setDate(date.getDate() - i);
        if (Math.random() > 0.4) {
            transactions.push({
                amount: Math.floor(Math.random() * 1000) + 100,
                category: ['food', 'transport', 'entertainment'][Math.floor(Math.random() * 3)],
                type: 'expense',
                date: date.toISOString()
            });
        }
    }
    // Salary
    for (let i = 0; i < 3; i++) {
        const d = new Date(now);
        d.setMonth(d.getMonth() - i);
        d.setDate(1);
        transactions.push({ amount: 80000, category: 'salary', type: 'income', date: d.toISOString() });
    }
    return transactions;
}

console.log('<------> Cloud Functions loaded <------>');
