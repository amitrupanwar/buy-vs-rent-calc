# Buy vs Rent Calculator — Android Application

A multi-variable financial decision engine and Android application designed to evaluate the net wealth differential between purchasing a home (via mortgage financing or full cash) versus renting and investing the saved capital into opportunity investments.

---

## 🌟 Key Features

### 1. On-Demand Evaluation & Key Derived Metrics
- **On-Demand Calculation Engine**: Computes financial trajectories when tapping **"Evaluate Buy vs Rent"**.
- **Instant Derived Metrics**:
  - **Actual Property Cost**: Total Purchase Price + Registration/Stamp Duty.
  - **Initial Outflow**: Down Payment + Registration Amount (or Full Cash + Registration).
  - **Financed Loan Amount**: Principal loan balance.
  - **Monthly EMI**: Amortized monthly loan payment.
  - **Recommendation Summary**: Winner badge, break-even crossover month/year, and final net worth comparison.

### 2. Loan Amortization & Cash Pay Strategies
- **Loan Financing Mode**: Computes monthly interest vs principal breakdown, remaining loan balance, and EMI amortization over customizable tenures (1 to 30 years).
- **Part Prepayments**: Configurable prepayment amount and frequency (**Monthly**, **Quarterly**, **Semi-Annual**, **Annual**) to accelerate loan payoff.
- **Full Cash Pay Mode**: Automatically hides loan/EMI fields and evaluates 100% upfront cash purchase dynamics.
- **Property Expenses & Appreciation**: Models society maintenance, annual upkeep/repairs, and property tax with annual inflation/escalation rates.

### 3. Rent & Opportunity Investment Engine
- **Refundable Security Deposit**: Deposit is preserved as a refundable asset in `Rent Net Worth`.
- **Rent Escalations**: Models annual rent increases ($e_{\text{annual}}$).
- **Annual Relocation & Brokerage**: Includes recurring annual shifting/relocation and brokerage expenses.
- **Opportunity Investment Compounding**: Injects monthly cash savings ($\text{Buy Outflow} - \text{Rent Outflow}$) into a portfolio compounding at customizable frequencies (**Monthly**, **Quarterly**, **Semi-Annual**, **Annual**).
- **Capital Gains Tax**: Deducts unrealized capital gains tax to compute the net liquidation portfolio value.

### 4. Expandable Year-to-Month Progression Ledger
- **Day 1 Initial Outflow Row**: Prepend an `Initial` row reflecting starting capital allocation, down payment, deposit, and initial portfolio deposit.
- **Expandable Year Hierarchy**: Default view shows compact Year summary rows (`Year 1 ▼`, `Year 2 ▼`, ...).
- **Accounting Ledger Flow**: Tapping any Year row expands its **12 monthly sub-rows** (`│ Mo 1` to `│ Mo 12`), followed immediately by the **`★ Yr X Total ▲` Annual Tally Summary Row**!
- **Table & Cards Views**: Switch between compact cards and a multi-column horizontally scrollable table.

### 5. Two Visual Color-Grouped Sections
- **BUY SCENARIO** (Light Blue Tint `#150284C7`): EMI, Principal, Interest, Pending Loan, Expenses, Property Price, Money Outflow, Cumulative Outflow, and Buy Net Worth.
- **RENT & INVEST SCENARIO** (Light Rose Tint `#15E11D48`): Rent Paid, Money Outflow, Injected Capital ($\Delta_{\text{cash}}$), Portfolio Value, Cumulative Outflow, and Rent Net Worth.
- **Exact Digits Formatting**: Displays exact numbers with standard comma grouping (`₹5,000,000`, `₹34,720`) rather than abbreviated scale units.

### 6. Interactive Comparison Line Chart
- Placed at the very bottom of the screen.
- Features **Net Worth** and **Cumulative Outflow** curve modes with touch tooltips drawn on custom Canvas.

### 7. Local Persistence & Regionalization
- **Room Database**: Offline persistence of decision profiles with save, load, and delete capabilities.
- **Currency Regionalization**: Locale region auto-detection supporting **₹ (INR)**, **$ (USD)**, **€ (EUR)**, **£ (GBP)**, **A$ (AUD)**, **C$ (CAD)**, **¥ (JPY)**.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.0.21
- **Android Gradle Plugin**: AGP 8.7.3 / KSP 2.0.21-1.0.28
- **Target SDK**: Android 35 (Java 17 JVM target)
- **Architecture**: Clean Architecture + MVVM (Data, Domain, UI)
- **Jetpack Components**:
  - **Room Database 2.6.1**: Local persistence of decision profiles
  - **Coroutines & StateFlow**: Reactive state management
  - **ViewBinding**: Type-safe layout interaction
  - **Navigation Component**: Single-activity navigation
- **UI Framework**: Material Design 3 (MaterialCardView, MaterialButtonToggleGroup, Custom Canvas Line Chart)

---

## 📂 Project Structure

```
app/src/main/java/com/amitr/buyvsrentcalc/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt               # Room database configuration
│   │   ├── dao/HomeDecisionDao.kt        # Room DAO for profiles
│   │   └── entity/HomeDecisionEntity.kt  # DB entity schema
│   └── repository/HomeDecisionRepository.kt
├── domain/
│   ├── engine/HomeCalculatorEngine.kt   # Core 30-year monthly calculation engine
│   └── model/                            # Financial domain data models
│       ├── HomeBuyParams.kt
│       ├── HomeRentParams.kt
│       ├── InvestmentParams.kt
│       ├── HomeDecisionConfig.kt
│       ├── ScheduleDisplayRow.kt
│       └── ...
├── ui/
│   ├── home/
│   │   ├── HomeCalculatorFragment.kt     # Main calculator view & controls
│   │   ├── HomeCalculatorViewModel.kt    # StateFlow view model
│   │   ├── ComparisonLineChartView.kt    # Custom Canvas line chart
│   │   ├── ProgressionAdapter.kt         # Expandable table/card adapter
│   │   └── HorizonSliderView.kt          # Evaluation horizon slider
│   └── settings/                         # Regional currency settings
└── util/
    └── CurrencyFormatter.kt              # Locale auto-detection & digit formatting
```

---

## 📐 Mathematical Rules Specification

For complete mathematical equations, compounding schedules, EMI amortization formulas, part prepayment schedules, and tax deduction rules, refer to the [Calculation Rules Specification](CALCULATION_RULES.artifact.md).

---

## 🚀 Building & Running

### Prerequisites
- **Android Studio**: Ladybug / 2024.1+ or newer
- **JDK**: Java 17
- **Gradle**: 8.9+ (Kotlin DSL)

### Build Commands
```bash
# Clone repository
git clone git@github.com:amitrupanwar/buy-vs-rent-calc.git
cd buy-vs-rent-calc

# Build debug APK
./gradlew assembleDebug

# Run unit test suite
./gradlew testDebugUnitTest
```

---

## 📄 License & Author

Developed by **Amit Rupanwar** ([@amitrupanwar](https://github.com/amitrupanwar)).
