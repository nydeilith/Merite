🛡️ Merite – Gamified Task & Reward Tracker
Merite, günlük görevlerinizi yönetmenize, hedefler belirlemenize ve üretkenliğinizi oyunlaştırarak (gamification) artırmanıza yardımcı olan modern bir masaüstü uygulamasıdır.

Kullanıcı motivasyonunu artırmak için XP sistemi, sanal ekonomi (ödül mağazası) ve gelişmiş analizler sunar.

🚀 What's New in v1.1.0?
🌍 Multi-language Support: Instant switching between English 🇺🇸 and Turkish 🇹🇷.

🌙 Dark/Light Mode: Improved UI with a dynamic theme switcher and Clockify-inspired dark theme.

🔊 Sound Effects: Interactive audio feedback for tasks, rewards, and navigation.

🎨 UI Overhaul: Fixed sidebar width, better readability for charts/tables, and a polished Splash Screen.

✨ Key Features
🎮 Gamification & Task Management
Task Tracking: Add, edit, and delete tasks with assigned difficulty points.

XP & Ranking: Earn points to level up and unlock ranks (e.g., "Cyber Ninja").

Streaks: Maintain daily streaks to build consistency.

🛍️ Reward Economy
Virtual Store: Spend your hard-earned points on custom real-life rewards (e.g., "Watch a Movie", "Buy Coffee").

Balance System: Tracks earned vs. spent points automatically.

📊 Analytics & Goals
Smart Reporting: Visual pie charts and line graphs showing productivity trends over the last 30 days.

Goal Tracking: Set daily or weekly goals and monitor progress via progress bars.

💻 User Experience (UX)
Modern Interface: Clean JavaFX design with CSS styling.

Splash Screen: Professional loading screen on startup.

Portable: Runs without installation.

📥 Download & Installation (Windows)
Merite is designed to be portable. It includes a bundled Java Runtime Environment (JRE), so you do not need to install Java on your computer.

Go to the Releases Page.

Download the latest .zip file (e.g., Merite-v1.1.0-Windows.zip).

Extract the zip file to a folder.

Double-click Merite.exe to launch.

⚠️ Note: Windows SmartScreen might display a warning since this is an open-source student project and not digitally signed. Click "More Info" -> "Run Anyway" to start the app.

🛠️ Development & Building
If you want to contribute or build the project from source:

Prerequisites
JDK 17 or higher

Apache Maven

Build Commands
Clone the repository:

Bash

git clone https://github.com/nydeilith/Merite.git
cd Merite
Run in Development Mode:

Bash

mvn javafx:run
Build JAR Package:

Bash

mvn clean package
The executable jar will be located at: target/GorevTakipUygulamasi-1.0-SNAPSHOT.jar

📂 Project Structure
Plaintext

Merite/
 ├── src/
 │    ├── main/java/com/gorevtakip/
 │    │      ├── AnaUygulama.java        # Main Entry & UI Logic
 │    │      ├── VeriYoneticisi.java     # JSON Persistence Layer
 │    │      ├── SesYoneticisi.java      # Audio Manager
 │    │      └── ... (Controllers)
 │    ├── main/resources/
 │           ├── i18n/                   # Language Properties (EN/TR)
 │           ├── images/                 # Icons & Banners
 │           ├── sounds/                 # SFX Files
 │           ├── dark.css                # Dark Theme Styles
 │           └── style.css               # Light Theme Styles
 ├── target/
 └── pom.xml                             # Maven Dependencies
📄 License
This project is open source and available under the MIT License.

💬 Contact
Developed by Kerem Yiğit Elbasan

GitHub: nydeilith

LinkedIn: Kerem Yiğit Elbasan
