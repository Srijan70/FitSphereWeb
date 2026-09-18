// Small fetch wrapper - all calls include cookies so the Spring session works.
const Api = {
  async get(url) {
    const res = await fetch(url, { credentials: "same-origin" });
    return Api._handle(res);
  },
  async post(url, body) {
    const res = await fetch(url, {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: body ? JSON.stringify(body) : undefined,
    });
    return Api._handle(res);
  },
  async put(url, body) {
    const res = await fetch(url, {
      method: "PUT",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    return Api._handle(res);
  },
  async _handle(res) {
    let data = null;
    try { data = await res.json(); } catch (e) { /* no body */ }
    if (!res.ok) {
      const message = (data && data.error) ? data.error : `Request failed (${res.status})`;
      const err = new Error(message);
      err.status = res.status;
      throw err;
    }
    return data;
  },
};
