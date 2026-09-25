# Product Requirements Document (PRD): Buy vs. Rent Calculator App

## 1. Executive Summary & Core Purpose
The **Buy vs. Rent Calculator** is an offline-first Android application designed to help users evaluate financial decisions between purchasing (buying) an asset versus renting, leasing, or using on-demand subscription services.

While applicable to any asset decision, the app features tailored specialized modules:
1. **Real Estate / Home**: Buy (Mortgage Loan / Full Cash) vs. Rent (Housing).
2. **Vehicle / Automobile**: 4-Way Comparison — **Buy with Loan** vs. **Buy with Full Cash** vs. **Full Lease** vs. **On-Demand Ride-Hailing (Uber / Taxi / Subscription)** based on commute requirements.
3. **Custom Asset**: Generic capital purchase vs. rental/subscription decision.

The application computes a multi-variable financial model comparing cash outflows, operational expenses, loan amortization, asset depreciation/appreciation, and investment returns across multiple options (**FD, RD, Mutual Funds / Stocks**) net of taxes.

> **Note on Neutral Costs (e.g., Highway Tolls)**: Universal expenses that are incurred equally regardless of decision mode (such as highway tolls, which are paid whether driving an owned car, leased car, or riding in a taxi) are excluded from the delta model to ensure a clean, accurate financial differential.

---

## 2. Target Use Cases & Decision Scenarios

### 2.1 Home Purchase vs. Renting
- Evaluates down payment, loan EMI, society/HOA maintenance, property tax, property appreciation, rent escalation, security deposit, and investment returns from investing saved money into **FD, RD, or Mutual Funds**.

### 2.2 Vehicle / Automobile Decision Matrix
The vehicle module compares **four distinct financial paths** evaluated against the user's actual commute distance:

| Vehicle Strategy | Acquisition Mode | Key Cash Outflows | Equity / Resale Value |
| :--- | :--- | :--- | :--- |
| **1. Buy via Loan** | Financing | Down payment, Loan EMI, Fuel, Servicing, Insurance, Tyres, Wash, Dedicated Parking | Depreciated Resale Value at end minus remaining loan balance |
| **2. Buy via Full Cash** | 100% Upfront Cash | Full Purchase Price, Fuel, Servicing, Insurance, Tyres, Wash, Dedicated Parking (High opportunity cost) | Depreciated Resale Value at end of tenure |
| **3. Full Lease** | Monthly Subscription | Fixed Monthly Lease (Includes maintenance & insurance; tax deductible for corporate lease) | Zero equity at end of lease term |
| **4. On-Demand Ride-Hailing** | Uber / Taxi / Cab | Per-km fare + Base fare + Surge multiplier (Zero upkeep, zero insurance, zero down payment, zero parking) | Zero asset; Saved capital fully invested in FD / RD / Stocks |

---

## 3. Detailed Financial Inputs & Parameters

### 3.1 Asset & Profile Parameters
| Parameter | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| **Asset Category** | Category | Home / Car / Custom | Selects domain-specific input fields |
| **Profile Title** | String | "Tesla Model 3 Buy vs Uber" | Identifier for saved decision profile |
| **Total Purchase Price** | Currency | $35,000 / ₹1,500,000 | Total upfront purchase cost |
| **Asset Value Change Rate** | Percentage | -12.0% (Car) / +5.0% (Home) | Annual appreciation (+) or depreciation (-) rate |
| **Evaluation Horizon** | Years | 5 Years (Car) / 20 Years (Home) | Duration of comparison analysis (1 to 30 years) |

### 3.2 Vehicle Commute & Operational Inputs (Car Category)
| Parameter | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| **Monthly Commute Distance** | Distance | 1,200 km / month | Estimated monthly travel requirements |
| **Fuel Type** | Selection | Petrol / Diesel / EV / CNG | Fuel category for efficiency calculation |
| **Fuel Efficiency (Mileage)** | Efficiency | 15 km/L (35 mpg) / 6 km/kWh | Mileage / energy consumption rate |
| **Fuel / Energy Price** | Currency / Unit | $1.20 / Litre (₹100 / L) | Current price per unit of fuel/electricity |
| **Periodic Servicing & Repair** | Currency / Year | $300 / year | Regular routine maintenance cost |
| **Tyre Replacement Interval & Cost** | Distance / Cost | Every 40,000 km ($400) | Wear and tear component replacement |
| **Vehicle Wash & Detailing** | Currency / Month | $25 / month | Monthly cleaning and upkeep |
| **Annual Insurance & Registration** | Currency / Year | $1,000 / year (with NCB reduction) | Comprehensive insurance & road tax |
| **Dedicated Parking / Garage** | Currency / Month | $30 / month | Recurring home/office parking specific to vehicle ownership |

*(Note: Tolls are excluded as they are paid identically in all modes.)*

### 3.3 Vehicle Lease & Ride-Hailing (Uber / Taxi) Parameters
| Parameter | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| **Full Lease Monthly Cost** | Currency / Month | $600 / month | All-inclusive fixed monthly lease fee |
| **Lease Includes Maintenance/Ins.**| Toggle | True | Whether lease covers service & insurance |
| **Tax Benefit on Lease** | Percentage | 0% (or income tax slab e.g. 30%) | Tax deduction benefit if company lease |
| **Uber / Cab Fare Rate per Km** | Currency / Km | $1.20 / km (₹25 / km) | Distance-based ride-hailing charge |
| **Uber Base Fare & Minimums** | Currency / Trip | $3.00 / trip | Base fee per ride |
| **Average Monthly Rides** | Number | 40 Rides / month | Estimated number of trips per month |
| **Surge / Peak Pricing Factor** | Multiplier | 1.1x | Average surge multiplier applied to cab fares |

### 3.4 Investment Options for Saved Capital (FD, RD, Stocks)
When renting/leasing or using cabs, initial saved capital (Down Payment + Buying Fees) and monthly cash savings (Buy Outflow - Rent Outflow) are invested.

| Investment Instrument | Mode | Key Parameters | Description |
| :--- | :--- | :--- | :--- |
| **Fixed Deposit (FD)** | Lump Sum | Interest Rate % (e.g. 7.0% p.a.), Tax Slab % (TDS) | Secured lump-sum deposit compounding quarterly/annually |
| **Recurring Deposit (RD)** | Monthly Systematic | Interest Rate % (e.g. 6.8% p.a.), Tax Slab % | Monthly recurring savings invested at guaranteed rate |
| **Mutual Funds / Stocks** | Lump Sum + SIP | Expected Return % (e.g. 11.0% p.a.), LTCG/STCG Tax % (12.5%) | Equity investment with market compounding & capital gains tax |
| **Custom Allocation** | Split % | % split between FD, RD, and Stocks | Hybrid portfolio matching user risk preference |

---

## 4. Calculation Engine & Multi-Scenario Logic

### 4.1 Vehicle Ownership Outflow Equation
$$\text{Monthly Buy Outflow}_m = \text{EMI}_m + \text{Fuel Cost}_m + \text{Pro-rated Service}_m + \text{Tyre Fund}_m + \text{Insurance}_m + \text{Wash}_m + \text{Dedicated Parking}_m$$

Where:
$$\text{Fuel Cost}_m = \frac{\text{Monthly Commute Distance}}{\text{Mileage}} \times \text{Fuel Price}$$

### 4.2 Ride-Hailing Outflow Equation
$$\text{Monthly Ride-Hailing Outflow}_m = (\text{Total Km} \times \text{Rate/Km} + \text{Total Rides} \times \text{Base Fare}) \times \text{Surge Factor}$$

### 4.3 Investment Portfolio Growth Engine
For each month $m$:
- **FD Portfolio**: Lump sum grows at $\left(1 + \frac{r_{\text{fd}}}{4}\right)^{4/12}$ with tax deducted annually on interest accrual.
- **RD / SIP Portfolio**: Monthly surplus $\Delta_m = \text{Buy Outflow}_m - \text{Rent/Cab Outflow}_m$ added to recurring deposit/SIP.
- **Net Wealth Comparison**:
  $$\text{Buy Net Worth}_m = \text{Depreciated Vehicle / Home Value}_m - \text{Loan Principal Remaining}_m$$
  $$\text{Rent/Cab Net Worth}_m = \text{Post-Tax Portfolio Value (FD + RD + Stocks)}_m + \text{Refundable Deposit}$$

---

## 5. UI/UX Requirements & Visualizations

### 5.1 Dynamic Input Wizard & Presets
- **Category Picker**: Home, Car (Commute-based), Custom.
- **Commute Calculator Widget** (for Car mode): Slider for daily/monthly travel distance, fuel type selector, and mileage preset.
- **Investment Preferences Drawer**: Choose preferred investment bucket (**FD**, **RD**, **Mutual Funds**, or **Mix**), input interest rates, and select tax slab.

### 5.2 Multi-Curve Amortization & Difference Chart
- **4-Way Comparison Visualizer**:
  - Curve 1: **Buy Option (Loan) Net Worth**
  - Curve 2: **Buy Option (Full Cash) Net Worth**
  - Curve 3: **Lease Option Net Worth**
  - Curve 4: **On-Demand Ride-Hailing (Uber) Net Worth**
- **Break-Even & Crossover Markers**: Highlights the exact month where owning becomes cheaper than ride-hailing/leasing based on travel volume.
- **Cost Breakdown Donut Chart**: Visual pie chart of monthly car expenses (Fuel vs. EMI vs. Maintenance vs. Insurance vs. Depreciation).

### 5.3 Saved Decision Checks & Recheck
- **Room Database Storage**: Local storage for all decision profiles.
- **Parameter Sensitivity Slider**: Instant live updating chart when tweaking commute distance (e.g. "What if my commute increases from 1,000 km to 2,000 km/month?").
- **Side-by-Side Comparison Matrix**: Compare saved scenarios in a tabular grid.

---

## 6. Technical Architecture & Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Clean Architecture (Data, Domain, UI layers)
- **Database**: Room DB (Offline storage of profiles & commute presets)
- **UI Framework**: Material Design 3 (Jetpack Compose / ViewBinding)
- **Charting**: Vico / MPAndroidChart / Compose Canvas for multi-curve line charts and breakdown donuts.
- **Asynchronous Processing**: Kotlin Coroutines & StateFlow.

---

## 7. Verification & Testing Strategy
1. **Financial Formula Tests**:
   - Validate FD quarterly compounding with TDS tax deduction.
   - Validate RD monthly compounding calculations.
   - Validate Vehicle depreciation curves and per-km fuel cost math.
2. **Multi-Scenario Comparison Tests**:
   - Verify 4-way decision logic (Buy via Loan vs Buy via Cash vs Lease vs Uber) with zero divide/null safety.
3. **UI & Persistence Tests**:
   - Verify parameter recheck, slider adjustments, and Room DB updates.
