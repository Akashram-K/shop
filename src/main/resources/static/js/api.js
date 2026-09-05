/**
 * RoyaL Jwellery - Centralized API Service & State Management
 */

const API = {
  currentUser: null,
  cartData: null,

  async request(endpoint, options = {}) {
    const defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    const config = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...options.headers
      }
    };

    try {
      const response = await fetch(endpoint, config);
      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || `HTTP ${response.status}: ${response.statusText}`);
      }

      return data;
    } catch (error) {
      console.error(`API Error on [${config.method || 'GET'} ${endpoint}]:`, error);
      throw error;
    }
  },

  // Auth APIs
  async login(email, password) {
    const res = await this.request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    this.currentUser = res.data;
    return res;
  },

  async register(registerData) {
    const res = await this.request('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(registerData)
    });
    this.currentUser = res.data;
    return res;
  },

  async logout() {
    try {
      await this.request('/api/auth/logout', { method: 'POST' });
    } finally {
      this.currentUser = null;
      this.cartData = null;
    }
  },

  async checkAuth() {
    try {
      const res = await this.request('/api/auth/me');
      if (res.data) {
        this.currentUser = res.data;
        return this.currentUser;
      }
    } catch (e) {
      this.currentUser = null;
    }
    return null;
  },

  // Customer Profile & Address APIs
  async getProfile() {
    return this.request('/api/user/profile');
  },

  async updateProfile(fullName, phone) {
    return this.request('/api/user/profile', {
      method: 'PUT',
      body: JSON.stringify({ fullName, phone })
    });
  },

  async getAddresses() {
    return this.request('/api/user/addresses');
  },

  async addAddress(addressData) {
    return this.request('/api/user/addresses', {
      method: 'POST',
      body: JSON.stringify(addressData)
    });
  },

  // Products & Categories APIs
  async getProducts(search = '', categoryId = null, featured = null) {
    let url = '/api/products?';
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (categoryId) params.append('categoryId', categoryId);
    if (featured !== null) params.append('featured', featured);
    return this.request(url + params.toString());
  },

  async getProduct(id) {
    return this.request(`/api/products/${id}`);
  },

  async getCategories() {
    return this.request('/api/categories');
  },

  // Shopping Cart APIs
  async getCart() {
    const res = await this.request('/api/cart');
    this.cartData = res.data;
    return res;
  },

  async addToCart(productId, quantity = 1) {
    const res = await this.request('/api/cart/items', {
      method: 'POST',
      body: JSON.stringify({ productId, quantity })
    });
    this.cartData = res.data;
    return res;
  },

  async updateCartItem(itemId, quantity) {
    const res = await this.request(`/api/cart/items/${itemId}`, {
      method: 'PUT',
      body: JSON.stringify({ quantity })
    });
    this.cartData = res.data;
    return res;
  },

  async removeFromCart(itemId) {
    const res = await this.request(`/api/cart/items/${itemId}`, {
      method: 'DELETE'
    });
    this.cartData = res.data;
    return res;
  },

  async clearCart() {
    const res = await this.request('/api/cart/clear', {
      method: 'DELETE'
    });
    this.cartData = { items: [], totalItems: 0, totalAmount: 0 };
    return res;
  },

  // Orders APIs
  async placeOrder(orderData) {
    const res = await this.request('/api/orders', {
      method: 'POST',
      body: JSON.stringify(orderData)
    });
    this.cartData = { items: [], totalItems: 0, totalAmount: 0 };
    return res;
  },

  async getMyOrders() {
    return this.request('/api/orders');
  },

  async getOrder(id) {
    return this.request(`/api/orders/${id}`);
  },

  async cancelOrder(id) {
    return this.request(`/api/orders/${id}/cancel`, {
      method: 'POST'
    });
  },

  // Admin APIs
  async getAdminStats() {
    return this.request('/api/admin/stats');
  },

  async getAdminProducts() {
    return this.request('/api/admin/products');
  },

  async uploadProductImage(file) {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch('/api/admin/products/upload-image', {
      method: 'POST',
      body: formData,
      credentials: 'same-origin'
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Image upload failed');
    }
    return data;
  },

  async createProduct(productData) {
    return this.request('/api/admin/products', {
      method: 'POST',
      body: JSON.stringify(productData)
    });
  },

  async updateProduct(id, productData) {
    return this.request(`/api/admin/products/${id}`, {
      method: 'PUT',
      body: JSON.stringify(productData)
    });
  },

  async toggleProductStatus(id) {
    return this.request(`/api/admin/products/${id}/status`, {
      method: 'PATCH'
    });
  },

  async deleteProduct(id) {
    return this.request(`/api/admin/products/${id}`, {
      method: 'DELETE'
    });
  },

  async getAdminOrders(search = '', status = '') {
    let url = '/api/admin/orders?';
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (status) params.append('status', status);
    return this.request(url + params.toString());
  },

  async updateOrderStatus(id, status) {
    return this.request(`/api/admin/orders/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    });
  },

  async getAdminCustomers() {
    return this.request('/api/admin/customers');
  },

  async getNotifications() {
    return this.request('/api/admin/notifications');
  },

  async markNotificationRead(id) {
    return this.request(`/api/admin/notifications/${id}/read`, {
      method: 'PATCH'
    });
  },

  async markAllNotificationsRead() {
    return this.request('/api/admin/notifications/read-all', {
      method: 'POST'
    });
  }
};

// Toast Notifications Helper
function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;

  let icon = 'fa-info-circle';
  if (type === 'success') icon = 'fa-check-circle';
  if (type === 'error') icon = 'fa-exclamation-triangle';
  if (type === 'gold') icon = 'fa-crown';

  toast.innerHTML = `
    <i class="fas ${icon}" style="font-size: 1.2rem; color: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#d4af37'};"></i>
    <div style="flex:1;">${message}</div>
    <button style="background:transparent; border:none; color:#64748b; cursor:pointer;" onclick="this.parentElement.remove()">
      <i class="fas fa-times"></i>
    </button>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4500);
}

// Currency Formatter
function formatINR(amount) {
  if (amount === undefined || amount === null) return '₹0';
  return '₹' + Number(amount).toLocaleString('en-IN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 0
  });
}
