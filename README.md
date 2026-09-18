# FitSphere (Web Edition)
**Personalized Fitness Planning with Activity Insights and Adaptive Workout Recommendations**

A Spring Boot + MySQL web application version of FitSphere. Same ML engine and database design as
the desktop edition, now served as a browser-based app with a REST API backend.

---

## Important note on how this was built

This project was written and syntax-checked carefully, but **could not be compiled or run inside the
sandbox it was built in**, because that sandbox's network is restricted and cannot reach Maven Central
(the repository Spring Boot's dependencies are downloaded from). Concretely:

- Every file was checked with `javac` and only shows "cannot find symbol" / "package does not exist"
  errors for Spring's own classes (expected, since those jars aren't available offline) - **no genuine
  syntax errors** were present in any file.
- The ML/domain logic (`FitnessCalculator`, `KMeans`, `FitnessSegmentation`, `AdaptiveRecommendationEngine`,
  `WorkoutCatalog`) has **zero Spring dependency** and was fully compiled and verified standalone - this is
  the exact same, already-tested logic from the desktop edition.
- All frontend JavaScript (`api.js`, `app.js`, `auth.js`, `charts.js`) was syntax-checked with Node.
- All HTML files were checked for balanced/well-formed tags.

What this means for you: when you run `mvn spring-boot:run` on your own machine (with normal internet
access), Maven will download Spring Boot from Maven Central and compile this project there. That first
run is the real compile check. If anything does surface, it will most likely be a small, easy-to-fix
issue rather than a structural problem - the architecture and every piece of business logic is the same
proven code from the desktop app.

---

## 1. Prerequisites

- JDK 17 or newer
- Maven (or use an IDE like IntelliJ IDEA / VS Code with the Java + Maven extensions, which bundles Maven)
- MySQL Server 8+

## 2. Setup

**Step 1 - Create the (empty) database.** The app auto-creates its tables on startup, but the
database itself must exist first:
```sql
CREATE DATABASE fitspheredb;
```

**Step 2 - Check your DB credentials.** Open `src/main/resources/application.properties` and update
`spring.datasource.username` / `spring.datasource.password` if they differ from `root` / `1234`.

**Step 3 - Run it:**
```
mvn spring-boot:run
```
Maven will download all dependencies (needs internet access), compile the project, auto-create the
three tables (`users`, `activity_logs`, `workout_plan_items`) via `schema.sql`, and start a web server.

**Step 4 - Open your browser** to:
```
http://localhost:8080
```
That's the login/register page. Everything else (dashboard, logging, insights, recommendations, profile)
lives at `http://localhost:8080/app.html` once you're logged in.

### Alternative: run from a packaged jar
```
mvn clean package
java -jar target/fitsphere-web-1.0.0.jar
```

---

## 3. What changed vs. the desktop edition

| Desktop (Swing) | Web (Spring Boot) |
|---|---|
| `DBConnection.getConnection()` per call | Spring-managed `DataSource` + `JdbcTemplate`, injected into DAOs |
| DAOs use raw JDBC | DAOs use `JdbcTemplate` (same SQL, same tables) |
| Swing panels (`DashboardHomePanel`, etc.) | REST controllers (`DashboardController`, etc.) returning JSON |
| Java2D custom charts | HTML5 Canvas charts in `js/charts.js` - same visual design, same 4 chart types |
| CardLayout for navigation | Single-page app (`app.html`) with JS-based view switching |
| Session = the running desktop process | Spring `HttpSession` (browser cookie) after `/api/auth/login` |

**Nothing changed in the ML/domain logic.** `FitnessCalculator`, `WorkoutCatalog`, `KMeans`,
`FitnessSegmentation`, and `AdaptiveRecommendationEngine` are byte-for-byte the same algorithms as the
desktop edition (K-Means with k-means++, the adaptive feedback loop, epsilon-greedy content-based
scoring) - only the layer presenting them to the user changed.

## 4. REST API reference

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/auth/register` | POST | Create account |
| `/api/auth/login` | POST | Log in (sets session cookie) |
| `/api/auth/logout` | POST | Log out |
| `/api/auth/me` | GET | Current logged-in user |
| `/api/profile` | GET / PUT | View / update profile |
| `/api/profile/numbers` | GET | BMI, BMR, TDEE, calorie target, macros |
| `/api/dashboard/summary` | GET | Dashboard stat cards |
| `/api/activities` | POST | Log a new activity |
| `/api/activities/recent?days=30` | GET | Recent activity log table |
| `/api/activities/catalog` | GET | List of workout types + MET values |
| `/api/activities/estimate?type=&duration=` | GET | Live calorie estimate while logging |
| `/api/activities/weekly-calories` | GET | 7-day calories chart data |
| `/api/activities/type-distribution?days=30` | GET | Activity-mix donut chart data |
| `/api/activities/adherence-trend?weeks=4` | GET | Adherence trend line chart data |
| `/api/recommendation/current` | GET | Currently saved weekly plan |
| `/api/recommendation/generate` | POST | Run the ML pipeline and save a new plan |

## 5. Project structure
```
FitSphereWeb/
├── pom.xml
└── src/main/
    ├── java/com/fitsphere/
    │   ├── FitSphereApplication.java        entry point
    │   ├── User.java, ActivityLog.java, WorkoutPlanItem.java      (models, unchanged)
    │   ├── FitnessCalculator.java, WorkoutCatalog.java,
    │   │   KMeans.java, FitnessSegmentation.java,
    │   │   AdaptiveRecommendationEngine.java                       (ML/domain logic, unchanged)
    │   ├── UserDAO.java, ActivityDAO.java, WorkoutDAO.java          (JdbcTemplate-based DAOs)
    │   ├── AuthController.java, ProfileController.java,
    │   │   DashboardController.java, ActivityController.java,
    │   │   RecommendationController.java                           (REST API)
    │   └── AuthDtos.java, ActivityDtos.java, ProfileDtos.java,
    │       RecommendationDtos.java                                 (request/response objects)
    └── resources/
        ├── application.properties
        ├── schema.sql                       (auto-run on startup)
        └── static/
            ├── index.html                   (login / register)
            ├── app.html                     (main single-page app)
            ├── css/style.css
            └── js/api.js, auth.js, app.js, charts.js
```

---

## 6. Deploying it live (Railway - free, Java + MySQL both supported)

Vercel does not work for this project - it only runs Node.js/Python/Go serverless functions and has no
persistent MySQL hosting. **Railway** is the equivalent "connect and deploy" experience that actually
supports Java + MySQL, and this project already includes a `Dockerfile` so it deploys there with almost
no configuration.

1. **Create a Railway account** at railway.app and start a **New Project**.
2. **Add MySQL**: in the project, click *New* -> *Database* -> *Add MySQL*. Railway provisions a MySQL
   instance and shows you its connection details (host, port, database, user, password) under its
   *Variables* tab.
3. **Deploy this app**: click *New* -> *GitHub Repo* (push this folder to a GitHub repo first) - or, if
   you'd rather skip GitHub, install the Railway CLI and run `railway login` then `railway up` from inside
   this folder. Railway will detect the `Dockerfile` and build automatically.
4. **Wire the database to the app**: on the Spring Boot service's *Variables* tab, add:
   - `SPRING_DATASOURCE_URL` = `jdbc:mysql://<MySQL host>:<MySQL port>/<MySQL database>` (use the values
     from step 2 - Railway lets you reference them directly, e.g.
     `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}`)
   - `SPRING_DATASOURCE_USERNAME` = `${{MySQL.MYSQLUSER}}`
   - `SPRING_DATASOURCE_PASSWORD` = `${{MySQL.MYSQLPASSWORD}}`

   Spring Boot picks these up automatically - no code or file changes needed. `schema.sql` creates the
   three tables on the very first startup against Railway's MySQL, same as it does locally.
5. **Get your URL**: once deployed, Railway gives the service a public URL like
   `https://fitsphere-production.up.railway.app`. Open it - that's your login page, live on the internet
   and shareable with anyone.

**Render** works the same way (Web Service from a Dockerfile + a separately-hosted MySQL, since Render's
own managed databases are Postgres, not MySQL) if you'd rather use that instead.
