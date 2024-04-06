package com.saga.payment.service;

import com.saga.commons.dto.OrderRequestDto;
import com.saga.commons.dto.PaymentRequestDto;
import com.saga.commons.event.OrderEvent;
import com.saga.commons.event.PaymentEvent;
import com.saga.commons.event.PaymentStatus;
import com.saga.payment.entity.UserBalance;
import com.saga.payment.entity.UserTransaction;
import com.saga.payment.repo.UserBalanceRepository;
import com.saga.payment.repo.UserTransactionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PaymentService {

    @Autowired
    private UserBalanceRepository userBalanceRepository;
    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @PostConstruct
    public void initUserBalanceInDB(){
        userBalanceRepository.saveAll(Stream.of(new UserBalance(101, 5000),
                new UserBalance(102, 3000),
                new UserBalance(103, 4200),
                new UserBalance(104, 20000),
                new UserBalance(105, 999)).collect(Collectors.toList()));
    }

    @Transactional
    public PaymentEvent newOrderEvent(OrderEvent orderEvent) {
        OrderRequestDto orderRequestDto = orderEvent.getOrderRequestDto();
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto
                (orderRequestDto.getOrderId(), orderRequestDto.getUserId(), orderRequestDto.getAmount());
        return userBalanceRepository.findById(orderRequestDto.getUserId())
                .filter(ub -> ub.getAmount() > orderRequestDto.getAmount())
                .map(ub -> {
                    ub.setAmount(ub.getAmount() - orderRequestDto.getAmount());
                    userTransactionRepository.save(new UserTransaction(orderRequestDto.getOrderId(), orderRequestDto.getUserId(), orderRequestDto.getAmount()));
                    return new PaymentEvent(paymentRequestDto, PaymentStatus.PAYMENT_COMPLETED);
                }).orElse(new PaymentEvent(paymentRequestDto, PaymentStatus.PAYMENT_FAILED));
    }

    @Transactional
    public void cancelOrderEvent(OrderEvent orderEvent) {
        userTransactionRepository.findById(orderEvent.getOrderRequestDto().getOrderId())
                .ifPresent(ut -> {
                    userTransactionRepository.delete(ut);
                    userBalanceRepository.findById(ut.getUserId())
                            .ifPresent(ub -> ub.setAmount(ub.getAmount()+ut.getAmount()));
                });
    }
}
