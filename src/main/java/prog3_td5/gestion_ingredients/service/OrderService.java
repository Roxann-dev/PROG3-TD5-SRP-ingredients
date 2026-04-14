package prog3_td5.gestion_ingredients.service;

import org.springframework.stereotype.Service;
import prog3_td5.gestion_ingredients.entity.Order;
import prog3_td5.gestion_ingredients.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order saveOrder(Order order) {
        return orderRepository.saveOrder(order);
    }
}