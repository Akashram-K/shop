/**
 * RoyaL Jwellery - Storefront & Customer Experience Logic
 */

const App = {
  currentCategory: null,
  searchQuery: '',
  products: [],
  categories: [],
  selectedProductForModal: null,

  async init() {
    this.bindEvents();
    await this.loadInitialState();
    this.handleRoute();
  },

  async loadInitialState() {
    try {
      const user = await API.checkAuth();
      this.updateAuthUI(user);

      if (user) {
        await this.syncCart();
      }

      await Promise.all([
        this.loadCategories(),
        this.loadProducts()
      ]);
    } catch (err) {
      console.error('Initialization error:', err);
    }
  },

  bindEvents() {
    // Search with debounce
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
      let debounceTimer;
      searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
          this.searchQuery = e.target.value.trim();
          this.loadProducts();
        }, 300);
      });
    }

    // Window PopState for SPA hash navigation
    window.addEventListener('popstate', () => this.handleRoute());
  },

  handleRoute() {
    const path = window.location.pathname;
    const hash = window.location.hash;

    if (path === '/admin' || hash === '#admin') {
      this.showAdminView();
    } else if (path === '/orders' || hash === '#orders') {
      this.showOrdersView();
    } else if (path === '/profile' || hash === '#profile') {
      this.showProfileView();
    } else {
      this.showStoreView();
    }
  },

  showStoreView() {
    document.getElementById('store-view').style.display = 'block';
    const adminView = document.getElementById('admin-view');
    if (adminView) adminView.style.display = 'none';
    const customerPortal = document.getElementById('customer-portal-view');
    if (customerPortal) customerPortal.style.display = 'none';
  },

  async showAdminView() {
    const user = API.currentUser;
    if (!user || user.role !== 'ROLE_ADMIN') {
      showToast('Admin login required to access Father Dashboard', 'error');
      this.openAuthModal('login');
      return;
    }

    document.getElementById('store-view').style.display = 'none';
    const customerPortal = document.getElementById('customer-portal-view');
    if (customerPortal) customerPortal.style.display = 'none';

    const adminView = document.getElementById('admin-view');
    if (adminView) {
      adminView.style.display = 'block';
      if (window.AdminPortal) {
        window.AdminPortal.init();
      }
    }
  },

  async showOrdersView() {
    if (!API.currentUser) {
      showToast('Please log in to view your orders', 'info');
      this.openAuthModal('login');
      return;
    }

    document.getElementById('store-view').style.display = 'none';
    const adminView = document.getElementById('admin-view');
    if (adminView) adminView.style.display = 'none';

    const customerPortal = document.getElementById('customer-portal-view');
    if (customerPortal) {
      customerPortal.style.display = 'block';
      this.switchCustomerTab('orders');
      await this.loadCustomerOrders();
    }
  },

  async showProfileView() {
    if (!API.currentUser) {
      showToast('Please log in to view your profile', 'info');
      this.openAuthModal('login');
      return;
    }

    document.getElementById('store-view').style.display = 'none';
    const adminView = document.getElementById('admin-view');
    if (adminView) adminView.style.display = 'none';

    const customerPortal = document.getElementById('customer-portal-view');
    if (customerPortal) {
      customerPortal.style.display = 'block';
      this.switchCustomerTab('profile');
      this.renderCustomerProfile();
    }
  },

  updateAuthUI(user) {
    const guestNav = document.getElementById('nav-guest-actions');
    const userNav = document.getElementById('nav-user-actions');
    const adminLink = document.getElementById('nav-admin-link');
    const userNameSpan = document.getElementById('user-display-name');
    const userRoleSpan = document.getElementById('user-display-role');

    if (user) {
      if (guestNav) guestNav.style.display = 'none';
      if (userNav) userNav.style.display = 'flex';
      if (userNameSpan) userNameSpan.textContent = user.fullName;
      if (userRoleSpan) userRoleSpan.textContent = user.role === 'ROLE_ADMIN' ? 'Store Owner / Admin' : 'Valued Patron';

      if (adminLink) {
        adminLink.style.display = user.role === 'ROLE_ADMIN' ? 'flex' : 'none';
      }
    } else {
      if (guestNav) guestNav.style.display = 'flex';
      if (userNav) userNav.style.display = 'none';
      if (adminLink) adminLink.style.display = 'none';
    }
  },

  toggleUserDropdown() {
    const dd = document.getElementById('user-dropdown-menu');
    if (dd) dd.classList.toggle('show');
  },

  // Categories
  async loadCategories() {
    try {
      const res = await API.getCategories();
      this.categories = res.data || [];
      this.renderCategoryPills();
    } catch (e) {
      console.error('Failed to load categories', e);
    }
  },

  renderCategoryPills() {
    const container = document.getElementById('category-pills-container');
    if (!container) return;

    let html = `
      <button class="category-pill ${!this.currentCategory ? 'active' : ''}" onclick="App.filterCategory(null)">
        <i class="fas fa-gem"></i> All Masterpieces
      </button>
    `;

    this.categories.forEach(cat => {
      const isActive = this.currentCategory === cat.id ? 'active' : '';
      html += `
        <button class="category-pill ${isActive}" onclick="App.filterCategory(${cat.id})">
          <i class="fas ${cat.icon || 'fa-sparkles'}"></i> ${cat.name}
        </button>
      `;
    });

    container.innerHTML = html;
  },

  filterCategory(catId) {
    this.currentCategory = catId;
    this.renderCategoryPills();
    this.loadProducts();
  },

  // Products
  async loadProducts() {
    const grid = document.getElementById('products-grid');
    if (!grid) return;

    grid.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 4rem 1rem; color: #94a3b8;">
        <i class="fas fa-spinner fa-spin" style="font-size: 2.5rem; color: #d4af37; margin-bottom: 1rem;"></i>
        <p>Curating royal collection...</p>
      </div>
    `;

    try {
      const res = await API.getProducts(this.searchQuery, this.currentCategory);
      this.products = res.data || [];
      this.renderProducts();
    } catch (e) {
      grid.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: #ef4444;">
          <i class="fas fa-exclamation-circle" style="font-size: 2.5rem; margin-bottom: 1rem;"></i>
          <p>Failed to load products. Please refresh.</p>
        </div>
      `;
    }
  },

  renderProducts() {
    const grid = document.getElementById('products-grid');
    if (!grid) return;

    if (this.products.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 4rem 1rem; color: #94a3b8;">
          <i class="fas fa-search" style="font-size: 3rem; color: rgba(212, 175, 55, 0.4); margin-bottom: 1rem;"></i>
          <h3 style="color: #f8fafc; margin-bottom: 0.5rem;">No Jewellery Found</h3>
          <p>Try searching for diamond, necklace, kada or select another royal category.</p>
        </div>
      `;
      return;
    }

    grid.innerHTML = this.products.map(p => {
      let stockClass = 'stock-in';
      let stockText = 'In Stock';
      let isOutOfStock = p.stockQuantity <= 0;

      if (isOutOfStock) {
        stockClass = 'stock-out';
        stockText = 'Out of Stock';
      } else if (p.stockQuantity <= 3) {
        stockClass = 'stock-low';
        stockText = `Only ${p.stockQuantity} Left`;
      }

      return `
        <div class="product-card">
          <div class="product-thumb" onclick="App.openProductModal(${p.id})">
            <img src="${p.imageUrl || 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=800&q=80'}" alt="${p.name}" loading="lazy" />
            <div class="product-badge-purity">${p.karatOrPurity || '22K Gold'}</div>
            <div class="product-badge-stock ${stockClass}">${stockText}</div>
          </div>
          <div class="product-body">
            <div class="product-category">${p.categoryName || 'Royal Collection'}</div>
            <h3 class="product-title" onclick="App.openProductModal(${p.id})">${p.name}</h3>
            <p class="product-desc">${p.description || ''}</p>
            <div class="product-specs">
              <span><i class="fas fa-weight-hanging"></i> ${p.weightGrams || 'N/A'}</span>
              <span><i class="fas fa-certificate"></i> BIS Hallmarked</span>
            </div>
            <div class="product-footer">
              <div class="product-price">${formatINR(p.price)}</div>
              <button class="btn-add-cart" ${isOutOfStock ? 'disabled' : ''} onclick="App.handleAddToCart(${p.id})">
                <i class="fas fa-shopping-bag"></i> ${isOutOfStock ? 'Sold Out' : 'Add to Cart'}
              </button>
            </div>
          </div>
        </div>
      `;
    }).join('');
  },

  // Product Details Modal
  openProductModal(productId) {
    const product = this.products.find(p => p.id === productId);
    if (!product) return;
    this.selectedProductForModal = product;

    document.getElementById('modal-product-img').src = product.imageUrl;
    document.getElementById('modal-product-category').textContent = product.categoryName || 'Royal Collection';
    document.getElementById('modal-product-title').textContent = product.name;
    document.getElementById('modal-product-price').textContent = formatINR(product.price);
    document.getElementById('modal-product-purity').textContent = product.karatOrPurity || '22K Hallmarked Gold';
    document.getElementById('modal-product-weight').textContent = product.weightGrams || 'Standard weight';
    document.getElementById('modal-product-desc').textContent = product.description || '';
    document.getElementById('modal-product-stock').textContent = `${product.stockQuantity} items available in boutique stock`;

    const qtyInput = document.getElementById('modal-product-qty');
    if (qtyInput) {
      qtyInput.value = 1;
      qtyInput.max = product.stockQuantity;
    }

    const addBtn = document.getElementById('modal-btn-add-cart');
    if (addBtn) {
      addBtn.disabled = product.stockQuantity <= 0;
      addBtn.innerHTML = product.stockQuantity <= 0 
        ? '<i class="fas fa-ban"></i> Currently Unavailable' 
        : '<i class="fas fa-shopping-bag"></i> Add to Royal Cart';
    }

    this.openModal('product-details-modal');
  },

  modalAdjustQty(delta) {
    const input = document.getElementById('modal-product-qty');
    if (!input || !this.selectedProductForModal) return;
    let val = parseInt(input.value) + delta;
    if (val < 1) val = 1;
    if (val > this.selectedProductForModal.stockQuantity) {
      val = this.selectedProductForModal.stockQuantity;
      showToast(`Only ${val} items available in stock`, 'info');
    }
    input.value = val;
  },

  async handleModalAddToCart() {
    if (!this.selectedProductForModal) return;
    const qty = parseInt(document.getElementById('modal-product-qty').value) || 1;
    await this.handleAddToCart(this.selectedProductForModal.id, qty);
    this.closeModal('product-details-modal');
  },

  // Cart Operations
  async syncCart() {
    try {
      const res = await API.getCart();
      this.updateCartBadge(res.data?.totalItems || 0);
      this.renderCartDrawer(res.data);
    } catch (e) {
      // User might be unauthenticated
      this.updateCartBadge(0);
    }
  },

  updateCartBadge(count) {
    const badge = document.getElementById('cart-badge-count');
    if (badge) {
      badge.textContent = count;
      badge.style.display = count > 0 ? 'flex' : 'none';
    }
  },

  async handleAddToCart(productId, quantity = 1) {
    if (!API.currentUser) {
      showToast('Please log in or create an account to start shopping', 'info');
      this.openAuthModal('login');
      return;
    }

    try {
      const res = await API.addToCart(productId, quantity);
      this.updateCartBadge(res.data?.totalItems || 0);
      this.renderCartDrawer(res.data);
      showToast('✨ Masterpiece added to your cart!', 'success');
      this.openCartDrawer();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  openCartDrawer() {
    if (!API.currentUser) {
      this.openAuthModal('login');
      return;
    }
    this.syncCart();
    document.getElementById('cart-overlay').classList.add('open');
  },

  closeCartDrawer() {
    document.getElementById('cart-overlay').classList.remove('open');
  },

  renderCartDrawer(cart) {
    const body = document.getElementById('cart-drawer-body');
    const footer = document.getElementById('cart-drawer-footer');
    if (!body || !footer) return;

    if (!cart || !cart.items || cart.items.length === 0) {
      body.innerHTML = `
        <div class="cart-empty">
          <i class="fas fa-gem"></i>
          <h4 style="color: #f8fafc; margin-bottom: 0.5rem;">Your Cart is Empty</h4>
          <p style="font-size: 0.88rem;">Explore our royal collection and discover eternal fine jewellery.</p>
        </div>
      `;
      footer.style.display = 'none';
      return;
    }

    footer.style.display = 'block';

    body.innerHTML = cart.items.map(item => `
      <div class="cart-item">
        <img class="cart-item-img" src="${item.imageUrl}" alt="${item.productName}">
        <div class="cart-item-info">
          <div class="cart-item-title">${item.productName}</div>
          <div class="cart-item-purity">${item.karatOrPurity || 'Fine Jewellery'}</div>
          <div class="cart-item-price">${formatINR(item.productPrice)}</div>
        </div>
        <div class="cart-item-actions">
          <div class="qty-control">
            <button class="qty-btn" onclick="App.updateCartQty(${item.id}, ${item.quantity - 1})">-</button>
            <span class="qty-val">${item.quantity}</span>
            <button class="qty-btn" onclick="App.updateCartQty(${item.id}, ${item.quantity + 1})">+</button>
          </div>
          <button class="btn-remove-item" onclick="App.removeCartItem(${item.id})" title="Remove item">
            <i class="fas fa-trash-alt"></i>
          </button>
        </div>
      </div>
    `).join('');

    document.getElementById('cart-subtotal-val').textContent = formatINR(cart.totalAmount);
    document.getElementById('cart-total-val').textContent = formatINR(cart.totalAmount);
  },

  async updateCartQty(itemId, newQty) {
    try {
      const res = await API.updateCartItem(itemId, newQty);
      this.updateCartBadge(res.data?.totalItems || 0);
      this.renderCartDrawer(res.data);
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  async removeCartItem(itemId) {
    try {
      const res = await API.removeFromCart(itemId);
      this.updateCartBadge(res.data?.totalItems || 0);
      this.renderCartDrawer(res.data);
      showToast('Item removed from cart', 'info');
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  // Checkout Flow
  async proceedToCheckout() {
    if (!API.currentUser) {
      this.closeCartDrawer();
      this.openAuthModal('login');
      return;
    }

    this.closeCartDrawer();
    await this.prepareCheckoutModal();
    this.openModal('checkout-modal');
  },

  async prepareCheckoutModal() {
    const user = API.currentUser;
    const cart = API.cartData;

    document.getElementById('checkout-items-count').textContent = cart?.totalItems || 0;
    document.getElementById('checkout-total-val').textContent = formatINR(cart?.totalAmount);

    // Populate Addresses
    const addrContainer = document.getElementById('checkout-addresses-list');
    try {
      const res = await API.getAddresses();
      const addresses = res.data || [];

      if (addresses.length > 0) {
        addrContainer.innerHTML = `
          <div style="margin-bottom: 0.8rem; font-weight: 600; color: #f3e5ab; font-size: 0.9rem;">
            Select Saved Delivery Address:
          </div>
          ${addresses.map((addr, idx) => `
            <label style="display: flex; gap: 0.8rem; background: rgba(7,9,14,0.8); border: 1px solid var(--border-glass); padding: 0.8rem; border-radius: var(--radius-sm); margin-bottom: 0.6rem; cursor: pointer;">
              <input type="radio" name="checkout-addr-radio" value="${addr.id}" ${addr.default || idx === 0 ? 'checked' : ''} onchange="App.toggleNewAddressForm(false)">
              <div style="font-size: 0.86rem;">
                <strong>${addr.recipientName || user.fullName}</strong> (${addr.phone || user.phone})<br>
                <span style="color: #94a3b8;">${addr.formattedAddress}</span>
              </div>
            </label>
          `).join('')}
          <label style="display: flex; gap: 0.8rem; background: rgba(7,9,14,0.4); border: 1px dashed var(--border-glass); padding: 0.8rem; border-radius: var(--radius-sm); cursor: pointer;">
            <input type="radio" name="checkout-addr-radio" value="new" onchange="App.toggleNewAddressForm(true)">
            <div style="font-size: 0.88rem; color: #d4af37; font-weight: 600;">
              + Deliver to a Different Address
            </div>
          </label>
        `;
        document.getElementById('checkout-new-address-form').style.display = 'none';
      } else {
        addrContainer.innerHTML = '';
        document.getElementById('checkout-new-address-form').style.display = 'block';
      }
    } catch (e) {
      document.getElementById('checkout-new-address-form').style.display = 'block';
    }
  },

  toggleNewAddressForm(show) {
    const form = document.getElementById('checkout-new-address-form');
    if (form) form.style.display = show ? 'block' : 'none';
  },

  async handlePlaceOrder(e) {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-order');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing Royal Order...';

    try {
      const selectedRadio = document.querySelector('input[name="checkout-addr-radio"]:checked');
      const orderPayload = {
        paymentMethod: document.getElementById('checkout-payment-method').value,
        notes: document.getElementById('checkout-notes').value
      };

      if (selectedRadio && selectedRadio.value !== 'new') {
        orderPayload.addressId = parseInt(selectedRadio.value);
      } else {
        orderPayload.recipientName = document.getElementById('co-name').value;
        orderPayload.phone = document.getElementById('co-phone').value;
        orderPayload.street = document.getElementById('co-street').value;
        orderPayload.city = document.getElementById('co-city').value;
        orderPayload.state = document.getElementById('co-state').value;
        orderPayload.pincode = document.getElementById('co-pincode').value;
        orderPayload.landmark = document.getElementById('co-landmark').value;
        orderPayload.saveAddress = document.getElementById('co-save-addr').checked;
      }

      const res = await API.placeOrder(orderPayload);
      this.closeModal('checkout-modal');
      this.updateCartBadge(0);

      // Show Order Success Screen
      this.showOrderConfirmation(res.data);
      showToast('🎉 Order placed successfully!', 'success');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="fas fa-shield-alt"></i> Confirm & Place Order';
    }
  },

  showOrderConfirmation(order) {
    document.getElementById('conf-order-number').textContent = order.orderNumber;
    document.getElementById('conf-order-total').textContent = formatINR(order.totalAmount);
    document.getElementById('conf-recipient-name').textContent = order.recipientName;
    document.getElementById('conf-recipient-phone').textContent = order.recipientPhone;
    document.getElementById('conf-address').textContent = order.deliveryAddress;
    document.getElementById('conf-payment').textContent = order.paymentMethod;

    this.openModal('order-success-modal');
  },

  // Customer Portal & Tracking
  switchCustomerTab(tab) {
    document.querySelectorAll('.cust-tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.cust-tab-content').forEach(c => c.style.display = 'none');

    const btn = document.getElementById(`cust-tab-btn-${tab}`);
    if (btn) btn.classList.add('active');

    const content = document.getElementById(`cust-tab-${tab}`);
    if (content) content.style.display = 'block';
  },

  async loadCustomerOrders() {
    const container = document.getElementById('customer-orders-list');
    if (!container) return;

    container.innerHTML = `
      <div style="text-align:center; padding: 2rem; color: #94a3b8;">
        <i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #d4af37;"></i>
        <p style="margin-top:0.8rem;">Retrieving order history...</p>
      </div>
    `;

    try {
      const res = await API.getMyOrders();
      const orders = res.data || [];

      if (orders.length === 0) {
        container.innerHTML = `
          <div style="text-align: center; padding: 3rem; color: #94a3b8;">
            <i class="fas fa-box-open" style="font-size: 3rem; color: rgba(212,175,55,0.4); margin-bottom: 1rem;"></i>
            <h3>No Orders Placed Yet</h3>
            <p style="margin-top: 0.4rem;">Browse our collection and treasure your first royal jewellery piece.</p>
            <button class="nav-btn primary" style="margin-top: 1.2rem;" onclick="App.showStoreView()">Explore Boutique</button>
          </div>
        `;
        return;
      }

      container.innerHTML = orders.map(o => `
        <div style="background: var(--bg-card); border: 1px solid var(--border-glass); border-radius: var(--radius-md); padding: 1.4rem; margin-bottom: 1.4rem;">
          <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 0.8rem; margin-bottom: 1rem; flex-wrap: wrap; gap: 0.5rem;">
            <div>
              <span style="font-family:'Cinzel',serif; font-weight: 700; color: #f3e5ab; font-size: 1.05rem;">Order #${o.orderNumber}</span>
              <span style="font-size: 0.8rem; color: #64748b; margin-left: 0.8rem;">${new Date(o.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 0.8rem;">
              <span class="status-badge ${o.status.toLowerCase()}">${o.status}</span>
              <button class="btn-icon" title="Track Order Timeline" onclick="App.openOrderTracking(${o.id})">
                <i class="fas fa-shipping-fast"></i>
              </button>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 1rem; margin-bottom: 1rem;">
            ${o.items.map(item => `
              <div style="display: flex; gap: 0.8rem; align-items: center;">
                <img src="${item.imageUrl}" style="width: 48px; height: 48px; border-radius: 6px; object-fit: cover; background: #000;">
                <div>
                  <div style="font-size: 0.85rem; font-weight: 600; color: #f8fafc;">${item.productName}</div>
                  <div style="font-size: 0.78rem; color: #94a3b8;">Qty: ${item.quantity} × ${formatINR(item.productPrice)}</div>
                </div>
              </div>
            `).join('')}
          </div>

          <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid rgba(255,255,255,0.06); padding-top: 0.8rem; font-size: 0.9rem;">
            <div><strong>Deliver to:</strong> <span style="color: #94a3b8;">${o.recipientName} (${o.recipientPhone})</span></div>
            <div style="font-family:'Cinzel',serif; font-size: 1.15rem; font-weight: 800; color: #f3e5ab;">Total: ${formatINR(o.totalAmount)}</div>
          </div>
        </div>
      `).join('');
    } catch (e) {
      container.innerHTML = `<p style="color:#ef4444;">Failed to load orders: ${e.message}</p>`;
    }
  },

  async openOrderTracking(orderId) {
    try {
      const res = await API.getOrder(orderId);
      const order = res.data;

      document.getElementById('track-order-num').textContent = order.orderNumber;
      document.getElementById('track-order-date').textContent = new Date(order.createdAt).toLocaleDateString('en-IN', {
        day: 'numeric', month: 'long', year: 'numeric'
      });
      document.getElementById('track-order-total').textContent = formatINR(order.totalAmount);
      document.getElementById('track-delivery-address').textContent = order.deliveryAddress;

      // Status progress
      const statuses = ['PLACED', 'CONFIRMED', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED'];
      const currentIndex = statuses.indexOf(order.status);
      const isCancelled = order.status === 'CANCELLED';

      const progressFill = document.getElementById('tracking-progress-fill');
      const timelineBox = document.getElementById('tracking-timeline-box');

      if (isCancelled) {
        timelineBox.innerHTML = `
          <div style="background: rgba(239, 68, 68, 0.15); border: 1px solid rgba(239, 68, 68, 0.3); padding: 1.2rem; border-radius: var(--radius-sm); text-align: center; color: #f87171;">
            <i class="fas fa-times-circle" style="font-size: 2rem; margin-bottom: 0.5rem;"></i>
            <h4>This Order has been Cancelled</h4>
            <p style="font-size: 0.85rem; margin-top: 0.3rem;">Any debited amounts or stock holds have been reversed.</p>
          </div>
        `;
      } else {
        const percent = currentIndex >= 0 ? (currentIndex / (statuses.length - 1)) * 100 : 0;
        progressFill.style.width = `${percent}%`;

        statuses.forEach((st, idx) => {
          const stepEl = document.getElementById(`track-step-${st.toLowerCase()}`);
          if (stepEl) {
            stepEl.className = 'tracking-step';
            if (idx < currentIndex) {
              stepEl.classList.add('completed');
            } else if (idx === currentIndex) {
              stepEl.classList.add('active');
            }
          }
        });
      }

      // Items list
      document.getElementById('track-items-list').innerHTML = order.items.map(item => `
        <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0; border-bottom: 1px solid rgba(255,255,255,0.04);">
          <div style="display: flex; align-items: center; gap: 0.8rem;">
            <img src="${item.imageUrl}" style="width: 40px; height: 40px; border-radius: 4px; object-fit: cover;">
            <div>
              <div style="font-size: 0.88rem; font-weight: 600;">${item.productName}</div>
              <div style="font-size: 0.75rem; color: #94a3b8;">Qty: ${item.quantity}</div>
            </div>
          </div>
          <div style="font-weight: 700; color: #f3e5ab;">${formatINR(item.subtotal)}</div>
        </div>
      `).join('');

      // Cancel button
      const cancelBtn = document.getElementById('btn-cancel-my-order');
      if (cancelBtn) {
        cancelBtn.style.display = (order.status === 'PLACED' || order.status === 'CONFIRMED') ? 'block' : 'none';
        cancelBtn.onclick = () => App.handleCancelOrder(order.id);
      }

      this.openModal('order-tracking-modal');
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  async handleCancelOrder(orderId) {
    if (!confirm('Are you sure you want to cancel this order?')) return;
    try {
      await API.cancelOrder(orderId);
      showToast('Order cancelled successfully', 'info');
      this.closeModal('order-tracking-modal');
      this.loadCustomerOrders();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  renderCustomerProfile() {
    const user = API.currentUser;
    if (!user) return;

    document.getElementById('prof-name').value = user.fullName || '';
    document.getElementById('prof-email').value = user.email || '';
    document.getElementById('prof-phone').value = user.phone || '';

    this.loadProfileAddresses();
  },

  async loadProfileAddresses() {
    const container = document.getElementById('profile-addresses-list');
    if (!container) return;

    try {
      const res = await API.getAddresses();
      const addresses = res.data || [];

      if (addresses.length === 0) {
        container.innerHTML = '<p style="color:#64748b;">No saved addresses yet.</p>';
        return;
      }

      container.innerHTML = addresses.map(addr => `
        <div style="background: rgba(7,9,14,0.8); border: 1px solid var(--border-glass); border-radius: var(--radius-sm); padding: 1rem; margin-bottom: 0.8rem;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.3rem;">
            <strong>${addr.recipientName}</strong>
            ${addr.default ? '<span style="font-size: 0.7rem; background: rgba(212,175,55,0.2); color: #d4af37; padding: 0.15rem 0.4rem; border-radius: 4px;">Default</span>' : ''}
          </div>
          <div style="font-size: 0.85rem; color: #94a3b8;">${addr.formattedAddress}</div>
        </div>
      `).join('');
    } catch (e) {
      console.error(e);
    }
  },

  async handleUpdateProfile(e) {
    e.preventDefault();
    const fullName = document.getElementById('prof-name').value;
    const phone = document.getElementById('prof-phone').value;

    try {
      const res = await API.updateProfile(fullName, phone);
      API.currentUser = res.data;
      this.updateAuthUI(res.data);
      showToast('Profile updated successfully!', 'success');
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  async handleAddProfileAddress(e) {
    e.preventDefault();
    const payload = {
      recipientName: document.getElementById('new-addr-name').value,
      phone: document.getElementById('new-addr-phone').value,
      street: document.getElementById('new-addr-street').value,
      city: document.getElementById('new-addr-city').value,
      state: document.getElementById('new-addr-state').value,
      pincode: document.getElementById('new-addr-pincode').value,
      landmark: document.getElementById('new-addr-landmark').value,
      default: document.getElementById('new-addr-default').checked
    };

    try {
      await API.addAddress(payload);
      showToast('New address saved!', 'success');
      this.closeModal('add-address-modal');
      this.loadProfileAddresses();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  // Auth Modals & Actions
  openAuthModal(type = 'login') {
    if (type === 'login') {
      document.getElementById('auth-login-box').style.display = 'block';
      document.getElementById('auth-register-box').style.display = 'none';
    } else {
      document.getElementById('auth-login-box').style.display = 'none';
      document.getElementById('auth-register-box').style.display = 'block';
    }
    this.openModal('auth-modal');
  },

  async handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    try {
      const res = await API.login(email, password);
      showToast(`Welcome back, ${res.data.fullName}!`, 'success');
      this.closeModal('auth-modal');
      this.updateAuthUI(res.data);
      await this.syncCart();

      if (res.data.role === 'ROLE_ADMIN') {
        this.showAdminView();
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  async handleRegister(e) {
    e.preventDefault();
    const payload = {
      fullName: document.getElementById('reg-name').value,
      email: document.getElementById('reg-email').value,
      phone: document.getElementById('reg-phone').value,
      password: document.getElementById('reg-password').value,
      street: document.getElementById('reg-street').value,
      city: document.getElementById('reg-city').value,
      state: document.getElementById('reg-state').value,
      pincode: document.getElementById('reg-pincode').value,
      landmark: document.getElementById('reg-landmark').value
    };

    try {
      const res = await API.register(payload);
      showToast(`Account created! Welcome to RoyaL Jwellery, ${res.data.fullName}.`, 'success');
      this.closeModal('auth-modal');
      this.updateAuthUI(res.data);
      await this.syncCart();
    } catch (err) {
      showToast(err.message, 'error');
    }
  },

  async handleLogout() {
    try {
      await API.logout();
      showToast('You have been safely logged out', 'info');
      this.updateAuthUI(null);
      this.updateCartBadge(0);
      this.showStoreView();
    } catch (e) {
      console.error(e);
    }
  },

  // Modal Generic Helpers
  openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('open');
  },

  closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('open');
  }
};

// Start application upon DOM loaded
document.addEventListener('DOMContentLoaded', () => {
  App.init();
});
