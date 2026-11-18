package com.kidstv.app.server

object TvWebPortalHtml {

    fun buildHtml(): String = """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<title>KidTube • Painel dos Pais</title>
<style>
  :root {
    --bg: #0b0f17;
    --surface: #131a26;
    --surface-2: #1b2434;
    --border: #283548;
    --text: #f1f5f9;
    --muted: #94a3b8;
    --accent: #38bdf8;
    --accent-2: #2563eb;
    --danger: #ef4444;
    --ok: #22c55e;
    --warn: #f59e0b;
    --radius: 14px;
  }
  * { box-sizing: border-box; font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; }
  body { margin: 0; background: var(--bg); color: var(--text); padding: 16px; }
  .wrap { max-width: 680px; margin: 0 auto; }
  .header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
  .brand { font-size: 20px; font-weight: 700; display: flex; align-items: center; gap: 10px; }
  .badge { font-size: 12px; background: rgba(56,189,248,.16); color: var(--accent); padding: 4px 10px; border-radius: 999px; border: 1px solid rgba(56,189,248,.35); }
  .card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); padding: 16px; margin-bottom: 14px; }
  .card h2 { margin: 0 0 10px 0; font-size: 16px; }
  .muted { color: var(--muted); font-size: 13px; line-height: 1.4; }
  label { display: block; font-size: 13px; color: var(--muted); margin: 10px 0 6px; }
  input, select { width: 100%; padding: 12px; border-radius: 10px; border: 1px solid var(--border); background: var(--surface-2); color: var(--text); font-size: 15px; }
  button { border: 0; border-radius: 10px; padding: 12px 16px; font-size: 14px; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; gap: 8px; }
  .btn-primary { background: linear-gradient(135deg, var(--accent-2), #0284c7); color: #fff; width: 100%; margin-top: 12px; }
  .btn-secondary { background: var(--surface-2); color: var(--text); border: 1px solid var(--border); }
  .btn-danger { background: rgba(239,68,68,.15); color: #fca5a5; border: 1px solid rgba(239,68,68,.35); padding: 8px 10px; font-size: 12px; }
  .btn-sm { padding: 8px 12px; font-size: 12px; }
  .bar-bg { width: 100%; height: 10px; border-radius: 999px; background: var(--surface-2); overflow: hidden; margin: 8px 0; border: 1px solid var(--border); }
  .bar-fill { height: 100%; background: linear-gradient(90deg, var(--ok), var(--accent)); transition: width .25s ease; }
  .row { display: flex; gap: 8px; align-items: center; }
  .row-between { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
  .item { display: flex; justify-content: space-between; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px solid var(--border); }
  .item:first-child { border-top: 0; }
  .item-title { font-weight: 600; font-size: 14px; }
  .item-sub { font-size: 12px; color: var(--muted); margin-top: 2px; }
  .msg { padding: 10px 12px; border-radius: 10px; margin-top: 10px; font-size: 13px; display: none; }
  .msg.ok { display: block; background: rgba(34,197,94,.14); border: 1px solid rgba(34,197,94,.35); color: #86efac; }
  .msg.err { display: block; background: rgba(239,68,68,.14); border: 1px solid rgba(239,68,68,.35); color: #fca5a5; }
  .hidden { display: none !important; }
  .tabs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 14px; }
  .tab { background: var(--surface); color: var(--muted); border: 1px solid var(--border); padding: 10px; border-radius: 10px; text-align: center; font-size: 13px; font-weight: 600; cursor: pointer; }
  .tab.active { background: rgba(56,189,248,.16); color: var(--accent); border-color: var(--accent); }
</style>
</head>
<body>
<div class="wrap">
  <div class="header">
    <div class="brand">
      <svg width="28" height="28" viewBox="0 0 108 108" aria-hidden="true">
        <line x1="46" y1="36" x2="39" y2="26" stroke="#38BDF8" stroke-width="5" stroke-linecap="round"/>
        <line x1="62" y1="36" x2="69" y2="26" stroke="#38BDF8" stroke-width="5" stroke-linecap="round"/>
        <rect x="27" y="35" width="54" height="39" rx="10" fill="#E2E8F0"/>
        <rect x="31" y="39" width="46" height="31" rx="7" fill="#0B1120"/>
        <rect x="40" y="44" width="28" height="21" rx="6" fill="#F43F5E"/>
        <polygon points="50.5,49.2 60.5,54.5 50.5,59.8" fill="#FFFFFF"/>
        <rect x="43" y="77" width="22" height="4" rx="2" fill="#38BDF8"/>
      </svg>
      <span>Kid<span style="background:#F43F5E;color:#fff;padding:1px 7px;border-radius:6px;margin-left:2px;">Tube</span></span>
      <span class="badge">Painel dos Pais</span>
    </div>
    <button id="logoutBtn" class="btn-secondary btn-sm hidden" onclick="logout()">Bloquear</button>
  </div>

  <!-- AUTH / FIRST TIME SETUP -->
  <div id="authCard" class="card">
    <h2 id="authTitle">Carregando...</h2>
    <p id="authDesc" class="muted"></p>
    <div id="pinForm">
      <label for="pinInput">Senha / PIN dos pais (mín. 4 dígitos)</label>
      <input id="pinInput" type="password" inputmode="numeric" autocomplete="off" placeholder="Digite a senha">
      <div id="confirmGroup" class="hidden">
        <label for="pinConfirmInput">Confirmar senha</label>
        <input id="pinConfirmInput" type="password" inputmode="numeric" autocomplete="off" placeholder="Repita a senha">
      </div>
      <button class="btn-primary" onclick="submitAuth()">Continuar</button>
      <div id="authMsg" class="msg"></div>
    </div>
  </div>

  <!-- MAIN PANEL -->
  <div id="panel" class="hidden">
    <!-- STORAGE CARD -->
    <div class="card">
      <div class="row-between">
        <h2>Armazenamento da TV</h2>
        <span id="storagePct" class="badge">0%</span>
      </div>
      <div class="bar-bg"><div id="storageBar" class="bar-fill" style="width:0%"></div></div>
      <div id="storageInfo" class="muted"></div>
      <div class="row" style="margin-top:10px;">
        <input id="quotaInput" type="number" min="512" step="256" placeholder="Cota em MB (ex: 4096)">
        <button class="btn-secondary" style="white-space:nowrap;" onclick="saveQuota()">Salvar cota</button>
      </div>
    </div>

    <div class="tabs">
      <div class="tab active" onclick="showTab('yt', this)">YouTube</div>
      <div class="tab" onclick="showTab('upload', this)">Enviar Vídeo</div>
      <div class="tab" onclick="showTab('catalog', this)">Catálogo</div>
    </div>

    <!-- YOUTUBE TAB -->
    <div id="tab-yt" class="card">
      <h2>Adicionar vídeo do YouTube na TV</h2>
      <label for="ytUrl">URL ou ID do YouTube</label>
      <input id="ytUrl" type="url" placeholder="https://www.youtube.com/watch?v=...">
      <label for="ytTitle">Título (opcional - busca automático se vazio)</label>
      <input id="ytTitle" type="text" placeholder="Ex.: Desenho educativo">
      <label for="ytPlaylist">Adicionar na playlist (opcional)</label>
      <select id="ytPlaylist"><option value="">Nenhuma (só no Feed)</option></select>
      <button class="btn-primary" onclick="addYouTube()">Adicionar na TV</button>
      <div id="ytMsg" class="msg"></div>
    </div>

    <!-- UPLOAD TAB -->
    <div id="tab-upload" class="card">
      <h2>Enviar vídeo do celular para a TV</h2>
      <p class="muted">O vídeo é salvo dentro do espaço controlado da TV e funciona sem internet.</p>
      <label for="fileInput">Escolher vídeo do celular</label>
      <input id="fileInput" type="file" accept="video/*">
      <label for="upTitle">Título (opcional)</label>
      <input id="upTitle" type="text" placeholder="Nome exibido para a criança">
      <label for="upPlaylist">Adicionar na playlist (opcional)</label>
      <select id="upPlaylist"><option value="">Nenhuma (só no Feed)</option></select>
      <div id="upProgressWrap" class="hidden" style="margin-top:10px;">
        <div class="bar-bg"><div id="upBar" class="bar-fill" style="width:0%"></div></div>
        <div id="upPct" class="muted">0%</div>
      </div>
      <button class="btn-primary" onclick="uploadVideo()">Enviar para a TV</button>
      <div id="upMsg" class="msg"></div>
    </div>

    <!-- CATALOG TAB -->
    <div id="tab-catalog" class="card">
      <h2>Canais / Playlists na TV</h2>
      <div class="row">
        <input id="newPlNum" type="number" min="1" style="max-width:92px;" placeholder="CH #">
        <input id="newPlName" type="text" placeholder="Nome do canal (ex: Desenhos)">
      </div>
      <div class="row" style="margin-top:8px;">
        <select id="newPlIcon">
          <option value="tv">Ícone: TV</option>
          <option value="star">Ícone: Favoritos</option>
          <option value="rocket">Ícone: Aventura</option>
          <option value="music">Ícone: Música</option>
          <option value="pets">Ícone: Animais</option>
          <option value="book">Ícone: Histórias</option>
          <option value="palette">Ícone: Arte</option>
          <option value="science">Ícone: Ciência</option>
          <option value="sports">Ícone: Esportes</option>
          <option value="bedtime">Ícone: Dormir</option>
        </select>
        <button class="btn-secondary" style="white-space:nowrap;" onclick="createPlaylist()">Criar Canal</button>
      </div>
      <div id="playlistList" style="margin-top:12px;"></div>
      <h2 style="margin-top:18px;">Vídeos na TV</h2>
      <div id="videoList"></div>
    </div>
  </div>
</div>

<script>
const params = new URLSearchParams(location.search);
const pairToken = params.get('pair') || '';
let sessionToken = localStorage.getItem('kidstv_session') || '';
let needsSetup = false;
let storageSnapshot = null;

function setMsg(id, text, ok) {
  const el = document.getElementById(id);
  el.textContent = text;
  el.className = 'msg ' + (ok ? 'ok' : 'err');
}

function clearMsg(id) {
  const el = document.getElementById(id);
  el.className = 'msg';
  el.textContent = '';
}

function fmtBytes(b) {
  if (!b || b <= 0) return '0 MB';
  const mb = b / (1024 * 1024);
  if (mb >= 1024) return (mb / 1024).toFixed(2) + ' GB';
  return mb.toFixed(1) + ' MB';
}

async function api(path, opts = {}) {
  const headers = opts.headers || {};
  if (sessionToken) headers['X-Session-Token'] = sessionToken;
  if (pairToken) headers['X-Pair-Token'] = pairToken;
  const res = await fetch(path, { ...opts, headers });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || ('Erro HTTP ' + res.status));
  return data;
}

async function init() {
  try {
    const status = await api('/api/status');
    needsSetup = !status.hasPinConfigured;
    if (status.authenticated) {
      showPanel();
    } else {
      showAuth(needsSetup);
    }
  } catch (e) {
    document.getElementById('authTitle').textContent = 'Erro de conexão';
    document.getElementById('authDesc').textContent = e.message;
  }
}

function showAuth(setupMode) {
  document.getElementById('authCard').classList.remove('hidden');
  document.getElementById('panel').classList.add('hidden');
  document.getElementById('logoutBtn').classList.add('hidden');
  if (setupMode) {
    document.getElementById('authTitle').textContent = 'Primeira configuração';
    document.getElementById('authDesc').textContent = 'Crie a senha dos pais. Ela será usada para abrir este painel e destravar a TV.';
    document.getElementById('confirmGroup').classList.remove('hidden');
  } else {
    document.getElementById('authTitle').textContent = 'Área protegida dos pais';
    document.getElementById('authDesc').textContent = 'Digite a senha configurada para gerenciar os vídeos e o espaço da TV.';
    document.getElementById('confirmGroup').classList.add('hidden');
  }
}

async function submitAuth() {
  clearMsg('authMsg');
  const pin = document.getElementById('pinInput').value.trim();
  if (pin.length < 4) {
    setMsg('authMsg', 'Use pelo menos 4 caracteres/dígitos.', false);
    return;
  }
  try {
    if (needsSetup) {
      const confirm = document.getElementById('pinConfirmInput').value.trim();
      if (pin !== confirm) {
        setMsg('authMsg', 'As senhas não conferem.', false);
        return;
      }
      const res = await api('/api/auth/setup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin })
      });
      sessionToken = res.sessionToken;
    } else {
      const res = await api('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin })
      });
      sessionToken = res.sessionToken;
    }
    localStorage.setItem('kidstv_session', sessionToken);
    document.getElementById('pinInput').value = '';
    document.getElementById('pinConfirmInput').value = '';
    showPanel();
  } catch (e) {
    setMsg('authMsg', e.message, false);
  }
}

function logout() {
  sessionToken = '';
  localStorage.removeItem('kidstv_session');
  init();
}

function showPanel() {
  document.getElementById('authCard').classList.add('hidden');
  document.getElementById('panel').classList.remove('hidden');
  document.getElementById('logoutBtn').classList.remove('hidden');
  refreshCatalog();
}

function showTab(name, el) {
  ['yt', 'upload', 'catalog'].forEach(t => {
    document.getElementById('tab-' + t).classList.toggle('hidden', t !== name);
  });
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
}

async function refreshCatalog() {
  try {
    const data = await api('/api/catalog');
    storageSnapshot = data.storage;
    const s = data.storage;
    const pct = Math.min(100, Math.round((s.usedRatioOfQuota || 0) * 100));
    document.getElementById('storagePct').textContent = pct + '% da cota';
    document.getElementById('storageBar').style.width = pct + '%';
    document.getElementById('storageInfo').textContent =
      'Usado pelos vídeos: ' + fmtBytes(s.appVideosBytes) +
      ' / Cota: ' + fmtBytes(s.quotaBytes) +
      ' • Livre real na TV: ' + fmtBytes(s.freeDeviceBytes) +
      ' (disponível p/ novos vídeos: ' + fmtBytes(s.remainingAllowedBytes) + ')';
    document.getElementById('quotaInput').value = Math.round(s.quotaBytes / (1024 * 1024));

    const plOptions = '<option value="">Nenhuma (só no Feed)</option>' +
      data.playlists.map(p => '<option value="' + p.id + '">CH ' + String(p.channelNumber || 1).padStart(2, '0') + ' • ' + escapeHtml(p.name) + ' (' + p.videoCount + ')</option>').join('');
    document.getElementById('ytPlaylist').innerHTML = plOptions;
    document.getElementById('upPlaylist').innerHTML = plOptions;

    document.getElementById('playlistList').innerHTML = data.playlists.length === 0
      ? '<p class="muted">Nenhum canal/playlist criado ainda.</p>'
      : data.playlists.map(p =>
          '<div class="item"><div><div class="item-title">CH ' + String(p.channelNumber || 1).padStart(2, '0') + ' • ' + escapeHtml(p.name) +
          '</div><div class="item-sub">' + p.videoCount + ' vídeo(s) • ícone: ' + escapeHtml(p.iconKey || 'tv') + '</div></div>' +
          '<button class="btn-danger" onclick="deletePlaylist(' + p.id + ')">Excluir</button></div>'
        ).join('');

    document.getElementById('videoList').innerHTML = data.videos.length === 0
      ? '<p class="muted">Nenhum vídeo adicionado na TV.</p>'
      : data.videos.map(v =>
          '<div class="item"><div><div class="item-title">' + escapeHtml(v.title) +
          '</div><div class="item-sub">' + (v.sourceType === 'YOUTUBE' ? 'YouTube' : ('Arquivo local • ' + fmtBytes(v.fileSizeBytes))) +
          '</div></div><div class="row">' +
          '<button class="btn-secondary btn-sm" onclick="playOnTv(' + v.id + ')">Tocar na TV</button>' +
          '<button class="btn-danger" onclick="deleteVideo(' + v.id + ')">Apagar</button></div></div>'
        ).join('');
  } catch (e) {
    if (e.message.includes('401') || e.message.toLowerCase().includes('autoriz')) logout();
  }
}

async function addYouTube() {
  clearMsg('ytMsg');
  const url = document.getElementById('ytUrl').value.trim();
  const title = document.getElementById('ytTitle').value.trim();
  const playlistId = document.getElementById('ytPlaylist').value;
  if (!url) { setMsg('ytMsg', 'Cole a URL do YouTube.', false); return; }
  try {
    await api('/api/videos/youtube', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url, title, playlistId: playlistId ? Number(playlistId) : null })
    });
    document.getElementById('ytUrl').value = '';
    document.getElementById('ytTitle').value = '';
    setMsg('ytMsg', 'Vídeo do YouTube adicionado na TV!', true);
    refreshCatalog();
  } catch (e) {
    setMsg('ytMsg', e.message, false);
  }
}

function uploadVideo() {
  clearMsg('upMsg');
  const fileEl = document.getElementById('fileInput');
  const file = fileEl.files && fileEl.files[0];
  if (!file) { setMsg('upMsg', 'Selecione um arquivo de vídeo.', false); return; }
  if (storageSnapshot && file.size > storageSnapshot.remainingAllowedBytes) {
    setMsg('upMsg', 'Arquivo (' + fmtBytes(file.size) + ') excede o espaço disponível permitido na TV (' + fmtBytes(storageSnapshot.remainingAllowedBytes) + ').', false);
    return;
  }
  const title = document.getElementById('upTitle').value.trim();
  const playlistId = document.getElementById('upPlaylist').value;
  const form = new FormData();
  form.append('file', file);
  form.append('title', title);
  if (playlistId) form.append('playlistId', playlistId);

  const wrap = document.getElementById('upProgressWrap');
  const bar = document.getElementById('upBar');
  const pct = document.getElementById('upPct');
  wrap.classList.remove('hidden');
  bar.style.width = '0%';
  pct.textContent = '0%';

  const xhr = new XMLHttpRequest();
  xhr.open('POST', '/api/videos/upload');
  if (sessionToken) xhr.setRequestHeader('X-Session-Token', sessionToken);
  if (pairToken) xhr.setRequestHeader('X-Pair-Token', pairToken);
  xhr.upload.onprogress = (ev) => {
    if (ev.lengthComputable) {
      const p = Math.round((ev.loaded / ev.total) * 100);
      bar.style.width = p + '%';
      pct.textContent = p + '% (' + fmtBytes(ev.loaded) + ' / ' + fmtBytes(ev.total) + ')';
    }
  };
  xhr.onload = () => {
    const res = JSON.parse(xhr.responseText || '{}');
    if (xhr.status >= 200 && xhr.status < 300) {
      fileEl.value = '';
      document.getElementById('upTitle').value = '';
      setMsg('upMsg', 'Vídeo enviado e salvo na TV!', true);
      refreshCatalog();
    } else {
      setMsg('upMsg', res.error || 'Falha no envio.', false);
    }
  };
  xhr.onerror = () => setMsg('upMsg', 'Erro de rede ao enviar vídeo.', false);
  xhr.send(form);
}

async function createPlaylist() {
  const name = document.getElementById('newPlName').value.trim();
  const numVal = Number(document.getElementById('newPlNum').value);
  const iconKey = document.getElementById('newPlIcon').value;
  if (!name) return;
  const payload = { name, iconKey };
  if (numVal > 0) payload.channelNumber = numVal;
  await api('/api/playlists', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  document.getElementById('newPlName').value = '';
  document.getElementById('newPlNum').value = '';
  refreshCatalog();
}

async function deletePlaylist(id) {
  await api('/api/playlists?id=' + id, { method: 'DELETE' });
  refreshCatalog();
}

async function deleteVideo(id) {
  await api('/api/videos?id=' + id, { method: 'DELETE' });
  refreshCatalog();
}

async function playOnTv(id) {
  await api('/api/tv/play', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ videoId: id })
  });
}

async function saveQuota() {
  const quotaMb = Number(document.getElementById('quotaInput').value);
  if (!quotaMb) return;
  await api('/api/storage/quota', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quotaMb })
  });
  refreshCatalog();
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
}

init();
</script>
</body>
</html>
""".trimIndent()
}
