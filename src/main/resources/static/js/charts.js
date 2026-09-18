const Charts = {

  _setupCanvas(canvas) {
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    const ctx = canvas.getContext("2d");
    ctx.scale(dpr, dpr);
    return { ctx, w: rect.width, h: rect.height };
  },

  drawBar(canvas, title, labels, values, color) {
    const { ctx, w, h } = Charts._setupCanvas(canvas);
    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = "#21252d";
    ctx.font = "bold 14px Segoe UI";
    ctx.fillText(title, 10, 20);

    const padTop = 40, padBottom = 26, padLeft = 10, padRight = 10;
    const chartW = w - padLeft - padRight;
    const chartH = h - padTop - padBottom;
    const max = Math.max(1, ...values) * 1.15;
    const n = values.length;
    const gap = 10;
    const barW = Math.max(6, (chartW - gap * (n - 1)) / n);

    ctx.strokeStyle = "#e4e7ed";
    ctx.beginPath();
    ctx.moveTo(padLeft, padTop + chartH);
    ctx.lineTo(padLeft + chartW, padTop + chartH);
    ctx.stroke();

    ctx.font = "10px Segoe UI";
    for (let i = 0; i < n; i++) {
      const barH = chartH * (values[i] / max);
      const x = padLeft + i * (barW + gap);
      const y = padTop + chartH - barH;
      ctx.fillStyle = color;
      Charts._roundRect(ctx, x, y, barW, barH, 4);
      ctx.fill();

      ctx.fillStyle = "#78808c";
      const label = labels[i];
      const lw = ctx.measureText(label).width;
      ctx.fillText(label, x + (barW - lw) / 2, padTop + chartH + 16);
    }
  },

  drawLine(canvas, title, labels, values, color) {
    const { ctx, w, h } = Charts._setupCanvas(canvas);
    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = "#21252d";
    ctx.font = "bold 14px Segoe UI";
    ctx.fillText(title, 10, 20);

    const padTop = 40, padBottom = 26, padLeft = 10, padRight = 10;
    const chartW = w - padLeft - padRight;
    const chartH = h - padTop - padBottom;
    const max = Math.max(1, ...values) * 1.15;
    const n = values.length;
    if (n < 2) return;

    ctx.strokeStyle = "#e4e7ed";
    ctx.beginPath();
    ctx.moveTo(padLeft, padTop + chartH);
    ctx.lineTo(padLeft + chartW, padTop + chartH);
    ctx.stroke();

    const xs = [], ys = [];
    for (let i = 0; i < n; i++) {
      xs.push(padLeft + chartW * (i / (n - 1)));
      ys.push(padTop + chartH - chartH * (values[i] / max));
    }

    ctx.strokeStyle = color;
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    ctx.moveTo(xs[0], ys[0]);
    for (let i = 1; i < n; i++) ctx.lineTo(xs[i], ys[i]);
    ctx.stroke();

    for (let i = 0; i < n; i++) {
      ctx.fillStyle = color;
      ctx.beginPath(); ctx.arc(xs[i], ys[i], 4, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = "#fff";
      ctx.beginPath(); ctx.arc(xs[i], ys[i], 2, 0, Math.PI * 2); ctx.fill();
    }

    ctx.fillStyle = "#78808c";
    ctx.font = "10px Segoe UI";
    for (let i = 0; i < n; i++) {
      const lw = ctx.measureText(labels[i]).width;
      ctx.fillText(labels[i], xs[i] - lw / 2, padTop + chartH + 16);
    }
  },

  drawDonut(canvas, title, labels, values, colors) {
    const { ctx, w, h } = Charts._setupCanvas(canvas);
    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = "#21252d";
    ctx.font = "bold 14px Segoe UI";
    ctx.fillText(title, 10, 20);

    const total = values.reduce((a, b) => a + b, 0);
    const cx = 70, cy = 90, rOuter = 55, rInner = 28;

    if (total <= 0) {
      ctx.fillStyle = "#78808c";
      ctx.font = "13px Segoe UI";
      ctx.fillText("No activity logged yet", 10, 90);
      return;
    }

    let startAngle = -Math.PI / 2;
    for (let i = 0; i < values.length; i++) {
      const angle = (values[i] / total) * Math.PI * 2;
      ctx.fillStyle = colors[i % colors.length];
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, rOuter, startAngle, startAngle + angle);
      ctx.closePath();
      ctx.fill();
      startAngle += angle;
    }
    ctx.fillStyle = "#ffffff";
    ctx.beginPath(); ctx.arc(cx, cy, rInner, 0, Math.PI * 2); ctx.fill();

    let legendY = 45;
    ctx.font = "11px Segoe UI";
    for (let i = 0; i < labels.length; i++) {
      ctx.fillStyle = colors[i % colors.length];
      ctx.fillRect(150, legendY, 10, 10);
      ctx.fillStyle = "#21252d";
      ctx.fillText(labels[i], 166, legendY + 9);
      legendY += 20;
    }
  },

  drawGauge(canvas, title, value, min, max, subLabel) {
    const { ctx, w, h } = Charts._setupCanvas(canvas);
    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = "#21252d";
    ctx.font = "bold 14px Segoe UI";
    ctx.fillText(title, 10, 20);

    const cx = w / 2, cy = 100, r = 70;
    const zoneColors = ["#4089ff", "#00bf8f", "#ff914d", "#ff5a5a"];
    const zoneWidths = [0.155, 0.26, 0.19, 0.395];

    let angle = Math.PI; // start at 180deg (left)
    ctx.lineWidth = 14;
    ctx.lineCap = "butt";
    for (let i = 0; i < zoneColors.length; i++) {
      const sweep = Math.PI * zoneWidths[i];
      ctx.strokeStyle = zoneColors[i];
      ctx.beginPath();
      ctx.arc(cx, cy, r, angle, angle - sweep, true);
      ctx.stroke();
      angle -= sweep;
    }

    const clamped = Math.max(min, Math.min(max, value));
    const ratio = (clamped - min) / (max - min);
    const needleAngle = Math.PI - ratio * Math.PI;
    const nx = cx + (r - 10) * Math.cos(needleAngle);
    const ny = cy - (r - 10) * Math.sin(needleAngle);

    ctx.strokeStyle = "#21252d";
    ctx.lineWidth = 3;
    ctx.beginPath(); ctx.moveTo(cx, cy); ctx.lineTo(nx, ny); ctx.stroke();
    ctx.fillStyle = "#21252d";
    ctx.beginPath(); ctx.arc(cx, cy, 5, 0, Math.PI * 2); ctx.fill();

    ctx.font = "bold 24px Segoe UI";
    ctx.textAlign = "center";
    ctx.fillText(value.toFixed(1), cx, cy + 40);
    ctx.font = "12px Segoe UI";
    ctx.fillStyle = "#78808c";
    ctx.fillText(subLabel, cx, cy + 58);
    ctx.textAlign = "left";
  },

  _roundRect(ctx, x, y, w, h, r) {
    if (h < 0) { y += h; h = -h; }
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  },
};
