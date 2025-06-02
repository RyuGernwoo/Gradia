# Gradia
---
# Overview
Gradia is a combination of Grade and AI, an app service that provides personalized learning management and grade prediction services based on the user's coursework, learning time, and grade data.

## Goals
* **Provide Personalized Learning Environment**
* **Encourage Active Learning Participation**
* **Support Efficient Time Management**

# Features
## User-Customized Subject Management
* **Subject Selection** : Users can select subjects from the list of subjects registered in the system, or add them to the mangement list by directly entering the subject name.
* **Subject Information Management** : Related information such as the syllabus and exam schedule of the selected subject can be recorded and managed in the system.

## Record and Manage Learning time
* **Enter Daily Learning Time** : Users can easily enter daily learning time for each subject
* **Learning Time Statistics** : Based on the entered learning time, statistics and trends for learning time by subject, day, week, and month are provided visually through graphs
* **Set Learning Time Goals** : User can set learning time goals by subject or overall and check the goal achievement rate

## Difficulty Analysis and Prediction Model Learning
* **User Data Collection** : Collect users' subject-specific study time and actual grade data
* **Model Learning** : Based on the collected data, learn a subject-specific difficulty, study time-to-grade prediction model using a machine learning algorithm
* **Difficulty Provision** : Calculate the difficulty of each subject and provide it to users.

## Personalized Learning Analysis and Recommendation
* **Present Predicted Grades** : Predicted grades for each subject are provided based on the user's study time data
* **Recommend Learning Priority** : Predicted grades and subject difficulty are recommended to the user
* **Recommend Optimal Study Schedule** : Predicted study time is distributed based on the user's study priority and the remaining period is provided
* **Analysis of Strengths and Weeknesses** : Analyze strong and weak subjects based on user data and provides feedback on the direction of improvement
* **Provides Customized Feedback** : Predicted learning advice and improvement directions based on the results of the study pattern analysis
* **Provides Goal Achievement Rate** : Visually provides the achievement rate compared to the goals (Study time, Grades, etc..) set by user

# Expected Effects
* **Increased Learning Efficiency** : Maximize learning efficiency through data-based analysis and customized information
* **Improved Self-Directed Learning Ability** : Improved Self-directed learning ability by increasing understanding of individual learning situations
* **Imporved Grades** : Improved learning performance through efficient learning strategies and time management

---

# Team
## 권영훈
### Backend Server Development
Build backend server using FastAPI to handle API requests, manage data, and serve AI service.

Backend Repository: [gradia-backend](https://github.com/windopper/gradia-backend)
### AI Analysis UI Implementation
Designed and developed an interface that allows users to interact with AI-driven data analysis.
### AI Prompt Engineering & Backend Integration
Created and optimized AI prompts tailored for specific analytical tasks and integrated into the backend.
### Everytime Timetable Parsing
Implemented functionality to parse and extract class schedule data from Everytime, enabling integration of user timetable information into the analysis system.
### Data Synchronization System
Implemented a real-time data synchronization system to ensure consistent and up-to-date data flow between the frontend, backend, and external sources.

## 류건우
### Home & Profile UI Design and Feature Implementation
- Designed user interfaces and implemented key features for the home and user profile sections.
### Social Login System Implementation
- Developed and integrated a system enabling users to log in via social media accounts.
### Integrate account information and profile
- Connected and synchronized user account data with their respective profile displays.

## 이정우
### Android RoomDB Integration and Data Management 
- Designed the application's local database schema.
- Implemented database construction and CRUD (Create, Read, Update, Delete) functionalities using Android RoomDB Library.
### Core Feature Development for Study Time Recording and Timeline 
#### Record Features
- Implemented UI state preservation for running timer/stopwatch services and state restoration upon app restart, ensuring data integrity even after forced app termination.
- Developed background Timer and Stopwatch services utilizing ForegroundService, Runnable, and BroadcastReceiver.
Implemented the dialog interface for adding new study sessions.
#### Timeline Features
- Implemented daily/weekly study session visualization using a custom ScheduleView widget.
- Integrated functionality to view details and edit study sessions upon selection within the timeline.
- Implemented the display of statistical information based on daily study data.
#### Subject Management Feature Design and Development 
- Designed the UI/UX for comprehensive subject management screens (including list, details, add, and edit functionalities).
- Implemented subject data CRUD functionalities and associated user interface logic.
#### Development of Key UI Custom Widgets 
- Developed a Pomodoro-style timer custom widget.
  - [Android-Pomodoro-Timer-Widget](https://github.com/orion-gz/Android-Pomodoro-Timer-Widget)
- Developed a custom ScheduleView widget for displaying schedules in a timetable format.
  - [Android-Schedule-View-Widget](https://github.com/orion-gz/Android-Schedule-View-Widget)

## 강지윤
#### Subject tab implementation
- List screen that summarizes subject name search box, sorting bar, and subject information in box form
: Add subjects (as subject db items), modify, subject details, and check box-based add-ons
- (When the Save button is clicked on the bottom, it returns to the Room DB and the subject list, pre-loads existing information when modified, and when the Delete button is clicked, the confirmation pop-up is deleted and the list is updated from the DB)
- Add subject-related entities, repository, ViewModel structures
#### Analysis tab implementation
- Use MPAndroid Chart to visualize weekly cumulative data in a bar form
: Separate tabs by subject and day of the week to implement graphs
- Visualize achievement relative bar(progress par) to learning objectives in horizontal bar form
#### Tutorial implementation
- Use the Spotlight 2.0.4 library to step-by-step guide each feature in the bottom bar when the first app is launched
: Introduce each feature in overlay form
: Designed to run tutorials only once with Shared Preferences

---
# Related Projects
- [Gradia_ML](https://github.com/windopper/gradia-ml)
- [Gradia-BackEnd](https://github.com/windopper/gradia-backend)
- [Android-Pomodoro-Timer-Widget](https://github.com/orion-gz/Android-Pomodoro-Timer-Widget)
- [Android-Schedule-View-Widget](https://github.com/orion-gz/Android-Schedule-View-Widget)
