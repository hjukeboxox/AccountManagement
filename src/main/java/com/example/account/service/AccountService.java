package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static com.example.account.type.AccountStatus.IN_USE;

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
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

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
}
