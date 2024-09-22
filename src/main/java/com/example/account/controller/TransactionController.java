package com.example.account.controller;

import com.example.account.dto.CancelBalance;
import com.example.account.dto.QueryTransactionResponse;
import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transaction/use")
    public UseBalance.Response useBalance(
            @Valid @RequestBody UseBalance.Request request
    ) {
        //  1회성 변수는 가급정 생성하지 않도록 신경쓴다
// TransactionDto transactionDto = transactionService.useBalance(request.getUserId(), request.getAccountNumber(), request.getAmount());
        //fail일때 저장해주어야함. 지금은 성공만 가정함 -> try catch로 적용해주기

        try {
            return UseBalance.Response.from(
                    transactionService.useBalance(request.getUserId(),
                            request.getAccountNumber(), request.getAmount())
            );
        } catch (AccountException e) {
            //비지니스로직에서 의도적인 에러가 발생하였을때 여기서는 UseBalance로직에서 에러발생 가능성
            log.error("Failed to use balance.");

            //아래 코드에서 실패내용을 저장해줌
            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @PostMapping("/transaction/cancel")
    public CancelBalance.Response cancelBalance(
            @Valid @RequestBody CancelBalance.Request request
    ) {
        //  1회성 변수는 가급정 생성하지 않도록 신경쓴다
// TransactionDto transactionDto = transactionService.useBalance(request.getUserId(), request.getAccountNumber(), request.getAmount());
        //fail일때 저장해주어야함. 지금은 성공만 가정함 -> try catch로 적용해주기

        try {
            return CancelBalance.Response.from(
                    transactionService.cancelBalance(request.getTransactionId(),
                            request.getAccountNumber(), request.getAmount())
            );
        } catch (AccountException e) {
            //비지니스로직에서 의도적인 에러가 발생하였을때 여기서는 UseBalance로직에서 에러발생 가능성
            log.error("Failed to use balance.");

            //아래 코드에서 실패내용을 저장해줌
            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(
            @PathVariable String transactionId) {
        return QueryTransactionResponse.from(
                transactionService.queryTransaction(transactionId)
        );
    }


}
