package edu.itc.se.shop_jpa.seed;

import edu.itc.se.shop_jpa.domain.*;
import java.lang.reflect.Method;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {
    private final ApplicationContext context;

    public DataSeeder(ApplicationContext context) {
        this.context = context;
    }

    private Object repository(String beanName) {
        return context.getBean(beanName);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Object target, String methodName, Object... args) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (method.getParameterCount() != args.length) continue;

            boolean matches = true;
            for (int i = 0; i < args.length; i++) {
                Class<?> parameterType = method.getParameterTypes()[i];
                Object arg = args[i];
                if (arg != null && !parameterType.isAssignableFrom(arg.getClass())) {
                    matches = false;
                    break;
                }
            }
            if (!matches) continue;

            try {
                return (T) method.invoke(target, args);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to invoke method " + methodName + " on " + target.getClass(), e);
            }
        }
        throw new IllegalStateException("No matching method found: " + methodName + " on " + target.getClass());
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Object customers = repository("customerRepository");
        Object products = repository("productRepository");
        Object orders = repository("orderRepository");

        Long customerCount = invoke(customers, "count");
        if (customerCount > 0L) return;
        Random rnd = new Random(42);

        List<Customer> cs = invoke(customers, "saveAll", List.of(
                customer("Ada Lovelace", "ada@itc.edu"),
                customer("Grace Hopper", "grace@itc.edu"),
                customer("Alan Turing", "alan@itc.edu")));

        Map<String, Tag> tags = new HashMap<>();
        for (String t : List.of("gaming", "office", "wireless", "ergonomic")) tags.put(t, new Tag(t));

        List<Product> ps = new ArrayList<>();
        ps.add(p("KB-1", "Mechanical Keyboard", Category.HARDWARE, "49.90", 25, tags, "gaming", "office"));
        ps.add(p("KB-3", "Wireless Keyboard",   Category.HARDWARE, "79.00", 4,  tags, "wireless", "office"));
        ps.add(p("KB-7", "Compact Keyboard",    Category.HARDWARE, "59.00", 12, tags, "ergonomic"));
        ps.add(p("MS-1", "Optical Mouse",       Category.HARDWARE, "19.50", 40, tags, "office"));
        ps.add(p("MS-2", "Ergonomic Mouse",     Category.HARDWARE, "45.00", 2,  tags, "ergonomic", "wireless"));
        ps.add(p("MN-1", "27in Monitor",        Category.HARDWARE, "289.00", 8, tags, "office"));
        ps.add(p("HS-1", "USB Headset",         Category.HARDWARE, "65.00", 20, tags, "gaming"));
        ps.add(p("IDE-1", "IDE License",        Category.HARDWARE, "149.00", 100, tags));
        ps.add(p("AV-1", "Antivirus",           Category.HARDWARE, "29.99", 100, tags));
        ps.add(p("BK-1", "Clean Code",          Category.BOOKS, "35.00", 15, tags));
        ps.add(p("BK-2", "Refactoring",         Category.BOOKS, "42.00", 0,  tags));
        ps.add(p("BK-3", "Effective Java",      Category.BOOKS, "39.50", 6,  tags));
        ps = invoke(products, "saveAll", ps);   // tags are persisted via cascade on Product.tags

        List<Order> os = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Customer c = cs.get(rnd.nextInt(cs.size()));
            Order o = new Order(c);
            List<Product> pool = new ArrayList<>(ps);
            Collections.shuffle(pool, rnd);
            int n = 2 + rnd.nextInt(3);                       // 2..4 lines
            for (int j = 0; j < n; j++) o.addLine(pool.get(j), 1 + rnd.nextInt(3));
            os.add(o);
        }
        invoke(orders, "saveAll", os);
    }

    private static Product p(String sku, String name, Category cat, String price, int stock,
                             Map<String, Tag> tags, String... tagNames) {
        Product p = new Product(sku, name, Money.eur(price), cat);
        p.setStock(stock);
        for (String t : tagNames) p.tag(tags.get(t));
        return p;
    }

    private static Customer customer(String name, String email) {
        return new Customer(name, email, null, null);
    }
}