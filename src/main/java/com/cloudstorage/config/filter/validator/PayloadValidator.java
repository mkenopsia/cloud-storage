package com.cloudstorage.config.filter.validator;

import com.cloudstorage.exception.UserCredentialsValidationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayloadValidator {

    private final Validator validator;

    public void validate(Object payload) {
        var violations = validator.validate(payload);
        if(!violations.isEmpty()) {
            for(var violation : violations) {
                throw new UserCredentialsValidationException(violation.getMessage().substring(1, violation.getMessage().length()-1));
            }
        }
    }
}
