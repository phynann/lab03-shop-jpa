
package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import java.time.Instant;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    @Query("select distinct o from Order o join fetch o.lines where o.status = :status")
    List<Order> findByStatusWithLines(@Param("status") OrderStatus status);

    @EntityGraph(attributePaths = {"lines", "lines.product"})
    List<Order> findWithGraphByStatus(OrderStatus status);

    // Find orders by customer email and status
    List<Order> findByCustomerEmailAndStatus(
            String email,
            OrderStatus status
    );

    // Find orders by customer email and status, newest first
    List<Order> findByCustomerEmailAndStatusOrderByPlacedAtDesc(
            String email,
            OrderStatus status
    );

    // Count orders by status
    long countByStatus(OrderStatus status);

    // Find the latest order of a customer
    Optional<Order> findFirstByCustomerIdOrderByPlacedAtDesc(
            Long customerId
    );

    // Pagination with interface projection
    Page<OrderSummary> findByStatus(
            OrderStatus status,
            Pageable pageable
    );

    // Find orders with their lines and products
    @Query("""
            select distinct o
            from Order o
            join fetch o.lines l
            join fetch l.product
            where o.customer.email = :email
            order by o.placedAt desc
            """)
    List<Order> findWithLinesByCustomerEmail(
            @Param("email") String email
    );

    // Count order lines by order status
    @Query("""
            select count(l)
            from OrderLine l
            where l.order.status = :status
            """)
    long countLinesByStatus(
            @Param("status") OrderStatus status
    );

    @Query("""
            select o
            from Order o
            where (:afterPlacedAt is null
                   or o.placedAt < :afterPlacedAt
                   or (o.placedAt = :afterPlacedAt and o.id < :afterId))
            order by o.placedAt desc, o.id desc
            """)
    Slice<OrderSummary> findFeedSlice(
            @Param("afterPlacedAt") Instant afterPlacedAt,
            @Param("afterId") Long afterId,
            Pageable pageable
    );

    Window<Order> findAllByOrderByPlacedAtDescIdDesc(
            ScrollPosition position,
            Limit limit
    );
}
