package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     * 사용자가 있는지 조회
     * 계좌에 번호를 생성하고
     * 계좌를 저장하고, 그 정보를 넘긴다.
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        //리턴값 옵셔널인데 값이 없거나 문제가있으면 익셉션을 날림 orElseThrow
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        //경우의 수가 많아질경우 지저분해짐 별도의 매소드로 빼는것이 좋다.
        validateCreateAccount(accountUser);


        //혹은 new IllegalStateException() , new IllegalArgumentException().. 상황에 맞는것을.. 상황에 맞는게 없다면 커스텀 익셉션을 만들어야함.

        //현재 제일 마지막에 생성된 계좌번호를 가져오기. 그 계좌의 번호보다 +1 숫자를 넣어줌
        //어카운드를 숫자로 파스한다음에 +1 연산을 해주고 다시 문자로 바꿈
        //+ "" 문자로 바뀌주기.. toString써도 됨
        //계좌번호가 없는경우 orElse로 계좌번호 1000000000로 계좌번호만들어줌

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
                .orElse("1000000000");

        //위 정보 저장 후 리턴
        //엔티티클래스 그대로 넘기면..레이지 로딩 + 추가쿼리 날림 시 -> 오류 발생할수있는 트랜젝션 문제 해결 및
        // 혹은 모든 정보를 보내주진않아도 되어서-> DTO생성해서 넘기기
        /* 일회성 변수는 쓰지않도록 한다..
        Account account = accountRepository.save(
                Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build()
        );
        */

        return AccountDto.fromEntity(accountRepository.save(
                Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build())
        );
    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
        }
    }

    @Transactional
    public Account getAccount(Long id) {
        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }


    @Transactional
    public AccountDto deleteAccount(@NotNull @Min(1) Long userId, @NotBlank @Size(min = 10, max = 10) String accountNumber) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    @Transactional  //안붙이면 레이지로딩이라 정상적 조회가 안됌 -> null 뜸
    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }

    }

    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(()-> new AccountException(USER_NOT_FOUND));
        List<Account> accounts = accountRepository.findByAccountUser(accountUser);
        //List<Account> -> List<AccountDto>로 변환해서 -> 리스트로바꾸고 리턴
        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
        //.map(account-> AccountDto.formEntity(account))도 같은 동작
    }
}
