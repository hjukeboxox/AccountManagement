package com.example.account.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
    //스프링에서 어플리케이션실행시 자동스캔... 하면서 jpaAuditing이 켜지게됨 -> db에 관련된 어노테이션이 붙은 필드에 자동으로 저장해줌.

}
