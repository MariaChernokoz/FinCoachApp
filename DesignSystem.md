# FinCoach — Design System

## Inconsistencies Found (Current State)

### 1. Corner Radii — no clear system
Three different values are used for visually identical things:

| Where | Current value | Should be |
|-------|--------------|-----------|
| Main cards (transactions, goals, charts) | 22 | ✓ keep 22 — this is the card token |
| `OverflowMetricCard` (balance card child) | 16 | → 22 |
| `AnalyticsSummaryCards` | 16 | → 22 |
| Empty state cards | 22 on overlay, but button inside uses 14 | → button 16 |
| Chat input field | 20 | → 16 (same as search bar) |
| Message bubbles | 18 | ✓ keep 18 — messaging convention |
| Hint chips | 18 | → 20 (pill/capsule feel) |
| Period selector buttons | 12 | ✓ keep 12 — small control token |
| Sort/filter icon buttons | 12 | ✓ keep 12 |
| Search bars | 16 | ✓ keep 16 — input token |

**Root cause:** three radius tokens were never explicitly defined, so each component picked its own value.

---

### 2. Typography — inconsistent hierarchy

| Role | Current values | Should be |
|------|---------------|-----------|
| Section / screen header | 18 semibold in charts, 20 bold was in Goals | 18 semibold everywhere |
| Row / card title | 17 semibold (Transaction, Budget) vs **16 semibold** (Goal) | 17 semibold everywhere |
| Primary amount | 15 bold (SummaryCards) vs **17 bold** (OverflowMetric) vs **34 bold** (total balance) | depends on context, but 15 bold for inline, 17 bold for card |
| Secondary label / category | 13 in some, 14 in others | 13 for captions, 14 for secondary body |
| Chart axis / footnote | 11 | ✓ keep 11 |

---

### 3. Icon circle sizes — three sizes for the same concept

| Component | Circle size |
|-----------|-------------|
| `TransactionRowView` | 40 × 40 |
| `BudgetCardView` | 44 × 44 |
| `CategoryDetailView` | 36 × 36 |

All three represent "category icon inside a circle inside a list row". Should be one size (40 × 40).

---

### 4. Row padding — two patterns

| Component | Padding |
|-----------|---------|
| `TransactionRowView`, `BudgetCardView`, `GoalCardView` | 18H / 16V |
| `AnalyticsSummaryCards` | 12H / 14V |
| `QuoteOfTheDayView` | 20H / 18V |

Standard list row should be 16H / 16V or 18H / 16V — pick one.

---

### 5. Section spacing inconsistency

| Screen | VStack spacing between sections |
|--------|---------------------------------|
| `GoalsView` | 20 |
| `AnalyticsView` | 16 |
| `TransactionsView` | 16 |

Should be unified at 20.

---

### 6. Divider leading padding — three values

| Component | Divider indent |
|-----------|---------------|
| Transaction list | `.leading, 66` |
| Budget list | `.leading, 82` |
| Goal list | `.leading, 90` |

Should all match: circle width (40) + row left padding (18) = **58**. Or round to 60.

---

### 7. Hardcoded system colors instead of AppColors

- `AIAssistantView`: uses `.white` instead of `AppColors.whiteFrameColor`
- `AIAssistantView`: uses `.gray` instead of `AppColors.grayTextColor`
- `SettingsView`: uses `.gray` for email text
- `AuthView`: uses `Color(.systemGray4)`, `Color(.systemBackground)`

These bypass the design token system and break dark-mode readiness.

---

### 8. Button heights — three values with no clear rule

| Context | Height |
|---------|--------|
| Auth primary buttons | 50 |
| Goal/budget CTA buttons (in empty states) | 48 |
| Search bar | 40 |
| Period selector | 36 |

Primary CTAs (full-width green buttons) should all be **50**. Secondary/compact buttons 40.

---

### 9. Shadow system — inconsistent

Cards (transactions, goals, budgets, charts) have **no shadow**, relying on the 1px border for separation.  
AI messages use `shadow(radius: 4, y: 2, opacity: 0.07)`.  
Example question cards use `shadow(radius: 3, y: 1, opacity: 0.05)`.  
FAB uses `shadow(radius: 14, y: 8, opacity: 0.35)`.

The white cards on gray background need **either** the 1px border (current, consistent) **or** a subtle shadow — mixing them creates visual noise. Should pick one approach for all white-on-gray cards.

---

## Design Tokens (Target System)

### Corner Radius

| Token | Value | Usage |
|-------|-------|-------|
| `radius.card` | 22 | All cards, sheets, chart containers, empty state containers |
| `radius.input` | 16 | Search bars, text fields, auth inputs |
| `radius.button` | 16 | Full-width CTA buttons, small action buttons |
| `radius.control` | 12 | Period selector, sort/filter icon buttons |
| `radius.chip` | 20 | Hint chips, tags (pill shape) |
| `radius.bubble` | 18 | Chat message bubbles |
| `radius.bar` | 4 | Progress bars, bar chart segments |

---

### Typography

| Token | Size | Weight | Usage |
|-------|------|--------|-------|
| `text.sectionTitle` | 18 | .semibold | Section headers within a screen |
| `text.cardTitle` | 17 | .semibold | Primary label in list rows and cards |
| `text.cardSubtitle` | 13 | .regular | Secondary label, category name under title |
| `text.amount` | 17 | .bold | Currency amounts in cards |
| `text.amountLarge` | 34 | .bold | Total balance hero number |
| `text.amountSmall` | 15 | .bold | Compact amounts (summary cards) |
| `text.body` | 15 | .regular | Body text, chat messages, search placeholder |
| `text.caption` | 13 | .regular | Supporting info, timestamps, chart labels |
| `text.footnote` | 11 | .regular | Chart axes, percentage inside tiny spaces |
| `text.label` | 14 | .medium | Button labels, period selector, filter labels |

---

### Spacing

| Token | Value | Usage |
|-------|-------|-------|
| `spacing.screenH` | 20 | Horizontal edge padding for all screens |
| `spacing.screenTop` | 18 | Top padding after nav bar |
| `spacing.screenBottom` | 96 | Bottom padding when FAB is present, 32 otherwise |
| `spacing.sectionGap` | 20 | Vertical gap between major sections on a screen |
| `spacing.itemGap` | 10 | Vertical gap between rows inside a card group |
| `spacing.rowH` | 18 | Horizontal padding inside list rows |
| `spacing.rowV` | 16 | Vertical padding inside list rows |
| `spacing.cardInner` | 16 | Padding inside standalone cards (summary, chart) |
| `spacing.fab.trailing` | 24 | FAB right margin |
| `spacing.fab.bottom` | 24 | FAB bottom margin |

---

### Colors

| Token | AppColors reference | Usage |
|-------|--------------------|----|
| `color.primary` | `lightGreenFrameColor` | Primary brand — active states, FAB, CTA buttons, accents |
| `color.primaryDark` | `darkGreenFrameColor` | Income amounts, active sort/filter icons |
| `color.background` | `backgroundGray` (#F6F6F6) | All screen backgrounds |
| `color.surface` | `whiteFrameColor` | Cards, input fields, message bubbles |
| `color.border` | `lightGrayFrameColor` | All 1px card/input borders |
| `color.textPrimary` | `blackTextColor` | Headings, amounts, primary content |
| `color.textSecondary` | `grayTextColor` | Labels, placeholders, captions |
| `color.textTertiary` | `darkGrayTextColor` | Section dates, chart axis labels |
| `color.danger` | `.red` | Delete actions, exceeded budget |
| `color.warning` | `.orange` | Budget at 70%+ |

**Rule:** never use `.white`, `.gray`, `Color(.systemBackground)` directly. Always use `AppColors.*`.

---

### Icon Circles (Category/Entity icons in list rows)

| Context | Size | Background opacity |
|---------|------|--------------------|
| List row (transaction, budget) | 40 × 40 | `primary.opacity(0.12)` |
| Detail/sheet view | 36 × 36 | `primary.opacity(0.12)` |
| Empty state illustration | 72 × 72 | `primary.opacity(0.14)` |

---

### Borders

All cards and input fields use a single rule:  
`.overlay(RoundedRectangle(cornerRadius: X).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))`

Active/focused inputs switch to:  
`.stroke(AppColors.lightGreenFrameColor, lineWidth: 1)`

No component should use `lineWidth > 1` for UI borders (charts excluded).

---

### Shadows

| Context | Rule |
|---------|------|
| White cards on gray background | **1px border only, no shadow** |
| FAB | `shadow(color: primary.opacity(0.35), radius: 14, x: 0, y: 8)` |
| Floating sheets / banners | `shadow(color: .black.opacity(0.1), radius: 8, x: 0, y: 4)` |

Do not mix border + shadow on the same card.

---

### Divider Indent (in list rows)

Formula: `row horizontal padding (18) + icon size (40) + icon-to-text gap (12) = 70`  
→ All list dividers use `.padding(.leading, 70)`

---

### FAB (Floating Action Button)

- Size: 58 × 58, `Circle()` shape
- Background: `AppColors.lightGreenFrameColor`
- Icon: `plus`, size 22 semibold, `.white`
- Shadow: `color: lightGreenFrameColor.opacity(0.35), radius: 14, x: 0, y: 8`
- Position: `.trailing, 24` / `.bottom, 24`

---

### Full-width CTA Buttons (inside empty states / sheets)

- Height: 50
- Corner radius: 16
- Background: `AppColors.lightGreenFrameColor`
- Font: `.headline` or 16 semibold, `.white`
- `frame(maxWidth: .infinity)`

---

### List Card Container Pattern

```
VStack(spacing: 0) {
    ForEach(items) { item in
        RowView(item)
        if item != last {
            Divider().padding(.leading, 70)
        }
    }
}
.background(AppColors.whiteFrameColor)
.clipShape(RoundedRectangle(cornerRadius: 22))
.overlay(RoundedRectangle(cornerRadius: 22).stroke(AppColors.lightGrayFrameColor, lineWidth: 1))
```
