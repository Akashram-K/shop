package com.shop.shop.config;

import com.shop.shop.entity.*;
import com.shop.shop.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           AddressRepository addressRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           CartRepository cartRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        initUsers();
        initCategoriesAndProducts();
    }

    private void initUsers() {
        // 1. Admin / Father Account (Krishnan)
        String adminEmail = "krishnansopi@gmail.com";
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        if (admin == null) {
            admin = new User(
                    "Krishnan (Store Owner)",
                    adminEmail,
                    passwordEncoder.encode("krishnan@1979"),
                    "+91 98765 43210",
                    Role.ROLE_ADMIN
            );
            admin = userRepository.save(admin);

            Address adminAddress = new Address(
                    admin,
                    "RoyaL Jwellery Store",
                    "+91 98765 43210",
                    "108 Gold Souk Palace, Royal Heritage Avenue",
                    "Mumbai",
                    "Maharashtra",
                    "400001",
                    "Opposite Diamond Plaza",
                    true
            );
            addressRepository.save(adminAddress);
            cartRepository.save(new Cart(admin));
        } else {
            // Update password & ensure ADMIN role
            admin.setPassword(passwordEncoder.encode("krishnan@1979"));
            admin.setRole(Role.ROLE_ADMIN);
            admin.setFullName("Krishnan (Store Owner)");
            userRepository.save(admin);
        }

        // 2. Demo Customer Account
        if (userRepository.findByEmail("customer@gmail.com").isEmpty()) {
            User customer = new User(
                    "Rahul Sharma",
                    "customer@gmail.com",
                    passwordEncoder.encode("customer123"),
                    "+91 98123 45678",
                    Role.ROLE_CUSTOMER
            );
            customer = userRepository.save(customer);

            Address customerAddress = new Address(
                    customer,
                    "Rahul Sharma",
                    "+91 98123 45678",
                    "Flat 4B, Emerald Heights, MG Road",
                    "Bengaluru",
                    "Karnataka",
                    "560001",
                    "Near Trinity Circle Metro Station",
                    true
            );
            addressRepository.save(customerAddress);
            cartRepository.save(new Cart(customer));
        }
    }

    private void initCategoriesAndProducts() {
        if (categoryRepository.count() > 0) {
            return;
        }

        // Categories
        Category necklaces = categoryRepository.save(new Category(
                "Royal Necklaces & Chokers",
                "Grand 22K yellow gold and antique chokers handcrafted for auspicious celebrations.",
                "fa-crown",
                "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=800&q=80"
        ));

        Category rings = categoryRepository.save(new Category(
                "Diamond & Solitaire Rings",
                "Certified VVS1 clarity diamond rings, engagement solitaires, and eternity bands.",
                "fa-gem",
                "https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=800&q=80"
        ));

        Category bridal = categoryRepository.save(new Category(
                "Kundan & Bridal Sets",
                "Intricate Jadau and Kundan bridal jewelry featuring meenakari enameling and pearls.",
                "fa-heart",
                "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?auto=format&fit=crop&w=800&q=80"
        ));

        Category bangles = categoryRepository.save(new Category(
                "Gold & Platinum Bangles",
                "Traditional kada bangles, contemporary platinum bracelets, and temple work kadas.",
                "fa-ring",
                "https://images.unsplash.com/photo-1611591475102-4545d6a2f3a6?auto=format&fit=crop&w=800&q=80"
        ));

        Category earrings = categoryRepository.save(new Category(
                "Royal Earrings & Jhumkas",
                "Chandelier drop jhumkas, diamond studs, and polki peacock earrings.",
                "fa-sparkles",
                "https://images.unsplash.com/photo-1630019852942-f89202989a59?auto=format&fit=crop&w=800&q=80"
        ));

        Category pendants = categoryRepository.save(new Category(
                "Gemstone & Pearl Pendants",
                "Natural Colombian emeralds, Burmese rubies, and South Sea pearls set in 18K gold.",
                "fa-gem",
                "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?auto=format&fit=crop&w=800&q=80"
        ));

        // Products
        productRepository.save(new Product(
                "Maharani Kundan & Emerald Bridal Necklace",
                "Exquisite royal heirloom necklace adorned with uncut polki diamonds, hand-strung Colombian emerald drops, and 22K hallmarked yellow gold finish.",
                new BigDecimal("148500.00"),
                "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=800&q=80",
                necklaces,
                5,
                true,
                true,
                "22K Gold & Natural Emerald",
                "48.20 g"
        ));

        productRepository.save(new Product(
                "Princess Cut Solitaire Diamond Ring (1.2 Ct)",
                "IGI Certified 1.20 Carat Princess Cut Diamond Ring in 18K White Gold with micro-pave diamond band. Color: D, Clarity: VVS1.",
                new BigDecimal("89000.00"),
                "https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=800&q=80",
                rings,
                8,
                true,
                true,
                "18K White Gold / 1.2ct Diamond",
                "4.80 g"
        ));

        productRepository.save(new Product(
                "Imperial Peacock Gold Choker Necklace",
                "A regal antique masterpiece featuring divine peacock motifs in 22K yellow gold with subtle ruby accents and freshwater pearls.",
                new BigDecimal("125000.00"),
                "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?auto=format&fit=crop&w=800&q=80",
                necklaces,
                4,
                true,
                true,
                "22K Yellow Gold (916 BIS)",
                "36.50 g"
        ));

        productRepository.save(new Product(
                "Kundan Polki Bridal Chandelier Jhumkas",
                "Multi-tiered grand bridal jhumkas embellished with royal polki stones, hanging pearl clusters, and delicate red meenakari work.",
                new BigDecimal("42500.00"),
                "https://images.unsplash.com/photo-1630019852942-f89202989a59?auto=format&fit=crop&w=800&q=80",
                earrings,
                12,
                true,
                true,
                "22K Yellow Gold",
                "22.40 g"
        ));

        productRepository.save(new Product(
                "Royal Heritage Filigree Gold Kada Bangles (Pair)",
                "A pair of heavy hand-crafted filigree gold kadas with screw lock mechanism and intricate royal relief carvings.",
                new BigDecimal("98000.00"),
                "https://images.unsplash.com/photo-1611591475102-4545d6a2f3a6?auto=format&fit=crop&w=800&q=80",
                bangles,
                6,
                true,
                true,
                "22K Yellow Gold (916 BIS)",
                "32.00 g"
        ));

        productRepository.save(new Product(
                "Natural Ruby & Diamond Halo Drop Earrings",
                "Pigeon blood certified Burmese rubies encircled by radiant halo diamonds in 18K rose gold setting.",
                new BigDecimal("54000.00"),
                "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?auto=format&fit=crop&w=800&q=80",
                earrings,
                7,
                true,
                false,
                "18K Rose Gold & Natural Ruby",
                "8.60 g"
        ));

        productRepository.save(new Product(
                "Classic Platinum & Rose Gold Couple Bands",
                "Matching his & hers luxury bands in 950 Platinum and 18K Rose Gold with subtle flush-set diamond accents.",
                new BigDecimal("62000.00"),
                "https://images.unsplash.com/photo-1598560917505-59a3ad559071?auto=format&fit=crop&w=800&q=80",
                rings,
                10,
                true,
                false,
                "950 Platinum & 18K Rose Gold",
                "12.00 g"
        ));

        productRepository.save(new Product(
                "South Sea Golden Pearl & Diamond Pendant",
                "Flawless 13mm natural golden South Sea pearl suspended beneath a brilliant cluster of round and marquise diamonds.",
                new BigDecimal("38500.00"),
                "https://images.unsplash.com/photo-1506630448388-4e683c67ddb0?auto=format&fit=crop&w=800&q=80",
                pendants,
                9,
                true,
                true,
                "18K Yellow Gold & South Sea Pearl",
                "7.20 g"
        ));

        productRepository.save(new Product(
                "Complete Royal Jadau Bridal Wedding Set",
                "The ultimate bridal collection: Grand choker, long rani haar, matching matha patti, haathphool, and matching heavy earrings in 22K gold.",
                new BigDecimal("285000.00"),
                "https://images.unsplash.com/photo-1602751584552-8ba73aad10e1?auto=format&fit=crop&w=800&q=80",
                bridal,
                2,
                true,
                true,
                "22K Gold, Polki & Basra Pearls",
                "115.00 g"
        ));

        productRepository.save(new Product(
                "Vintage Emerald Cut Diamond Tennis Bracelet",
                "Stunning continuous row of bezel-set brilliant diamonds totaling 4.5 Carats in 18K White Gold safety-clasp bracelet.",
                new BigDecimal("165000.00"),
                "https://images.unsplash.com/photo-1599643477877-530eb83abc8e?auto=format&fit=crop&w=800&q=80",
                bangles,
                3,
                true,
                false,
                "18K White Gold / 4.5ct Diamonds",
                "16.80 g"
        ));

        productRepository.save(new Product(
                "Lord Ganesha 24K Gold Coin (10 Grams - 999 Purity)",
                "Tamper-proof blister packed 24 Karat 999 pure gold coin with divine Lord Ganesha embossing. Perfect for gifting and investment.",
                new BigDecimal("76500.00"),
                "https://images.unsplash.com/photo-1610375461246-83df859d849d?auto=format&fit=crop&w=800&q=80",
                pendants,
                25,
                true,
                true,
                "24K Pure Gold (999 Purity)",
                "10.00 g"
        ));

        productRepository.save(new Product(
                "Antique Temple Work Lakshmi Kasu Mala Necklace",
                "Traditional South Indian Goddess Lakshmi coin necklace in 22K yellow gold with matte antique polish and ruby cabochons.",
                new BigDecimal("112000.00"),
                "https://images.unsplash.com/photo-1601121141461-9d6647bca1ed?auto=format&fit=crop&w=800&q=80",
                necklaces,
                4,
                true,
                false,
                "22K Antique Yellow Gold",
                "34.10 g"
        ));

        // Create an initial sample order to showcase admin metrics and notification immediately
        User demoCustomer = userRepository.findByEmail("customer@gmail.com").orElse(null);
        Product sampleProduct = productRepository.findAll().stream().findFirst().orElse(null);
        if (demoCustomer != null && sampleProduct != null && orderRepository.count() == 0) {
            String orderNum = "RJ-260905-1025";
            BigDecimal totalAmt = sampleProduct.getPrice();

            Order sampleOrder = new Order(
                    orderNum,
                    demoCustomer,
                    totalAmt,
                    OrderStatus.PLACED,
                    demoCustomer.getFullName(),
                    demoCustomer.getPhone(),
                    "Flat 4B, Emerald Heights, MG Road, Near Trinity Circle Metro Station, Bengaluru, Karnataka - 560001",
                    "Cash on Delivery",
                    "Please call before delivery"
            );
            sampleOrder = orderRepository.save(sampleOrder);

            OrderItem item = new OrderItem(
                    sampleOrder,
                    sampleProduct,
                    sampleProduct.getName(),
                    sampleProduct.getPrice(),
                    1,
                    totalAmt,
                    sampleProduct.getImageUrl()
            );
            orderItemRepository.save(item);
            sampleOrder.setItems(List.of(item));

            notificationRepository.save(new Notification(
                    "New Order " + orderNum + " Received",
                    "New order #" + orderNum + " placed by " + demoCustomer.getFullName() + ". Total: ₹" + totalAmt,
                    sampleOrder.getId(),
                    orderNum,
                    totalAmt,
                    demoCustomer.getFullName()
            ));
        }
    }
}
