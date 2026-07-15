# 🌳 GitForge - Git Version Control Simulator

<div align="center">

![Java](https://img.shields.io/badge/Java-24-orange?style=for-the-badge&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-blue?style=for-the-badge)
![SQLite](https://img.shields.io/badge/SQLite-Database-003B57?style=for-the-badge&logo=sqlite)
![Maven](https://img.shields.io/badge/Maven-Build-red?style=for-the-badge&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**A desktop application that simulates Git version control operations while demonstrating core Data Structures & Algorithms through an interactive visual interface.**

</div>

---

## 📖 Overview

GitForge is a JavaFX-based desktop application developed as a semester project to simulate the core concepts of Git version control.

Instead of interacting with the actual Git CLI, GitForge models repositories, commits, branches, merges, and commit history using custom data structures and stores all project data in SQLite.

The application provides an intuitive graphical interface for understanding Git workflows while showcasing the implementation of fundamental Data Structures & Algorithms.

---

## ✨ Features

### 📁 Repository Management

- Create repositories
- Edit repository information
- Delete repositories
- Search repositories
- Repository details panel

---

### 💾 Commit Management

- Create simulated commits
- Commit history
- Commit details
- Parent-child commit relationships
- Auto-generated commit hashes

---

### 🌿 Branch Management

- Create branches
- Rename branches
- Delete branches
- Switch active branch
- Branch hierarchy

---

### 🔀 Merge Simulation

- Merge branches
- Merge history
- Merge commit generation
- Conflict simulation
- Merge statistics

---

### 🌳 Interactive Commit Graph

- Directed Acyclic Graph (DAG)
- Interactive commit visualization
- Parent-child relationships
- Merge commit visualization
- Zoom & Pan
- Branch color indicators

---

### 📊 Analytics Dashboard

- Repository statistics
- Commit analytics
- Branch analytics
- Merge statistics
- Charts and visual reports
- Activity tracking

---

### ⚙️ Settings

- Theme preferences
- Application settings
- Database information
- About section

---

## 🧠 Data Structures & Algorithms

GitForge demonstrates multiple DSA concepts through practical implementation.

| Data Structure | Purpose |
|---------------|---------|
| Linked List | Commit History |
| Tree | Branch Hierarchy |
| Directed Acyclic Graph (DAG) | Commit Graph Visualization |
| HashMap | Analytics Cache |
| Queue (BFS) | Graph Traversal |
| Stack (DFS) | Graph Navigation |
| ArrayList | UI Collections |

---

## 🏗 Architecture

```
JavaFX UI
     │
Controllers
     │
Service Layer
     │
DAO Layer
     │
SQLite Database
```

---

## 🛠 Tech Stack

### Frontend

- JavaFX
- FXML
- CSS

### Backend

- Java 24

### Database

- SQLite

### Build Tool

- Maven

### Libraries

- MaterialFX
- Ikonli
- JUnit 5

---

## 📂 Project Structure

```
GitForge
│
├── src
│   ├── main
│   │   ├── java
│   │   ├── resources
│   │   └── database
│   │
│   └── test
│
├── docs
│
├── pom.xml
│
└── README.md
```

---

## 🚀 Getting Started

### Clone Repository

```bash
git clone https://github.com/Laraibkhalid111/GitForge.git
```

### Navigate

```bash
cd GitForge
```

### Run

```bash
./mvnw javafx:run
```

---


---

## 🎯 Learning Objectives

- Understand Git workflows
- Visualize version control concepts
- Apply Data Structures in a real-world project
- Build desktop applications using JavaFX
- Work with relational databases using SQLite

---

## 🔮 Future Improvements

- Real Git integration using JGit
- Remote repository simulation
- Pull Request simulation
- Conflict resolution interface
- User authentication
- Team collaboration features
- File diff viewer
- Export repository reports

---

## 👨‍💻 Author

**Laraib Khalid**

- BS Software Engineering
- COMSATS University Islamabad, Abbottabad Campus

GitHub: https://github.com/Laraibkhalid111

---

## ⭐ If you like this project

Consider giving the repository a ⭐ on GitHub.

---

<div align="center">

**GitForge — Understanding Git Through Visualization**
