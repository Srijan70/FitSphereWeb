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
  badge.style.background = SEGMENT_COLORS[resp.segment] || "#4089ff";
  document.getElementById("recoExplanation").textContent = resp.explanation;

  const grid = document.getElementById("planGrid");
  const days = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];
  grid.innerHTML = days.map(day => {
    const item = (resp.plan || []).find(p => p.dayOfWeek === day);
    if (!item) {
      return `<div class="plan-day"><div class="day-label">${day}</div><div class="workout-type">-</div></div>`;
    }
    const dots = "\u25CF".repeat(item.intensityLevel) + "\u25CB".repeat(5 - item.intensityLevel);
    return `
      <div class="plan-day">
        <div class="day-label">${day}</div>
        <div class="workout-type">${item.workoutType}</div>
        <div class="duration">${item.durationMinutes} min</div>
        <div class="intensity">${dots}</div>
      </div>`;
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
