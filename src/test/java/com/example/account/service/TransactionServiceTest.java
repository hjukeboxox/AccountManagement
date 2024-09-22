package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.AccountDto;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.example.account.type.TransactionResultType.F;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(
                        Transaction.builder()
                                .account(account)
                                .transactionType(TransactionType.USE)
                                .transactionResultType(TransactionResultType.S)
                                .transactionId("transactionId")
                                .transactedAt(LocalDateTime.now())
                                .amount(1000L)
                                .balanceSnapshot(9000L)
                                .build()

                );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService.useBalance(1L, "1000000000", 200L);

        //then
        //저장검증 verify captor가 감 가져감
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L,captor.getValue().getAmount()); //captor가 가져간 저장 벨류와 동일한지
        assertEquals(9800L,captor.getValue().getBalanceSnapshot()); //captor가 가져간 저장 벨류와 동일한지
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot()); //잔액
        assertEquals(1000L, transactionDto.getAmount()); //사용금액
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        //서비스 로직에서 익셉션이 먼저 발생되므로.. 그것부터 처리해준다
        AccountException exception = assertThrows(AccountException.class, () -> transactionService.useBalance(1L, "1000000000",1000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void deleteAccount_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class, () -> transactionService.useBalance(1L, "1000000000",1000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
    void deleteAccountFailed_userUnMatch() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        AccountUser harry = AccountUser.builder()
                .id(13L)
                .name("Harry").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        //when
        AccountException exception = assertThrows(AccountException.class, () -> transactionService.useBalance(1L, "1234567890",1000L));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }


    @Test
    @DisplayName("해지 계좌는 사용할 수 없다.")
    void deleteAccountFailed_alreadyUnregistered() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        //when
        AccountException exception = assertThrows(AccountException.class, () -> transactionService.useBalance(1L, "1234567890",1000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }


    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    void exceedAmount_UseBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(100L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        //then
        AccountException exception = assertThrows(AccountException.class, () -> transactionService.useBalance(1L, "1234567890",1000L));
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        //저장안함 times -> 0
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("pobi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(
                        Transaction.builder()
                                .account(account)
                                .transactionType(TransactionType.USE)
                                .transactionResultType(TransactionResultType.S)
                                .transactionId("transactionId")
                                .transactedAt(LocalDateTime.now())
                                .amount(1000L)
                                .balanceSnapshot(9000L)
                                .build()
                );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedUseTransaction( "1000000000", 200L);

        //then
        //저장검증 verify captor가 감 가져감
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L,captor.getValue().getAmount()); //captor가 가져간 저장 벨류와 동일한지
        assertEquals(10000L,captor.getValue().getBalanceSnapshot()); //captor가 가져간 저장 벨류와 동일한지
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

}