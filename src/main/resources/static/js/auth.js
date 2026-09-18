const loginCard = document.getElementById("loginCard");
const registerCard = document.getElementById("registerCard");

document.getElementById("showRegister").addEventListener("click", () => {
  loginCard.classList.add("hidden");
  registerCard.classList.remove("hidden");
});
document.getElementById("showLogin").addEventListener("click", () => {
  registerCard.classList.add("hidden");
  loginCard.classList.remove("hidden");
});

function showError(elId, message) {
  const el = document.getElementById(elId);
  el.textContent = message;
  el.classList.remove("hidden");
}
function hideError(elId) {
  document.getElementById(elId).classList.add("hidden");
}

document.getElementById("loginBtn").addEventListener("click", async () => {
  hideError("loginError");
  const email = document.getElementById("loginEmail").value.trim();
  const password = document.getElementById("loginPassword").value;

  if (!email || !password) {
    showError("loginError", "Please enter both email and password.");
    return;
  }
  try {
    await Api.post("/api/auth/login", { email, password });
    window.location.href = "app.html";
  } catch (e) {
    showError("loginError", e.message);
  }
});

document.getElementById("registerBtn").addEventListener("click", async () => {
  hideError("registerError");

  const payload = {
    name: document.getElementById("regName").value.trim(),
    email: document.getElementById("regEmail").value.trim(),
    password: document.getElementById("regPassword").value,
    age: parseInt(document.getElementById("regAge").value, 10),
    gender: document.getElementById("regGender").value,
    heightCm: parseFloat(document.getElementById("regHeight").value),
    weightKg: parseFloat(document.getElementById("regWeight").value),
    goal: document.getElementById("regGoal").value,
    activityLevel: document.getElementById("regActivity").value,
  };

  if (!payload.name || !payload.email || !payload.password || !payload.age
      || !payload.heightCm || !payload.weightKg) {
    showError("registerError", "Please fill in all fields.");
    return;
  }

  try {
    await Api.post("/api/auth/register", payload);
    // auto-login right after registering
    await Api.post("/api/auth/login", { email: payload.email, password: payload.password });
    window.location.href = "app.html";
  } catch (e) {
    showError("registerError", e.message);
  }
});

// If already logged in, skip straight to the app.
(async () => {
  try {
    await Api.get("/api/auth/me");
    window.location.href = "app.html";
  } catch (e) { /* not logged in - stay here */ }
})();
