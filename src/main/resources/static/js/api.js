/**
 * Cyber Incident Reporting System (CIRS) - Core API Client & Utilities
 */
const CIRS = (() => {
  const API_BASE = '/api/v1';
  const TOKEN_KEY = 'cirs_jwt_token';
  const USER_KEY = 'cirs_user_data';

  // --- Auth Utilities ---
  const auth = {
    setSession(token, user) {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    },
    clearSession() {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    },
    getToken() {
      return localStorage.getItem(TOKEN_KEY);
    },
    getUser() {
      const u = localStorage.getItem(USER_KEY);
      return u ? JSON.parse(u) : null;
    },
    isAuthenticated() {
      return !!this.getToken();
    },
    getRole() {
      const u = this.getUser();
      return u ? u.role : null;
    },
    logout() {
      this.clearSession();
      window.location.href = '/login.html';
    },
    checkRoleAndRedirect(allowedRoles = []) {
      if (!this.isAuthenticated()) {
        window.location.href = '/login.html?redirect=' + encodeURIComponent(window.location.pathname);
        return false;
      }
      const role = this.getRole();
      if (allowedRoles.length > 0 && !allowedRoles.includes(role)) {
        alert('Access Denied: You do not have permissions for this page.');
        // Redirect to appropriate home
        if (role === 'ROLE_ADMIN') window.location.href = '/admin/dashboard.html';
        else if (role === 'ROLE_INVESTIGATOR') window.location.href = '/investigator/dashboard.html';
        else window.location.href = '/user/dashboard.html';
        return false;
      }
      return true;
    }
  };

  // --- HTTP Request Engine ---
  async function request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;
    const headers = options.headers || {};

    const token = auth.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    options.headers = headers;

    try {
      const res = await fetch(url, options);

      if (res.status === 401) {
        auth.clearSession();
        if (!window.location.pathname.endsWith('login.html')) {
          window.location.href = '/login.html?session_expired=true';
        }
        throw new Error('Authentication expired. Please log in again.');
      }

      // Check if response is JSON or binary
      const contentType = res.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        const json = await res.json();
        if (!res.ok) {
          throw new Error(json.message || `Request failed with status ${res.status}`);
        }
        return json;
      } else {
        if (!res.ok) {
          throw new Error(`Request failed with status ${res.status}`);
        }
        return res; // raw response for file downloads
      }
    } catch (err) {
      console.error(`API Error [${endpoint}]:`, err);
      throw err;
    }
  }

  // --- UI Helpers ---
  function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.style.position = 'fixed';
      container.style.bottom = '20px';
      container.style.right = '20px';
      container.style.zIndex = '9999';
      container.style.display = 'flex';
      container.style.flexDirection = 'column';
      container.style.gap = '10px';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `alert alert-${type}`;
    toast.style.boxShadow = '0 10px 25px rgba(0,0,0,0.5)';
    toast.style.margin = '0';
    toast.style.minWidth = '280px';
    toast.style.maxWidth = '400px';
    toast.innerText = message;

    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  function formatDate(isoStr) {
    if (!isoStr) return 'N/A';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoStr;
    }
  }

  function getStatusBadge(status) {
    const formatted = (status || 'SUBMITTED').replace(/_/g, ' ');
    return `<span class="badge badge-${status}">${formatted}</span>`;
  }

  function getSeverityBadge(severity) {
    return `<span class="badge badge-${severity || 'MEDIUM'}">${severity || 'MEDIUM'}</span>`;
  }

  function renderNavbar(activeTab = '') {
    const user = auth.getUser();
    const nav = document.getElementById('main-navbar');
    if (!nav) return;

    if (!user) {
      nav.innerHTML = `
        <div class="nav-brand">
          <span class="brand-shield">🛡️</span>
          <span>CIRS <small style="font-size:0.75rem; color:var(--cyber-cyan);">Cyber Incident Portal</small></span>
        </div>
        <ul class="nav-links">
          <li><a href="/index.html" class="nav-item ${activeTab==='home'?'active':''}">Home</a></li>
          <li><a href="/login.html" class="btn btn-secondary btn-sm">Sign In</a></li>
          <li><a href="/register.html" class="btn btn-primary btn-sm">Register Incident</a></li>
        </ul>
      `;
      return;
    }

    let links = '';
    if (user.role === 'ROLE_USER') {
      links = `
        <li><a href="/user/dashboard.html" class="nav-item ${activeTab==='dashboard'?'active':''}">My Reports</a></li>
        <li><a href="/user/report-incident.html" class="btn btn-primary btn-sm">+ Report Incident</a></li>
      `;
    } else if (user.role === 'ROLE_INVESTIGATOR') {
      links = `
        <li><a href="/investigator/dashboard.html" class="nav-item ${activeTab==='dashboard'?'active':''}">Assigned Incidents</a></li>
      `;
    } else if (user.role === 'ROLE_ADMIN') {
      links = `
        <li><a href="/admin/dashboard.html" class="nav-item ${activeTab==='dashboard'?'active':''}">Dashboard</a></li>
        <li><a href="/admin/incidents.html" class="nav-item ${activeTab==='incidents'?'active':''}">All Incidents</a></li>
        <li><a href="/admin/users.html" class="nav-item ${activeTab==='users'?'active':''}">User Management</a></li>
        <li><a href="/admin/categories.html" class="nav-item ${activeTab==='categories'?'active':''}">Categories</a></li>
        <li><a href="/admin/audit-logs.html" class="nav-item ${activeTab==='audit'?'active':''}">Audit Logs</a></li>
      `;
    }

    nav.innerHTML = `
      <div class="nav-brand">
        <span class="brand-shield">🛡️</span>
        <a href="/" style="color:inherit;">CIRS <small style="font-size:0.75rem; color:var(--cyber-cyan);">Security Portal</small></a>
      </div>
      <ul class="nav-links">
        ${links}
        <li class="user-badge">
          <span>${user.fullName || user.username}</span>
          <span class="role-tag ${user.role}">${user.role.replace('ROLE_', '')}</span>
          <button onclick="CIRS.auth.logout()" class="btn btn-danger btn-sm" style="padding:0.2rem 0.5rem; margin-left:0.4rem;">Logout</button>
        </li>
      </ul>
    `;
  }

  return {
    auth,
    api: {
      get: (ep) => request(ep, { method: 'GET' }),
      post: (ep, data) => request(ep, { method: 'POST', body: JSON.stringify(data) }),
      postMultipart: (ep, formData) => request(ep, { method: 'POST', body: formData }),
      patch: (ep, data) => request(ep, { method: 'PATCH', body: JSON.stringify(data) }),
      put: (ep, data) => request(ep, { method: 'PUT', body: JSON.stringify(data) }),
      delete: (ep) => request(ep, { method: 'DELETE' }),
      download: (ep) => request(ep, { method: 'GET' })
    },
    ui: {
      showToast,
      formatDate,
      getStatusBadge,
      getSeverityBadge,
      renderNavbar
    }
  };
})();
