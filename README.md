# Food Pantry Inventory & Demand Manager

## About

**Food Pantry Inventory & Demand Manager** is an interactive desktop application built in Scala 3 and ScalaFX. It addresses **UN Sustainable Development Goal 1 (No Poverty)** and **SDG 12 (Responsible Consumption & Production)** by streamlining food bank inventory logging, tracking household dietary requirements, and automating daily resource allocation.

## Project Summary

Pantry coordinators can log donated food items (both perishable with dates, and non-perishable), record household demands/dietary restrictions, and automatically generate optimal daily distribution plans. The system utilizes a specialized **Waste-Minimizing Expiry-First** matching algorithm that prioritizes distributing food close to expiration to eliminate food waste, while automatically ensuring dietary constraints are fully respected.

### Key Features
1. **Analytics Dashboard & KPI Cards**: Provides immediate insight into total inventory count, pending requests, helped families, and lists perishables expiring in less than 3 days. Includes an interactive `PieChart` visualizer representing stock categories.
2. **Interactive Inventory Logger**: Add, view, filter, delete, and export food items. Dynamically switches inputs between perishable and non-perishable variants.
3. **Interactive Request Logger**: Log and manage recipient family requests, capturing household sizes, specific food preferences, and dietary restrictions.
4. **Daily Distribution Planner**: Implements the core distribution policy, matching compatible stock to families, logging potential food waste prevented, and executing final inventory write-offs dynamically.

---

## Technical Details

- **Language & Engine**: Scala 3.3.3, JVM 21
- **UI Toolkit**: ScalaFX 21 (running on JavaFX 21)
- **Architecture**: Domain-Driven design utilizing pure functional immutable constructs (zero `var` and zero mutable collections in business logic) and type-safe data serialization.
- **Error Handling**: Wrapped all I/O parsing and file reading/writing routines in `scala.util.Try`.

---

## Setup & Running Instructions

### Prerequisites
- **JDK 21** or later installed.
- **sbt** (Scala Build Tool) version 1.9+ installed.

### Compile
To compile the codebase with unused import warning checkers enabled:
```bash
sbt -Wunused clean compile
```

### Run Tests
To run unit tests verifying the distribution matching and model states:
```bash
sbt test
```

### Run the Application
To run the ScalaFX desktop GUI:
```bash
sbt run
```

---

## AI Use Summary

This project was built under Sunway University's **Tier C (AI-Integrated)** academic policy.
- **AI Tools Used**: Gemini, Antigravity.
- **Integration Rationale**: AI assisted in designing the ScalaFX scene layout structure and the recursive immutable allocation loop. All AI outputs were reviewed, typed-checked, refactored to conform to strict immutability criteria, and verified.
- **Logs**: Refer to [ai/interaction_log.md](file:///Users/jadewenxi/Documents/Project_23093495/ai/interaction_log.md) and [docs/ai_reflection.md](file:///Users/jadewenxi/Documents/Project_23093495/docs/ai_reflection.md) for full traceability.

---

## Third-Party Citations & Licenses

This project utilizes the following external libraries:
- **ScalaFX**: BSD 3-Clause License (https://github.com/scalafx/scalafx)
- **OpenJFX (JavaFX)**: GPLv2 with Classpath Exception (https://openjfx.io/)
- **ScalaTest**: Apache License, Version 2.0 (https://www.scalatest.org/)
- **Apache PDFBox**: Apache License, Version 2.0 (https://pdfbox.apache.org/)
