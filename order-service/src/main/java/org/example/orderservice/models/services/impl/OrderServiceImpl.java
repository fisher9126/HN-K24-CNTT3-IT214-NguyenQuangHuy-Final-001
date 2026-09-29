package org.example.orderservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.events.OrderCreatedEvent;
import org.example.orderservice.models.constants.OrderStatus;
import org.example.orderservice.models.dto.requests.CreateOrderDetailRequest;
import org.example.orderservice.models.dto.requests.CreateOrderRequest;
import org.example.orderservice.models.dto.responses.OrderDetailResponse;
import org.example.orderservice.models.dto.responses.OrderResponse;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.example.orderservice.models.entities.Order;
import org.example.orderservice.models.entities.OrderDetail;
import org.example.orderservice.models.repositories.OrderDetailRepository;
import org.example.orderservice.models.repositories.OrderRepository;
import org.example.orderservice.models.services.OrderService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductGatewayService productGatewayService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        List<ProductResponse> products = new ArrayList<>(request.items().size());
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderDetailRequest item : request.items()) {
            ProductResponse product = productGatewayService.getProductById(item.productId());
            products.add(product);
            total = total.add(BigDecimal.valueOf(product.price()).multiply(BigDecimal.valueOf(item.quantity())));
        }
        Order order = orderRepository.save(Order.builder()
                .customerName(request.customerName())
                .total(total.doubleValue())
                .status(OrderStatus.PENDING)
                .build());
        List<OrderDetail> details = new ArrayList<>(request.items().size());
        for (int i = 0; i < request.items().size(); i++) {
            CreateOrderDetailRequest item = request.items().get(i);
            ProductResponse product = products.get(i);
            details.add(OrderDetail.builder()
                    .order(order)
                    .productId(product.id())
                    .quantity(item.quantity())
                    .unitPrice(product.price())
                    .build());
        }
        List<OrderDetail> savedDetails = orderDetailRepository.saveAll(details);
        List<OrderDetailResponse> responseItems = new ArrayList<>(savedDetails.size());
        for (int i = 0; i < savedDetails.size(); i++) {
            OrderDetail detail = savedDetails.get(i);
            ProductResponse product = products.get(i);
            double subtotal = BigDecimal.valueOf(detail.getUnitPrice())
                    .multiply(BigDecimal.valueOf(detail.getQuantity()))
                    .doubleValue();
            responseItems.add(new OrderDetailResponse(
                    detail.getId(),
                    detail.getProductId(),
                    product.name(),
                    detail.getQuantity(),
                    detail.getUnitPrice(),
                    subtotal
            ));
        }
        eventPublisher.publishEvent(new OrderCreatedEvent(request.customerEmail()));
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getTotal(),
                order.getStatus(),
                responseItems
        );
    }
}
