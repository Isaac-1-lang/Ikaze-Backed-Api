package com.ecommerce.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
//@Setter
//@Getter
public class VerifyCodeRequestDto {

    private String email;
    private String code;
}

