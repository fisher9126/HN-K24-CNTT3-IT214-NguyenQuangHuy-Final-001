package org.example.orderservice.models.services.impl;

import org.example.orderservice.events.OrderCreatedEvent;
import org.example.orderservice.models.constants.OrderStatus;
import org.example.orderservice.models.dto.requests.CreateOrderDetailRequest;
import org.example.orderservice.models.dto.requests.CreateOrderRequest;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.example.orderservice.models.entities.Order;
import org.example.orderservice.models.entities.OrderDetail;
import org.example.orderservice.models.repositories.OrderDetailRepository;
import org.example.orderservice.models.repositories.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    @Test
    void createsOrderUsingProductPricesAndPublishesRecipient() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderDetailRepository detailRepository = mock(OrderDetailRepository.class);
        ProductGatewayService productGateway = mock(ProductGatewayService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        OrderServiceImpl orderService = new OrderServiceImpl(
                orderRepository, detailRepository, productGateway, eventPublisher
        );

        when(productGateway.getProductById(11L)).thenReturn(new ProductResponse(11L, "Keyboard", 25.0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(3L);
            return order;
        });
        when(detailRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<OrderDetail> details = invocation.getArgument(0);
            details.get(0).setId(7L);
            return details;
        });

        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyen Van A",
                "customer@example.com",
                List.of(new CreateOrderDetailRequest(11L, 2))
        );

        var response = orderService.createOrder(request);

        assertEquals(3L, response.id());
        assertEquals(50.0, response.total());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals("Keyboard", response.items().get(0).productName());
        assertEquals(50.0, response.items().get(0).subtotal());
        verify(eventPublisher).publishEvent(new OrderCreatedEvent("customer@example.com"));
    }
}
