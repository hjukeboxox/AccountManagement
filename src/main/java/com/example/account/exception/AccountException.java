package com.example.account.exception;


import com.example.account.type.ErrorCode;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountException extends RuntimeException {
    //checked 익셉션이 되기떄문에.. 롤백안해줌 ->사용 불편함... 그러해서 보통 RuntimeException을 extends함..기본적인
    private ErrorCode errorCode;
    private String errorMessage;

    public AccountException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }

}
