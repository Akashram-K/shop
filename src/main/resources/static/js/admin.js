/**
 * RoyaL Jwellery - Father / Store Owner Admin Portal Logic
 */

const AdminPortal = {
  activeTab: 'dashboard',
  stats: null,
  orders: [],
  products: [],
  customers: [],
  notifications: [],
  pollTimer: null,
  editingProductId: null,

  async init() {
    this.switchTab('dashboard');
    await this.loadAllData();
    this.startNotificationPolling();
  },

  async loadAllData() {
    await Promise.all([
      this.loadDashboardStats(),
      this.loadNotifications(),
      this.loadOrders(),
      this.loadProducts(),
      this.loadCustomers()
    ]);
  },

  startNotificationPolling() {
    if (this.pollTimer) clearInterval(this.pollTimer);
    this.pollTimer = setInterval(() => {
      if (document.getElementById('admin-view')?.style.display !== 'none') {
        this.loadNotifications(true);
      }
    }, 10000);
  },

  switchTab(tabName) {
    this.activeTab = tabName;

    document.querySelectorAll('.admin-nav-link').forEach(link => {
      link.classList.remove('active');
    });
    document.querySelectorAll('.admin-tab-pane').forEach(pane => {
      pane.style.display = 'none';
    });

    const activeLink = document.getElementById(`admin-nav-${tabName}`);
    if (activeLink) activeLink.classList.add('active');

    const activePane = document.getElementById(`admin-pane-${tabName}`);
    if (activePane) activePane.style.display = 'block';

    if (tabName === 'dashboard') this.loadDashboardStats();
    if (tabName === 'orders') this.loadOrders();
    if (tabName === 'products') this.loadProducts();
    if (tabName === 'customers') this.loadCustomers();
    if (tabName === 'notifications') this.loadNotifications();
  },

  // Dashboard Stats
  async loadDashboardStats() {
    try {
      const res = await API.getAdminStats();
      this.stats = res.data;
      this.renderDashboardStats();
    } catch (e) {
      console.error('Failed to load admin stats', e);
    }
  },

  renderDashboardStats() {
    if (!this.stats) return;

    document.getElementById('stat-total-sales').textContent = formatINR(this.stats.totalSales);
    document.getElementById('stat-total-orders').textContent = this.stats.totalOrders;
    document.getElementById('stat-pending-orders').textContent = this.stats.pendingOrders;
    document.getElementById('stat-delivered-orders').textContent = this.stats.deliveredOrders;
    document.getElementById('stat-total-products').textContent = this.stats.totalProducts;
    document.getElementById('stat-total-customers').textContent = this.stats.totalCustomers;

    // Recent orders table
    const recentTableBody = document.getElementById('admin-recent-orders-tbody');
    if (recentTableBody) {
      if (this.stats.recentOrders?.length > 0) {
        recentTableBody.innerHTML = this.stats.recentOrders.map(o => `
          <tr>
            <td><strong>#${o.orderNumber}</strong></td>
            <td>${o.customerName}</td>
            <td>${formatINR(o.totalAmount)}</td>
            <td><span class="status-badge ${o.status.toLowerCase()}">${o.status}</span></td>
            <td>${new Date(o.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</td>
            <td>
              <button class="btn-icon" onclick="AdminPortal.openOrderDetailsModal(${o.id})" title="View Details">
                <i class="fas fa-eye"></i>
              </button>
            </td>
          </tr>
        `).join('');
      } else {
        recentTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:#64748b;">No recent orders.</td></tr>';
      }
    }

    // Low stock warnings
    const lowStockContainer = document.getElementById('admin-low-stock-list');
    if (lowStockContainer) {
      if (this.stats.lowStockProducts?.length > 0) {
        lowStockContainer.innerHTML = this.stats.lowStockProducts.map(p => `
          <div style="display:flex; justify-content:space-between; align-items:center; padding: 0.8rem; background:rgba(245,158,11,0.08); border:1px solid rgba(245,158,11,0.2); border-radius:var(--radius-sm); margin-bottom:0.6rem;">
            <div style="display:flex; gap:0.8rem; align-items:center;">
              <img src="${p.imageUrl}" style="width:36px; height:36px; border-radius:4px; object-fit:cover;">
              <div>
                <div style="font-size:0.88rem; font-weight:600;">${p.name}</div>
                <div style="font-size:0.75rem; color:#94a3b8;">${p.categoryName || ''}</div>
              </div>
            </div>
            <span style="font-weight:700; color:#fbbf24; font-size:0.88rem;">${p.stockQuantity} Left</span>
          </div>
        `).join('');
      } else {
        lowStockContainer.innerHTML = '<p style="color:#10b981; font-size:0.88rem;"><i class="fas fa-check-circle"></i> All products are adequately stocked.</p>';
      }
    }
  },

  // Notifications
  async loadNotifications(isPolling = false) {
    try {
      const res = await API.getNotifications();
      const prevUnreadCount = this.notifications.filter(n => !n.isRead).length;
      this.notifications = res.data || [];
      const newUnreadCount = this.notifications.filter(n => !n.isRead).length;

      this.updateNotificationBadge(newUnreadCount);
      this.renderNotificationsList();

      if (isPolling && newUnreadCount > prevUnreadCount) {
        showToast('🔔 New Order Notification Received for Father!', 'gold');
        this.loadDashboardStats();
        this.loadOrders();
      }
    } catch (e) {
      console.error(e);
    }
  },

  updateNotificationBadge(count) {
    const badge = document.getElementById('admin-notif-badge');
    const navBadge = document.getElementById('admin-nav-notif-badge');

    if (badge) {
      badge.textContent = count;
      badge.style.display = count > 0 ? 'flex' : 'none';
    }
    if (navBadge) {
      navBadge.textContent = count;
      navBadge.style.display = count > 0 ? 'inline-block' : 'none';
    }
  },

  toggleNotificationDropdown() {
    const dd = document.getElementById('admin-notif-dropdown');
    if (dd) dd.classList.toggle('show');
  },

  renderNotificationsList() {
    const list = document.getElementById('admin-notif-items-list');
    const paneList = document.getElementById('admin-notifications-pane-list');

    const html = this.notifications.length === 0
      ? '<p style="padding:1.5rem; text-align:center; color:#64748b;">No notifications yet.</p>'
      : this.notifications.map(n => `
          <div class="notif-item ${!n.isRead ? 'unread' : ''}" onclick="AdminPortal.handleNotificationClick(${n.id}, ${n.orderId})">
            <div class="notif-title"><i class="fas fa-shopping-bag" style="color:#d4af37;"></i> ${n.title}</div>
            <div class="notif-msg">${n.message}</div>
            <div class="notif-time">${new Date(n.createdAt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })} • ${new Date(n.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</div>
          </div>
        `).join('');

    if (list) list.innerHTML = html;
    if (paneList) paneList.innerHTML = html;
  },

  async handleNotificationClick(notifId, orderId) {
    try {
      await API.markNotificationRead(notifId);
      this.loadNotifications();
      if (orderId) {
        this.openOrderDetailsModal(orderId);
      }
    } catch (e) {
      console.error(e);
    }
  },

  async markAllNotificationsRead() {
    try {
      await API.markAllNotificationsRead();
      this.loadNotifications();
      showToast('All notifications marked as read', 'info');
    } catch (e) {
      console.error(e);
    }
  },

  // Orders Management
  async loadOrders() {
    const search = document.getElementById('admin-orders-search')?.value || '';
    const status = document.getElementById('admin-orders-filter-status')?.value || '';

    const tbody = document.getElementById('admin-orders-tbody');
    if (tbody) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding:2rem; color:#94a3b8;"><i class="fas fa-spinner fa-spin"></i> Loading orders...</td></tr>';
    }

    try {
      const res = await API.getAdminOrders(search, status);
      this.orders = res.data || [];
      this.renderOrdersTable();
    } catch (e) {
      if (tbody) tbody.innerHTML = `<tr><td colspan="8" style="color:#ef4444; text-align:center;">Failed to load orders: ${e.message}</td></tr>`;
    }
  },

  renderOrdersTable() {
    const tbody = document.getElementById('admin-orders-tbody');
    if (!tbody) return;

    if (this.orders.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding:2.5rem; color:#64748b;">No orders match your filter criteria.</td></tr>';
      return;
    }

    tbody.innerHTML = this.orders.map(o => `
      <tr>
        <td><strong>#${o.orderNumber}</strong></td>
        <td>
          <div style="font-weight:600;">${o.recipientName}</div>
          <div style="font-size:0.78rem; color:#94a3b8;">${o.recipientPhone}</div>
        </td>
        <td>${o.items?.length || 0} items</td>
        <td style="font-family:'Cinzel',serif; font-weight:700; color:#f3e5ab;">${formatINR(o.totalAmount)}</td>
        <td><span class="status-badge ${o.status.toLowerCase()}">${o.status}</span></td>
        <td style="font-size:0.8rem; color:#94a3b8;">${new Date(o.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</td>
        <td>
          <select style="background:rgba(7,9,14,0.9); color:var(--text-main); border:1px solid var(--border-glass); padding:0.3rem 0.6rem; border-radius:4px; font-size:0.82rem;" onchange="AdminPortal.handleUpdateOrderStatus(${o.id}, this.value)">
            <option value="PLACED" ${o.status === 'PLACED' ? 'selected' : ''}>PLACED</option>
            <option value="CONFIRMED" ${o.status === 'CONFIRMED' ? 'selected' : ''}>CONFIRMED</option>
            <option value="PACKED" ${o.status === 'PACKED' ? 'selected' : ''}>PACKED</option>
            <option value="OUT_FOR_DELIVERY" ${o.status === 'OUT_FOR_DELIVERY' ? 'selected' : ''}>OUT_FOR_DELIVERY</option>
            <option value="DELIVERED" ${o.status === 'DELIVERED' ? 'selected' : ''}>DELIVERED</option>
            <option value="CANCELLED" ${o.status === 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
          </select>
        </td>
        <td>
          <button class="btn-icon" onclick="AdminPortal.openOrderDetailsModal(${o.id})" title="Inspect Full Order">
            <i class="fas fa-eye"></i>
          </button>
        </td>
      </tr>
    `).join('');
  },

  async handleUpdateOrderStatus(orderId, newStatus) {
    try {
      await API.updateOrderStatus(orderId, newStatus);
      showToast(`Order #${orderId} status updated to ${newStatus}`, 'success');
      this.loadDashboardStats();
      this.loadOrders();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  async openOrderDetailsModal(orderId) {
    try {
      const res = await API.getOrder(orderId);
      const o = res.data;

      document.getElementById('admin-modal-order-num').textContent = o.orderNumber;
      document.getElementById('admin-modal-cust-name').textContent = o.customerName;
      document.getElementById('admin-modal-cust-email').textContent = o.customerEmail;
      document.getElementById('admin-modal-cust-phone').textContent = o.customerPhone;
      document.getElementById('admin-modal-recipient-name').textContent = o.recipientName;
      document.getElementById('admin-modal-recipient-phone').textContent = o.recipientPhone;
      document.getElementById('admin-modal-address').textContent = o.deliveryAddress;
      document.getElementById('admin-modal-payment').textContent = o.paymentMethod;
      document.getElementById('admin-modal-notes').textContent = o.notes || 'None provided';
      document.getElementById('admin-modal-status-badge').innerHTML = `<span class="status-badge ${o.status.toLowerCase()}">${o.status}</span>`;
      document.getElementById('admin-modal-total').textContent = formatINR(o.totalAmount);

      document.getElementById('admin-modal-items-tbody').innerHTML = o.items.map(item => `
        <tr>
          <td>
            <div style="display:flex; align-items:center; gap:0.6rem;">
              <img src="${item.imageUrl}" style="width:36px; height:36px; border-radius:4px; object-fit:cover;">
              <div>${item.productName}</div>
            </div>
          </td>
          <td>${formatINR(item.productPrice)}</td>
          <td>${item.quantity}</td>
          <td style="font-weight:700; color:#f3e5ab;">${formatINR(item.subtotal)}</td>
        </tr>
      `).join('');

      App.openModal('admin-order-modal');
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  // Products Management
  async loadProducts() {
    const tbody = document.getElementById('admin-products-tbody');
    if (tbody) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:2rem; color:#94a3b8;"><i class="fas fa-spinner fa-spin"></i> Loading inventory...</td></tr>';
    }

    try {
      const res = await API.getAdminProducts();
      this.products = res.data || [];
      this.renderProductsTable();
    } catch (e) {
      if (tbody) tbody.innerHTML = `<tr><td colspan="7" style="color:#ef4444; text-align:center;">Failed to load products: ${e.message}</td></tr>`;
    }
  },

  renderProductsTable() {
    const tbody = document.getElementById('admin-products-tbody');
    if (!tbody) return;

    if (this.products.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:2.5rem; color:#64748b;">No products in inventory. Click "Add New Product" to create one.</td></tr>';
      return;
    }

    tbody.innerHTML = this.products.map(p => `
      <tr>
        <td>
          <div style="display:flex; align-items:center; gap:0.8rem;">
            <img src="${p.imageUrl}" style="width:44px; height:44px; border-radius:6px; object-fit:cover; background:#000;">
            <div>
              <div style="font-weight:600; color:#f8fafc;">${p.name}</div>
              <div style="font-size:0.75rem; color:#94a3b8;">${p.karatOrPurity || ''} • ${p.weightGrams || ''}</div>
            </div>
          </div>
        </td>
        <td>${p.categoryName || 'N/A'}</td>
        <td style="font-family:'Cinzel',serif; font-weight:700; color:#f3e5ab;">${formatINR(p.price)}</td>
        <td>
          <span style="font-weight:700; color:${p.stockQuantity <= 3 ? '#fbbf24' : '#10b981'};">
            ${p.stockQuantity} pcs
          </span>
        </td>
        <td>
          <button style="background:${p.active ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)'}; color:${p.active ? '#34d399' : '#f87171'}; border:1px solid ${p.active ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)'}; padding:0.25rem 0.6rem; border-radius:var(--radius-full); font-size:0.75rem; font-weight:700; cursor:pointer;" onclick="AdminPortal.handleToggleProductStatus(${p.id})">
            ${p.active ? 'Active' : 'Inactive'}
          </button>
        </td>
        <td>
          <div class="action-btn-group">
            <button class="btn-icon" onclick="AdminPortal.openEditProductModal(${p.id})" title="Edit Product">
              <i class="fas fa-edit"></i>
            </button>
            <button class="btn-icon delete" onclick="AdminPortal.handleDeleteProduct(${p.id})" title="Delete Product">
              <i class="fas fa-trash-alt"></i>
            </button>
          </div>
        </td>
      </tr>
    `).join('');
  },

  // Drag and Drop & File Upload Handlers
  handleDragOver(e) {
    e.preventDefault();
    e.stopPropagation();
    const zone = document.getElementById('image-drop-zone');
    if (zone) zone.classList.add('dragover');
  },

  handleDragLeave(e) {
    e.preventDefault();
    e.stopPropagation();
    const zone = document.getElementById('image-drop-zone');
    if (zone) zone.classList.remove('dragover');
  },

  handleFileDrop(e) {
    e.preventDefault();
    e.stopPropagation();
    const zone = document.getElementById('image-drop-zone');
    if (zone) zone.classList.remove('dragover');

    const dt = e.dataTransfer;
    if (dt && dt.files && dt.files.length > 0) {
      this.uploadAndProcessFile(dt.files[0]);
    }
  },

  handleFileSelect(e) {
    const file = e.target.files?.[0];
    if (file) {
      this.uploadAndProcessFile(file);
    }
  },

  async uploadAndProcessFile(file) {
    // Validate file type
    const validTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    if (!validTypes.includes(file.type)) {
      showToast('Please select a valid image file (JPG, PNG, WEBP, GIF)', 'error');
      return;
    }

    // Validate size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      showToast('Image size exceeds 10MB limit. Please select a smaller photo.', 'error');
      return;
    }

    // Show immediate local preview
    const promptEl = document.getElementById('drop-zone-prompt');
    const previewContainer = document.getElementById('drop-zone-preview-container');
    const previewImg = document.getElementById('prod-image-preview');
    const statusBadge = document.getElementById('upload-status-badge');

    if (promptEl) promptEl.style.display = 'none';
    if (previewContainer) previewContainer.style.display = 'block';
    if (previewImg) previewImg.src = URL.createObjectURL(file);

    if (statusBadge) {
      statusBadge.className = 'upload-badge uploading';
      statusBadge.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Uploading to Server...';
    }

    try {
      const res = await API.uploadProductImage(file);
      const serverImageUrl = res.data.imageUrl;
      document.getElementById('prod-image').value = serverImageUrl;

      if (statusBadge) {
        statusBadge.className = 'upload-badge';
        statusBadge.innerHTML = '<i class="fas fa-check-circle"></i> Image Stored on Server';
      }
      showToast('Jewellery photo uploaded & saved successfully!', 'success');
    } catch (err) {
      console.error('Image upload failed:', err);
      if (statusBadge) {
        statusBadge.className = 'upload-badge';
        statusBadge.style.color = '#ef4444';
        statusBadge.style.borderColor = 'rgba(239,68,68,0.4)';
        statusBadge.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Upload Failed';
      }
      showToast(err.message || 'Failed to upload image to server', 'error');
    }
  },

  openAddProductModal() {
    this.editingProductId = null;
    document.getElementById('admin-product-modal-title').innerHTML = '<i class="fas fa-gem"></i> Add Royal Jewellery Piece';
    document.getElementById('prod-form-id').value = '';
    document.getElementById('prod-name').value = '';
    document.getElementById('prod-price').value = '';
    document.getElementById('prod-stock').value = '5';
    document.getElementById('prod-purity').value = '22K Yellow Gold (916 BIS)';
    document.getElementById('prod-weight').value = '15.00 g';
    document.getElementById('prod-image').value = '';
    document.getElementById('prod-desc').value = '';
    document.getElementById('prod-active').checked = true;
    document.getElementById('prod-featured').checked = false;

    // Reset file input & drop zone
    const fileInput = document.getElementById('prod-file-input');
    if (fileInput) fileInput.value = '';

    const promptEl = document.getElementById('drop-zone-prompt');
    const previewContainer = document.getElementById('drop-zone-preview-container');
    const statusBadge = document.getElementById('upload-status-badge');

    if (promptEl) promptEl.style.display = 'flex';
    if (previewContainer) previewContainer.style.display = 'none';
    if (statusBadge) {
      statusBadge.className = 'upload-badge';
      statusBadge.innerHTML = '<i class="fas fa-check-circle"></i> Image Ready';
    }

    this.populateCategorySelect('prod-category');
    App.openModal('admin-product-modal');
  },

  openEditProductModal(productId) {
    const product = this.products.find(p => p.id === productId);
    if (!product) return;

    this.editingProductId = productId;
    document.getElementById('admin-product-modal-title').innerHTML = '<i class="fas fa-edit"></i> Edit Royal Jewellery Piece';
    document.getElementById('prod-form-id').value = product.id;
    document.getElementById('prod-name').value = product.name;
    document.getElementById('prod-price').value = product.price;
    document.getElementById('prod-stock').value = product.stockQuantity;
    document.getElementById('prod-purity').value = product.karatOrPurity || '';
    document.getElementById('prod-weight').value = product.weightGrams || '';
    document.getElementById('prod-image').value = product.imageUrl || '';
    document.getElementById('prod-desc').value = product.description || '';
    document.getElementById('prod-active').checked = product.active;
    document.getElementById('prod-featured').checked = product.isFeatured;

    // Setup preview for existing product image
    const promptEl = document.getElementById('drop-zone-prompt');
    const previewContainer = document.getElementById('drop-zone-preview-container');
    const previewImg = document.getElementById('prod-image-preview');
    const statusBadge = document.getElementById('upload-status-badge');

    if (product.imageUrl) {
      if (promptEl) promptEl.style.display = 'none';
      if (previewContainer) previewContainer.style.display = 'block';
      if (previewImg) previewImg.src = product.imageUrl;
      if (statusBadge) {
        statusBadge.className = 'upload-badge';
        statusBadge.innerHTML = '<i class="fas fa-check-circle"></i> Current Image';
      }
    } else {
      if (promptEl) promptEl.style.display = 'flex';
      if (previewContainer) previewContainer.style.display = 'none';
    }

    this.populateCategorySelect('prod-category', product.categoryId);
    App.openModal('admin-product-modal');
  },

  populateCategorySelect(selectId, selectedId = null) {
    const select = document.getElementById(selectId);
    if (!select) return;

    select.innerHTML = App.categories.map(c => `
      <option value="${c.id}" ${c.id === selectedId ? 'selected' : ''}>${c.name}</option>
    `).join('');
  },

  async handleSaveProduct(e) {
    e.preventDefault();
    const imageUrl = document.getElementById('prod-image').value.trim();

    if (!imageUrl) {
      showToast('Please upload a product photo by dragging and dropping or browsing an image.', 'warning');
      return;
    }

    const payload = {
      name: document.getElementById('prod-name').value.trim(),
      categoryId: parseInt(document.getElementById('prod-category').value),
      price: parseFloat(document.getElementById('prod-price').value),
      stockQuantity: parseInt(document.getElementById('prod-stock').value),
      karatOrPurity: document.getElementById('prod-purity').value.trim(),
      weightGrams: document.getElementById('prod-weight').value.trim(),
      imageUrl: imageUrl,
      description: document.getElementById('prod-desc').value.trim(),
      active: document.getElementById('prod-active').checked,
      isFeatured: document.getElementById('prod-featured').checked
    };

    try {
      if (this.editingProductId) {
        await API.updateProduct(this.editingProductId, payload);
        showToast('Product updated successfully!', 'success');
      } else {
        await API.createProduct(payload);
        showToast('New jewellery item added to store!', 'success');
      }

      App.closeModal('admin-product-modal');
      this.loadProducts();
      this.loadDashboardStats();
      App.loadProducts();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  async handleToggleProductStatus(productId) {
    try {
      await API.toggleProductStatus(productId);
      showToast('Product visibility status updated', 'info');
      this.loadProducts();
      this.loadDashboardStats();
      App.loadProducts();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  async handleDeleteProduct(productId) {
    if (!confirm('Are you sure you want to delete this product?')) return;
    try {
      await API.deleteProduct(productId);
      showToast('Product removed from store', 'info');
      this.loadProducts();
      this.loadDashboardStats();
      App.loadProducts();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  // Customers Management
  async loadCustomers() {
    const tbody = document.getElementById('admin-customers-tbody');
    if (tbody) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding:2rem; color:#94a3b8;"><i class="fas fa-spinner fa-spin"></i> Loading patron directory...</td></tr>';
    }

    try {
      const res = await API.getAdminCustomers();
      this.customers = res.data || [];
      this.renderCustomersTable();
    } catch (e) {
      if (tbody) tbody.innerHTML = `<tr><td colspan="5" style="color:#ef4444; text-align:center;">Failed to load customers: ${e.message}</td></tr>`;
    }
  },

  renderCustomersTable() {
    const tbody = document.getElementById('admin-customers-tbody');
    if (!tbody) return;

    if (this.customers.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding:2.5rem; color:#64748b;">No registered customers yet.</td></tr>';
      return;
    }

    tbody.innerHTML = this.customers.map(c => `
      <tr>
        <td><strong>${c.fullName}</strong></td>
        <td>${c.email}</td>
        <td>${c.phone}</td>
        <td>${c.addresses?.length || 0} saved</td>
        <td><span class="status-badge confirmed">${c.role}</span></td>
      </tr>
    `).join('');
  }
};
