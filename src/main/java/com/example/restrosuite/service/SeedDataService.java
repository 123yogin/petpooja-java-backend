package com.example.restrosuite.service;

import com.example.restrosuite.entity.*;
import com.example.restrosuite.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeedDataService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SeedDataService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OutletRepository outletRepository;

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private MenuModifierRepository menuModifierRepository;

    @Autowired
    private MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        if (shouldSeedData()) {
            logger.info("");
            logger.info("========================================");
            logger.info("🌱 SEEDING DATABASE WITH INITIAL DATA");
            logger.info("========================================");
            seedUsers();
            seedMenuItems();
            seedTables();
            seedIngredients();
            seedSuppliers();
            seedTasks();
            seedEmployees();
            seedOutlets();
            seedModifiers();
            logger.info("✅ Seed data initialization complete!");
            logger.info("========================================");
            logger.info("");
        } else {
            logger.info("ℹ️  Database already contains data. Skipping main seed data initialization.");
            // Still seed suppliers, ingredients, tasks, employees, outlets, and modifiers if they don't exist
            seedSuppliers();
            seedIngredients();
            seedTasks();
            seedEmployees();
            seedOutlets();
            seedModifiers();
        }
    }

    private boolean shouldSeedData() {
        // Only seed if database is empty
        return userRepository.count() == 0 && menuItemRepository.count() == 0;
    }

    @Transactional
    public void seedAll() {
        logger.info("");
        logger.info("========================================");
        logger.info("🌱 MANUALLY SEEDING DATABASE");
        logger.info("========================================");
        seedUsers();
        seedMenuItems();
        seedTables();
            seedIngredients();
            seedSuppliers();
            seedTasks();
            seedEmployees();
            seedOutlets();
            seedModifiers();
        logger.info("✅ Seed data initialization complete!");
        logger.info("========================================");
        logger.info("");
    }

    private void seedUsers() {
        logger.info("👤 Seeding users...");
        
        List<String> emails = List.of(
            "admin@restaurant.com",
            "manager@restaurant.com",
            "cashier@restaurant.com",
            "kitchen@restaurant.com"
        );
        
        int created = 0;
        for (String email : emails) {
            if (userRepository.findByEmail(email).isEmpty()) {
                User user = null;
                if (email.equals("admin@restaurant.com")) {
                    user = User.builder()
                        .email("admin@restaurant.com")
                        .username("Admin")
                        .password(passwordEncoder.encode("admin123"))
                        .role("ADMIN")
                        .build();
                } else if (email.equals("manager@restaurant.com")) {
                    user = User.builder()
                        .email("manager@restaurant.com")
                        .username("Manager")
                        .password(passwordEncoder.encode("manager123"))
                        .role("MANAGER")
                        .build();
                } else if (email.equals("cashier@restaurant.com")) {
                    user = User.builder()
                        .email("cashier@restaurant.com")
                        .username("Cashier")
                        .password(passwordEncoder.encode("cashier123"))
                        .role("CASHIER")
                        .build();
                } else if (email.equals("kitchen@restaurant.com")) {
                    user = User.builder()
                        .email("kitchen@restaurant.com")
                        .username("Kitchen Staff")
                        .password(passwordEncoder.encode("kitchen123"))
                        .role("KITCHEN")
                        .build();
                }
                
                if (user != null) {
                    userRepository.save(user);
                    created++;
                }
            }
        }
        
        if (created > 0) {
            logger.info("   ✅ Created {} new users", created);
        } else {
            logger.info("   ⏭️  All seed users already exist");
        }
    }

    private void seedMenuItems() {
        logger.info("🍽️  Seeding menu items...");
        
        // Check if seed menu items already exist
        boolean hasSeedItems = menuItemRepository.findAll().stream()
            .anyMatch(item -> item.getName().equals("Chicken Biryani"));
        
        if (!hasSeedItems) {
            List<MenuItem> menuItems = List.of(
                // Main Course
                MenuItem.builder().name("Paneer Butter Masala").category("Main Course").price(250.0).available(true).build(),
                MenuItem.builder().name("Chicken Biryani").category("Main Course").price(300.0).available(true).build(),
                MenuItem.builder().name("Dal Makhani").category("Main Course").price(180.0).available(true).build(),
                MenuItem.builder().name("Butter Chicken").category("Main Course").price(280.0).available(true).build(),
                MenuItem.builder().name("Veg Biryani").category("Main Course").price(220.0).available(true).build(),
                MenuItem.builder().name("Mutton Curry").category("Main Course").price(350.0).available(true).build(),
                
                // Appetizers
                MenuItem.builder().name("Paneer Tikka").category("Appetizer").price(200.0).available(true).build(),
                MenuItem.builder().name("Chicken Wings").category("Appetizer").price(250.0).available(true).build(),
                MenuItem.builder().name("Spring Rolls").category("Appetizer").price(150.0).available(true).build(),
                MenuItem.builder().name("Samosa").category("Appetizer").price(50.0).available(true).build(),
                
                // Desserts
                MenuItem.builder().name("Gulab Jamun").category("Dessert").price(80.0).available(true).build(),
                MenuItem.builder().name("Ice Cream").category("Dessert").price(100.0).available(true).build(),
                MenuItem.builder().name("Kheer").category("Dessert").price(120.0).available(true).build(),
                MenuItem.builder().name("Rasgulla").category("Dessert").price(90.0).available(true).build(),
                
                // Beverages
                MenuItem.builder().name("Coca Cola").category("Beverage").price(50.0).available(true).build(),
                MenuItem.builder().name("Fresh Lime Soda").category("Beverage").price(60.0).available(true).build(),
                MenuItem.builder().name("Mango Lassi").category("Beverage").price(80.0).available(true).build(),
                MenuItem.builder().name("Masala Chai").category("Beverage").price(40.0).available(true).build(),
                
                // Breads
                MenuItem.builder().name("Naan").category("Bread").price(30.0).available(true).build(),
                MenuItem.builder().name("Roti").category("Bread").price(20.0).available(true).build(),
                MenuItem.builder().name("Garlic Naan").category("Bread").price(50.0).available(true).build(),
                MenuItem.builder().name("Butter Naan").category("Bread").price(40.0).available(true).build()
            );
            
            menuItemRepository.saveAll(menuItems);
            logger.info("   ✅ Created {} menu items", menuItems.size());
        } else {
            logger.info("   ⏭️  Menu items already exist");
        }
    }

    private void seedTables() {
        logger.info("🪑 Seeding tables...");
        
        // Check if seed tables already exist
        boolean hasSeedTables = tableRepository.findAll().stream()
            .anyMatch(table -> table.getTableNumber().equals("T10"));
        
        if (!hasSeedTables) {
            List<TableEntity> tables = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                tables.add(TableEntity.builder()
                    .tableNumber("T" + i)
                    .occupied(false)
                    .build());
            }
            
            tableRepository.saveAll(tables);
            logger.info("   ✅ Created {} tables", tables.size());
        } else {
            logger.info("   ⏭️  Tables already exist");
        }
    }

    private void seedIngredients() {
        logger.info("🥘 Seeding ingredients...");
        
        // Always seed ingredients (will skip if already exist)
        if (ingredientRepository.count() == 0) {
            List<Ingredient> ingredients = List.of(
                Ingredient.builder().name("Paneer").quantity(5000.0).unit("grams").threshold(500.0).build(),
                Ingredient.builder().name("Chicken").quantity(3000.0).unit("grams").threshold(500.0).build(),
                Ingredient.builder().name("Rice").quantity(10000.0).unit("grams").threshold(1000.0).build(),
                Ingredient.builder().name("Tomatoes").quantity(2000.0).unit("grams").threshold(200.0).build(),
                Ingredient.builder().name("Onions").quantity(3000.0).unit("grams").threshold(300.0).build(),
                Ingredient.builder().name("Spices").quantity(500.0).unit("grams").threshold(50.0).build(),
                Ingredient.builder().name("Milk").quantity(5000.0).unit("ml").threshold(500.0).build(),
                Ingredient.builder().name("Flour").quantity(5000.0).unit("grams").threshold(500.0).build()
            );
            
            ingredientRepository.saveAll(ingredients);
            logger.info("   ✅ Created {} ingredients", ingredients.size());
        } else {
            logger.info("   ⏭️  Ingredients already exist");
        }
    }

    private void seedSuppliers() {
        logger.info("🏢 Seeding suppliers...");
        
        // Always seed suppliers if none exist (similar to ingredients)
        if (supplierRepository.count() == 0) {
            List<Supplier> suppliers = List.of(
                Supplier.builder()
                    .name("Fresh Foods Ltd.")
                    .contact("+91 98765 43210")
                    .email("contact@freshfoods.com")
                    .build(),
                Supplier.builder()
                    .name("Spice World Distributors")
                    .contact("+91 98765 43211")
                    .email("orders@spiceworld.com")
                    .build(),
                Supplier.builder()
                    .name("Dairy Products Co.")
                    .contact("+91 98765 43212")
                    .email("sales@dairyproducts.com")
                    .build(),
                Supplier.builder()
                    .name("Grain Suppliers India")
                    .contact("+91 98765 43213")
                    .email("info@grainsuppliers.com")
                    .build(),
                Supplier.builder()
                    .name("Vegetable Market Wholesale")
                    .contact("+91 98765 43214")
                    .email("wholesale@vegmarket.com")
                    .build()
            );
            
            supplierRepository.saveAll(suppliers);
            logger.info("   ✅ Created {} suppliers", suppliers.size());
        } else {
            logger.info("   ⏭️  Suppliers already exist");
        }
    }

    private void seedTasks() {
        logger.info("📋 Seeding tasks...");
        
        // Always seed tasks if none exist
        if (taskRepository.count() == 0) {
            // Get users for task assignment
            List<User> allUsers = userRepository.findAll();
            User admin = allUsers.stream()
                .filter(u -> "ADMIN".equals(u.getRole()))
                .findFirst()
                .orElse(null);
            User manager = allUsers.stream()
                .filter(u -> "MANAGER".equals(u.getRole()))
                .findFirst()
                .orElse(null);
            User cashier = allUsers.stream()
                .filter(u -> "CASHIER".equals(u.getRole()))
                .findFirst()
                .orElse(null);
            
            List<Task> tasks = new ArrayList<>();
            
            // Create sample tasks
            if (cashier != null) {
                tasks.add(Task.builder()
                    .title("Daily Cash Reconciliation")
                    .description("Reconcile cash register at end of day and prepare daily sales report")
                    .assignedTo(cashier)
                    .createdBy(admin)
                    .status("PENDING")
                    .priority("HIGH")
                    .dueDate(LocalDateTime.now().plusDays(1).withHour(22).withMinute(0))
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build());
            }
            
            if (manager != null) {
                tasks.add(Task.builder()
                    .title("Weekly Inventory Check")
                    .description("Review inventory levels and place orders for low stock items")
                    .assignedTo(manager)
                    .createdBy(admin)
                    .status("IN_PROGRESS")
                    .priority("MEDIUM")
                    .dueDate(LocalDateTime.now().plusDays(3))
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build());
            }
            
            tasks.add(Task.builder()
                .title("Clean Kitchen Equipment")
                .description("Deep clean all kitchen equipment and ensure hygiene standards")
                .assignedTo(null) // Can be assigned later
                .createdBy(admin)
                .status("PENDING")
                .priority("MEDIUM")
                .dueDate(LocalDateTime.now().plusDays(2))
                .createdAt(LocalDateTime.now().minusHours(5))
                .notes("Schedule during off-peak hours")
                .build());
            
            tasks.add(Task.builder()
                .title("Update Menu Board")
                .description("Update the physical menu board with new items and prices")
                .assignedTo(null)
                .createdBy(admin)
                .status("PENDING")
                .priority("LOW")
                .dueDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());
            
            if (cashier != null) {
                tasks.add(Task.builder()
                    .title("Customer Feedback Review")
                    .description("Review and respond to customer feedback from this week")
                    .assignedTo(cashier)
                    .createdBy(manager)
                    .status("COMPLETED")
                    .priority("MEDIUM")
                    .dueDate(LocalDateTime.now().minusDays(1))
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .completedAt(LocalDateTime.now().minusDays(1).withHour(18))
                    .notes("All feedback addressed and responses sent")
                    .build());
            }
            
            if (!tasks.isEmpty()) {
                taskRepository.saveAll(tasks);
                logger.info("   ✅ Created {} tasks", tasks.size());
            } else {
                logger.info("   ⏭️  No users available to assign tasks");
            }
        } else {
            logger.info("   ⏭️  Tasks already exist");
        }
    }

    private void seedEmployees() {
        logger.info("👔 Seeding employees...");
        
        // Always seed employees if none exist
        if (employeeRepository.count() == 0) {
            // Get users to link with employees
            List<User> allUsers = userRepository.findAll();
            User admin = allUsers.stream()
                .filter(u -> "ADMIN".equals(u.getRole()))
                .findFirst()
                .orElse(null);
            User manager = allUsers.stream()
                .filter(u -> "MANAGER".equals(u.getRole()))
                .findFirst()
                .orElse(null);
            User cashier = allUsers.stream()
                .filter(u -> "CASHIER".equals(u.getRole()))
                .findFirst()
                .orElse(null);
            User kitchen = allUsers.stream()
                .filter(u -> "KITCHEN".equals(u.getRole()))
                .findFirst()
                .orElse(null);
            
            List<Employee> employees = new ArrayList<>();
            
            // Create sample employees
            employees.add(Employee.builder()
                .name("Rajesh Kumar")
                .email("rajesh@restaurant.com")
                .phone("+91 98765 43210")
                .employeeId("EMP001")
                .department("KITCHEN")
                .designation("Head Chef")
                .shift("MORNING")
                .basicSalary(45000.0)
                .allowances(5000.0)
                .deductions(2000.0)
                .joinDate(LocalDate.now().minusYears(2))
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .gender("MALE")
                .address("123 Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .emergencyContact("Priya Kumar")
                .emergencyPhone("+91 98765 43211")
                .isActive(true)
                .user(kitchen)
                .build());
            
            employees.add(Employee.builder()
                .name("Priya Sharma")
                .email("priya@restaurant.com")
                .phone("+91 98765 43220")
                .employeeId("EMP002")
                .department("SERVICE")
                .designation("Senior Waiter")
                .shift("EVENING")
                .basicSalary(25000.0)
                .allowances(3000.0)
                .deductions(1000.0)
                .joinDate(LocalDate.now().minusMonths(18))
                .dateOfBirth(LocalDate.of(1992, 8, 22))
                .gender("FEMALE")
                .address("456 Park Avenue")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400002")
                .emergencyContact("Rahul Sharma")
                .emergencyPhone("+91 98765 43221")
                .isActive(true)
                .user(cashier)
                .build());
            
            employees.add(Employee.builder()
                .name("Amit Patel")
                .email("amit@restaurant.com")
                .phone("+91 98765 43230")
                .employeeId("EMP003")
                .department("KITCHEN")
                .designation("Sous Chef")
                .shift("NIGHT")
                .basicSalary(35000.0)
                .allowances(4000.0)
                .deductions(1500.0)
                .joinDate(LocalDate.now().minusMonths(12))
                .dateOfBirth(LocalDate.of(1990, 3, 10))
                .gender("MALE")
                .address("789 Oak Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400003")
                .emergencyContact("Sneha Patel")
                .emergencyPhone("+91 98765 43231")
                .isActive(true)
                .build());
            
            employees.add(Employee.builder()
                .name("Sneha Reddy")
                .email("sneha@restaurant.com")
                .phone("+91 98765 43240")
                .employeeId("EMP004")
                .department("SERVICE")
                .designation("Waiter")
                .shift("MORNING")
                .basicSalary(20000.0)
                .allowances(2000.0)
                .deductions(800.0)
                .joinDate(LocalDate.now().minusMonths(6))
                .dateOfBirth(LocalDate.of(1995, 11, 5))
                .gender("FEMALE")
                .address("321 Elm Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400004")
                .emergencyContact("Vikram Reddy")
                .emergencyPhone("+91 98765 43241")
                .isActive(true)
                .build());
            
            employees.add(Employee.builder()
                .name("Vikram Singh")
                .email("vikram@restaurant.com")
                .phone("+91 98765 43250")
                .employeeId("EMP005")
                .department("MANAGEMENT")
                .designation("Assistant Manager")
                .shift("GENERAL")
                .basicSalary(40000.0)
                .allowances(6000.0)
                .deductions(2500.0)
                .joinDate(LocalDate.now().minusMonths(9))
                .dateOfBirth(LocalDate.of(1988, 7, 20))
                .gender("MALE")
                .address("654 Pine Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400005")
                .emergencyContact("Anita Singh")
                .emergencyPhone("+91 98765 43251")
                .isActive(true)
                .user(manager)
                .build());
            
            employeeRepository.saveAll(employees);
            logger.info("   ✅ Created {} employees", employees.size());
        } else {
            logger.info("   ⏭️  Employees already exist");
        }
    }

    private void seedOutlets() {
        logger.info("🏪 Seeding outlets...");
        
        // Always seed outlets if none exist
        if (outletRepository.count() == 0) {
            List<Outlet> outlets = List.of(
                Outlet.builder()
                    .name("Main Restaurant")
                    .code("OUTLET001")
                    .address("123 Main Street")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pincode("400001")
                    .phone("+91 98765 43210")
                    .email("main@restaurant.com")
                    .gstin("29ABCDE1234F1Z5")
                    .managerName("Vikram Singh")
                    .managerPhone("+91 98765 43250")
                    .isActive(true)
                    .build(),
                Outlet.builder()
                    .name("Branch Restaurant")
                    .code("OUTLET002")
                    .address("456 Park Avenue")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pincode("400002")
                    .phone("+91 98765 43220")
                    .email("branch@restaurant.com")
                    .gstin("29ABCDE1234F2Z6")
                    .managerName("Priya Sharma")
                    .managerPhone("+91 98765 43221")
                    .isActive(true)
                    .build()
            );
            
            outletRepository.saveAll(outlets);
            logger.info("   ✅ Created {} outlets", outlets.size());
        } else {
            logger.info("   ⏭️  Outlets already exist");
        }
    }

    private void seedModifiers() {
        logger.info("🔧 Seeding modifiers...");
        
        // Always seed modifiers if none exist
        if (modifierGroupRepository.count() == 0) {
            // Create modifier groups
            ModifierGroup sizeGroup = ModifierGroup.builder()
                .name("Size")
                .description("Select size for your item")
                .isRequired(true)
                .allowMultiple(false)
                .minSelection(1)
                .maxSelection(1)
                .isActive(true)
                .build();
            sizeGroup = modifierGroupRepository.save(sizeGroup);

            ModifierGroup spiceLevelGroup = ModifierGroup.builder()
                .name("Spice Level")
                .description("Choose your preferred spice level")
                .isRequired(false)
                .allowMultiple(false)
                .minSelection(0)
                .maxSelection(1)
                .isActive(true)
                .build();
            spiceLevelGroup = modifierGroupRepository.save(spiceLevelGroup);

            ModifierGroup toppingsGroup = ModifierGroup.builder()
                .name("Toppings")
                .description("Add extra toppings")
                .isRequired(false)
                .allowMultiple(true)
                .minSelection(0)
                .maxSelection(null)
                .isActive(true)
                .build();
            toppingsGroup = modifierGroupRepository.save(toppingsGroup);

            ModifierGroup breadTypeGroup = ModifierGroup.builder()
                .name("Bread Type")
                .description("Select bread type")
                .isRequired(false)
                .allowMultiple(false)
                .minSelection(0)
                .maxSelection(1)
                .isActive(true)
                .build();
            breadTypeGroup = modifierGroupRepository.save(breadTypeGroup);

            // Create modifiers for Size group
            List<MenuModifier> sizeModifiers = List.of(
                MenuModifier.builder()
                    .modifierGroup(sizeGroup)
                    .name("Small")
                    .description("Regular size")
                    .price(0.0)
                    .isActive(true)
                    .displayOrder(1)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(sizeGroup)
                    .name("Medium")
                    .description("Medium size")
                    .price(30.0)
                    .isActive(true)
                    .displayOrder(2)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(sizeGroup)
                    .name("Large")
                    .description("Large size")
                    .price(60.0)
                    .isActive(true)
                    .displayOrder(3)
                    .build()
            );
            menuModifierRepository.saveAll(sizeModifiers);

            // Create modifiers for Spice Level group
            List<MenuModifier> spiceModifiers = List.of(
                MenuModifier.builder()
                    .modifierGroup(spiceLevelGroup)
                    .name("Mild")
                    .description("Mild spice")
                    .price(0.0)
                    .isActive(true)
                    .displayOrder(1)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(spiceLevelGroup)
                    .name("Medium")
                    .description("Medium spice")
                    .price(0.0)
                    .isActive(true)
                    .displayOrder(2)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(spiceLevelGroup)
                    .name("Spicy")
                    .description("Spicy")
                    .price(0.0)
                    .isActive(true)
                    .displayOrder(3)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(spiceLevelGroup)
                    .name("Extra Spicy")
                    .description("Very spicy")
                    .price(0.0)
                    .isActive(true)
                    .displayOrder(4)
                    .build()
            );
            menuModifierRepository.saveAll(spiceModifiers);

            // Create modifiers for Toppings group
            List<MenuModifier> toppingModifiers = List.of(
                MenuModifier.builder()
                    .modifierGroup(toppingsGroup)
                    .name("Extra Cheese")
                    .description("Add extra cheese")
                    .price(30.0)
                    .isActive(true)
                    .displayOrder(1)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(toppingsGroup)
                    .name("Extra Onions")
                    .description("Add extra onions")
                    .price(10.0)
                    .isActive(true)
                    .displayOrder(2)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(toppingsGroup)
                    .name("Extra Tomatoes")
                    .description("Add extra tomatoes")
                    .price(10.0)
                    .isActive(true)
                    .displayOrder(3)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(toppingsGroup)
                    .name("Extra Butter")
                    .description("Add extra butter")
                    .price(20.0)
                    .isActive(true)
                    .displayOrder(4)
                    .build()
            );
            menuModifierRepository.saveAll(toppingModifiers);

            // Create modifiers for Bread Type group
            List<MenuModifier> breadModifiers = List.of(
                MenuModifier.builder()
                    .modifierGroup(breadTypeGroup)
                    .name("Plain Naan")
                    .description("Regular naan")
                    .price(0.0)
                    .isActive(true)
                    .displayOrder(1)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(breadTypeGroup)
                    .name("Butter Naan")
                    .description("Butter naan")
                    .price(10.0)
                    .isActive(true)
                    .displayOrder(2)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(breadTypeGroup)
                    .name("Garlic Naan")
                    .description("Garlic naan")
                    .price(20.0)
                    .isActive(true)
                    .displayOrder(3)
                    .build(),
                MenuModifier.builder()
                    .modifierGroup(breadTypeGroup)
                    .name("Roti")
                    .description("Plain roti")
                    .price(0.0)
                    .isActive(true)
                    .displayOrder(4)
                    .build()
            );
            menuModifierRepository.saveAll(breadModifiers);

            // Link modifier groups to some menu items
            List<MenuItem> allMenuItems = menuItemRepository.findAll();
            
            // Link Size and Spice Level to main course items
            for (MenuItem item : allMenuItems) {
                if ("Main Course".equals(item.getCategory())) {
                    // Link Size group
                    MenuItemModifierGroup link1 = MenuItemModifierGroup.builder()
                        .menuItem(item)
                        .modifierGroup(sizeGroup)
                        .displayOrder(1)
                        .build();
                    menuItemModifierGroupRepository.save(link1);
                    
                    // Link Spice Level group
                    MenuItemModifierGroup link2 = MenuItemModifierGroup.builder()
                        .menuItem(item)
                        .modifierGroup(spiceLevelGroup)
                        .displayOrder(2)
                        .build();
                    menuItemModifierGroupRepository.save(link2);
                    
                    // Link Toppings group
                    MenuItemModifierGroup link3 = MenuItemModifierGroup.builder()
                        .menuItem(item)
                        .modifierGroup(toppingsGroup)
                        .displayOrder(3)
                        .build();
                    menuItemModifierGroupRepository.save(link3);
                }
            }
            
            // Link Bread Type to bread items
            for (MenuItem item : allMenuItems) {
                if ("Bread".equals(item.getCategory())) {
                    MenuItemModifierGroup link = MenuItemModifierGroup.builder()
                        .menuItem(item)
                        .modifierGroup(breadTypeGroup)
                        .displayOrder(1)
                        .build();
                    menuItemModifierGroupRepository.save(link);
                }
            }

            logger.info("   ✅ Created modifier groups and modifiers");
        } else {
            logger.info("   ⏭️  Modifiers already exist");
        }
    }
}

