import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Order Status and Order Type Enums
enum OrderStatus {
    RECEIVED, PREPARING, BAKING, OUT_FOR_DELIVERY, DELIVERED
}

enum OrderType {
    PICKUP, DELIVERY
}

// User Class
class User {
    Long id;
    String name;
    String contactNumber;
    String address;
    int loyaltyPoints;
    List<Pizza> favoritePizzas = new ArrayList<>();

    User(Long id, String name, String contactNumber) {
        this.id = id;
        this.name = name;
        this.contactNumber = contactNumber;
        this.address = "Not Set";
        this.loyaltyPoints = 0;
    }

    void updateAddress(String address) {
        this.address = address;
    }

    void addLoyaltyPoints(int points) {
        this.loyaltyPoints += points;
    }

    void addToFavorites(Pizza pizza) {
        favoritePizzas.add(pizza);
    }

    void removeFromFavorites(Pizza pizza) {
        favoritePizzas.remove(pizza);
    }

    @Override
    public String toString() {
        return String.format("👤 User ID: %d | Name: %s | Contact: %s | Address: %s | Loyalty Points: %d",
                id, name, contactNumber, address, loyaltyPoints);
    }
}

// Pizza Class
class Pizza {
    String name;
    String crust;
    String sauce;
    String cheese;
    List<String> toppings;
    double basePrice;
    double rating = 0.0;
    int ratingCount = 0;

    Pizza(String name, String crust, String sauce, String cheese, List<String> toppings, double basePrice) {
        this.name = name;
        this.crust = crust;
        this.sauce = sauce;
        this.cheese = cheese;
        this.toppings = toppings;
        this.basePrice = basePrice;
    }

    void updateRating(double newRating) {
        rating = (rating * ratingCount + newRating) / (++ratingCount);
    }

    @Override
    public String toString() {
        return String.format(
                "🍕 Pizza: %s | Crust: %s | Sauce: %s | Cheese: %s | Toppings: %s | Base Price: $%.2f | Rating: %.2f",
                name, crust, sauce, cheese, String.join(", ", toppings), basePrice, rating);
    }
}

// Order Class
class Order {
    Long id;
    User user;
    Pizza pizza;
    OrderType type;
    String deliveryAddress;
    OrderStatus status;
    Date createdAt;
    String feedback = "No feedback given";
    Double pizzaRating = 0.0;

    Order(Long id, User user, Pizza pizza, OrderType type, String deliveryAddress) {
        this.id = id;
        this.user = user;
        this.pizza = pizza;
        this.type = type;
        this.deliveryAddress = deliveryAddress;
        this.status = OrderStatus.RECEIVED;
        this.createdAt = new Date();
    }

    void updateStatus() {
        switch (status) {
            case RECEIVED -> status = OrderStatus.PREPARING;
            case PREPARING -> status = OrderStatus.BAKING;
            case BAKING -> status = OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> status = OrderStatus.DELIVERED;
        }
    }

    void addFeedback(String feedback) {
        this.feedback = feedback;
    }

    void addPizzaRating(double rating) {
        this.pizzaRating = rating;
        pizza.updateRating(rating);
    }

    @Override
    public String toString() {
        return String.format("📦 Order ID: %d | Pizza: %s | Type: %s | Status: %s | Address: %s | Feedback: %s | Rating: %.1f",
                id, pizza.name, type, status, deliveryAddress, feedback, pizzaRating);
    }
}

// Promotion Class
class Promotion {
    String description;
    double discountAmount;
    double minOrderAmount;

    Promotion(String description, double discountAmount, double minOrderAmount) {
        this.description = description;
        this.discountAmount = discountAmount;
        this.minOrderAmount = minOrderAmount;
    }

    double applyPromotion(double orderAmount) {
        if (orderAmount >= minOrderAmount) {
            System.out.printf("🎉 Promotion applied: %s. Discount: $%.2f\n", description, discountAmount);
            return orderAmount - discountAmount;
        }
        return orderAmount;
    }
}

// Payment Class
class Payment {
    static final double DISCOUNT_RATE = 0.05;

    // Payment class: processing payment
    static double processPayment(User user, double amount) {
        double discount = 0;
        if (user.loyaltyPoints > 0) {
            // Calculate 5% discount based on available loyalty points
            discount = amount * DISCOUNT_RATE; // DISCOUNT_RATE = 0.05 (5% discount)
            System.out.printf("💸 Applied %d%% discount based on your loyalty points!\n", (int) (DISCOUNT_RATE * 100));
        }
        double totalAmount = amount - discount;
        System.out.printf("💸 Total amount after discount: $%.2f\n", totalAmount);
        return totalAmount;
    }

    // Deduct loyalty points used for the discount
    static void processSuccessfulPayment(User user, double amount) {
        System.out.println("💳 Payment processed successfully!");

        // Calculate points to deduct based on the total amount (1 point per $10 spent)
        int pointsSpent = (int) (amount / 10); // For every $10 spent, deduct 1 point

        // Check if the user has enough points
        if (user.loyaltyPoints >= pointsSpent) {
            user.loyaltyPoints -= pointsSpent;
            System.out.printf("🎉 You spent %d loyalty points! Remaining points: %d\n", pointsSpent, user.loyaltyPoints);
        } else {
            System.out.printf("❌ Not enough loyalty points. You have %d points.\n", user.loyaltyPoints);
        }
    }
}

// Main Pizza Ordering System
class PizzaOrderingSystem {
    static List<User> users = new ArrayList<>();
    static List<Pizza> pizzas = new ArrayList<>();
    static List<Order> orders = new ArrayList<>();
    static List<Promotion> promotions = new ArrayList<>();
    static List<String> notificationBuffer = new ArrayList<>();
    static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static Scanner scanner = new Scanner(System.in);
    static User currentUser = null;

    public static void main(String[] args) {
        seedData();
        startRealTimeUpdates();
        signUpOrLoginMenu();
    }

    static void seedData() {
        pizzas.add(new Pizza("Margherita", "Thin", "Tomato", "Mozzarella", List.of("Basil"), 10.0));
        pizzas.add(new Pizza("Pepperoni", "Thick", "Barbecue", "Cheddar", List.of("Pepperoni"), 12.0));

        promotions.add(new Promotion("🎉 Seasonal Special: $2 off on orders above $20", 2.0, 20.0));
    }

    static void startRealTimeUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            for (Order order : orders) {
                if (order.status != OrderStatus.DELIVERED) {
                    order.updateStatus();
                    notificationBuffer.add("🔔 Order Update: " + order);
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    static void signUpOrLoginMenu() {
        System.out.println("👤 Welcome to the Pizza Ordering System");

        while (true) {
            System.out.println("\n📜 Menu:");
            System.out.println("1. Sign Up");
            System.out.println("2. Log In");
            System.out.print("💡 Enter your choice: ");
            int choice = validateMenuChoice();
            if (choice == 1) {
                signUp();
                break;
            } else if (choice == 2) {
                login();
                break;
            } else {
                System.out.println("❌ Invalid choice. Please enter a valid option.");
            }
        }

        mainMenu();
    }

    static void signUp() {
        System.out.println("\n🎉 Sign Up");

        String name = getNonEmptyInput("💡 Enter your name: ");
        String contactNumber = getValidContactNumber();

        // Check if the contact number already exists
        for (User user : users) {
            if (user.contactNumber.equals(contactNumber)) {
                System.out.println("❌ Contact number already exists. Please log in.");
                return;
            }
        }

        User newUser = new User((long) (users.size() + 1), name, contactNumber);
        users.add(newUser);
        currentUser = newUser;
        System.out.println("✅ Sign-Up successful! Welcome, " + newUser.name);
    }

    static void login() {
        System.out.println("\n🔑 Log In");

        String contactNumber = getNonEmptyInput("💡 Enter your contact number: ");
        boolean foundUser = false;

        for (User user : users) {
            if (user.contactNumber.equals(contactNumber)) {
                currentUser = user;
                System.out.println("✅ Login successful! Welcome back, " + user.name);
                foundUser = true;
                break;
            }
        }

        if (!foundUser) {
            System.out.println("❌ User not found. Please sign up.");
        }
    }

    static void mainMenu() {
        while (true) {
            System.out.println("\n📜 Menu:");
            System.out.println("1. Customize a Pizza");
            System.out.println("2. Place an Order");
            System.out.println("3. Update Delivery Address");
            System.out.println("4. View User Profile and Favorites");
            System.out.println("5. Add to Favorites");
            System.out.println("6. Remove from Favorites");
            System.out.println("7. View Notifications");
            System.out.println("8. View Promotions");
            System.out.println("9. Give Feedback and Rating");
            System.out.println("10. Exit");
            System.out.print("💡 Enter your choice: ");
            int choice = validateMenuChoice();
            switch (choice) {
                case 1 -> customizePizza();
                case 2 -> placeOrder();
                case 3 -> updateAddress();
                case 4 -> viewUserProfileAndFavorites();
                case 5 -> addToFavorites();
                case 6 -> removeFromFavorites();
                case 7 -> viewNotifications();
                case 8 -> viewPromotions();
                case 9 -> giveFeedbackAndRating();
                case 10 -> {
                    System.out.println("👋 Goodbye!");
                    scheduler.shutdown();
                    return;
                }
                default -> System.out.println("❌ Invalid choice. Please enter a valid option.");
            }
        }
    }

    static int validateMenuChoice() {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice < 1 || choice > 10) {
                    System.out.print("❌ Invalid choice. Please enter a valid option (1-10): ");
                } else {
                    return choice;
                }
            } catch (NumberFormatException e) {
                System.out.print("❌ Invalid input. Please enter a number: ");
            }
        }
    }

    static String getNonEmptyInput(String prompt) {
        String input;
        while (true) {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("❌ Input cannot be empty. Please try again.");
        }
    }

    static String getValidContactNumber() {
        while (true) {
            String contactNumber = getNonEmptyInput("💡 Enter your contact number (10 digits): ");
            if (isValidContactNumber(contactNumber)) {
                return contactNumber;
            } else {
                System.out.println("❌ Invalid contact number. Please enter a valid 10-digit number.");
            }
        }
    }

    static boolean isValidContactNumber(String contactNumber) {
        return contactNumber.matches("^[0-9]{10}$");
    }

    static void customizePizza() {
        System.out.println("\n🍕 Customize your Pizza");

        String pizzaName = getNonEmptyInput("💡 Enter a name for your pizza (default: Custom Pizza): ");
        if (pizzaName.isEmpty()) {
            pizzaName = "Custom Pizza"; // Default name if no input is provided
        }

        String crust = getCrustChoice();
        String sauce = getSauceChoice();
        String cheese = getCheeseChoice();
        List<String> toppings = getToppingsChoice();

        double price = 20.00;

        Pizza customPizza = new Pizza(pizzaName, crust, sauce, cheese, toppings, price);
        pizzas.add(customPizza);
        System.out.println("✅ Custom pizza created: " + customPizza);
    }

    static String getCrustChoice() {
        System.out.println("💡 Choose crust:");
        System.out.println("1. Thin");
        System.out.println("2. Thick");
        System.out.println("3. Stuffed");
        return getChoiceFromMenu("💡 Enter your choice (1-3): ", 3);
    }

    static String getSauceChoice() {
        System.out.println("💡 Choose sauce:");
        System.out.println("1. Tomato");
        System.out.println("2. Barbecue");
        System.out.println("3. Pesto");
        return getChoiceFromMenu("💡 Enter your choice (1-3): ", 3);
    }

    static String getCheeseChoice() {
        System.out.println("💡 Choose cheese:");
        System.out.println("1. Mozzarella");
        System.out.println("2. Cheddar");
        System.out.println("3. Vegan Cheese");
        return getChoiceFromMenu("💡 Enter your choice (1-3): ", 3);
    }

    static List<String> getToppingsChoice() {
        List<String> toppings = new ArrayList<>();
        System.out.println("💡 Choose toppings (enter numbers separated by commas):");
        System.out.println("1. Pepperoni");
        System.out.println("2. Mushrooms");
        System.out.println("3. Olives");
        System.out.println("4. Basil");
        System.out.print("💡 Enter your choices: ");
        String[] choices = scanner.nextLine().split(",");
        for (String choice : choices) {
            try {
                int toppingChoice = Integer.parseInt(choice.trim());
                if (toppingChoice >= 1 && toppingChoice <= 4) {
                    toppings.add(getToppingName(toppingChoice));
                } else {
                    System.out.println("❌ Invalid topping number. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Please enter numbers separated by commas.");
            }
        }
        return toppings;
    }

    static String getToppingName(int choice) {
        switch (choice) {
            case 1: return "Pepperoni";
            case 2: return "Mushrooms";
            case 3: return "Olives";
            case 4: return "Basil";
            default: return "";
        }
    }

    static String getChoiceFromMenu(String prompt, int maxChoice) {
        int choice;
        while (true) {
            choice = validateNumericInput();
            if (choice >= 1 && choice <= maxChoice) {
                return getChoiceName(choice);
            }
            System.out.print("❌ Invalid choice. Please enter a valid option: ");
        }
    }

    static String getChoiceName(int choice) {
        switch (choice) {
            case 1: return "Thin";
            case 2: return "Thick";
            case 3: return "Stuffed";
            case 4: return "Tomato";
            case 5: return "Barbecue";
            case 6: return "Pesto";
            case 7: return "Mozzarella";
            case 8: return "Cheddar";
            case 9: return "Vegan Cheese";
            default: return "";
        }
    }

    static void placeOrder() {
        System.out.println("\n🍕 Place an Order");

        // Show available pizzas
        viewPizzas();

        // User selects a pizza
        int pizzaChoice = getValidPizzaChoice();
        Pizza pizza = pizzas.get(pizzaChoice);

        // Choose delivery or pickup
        String deliveryAddress = "";
        if (getDeliveryChoice()) {
            if (currentUser.address.equals("Not Set")) {
                System.out.println("❌ Address not set. Please update your address first.");
                return;
            }
            deliveryAddress = currentUser.address;
        }

        // Create the order
        Order order = new Order((long) (orders.size() + 1), currentUser, pizza, OrderType.DELIVERY, deliveryAddress);
        orders.add(order);

        // Add loyalty points based on pizza price (for example, $20 pizza gives 20 points)
        currentUser.addLoyaltyPoints((int) pizza.basePrice); // Adds loyalty points equal to the pizza price.

        double orderAmount = pizza.basePrice;
        orderAmount = applySeasonalSpecial(orderAmount); // Apply seasonal promotions

        // Ask for payment method
        String paymentMethod = getPaymentMethod();

        // If invalid payment method (null or incorrect), re-prompt
        if (paymentMethod == null) {
            System.out.println("❌ Invalid payment method. Please select a valid option.");
            return;
        }

        // Process payment based on method
        if (paymentMethod.equals("Credit Card") || paymentMethod.equals("Debit Card")) {
            System.out.println("🔐 Please provide card details for " + paymentMethod);

            // Capture and validate card details
            String cardNumber = getCardNumber();
            int expiryMonth = getCardExpiryMonth();
            int expiryYear = getCardExpiryYear();

            if (!validateCardDetails(cardNumber, expiryMonth, expiryYear)) {
                System.out.println("❌ Invalid card details. Please try again.");
                return;
            }
        }

        // Calculate total amount after any discounts
        double totalAmount = Payment.processPayment(currentUser, orderAmount);
        Payment.processSuccessfulPayment(currentUser, totalAmount);

        // Show order confirmation
        System.out.println("✅ Order placed successfully: " + order);
    }

    // Method to get the card number
    static String getCardNumber() {
        System.out.print("💳 Enter your card number (16 digits): ");
        String cardNumber = scanner.nextLine().trim();
        while (!cardNumber.matches("\\d{16}")) {
            System.out.print("❌ Invalid card number. It must be 16 digits. Please try again: ");
            cardNumber = scanner.nextLine().trim();
        }
        return cardNumber;
    }

    // Method to get the card expiry month
    static int getCardExpiryMonth() {
        System.out.print("💳 Enter the expiration month (1-12): ");
        int month = validateNumericInput();
        while (month < 1 || month > 12) {
            System.out.print("❌ Invalid expiration month. It must be between 1 and 12. Please try again: ");
            month = validateNumericInput();
        }
        return month;
    }

    // Method to get the card expiry year
    static int getCardExpiryYear() {
        System.out.print("💳 Enter the expiration year (e.g., 2024): ");
        int year = validateNumericInput();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        while (year < currentYear || (year == currentYear && getCardExpiryMonth() < Calendar.getInstance().get(Calendar.MONTH) + 1)) {
            System.out.println("❌ Invalid expiration year or month. It cannot be in the past.");
            System.out.print("💳 Enter the expiration year (e.g., 2024): ");
            year = validateNumericInput();
        }
        return year;
    }

    static boolean validateCardDetails(String cardNumber, int month, int year) {
        // Ensure the card number is 16 digits long and contains only numbers
        if (cardNumber.length() != 16 || !cardNumber.matches("[0-9]+")) {
            System.out.println("❌ Invalid card number. It must be 16 digits and contain only numbers.");
            return false;
        }

        // Ensure the expiration month is between 1 and 12
        if (month < 1 || month > 12) {
            System.out.println("❌ Invalid expiration month. It must be between 1 and 12.");
            return false;
        }

        // Ensure the expiration year is not in the past
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        if (year < currentYear) {
            System.out.println("❌ Invalid expiration year. It cannot be in the past.");
            return false;
        }

        // If the expiration year is the current year, ensure the month is not in the past
        if (year == currentYear && month < Calendar.getInstance().get(Calendar.MONTH) + 1) {
            System.out.println("❌ Invalid expiration date. The month has already passed.");
            return false;
        }

        return true;
    }

    static double applySeasonalSpecial(double orderAmount) {
        if (!promotions.isEmpty()) {
            Promotion seasonalSpecial = promotions.get(0);
            return seasonalSpecial.applyPromotion(orderAmount);
        }
        return orderAmount;
    }

    static void viewPizzas() {
        System.out.println("\n🍕 Available Pizzas:");
        for (int i = 0; i < pizzas.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, pizzas.get(i));
        }
    }

    static int getValidPizzaChoice() {
        while (true) {
            System.out.print("💡 Enter the number of the pizza you want: ");
            int choice = validateNumericInput() - 1; // Adjusting for zero-indexed list
            if (choice >= 0 && choice < pizzas.size()) {
                return choice;
            }
            System.out.println("❌ Invalid choice. Please select a valid pizza.");
        }
    }

    static boolean getDeliveryChoice() {
        System.out.print("💡 Delivery or Pickup? (1 for Delivery, 2 for Pickup): ");
        int choice = validateNumericInput();
        return choice == 1;
    }

    static String getPaymentMethod() {
        System.out.println("\n💳 Choose your payment method:");
        System.out.println("1. Credit Card");
        System.out.println("2. Debit Card");
        System.out.println("3. Cash");
        System.out.print("💡 Enter your choice (1-3): ");

        // Validate numeric input
        int choice = validateNumericInput();

        switch (choice) {
            case 1:
                return "Credit Card";
            case 2:
                return "Debit Card";
            case 3:
                return "Cash";
            default:
                return null;  // If invalid choice, return null
        }
    }

    static void giveFeedbackAndRating() {
        System.out.println("🌟 Provide Feedback and Rating");

        if (orders.isEmpty()) {
            System.out.println("❌ You don't have any orders to give feedback for.");
            return;
        }

        System.out.println("💬 Select an order to give feedback:");
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            if (order.status == OrderStatus.DELIVERED) {
                System.out.println((i + 1) + ". " + order.pizza.name);
            }
        }

        System.out.print("💡 Enter the number of the order to give feedback: ");
        int orderChoice = validateNumericInput() - 1;

        if (orderChoice >= 0 && orderChoice < orders.size()) {
            Order order = orders.get(orderChoice);
            System.out.print("💬 Enter your feedback: ");
            String feedback = scanner.nextLine();
            order.addFeedback(feedback);

            System.out.print("⭐ Rate the pizza (1 to 5): ");
            int rating = validateNumericInput();
            order.addPizzaRating(rating);

            System.out.println("✅ Thank you for your feedback and rating!");
        }
    }

    static void updateAddress() {
        System.out.println("\n📍 Update Delivery Address");
        System.out.println("💡 Choose your area:");
        List<String> colomboAreas = List.of(
                "Colombo 1 - Fort", "Colombo 2 - Slave Island", "Colombo 3 - Kollupitiya", "Colombo 4 - Bambalapitiya",
                "Colombo 5 - Havelock Town", "Colombo 6 - Wellawatte", "Colombo 7 - Cinnamon Gardens", "Colombo 8 - Borella",
                "Colombo 9 - Dematagoda", "Colombo 10 - Maradana", "Colombo 11 - Pettah", "Colombo 12 - Hulftsdorp",
                "Colombo 13 - Kotahena", "Colombo 14 - Grandpass", "Colombo 15 - Mutwal");

        for (int i = 0; i < colomboAreas.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, colomboAreas.get(i));
        }
        System.out.print("💡 Enter your choice: ");
        int areaChoice = validateNumericInput() - 1;
        String selectedArea = colomboAreas.get(areaChoice);

        System.out.print("💡 Enter your street name: ");
        String streetName = scanner.nextLine();

        System.out.print("💡 Enter an identifier (e.g., apartment number, floor): ");
        String identifier = scanner.nextLine();

        String fullAddress = String.format("%s, %s, %s", selectedArea, streetName, identifier);
        currentUser.updateAddress(fullAddress);
        System.out.println("✅ Address updated to: " + fullAddress);
    }

    static void viewNotifications() {
        System.out.println("\n🔔 Notifications:");
        if (notificationBuffer.isEmpty()) {
            System.out.println("❌ No notifications.");
        } else {
            for (String notification : notificationBuffer) {
                System.out.println(notification);
            }
        }
    }

    static void viewPromotions() {
        System.out.println("\n🎉 Current Promotions:");
        for (Promotion promo : promotions) {
            System.out.println(promo.description);
        }
    }

    static void viewUserProfileAndFavorites() {
        System.out.println("\n👤 User Profile");
        System.out.println(currentUser);
        System.out.println("\n🌟 Your Favorite Pizzas:");
        if (currentUser.favoritePizzas.isEmpty()) {
            System.out.println("❌ No favorite pizzas found.");
        } else {
            for (Pizza pizza : currentUser.favoritePizzas) {
                System.out.println(pizza);
            }
        }
    }

    // Method to validate numeric input
    static int validateNumericInput() {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());  // Parse the user input as integer
                return choice; // If valid, return the parsed value
            } catch (NumberFormatException e) {
                System.out.print("❌ Invalid input. Please enter a valid number: "); // Re-prompt if the input is invalid
            }
        }
    }

    static void addToFavorites() {
        viewPizzas();  // Display available pizzas
        System.out.print("💡 Enter the number of the pizza you want to add to favorites: ");
        int pizzaChoice = validateNumericInput() - 1;
        if (pizzaChoice < 0 || pizzaChoice >= pizzas.size()) {
            System.out.println("❌ Invalid choice.");
            return;
        }
        Pizza pizza = pizzas.get(pizzaChoice);
        currentUser.addToFavorites(pizza);
        System.out.println("✅ Added to favorites: " + pizza.name);
    }

    static void removeFromFavorites() {
        System.out.println("\n🌟 Your Favorite Pizzas:");
        if (currentUser.favoritePizzas.isEmpty()) {
            System.out.println("❌ No favorite pizzas found.");
            return;
        }
        for (int i = 0; i < currentUser.favoritePizzas.size(); i++) {
            System.out.printf("%d. %s\n", i + 1, currentUser.favoritePizzas.get(i));
        }
        System.out.print("💡 Enter the number of the pizza you want to remove from favorites: ");
        int pizzaChoice = validateNumericInput() - 1;
        if (pizzaChoice < 0 || pizzaChoice >= currentUser.favoritePizzas.size()) {
            System.out.println("❌ Invalid choice.");
            return;
        }
        Pizza pizza = currentUser.favoritePizzas.get(pizzaChoice);
        currentUser.removeFromFavorites(pizza);
        System.out.println("✅ Removed from favorites: " + pizza.name);
    }
}