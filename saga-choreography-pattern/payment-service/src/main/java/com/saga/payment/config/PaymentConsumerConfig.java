package com.saga.payment.config;

import com.saga.commons.event.OrderEvent;
import com.saga.commons.event.OrderStatus;
import com.saga.commons.event.PaymentEvent;
import com.saga.payment.service.PaymentService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Configuration
public class PaymentConsumerConfig {

    @Autowired
    private PaymentService paymentService;

    @Bean
    public Function<Flux<OrderEvent>, Flux<PaymentEvent>> paymentProcessor(){
        return orderEventFlux -> orderEventFlux.flatMap(this::processPayment);
    }

    private Mono<PaymentEvent> processPayment(OrderEvent orderEvent) {
        //get the userid
        //check balance availability
        //if sufficient then duct the amount and sent Payment successful
        //else send payment status as failed
        if(orderEvent.getOrderStatus().equals(OrderStatus.ORDER_CREATED)){
            return Mono.fromSupplier(() ->this.paymentService.newOrderEvent(orderEvent));
        }else{
            return Mono.fromRunnable(()-> this.paymentService.cancelOrderEvent(orderEvent));
        }
    }
}
