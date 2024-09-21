package com.example.account.dto;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class DeleteAccount {
    //static 클래스를 만들어서 이름지으면 명시적으로 잘 보임

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Request{
        //@Validation 적용할 것 설정 .null 및 최소값.. 그 외의 제한
        @NotNull
        @Min(1)
        private Long userId;

        @NotBlank
        @Size(min=10, max=10)
        private String accountNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response{
        private Long userId;
        private String accountNumber;
        private LocalDateTime unRegisteredAt;

        public static Response from(AccountDto accountDto) {
            return Response.builder()
                    .userId(accountDto.getUserId())
                    .accountNumber(accountDto.getAccountNumber())
                    .unRegisteredAt(accountDto.getUnRegisteredAt())
                    .build();
        }

    }
}
