package com.example.account.dto;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountInfo {//클라이언트에게 제공할 정보 ... 나중에 복잡한 상황이 생김.. 결국 용도에 따른 Dto가 필요함
    private String accountNumber;
    private Long balance;
}
