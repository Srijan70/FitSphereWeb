let currentUser = null;
const DAY_LABELS = { MON: "MON", TUE: "TUE", WED: "WED", THU: "THU", FRI: "FRI", SAT: "SAT", SUN: "SUN" };
const SEGMENT_COLORS = { Beginner: "#4089ff", Intermediate: "#ff914d", Advanced: "#9664ff" };
const DONUT_PALETTE = ["#00bf8f", "#4089ff", "#ff914d", "#9664ff", "#ff5a5a", "#78808c"];

function niceGoal(goal) {
  return ({ WEIGHT_LOSS: "Weight Loss", MUSCLE_GAIN: "Muscle Gain", ENDURANCE: "Endurance" })[goal] || "Maintenance";
}

// ---------------- Init ----------------
(async () => {
  try {
    currentUser = await Api.get("/api/auth/me");
  } catch (e) {
    window.location.href = "index.html";
    return;
  }

  const hour = new Date().getHours();
  const part = hour < 12 ? "Good morning" : (hour < 18 ? "Good afternoon" : "Good evening");
  document.getElementById("greeting").textContent = `${part}, ${currentUser.name.split(" ")[0]}!`;
  document.getElementById("todayDate").textContent = new Date().toLocaleDateString(undefined, {
    weekday: "long", year: "numeric", month: "short", day: "numeric",
  });

  document.querySelectorAll(".nav-btn[data-view]").forEach(btn => {
    btn.addEventListener("click", () => switchView(btn.dataset.view));
  });
  document.getElementById("logoutBtn").addEventListener("click", async () => {
    await Api.post("/api/auth/logout");
    window.location.href = "index.html";
  });

  await loadDashboard();
  await populateActivityCatalog();
})();

function switchView(view) {
  document.querySelectorAll(".view").forEach(v => v.classList.add("hidden"));
  document.getElementById("view-" + view).classList.remove("hidden");
  document.querySelectorAll(".nav-btn[data-view]").forEach(b => b.classList.toggle("active", b.dataset.view === view));

  if (view === "dashboard") loadDashboard();
  else if (view === "log") loadLogView();
  else if (view === "insights") loadInsights();
  else if (view === "reco") loadRecommendations();
  else if (view === "profile") loadProfile();
}

// ---------------- Dashboard ----------------
async function loadDashboard() {
  const summary = await Api.get("/api/dashboard/summary");
  const weekly = await Api.get("/api/activities/weekly-calories");

  const cards = document.getElementById("statCards");
  cards.innerHTML = `
    <div class="card"><div class="stat-title">BMI</div>
      <div class="stat-value" style="color:var(--accent-blue);">${summary.bmi.toFixed(1)}</div>
      <div class="stat-sub">${summary.bmiCategory}</div></div>
    <div class="card"><div class="stat-title">Daily Calorie Target</div>
      <div class="stat-value" style="color:var(--accent);">${Math.round(summary.calorieTarget)}</div>
      <div class="stat-sub">kcal / day</div></div>
    <div class="card"><div class="stat-title">Current Streak</div>
      <div class="stat-value" style="color:var(--accent-orange);">${summary.streak}</div>
      <div class="stat-sub">${summary.streak === 1 ? "day" : "days"}</div></div>
    <div class="card"><div class="stat-title">Goal</div>
      <div class="stat-value" style="color:var(--accent-purple); font-size:19px;">${niceGoal(summary.goal)}</div>
      <div class="stat-sub">Keep going!</div></div>
  `;

  Charts.drawLine(document.getElementById("dashboardChart"), "Calories Burned - Last 7 Days",
      weekly.labels, weekly.values, "#00bf8f");
}

// ---------------- Log Activity ----------------
let workoutCatalog = [];

async function populateActivityCatalog() {
  workoutCatalog = await Api.get("/api/activities/catalog");
  const select = document.getElementById("logType");
  select.innerHTML = workoutCatalog.map(w => `<option value="${w.name}">${w.name}</option>`).join("");
  select.addEventListener("change", updateEstimate);
  document.getElementById("logDuration").addEventListener("input", updateEstimate);
  document.getElementById("logRating").addEventListener("input", () => {
    document.getElementById("ratingVal").textContent = document.getElementById("logRating").value;
  });
  document.getElementById("logSubmitBtn").addEventListener("click", submitActivity);
  updateEstimate();
}

async function updateEstimate() {
  const type = document.getElementById("logType").value;
  const duration = parseInt(document.getElementById("logDuration").value || "0", 10);
  if (!type) return;
  try {
    const resp = await Api.get(`/api/activities/estimate?type=${encodeURIComponent(type)}&duration=${duration}`);
    document.getElementById("logEstimate").textContent = `~${Math.round(resp.estimatedCalories)} kcal`;
  } catch (e) { /* ignore */ }
}

async function submitActivity() {
  const payload = {
    activityType: document.getElementById("logType").value,
    durationMinutes: parseInt(document.getElementById("logDuration").value, 10),
    steps: parseInt(document.getElementById("logSteps").value || "0", 10),
    intensityRating: parseInt(document.getElementById("logRating").value, 10),
    completed: document.getElementById("logCompleted").checked,
  };
  try {
    await Api.post("/api/activities", payload);
    alert("Activity logged!");
    await loadLogView();
  } catch (e) {
    alert(e.message);
  }
}

async function loadLogView() {
  const logs = await Api.get("/api/activities/recent?days=30");
  const tbody = document.getElementById("logTableBody");
  tbody.innerHTML = logs.map(l => `
    <tr>
      <td>${l.logDate}</td>
      <td>${l.activityType}</td>
      <td>${l.durationMinutes} min</td>
      <td>${Math.round(l.caloriesBurned)}</td>
      <td>${l.steps}</td>
      <td>${l.intensityRating}/5</td>
      <td>${l.completed ? "Yes" : "No"}</td>
    </tr>
  `).join("");
}

// ---------------- Insights ----------------
async function loadInsights() {
  const weekly = await Api.get("/api/activities/weekly-calories");
  Charts.drawBar(document.getElementById("chartWeeklyCalories"), "Calories Burned - This Week",
      weekly.labels, weekly.values, "#00bf8f");

  const adherence = await Api.get("/api/activities/adherence-trend?weeks=4");
  Charts.drawLine(document.getElementById("chartAdherence"), "Adherence Trend (%)",
      adherence.labels, adherence.values, "#4089ff");

  const dist = await Api.get("/api/activities/type-distribution?days=30");
  Charts.drawDonut(document.getElementById("chartDistribution"), "Activity Mix - Last 30 Days",
      dist.labels, dist.values, DONUT_PALETTE);

  const numbers = await Api.get("/api/profile/numbers");
  Charts.drawGauge(document.getElementById("chartBmiGauge"), "BMI Gauge", numbers.bmi, 15, 40, numbers.bmiCategory);
}

// ---------------- Recommendations ----------------
// ---------------- Recommendations ----------------

async function loadRecommendations() {

  const resp = await Api.get("/api/recommendation/current");

  renderRecommendation(resp);

  document.getElementById("generatePlanBtn").onclick = async () => {

    try {

      const result = await Api.post("/api/recommendation/generate");

      renderRecommendation(result);

    } catch (e) {

      alert(e.message);

    }

  };
}


function renderRecommendation(resp) {

  const badge = document.getElementById("segmentBadge");

  badge.textContent = "Segment: " + resp.segment;

  badge.style.background =
    SEGMENT_COLORS[resp.segment] || "#4089ff";


  document.getElementById("recoExplanation").textContent =
    resp.explanation;


  const grid = document.getElementById("planGrid");

  const days = [
    "MON",
    "TUE",
    "WED",
    "THU",
    "FRI",
    "SAT",
    "SUN"
  ];


  /*
   * ============================================================
   * MUSCLE GAIN - 5 DAY PPL
   * ============================================================
   */

  const exercises = {

    "Push": [
      "Incline DB Press — 3 × 8–12",
      "Bench Press — 3 × 8–12",
      "Pec Dec Fly — 3 × 10–15",
      "Lateral Raise — 3 × 12–15",
      "V-Bar Pushdown — 3 × 10–15",
      "Overhead Tricep Extension — 3 × 10–15"
    ],


    "Pull + Abs": [
      "Lat Pulldown — 3 × 8–12",
      "Bent-Over Barbell Row in Smith Machine — 3 × 8–12",
      "Low Row Machine Chest Supported — 3 × 8–12",
      "Rear Delt Fly — 3 × 12–15",
      "EZ-Bar Curl — 3 × 10–12",
      "Hammer Curl — 3 × 10–12",
      "Cable Crunch — 3 × 12–15"
    ],


    "Legs": [
      "Squat — 3 × 5–8",
      "Romanian Deadlift — 3 × 6–10",
      "Hack Squat — 3 × 8–12",
      "Leg Curl — 3 × 8–12",
      "Leg Extension — 2 × 10–15",
      "Calf Raise — 3 × 8–15"
    ],


    "Chest + Shoulders + Triceps": [
      "Incline Machine Press — 3 × 8–12",
      "Low to High DB Fly — 3 × 10–15",
      "Shoulder Press — 3 × 8–12",
      "Cable Lateral Raise — 3 × 12–15",
      "V-Bar Tricep Pushdown — 3 × 10–15",
      "Skull Crushers / Overhead Extension — 3 × 10–15",
      "Pec Dec — 3 × 10–15"
    ],


    "Back + Biceps + Abs": [
      "Lat Pulldown — 3 × 8–12",
      "Close-Grip Lat Pulldown — 3 × 8–12",
      "T-Bar Row — 3 × 8–12",
      "Neutral-Grip Seated Row — 3 × 8–12",
      "Cable Pullover — 3 × 10–15",
      "Face Pull — 3 × 12–15",
      "EZ-Bar Curl — 3 × 10–12",
      "Hammer Curl — 3 × 10–12",
      "Cable Crunch — 3 × 12–15"
    ],


    /*
     * ============================================================
     * UPPER / LOWER
     * WEIGHT LOSS + ENDURANCE
     * ============================================================
     */

    "Upper A": [
      "Incline DB Press — 3 × 10–15",
      "Lat Pulldown — 3 × 10–15",
      "Seated Chest Press — 3 × 10–15",
      "Seated Cable Row — 3 × 10–15",
      "Cable Lateral Raise — 3 × 12–15",
      "Face Pull — 3 × 12–15",
      "Rope Pushdown — 2 × 12–15",
      "Cable Curl — 2 × 12–15"
    ],


    "Lower A": [
      "Hack Squat — 3 × 10–15",
      "Romanian Deadlift — 3 × 8–12",
      "Leg Press — 3 × 12–15",
      "Leg Curl — 3 × 12–15",
      "Leg Extension — 2 × 12–15",
      "Calf Raise — 3 × 12–20"
    ],


    "Upper B": [
      "Machine Chest Press — 3 × 10–15",
      "Close-Grip Lat Pulldown — 3 × 10–15",
      "Incline DB Press — 3 × 10–15",
      "Chest-Supported Row — 3 × 10–15",
      "Rear Delt Fly — 3 × 12–15",
      "Lateral Raise — 3 × 12–15",
      "Overhead Tricep Extension — 2 × 12–15",
      "Hammer Curl — 2 × 12–15"
    ],


    "Lower B": [
      "Squat — 3 × 8–12",
      "Leg Press — 3 × 12–15",
      "Romanian Deadlift — 3 × 8–12",
      "Leg Curl — 3 × 12–15",
      "Leg Extension — 2 × 12–15",
      "Calf Raise — 3 × 15–20",
      "Cable Crunch — 3 × 12–15"
    ],


    /*
     * ============================================================
     * MAINTENANCE - 3 DAY FULL BODY
     * ============================================================
     */

    "Full Body A": [
      "Squat — 3 × 8–12",
      "Incline DB Press — 3 × 8–12",
      "Lat Pulldown — 3 × 8–12",
      "Seated Cable Row — 3 × 10–12",
      "Lateral Raise — 2 × 12–15",
      "Leg Curl — 2 × 10–15",
      "Cable Crunch — 3 × 12–15"
    ],


    "Full Body B": [
      "Hack Squat — 3 × 8–12",
      "Machine Chest Press — 3 × 8–12",
      "Close-Grip Lat Pulldown — 3 × 8–12",
      "Chest-Supported Row — 3 × 8–12",
      "Shoulder Press — 2 × 8–12",
      "Leg Extension — 2 × 12–15",
      "EZ-Bar Curl — 3 × 10–12",
      "V-Bar Pushdown — 3 × 10–12"
    ],


    "Full Body C": [
      "Leg Press — 3 × 10–15",
      "Incline Machine Press — 3 × 8–12",
      "T-Bar Row — 3 × 8–12",
      "Romanian Deadlift — 3 × 8–12",
      "Cable Lateral Raise — 2 × 12–15",
      "Hammer Curl — 3 × 10–12",
      "Overhead Tricep Extension — 3 × 10–12",
      "Calf Raise — 3 × 12–20",
      "Cable Crunch — 3 × 12–15"
    ],


    /*
     * ============================================================
     * REST DAYS
     * ============================================================
     */

    "Active Rest": [
      "Light Walking — 15 min",
      "Full Body Stretching — 10 min",
      "Mobility Exercises — 5 min"
    ],


    "Rest": [
      "Rest and recovery",
      "Light stretching if required",
      "Stay hydrated"
    ]

  };


  /*
   * ============================================================
   * CREATE 7 DAY PLAN
   * ============================================================
   */

  grid.innerHTML = days.map(day => {

    const item =
      (resp.plan || []).find(p => p.dayOfWeek === day);


    if (!item) {

      return `
        <div class="plan-day">

          <div class="day-label">
            ${day}
          </div>

          <div class="workout-type">
            -
          </div>

        </div>
      `;

    }


    /*
     * Intensity indicator
     * Example:
     * 3/5 → ●●●○○
     */

    const safeIntensity =
      Math.max(1, Math.min(5, item.intensityLevel));

    const dots =
      "●".repeat(safeIntensity) +
      "○".repeat(5 - safeIntensity);


    /*
     * Find exercises for this workout.
     */

    const workoutExercises =
      exercises[item.workoutType] || [
        "Warm-up — 5 min",
        "Main workout — 20 min",
        "Cool-down — 5 min"
      ];


    return `

      <div class="plan-day">

        <div class="day-label">
          ${day}
        </div>


        <div class="workout-type">
          ${item.workoutType}
        </div>


        <div class="duration">
          ${item.durationMinutes} min
        </div>


        <div class="intensity">
          ${dots}
        </div>


        <div class="exercise-list">

          ${workoutExercises.map(exercise =>

            `<div class="exercise-item">
              ${exercise}
            </div>`

          ).join("")}

        </div>

      </div>

    `;

  }).join("");

}

// ---------------- Profile ----------------
async function loadProfile() {
  const user = await Api.get("/api/profile");
  document.getElementById("pfName").value = user.name;
  document.getElementById("pfAge").value = user.age;
  document.getElementById("pfGender").value = user.gender;
  document.getElementById("pfHeight").value = user.heightCm;
  document.getElementById("pfWeight").value = user.weightKg;
  document.getElementById("pfGoal").value = user.goal;
  document.getElementById("pfActivity").value = user.activityLevel;

  await refreshProfileNumbers();

  document.getElementById("profileSaveBtn").onclick = async () => {
    const payload = {
      name: document.getElementById("pfName").value.trim(),
      age: parseInt(document.getElementById("pfAge").value, 10),
      gender: document.getElementById("pfGender").value,
      heightCm: parseFloat(document.getElementById("pfHeight").value),
      weightKg: parseFloat(document.getElementById("pfWeight").value),
      goal: document.getElementById("pfGoal").value,
      activityLevel: document.getElementById("pfActivity").value,
    };
    try {
      currentUser = await Api.put("/api/profile", payload);
      alert("Profile updated!");
      const hour = new Date().getHours();
      const part = hour < 12 ? "Good morning" : (hour < 18 ? "Good afternoon" : "Good evening");
      document.getElementById("greeting").textContent = `${part}, ${currentUser.name.split(" ")[0]}!`;
      await refreshProfileNumbers();
    } catch (e) {
      alert(e.message);
    }
  };
}

async function refreshProfileNumbers() {
  const nums = await Api.get("/api/profile/numbers");
  document.getElementById("numBmi").textContent = `${nums.bmi.toFixed(1)} (${nums.bmiCategory})`;
  document.getElementById("numBmr").textContent = `${Math.round(nums.bmr)} kcal/day`;
  document.getElementById("numTdee").textContent = `${Math.round(nums.tdee)} kcal/day`;
  document.getElementById("numCalorieTarget").textContent = `${Math.round(nums.calorieTarget)} kcal/day`;
  document.getElementById("numMacros").textContent = `${nums.proteinG}g / ${nums.carbG}g / ${nums.fatG}g`;
}
