//
//  QuoteOfTheDayView.swift
//  FinCoach
//

import SwiftUI

struct QuoteOfTheDayView: View {
    private static let quotes: [String] = [
        "Финансовая свобода начинается с первого осознанного решения.",
        "Не работайте ради денег — заставьте деньги работать на вас.",
        "Бюджет — это не ограничение, а план вашей свободы.",
        "Лучшее время инвестировать было вчера. Второе лучшее — сегодня.",
        "Богатство — это разница между доходами и расходами, а не их размер.",
        "Маленькие ежедневные траты складываются в большие годовые расходы.",
        "Финансовая дисциплина сегодня — финансовая независимость завтра.",
        "Каждая сэкономленная сумма — это инвестиция в ваше будущее.",
        "Трать меньше, чем зарабатываешь — это единственный секрет богатства.",
        "Цели без плана — это просто мечты. Поставьте финансовую цель.",
        "Небольшие регулярные накопления творят большие чудеса.",
        "Осознанные траты делают жизнь богаче, а не беднее.",
        "Ваш кошелёк отражает ваши приоритеты — расставьте их правильно.",
        "Инвестиция в знания о деньгах окупается быстрее всего.",
        "Контролируйте деньги, иначе они будут контролировать вас.",
        "Путь к финансовой цели начинается с отслеживания расходов.",
        "Каждый отказ от лишнего — это согласие на большее.",
        "Деньги — инструмент. Научитесь им пользоваться мастерски.",
        "Сегодняшняя экономия — это завтрашняя возможность.",
        "Каждый рубль, сохранённый сегодня — это шаг к свободе завтра."
    ]

    private var quote: String {
        let dayOfYear = Calendar.current.ordinality(of: .day, in: .year, for: Date()) ?? 1
        return Self.quotes[(dayOfYear - 1) % Self.quotes.count]
    }

    var body: some View {
        HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(AppColors.grayTextColor)
                    Text("Мотивация дня")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppColors.grayTextColor)
                }
                
                Text(quote)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(AppColors.blackTextColor)
                    .fixedSize(horizontal: false, vertical: true)
                //.shadow(color: Color.black.opacity(0.07), radius: 4, x: 0, y: 2) //masha
            }
            Spacer()
            Image("pig_assistant")
                .resizable()
                .scaledToFit()
                .frame(width: 70, height: 70)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .background(AppColors.lightGreenFrameColor)
        .cornerRadius(16)
    }
}

#Preview {
    QuoteOfTheDayView()
        .padding()
        .background(AppColors.backgroundGray)
}
