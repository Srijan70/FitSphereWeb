const loginCard = document.getElementById("loginCard");
const registerCard = document.getElementById("registerCard");
const forgotCard = document.getElementById("forgotCard");

document.getElementById("showRegister").addEventListener("click", () => {
  loginCard.classList.add("hidden");
  registerCard.classList.remove("hidden");
  forgotCard.classList.add("hidden");
});

document.getElementById("showLogin").addEventListener("click", () => {
  registerCard.classList.add("hidden");
  forgotCard.classList.add("hidden");
  loginCard.classList.remove("hidden");
});

// ===================== FORGOT PASSWORD =====================

document.getElementById("showForgot").addEventListener("click", () => {
  loginCard.classList.add("hidden");
  registerCard.classList.add("hidden");
  forgotCard.classList.remove("hidden");

  hideError("forgotError");
});

document.getElementById("backToLogin").addEventListener("click", () => {
  forgotCard.classList.add("hidden");
  registerCard.classList.add("hidden");
  loginCard.classList.remove("hidden");

  hideError("forgotError");
});

function showError(elId, message) {
  const el = document.getElementById(elId);
  el.textContent = message;
  el.classList.remove("hidden");
}

function hideError(elId) {
  document.getElementById(elId).classList.add("hidden");
}

// ===================== LOGIN =====================

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

// ===================== REGISTER =====================

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

    await Api.post("/api/auth/login", {
      email: payload.email,
      password: payload.password
    });

    window.location.href = "app.html";
  } catch (e) {
    showError("registerError", e.message);
  }
});

// ===================== RESET PASSWORD =====================

document.getElementById("forgotBtn").addEventListener("click", async () => {
  hideError("forgotError");

  const email = document.getElementById("forgotEmail").value.trim();
  const newPassword = document.getElementById("forgotPassword").value;

  if (!email || !newPassword) {
    showError("forgotError", "Please enter your email and new password.");
    return;
  }

  try {
    await Api.post("/api/auth/forgot-password", {
      email: email,
      newPassword: newPassword
    });

    alert("Password reset successfully. You can now log in.");

    document.getElementById("forgotEmail").value = "";
    document.getElementById("forgotPassword").value = "";

    forgotCard.classList.add("hidden");
    loginCard.classList.remove("hidden");

  } catch (e) {
    showError("forgotError", e.message);
  }
});

// ===================== CHECK LOGIN =====================

(async () => {
  try {
    await Api.get("/api/auth/me");
    window.location.href = "app.html";
  } catch (e) {
    // Not logged in - stay on login page
  }
})();